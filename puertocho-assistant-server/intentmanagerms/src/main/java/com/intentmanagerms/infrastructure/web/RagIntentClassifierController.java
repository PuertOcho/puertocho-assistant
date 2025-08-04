package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.RagIntentClassifier;
import com.intentmanagerms.domain.model.IntentClassificationRequest;
import com.intentmanagerms.domain.model.IntentClassificationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el motor RAG de clasificación de intenciones.
 * Expone endpoints para clasificar texto y obtener estadísticas del sistema.
 */
@RestController
@RequestMapping("/api/v1/rag-classifier")
@Tag(name = "RAG Intent Classifier", description = "Motor RAG para clasificación de intenciones")
public class RagIntentClassifierController {
    
    private static final Logger logger = LoggerFactory.getLogger(RagIntentClassifierController.class);
    
    @Autowired
    private RagIntentClassifier ragIntentClassifier;
    
    /**
     * Clasifica texto simple
     */
    @PostMapping("/classify")
    @Operation(summary = "Clasificar intención de texto", 
               description = "Clasifica la intención de un texto usando el motor RAG")
    public ResponseEntity<IntentClassificationResult> classifyText(
            @Parameter(description = "Texto a clasificar") 
            @RequestParam String text) {
        
        logger.info("Recibida petición de clasificación para texto: '{}'", text);
        
        try {
            IntentClassificationResult result = ragIntentClassifier.classifyText(text);
            logger.info("Clasificación completada: {} (confidence: {})", 
                result.getIntentId(), result.getConfidenceScore());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error durante clasificación: {}", e.getMessage(), e);
            IntentClassificationResult errorResult = new IntentClassificationResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            errorResult.setIntentId("ayuda");
            errorResult.setConfidenceScore(0.0);
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Clasifica texto con metadata completa
     */
    @PostMapping("/classify/advanced")
    @Operation(summary = "Clasificar intención con metadata", 
               description = "Clasifica la intención de un texto con metadata contextual completa")
    public ResponseEntity<IntentClassificationResult> classifyWithMetadata(
            @Parameter(description = "Petición de clasificación con metadata") 
            @RequestBody IntentClassificationRequest request) {
        
        logger.info("Recibida petición de clasificación avanzada para texto: '{}'", request.getText());
        
        try {
            IntentClassificationResult result = ragIntentClassifier.classifyIntent(request);
            logger.info("Clasificación avanzada completada: {} (confidence: {})", 
                result.getIntentId(), result.getConfidenceScore());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error durante clasificación avanzada: {}", e.getMessage(), e);
            IntentClassificationResult errorResult = new IntentClassificationResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            errorResult.setIntentId("ayuda");
            errorResult.setConfidenceScore(0.0);
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Clasifica texto con session ID
     */
    @PostMapping("/classify/session/{sessionId}")
    @Operation(summary = "Clasificar intención con session ID", 
               description = "Clasifica la intención de un texto asociado a una sesión específica")
    public ResponseEntity<IntentClassificationResult> classifyWithSession(
            @Parameter(description = "ID de la sesión") 
            @PathVariable String sessionId,
            @Parameter(description = "Texto a clasificar") 
            @RequestParam String text) {
        
        logger.info("Recibida petición de clasificación con session {} para texto: '{}'", sessionId, text);
        
        try {
            IntentClassificationResult result = ragIntentClassifier.classifyText(text, sessionId);
            logger.info("Clasificación con session completada: {} (confidence: {})", 
                result.getIntentId(), result.getConfidenceScore());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error durante clasificación con session: {}", e.getMessage(), e);
            IntentClassificationResult errorResult = new IntentClassificationResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            errorResult.setIntentId("ayuda");
            errorResult.setConfidenceScore(0.0);
            
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
    
    /**
     * Clasifica múltiples textos en batch
     */
    @PostMapping("/classify/batch")
    @Operation(summary = "Clasificar múltiples textos", 
               description = "Clasifica múltiples textos en una sola petición")
    public ResponseEntity<Map<String, IntentClassificationResult>> classifyBatch(
            @Parameter(description = "Lista de textos a clasificar") 
            @RequestBody Map<String, String> textBatch) {
        
        logger.info("Recibida petición de clasificación batch con {} textos", textBatch.size());
        
        Map<String, IntentClassificationResult> results = new HashMap<>();
        
        try {
            for (Map.Entry<String, String> entry : textBatch.entrySet()) {
                String textId = entry.getKey();
                String text = entry.getValue();
                
                logger.debug("Clasificando texto {}: '{}'", textId, text);
                IntentClassificationResult result = ragIntentClassifier.classifyText(text);
                results.put(textId, result);
            }
            
            logger.info("Clasificación batch completada para {} textos", textBatch.size());
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error durante clasificación batch: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Obtiene estadísticas del motor RAG
     */
    @GetMapping("/statistics")
    @Operation(summary = "Obtener estadísticas del motor RAG", 
               description = "Retorna estadísticas de rendimiento y configuración del motor RAG")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        
        logger.debug("Recibida petición de estadísticas del motor RAG");
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // Estadísticas básicas del motor
            statistics.put("engine_name", "RAG Intent Classifier");
            statistics.put("version", "1.0.0");
            statistics.put("status", "ACTIVE");
            
            // Configuración actual
            statistics.put("default_max_examples", 5);
            statistics.put("default_confidence_threshold", 0.7);
            statistics.put("similarity_threshold", 0.6);
            statistics.put("enable_fallback", true);
            statistics.put("fallback_confidence_threshold", 0.5);
            statistics.put("max_processing_time_ms", 10000);
            
            // Métricas de rendimiento (simuladas por ahora)
            statistics.put("total_classifications", 0);
            statistics.put("average_processing_time_ms", 0);
            statistics.put("success_rate", 1.0);
            statistics.put("fallback_usage_rate", 0.0);
            
            // Información de componentes
            statistics.put("vector_store_available", true);
            statistics.put("llm_available", true);
            statistics.put("intent_config_available", true);
            
            logger.debug("Estadísticas del motor RAG generadas");
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del motor RAG: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Health check del motor RAG
     */
    @GetMapping("/health")
    @Operation(summary = "Health check del motor RAG", 
               description = "Verifica el estado de salud del motor RAG")
    public ResponseEntity<Map<String, Object>> getHealth() {
        
        logger.debug("Recibida petición de health check del motor RAG");
        
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Verificar componentes principales
            boolean vectorStoreHealthy = true; // TODO: Verificar VectorStoreService
            boolean llmHealthy = true; // TODO: Verificar LlmConfigurationService
            boolean intentConfigHealthy = true; // TODO: Verificar IntentConfigManager
            
            boolean overallHealthy = vectorStoreHealthy && llmHealthy && intentConfigHealthy;
            
            health.put("status", overallHealthy ? "HEALTHY" : "UNHEALTHY");
            health.put("timestamp", System.currentTimeMillis());
            
            // Estado de componentes
            Map<String, Object> components = new HashMap<>();
            components.put("vector_store", vectorStoreHealthy ? "UP" : "DOWN");
            components.put("llm_service", llmHealthy ? "UP" : "DOWN");
            components.put("intent_config", intentConfigHealthy ? "UP" : "DOWN");
            health.put("components", components);
            
            // Información adicional
            health.put("version", "1.0.0");
            health.put("uptime_ms", System.currentTimeMillis()); // TODO: Calcular uptime real
            
            logger.debug("Health check del motor RAG completado: {}", overallHealthy ? "HEALTHY" : "UNHEALTHY");
            
            if (overallHealthy) {
                return ResponseEntity.ok(health);
            } else {
                return ResponseEntity.status(503).body(health);
            }
            
        } catch (Exception e) {
            logger.error("Error durante health check del motor RAG: {}", e.getMessage(), e);
            
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("status", "ERROR");
            errorHealth.put("error", e.getMessage());
            errorHealth.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(errorHealth);
        }
    }
    
    /**
     * Test del motor RAG con ejemplos predefinidos
     */
    @PostMapping("/test")
    @Operation(summary = "Test del motor RAG", 
               description = "Ejecuta tests del motor RAG con ejemplos predefinidos")
    public ResponseEntity<Map<String, Object>> testClassifier() {
        
        logger.info("Iniciando test del motor RAG");
        
        try {
            Map<String, Object> testResults = new HashMap<>();
            Map<String, IntentClassificationResult> results = new HashMap<>();
            
            // Ejemplos de test
            String[] testTexts = {
                "¿qué tiempo hace en Madrid?",
                "enciende la luz del salón",
                "ayúdame con algo",
                "reproduce música relajante",
                "programa una alarma para mañana"
            };
            
            int successCount = 0;
            long totalProcessingTime = 0;
            
            for (String text : testTexts) {
                try {
                    long startTime = System.currentTimeMillis();
                    IntentClassificationResult result = ragIntentClassifier.classifyText(text);
                    long processingTime = System.currentTimeMillis() - startTime;
                    
                    results.put(text, result);
                    totalProcessingTime += processingTime;
                    
                    if (result.getSuccess()) {
                        successCount++;
                    }
                    
                    logger.debug("Test completado para '{}': {} ({}ms)", 
                        text, result.getIntentId(), processingTime);
                        
                } catch (Exception e) {
                    logger.error("Error en test para texto '{}': {}", text, e.getMessage());
                    IntentClassificationResult errorResult = new IntentClassificationResult();
                    errorResult.setSuccess(false);
                    errorResult.setErrorMessage(e.getMessage());
                    results.put(text, errorResult);
                }
            }
            
            // Estadísticas del test
            testResults.put("total_tests", testTexts.length);
            testResults.put("successful_tests", successCount);
            testResults.put("failed_tests", testTexts.length - successCount);
            testResults.put("success_rate", (double) successCount / testTexts.length);
            testResults.put("average_processing_time_ms", totalProcessingTime / testTexts.length);
            testResults.put("results", results);
            
            logger.info("Test del motor RAG completado: {}/{} exitosos", successCount, testTexts.length);
            
            return ResponseEntity.ok(testResults);
            
        } catch (Exception e) {
            logger.error("Error durante test del motor RAG: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("status", "FAILED");
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
} 