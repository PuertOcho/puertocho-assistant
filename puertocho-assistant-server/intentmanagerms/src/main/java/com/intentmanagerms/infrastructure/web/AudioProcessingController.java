package com.intentmanagerms.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.application.services.AudioProcessingService;
import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para el procesamiento de audio.
 * Maneja la recepción de archivos de audio y metadata contextual.
 */
@RestController
@RequestMapping("/api/v1/audio")
@CrossOrigin(origins = "*")
public class AudioProcessingController {

    private static final Logger logger = LoggerFactory.getLogger(AudioProcessingController.class);

    @Autowired
    private AudioProcessingService audioProcessingService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Procesa audio con metadata contextual
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> processAudio(
            @RequestParam(value = "audio", required = false) MultipartFile audioFile,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "processing_config", required = false) String processingConfigJson) {

        try {
            // Validar que se haya enviado un archivo
            if (audioFile == null) {
                return createErrorResponse("No se ha enviado ningún archivo de audio", HttpStatus.BAD_REQUEST);
            }

            logger.info("Recibida petición de procesamiento de audio: {}", audioFile.getOriginalFilename());

            // Validar archivo de audio
            if (audioFile.isEmpty()) {
                return createErrorResponse("El archivo de audio no puede estar vacío", HttpStatus.BAD_REQUEST);
            }

            // Crear petición de procesamiento
            AudioProcessingRequest request = createAudioProcessingRequest(audioFile, metadataJson, processingConfigJson);

            // Procesar audio
            AudioProcessingResult result = audioProcessingService.processAudio(request);

            // Crear respuesta
            Map<String, Object> response = createSuccessResponse(result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en el procesamiento de audio: {}", e.getMessage(), e);
            return createErrorResponse("Error en el procesamiento de audio: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Procesa audio simple sin metadata
     */
    @PostMapping(value = "/process/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> processSimpleAudio(
            @RequestParam(value = "audio", required = false) MultipartFile audioFile) {

        try {
            // Validar que se haya enviado un archivo
            if (audioFile == null) {
                return createErrorResponse("No se ha enviado ningún archivo de audio", HttpStatus.BAD_REQUEST);
            }

            logger.info("Recibida petición de procesamiento simple de audio: {}", audioFile.getOriginalFilename());

            // Validar archivo de audio
            if (audioFile.isEmpty()) {
                return createErrorResponse("El archivo de audio no puede estar vacío", HttpStatus.BAD_REQUEST);
            }

            // Procesar audio simple
            AudioProcessingResult result = audioProcessingService.processSimpleAudio(
                    audioFile.getBytes(),
                    audioFile.getOriginalFilename(),
                    audioFile.getContentType()
            );

            // Crear respuesta
            Map<String, Object> response = createSuccessResponse(result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en el procesamiento simple de audio: {}", e.getMessage(), e);
            return createErrorResponse("Error en el procesamiento de audio: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene los formatos de audio soportados
     */
    @GetMapping("/supported-formats")
    public ResponseEntity<Map<String, Object>> getSupportedFormats() {
        logger.debug("Solicitados formatos de audio soportados");

        try {
            String[] formats = audioProcessingService.getSupportedFormats();
            long maxFileSize = audioProcessingService.getMaxFileSizeBytes();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("supported_formats", formats);
            response.put("max_file_size_bytes", maxFileSize);
            response.put("max_file_size_mb", maxFileSize / (1024 * 1024));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo formatos soportados: {}", e.getMessage(), e);
            return createErrorResponse("Error obteniendo formatos soportados: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Health check del servicio de audio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        logger.debug("Health check del servicio de audio");

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("service", "audio-processing");
            response.put("timestamp", System.currentTimeMillis());
            response.put("supported_formats_count", audioProcessingService.getSupportedFormats().length);
            response.put("max_file_size_mb", audioProcessingService.getMaxFileSizeBytes() / (1024 * 1024));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en health check: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * Test del servicio con archivo de ejemplo
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testAudioProcessing() {
        logger.info("Ejecutando test del servicio de audio");

        try {
            // Crear datos de prueba
            byte[] testAudioData = "test audio data".getBytes();
            String testFilename = "test.wav";
            String testContentType = "audio/wav";

            // Procesar audio de prueba
            AudioProcessingResult result = audioProcessingService.processSimpleAudio(
                    testAudioData, testFilename, testContentType
            );

            // Crear respuesta de test
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("test_result", result);
            response.put("message", "Test del servicio de audio completado exitosamente");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en test del servicio: {}", e.getMessage(), e);
            return createErrorResponse("Error en test del servicio: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Crea una petición de procesamiento de audio
     */
    private AudioProcessingRequest createAudioProcessingRequest(MultipartFile audioFile, 
                                                              String metadataJson, 
                                                              String processingConfigJson) throws Exception {
        
        String requestId = UUID.randomUUID().toString();
        byte[] audioData = audioFile.getBytes();
        String filename = audioFile.getOriginalFilename();
        String contentType = audioFile.getContentType();

        // Parsear metadata si está presente
        AudioMetadata metadata = null;
        if (metadataJson != null && !metadataJson.trim().isEmpty()) {
            try {
                metadata = objectMapper.readValue(metadataJson, AudioMetadata.class);
            } catch (Exception e) {
                logger.warn("Error parseando metadata JSON: {}", e.getMessage());
            }
        }

        // Parsear configuración de procesamiento si está presente
        AudioProcessingRequest.AudioProcessingConfig processingConfig = null;
        if (processingConfigJson != null && !processingConfigJson.trim().isEmpty()) {
            try {
                processingConfig = objectMapper.readValue(processingConfigJson, 
                        AudioProcessingRequest.AudioProcessingConfig.class);
            } catch (Exception e) {
                logger.warn("Error parseando configuración JSON: {}", e.getMessage());
            }
        }

        return new AudioProcessingRequest(requestId, audioData, filename, contentType, 
                (long) audioData.length, metadata, processingConfig);
    }

    /**
     * Crea una respuesta de éxito
     */
    private Map<String, Object> createSuccessResponse(AudioProcessingResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", result);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Crea una respuesta de error
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String errorMessage, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(response);
    }
}
