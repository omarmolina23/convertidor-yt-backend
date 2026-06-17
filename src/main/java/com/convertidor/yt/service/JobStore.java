package com.convertidor.yt.service;

import com.convertidor.yt.config.ConverterProperties;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.Format;
import com.convertidor.yt.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Almacén de trabajos respaldado por Redis, compartido entre réplicas.
 *
 * <p>Los metadatos de cada trabajo viven en un hash {@code job:{id}} con TTL =
 * retención, así que expiran solos (reemplaza la limpieza en memoria del mapa).
 * El archivo generado sigue en el disco local del {@code work-dir}; un barrido
 * periódico elimina los directorios de trabajo más antiguos que la retención.
 */
@Component
public class JobStore {

    private static final Logger log = LoggerFactory.getLogger(JobStore.class);

    private static final String KEY_PREFIX = "job:";

    private final StringRedisTemplate redis;
    private final ConverterProperties properties;

    public JobStore(StringRedisTemplate redis, ConverterProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** Guarda (o actualiza) el trabajo y refresca su TTL. */
    public void save(ConversionJob job) {
        String key = KEY_PREFIX + job.getId();
        Map<String, String> hash = new HashMap<>();
        hash.put("format", job.getFormat().name());
        hash.put("status", job.getStatus().name());
        hash.put("progress", Integer.toString(job.getProgress()));
        putIfPresent(hash, "fileName", job.getFileName());
        putIfPresent(hash, "errorMessage", job.getErrorMessage());
        if (job.getOutputFile() != null) {
            hash.put("outputFile", job.getOutputFile().toString());
        }
        redis.opsForHash().putAll(key, hash);
        redis.expire(key, Duration.ofMinutes(properties.getRetentionMinutes()));
    }

    public Optional<ConversionJob> find(String id) {
        Map<Object, Object> hash = redis.opsForHash().entries(KEY_PREFIX + id);
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        ConversionJob job = new ConversionJob(id, Format.valueOf((String) hash.get("format")));
        job.setStatus(JobStatus.valueOf((String) hash.get("status")));
        job.setProgress(Integer.parseInt((String) hash.get("progress")));
        Object fileName = hash.get("fileName");
        if (fileName != null) {
            job.setFileName((String) fileName);
        }
        Object error = hash.get("errorMessage");
        if (error != null) {
            job.setErrorMessage((String) error);
        }
        Object outputFile = hash.get("outputFile");
        if (outputFile != null) {
            job.setOutputFile(Path.of((String) outputFile));
        }
        return Optional.of(job);
    }

    public void remove(String id) {
        redis.delete(KEY_PREFIX + id);
    }

    private void putIfPresent(Map<String, String> hash, String field, String value) {
        if (value != null) {
            hash.put(field, value);
        }
    }

    // ---------------------------------------------------------------------
    // Limpieza de archivos (los metadatos expiran solos por TTL de Redis)
    // ---------------------------------------------------------------------

    /**
     * Cada minuto elimina los directorios de trabajo cuyo tiempo de retención
     * expiró. Se basa en la antigüedad del directorio en disco, de modo que la
     * limpieza no depende de tener los metadatos en memoria.
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanupFiles() {
        Path workDir = Path.of(properties.getWorkDir());
        if (!Files.isDirectory(workDir)) {
            return;
        }
        Instant threshold = Instant.now().minus(Duration.ofMinutes(properties.getRetentionMinutes()));
        try (var entries = Files.list(workDir)) {
            entries.filter(Files::isDirectory).forEach(dir -> deleteIfExpired(dir, threshold));
        } catch (IOException e) {
            log.warn("No se pudo listar el work-dir {}: {}", workDir, e.getMessage());
        }
    }

    private void deleteIfExpired(Path dir, Instant threshold) {
        try {
            if (Files.getLastModifiedTime(dir).toInstant().isBefore(threshold)) {
                deleteRecursively(dir);
                log.info("Eliminado directorio de trabajo expirado {}", dir.getFileName());
            }
        } catch (IOException e) {
            log.warn("No se pudo limpiar {}: {}", dir, e.getMessage());
        }
    }

    private void deleteRecursively(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("No se pudo borrar {}: {}", p, e.getMessage());
                }
            });
        }
    }
}
