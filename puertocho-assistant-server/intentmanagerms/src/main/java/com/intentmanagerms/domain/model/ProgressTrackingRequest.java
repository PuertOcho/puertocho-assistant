package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Modelo de solicitud para el seguimiento de progreso de subtareas
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgressTrackingRequest {
    
    @JsonProperty("execution_session_id")
    private String executionSessionId;
    
    @JsonProperty("conversation_session_id")
    private String conversationSessionId;
    
    @JsonProperty("subtasks")
    private List<Subtask> subtasks;
    
    @JsonProperty("tracking_config")
    private ProgressTrackingConfig trackingConfig;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructor
    public ProgressTrackingRequest() {
    }
    
    // Getters y Setters
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
    
    public List<Subtask> getSubtasks() {
        return subtasks;
    }
    
    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }
    
    public ProgressTrackingConfig getTrackingConfig() {
        return trackingConfig;
    }
    
    public void setTrackingConfig(ProgressTrackingConfig trackingConfig) {
        this.trackingConfig = trackingConfig;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Configuración para el seguimiento de progreso
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProgressTrackingConfig {
        
        @JsonProperty("enable_real_time_tracking")
        private boolean enableRealTimeTracking = true;
        
        @JsonProperty("update_interval_ms")
        private long updateIntervalMs = 1000;
        
        @JsonProperty("enable_notifications")
        private boolean enableNotifications = true;
        
        @JsonProperty("enable_completion_validation")
        private boolean enableCompletionValidation = true;
        
        @JsonProperty("max_tracking_duration_minutes")
        private int maxTrackingDurationMinutes = 30;
        
        @JsonProperty("enable_auto_cleanup")
        private boolean enableAutoCleanup = true;
        
        @JsonProperty("cleanup_interval_minutes")
        private int cleanupIntervalMinutes = 5;
        
        // Constructor
        public ProgressTrackingConfig() {
        }
        
        // Getters y Setters
        public boolean isEnableRealTimeTracking() {
            return enableRealTimeTracking;
        }
        
        public void setEnableRealTimeTracking(boolean enableRealTimeTracking) {
            this.enableRealTimeTracking = enableRealTimeTracking;
        }
        
        public long getUpdateIntervalMs() {
            return updateIntervalMs;
        }
        
        public void setUpdateIntervalMs(long updateIntervalMs) {
            this.updateIntervalMs = updateIntervalMs;
        }
        
        public boolean isEnableNotifications() {
            return enableNotifications;
        }
        
        public void setEnableNotifications(boolean enableNotifications) {
            this.enableNotifications = enableNotifications;
        }
        
        public boolean isEnableCompletionValidation() {
            return enableCompletionValidation;
        }
        
        public void setEnableCompletionValidation(boolean enableCompletionValidation) {
            this.enableCompletionValidation = enableCompletionValidation;
        }
        
        public int getMaxTrackingDurationMinutes() {
            return maxTrackingDurationMinutes;
        }
        
        public void setMaxTrackingDurationMinutes(int maxTrackingDurationMinutes) {
            this.maxTrackingDurationMinutes = maxTrackingDurationMinutes;
        }
        
        public boolean isEnableAutoCleanup() {
            return enableAutoCleanup;
        }
        
        public void setEnableAutoCleanup(boolean enableAutoCleanup) {
            this.enableAutoCleanup = enableAutoCleanup;
        }
        
        public int getCleanupIntervalMinutes() {
            return cleanupIntervalMinutes;
        }
        
        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
            this.cleanupIntervalMinutes = cleanupIntervalMinutes;
        }
    }
}
