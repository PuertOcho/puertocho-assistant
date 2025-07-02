package com.intentmanagerms.application.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NluIntentRanking {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("confidence")
    private String confidence;
    
    @JsonProperty("utterance")
    private String utterance;
    
    // Constructores
    public NluIntentRanking() {}
    
    public NluIntentRanking(String name, String confidence, String utterance) {
        this.name = name;
        this.confidence = confidence;
        this.utterance = utterance;
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
    
    public String getUtterance() {
        return utterance;
    }
    
    public void setUtterance(String utterance) {
        this.utterance = utterance;
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
        return "NluIntentRanking{" +
                "name='" + name + '\'' +
                ", confidence='" + confidence + '\'' +
                ", utterance='" + utterance + '\'' +
                '}';
    }
} 