package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.TtsGenerationService;
import com.intentmanagerms.domain.model.TtsGenerationRequest;
import com.intentmanagerms.domain.model.TtsGenerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/tts")
@CrossOrigin(origins = "*")
public class TtsGenerationController {
    
    private static final Logger logger = LoggerFactory.getLogger(TtsGenerationController.class);
    
    @Autowired
    private TtsGenerationService ttsGenerationService;
    
    /**
     * Genera audio TTS de forma síncrona
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateAudio(@RequestBody TtsGenerationRequest request) {
        try {
            // Generar ID de request si no existe
            if (request.getRequestId() == null) {
                request.setRequestId(UUID.randomUUID().toString());
            }
            
            logger.info("Recibida petición TTS: {}", request.getSummary());
            
            // Generar audio
            TtsGenerationResponse response = ttsGenerationService.generateAudio(request);
            
            // Preparar respuesta
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("request_id", response.getRequestId());
            result.put("text", response.getText());
            result.put("language", response.getLanguage());
            result.put("voice", response.getVoice());
            result.put("provider", response.getProvider());
            result.put("audio_duration", response.getAudioDuration());
            result.put("sample_rate", response.getSampleRate());
            result.put("speed", response.getSpeed());
            result.put("processing_time_ms", response.getProcessingTimeMs());
            result.put("generated_at", response.getGeneratedAt());
            
            if (response.isSuccess()) {
                result.put("audio_data_size", response.getAudioDataSize());
                result.put("status", "success");
                return ResponseEntity.ok(result);
            } else {
                result.put("error_message", response.getErrorMessage());
                result.put("status", response.getStatus().toString().toLowerCase());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
        } catch (Exception e) {
            logger.error("Error en generación TTS: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error_message", e.getMessage());
            error.put("status", "error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Genera audio TTS de forma asíncrona
     */
    @PostMapping("/generate/async")
    public ResponseEntity<Map<String, Object>> generateAudioAsync(@RequestBody TtsGenerationRequest request) {
        try {
            // Generar ID de request si no existe
            if (request.getRequestId() == null) {
                request.setRequestId(UUID.randomUUID().toString());
            }
            
            logger.info("Recibida petición TTS asíncrona: {}", request.getSummary());
            
            // Iniciar generación asíncrona
            CompletableFuture<TtsGenerationResponse> future = ttsGenerationService.generateAudioAsync(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("request_id", request.getRequestId());
            result.put("status", "processing");
            result.put("message", "Generación TTS iniciada de forma asíncrona");
            
            return ResponseEntity.accepted().body(result);
            
        } catch (Exception e) {
            logger.error("Error iniciando generación TTS asíncrona: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error_message", e.getMessage());
            error.put("status", "error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Health check del servicio TTS
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", ttsGenerationService.isHealthy() ? "healthy" : "unhealthy");
            health.put("service", "tts-generation");
            health.put("timestamp", System.currentTimeMillis());
            
            // Añadir estadísticas básicas
            Map<String, Object> stats = ttsGenerationService.getStatistics();
            health.put("statistics", stats);
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error en health check TTS: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("error_message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Obtiene información sobre los proveedores disponibles
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        try {
            Map<String, Object> providers = new HashMap<>();
            providers.put("available_providers", new String[]{"azure", "kokoro"});
            providers.put("default_provider", "azure");
            providers.put("fallback_enabled", true);
            
            // Información específica de cada proveedor
            Map<String, Object> providerInfo = new HashMap<>();
            
            Map<String, Object> azureInfo = new HashMap<>();
            azureInfo.put("name", "Azure Cognitive Services TTS");
            azureInfo.put("url", "http://azure-tts-service:5000");
            azureInfo.put("enabled", true);
            azureInfo.put("languages", new String[]{"es-ES", "es-MX", "es-AR", "es-CO", "es-CL", "es-PE", "es-VE"});
            azureInfo.put("voices", new String[]{"Abril", "Elvira", "Esperanza", "Estrella", "Irene", "Laia", "Lia", "Lola", "Mar", "Nia", "Sol", "Tania", "Vega", "Vera"});
            providerInfo.put("azure", azureInfo);
            
            Map<String, Object> kokoroInfo = new HashMap<>();
            kokoroInfo.put("name", "Kokoro TTS v1.0");
            kokoroInfo.put("url", "http://kokoro-tts:5002");
            kokoroInfo.put("enabled", true);
            kokoroInfo.put("languages", new String[]{"es", "en", "fr", "it", "pt", "hi", "ja", "zh"});
            kokoroInfo.put("voices", new String[]{"ef_dora", "em_alex", "em_santa"});
            providerInfo.put("kokoro", kokoroInfo);
            
            providers.put("provider_details", providerInfo);
            
            return ResponseEntity.ok(providers);
            
        } catch (Exception e) {
            logger.error("Error obteniendo proveedores TTS: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error_message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Obtiene las voces disponibles por idioma
     */
    @GetMapping("/voices")
    public ResponseEntity<Map<String, Object>> getVoices(@RequestParam(required = false) String language) {
        try {
            Map<String, Object> voices = new HashMap<>();
            
            if (language == null || language.equals("all")) {
                // Todas las voces por idioma
                Map<String, Object> voicesByLanguage = new HashMap<>();
                
                // Voces Azure
                Map<String, Object> azureVoices = new HashMap<>();
                azureVoices.put("es-ES", new String[]{"Abril", "Elvira", "Esperanza", "Estrella", "Irene", "Laia", "Lia", "Lola", "Mar", "Nia", "Sol", "Tania", "Vega", "Vera"});
                azureVoices.put("es-MX", new String[]{"Dalia", "Renata"});
                azureVoices.put("es-AR", new String[]{"Elena"});
                azureVoices.put("es-CO", new String[]{"Gonzalo"});
                azureVoices.put("es-CL", new String[]{"Catalina"});
                azureVoices.put("es-PE", new String[]{"Camila"});
                azureVoices.put("es-VE", new String[]{"Paola"});
                voicesByLanguage.put("azure", azureVoices);
                
                // Voces Kokoro
                Map<String, Object> kokoroVoices = new HashMap<>();
                kokoroVoices.put("es", new String[]{"ef_dora", "em_alex", "em_santa"});
                kokoroVoices.put("en", new String[]{"af_bella", "af_heart", "af_nicole", "am_michael", "am_adam"});
                kokoroVoices.put("fr", new String[]{"ff_siwis"});
                kokoroVoices.put("it", new String[]{"if_sara", "im_nicola"});
                voicesByLanguage.put("kokoro", kokoroVoices);
                
                voices.put("voices_by_provider", voicesByLanguage);
                voices.put("total_languages", 8);
                voices.put("total_voices", 25);
                
            } else {
                // Voces para un idioma específico
                Map<String, Object> languageVoices = new HashMap<>();
                
                if (language.startsWith("es-")) {
                    // Azure voices para español
                    switch (language) {
                        case "es-ES":
                            languageVoices.put("azure", new String[]{"Abril", "Elvira", "Esperanza", "Estrella", "Irene", "Laia", "Lia", "Lola", "Mar", "Nia", "Sol", "Tania", "Vega", "Vera"});
                            break;
                        case "es-MX":
                            languageVoices.put("azure", new String[]{"Dalia", "Renata"});
                            break;
                        case "es-AR":
                            languageVoices.put("azure", new String[]{"Elena"});
                            break;
                        case "es-CO":
                            languageVoices.put("azure", new String[]{"Gonzalo"});
                            break;
                        case "es-CL":
                            languageVoices.put("azure", new String[]{"Catalina"});
                            break;
                        case "es-PE":
                            languageVoices.put("azure", new String[]{"Camila"});
                            break;
                        case "es-VE":
                            languageVoices.put("azure", new String[]{"Paola"});
                            break;
                    }
                } else {
                    // Kokoro voices
                    switch (language) {
                        case "es":
                            languageVoices.put("kokoro", new String[]{"ef_dora", "em_alex", "em_santa"});
                            break;
                        case "en":
                            languageVoices.put("kokoro", new String[]{"af_bella", "af_heart", "af_nicole", "am_michael", "am_adam"});
                            break;
                        case "fr":
                            languageVoices.put("kokoro", new String[]{"ff_siwis"});
                            break;
                        case "it":
                            languageVoices.put("kokoro", new String[]{"if_sara", "im_nicola"});
                            break;
                    }
                }
                
                voices.put("language", language);
                voices.put("voices", languageVoices);
                voices.put("total_voices", languageVoices.values().stream().mapToInt(v -> ((String[]) v).length).sum());
            }
            
            return ResponseEntity.ok(voices);
            
        } catch (Exception e) {
            logger.error("Error obteniendo voces TTS: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error_message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Test del servicio TTS con texto de ejemplo
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testTts(@RequestBody(required = false) Map<String, Object> request) {
        try {
            // Texto de prueba por defecto
            String testText = "¡Hola! Soy tu asistente de voz. ¿En qué puedo ayudarte?";
            String language = "es";
            String voice = null;
            String provider = null;
            
            // Usar parámetros del request si se proporcionan
            if (request != null) {
                testText = (String) request.getOrDefault("text", testText);
                language = (String) request.getOrDefault("language", language);
                voice = (String) request.get("voice");
                provider = (String) request.get("provider");
            }
            
            logger.info("Ejecutando test TTS: '{}'", testText);
            
            // Crear request de prueba
            TtsGenerationRequest testRequest = new TtsGenerationRequest(testText, language, voice);
            testRequest.setRequestId("test-" + UUID.randomUUID().toString());
            testRequest.setProvider(provider);
            
            // Generar audio
            TtsGenerationResponse response = ttsGenerationService.generateAudio(testRequest);
            
            // Preparar resultado
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("test_text", testText);
            result.put("language", language);
            result.put("voice", voice);
            result.put("provider", provider);
            result.put("processing_time_ms", response.getProcessingTimeMs());
            
            if (response.isSuccess()) {
                result.put("audio_duration", response.getAudioDuration());
                result.put("sample_rate", response.getSampleRate());
                result.put("provider_used", response.getProvider());
                result.put("message", "Test TTS completado exitosamente");
            } else {
                result.put("error_message", response.getErrorMessage());
                result.put("message", "Test TTS falló");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error en test TTS: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error_message", e.getMessage());
            error.put("message", "Test TTS falló");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Obtiene estadísticas del servicio TTS
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = ttsGenerationService.getStatistics();
            stats.put("endpoint", "/api/v1/tts/stats");
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas TTS: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error_message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Limpia el cache del servicio TTS
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            ttsGenerationService.clearCache();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Cache TTS limpiado exitosamente");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error limpiando cache TTS: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error_message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
