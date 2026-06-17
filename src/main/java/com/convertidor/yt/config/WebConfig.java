package com.convertidor.yt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración CORS para permitir que el frontend Flutter (web) consuma la API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${converter.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // allowedOriginPatterns (en vez de allowedOrigins) acepta comodines, p.ej.
        // "https://*.vercel.app" para cubrir las preview deployments de Vercel.
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
