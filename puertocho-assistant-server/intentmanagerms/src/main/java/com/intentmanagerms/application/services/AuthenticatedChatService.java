package com.intentmanagerms.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Servicio para realizar peticiones autenticadas a través del gateway.
 * Demuestra el uso del AuthService para obtener tokens JWT automáticamente.
 */
@Service
public class AuthenticatedChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedChatService.class);
    
    private final AuthService authService;
    private final WebClient webClient;
    
    public AuthenticatedChatService(AuthService authService, WebClient.Builder webClientBuilder) {
        this.authService = authService;
        this.webClient = webClientBuilder
                .baseUrl("http://puertocho-assistant-gateway:10002")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
    
    /**
     * Ejemplo de cómo hacer una petición autenticada al endpoint /api/assistant/chat
     * usando el token JWT obtenido automáticamente.
     */
    public String sendAuthenticatedChatMessage(String message, String sessionId) {
        if (!authService.isAutoLoginEnabled()) {
            throw new IllegalStateException("El autologin está deshabilitado. No se pueden hacer peticiones autenticadas.");
        }
        
        try {
            // Obtener token JWT válido (se autologa automáticamente si es necesario)
            String token = authService.getValidToken();
            
            // Crear el payload para el chat
            String chatPayload = String.format(
                "{\"text\": \"%s\", \"sessionId\": \"%s\", \"enableTts\": false}",
                message.replace("\"", "\\\""),
                sessionId != null ? sessionId : "auto-session"
            );
            
            logger.info("Enviando mensaje autenticado: '{}'", message);
            
            // Hacer la petición autenticada
            String response = webClient.post()
                    .uri("/api/assistant/chat")
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .bodyValue(chatPayload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            logger.info("Respuesta del chat autenticado recibida exitosamente");
            return response;
            
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                // Token expirado o inválido, invalidar y reintentar una vez
                logger.warn("Token JWT rechazado, invalidando e intentando de nuevo...");
                authService.invalidateToken();
                return retryAuthenticatedRequest(message, sessionId);
            } else {
                String errorMsg = String.format("Error HTTP %d en petición autenticada: %s", 
                                              e.getStatusCode().value(), e.getResponseBodyAsString());
                logger.error(errorMsg);
                throw new AuthenticatedChatException(errorMsg, e);
            }
        } catch (AuthService.AuthServiceException e) {
            String errorMsg = "Error de autenticación: " + e.getMessage();
            logger.error(errorMsg);
            throw new AuthenticatedChatException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Error inesperado en petición autenticada: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new AuthenticatedChatException(errorMsg, e);
        }
    }
    
    /**
     * Reintenta la petición una vez después de invalidar el token.
     */
    private String retryAuthenticatedRequest(String message, String sessionId) {
        try {
            String token = authService.getValidToken();
            
            String chatPayload = String.format(
                "{\"text\": \"%s\", \"sessionId\": \"%s\", \"enableTts\": false}",
                message.replace("\"", "\\\""),
                sessionId != null ? sessionId : "auto-session"
            );
            
            return webClient.post()
                    .uri("/api/assistant/chat")
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .bodyValue(chatPayload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
                    
        } catch (Exception e) {
            String errorMsg = "Error en reintento de petición autenticada: " + e.getMessage();
            logger.error(errorMsg);
            throw new AuthenticatedChatException(errorMsg, e);
        }
    }
    
    /**
     * Verifica si las peticiones autenticadas están disponibles.
     */
    public boolean isAuthenticatedRequestsEnabled() {
        return authService.isAutoLoginEnabled();
    }
    
    /**
     * Excepción personalizada para errores en peticiones autenticadas.
     */
    public static class AuthenticatedChatException extends RuntimeException {
        public AuthenticatedChatException(String message) {
            super(message);
        }
        
        public AuthenticatedChatException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
