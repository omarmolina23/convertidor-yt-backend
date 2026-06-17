package com.convertidor.yt.controller;

import com.convertidor.yt.dto.ConversionRequest;
import com.convertidor.yt.dto.ConversionResponse;
import com.convertidor.yt.dto.JobStatusResponse;
import com.convertidor.yt.exception.ConversionException;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.JobStatus;
import com.convertidor.yt.service.ConversionService;
import com.convertidor.yt.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints REST del convertidor.
 */
@RestController
@RequestMapping("/api/v1/conversions")
@Tag(name = "Conversiones", description = "Convierte enlaces de YouTube a MP3/MP4")
public class ConversionController {

    private final ConversionService conversionService;
    private final StorageService storageService;

    public ConversionController(ConversionService conversionService, StorageService storageService) {
        this.conversionService = conversionService;
        this.storageService = storageService;
    }

    @Operation(summary = "Crea un trabajo de conversión (asíncrono)")
    @PostMapping
    public ResponseEntity<ConversionResponse> create(@Valid @RequestBody ConversionRequest request) {
        ConversionJob job = conversionService.submit(request);
        ConversionResponse body = new ConversionResponse(
                job.getId(),
                job.getStatus().name(),
                "/api/v1/conversions/" + job.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @Operation(summary = "Consulta el estado y progreso de un trabajo")
    @GetMapping("/{id}")
    public JobStatusResponse status(@PathVariable String id) {
        return JobStatusResponse.from(conversionService.getJob(id));
    }

    @Operation(summary = "Descarga el archivo convertido cuando está listo")
    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable String id) {
        ConversionJob job = conversionService.getJob(id);
        if (job.getStatus() != JobStatus.READY || job.getStorageKey() == null) {
            throw new ConversionException(
                    "El archivo aún no está listo (estado: " + job.getStatus() + ")");
        }
        // Modo local: stream del archivo. Modo r2: redirect a una URL prefirmada.
        return storageService.download(job);
    }
}
