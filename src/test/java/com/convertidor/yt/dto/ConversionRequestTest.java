package com.convertidor.yt.dto;

import com.convertidor.yt.model.Format;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de las restricciones de Bean Validation declaradas en
 * {@link ConversionRequest} y de su método {@link ConversionRequest#hasInterval()}.
 *
 * <p>Se valida con un {@link Validator} directo (sin levantar el contexto de
 * Spring) para que sean rápidas y enfocadas en las reglas del DTO.
 */
@DisplayName("ConversionRequest: validación de la petición")
class ConversionRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private ConversionRequest request(
            String url, Format format, String quality, String start, String end) {
        return new ConversionRequest(url, format, quality, start, end);
    }

    @Test
    @DisplayName("Una petición válida no produce violaciones")
    void peticionValidaNoTieneViolaciones() {
        var req = request(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", Format.MP3, "192", null, null);
        assertThat(validator.validate(req)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/abc123xyz",
            "youtube.com/watch?v=abcdef123"
    })
    @DisplayName("Acepta distintas formas de enlace de YouTube")
    void aceptaEnlacesDeYouTube(String url) {
        var req = request(url, Format.MP4, "720", null, null);
        assertThat(validator.validate(req)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/video",
            "https://vimeo.com/123456",
            "not-a-url",
            "ftp://youtube.com/watch?v=abcdef"
    })
    @DisplayName("Rechaza enlaces que no son de YouTube")
    void rechazaEnlacesQueNoSonDeYouTube(String url) {
        var req = request(url, Format.MP3, "192", null, null);
        assertThat(validator.validate(req)).isNotEmpty();
    }

    @Test
    @DisplayName("Rechaza la URL en blanco")
    void rechazaUrlEnBlanco() {
        var req = request("   ", Format.MP3, "192", null, null);
        assertThat(validator.validate(req)).isNotEmpty();
    }

    @Test
    @DisplayName("Rechaza el formato nulo")
    void rechazaFormatoNulo() {
        var req = request("https://youtu.be/dQw4w9WgXcQ", null, "192", null, null);
        assertThat(validator.validate(req)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1", "12345", "19.2"})
    @DisplayName("Rechaza calidades que no son 2–4 dígitos")
    void rechazaCalidadNoNumerica(String quality) {
        var req = request("https://youtu.be/dQw4w9WgXcQ", Format.MP3, quality, null, null);
        assertThat(validator.validate(req)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00:30", "01:45", "1:02:03", "12:59:59"})
    @DisplayName("Acepta tiempos MM:SS y HH:MM:SS")
    void aceptaTiemposValidos(String time) {
        var req = request("https://youtu.be/dQw4w9WgXcQ", Format.MP3, "192", time, time);
        assertThat(validator.validate(req)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1:90", "99:99", "abc", "12:3"})
    @DisplayName("Rechaza tiempos con formato inválido")
    void rechazaTiemposConFormatoInvalido(String time) {
        var req = request("https://youtu.be/dQw4w9WgXcQ", Format.MP3, "192", time, null);
        assertThat(validator.validate(req)).isNotEmpty();
    }

    @Test
    @DisplayName("hasInterval() es true solo cuando hay inicio y fin")
    void hasIntervalSoloConAmbosTiempos() {
        assertThat(request("u", Format.MP3, null, "00:10", "00:20").hasInterval()).isTrue();
        assertThat(request("u", Format.MP3, null, "00:10", null).hasInterval()).isFalse();
        assertThat(request("u", Format.MP3, null, null, "00:20").hasInterval()).isFalse();
        assertThat(request("u", Format.MP3, null, "   ", "00:20").hasInterval()).isFalse();
        assertThat(request("u", Format.MP3, null, null, null).hasInterval()).isFalse();
    }
}
