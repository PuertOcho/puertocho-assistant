package com.intentmanagerms.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Representa el voto de un LLM individual en una ronda de votación.
 * Contiene la clasificación propuesta por el LLM y metadatos del voto.
 */
public class LlmVote {
    private String voteId;
    private String llmId;
    private String llmName;
    private String llmRole;
    private Double llmWeight;
    private String intent;
    private Map<String, Object> entities;
    private Double confidence;
    private String reasoning;
    private List<Map<String, Object>> subtasks;
    private LocalDateTime voteTime;
    private Long processingTimeMs;
    private VoteStatus status;
    private String errorMessage;
    private Map<String, Object> metadata;

    // Constructor por defecto
    public LlmVote() {
        this.voteTime = LocalDateTime.now();
        this.status = VoteStatus.PENDING;
    }

    // Constructor con parámetros principales
    public LlmVote(String voteId, String llmId, String llmName, String intent, Double confidence) {
        this();
        this.voteId = voteId;
        this.llmId = llmId;
        this.llmName = llmName;
        this.intent = intent;
        this.confidence = confidence;
    }

    // Getters y Setters
    public String getVoteId() {
        return voteId;
    }

    public void setVoteId(String voteId) {
        this.voteId = voteId;
    }

    public String getLlmId() {
        return llmId;
    }

    public void setLlmId(String llmId) {
        this.llmId = llmId;
    }

    public String getLlmName() {
        return llmName;
    }

    public void setLlmName(String llmName) {
        this.llmName = llmName;
    }

    public String getLlmRole() {
        return llmRole;
    }

    public void setLlmRole(String llmRole) {
        this.llmRole = llmRole;
    }

    public Double getLlmWeight() {
        return llmWeight;
    }

    public void setLlmWeight(Double llmWeight) {
        this.llmWeight = llmWeight;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Map<String, Object> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, Object> entities) {
        this.entities = entities;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<Map<String, Object>> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Map<String, Object>> subtasks) {
        this.subtasks = subtasks;
    }

    public LocalDateTime getVoteTime() {
        return voteTime;
    }

    public void setVoteTime(LocalDateTime voteTime) {
        this.voteTime = voteTime;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public VoteStatus getStatus() {
        return status;
    }

    public void setStatus(VoteStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Verifica si el voto es válido.
     */
    public boolean isValid() {
        return status == VoteStatus.COMPLETED && 
               intent != null && 
               confidence != null && 
               confidence >= 0.0 && 
               confidence <= 1.0;
    }

    /**
     * Calcula el score ponderado del voto.
     */
    public double getWeightedScore() {
        if (!isValid() || llmWeight == null) {
            return 0.0;
        }
        return confidence * llmWeight;
    }

    @Override
    public String toString() {
        return "LlmVote{" +
                "voteId='" + voteId + '\'' +
                ", llmId='" + llmId + '\'' +
                ", llmName='" + llmName + '\'' +
                ", intent='" + intent + '\'' +
                ", confidence=" + confidence +
                ", status=" + status +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }

    /**
     * Estados posibles de un voto.
     */
    public enum VoteStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        TIMEOUT
    }
} 