package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.ConversationManager;
import com.intentmanagerms.domain.model.ConversationSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para el gestor de conversaciones.
 * Proporciona endpoints para gestionar conversaciones, procesar mensajes y obtener estadísticas.
 */
@RestController
@RequestMapping("/api/v1/conversation")
@CrossOrigin(origins = "*")
public class ConversationManagerController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationManagerController.class);

    @Autowired
    private ConversationManager conversationManager;

    /**
     * Procesa un mensaje del usuario en el contexto de una conversación.
     */
    @PostMapping("/process")
    public ResponseEntity<ConversationManager.ConversationResponse> processMessage(
            @RequestBody ConversationManager.ConversationRequest request) {
        
        logger.info("Procesando mensaje para sesión {}: {}", request.getSessionId(), request.getUserMessage());
        
        try {
            ConversationManager.ConversationResponse response = conversationManager.processMessage(request);
            
            if (response.isSuccess()) {
                logger.info("Mensaje procesado exitosamente para sesión {}: intent={}, confidence={}", 
                    request.getSessionId(), response.getDetectedIntent(), response.getConfidenceScore());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Error procesando mensaje para sesión {}: {}", 
                    request.getSessionId(), response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando mensaje para sesión {}: {}", 
                request.getSessionId(), e.getMessage(), e);
            
            ConversationManager.ConversationResponse errorResponse = new ConversationManager.ConversationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Error interno del servidor: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Obtiene una sesión de conversación existente.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ConversationSession> getSession(@PathVariable String sessionId) {
        logger.info("Obteniendo sesión: {}", sessionId);
        
        try {
            ConversationSession session = conversationManager.getSession(sessionId);
            
            if (session != null) {
                return ResponseEntity.ok(session);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error obteniendo sesión {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Crea una nueva sesión de conversación.
     */
    @PostMapping("/session")
    public ResponseEntity<ConversationSession> createSession(
            @RequestParam String userId,
            @RequestParam(required = false) String sessionId) {
        
        logger.info("Creando nueva sesión para usuario: {}", userId);
        
        try {
            String finalSessionId = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString();
            ConversationSession session = conversationManager.getOrCreateSession(finalSessionId, userId);
            
            logger.info("Sesión creada exitosamente: {}", session.getSessionId());
            return ResponseEntity.ok(session);
            
        } catch (Exception e) {
            logger.error("Error creando sesión para usuario {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Finaliza una sesión de conversación.
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> endSession(@PathVariable String sessionId) {
        logger.info("Finalizando sesión: {}", sessionId);
        
        try {
            boolean success = conversationManager.endSession(sessionId);
            
            if (success) {
                logger.info("Sesión finalizada exitosamente: {}", sessionId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Sesión finalizada exitosamente",
                    "session_id", sessionId
                ));
            } else {
                logger.warn("No se pudo finalizar la sesión: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error finalizando sesión {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Error finalizando sesión: " + e.getMessage()
            ));
        }
    }

    /**
     * Cancela una sesión de conversación.
     */
    @PostMapping("/session/{sessionId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSession(@PathVariable String sessionId) {
        logger.info("Cancelando sesión: {}", sessionId);
        
        try {
            boolean success = conversationManager.cancelSession(sessionId);
            
            if (success) {
                logger.info("Sesión cancelada exitosamente: {}", sessionId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Sesión cancelada exitosamente",
                    "session_id", sessionId
                ));
            } else {
                logger.warn("No se pudo cancelar la sesión: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error cancelando sesión {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Error cancelando sesión: " + e.getMessage()
            ));
        }
    }

    /**
     * Limpia sesiones expiradas.
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredSessions() {
        logger.info("Limpiando sesiones expiradas");
        
        try {
            conversationManager.cleanupExpiredSessions();
            
            logger.info("Limpieza de sesiones expiradas completada");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Limpieza de sesiones expiradas completada"
            ));
            
        } catch (Exception e) {
            logger.error("Error limpiando sesiones expiradas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Error limpiando sesiones expiradas: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene estadísticas del gestor de conversaciones.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.info("Obteniendo estadísticas del gestor de conversaciones");
        
        try {
            Map<String, Object> stats = conversationManager.getStatistics();
            
            logger.info("Estadísticas obtenidas exitosamente");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error obteniendo estadísticas: " + e.getMessage()
            ));
        }
    }

    /**
     * Verifica el estado de salud del servicio.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        logger.info("Verificando estado de salud del gestor de conversaciones");
        
        try {
            boolean isHealthy = conversationManager.isHealthy();
            
            Map<String, Object> healthInfo = Map.of(
                "service", "ConversationManager",
                "status", isHealthy ? "healthy" : "unhealthy",
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            if (isHealthy) {
                logger.info("Estado de salud: HEALTHY");
                return ResponseEntity.ok(healthInfo);
            } else {
                logger.warn("Estado de salud: UNHEALTHY");
                return ResponseEntity.status(503).body(healthInfo);
            }
            
        } catch (Exception e) {
            logger.error("Error verificando estado de salud: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "service", "ConversationManager",
                "status", "error",
                "error", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }

    /**
     * Endpoint de prueba para verificar la funcionalidad básica.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConversation() {
        logger.info("Ejecutando prueba del gestor de conversaciones");
        
        try {
            // Crear una sesión de prueba
            String testUserId = "test-user-" + System.currentTimeMillis();
            String testSessionId = "test-session-" + System.currentTimeMillis();
            
            ConversationSession session = conversationManager.getOrCreateSession(testSessionId, testUserId);
            
            // Procesar un mensaje de prueba
            ConversationManager.ConversationRequest request = new ConversationManager.ConversationRequest();
            request.setSessionId(testSessionId);
            request.setUserId(testUserId);
            request.setUserMessage("¿Qué tiempo hace en Madrid?");
            
            ConversationManager.ConversationResponse response = conversationManager.processMessage(request);
            
            // Finalizar la sesión de prueba
            conversationManager.endSession(testSessionId);
            
            Map<String, Object> testResult = Map.of(
                "success", true,
                "test_session_id", testSessionId,
                "test_user_id", testUserId,
                "message_processed", response.isSuccess(),
                "detected_intent", response.getDetectedIntent(),
                "confidence_score", response.getConfidenceScore(),
                "system_response", response.getSystemResponse(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            logger.info("Prueba completada exitosamente");
            return ResponseEntity.ok(testResult);
            
        } catch (Exception e) {
            logger.error("Error en prueba del gestor de conversaciones: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Error en prueba: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }
} 