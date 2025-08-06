package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contexto persistente de una conversación.
 * Mantiene información contextual que persiste a lo largo de la conversación.
 */
public class ConversationContext {

    @JsonProperty("context_id")
    private String contextId;

    @JsonProperty("user_preferences")
    private Map<String, Object> userPreferences;

    @JsonProperty("conversation_metadata")
    private Map<String, Object> conversationMetadata;

    @JsonProperty("device_context")
    private Map<String, Object> deviceContext;

    @JsonProperty("location_context")
    private Map<String, Object> locationContext;

    @JsonProperty("temporal_context")
    private Map<String, Object> temporalContext;

    @JsonProperty("intent_history")
    private Map<String, Integer> intentHistory;

    @JsonProperty("entity_cache")
    private Map<String, Object> entityCache;

    @JsonProperty("conversation_summary")
    private String conversationSummary;

    @JsonProperty("context_compression_level")
    private int contextCompressionLevel;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonProperty("last_compression_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastCompressionAt;

    public ConversationContext() {
        this.contextId = UUID.randomUUID().toString();
        this.userPreferences = new HashMap<>();
        this.conversationMetadata = new HashMap<>();
        this.deviceContext = new HashMap<>();
        this.locationContext = new HashMap<>();
        this.temporalContext = new HashMap<>();
        this.intentHistory = new HashMap<>();
        this.entityCache = new HashMap<>();
        this.conversationSummary = "";
        this.contextCompressionLevel = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastCompressionAt = LocalDateTime.now();
    }

    // Getters y Setters
    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public Map<String, Object> getUserPreferences() {
        return userPreferences;
    }

    public void setUserPreferences(Map<String, Object> userPreferences) {
        this.userPreferences = userPreferences;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getConversationMetadata() {
        return conversationMetadata;
    }

    public void setConversationMetadata(Map<String, Object> conversationMetadata) {
        this.conversationMetadata = conversationMetadata;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getDeviceContext() {
        return deviceContext;
    }

    public void setDeviceContext(Map<String, Object> deviceContext) {
        this.deviceContext = deviceContext;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getLocationContext() {
        return locationContext;
    }

    public void setLocationContext(Map<String, Object> locationContext) {
        this.locationContext = locationContext;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getTemporalContext() {
        return temporalContext;
    }

    public void setTemporalContext(Map<String, Object> temporalContext) {
        this.temporalContext = temporalContext;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Integer> getIntentHistory() {
        return intentHistory;
    }

    public void setIntentHistory(Map<String, Integer> intentHistory) {
        this.intentHistory = intentHistory;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getEntityCache() {
        return entityCache;
    }

    public void setEntityCache(Map<String, Object> entityCache) {
        this.entityCache = entityCache;
        this.updatedAt = LocalDateTime.now();
    }

    public String getConversationSummary() {
        return conversationSummary;
    }

    public void setConversationSummary(String conversationSummary) {
        this.conversationSummary = conversationSummary;
        this.updatedAt = LocalDateTime.now();
    }

    public int getContextCompressionLevel() {
        return contextCompressionLevel;
    }

    public void setContextCompressionLevel(int contextCompressionLevel) {
        this.contextCompressionLevel = contextCompressionLevel;
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

    public LocalDateTime getLastCompressionAt() {
        return lastCompressionAt;
    }

    public void setLastCompressionAt(LocalDateTime lastCompressionAt) {
        this.lastCompressionAt = lastCompressionAt;
    }

    // Métodos de utilidad
    public void updateUserPreference(String key, Object value) {
        this.userPreferences.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    public Object getUserPreference(String key) {
        return this.userPreferences.get(key);
    }

    public void updateConversationMetadata(String key, Object value) {
        this.conversationMetadata.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    public Object getConversationMetadata(String key) {
        return this.conversationMetadata.get(key);
    }

    public void updateDeviceContext(String key, Object value) {
        this.deviceContext.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    public Object getDeviceContext(String key) {
        return this.deviceContext.get(key);
    }

    public void updateLocationContext(String key, Object value) {
        this.locationContext.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    public Object getLocationContext(String key) {
        return this.locationContext.get(key);
    }

    public void updateTemporalContext(String key, Object value) {
        this.temporalContext.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    public Object getTemporalContext(String key) {
        return this.temporalContext.get(key);
    }

    public void recordIntent(String intentId) {
        this.intentHistory.merge(intentId, 1, Integer::sum);
        this.updatedAt = LocalDateTime.now();
    }

    public int getIntentCount(String intentId) {
        return this.intentHistory.getOrDefault(intentId, 0);
    }

    public void cacheEntity(String key, Object value) {
        this.entityCache.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    public Object getCachedEntity(String key) {
        return this.entityCache.get(key);
    }

    public void compressContext() {
        this.contextCompressionLevel++;
        this.lastCompressionAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean needsCompression(int threshold) {
        return this.contextCompressionLevel < threshold;
    }

    public void updateActivity() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ConversationContext{" +
                "contextId='" + contextId + '\'' +
                ", contextCompressionLevel=" + contextCompressionLevel +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 