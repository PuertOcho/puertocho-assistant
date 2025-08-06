package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.LlmResponse;
import com.intentmanagerms.domain.model.IntentExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio para generar preguntas dinámicas contextuales usando LLM.
 * Analiza el contexto conversacional y genera preguntas específicas para obtener información faltante.
 */
@Service
public class DynamicQuestionGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DynamicQuestionGenerator.class);

    @Autowired
    private IntentConfigManager intentConfigManager;

    @Value("${slot-filling.enable-dynamic-questions:true}")
    private boolean enableDynamicQuestions;

    @Value("${slot-filling.max-attempts:3}")
    private int maxSlotAttempts;

    @Value("${slot-filling.confidence-threshold:0.7}")
    private double confidenceThreshold;

    @Value("${slot-filling.enable-context-aware-questions:true}")
    private boolean enableContextAwareQuestions;



    /**
     * Genera una pregunta dinámica para obtener un slot específico
     */
    public String generateQuestionForSlot(String intentId, String slotName, 
                                        Map<String, Object> currentSlots,
                                        Map<String, Object> conversationContext,
                                        String userMessage) {
        
        long startTime = System.currentTimeMillis();
        logger.info("Generando pregunta dinámica para slot '{}' en intent '{}'", slotName, intentId);

        try {
            if (!enableDynamicQuestions) {
                return getStaticQuestionForSlot(intentId, slotName);
            }

            // Construir prompt contextual
            String prompt = buildContextualPrompt(intentId, slotName, currentSlots, conversationContext, userMessage);
            
            // Llamar al LLM para generar la pregunta
            LlmResponse llmResponse = callLlmForQuestionGeneration(prompt);
            
            String generatedQuestion = extractQuestionFromLlmResponse(llmResponse);
            
            if (generatedQuestion == null || generatedQuestion.trim().isEmpty()) {
                logger.warn("LLM no generó pregunta válida, usando fallback estático para slot '{}'", slotName);
                return getStaticQuestionForSlot(intentId, slotName);
            }

            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Pregunta generada para slot '{}' en {}ms: {}", slotName, processingTime, generatedQuestion);
            
            return generatedQuestion;

        } catch (Exception e) {
            logger.error("Error generando pregunta dinámica para slot '{}': {}", slotName, e.getMessage(), e);
            return getStaticQuestionForSlot(intentId, slotName);
        }
    }

    /**
     * Genera múltiples preguntas para slots faltantes
     */
    public Map<String, String> generateQuestionsForMissingSlots(String intentId, 
                                                              List<String> missingSlots,
                                                              Map<String, Object> currentSlots,
                                                              Map<String, Object> conversationContext,
                                                              String userMessage) {
        
        Map<String, String> questions = new HashMap<>();
        
        for (String slotName : missingSlots) {
            String question = generateQuestionForSlot(intentId, slotName, currentSlots, conversationContext, userMessage);
            questions.put(slotName, question);
        }
        
        return questions;
    }

    /**
     * Determina cuál es la mejor pregunta a hacer basándose en prioridad y contexto
     */
    public String selectBestQuestionToAsk(String intentId, 
                                        List<String> missingSlots,
                                        Map<String, Object> currentSlots,
                                        Map<String, Object> conversationContext,
                                        String userMessage) {
        
        if (missingSlots == null || missingSlots.isEmpty()) {
            return null;
        }

        // Determinar prioridad de slots
        String prioritySlot = determinePrioritySlot(intentId, missingSlots, conversationContext);
        
        return generateQuestionForSlot(intentId, prioritySlot, currentSlots, conversationContext, userMessage);
    }

    /**
     * Genera pregunta de clarificación cuando hay ambigüedad
     */
    public String generateClarificationQuestion(String intentId,
                                              String slotName,
                                              List<String> ambiguousValues,
                                              Map<String, Object> conversationContext) {
        
        logger.info("Generando pregunta de clarificación para slot '{}' con valores ambiguos: {}", slotName, ambiguousValues);

        try {
            String prompt = buildClarificationPrompt(intentId, slotName, ambiguousValues, conversationContext);
            LlmResponse llmResponse = callLlmForQuestionGeneration(prompt);
            String clarificationQuestion = extractQuestionFromLlmResponse(llmResponse);
            
            if (clarificationQuestion == null || clarificationQuestion.trim().isEmpty()) {
                return buildDefaultClarificationQuestion(slotName, ambiguousValues);
            }
            
            return clarificationQuestion;

        } catch (Exception e) {
            logger.error("Error generando pregunta de clarificación: {}", e.getMessage(), e);
            return buildDefaultClarificationQuestion(slotName, ambiguousValues);
        }
    }

    // Métodos privados

    private String buildContextualPrompt(String intentId, String slotName, 
                                       Map<String, Object> currentSlots,
                                       Map<String, Object> conversationContext,
                                       String userMessage) {
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Eres un asistente virtual inteligente. Necesitas generar una pregunta natural y contextual ");
        prompt.append("para obtener información específica de un usuario.\n\n");
        
        prompt.append("CONTEXTO:\n");
        prompt.append("- Intención actual: ").append(intentId).append("\n");
        prompt.append("- Slot a obtener: ").append(slotName).append("\n");
        prompt.append("- Mensaje del usuario: \"").append(userMessage).append("\"\n");
        
        if (currentSlots != null && !currentSlots.isEmpty()) {
            prompt.append("- Información ya obtenida: ").append(currentSlots).append("\n");
        }
        
        if (enableContextAwareQuestions && conversationContext != null) {
            prompt.append("- Contexto conversacional: ").append(conversationContext).append("\n");
        }
        
        prompt.append("\nREQUISITOS:\n");
        prompt.append("1. Genera UNA pregunta concisa y natural en español\n");
        prompt.append("2. La pregunta debe ser específica para obtener el slot '").append(slotName).append("'\n");
        prompt.append("3. Debe ser amigable y contextual\n");
        prompt.append("4. No repitas información que ya tienes\n");
        prompt.append("5. Responde SOLO con la pregunta, sin explicaciones adicionales\n\n");
        
        prompt.append("PREGUNTA:");
        
        return prompt.toString();
    }

    private String buildClarificationPrompt(String intentId, String slotName, 
                                          List<String> ambiguousValues,
                                          Map<String, Object> conversationContext) {
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Genera una pregunta de clarificación amigable para que el usuario elija entre estas opciones:\n\n");
        prompt.append("Slot: ").append(slotName).append("\n");
        prompt.append("Opciones: ").append(String.join(", ", ambiguousValues)).append("\n");
        prompt.append("Intención: ").append(intentId).append("\n\n");
        
        prompt.append("Genera una pregunta natural que le permita al usuario elegir una opción.\n");
        prompt.append("Responde SOLO con la pregunta:");
        
        return prompt.toString();
    }

    private LlmResponse callLlmForQuestionGeneration(String prompt) {
        try {
            // TODO: Implementar llamada real al LLM
            // Por ahora simulamos una respuesta para testing
            return simulateLlmResponseForQuestion(prompt);
        } catch (Exception e) {
            logger.error("Error llamando LLM para generar pregunta: {}", e.getMessage(), e);
            throw new RuntimeException("Error en generación de pregunta con LLM", e);
        }
    }

    /**
     * Simula respuesta del LLM para generación de preguntas
     */
    private LlmResponse simulateLlmResponseForQuestion(String prompt) {
        LlmResponse response = new LlmResponse();
        response.setLlmId("primary");
        response.setModel("simulated-llm");
        response.setSuccess(true);
        
        // Generar pregunta basada en el contexto del prompt
        String question = generateSimulatedQuestion(prompt);
        response.setContent(question);
        response.setResponseTimeMs(50L);
        
        return response;
    }

    /**
     * Genera pregunta simulada basada en el prompt
     */
    private String generateSimulatedQuestion(String prompt) {
        if (prompt.contains("ubicacion") || prompt.contains("lugar")) {
            return "¿En qué ubicación necesitas esta información?";
        } else if (prompt.contains("fecha")) {
            return "¿Para qué fecha necesitas esto?";
        } else if (prompt.contains("hora")) {
            return "¿A qué hora prefieres que sea?";
        } else if (prompt.contains("temperatura")) {
            return "¿Qué temperatura te gustaría?";
        } else if (prompt.contains("nombre")) {
            return "¿Cuál es el nombre que necesitas especificar?";
        } else {
            return "¿Podrías proporcionar más información?";
        }
    }

    private String extractQuestionFromLlmResponse(LlmResponse llmResponse) {
        if (llmResponse == null || llmResponse.getContent() == null) {
            return null;
        }
        
        String response = llmResponse.getContent().trim();
        
        // Limpiar respuesta si viene con formato adicional
        if (response.toLowerCase().startsWith("pregunta:")) {
            response = response.substring(9).trim();
        }
        
        // Asegurar que termine con signo de interrogación
        if (!response.endsWith("?")) {
            response += "?";
        }
        
        return response;
    }

    private String getStaticQuestionForSlot(String intentId, String slotName) {
        try {
            IntentExample intentExample = intentConfigManager.getIntent(intentId);
            
            if (intentExample != null) {
                Map<String, String> slotQuestions = intentExample.getSlotFillingQuestions();
                
                if (slotQuestions != null && slotQuestions.containsKey(slotName)) {
                    return slotQuestions.get(slotName);
                }
            }
            
            // Fallback por defecto
            return generateDefaultQuestion(slotName);
            
        } catch (Exception e) {
            logger.error("Error obteniendo pregunta estática para slot '{}': {}", slotName, e.getMessage());
            return generateDefaultQuestion(slotName);
        }
    }

    private String generateDefaultQuestion(String slotName) {
        switch (slotName.toLowerCase()) {
            case "ubicacion":
            case "lugar":
                return "¿En qué lugar o ubicación?";
            case "fecha":
                return "¿Para qué fecha?";
            case "hora":
                return "¿A qué hora?";
            case "cantidad":
                return "¿Qué cantidad?";
            case "nombre":
                return "¿Cuál es el nombre?";
            default:
                return "¿Podrías especificar " + slotName + "?";
        }
    }

    private String determinePrioritySlot(String intentId, List<String> missingSlots, Map<String, Object> conversationContext) {
        // Por ahora, simple FIFO, pero se puede implementar lógica más sofisticada
        if (missingSlots == null || missingSlots.isEmpty()) {
            return null;
        }
        
        // Priorizar ciertos slots comunes
        List<String> priorityOrder = Arrays.asList("ubicacion", "lugar", "fecha", "hora", "nombre");
        
        for (String prioritySlot : priorityOrder) {
            if (missingSlots.contains(prioritySlot)) {
                return prioritySlot;
            }
        }
        
        // Si no hay slots prioritarios, tomar el primero
        return missingSlots.get(0);
    }

    private String buildDefaultClarificationQuestion(String slotName, List<String> ambiguousValues) {
        return String.format("Encontré varias opciones para %s: %s. ¿Cuál prefieres?", 
                           slotName, String.join(", ", ambiguousValues));
    }

    /**
     * Obtiene estadísticas del generador de preguntas
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enable_dynamic_questions", enableDynamicQuestions);
        stats.put("max_slot_attempts", maxSlotAttempts);
        stats.put("confidence_threshold", confidenceThreshold);
        stats.put("enable_context_aware_questions", enableContextAwareQuestions);
        stats.put("service_status", "active");
        return stats;
    }
}