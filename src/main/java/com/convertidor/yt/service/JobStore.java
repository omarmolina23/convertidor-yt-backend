package com.convertidor.yt.service;

import com.convertidor.yt.config.ConverterProperties;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.Format;
import com.convertidor.yt.model.JobStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Almacén de metadatos de los trabajos, respaldado por Redis y compartido entre
 * réplicas. Cada trabajo es un hash {@code job:{id}} con TTL = retención, así que
 * expira solo (la limpieza de los archivos la hace {@link StorageService}).
 */
@Component
public class JobStore {

    private static final String KEY_PREFIX = "job:";

    private final StringRedisTemplate redis;
    private final ConverterProperties properties;

    public JobStore(StringRedisTemplate redis, ConverterProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** Guarda (o actualiza) el trabajo y refresca su TTL. */
    public void save(ConversionJob job) {
        String key = KEY_PREFIX + job.getId();
        Map<String, String> hash = new HashMap<>();
        hash.put("format", job.getFormat().name());
        hash.put("status", job.getStatus().name());
        hash.put("progress", Integer.toString(job.getProgress()));
        putIfPresent(hash, "fileName", job.getFileName());
        putIfPresent(hash, "errorMessage", job.getErrorMessage());
        putIfPresent(hash, "storageKey", job.getStorageKey());
        redis.opsForHash().putAll(key, hash);
        redis.expire(key, Duration.ofMinutes(properties.getRetentionMinutes()));
    }

    public Optional<ConversionJob> find(String id) {
        Map<Object, Object> hash = redis.opsForHash().entries(KEY_PREFIX + id);
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        ConversionJob job = new ConversionJob(id, Format.valueOf((String) hash.get("format")));
        job.setStatus(JobStatus.valueOf((String) hash.get("status")));
        job.setProgress(Integer.parseInt((String) hash.get("progress")));
        setIfPresent(hash, "fileName", job::setFileName);
        setIfPresent(hash, "errorMessage", job::setErrorMessage);
        setIfPresent(hash, "storageKey", job::setStorageKey);
        return Optional.of(job);
    }

    public void remove(String id) {
        redis.delete(KEY_PREFIX + id);
    }

    private void putIfPresent(Map<String, String> hash, String field, String value) {
        if (value != null) {
            hash.put(field, value);
        }
    }

    private void setIfPresent(Map<Object, Object> hash, String field, Consumer<String> setter) {
        Object value = hash.get(field);
        if (value != null) {
            setter.accept((String) value);
        }
    }
}
