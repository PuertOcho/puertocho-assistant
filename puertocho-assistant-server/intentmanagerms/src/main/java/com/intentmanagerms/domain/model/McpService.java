package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Representa un servicio MCP completo con sus acciones
 */
public class McpService {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("timeout")
    private Integer timeout;
    
    @JsonProperty("retry_attempts")
    private Integer retryAttempts;
    
    @JsonProperty("headers")
    private Map<String, String> headers;
    
    @JsonProperty("actions")
    private Map<String, McpAction> actions;
    
    @JsonProperty("health_check_endpoint")
    private String healthCheckEndpoint;
    
    @JsonProperty("health_check_interval")
    private Integer healthCheckInterval;
    
    @JsonProperty("circuit_breaker_enabled")
    private Boolean circuitBreakerEnabled;
    
    @JsonProperty("circuit_breaker_threshold")
    private Integer circuitBreakerThreshold;
    
    // Constructor por defecto
    public McpService() {}
    
    // Constructor con parámetros principales
    public McpService(String name, String url, Map<String, McpAction> actions) {
        this.name = name;
        this.url = url;
        this.actions = actions;
        this.enabled = true;
    }
    
    // Getters y Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public Integer getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    public Integer getRetryAttempts() {
        return retryAttempts;
    }
    
    public void setRetryAttempts(Integer retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public Map<String, McpAction> getActions() {
        return actions;
    }
    
    public void setActions(Map<String, McpAction> actions) {
        this.actions = actions;
    }
    
    public String getHealthCheckEndpoint() {
        return healthCheckEndpoint;
    }
    
    public void setHealthCheckEndpoint(String healthCheckEndpoint) {
        this.healthCheckEndpoint = healthCheckEndpoint;
    }
    
    public Integer getHealthCheckInterval() {
        return healthCheckInterval;
    }
    
    public void setHealthCheckInterval(Integer healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }
    
    public Boolean getCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }
    
    public void setCircuitBreakerEnabled(Boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }
    
    public Integer getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }
    
    public void setCircuitBreakerThreshold(Integer circuitBreakerThreshold) {
        this.circuitBreakerThreshold = circuitBreakerThreshold;
    }
    
    /**
     * Verifica si el servicio está habilitado
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    /**
     * Obtiene una acción específica por su ID
     */
    public McpAction getAction(String actionId) {
        return actions != null ? actions.get(actionId) : null;
    }
    
    /**
     * Verifica si existe una acción específica
     */
    public boolean hasAction(String actionId) {
        return actions != null && actions.containsKey(actionId);
    }
    
    /**
     * Obtiene el número total de acciones
     */
    public int getActionCount() {
        return actions != null ? actions.size() : 0;
    }
    
    /**
     * Obtiene el número de acciones habilitadas
     */
    public int getEnabledActionCount() {
        if (actions == null) {
            return 0;
        }
        
        return (int) actions.values().stream()
                .filter(McpAction::isEnabled)
                .count();
    }
    
    /**
     * Obtiene el timeout con valor por defecto
     */
    public int getTimeoutOrDefault(int defaultTimeout) {
        return timeout != null ? timeout : defaultTimeout;
    }
    
    /**
     * Obtiene el número de reintentos con valor por defecto
     */
    public int getRetryAttemptsOrDefault(int defaultRetries) {
        return retryAttempts != null ? retryAttempts : defaultRetries;
    }
    
    /**
     * Obtiene el intervalo de health check con valor por defecto
     */
    public int getHealthCheckIntervalOrDefault(int defaultInterval) {
        return healthCheckInterval != null ? healthCheckInterval : defaultInterval;
    }
    
    /**
     * Verifica si el circuit breaker está habilitado
     */
    public boolean isCircuitBreakerEnabled() {
        return circuitBreakerEnabled != null && circuitBreakerEnabled;
    }
    
    /**
     * Obtiene el umbral del circuit breaker con valor por defecto
     */
    public int getCircuitBreakerThresholdOrDefault(int defaultThreshold) {
        return circuitBreakerThreshold != null ? circuitBreakerThreshold : defaultThreshold;
    }
    
    /**
     * Obtiene todas las acciones habilitadas
     */
    public Map<String, McpAction> getEnabledActions() {
        if (actions == null) {
            return Map.of();
        }
        
        return actions.entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
    
    /**
     * Obtiene todas las acciones por método HTTP
     */
    public Map<String, java.util.List<McpAction>> getActionsByMethod() {
        if (actions == null) {
            return Map.of();
        }
        
        return actions.values().stream()
                .filter(McpAction::isEnabled)
                .collect(java.util.stream.Collectors.groupingBy(
                        McpAction::getMethod,
                        java.util.stream.Collectors.toList()
                ));
    }
    
    @Override
    public String toString() {
        return "McpService{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", enabled=" + enabled +
                ", actionCount=" + getActionCount() +
                ", enabledActionCount=" + getEnabledActionCount() +
                '}';
    }
} 