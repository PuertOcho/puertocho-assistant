package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.IntelligentFallbackService;
import com.intentmanagerms.domain.model.IntentClassificationRequest;
import com.intentmanagerms.domain.model.IntentClassificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el servicio de fallback inteligente.
 * 
 * Endpoints disponibles:
 * - GET /api/v1/fallback/statistics - Estadísticas del servicio
 * - GET /api/v1/fallback/health - Health check
 * - POST /api/v1/fallback/test - Test del servicio
 * - POST /api/v1/fallback/classify - Clasificación con fallback forzado
 */
@RestController
@RequestMapping("/api/v1/fallback")
@CrossOrigin(origins = "*")
public class IntelligentFallbackController {
    
    private static final Logger logger = LoggerFactory.getLogger(IntelligentFallbackController.class);
    
    @Autowired
    private IntelligentFallbackService intelligentFallbackService;
    
    /**
     * Obtiene estadísticas del servicio de fallback inteligente
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            logger.info("Obteniendo estadísticas del servicio de fallback inteligente");
            
            Map<String, Object> statistics = intelligentFallbackService.getFallbackStatistics();
            
            // Añadir metadata adicional
            statistics.put("timestamp", java.time.LocalDateTime.now().toString());
            statistics.put("service", "IntelligentFallbackService");
            statistics.put("version", "1.0.0");
            
            logger.info("Estadísticas del fallback obtenidas exitosamente");
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del fallback: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error obteniendo estadísticas del fallback");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Health check del servicio de fallback inteligente
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            logger.debug("Verificando salud del servicio de fallback inteligente");
            
            boolean isHealthy = intelligentFallbackService.isHealthy();
            
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", isHealthy ? "HEALTHY" : "UNHEALTHY");
            healthInfo.put("service", "IntelligentFallbackService");
            healthInfo.put("timestamp", java.time.LocalDateTime.now().toString());
            healthInfo.put("healthy", isHealthy);
            
            if (isHealthy) {
                healthInfo.put("message", "Servicio de fallback inteligente funcionando correctamente");
                logger.debug("Health check del fallback: HEALTHY");
                return ResponseEntity.ok(healthInfo);
            } else {
                healthInfo.put("message", "Servicio de fallback inteligente con problemas");
                logger.warn("Health check del fallback: UNHEALTHY");
                return ResponseEntity.status(503).body(healthInfo);
            }
            
        } catch (Exception e) {
            logger.error("Error en health check del fallback: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("service", "IntelligentFallbackService");
            errorResponse.put("error", "Error verificando salud del servicio");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Test del servicio de fallback inteligente
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testFallback() {
        try {
            logger.info("Iniciando test del servicio de fallback inteligente");
            
            // Crear request de prueba
            IntentClassificationRequest testRequest = new IntentClassificationRequest("test fallback");
            IntentClassificationResult originalResult = new IntentClassificationResult();
            originalResult.setIntentId("test");
            originalResult.setConfidenceScore(0.1);
            
            long startTime = System.currentTimeMillis();
            
            // Aplicar fallback inteligente
            IntentClassificationResult result = intelligentFallbackService.applyIntelligentFallback(
                testRequest, originalResult, java.util.Collections.emptyList(), startTime);
            
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("success", true);
            testResult.put("original_intent", "test");
            testResult.put("original_confidence", 0.1);
            testResult.put("fallback_intent", result.getIntentId());
            testResult.put("fallback_confidence", result.getConfidenceScore());
            testResult.put("fallback_used", result.getFallbackUsed());
            testResult.put("fallback_reason", result.getFallbackReason());
            testResult.put("processing_time_ms", result.getProcessingTimeMs());
            testResult.put("timestamp", java.time.LocalDateTime.now().toString());
            
            logger.info("Test del fallback completado exitosamente: {} (confidence: {})", 
                result.getIntentId(), result.getConfidenceScore());
            
            return ResponseEntity.ok(testResult);
            
        } catch (Exception e) {
            logger.error("Error en test del fallback: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error en test del servicio de fallback");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Clasificación con fallback forzado para testing
     */
    @PostMapping("/classify")
    public ResponseEntity<Map<String, Object>> classifyWithFallback(
            @RequestBody IntentClassificationRequest request) {
        try {
            logger.info("Clasificación con fallback forzado para texto: '{}'", request.getText());
            
            // Crear resultado original con confidence bajo para forzar fallback
            IntentClassificationResult originalResult = new IntentClassificationResult();
            originalResult.setIntentId("unknown");
            originalResult.setConfidenceScore(0.1);
            
            long startTime = System.currentTimeMillis();
            
            // Aplicar fallback inteligente
            IntentClassificationResult result = intelligentFallbackService.applyIntelligentFallback(
                request, originalResult, java.util.Collections.emptyList(), startTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("text", request.getText());
            response.put("intent_id", result.getIntentId());
            response.put("confidence_score", result.getConfidenceScore());
            response.put("fallback_used", result.getFallbackUsed());
            response.put("fallback_reason", result.getFallbackReason());
            response.put("processing_time_ms", result.getProcessingTimeMs());
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            logger.info("Clasificación con fallback completada: {} (confidence: {})", 
                result.getIntentId(), result.getConfidenceScore());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error en clasificación con fallback: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error en clasificación con fallback");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Test de múltiples niveles de degradación
     */
    @PostMapping("/test-degradation")
    public ResponseEntity<Map<String, Object>> testDegradationLevels(
            @RequestBody IntentClassificationRequest request) {
        try {
            logger.info("Test de niveles de degradación para texto: '{}'", request.getText());
            
            // Crear resultado original con confidence bajo para forzar fallback
            IntentClassificationResult originalResult = new IntentClassificationResult();
            originalResult.setIntentId("unknown");
            originalResult.setConfidenceScore(0.1);
            
            long startTime = System.currentTimeMillis();
            
            // Aplicar fallback inteligente
            IntentClassificationResult result = intelligentFallbackService.applyIntelligentFallback(
                request, originalResult, java.util.Collections.emptyList(), startTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("text", request.getText());
            response.put("original_intent", "unknown");
            response.put("original_confidence", 0.1);
            response.put("final_intent", result.getIntentId());
            response.put("final_confidence", result.getConfidenceScore());
            response.put("fallback_used", result.getFallbackUsed());
            response.put("fallback_reason", result.getFallbackReason());
            response.put("processing_time_ms", result.getProcessingTimeMs());
            response.put("degradation_test", true);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            logger.info("Test de degradación completado: {} (confidence: {})", 
                result.getIntentId(), result.getConfidenceScore());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error en test de degradación: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error en test de degradación");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 