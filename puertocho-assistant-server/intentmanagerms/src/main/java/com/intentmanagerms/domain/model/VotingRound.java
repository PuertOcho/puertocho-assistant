package com.intentmanagerms.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Representa una ronda de votación en el sistema MoE (Mixture of Experts).
 * Contiene todos los votos de los LLMs participantes y el resultado del consenso.
 */
public class VotingRound {
    private String roundId;
    private String requestId;
    private String userMessage;
    private Map<String, Object> conversationContext;
    private List<String> conversationHistory;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<LlmVote> votes;
    private VotingConsensus consensus;
    private VotingStatus status;
    private String errorMessage;
    private Map<String, Object> metadata;

    // Constructor por defecto
    public VotingRound() {
        this.startTime = LocalDateTime.now();
        this.status = VotingStatus.IN_PROGRESS;
    }

    // Constructor con parámetros principales
    public VotingRound(String roundId, String requestId, String userMessage) {
        this();
        this.roundId = roundId;
        this.requestId = requestId;
        this.userMessage = userMessage;
    }

    // Getters y Setters
    public String getRoundId() {
        return roundId;
    }

    public void setRoundId(String roundId) {
        this.roundId = roundId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public Map<String, Object> getConversationContext() {
        return conversationContext;
    }

    public void setConversationContext(Map<String, Object> conversationContext) {
        this.conversationContext = conversationContext;
    }

    public List<String> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<String> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<LlmVote> getVotes() {
        return votes;
    }

    public void setVotes(List<LlmVote> votes) {
        this.votes = votes;
    }

    public VotingConsensus getConsensus() {
        return consensus;
    }

    public void setConsensus(VotingConsensus consensus) {
        this.consensus = consensus;
    }

    public VotingStatus getStatus() {
        return status;
    }

    public void setStatus(VotingStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Calcula la duración de la ronda de votación en milisegundos.
     */
    public long getDurationMs() {
        if (endTime != null && startTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }

    /**
     * Verifica si la ronda está completada.
     */
    public boolean isCompleted() {
        return status == VotingStatus.COMPLETED || status == VotingStatus.FAILED;
    }

    /**
     * Verifica si la ronda fue exitosa.
     */
    public boolean isSuccessful() {
        return status == VotingStatus.COMPLETED && consensus != null;
    }

    @Override
    public String toString() {
        return "VotingRound{" +
                "roundId='" + roundId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", status=" + status +
                ", votesCount=" + (votes != null ? votes.size() : 0) +
                ", consensus=" + consensus +
                ", durationMs=" + getDurationMs() +
                '}';
    }

    /**
     * Estados posibles de una ronda de votación.
     */
    public enum VotingStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        TIMEOUT
    }
} 