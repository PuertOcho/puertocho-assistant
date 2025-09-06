package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.ConversationManager;
import com.intentmanagerms.application.services.WhisperTranscriptionService;
import com.intentmanagerms.application.services.TtsGenerationService;
import com.intentmanagerms.domain.model.ConversationSession;
import com.intentmanagerms.domain.model.WhisperTranscriptionRequest;
import com.intentmanagerms.domain.model.WhisperTranscriptionResponse;
import com.intentmanagerms.domain.model.TtsGenerationRequest;
import com.intentmanagerms.domain.model.TtsGenerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * Controlador REST para el gestor de conversaciones.
 * Proporciona endpoints para gestionar conversaciones, procesar mensajes (texto y audio) y obtener estadísticas.
 */
@RestController
@RequestMapping("/api/v1/conversation")
@CrossOrigin(origins = "*")
public class ConversationManagerController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationManagerController.class);

    @Autowired
    private ConversationManager conversationManager;

    @Autowired
    private WhisperTranscriptionService whisperTranscriptionService;

    @Autowired
    private TtsGenerationService ttsGenerationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Procesa un mensaje de texto del usuario en el contexto de una conversación.
     */
    @PostMapping("/process")
    public ResponseEntity<ConversationManager.ConversationResponse> processMessage(
            @RequestBody ConversationManager.ConversationRequest request) {
        
        logger.info("Procesando mensaje de texto para sesión {}: {}", request.getSessionId(), request.getUserMessage());
                
        // 🔍 DEBUG: Mostrar metadata recibida en endpoint de texto
        logger.info("🔍 DEBUG TEXTO - sessionId: {}", request.getSessionId());
        logger.info("🔍 DEBUG TEXTO - userId: {}", request.getUserId());
        logger.info("🔍 DEBUG TEXTO - userMessage: '{}'", request.getUserMessage());
        logger.info("🔍 DEBUG TEXTO - metadata es null: {}", request.getMetadata() == null);
        if (request.getMetadata() != null) {
            logger.info("🔍 DEBUG TEXTO - metadata keys: {}", request.getMetadata().keySet());
            logger.info("🔍 DEBUG TEXTO - metadata completa: {}", request.getMetadata());
        }

        try {
            ConversationManager.ConversationResponse response = conversationManager.processMessage(request);
            
            if (response.isSuccess()) {
                logger.info("Mensaje de texto procesado exitosamente para sesión {}: intent={}, confidence={}", 
                    request.getSessionId(), response.getDetectedIntent(), response.getConfidenceScore());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Error procesando mensaje de texto para sesión {}: {}", 
                    request.getSessionId(), response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando mensaje de texto para sesión {}: {}", 
                request.getSessionId(), e.getMessage(), e);
            
            ConversationManager.ConversationResponse errorResponse = new ConversationManager.ConversationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Error interno del servidor: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Procesa un mensaje de audio del usuario en el contexto de una conversación.
     * Integra transcripción, clasificación de intención y generación de respuesta de audio.
     */
    @PostMapping(value = "/process/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConversationAudioResponse> processAudioMessage(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("sessionId") String sessionId,
            @RequestParam("userId") String userId,
            @RequestParam(value = "language", required = false, defaultValue = "es") String language,
            @RequestParam(value = "generateAudioResponse", required = false, defaultValue = "true") boolean generateAudioResponse,
            @RequestParam(value = "metadata", required = false) String metadataJson) {
        
        logger.info("Procesando mensaje de audio para sesión {}: archivo={}, tamaño={} bytes", 
                   sessionId, audioFile.getOriginalFilename(), audioFile.getSize());
        
        try {
            // Validar archivo de audio
            if (audioFile.isEmpty()) {
                return createAudioErrorResponse("El archivo de audio no puede estar vacío", HttpStatus.BAD_REQUEST);
            }

            // 1. Transcribir audio usando Whisper
            logger.info("Transcribiendo audio para sesión {}", sessionId);
            WhisperTranscriptionRequest whisperRequest = WhisperTranscriptionRequest.builder()
                .language(language)
                .timeoutSeconds(30)
                .maxRetries(3)
                .model("base")
                .build();
            
            WhisperTranscriptionResponse whisperResponse = whisperTranscriptionService.transcribeAudio(
                audioFile.getBytes(), whisperRequest);
            
            if (whisperResponse.getStatus() != WhisperTranscriptionResponse.TranscriptionStatus.SUCCESS) {
                logger.error("Error en transcripción para sesión {}: {}", sessionId, whisperResponse.getErrorMessage());
                return createAudioErrorResponse("Error en transcripción: " + whisperResponse.getErrorMessage(), 
                                              HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            String transcribedText = whisperResponse.getTranscription();
            logger.info("Audio transcrito para sesión {}: '{}'", sessionId, transcribedText);

            // 2. Procesar mensaje transcrito con ConversationManager
            logger.info("Procesando mensaje transcrito con ConversationManager para sesión {}", sessionId);
            ConversationManager.ConversationRequest conversationRequest = new ConversationManager.ConversationRequest();
            conversationRequest.setSessionId(sessionId);
            conversationRequest.setUserId(userId);
            conversationRequest.setUserMessage(transcribedText);
            
            // Añadir metadata si se proporciona
            logger.info("🔍 DEBUG METADATA - Evaluando metadata...");
            logger.info("🔍 DEBUG METADATA - metadataJson != null: {}", metadataJson != null);
            if (metadataJson != null) {
                logger.info("🔍 DEBUG METADATA - metadataJson.trim().isEmpty(): {}", metadataJson.trim().isEmpty());
                logger.info("🔍 DEBUG METADATA - Contenido completo: '{}'", metadataJson);
            }
            if (metadataJson != null && !metadataJson.trim().isEmpty()) {
                logger.info("🔍 DEBUG METADATA - Iniciando parsing de JSON metadata...");
                try {
                    // ✅ PARSEAR JSON DE MCP/TARGET CONTEXT 
                    Map<String, Object> metadata = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
                    logger.info("🔍 DEBUG METADATA - JSON parseado exitosamente. Claves disponibles: {}", metadata.keySet());
                    logger.info("🔍 DEBUG METADATA - Contenido completo del Map: {}", metadata);
                    
                    conversationRequest.setMetadata(metadata);
                    logger.info("Metadata parseada y añadida para sesión {}: mcp={}, target={}", 
                               sessionId, 
                               metadata.get("mcpContext"), 
                               metadata.get("targetContext"));
                } catch (Exception e) {
                    logger.warn("Error parseando metadata JSON para sesión {}: {} - metadata ignorada", sessionId, e.getMessage());
                    // Continuar sin metadata si hay error en el parsing
                }
            }
            
            ConversationManager.ConversationResponse conversationResponse = conversationManager.processMessage(conversationRequest);
            
            if (!conversationResponse.isSuccess()) {
                logger.error("Error procesando mensaje transcrito para sesión {}: {}", 
                           sessionId, conversationResponse.getErrorMessage());
                return createAudioErrorResponse("Error procesando mensaje: " + conversationResponse.getErrorMessage(), 
                                              HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // 3. Generar respuesta de audio si se solicita
            byte[] audioResponse = null;
            if (generateAudioResponse && conversationResponse.getSystemResponse() != null) {
                logger.info("Generando respuesta de audio para sesión {}", sessionId);
                try {
                    TtsGenerationRequest ttsRequest = new TtsGenerationRequest();
                    ttsRequest.setText(conversationResponse.getSystemResponse());
                    ttsRequest.setLanguage(language);
                    ttsRequest.setVoice("Abril");
                    ttsRequest.setSpeed(1.0);
                    
                    TtsGenerationResponse ttsResponse = ttsGenerationService.generateAudio(ttsRequest);
                    
                    if (ttsResponse.isSuccess()) {
                        audioResponse = ttsResponse.getAudioData();
                        logger.info("Respuesta de audio generada para sesión {}: {} bytes", sessionId, audioResponse.length);
                    } else {
                        logger.warn("Error generando respuesta de audio para sesión {}: {}", 
                                  sessionId, ttsResponse.getErrorMessage());
                    }
                } catch (Exception e) {
                    logger.warn("Error generando respuesta de audio para sesión {}: {}", sessionId, e.getMessage());
                }
            }

            // 4. Crear respuesta unificada
            ConversationAudioResponse response = new ConversationAudioResponse();
            response.setSuccess(true);
            response.setSessionId(sessionId);
            response.setTranscribedText(transcribedText);
            response.setSystemResponse(conversationResponse.getSystemResponse());
            response.setDetectedIntent(conversationResponse.getDetectedIntent());
            response.setConfidenceScore(conversationResponse.getConfidenceScore());
            response.setExtractedEntities(conversationResponse.getExtractedEntities());
            response.setSessionState(conversationResponse.getSessionState());
            response.setTurnCount(conversationResponse.getTurnCount());
            response.setProcessingTimeMs(conversationResponse.getProcessingTimeMs());
            response.setAudioResponse(audioResponse);
            response.setAudioResponseGenerated(audioResponse != null);
            response.setWhisperConfidence(whisperResponse.getConfidence());
            response.setWhisperLanguage(whisperResponse.getDetectedLanguage());

            logger.info("Mensaje de audio procesado exitosamente para sesión {}: intent={}, confidence={}", 
                       sessionId, response.getDetectedIntent(), response.getConfidenceScore());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error procesando mensaje de audio para sesión {}: {}", sessionId, e.getMessage(), e);
            return createAudioErrorResponse("Error procesando mensaje de audio: " + e.getMessage(), 
                                          HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Procesa un mensaje de audio simple sin generar respuesta de audio.
     */
    @PostMapping(value = "/process/audio/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConversationAudioResponse> processSimpleAudioMessage(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("sessionId") String sessionId,
            @RequestParam("userId") String userId,
            @RequestParam(value = "language", required = false, defaultValue = "es") String language) {
        
        return processAudioMessage(audioFile, sessionId, userId, language, false, null);
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
            
            conversationManager.getOrCreateSession(testSessionId, testUserId);
            
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

    private ResponseEntity<ConversationAudioResponse> createAudioErrorResponse(String message, HttpStatus status) {
        ConversationAudioResponse response = new ConversationAudioResponse();
        response.setSuccess(false);
        response.setErrorMessage(message);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Clase de respuesta para mensajes de audio procesados en conversación.
     * Extiende la respuesta de conversación normal con información adicional de audio.
     */
    public static class ConversationAudioResponse {
        private boolean success;
        private String sessionId;
        private String transcribedText;
        private String systemResponse;
        private String detectedIntent;
        private double confidenceScore;
        private Map<String, Object> extractedEntities;
        private String sessionState;
        private int turnCount;
        private long processingTimeMs;
        private byte[] audioResponse;
        private boolean audioResponseGenerated;
        private Double whisperConfidence;
        private String whisperLanguage;
        private String errorMessage;

        // Getters y Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getTranscribedText() { return transcribedText; }
        public void setTranscribedText(String transcribedText) { this.transcribedText = transcribedText; }
        
        public String getSystemResponse() { return systemResponse; }
        public void setSystemResponse(String systemResponse) { this.systemResponse = systemResponse; }
        
        public String getDetectedIntent() { return detectedIntent; }
        public void setDetectedIntent(String detectedIntent) { this.detectedIntent = detectedIntent; }
        
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
        
        public Map<String, Object> getExtractedEntities() { return extractedEntities; }
        public void setExtractedEntities(Map<String, Object> extractedEntities) { this.extractedEntities = extractedEntities; }
        
        public String getSessionState() { return sessionState; }
        public void setSessionState(String sessionState) { this.sessionState = sessionState; }
        
        public int getTurnCount() { return turnCount; }
        public void setTurnCount(int turnCount) { this.turnCount = turnCount; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        
        public byte[] getAudioResponse() { return audioResponse; }
        public void setAudioResponse(byte[] audioResponse) { this.audioResponse = audioResponse; }
        
        public boolean isAudioResponseGenerated() { return audioResponseGenerated; }
        public void setAudioResponseGenerated(boolean audioResponseGenerated) { this.audioResponseGenerated = audioResponseGenerated; }
        
        public Double getWhisperConfidence() { return whisperConfidence; }
        public void setWhisperConfidence(Double whisperConfidence) { this.whisperConfidence = whisperConfidence; }
        
        public String getWhisperLanguage() { return whisperLanguage; }
        public void setWhisperLanguage(String whisperLanguage) { this.whisperLanguage = whisperLanguage; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    // ❌ CLASES ELIMINADAS: EnrichedConversationRequest, ClarificationRequest
    // ✅ NUEVO COMPORTAMIENTO: El contexto MCP/Target se envía mediante:
    //    1. Campo 'metadata' estándar en ConversationRequest para /process
    //    2. Parámetro 'metadata' JSON string para /process/audio
    //    3. Mensaje WebSocket con campo 'metadata' para /ws/conversation
    //
    // Formato de metadata esperado:
    // {
    //   "mcpContext": {
    //     "selectedMcp": "docker_mcp",
    //     "buttonId": "docker_status",
    //     "timestamp": "..."
    //   },
    //   "targetContext": {
    //     "selectedTarget": "raspberry_pi_local",
    //     "isPersistent": true,
    //     "timestamp": "..."
    //   },
    //   "interactionContext": {
    //     "source": "button_click",
    //     "timestamp": "..."
    //   },
    //   "clarificationContext": {  // Para resolución de conflictos
    //     "isResponse": true,
    //     "selectedOption": "mcp",
    //     "conflictId": "...",
    //     "timestamp": "..."
    //   }
    // }
}