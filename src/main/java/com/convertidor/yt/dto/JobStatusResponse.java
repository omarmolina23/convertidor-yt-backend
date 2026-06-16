package com.convertidor.yt.dto;

import com.convertidor.yt.model.ConversionJob;

/**
 * Estado de un trabajo expuesto al cliente para hacer polling.
 */
public record JobStatusResponse(
        String jobId,
        String status,
        int progress,
        String format,
        String fileName,
        String downloadUrl,
        String error
) {
    public static JobStatusResponse from(ConversionJob job) {
        boolean ready = job.getStatus().name().equals("READY");
        return new JobStatusResponse(
                job.getId(),
                job.getStatus().name(),
                job.getProgress(),
                job.getFormat().name(),
                job.getFileName(),
                ready ? "/api/v1/conversions/" + job.getId() + "/download" : null,
                job.getErrorMessage()
        );
    }
}
