package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Modelo de resultado para la descomposición dinámica de subtareas.
 * Contiene las subtareas generadas, el plan de ejecución y estadísticas del proceso.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubtaskDecompositionResult {
    
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("conversation_session_id")
    private String conversationSessionId;
    
    @JsonProperty("user_message")
    private String userMessage;
    
    @JsonProperty("subtasks")
    private List<Subtask> subtasks;
    
    @JsonProperty("execution_plan")
    private ExecutionPlan executionPlan;
    
    @JsonProperty("decomposition_confidence")
    private Double decompositionConfidence;
    
    @JsonProperty("total_estimated_duration_ms")
    private Long totalEstimatedDurationMs;
    
    @JsonProperty("can_execute_parallel")
    private Boolean canExecuteParallel;
    
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    @JsonProperty("extraction_methods_used")
    private List<String> extractionMethodsUsed;
    
    @JsonProperty("dependencies_detected")
    private Boolean dependenciesDetected;
    
    @JsonProperty("priorities_assigned")
    private Boolean prioritiesAssigned;
    
    @JsonProperty("statistics")
    private Map<String, Object> statistics;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    // Constructores
    public SubtaskDecompositionResult() {
        this.createdAt = LocalDateTime.now();
        this.canExecuteParallel = true;
        this.dependenciesDetected = false;
        this.prioritiesAssigned = false;
    }
    
    public SubtaskDecompositionResult(String requestId, String conversationSessionId, String userMessage) {
        this();
        this.requestId = requestId;
        this.conversationSessionId = conversationSessionId;
        this.userMessage = userMessage;
    }
    
    // Métodos de utilidad
    public int getTotalSubtasks() {
        return subtasks != null ? subtasks.size() : 0;
    }
    
    public int getPendingSubtasks() {
        if (subtasks == null) return 0;
        return (int) subtasks.stream()
                .filter(subtask -> subtask.getStatus() == SubtaskStatus.PENDING)
                .count();
    }
    
    public int getCompletedSubtasks() {
        if (subtasks == null) return 0;
        return (int) subtasks.stream()
                .filter(subtask -> subtask.getStatus() == SubtaskStatus.COMPLETED)
                .count();
    }
    
    public int getFailedSubtasks() {
        if (subtasks == null) return 0;
        return (int) subtasks.stream()
                .filter(subtask -> subtask.getStatus() == SubtaskStatus.FAILED)
                .count();
    }
    
    public boolean isAllSubtasksCompleted() {
        return getCompletedSubtasks() == getTotalSubtasks() && getTotalSubtasks() > 0;
    }
    
    public boolean hasFailedSubtasks() {
        return getFailedSubtasks() > 0;
    }
    
    public double getCompletionPercentage() {
        if (getTotalSubtasks() == 0) return 0.0;
        return (double) getCompletedSubtasks() / getTotalSubtasks() * 100.0;
    }
    
    // Getters y Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getConversationSessionId() {
        return conversationSessionId;
    }
    
    public void setConversationSessionId(String conversationSessionId) {
        this.conversationSessionId = conversationSessionId;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
    
    public List<Subtask> getSubtasks() {
        return subtasks;
    }
    
    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }
    
    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }
    
    public void setExecutionPlan(ExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }
    
    public Double getDecompositionConfidence() {
        return decompositionConfidence;
    }
    
    public void setDecompositionConfidence(Double decompositionConfidence) {
        this.decompositionConfidence = decompositionConfidence;
    }
    
    public Long getTotalEstimatedDurationMs() {
        return totalEstimatedDurationMs;
    }
    
    public void setTotalEstimatedDurationMs(Long totalEstimatedDurationMs) {
        this.totalEstimatedDurationMs = totalEstimatedDurationMs;
    }
    
    public Boolean getCanExecuteParallel() {
        return canExecuteParallel;
    }
    
    public void setCanExecuteParallel(Boolean canExecuteParallel) {
        this.canExecuteParallel = canExecuteParallel;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public List<String> getExtractionMethodsUsed() {
        return extractionMethodsUsed;
    }
    
    public void setExtractionMethodsUsed(List<String> extractionMethodsUsed) {
        this.extractionMethodsUsed = extractionMethodsUsed;
    }
    
    public Boolean getDependenciesDetected() {
        return dependenciesDetected;
    }
    
    public void setDependenciesDetected(Boolean dependenciesDetected) {
        this.dependenciesDetected = dependenciesDetected;
    }
    
    public Boolean getPrioritiesAssigned() {
        return prioritiesAssigned;
    }
    
    public void setPrioritiesAssigned(Boolean prioritiesAssigned) {
        this.prioritiesAssigned = prioritiesAssigned;
    }
    
    public Map<String, Object> getStatistics() {
        return statistics;
    }
    
    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "SubtaskDecompositionResult{" +
                "requestId='" + requestId + '\'' +
                ", conversationSessionId='" + conversationSessionId + '\'' +
                ", userMessage='" + userMessage + '\'' +
                ", totalSubtasks=" + getTotalSubtasks() +
                ", completedSubtasks=" + getCompletedSubtasks() +
                ", failedSubtasks=" + getFailedSubtasks() +
                ", decompositionConfidence=" + decompositionConfidence +
                ", canExecuteParallel=" + canExecuteParallel +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
    
    /**
     * Clase interna que representa el plan de ejecución de las subtareas.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExecutionPlan {
        
        @JsonProperty("plan_id")
        private String planId;
        
        @JsonProperty("execution_steps")
        private List<ExecutionStep> executionSteps;
        
        @JsonProperty("parallel_groups")
        private List<List<String>> parallelGroups;
        
        @JsonProperty("estimated_total_duration_ms")
        private Long estimatedTotalDurationMs;
        
        @JsonProperty("max_parallel_tasks")
        private Integer maxParallelTasks;
        
        @JsonProperty("optimization_level")
        private String optimizationLevel;
        
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        
        // Constructores
        public ExecutionPlan() {
            this.createdAt = LocalDateTime.now();
            this.maxParallelTasks = 3;
            this.optimizationLevel = "balanced";
        }
        
        // Getters y Setters
        public String getPlanId() {
            return planId;
        }
        
        public void setPlanId(String planId) {
            this.planId = planId;
        }
        
        public List<ExecutionStep> getExecutionSteps() {
            return executionSteps;
        }
        
        public void setExecutionSteps(List<ExecutionStep> executionSteps) {
            this.executionSteps = executionSteps;
        }
        
        public List<List<String>> getParallelGroups() {
            return parallelGroups;
        }
        
        public void setParallelGroups(List<List<String>> parallelGroups) {
            this.parallelGroups = parallelGroups;
        }
        
        public Long getEstimatedTotalDurationMs() {
            return estimatedTotalDurationMs;
        }
        
        public void setEstimatedTotalDurationMs(Long estimatedTotalDurationMs) {
            this.estimatedTotalDurationMs = estimatedTotalDurationMs;
        }
        
        public Integer getMaxParallelTasks() {
            return maxParallelTasks;
        }
        
        public void setMaxParallelTasks(Integer maxParallelTasks) {
            this.maxParallelTasks = maxParallelTasks;
        }
        
        public String getOptimizationLevel() {
            return optimizationLevel;
        }
        
        public void setOptimizationLevel(String optimizationLevel) {
            this.optimizationLevel = optimizationLevel;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
    
    /**
     * Clase interna que representa un paso de ejecución en el plan.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExecutionStep {
        
        @JsonProperty("step_id")
        private String stepId;
        
        @JsonProperty("step_order")
        private Integer stepOrder;
        
        @JsonProperty("subtask_ids")
        private List<String> subtaskIds;
        
        @JsonProperty("execution_type")
        private String executionType; // "sequential" o "parallel"
        
        @JsonProperty("estimated_duration_ms")
        private Long estimatedDurationMs;
        
        @JsonProperty("dependencies")
        private List<String> dependencies;
        
        @JsonProperty("description")
        private String description;
        
        // Constructores
        public ExecutionStep() {
            this.executionType = "sequential";
        }
        
        public ExecutionStep(Integer stepOrder, List<String> subtaskIds) {
            this();
            this.stepOrder = stepOrder;
            this.subtaskIds = subtaskIds;
        }
        
        // Getters y Setters
        public String getStepId() {
            return stepId;
        }
        
        public void setStepId(String stepId) {
            this.stepId = stepId;
        }
        
        public Integer getStepOrder() {
            return stepOrder;
        }
        
        public void setStepOrder(Integer stepOrder) {
            this.stepOrder = stepOrder;
        }
        
        public List<String> getSubtaskIds() {
            return subtaskIds;
        }
        
        public void setSubtaskIds(List<String> subtaskIds) {
            this.subtaskIds = subtaskIds;
        }
        
        public String getExecutionType() {
            return executionType;
        }
        
        public void setExecutionType(String executionType) {
            this.executionType = executionType;
        }
        
        public Long getEstimatedDurationMs() {
            return estimatedDurationMs;
        }
        
        public void setEstimatedDurationMs(Long estimatedDurationMs) {
            this.estimatedDurationMs = estimatedDurationMs;
        }
        
        public List<String> getDependencies() {
            return dependencies;
        }
        
        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
} 