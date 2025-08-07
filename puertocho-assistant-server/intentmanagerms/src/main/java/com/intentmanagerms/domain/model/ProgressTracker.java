package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio para el seguimiento de progreso de subtareas
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgressTracker {
    
    @JsonProperty("tracker_id")
    private String trackerId;
    
    @JsonProperty("execution_session_id")
    private String executionSessionId;
    
    @JsonProperty("conversation_session_id")
    private String conversationSessionId;
    
    @JsonProperty("state")
    private ProgressState state = ProgressState.INITIALIZED;
    
    @JsonProperty("total_subtasks")
    private int totalSubtasks;
    
    @JsonProperty("completed_subtasks")
    private int completedSubtasks;
    
    @JsonProperty("failed_subtasks")
    private int failedSubtasks;
    
    @JsonProperty("in_progress_subtasks")
    private int inProgressSubtasks;
    
    @JsonProperty("pending_subtasks")
    private int pendingSubtasks;
    
    @JsonProperty("progress_percentage")
    private double progressPercentage = 0.0;
    
    @JsonProperty("overall_confidence")
    private double overallConfidence = 0.0;
    
    @JsonProperty("subtask_progress")
    private List<SubtaskProgress> subtaskProgress;
    
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
    
    @JsonProperty("total_execution_time_ms")
    private long totalExecutionTimeMs;
    
    @JsonProperty("average_execution_time_per_task_ms")
    private long averageExecutionTimePerTaskMs;
    
    @JsonProperty("is_cancelled")
    private boolean isCancelled = false;
    
    @JsonProperty("is_completed")
    private boolean isCompleted = false;
    
    @JsonProperty("completion_message")
    private String completionMessage;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    // Constructor
    public ProgressTracker() {
        this.startedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        this.subtaskProgress = new ArrayList<>();
    }
    
    // Métodos de utilidad para cálculo de progreso
    public void updateProgress() {
        if (totalSubtasks > 0) {
            this.progressPercentage = ((double) completedSubtasks / totalSubtasks) * 100.0;
        }
        
        // Actualizar estado basado en el progreso
        if (completedSubtasks == totalSubtasks && totalSubtasks > 0) {
            this.state = ProgressState.COMPLETED;
            this.isCompleted = true;
            this.completedAt = LocalDateTime.now();
        } else if (failedSubtasks > 0 && (failedSubtasks + completedSubtasks) == totalSubtasks) {
            this.state = ProgressState.FAILED;
        } else if (inProgressSubtasks > 0 || completedSubtasks > 0) {
            this.state = ProgressState.IN_PROGRESS;
        }
        
        this.lastUpdatedAt = LocalDateTime.now();
        
        // Debug log para verificar el cálculo
        System.out.println("DEBUG ProgressTracker.updateProgress() - " +
                          "totalSubtasks: " + totalSubtasks + 
                          ", completedSubtasks: " + completedSubtasks + 
                          ", progressPercentage: " + progressPercentage + 
                          ", state: " + state + 
                          ", isAllCompleted: " + (completedSubtasks == totalSubtasks && totalSubtasks > 0));
    }
    
    public void addSubtaskProgress(SubtaskProgress subtaskProgress) {
        if (this.subtaskProgress == null) {
            this.subtaskProgress = new ArrayList<>();
        }
        this.subtaskProgress.add(subtaskProgress);
        updateProgress();
    }
    
    public void markSubtaskCompleted(String subtaskId) {
        this.completedSubtasks++;
        // Si la subtarea estaba en progreso, decrementar inProgressSubtasks
        if (this.inProgressSubtasks > 0) {
            this.inProgressSubtasks--;
        } else if (this.pendingSubtasks > 0) {
            // Si no estaba en progreso, debe haber estado pendiente
            this.pendingSubtasks--;
        }
        
        // Debug log para verificar el estado de los contadores
        System.out.println("DEBUG markSubtaskCompleted() - " +
                          "subtaskId: " + subtaskId + 
                          ", completedSubtasks: " + completedSubtasks + 
                          ", totalSubtasks: " + totalSubtasks + 
                          ", pendingSubtasks: " + pendingSubtasks + 
                          ", inProgressSubtasks: " + inProgressSubtasks);
        
        updateProgress();
    }
    
    public void markSubtaskFailed(String subtaskId) {
        this.failedSubtasks++;
        // Si la subtarea estaba en progreso, decrementar inProgressSubtasks
        if (this.inProgressSubtasks > 0) {
            this.inProgressSubtasks--;
        } else if (this.pendingSubtasks > 0) {
            // Si no estaba en progreso, debe haber estado pendiente
            this.pendingSubtasks--;
        }
        updateProgress();
    }
    
    public void markSubtaskInProgress(String subtaskId) {
        this.inProgressSubtasks++;
        this.pendingSubtasks = Math.max(0, this.pendingSubtasks - 1);
        updateProgress();
    }
    
    public boolean isAllCompleted() {
        return completedSubtasks == totalSubtasks && totalSubtasks > 0;
    }
    
    public boolean hasFailures() {
        return failedSubtasks > 0;
    }
    
    public boolean canContinue() {
        return !isCancelled && !isCompleted && state != ProgressState.FAILED;
    }
    
    // Getters y Setters
    public String getTrackerId() {
        return trackerId;
    }
    
    public void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
    }
    
    public String getExecutionSessionId() {
        return executionSessionId;
    }
    
    public void setExecutionSessionId(String executionSessionId) {
        this.executionSessionId = executionSessionId;
    }
    
    public String getConversationSessionId() {
        return conversationSessionId;
    }
    
    public void setConversationSessionId(String conversationSessionId) {
        this.conversationSessionId = conversationSessionId;
    }
    
    public ProgressState getState() {
        return state;
    }
    
    public void setState(ProgressState state) {
        this.state = state;
    }
    
    public int getTotalSubtasks() {
        return totalSubtasks;
    }
    
    public void setTotalSubtasks(int totalSubtasks) {
        this.totalSubtasks = totalSubtasks;
    }
    
    public int getCompletedSubtasks() {
        return completedSubtasks;
    }
    
    public void setCompletedSubtasks(int completedSubtasks) {
        this.completedSubtasks = completedSubtasks;
    }
    
    public int getFailedSubtasks() {
        return failedSubtasks;
    }
    
    public void setFailedSubtasks(int failedSubtasks) {
        this.failedSubtasks = failedSubtasks;
    }
    
    public int getInProgressSubtasks() {
        return inProgressSubtasks;
    }
    
    public void setInProgressSubtasks(int inProgressSubtasks) {
        this.inProgressSubtasks = inProgressSubtasks;
    }
    
    public int getPendingSubtasks() {
        return pendingSubtasks;
    }
    
    public void setPendingSubtasks(int pendingSubtasks) {
        this.pendingSubtasks = pendingSubtasks;
    }
    
    public double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public double getOverallConfidence() {
        return overallConfidence;
    }
    
    public void setOverallConfidence(double overallConfidence) {
        this.overallConfidence = overallConfidence;
    }
    
    public List<SubtaskProgress> getSubtaskProgress() {
        return subtaskProgress;
    }
    
    public void setSubtaskProgress(List<SubtaskProgress> subtaskProgress) {
        this.subtaskProgress = subtaskProgress;
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
    
    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }
    
    public void setTotalExecutionTimeMs(long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }
    
    public long getAverageExecutionTimePerTaskMs() {
        return averageExecutionTimePerTaskMs;
    }
    
    public void setAverageExecutionTimePerTaskMs(long averageExecutionTimePerTaskMs) {
        this.averageExecutionTimePerTaskMs = averageExecutionTimePerTaskMs;
    }
    
    public boolean isCancelled() {
        return isCancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
    
    public String getCompletionMessage() {
        return completionMessage;
    }
    
    public void setCompletionMessage(String completionMessage) {
        this.completionMessage = completionMessage;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public enum ProgressState {
        INITIALIZED("initialized", "Progreso inicializado"),
        IN_PROGRESS("in_progress", "Progreso en curso"),
        COMPLETED("completed", "Progreso completado"),
        FAILED("failed", "Progreso fallido"),
        CANCELLED("cancelled", "Progreso cancelado"),
        PAUSED("paused", "Progreso pausado");
        
        private final String value;
        private final String description;
        
        ProgressState(String value, String description) {
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
