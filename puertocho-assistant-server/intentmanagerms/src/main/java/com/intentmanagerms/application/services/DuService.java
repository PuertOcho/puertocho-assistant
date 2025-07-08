package com.intentmanagerms.application.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.application.services.dto.EntityInfo;
import com.intentmanagerms.application.services.dto.IntentInfo;
import com.intentmanagerms.application.services.dto.IntentMessage;
import com.intentmanagerms.application.services.dto.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Servicio para interactuar con el servicio DU (Digital Understanding) Rasa Pro/CALM.
 * Maneja el formato específico de respuesta del endpoint /webhooks/rest/webhook.
 */
@Service
public class DuService {
    
    private static final Logger logger = LoggerFactory.getLogger(DuService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public DuService(WebClient.Builder webClientBuilder, @Value("${du.url:http://du-puertocho-ms:5005/webhooks/rest/webhook}") String duUrl) {
        this.webClient = webClientBuilder
                .baseUrl(duUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
        this.objectMapper = new ObjectMapper();
        
        logger.info("DuService inicializado con URL: {}", duUrl);
    }
    
    /**
     * Analiza el texto del usuario utilizando el endpoint /webhooks/rest/webhook de Rasa.
     * 
     * @param userText El texto del usuario a analizar
     * @param senderId ID del remitente (para seguimiento de conversación)
     * @return IntentMessage con la intención, entidades y otra información
     */
    public IntentMessage analyzeText(String userText, String senderId) {
        if (userText == null || userText.trim().isEmpty()) {
            throw new DuServiceException("El texto del usuario no puede estar vacío");
        }
        
        if (senderId == null || senderId.trim().isEmpty()) {
            senderId = "default";
        }
        
        try {
            logger.debug("Enviando texto: '{}' al webhook de Rasa con senderId: {}", userText, senderId);
            
            // Crear el cuerpo de la petición según el formato de Rasa webhook
            String jsonBody = String.format("{\"sender\": \"%s\", \"message\": \"%s\"}", 
                                          senderId.replace("\"", "\\\""), 
                                          userText.replace("\"", "\\\""));
            
            // Llamar al webhook y obtener la respuesta
            List<WebhookResponse> responses = webClient.post()
                    .bodyValue(jsonBody)
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToFlux(WebhookResponse.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            if (responses == null || responses.isEmpty()) {
                throw new DuServiceException("No se recibió respuesta del servicio DU/Rasa");
            }
            
            // Tomar la primera respuesta y procesarla
            String responseText = responses.get(0).getText();
            
            return parseRasaResponse(responseText, userText);
            
        } catch (WebClientResponseException e) {
            String errorMsg = String.format("Error HTTP %d al consultar DU/Rasa: %s", 
                                          e.getStatusCode().value(), e.getResponseBodyAsString());
            logger.error(errorMsg, e);
            throw new DuServiceException(errorMsg, e);
            
        } catch (Exception e) {
            String errorMsg = "Error inesperado al consultar el servicio DU/Rasa: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new DuServiceException(errorMsg, e);
        }
    }
    
    /**
     * Parsea el texto de respuesta de Rasa a un objeto IntentMessage.
     */
    private IntentMessage parseRasaResponse(String responseText, String originalUserText) throws JsonProcessingException {
        logger.debug("Parseando respuesta de Rasa: {}", responseText);
        
        // La respuesta es un JSON con formato específico de Rasa Pro
        JsonNode rootNode = objectMapper.readTree(responseText);
        
        IntentMessage result = new IntentMessage();
        result.setText(originalUserText);
        
        // Extraer intención
        String intentName = rootNode.has("intent") ? rootNode.get("intent").asText() : null;
        IntentInfo intentInfo = new IntentInfo();
        intentInfo.setName(intentName);
        intentInfo.setConfidence("0.9"); // No viene en la respuesta, asignamos un valor por defecto
        result.setIntent(intentInfo);
        
        // Extraer status
        if (rootNode.has("status")) {
            result.setStatus(rootNode.get("status").asText());
        }
        
        // Extraer entidades
        List<EntityInfo> entities = new ArrayList<>();
        if (rootNode.has("entities") && rootNode.get("entities").isObject()) {
            JsonNode entitiesNode = rootNode.get("entities");
            Iterator<Map.Entry<String, JsonNode>> fields = entitiesNode.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String entityName = field.getKey();
                String entityValue = field.getValue().asText();                    // Omitir entidades con valor null
                if (!entityValue.equals("null")) {
                    EntityInfo entityInfo = new EntityInfo();
                    entityInfo.setEntity(entityName);
                    entityInfo.setValue(entityValue);
                    entityInfo.setConfidenceEntity("0.9"); // Valor por defecto
                    entities.add(entityInfo);
                }
            }
        }
        result.setEntities(entities);
        
        // Extraer missing_entities
        if (rootNode.has("missing_entities") && rootNode.get("missing_entities").isArray()) {
            List<String> missingEntities = new ArrayList<>();
            for (JsonNode item : rootNode.get("missing_entities")) {
                missingEntities.add(item.asText());
            }
            result.setMissingEntities(missingEntities);
        }
        
        return result;
    }
    
    /**
     * Excepción personalizada para errores del servicio DU/Rasa.
     */
    public static class DuServiceException extends RuntimeException {
        public DuServiceException(String message) {
            super(message);
        }
        
        public DuServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
