package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class TtsGenerationRequest {
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("language")
    private String language = "es";
    
    @JsonProperty("voice")
    private String voice;
    
    @JsonProperty("speed")
    private Double speed = 1.0;
    
    @JsonProperty("gender_preference")
    private String genderPreference; // "female", "male", null
    
    @JsonProperty("provider")
    private String provider; // "azure", "kokoro", "auto"
    
    @JsonProperty("timeout_seconds")
    private Integer timeoutSeconds = 30;
    
    @JsonProperty("max_retries")
    private Integer maxRetries = 3;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("request_id")
    private String requestId;
    
    // Constructores
    public TtsGenerationRequest() {}
    
    public TtsGenerationRequest(String text) {
        this.text = text;
    }
    
    public TtsGenerationRequest(String text, String language, String voice) {
        this.text = text;
        this.language = language;
        this.voice = voice;
    }
    
    // Getters y Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getVoice() { return voice; }
    public void setVoice(String voice) { this.voice = voice; }
    
    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }
    
    public String getGenderPreference() { return genderPreference; }
    public void setGenderPreference(String genderPreference) { this.genderPreference = genderPreference; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    // Métodos de utilidad
    public boolean hasValidText() {
        return text != null && !text.trim().isEmpty();
    }
    
    public boolean hasValidSpeed() {
        return speed != null && speed >= 0.5 && speed <= 2.0;
    }
    
    public boolean hasValidTimeout() {
        return timeoutSeconds != null && timeoutSeconds > 0 && timeoutSeconds <= 120;
    }
    
    public boolean hasValidRetries() {
        return maxRetries != null && maxRetries >= 0 && maxRetries <= 5;
    }
    
    public String getSummary() {
        return String.format("TTS Request: '%s' [Lang: %s, Voice: %s, Provider: %s, Speed: %.1f]", 
                text != null ? (text.length() > 50 ? text.substring(0, 50) + "..." : text) : "null",
                language, voice != null ? voice : "auto", provider != null ? provider : "auto", speed);
    }
}
