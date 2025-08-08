package com.intentmanagerms.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.domain.model.TtsGenerationRequest;
import com.intentmanagerms.domain.model.TtsGenerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TtsGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TtsGenerationService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Configuración TTS
    @Value("${tts.enabled:true}")
    private boolean ttsEnabled;
    
    @Value("${tts.default-provider:azure}")
    private String defaultProvider;
    
    @Value("${tts.timeout:30}")
    private int defaultTimeout;
    
    @Value("${tts.max-retries:3}")
    private int defaultMaxRetries;
    
    @Value("${tts.cache-enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${tts.cache-ttl:3600}")
    private int cacheTtlSeconds;
    
    // URLs de servicios TTS
    @Value("${tts.providers.azure.url:http://azure-tts-ms:5000}")
    private String azureTtsUrl;
    
    @Value("${tts.providers.azure.enabled:true}")
    private boolean azureTtsEnabled;
    
    @Value("${tts.providers.kokoro.url:http://kokoro-tts-ms:5002}")
    private String kokoroTtsUrl;
    
    @Value("${tts.providers.kokoro.enabled:true}")
    private boolean kokoroTtsEnabled;
    
    // Cache de audio generado
    private final Map<String, CachedAudio> audioCache = new ConcurrentHashMap<>();
    
    // Estadísticas
    private long totalRequests = 0;
    private long successfulRequests = 0;
    private long failedRequests = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    
    /**
     * Genera audio TTS de forma síncrona
     */
    public TtsGenerationResponse generateAudio(TtsGenerationRequest request) {
        long startTime = System.currentTimeMillis();
        totalRequests++;
        
        logger.info("Iniciando generación TTS: {}", request.getSummary());
        
        try {
            // Validar request
            validateRequest(request);
            
            // Verificar cache
            if (cacheEnabled && request.getProvider() == null) {
                String cacheKey = generateCacheKey(request);
                CachedAudio cachedAudio = audioCache.get(cacheKey);
                if (cachedAudio != null && !cachedAudio.isExpired()) {
                    cacheHits++;
                    logger.debug("Cache hit para texto: '{}'", request.getText());
                    return createResponseFromCache(request, cachedAudio, System.currentTimeMillis() - startTime);
                }
            }
            
            cacheMisses++;
            
            // Determinar proveedor
            String provider = determineProvider(request);
            
            // Generar audio
            TtsGenerationResponse response = generateAudioWithProvider(request, provider);
            
            // Cachear resultado exitoso
            if (cacheEnabled && response.isSuccess() && request.getProvider() == null) {
                String cacheKey = generateCacheKey(request);
                audioCache.put(cacheKey, new CachedAudio(response, cacheTtlSeconds));
            }
            
            // Actualizar estadísticas
            if (response.isSuccess()) {
                successfulRequests++;
            } else {
                failedRequests++;
            }
            
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            logger.info("Generación TTS completada: {}", response.getSummary());
            
            return response;
            
        } catch (Exception e) {
            failedRequests++;
            logger.error("Error en generación TTS: {}", e.getMessage(), e);
            return TtsGenerationResponse.failed(request.getRequestId(), request.getText(), e.getMessage());
        }
    }
    
    /**
     * Genera audio TTS de forma asíncrona
     */
    public CompletableFuture<TtsGenerationResponse> generateAudioAsync(TtsGenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> generateAudio(request));
    }
    
    /**
     * Genera audio con fallback entre proveedores
     */
    private TtsGenerationResponse generateAudioWithProvider(TtsGenerationRequest request, String primaryProvider) {
        List<String> providers = getProviderPriorityList(primaryProvider);
        
        for (String provider : providers) {
            try {
                logger.debug("Intentando generación con proveedor: {}", provider);
                TtsGenerationResponse response = callTtsProvider(request, provider);
                
                if (response.isSuccess()) {
                    logger.info("Generación exitosa con proveedor: {}", provider);
                    return response;
                } else {
                    logger.warn("Proveedor {} falló: {}", provider, response.getErrorMessage());
                }
                
            } catch (Exception e) {
                logger.warn("Error con proveedor {}: {}", provider, e.getMessage());
            }
        }
        
        // Todos los proveedores fallaron
        logger.error("Todos los proveedores TTS fallaron para: {}", request.getText());
        return TtsGenerationResponse.failed(request.getRequestId(), request.getText(), 
                "Todos los proveedores TTS no están disponibles");
    }
    
    /**
     * Llama a un proveedor TTS específico
     */
    private TtsGenerationResponse callTtsProvider(TtsGenerationRequest request, String provider) {
        try {
            String url = getProviderUrl(provider);
            String endpoint = "/synthesize_json";
            
            // Preparar request para el proveedor
            Map<String, Object> providerRequest = new HashMap<>();
            providerRequest.put("text", request.getText());
            providerRequest.put("language", request.getLanguage());
            providerRequest.put("voice", request.getVoice());
            providerRequest.put("speed", request.getSpeed());
            providerRequest.put("gender_preference", request.getGenderPreference());
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(providerRequest, headers);
            
            // Timeout
            int timeout = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : defaultTimeout;
            
            logger.debug("Llamando a {} con timeout {}s", url + endpoint, timeout);
            
            // Realizar llamada HTTP
            ResponseEntity<Map> response = restTemplate.exchange(
                url + endpoint,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseProviderResponse(request, provider, response.getBody());
            } else {
                return TtsGenerationResponse.failed(request.getRequestId(), request.getText(), 
                        "Respuesta inválida del proveedor: " + provider);
            }
            
        } catch (ResourceAccessException e) {
            logger.warn("Timeout o error de conexión con {}: {}", provider, e.getMessage());
            return TtsGenerationResponse.timeout(request.getRequestId(), request.getText(), provider);
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.warn("Error HTTP con {}: {} - {}", provider, e.getStatusCode(), e.getMessage());
            return TtsGenerationResponse.providerUnavailable(request.getRequestId(), request.getText(), provider);
            
        } catch (Exception e) {
            logger.error("Error inesperado con {}: {}", provider, e.getMessage(), e);
            return TtsGenerationResponse.failed(request.getRequestId(), request.getText(), 
                    "Error con proveedor " + provider + ": " + e.getMessage());
        }
    }
    
    /**
     * Parsea la respuesta del proveedor TTS
     */
    private TtsGenerationResponse parseProviderResponse(TtsGenerationRequest request, String provider, Map<String, Object> response) {
        try {
            boolean success = (Boolean) response.getOrDefault("success", false);
            
            if (!success) {
                String error = (String) response.getOrDefault("error", "Error desconocido del proveedor");
                return TtsGenerationResponse.failed(request.getRequestId(), request.getText(), error);
            }
            
            // Extraer datos de la respuesta
            String text = (String) response.get("text");
            String language = (String) response.get("language");
            String voice = (String) response.get("voice");
            Double audioDuration = response.get("audio_duration") != null ? 
                    Double.valueOf(response.get("audio_duration").toString()) : null;
            Integer sampleRate = response.get("sample_rate") != null ? 
                    Integer.valueOf(response.get("sample_rate").toString()) : null;
            Double speed = response.get("speed") != null ? 
                    Double.valueOf(response.get("speed").toString()) : null;
            
            // Por ahora, no tenemos audio_data en la respuesta JSON, solo metadata
            // En una implementación real, necesitaríamos obtener el archivo de audio
            byte[] audioData = new byte[0]; // Placeholder
            
            return TtsGenerationResponse.success(
                request.getRequestId(), text, language, voice, provider, 
                audioData, audioDuration, sampleRate, speed
            );
            
        } catch (Exception e) {
            logger.error("Error parseando respuesta de {}: {}", provider, e.getMessage());
            return TtsGenerationResponse.failed(request.getRequestId(), request.getText(), 
                    "Error parseando respuesta del proveedor");
        }
    }
    
    /**
     * Valida el request de TTS
     */
    private void validateRequest(TtsGenerationRequest request) {
        if (!ttsEnabled) {
            throw new IllegalStateException("TTS está deshabilitado");
        }
        
        if (!request.hasValidText()) {
            throw new IllegalArgumentException("Texto no puede estar vacío");
        }
        
        if (!request.hasValidSpeed()) {
            throw new IllegalArgumentException("Velocidad debe estar entre 0.5 y 2.0");
        }
        
        if (!request.hasValidTimeout()) {
            throw new IllegalArgumentException("Timeout debe estar entre 1 y 120 segundos");
        }
        
        if (!request.hasValidRetries()) {
            throw new IllegalArgumentException("Máximo de reintentos debe estar entre 0 y 5");
        }
    }
    
    /**
     * Determina el proveedor a usar
     */
    private String determineProvider(TtsGenerationRequest request) {
        if (request.getProvider() != null) {
            return request.getProvider();
        }
        return defaultProvider;
    }
    
    /**
     * Obtiene la lista de proveedores en orden de prioridad
     */
    private List<String> getProviderPriorityList(String primaryProvider) {
        List<String> providers = new ArrayList<>();
        
        // Añadir proveedor primario
        if (isProviderEnabled(primaryProvider)) {
            providers.add(primaryProvider);
        }
        
        // Añadir proveedores de fallback
        if (!"azure".equals(primaryProvider) && azureTtsEnabled) {
            providers.add("azure");
        }
        if (!"kokoro".equals(primaryProvider) && kokoroTtsEnabled) {
            providers.add("kokoro");
        }
        
        return providers;
    }
    
    /**
     * Verifica si un proveedor está habilitado
     */
    private boolean isProviderEnabled(String provider) {
        switch (provider) {
            case "azure":
                return azureTtsEnabled;
            case "kokoro":
                return kokoroTtsEnabled;
            default:
                return false;
        }
    }
    
    /**
     * Obtiene la URL del proveedor
     */
    private String getProviderUrl(String provider) {
        switch (provider) {
            case "azure":
                return azureTtsUrl;
            case "kokoro":
                return kokoroTtsUrl;
            default:
                throw new IllegalArgumentException("Proveedor no soportado: " + provider);
        }
    }
    
    /**
     * Genera clave de cache
     */
    private String generateCacheKey(TtsGenerationRequest request) {
        return String.format("%s_%s_%s_%.1f", 
                request.getText().hashCode(),
                request.getLanguage(),
                request.getVoice() != null ? request.getVoice() : "auto",
                request.getSpeed());
    }
    
    /**
     * Crea respuesta desde cache
     */
    private TtsGenerationResponse createResponseFromCache(TtsGenerationRequest request, CachedAudio cachedAudio, long processingTime) {
        TtsGenerationResponse response = new TtsGenerationResponse(request.getRequestId());
        response.setSuccess(true);
        response.setStatus(TtsGenerationResponse.GenerationStatus.SUCCESS);
        response.setText(cachedAudio.getResponse().getText());
        response.setLanguage(cachedAudio.getResponse().getLanguage());
        response.setVoice(cachedAudio.getResponse().getVoice());
        response.setProvider(cachedAudio.getResponse().getProvider() + "_cached");
        response.setAudioData(cachedAudio.getResponse().getAudioData());
        response.setAudioDuration(cachedAudio.getResponse().getAudioDuration());
        response.setSampleRate(cachedAudio.getResponse().getSampleRate());
        response.setSpeed(cachedAudio.getResponse().getSpeed());
        response.setProcessingTimeMs(processingTime);
        response.setGeneratedAt(LocalDateTime.now());
        
        return response;
    }
    
    /**
     * Obtiene estadísticas del servicio
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_requests", totalRequests);
        stats.put("successful_requests", successfulRequests);
        stats.put("failed_requests", failedRequests);
        stats.put("success_rate", totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0);
        stats.put("cache_hits", cacheHits);
        stats.put("cache_misses", cacheMisses);
        stats.put("cache_hit_rate", (cacheHits + cacheMisses) > 0 ? (double) cacheHits / (cacheHits + cacheMisses) : 0.0);
        stats.put("cache_size", audioCache.size());
        stats.put("tts_enabled", ttsEnabled);
        stats.put("default_provider", defaultProvider);
        stats.put("azure_enabled", azureTtsEnabled);
        stats.put("kokoro_enabled", kokoroTtsEnabled);
        stats.put("azure_url", azureTtsUrl);
        stats.put("kokoro_url", kokoroTtsUrl);
        
        return stats;
    }
    
    /**
     * Limpia el cache
     */
    public void clearCache() {
        audioCache.clear();
        logger.info("Cache TTS limpiado");
    }
    
    /**
     * Verifica la salud del servicio
     */
    public boolean isHealthy() {
        return ttsEnabled && (azureTtsEnabled || kokoroTtsEnabled);
    }
    
    /**
     * Clase interna para cache de audio
     */
    private static class CachedAudio {
        private final TtsGenerationResponse response;
        private final LocalDateTime cachedAt;
        private final int ttlSeconds;
        
        public CachedAudio(TtsGenerationResponse response, int ttlSeconds) {
            this.response = response;
            this.cachedAt = LocalDateTime.now();
            this.ttlSeconds = ttlSeconds;
        }
        
        public TtsGenerationResponse getResponse() {
            return response;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(cachedAt.plusSeconds(ttlSeconds));
        }
    }
}
