package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Representa la configuración completa de intenciones cargada desde JSON
 */
public class IntentConfiguration {
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("intents")
    private Map<String, IntentExample> intents;
    
    @JsonProperty("global_settings")
    private GlobalIntentSettings globalSettings;
    
    // Constructor por defecto
    public IntentConfiguration() {}
    
    // Constructor con parámetros principales
    public IntentConfiguration(String version, String description, Map<String, IntentExample> intents) {
        this.version = version;
        this.description = description;
        this.intents = intents;
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
    
    public Map<String, IntentExample> getIntents() {
        return intents;
    }
    
    public void setIntents(Map<String, IntentExample> intents) {
        this.intents = intents;
    }
    
    public GlobalIntentSettings getGlobalSettings() {
        return globalSettings;
    }
    
    public void setGlobalSettings(GlobalIntentSettings globalSettings) {
        this.globalSettings = globalSettings;
    }
    
    /**
     * Obtiene una intención específica por su ID
     */
    public IntentExample getIntent(String intentId) {
        return intents != null ? intents.get(intentId) : null;
    }
    
    /**
     * Verifica si existe una intención específica
     */
    public boolean hasIntent(String intentId) {
        return intents != null && intents.containsKey(intentId);
    }
    
    /**
     * Obtiene el número total de intenciones configuradas
     */
    public int getIntentCount() {
        return intents != null ? intents.size() : 0;
    }
    
    /**
     * Obtiene el número total de ejemplos de todas las intenciones
     */
    public int getTotalExampleCount() {
        if (intents == null) {
            return 0;
        }
        
        return intents.values().stream()
                .mapToInt(IntentExample::getExampleCount)
                .sum();
    }
    
    /**
     * Obtiene todas las acciones MCP disponibles
     */
    public java.util.Set<String> getAvailableMcpActions() {
        if (intents == null) {
            return java.util.Set.of();
        }
        
        return intents.values().stream()
                .map(IntentExample::getMcpAction)
                .filter(action -> action != null && !action.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Obtiene intenciones por dominio de experto
     */
    public Map<String, java.util.List<IntentExample>> getIntentsByExpertDomain() {
        if (intents == null) {
            return Map.of();
        }
        
        return intents.values().stream()
                .filter(intent -> intent.getExpertDomain() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        IntentExample::getExpertDomain,
                        java.util.stream.Collectors.toList()
                ));
    }
    
    @Override
    public String toString() {
        return "IntentConfiguration{" +
                "version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", intentCount=" + getIntentCount() +
                ", totalExamples=" + getTotalExampleCount() +
                '}';
    }
    
    /**
     * Configuración global para todas las intenciones
     */
    public static class GlobalIntentSettings {
        
        @JsonProperty("default_confidence_threshold")
        private Double defaultConfidenceThreshold;
        
        @JsonProperty("default_max_examples_for_rag")
        private Integer defaultMaxExamplesForRag;
        
        @JsonProperty("enable_hot_reload")
        private Boolean enableHotReload;
        
        @JsonProperty("reload_interval_seconds")
        private Integer reloadIntervalSeconds;
        
        @JsonProperty("fallback_intent")
        private String fallbackIntent;
        
        @JsonProperty("unknown_intent_response")
        private String unknownIntentResponse;
        
        // Constructor por defecto
        public GlobalIntentSettings() {}
        
        // Getters y Setters
        public Double getDefaultConfidenceThreshold() {
            return defaultConfidenceThreshold;
        }
        
        public void setDefaultConfidenceThreshold(Double defaultConfidenceThreshold) {
            this.defaultConfidenceThreshold = defaultConfidenceThreshold;
        }
        
        public Integer getDefaultMaxExamplesForRag() {
            return defaultMaxExamplesForRag;
        }
        
        public void setDefaultMaxExamplesForRag(Integer defaultMaxExamplesForRag) {
            this.defaultMaxExamplesForRag = defaultMaxExamplesForRag;
        }
        
        public Boolean getEnableHotReload() {
            return enableHotReload;
        }
        
        public void setEnableHotReload(Boolean enableHotReload) {
            this.enableHotReload = enableHotReload;
        }
        
        public Integer getReloadIntervalSeconds() {
            return reloadIntervalSeconds;
        }
        
        public void setReloadIntervalSeconds(Integer reloadIntervalSeconds) {
            this.reloadIntervalSeconds = reloadIntervalSeconds;
        }
        
        public String getFallbackIntent() {
            return fallbackIntent;
        }
        
        public void setFallbackIntent(String fallbackIntent) {
            this.fallbackIntent = fallbackIntent;
        }
        
        public String getUnknownIntentResponse() {
            return unknownIntentResponse;
        }
        
        public void setUnknownIntentResponse(String unknownIntentResponse) {
            this.unknownIntentResponse = unknownIntentResponse;
        }
        
        @Override
        public String toString() {
            return "GlobalIntentSettings{" +
                    "defaultConfidenceThreshold=" + defaultConfidenceThreshold +
                    ", defaultMaxExamplesForRag=" + defaultMaxExamplesForRag +
                    ", enableHotReload=" + enableHotReload +
                    ", fallbackIntent='" + fallbackIntent + '\'' +
                    '}';
        }
    }
} 