package com.intentmanagerms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Estado de conversación para gestionar diálogos multivuelta.
 * Almacenado en Redis con TTL automático.
 */
@RedisHash("conversation")
public class ConversationState {

    @Id
    private String sessionId;
    
    private String currentIntent;
    private Set<String> requiredEntities;
    private Map<String, String> collectedEntities;
    private String lastMessage;
    private String lastResponse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ConversationStatus status;
    
    @TimeToLive
    private Long ttlSeconds = 1800L; // 30 minutos por defecto

    public ConversationState() {
        this.collectedEntities = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ConversationStatus.ACTIVE;
    }

    public ConversationState(String sessionId) {
        this();
        this.sessionId = sessionId;
    }

    // Getters y setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCurrentIntent() {
        return currentIntent;
    }

    public void setCurrentIntent(String currentIntent) {
        this.currentIntent = currentIntent;
        this.updatedAt = LocalDateTime.now();
    }

    public Set<String> getRequiredEntities() {
        return requiredEntities;
    }

    public void setRequiredEntities(Set<String> requiredEntities) {
        this.requiredEntities = requiredEntities;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, String> getCollectedEntities() {
        return collectedEntities;
    }

    public void setCollectedEntities(Map<String, String> collectedEntities) {
        this.collectedEntities = collectedEntities;
        this.updatedAt = LocalDateTime.now();
    }

    public void addEntity(String entityName, String entityValue) {
        this.collectedEntities.put(entityName, entityValue);
        this.updatedAt = LocalDateTime.now();
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(String lastResponse) {
        this.lastResponse = lastResponse;
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

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    // Métodos de utilidad
    public boolean isComplete() {
        if (requiredEntities == null || requiredEntities.isEmpty()) {
            return true;
        }
        return requiredEntities.stream()
                .allMatch(entity -> collectedEntities.containsKey(entity) && 
                         collectedEntities.get(entity) != null && 
                         !collectedEntities.get(entity).trim().isEmpty());
    }

    public Set<String> getMissingEntities() {
        if (requiredEntities == null) {
            return Set.of();
        }
        return requiredEntities.stream()
                .filter(entity -> !collectedEntities.containsKey(entity) || 
                                collectedEntities.get(entity) == null || 
                                collectedEntities.get(entity).trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }

    public void reset() {
        this.currentIntent = null;
        this.requiredEntities = null;
        this.collectedEntities.clear();
        this.lastMessage = null;
        this.lastResponse = null;
        this.status = ConversationStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ConversationState{" +
                "sessionId='" + sessionId + '\'' +
                ", currentIntent='" + currentIntent + '\'' +
                ", requiredEntities=" + requiredEntities +
                ", collectedEntities=" + collectedEntities +
                ", status=" + status +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 