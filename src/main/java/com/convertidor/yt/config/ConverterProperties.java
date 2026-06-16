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

    /** Usa aria2c como descargador externo (descargas multi-conexión, más rápidas). */
    private boolean useAria2 = true;

    /** Ruta del ejecutable aria2c. */
    private String aria2Path = "aria2c";

    /** Argumentos para aria2c (conexiones/segmentos). Equilibrio velocidad vs. rate-limit. */
    private String aria2Args = "-x8 -s8 -k1M";

    /** Fragmentos a descargar en paralelo cuando el formato es fragmentado (DASH/HLS). */
    private int concurrentFragments = 4;

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

    public boolean isUseAria2() {
        return useAria2;
    }

    public void setUseAria2(boolean useAria2) {
        this.useAria2 = useAria2;
    }

    public String getAria2Path() {
        return aria2Path;
    }

    public void setAria2Path(String aria2Path) {
        this.aria2Path = aria2Path;
    }

    public String getAria2Args() {
        return aria2Args;
    }

    public void setAria2Args(String aria2Args) {
        this.aria2Args = aria2Args;
    }

    public int getConcurrentFragments() {
        return concurrentFragments;
    }

    public void setConcurrentFragments(int concurrentFragments) {
        this.concurrentFragments = concurrentFragments;
    }
}
