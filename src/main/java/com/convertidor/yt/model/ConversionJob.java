package com.convertidor.yt.model;

import java.time.Instant;

/**
 * Representa un trabajo de conversión en memoria. Mutable porque su estado y
 * progreso cambian a medida que yt-dlp avanza.
 */
public class ConversionJob {

    private final String id;
    private final Format format;
    private final Instant createdAt;

    private volatile JobStatus status = JobStatus.PENDING;
    private volatile int progress = 0;            // 0-100
    private volatile String errorMessage;
    private volatile String storageKey;           // ruta local o key del objeto en R2
    private volatile String fileName;             // nombre amigable para descarga

    public ConversionJob(String id, Format format) {
        this.id = id;
        this.format = format;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Format getFormat() {
        return format;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
