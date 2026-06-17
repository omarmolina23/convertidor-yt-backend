package com.convertidor.yt.service;

import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.Format;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;

/**
 * Abstracción del almacenamiento del archivo convertido. Dos estrategias,
 * elegidas por {@code converter.storage-type}:
 * <ul>
 *   <li><b>local</b>: disco del work-dir (una instancia).</li>
 *   <li><b>r2</b>: object storage compatible con S3 (Cloudflare R2), apto para
 *       varias réplicas.</li>
 * </ul>
 */
public interface StorageService {

    /**
     * Persiste el archivo recién convertido y devuelve la referencia a guardar en
     * el trabajo: la ruta local (modo local) o la key del objeto (modo r2).
     */
    String store(ConversionJob job, Path localFile);

    /**
     * Construye la respuesta de descarga: stream del archivo (local) o un
     * redirect 302 a una URL prefirmada de corta duración (r2).
     */
    ResponseEntity<?> download(ConversionJob job);

    /** Elimina los archivos/objetos cuyo tiempo de retención ya expiró. */
    void cleanupExpired();

    /** Content-Type según el formato de salida. */
    static String contentType(Format format) {
        return switch (format) {
            case MP3 -> "audio/mpeg";
            case MP4 -> "video/mp4";
        };
    }
}
