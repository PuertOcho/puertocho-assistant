package com.intentmanagerms.application.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.application.services.dto.NluMessage;
import com.intentmanagerms.application.services.dto.NluResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
public class NluService {
    
    private static final Logger logger = LoggerFactory.getLogger(NluService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String nluUrl;
    private final String defaultDomain;
    private final String defaultLocale;
    
    public NluService(WebClient.Builder webClientBuilder,
                     @Value("${nlu.url:http://localhost:5001}") String nluUrl,
                     @Value("${nlu.domain:hogar}") String defaultDomain,
                     @Value("${nlu.locale:es}") String defaultLocale) {
        this.webClient = webClientBuilder
                .baseUrl(nluUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
        this.objectMapper = new ObjectMapper();
        this.nluUrl = nluUrl;
        this.defaultDomain = defaultDomain;
        this.defaultLocale = defaultLocale;
        
        logger.info("NluService inicializado con URL: {}, dominio: {}, idioma: {}", 
                   nluUrl, defaultDomain, defaultLocale);
    }
    
    /**
     * Analiza el texto del usuario y obtiene la intención y entidades detectadas.
     * 
     * @param userText El texto del usuario a analizar
     * @return NluMessage con la intención, entidades y información adicional
     * @throws NluServiceException Si ocurre un error durante el análisis
     */
    public NluMessage analyzeText(String userText) {
        return analyzeText(userText, defaultDomain, defaultLocale);
    }
    
    /**
     * Analiza el texto del usuario con dominio y idioma específicos.
     * 
     * @param userText El texto del usuario a analizar
     * @param domain El dominio específico (ej: hogar, música, etc.)
     * @param locale El idioma específico (ej: es, en, etc.)
     * @return NluMessage con la intención, entidades y información adicional
     * @throws NluServiceException Si ocurre un error durante el análisis
     */
    public NluMessage analyzeText(String userText, String domain, String locale) {
        if (userText == null || userText.trim().isEmpty()) {
            throw new NluServiceException("El texto del usuario no puede estar vacío");
        }
        
        try {
            logger.debug("Analizando texto: '{}' con dominio: {} y locale: {}", userText, domain, locale);
            
            NluResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/predict")
                            .queryParam("domain", domain)
                            .queryParam("locale", locale)
                            .queryParam("userUtterance", userText)
                            .build())
                    .retrieve()
                    .bodyToMono(NluResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            if (response == null) {
                throw new NluServiceException("Respuesta nula del servicio NLU");
            }
            
            // Parsear el mensaje JSON interno
            NluMessage nluMessage = parseNluMessage(response.getMessage());
            
            logger.debug("Intención detectada: {} con confianza: {}", 
                        nluMessage.getIntent().getName(), 
                        nluMessage.getIntent().getConfidence());
            
            return nluMessage;
            
        } catch (WebClientResponseException e) {
            String errorMsg = String.format("Error HTTP %d al consultar NLU: %s", 
                                          e.getStatusCode().value(), e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            throw new NluServiceException(errorMsg, e);
            
        } catch (Exception e) {
            String errorMsg = "Error inesperado al consultar el servicio NLU: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new NluServiceException(errorMsg, e);
        }
    }
    
    /**
     * Verifica si el servicio NLU está disponible.
     * 
     * @return true si el servicio responde correctamente, false en caso contrario
     */
    public boolean isServiceHealthy() {
        try {
            String response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            return response != null && response.contains("OK");
            
        } catch (Exception e) {
            logger.warn("Servicio NLU no disponible: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Entrena el modelo NLU para un dominio específico.
     * 
     * @param domain El dominio a entrenar
     * @param locale El idioma del entrenamiento
     * @return true si el entrenamiento fue exitoso, false en caso contrario
     */
    public boolean trainModel(String domain, String locale) {
        try {
            logger.info("Iniciando entrenamiento del modelo NLU para dominio: {} y locale: {}", domain, locale);
            
            NluResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/train")
                            .queryParam("domain", domain)
                            .queryParam("locale", locale)
                            .build())
                    .retrieve()
                    .bodyToMono(NluResponse.class)
                    .timeout(Duration.ofMinutes(10)) // El entrenamiento puede tomar tiempo
                    .block();
            
            boolean success = response != null && "TRAIN_SUCCESS".equals(response.getMessageId());
            
            if (success) {
                logger.info("Entrenamiento completado exitosamente para dominio: {}", domain);
            } else {
                logger.error("Entrenamiento falló para dominio: {}", domain);
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error durante el entrenamiento del modelo NLU: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private NluMessage parseNluMessage(String messageJson) throws NluServiceException {
        try {
            return objectMapper.readValue(messageJson, NluMessage.class);
        } catch (JsonProcessingException e) {
            throw new NluServiceException("Error parseando respuesta JSON del servicio NLU: " + e.getMessage(), e);
        }
    }
    
    /**
     * Excepción personalizada para errores del servicio NLU.
     */
    public static class NluServiceException extends RuntimeException {
        public NluServiceException(String message) {
            super(message);
        }
        
        public NluServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 