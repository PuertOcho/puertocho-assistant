package com.intentmanagerms.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Representa el consenso resultante de una ronda de votación entre múltiples LLMs.
 * Contiene la decisión final y metadatos sobre el proceso de consenso.
 */
public class VotingConsensus {
    private String consensusId;
    private String finalIntent;
    private String finalResponse;
    private Map<String, Object> finalEntities;
    private Double consensusConfidence;
    private Integer participatingVotes;
    private Integer totalVotes;
    private AgreementLevel agreementLevel;
    private String consensusMethod;
    private List<Map<String, Object>> finalSubtasks;
    private Map<String, Object> consensusMetrics;
    private String reasoning;
    private Map<String, Object> metadata;

    // Constructor por defecto
    public VotingConsensus() {}

    // Constructor con parámetros principales
    public VotingConsensus(String consensusId, String finalIntent, Double consensusConfidence, 
                          Integer participatingVotes, Integer totalVotes) {
        this.consensusId = consensusId;
        this.finalIntent = finalIntent;
        this.consensusConfidence = consensusConfidence;
        this.participatingVotes = participatingVotes;
        this.totalVotes = totalVotes;
    }

    // Getters y Setters
    public String getConsensusId() {
        return consensusId;
    }

    public void setConsensusId(String consensusId) {
        this.consensusId = consensusId;
    }

    public String getFinalIntent() {
        return finalIntent;
    }

    public void setFinalIntent(String finalIntent) {
        this.finalIntent = finalIntent;
    }

    public String getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(String finalResponse) {
        this.finalResponse = finalResponse;
    }

    public Map<String, Object> getFinalEntities() {
        return finalEntities;
    }

    public void setFinalEntities(Map<String, Object> finalEntities) {
        this.finalEntities = finalEntities;
    }

    public Double getConsensusConfidence() {
        return consensusConfidence;
    }

    public void setConsensusConfidence(Double consensusConfidence) {
        this.consensusConfidence = consensusConfidence;
    }

    public Integer getParticipatingVotes() {
        return participatingVotes;
    }

    public void setParticipatingVotes(Integer participatingVotes) {
        this.participatingVotes = participatingVotes;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }

    public AgreementLevel getAgreementLevel() {
        return agreementLevel;
    }

    public void setAgreementLevel(AgreementLevel agreementLevel) {
        this.agreementLevel = agreementLevel;
    }

    public String getConsensusMethod() {
        return consensusMethod;
    }

    public void setConsensusMethod(String consensusMethod) {
        this.consensusMethod = consensusMethod;
    }

    public List<Map<String, Object>> getFinalSubtasks() {
        return finalSubtasks;
    }

    public void setFinalSubtasks(List<Map<String, Object>> finalSubtasks) {
        this.finalSubtasks = finalSubtasks;
    }

    public Map<String, Object> getConsensusMetrics() {
        return consensusMetrics;
    }

    public void setConsensusMetrics(Map<String, Object> consensusMetrics) {
        this.consensusMetrics = consensusMetrics;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Calcula el porcentaje de participación en la votación.
     */
    public double getParticipationRate() {
        if (totalVotes == null || totalVotes == 0) {
            return 0.0;
        }
        return (double) participatingVotes / totalVotes * 100.0;
    }

    /**
     * Verifica si el consenso es válido.
     */
    public boolean isValid() {
        return finalIntent != null && 
               consensusConfidence != null && 
               consensusConfidence >= 0.0 && 
               consensusConfidence <= 1.0 &&
               participatingVotes != null && 
               participatingVotes > 0;
    }

    /**
     * Verifica si hay unanimidad en la votación.
     */
    public boolean isUnanimous() {
        return agreementLevel == AgreementLevel.UNANIMOUS;
    }

    /**
     * Verifica si hay mayoría en la votación.
     */
    public boolean hasMajority() {
        return agreementLevel == AgreementLevel.MAJORITY || agreementLevel == AgreementLevel.UNANIMOUS;
    }

    @Override
    public String toString() {
        return "VotingConsensus{" +
                "consensusId='" + consensusId + '\'' +
                ", finalIntent='" + finalIntent + '\'' +
                ", consensusConfidence=" + consensusConfidence +
                ", participatingVotes=" + participatingVotes +
                ", totalVotes=" + totalVotes +
                ", agreementLevel=" + agreementLevel +
                ", consensusMethod='" + consensusMethod + '\'' +
                '}';
    }

    /**
     * Niveles de acuerdo en la votación.
     */
    public enum AgreementLevel {
        UNANIMOUS("Unánime - Todos los LLMs están de acuerdo"),
        MAJORITY("Mayoría - La mayoría de LLMs están de acuerdo"),
        PLURALITY("Pluralidad - El voto más común sin mayoría"),
        SPLIT("Dividido - No hay consenso claro"),
        FAILED("Fallido - No se pudo alcanzar consenso");

        private final String description;

        AgreementLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
} 