package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Modelo de dominio para representar una entidad extraída del texto.
 * Una entidad es una pieza de información específica identificada en el texto del usuario.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Entity {

    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("entity_type")
    private String entityType;

    @JsonProperty("value")
    private String value;

    @JsonProperty("normalized_value")
    private String normalizedValue;

    @JsonProperty("confidence_score")
    private double confidenceScore;

    @JsonProperty("start_position")
    private int startPosition;

    @JsonProperty("end_position")
    private int endPosition;

    @JsonProperty("extraction_method")
    private String extractionMethod;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("context")
    private String context;

    @JsonProperty("is_resolved")
    private boolean isResolved;

    @JsonProperty("resolved_value")
    private String resolvedValue;

    @JsonProperty("extracted_at")
    private LocalDateTime extractedAt;

    @JsonProperty("validated_at")
    private LocalDateTime validatedAt;

    // Constructores
    public Entity() {
        this.extractedAt = LocalDateTime.now();
        this.isResolved = false;
    }

    public Entity(String entityType, String value, double confidenceScore) {
        this();
        this.entityType = entityType;
        this.value = value;
        this.confidenceScore = confidenceScore;
        this.entityId = generateEntityId();
    }

    public Entity(String entityType, String value, String normalizedValue, double confidenceScore) {
        this(entityType, value, confidenceScore);
        this.normalizedValue = normalizedValue;
    }

    // Métodos de utilidad
    private String generateEntityId() {
        return "entity_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    public boolean isValid() {
        return value != null && !value.trim().isEmpty() && confidenceScore > 0.0;
    }

    public boolean isHighConfidence() {
        return confidenceScore >= 0.8;
    }

    public boolean isMediumConfidence() {
        return confidenceScore >= 0.6 && confidenceScore < 0.8;
    }

    public boolean isLowConfidence() {
        return confidenceScore < 0.6;
    }

    public void resolve(String resolvedValue) {
        this.resolvedValue = resolvedValue;
        this.isResolved = true;
    }

    public void validate() {
        this.validatedAt = LocalDateTime.now();
    }

    public String getEffectiveValue() {
        return isResolved ? resolvedValue : (normalizedValue != null ? normalizedValue : value);
    }

    // Getters y Setters
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public void setNormalizedValue(String normalizedValue) {
        this.normalizedValue = normalizedValue;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public String getExtractionMethod() {
        return extractionMethod;
    }

    public void setExtractionMethod(String extractionMethod) {
        this.extractionMethod = extractionMethod;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public String getResolvedValue() {
        return resolvedValue;
    }

    public void setResolvedValue(String resolvedValue) {
        this.resolvedValue = resolvedValue;
    }

    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(LocalDateTime extractedAt) {
        this.extractedAt = extractedAt;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }

    @Override
    public String toString() {
        return String.format("Entity{type='%s', value='%s', confidence=%.2f, method='%s'}", 
                           entityType, value, confidenceScore, extractionMethod);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Entity entity = (Entity) obj;
        return entityId != null && entityId.equals(entity.entityId);
    }

    @Override
    public int hashCode() {
        return entityId != null ? entityId.hashCode() : 0;
    }
} 