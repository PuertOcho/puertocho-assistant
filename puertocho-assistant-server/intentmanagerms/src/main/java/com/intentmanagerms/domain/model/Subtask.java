package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio para una subtarea individual dentro de una descomposición dinámica.
 * Representa una acción específica que debe ejecutarse como parte de una petición compleja.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Subtask {
    
    @JsonProperty("subtask_id")
    private String subtaskId;
    
    @JsonProperty("action")
    private String action;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("entities")
    private Map<String, Object> entities;
    
    @JsonProperty("dependencies")
    private List<String> dependencies;
    
    @JsonProperty("priority")
    private String priority;
    
    @JsonProperty("estimated_duration_ms")
    private Long estimatedDurationMs;
    
    @JsonProperty("max_retries")
    private Integer maxRetries;
    
    @JsonProperty("retry_count")
    private Integer retryCount;
    
    @JsonProperty("status")
    private SubtaskStatus status;
    
    @JsonProperty("confidence_score")
    private Double confidenceScore;
    
    @JsonProperty("execution_order")
    private Integer executionOrder;
    
    @JsonProperty("can_execute_parallel")
    private Boolean canExecuteParallel;
    
    @JsonProperty("required_slots")
    private List<String> requiredSlots;
    
    @JsonProperty("optional_slots")
    private List<String> optionalSlots;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonProperty("started_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;
    
    @JsonProperty("completed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
    
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    // Constructores
    public Subtask() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = SubtaskStatus.PENDING;
        this.retryCount = 0;
        this.canExecuteParallel = true;
        this.confidenceScore = 1.0;
    }
    
    public Subtask(String action, String description) {
        this();
        this.action = action;
        this.description = description;
    }
    
    // Métodos de utilidad
    public boolean isReadyToExecute() {
        return status == SubtaskStatus.PENDING && 
               (dependencies == null || dependencies.isEmpty());
    }
    
    public boolean isCompleted() {
        return status == SubtaskStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == SubtaskStatus.FAILED;
    }
    
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void startExecution() {
        this.status = SubtaskStatus.EXECUTING;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void completeExecution(Object result) {
        this.status = SubtaskStatus.COMPLETED;
        this.result = result;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.processingTimeMs = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
        }
    }
    
    public void failExecution(String errorMessage) {
        this.status = SubtaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.processingTimeMs = java.time.Duration.between(this.startedAt, LocalDateTime.now()).toMillis();
        }
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
    
    public Map<String, Object> getEntities() {
        return entities;
    }
    
    public void setEntities(Map<String, Object> entities) {
        this.entities = entities;
    }
    
    public List<String> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public Long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }
    
    public void setEstimatedDurationMs(Long estimatedDurationMs) {
        this.estimatedDurationMs = estimatedDurationMs;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public SubtaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(SubtaskStatus status) {
        this.status = status;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public Integer getExecutionOrder() {
        return executionOrder;
    }
    
    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }
    
    public Boolean getCanExecuteParallel() {
        return canExecuteParallel;
    }
    
    public void setCanExecuteParallel(Boolean canExecuteParallel) {
        this.canExecuteParallel = canExecuteParallel;
    }
    
    public List<String> getRequiredSlots() {
        return requiredSlots;
    }
    
    public void setRequiredSlots(List<String> requiredSlots) {
        this.requiredSlots = requiredSlots;
    }
    
    public List<String> getOptionalSlots() {
        return optionalSlots;
    }
    
    public void setOptionalSlots(List<String> optionalSlots) {
        this.optionalSlots = optionalSlots;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    @Override
    public String toString() {
        return "Subtask{" +
                "subtaskId='" + subtaskId + '\'' +
                ", action='" + action + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", confidenceScore=" + confidenceScore +
                ", executionOrder=" + executionOrder +
                '}';
    }
} 