package com.convertidor.yt.service;

import com.convertidor.yt.config.ConverterProperties;
import com.convertidor.yt.exception.ConversionException;
import com.convertidor.yt.model.ConversionJob;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Almacenamiento en object storage compatible con S3 (Cloudflare R2). Sube el
 * archivo convertido y sirve la descarga con un redirect 302 a una URL
 * prefirmada, de modo que cualquier réplica lo alcance y el backend no gaste
 * ancho de banda. El bucket permanece privado.
 */
@Service
@ConditionalOnProperty(name = "converter.storage-type", havingValue = "r2")
public class R2StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);

    private final ConverterProperties properties;
    private final String bucket;
    private final S3Client s3;
    private final S3Presigner presigner;

    public R2StorageService(ConverterProperties properties) {
        this.properties = properties;
        this.bucket = properties.getS3Bucket();

        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getS3AccessKey(), properties.getS3SecretKey()));
        var endpoint = URI.create(properties.getS3Endpoint());
        var region = Region.of(properties.getS3Region());
        // Path-style: necesario para endpoints custom (R2/MinIO).
        var config = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        this.s3 = S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentials)
                .region(region)
                .serviceConfiguration(config)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentials)
                .region(region)
                .serviceConfiguration(config)
                .build();
    }

    @Override
    public String store(ConversionJob job, Path localFile) {
        String key = job.getId() + "/" + localFile.getFileName();
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(StorageService.contentType(job.getFormat()))
                            .build(),
                    localFile);
        } catch (Exception e) {
            throw new ConversionException("No se pudo subir el archivo a R2: " + e.getMessage(), e);
        }
        deleteLocalQuietly(localFile);
        return key;
    }

    @Override
    public ResponseEntity<?> download(ConversionJob job) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(job.getStorageKey())
                .responseContentType(StorageService.contentType(job.getFormat()))
                .responseContentDisposition("attachment; filename=\"" + job.getFileName() + "\"")
                .build();
        var presigned = presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(properties.getS3PresignMinutes()))
                        .getObjectRequest(get)
                        .build());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presigned.url().toString()))
                .build();
    }

    /** Cada minuto elimina los objetos del bucket más antiguos que la retención. */
    @Override
    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpired() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(properties.getRetentionMinutes()));
        try {
            var listed = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build());
            for (S3Object object : listed.contents()) {
                if (object.lastModified().isBefore(threshold)) {
                    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(object.key()).build());
                    log.info("Eliminado objeto R2 expirado {}", object.key());
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo limpiar el bucket {}: {}", bucket, e.getMessage());
        }
    }

    private void deleteLocalQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
            Path parent = file.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                try (var stream = Files.list(parent)) {
                    if (stream.findAny().isEmpty()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo local {}: {}", file, e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        s3.close();
        presigner.close();
    }
}
