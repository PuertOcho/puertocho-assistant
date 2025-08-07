package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Modelo de dominio para el plan de ejecución de subtareas.
 * Define cómo se ejecutarán las subtareas según sus dependencias.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionPlan {
    
    @JsonProperty("plan_id")
    private String planId;
    
    @JsonProperty("dependency_levels")
    private List<List<Subtask>> dependencyLevels;
    
    @JsonProperty("parallel_groups")
    private List<List<String>> parallelGroups;
    
    @JsonProperty("execution_order")
    private List<String> executionOrder;
    
    @JsonProperty("estimated_duration_ms")
    private Long estimatedDurationMs;
    
    @JsonProperty("can_execute_parallel")
    private Boolean canExecuteParallel;
    
    @JsonProperty("dependency_graph")
    private Map<String, List<String>> dependencyGraph;
    
    @JsonProperty("critical_path")
    private List<String> criticalPath;
    
    @JsonProperty("optimization_level")
    private String optimizationLevel;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructor
    public ExecutionPlan() {
        this.canExecuteParallel = true;
        this.optimizationLevel = "standard";
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ExecutionPlan plan = new ExecutionPlan();
        
        public Builder planId(String planId) {
            plan.planId = planId;
            return this;
        }
        
        public Builder dependencyLevels(List<List<Subtask>> dependencyLevels) {
            plan.dependencyLevels = dependencyLevels;
            return this;
        }
        
        public Builder parallelGroups(List<List<String>> parallelGroups) {
            plan.parallelGroups = parallelGroups;
            return this;
        }
        
        public Builder executionOrder(List<String> executionOrder) {
            plan.executionOrder = executionOrder;
            return this;
        }
        
        public Builder estimatedDurationMs(Long estimatedDurationMs) {
            plan.estimatedDurationMs = estimatedDurationMs;
            return this;
        }
        
        public Builder canExecuteParallel(Boolean canExecuteParallel) {
            plan.canExecuteParallel = canExecuteParallel;
            return this;
        }
        
        public Builder dependencyGraph(Map<String, List<String>> dependencyGraph) {
            plan.dependencyGraph = dependencyGraph;
            return this;
        }
        
        public Builder criticalPath(List<String> criticalPath) {
            plan.criticalPath = criticalPath;
            return this;
        }
        
        public Builder optimizationLevel(String optimizationLevel) {
            plan.optimizationLevel = optimizationLevel;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            plan.metadata = metadata;
            return this;
        }
        
        public ExecutionPlan build() {
            return plan;
        }
    }
    
    // Métodos de utilidad
    public int getTotalLevels() {
        return dependencyLevels != null ? dependencyLevels.size() : 0;
    }
    
    public int getTotalSubtasks() {
        if (dependencyLevels == null) {
            return 0;
        }
        return dependencyLevels.stream()
                .mapToInt(List::size)
                .sum();
    }
    
    public boolean hasDependencies() {
        return dependencyLevels != null && dependencyLevels.size() > 1;
    }
    
    public boolean canExecuteInParallel() {
        return canExecuteParallel != null && canExecuteParallel;
    }
    
    public List<Subtask> getSubtasksAtLevel(int level) {
        if (dependencyLevels != null && level >= 0 && level < dependencyLevels.size()) {
            return dependencyLevels.get(level);
        }
        return null;
    }
    
    public boolean isCriticalSubtask(String subtaskId) {
        return criticalPath != null && criticalPath.contains(subtaskId);
    }
    
    // Getters y Setters
    public String getPlanId() {
        return planId;
    }
    
    public void setPlanId(String planId) {
        this.planId = planId;
    }
    
    public List<List<Subtask>> getDependencyLevels() {
        return dependencyLevels;
    }
    
    public void setDependencyLevels(List<List<Subtask>> dependencyLevels) {
        this.dependencyLevels = dependencyLevels;
    }
    
    public List<List<String>> getParallelGroups() {
        return parallelGroups;
    }
    
    public void setParallelGroups(List<List<String>> parallelGroups) {
        this.parallelGroups = parallelGroups;
    }
    
    public List<String> getExecutionOrder() {
        return executionOrder;
    }
    
    public void setExecutionOrder(List<String> executionOrder) {
        this.executionOrder = executionOrder;
    }
    
    public Long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }
    
    public void setEstimatedDurationMs(Long estimatedDurationMs) {
        this.estimatedDurationMs = estimatedDurationMs;
    }
    
    public Boolean getCanExecuteParallel() {
        return canExecuteParallel;
    }
    
    public void setCanExecuteParallel(Boolean canExecuteParallel) {
        this.canExecuteParallel = canExecuteParallel;
    }
    
    public Map<String, List<String>> getDependencyGraph() {
        return dependencyGraph;
    }
    
    public void setDependencyGraph(Map<String, List<String>> dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }
    
    public List<String> getCriticalPath() {
        return criticalPath;
    }
    
    public void setCriticalPath(List<String> criticalPath) {
        this.criticalPath = criticalPath;
    }
    
    public String getOptimizationLevel() {
        return optimizationLevel;
    }
    
    public void setOptimizationLevel(String optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "ExecutionPlan{" +
                "planId='" + planId + '\'' +
                ", totalLevels=" + getTotalLevels() +
                ", totalSubtasks=" + getTotalSubtasks() +
                ", canExecuteParallel=" + canExecuteParallel +
                ", estimatedDurationMs=" + estimatedDurationMs +
                '}';
    }
} 