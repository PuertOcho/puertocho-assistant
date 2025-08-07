package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Modelo de dominio para el resultado de ejecución de una subtarea individual.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubtaskExecutionResult {
    
    @JsonProperty("subtask_id")
    private String subtaskId;
    
    @JsonProperty("action")
    private String action;
    
    @JsonProperty("status")
    private SubtaskStatus status;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("execution_time_ms")
    private Long executionTimeMs;
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("critical_error")
    private Boolean criticalError;
    
    @JsonProperty("retry_count")
    private Integer retryCount;
    
    @JsonProperty("metadata")
    private Object metadata;
    
    @JsonProperty("started_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;
    
    @JsonProperty("completed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
    
    // Constructor
    public SubtaskExecutionResult() {
        this.success = false;
        this.criticalError = false;
        this.retryCount = 0;
        this.startedAt = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private SubtaskExecutionResult result = new SubtaskExecutionResult();
        
        public Builder subtaskId(String subtaskId) {
            result.subtaskId = subtaskId;
            return this;
        }
        
        public Builder action(String action) {
            result.action = action;
            return this;
        }
        
        public Builder status(SubtaskStatus status) {
            result.status = status;
            return this;
        }
        
        public Builder result(Object resultValue) {
            result.result = resultValue;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }
        
        public Builder executionTimeMs(Long executionTimeMs) {
            result.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public Builder success(Boolean success) {
            result.success = success;
            return this;
        }
        
        public Builder criticalError(Boolean criticalError) {
            result.criticalError = criticalError;
            return this;
        }
        
        public Builder retryCount(Integer retryCount) {
            result.retryCount = retryCount;
            return this;
        }
        
        public Builder metadata(Object metadata) {
            result.metadata = metadata;
            return this;
        }
        
        public Builder startedAt(LocalDateTime startedAt) {
            result.startedAt = startedAt;
            return this;
        }
        
        public Builder completedAt(LocalDateTime completedAt) {
            result.completedAt = completedAt;
            return this;
        }
        
        public SubtaskExecutionResult build() {
            result.completedAt = LocalDateTime.now();
            return result;
        }
    }
    
    // Métodos de utilidad
    public boolean isSuccess() {
        return success != null && success;
    }
    
    public boolean isCriticalError() {
        return criticalError != null && criticalError;
    }
    
    public boolean isFailed() {
        return !isSuccess();
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
    
    public SubtaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(SubtaskStatus status) {
        this.status = status;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public Boolean getCriticalError() {
        return criticalError;
    }
    
    public void setCriticalError(Boolean criticalError) {
        this.criticalError = criticalError;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Object getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
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
    
    @Override
    public String toString() {
        return "SubtaskExecutionResult{" +
                "subtaskId='" + subtaskId + '\'' +
                ", action='" + action + '\'' +
                ", status=" + status +
                ", success=" + success +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
} 