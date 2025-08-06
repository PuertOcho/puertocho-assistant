package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Representa un turno de conversación entre el usuario y el sistema.
 * Contiene el mensaje del usuario, la respuesta del sistema y metadatos del turno.
 */
public class ConversationTurn {

    @JsonProperty("turn_id")
    private String turnId;

    @JsonProperty("turn_number")
    private int turnNumber;

    @JsonProperty("user_message")
    private String userMessage;

    @JsonProperty("system_response")
    private String systemResponse;

    @JsonProperty("detected_intent")
    private String detectedIntent;

    @JsonProperty("confidence_score")
    private double confidenceScore;

    @JsonProperty("extracted_entities")
    private Map<String, Object> extractedEntities;

    @JsonProperty("slots_filled")
    private Map<String, Object> slotsFilled;

    @JsonProperty("actions_executed")
    private Map<String, Object> actionsExecuted;

    @JsonProperty("processing_time_ms")
    private long processingTimeMs;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("turn_type")
    private TurnType turnType;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public ConversationTurn() {
        this.turnId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.turnType = TurnType.USER_MESSAGE;
        this.confidenceScore = 0.0;
        this.processingTimeMs = 0;
    }

    public ConversationTurn(String userMessage) {
        this();
        this.userMessage = userMessage;
    }

    public ConversationTurn(String userMessage, String systemResponse) {
        this(userMessage);
        this.systemResponse = systemResponse;
        this.turnType = TurnType.COMPLETE_TURN;
    }

    // Getters y Setters
    public String getTurnId() {
        return turnId;
    }

    public void setTurnId(String turnId) {
        this.turnId = turnId;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getSystemResponse() {
        return systemResponse;
    }

    public void setSystemResponse(String systemResponse) {
        this.systemResponse = systemResponse;
        this.turnType = TurnType.COMPLETE_TURN;
    }

    public String getDetectedIntent() {
        return detectedIntent;
    }

    public void setDetectedIntent(String detectedIntent) {
        this.detectedIntent = detectedIntent;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Map<String, Object> getExtractedEntities() {
        return extractedEntities;
    }

    public void setExtractedEntities(Map<String, Object> extractedEntities) {
        this.extractedEntities = extractedEntities;
    }

    public Map<String, Object> getSlotsFilled() {
        return slotsFilled;
    }

    public void setSlotsFilled(Map<String, Object> slotsFilled) {
        this.slotsFilled = slotsFilled;
    }

    public Map<String, Object> getActionsExecuted() {
        return actionsExecuted;
    }

    public void setActionsExecuted(Map<String, Object> actionsExecuted) {
        this.actionsExecuted = actionsExecuted;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TurnType getTurnType() {
        return turnType;
    }

    public void setTurnType(TurnType turnType) {
        this.turnType = turnType;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Métodos de utilidad
    public void addExtractedEntity(String key, Object value) {
        if (this.extractedEntities == null) {
            this.extractedEntities = new java.util.HashMap<>();
        }
        this.extractedEntities.put(key, value);
    }

    public Object getExtractedEntity(String key) {
        return this.extractedEntities != null ? this.extractedEntities.get(key) : null;
    }

    public void addSlotFilled(String key, Object value) {
        if (this.slotsFilled == null) {
            this.slotsFilled = new java.util.HashMap<>();
        }
        this.slotsFilled.put(key, value);
    }

    public Object getSlotFilled(String key) {
        return this.slotsFilled != null ? this.slotsFilled.get(key) : null;
    }

    public void addActionExecuted(String action, Object result) {
        if (this.actionsExecuted == null) {
            this.actionsExecuted = new java.util.HashMap<>();
        }
        this.actionsExecuted.put(action, result);
    }

    public Object getActionExecuted(String action) {
        return this.actionsExecuted != null ? this.actionsExecuted.get(action) : null;
    }

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }

    public boolean isComplete() {
        return this.turnType == TurnType.COMPLETE_TURN;
    }

    public boolean hasUserMessage() {
        return this.userMessage != null && !this.userMessage.trim().isEmpty();
    }

    public boolean hasSystemResponse() {
        return this.systemResponse != null && !this.systemResponse.trim().isEmpty();
    }

    public boolean hasDetectedIntent() {
        return this.detectedIntent != null && !this.detectedIntent.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "ConversationTurn{" +
                "turnId='" + turnId + '\'' +
                ", turnNumber=" + turnNumber +
                ", userMessage='" + userMessage + '\'' +
                ", detectedIntent='" + detectedIntent + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", turnType=" + turnType +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Tipos de turno de conversación.
     */
    public enum TurnType {
        USER_MESSAGE("user_message", "Solo mensaje del usuario"),
        SYSTEM_RESPONSE("system_response", "Solo respuesta del sistema"),
        COMPLETE_TURN("complete_turn", "Turno completo usuario-sistema"),
        SLOT_FILLING("slot_filling", "Llenado de slots"),
        ERROR("error", "Turno con error");

        private final String code;
        private final String description;

        TurnType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return code;
        }
    }
} 