package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modelo de dominio para una sesión de ejecución de tareas.
 * Mantiene el estado y progreso de una ejecución de subtareas.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskExecutionSession {
    
    @JsonProperty("execution_id")
    private String executionId;
    
    @JsonProperty("conversation_session_id")
    private String conversationSessionId;
    
    @JsonProperty("subtasks")
    private List<Subtask> subtasks;
    
    @JsonProperty("total_subtasks")
    private Integer totalSubtasks;
    
    @JsonProperty("completed_subtasks")
    private Integer completedSubtasks;
    
    @JsonProperty("progress")
    private Double progress;
    
    @JsonProperty("current_level")
    private Integer currentLevel;
    
    @JsonProperty("execution_plan")
    private ExecutionPlan executionPlan;
    
    @JsonProperty("subtask_statuses")
    private Map<String, SubtaskStatus> subtaskStatuses;
    
    @JsonProperty("level_results")
    private Map<Integer, List<SubtaskExecutionResult>> levelResults;
    
    @JsonProperty("is_cancelled")
    private Boolean isCancelled;
    
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
    
    // Constructor
    public TaskExecutionSession() {
        this.subtaskStatuses = new ConcurrentHashMap<>();
        this.levelResults = new ConcurrentHashMap<>();
        this.isCancelled = false;
        this.progress = 0.0;
        this.completedSubtasks = 0;
        this.currentLevel = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private TaskExecutionSession session = new TaskExecutionSession();
        
        public Builder executionId(String executionId) {
            session.executionId = executionId;
            return this;
        }
        
        public Builder conversationSessionId(String conversationSessionId) {
            session.conversationSessionId = conversationSessionId;
            return this;
        }
        
        public Builder subtasks(List<Subtask> subtasks) {
            session.subtasks = subtasks;
            return this;
        }
        
        public Builder totalSubtasks(Integer totalSubtasks) {
            session.totalSubtasks = totalSubtasks;
            return this;
        }
        
        public Builder completedSubtasks(Integer completedSubtasks) {
            session.completedSubtasks = completedSubtasks;
            return this;
        }
        
        public Builder progress(Double progress) {
            session.progress = progress;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            session.createdAt = createdAt;
            return this;
        }
        
        public TaskExecutionSession build() {
            return session;
        }
    }
    
    // Métodos de utilidad
    public void updateSubtaskStatus(String subtaskId, SubtaskStatus status) {
        this.subtaskStatuses.put(subtaskId, status);
        this.updatedAt = LocalDateTime.now();
        
        if (status == SubtaskStatus.COMPLETED) {
            this.completedSubtasks++;
        }
    }
    
    public void updateProgress(Double progress, Integer level, List<SubtaskExecutionResult> levelResults) {
        this.progress = progress;
        this.currentLevel = level;
        this.levelResults.put(level, levelResults);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void start() {
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void complete() {
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.progress = 100.0;
    }
    
    public void cancel() {
        this.isCancelled = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return !isCancelled && completedAt == null;
    }
    
    public boolean isCompleted() {
        return completedAt != null;
    }
    
    public boolean isCancelled() {
        return isCancelled;
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
    
    public List<Subtask> getSubtasks() {
        return subtasks;
    }
    
    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }
    
    public Integer getTotalSubtasks() {
        return totalSubtasks;
    }
    
    public void setTotalSubtasks(Integer totalSubtasks) {
        this.totalSubtasks = totalSubtasks;
    }
    
    public Integer getCompletedSubtasks() {
        return completedSubtasks;
    }
    
    public void setCompletedSubtasks(Integer completedSubtasks) {
        this.completedSubtasks = completedSubtasks;
    }
    
    public Double getProgress() {
        return progress;
    }
    
    public void setProgress(Double progress) {
        this.progress = progress;
    }
    
    public Integer getCurrentLevel() {
        return currentLevel;
    }
    
    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }
    
    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }
    
    public void setExecutionPlan(ExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }
    
    public Map<String, SubtaskStatus> getSubtaskStatuses() {
        return subtaskStatuses;
    }
    
    public void setSubtaskStatuses(Map<String, SubtaskStatus> subtaskStatuses) {
        this.subtaskStatuses = subtaskStatuses;
    }
    
    public Map<Integer, List<SubtaskExecutionResult>> getLevelResults() {
        return levelResults;
    }
    
    public void setLevelResults(Map<Integer, List<SubtaskExecutionResult>> levelResults) {
        this.levelResults = levelResults;
    }
    
    public Boolean getIsCancelled() {
        return isCancelled;
    }
    
    public void setIsCancelled(Boolean isCancelled) {
        this.isCancelled = isCancelled;
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
    
    @Override
    public String toString() {
        return "TaskExecutionSession{" +
                "executionId='" + executionId + '\'' +
                ", progress=" + progress +
                ", completedSubtasks=" + completedSubtasks +
                ", totalSubtasks=" + totalSubtasks +
                ", isCancelled=" + isCancelled +
                '}';
    }
} 