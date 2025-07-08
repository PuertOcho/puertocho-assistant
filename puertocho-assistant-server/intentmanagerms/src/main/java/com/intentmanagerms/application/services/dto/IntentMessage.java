package com.intentmanagerms.application.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class IntentMessage {
    @JsonProperty("intent")
    private IntentInfo intent;
    
    @JsonProperty("entities")
    private List<EntityInfo> entities;
    
    @JsonProperty("intent_ranking")
    private List<IntentRankingInfo> intentRanking;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("missing_entities")
    private List<String> missingEntities;
    
    // Constructores
    public IntentMessage() {}
    
    public IntentMessage(IntentInfo intent, List<EntityInfo> entities, 
                     List<IntentRankingInfo> intentRanking, String text) {
        this.intent = intent;
        this.entities = entities;
        this.intentRanking = intentRanking;
        this.text = text;
    }
    
    public IntentInfo getIntent() {
        return intent;
    }
    
    public void setIntent(IntentInfo intent) {
        this.intent = intent;
    }
    
    public List<EntityInfo> getEntities() {
        return entities;
    }
    
    public void setEntities(List<EntityInfo> entities) {
        this.entities = entities;
    }
    
    public List<IntentRankingInfo> getIntentRanking() {
        return intentRanking;
    }
    
    public void setIntentRanking(List<IntentRankingInfo> intentRanking) {
        this.intentRanking = intentRanking;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<String> getMissingEntities() {
        return missingEntities;
    }
    
    public void setMissingEntities(List<String> missingEntities) {
        this.missingEntities = missingEntities;
    }
    
    @Override
    public String toString() {
        return "IntentMessage{" +
                "intent=" + intent +
                ", entities=" + entities +
                ", intentRanking=" + intentRanking +
                ", text='" + text + '\'' +
                ", status='" + status + '\'' +
                ", missingEntities=" + missingEntities +
                '}';
    }
} 