package com.convertidor.yt.dto;

import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.Format;
import com.convertidor.yt.model.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del mapeo {@link JobStatusResponse#from(ConversionJob)} que traduce el
 * modelo interno a la vista expuesta por la API.
 */
@DisplayName("JobStatusResponse: mapeo del estado del trabajo")
class JobStatusResponseTest {

    @Test
    @DisplayName("Un trabajo READY expone la URL de descarga")
    void mapeaTrabajoListoConUrlDeDescarga() {
        var job = new ConversionJob("abc-123", Format.MP3);
        job.setStatus(JobStatus.READY);
        job.setProgress(100);
        job.setFileName("cancion.mp3");
        job.setOutputFile(Path.of("/tmp/cancion.mp3"));

        var res = JobStatusResponse.from(job);

        assertThat(res.jobId()).isEqualTo("abc-123");
        assertThat(res.status()).isEqualTo("READY");
        assertThat(res.progress()).isEqualTo(100);
        assertThat(res.format()).isEqualTo("MP3");
        assertThat(res.fileName()).isEqualTo("cancion.mp3");
        assertThat(res.downloadUrl()).isEqualTo("/api/v1/conversions/abc-123/download");
        assertThat(res.error()).isNull();
    }

    @Test
    @DisplayName("Un trabajo en progreso no expone URL de descarga")
    void noExponeUrlDeDescargaSiNoEstaListo() {
        var job = new ConversionJob("abc-123", Format.MP4);
        job.setStatus(JobStatus.PROCESSING);
        job.setProgress(40);

        var res = JobStatusResponse.from(job);

        assertThat(res.status()).isEqualTo("PROCESSING");
        assertThat(res.progress()).isEqualTo(40);
        assertThat(res.format()).isEqualTo("MP4");
        assertThat(res.downloadUrl()).isNull();
    }

    @Test
    @DisplayName("Un trabajo fallido expone el mensaje de error")
    void exponeElMensajeDeErrorCuandoFalla() {
        var job = new ConversionJob("abc-123", Format.MP3);
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage("yt-dlp falló");

        var res = JobStatusResponse.from(job);

        assertThat(res.status()).isEqualTo("FAILED");
        assertThat(res.downloadUrl()).isNull();
        assertThat(res.error()).isEqualTo("yt-dlp falló");
    }
}
