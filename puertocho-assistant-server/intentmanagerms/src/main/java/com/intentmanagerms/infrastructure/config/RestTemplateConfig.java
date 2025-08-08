package com.intentmanagerms.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración para RestTemplate
 * Proporciona el bean RestTemplate necesario para los servicios HTTP
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Bean de RestTemplate configurado con timeouts apropiados
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Configurar timeouts
        factory.setConnectTimeout(10000); // 10 segundos para conexión
        factory.setReadTimeout(30000);    // 30 segundos para lectura
        
        return new RestTemplate(factory);
    }
}
