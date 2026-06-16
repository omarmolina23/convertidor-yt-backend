package com.convertidor.yt.controller;

import com.convertidor.yt.dto.ConversionRequest;
import com.convertidor.yt.dto.ConversionResponse;
import com.convertidor.yt.dto.JobStatusResponse;
import com.convertidor.yt.exception.ConversionException;
import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.JobStatus;
import com.convertidor.yt.service.ConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

/**
 * Endpoints REST del convertidor.
 */
@RestController
@RequestMapping("/api/v1/conversions")
@Tag(name = "Conversiones", description = "Convierte enlaces de YouTube a MP3/MP4")
public class ConversionController {

    private final ConversionService conversionService;

    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
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
    public ResponseEntity<Resource> download(@PathVariable String id) {
        ConversionJob job = conversionService.getJob(id);
        if (job.getStatus() != JobStatus.READY || job.getOutputFile() == null) {
            throw new ConversionException("El archivo aún no está listo (estado: " + job.getStatus() + ")");
        }

        Path file = job.getOutputFile();
        Resource resource = new FileSystemResource(file);
        MediaType mediaType = switch (job.getFormat()) {
            case MP3 -> MediaType.parseMediaType("audio/mpeg");
            case MP4 -> MediaType.parseMediaType("video/mp4");
        };

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + job.getFileName() + "\"")
                .body(resource);
    }
}
