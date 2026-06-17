package com.convertidor.yt.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link GlobalExceptionHandler}: cada excepción se traduce al código
 * HTTP y cuerpo JSON esperados. Se invoca el handler directamente (sin MVC).
 */
@DisplayName("GlobalExceptionHandler: mapeo de excepciones a HTTP")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("JobNotFoundException → 404 con el id en el mensaje")
    void trabajoNoEncontradoDevuelve404() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleNotFound(new JobNotFoundException("xyz"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).containsEntry("status", 404);
        assertThat(res.getBody()).containsKey("timestamp");
        assertThat(res.getBody().get("message").toString()).contains("xyz");
    }

    @Test
    @DisplayName("ConversionException → 422")
    void errorDeConversionDevuelve422() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleConversion(new ConversionException("formato no soportado"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).containsEntry("message", "formato no soportado");
    }

    @Test
    @DisplayName("Excepción genérica → 500")
    void errorGenericoDevuelve500() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleGeneric(new RuntimeException("boom"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().get("message").toString()).contains("boom");
    }
}
