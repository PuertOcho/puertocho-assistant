package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Resultado de clasificación de intenciones usando RAG.
 * Incluye información detallada sobre el proceso de clasificación y ejemplos utilizados.
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
    
    @JsonProperty("rag_examples_used")
    private List<RagExample> ragExamplesUsed;
    
    @JsonProperty("prompt_used")
    private String promptUsed;
    
    @JsonProperty("llm_response")
    private String llmResponse;
    
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    @JsonProperty("vector_search_time_ms")
    private Long vectorSearchTimeMs;
    
    @JsonProperty("llm_inference_time_ms")
    private Long llmInferenceTimeMs;
    
    @JsonProperty("similarity_scores")
    private List<Double> similarityScores;
    
    @JsonProperty("fallback_used")
    private Boolean fallbackUsed;
    
    @JsonProperty("fallback_reason")
    private String fallbackReason;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    public IntentClassificationResult() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
        this.fallbackUsed = false;
    }
    
    public IntentClassificationResult(String intentId, Double confidenceScore) {
        this();
        this.intentId = intentId;
        this.confidenceScore = confidenceScore;
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
    
    public List<RagExample> getRagExamplesUsed() {
        return ragExamplesUsed;
    }
    
    public void setRagExamplesUsed(List<RagExample> ragExamplesUsed) {
        this.ragExamplesUsed = ragExamplesUsed;
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
    
    public Long getVectorSearchTimeMs() {
        return vectorSearchTimeMs;
    }
    
    public void setVectorSearchTimeMs(Long vectorSearchTimeMs) {
        this.vectorSearchTimeMs = vectorSearchTimeMs;
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
    }
    
    /**
     * Ejemplo RAG utilizado en la clasificación
     */
    public static class RagExample {
        @JsonProperty("example_text")
        private String exampleText;
        
        @JsonProperty("intent_id")
        private String intentId;
        
        @JsonProperty("similarity_score")
        private Double similarityScore;
        
        @JsonProperty("source")
        private String source;
        
        @JsonProperty("metadata")
        private Map<String, Object> metadata;
        
        public RagExample() {}
        
        public RagExample(String exampleText, String intentId, Double similarityScore) {
            this.exampleText = exampleText;
            this.intentId = intentId;
            this.similarityScore = similarityScore;
        }
        
        // Getters y Setters
        public String getExampleText() {
            return exampleText;
        }
        
        public void setExampleText(String exampleText) {
            this.exampleText = exampleText;
        }
        
        public String getIntentId() {
            return intentId;
        }
        
        public void setIntentId(String intentId) {
            this.intentId = intentId;
        }
        
        public Double getSimilarityScore() {
            return similarityScore;
        }
        
        public void setSimilarityScore(Double similarityScore) {
            this.similarityScore = similarityScore;
        }
        
        public String getSource() {
            return source;
        }
        
        public void setSource(String source) {
            this.source = source;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
        
        @Override
        public String toString() {
            return "RagExample{" +
                    "exampleText='" + exampleText + '\'' +
                    ", intentId='" + intentId + '\'' +
                    ", similarityScore=" + similarityScore +
                    ", source='" + source + '\'' +
                    ", metadata=" + metadata +
                    '}';
        }
    }
    
    /**
     * Métodos de utilidad
     */
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore >= 0.8;
    }
    
    public boolean isMediumConfidence() {
        return confidenceScore != null && confidenceScore >= 0.6 && confidenceScore < 0.8;
    }
    
    public boolean isLowConfidence() {
        return confidenceScore != null && confidenceScore < 0.6;
    }
    
    public double getAverageSimilarityScore() {
        if (similarityScores == null || similarityScores.isEmpty()) {
            return 0.0;
        }
        return similarityScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    public int getRagExamplesCount() {
        return ragExamplesUsed != null ? ragExamplesUsed.size() : 0;
    }
    
    @Override
    public String toString() {
        return "IntentClassificationResult{" +
                "intentId='" + intentId + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", detectedEntities=" + detectedEntities +
                ", mcpAction='" + mcpAction + '\'' +
                ", expertDomain='" + expertDomain + '\'' +
                ", ragExamplesUsed=" + (ragExamplesUsed != null ? ragExamplesUsed.size() : 0) + " examples" +
                ", processingTimeMs=" + processingTimeMs +
                ", fallbackUsed=" + fallbackUsed +
                ", success=" + success +
                ", timestamp=" + timestamp +
                '}';
    }
} 