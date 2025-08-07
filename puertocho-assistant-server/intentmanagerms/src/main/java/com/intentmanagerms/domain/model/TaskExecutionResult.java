package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio para el resultado completo de ejecución de tareas.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskExecutionResult {
    
    @JsonProperty("execution_id")
    private String executionId;
    
    @JsonProperty("conversation_session_id")
    private String conversationSessionId;
    
    @JsonProperty("total_tasks")
    private Integer totalTasks;
    
    @JsonProperty("successful_tasks")
    private Integer successfulTasks;
    
    @JsonProperty("failed_tasks")
    private Integer failedTasks;
    
    @JsonProperty("all_successful")
    private Boolean allSuccessful;
    
    @JsonProperty("total_execution_time_ms")
    private Long totalExecutionTimeMs;
    
    @JsonProperty("results")
    private List<SubtaskExecutionResult> results;
    
    @JsonProperty("execution_plan")
    private ExecutionPlan executionPlan;
    
    @JsonProperty("statistics")
    private Map<String, Object> statistics;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonProperty("completed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
    
    // Constructor
    public TaskExecutionResult() {
        this.createdAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private TaskExecutionResult result = new TaskExecutionResult();
        
        public Builder executionId(String executionId) {
            result.executionId = executionId;
            return this;
        }
        
        public Builder conversationSessionId(String conversationSessionId) {
            result.conversationSessionId = conversationSessionId;
            return this;
        }
        
        public Builder totalTasks(Integer totalTasks) {
            result.totalTasks = totalTasks;
            return this;
        }
        
        public Builder successfulTasks(Integer successfulTasks) {
            result.successfulTasks = successfulTasks;
            return this;
        }
        
        public Builder failedTasks(Integer failedTasks) {
            result.failedTasks = failedTasks;
            return this;
        }
        
        public Builder allSuccessful(Boolean allSuccessful) {
            result.allSuccessful = allSuccessful;
            return this;
        }
        
        public Builder totalExecutionTimeMs(Long totalExecutionTimeMs) {
            result.totalExecutionTimeMs = totalExecutionTimeMs;
            return this;
        }
        
        public Builder results(List<SubtaskExecutionResult> results) {
            result.results = results;
            return this;
        }
        
        public Builder executionPlan(ExecutionPlan executionPlan) {
            result.executionPlan = executionPlan;
            return this;
        }
        
        public Builder statistics(Map<String, Object> statistics) {
            result.statistics = statistics;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            result.createdAt = createdAt;
            return this;
        }
        
        public Builder completedAt(LocalDateTime completedAt) {
            result.completedAt = completedAt;
            return this;
        }
        
        public TaskExecutionResult build() {
            return result;
        }
    }
    
    // Métodos de utilidad
    public boolean isSuccessful() {
        return allSuccessful != null && allSuccessful;
    }
    
    public boolean hasErrors() {
        return failedTasks != null && failedTasks > 0;
    }
    
    public double getSuccessRate() {
        if (totalTasks == null || totalTasks == 0) {
            return 0.0;
        }
        return (double) successfulTasks / totalTasks;
    }
    
    // Getters y Setters
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public String getConversationSessionId() {
        return conversationSessionId;
    }
    
    public void setConversationSessionId(String conversationSessionId) {
        this.conversationSessionId = conversationSessionId;
    }
    
    public Integer getTotalTasks() {
        return totalTasks;
    }
    
    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }
    
    public Integer getSuccessfulTasks() {
        return successfulTasks;
    }
    
    public void setSuccessfulTasks(Integer successfulTasks) {
        this.successfulTasks = successfulTasks;
    }
    
    public Integer getFailedTasks() {
        return failedTasks;
    }
    
    public void setFailedTasks(Integer failedTasks) {
        this.failedTasks = failedTasks;
    }
    
    public Boolean getAllSuccessful() {
        return allSuccessful;
    }
    
    public void setAllSuccessful(Boolean allSuccessful) {
        this.allSuccessful = allSuccessful;
    }
    
    public Long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }
    
    public void setTotalExecutionTimeMs(Long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }
    
    public List<SubtaskExecutionResult> getResults() {
        return results;
    }
    
    public void setResults(List<SubtaskExecutionResult> results) {
        this.results = results;
    }
    
    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }
    
    public void setExecutionPlan(ExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }
    
    public Map<String, Object> getStatistics() {
        return statistics;
    }
    
    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    @Override
    public String toString() {
        return "TaskExecutionResult{" +
                "executionId='" + executionId + '\'' +
                ", totalTasks=" + totalTasks +
                ", successfulTasks=" + successfulTasks +
                ", failedTasks=" + failedTasks +
                ", allSuccessful=" + allSuccessful +
                ", totalExecutionTimeMs=" + totalExecutionTimeMs +
                '}';
    }
} 