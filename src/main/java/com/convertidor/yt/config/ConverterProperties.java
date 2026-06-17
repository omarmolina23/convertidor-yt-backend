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

    /** Archivo de cookies (formato Netscape) para autenticar yt-dlp ante YouTube. Vacío = sin cookies. */
    private String cookiesFile = "";

    /** Proxy para las descargas de yt-dlp (http://… o socks5://…). Vacío = sin proxy. */
    private String proxy = "";

    /** Hilos núcleo del pool de conversión. */
    private int poolCoreSize = 2;

    /** Hilos máximos del pool de conversión. */
    private int poolMaxSize = 4;

    /** Capacidad de la cola de trabajos en espera. */
    private int queueCapacity = 50;

    /** Duración máxima del video en segundos (0 = sin límite). */
    private long maxDurationSeconds = 0;

    /** Estrategia de almacenamiento del archivo convertido: "local" o "r2". */
    private String storageType = "local";

    /** Endpoint S3-compatible (R2: https://&lt;cuenta&gt;.r2.cloudflarestorage.com). */
    private String s3Endpoint = "";

    /** Bucket de object storage. */
    private String s3Bucket = "";

    /** Access Key ID (token de R2). */
    private String s3AccessKey = "";

    /** Secret Access Key (token de R2). */
    private String s3SecretKey = "";

    /** Región de firma ("auto" en R2). */
    private String s3Region = "auto";

    /** Minutos de validez de las URLs prefirmadas de descarga. */
    private long s3PresignMinutes = 10;

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

    public String getCookiesFile() {
        return cookiesFile;
    }

    public void setCookiesFile(String cookiesFile) {
        this.cookiesFile = cookiesFile;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getPoolCoreSize() {
        return poolCoreSize;
    }

    public void setPoolCoreSize(int poolCoreSize) {
        this.poolCoreSize = poolCoreSize;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public void setMaxDurationSeconds(long maxDurationSeconds) {
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getS3AccessKey() {
        return s3AccessKey;
    }

    public void setS3AccessKey(String s3AccessKey) {
        this.s3AccessKey = s3AccessKey;
    }

    public String getS3SecretKey() {
        return s3SecretKey;
    }

    public void setS3SecretKey(String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
    }

    public String getS3Region() {
        return s3Region;
    }

    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    public long getS3PresignMinutes() {
        return s3PresignMinutes;
    }

    public void setS3PresignMinutes(long s3PresignMinutes) {
        this.s3PresignMinutes = s3PresignMinutes;
    }
}
