package com.convertidor.yt.dto;

/**
 * Respuesta al crear un trabajo de conversión.
 *
 * @param jobId     identificador del trabajo
 * @param status    estado inicial
 * @param statusUrl URL para consultar el progreso
 */
public record ConversionResponse(String jobId, String status, String statusUrl) {
}
