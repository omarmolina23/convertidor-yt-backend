package com.convertidor.yt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades configurables del convertidor (prefijo "converter" en application.yml).
 */
@ConfigurationProperties(prefix = "converter")
public class ConverterProperties {

    /** Ruta del ejecutable yt-dlp. */
    private String ytDlpPath = "yt-dlp";

    /** Ruta del ejecutable ffmpeg (yt-dlp lo usa internamente). */
    private String ffmpegPath = "ffmpeg";

    /** Directorio de trabajo donde se guardan los archivos generados. */
    private String workDir = System.getProperty("java.io.tmpdir") + "/convertidor-yt";

    /** Minutos tras los cuales un trabajo terminado y su archivo se eliminan. */
    private long retentionMinutes = 30;

    /** Timeout en segundos para el proceso de yt-dlp. */
    private long processTimeoutSeconds = 600;

    public String getYtDlpPath() {
        return ytDlpPath;
    }

    public void setYtDlpPath(String ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public long getRetentionMinutes() {
        return retentionMinutes;
    }

    public void setRetentionMinutes(long retentionMinutes) {
        this.retentionMinutes = retentionMinutes;
    }

    public long getProcessTimeoutSeconds() {
        return processTimeoutSeconds;
    }

    public void setProcessTimeoutSeconds(long processTimeoutSeconds) {
        this.processTimeoutSeconds = processTimeoutSeconds;
    }
}
