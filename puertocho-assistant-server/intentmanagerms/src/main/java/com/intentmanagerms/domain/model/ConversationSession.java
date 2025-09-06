package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modelo de sesión conversacional que mantiene el estado completo de una conversación.
 * Incluye contexto histórico, estado actual, y metadatos de la sesión.
 */
public class ConversationSession {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("state")
    private ConversationState state;

    @JsonProperty("context")
    private ConversationContext context;

    @JsonProperty("conversation_history")
    private List<ConversationTurn> conversationHistory;

    @JsonProperty("current_intent")
    private String currentIntent;

    @JsonProperty("slots")
    private Map<String, Object> slots;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonProperty("last_activity")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActivity;

    @JsonProperty("turn_count")
    private int turnCount;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("timeout_minutes")
    private int timeoutMinutes;

    @JsonProperty("max_turns")
    private int maxTurns;

    public ConversationSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.state = ConversationState.ACTIVE;
        this.isActive = true;
        this.turnCount = 0;
        this.timeoutMinutes = 30;
        this.maxTurns = 50;
    }

    public ConversationSession(String userId) {
        this();
        this.userId = userId;
        this.context = new ConversationContext();
    }

    // Getters y Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ConversationState getState() {
        return state;
    }

    public void setState(ConversationState state) {
        this.state = state;
        this.updatedAt = LocalDateTime.now();
    }

    public ConversationContext getContext() {
        return context;
    }

    public void setContext(ConversationContext context) {
        this.context = context;
        this.updatedAt = LocalDateTime.now();
    }

    public List<ConversationTurn> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<ConversationTurn> conversationHistory) {
        this.conversationHistory = conversationHistory;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCurrentIntent() {
        return currentIntent;
    }

    public void setCurrentIntent(String currentIntent) {
        this.currentIntent = currentIntent;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, Object> slots) {
        this.slots = slots;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = LocalDateTime.now();
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    // Métodos de utilidad
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementTurnCount() {
        this.turnCount++;
        this.updateActivity();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.lastActivity.plusMinutes(this.timeoutMinutes));
    }

    public boolean hasReachedMaxTurns() {
        return this.turnCount >= this.maxTurns;
    }

    public void addTurn(ConversationTurn turn) {
        if (this.conversationHistory == null) {
            this.conversationHistory = new java.util.ArrayList<>();
        }
        this.conversationHistory.add(turn);
        this.incrementTurnCount();
    }

    public ConversationTurn getLastTurn() {
        if (this.conversationHistory != null && !this.conversationHistory.isEmpty()) {
            return this.conversationHistory.get(this.conversationHistory.size() - 1);
        }
        return null;
    }

    public void updateSlot(String key, Object value) {
        if (this.slots == null) {
            this.slots = new java.util.HashMap<>();
        }
        this.slots.put(key, value);
        this.updateActivity();
    }

    public Object getSlot(String key) {
        return this.slots != null ? this.slots.get(key) : null;
    }

    public void updateMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        this.updateActivity();
    }

    public Object getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }

    public void removeMetadata(String key) {
        if (this.metadata != null) {
            this.metadata.remove(key);
            this.updateActivity();
        }
    }

    @Override
    public String toString() {
        return "ConversationSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", state=" + state +
                ", turnCount=" + turnCount +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", lastActivity=" + lastActivity +
                '}';
    }
} 