package com.intentmanagerms.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Solicitud de llenado de slots para una intención específica.
 * Contiene información contextual necesaria para el slot-filling dinámico.
 */
public class SlotFillingRequest {

    private String intentId;
    private String userMessage;
    private String sessionId;
    private Map<String, Object> currentSlots;
    private List<String> requiredEntities;
    private List<String> optionalEntities;
    private Map<String, Object> conversationContext;
    private Map<String, Object> userPreferences;
    private boolean enableDynamicQuestions = true;
    private int maxSlotAttempts = 3;
    private double confidenceThreshold = 0.7;

    public SlotFillingRequest() {}

    public SlotFillingRequest(String intentId, String userMessage, String sessionId) {
        this.intentId = intentId;
        this.userMessage = userMessage;
        this.sessionId = sessionId;
    }

    // Getters y Setters
    public String getIntentId() {
        return intentId;
    }

    public void setIntentId(String intentId) {
        this.intentId = intentId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getCurrentSlots() {
        return currentSlots;
    }

    public void setCurrentSlots(Map<String, Object> currentSlots) {
        this.currentSlots = currentSlots;
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

    public Map<String, Object> getConversationContext() {
        return conversationContext;
    }

    public void setConversationContext(Map<String, Object> conversationContext) {
        this.conversationContext = conversationContext;
    }

    public Map<String, Object> getUserPreferences() {
        return userPreferences;
    }

    public void setUserPreferences(Map<String, Object> userPreferences) {
        this.userPreferences = userPreferences;
    }

    public boolean isEnableDynamicQuestions() {
        return enableDynamicQuestions;
    }

    public void setEnableDynamicQuestions(boolean enableDynamicQuestions) {
        this.enableDynamicQuestions = enableDynamicQuestions;
    }

    public int getMaxSlotAttempts() {
        return maxSlotAttempts;
    }

    public void setMaxSlotAttempts(int maxSlotAttempts) {
        this.maxSlotAttempts = maxSlotAttempts;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    @Override
    public String toString() {
        return "SlotFillingRequest{" +
                "intentId='" + intentId + '\'' +
                ", userMessage='" + userMessage + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", currentSlots=" + currentSlots +
                ", requiredEntities=" + requiredEntities +
                ", optionalEntities=" + optionalEntities +
                ", enableDynamicQuestions=" + enableDynamicQuestions +
                '}';
    }
}