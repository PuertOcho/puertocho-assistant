package com.intentmanagerms.application.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.application.services.dto.IntentMessage;
import com.intentmanagerms.application.services.dto.IntentResponse;
import com.intentmanagerms.application.services.dto.IntentInfo;
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
    private final LlmIntentClassifierService llmClassifier;
    
    public NluService(WebClient.Builder webClientBuilder,
                     @Value("${nlu.url:http://localhost:5001}") String nluUrl,
                     @Value("${nlu.domain:intents}") String defaultDomain,
                     @Value("${nlu.locale:es}") String defaultLocale,
                     LlmIntentClassifierService llmClassifier) {
        this.webClient = webClientBuilder
                .baseUrl(nluUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
        this.objectMapper = new ObjectMapper();
        this.nluUrl = nluUrl;
        this.defaultDomain = defaultDomain;
        this.defaultLocale = defaultLocale;
        this.llmClassifier = llmClassifier;
        
        logger.info("NluService inicializado con URL: {}, dominio: {}, idioma: {}", 
                   nluUrl, defaultDomain, defaultLocale);
    }
    
    /**
     * Analiza el texto del usuario y obtiene la intención y entidades detectadas.
     * 
     * @param userText El texto del usuario a analizar
     * @return IntentMessage con la intención, entidades y información adicional
     * @throws NluServiceException Si ocurre un error durante el análisis
     */
    public IntentMessage analyzeText(String userText) {
        return analyzeText(userText, defaultDomain, defaultLocale);
    }
    
    /**
     * Analiza el texto del usuario con dominio y idioma específicos.
     * 
     * @param userText El texto del usuario a analizar
     * @param domain El dominio específico (ej: intents, música, etc.)
     * @param locale El idioma específico (ej: es, en, etc.)
     * @return IntentMessage con la intención, entidades y información adicional
     * @throws NluServiceException Si ocurre un error durante el análisis
     */
    public IntentMessage analyzeText(String userText, String domain, String locale) {
        if (userText == null || userText.trim().isEmpty()) {
            throw new NluServiceException("El texto del usuario no puede estar vacío");
        }
        
        try {
            logger.debug("Analizando texto: '{}' con dominio: {} y locale: {}", userText, domain, locale);
            
            IntentResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/predict")
                            .queryParam("domain", domain)
                            .queryParam("locale", locale)
                            .queryParam("userUtterance", userText)
                            .build())
                    .retrieve()
                    .bodyToMono(IntentResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            if (response == null) {
                throw new NluServiceException("Respuesta nula del servicio NLU");
            }
            
            // Parsear el mensaje JSON interno
            IntentMessage intentMessage = parseIntentMessage(response.getMessage());
            
            double conf = intentMessage.getIntent().getConfidenceAsDouble();
            logger.debug("Intención detectada: {} con confianza: {}",
                        intentMessage.getIntent().getName(), conf);
            
            // Fallback LLM si confianza baja
            if (conf < 0.3) {
                String llmIntent = llmClassifier.predictIntent(userText);
                if (llmIntent != null) {
                    logger.info("LLM clasificó la intención como '{}' (fallback)", llmIntent);
                    intentMessage.getIntent().setName(llmIntent);
                    intentMessage.getIntent().setConfidence("0.80");
                }
            }
            
            return intentMessage;
            
        } catch (WebClientResponseException e) {
            String errorMsg = String.format("Error HTTP %d al consultar NLU: %s", 
                                          e.getStatusCode().value(), e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            // Fallback al LLM si es posible
            String llmIntent = llmClassifier.predictIntent(userText);
            if (llmIntent != null) {
                logger.warn("NLU devolvió {} – usando LLM para clasificar como '{}'", e.getStatusCode(), llmIntent);
                IntentMessage nm = new IntentMessage();
                IntentInfo ni = new IntentInfo();
                ni.setName(llmIntent);
                ni.setConfidence("0.80");
                nm.setIntent(ni);
                nm.setEntities(java.util.List.of());
                nm.setIntentRanking(java.util.List.of());
                nm.setText(userText);
                return nm;
            }
            throw new NluServiceException(errorMsg, e);
            
        } catch (Exception e) {
            String errorMsg = "Error inesperado al consultar el servicio NLU: " + e.getMessage();
            logger.error(errorMsg, e);
            // Fallback total al LLM
            String llmIntent = llmClassifier.predictIntent(userText);
            if (llmIntent != null) {
                logger.warn("NLU falló; usando LLM para clasificar como '{}'", llmIntent);
                IntentMessage nm = new IntentMessage();
                IntentInfo ni = new IntentInfo();
                ni.setName(llmIntent);
                ni.setConfidence("0.80");
                nm.setIntent(ni);
                nm.setEntities(java.util.List.of());
                nm.setIntentRanking(java.util.List.of());
                nm.setText(userText);
                return nm;
            }
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
            
            IntentResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/train")
                            .queryParam("domain", domain)
                            .queryParam("locale", locale)
                            .build())
                    .retrieve()
                    .bodyToMono(IntentResponse.class)
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
    
    private IntentMessage parseIntentMessage(String messageJson) throws NluServiceException {
        try {
            return objectMapper.readValue(messageJson, IntentMessage.class);
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