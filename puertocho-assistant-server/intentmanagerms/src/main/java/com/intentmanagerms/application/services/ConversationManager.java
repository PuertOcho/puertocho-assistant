package com.intentmanagerms.application.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gestor principal de conversaciones que mantiene contexto persistente y coordina el flujo conversacional.
 * Integra con RagIntentClassifier y sistema de votación MoE para proporcionar conversaciones inteligentes.
 */
@Service
public class ConversationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConversationManager.class);

    @Autowired
    private RagIntentClassifier ragIntentClassifier;

    @Autowired
    private LlmVotingService llmVotingService;

    @Autowired
    private SlotFillingService slotFillingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Configuración
    @Value("${conversation.session.timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    @Value("${conversation.max-turns:50}")
    private int maxTurns;

    @Value("${conversation.context-compression-threshold:10}")
    private int contextCompressionThreshold;

    @Value("${conversation.enable-anaphora-resolution:true}")
    private boolean enableAnaphoraResolution;

    @Value("${conversation.enable-dynamic-decomposition:true}")
    private boolean enableDynamicDecomposition;

    @Value("${conversation.enable-parallel-execution:true}")
    private boolean enableParallelExecution;

    @Value("${conversation.max-parallel-tasks:3}")
    private int maxParallelTasks;

    @Value("${conversation.progress-update-interval-ms:1000}")
    private long progressUpdateIntervalMs;

    // Cache en memoria para sesiones activas
    private final Map<String, ConversationSession> activeSessions = new ConcurrentHashMap<>();

    // Estadísticas
    private long totalSessionsCreated = 0;
    private long totalSessionsCompleted = 0;
    private long totalTurnsProcessed = 0;
    private long totalProcessingTimeMs = 0;

    /**
     * Procesa un mensaje del usuario en el contexto de una conversación.
     */
    public ConversationResponse processMessage(ConversationRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId();
        String userId = request.getUserId();
        String userMessage = request.getUserMessage();

        logger.info("Procesando mensaje para sesión {}: {}", sessionId, userMessage);

        try {
            // Obtener o crear sesión
            ConversationSession session = getOrCreateSession(sessionId, userId);
            
            // Verificar si la sesión puede recibir mensajes
            if (!session.getState().canReceiveMessages()) {
                return createErrorResponse("La sesión no puede recibir mensajes en estado: " + session.getState());
            }

            // Crear turno de conversación
            ConversationTurn turn = new ConversationTurn(userMessage);
            turn.setTurnNumber(session.getTurnCount() + 1);

            // Clasificar intención usando RAG
            IntentClassificationRequest classificationRequest = new IntentClassificationRequest();
            classificationRequest.setText(userMessage);
            classificationRequest.setSessionId(sessionId);
            classificationRequest.setUserId(userId);
            classificationRequest.setContextMetadata(buildContextMetadata(session));

            IntentClassificationResult classificationResult = ragIntentClassifier.classifyIntent(classificationRequest);
            
            // Actualizar turno con resultados de clasificación
            turn.setDetectedIntent(classificationResult.getIntentId());
            turn.setConfidenceScore(classificationResult.getConfidenceScore() != null ? classificationResult.getConfidenceScore() : 0.0);
            turn.setExtractedEntities(classificationResult.getDetectedEntities());

            // Usar sistema de votación MoE si está habilitado
            // TODO: Implementar verificación de MoE habilitado cuando esté disponible
            VotingRound votingRound = llmVotingService.executeVotingRound(
                sessionId, 
                userMessage, 
                buildConversationContext(session),
                buildConversationHistory(session)
            );
            
            // Actualizar turno con resultados de votación
            if (votingRound.getConsensus() != null) {
                turn.setDetectedIntent(votingRound.getConsensus().getFinalIntent());
                turn.setConfidenceScore(votingRound.getConsensus().getConsensusConfidence() != null ? votingRound.getConsensus().getConsensusConfidence() : 0.0);
                turn.setExtractedEntities(votingRound.getConsensus().getFinalEntities());
            }

            // Actualizar contexto de la sesión
            updateSessionContext(session, turn, classificationResult);

            // Generar respuesta del sistema
            String systemResponse = generateSystemResponse(session, turn, classificationResult);
            turn.setSystemResponse(systemResponse);

            // Añadir turno a la sesión
            session.addTurn(turn);

            // Persistir sesión
            persistSession(session);

            // Actualizar estadísticas
            updateStatistics(startTime);

            logger.info("Mensaje procesado exitosamente para sesión {}: intent={}, confidence={}", 
                sessionId, turn.getDetectedIntent(), turn.getConfidenceScore());

            return createSuccessResponse(session, turn, systemResponse);

        } catch (Exception e) {
            logger.error("Error procesando mensaje para sesión {}: {}", sessionId, e.getMessage(), e);
            return createErrorResponse("Error procesando mensaje: " + e.getMessage());
        }
    }

    /**
     * Obtiene o crea una sesión de conversación.
     */
    public ConversationSession getOrCreateSession(String sessionId, String userId) {
        // Buscar en cache local primero
        ConversationSession session = activeSessions.get(sessionId);
        
        if (session != null && !session.isExpired()) {
            session.updateActivity();
            return session;
        }

        // Buscar en Redis
        session = loadSessionFromRedis(sessionId);
        
        if (session != null && !session.isExpired()) {
            session.updateActivity();
            activeSessions.put(sessionId, session);
            return session;
        }

        // Crear nueva sesión
        session = new ConversationSession(userId);
        session.setSessionId(sessionId);
        session.setTimeoutMinutes(sessionTimeoutMinutes);
        session.setMaxTurns(maxTurns);
        
        activeSessions.put(sessionId, session);
        persistSession(session);
        
        totalSessionsCreated++;
        logger.info("Nueva sesión creada: {}", sessionId);
        
        return session;
    }

    /**
     * Obtiene una sesión existente.
     */
    public ConversationSession getSession(String sessionId) {
        ConversationSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            session = loadSessionFromRedis(sessionId);
            if (session != null && !session.isExpired()) {
                activeSessions.put(sessionId, session);
            }
        }
        
        return session;
    }

    /**
     * Finaliza una sesión de conversación.
     */
    public boolean endSession(String sessionId) {
        ConversationSession session = getSession(sessionId);
        
        if (session != null) {
            session.setState(ConversationState.COMPLETED);
            session.setActive(false);
            persistSession(session);
            activeSessions.remove(sessionId);
            
            totalSessionsCompleted++;
            logger.info("Sesión finalizada: {}", sessionId);
            
            return true;
        }
        
        return false;
    }

    /**
     * Cancela una sesión de conversación.
     */
    public boolean cancelSession(String sessionId) {
        ConversationSession session = getSession(sessionId);
        
        if (session != null) {
            session.setState(ConversationState.CANCELLED);
            session.setActive(false);
            persistSession(session);
            activeSessions.remove(sessionId);
            
            logger.info("Sesión cancelada: {}", sessionId);
            
            return true;
        }
        
        return false;
    }

    /**
     * Limpia sesiones expiradas.
     */
    public void cleanupExpiredSessions() {
        List<String> expiredSessions = new ArrayList<>();
        
        for (Map.Entry<String, ConversationSession> entry : activeSessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredSessions.add(entry.getKey());
            }
        }
        
        for (String sessionId : expiredSessions) {
            ConversationSession session = activeSessions.remove(sessionId);
            if (session != null) {
                session.setState(ConversationState.EXPIRED);
                session.setActive(false);
                persistSession(session);
                logger.info("Sesión expirada limpiada: {}", sessionId);
            }
        }
    }

    /**
     * Obtiene estadísticas del gestor de conversaciones.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("active_sessions", activeSessions.size());
        stats.put("total_sessions_created", totalSessionsCreated);
        stats.put("total_sessions_completed", totalSessionsCompleted);
        stats.put("total_turns_processed", totalTurnsProcessed);
        stats.put("average_processing_time_ms", 
            totalTurnsProcessed > 0 ? totalProcessingTimeMs / totalTurnsProcessed : 0);
        stats.put("session_timeout_minutes", sessionTimeoutMinutes);
        stats.put("max_turns", maxTurns);
        stats.put("context_compression_threshold", contextCompressionThreshold);
        stats.put("enable_anaphora_resolution", enableAnaphoraResolution);
        stats.put("enable_dynamic_decomposition", enableDynamicDecomposition);
        stats.put("enable_parallel_execution", enableParallelExecution);
        stats.put("max_parallel_tasks", maxParallelTasks);
        stats.put("progress_update_interval_ms", progressUpdateIntervalMs);
        
        return stats;
    }

    /**
     * Verifica el estado de salud del servicio.
     */
    public boolean isHealthy() {
        try {
            // Verificar conexión a Redis
            redisTemplate.opsForValue().get("health_check");
            
            // Verificar servicios dependientes
            boolean ragHealthy = ragIntentClassifier != null;
            boolean votingHealthy = llmVotingService != null;
            
            return ragHealthy && votingHealthy;
        } catch (Exception e) {
            logger.error("Error en health check: {}", e.getMessage());
            return false;
        }
    }

    // Métodos privados de utilidad

    private Map<String, Object> buildContextMetadata(ConversationSession session) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (session.getContext() != null) {
            metadata.put("user_preferences", session.getContext().getUserPreferences());
            metadata.put("device_context", session.getContext().getDeviceContext());
            metadata.put("location_context", session.getContext().getLocationContext());
            metadata.put("temporal_context", session.getContext().getTemporalContext());
            metadata.put("intent_history", session.getContext().getIntentHistory());
        }
        
        metadata.put("turn_count", session.getTurnCount());
        metadata.put("session_state", session.getState().getCode());
        metadata.put("session_duration_minutes", 
            java.time.Duration.between(session.getCreatedAt(), LocalDateTime.now()).toMinutes());
        
        return metadata;
    }

    private Map<String, Object> buildConversationContext(ConversationSession session) {
        Map<String, Object> context = new HashMap<>();
        
        if (session.getContext() != null) {
            context.put("context_id", session.getContext().getContextId());
            context.put("user_preferences", session.getContext().getUserPreferences());
            context.put("conversation_metadata", session.getContext().getConversationMetadata());
            context.put("entity_cache", session.getContext().getEntityCache());
            context.put("conversation_summary", session.getContext().getConversationSummary());
        }
        
        context.put("current_intent", session.getCurrentIntent());
        context.put("slots", session.getSlots());
        context.put("turn_count", session.getTurnCount());
        context.put("session_state", session.getState().getCode());
        
        return context;
    }

    private List<String> buildConversationHistory(ConversationSession session) {
        List<String> history = new ArrayList<>();
        
        if (session.getConversationHistory() != null) {
            for (ConversationTurn turn : session.getConversationHistory()) {
                if (turn.hasUserMessage()) {
                    history.add("Usuario: " + turn.getUserMessage());
                }
                if (turn.hasSystemResponse()) {
                    history.add("Sistema: " + turn.getSystemResponse());
                }
            }
        }
        
        return history;
    }

    private void updateSessionContext(ConversationSession session, ConversationTurn turn, IntentClassificationResult result) {
        // Actualizar intent actual
        if (result.getIntentId() != null) {
            session.setCurrentIntent(result.getIntentId());
            session.getContext().recordIntent(result.getIntentId());
        }

        // Actualizar slots
        if (result.getDetectedEntities() != null) {
            for (Map.Entry<String, Object> entry : result.getDetectedEntities().entrySet()) {
                session.updateSlot(entry.getKey(), entry.getValue());
                session.getContext().cacheEntity(entry.getKey(), entry.getValue());
            }
        }

        // Actualizar metadatos de conversación
        session.updateMetadata("last_intent", result.getIntentId());
        session.updateMetadata("last_confidence", result.getConfidenceScore());
        session.updateMetadata("last_processing_time_ms", turn.getProcessingTimeMs());

        // Comprimir contexto si es necesario
        if (session.getContext().needsCompression(contextCompressionThreshold)) {
            session.getContext().compressContext();
        }
    }

    private String generateSystemResponse(ConversationSession session, ConversationTurn turn, IntentClassificationResult result) {
        // Verificar confianza de clasificación
        if (result.getConfidenceScore() != null && result.getConfidenceScore() < 0.7) {
            return "No estoy seguro de entender. ¿Podrías reformular tu petición?";
        }

        try {
            // Preparar solicitud de slot-filling
            SlotFillingRequest slotRequest = new SlotFillingRequest(
                result.getIntentId(), 
                turn.getUserMessage(), 
                session.getSessionId()
            );
            slotRequest.setCurrentSlots(session.getSlots() != null ? session.getSlots() : new HashMap<>());
            slotRequest.setConversationContext(buildSlotFillingContext(session));
            slotRequest.setUserPreferences(getUserPreferences(session));

            // Procesar slot-filling
            SlotFillingResult slotResult = slotFillingService.processSlotFilling(slotRequest);
            
            if (!slotResult.isSuccess()) {
                logger.warn("Error en slot-filling para intent '{}': {}", result.getIntentId(), slotResult.getErrorMessage());
                return generateFallbackResponse(result.getIntentId());
            }

            // Actualizar slots en la sesión
            if (slotResult.getFilledSlots() != null) {
                session.getSlots().putAll(slotResult.getFilledSlots());
            }

            // Verificar si faltan slots
            if (!slotResult.isSlotsCompleted()) {
                // Cambiar estado de la sesión a WAITING_SLOTS
                session.setState(ConversationState.WAITING_SLOTS);
                
                // Devolver pregunta generada dinámicamente
                String question = slotResult.getGeneratedQuestion();
                if (question != null && !question.trim().isEmpty()) {
                    return question;
                } else {
                    return generateFallbackSlotQuestion(slotResult.getNextSlotToFill());
                }
            } else {
                // Todos los slots están completos, proceder con la acción
                session.setState(ConversationState.EXECUTING_TASKS);
                return generateExecutionResponse(result.getIntentId(), slotResult.getFilledSlots());
            }

        } catch (Exception e) {
            logger.error("Error en generación de respuesta con slot-filling para intent '{}': {}", 
                        result.getIntentId(), e.getMessage(), e);
            return generateFallbackResponse(result.getIntentId());
        }
    }

    /**
     * Construye el contexto para slot-filling desde la sesión conversacional
     */
    private Map<String, Object> buildSlotFillingContext(ConversationSession session) {
        Map<String, Object> context = new HashMap<>();
        
        if (session.getContext() != null) {
            context.put("user_preferences", session.getContext().getUserPreferences());
            context.put("entity_cache", session.getContext().getEntityCache());
            context.put("device_context", session.getContext().getDeviceContext());
            context.put("location_context", session.getContext().getLocationContext());
            context.put("conversation_metadata", session.getContext().getConversationMetadata());
        }
        
        context.put("session_id", session.getSessionId());
        context.put("turn_count", session.getTurnCount());
        context.put("current_intent", session.getCurrentIntent());
        
        return context;
    }

    /**
     * Obtiene preferencias del usuario desde la sesión
     */
    private Map<String, Object> getUserPreferences(ConversationSession session) {
        if (session.getContext() != null && session.getContext().getUserPreferences() != null) {
            return session.getContext().getUserPreferences();
        }
        return new HashMap<>();
    }

    /**
     * Genera respuesta cuando todos los slots están completos y se puede ejecutar la acción
     */
    private String generateExecutionResponse(String intentId, Map<String, Object> filledSlots) {
        switch (intentId) {
            case "consultar_tiempo":
                String ubicacion = (String) filledSlots.get("ubicacion");
                return ubicacion != null ? 
                    "Consultando el tiempo en " + ubicacion + "..." :
                    "Consultando el tiempo...";
                    
            case "encender_luz":
                String lugar = (String) filledSlots.get("lugar");
                return lugar != null ?
                    "Encendiendo la luz en " + lugar + "..." :
                    "Encendiendo la luz...";
                    
            default:
                return "Procesando tu petición...";
        }
    }

    /**
     * Genera respuesta de fallback cuando falla el slot-filling
     */
    private String generateFallbackResponse(String intentId) {
        switch (intentId) {
            case "consultar_tiempo":
                return "¿De qué ciudad quieres consultar el tiempo?";
            case "encender_luz":
                return "¿En qué habitación quieres encender la luz?";
            default:
                return "¿Podrías proporcionar más información para completar tu petición?";
        }
    }

    /**
     * Genera pregunta de fallback para un slot específico
     */
    private String generateFallbackSlotQuestion(String slotName) {
        if (slotName == null) {
            return "¿Podrías proporcionar más información?";
        }
        
        switch (slotName.toLowerCase()) {
            case "ubicacion":
            case "lugar":
                return "¿En qué lugar o ubicación?";
            case "fecha":
                return "¿Para qué fecha?";
            case "hora":
                return "¿A qué hora?";
            default:
                return "¿Podrías especificar " + slotName + "?";
        }
    }

    private void persistSession(ConversationSession session) {
        try {
            String key = "conversation:" + session.getSessionId();
            redisTemplate.opsForValue().set(key, session, sessionTimeoutMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("Error persistiendo sesión {}: {}", session.getSessionId(), e.getMessage());
        }
    }

    private ConversationSession loadSessionFromRedis(String sessionId) {
        try {
            String key = "conversation:" + sessionId;
            Object obj = redisTemplate.opsForValue().get(key);
            return obj instanceof ConversationSession ? (ConversationSession) obj : null;
        } catch (Exception e) {
            logger.error("Error cargando sesión {} desde Redis: {}", sessionId, e.getMessage());
            return null;
        }
    }

    private ConversationResponse createSuccessResponse(ConversationSession session, ConversationTurn turn, String systemResponse) {
        ConversationResponse response = new ConversationResponse();
        response.setSuccess(true);
        response.setSessionId(session.getSessionId());
        response.setSystemResponse(systemResponse);
        response.setDetectedIntent(turn.getDetectedIntent());
        response.setConfidenceScore(turn.getConfidenceScore());
        response.setExtractedEntities(turn.getExtractedEntities());
        response.setSessionState(session.getState().getCode());
        response.setTurnCount(session.getTurnCount());
        response.setProcessingTimeMs(turn.getProcessingTimeMs());
        return response;
    }

    private ConversationResponse createErrorResponse(String errorMessage) {
        ConversationResponse response = new ConversationResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    private void updateStatistics(long startTime) {
        totalTurnsProcessed++;
        totalProcessingTimeMs += (System.currentTimeMillis() - startTime);
    }

    // Clases de request/response

    public static class ConversationRequest {
        @JsonProperty("sessionId")
        private String sessionId;
        @JsonProperty("userId")
        private String userId;
        @JsonProperty("userMessage")
        private String userMessage;
        @JsonProperty("metadata")
        private Map<String, Object> metadata;

        // Getters y Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    public static class ConversationResponse {
        private boolean success;
        private String sessionId;
        private String systemResponse;
        private String detectedIntent;
        private double confidenceScore;
        private Map<String, Object> extractedEntities;
        private String sessionState;
        private int turnCount;
        private long processingTimeMs;
        private String errorMessage;

        // Getters y Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
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
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
} 