package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response model para transcripción de audio usando Whisper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhisperTranscriptionResponse {
    
    /**
     * Texto transcrito del audio
     */
    @JsonProperty("transcription")
    private String transcription;
    
    /**
     * Idioma usado para la transcripción
     */
    @JsonProperty("language")
    private String language;
    
    /**
     * Idioma detectado automáticamente por Whisper
     */
    @JsonProperty("detected_language")
    private String detectedLanguage;
    
    /**
     * Confianza de la transcripción (0.0 - 1.0)
     */
    @JsonProperty("confidence")
    private Double confidence;
    
    /**
     * Duración del audio en segundos
     */
    @JsonProperty("duration_seconds")
    private Double durationSeconds;
    
    /**
     * Timestamp de cuando se realizó la transcripción
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    /**
     * Información del archivo de debug si está habilitado
     */
    @JsonProperty("debug_info")
    private DebugInfo debugInfo;
    
    /**
     * Estado de la transcripción
     */
    @JsonProperty("status")
    private TranscriptionStatus status;
    
    /**
     * Mensaje de error si la transcripción falló
     */
    @JsonProperty("error_message")
    private String errorMessage;
    
    /**
     * Tiempo de procesamiento en milisegundos
     */
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebugInfo {
        /**
         * Nombre del archivo de audio guardado para debug
         */
        @JsonProperty("debug_audio_file")
        private String debugAudioFile;
        
        /**
         * URL para acceder al archivo de debug
         */
        @JsonProperty("debug_audio_url")
        private String debugAudioUrl;
        
        /**
         * Tamaño del archivo de audio en bytes
         */
        @JsonProperty("file_size_bytes")
        private Long fileSizeBytes;
    }
    
    public enum TranscriptionStatus {
        SUCCESS,
        ERROR,
        TIMEOUT,
        INVALID_AUDIO,
        SERVICE_UNAVAILABLE
    }
}
