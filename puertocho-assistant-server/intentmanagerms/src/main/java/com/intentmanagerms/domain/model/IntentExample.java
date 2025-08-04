package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Representa un ejemplo de intención con sus ejemplos de texto y configuración
 */
public class IntentExample {
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("examples")
    private List<String> examples;
    
    @JsonProperty("required_entities")
    private List<String> requiredEntities;
    
    @JsonProperty("optional_entities")
    private List<String> optionalEntities;
    
    @JsonProperty("mcp_action")
    private String mcpAction;
    
    @JsonProperty("slot_filling_questions")
    private Map<String, String> slotFillingQuestions;
    
    @JsonProperty("simple_response")
    private String simpleResponse;
    
    @JsonProperty("expert_domain")
    private String expertDomain;
    
    @JsonProperty("confidence_threshold")
    private Double confidenceThreshold;
    
    @JsonProperty("max_examples_for_rag")
    private Integer maxExamplesForRag;
    
    // Constructor por defecto
    public IntentExample() {}
    
    // Constructor con parámetros principales
    public IntentExample(String description, List<String> examples, String mcpAction) {
        this.description = description;
        this.examples = examples;
        this.mcpAction = mcpAction;
    }
    
    // Getters y Setters
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getExamples() {
        return examples;
    }
    
    public void setExamples(List<String> examples) {
        this.examples = examples;
    }
    
    public List<String> getRequiredEntities() {
        return requiredEntities;
    }
    
    public void setRequiredEntities(List<String> requiredEntities) {
        this.requiredEntities = requiredEntities;
    }
    
    public List<String> getOptionalEntities() {
        return optionalEntities;
    }
    
    public void setOptionalEntities(List<String> optionalEntities) {
        this.optionalEntities = optionalEntities;
    }
    
    public String getMcpAction() {
        return mcpAction;
    }
    
    public void setMcpAction(String mcpAction) {
        this.mcpAction = mcpAction;
    }
    
    public Map<String, String> getSlotFillingQuestions() {
        return slotFillingQuestions;
    }
    
    public void setSlotFillingQuestions(Map<String, String> slotFillingQuestions) {
        this.slotFillingQuestions = slotFillingQuestions;
    }
    
    public String getSimpleResponse() {
        return simpleResponse;
    }
    
    public void setSimpleResponse(String simpleResponse) {
        this.simpleResponse = simpleResponse;
    }
    
    public String getExpertDomain() {
        return expertDomain;
    }
    
    public void setExpertDomain(String expertDomain) {
        this.expertDomain = expertDomain;
    }
    
    public Double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    public void setConfidenceThreshold(Double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
    
    public Integer getMaxExamplesForRag() {
        return maxExamplesForRag;
    }
    
    public void setMaxExamplesForRag(Integer maxExamplesForRag) {
        this.maxExamplesForRag = maxExamplesForRag;
    }
    
    /**
     * Obtiene el número total de ejemplos disponibles
     */
    public int getExampleCount() {
        return examples != null ? examples.size() : 0;
    }
    
    /**
     * Obtiene el número total de entidades requeridas
     */
    public int getRequiredEntityCount() {
        return requiredEntities != null ? requiredEntities.size() : 0;
    }
    
    /**
     * Verifica si la intención tiene una respuesta simple
     */
    public boolean hasSimpleResponse() {
        return simpleResponse != null && !simpleResponse.trim().isEmpty();
    }
    
    /**
     * Obtiene ejemplos limitados para RAG (para evitar prompts muy largos)
     */
    public List<String> getExamplesForRag() {
        if (examples == null || examples.isEmpty()) {
            return List.of();
        }
        
        int maxExamples = maxExamplesForRag != null ? maxExamplesForRag : 5;
        return examples.stream()
                .limit(maxExamples)
                .toList();
    }
    
    /**
     * Obtiene todas las entidades (requeridas + opcionales)
     */
    public List<String> getAllEntities() {
        List<String> allEntities = new java.util.ArrayList<>();
        
        if (requiredEntities != null) {
            allEntities.addAll(requiredEntities);
        }
        
        if (optionalEntities != null) {
            allEntities.addAll(optionalEntities);
        }
        
        return allEntities;
    }
    
    @Override
    public String toString() {
        return "IntentExample{" +
                "description='" + description + '\'' +
                ", examples=" + examples +
                ", requiredEntities=" + requiredEntities +
                ", mcpAction='" + mcpAction + '\'' +
                ", hasSimpleResponse=" + hasSimpleResponse() +
                '}';
    }
} 