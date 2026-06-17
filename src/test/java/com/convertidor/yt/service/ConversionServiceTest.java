package com.convertidor.yt.service;

import com.convertidor.yt.dto.ConversionRequest;
import com.convertidor.yt.exception.ConversionException;
import com.convertidor.yt.exception.JobNotFoundException;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.Format;
import com.convertidor.yt.model.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de orquestación de {@link ConversionService} con dependencias
 * simuladas (Mockito).
 *
 * <p>Se inyecta un ejecutor síncrono ({@code Runnable::run}) para que el
 * procesamiento corra en el mismo hilo y el resultado pueda verificarse sin
 * esperas ni condiciones de carrera.
 */
@DisplayName("ConversionService: orquestación de la conversión")
class ConversionServiceTest {

    private YtDlpService ytDlpService;
    private JobStore jobStore;
    private ConversionService service;

    @BeforeEach
    void setUp() {
        ytDlpService = mock(YtDlpService.class);
        jobStore = mock(JobStore.class);
        service = new ConversionService(ytDlpService, jobStore, Runnable::run);
    }

    @Test
    @DisplayName("submit guarda el trabajo y lo deja READY al terminar la descarga")
    void submitGuardaElTrabajoYLoDejaListo() {
        var request = new ConversionRequest(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", Format.MP3, "192", null, null);
        when(ytDlpService.download(any(), any())).thenReturn(Path.of("cancion.mp3"));

        ConversionJob job = service.submit(request);

        verify(jobStore, atLeastOnce()).save(job);
        verify(ytDlpService).download(eq(job), eq(request));
        assertThat(job.getStatus()).isEqualTo(JobStatus.READY);
        assertThat(job.getProgress()).isEqualTo(100);
        assertThat(job.getFileName()).isEqualTo("cancion.mp3");
        assertThat(job.getOutputFile()).isEqualTo(Path.of("cancion.mp3"));
        assertThat(job.getFormat()).isEqualTo(Format.MP3);
    }

    @Test
    @DisplayName("submit marca FAILED y guarda el error si la descarga lanza excepción")
    void submitMarcaFalloSiLaDescargaLanzaExcepcion() {
        var request = new ConversionRequest(
                "https://youtu.be/dQw4w9WgXcQ", Format.MP4, "720", null, null);
        when(ytDlpService.download(any(), any()))
                .thenThrow(new ConversionException("yt-dlp falló"));

        ConversionJob job = service.submit(request);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("yt-dlp falló");
        assertThat(job.getProgress()).isZero();
    }

    @Test
    @DisplayName("getJob devuelve el trabajo existente")
    void getJobDevuelveElTrabajoExistente() {
        var job = new ConversionJob("id-1", Format.MP3);
        when(jobStore.find("id-1")).thenReturn(Optional.of(job));

        assertThat(service.getJob("id-1")).isSameAs(job);
    }

    @Test
    @DisplayName("getJob lanza JobNotFoundException si el id no existe")
    void getJobLanzaExcepcionSiNoExiste() {
        when(jobStore.find("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJob("missing"))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("missing");
    }
}
