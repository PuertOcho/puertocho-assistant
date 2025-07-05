package com.intentmanagerms.application.services;

import com.intentmanagerms.application.services.dto.NluMessage;
import com.intentmanagerms.domain.model.ConversationState;
import com.intentmanagerms.domain.model.ConversationStatus;
import com.intentmanagerms.domain.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Gestor de diálogos para conversaciones multivuelta.
 * Maneja el slot-filling y la continuidad de conversaciones.
 */
@Service
public class DialogManager {

    private static final Logger logger = LoggerFactory.getLogger(DialogManager.class);

    private final ConversationRepository conversationRepository;
    private final NluService nluService;

    // Definición de entidades requeridas por intención
    private static final Map<String, Set<String>> INTENT_REQUIRED_ENTITIES = Map.of(
        "encender_luz", Set.of("lugar"),
        "apagar_luz", Set.of("lugar"),
        "reproducir_musica", Set.of(), // Sin entidades obligatorias
        "consultar_tiempo", Set.of("ubicacion"),
        "accion_compleja_taiga", Set.of("proyecto")
    );

    // Plantillas de preguntas para entidades faltantes
    private static final Map<String, String> ENTITY_QUESTIONS = Map.of(
        "lugar", "¿En qué habitación quieres que %s?",
        "ubicacion", "¿De qué ciudad quieres consultar el tiempo?",
        "proyecto", "¿Sobre qué proyecto quieres que %s?",
        "artista", "¿Qué artista quieres escuchar?",
        "genero", "¿Qué género musical prefieres?"
    );

    public DialogManager(ConversationRepository conversationRepository, NluService nluService) {
        this.conversationRepository = conversationRepository;
        this.nluService = nluService;
    }

    /**
     * Procesa un mensaje del usuario y gestiona el estado de conversación.
     * 
     * @param userMessage Mensaje del usuario
     * @param sessionId ID de sesión (opcional, se genera si no existe)
     * @return Resultado del procesamiento
     */
    public DialogResult processMessage(String userMessage, String sessionId) {
        try {
            logger.info("Procesando mensaje: '{}' para sesión: {}", userMessage, sessionId);

            // Generar sessionId si no existe
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString();
                logger.info("Generado nuevo sessionId: {}", sessionId);
            }

            // Obtener o crear estado de conversación
            ConversationState conversation = getOrCreateConversation(sessionId);

            // Verificar comandos especiales
            if (isResetCommand(userMessage)) {
                return handleResetCommand(conversation);
            }

            if (isCancelCommand(userMessage)) {
                return handleCancelCommand(conversation);
            }

            // Analizar mensaje con NLU
            NluMessage nluResult = nluService.analyzeText(userMessage);
            String detectedIntent = nluResult.getIntent().getName();
            double confidence = nluResult.getIntent().getConfidenceAsDouble();

            logger.debug("Intención detectada: '{}' con confianza: {}", detectedIntent, confidence);

            // Actualizar estado de conversación
            conversation.setLastMessage(userMessage);

            // Si hay una conversación activa, intentar completar entidades
            if (conversation.getCurrentIntent() != null) {
                return continueExistingConversation(conversation, nluResult);
            } else {
                return startNewConversation(conversation, nluResult, confidence);
            }

        } catch (Exception e) {
            logger.error("Error procesando mensaje: {}", e.getMessage(), e);
            return DialogResult.error("Error procesando tu mensaje. Por favor, inténtalo de nuevo.");
        }
    }

    /**
     * Obtiene una conversación existente o crea una nueva.
     */
    private ConversationState getOrCreateConversation(String sessionId) {
        Optional<ConversationState> existing = conversationRepository.findBySessionId(sessionId);
        
        if (existing.isPresent()) {
            logger.debug("Conversación existente encontrada para sesión: {}", sessionId);
            return existing.get();
        }

        logger.debug("Creando nueva conversación para sesión: {}", sessionId);
        ConversationState newConversation = new ConversationState(sessionId);
        return conversationRepository.save(newConversation);
    }

    /**
     * Inicia una nueva conversación con una intención detectada.
     */
    private DialogResult startNewConversation(ConversationState conversation, NluMessage nluResult, double confidence) {
        String intent = nluResult.getIntent().getName();
        
        // Verificar confianza mínima
        if (confidence < 0.25) {
            logger.info("Confianza insuficiente ({}) para intención '{}'", confidence, intent);
            return DialogResult.clarification("No estoy seguro de qué quieres hacer. ¿Podrías explicármelo de otra manera?");
        }

        // Configurar nueva conversación
        conversation.setCurrentIntent(intent);
        Set<String> requiredEntities = INTENT_REQUIRED_ENTITIES.getOrDefault(intent, Set.of());
        conversation.setRequiredEntities(requiredEntities);

        // Extraer entidades del mensaje actual
        extractAndStoreEntities(conversation, nluResult);

        // Verificar si la acción está completa
        if (conversation.isComplete()) {
            return completeAction(conversation);
        } else {
            return requestMissingEntities(conversation);
        }
    }

    /**
     * Continúa una conversación existente intentando completar entidades.
     */
    private DialogResult continueExistingConversation(ConversationState conversation, NluMessage nluResult) {
        logger.debug("Continuando conversación existente para intención: {}", conversation.getCurrentIntent());

        // Extraer entidades del nuevo mensaje
        extractAndStoreEntities(conversation, nluResult);

        // Verificar si ahora está completa
        if (conversation.isComplete()) {
            return completeAction(conversation);
        } else {
            return requestMissingEntities(conversation);
        }
    }

    /**
     * Extrae entidades del resultado NLU y las almacena en la conversación.
     */
    private void extractAndStoreEntities(ConversationState conversation, NluMessage nluResult) {
        nluResult.getEntities().forEach(entity -> {
            String entityName = entity.getEntity();
            String entityValue = entity.getValue();
            
            if (conversation.getRequiredEntities().contains(entityName)) {
                conversation.addEntity(entityName, entityValue);
                logger.debug("Entidad extraída: {} = {}", entityName, entityValue);
            }
        });

        conversationRepository.save(conversation);
    }

    /**
     * Solicita las entidades faltantes al usuario.
     */
    private DialogResult requestMissingEntities(ConversationState conversation) {
        Set<String> missingEntities = conversation.getMissingEntities();
        
        if (missingEntities.isEmpty()) {
            return completeAction(conversation);
        }

        // Tomar la primera entidad faltante
        String missingEntity = missingEntities.iterator().next();
        String question = generateQuestionForEntity(missingEntity, conversation.getCurrentIntent());
        
        conversation.setLastResponse(question);
        conversationRepository.save(conversation);

        logger.debug("Solicitando entidad faltante: {} para intención: {}", missingEntity, conversation.getCurrentIntent());
        
        return DialogResult.followUp(question, conversation.getSessionId());
    }

    /**
     * Completa la acción cuando todas las entidades están disponibles.
     */
    private DialogResult completeAction(ConversationState conversation) {
        logger.info("Acción completa para intención: {} con entidades: {}", 
                   conversation.getCurrentIntent(), conversation.getCollectedEntities());

        // Marcar conversación como completada
        conversation.setStatus(ConversationStatus.COMPLETED);
        conversationRepository.save(conversation);

        // Devolver resultado para ejecutar la acción
        return DialogResult.actionReady(
            conversation.getCurrentIntent(),
            conversation.getCollectedEntities(),
            conversation.getSessionId()
        );
    }

    /**
     * Genera una pregunta apropiada para una entidad faltante.
     */
    private String generateQuestionForEntity(String entityName, String intent) {
        String template = ENTITY_QUESTIONS.get(entityName);
        
        if (template == null) {
            return "¿Podrías especificar " + entityName + "?";
        }

        // Personalizar la pregunta según la intención
        String action = getActionDescription(intent);
        return String.format(template, action);
    }

    /**
     * Obtiene una descripción de la acción para usar en preguntas.
     */
    private String getActionDescription(String intent) {
        return switch (intent) {
            case "encender_luz" -> "encienda la luz";
            case "apagar_luz" -> "apague la luz";
            case "reproducir_musica" -> "reproduzca música";
            case "consultar_tiempo" -> "consulte el tiempo";
            case "accion_compleja_taiga" -> "ejecute la acción";
            default -> "realice la acción";
        };
    }

    /**
     * Maneja comando de reset.
     */
    private DialogResult handleResetCommand(ConversationState conversation) {
        conversation.reset();
        conversationRepository.save(conversation);
        logger.info("Conversación reiniciada para sesión: {}", conversation.getSessionId());
        return DialogResult.success("Conversación reiniciada. ¿En qué puedo ayudarte?");
    }

    /**
     * Maneja comando de cancelación.
     */
    private DialogResult handleCancelCommand(ConversationState conversation) {
        conversation.setStatus(ConversationStatus.CANCELLED);
        conversationRepository.save(conversation);
        logger.info("Conversación cancelada para sesión: {}", conversation.getSessionId());
        return DialogResult.success("Operación cancelada. ¿Hay algo más en lo que pueda ayudarte?");
    }

    /**
     * Verifica si el mensaje es un comando de reset.
     */
    private boolean isResetCommand(String message) {
        return message.toLowerCase().matches(".*(empezar de nuevo|reiniciar|reset|comenzar otra vez).*");
    }

    /**
     * Verifica si el mensaje es un comando de cancelación.
     */
    private boolean isCancelCommand(String message) {
        return message.toLowerCase().matches(".*(cancelar|cancel|olvida|déjalo|no importa).*");
    }
} 