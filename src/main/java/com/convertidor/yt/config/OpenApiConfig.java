package com.convertidor.yt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación OpenAPI (Swagger UI en /swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI convertidorOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Convertidor YouTube → MP3/MP4 API")
                .description("API REST que convierte enlaces de YouTube a MP3 o MP4, "
                        + "con soporte para descarga completa o por intervalo de tiempo.")
                .version("1.0.0")
                .license(new License().name("MIT")));
    }
}
