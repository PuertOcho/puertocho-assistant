package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal de llenado automático de slots.
 * Orquesta la extracción, validación y generación de preguntas dinámicas para completar slots.
 */
@Service
public class SlotFillingService {

    private static final Logger logger = LoggerFactory.getLogger(SlotFillingService.class);

    @Autowired
    private SlotExtractor slotExtractor;

    @Autowired
    private SlotValidator slotValidator;

    @Autowired
    private DynamicQuestionGenerator dynamicQuestionGenerator;

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
     * Procesa el llenado de slots para una intención específica
     */
    public SlotFillingResult processSlotFilling(SlotFillingRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Procesando slot-filling para intent '{}': {}", request.getIntentId(), request.getUserMessage());

        SlotFillingResult result = new SlotFillingResult(true);

        try {
            // Obtener configuración de la intención
            IntentExample intentExample = intentConfigManager.getIntent(request.getIntentId());
            if (intentExample == null) {
                return createErrorResult("Intención no encontrada: " + request.getIntentId());
            }

            // Preparar listas de entidades requeridas y opcionales
            List<String> requiredEntities = intentExample.getRequiredEntities() != null ? 
                intentExample.getRequiredEntities() : new ArrayList<>();
            List<String> optionalEntities = intentExample.getOptionalEntities() != null ? 
                intentExample.getOptionalEntities() : new ArrayList<>();

            // Combinar todas las entidades objetivo
            List<String> allTargetSlots = new ArrayList<>();
            allTargetSlots.addAll(requiredEntities);
            allTargetSlots.addAll(optionalEntities);

            // Extraer slots desde el mensaje del usuario
            SlotExtractor.ExtractionResult extractionResult = slotExtractor.extractSlots(
                request.getIntentId(),
                request.getUserMessage(),
                allTargetSlots,
                request.getConversationContext()
            );

            // Combinar slots actuales con slots extraídos
            Map<String, Object> combinedSlots = new HashMap<>();
            if (request.getCurrentSlots() != null) {
                combinedSlots.putAll(request.getCurrentSlots());
            }
            if (extractionResult.getExtractedSlots() != null) {
                combinedSlots.putAll(extractionResult.getExtractedSlots());
            }

            // Validar todos los slots
            SlotValidator.ValidationResult validationResult = slotValidator.validateSlots(
                request.getIntentId(),
                combinedSlots,
                requiredEntities,
                null // TODO: Implementar reglas de validación específicas
            );

            // Usar solo slots válidos
            Map<String, Object> validSlots = new HashMap<>();
            if (validationResult.isValid()) {
                validSlots.putAll(validationResult.getNormalizedSlots());
            } else {
                // Solo incluir slots que pasaron validación individual
                for (Map.Entry<String, Boolean> entry : validationResult.getValidatedSlots().entrySet()) {
                    if (entry.getValue() && combinedSlots.containsKey(entry.getKey())) {
                        Object normalizedValue = validationResult.getNormalizedSlots().get(entry.getKey());
                        validSlots.put(entry.getKey(), normalizedValue != null ? normalizedValue : combinedSlots.get(entry.getKey()));
                    }
                }
            }

            // Determinar slots faltantes
            List<String> missingSlots = requiredEntities.stream()
                .filter(slot -> !validSlots.containsKey(slot) || validSlots.get(slot) == null)
                .collect(Collectors.toList());

            // Configurar resultado
            result.setFilledSlots(validSlots);
            result.setMissingSlots(missingSlots);
            result.setSlotsCompleted(missingSlots.isEmpty());

            // Extraer confidence scores
            Map<String, Double> confidenceScores = new HashMap<>();
            if (extractionResult.getConfidenceScores() != null) {
                confidenceScores.putAll(extractionResult.getConfidenceScores());
            }
            if (validationResult.getConfidenceScores() != null) {
                for (Map.Entry<String, Double> entry : validationResult.getConfidenceScores().entrySet()) {
                    String slotName = entry.getKey();
                    if (validSlots.containsKey(slotName)) {
                        // Promedio de confianza de extracción y validación
                        double extractionConf = confidenceScores.getOrDefault(slotName, 0.8);
                        double validationConf = entry.getValue();
                        confidenceScores.put(slotName, (extractionConf + validationConf) / 2.0);
                    }
                }
            }
            result.setSlotExtractionConfidence(confidenceScores);

            // Calcular confianza general
            double overallConfidence = calculateOverallConfidence(confidenceScores, missingSlots.size(), requiredEntities.size());
            result.setOverallConfidence(overallConfidence);

            // Generar pregunta si faltan slots
            if (!missingSlots.isEmpty()) {
                String nextSlot = missingSlots.get(0); // Tomar el primer slot faltante
                result.setNextSlotToFill(nextSlot);

                if (enableDynamicQuestions) {
                    String generatedQuestion = dynamicQuestionGenerator.generateQuestionForSlot(
                        request.getIntentId(),
                        nextSlot,
                        validSlots,
                        request.getConversationContext(),
                        request.getUserMessage()
                    );
                    result.setGeneratedQuestion(generatedQuestion);
                } else {
                    result.setGeneratedQuestion(getStaticQuestionForSlot(intentExample, nextSlot));
                }
            }

            // Configurar errores de validación si existen
            if (!validationResult.isValid() && validationResult.getValidationErrors() != null) {
                result.setValidationErrors(validationResult.getValidationErrors());
            }

            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Slot-filling completado para intent '{}' en {}ms. Slots completados: {}, Faltan: {}", 
                       request.getIntentId(), result.getProcessingTimeMs(), result.isSlotsCompleted(), missingSlots.size());

            return result;

        } catch (Exception e) {
            logger.error("Error en slot-filling para intent '{}': {}", request.getIntentId(), e.getMessage(), e);
            return createErrorResult("Error en procesamiento de slots: " + e.getMessage());
        }
    }

    /**
     * Extrae un slot específico desde una respuesta del usuario
     */
    public SlotFillingResult extractSpecificSlot(String intentId, String slotName, String userMessage, 
                                               Map<String, Object> currentSlots,
                                               Map<String, Object> conversationContext) {
        
        SlotFillingRequest request = new SlotFillingRequest(intentId, userMessage, null);
        request.setCurrentSlots(currentSlots);
        request.setConversationContext(conversationContext);
        request.setRequiredEntities(Arrays.asList(slotName));

        return processSlotFilling(request);
    }

    /**
     * Verifica si todos los slots requeridos están completos para una intención
     */
    public boolean areRequiredSlotsComplete(String intentId, Map<String, Object> slots) {
        try {
            IntentExample intentExample = intentConfigManager.getIntent(intentId);
            if (intentExample == null || intentExample.getRequiredEntities() == null) {
                return true; // No hay slots requeridos
            }

            for (String requiredSlot : intentExample.getRequiredEntities()) {
                if (!slots.containsKey(requiredSlot) || slots.get(requiredSlot) == null) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("Error verificando slots completos para intent '{}': {}", intentId, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la siguiente pregunta a hacer basándose en slots faltantes
     */
    public String getNextQuestion(String intentId, Map<String, Object> currentSlots, 
                                 Map<String, Object> conversationContext,
                                 String lastUserMessage) {
        
        try {
            IntentExample intentExample = intentConfigManager.getIntent(intentId);
            if (intentExample == null || intentExample.getRequiredEntities() == null) {
                return null;
            }

            // Encontrar el primer slot faltante
            for (String requiredSlot : intentExample.getRequiredEntities()) {
                if (!currentSlots.containsKey(requiredSlot) || currentSlots.get(requiredSlot) == null) {
                    
                    return dynamicQuestionGenerator.generateQuestionForSlot(
                        intentId, requiredSlot, currentSlots, conversationContext, lastUserMessage
                    );
                }
            }

            return null; // Todos los slots están completos

        } catch (Exception e) {
            logger.error("Error generando siguiente pregunta para intent '{}': {}", intentId, e.getMessage());
            return "¿Podrías proporcionar más información?";
        }
    }

    // Métodos privados

    private double calculateOverallConfidence(Map<String, Double> confidenceScores, int missingSlots, int totalRequired) {
        if (confidenceScores.isEmpty()) {
            return 0.0;
        }

        double avgConfidence = confidenceScores.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        // Penalizar por slots faltantes
        double completionRatio = totalRequired > 0 ? (double)(totalRequired - missingSlots) / totalRequired : 1.0;
        
        return avgConfidence * completionRatio;
    }

    private String getStaticQuestionForSlot(IntentExample intentExample, String slotName) {
        if (intentExample.getSlotFillingQuestions() != null && 
            intentExample.getSlotFillingQuestions().containsKey(slotName)) {
            return intentExample.getSlotFillingQuestions().get(slotName);
        }

        // Fallback por defecto
        return generateDefaultQuestion(slotName);
    }

    private String generateDefaultQuestion(String slotName) {
        switch (slotName.toLowerCase()) {
            case "ubicacion":
            case "lugar":
                return "¿En qué lugar?";
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

    private SlotFillingResult createErrorResult(String errorMessage) {
        SlotFillingResult result = new SlotFillingResult(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * Obtiene estadísticas del servicio de slot-filling
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enable_dynamic_questions", enableDynamicQuestions);
        stats.put("max_slot_attempts", maxSlotAttempts);
        stats.put("confidence_threshold", confidenceThreshold);
        stats.put("enable_context_aware_questions", enableContextAwareQuestions);
        stats.put("service_status", "active");
        
        // Agregar estadísticas de los sub-servicios
        stats.put("slot_extractor", slotExtractor.getStatistics());
        stats.put("slot_validator", slotValidator.getStatistics());
        stats.put("question_generator", dynamicQuestionGenerator.getStatistics());
        
        return stats;
    }

    /**
     * Verifica la salud del servicio
     */
    public boolean isHealthy() {
        try {
            return slotExtractor != null && 
                   slotValidator != null && 
                   dynamicQuestionGenerator != null && 
                   intentConfigManager != null;
        } catch (Exception e) {
            logger.error("Error en health check de SlotFillingService: {}", e.getMessage());
            return false;
        }
    }
}