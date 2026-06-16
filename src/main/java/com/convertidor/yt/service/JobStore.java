package com.convertidor.yt.service;

import com.convertidor.yt.config.ConverterProperties;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén en memoria de los trabajos de conversión. Limpia periódicamente los
 * trabajos antiguos y sus archivos para no llenar el disco.
 */
@Component
public class JobStore {

    private static final Logger log = LoggerFactory.getLogger(JobStore.class);

    private final Map<String, ConversionJob> jobs = new ConcurrentHashMap<>();
    private final ConverterProperties properties;

    public JobStore(ConverterProperties properties) {
        this.properties = properties;
    }

    public void save(ConversionJob job) {
        jobs.put(job.getId(), job);
    }

    public Optional<ConversionJob> find(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public void remove(String id) {
        ConversionJob job = jobs.remove(id);
        if (job != null) {
            deleteFileQuietly(job.getOutputFile());
        }
    }

    /**
     * Cada minuto elimina trabajos cuyo tiempo de retención expiró.
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(properties.getRetentionMinutes()));
        jobs.values().removeIf(job -> {
            boolean expired = job.getCreatedAt().isBefore(threshold);
            boolean finished = job.getStatus() == JobStatus.READY || job.getStatus() == JobStatus.FAILED;
            if (expired && finished) {
                log.info("Eliminando trabajo expirado {}", job.getId());
                deleteFileQuietly(job.getOutputFile());
                return true;
            }
            return false;
        });
    }

    private void deleteFileQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
            Path parent = file.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                try (var stream = Files.list(parent)) {
                    if (stream.findAny().isEmpty()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo {}: {}", file, e.getMessage());
        }
    }
}
