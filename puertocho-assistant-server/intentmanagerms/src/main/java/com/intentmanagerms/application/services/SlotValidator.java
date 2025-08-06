package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Servicio para validar y normalizar slots extraídos.
 * Utiliza reglas de validación configurables y LLM para validaciones complejas.
 */
@Service
public class SlotValidator {

    private static final Logger logger = LoggerFactory.getLogger(SlotValidator.class);

    @Value("${slot-filling.enable-llm-validation:true}")
    private boolean enableLlmValidation;

    @Value("${slot-filling.validation-confidence-threshold:0.8}")
    private double validationConfidenceThreshold;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Patrones de validación comunes
    private final Map<String, Pattern> validationPatterns = new HashMap<>();

    public SlotValidator() {
        initializeValidationPatterns();
    }

    /**
     * Valida todos los slots extraídos
     */
    public ValidationResult validateSlots(String intentId, Map<String, Object> slots, 
                                        List<String> requiredSlots,
                                        Map<String, Object> validationRules) {
        
        long startTime = System.currentTimeMillis();
        logger.info("Validando slots para intent '{}': {}", intentId, slots);

        ValidationResult result = new ValidationResult();
        result.setIntentId(intentId);
        result.setValidatedSlots(new HashMap<>());
        result.setValidationErrors(new ArrayList<>());
        result.setNormalizedSlots(new HashMap<>());
        result.setConfidenceScores(new HashMap<>());

        if (slots == null || slots.isEmpty()) {
            result.setValid(true);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }

        try {
            boolean allValid = true;

            for (Map.Entry<String, Object> entry : slots.entrySet()) {
                String slotName = entry.getKey();
                Object slotValue = entry.getValue();

                SlotValidationResult slotResult = validateSingleSlot(intentId, slotName, slotValue, validationRules);
                
                result.getValidatedSlots().put(slotName, slotResult.isValid());
                result.getConfidenceScores().put(slotName, slotResult.getConfidence());
                
                if (slotResult.isValid()) {
                    result.getNormalizedSlots().put(slotName, slotResult.getNormalizedValue());
                } else {
                    allValid = false;
                    result.getValidationErrors().addAll(slotResult.getErrors());
                }
            }

            // Validar slots requeridos
            if (requiredSlots != null) {
                for (String requiredSlot : requiredSlots) {
                    if (!slots.containsKey(requiredSlot) || slots.get(requiredSlot) == null) {
                        allValid = false;
                        result.getValidationErrors().add("Slot requerido faltante: " + requiredSlot);
                    }
                }
            }

            result.setValid(allValid);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Validación completada para intent '{}' en {}ms. Válido: {}", 
                       intentId, result.getProcessingTimeMs(), allValid);

            return result;

        } catch (Exception e) {
            logger.error("Error validando slots para intent '{}': {}", intentId, e.getMessage(), e);
            result.setValid(false);
            result.getValidationErrors().add("Error interno de validación: " + e.getMessage());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * Valida un slot individual
     */
    public SlotValidationResult validateSingleSlot(String intentId, String slotName, Object slotValue,
                                                 Map<String, Object> validationRules) {
        
        logger.debug("Validando slot '{}' con valor '{}' para intent '{}'", slotName, slotValue, intentId);

        SlotValidationResult result = new SlotValidationResult();
        result.setSlotName(slotName);
        result.setOriginalValue(slotValue);
        result.setErrors(new ArrayList<>());

        try {
            // Validación básica
            if (slotValue == null) {
                result.setValid(false);
                result.getErrors().add("Valor nulo para slot " + slotName);
                return result;
            }

            String stringValue = slotValue.toString().trim();
            if (stringValue.isEmpty()) {
                result.setValid(false);
                result.getErrors().add("Valor vacío para slot " + slotName);
                return result;
            }

            // Validación por tipo de slot
            SlotValidationResult typeValidation = validateBySlotType(slotName, stringValue);
            if (!typeValidation.isValid()) {
                return typeValidation;
            }

            // Validación con reglas específicas
            if (validationRules != null && validationRules.containsKey(slotName)) {
                SlotValidationResult ruleValidation = validateWithRules(slotName, stringValue, validationRules.get(slotName));
                if (!ruleValidation.isValid()) {
                    return ruleValidation;
                }
            }

            // Validación con LLM si está habilitada
            if (enableLlmValidation) {
                SlotValidationResult llmValidation = validateWithLlm(intentId, slotName, stringValue);
                if (!llmValidation.isValid()) {
                    return llmValidation;
                }
                result.setConfidence(llmValidation.getConfidence());
            } else {
                result.setConfidence(0.9); // Confianza alta para validaciones básicas
            }

            // Normalizar valor
            Object normalizedValue = normalizeSlotValue(slotName, stringValue);
            result.setNormalizedValue(normalizedValue);
            result.setValid(true);

            return result;

        } catch (Exception e) {
            logger.error("Error validando slot '{}': {}", slotName, e.getMessage(), e);
            result.setValid(false);
            result.getErrors().add("Error de validación: " + e.getMessage());
            return result;
        }
    }

    /**
     * Normaliza un valor de slot a su formato estándar
     */
    public Object normalizeSlotValue(String slotName, String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        try {
            switch (slotName.toLowerCase()) {
                case "ubicacion":
                case "lugar":
                case "ciudad":
                    return normalizeLocation(value);
                    
                case "fecha":
                    return normalizeDate(value);
                    
                case "hora":
                    return normalizeTime(value);
                    
                case "temperatura":
                    return normalizeTemperature(value);
                    
                case "nombre":
                    return normalizeName(value);
                    
                default:
                    return value.trim();
            }
        } catch (Exception e) {
            logger.warn("Error normalizando slot '{}' con valor '{}': {}", slotName, value, e.getMessage());
            return value.trim();
        }
    }

    // Métodos privados

    private void initializeValidationPatterns() {
        // Patrones para diferentes tipos de datos
        validationPatterns.put("email", Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$"));
        validationPatterns.put("telefono", Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]{7,}$"));
        validationPatterns.put("fecha", Pattern.compile("^\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}$"));
        validationPatterns.put("hora", Pattern.compile("^\\d{1,2}[:]\\d{2}(\\s*(AM|PM|am|pm))?$"));
        validationPatterns.put("numero", Pattern.compile("^[0-9]+([.,][0-9]+)?$"));
    }

    private SlotValidationResult validateBySlotType(String slotName, String value) {
        SlotValidationResult result = new SlotValidationResult();
        result.setSlotName(slotName);
        result.setOriginalValue(value);
        result.setErrors(new ArrayList<>());

        switch (slotName.toLowerCase()) {
            case "email":
                if (!validationPatterns.get("email").matcher(value).matches()) {
                    result.setValid(false);
                    result.getErrors().add("Formato de email inválido");
                    return result;
                }
                break;
                
            case "telefono":
                if (!validationPatterns.get("telefono").matcher(value).matches()) {
                    result.setValid(false);
                    result.getErrors().add("Formato de teléfono inválido");
                    return result;
                }
                break;
                
            case "fecha":
                if (!isValidDate(value)) {
                    result.setValid(false);
                    result.getErrors().add("Formato de fecha inválido");
                    return result;
                }
                break;
                
            case "hora":
                if (!isValidTime(value)) {
                    result.setValid(false);
                    result.getErrors().add("Formato de hora inválido");
                    return result;
                }
                break;
        }

        result.setValid(true);
        result.setConfidence(0.9);
        return result;
    }

    private SlotValidationResult validateWithRules(String slotName, String value, Object rules) {
        SlotValidationResult result = new SlotValidationResult();
        result.setSlotName(slotName);
        result.setOriginalValue(value);
        result.setErrors(new ArrayList<>());
        result.setValid(true);
        result.setConfidence(0.8);

        // Implementar validaciones con reglas específicas cuando sea necesario
        // Por ahora, validación básica
        
        return result;
    }

    private SlotValidationResult validateWithLlm(String intentId, String slotName, String value) {
        SlotValidationResult result = new SlotValidationResult();
        result.setSlotName(slotName);
        result.setOriginalValue(value);
        result.setErrors(new ArrayList<>());

        try {
            String prompt = buildValidationPrompt(intentId, slotName, value);
            LlmResponse llmResponse = simulateLlmResponseForValidation(prompt);
            
            ValidationLlmResult llmResult = parseValidationLlmResponse(llmResponse);
            
            result.setValid(llmResult.isValid());
            result.setConfidence(llmResult.getConfidence());
            
            if (!llmResult.isValid()) {
                result.getErrors().add(llmResult.getReason());
            }

            return result;

        } catch (Exception e) {
            logger.error("Error en validación LLM para slot '{}': {}", slotName, e.getMessage());
            // Fallback a validación exitosa con baja confianza
            result.setValid(true);
            result.setConfidence(0.5);
            return result;
        }
    }

    private String buildValidationPrompt(String intentId, String slotName, String value) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Valida si el siguiente valor es apropiado para el contexto dado:\n\n");
        prompt.append("Intención: ").append(intentId).append("\n");
        prompt.append("Slot: ").append(slotName).append("\n");
        prompt.append("Valor: ").append(value).append("\n\n");
        
        prompt.append("Responde en formato JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"valid\": true/false,\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"reason\": \"explicación si no es válido\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private ValidationLlmResult parseValidationLlmResponse(LlmResponse llmResponse) {
        ValidationLlmResult result = new ValidationLlmResult();
        
        try {
            String response = llmResponse.getContent().trim();
            JsonNode jsonNode = objectMapper.readTree(response);
            
            result.setValid(jsonNode.get("valid").asBoolean());
            result.setConfidence(jsonNode.get("confidence").asDouble());
            
            if (jsonNode.has("reason")) {
                result.setReason(jsonNode.get("reason").asText());
            }
            
        } catch (Exception e) {
            logger.warn("Error parseando respuesta LLM de validación: {}", e.getMessage());
            result.setValid(true);
            result.setConfidence(0.5);
            result.setReason("Error en parseo de validación");
        }
        
        return result;
    }

    /**
     * Simula respuesta del LLM para validación
     */
    private LlmResponse simulateLlmResponseForValidation(String prompt) {
        LlmResponse response = new LlmResponse();
        response.setLlmId("primary");
        response.setModel("simulated-llm");
        response.setSuccess(true);
        response.setResponseTimeMs(30L);
        
        // Generar respuesta de validación simulada
        String validationResponse = generateSimulatedValidation(prompt);
        response.setContent(validationResponse);
        
        return response;
    }

    /**
     * Genera validación simulada basada en el prompt
     */
    private String generateSimulatedValidation(String prompt) {
        // Simulación simple: la mayoría de valores son válidos
        boolean isValid = !prompt.contains("@invalid@"); // Marcador especial para pruebas
        double confidence = isValid ? 0.85 : 0.2;
        String reason = isValid ? "Valor apropiado para el contexto" : "Valor no válido para este slot";
        
        return String.format(
            "{\n  \"valid\": %s,\n  \"confidence\": %.2f,\n  \"reason\": \"%s\"\n}",
            isValid, confidence, reason
        );
    }

    // Métodos de normalización específicos

    private String normalizeLocation(String location) {
        // Capitalizar primera letra de cada palabra
        String[] words = location.toLowerCase().trim().split("\\s+");
        StringBuilder normalized = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                normalized.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    normalized.append(word.substring(1));
                }
                normalized.append(" ");
            }
        }
        
        return normalized.toString().trim();
    }

    private String normalizeDate(String date) {
        // Implementar normalización de fechas
        return date.trim();
    }

    private String normalizeTime(String time) {
        // Implementar normalización de horas
        return time.trim();
    }

    private String normalizeTemperature(String temperature) {
        // Extraer número y normalizar unidades
        return temperature.trim();
    }

    private String normalizeName(String name) {
        return normalizeLocation(name); // Usar misma lógica de capitalización
    }

    private boolean isValidDate(String date) {
        return validationPatterns.get("fecha").matcher(date).matches();
    }

    private boolean isValidTime(String time) {
        return validationPatterns.get("hora").matcher(time).matches();
    }

    // Clases internas para resultados
    
    public static class ValidationResult {
        private String intentId;
        private boolean valid;
        private Map<String, Boolean> validatedSlots;
        private Map<String, Object> normalizedSlots;
        private Map<String, Double> confidenceScores;
        private List<String> validationErrors;
        private long processingTimeMs;

        // Getters y Setters
        public String getIntentId() { return intentId; }
        public void setIntentId(String intentId) { this.intentId = intentId; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public Map<String, Boolean> getValidatedSlots() { return validatedSlots; }
        public void setValidatedSlots(Map<String, Boolean> validatedSlots) { this.validatedSlots = validatedSlots; }
        
        public Map<String, Object> getNormalizedSlots() { return normalizedSlots; }
        public void setNormalizedSlots(Map<String, Object> normalizedSlots) { this.normalizedSlots = normalizedSlots; }
        
        public Map<String, Double> getConfidenceScores() { return confidenceScores; }
        public void setConfidenceScores(Map<String, Double> confidenceScores) { this.confidenceScores = confidenceScores; }
        
        public List<String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }

    public static class SlotValidationResult {
        private String slotName;
        private Object originalValue;
        private Object normalizedValue;
        private boolean valid;
        private double confidence;
        private List<String> errors;

        // Getters y Setters
        public String getSlotName() { return slotName; }
        public void setSlotName(String slotName) { this.slotName = slotName; }
        
        public Object getOriginalValue() { return originalValue; }
        public void setOriginalValue(Object originalValue) { this.originalValue = originalValue; }
        
        public Object getNormalizedValue() { return normalizedValue; }
        public void setNormalizedValue(Object normalizedValue) { this.normalizedValue = normalizedValue; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }

    private static class ValidationLlmResult {
        private boolean valid;
        private double confidence;
        private String reason;

        // Getters y Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * Obtiene estadísticas del validador
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enable_llm_validation", enableLlmValidation);
        stats.put("validation_confidence_threshold", validationConfidenceThreshold);
        stats.put("validation_patterns_count", validationPatterns.size());
        stats.put("service_status", "active");
        return stats;
    }
}