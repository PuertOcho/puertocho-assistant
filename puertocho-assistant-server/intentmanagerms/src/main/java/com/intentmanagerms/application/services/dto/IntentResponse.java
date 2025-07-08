package com.intentmanagerms.application.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IntentResponse {
    @JsonProperty("messageId")
    private String messageId;
    
    @JsonProperty("domain")
    private String domain;
    
    @JsonProperty("locale")
    private String locale;
    
    @JsonProperty("userUtterance")
    private String userUtterance;
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("message")
    private String message; // JSON string que contiene IntentMessage
    
    // Constructores
    public IntentResponse() {}
    
    public IntentResponse(String messageId, String domain, String locale, 
                      String userUtterance, String model, String message) {
        this.messageId = messageId;
        this.domain = domain;
        this.locale = locale;
        this.userUtterance = userUtterance;
        this.model = model;
        this.message = message;
    }
    
    // Getters y Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getLocale() {
        return locale;
    }
    
    public void setLocale(String locale) {
        this.locale = locale;
    }
    
    public String getUserUtterance() {
        return userUtterance;
    }
    
    public void setUserUtterance(String userUtterance) {
        this.userUtterance = userUtterance;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "IntentResponse{" +
                "messageId='" + messageId + '\'' +
                ", domain='" + domain + '\'' +
                ", locale='" + locale + '\'' +
                ", userUtterance='" + userUtterance + '\'' +
                ", model='" + model + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
} 