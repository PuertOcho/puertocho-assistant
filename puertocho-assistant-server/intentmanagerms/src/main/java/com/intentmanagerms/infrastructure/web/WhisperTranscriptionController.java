package com.intentmanagerms.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.intentmanagerms.application.services.WhisperTranscriptionService;
import com.intentmanagerms.domain.model.WhisperTranscriptionRequest;
import com.intentmanagerms.domain.model.WhisperTranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para transcripción de audio usando Whisper
 */
@RestController
@RequestMapping("/api/v1/whisper")
@CrossOrigin(origins = "*")
public class WhisperTranscriptionController {
    
    private final WhisperTranscriptionService whisperService;
    
    @Autowired
    public WhisperTranscriptionController(WhisperTranscriptionService whisperService) {
        this.whisperService = whisperService;
    }
    
    /**
     * Transcribe audio usando Whisper
     * POST /api/v1/whisper/transcribe
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WhisperTranscriptionResponse> transcribeAudio(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "language", required = false, defaultValue = "es") String language,
            @RequestParam(value = "timeout_seconds", required = false) Integer timeoutSeconds,
            @RequestParam(value = "max_retries", required = false) Integer maxRetries,
            @RequestParam(value = "model", required = false) String model) {
        
        try {
            // Validar archivo de audio
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Audio file is empty", 
                        WhisperTranscriptionResponse.TranscriptionStatus.INVALID_AUDIO));
            }
            
            // Crear request
            WhisperTranscriptionRequest request = WhisperTranscriptionRequest.builder()
                .language(language)
                .timeoutSeconds(timeoutSeconds)
                .maxRetries(maxRetries)
                .model(model)
                .build();
            
            // Realizar transcripción
            WhisperTranscriptionResponse response = whisperService.transcribeAudio(
                audioFile.getBytes(), request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Transcription failed: " + e.getMessage(), 
                    WhisperTranscriptionResponse.TranscriptionStatus.ERROR));
        }
    }
    
    /**
     * Transcribe audio de forma asíncrona
     * POST /api/v1/whisper/transcribe/async
     */
    @PostMapping(value = "/transcribe/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> transcribeAudioAsync(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "language", required = false, defaultValue = "es") String language,
            @RequestParam(value = "timeout_seconds", required = false) Integer timeoutSeconds,
            @RequestParam(value = "max_retries", required = false) Integer maxRetries,
            @RequestParam(value = "model", required = false) String model) {
        
        try {
            // Validar archivo de audio
            if (audioFile.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Audio file is empty");
                errorResponse.put("status", "ERROR");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Crear request
            WhisperTranscriptionRequest request = WhisperTranscriptionRequest.builder()
                .language(language)
                .timeoutSeconds(timeoutSeconds)
                .maxRetries(maxRetries)
                .model(model)
                .build();
            
            // Iniciar transcripción asíncrona
            CompletableFuture<WhisperTranscriptionResponse> future = 
                whisperService.transcribeAudioAsync(audioFile.getBytes(), request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "PROCESSING");
            response.put("message", "Transcription started asynchronously");
            response.put("task_id", future.hashCode()); // ID simple para tracking
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to start transcription: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Verifica el estado del servicio Whisper
     * GET /api/v1/whisper/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        
        boolean isHealthy = whisperService.isServiceHealthy();
        response.put("status", isHealthy ? "HEALTHY" : "UNHEALTHY");
        response.put("service", "whisper-transcription");
        response.put("timestamp", System.currentTimeMillis());
        
        if (isHealthy) {
            JsonNode serviceInfo = whisperService.getServiceInfo();
            if (serviceInfo != null) {
                response.put("service_info", serviceInfo);
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene información del servicio Whisper
     * GET /api/v1/whisper/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> response = new HashMap<>();
        
        JsonNode serviceInfo = whisperService.getServiceInfo();
        if (serviceInfo != null) {
            response.put("service_info", serviceInfo);
            response.put("status", "AVAILABLE");
        } else {
            response.put("status", "UNAVAILABLE");
            response.put("error", "Could not retrieve service information");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test de transcripción con archivo de ejemplo
     * POST /api/v1/whisper/test
     */
    @PostMapping("/test")
    public ResponseEntity<WhisperTranscriptionResponse> testTranscription(
            @RequestParam(value = "language", required = false, defaultValue = "es") String language) {
        
        try {
            // Crear datos de audio de prueba (silencioso)
            byte[] testAudioData = createTestAudioData();
            
            // Crear request
            WhisperTranscriptionRequest request = WhisperTranscriptionRequest.builder()
                .language(language)
                .timeoutSeconds(10)
                .maxRetries(1)
                .build();
            
            // Realizar transcripción de prueba
            WhisperTranscriptionResponse response = whisperService.transcribeAudio(testAudioData, request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Test transcription failed: " + e.getMessage(), 
                    WhisperTranscriptionResponse.TranscriptionStatus.ERROR));
        }
    }
    
    /**
     * Obtiene estadísticas del servicio
     * GET /api/v1/whisper/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("service", "whisper-transcription");
        response.put("status", whisperService.isServiceHealthy() ? "ACTIVE" : "INACTIVE");
        response.put("timestamp", System.currentTimeMillis());
        response.put("supported_languages", "es,en");
        response.put("default_model", "base");
        response.put("max_file_size", "10MB");
        response.put("supported_formats", "wav,mp3,m4a,flac");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint de debug para ver la configuración del servicio
     * GET /api/v1/whisper/debug
     */
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> getDebugInfo() {
        Map<String, Object> response = new HashMap<>();
        
        // Obtener información de debug del servicio
        JsonNode serviceInfo = whisperService.getServiceInfo();
        
        response.put("service", "whisper-transcription");
        response.put("service_info", serviceInfo);
        response.put("health_check", whisperService.isServiceHealthy());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Crea una respuesta de error
     */
    private WhisperTranscriptionResponse createErrorResponse(String errorMessage, 
            WhisperTranscriptionResponse.TranscriptionStatus status) {
        
        return WhisperTranscriptionResponse.builder()
            .status(status)
            .errorMessage(errorMessage)
            .timestamp(java.time.LocalDateTime.now())
            .processingTimeMs(0L)
            .build();
    }
    
    /**
     * Crea datos de audio de prueba (WAV silencioso)
     */
    private byte[] createTestAudioData() {
        // WAV header para 1 segundo de silencio a 16kHz, 16-bit, mono
        byte[] wavHeader = {
            (byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46, // "RIFF"
            (byte) 0x24, (byte) 0x08, (byte) 0x00, (byte) 0x00, // File size - 8
            (byte) 0x57, (byte) 0x41, (byte) 0x56, (byte) 0x45, // "WAVE"
            (byte) 0x66, (byte) 0x6D, (byte) 0x74, (byte) 0x20, // "fmt "
            (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, // Chunk size
            (byte) 0x01, (byte) 0x00,                          // Audio format (PCM)
            (byte) 0x01, (byte) 0x00,                          // Channels (mono)
            (byte) 0x40, (byte) 0x3E, (byte) 0x00, (byte) 0x00, // Sample rate (16000)
            (byte) 0x80, (byte) 0x7C, (byte) 0x00, (byte) 0x00, // Byte rate
            (byte) 0x02, (byte) 0x00,                          // Block align
            (byte) 0x10, (byte) 0x00,                          // Bits per sample
            (byte) 0x64, (byte) 0x61, (byte) 0x74, (byte) 0x61, // "data"
            (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00  // Data size
        };
        
        // 1 segundo de silencio (16000 samples * 2 bytes)
        byte[] silence = new byte[32000];
        
        // Combinar header y datos
        byte[] result = new byte[wavHeader.length + silence.length];
        System.arraycopy(wavHeader, 0, result, 0, wavHeader.length);
        System.arraycopy(silence, 0, result, wavHeader.length, silence.length);
        
        return result;
    }
}
