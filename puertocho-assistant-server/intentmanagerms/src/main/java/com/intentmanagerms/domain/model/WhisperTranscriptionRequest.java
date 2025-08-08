package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model para configuración de transcripción de audio usando Whisper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhisperTranscriptionRequest {
    
    /**
     * Idioma para la transcripción (es, en, etc.)
     */
    @JsonProperty("language")
    private String language;
    
    /**
     * Timeout en segundos para la transcripción
     */
    @JsonProperty("timeout_seconds")
    private Integer timeoutSeconds;
    
    /**
     * Número máximo de reintentos
     */
    @JsonProperty("max_retries")
    private Integer maxRetries;
    
    /**
     * Modelo de Whisper a usar (base, small, medium, large)
     */
    @JsonProperty("model")
    private String model;
}
