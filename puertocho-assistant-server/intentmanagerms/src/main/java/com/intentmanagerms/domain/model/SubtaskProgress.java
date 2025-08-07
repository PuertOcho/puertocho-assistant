package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Modelo de dominio para el progreso de subtareas individuales
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubtaskProgress {
    
    @JsonProperty("subtask_id")
    private String subtaskId;
    
    @JsonProperty("action")
    private String action;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("status")
    private SubtaskStatus status = SubtaskStatus.PENDING;
    
    @JsonProperty("progress_percentage")
    private double progressPercentage = 0.0;
    
    @JsonProperty("confidence_score")
    private double confidenceScore = 0.0;
    
    @JsonProperty("entities")
    private Map<String, Object> entities;
    
    @JsonProperty("dependencies")
    private Map<String, Object> dependencies;
    
    @JsonProperty("result")
    private Map<String, Object> result;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("started_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;
    
    @JsonProperty("last_updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdatedAt;
    
    @JsonProperty("completed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
    
    @JsonProperty("execution_time_ms")
    private long executionTimeMs;
    
    @JsonProperty("retry_count")
    private int retryCount;
    
    @JsonProperty("is_critical")
    private boolean isCritical = false;
    
    @JsonProperty("is_completed")
    private boolean isCompleted = false;
    
    @JsonProperty("is_failed")
    private boolean isFailed = false;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("completion_message")
    private String completionMessage;
    
    // Constructor
    public SubtaskProgress() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    // Métodos de utilidad para gestión de estado
    public void markStarted() {
        this.status = SubtaskStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void markCompleted(Object result) {
        this.status = SubtaskStatus.COMPLETED;
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        this.progressPercentage = 100.0;
        
        if (result instanceof Map) {
            this.result = (Map<String, Object>) result;
        } else {
            this.result = Map.of("result", result);
        }
        
        if (this.startedAt != null) {
            this.executionTimeMs = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
        }
    }
    
    public void markFailed(String errorMessage) {
        this.status = SubtaskStatus.FAILED;
        this.isFailed = true;
        this.errorMessage = errorMessage;
        this.lastUpdatedAt = LocalDateTime.now();
        this.progressPercentage = 0.0;
    }
    
    public void updateProgress(double progressPercentage) {
        this.progressPercentage = Math.min(100.0, Math.max(0.0, progressPercentage));
        this.lastUpdatedAt = LocalDateTime.now();
        
        if (this.progressPercentage >= 100.0) {
            this.status = SubtaskStatus.COMPLETED;
            this.isCompleted = true;
        }
    }
    
    public void incrementRetry() {
        this.retryCount++;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public boolean canRetry(int maxRetries) {
        return this.retryCount < maxRetries && this.status == SubtaskStatus.FAILED;
    }
    
    public boolean isBlocking() {
        return this.isCritical && this.status == SubtaskStatus.FAILED;
    }
    
    // Getters y Setters
    public String getSubtaskId() {
        return subtaskId;
    }
    
    public void setSubtaskId(String subtaskId) {
        this.subtaskId = subtaskId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public SubtaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(SubtaskStatus status) {
        this.status = status;
    }
    
    public double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public Map<String, Object> getEntities() {
        return entities;
    }
    
    public void setEntities(Map<String, Object> entities) {
        this.entities = entities;
    }
    
    public Map<String, Object> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(Map<String, Object> dependencies) {
        this.dependencies = dependencies;
    }
    
    public Map<String, Object> getResult() {
        return result;
    }
    
    public void setResult(Map<String, Object> result) {
        this.result = result;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public boolean isCritical() {
        return isCritical;
    }
    
    public void setCritical(boolean critical) {
        isCritical = critical;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
    
    public boolean isFailed() {
        return isFailed;
    }
    
    public void setFailed(boolean failed) {
        isFailed = failed;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getCompletionMessage() {
        return completionMessage;
    }
    
    public void setCompletionMessage(String completionMessage) {
        this.completionMessage = completionMessage;
    }
    
    public enum SubtaskStatus {
        PENDING("pending", "Pendiente"),
        IN_PROGRESS("in_progress", "En progreso"),
        COMPLETED("completed", "Completado"),
        FAILED("failed", "Fallido"),
        CANCELLED("cancelled", "Cancelado"),
        SKIPPED("skipped", "Omitido"),
        RETRYING("retrying", "Reintentando");
        
        private final String value;
        private final String description;
        
        SubtaskStatus(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
