package com.convertidor.yt.service;

import com.convertidor.yt.config.ConverterProperties;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración del almacenamiento R2/S3 contra un MinIO local.
 * Solo corre si la variable de entorno {@code S3_IT=true} (no se ejecuta en CI).
 *
 * <p>Preparar antes:
 * <pre>
 *   docker network create conv-net
 *   docker run -d --name minio --network conv-net -p 9000:9000 \
 *       -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin \
 *       minio/minio server /data
 *   docker run --rm --network conv-net minio/mc \
 *       sh -c "mc alias set m http://minio:9000 minioadmin minioadmin && mc mb m/conversions"
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "S3_IT", matches = "true")
class R2StorageServiceIT {

    private ConverterProperties props() {
        ConverterProperties p = new ConverterProperties();
        p.setStorageType("r2");
        p.setS3Endpoint(env("R2_ENDPOINT", "http://localhost:9000"));
        p.setS3Bucket(env("R2_BUCKET", "conversions"));
        p.setS3AccessKey(env("R2_ACCESS_KEY", "minioadmin"));
        p.setS3SecretKey(env("R2_SECRET_KEY", "minioadmin"));
        p.setS3Region(env("R2_REGION", "us-east-1"));
        p.setS3PresignMinutes(10);
        return p;
    }

    private String env(String name, String fallback) {
        String v = System.getenv(name);
        return v != null ? v : fallback;
    }

    @Test
    void subeYDescargaPorUrlPrefirmada() throws IOException, InterruptedException {
        R2StorageService storage = new R2StorageService(props());
        try {
            ConversionJob job = new ConversionJob("it-" + System.currentTimeMillis(), Format.MP3);
            job.setFileName("cancion.mp3");

            Path tmp = Files.createTempFile("it", ".mp3");
            Files.writeString(tmp, "contenido-de-prueba");

            String key = storage.store(job, tmp);
            job.setStorageKey(key);
            assertThat(key).contains(job.getId());
            assertThat(Files.exists(tmp)).isFalse(); // el local se borra tras subir

            ResponseEntity<?> response = storage.download(job);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
            URI presigned = response.getHeaders().getLocation();
            assertThat(presigned).isNotNull();

            HttpResponse<String> fetched = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(presigned).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(fetched.statusCode()).isEqualTo(200);
            assertThat(fetched.body()).isEqualTo("contenido-de-prueba");
        } finally {
            storage.close();
        }
    }
}
