package com.convertidor.yt.service;

import com.convertidor.yt.config.ConverterProperties;
import com.convertidor.yt.model.ConversionJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

/**
 * Almacenamiento en el disco local del work-dir. Estrategia por defecto (una
 * sola instancia): el archivo ya queda escrito por yt-dlp, así que solo se sirve
 * y se limpia por antigüedad.
 */
@Service
@ConditionalOnProperty(name = "converter.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final ConverterProperties properties;

    public LocalStorageService(ConverterProperties properties) {
        this.properties = properties;
    }

    @Override
    public String store(ConversionJob job, Path localFile) {
        // El archivo ya está en el work-dir; la referencia es su ruta absoluta.
        return localFile.toAbsolutePath().toString();
    }

    @Override
    public ResponseEntity<?> download(ConversionJob job) {
        Resource resource = new FileSystemResource(Path.of(job.getStorageKey()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(StorageService.contentType(job.getFormat())))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + job.getFileName() + "\"")
                .body(resource);
    }

    /**
     * Cada minuto elimina los directorios de trabajo más antiguos que la
     * retención (basado en la antigüedad del directorio en disco).
     */
    @Override
    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpired() {
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
