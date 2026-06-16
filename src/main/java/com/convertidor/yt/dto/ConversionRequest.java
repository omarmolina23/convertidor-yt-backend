package com.convertidor.yt.dto;

import com.convertidor.yt.model.Format;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Petición de conversión enviada por el cliente.
 *
 * @param url       enlace de YouTube (validado por patrón para evitar inyección de comandos)
 * @param format    MP3 o MP4
 * @param quality   calidad opcional: bitrate de audio (ej. "192") para MP3 o
 *                  altura máxima en píxeles (ej. "720", "1080") para MP4
 * @param startTime inicio del recorte opcional, formato HH:MM:SS o MM:SS
 * @param endTime   fin del recorte opcional, formato HH:MM:SS o MM:SS
 */
public record ConversionRequest(

        @NotBlank(message = "La URL es obligatoria")
        @Pattern(
                regexp = "^(https?://)?(www\\.)?(youtube\\.com/(watch\\?v=|shorts/|embed/)|youtu\\.be/)[A-Za-z0-9_\\-]{6,}.*$",
                message = "Debe ser un enlace válido de YouTube"
        )
        String url,

        @NotNull(message = "El formato es obligatorio (MP3 o MP4)")
        Format format,

        @Pattern(regexp = "^[0-9]{2,4}$", message = "La calidad debe ser numérica (ej. 192 o 720)")
        String quality,

        @Pattern(regexp = "^([0-9]{1,2}:)?[0-5]?[0-9]:[0-5][0-9]$", message = "startTime debe tener formato HH:MM:SS o MM:SS")
        String startTime,

        @Pattern(regexp = "^([0-9]{1,2}:)?[0-5]?[0-9]:[0-5][0-9]$", message = "endTime debe tener formato HH:MM:SS o MM:SS")
        String endTime
) {
    /** Indica si la petición pide un recorte por intervalo de tiempo. */
    public boolean hasInterval() {
        return startTime != null && !startTime.isBlank()
                && endTime != null && !endTime.isBlank();
    }
}
