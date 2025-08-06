package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Modelo de solicitud para la descomposición dinámica de subtareas.
 * Contiene toda la información necesaria para analizar una petición compleja
 * y descomponerla en múltiples subtareas ejecutables.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubtaskDecompositionRequest {
    
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("user_message")
    private String userMessage;
    
    @JsonProperty("conversation_session_id")
    private String conversationSessionId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("context")
    private Map<String, Object> context;
    
    @JsonProperty("available_actions")
    private List<String> availableActions;
    
    @JsonProperty("max_subtasks")
    private Integer maxSubtasks;
    
    @JsonProperty("enable_dependency_detection")
    private Boolean enableDependencyDetection;
    
    @JsonProperty("enable_priority_assignment")
    private Boolean enablePriorityAssignment;
    
    @JsonProperty("enable_parallel_execution")
    private Boolean enableParallelExecution;
    
    @JsonProperty("confidence_threshold")
    private Double confidenceThreshold;
    
    @JsonProperty("extraction_methods")
    private List<String> extractionMethods;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructores
    public SubtaskDecompositionRequest() {
        this.maxSubtasks = 10;
        this.enableDependencyDetection = true;
        this.enablePriorityAssignment = true;
        this.enableParallelExecution = true;
        this.confidenceThreshold = 0.7;
        this.language = "es";
    }
    
    public SubtaskDecompositionRequest(String userMessage, String conversationSessionId) {
        this();
        this.userMessage = userMessage;
        this.conversationSessionId = conversationSessionId;
    }
    
    // Getters y Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
    
    public String getConversationSessionId() {
        return conversationSessionId;
    }
    
    public void setConversationSessionId(String conversationSessionId) {
        this.conversationSessionId = conversationSessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    public List<String> getAvailableActions() {
        return availableActions;
    }
    
    public void setAvailableActions(List<String> availableActions) {
        this.availableActions = availableActions;
    }
    
    public Integer getMaxSubtasks() {
        return maxSubtasks;
    }
    
    public void setMaxSubtasks(Integer maxSubtasks) {
        this.maxSubtasks = maxSubtasks;
    }
    
    public Boolean getEnableDependencyDetection() {
        return enableDependencyDetection;
    }
    
    public void setEnableDependencyDetection(Boolean enableDependencyDetection) {
        this.enableDependencyDetection = enableDependencyDetection;
    }
    
    public Boolean getEnablePriorityAssignment() {
        return enablePriorityAssignment;
    }
    
    public void setEnablePriorityAssignment(Boolean enablePriorityAssignment) {
        this.enablePriorityAssignment = enablePriorityAssignment;
    }
    
    public Boolean getEnableParallelExecution() {
        return enableParallelExecution;
    }
    
    public void setEnableParallelExecution(Boolean enableParallelExecution) {
        this.enableParallelExecution = enableParallelExecution;
    }
    
    public Double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    public void setConfidenceThreshold(Double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
    
    public List<String> getExtractionMethods() {
        return extractionMethods;
    }
    
    public void setExtractionMethods(List<String> extractionMethods) {
        this.extractionMethods = extractionMethods;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "SubtaskDecompositionRequest{" +
                "requestId='" + requestId + '\'' +
                ", userMessage='" + userMessage + '\'' +
                ", conversationSessionId='" + conversationSessionId + '\'' +
                ", maxSubtasks=" + maxSubtasks +
                ", enableDependencyDetection=" + enableDependencyDetection +
                ", enablePriorityAssignment=" + enablePriorityAssignment +
                ", enableParallelExecution=" + enableParallelExecution +
                ", confidenceThreshold=" + confidenceThreshold +
                ", language='" + language + '\'' +
                '}';
    }
} 