package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Servicio para validación de entidades extraídas.
 * Implementa validación por reglas, normalización y validación contextual.
 */
@Service
public class EntityValidator {

    private static final Logger logger = LoggerFactory.getLogger(EntityValidator.class);

    @Value("${entity.validation.enable-rule-validation:true}")
    private boolean enableRuleValidation;

    @Value("${entity.validation.enable-normalization:true}")
    private boolean enableNormalization;

    @Value("${entity.validation.enable-contextual-validation:true}")
    private boolean enableContextualValidation;

    @Value("${entity.validation.enable-llm-validation:true}")
    private boolean enableLlmValidation;

    @Value("${entity.validation.confidence-threshold:0.6}")
    private double confidenceThreshold;

    // Patrones de validación
    private final Map<String, Pattern> validationPatterns = new HashMap<>();
    private final Map<String, Set<String>> validValues = new HashMap<>();

    public EntityValidator() {
        initializeValidationPatterns();
        initializeValidValues();
    }

    /**
     * Inicializa los patrones de validación.
     */
    private void initializeValidationPatterns() {
        // Patrones para ubicaciones
        validationPatterns.put("ubicacion", Pattern.compile("^[A-Za-záéíóúñÁÉÍÓÚÑ\\s]{2,50}$"));

        // Patrones para fechas
        validationPatterns.put("fecha", Pattern.compile("^(hoy|mañana|ayer|pasado\\s+mañana|anteayer|\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})$", Pattern.CASE_INSENSITIVE));

        // Patrones para horas
        validationPatterns.put("hora", Pattern.compile("^(\\d{1,2}[:.]\\d{2}|\\d{1,2}\\s+(?:horas?|h)|mediodía|medianoche|mañana|tarde|noche)$", Pattern.CASE_INSENSITIVE));

        // Patrones para temperaturas
        validationPatterns.put("temperatura", Pattern.compile("^\\d+\\s*(?:grados?|°)(?:\\s*[CcFf])?$"));

        // Patrones para nombres
        validationPatterns.put("nombre", Pattern.compile("^[A-Za-záéíóúñÁÉÍÓÚÑ\\s]{2,50}$"));

        // Patrones para lugares
        validationPatterns.put("lugar", Pattern.compile("^(salón|comedor|dormitorio|habitación|cocina|baño|oficina|garaje|jardín|terraza)$", Pattern.CASE_INSENSITIVE));

        // Patrones para artistas
        validationPatterns.put("artista", Pattern.compile("^[A-Za-záéíóúñÁÉÍÓÚÑ\\s\\-]{2,100}$"));

        // Patrones para géneros
        validationPatterns.put("genero", Pattern.compile("^(rock|jazz|clásica|pop|reggaeton|flamenco|electrónica|folk|blues|salsa)$", Pattern.CASE_INSENSITIVE));

        // Patrones para canciones
        validationPatterns.put("cancion", Pattern.compile("^[A-Za-záéíóúñÁÉÍÓÚÑ\\s\\-\\d]{2,200}$"));
    }

    /**
     * Inicializa los valores válidos predefinidos.
     */
    private void initializeValidValues() {
        // Valores válidos para lugares
        validValues.put("lugar", new HashSet<>(Arrays.asList(
            "salón", "comedor", "dormitorio", "habitación", "cocina", "baño", "oficina", "garaje", "jardín", "terraza"
        )));

        // Valores válidos para géneros musicales
        validValues.put("genero", new HashSet<>(Arrays.asList(
            "rock", "jazz", "clásica", "pop", "reggaeton", "flamenco", "electrónica", "folk", "blues", "salsa"
        )));

        // Valores válidos para fechas relativas
        validValues.put("fecha_relativa", new HashSet<>(Arrays.asList(
            "hoy", "mañana", "ayer", "pasado mañana", "anteayer"
        )));

        // Valores válidos para horas relativas
        validValues.put("hora_relativa", new HashSet<>(Arrays.asList(
            "mediodía", "medianoche", "mañana", "tarde", "noche"
        )));
    }

    /**
     * Valida una lista de entidades.
     */
    public List<Entity> validateEntities(List<Entity> entities, EntityExtractionRequest request, EntityExtractionResult result) {
        List<Entity> validatedEntities = new ArrayList<>();

        for (Entity entity : entities) {
            try {
                Entity validatedEntity = validateEntity(entity, request, result);
                if (validatedEntity != null) {
                    validatedEntities.add(validatedEntity);
                }
            } catch (Exception e) {
                logger.warn("Error validando entidad {}: {}", entity.getEntityId(), e.getMessage());
                result.addWarning("Error validando entidad " + entity.getEntityId() + ": " + e.getMessage());
            }
        }

        logger.debug("Validación completada: {} entidades validadas de {}", validatedEntities.size(), entities.size());
        return validatedEntities;
    }

    /**
     * Valida una entidad individual.
     */
    public Entity validateEntity(Entity entity, EntityExtractionRequest request, EntityExtractionResult result) {
        if (entity == null || !entity.isValid()) {
            return null;
        }

        Entity validatedEntity = entity;
        double validationScore = 1.0;
        List<String> validationMessages = new ArrayList<>();

        // Validación por reglas
        if (enableRuleValidation) {
            double ruleScore = validateByRules(entity, validationMessages);
            validationScore *= ruleScore;
        }

        // Normalización
        if (enableNormalization) {
            validatedEntity = normalizeEntity(validatedEntity);
        }

        // Validación contextual
        if (enableContextualValidation) {
            double contextualScore = validateContextually(validatedEntity, request, validationMessages);
            validationScore *= contextualScore;
        }

        // Validación LLM (simulada)
        if (enableLlmValidation) {
            double llmScore = validateByLlm(validatedEntity, request, validationMessages);
            validationScore *= llmScore;
        }

        // Actualizar confianza basada en validación
        double finalConfidence = validatedEntity.getConfidenceScore() * validationScore;
        validatedEntity.setConfidenceScore(Math.min(finalConfidence, 1.0));

        // Marcar como validada
        validatedEntity.validate();

        // Agregar mensajes de validación al resultado
        for (String message : validationMessages) {
            if (message.startsWith("ERROR:")) {
                result.addValidationError(message.substring(6));
            } else if (message.startsWith("WARNING:")) {
                result.addWarning(message.substring(8));
            }
        }

        // Retornar entidad si cumple con el umbral de confianza
        return validatedEntity.getConfidenceScore() >= confidenceThreshold ? validatedEntity : null;
    }

    /**
     * Valida entidad por reglas predefinidas.
     */
    private double validateByRules(Entity entity, List<String> validationMessages) {
        double score = 1.0;
        String entityType = entity.getEntityType();
        String value = entity.getValue();

        // Validación por patrón
        Pattern pattern = validationPatterns.get(entityType);
        if (pattern != null && !pattern.matcher(value).matches()) {
            validationMessages.add("WARNING: Valor '" + value + "' no coincide con el patrón esperado para " + entityType);
            score *= 0.8;
        }

        // Validación por valores válidos
        Set<String> validValuesSet = validValues.get(entityType);
        if (validValuesSet != null) {
            if (!validValuesSet.contains(value.toLowerCase())) {
                validationMessages.add("WARNING: Valor '" + value + "' no está en la lista de valores válidos para " + entityType);
                score *= 0.9;
            } else {
                score *= 1.1; // Bonus por valor válido
            }
        }

        // Validaciones específicas por tipo
        switch (entityType) {
            case "ubicacion":
                score *= validateLocation(value, validationMessages);
                break;
            case "fecha":
                score *= validateDate(value, validationMessages);
                break;
            case "hora":
                score *= validateTime(value, validationMessages);
                break;
            case "temperatura":
                score *= validateTemperature(value, validationMessages);
                break;
            case "nombre":
                score *= validateName(value, validationMessages);
                break;
        }

        return Math.max(score, 0.0);
    }

    /**
     * Normaliza una entidad.
     */
    private Entity normalizeEntity(Entity entity) {
        String normalizedValue = normalizeValue(entity.getValue(), entity.getEntityType());
        entity.setNormalizedValue(normalizedValue);
        return entity;
    }

    /**
     * Normaliza un valor según su tipo.
     */
    private String normalizeValue(String value, String entityType) {
        if (value == null) return null;

        String normalized = value.trim();

        switch (entityType) {
            case "ubicacion":
                // Capitalizar primera letra de cada palabra
                normalized = capitalizeWords(normalized);
                break;
            case "fecha":
                // Normalizar fechas relativas
                normalized = normalizeDate(normalized);
                break;
            case "hora":
                // Normalizar formato de hora
                normalized = normalizeTime(normalized);
                break;
            case "temperatura":
                // Normalizar formato de temperatura
                normalized = normalizeTemperature(normalized);
                break;
            case "nombre":
                // Capitalizar nombre
                normalized = capitalizeWords(normalized);
                break;
            case "lugar":
                // Normalizar a minúsculas
                normalized = normalized.toLowerCase();
                break;
            case "genero":
                // Normalizar género musical
                normalized = normalizeGenre(normalized);
                break;
        }

        return normalized;
    }

    /**
     * Valida entidad contextualmente.
     */
    private double validateContextually(Entity entity, EntityExtractionRequest request, List<String> validationMessages) {
        double score = 1.0;

        // Validación basada en el contexto de la conversación
        if (request.hasConversationSession()) {
            // Verificar consistencia con entidades previas
            score *= validateConsistency(entity, request, validationMessages);
        }

        // Validación basada en la intención
        if (request.hasIntent()) {
            score *= validateIntentConsistency(entity, request.getIntent(), validationMessages);
        }

        return Math.max(score, 0.0);
    }

    /**
     * Valida entidad usando LLM (simulado).
     */
    private double validateByLlm(Entity entity, EntityExtractionRequest request, List<String> validationMessages) {
        // Simulación de validación LLM
        double score = 0.9; // Base score

        // Simular validación basada en el tipo de entidad
        switch (entity.getEntityType()) {
            case "ubicacion":
                if (entity.getValue().length() > 2) score += 0.05;
                if (entity.getValue().matches(".*[A-Z].*")) score += 0.05;
                break;
            case "fecha":
                if (entity.getValue().matches("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}")) score += 0.1;
                break;
            case "hora":
                if (entity.getValue().matches("\\d{1,2}[:.]\\d{2}")) score += 0.1;
                break;
        }

        return Math.min(score, 1.0);
    }

    // Métodos de validación específicos
    private double validateLocation(String value, List<String> messages) {
        double score = 1.0;
        
        if (value.length() < 2) {
            messages.add("ERROR: Ubicación demasiado corta: " + value);
            score *= 0.5;
        }
        
        if (value.length() > 50) {
            messages.add("WARNING: Ubicación muy larga: " + value);
            score *= 0.8;
        }
        
        return score;
    }

    private double validateDate(String value, List<String> messages) {
        double score = 1.0;
        
        // Validar fechas numéricas
        if (value.matches("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}")) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate.parse(value, formatter);
            } catch (DateTimeParseException e) {
                messages.add("ERROR: Formato de fecha inválido: " + value);
                score *= 0.3;
            }
        }
        
        return score;
    }

    private double validateTime(String value, List<String> messages) {
        double score = 1.0;
        
        // Validar horas numéricas
        if (value.matches("\\d{1,2}[:.]\\d{2}")) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                LocalTime.parse(value, formatter);
            } catch (DateTimeParseException e) {
                messages.add("ERROR: Formato de hora inválido: " + value);
                score *= 0.3;
            }
        }
        
        return score;
    }

    private double validateTemperature(String value, List<String> messages) {
        double score = 1.0;
        
        try {
            String numericPart = value.replaceAll("[^\\d.-]", "");
            double temp = Double.parseDouble(numericPart);
            
            if (temp < -50 || temp > 100) {
                messages.add("WARNING: Temperatura fuera de rango razonable: " + temp);
                score *= 0.8;
            }
        } catch (NumberFormatException e) {
            messages.add("ERROR: No se puede parsear temperatura: " + value);
            score *= 0.3;
        }
        
        return score;
    }

    private double validateName(String value, List<String> messages) {
        double score = 1.0;
        
        if (value.length() < 2) {
            messages.add("ERROR: Nombre demasiado corto: " + value);
            score *= 0.5;
        }
        
        if (value.length() > 50) {
            messages.add("WARNING: Nombre muy largo: " + value);
            score *= 0.8;
        }
        
        return score;
    }

    // Métodos de normalización específicos
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;
        
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].isEmpty()) {
                result.append(words[i].substring(0, 1).toUpperCase())
                      .append(words[i].substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }

    private String normalizeDate(String date) {
        if (date == null) return date;
        
        String lower = date.toLowerCase();
        switch (lower) {
            case "hoy": return "hoy";
            case "mañana": return "mañana";
            case "ayer": return "ayer";
            case "pasado mañana": return "pasado mañana";
            case "anteayer": return "anteayer";
            default: return date;
        }
    }

    private String normalizeTime(String time) {
        if (time == null) return time;
        
        String lower = time.toLowerCase();
        switch (lower) {
            case "mediodía": return "mediodía";
            case "medianoche": return "medianoche";
            case "mañana": return "mañana";
            case "tarde": return "tarde";
            case "noche": return "noche";
            default: return time;
        }
    }

    private String normalizeTemperature(String temp) {
        if (temp == null) return temp;
        
        // Extraer número y normalizar formato
        String numericPart = temp.replaceAll("[^\\d.-]", "");
        if (!numericPart.isEmpty()) {
            return numericPart + "°C";
        }
        
        return temp;
    }

    private String normalizeGenre(String genre) {
        if (genre == null) return genre;
        
        String lower = genre.toLowerCase();
        switch (lower) {
            case "rock": return "rock";
            case "jazz": return "jazz";
            case "clásica": return "clásica";
            case "pop": return "pop";
            case "reggaeton": return "reggaeton";
            case "flamenco": return "flamenco";
            case "electrónica": return "electrónica";
            case "folk": return "folk";
            case "blues": return "blues";
            case "salsa": return "salsa";
            default: return genre;
        }
    }

    // Métodos de validación contextual
    private double validateConsistency(Entity entity, EntityExtractionRequest request, List<String> messages) {
        // Simulación de validación de consistencia
        return 1.0;
    }

    private double validateIntentConsistency(Entity entity, String intent, List<String> messages) {
        // Simulación de validación de consistencia con intención
        return 1.0;
    }

    /**
     * Obtiene estadísticas del validador.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("rule_validation_enabled", enableRuleValidation);
        stats.put("normalization_enabled", enableNormalization);
        stats.put("contextual_validation_enabled", enableContextualValidation);
        stats.put("llm_validation_enabled", enableLlmValidation);
        stats.put("confidence_threshold", confidenceThreshold);
        stats.put("validation_patterns_count", validationPatterns.size());
        stats.put("valid_values_sets_count", validValues.size());
        return stats;
    }
} 