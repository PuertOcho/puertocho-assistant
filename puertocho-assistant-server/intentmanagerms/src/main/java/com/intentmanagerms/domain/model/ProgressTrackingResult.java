package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Modelo de resultado para el seguimiento de progreso de subtareas
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgressTrackingResult {
    
    @JsonProperty("tracker_id")
    private String trackerId;
    
    @JsonProperty("execution_session_id")
    private String executionSessionId;
    
    @JsonProperty("conversation_session_id")
    private String conversationSessionId;
    
    @JsonProperty("progress_tracker")
    private ProgressTracker progressTracker;
    
    @JsonProperty("completion_status")
    private CompletionStatus completionStatus;
    
    @JsonProperty("statistics")
    private ProgressStatistics statistics;
    
    @JsonProperty("notifications")
    private List<ProgressNotification> notifications;
    
    @JsonProperty("processing_time_ms")
    private long processingTimeMs;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Constructor
    public ProgressTrackingResult() {
        this.createdAt = LocalDateTime.now();
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
    
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }
    
    public void setProgressTracker(ProgressTracker progressTracker) {
        this.progressTracker = progressTracker;
    }
    
    public CompletionStatus getCompletionStatus() {
        return completionStatus;
    }
    
    public void setCompletionStatus(CompletionStatus completionStatus) {
        this.completionStatus = completionStatus;
    }
    
    public ProgressStatistics getStatistics() {
        return statistics;
    }
    
    public void setStatistics(ProgressStatistics statistics) {
        this.statistics = statistics;
    }
    
    public List<ProgressNotification> getNotifications() {
        return notifications;
    }
    
    public void setNotifications(List<ProgressNotification> notifications) {
        this.notifications = notifications;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Estado de completitud del progreso
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompletionStatus {
        
        @JsonProperty("is_completed")
        private boolean isCompleted;
        
        @JsonProperty("completion_percentage")
        private double completionPercentage;
        
        @JsonProperty("remaining_subtasks")
        private int remainingSubtasks;
        
        @JsonProperty("estimated_completion_time_ms")
        private long estimatedCompletionTimeMs;
        
        @JsonProperty("completion_message")
        private String completionMessage;
        
        @JsonProperty("validation_errors")
        private List<String> validationErrors;
        
        // Constructor
        public CompletionStatus() {
        }
        
        // Getters y Setters
        public boolean isCompleted() {
            return isCompleted;
        }
        
        public void setCompleted(boolean completed) {
            isCompleted = completed;
        }
        
        public double getCompletionPercentage() {
            return completionPercentage;
        }
        
        public void setCompletionPercentage(double completionPercentage) {
            this.completionPercentage = completionPercentage;
        }
        
        public int getRemainingSubtasks() {
            return remainingSubtasks;
        }
        
        public void setRemainingSubtasks(int remainingSubtasks) {
            this.remainingSubtasks = remainingSubtasks;
        }
        
        public long getEstimatedCompletionTimeMs() {
            return estimatedCompletionTimeMs;
        }
        
        public void setEstimatedCompletionTimeMs(long estimatedCompletionTimeMs) {
            this.estimatedCompletionTimeMs = estimatedCompletionTimeMs;
        }
        
        public String getCompletionMessage() {
            return completionMessage;
        }
        
        public void setCompletionMessage(String completionMessage) {
            this.completionMessage = completionMessage;
        }
        
        public List<String> getValidationErrors() {
            return validationErrors;
        }
        
        public void setValidationErrors(List<String> validationErrors) {
            this.validationErrors = validationErrors;
        }
    }
    
    /**
     * Estadísticas del progreso
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProgressStatistics {
        
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
        
        @JsonProperty("average_execution_time_ms")
        private long averageExecutionTimeMs;
        
        @JsonProperty("total_execution_time_ms")
        private long totalExecutionTimeMs;
        
        @JsonProperty("success_rate")
        private double successRate;
        
        @JsonProperty("failure_rate")
        private double failureRate;
        
        @JsonProperty("progress_rate_per_minute")
        private double progressRatePerMinute;
        
        // Constructor
        public ProgressStatistics() {
        }
        
        // Getters y Setters
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
        
        public long getAverageExecutionTimeMs() {
            return averageExecutionTimeMs;
        }
        
        public void setAverageExecutionTimeMs(long averageExecutionTimeMs) {
            this.averageExecutionTimeMs = averageExecutionTimeMs;
        }
        
        public long getTotalExecutionTimeMs() {
            return totalExecutionTimeMs;
        }
        
        public void setTotalExecutionTimeMs(long totalExecutionTimeMs) {
            this.totalExecutionTimeMs = totalExecutionTimeMs;
        }
        
        public double getSuccessRate() {
            return successRate;
        }
        
        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }
        
        public double getFailureRate() {
            return failureRate;
        }
        
        public void setFailureRate(double failureRate) {
            this.failureRate = failureRate;
        }
        
        public double getProgressRatePerMinute() {
            return progressRatePerMinute;
        }
        
        public void setProgressRatePerMinute(double progressRatePerMinute) {
            this.progressRatePerMinute = progressRatePerMinute;
        }
    }
    
    /**
     * Notificación de progreso
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProgressNotification {
        
        @JsonProperty("notification_id")
        private String notificationId;
        
        @JsonProperty("type")
        private NotificationType type;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("subtask_id")
        private String subtaskId;
        
        @JsonProperty("progress_percentage")
        private double progressPercentage;
        
        @JsonProperty("timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;
        
        @JsonProperty("metadata")
        private Map<String, Object> metadata;
        
        // Constructor
        public ProgressNotification() {
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters y Setters
        public String getNotificationId() {
            return notificationId;
        }
        
        public void setNotificationId(String notificationId) {
            this.notificationId = notificationId;
        }
        
        public NotificationType getType() {
            return type;
        }
        
        public void setType(NotificationType type) {
            this.type = type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getSubtaskId() {
            return subtaskId;
        }
        
        public void setSubtaskId(String subtaskId) {
            this.subtaskId = subtaskId;
        }
        
        public double getProgressPercentage() {
            return progressPercentage;
        }
        
        public void setProgressPercentage(double progressPercentage) {
            this.progressPercentage = progressPercentage;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
        
        public enum NotificationType {
            PROGRESS_UPDATE("progress_update", "Actualización de progreso"),
            SUBTASK_STARTED("subtask_started", "Subtarea iniciada"),
            SUBTASK_COMPLETED("subtask_completed", "Subtarea completada"),
            SUBTASK_FAILED("subtask_failed", "Subtarea fallida"),
            COMPLETION_REACHED("completion_reached", "Completitud alcanzada"),
            ERROR_OCCURRED("error_occurred", "Error ocurrido"),
            WARNING("warning", "Advertencia");
            
            private final String value;
            private final String description;
            
            NotificationType(String value, String description) {
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
}
