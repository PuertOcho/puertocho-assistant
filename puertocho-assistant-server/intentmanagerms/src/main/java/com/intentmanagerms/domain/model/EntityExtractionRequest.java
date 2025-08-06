package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio para solicitudes de extracción de entidades.
 * Define los parámetros necesarios para extraer entidades de un texto.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityExtractionRequest {

    @JsonProperty("text")
    private String text;

    @JsonProperty("entity_types")
    private List<String> entityTypes;

    @JsonProperty("context")
    private String context;

    @JsonProperty("conversation_session_id")
    private String conversationSessionId;

    @JsonProperty("intent")
    private String intent;

    @JsonProperty("extraction_methods")
    private List<String> extractionMethods;

    @JsonProperty("confidence_threshold")
    private double confidenceThreshold;

    @JsonProperty("enable_anaphora_resolution")
    private boolean enableAnaphoraResolution;

    @JsonProperty("enable_context_resolution")
    private boolean enableContextResolution;

    @JsonProperty("enable_validation")
    private boolean enableValidation;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("language")
    private String language;

    @JsonProperty("max_entities")
    private int maxEntities;

    // Constructores
    public EntityExtractionRequest() {
        this.confidenceThreshold = 0.6;
        this.enableAnaphoraResolution = true;
        this.enableContextResolution = true;
        this.enableValidation = true;
        this.language = "es";
        this.maxEntities = 10;
    }

    public EntityExtractionRequest(String text) {
        this();
        this.text = text;
    }

    public EntityExtractionRequest(String text, List<String> entityTypes) {
        this(text);
        this.entityTypes = entityTypes;
    }

    // Métodos de utilidad
    public boolean hasContext() {
        return context != null && !context.trim().isEmpty();
    }

    public boolean hasConversationSession() {
        return conversationSessionId != null && !conversationSessionId.trim().isEmpty();
    }

    public boolean hasIntent() {
        return intent != null && !intent.trim().isEmpty();
    }

    public boolean shouldUseAnaphoraResolution() {
        return enableAnaphoraResolution && hasConversationSession();
    }

    public boolean shouldUseContextResolution() {
        return enableContextResolution && hasContext();
    }

    public boolean shouldValidate() {
        return enableValidation;
    }

    public boolean hasSpecificEntityTypes() {
        return entityTypes != null && !entityTypes.isEmpty();
    }

    public boolean hasSpecificExtractionMethods() {
        return extractionMethods != null && !extractionMethods.isEmpty();
    }

    // Getters y Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(List<String> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getConversationSessionId() {
        return conversationSessionId;
    }

    public void setConversationSessionId(String conversationSessionId) {
        this.conversationSessionId = conversationSessionId;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<String> getExtractionMethods() {
        return extractionMethods;
    }

    public void setExtractionMethods(List<String> extractionMethods) {
        this.extractionMethods = extractionMethods;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public boolean isEnableAnaphoraResolution() {
        return enableAnaphoraResolution;
    }

    public void setEnableAnaphoraResolution(boolean enableAnaphoraResolution) {
        this.enableAnaphoraResolution = enableAnaphoraResolution;
    }

    public boolean isEnableContextResolution() {
        return enableContextResolution;
    }

    public void setEnableContextResolution(boolean enableContextResolution) {
        this.enableContextResolution = enableContextResolution;
    }

    public boolean isEnableValidation() {
        return enableValidation;
    }

    public void setEnableValidation(boolean enableValidation) {
        this.enableValidation = enableValidation;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getMaxEntities() {
        return maxEntities;
    }

    public void setMaxEntities(int maxEntities) {
        this.maxEntities = maxEntities;
    }

    @Override
    public String toString() {
        return String.format("EntityExtractionRequest{text='%s', entityTypes=%s, intent='%s', confidenceThreshold=%.2f}", 
                           text, entityTypes, intent, confidenceThreshold);
    }
} 