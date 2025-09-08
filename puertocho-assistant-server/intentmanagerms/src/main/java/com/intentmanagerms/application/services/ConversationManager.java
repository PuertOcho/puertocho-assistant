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
    private JsonIntentClassifier jsonIntentClassifier;

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
        
        // Debug información del contexto de la petición
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            logger.info("DEBUG - Metadata presente en la petición para sesión {}", sessionId);
            
            Object mcpContext = request.getMetadata().get("mcpContext");
            if (mcpContext != null) {
                try {
                    Map<String, Object> mcp = (Map<String, Object>) mcpContext;
                    logger.info("DEBUG - MCP Context: selectedMcp={}, buttonId={}, timestamp={}", 
                              mcp.get("selectedMcp"), mcp.get("buttonId"), mcp.get("timestamp"));
                } catch (Exception e) {
                    logger.warn("DEBUG - Error parsing MCP context: {}", e.getMessage());
                }
            } else {
                logger.info("DEBUG - No MCP context in request metadata");
            }
            
            Object targetContext = request.getMetadata().get("targetContext");
            if (targetContext != null) {
                try {
                    Map<String, Object> target = (Map<String, Object>) targetContext;
                    logger.info("DEBUG - Target Context: selectedTarget={}, isPersistent={}, timestamp={}", 
                              target.get("selectedTarget"), target.get("isPersistent"), target.get("timestamp"));
                } catch (Exception e) {
                    logger.warn("DEBUG - Error parsing Target context: {}", e.getMessage());
                }
            } else {
                logger.info("DEBUG - No Target context in request metadata");
            }
            
            Object interactionContext = request.getMetadata().get("interactionContext");
            if (interactionContext != null) {
                try {
                    Map<String, Object> interaction = (Map<String, Object>) interactionContext;
                    logger.info("DEBUG - Interaction Context: source={}, timestamp={}", 
                              interaction.get("source"), interaction.get("timestamp"));
                } catch (Exception e) {
                    logger.warn("DEBUG - Error parsing Interaction context: {}", e.getMessage());
                }
            } else {
                logger.info("DEBUG - No Interaction context in request metadata");
            }
        } else {
            logger.info("DEBUG - No metadata presente en la petición para sesión {}", sessionId);
        }

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

            // ✅ PROCESAR CONTEXTO MCP Y TARGET ENRIQUECIDO
            Map<String, Object> enrichedContext = processEnrichedContext(request.getMetadata(), session);
            
            // ✅ MANEJAR RESPUESTAS DE CLARIFICACIÓN PRIMERO
            if (request.getMetadata() != null && request.getMetadata().containsKey("clarificationContext")) {
                return processClarificationResponse(session, request.getMetadata(), userMessage, turn);
            }
            
            // ✅ DETECTAR Y MANEJAR CONFLICTOS DE CONTEXTO (solo si no es clarificación)
            ConflictResolution conflictCheck = detectContextConflicts(userMessage, enrichedContext);
            if (conflictCheck.hasConflict()) {
                return createConflictResponse(session, conflictCheck, turn);
            }

            // Clasificar intención usando JSON directo con contexto enriquecido
            IntentClassificationRequest classificationRequest = new IntentClassificationRequest();
            classificationRequest.setText(userMessage);
            classificationRequest.setSessionId(sessionId);
            classificationRequest.setUserId(userId);
            // ✅ COMBINAR CONTEXTO TRADICIONAL CON CONTEXTO ENRIQUECIDO
            Map<String, Object> combinedMetadata = buildContextMetadata(session);
            combinedMetadata.putAll(enrichedContext);
            classificationRequest.setContextMetadata(combinedMetadata);

            IntentClassificationResult classificationResult = jsonIntentClassifier.classifyIntent(classificationRequest);
            
            // Actualizar turno con resultados de clasificación
            turn.setDetectedIntent(classificationResult.getIntentId());
            turn.setConfidenceScore(classificationResult.getConfidenceScore() != null ? classificationResult.getConfidenceScore() : 0.0);
            turn.setExtractedEntities(classificationResult.getDetectedEntities());

            // ✅ USAR SISTEMA DE VOTACIÓN CON CONTEXTO ENRIQUECIDO
            Map<String, Object> conversationContext = buildConversationContext(session);
            conversationContext.putAll(enrichedContext);
            
            VotingRound votingRound = llmVotingService.executeVotingRound(
                sessionId, 
                userMessage, 
                conversationContext,
                buildConversationHistory(session)
            );
            
            // Actualizar turno con resultados de votación
            if (votingRound.getConsensus() != null) {
                turn.setDetectedIntent(votingRound.getConsensus().getFinalIntent());
                turn.setConfidenceScore(votingRound.getConsensus().getConsensusConfidence() != null ? votingRound.getConsensus().getConsensusConfidence() : 0.0);
                turn.setExtractedEntities(votingRound.getConsensus().getFinalEntities());
            }

            // Actualizar contexto de la sesión usando SIEMPRE el resultado final del turno (consenso MoE)
            updateSessionContext(session, turn);

            // Generar respuesta del sistema basada en la intención/confianza finales del turno
            String systemResponse = generateSystemResponse(session, turn);
            turn.setSystemResponse(systemResponse);

            // Añadir turno a la sesión
            session.addTurn(turn);

            // Persistir sesión
            persistSession(session);

            // Actualizar estadísticas
            updateStatistics(startTime);

            logger.info("Mensaje procesado exitosamente para sesión {}: intent={}, confidence={}", 
                sessionId, turn.getDetectedIntent(), turn.getConfidenceScore());
            
            // Debug final: resumen del contexto procesado
            Object mcpCtx = session.getMetadata("mcpContext");
            Object targetCtx = session.getMetadata("persistent_target");
            logger.debug("DEBUG - Final session context: MCP={}, Target={}, State={}", 
                        mcpCtx != null ? ((Map<String, Object>) mcpCtx).get("selectedMcp") : "none",
                        targetCtx != null ? ((Map<String, Object>) targetCtx).get("label") : "none",
                        session.getState());

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
            boolean jsonClassifierHealthy = jsonIntentClassifier != null && jsonIntentClassifier.isHealthy();
            boolean votingHealthy = llmVotingService != null;
            
            return jsonClassifierHealthy && votingHealthy;
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

    private void updateSessionContext(ConversationSession session, ConversationTurn turn) {
        // Actualizar intent actual con el intent final del turno
        if (turn.getDetectedIntent() != null) {
            session.setCurrentIntent(turn.getDetectedIntent());
            if (session.getContext() != null) {
                session.getContext().recordIntent(turn.getDetectedIntent());
            }
        }

        // Actualizar slots con las entidades finales del turno
        if (turn.getExtractedEntities() != null) {
            for (Map.Entry<String, Object> entry : turn.getExtractedEntities().entrySet()) {
                session.updateSlot(entry.getKey(), entry.getValue());
                if (session.getContext() != null) {
                    session.getContext().cacheEntity(entry.getKey(), entry.getValue());
                }
            }
        }

        // Actualizar metadatos de conversación
        session.updateMetadata("last_intent", turn.getDetectedIntent());
        session.updateMetadata("last_confidence", turn.getConfidenceScore());
        session.updateMetadata("last_processing_time_ms", turn.getProcessingTimeMs());

        // Comprimir contexto si es necesario
        if (session.getContext() != null && session.getContext().needsCompression(contextCompressionThreshold)) {
            session.getContext().compressContext();
        }
    }

    private String generateSystemResponse(ConversationSession session, ConversationTurn turn) {
        // Verificar confianza usando SIEMPRE la confianza final del turno
        if (turn.getConfidenceScore() < 0.7) {
            return "No estoy seguro de entender. ¿Podrías reformular tu petición?";
        }

        try {
            // Preparar solicitud de slot-filling con el intent final del turno
            SlotFillingRequest slotRequest = new SlotFillingRequest(
                turn.getDetectedIntent(), 
                turn.getUserMessage(), 
                session.getSessionId()
            );
            slotRequest.setCurrentSlots(session.getSlots() != null ? session.getSlots() : new HashMap<>());
            slotRequest.setConversationContext(buildSlotFillingContext(session));
            slotRequest.setUserPreferences(getUserPreferences(session));

            // Procesar slot-filling
            SlotFillingResult slotResult = slotFillingService.processSlotFilling(slotRequest);
            
            if (!slotResult.isSuccess()) {
                logger.warn("Error en slot-filling para intent '{}': {}", turn.getDetectedIntent(), slotResult.getErrorMessage());
                return generateFallbackResponse(turn.getDetectedIntent());
            }

            // Actualizar slots en la sesión
                if (slotResult.getFilledSlots() != null && !slotResult.getFilledSlots().isEmpty()) {
                // Asegurar que el mapa de slots de la sesión esté inicializado
                if (session.getSlots() == null) {
                    session.updateSlot("__init__", null);
                    session.getSlots().remove("__init__");
                }
                for (Map.Entry<String, Object> entry : slotResult.getFilledSlots().entrySet()) {
                    session.updateSlot(entry.getKey(), entry.getValue());
                }
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
                // Todos los slots están completos
                if (isExecutableIntent(turn.getDetectedIntent())) {
                    // Proceder con la acción solo para intents ejecutables
                    session.setState(ConversationState.EXECUTING_TASKS);
                    String execResponse = generateExecutionResponse(turn.getDetectedIntent(), slotResult.getFilledSlots());
                    // Simular finalización inmediata de la tarea para permitir continuidad de conversación
                    session.setState(ConversationState.ACTIVE);
                    return execResponse;
                } else {
                    // Mantener la conversación activa para intents informativos/genéricos
                    session.setState(ConversationState.ACTIVE);
                    return generateFallbackResponse(turn.getDetectedIntent());
                }
            }

        } catch (Exception e) {
            logger.error("Error en generación de respuesta con slot-filling para intent '{}': {}", 
                        turn.getDetectedIntent(), e.getMessage(), e);
            return generateFallbackResponse(turn.getDetectedIntent());
        }
    }

    private boolean isExecutableIntent(String intentId) {
        if (intentId == null) {
            return false;
        }
        switch (intentId) {
            case "consultar_tiempo":
            case "encender_luz":
                return true;
            default:
                return false;
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

    // ✅ NUEVAS FUNCIONES PARA PROCESAMIENTO DE CONTEXTO MCP/TARGET

    /**
     * Procesa el contexto enriquecido de MCP y Target desde la metadata
     */
    private Map<String, Object> processEnrichedContext(Map<String, Object> metadata, ConversationSession session) {
        Map<String, Object> enrichedContext = new HashMap<>();
        
        if (metadata != null) {
            // Procesar contexto MCP
            Object mcpContext = metadata.get("mcpContext");
            if (mcpContext instanceof Map) {
                Map<String, Object> mcp = (Map<String, Object>) mcpContext;
                enrichedContext.put("available_mcp_actions", mcp.get("availableActions"));
                enrichedContext.put("selected_mcp_action", mcp);  // ✅ Guardar objeto completo
                enrichedContext.put("mcp_matrix_config", mcp.get("matrixConfig"));
                
                // IMPORTANTE: Marcar que hay contexto MCP activo
                enrichedContext.put("mcp_context_active", true);
                
                // ✅ GUARDAR CONTEXTO MCP EN SESIÓN PARA RECUPERACIÓN POSTERIOR
                session.updateMetadata("mcpContext", mcp);
                
                logger.info("Processing MCP context: selectedMcp={}, buttonId={}", 
                           mcp.get("selectedMcp"), mcp.get("buttonId"));
                logger.debug("DEBUG - MCP Context full details: availableActions={}, matrixConfig={}", 
                           mcp.get("availableActions"), mcp.get("matrixConfig"));
            }
            
            // Procesar contexto de target
            Object targetContext = metadata.get("targetContext");
            if (targetContext instanceof Map) {
                Map<String, Object> target = (Map<String, Object>) targetContext;
                enrichedContext.put("selected_target", target.get("selectedTarget"));
                enrichedContext.put("available_targets", target.get("availableTargets"));
                
                // ✅ PERSISTIR TARGET EN SESIÓN
                Map<String, Object> selectedTarget = (Map<String, Object>) target.get("selectedTarget");
                if (selectedTarget != null) {
                    session.updateMetadata("persistent_target", selectedTarget);
                    logger.info("Target persisted in session: {} ({})", 
                               selectedTarget.get("label"), selectedTarget.get("id"));
                    logger.debug("DEBUG - Target full details: isPersistent={}, availableTargets={}", 
                               target.get("isPersistent"), target.get("availableTargets"));
                }
            } else {
                // ✅ RECUPERAR TARGET PERSISTENTE SI NO SE ENVÍA
                Object persistentTarget = session.getMetadata("persistent_target");
                if (persistentTarget != null) {
                    enrichedContext.put("selected_target", persistentTarget);
                    logger.info("Using persistent target from session: {}", 
                               ((Map<String, Object>) persistentTarget).get("label"));
                }
            }
            
            // Procesar contexto de interacción
            Object interactionContext = metadata.get("interactionContext");
            if (interactionContext instanceof Map) {
                Map<String, Object> interaction = (Map<String, Object>) interactionContext;
                enrichedContext.put("interaction_source", interaction.get("source"));
                enrichedContext.put("triggered_action", interaction.get("triggeredAction"));
                
                logger.info("Processing interaction context: source={}, action={}", 
                           interaction.get("source"), interaction.get("triggeredAction"));
            }
        }
        
        return enrichedContext;
    }

    /**
     * Detecta conflictos entre el contexto MCP seleccionado y el comando de voz
     */
    private ConflictResolution detectContextConflicts(String userMessage, Map<String, Object> enrichedContext) {
        ConflictResolution resolution = new ConflictResolution();
        resolution.setHasConflict(false);
        
        // Verificar si hay contexto MCP activo
        boolean mcpActive = Boolean.TRUE.equals(enrichedContext.get("mcp_context_active"));
        Object selectedMcpAction = enrichedContext.get("selected_mcp_action");
        
        if (mcpActive && selectedMcpAction instanceof Map) {
            Map<String, Object> mcpAction = (Map<String, Object>) selectedMcpAction;
            String selectedMcp = (String) mcpAction.get("selectedMcp");  // ✅ Usar selectedMcp
            String buttonId = (String) mcpAction.get("buttonId");
            
            // ✅ DETECTAR CONFLICTOS ESPECÍFICOS
            if (isGenericSystemCommand(userMessage) && !isCompatibleWithMcp(userMessage, selectedMcp)) {
                resolution.setHasConflict(true);
                resolution.setConflictType("mcp_system_conflict");
                resolution.setClarificationMessage(
                    String.format("Tienes seleccionado el botón %s (%s), pero tu comando '%s' parece ser general. " +
                                 "¿Quieres ejecutar '%s' en el contexto de %s o es un comando independiente?",
                                 buttonId != null ? buttonId : "MCP", selectedMcp, userMessage, userMessage, selectedMcp)
                );
                
                logger.info("Conflict detected: MCP '{}' vs command '{}'", selectedMcp, userMessage);
                logger.debug("DEBUG - Conflict details: buttonId={}, mcpAction={}, enrichedContext={}", 
                           buttonId, selectedMcpAction, enrichedContext.keySet());
            }
        }
        
        return resolution;
    }

    /**
     * Verifica si un mensaje es un comando genérico del sistema
     */
    private boolean isGenericSystemCommand(String userMessage) {
        String lowerMessage = userMessage.toLowerCase().trim();
        String[] genericCommands = {"apagar", "reiniciar", "parar", "detener", "cerrar", "salir", "terminar"};
        
        for (String cmd : genericCommands) {
            if (lowerMessage.contains(cmd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si un comando es compatible con el MCP seleccionado
     */
    private boolean isCompatibleWithMcp(String userMessage, String mcpActionType) {
        String lowerMessage = userMessage.toLowerCase().trim();
        
        switch (mcpActionType) {
            case "docker_mcp":
                return lowerMessage.contains("contenedor") || lowerMessage.contains("container") ||
                       lowerMessage.contains("imagen") || lowerMessage.contains("docker") ||
                       lowerMessage.contains("mostrar") || lowerMessage.contains("listar");
            case "browser_mcp":
                return lowerMessage.contains("página") || lowerMessage.contains("web") ||
                       lowerMessage.contains("navegador") || lowerMessage.contains("buscar");
            case "google_maps_mcp":
                return lowerMessage.contains("mapa") || lowerMessage.contains("navegación") ||
                       lowerMessage.contains("dirección") || lowerMessage.contains("ubicación");
            case "home_mcp":
                return lowerMessage.contains("casa") || lowerMessage.contains("hogar") ||
                       lowerMessage.contains("domótica") || lowerMessage.contains("temperatura");
            // Agregar más compatibilidades según MCPs disponibles
            default:
                return false;
        }
    }

    /**
     * Crea una respuesta de conflicto que requiere aclaración del usuario
     */
    private ConversationResponse createConflictResponse(ConversationSession session, ConflictResolution conflict, ConversationTurn turn) {
        ConversationResponse response = new ConversationResponse();
        response.setSuccess(true);
        response.setSessionId(session.getSessionId());
        response.setSystemResponse(conflict.getClarificationMessage());
        response.setDetectedIntent("clarification_needed");
        response.setConfidenceScore(1.0); // Alta confianza en la necesidad de aclaración
        response.setSessionState(ConversationState.WAITING_CLARIFICATION.getCode());
        response.setTurnCount(session.getTurnCount());
        response.setProcessingTimeMs(turn.getProcessingTimeMs());
        
        // Actualizar estado de sesión para esperar aclaración
        session.setState(ConversationState.WAITING_CLARIFICATION);
        session.updateMetadata("conflict_type", conflict.getConflictType());
        session.updateMetadata("awaiting_clarification", true);
        
        // ✅ GUARDAR CONTEXTO ORIGINAL DEL CONFLICTO PARA RECUPERAR DESPUÉS
        session.updateMetadata("conflict_original_message", turn.getUserMessage());
        if (session.getMetadata("mcpContext") != null) {
            session.updateMetadata("last_mcp_context", session.getMetadata("mcpContext"));
        }
        
        // Guardar el turno en el historial
        session.addTurn(turn);
        persistSession(session);
        
        logger.info("Conflict response created for session {}: {}", session.getSessionId(), conflict.getConflictType());
        
        return response;
    }

    /**
     * ✅ PROCESA RESPUESTAS DE CLARIFICACIÓN DE CONFLICTOS MCP/TARGET
     * Maneja cuando el usuario responde a una solicitud de clarificación de conflicto
     */
    private ConversationResponse processClarificationResponse(ConversationSession session, 
                                                            Map<String, Object> metadata, 
                                                            String userResponse, 
                                                            ConversationTurn turn) {
        
        Map<String, Object> clarificationData = (Map<String, Object>) metadata.get("clarificationContext");
        String selectedOption = (String) clarificationData.get("selectedOption");
        
        logger.info("Processing clarification response for session {}: option={}", 
                   session.getSessionId(), selectedOption);
        logger.debug("DEBUG - Clarification context full details: conflictId={}, isResponse={}", 
                   clarificationData.get("conflictId"), clarificationData.get("isResponse"));
        
        // Restaurar estado normal de la sesión
        session.setState(ConversationState.PROCESSING);
        session.removeMetadata("awaiting_clarification");
        session.removeMetadata("conflict_type");
        
        // Construir contexto basado en la selección del usuario
        Map<String, Object> resolvedContext = new HashMap<>();
        
        if ("mcp".equals(selectedOption)) {
            // Usuario eligió usar el contexto MCP
            Object mcpContext = session.getMetadata("last_mcp_context");
            if (mcpContext != null) {
                resolvedContext.put("mcpContext", mcpContext);
                resolvedContext.put("mcp_context_active", true);
                logger.info("Resolved conflict: Using MCP context for session {}", session.getSessionId());
                logger.debug("DEBUG - MCP context restored: {}", mcpContext);
            }
        } else if ("voice".equals(selectedOption)) {
            // Usuario eligió comando independiente (sin MCP)
            resolvedContext.put("independent_command", true);
            logger.info("Resolved conflict: Using independent voice command for session {}", session.getSessionId());
            logger.debug("DEBUG - Independent command mode activated, no MCP context");
        }
        
        // Recuperar el mensaje original que causó el conflicto
        String originalMessage = (String) session.getMetadata("conflict_original_message");
        if (originalMessage != null) {
            turn.setUserMessage(originalMessage);
        }
        
        // Construir contexto combinado
        Map<String, Object> conversationContext = buildConversationContext(session);
        conversationContext.putAll(resolvedContext);
        
        // Procesar con el sistema de votación usando el contexto resuelto
        VotingRound votingRound = llmVotingService.executeVotingRound(
            session.getSessionId(), 
            originalMessage != null ? originalMessage : userResponse, 
            conversationContext,
            buildConversationHistory(session)
        );
        
        // Generar respuesta
        ConversationResponse response = new ConversationResponse();
        response.setSuccess(true);
        response.setSessionId(session.getSessionId());
        
        if (votingRound.getConsensus() != null) {
            response.setSystemResponse(votingRound.getConsensus().getFinalResponse());
            response.setDetectedIntent(votingRound.getConsensus().getFinalIntent());
            response.setConfidenceScore(votingRound.getConsensus().getConsensusConfidence());
            
            // Actualizar turno con resultados
            turn.setDetectedIntent(votingRound.getConsensus().getFinalIntent());
            turn.setConfidenceScore(votingRound.getConsensus().getConsensusConfidence());
            turn.setSystemResponse(votingRound.getConsensus().getFinalResponse());
        } else {
            response.setSystemResponse("Clarificación procesada pero no se pudo generar respuesta definitiva.");
            response.setDetectedIntent("clarification_processed");
            response.setConfidenceScore(0.5);
        }
        
        response.setSessionState(ConversationState.PROCESSING.getCode());
        response.setTurnCount(session.getTurnCount() + 1);
        
        // Limpiar metadata temporal del conflicto
        session.removeMetadata("conflict_original_message");
        session.removeMetadata("last_mcp_context");
        
        // Añadir turno y persistir
        session.addTurn(turn);
        persistSession(session);
        
        logger.info("Clarification processed successfully for session {}: final_intent={}", 
                   session.getSessionId(), response.getDetectedIntent());
        
        return response;
    }

    /**
     * Clase para manejo de resolución de conflictos
     */
    private static class ConflictResolution {
        private boolean hasConflict;
        private String conflictType;
        private String clarificationMessage;
        
        public boolean hasConflict() { return hasConflict; }
        public void setHasConflict(boolean hasConflict) { this.hasConflict = hasConflict; }
        
        public String getConflictType() { return conflictType; }
        public void setConflictType(String conflictType) { this.conflictType = conflictType; }
        
        public String getClarificationMessage() { return clarificationMessage; }
        public void setClarificationMessage(String message) { this.clarificationMessage = message; }
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