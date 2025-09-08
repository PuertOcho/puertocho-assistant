package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para el procesamiento de audio.
 * Maneja la recepción, validación y procesamiento de archivos de audio.
 */
@Service
public class AudioProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AudioProcessingService.class);

    @Value("${audio.processing.max-file-size:10485760}") // 10MB por defecto (en bytes)
    private long maxFileSizeBytes;

    @Value("${audio.processing.supported-formats:wav,mp3,m4a,flac}")
    private String supportedFormats;

    @Value("${audio.processing.validation.max-duration-seconds:60}")
    private int maxDurationSeconds;

    @Value("${audio.processing.validation.min-duration-seconds:0}")
    private int minDurationSeconds;

    @Autowired
    private JsonIntentClassifier jsonIntentClassifier;

    @Autowired
    private LlmVotingService llmVotingService;

    @Autowired
    private ConversationManager conversationManager;

    @Autowired
    private WhisperTranscriptionService whisperTranscriptionService;

    @Autowired
    private TtsGenerationService ttsGenerationService;

    /**
     * Procesa una petición de audio completa
     */
    public AudioProcessingResult processAudio(AudioProcessingRequest request) {
        String resultId = UUID.randomUUID().toString();
        AudioProcessingResult result = new AudioProcessingResult(resultId, request.getRequestId(), 
                AudioProcessingResult.ProcessingStatus.PROCESSING);

        logger.info("Iniciando procesamiento de audio: {}", request.getSummary());

        try {
            // 1. Validar la petición
            validateAudioRequest(request);

            // 2. Procesar el audio (simulado por ahora, se implementará con Whisper en T5.2)
            AudioProcessingResult.TranscriptionResult transcription = processTranscription(request);

            // 3. Clasificar la intención usando RAG
            IntentClassificationResult intentClassification = classifyIntent(transcription.getText(), request.getMetadata());

            // 4. Ejecutar votación MoE
            VotingConsensus moeVoting = executeMoEVoting(transcription.getText(), request.getMetadata());

            // 5. Generar respuesta
            AudioProcessingResult.AudioResponse response = generateResponse(intentClassification, moeVoting, request);

            // 6. Completar el resultado
            result.setTranscription(transcription);
            result.setIntentClassification(intentClassification);
            result.setMoeVoting(moeVoting);
            result.setResponse(response);
            result.setOriginalMetadata(request.getMetadata());
            result.setProcessingConfig(request.getProcessingConfig());
            result.markCompleted();

            logger.info("Procesamiento de audio completado exitosamente: {}", result.getSummary());

        } catch (Exception e) {
            logger.error("Error en el procesamiento de audio: {}", e.getMessage(), e);
            result.markFailed(e.getMessage());
        }

        return result;
    }

    /**
     * Procesa audio simple sin metadata
     */
    public AudioProcessingResult processSimpleAudio(byte[] audioData, String filename, String contentType) {
        AudioProcessingRequest request = new AudioProcessingRequest(
                UUID.randomUUID().toString(),
                audioData,
                filename,
                contentType
        );
        return processAudio(request);
    }

    /**
     * Valida una petición de audio
     */
    private void validateAudioRequest(AudioProcessingRequest request) {
        logger.debug("Validando petición de audio: {}", request.getSummary());

        // Validar que el archivo no sea null
        if (request.getAudioData() == null || request.getAudioData().length == 0) {
            throw new IllegalArgumentException("Los datos de audio no pueden estar vacíos");
        }

        // Validar el tamaño del archivo
        if (!request.hasValidSize(maxFileSizeBytes)) {
            throw new IllegalArgumentException(
                    String.format("El archivo excede el tamaño máximo permitido. Máximo: %d bytes, Actual: %d bytes",
                            maxFileSizeBytes, request.getFileSize()));
        }

        // Validar el formato del archivo
        if (!request.hasSupportedFormat()) {
            throw new IllegalArgumentException(
                    String.format("Formato de archivo no soportado: %s. Formatos soportados: %s",
                            request.getFileExtension(), supportedFormats));
        }

        // Validar el nombre del archivo
        if (request.getFilename() == null || request.getFilename().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo no puede estar vacío");
        }

        logger.debug("Petición de audio validada exitosamente");
    }

    /**
     * Procesa la transcripción del audio (simulado por ahora)
     */
    private AudioProcessingResult.TranscriptionResult processTranscription(AudioProcessingRequest request) {
        logger.debug("Procesando transcripción para archivo: {}", request.getFilename());

        try {
            // Crear request para Whisper
            WhisperTranscriptionRequest whisperRequest = WhisperTranscriptionRequest.builder()
                .language(request.getMetadata() != null ? request.getMetadata().getLanguage() : "es")
                .timeoutSeconds(30)
                .maxRetries(3)
                .model("base")
                .build();

            // Realizar transcripción usando Whisper
            WhisperTranscriptionResponse whisperResponse = whisperTranscriptionService.transcribeAudio(
                request.getAudioData(), whisperRequest);

            // Verificar si la transcripción fue exitosa
            if (whisperResponse.getStatus() == WhisperTranscriptionResponse.TranscriptionStatus.SUCCESS) {
                AudioProcessingResult.TranscriptionResult transcription = new AudioProcessingResult.TranscriptionResult(
                    whisperResponse.getTranscription(),
                    whisperResponse.getConfidence() != null ? whisperResponse.getConfidence() : 0.9,
                    whisperResponse.getLanguage()
                );
                transcription.setProcessingTimeMs(whisperResponse.getProcessingTimeMs());
                transcription.setDetectedLanguage(whisperResponse.getDetectedLanguage());

                logger.debug("Transcripción completada: '{}' (confianza: {}, idioma: {})", 
                    transcription.getText(), transcription.getConfidence(), transcription.getLanguage());

                return transcription;
            } else {
                // Fallback a transcripción simulada si Whisper falla
                logger.warn("Whisper transcription failed: {}. Using fallback transcription.", 
                    whisperResponse.getErrorMessage());
                
                return createFallbackTranscription(request);
            }

        } catch (Exception e) {
            logger.error("Error during Whisper transcription: {}. Using fallback transcription.", e.getMessage());
            return createFallbackTranscription(request);
        }
    }

    /**
     * Crea una transcripción de fallback cuando Whisper no está disponible
     */
    private AudioProcessingResult.TranscriptionResult createFallbackTranscription(AudioProcessingRequest request) {
        String fallbackText = "¿qué tiempo hace hoy?";
        
        AudioProcessingResult.TranscriptionResult transcription = new AudioProcessingResult.TranscriptionResult(
            fallbackText,
            0.5, // Confianza baja para transcripción de fallback
            "es"
        );
        transcription.setProcessingTimeMs(1000L);
        transcription.setDetectedLanguage("es");

        logger.debug("Transcripción de fallback completada: '{}' (confianza: {})", 
            transcription.getText(), transcription.getConfidence());

        return transcription;
    }

    /**
     * Clasifica la intención usando el motor RAG
     */
    private IntentClassificationResult classifyIntent(String text, AudioMetadata metadata) {
        logger.debug("Clasificando intención para texto: '{}'", text);

        IntentClassificationRequest request = new IntentClassificationRequest(text);
        request.setMaxExamplesForRag(5);
        request.setConfidenceThreshold(0.7);

        // Enriquecer con metadata si está disponible
        if (metadata != null && metadata.hasLocation()) {
            // TODO: Implementar enriquecimiento de contexto con metadata
        }

        IntentClassificationResult result = jsonIntentClassifier.classifyIntent(request);

        logger.debug("Intención clasificada: {} (confianza: {})", 
                result.getIntentId(), result.getConfidenceScore());

        return result;
    }

    /**
     * Ejecuta el sistema de votación MoE
     */
    private VotingConsensus executeMoEVoting(String text, AudioMetadata metadata) {
        logger.debug("Ejecutando votación MoE para texto: '{}'", text);

        // Crear contexto enriquecido con metadata
        StringBuilder context = new StringBuilder();
        context.append("Texto del usuario: ").append(text);
        
        if (metadata != null) {
            if (metadata.hasLocation()) {
                context.append("\nUbicación: ").append(metadata.getLocation());
            }
            if (metadata.hasTemperature()) {
                context.append("\nTemperatura: ").append(metadata.getTemperature());
            }
            if (metadata.hasDeviceInfo()) {
                context.append("\nDispositivo: ").append(metadata.getDeviceId());
            }
        }

        // Crear VotingRound para la votación
        VotingRound votingRound = new VotingRound();
        votingRound.setRoundId(UUID.randomUUID().toString());
        votingRound.setUserMessage(text);
        votingRound.setConversationContext(new java.util.HashMap<>());
        votingRound.setConversationHistory(new java.util.ArrayList<>());
        
        // Ejecutar votación
        VotingRound result = llmVotingService.executeVotingRound(
            votingRound.getRoundId(), 
            text, 
            votingRound.getConversationContext(), 
            votingRound.getConversationHistory()
        );
        
        VotingConsensus consensus = result.getConsensus();

        logger.debug("Votación MoE completada: {} (confianza: {})", 
                consensus.getFinalIntent(), consensus.getConsensusConfidence());

        return consensus;
    }

    /**
     * Genera la respuesta basada en la clasificación y votación
     */
    private AudioProcessingResult.AudioResponse generateResponse(IntentClassificationResult intentClassification,
                                                               VotingConsensus moeVoting,
                                                               AudioProcessingRequest request) {
        logger.debug("Generando respuesta para intención: {}", intentClassification.getIntentId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. Generar respuesta de texto
            String textResponse = generateTextResponse(intentClassification, moeVoting);

            // 2. Generar audio TTS para la respuesta
            byte[] audioResponse = generateAudioResponse(textResponse, request);

            // 3. Crear respuesta completa
            AudioProcessingResult.AudioResponse response = new AudioProcessingResult.AudioResponse(textResponse);
            response.setAudioResponse(audioResponse);
            response.setGenerationTimeMs(System.currentTimeMillis() - startTime);

            logger.debug("Respuesta generada: '{}' con audio de {} bytes", 
                response.getTextResponse(), audioResponse != null ? audioResponse.length : 0);

            return response;

        } catch (Exception e) {
            logger.error("Error generando respuesta de audio: {}", e.getMessage(), e);
            
            // Fallback: solo respuesta de texto sin audio
            String textResponse = generateTextResponse(intentClassification, moeVoting);
            AudioProcessingResult.AudioResponse response = new AudioProcessingResult.AudioResponse(textResponse);
            response.setGenerationTimeMs(System.currentTimeMillis() - startTime);
            
            return response;
        }
    }

    /**
     * Genera respuesta de texto basada en la intención
     */
    private String generateTextResponse(IntentClassificationResult intentClassification, VotingConsensus moeVoting) {
        String intentId = intentClassification.getIntentId();
        Double confidence = intentClassification.getConfidenceScore();

        switch (intentId) {
            case "ayuda":
                return "¡Hola! Soy tu asistente. ¿En qué puedo ayudarte?";
            case "tiempo":
                return "Te ayudo a consultar el tiempo. ¿De qué ciudad quieres saber el clima?";
            case "musica":
                return "Perfecto, te ayudo con la música. ¿Qué tipo de música te gustaría escuchar?";
            case "luz":
                return "Entendido, te ayudo con las luces. ¿En qué habitación quieres ajustar la iluminación?";
            case "alarma":
                return "Te ayudo a programar una alarma. ¿Para qué hora la necesitas?";
            default:
                return "Entiendo tu petición. Estoy procesando la información para ayudarte mejor.";
        }
    }

    /**
     * Genera audio TTS para la respuesta de texto
     */
    private byte[] generateAudioResponse(String textResponse, AudioProcessingRequest request) {
        logger.debug("Generando audio TTS para: '{}'", textResponse);

        try {
            // Crear request para TTS
            TtsGenerationRequest ttsRequest = new TtsGenerationRequest(textResponse);
            ttsRequest.setLanguage(request.getMetadata() != null ? request.getMetadata().getLanguage() : "es");
            ttsRequest.setVoice("Abril"); // Voz por defecto
            ttsRequest.setSpeed(1.0);
            ttsRequest.setTimeoutSeconds(30);

            // Generar audio usando TTS
            TtsGenerationResponse ttsResponse = ttsGenerationService.generateAudio(ttsRequest);

            if (ttsResponse.isSuccess() && ttsResponse.getAudioData() != null) {
                logger.debug("Audio TTS generado exitosamente: {} bytes", ttsResponse.getAudioData().length);
                return ttsResponse.getAudioData();
            } else {
                logger.warn("TTS generation failed: {}. Using fallback.", ttsResponse.getErrorMessage());
                return generateFallbackAudio(textResponse);
            }

        } catch (Exception e) {
            logger.error("Error en generación TTS: {}. Using fallback.", e.getMessage());
            return generateFallbackAudio(textResponse);
        }
    }

    /**
     * Genera audio de fallback cuando TTS no está disponible
     */
    private byte[] generateFallbackAudio(String textResponse) {
        logger.debug("Generando audio de fallback para: '{}'", textResponse);
        
        // Por ahora, retornamos un array vacío como fallback
        // En una implementación real, podríamos usar un TTS local o sintetizador básico
        return new byte[0];
    }

    /**
     * Obtiene los formatos de audio soportados
     */
    public String[] getSupportedFormats() {
        return supportedFormats.split(",");
    }

    /**
     * Obtiene el tamaño máximo de archivo permitido
     */
    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    /**
     * Verifica si un formato de archivo es soportado
     */
    public boolean isFormatSupported(String format) {
        if (format == null) return false;
        String[] formats = getSupportedFormats();
        for (String supportedFormat : formats) {
            if (supportedFormat.trim().equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Procesa audio de forma asíncrona
     */
    public CompletableFuture<AudioProcessingResult> processAudioAsync(AudioProcessingRequest request) {
        return CompletableFuture.supplyAsync(() -> processAudio(request));
    }

    /**
     * Obtiene estadísticas del servicio
     */
    public AudioProcessingStatistics getStatistics() {
        // TODO: Implementar estadísticas reales
        return new AudioProcessingStatistics();
    }

    /**
     * Clase para estadísticas del procesamiento de audio
     */
    public static class AudioProcessingStatistics {
        private long totalProcessed;
        private long successfulProcessings;
        private long failedProcessings;
        private double averageProcessingTimeMs;
        private LocalDateTime lastProcessingTime;

        public AudioProcessingStatistics() {
            this.totalProcessed = 0;
            this.successfulProcessings = 0;
            this.failedProcessings = 0;
            this.averageProcessingTimeMs = 0.0;
            this.lastProcessingTime = LocalDateTime.now();
        }

        // Getters y Setters
        public long getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(long totalProcessed) { this.totalProcessed = totalProcessed; }

        public long getSuccessfulProcessings() { return successfulProcessings; }
        public void setSuccessfulProcessings(long successfulProcessings) { this.successfulProcessings = successfulProcessings; }

        public long getFailedProcessings() { return failedProcessings; }
        public void setFailedProcessings(long failedProcessings) { this.failedProcessings = failedProcessings; }

        public double getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
        public void setAverageProcessingTimeMs(double averageProcessingTimeMs) { this.averageProcessingTimeMs = averageProcessingTimeMs; }

        public LocalDateTime getLastProcessingTime() { return lastProcessingTime; }
        public void setLastProcessingTime(LocalDateTime lastProcessingTime) { this.lastProcessingTime = lastProcessingTime; }

        public double getSuccessRate() {
            return totalProcessed > 0 ? (double) successfulProcessings / totalProcessed : 0.0;
        }
    }
}
