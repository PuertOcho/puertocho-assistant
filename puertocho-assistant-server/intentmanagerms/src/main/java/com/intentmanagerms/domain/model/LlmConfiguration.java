package com.intentmanagerms.domain.model;

import java.time.Duration;

/**
 * Configuración para un LLM específico.
 * Contiene todos los parámetros necesarios para configurar y usar un LLM.
 */
public class LlmConfiguration {
    private String id;
    private String name;
    private LlmProvider provider;
    private String model;
    private String apiKey;
    private String baseUrl;
    private Duration timeout;
    private Integer maxTokens;
    private Double temperature;
    private Double topP;
    private Integer maxRetries;
    private Duration retryDelay;
    private boolean enabled;
    private String role;
    private Double weight;

    // Constructor por defecto
    public LlmConfiguration() {}

    // Constructor con parámetros principales
    public LlmConfiguration(String id, String name, LlmProvider provider, String model, String apiKey) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.model = model;
        this.apiKey = apiKey;
        this.enabled = true;
        this.weight = 1.0;
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

    public LlmProvider getProvider() {
        return provider;
    }

    public void setProvider(LlmProvider provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "LlmConfiguration{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", provider=" + provider +
                ", model='" + model + '\'' +
                ", enabled=" + enabled +
                ", role='" + role + '\'' +
                ", weight=" + weight +
                '}';
    }
} 