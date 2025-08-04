package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Modelo para peticiones de clasificación de intenciones.
 * Soporta texto directo y metadata contextual para futuras integraciones con audio.
 */
public class IntentClassificationRequest {
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("context_metadata")
    private Map<String, Object> contextMetadata;
    
    @JsonProperty("audio_metadata")
    private AudioMetadata audioMetadata;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("max_examples_for_rag")
    private Integer maxExamplesForRag;
    
    @JsonProperty("confidence_threshold")
    private Double confidenceThreshold;
    
    @JsonProperty("enable_fallback")
    private Boolean enableFallback;
    
    public IntentClassificationRequest() {
        this.timestamp = LocalDateTime.now();
        this.enableFallback = true;
    }
    
    public IntentClassificationRequest(String text) {
        this();
        this.text = text;
    }
    
    public IntentClassificationRequest(String text, String sessionId) {
        this(text);
        this.sessionId = sessionId;
    }
    
    // Getters y Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Map<String, Object> getContextMetadata() {
        return contextMetadata;
    }
    
    public void setContextMetadata(Map<String, Object> contextMetadata) {
        this.contextMetadata = contextMetadata;
    }
    
    public AudioMetadata getAudioMetadata() {
        return audioMetadata;
    }
    
    public void setAudioMetadata(AudioMetadata audioMetadata) {
        this.audioMetadata = audioMetadata;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getMaxExamplesForRag() {
        return maxExamplesForRag;
    }
    
    public void setMaxExamplesForRag(Integer maxExamplesForRag) {
        this.maxExamplesForRag = maxExamplesForRag;
    }
    
    public Double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    public void setConfidenceThreshold(Double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
    
    public Boolean getEnableFallback() {
        return enableFallback;
    }
    
    public void setEnableFallback(Boolean enableFallback) {
        this.enableFallback = enableFallback;
    }
    
    /**
     * Metadata específica para audio que se usará más adelante
     */
    public static class AudioMetadata {
        @JsonProperty("audio_file_path")
        private String audioFilePath;
        
        @JsonProperty("transcription_confidence")
        private Double transcriptionConfidence;
        
        @JsonProperty("audio_duration_ms")
        private Long audioDurationMs;
        
        @JsonProperty("sample_rate")
        private Integer sampleRate;
        
        @JsonProperty("channels")
        private Integer channels;
        
        @JsonProperty("device_info")
        private Map<String, Object> deviceInfo;
        
        @JsonProperty("location")
        private Map<String, Object> location;
        
        @JsonProperty("ambient_data")
        private Map<String, Object> ambientData;
        
        public AudioMetadata() {}
        
        // Getters y Setters
        public String getAudioFilePath() {
            return audioFilePath;
        }
        
        public void setAudioFilePath(String audioFilePath) {
            this.audioFilePath = audioFilePath;
        }
        
        public Double getTranscriptionConfidence() {
            return transcriptionConfidence;
        }
        
        public void setTranscriptionConfidence(Double transcriptionConfidence) {
            this.transcriptionConfidence = transcriptionConfidence;
        }
        
        public Long getAudioDurationMs() {
            return audioDurationMs;
        }
        
        public void setAudioDurationMs(Long audioDurationMs) {
            this.audioDurationMs = audioDurationMs;
        }
        
        public Integer getSampleRate() {
            return sampleRate;
        }
        
        public void setSampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
        }
        
        public Integer getChannels() {
            return channels;
        }
        
        public void setChannels(Integer channels) {
            this.channels = channels;
        }
        
        public Map<String, Object> getDeviceInfo() {
            return deviceInfo;
        }
        
        public void setDeviceInfo(Map<String, Object> deviceInfo) {
            this.deviceInfo = deviceInfo;
        }
        
        public Map<String, Object> getLocation() {
            return location;
        }
        
        public void setLocation(Map<String, Object> location) {
            this.location = location;
        }
        
        public Map<String, Object> getAmbientData() {
            return ambientData;
        }
        
        public void setAmbientData(Map<String, Object> ambientData) {
            this.ambientData = ambientData;
        }
        
        @Override
        public String toString() {
            return "AudioMetadata{" +
                    "audioFilePath='" + audioFilePath + '\'' +
                    ", transcriptionConfidence=" + transcriptionConfidence +
                    ", audioDurationMs=" + audioDurationMs +
                    ", sampleRate=" + sampleRate +
                    ", channels=" + channels +
                    ", deviceInfo=" + deviceInfo +
                    ", location=" + location +
                    ", ambientData=" + ambientData +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "IntentClassificationRequest{" +
                "text='" + text + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", contextMetadata=" + contextMetadata +
                ", audioMetadata=" + audioMetadata +
                ", timestamp=" + timestamp +
                ", maxExamplesForRag=" + maxExamplesForRag +
                ", confidenceThreshold=" + confidenceThreshold +
                ", enableFallback=" + enableFallback +
                '}';
    }
} 