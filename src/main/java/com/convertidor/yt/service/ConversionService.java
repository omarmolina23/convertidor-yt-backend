package com.convertidor.yt.service;

import com.convertidor.yt.dto.ConversionRequest;
import com.convertidor.yt.exception.JobNotFoundException;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Orquesta el ciclo de vida de una conversión: crea el trabajo, lo ejecuta de
 * forma asíncrona y actualiza su estado/progreso.
 */
@Service
public class ConversionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionService.class);

    private final YtDlpService ytDlpService;
    private final JobStore jobStore;
    private final TaskExecutor executor;

    public ConversionService(YtDlpService ytDlpService,
                             JobStore jobStore,
                             @Qualifier("conversionExecutor") TaskExecutor executor) {
        this.ytDlpService = ytDlpService;
        this.jobStore = jobStore;
        this.executor = executor;
    }

    /**
     * Crea el trabajo, lo registra y dispara el procesamiento en el pool de hilos.
     * El método retorna de inmediato; el HTTP POST responde 202 sin esperar a que
     * termine la descarga (el cliente consulta el progreso por polling).
     */
    public ConversionJob submit(ConversionRequest request) {
        ConversionJob job = new ConversionJob(UUID.randomUUID().toString(), request.format());
        jobStore.save(job);
        executor.execute(() -> process(job, request));
        return job;
    }

    public ConversionJob getJob(String id) {
        return jobStore.find(id).orElseThrow(() -> new JobNotFoundException(id));
    }

    void process(ConversionJob job, ConversionRequest request) {
        try {
            job.setStatus(JobStatus.PROCESSING);
            jobStore.save(job);
            Path output = ytDlpService.download(job, request);
            job.setOutputFile(output);
            job.setFileName(output.getFileName().toString());
            job.setProgress(100);
            job.setStatus(JobStatus.READY);
            jobStore.save(job);
            log.info("Trabajo {} listo: {}", job.getId(), job.getFileName());
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            jobStore.save(job);
            log.error("Trabajo {} falló: {}", job.getId(), e.getMessage());
        }
    }
}
