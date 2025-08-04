package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Configuración del sistema de votación MoE (Mixture of Experts).
 * Contiene todos los parámetros necesarios para configurar el sistema de votación.
 */
public class VotingConfiguration {
    @JsonProperty("version")
    private String version;
    @JsonProperty("description")
    private String description;
    @JsonProperty("voting_system")
    private VotingSystem votingSystem;
    @JsonProperty("single_llm_mode")
    private SingleLlmMode singleLlmMode;
    @JsonProperty("response_formats")
    private ResponseFormats responseFormats;

    // Constructor por defecto
    public VotingConfiguration() {}

    // Constructor con parámetros principales
    public VotingConfiguration(String version, String description, VotingSystem votingSystem) {
        this.version = version;
        this.description = description;
        this.votingSystem = votingSystem;
    }

    // Getters y Setters
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VotingSystem getVotingSystem() {
        return votingSystem;
    }

    public void setVotingSystem(VotingSystem votingSystem) {
        this.votingSystem = votingSystem;
    }

    public SingleLlmMode getSingleLlmMode() {
        return singleLlmMode;
    }

    public void setSingleLlmMode(SingleLlmMode singleLlmMode) {
        this.singleLlmMode = singleLlmMode;
    }

    public ResponseFormats getResponseFormats() {
        return responseFormats;
    }

    public void setResponseFormats(ResponseFormats responseFormats) {
        this.responseFormats = responseFormats;
    }

    @Override
    public String toString() {
        return "VotingConfiguration{" +
                "version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", votingSystem=" + votingSystem +
                '}';
    }

    /**
     * Configuración del sistema de votación principal.
     */
    public static class VotingSystem {
        @JsonProperty("enabled")
        private boolean enabled;
        @JsonProperty("max_debate_rounds")
        private int maxDebateRounds;
        @JsonProperty("consensus_threshold")
        private double consensusThreshold;
        @JsonProperty("timeout_per_vote")
        private int timeoutPerVote;
        @JsonProperty("parallel_voting")
        private boolean parallelVoting;
        @JsonProperty("llm_participants")
        private List<LlmParticipant> llmParticipants;
        @JsonProperty("consensus_rules")
        private ConsensusRules consensusRules;
        @JsonProperty("fallback_strategy")
        private FallbackStrategy fallbackStrategy;

        // Constructor por defecto
        public VotingSystem() {}

        // Constructor con parámetros principales
        public VotingSystem(boolean enabled, int maxDebateRounds, double consensusThreshold) {
            this.enabled = enabled;
            this.maxDebateRounds = maxDebateRounds;
            this.consensusThreshold = consensusThreshold;
        }

        // Getters y Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxDebateRounds() {
            return maxDebateRounds;
        }

        public void setMaxDebateRounds(int maxDebateRounds) {
            this.maxDebateRounds = maxDebateRounds;
        }

        public double getConsensusThreshold() {
            return consensusThreshold;
        }

        public void setConsensusThreshold(double consensusThreshold) {
            this.consensusThreshold = consensusThreshold;
        }

        public int getTimeoutPerVote() {
            return timeoutPerVote;
        }

        public void setTimeoutPerVote(int timeoutPerVote) {
            this.timeoutPerVote = timeoutPerVote;
        }

        public boolean isParallelVoting() {
            return parallelVoting;
        }

        public void setParallelVoting(boolean parallelVoting) {
            this.parallelVoting = parallelVoting;
        }

        public List<LlmParticipant> getLlmParticipants() {
            return llmParticipants;
        }

        public void setLlmParticipants(List<LlmParticipant> llmParticipants) {
            this.llmParticipants = llmParticipants;
        }

        public ConsensusRules getConsensusRules() {
            return consensusRules;
        }

        public void setConsensusRules(ConsensusRules consensusRules) {
            this.consensusRules = consensusRules;
        }

        public FallbackStrategy getFallbackStrategy() {
            return fallbackStrategy;
        }

        public void setFallbackStrategy(FallbackStrategy fallbackStrategy) {
            this.fallbackStrategy = fallbackStrategy;
        }

        @Override
        public String toString() {
            return "VotingSystem{" +
                    "enabled=" + enabled +
                    ", maxDebateRounds=" + maxDebateRounds +
                    ", consensusThreshold=" + consensusThreshold +
                    ", llmParticipantsCount=" + (llmParticipants != null ? llmParticipants.size() : 0) +
                    '}';
        }
    }

    /**
     * Participante LLM en el sistema de votación.
     */
    public static class LlmParticipant {
        @JsonProperty("id")
        private String id;
        @JsonProperty("name")
        private String name;
        @JsonProperty("provider")
        private String provider;
        @JsonProperty("model")
        private String model;
        @JsonProperty("role")
        private String role;
        @JsonProperty("weight")
        private double weight;
        @JsonProperty("temperature")
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
        @JsonProperty("prompt_template")
        private String promptTemplate;

        // Constructor por defecto
        public LlmParticipant() {}

        // Constructor con parámetros principales
        public LlmParticipant(String id, String name, String provider, String model, String role, double weight) {
            this.id = id;
            this.name = name;
            this.provider = provider;
            this.model = model;
            this.role = role;
            this.weight = weight;
        }

        // Getters y Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public String getPromptTemplate() {
            return promptTemplate;
        }

        public void setPromptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
        }

        @Override
        public String toString() {
            return "LlmParticipant{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", provider='" + provider + '\'' +
                    ", model='" + model + '\'' +
                    ", role='" + role + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

    /**
     * Reglas para el proceso de consenso.
     */
    public static class ConsensusRules {
        @JsonProperty("unanimous_required")
        private boolean unanimousRequired;
        @JsonProperty("majority_threshold")
        private int majorityThreshold;
        @JsonProperty("confidence_weight")
        private double confidenceWeight;
        @JsonProperty("reasoning_similarity_weight")
        private double reasoningSimilarityWeight;
        @JsonProperty("entity_agreement_weight")
        private double entityAgreementWeight;
        @JsonProperty("intent_agreement_weight")
        private double intentAgreementWeight;

        // Constructor por defecto
        public ConsensusRules() {}

        // Constructor con parámetros principales
        public ConsensusRules(boolean unanimousRequired, int majorityThreshold) {
            this.unanimousRequired = unanimousRequired;
            this.majorityThreshold = majorityThreshold;
        }

        // Getters y Setters
        public boolean isUnanimousRequired() {
            return unanimousRequired;
        }

        public void setUnanimousRequired(boolean unanimousRequired) {
            this.unanimousRequired = unanimousRequired;
        }

        public int getMajorityThreshold() {
            return majorityThreshold;
        }

        public void setMajorityThreshold(int majorityThreshold) {
            this.majorityThreshold = majorityThreshold;
        }

        public double getConfidenceWeight() {
            return confidenceWeight;
        }

        public void setConfidenceWeight(double confidenceWeight) {
            this.confidenceWeight = confidenceWeight;
        }

        public double getReasoningSimilarityWeight() {
            return reasoningSimilarityWeight;
        }

        public void setReasoningSimilarityWeight(double reasoningSimilarityWeight) {
            this.reasoningSimilarityWeight = reasoningSimilarityWeight;
        }

        public double getEntityAgreementWeight() {
            return entityAgreementWeight;
        }

        public void setEntityAgreementWeight(double entityAgreementWeight) {
            this.entityAgreementWeight = entityAgreementWeight;
        }

        public double getIntentAgreementWeight() {
            return intentAgreementWeight;
        }

        public void setIntentAgreementWeight(double intentAgreementWeight) {
            this.intentAgreementWeight = intentAgreementWeight;
        }

        @Override
        public String toString() {
            return "ConsensusRules{" +
                    "unanimousRequired=" + unanimousRequired +
                    ", majorityThreshold=" + majorityThreshold +
                    ", confidenceWeight=" + confidenceWeight +
                    ", intentAgreementWeight=" + intentAgreementWeight +
                    '}';
        }
    }

    /**
     * Estrategia de fallback cuando el sistema de votación falla.
     */
    public static class FallbackStrategy {
        @JsonProperty("on_no_consensus")
        private String onNoConsensus;
        @JsonProperty("on_timeout")
        private String onTimeout;
        @JsonProperty("on_error")
        private String onError;
        @JsonProperty("primary_llm_id")
        private String primaryLlmId;

        // Constructor por defecto
        public FallbackStrategy() {}

        // Constructor con parámetros principales
        public FallbackStrategy(String onNoConsensus, String onTimeout, String onError) {
            this.onNoConsensus = onNoConsensus;
            this.onTimeout = onTimeout;
            this.onError = onError;
        }

        // Getters y Setters
        public String getOnNoConsensus() {
            return onNoConsensus;
        }

        public void setOnNoConsensus(String onNoConsensus) {
            this.onNoConsensus = onNoConsensus;
        }

        public String getOnTimeout() {
            return onTimeout;
        }

        public void setOnTimeout(String onTimeout) {
            this.onTimeout = onTimeout;
        }

        public String getOnError() {
            return onError;
        }

        public void setOnError(String onError) {
            this.onError = onError;
        }

        public String getPrimaryLlmId() {
            return primaryLlmId;
        }

        public void setPrimaryLlmId(String primaryLlmId) {
            this.primaryLlmId = primaryLlmId;
        }

        @Override
        public String toString() {
            return "FallbackStrategy{" +
                    "onNoConsensus='" + onNoConsensus + '\'' +
                    ", onTimeout='" + onTimeout + '\'' +
                    ", onError='" + onError + '\'' +
                    ", primaryLlmId='" + primaryLlmId + '\'' +
                    '}';
        }
    }

    /**
     * Configuración para modo LLM único (fallback).
     */
    public static class SingleLlmMode {
        @JsonProperty("provider")
        private String provider;
        @JsonProperty("model")
        private String model;
        @JsonProperty("temperature")
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
        @JsonProperty("prompt_template")
        private String promptTemplate;

        // Constructor por defecto
        public SingleLlmMode() {}

        // Constructor con parámetros principales
        public SingleLlmMode(String provider, String model, double temperature, int maxTokens) {
            this.provider = provider;
            this.model = model;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }

        // Getters y Setters
        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public String getPromptTemplate() {
            return promptTemplate;
        }

        public void setPromptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
        }

        @Override
        public String toString() {
            return "SingleLlmMode{" +
                    "provider='" + provider + '\'' +
                    ", model='" + model + '\'' +
                    ", temperature=" + temperature +
                    ", maxTokens=" + maxTokens +
                    '}';
        }
    }

    /**
     * Formatos de respuesta esperados.
     */
    public static class ResponseFormats {
        @JsonProperty("vote_response")
        private Map<String, String> voteResponse;
        @JsonProperty("consensus_response")
        private Map<String, String> consensusResponse;

        // Constructor por defecto
        public ResponseFormats() {}

        // Constructor con parámetros principales
        public ResponseFormats(Map<String, String> voteResponse, Map<String, String> consensusResponse) {
            this.voteResponse = voteResponse;
            this.consensusResponse = consensusResponse;
        }

        // Getters y Setters
        public Map<String, String> getVoteResponse() {
            return voteResponse;
        }

        public void setVoteResponse(Map<String, String> voteResponse) {
            this.voteResponse = voteResponse;
        }

        public Map<String, String> getConsensusResponse() {
            return consensusResponse;
        }

        public void setConsensusResponse(Map<String, String> consensusResponse) {
            this.consensusResponse = consensusResponse;
        }

        @Override
        public String toString() {
            return "ResponseFormats{" +
                    "voteResponse=" + voteResponse +
                    ", consensusResponse=" + consensusResponse +
                    '}';
        }
    }
} 