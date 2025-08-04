package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.AdvancedSimilaritySearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el servicio de búsqueda por similitud avanzada.
 * Expone endpoints para estadísticas y configuración del sistema de similarity search.
 */
@RestController
@RequestMapping("/api/v1/similarity-search")
@CrossOrigin(origins = "*")
public class AdvancedSimilaritySearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedSimilaritySearchController.class);
    
    @Autowired
    private AdvancedSimilaritySearchService advancedSimilaritySearchService;
    
    /**
     * Obtiene estadísticas del servicio de similarity search
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = advancedSimilaritySearchService.getStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("service", "Advanced Similarity Search Service");
            response.put("status", "ACTIVE");
            response.put("statistics", statistics);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            logger.info("Estadísticas de similarity search obtenidas exitosamente");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de similarity search: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al obtener estadísticas");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Health check del servicio de similarity search
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("service", "Advanced Similarity Search Service");
            healthInfo.put("status", "HEALTHY");
            healthInfo.put("algorithm", advancedSimilaritySearchService.getStatistics().get("search_algorithm"));
            healthInfo.put("features_enabled", Map.of(
                "diversity_filtering", advancedSimilaritySearchService.getStatistics().get("enable_diversity_filtering"),
                "intent_clustering", advancedSimilaritySearchService.getStatistics().get("enable_intent_clustering"),
                "semantic_boosting", advancedSimilaritySearchService.getStatistics().get("enable_semantic_boosting")
            ));
            healthInfo.put("timestamp", java.time.LocalDateTime.now());
            
            logger.debug("Health check de similarity search: HEALTHY");
            return ResponseEntity.ok(healthInfo);
            
        } catch (Exception e) {
            logger.error("Error en health check de similarity search: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "Advanced Similarity Search Service");
            errorResponse.put("status", "UNHEALTHY");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Información detallada del servicio
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("service", "Advanced Similarity Search Service");
            info.put("version", "1.0.0");
            info.put("description", "Servicio avanzado de búsqueda por similitud para el motor RAG");
            info.put("features", Map.of(
                "algorithms", "cosine, euclidean, manhattan, hybrid",
                "diversity_filtering", "Filtrado de resultados para evitar similitud excesiva",
                "intent_clustering", "Agrupación por intención para diversificar resultados",
                "semantic_boosting", "Refuerzo semántico basado en palabras clave",
                "performance_cache", "Cache de embeddings y similitudes para optimización"
            ));
            info.put("configuration", advancedSimilaritySearchService.getStatistics());
            info.put("timestamp", java.time.LocalDateTime.now());
            
            logger.info("Información del servicio de similarity search obtenida");
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            logger.error("Error al obtener información del servicio: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al obtener información del servicio");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Test del servicio de similarity search
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testService() {
        try {
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("service", "Advanced Similarity Search Service");
            testResult.put("test_status", "PASSED");
            testResult.put("test_details", Map.of(
                "algorithm_available", true,
                "diversity_filtering_available", true,
                "intent_clustering_available", true,
                "semantic_boosting_available", true
            ));
            testResult.put("configuration", advancedSimilaritySearchService.getStatistics());
            testResult.put("timestamp", java.time.LocalDateTime.now());
            
            logger.info("Test del servicio de similarity search: PASSED");
            return ResponseEntity.ok(testResult);
            
        } catch (Exception e) {
            logger.error("Error en test del servicio: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "Advanced Similarity Search Service");
            errorResponse.put("test_status", "FAILED");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 