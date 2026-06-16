package com.convertidor.yt.controller;

import com.convertidor.yt.model.ConversionJob;
import com.convertidor.yt.model.Format;
import com.convertidor.yt.model.JobStatus;
import com.convertidor.yt.service.ConversionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionController.class)
class ConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversionService conversionService;

    @Test
    void creaTrabajoConPeticionValida() throws Exception {
        ConversionJob job = new ConversionJob("abc-123", Format.MP3);
        when(conversionService.submit(any())).thenReturn(job);

        String body = """
                {"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ","format":"MP3"}
                """;

        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("abc-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rechazaUrlInvalida() throws Exception {
        String body = """
                {"url":"https://example.com/video","format":"MP3"}
                """;

        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devuelveEstadoDelTrabajo() throws Exception {
        ConversionJob job = new ConversionJob("abc-123", Format.MP4);
        job.setStatus(JobStatus.PROCESSING);
        job.setProgress(42);
        when(conversionService.getJob("abc-123")).thenReturn(job);

        mockMvc.perform(get("/api/v1/conversions/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.progress").value(42));
    }
}
