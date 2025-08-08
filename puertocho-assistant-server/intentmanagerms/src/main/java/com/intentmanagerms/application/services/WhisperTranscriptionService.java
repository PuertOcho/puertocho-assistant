package com.intentmanagerms.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.domain.model.WhisperTranscriptionRequest;
import com.intentmanagerms.domain.model.WhisperTranscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para transcripción de audio usando Whisper
 * Se comunica con el servicio whisper-api para realizar transcripciones
 */
@Service
public class WhisperTranscriptionService {
    
    private static final Logger log = LoggerFactory.getLogger(WhisperTranscriptionService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${whisper.service.url:http://192.168.1.88:5000}")
    private String whisperServiceUrl;
    
    @Value("${whisper.enabled:true}")
    private boolean whisperEnabled;
    
    @Value("${whisper.timeout:30}")
    private int defaultTimeout;
    
    @Value("${whisper.max-retries:3}")
    private int maxRetries;
    
    @Value("${whisper.supported-languages:es,en}")
    private String supportedLanguages;
    
    @Value("${whisper.model:base}")
    private String defaultModel;
    
    public WhisperTranscriptionService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Transcribe audio usando Whisper
     * 
     * @param audioData Datos del archivo de audio
     * @param request Configuración de la transcripción
     * @return Respuesta con la transcripción
     */
    public WhisperTranscriptionResponse transcribeAudio(byte[] audioData, WhisperTranscriptionRequest request) {
        long startTime = System.currentTimeMillis();
        
        if (!whisperEnabled) {
            log.warn("Whisper service is disabled");
            return createErrorResponse("Whisper service is disabled", 
                WhisperTranscriptionResponse.TranscriptionStatus.SERVICE_UNAVAILABLE, startTime);
        }
        
        try {
            // Validar audio
            if (audioData == null || audioData.length == 0) {
                return createErrorResponse("Audio data is empty", 
                    WhisperTranscriptionResponse.TranscriptionStatus.INVALID_AUDIO, startTime);
            }
            
            // Configurar request
            WhisperTranscriptionRequest finalRequest = configureRequest(request);
            
            // Realizar transcripción con reintentos
            return performTranscriptionWithRetry(audioData, finalRequest, startTime);
            
        } catch (Exception e) {
            log.error("Error during transcription: {}", e.getMessage(), e);
            return createErrorResponse("Transcription failed: " + e.getMessage(), 
                WhisperTranscriptionResponse.TranscriptionStatus.ERROR, startTime);
        }
    }
    
    /**
     * Transcribe audio de forma asíncrona
     */
    public CompletableFuture<WhisperTranscriptionResponse> transcribeAudioAsync(
            byte[] audioData, WhisperTranscriptionRequest request) {
        return CompletableFuture.supplyAsync(() -> transcribeAudio(audioData, request));
    }
    
    /**
     * Verifica el estado del servicio Whisper
     */
    public boolean isServiceHealthy() {
        if (!whisperEnabled) {
            return false;
        }
        
        try {
            String healthUrl = whisperServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Whisper service health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene información sobre el servicio Whisper
     */
    public JsonNode getServiceInfo() {
        try {
            String statusUrl = whisperServiceUrl + "/status";
            ResponseEntity<String> response = restTemplate.getForEntity(statusUrl, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get Whisper service info: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Configura el request con valores por defecto
     */
    private WhisperTranscriptionRequest configureRequest(WhisperTranscriptionRequest request) {
        if (request == null) {
            request = new WhisperTranscriptionRequest();
        }
        
        // Establecer valores por defecto
        if (request.getLanguage() == null || request.getLanguage().isEmpty()) {
            request.setLanguage("es"); // Español por defecto
        }
        
        if (request.getTimeoutSeconds() == null) {
            request.setTimeoutSeconds(defaultTimeout);
        }
        
        if (request.getMaxRetries() == null) {
            request.setMaxRetries(maxRetries);
        }
        
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel(defaultModel);
        }
        
        return request;
    }
    
    /**
     * Realiza la transcripción con reintentos
     */
    private WhisperTranscriptionResponse performTranscriptionWithRetry(
            byte[] audioData, WhisperTranscriptionRequest request, long startTime) {
        
        int attempts = 0;
        int maxAttempts = request.getMaxRetries() + 1;
        
        while (attempts < maxAttempts) {
            attempts++;
            
            try {
                log.debug("Transcription attempt {}/{}", attempts, maxAttempts);
                return performTranscription(audioData, request, startTime);
                
            } catch (HttpClientErrorException e) {
                log.warn("HTTP client error on attempt {}: {}", attempts, e.getMessage());
                if (attempts >= maxAttempts) {
                    return createErrorResponse("HTTP client error: " + e.getMessage(), 
                        WhisperTranscriptionResponse.TranscriptionStatus.ERROR, startTime);
                }
                
            } catch (HttpServerErrorException e) {
                log.warn("HTTP server error on attempt {}: {}", attempts, e.getMessage());
                if (attempts >= maxAttempts) {
                    return createErrorResponse("HTTP server error: " + e.getMessage(), 
                        WhisperTranscriptionResponse.TranscriptionStatus.ERROR, startTime);
                }
                
            } catch (ResourceAccessException e) {
                log.warn("Resource access error on attempt {}: {}", attempts, e.getMessage());
                if (attempts >= maxAttempts) {
                    return createErrorResponse("Service unavailable: " + e.getMessage(), 
                        WhisperTranscriptionResponse.TranscriptionStatus.SERVICE_UNAVAILABLE, startTime);
                }
                
            } catch (Exception e) {
                log.error("Unexpected error on attempt {}: {}", attempts, e.getMessage(), e);
                if (attempts >= maxAttempts) {
                    return createErrorResponse("Unexpected error: " + e.getMessage(), 
                        WhisperTranscriptionResponse.TranscriptionStatus.ERROR, startTime);
                }
            }
            
            // Esperar antes del siguiente intento
            if (attempts < maxAttempts) {
                try {
                    Thread.sleep(1000 * attempts); // Backoff exponencial
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return createErrorResponse("Transcription interrupted", 
                        WhisperTranscriptionResponse.TranscriptionStatus.ERROR, startTime);
                }
            }
        }
        
        return createErrorResponse("Max retry attempts exceeded", 
            WhisperTranscriptionResponse.TranscriptionStatus.ERROR, startTime);
    }
    
    /**
     * Realiza la transcripción individual
     */
    private WhisperTranscriptionResponse performTranscription(
            byte[] audioData, WhisperTranscriptionRequest request, long startTime) {
        
        String transcribeUrl = whisperServiceUrl + "/transcribe";
        
        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        // Configurar body multipart
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        // Agregar archivo de audio
        ByteArrayResource audioResource = new ByteArrayResource(audioData) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        };
        body.add("audio", audioResource);
        
        // Agregar parámetros según la API del microservicio
        body.add("language", request.getLanguage());
        
        // Parámetros opcionales según la documentación
        if (request.getModel() != null && !request.getModel().isEmpty()) {
            // El microservicio no acepta parámetro de modelo directamente
            // pero podemos usar el método específico si es necesario
        }
        
        // Crear request entity
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // Configurar timeout - usar SimpleClientHttpRequestFactory
        if (restTemplate.getRequestFactory() instanceof org.springframework.http.client.SimpleClientHttpRequestFactory) {
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                (org.springframework.http.client.SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
            factory.setReadTimeout(request.getTimeoutSeconds() * 1000);
        }
        
        // Realizar request
        ResponseEntity<String> response = restTemplate.exchange(
            transcribeUrl, HttpMethod.POST, requestEntity, String.class);
        
        // Procesar respuesta
        return processTranscriptionResponse(response, startTime);
    }
    
    /**
     * Procesa la respuesta de transcripción
     */
    private WhisperTranscriptionResponse processTranscriptionResponse(
            ResponseEntity<String> response, long startTime) {
        
        try {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            
            WhisperTranscriptionResponse.TranscriptionStatus status = 
                response.getStatusCode() == HttpStatus.OK ? 
                WhisperTranscriptionResponse.TranscriptionStatus.SUCCESS : 
                WhisperTranscriptionResponse.TranscriptionStatus.ERROR;
            
            if (status == WhisperTranscriptionResponse.TranscriptionStatus.SUCCESS) {
                // Extraer campos según la respuesta del microservicio whisper-ms
                String transcription = responseJson.has("transcription") ? 
                    responseJson.get("transcription").asText() : "";
                String language = responseJson.has("language") ? 
                    responseJson.get("language").asText() : "es";
                String detectedLanguage = responseJson.has("detected_language") ? 
                    responseJson.get("detected_language").asText() : language;
                String method = responseJson.has("method") ? 
                    responseJson.get("method").asText() : "unknown";
                
                // Calcular confianza basada en el método usado
                Double confidence = 0.9; // Por defecto
                if ("external".equals(method)) {
                    confidence = 0.95; // API externa es más precisa
                } else if ("local".equals(method)) {
                    confidence = 0.85; // Modelo local
                }
                
                return WhisperTranscriptionResponse.builder()
                    .transcription(transcription)
                    .language(language)
                    .detectedLanguage(detectedLanguage)
                    .confidence(confidence)
                    .timestamp(LocalDateTime.now())
                    .status(status)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .debugInfo(extractDebugInfo(responseJson))
                    .build();
            } else {
                String errorMessage = responseJson.has("error") ? 
                    responseJson.get("error").asText() : "Unknown error";
                return createErrorResponse(errorMessage, status, startTime);
            }
            
        } catch (IOException e) {
            log.error("Error parsing transcription response: {}", e.getMessage());
            return createErrorResponse("Error parsing response: " + e.getMessage(), 
                WhisperTranscriptionResponse.TranscriptionStatus.ERROR, startTime);
        }
    }
    
    /**
     * Extrae información de debug de la respuesta
     */
    private WhisperTranscriptionResponse.DebugInfo extractDebugInfo(JsonNode responseJson) {
        WhisperTranscriptionResponse.DebugInfo.DebugInfoBuilder builder = 
            WhisperTranscriptionResponse.DebugInfo.builder();
        
        boolean hasDebugInfo = false;
        
        if (responseJson.has("debug_audio_file")) {
            builder.debugAudioFile(responseJson.get("debug_audio_file").asText());
            hasDebugInfo = true;
        }
        
        if (responseJson.has("debug_audio_url")) {
            builder.debugAudioUrl(responseJson.get("debug_audio_url").asText());
            hasDebugInfo = true;
        }
        
        // Agregar información adicional del método usado
        if (responseJson.has("method")) {
            String method = responseJson.get("method").asText();
            if ("local".equals(method)) {
                builder.debugAudioFile("local_transcription");
            } else if ("external".equals(method)) {
                builder.debugAudioFile("external_transcription");
            }
            hasDebugInfo = true;
        }
        
        return hasDebugInfo ? builder.build() : null;
    }
    
    /**
     * Crea una respuesta de error
     */
    private WhisperTranscriptionResponse createErrorResponse(
            String errorMessage, 
            WhisperTranscriptionResponse.TranscriptionStatus status, 
            long startTime) {
        
        return WhisperTranscriptionResponse.builder()
            .status(status)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .processingTimeMs(System.currentTimeMillis() - startTime)
            .build();
    }
}
