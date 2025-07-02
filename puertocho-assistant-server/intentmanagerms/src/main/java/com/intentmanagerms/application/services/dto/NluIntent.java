package com.intentmanagerms.application.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NluIntent {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("confidence")
    private String confidence;
    
    // Constructores
    public NluIntent() {}
    
    public NluIntent(String name, String confidence) {
        this.name = name;
        this.confidence = confidence;
    }
    
    // Getters y Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getConfidence() {
        return confidence;
    }
    
    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
    
    // Método helper para obtener confianza como double
    public double getConfidenceAsDouble() {
        try {
            return Double.parseDouble(confidence);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    @Override
    public String toString() {
        return "NluIntent{" +
                "name='" + name + '\'' +
                ", confidence='" + confidence + '\'' +
                '}';
    }
} 