package com.intentmanagerms.application.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookResponse {
    @JsonProperty("recipient_id")
    private String recipientId;
    
    @JsonProperty("text")
    private String text; // JSON string que contiene la respuesta formateada
    
    // Constructores
    public WebhookResponse() {}
    
    public WebhookResponse(String recipientId, String text) {
        this.recipientId = recipientId;
        this.text = text;
    }
    
    public String getRecipientId() {
        return recipientId;
    }
    
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return "WebhookResponse{" +
                "recipientId='" + recipientId + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
