package com.convertidor.yt.service;

import com.convertidor.yt.config.ConverterProperties;
import com.convertidor.yt.dto.ConversionRequest;
import com.convertidor.yt.exception.ConversionException;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsula la construcción y ejecución del comando yt-dlp. Reporta el progreso
 * al {@link ConversionJob} leyendo la salida estándar línea por línea.
 */
@Service
public class YtDlpService {

    private static final Logger log = LoggerFactory.getLogger(YtDlpService.class);

    /** Captura el porcentaje de las líneas "[download]  42.3% of ..." (descargador nativo). */
    private static final Pattern PROGRESS = Pattern.compile("\\[download]\\s+(\\d{1,3})(?:\\.\\d+)?%");

    /** Captura el porcentaje de las líneas de aria2c, p. ej. "[#7d0a3e 50MiB/119MiB(42%) ...]". */
    private static final Pattern ARIA2_PROGRESS = Pattern.compile("\\((\\d{1,3})(?:\\.\\d+)?%\\)");

    private final ConverterProperties properties;

    public YtDlpService(ConverterProperties properties) {
        this.properties = properties;
    }

    /**
     * Descarga y convierte el contenido del trabajo. Devuelve la ruta del archivo final.
     *
     * <p>Para recortes por intervalo se intenta primero la descarga parcial con
     * {@code --download-sections} (rápida, poco ancho de banda). Si falla —típicamente
     * con un 403 cuando ffmpeg pide la URL directamente a YouTube— se recurre a un
     * fallback robusto: descargar el medio completo con el descargador nativo de
     * yt-dlp y recortar el intervalo localmente con ffmpeg.
     */
    public Path download(ConversionJob job, ConversionRequest request) {
        Path jobDir = createJobDir(job);

        if (request.hasInterval()) {
            try {
                return runSectionDownload(job, request, jobDir);
            } catch (ConversionException e) {
                log.warn("Descarga por sección falló para {} ({}). Fallback: descarga completa + recorte local.",
                        job.getId(), e.getMessage());
                resetDir(jobDir);
                job.setProgress(0);
                return fullDownloadThenTrim(job, request, jobDir);
            }
        }
        return runFullDownload(job, request, jobDir);
    }

    // ---------------------------------------------------------------------
    // Estrategias de descarga
    // ---------------------------------------------------------------------

    /** Camino rápido: yt-dlp descarga solo la sección pedida con ffmpeg. */
    private Path runSectionDownload(ConversionJob job, ConversionRequest request, Path jobDir) {
        List<String> cmd = buildYtDlpCommand(request, jobDir, true, false);
        log.info("Ejecutando (sección): {}", String.join(" ", cmd));
        return execAndLocate(cmd, job, jobDir);
    }

    /**
     * Descarga del medio completo (sin recorte). Si aria2c está habilitado, lo
     * intenta primero (descarga multi-conexión, más rápida); si falla —algunas
     * URLs de YouTube rechazan a los descargadores externos— reintenta con el
     * descargador nativo de yt-dlp, que siempre funciona.
     */
    private Path runFullDownload(ConversionJob job, ConversionRequest request, Path jobDir) {
        if (properties.isUseAria2()) {
            try {
                List<String> cmd = buildYtDlpCommand(request, jobDir, false, true);
                log.info("Ejecutando (completo, aria2): {}", String.join(" ", cmd));
                return execAndLocate(cmd, job, jobDir);
            } catch (ConversionException e) {
                log.warn("Descarga con aria2 falló ({}). Reintentando con el descargador nativo.",
                        e.getMessage());
                resetDir(jobDir);
                job.setProgress(0);
            }
        }
        List<String> cmd = buildYtDlpCommand(request, jobDir, false, false);
        log.info("Ejecutando (completo, nativo): {}", String.join(" ", cmd));
        return execAndLocate(cmd, job, jobDir);
    }

    private Path execAndLocate(List<String> cmd, ConversionJob job, Path jobDir) {
        runProcess(cmd, job, "yt-dlp");
        return locateOutputFile(jobDir);
    }

    /** Fallback robusto: descarga completa con yt-dlp y recorte local con ffmpeg. */
    private Path fullDownloadThenTrim(ConversionJob job, ConversionRequest request, Path jobDir) {
        Path full = runFullDownload(job, request, jobDir);
        return trimWithFfmpeg(job, full, request);
    }

    // ---------------------------------------------------------------------
    // Construcción de comandos
    // ---------------------------------------------------------------------

    /**
     * Construye los argumentos de yt-dlp. Cada argumento se pasa por separado a
     * ProcessBuilder (no se usa shell), lo que evita inyección de comandos.
     *
     * @param withSection si {@code true} y la petición tiene intervalo, añade el
     *                    recorte parcial con {@code --download-sections}.
     * @param useAria2    si {@code true}, usa aria2c como descargador externo.
     */
    private List<String> buildYtDlpCommand(ConversionRequest request, Path jobDir,
                                           boolean withSection, boolean useAria2) {
        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getYtDlpPath());
        cmd.add("--no-playlist");
        cmd.add("--newline");                 // una línea de progreso por actualización
        cmd.add("--restrict-filenames");      // nombres de archivo seguros
        cmd.add("--no-part");
        cmd.add("--retries");
        cmd.add("3");
        cmd.add("--fragment-retries");
        cmd.add("3");

        // Anti-bloqueo de YouTube: cookies de una sesión autenticada y/o proxy.
        String cookies = properties.getCookiesFile();
        if (cookies != null && !cookies.isBlank()) {
            cmd.add("--cookies");
            cmd.add(cookies);
        }
        String proxy = properties.getProxy();
        if (proxy != null && !proxy.isBlank()) {
            cmd.add("--proxy");
            cmd.add(proxy);
        }

        // Aceleración: descargar fragmentos en paralelo (DASH/HLS).
        cmd.add("--concurrent-fragments");
        cmd.add(String.valueOf(properties.getConcurrentFragments()));

        // aria2c (multi-conexión) acelera mucho los archivos de una sola pieza.
        // No aplica al recorte por sección, que yt-dlp resuelve con ffmpeg.
        if (useAria2) {
            cmd.add("--downloader");
            cmd.add(properties.getAria2Path());
            cmd.add("--downloader-args");
            cmd.add("aria2c:" + properties.getAria2Args());
        }

        // Solo indicamos --ffmpeg-location cuando se configura una RUTA real.
        // Si es el valor por defecto "ffmpeg", dejamos que yt-dlp lo encuentre en
        // el PATH (pasar el nombre como ruta hace que yt-dlp lo deshabilite).
        String ffmpeg = properties.getFfmpegPath();
        if (ffmpeg != null && !ffmpeg.isBlank() && !ffmpeg.equals("ffmpeg")) {
            cmd.add("--ffmpeg-location");
            cmd.add(ffmpeg);
        }

        cmd.add("-o");
        cmd.add(jobDir.resolve("%(title)s.%(ext)s").toString());

        if (withSection && request.hasInterval()) {
            cmd.add("--download-sections");
            cmd.add("*" + request.startTime() + "-" + request.endTime());
            cmd.add("--force-keyframes-at-cuts"); // cortes precisos
        }

        if (request.format() == Format.MP3) {
            cmd.add("-x");                     // extraer audio
            cmd.add("--audio-format");
            cmd.add("mp3");
            cmd.add("--audio-quality");
            cmd.add(request.quality() != null ? request.quality() + "K" : "192K");
        } else {
            String height = request.quality() != null ? request.quality() : "720";
            cmd.add("-f");
            cmd.add("bv*[height<=" + height + "]+ba/b[height<=" + height + "]");
            cmd.add("--merge-output-format");
            cmd.add("mp4");
        }

        cmd.add(request.url());
        return cmd;
    }

    /**
     * Recorta [startTime, endTime] de un archivo local usando ffmpeg y devuelve la
     * ruta del recorte. Borra el archivo completo original para no servirlo ni ocupar disco.
     */
    private Path trimWithFfmpeg(ConversionJob job, Path full, ConversionRequest request) {
        String name = full.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        Path output = full.resolveSibling(base + "_clip" + ext);

        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getFfmpegPath());
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(full.toString());
        cmd.add("-ss");
        cmd.add(request.startTime());
        cmd.add("-to");
        cmd.add(request.endTime());
        cmd.add("-c");
        cmd.add("copy");
        cmd.add("-avoid_negative_ts");
        cmd.add("make_zero");
        cmd.add(output.toString());

        log.info("Recortando con ffmpeg: {}", String.join(" ", cmd));
        runProcess(cmd, job, "ffmpeg");

        try {
            Files.deleteIfExists(full);
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo completo {}: {}", full, e.getMessage());
        }
        return output;
    }

    // ---------------------------------------------------------------------
    // Utilidades de proceso y archivos
    // ---------------------------------------------------------------------

    private void runProcess(List<String> command, ConversionJob job, String tool) {
        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ConversionException("No se pudo iniciar " + tool + ". ¿Está instalado?", e);
        }

        StringBuilder tail = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                updateProgress(job, line);
                appendTail(tail, line);
            }

            boolean finished = process.waitFor(properties.getProcessTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ConversionException("La conversión superó el tiempo máximo permitido");
            }
            if (process.exitValue() != 0) {
                throw new ConversionException(tool + " falló: " + tail);
            }
        } catch (IOException e) {
            throw new ConversionException("Error leyendo la salida de " + tool, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ConversionException("La conversión fue interrumpida", e);
        }
    }

    private void updateProgress(ConversionJob job, String line) {
        Matcher m = PROGRESS.matcher(line);
        if (m.find()) {
            job.setProgress(Integer.parseInt(m.group(1)));
            return;
        }
        Matcher a = ARIA2_PROGRESS.matcher(line);
        if (a.find()) {
            job.setProgress(Integer.parseInt(a.group(1)));
        }
    }

    /** Mantiene solo las últimas líneas para construir un mensaje de error útil. */
    private void appendTail(StringBuilder tail, String line) {
        tail.append(line).append('\n');
        if (tail.length() > 2000) {
            tail.delete(0, tail.length() - 2000);
        }
    }

    private Path createJobDir(ConversionJob job) {
        Path jobDir = Path.of(properties.getWorkDir(), job.getId());
        try {
            Files.createDirectories(jobDir);
        } catch (IOException e) {
            throw new ConversionException("No se pudo crear el directorio de trabajo", e);
        }
        return jobDir;
    }

    /** Vacía el directorio del trabajo antes de reintentar con otra estrategia. */
    private void resetDir(Path jobDir) {
        try (var stream = Files.list(jobDir)) {
            stream.forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("No se pudo borrar {}: {}", p, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("No se pudo limpiar el directorio {}: {}", jobDir, e.getMessage());
        }
    }

    /** Devuelve el archivo más reciente del directorio del trabajo (el resultado final). */
    private Path locateOutputFile(Path jobDir) {
        try (var stream = Files.list(jobDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .orElseThrow(() -> new ConversionException("No se generó ningún archivo de salida"));
        } catch (IOException e) {
            throw new ConversionException("No se pudo localizar el archivo generado", e);
        }
    }
}
