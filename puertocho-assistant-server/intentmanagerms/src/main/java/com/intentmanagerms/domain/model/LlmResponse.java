package com.intentmanagerms.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Respuesta de un LLM incluyendo metadatos y información de rendimiento.
 */
public class LlmResponse {
    private String id;
    private String llmId;
    private String content;
    private String model;
    private LocalDateTime timestamp;
    private Long responseTimeMs;
    private Integer tokensUsed;
    private Double confidence;
    private Map<String, Object> metadata;
    private boolean success;
    private String errorMessage;

    // Constructor por defecto
    public LlmResponse() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
    }

    // Constructor para respuesta exitosa
    public LlmResponse(String id, String llmId, String content, String model) {
        this();
        this.id = id;
        this.llmId = llmId;
        this.content = content;
        this.model = model;
    }

    // Constructor para respuesta con error
    public LlmResponse(String id, String llmId, String errorMessage) {
        this();
        this.id = id;
        this.llmId = llmId;
        this.errorMessage = errorMessage;
        this.success = false;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLlmId() {
        return llmId;
    }

    public void setLlmId(String llmId) {
        this.llmId = llmId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
        return "LlmResponse{" +
                "id='" + id + '\'' +
                ", llmId='" + llmId + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\'' +
                ", model='" + model + '\'' +
                ", success=" + success +
                ", responseTimeMs=" + responseTimeMs +
                ", confidence=" + confidence +
                '}';
    }
} 