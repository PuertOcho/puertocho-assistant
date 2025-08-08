package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

public class TtsGenerationResponse {
    
    public enum GenerationStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        PROVIDER_UNAVAILABLE
    }
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("status")
    private GenerationStatus status;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("voice")
    private String voice;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("audio_data")
    private byte[] audioData;
    
    @JsonProperty("audio_duration")
    private Double audioDuration;
    
    @JsonProperty("sample_rate")
    private Integer sampleRate;
    
    @JsonProperty("speed")
    private Double speed;
    
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("debug_info")
    private Map<String, Object> debugInfo;
    
    // Constructores
    public TtsGenerationResponse() {
        this.generatedAt = LocalDateTime.now();
    }
    
    public TtsGenerationResponse(String requestId) {
        this.requestId = requestId;
        this.generatedAt = LocalDateTime.now();
    }
    
    // Métodos de utilidad para crear respuestas
    public static TtsGenerationResponse success(String requestId, String text, String language, 
                                               String voice, String provider, byte[] audioData, 
                                               Double audioDuration, Integer sampleRate, Double speed) {
        TtsGenerationResponse response = new TtsGenerationResponse(requestId);
        response.setSuccess(true);
        response.setStatus(GenerationStatus.SUCCESS);
        response.setText(text);
        response.setLanguage(language);
        response.setVoice(voice);
        response.setProvider(provider);
        response.setAudioData(audioData);
        response.setAudioDuration(audioDuration);
        response.setSampleRate(sampleRate);
        response.setSpeed(speed);
        return response;
    }
    
    public static TtsGenerationResponse failed(String requestId, String text, String errorMessage) {
        TtsGenerationResponse response = new TtsGenerationResponse(requestId);
        response.setSuccess(false);
        response.setStatus(GenerationStatus.FAILED);
        response.setText(text);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    public static TtsGenerationResponse timeout(String requestId, String text, String provider) {
        TtsGenerationResponse response = new TtsGenerationResponse(requestId);
        response.setSuccess(false);
        response.setStatus(GenerationStatus.TIMEOUT);
        response.setText(text);
        response.setProvider(provider);
        response.setErrorMessage("TTS generation timed out");
        return response;
    }
    
    public static TtsGenerationResponse providerUnavailable(String requestId, String text, String provider) {
        TtsGenerationResponse response = new TtsGenerationResponse(requestId);
        response.setSuccess(false);
        response.setStatus(GenerationStatus.PROVIDER_UNAVAILABLE);
        response.setText(text);
        response.setProvider(provider);
        response.setErrorMessage("TTS provider unavailable");
        return response;
    }
    
    // Getters y Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public GenerationStatus getStatus() { return status; }
    public void setStatus(GenerationStatus status) { this.status = status; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getVoice() { return voice; }
    public void setVoice(String voice) { this.voice = voice; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public byte[] getAudioData() { return audioData; }
    public void setAudioData(byte[] audioData) { this.audioData = audioData; }
    
    public Double getAudioDuration() { return audioDuration; }
    public void setAudioDuration(Double audioDuration) { this.audioDuration = audioDuration; }
    
    public Integer getSampleRate() { return sampleRate; }
    public void setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; }
    
    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Map<String, Object> getDebugInfo() { return debugInfo; }
    public void setDebugInfo(Map<String, Object> debugInfo) { this.debugInfo = debugInfo; }
    
    // Métodos de utilidad
    public boolean hasAudioData() {
        return audioData != null && audioData.length > 0;
    }
    
    public int getAudioDataSize() {
        return audioData != null ? audioData.length : 0;
    }
    
    public String getSummary() {
        if (success) {
            return String.format("TTS Success: '%s' [Provider: %s, Voice: %s, Duration: %.2fs, Size: %d bytes]", 
                    text != null ? (text.length() > 30 ? text.substring(0, 30) + "..." : text) : "null",
                    provider, voice, audioDuration != null ? audioDuration : 0.0, getAudioDataSize());
        } else {
            return String.format("TTS Failed: '%s' [Status: %s, Error: %s]", 
                    text != null ? (text.length() > 30 ? text.substring(0, 30) + "..." : text) : "null",
                    status, errorMessage);
        }
    }
}
