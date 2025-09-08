package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Resultado de clasificación de intenciones.
 * Incluye información detallada sobre el proceso de clasificación y metadata del análisis.
 */
public class IntentClassificationResult {
    
    @JsonProperty("intent_id")
    private String intentId;
    
    @JsonProperty("confidence_score")
    private Double confidenceScore;
    
    @JsonProperty("detected_entities")
    private Map<String, Object> detectedEntities;
    
    @JsonProperty("mcp_action")
    private String mcpAction;
    
    @JsonProperty("expert_domain")
    private String expertDomain;
    
    @JsonProperty("examples_used")
    private List<String> examplesUsed;
    
    @JsonProperty("prompt_used")
    private String promptUsed;
    
    @JsonProperty("llm_response")
    private String llmResponse;
    
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    @JsonProperty("similarity_analysis_time_ms")
    private Long similarityAnalysisTimeMs;
    
    @JsonProperty("llm_inference_time_ms")
    private Long llmInferenceTimeMs;
    
    @JsonProperty("similarity_scores")
    private List<Double> similarityScores;
    
    @JsonProperty("fallback_used")
    private Boolean fallbackUsed;
    
    @JsonProperty("fallback_reason")
    private String fallbackReason;
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // Constructor por defecto
    public IntentClassificationResult() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
        this.fallbackUsed = false;
    }
    
    // Constructor con parámetros principales
    public IntentClassificationResult(String intentId, Double confidenceScore, Map<String, Object> detectedEntities) {
        this();
        this.intentId = intentId;
        this.confidenceScore = confidenceScore;
        this.detectedEntities = detectedEntities;
    }
    
    // Getters y Setters
    public String getIntentId() {
        return intentId;
    }
    
    public void setIntentId(String intentId) {
        this.intentId = intentId;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public Map<String, Object> getDetectedEntities() {
        return detectedEntities;
    }
    
    public void setDetectedEntities(Map<String, Object> detectedEntities) {
        this.detectedEntities = detectedEntities;
    }
    
    public String getMcpAction() {
        return mcpAction;
    }
    
    public void setMcpAction(String mcpAction) {
        this.mcpAction = mcpAction;
    }
    
    public String getExpertDomain() {
        return expertDomain;
    }
    
    public void setExpertDomain(String expertDomain) {
        this.expertDomain = expertDomain;
    }
    
    public List<String> getExamplesUsed() {
        return examplesUsed;
    }
    
    public void setExamplesUsed(List<String> examplesUsed) {
        this.examplesUsed = examplesUsed;
    }
    
    public String getPromptUsed() {
        return promptUsed;
    }
    
    public void setPromptUsed(String promptUsed) {
        this.promptUsed = promptUsed;
    }
    
    public String getLlmResponse() {
        return llmResponse;
    }
    
    public void setLlmResponse(String llmResponse) {
        this.llmResponse = llmResponse;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public Long getSimilarityAnalysisTimeMs() {
        return similarityAnalysisTimeMs;
    }
    
    public void setSimilarityAnalysisTimeMs(Long similarityAnalysisTimeMs) {
        this.similarityAnalysisTimeMs = similarityAnalysisTimeMs;
    }
    
    public Long getLlmInferenceTimeMs() {
        return llmInferenceTimeMs;
    }
    
    public void setLlmInferenceTimeMs(Long llmInferenceTimeMs) {
        this.llmInferenceTimeMs = llmInferenceTimeMs;
    }
    
    public List<Double> getSimilarityScores() {
        return similarityScores;
    }
    
    public void setSimilarityScores(List<Double> similarityScores) {
        this.similarityScores = similarityScores;
    }
    
    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }
    
    public void setFallbackUsed(Boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }
    
    public String getFallbackReason() {
        return fallbackReason;
    }
    
    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = false;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    // Métodos de utilidad
    
    /**
     * Verifica si la clasificación fue exitosa y confiable
     */
    public boolean isSuccessfulClassification() {
        return success != null && success && 
               confidenceScore != null && confidenceScore > 0.7 && 
               !Boolean.TRUE.equals(fallbackUsed);
    }
    
    /**
     * Obtiene la confianza promedio de similitud
     */
    public double getAverageSimilarityScore() {
        return similarityScores != null && !similarityScores.isEmpty() ?
                similarityScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0) : 0.0;
    }
    
    /**
     * Obtiene el número de ejemplos utilizados
     */
    public int getExamplesCount() {
        return examplesUsed != null ? examplesUsed.size() : 0;
    }
    
    /**
     * Verifica si se detectaron entidades
     */
    public boolean hasEntities() {
        return detectedEntities != null && !detectedEntities.isEmpty();
    }
    
    @Override
    public String toString() {
        return "IntentClassificationResult{" +
                "intentId='" + intentId + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", detectedEntities=" + (detectedEntities != null ? detectedEntities.size() : 0) + " entities" +
                ", mcpAction='" + mcpAction + '\'' +
                ", expertDomain='" + expertDomain + '\'' +
                ", examplesUsed=" + getExamplesCount() + " examples" +
                ", processingTimeMs=" + processingTimeMs +
                ", fallbackUsed=" + fallbackUsed +
                ", success=" + success +
                ", timestamp=" + timestamp +
                '}';
    }
}