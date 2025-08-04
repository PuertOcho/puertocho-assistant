package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Representa el registro completo de servicios MCP
 */
public class McpRegistry {
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("services")
    private Map<String, McpService> services;
    
    @JsonProperty("fallback_responses")
    private Map<String, String> fallbackResponses;
    
    @JsonProperty("global_settings")
    private GlobalMcpSettings globalSettings;
    
    // Constructor por defecto
    public McpRegistry() {}
    
    // Constructor con parámetros principales
    public McpRegistry(String version, String description, Map<String, McpService> services) {
        this.version = version;
        this.description = description;
        this.services = services;
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
    
    public Map<String, McpService> getServices() {
        return services;
    }
    
    public void setServices(Map<String, McpService> services) {
        this.services = services;
    }
    
    public Map<String, String> getFallbackResponses() {
        return fallbackResponses;
    }
    
    public void setFallbackResponses(Map<String, String> fallbackResponses) {
        this.fallbackResponses = fallbackResponses;
    }
    
    public GlobalMcpSettings getGlobalSettings() {
        return globalSettings;
    }
    
    public void setGlobalSettings(GlobalMcpSettings globalSettings) {
        this.globalSettings = globalSettings;
    }
    
    /**
     * Obtiene un servicio específico por su ID
     */
    public McpService getService(String serviceId) {
        return services != null ? services.get(serviceId) : null;
    }
    
    /**
     * Verifica si existe un servicio específico
     */
    public boolean hasService(String serviceId) {
        return services != null && services.containsKey(serviceId);
    }
    
    /**
     * Obtiene el número total de servicios
     */
    public int getServiceCount() {
        return services != null ? services.size() : 0;
    }
    
    /**
     * Obtiene el número de servicios habilitados
     */
    public int getEnabledServiceCount() {
        if (services == null) {
            return 0;
        }
        
        return (int) services.values().stream()
                .filter(McpService::isEnabled)
                .count();
    }
    
    /**
     * Obtiene el número total de acciones en todos los servicios
     */
    public int getTotalActionCount() {
        if (services == null) {
            return 0;
        }
        
        return services.values().stream()
                .mapToInt(McpService::getActionCount)
                .sum();
    }
    
    /**
     * Obtiene el número total de acciones habilitadas
     */
    public int getTotalEnabledActionCount() {
        if (services == null) {
            return 0;
        }
        
        return services.values().stream()
                .mapToInt(McpService::getEnabledActionCount)
                .sum();
    }
    
    /**
     * Obtiene todos los servicios habilitados
     */
    public Map<String, McpService> getEnabledServices() {
        if (services == null) {
            return Map.of();
        }
        
        return services.entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
    
    /**
     * Obtiene una acción específica por su ID global
     */
    public McpAction getAction(String actionId) {
        if (services == null) {
            return null;
        }
        
        for (McpService service : services.values()) {
            McpAction action = service.getAction(actionId);
            if (action != null) {
                return action;
            }
        }
        
        return null;
    }
    
    /**
     * Verifica si existe una acción específica
     */
    public boolean hasAction(String actionId) {
        return getAction(actionId) != null;
    }
    
    /**
     * Obtiene el servicio que contiene una acción específica
     */
    public McpService getServiceForAction(String actionId) {
        if (services == null) {
            return null;
        }
        
        for (Map.Entry<String, McpService> entry : services.entrySet()) {
            if (entry.getValue().hasAction(actionId)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Obtiene todas las acciones disponibles
     */
    public Map<String, McpAction> getAllActions() {
        if (services == null) {
            return Map.of();
        }
        
        Map<String, McpAction> allActions = new java.util.HashMap<>();
        
        for (McpService service : services.values()) {
            if (service.isEnabled()) {
                allActions.putAll(service.getEnabledActions());
            }
        }
        
        return allActions;
    }
    
    /**
     * Obtiene todas las acciones por método HTTP
     */
    public Map<String, java.util.List<McpAction>> getAllActionsByMethod() {
        if (services == null) {
            return Map.of();
        }
        
        return services.values().stream()
                .filter(McpService::isEnabled)
                .flatMap(service -> service.getEnabledActions().values().stream())
                .collect(java.util.stream.Collectors.groupingBy(
                        McpAction::getMethod,
                        java.util.stream.Collectors.toList()
                ));
    }
    
    /**
     * Obtiene una respuesta de fallback específica
     */
    public String getFallbackResponse(String responseType) {
        if (fallbackResponses == null) {
            return "Ha ocurrido un error inesperado.";
        }
        
        return fallbackResponses.getOrDefault(responseType, 
                fallbackResponses.getOrDefault("general_error", "Ha ocurrido un error inesperado."));
    }
    
    /**
     * Obtiene una respuesta de fallback formateada
     */
    public String getFormattedFallbackResponse(String responseType, String... params) {
        String template = getFallbackResponse(responseType);
        
        if (params.length >= 1) {
            template = template.replace("{service_name}", params[0]);
        }
        if (params.length >= 2) {
            template = template.replace("{action_name}", params[1]);
        }
        
        return template;
    }
    
    @Override
    public String toString() {
        return "McpRegistry{" +
                "version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", serviceCount=" + getServiceCount() +
                ", enabledServiceCount=" + getEnabledServiceCount() +
                ", totalActionCount=" + getTotalActionCount() +
                ", totalEnabledActionCount=" + getTotalEnabledActionCount() +
                '}';
    }
    
    /**
     * Configuración global para todos los servicios MCP
     */
    public static class GlobalMcpSettings {
        
        @JsonProperty("default_timeout")
        private Integer defaultTimeout;
        
        @JsonProperty("default_retry_attempts")
        private Integer defaultRetryAttempts;
        
        @JsonProperty("default_health_check_interval")
        private Integer defaultHealthCheckInterval;
        
        @JsonProperty("circuit_breaker_enabled")
        private Boolean circuitBreakerEnabled;
        
        @JsonProperty("circuit_breaker_threshold")
        private Integer circuitBreakerThreshold;
        
        @JsonProperty("enable_health_checks")
        private Boolean enableHealthChecks;
        
        @JsonProperty("enable_metrics")
        private Boolean enableMetrics;
        
        @JsonProperty("log_requests")
        private Boolean logRequests;
        
        // Constructor por defecto
        public GlobalMcpSettings() {}
        
        // Getters y Setters
        public Integer getDefaultTimeout() {
            return defaultTimeout;
        }
        
        public void setDefaultTimeout(Integer defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }
        
        public Integer getDefaultRetryAttempts() {
            return defaultRetryAttempts;
        }
        
        public void setDefaultRetryAttempts(Integer defaultRetryAttempts) {
            this.defaultRetryAttempts = defaultRetryAttempts;
        }
        
        public Integer getDefaultHealthCheckInterval() {
            return defaultHealthCheckInterval;
        }
        
        public void setDefaultHealthCheckInterval(Integer defaultHealthCheckInterval) {
            this.defaultHealthCheckInterval = defaultHealthCheckInterval;
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
        
        public Boolean getEnableHealthChecks() {
            return enableHealthChecks;
        }
        
        public void setEnableHealthChecks(Boolean enableHealthChecks) {
            this.enableHealthChecks = enableHealthChecks;
        }
        
        public Boolean getEnableMetrics() {
            return enableMetrics;
        }
        
        public void setEnableMetrics(Boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
        }
        
        public Boolean getLogRequests() {
            return logRequests;
        }
        
        public void setLogRequests(Boolean logRequests) {
            this.logRequests = logRequests;
        }
        
        @Override
        public String toString() {
            return "GlobalMcpSettings{" +
                    "defaultTimeout=" + defaultTimeout +
                    ", defaultRetryAttempts=" + defaultRetryAttempts +
                    ", circuitBreakerEnabled=" + circuitBreakerEnabled +
                    ", enableHealthChecks=" + enableHealthChecks +
                    '}';
        }
    }
} 