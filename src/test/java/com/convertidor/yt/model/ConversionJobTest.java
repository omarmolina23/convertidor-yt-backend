package com.convertidor.yt.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del modelo en memoria {@link ConversionJob}: valores iniciales y la
 * regla de acotado del progreso.
 */
@DisplayName("ConversionJob: modelo de trabajo")
class ConversionJobTest {

    @Test
    @DisplayName("El estado inicial es PENDING con progreso 0")
    void estadoInicialEsPendienteConProgresoCero() {
        var job = new ConversionJob("id-1", Format.MP3);

        assertThat(job.getId()).isEqualTo("id-1");
        assertThat(job.getFormat()).isEqualTo(Format.MP3);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getProgress()).isZero();
        assertThat(job.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(job.getErrorMessage()).isNull();
        assertThat(job.getStorageKey()).isNull();
        assertThat(job.getFileName()).isNull();
    }

    @Test
    @DisplayName("El progreso se acota siempre al rango 0–100")
    void elProgresoSeAcotaAlRango0a100() {
        var job = new ConversionJob("id", Format.MP4);

        job.setProgress(50);
        assertThat(job.getProgress()).isEqualTo(50);

        job.setProgress(-10);
        assertThat(job.getProgress()).isZero();

        job.setProgress(150);
        assertThat(job.getProgress()).isEqualTo(100);
    }

    @Test
    @DisplayName("Guarda estado, archivo, nombre y mensaje de error")
    void guardaEstadoArchivoYError() {
        var job = new ConversionJob("id", Format.MP4);

        job.setStatus(JobStatus.READY);
        job.setFileName("video.mp4");
        job.setStorageKey("id/video.mp4");
        job.setErrorMessage("algo salió mal");

        assertThat(job.getStatus()).isEqualTo(JobStatus.READY);
        assertThat(job.getFileName()).isEqualTo("video.mp4");
        assertThat(job.getStorageKey()).isEqualTo("id/video.mp4");
        assertThat(job.getErrorMessage()).isEqualTo("algo salió mal");
    }
}
