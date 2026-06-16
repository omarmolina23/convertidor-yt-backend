package com.convertidor.yt.model;

/**
 * Estados del ciclo de vida de un trabajo de conversión.
 */
public enum JobStatus {
    /** En cola, aún no inicia el procesamiento. */
    PENDING,
    /** yt-dlp/ffmpeg en ejecución. */
    PROCESSING,
    /** Archivo listo para descargar. */
    READY,
    /** Falló la conversión. */
    FAILED
}
