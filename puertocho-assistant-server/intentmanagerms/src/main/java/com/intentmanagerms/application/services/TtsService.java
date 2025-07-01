package com.intentmanagerms.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.time.Duration;
import java.time.Instant;

/**
 * Servicio para síntesis de texto a voz con lógica de fallback
 * F5-TTS (principal) → Azure TTS (respaldo) → Kokoro TTS (alternativo)
 */
@Service
public class TtsService {

    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // URLs de servicios TTS configurables
    @Value("${tts.f5.url:http://puertocho-assistant-f5-tts:5005}")
    private String f5TtsUrl;
    
    @Value("${tts.azure.url:http://puertocho-assistant-azure-tts:5000}")
    private String azureTtsUrl;
    
    @Value("${tts.kokoro.url:http://puertocho-assistant-kokoro-tts:5002}")
    private String kokoroTtsUrl;
    
    // Configuración de comportamiento
    @Value("${tts.primary.service:f5_tts}")
    private String primaryTtsService;
    
    @Value("${tts.fallback.service:azure_tts}")
    private String fallbackTtsService;
    
    @Value("${tts.timeout.seconds:30}")
    private int timeoutSeconds;
    
    @Value("${tts.retry.enabled:true}")
    private boolean retryEnabled;
    
    @Value("${tts.default.voice:es_female}")
    private String defaultVoice;
    
    @Value("${tts.default.language:es}")
    private String defaultLanguage;

    public TtsService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sintetiza texto a voz usando la lógica de fallback
     * @param text Texto a sintetizar
     * @param language Idioma (opcional, default: es)
     * @param voice Voz (opcional, default: es_female)
     * @return Respuesta con información del audio generado
     */
    public TtsResponse synthesizeText(String text, String language, String voice) {
        Instant startTime = Instant.now();
        
        // Usar valores por defecto si no se especifican
        language = (language != null && !language.isEmpty()) ? language : defaultLanguage;
        voice = (voice != null && !voice.isEmpty()) ? voice : defaultVoice;
        
        logger.info("🎤 Iniciando síntesis TTS: texto='{}', idioma='{}', voz='{}'", 
                   text.substring(0, Math.min(text.length(), 50)), language, voice);
        
        // 1. Intentar con servicio principal (F5-TTS por defecto)
        if ("f5_tts".equals(primaryTtsService)) {
            TtsResponse response = tryF5Tts(text, language, voice);
            if (response.isSuccess()) {
                Duration duration = Duration.between(startTime, Instant.now());
                logger.info("✅ Síntesis exitosa con F5-TTS en {}ms", duration.toMillis());
                response.setServiceUsed("f5_tts");
                response.setProcessingTimeMs(duration.toMillis());
                return response;
            }
        }
        
        // 2. Fallback a servicio de respaldo (Azure TTS por defecto)
        if ("azure_tts".equals(fallbackTtsService)) {
            logger.warn("⚠️ F5-TTS no disponible, intentando con Azure TTS...");
            TtsResponse response = tryAzureTts(text, language, voice);
            if (response.isSuccess()) {
                Duration duration = Duration.between(startTime, Instant.now());
                logger.info("✅ Síntesis exitosa con Azure TTS (fallback) en {}ms", duration.toMillis());
                response.setServiceUsed("azure_tts");
                response.setProcessingTimeMs(duration.toMillis());
                return response;
            }
        }
        
        // 3. Último recurso: Kokoro TTS
        logger.warn("⚠️ Servicios principales no disponibles, intentando con Kokoro TTS...");
        TtsResponse response = tryKokoroTts(text, language, voice);
        if (response.isSuccess()) {
            Duration duration = Duration.between(startTime, Instant.now());
            logger.info("✅ Síntesis exitosa con Kokoro TTS (último recurso) en {}ms", duration.toMillis());
            response.setServiceUsed("kokoro_tts");
            response.setProcessingTimeMs(duration.toMillis());
            return response;
        }
        
        // 4. Todos los servicios fallaron
        Duration duration = Duration.between(startTime, Instant.now());
        logger.error("❌ FALLO CRÍTICO: Todos los servicios TTS están inaccesibles");
        
        TtsResponse errorResponse = new TtsResponse();
        errorResponse.setSuccess(false);
        errorResponse.setError("Todos los servicios TTS están inaccesibles");
        errorResponse.setServiceUsed("none");
        errorResponse.setProcessingTimeMs(duration.toMillis());
        
        return errorResponse;
    }

    /**
     * Intenta síntesis con F5-TTS
     */
    private TtsResponse tryF5Tts(String text, String language, String voice) {
        try {
            String url = f5TtsUrl + "/synthesize_json";
            
            // Construir payload para F5-TTS
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", text);
            payload.put("language", language);
            payload.put("voice", voice);
            payload.put("speed", 0.9); // Velocidad óptima para F5-TTS español
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            logger.debug("🔍 Llamando a F5-TTS: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                TtsResponse ttsResponse = new TtsResponse();
                ttsResponse.setSuccess(jsonResponse.path("success").asBoolean(false));
                ttsResponse.setModel(jsonResponse.path("model").asText("spanish-f5"));
                ttsResponse.setAudioDuration(jsonResponse.path("audio_duration").asDouble(0.0));
                ttsResponse.setSampleRate(jsonResponse.path("sample_rate").asInt(24000));
                ttsResponse.setDebugAudioUrl(jsonResponse.path("debug_audio_url").asText(""));
                
                return ttsResponse;
            }
            
        } catch (HttpClientErrorException e) {
            logger.warn("⚠️ Error HTTP en F5-TTS: {} - {}", e.getStatusCode(), e.getMessage());
        } catch (ResourceAccessException e) {
            logger.warn("⚠️ F5-TTS no accesible: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("⚠️ Error inesperado en F5-TTS: {}", e.getMessage());
        }
        
        return createErrorResponse("F5-TTS no disponible");
    }

    /**
     * Intenta síntesis con Azure TTS
     */
    private TtsResponse tryAzureTts(String text, String language, String voice) {
        try {
            String url = azureTtsUrl + "/synthesize_json";
            
            // Adaptar configuración para Azure TTS
            String azureLanguage = mapToAzureLanguage(language);
            String azureVoice = mapToAzureVoice(voice, azureLanguage);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", text);
            payload.put("language", azureLanguage);
            payload.put("voice", azureVoice);
            payload.put("speed", 1.0);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            logger.debug("🔍 Llamando a Azure TTS: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                TtsResponse ttsResponse = new TtsResponse();
                ttsResponse.setSuccess(true); // Azure TTS format might differ
                ttsResponse.setModel("azure-tts");
                ttsResponse.setAudioDuration(0.0); // Azure might not provide this
                ttsResponse.setSampleRate(16000); // Azure default
                
                return ttsResponse;
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ Error en Azure TTS: {}", e.getMessage());
        }
        
        return createErrorResponse("Azure TTS no disponible");
    }

    /**
     * Intenta síntesis con Kokoro TTS
     */
    private TtsResponse tryKokoroTts(String text, String language, String voice) {
        try {
            String url = kokoroTtsUrl + "/synthesize_json";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", text);
            payload.put("language", language);
            payload.put("voice", "ef_dora"); // Kokoro voice
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            logger.debug("🔍 Llamando a Kokoro TTS: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                TtsResponse ttsResponse = new TtsResponse();
                ttsResponse.setSuccess(true);
                ttsResponse.setModel("kokoro-tts");
                ttsResponse.setAudioDuration(0.0);
                ttsResponse.setSampleRate(22050); // Kokoro default
                
                return ttsResponse;
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ Error en Kokoro TTS: {}", e.getMessage());
        }
        
        return createErrorResponse("Kokoro TTS no disponible");
    }

    /**
     * Verifica disponibilidad de servicios TTS
     */
    public Map<String, Boolean> checkTtsAvailability() {
        Map<String, Boolean> availability = new HashMap<>();
        
        // Verificar F5-TTS
        availability.put("f5_tts", checkServiceHealth(f5TtsUrl + "/health"));
        
        // Verificar Azure TTS
        availability.put("azure_tts", checkServiceHealth(azureTtsUrl + "/health"));
        
        // Verificar Kokoro TTS
        availability.put("kokoro_tts", checkServiceHealth(kokoroTtsUrl + "/health"));
        
        logger.info("📊 Estado servicios TTS: {}", availability);
        return availability;
    }

    private boolean checkServiceHealth(String healthUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }

    private TtsResponse createErrorResponse(String error) {
        TtsResponse response = new TtsResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }

    // Mapeo de idiomas para Azure TTS
    private String mapToAzureLanguage(String language) {
        switch (language.toLowerCase()) {
            case "es": return "es-ES";
            case "en": return "en-US";
            case "fr": return "fr-FR";
            default: return "es-ES";
        }
    }

    // Mapeo de voces para Azure TTS
    private String mapToAzureVoice(String voice, String azureLanguage) {
        if (azureLanguage.equals("es-ES")) {
            switch (voice.toLowerCase()) {
                case "es_female":
                case "es_maria": return "Abril";
                case "es_male":
                case "es_carlos": return "Alvaro";
                default: return "Abril";
            }
        }
        return "Abril"; // Default Spanish voice
    }

    /**
     * Clase para encapsular respuesta de TTS
     */
    public static class TtsResponse {
        private boolean success;
        private String error;
        private String serviceUsed;
        private String model;
        private double audioDuration;
        private int sampleRate;
        private String debugAudioUrl;
        private long processingTimeMs;

        // Getters y setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getServiceUsed() { return serviceUsed; }
        public void setServiceUsed(String serviceUsed) { this.serviceUsed = serviceUsed; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public double getAudioDuration() { return audioDuration; }
        public void setAudioDuration(double audioDuration) { this.audioDuration = audioDuration; }
        
        public int getSampleRate() { return sampleRate; }
        public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
        
        public String getDebugAudioUrl() { return debugAudioUrl; }
        public void setDebugAudioUrl(String debugAudioUrl) { this.debugAudioUrl = debugAudioUrl; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }
} 