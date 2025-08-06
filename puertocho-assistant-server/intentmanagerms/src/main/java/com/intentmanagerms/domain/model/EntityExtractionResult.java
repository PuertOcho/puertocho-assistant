package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio para resultados de extracción de entidades.
 * Contiene las entidades extraídas y metadatos del proceso de extracción.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityExtractionResult {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("text")
    private String text;

    @JsonProperty("entities")
    private List<Entity> entities;

    @JsonProperty("extraction_methods_used")
    private List<String> extractionMethodsUsed;

    @JsonProperty("processing_time_ms")
    private long processingTimeMs;

    @JsonProperty("confidence_threshold")
    private double confidenceThreshold;

    @JsonProperty("total_entities_found")
    private int totalEntitiesFound;

    @JsonProperty("high_confidence_entities")
    private int highConfidenceEntities;

    @JsonProperty("medium_confidence_entities")
    private int mediumConfidenceEntities;

    @JsonProperty("low_confidence_entities")
    private int lowConfidenceEntities;

    @JsonProperty("anaphora_resolved")
    private int anaphoraResolved;

    @JsonProperty("context_resolved")
    private int contextResolved;

    @JsonProperty("validation_errors")
    private List<String> validationErrors;

    @JsonProperty("warnings")
    private List<String> warnings;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("extracted_at")
    private LocalDateTime extractedAt;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("error_message")
    private String errorMessage;

    // Constructores
    public EntityExtractionResult() {
        this.extractedAt = LocalDateTime.now();
        this.success = true;
        this.requestId = generateRequestId();
    }

    public EntityExtractionResult(String text, List<Entity> entities) {
        this();
        this.text = text;
        this.entities = entities;
        calculateStatistics();
    }

    // Métodos de utilidad
    private String generateRequestId() {
        return "entity_extraction_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    public void calculateStatistics() {
        if (entities == null) {
            this.totalEntitiesFound = 0;
            this.highConfidenceEntities = 0;
            this.mediumConfidenceEntities = 0;
            this.lowConfidenceEntities = 0;
            this.anaphoraResolved = 0;
            this.contextResolved = 0;
            return;
        }

        this.totalEntitiesFound = entities.size();
        this.highConfidenceEntities = (int) entities.stream().filter(Entity::isHighConfidence).count();
        this.mediumConfidenceEntities = (int) entities.stream().filter(Entity::isMediumConfidence).count();
        this.lowConfidenceEntities = (int) entities.stream().filter(Entity::isLowConfidence).count();
        this.anaphoraResolved = (int) entities.stream().filter(Entity::isResolved).count();
        this.contextResolved = (int) entities.stream()
                .filter(e -> e.getExtractionMethod() != null && e.getExtractionMethod().contains("context"))
                .count();
    }

    public List<Entity> getEntitiesByType(String entityType) {
        if (entities == null) return List.of();
        return entities.stream()
                .filter(e -> entityType.equals(e.getEntityType()))
                .toList();
    }

    public List<Entity> getHighConfidenceEntities() {
        if (entities == null) return List.of();
        return entities.stream()
                .filter(Entity::isHighConfidence)
                .toList();
    }

    public List<Entity> getValidEntities() {
        if (entities == null) return List.of();
        return entities.stream()
                .filter(Entity::isValid)
                .toList();
    }

    public boolean hasEntities() {
        return entities != null && !entities.isEmpty();
    }

    public boolean hasHighConfidenceEntities() {
        return highConfidenceEntities > 0;
    }

    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public void addEntity(Entity entity) {
        if (entities == null) {
            entities = new java.util.ArrayList<>();
        }
        entities.add(entity);
        calculateStatistics();
    }

    public void addValidationError(String error) {
        if (validationErrors == null) {
            validationErrors = new java.util.ArrayList<>();
        }
        validationErrors.add(error);
    }

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new java.util.ArrayList<>();
        }
        warnings.add(warning);
    }

    public void markAsFailed(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
    }

    public double getAverageConfidence() {
        if (entities == null || entities.isEmpty()) return 0.0;
        return entities.stream()
                .mapToDouble(Entity::getConfidenceScore)
                .average()
                .orElse(0.0);
    }

    // Getters y Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities;
        calculateStatistics();
    }

    public List<String> getExtractionMethodsUsed() {
        return extractionMethodsUsed;
    }

    public void setExtractionMethodsUsed(List<String> extractionMethodsUsed) {
        this.extractionMethodsUsed = extractionMethodsUsed;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public int getTotalEntitiesFound() {
        return totalEntitiesFound;
    }

    public void setTotalEntitiesFound(int totalEntitiesFound) {
        this.totalEntitiesFound = totalEntitiesFound;
    }

    public int getHighConfidenceEntitiesCount() {
        return highConfidenceEntities;
    }

    public void setHighConfidenceEntities(int highConfidenceEntities) {
        this.highConfidenceEntities = highConfidenceEntities;
    }

    public int getMediumConfidenceEntities() {
        return mediumConfidenceEntities;
    }

    public void setMediumConfidenceEntities(int mediumConfidenceEntities) {
        this.mediumConfidenceEntities = mediumConfidenceEntities;
    }

    public int getLowConfidenceEntities() {
        return lowConfidenceEntities;
    }

    public void setLowConfidenceEntities(int lowConfidenceEntities) {
        this.lowConfidenceEntities = lowConfidenceEntities;
    }

    public int getAnaphoraResolved() {
        return anaphoraResolved;
    }

    public void setAnaphoraResolved(int anaphoraResolved) {
        this.anaphoraResolved = anaphoraResolved;
    }

    public int getContextResolved() {
        return contextResolved;
    }

    public void setContextResolved(int contextResolved) {
        this.contextResolved = contextResolved;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(LocalDateTime extractedAt) {
        this.extractedAt = extractedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return String.format("EntityExtractionResult{requestId='%s', entities=%d, success=%s, processingTime=%dms}", 
                           requestId, totalEntitiesFound, success, processingTimeMs);
    }
} 