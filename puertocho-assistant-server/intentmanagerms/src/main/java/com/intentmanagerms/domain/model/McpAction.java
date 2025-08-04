package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Representa una acción MCP individual con su configuración
 */
public class McpAction {
    
    @JsonProperty("endpoint")
    private String endpoint;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("required_params")
    private List<String> requiredParams;
    
    @JsonProperty("optional_params")
    private List<String> optionalParams;
    
    @JsonProperty("response_type")
    private String responseType;
    
    @JsonProperty("content_type")
    private String contentType;
    
    @JsonProperty("timeout")
    private Integer timeout;
    
    @JsonProperty("retry_attempts")
    private Integer retryAttempts;
    
    @JsonProperty("headers")
    private Map<String, String> headers;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    // Constructor por defecto
    public McpAction() {}
    
    // Constructor con parámetros principales
    public McpAction(String endpoint, String method, String description) {
        this.endpoint = endpoint;
        this.method = method;
        this.description = description;
        this.enabled = true;
    }
    
    // Getters y Setters
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getRequiredParams() {
        return requiredParams;
    }
    
    public void setRequiredParams(List<String> requiredParams) {
        this.requiredParams = requiredParams;
    }
    
    public List<String> getOptionalParams() {
        return optionalParams;
    }
    
    public void setOptionalParams(List<String> optionalParams) {
        this.optionalParams = optionalParams;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
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
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Obtiene el número total de parámetros (requeridos + opcionales)
     */
    public int getTotalParamCount() {
        int count = 0;
        if (requiredParams != null) {
            count += requiredParams.size();
        }
        if (optionalParams != null) {
            count += optionalParams.size();
        }
        return count;
    }
    
    /**
     * Verifica si la acción está habilitada
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    /**
     * Obtiene todos los parámetros (requeridos + opcionales)
     */
    public List<String> getAllParams() {
        List<String> allParams = new java.util.ArrayList<>();
        
        if (requiredParams != null) {
            allParams.addAll(requiredParams);
        }
        
        if (optionalParams != null) {
            allParams.addAll(optionalParams);
        }
        
        return allParams;
    }
    
    /**
     * Verifica si un parámetro es requerido
     */
    public boolean isRequiredParam(String paramName) {
        return requiredParams != null && requiredParams.contains(paramName);
    }
    
    /**
     * Verifica si un parámetro es opcional
     */
    public boolean isOptionalParam(String paramName) {
        return optionalParams != null && optionalParams.contains(paramName);
    }
    
    /**
     * Verifica si la acción acepta un parámetro específico
     */
    public boolean acceptsParam(String paramName) {
        return isRequiredParam(paramName) || isOptionalParam(paramName);
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
    
    @Override
    public String toString() {
        return "McpAction{" +
                "endpoint='" + endpoint + '\'' +
                ", method='" + method + '\'' +
                ", description='" + description + '\'' +
                ", requiredParams=" + requiredParams +
                ", enabled=" + enabled +
                '}';
    }
} 