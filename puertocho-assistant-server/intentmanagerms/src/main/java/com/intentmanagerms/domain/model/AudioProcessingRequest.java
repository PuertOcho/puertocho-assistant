package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Modelo de dominio para peticiones de procesamiento de audio.
 * Contiene el audio y metadata asociada para su procesamiento.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudioProcessingRequest {

    /**
     * Identificador único de la petición
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * Datos del archivo de audio en bytes
     */
    @JsonProperty("audio_data")
    private byte[] audioData;

    /**
     * Nombre del archivo de audio
     */
    @JsonProperty("filename")
    private String filename;

    /**
     * Tipo MIME del archivo de audio
     */
    @JsonProperty("content_type")
    private String contentType;

    /**
     * Tamaño del archivo en bytes
     */
    @JsonProperty("file_size")
    private Long fileSize;

    /**
     * Metadata contextual del audio
     */
    @JsonProperty("metadata")
    private AudioMetadata metadata;

    /**
     * Configuración de procesamiento específica para esta petición
     */
    @JsonProperty("processing_config")
    private AudioProcessingConfig processingConfig;

    /**
     * Constructor por defecto
     */
    public AudioProcessingRequest() {
    }

    /**
     * Constructor básico
     */
    public AudioProcessingRequest(String requestId, byte[] audioData, String filename, String contentType) {
        this.requestId = requestId;
        this.audioData = audioData;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = audioData != null ? (long) audioData.length : 0L;
    }

    /**
     * Constructor completo
     */
    public AudioProcessingRequest(String requestId, byte[] audioData, String filename, String contentType,
                                 Long fileSize, AudioMetadata metadata, AudioProcessingConfig processingConfig) {
        this.requestId = requestId;
        this.audioData = audioData;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize != null ? fileSize : (audioData != null ? (long) audioData.length : 0L);
        this.metadata = metadata;
        this.processingConfig = processingConfig;
    }

    // Getters y Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public byte[] getAudioData() { return audioData; }
    public void setAudioData(byte[] audioData) { 
        this.audioData = audioData;
        this.fileSize = audioData != null ? (long) audioData.length : 0L;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public AudioMetadata getMetadata() { return metadata; }
    public void setMetadata(AudioMetadata metadata) { this.metadata = metadata; }

    public AudioProcessingConfig getProcessingConfig() { return processingConfig; }
    public void setProcessingConfig(AudioProcessingConfig processingConfig) { this.processingConfig = processingConfig; }

    /**
     * Obtiene la extensión del archivo
     */
    public String getFileExtension() {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1).toLowerCase() : "";
    }

    /**
     * Verifica si el archivo tiene un formato soportado
     */
    public boolean hasSupportedFormat() {
        String extension = getFileExtension();
        return extension.matches("^(wav|mp3|m4a|flac)$");
    }

    /**
     * Verifica si el archivo tiene un tamaño válido
     */
    public boolean hasValidSize(long maxSizeBytes) {
        return fileSize != null && fileSize > 0 && fileSize <= maxSizeBytes;
    }

    /**
     * Obtiene un resumen de la petición para logging
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("AudioProcessingRequest{");
        summary.append("requestId=").append(requestId);
        summary.append(", filename=").append(filename);
        summary.append(", contentType=").append(contentType);
        summary.append(", fileSize=").append(fileSize).append(" bytes");
        if (metadata != null) {
            summary.append(", metadata=").append(metadata.getSummary());
        }
        summary.append("}");
        return summary.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioProcessingRequest that = (AudioProcessingRequest) o;
        return Objects.equals(requestId, that.requestId) &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(fileSize, that.fileSize) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(processingConfig, that.processingConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, filename, contentType, fileSize, metadata, processingConfig);
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /**
     * Configuración específica para el procesamiento de audio
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AudioProcessingConfig {
        
        /**
         * Idioma preferido para la transcripción
         */
        @JsonProperty("preferred_language")
        private String preferredLanguage;

        /**
         * Modelo de Whisper a usar
         */
        @JsonProperty("whisper_model")
        private String whisperModel;

        /**
         * Si debe generar respuesta de audio
         */
        @JsonProperty("generate_audio_response")
        private Boolean generateAudioResponse;

        /**
         * Si debe generar respuesta de texto
         */
        @JsonProperty("generate_text_response")
        private Boolean generateTextResponse;

        /**
         * Timeout específico para esta petición en segundos
         */
        @JsonProperty("timeout_seconds")
        private Integer timeoutSeconds;

        /**
         * Constructor por defecto
         */
        public AudioProcessingConfig() {
        }

        /**
         * Constructor básico
         */
        public AudioProcessingConfig(String preferredLanguage, String whisperModel) {
            this.preferredLanguage = preferredLanguage;
            this.whisperModel = whisperModel;
            this.generateAudioResponse = true;
            this.generateTextResponse = true;
        }

        /**
         * Constructor completo
         */
        public AudioProcessingConfig(String preferredLanguage, String whisperModel, 
                                   Boolean generateAudioResponse, Boolean generateTextResponse,
                                   Integer timeoutSeconds) {
            this.preferredLanguage = preferredLanguage;
            this.whisperModel = whisperModel;
            this.generateAudioResponse = generateAudioResponse;
            this.generateTextResponse = generateTextResponse;
            this.timeoutSeconds = timeoutSeconds;
        }

        // Getters y Setters
        public String getPreferredLanguage() { return preferredLanguage; }
        public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

        public String getWhisperModel() { return whisperModel; }
        public void setWhisperModel(String whisperModel) { this.whisperModel = whisperModel; }

        public Boolean getGenerateAudioResponse() { return generateAudioResponse; }
        public void setGenerateAudioResponse(Boolean generateAudioResponse) { this.generateAudioResponse = generateAudioResponse; }

        public Boolean getGenerateTextResponse() { return generateTextResponse; }
        public void setGenerateTextResponse(Boolean generateTextResponse) { this.generateTextResponse = generateTextResponse; }

        public Integer getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AudioProcessingConfig that = (AudioProcessingConfig) o;
            return Objects.equals(preferredLanguage, that.preferredLanguage) &&
                    Objects.equals(whisperModel, that.whisperModel) &&
                    Objects.equals(generateAudioResponse, that.generateAudioResponse) &&
                    Objects.equals(generateTextResponse, that.generateTextResponse) &&
                    Objects.equals(timeoutSeconds, that.timeoutSeconds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(preferredLanguage, whisperModel, generateAudioResponse, generateTextResponse, timeoutSeconds);
        }

        @Override
        public String toString() {
            return "AudioProcessingConfig{" +
                    "preferredLanguage='" + preferredLanguage + '\'' +
                    ", whisperModel='" + whisperModel + '\'' +
                    ", generateAudioResponse=" + generateAudioResponse +
                    ", generateTextResponse=" + generateTextResponse +
                    ", timeoutSeconds=" + timeoutSeconds +
                    '}';
        }
    }
}
