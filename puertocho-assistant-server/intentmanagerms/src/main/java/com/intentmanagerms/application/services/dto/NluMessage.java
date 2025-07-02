package com.intentmanagerms.application.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class NluMessage {
    @JsonProperty("intent")
    private NluIntent intent;
    
    @JsonProperty("entities")
    private List<NluEntity> entities;
    
    @JsonProperty("intent_ranking")
    private List<NluIntentRanking> intentRanking;
    
    @JsonProperty("text")
    private String text;
    
    // Constructores
    public NluMessage() {}
    
    public NluMessage(NluIntent intent, List<NluEntity> entities, 
                     List<NluIntentRanking> intentRanking, String text) {
        this.intent = intent;
        this.entities = entities;
        this.intentRanking = intentRanking;
        this.text = text;
    }
    
    // Getters y Setters
    public NluIntent getIntent() {
        return intent;
    }
    
    public void setIntent(NluIntent intent) {
        this.intent = intent;
    }
    
    public List<NluEntity> getEntities() {
        return entities;
    }
    
    public void setEntities(List<NluEntity> entities) {
        this.entities = entities;
    }
    
    public List<NluIntentRanking> getIntentRanking() {
        return intentRanking;
    }
    
    public void setIntentRanking(List<NluIntentRanking> intentRanking) {
        this.intentRanking = intentRanking;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return "NluMessage{" +
                "intent=" + intent +
                ", entities=" + entities +
                ", intentRanking=" + intentRanking +
                ", text='" + text + '\'' +
                '}';
    }
} 