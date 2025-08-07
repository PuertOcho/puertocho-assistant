package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Modelo de dominio para resultados de procesamiento de audio.
 * Contiene la transcripción, clasificación de intención y respuesta generada.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudioProcessingResult {

    /**
     * Identificador único del resultado
     */
    @JsonProperty("result_id")
    private String resultId;

    /**
     * Identificador de la petición original
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * Estado del procesamiento
     */
    @JsonProperty("status")
    private ProcessingStatus status;

    /**
     * Mensaje de error si el procesamiento falló
     */
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * Timestamp de inicio del procesamiento
     */
    @JsonProperty("start_time")
    private LocalDateTime startTime;

    /**
     * Timestamp de fin del procesamiento
     */
    @JsonProperty("end_time")
    private LocalDateTime endTime;

    /**
     * Duración del procesamiento en milisegundos
     */
    @JsonProperty("processing_duration_ms")
    private Long processingDurationMs;

    /**
     * Resultado de la transcripción
     */
    @JsonProperty("transcription")
    private TranscriptionResult transcription;

    /**
     * Resultado de la clasificación de intención
     */
    @JsonProperty("intent_classification")
    private IntentClassificationResult intentClassification;

    /**
     * Resultado del sistema de votación MoE
     */
    @JsonProperty("moe_voting")
    private VotingConsensus moeVoting;

    /**
     * Respuesta generada
     */
    @JsonProperty("response")
    private AudioResponse response;

    /**
     * Metadata del audio original
     */
    @JsonProperty("original_metadata")
    private AudioMetadata originalMetadata;

    /**
     * Configuración de procesamiento utilizada
     */
    @JsonProperty("processing_config")
    private AudioProcessingRequest.AudioProcessingConfig processingConfig;

    /**
     * Constructor por defecto
     */
    public AudioProcessingResult() {
        this.startTime = LocalDateTime.now();
    }

    /**
     * Constructor básico
     */
    public AudioProcessingResult(String resultId, String requestId, ProcessingStatus status) {
        this.resultId = resultId;
        this.requestId = requestId;
        this.status = status;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Constructor completo
     */
    public AudioProcessingResult(String resultId, String requestId, ProcessingStatus status,
                               String errorMessage, LocalDateTime startTime, LocalDateTime endTime,
                               Long processingDurationMs, TranscriptionResult transcription,
                               IntentClassificationResult intentClassification, VotingConsensus moeVoting,
                               AudioResponse response, AudioMetadata originalMetadata,
                               AudioProcessingRequest.AudioProcessingConfig processingConfig) {
        this.resultId = resultId;
        this.requestId = requestId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.startTime = startTime;
        this.endTime = endTime;
        this.processingDurationMs = processingDurationMs;
        this.transcription = transcription;
        this.intentClassification = intentClassification;
        this.moeVoting = moeVoting;
        this.response = response;
        this.originalMetadata = originalMetadata;
        this.processingConfig = processingConfig;
    }

    // Getters y Setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public ProcessingStatus getStatus() { return status; }
    public void setStatus(ProcessingStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Long getProcessingDurationMs() { return processingDurationMs; }
    public void setProcessingDurationMs(Long processingDurationMs) { this.processingDurationMs = processingDurationMs; }

    public TranscriptionResult getTranscription() { return transcription; }
    public void setTranscription(TranscriptionResult transcription) { this.transcription = transcription; }

    public IntentClassificationResult getIntentClassification() { return intentClassification; }
    public void setIntentClassification(IntentClassificationResult intentClassification) { this.intentClassification = intentClassification; }

    public VotingConsensus getMoeVoting() { return moeVoting; }
    public void setMoeVoting(VotingConsensus moeVoting) { this.moeVoting = moeVoting; }

    public AudioResponse getResponse() { return response; }
    public void setResponse(AudioResponse response) { this.response = response; }

    public AudioMetadata getOriginalMetadata() { return originalMetadata; }
    public void setOriginalMetadata(AudioMetadata originalMetadata) { this.originalMetadata = originalMetadata; }

    public AudioProcessingRequest.AudioProcessingConfig getProcessingConfig() { return processingConfig; }
    public void setProcessingConfig(AudioProcessingRequest.AudioProcessingConfig processingConfig) { this.processingConfig = processingConfig; }

    /**
     * Marca el procesamiento como completado
     */
    public void markCompleted() {
        this.status = ProcessingStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.processingDurationMs = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    /**
     * Marca el procesamiento como fallido
     */
    public void markFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.processingDurationMs = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    /**
     * Verifica si el procesamiento fue exitoso
     */
    public boolean isSuccessful() {
        return ProcessingStatus.COMPLETED.equals(this.status);
    }

    /**
     * Verifica si el procesamiento falló
     */
    public boolean isFailed() {
        return ProcessingStatus.FAILED.equals(this.status);
    }

    /**
     * Obtiene un resumen del resultado para logging
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("AudioProcessingResult{");
        summary.append("resultId=").append(resultId);
        summary.append(", requestId=").append(requestId);
        summary.append(", status=").append(status);
        if (processingDurationMs != null) {
            summary.append(", duration=").append(processingDurationMs).append("ms");
        }
        if (transcription != null) {
            summary.append(", transcription=").append(transcription.getText());
        }
        if (intentClassification != null) {
            summary.append(", intent=").append(intentClassification.getIntentId());
        }
        if (errorMessage != null) {
            summary.append(", error=").append(errorMessage);
        }
        summary.append("}");
        return summary.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioProcessingResult that = (AudioProcessingResult) o;
        return Objects.equals(resultId, that.resultId) &&
                Objects.equals(requestId, that.requestId) &&
                status == that.status &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(processingDurationMs, that.processingDurationMs) &&
                Objects.equals(transcription, that.transcription) &&
                Objects.equals(intentClassification, that.intentClassification) &&
                Objects.equals(moeVoting, that.moeVoting) &&
                Objects.equals(response, that.response) &&
                Objects.equals(originalMetadata, that.originalMetadata) &&
                Objects.equals(processingConfig, that.processingConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultId, requestId, status, errorMessage, startTime, endTime,
                processingDurationMs, transcription, intentClassification, moeVoting, response,
                originalMetadata, processingConfig);
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /**
     * Estados del procesamiento de audio
     */
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Resultado de la transcripción
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TranscriptionResult {
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("confidence")
        private Double confidence;
        
        @JsonProperty("language")
        private String language;
        
        @JsonProperty("segments")
        private List<TranscriptionSegment> segments;
        
        @JsonProperty("processing_time_ms")
        private Long processingTimeMs;

        public TranscriptionResult() {
        }

        public TranscriptionResult(String text, Double confidence, String language) {
            this.text = text;
            this.confidence = confidence;
            this.language = language;
        }

        // Getters y Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public List<TranscriptionSegment> getSegments() { return segments; }
        public void setSegments(List<TranscriptionSegment> segments) { this.segments = segments; }

        public Long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TranscriptionResult that = (TranscriptionResult) o;
            return Objects.equals(text, that.text) &&
                    Objects.equals(confidence, that.confidence) &&
                    Objects.equals(language, that.language) &&
                    Objects.equals(segments, that.segments) &&
                    Objects.equals(processingTimeMs, that.processingTimeMs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, confidence, language, segments, processingTimeMs);
        }

        @Override
        public String toString() {
            return "TranscriptionResult{" +
                    "text='" + text + '\'' +
                    ", confidence=" + confidence +
                    ", language='" + language + '\'' +
                    ", processingTimeMs=" + processingTimeMs +
                    '}';
        }
    }

    /**
     * Segmento de transcripción
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TranscriptionSegment {
        
        @JsonProperty("start")
        private Double start;
        
        @JsonProperty("end")
        private Double end;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("confidence")
        private Double confidence;

        public TranscriptionSegment() {
        }

        public TranscriptionSegment(Double start, Double end, String text, Double confidence) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.confidence = confidence;
        }

        // Getters y Setters
        public Double getStart() { return start; }
        public void setStart(Double start) { this.start = start; }

        public Double getEnd() { return end; }
        public void setEnd(Double end) { this.end = end; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TranscriptionSegment that = (TranscriptionSegment) o;
            return Objects.equals(start, that.start) &&
                    Objects.equals(end, that.end) &&
                    Objects.equals(text, that.text) &&
                    Objects.equals(confidence, that.confidence);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, text, confidence);
        }

        @Override
        public String toString() {
            return "TranscriptionSegment{" +
                    "start=" + start +
                    ", end=" + end +
                    ", text='" + text + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }

    /**
     * Respuesta de audio generada
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AudioResponse {
        
        @JsonProperty("text_response")
        private String textResponse;
        
        @JsonProperty("audio_response")
        private byte[] audioResponse;
        
        @JsonProperty("audio_format")
        private String audioFormat;
        
        @JsonProperty("audio_duration_seconds")
        private Double audioDurationSeconds;
        
        @JsonProperty("generation_time_ms")
        private Long generationTimeMs;

        public AudioResponse() {
        }

        public AudioResponse(String textResponse) {
            this.textResponse = textResponse;
        }

        public AudioResponse(String textResponse, byte[] audioResponse, String audioFormat) {
            this.textResponse = textResponse;
            this.audioResponse = audioResponse;
            this.audioFormat = audioFormat;
        }

        // Getters y Setters
        public String getTextResponse() { return textResponse; }
        public void setTextResponse(String textResponse) { this.textResponse = textResponse; }

        public byte[] getAudioResponse() { return audioResponse; }
        public void setAudioResponse(byte[] audioResponse) { this.audioResponse = audioResponse; }

        public String getAudioFormat() { return audioFormat; }
        public void setAudioFormat(String audioFormat) { this.audioFormat = audioFormat; }

        public Double getAudioDurationSeconds() { return audioDurationSeconds; }
        public void setAudioDurationSeconds(Double audioDurationSeconds) { this.audioDurationSeconds = audioDurationSeconds; }

        public Long getGenerationTimeMs() { return generationTimeMs; }
        public void setGenerationTimeMs(Long generationTimeMs) { this.generationTimeMs = generationTimeMs; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AudioResponse that = (AudioResponse) o;
            return Objects.equals(textResponse, that.textResponse) &&
                    Objects.equals(audioFormat, that.audioFormat) &&
                    Objects.equals(audioDurationSeconds, that.audioDurationSeconds) &&
                    Objects.equals(generationTimeMs, that.generationTimeMs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(textResponse, audioFormat, audioDurationSeconds, generationTimeMs);
        }

        @Override
        public String toString() {
            return "AudioResponse{" +
                    "textResponse='" + textResponse + '\'' +
                    ", audioFormat='" + audioFormat + '\'' +
                    ", audioDurationSeconds=" + audioDurationSeconds +
                    ", generationTimeMs=" + generationTimeMs +
                    '}';
        }
    }
}
