package com.convertidor.yt.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración general: habilita las propiedades y define el pool de hilos
 * usado para ejecutar las conversiones de forma asíncrona.
 */
@Configuration
@EnableConfigurationProperties(ConverterProperties.class)
public class AppConfig {

    @Bean(name = "conversionExecutor")
    public TaskExecutor conversionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("conv-");
        executor.initialize();
        return executor;
    }
}
