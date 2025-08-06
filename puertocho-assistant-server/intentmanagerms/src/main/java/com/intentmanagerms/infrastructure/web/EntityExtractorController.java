package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.EntityExtractor;
import com.intentmanagerms.domain.model.EntityExtractionRequest;
import com.intentmanagerms.domain.model.EntityExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el servicio de extracción de entidades.
 * Proporciona endpoints para extracción, validación y resolución de entidades.
 */
@RestController
@RequestMapping("/api/v1/entity-extraction")
@CrossOrigin(origins = "*")
public class EntityExtractorController {

    private static final Logger logger = LoggerFactory.getLogger(EntityExtractorController.class);

    @Autowired
    private EntityExtractor entityExtractor;

    /**
     * Extrae entidades de un texto.
     */
    @PostMapping("/extract")
    public ResponseEntity<EntityExtractionResult> extractEntities(@RequestBody EntityExtractionRequest request) {
        try {
            logger.info("Solicitud de extracción de entidades recibida: {}", request.getText());
            
            EntityExtractionResult result = entityExtractor.extractEntities(request);
            
            if (result.isSuccess()) {
                logger.info("Extracción exitosa: {} entidades encontradas", result.getTotalEntitiesFound());
                return ResponseEntity.ok(result);
            } else {
                logger.error("Error en extracción: {}", result.getErrorMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando solicitud de extracción: {}", e.getMessage(), e);
            EntityExtractionResult errorResult = new EntityExtractionResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * Extrae entidades de un texto simple.
     */
    @PostMapping("/extract-simple")
    public ResponseEntity<EntityExtractionResult> extractEntitiesSimple(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResult("El texto no puede estar vacío"));
            }

            EntityExtractionRequest extractionRequest = new EntityExtractionRequest(text);
            
            // Configurar parámetros opcionales
            if (request.containsKey("entity_types")) {
                @SuppressWarnings("unchecked")
                var entityTypes = (java.util.List<String>) request.get("entity_types");
                extractionRequest.setEntityTypes(entityTypes);
            }
            
            if (request.containsKey("confidence_threshold")) {
                double threshold = ((Number) request.get("confidence_threshold")).doubleValue();
                extractionRequest.setConfidenceThreshold(threshold);
            }
            
            if (request.containsKey("max_entities")) {
                int maxEntities = ((Number) request.get("max_entities")).intValue();
                extractionRequest.setMaxEntities(maxEntities);
            }

            return extractEntities(extractionRequest);
            
        } catch (Exception e) {
            logger.error("Error procesando solicitud simple: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResult("Error procesando solicitud: " + e.getMessage()));
        }
    }

    /**
     * Extrae entidades de un texto con contexto conversacional.
     */
    @PostMapping("/extract-with-context")
    public ResponseEntity<EntityExtractionResult> extractEntitiesWithContext(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            String sessionId = (String) request.get("conversation_session_id");
            String context = (String) request.get("context");
            String intent = (String) request.get("intent");
            
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResult("El texto no puede estar vacío"));
            }

            EntityExtractionRequest extractionRequest = new EntityExtractionRequest(text);
            extractionRequest.setConversationSessionId(sessionId);
            extractionRequest.setContext(context);
            extractionRequest.setIntent(intent);
            
            // Configurar parámetros adicionales
            if (request.containsKey("enable_anaphora_resolution")) {
                boolean enableAnaphora = (Boolean) request.get("enable_anaphora_resolution");
                extractionRequest.setEnableAnaphoraResolution(enableAnaphora);
            }
            
            if (request.containsKey("enable_context_resolution")) {
                boolean enableContext = (Boolean) request.get("enable_context_resolution");
                extractionRequest.setEnableContextResolution(enableContext);
            }

            return extractEntities(extractionRequest);
            
        } catch (Exception e) {
            logger.error("Error procesando solicitud con contexto: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResult("Error procesando solicitud: " + e.getMessage()));
        }
    }

    /**
     * Extrae entidades específicas de un texto.
     */
    @PostMapping("/extract-specific")
    public ResponseEntity<EntityExtractionResult> extractSpecificEntities(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            @SuppressWarnings("unchecked")
            var entityTypes = (java.util.List<String>) request.get("entity_types");
            
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResult("El texto no puede estar vacío"));
            }
            
            if (entityTypes == null || entityTypes.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResult("Debe especificar al menos un tipo de entidad"));
            }

            EntityExtractionRequest extractionRequest = new EntityExtractionRequest(text, entityTypes);
            
            // Configurar métodos de extracción específicos
            if (request.containsKey("extraction_methods")) {
                @SuppressWarnings("unchecked")
                var methods = (java.util.List<String>) request.get("extraction_methods");
                extractionRequest.setExtractionMethods(methods);
            }

            return extractEntities(extractionRequest);
            
        } catch (Exception e) {
            logger.error("Error procesando solicitud específica: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResult("Error procesando solicitud: " + e.getMessage()));
        }
    }

    /**
     * Valida entidades extraídas.
     */
    @PostMapping("/validate")
    public ResponseEntity<EntityExtractionResult> validateEntities(@RequestBody EntityExtractionRequest request) {
        try {
            logger.info("Solicitud de validación de entidades recibida");
            
            // Crear una solicitud de extracción con validación habilitada
            request.setEnableValidation(true);
            EntityExtractionResult result = entityExtractor.extractEntities(request);
            
            if (result.isSuccess()) {
                logger.info("Validación exitosa: {} entidades validadas", result.getTotalEntitiesFound());
                return ResponseEntity.ok(result);
            } else {
                logger.error("Error en validación: {}", result.getErrorMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando solicitud de validación: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResult("Error en validación: " + e.getMessage()));
        }
    }

    /**
     * Resuelve anáforas en entidades.
     */
    @PostMapping("/resolve-anaphoras")
    public ResponseEntity<EntityExtractionResult> resolveAnaphoras(@RequestBody EntityExtractionRequest request) {
        try {
            logger.info("Solicitud de resolución de anáforas recibida");
            
            // Crear una solicitud de extracción con resolución de anáforas habilitada
            request.setEnableAnaphoraResolution(true);
            EntityExtractionResult result = entityExtractor.extractEntities(request);
            
            if (result.isSuccess()) {
                logger.info("Resolución exitosa: {} anáforas resueltas", result.getAnaphoraResolved());
                return ResponseEntity.ok(result);
            } else {
                logger.error("Error en resolución: {}", result.getErrorMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando solicitud de resolución: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResult("Error en resolución: " + e.getMessage()));
        }
    }

    /**
     * Limpia el cache de extracción.
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            logger.info("Solicitud de limpieza de cache recibida");
            
            entityExtractor.clearCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cache limpiado exitosamente");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            logger.info("Cache limpiado exitosamente");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error limpiando cache: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Error limpiando cache: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene estadísticas del servicio.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            logger.info("Solicitud de estadísticas recibida");
            
            Map<String, Object> stats = entityExtractor.getStatistics();
            stats.put("timestamp", java.time.LocalDateTime.now());
            stats.put("service", "EntityExtractor");
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Error obteniendo estadísticas: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Verifica el estado de salud del servicio.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean isHealthy = entityExtractor.isHealthy();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", isHealthy ? "healthy" : "unhealthy");
            response.put("service", "EntityExtractor");
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("healthy", isHealthy);
            
            if (isHealthy) {
                logger.debug("Health check exitoso");
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Health check fallido");
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error en health check: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("service", "EntityExtractor");
            response.put("error", e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("healthy", false);
            
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Endpoint de prueba automatizada.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> runTest() {
        try {
            logger.info("Ejecutando prueba automatizada del EntityExtractor");
            
            Map<String, Object> testResults = new HashMap<>();
            testResults.put("service", "EntityExtractor");
            testResults.put("timestamp", java.time.LocalDateTime.now());
            
            // Prueba 1: Extracción simple
            EntityExtractionRequest simpleRequest = new EntityExtractionRequest("¿Qué tiempo hace en Madrid mañana?");
            EntityExtractionResult simpleResult = entityExtractor.extractEntities(simpleRequest);
            testResults.put("simple_extraction", simpleResult.isSuccess());
            testResults.put("simple_entities_found", simpleResult.getTotalEntitiesFound());
            
            // Prueba 2: Extracción con tipos específicos
            EntityExtractionRequest specificRequest = new EntityExtractionRequest("Enciende la luz del salón");
            specificRequest.setEntityTypes(java.util.Arrays.asList("lugar"));
            EntityExtractionResult specificResult = entityExtractor.extractEntities(specificRequest);
            testResults.put("specific_extraction", specificResult.isSuccess());
            testResults.put("specific_entities_found", specificResult.getTotalEntitiesFound());
            
            // Prueba 3: Health check
            boolean isHealthy = entityExtractor.isHealthy();
            testResults.put("health_check", isHealthy);
            
            // Prueba 4: Estadísticas
            Map<String, Object> stats = entityExtractor.getStatistics();
            testResults.put("statistics_available", !stats.isEmpty());
            
            // Resumen
            boolean allTestsPassed = (Boolean) testResults.get("simple_extraction") &&
                                   (Boolean) testResults.get("specific_extraction") &&
                                   (Boolean) testResults.get("health_check") &&
                                   (Boolean) testResults.get("statistics_available");
            
            testResults.put("all_tests_passed", allTestsPassed);
            testResults.put("success", allTestsPassed);
            
            logger.info("Prueba automatizada completada: {}", allTestsPassed ? "ÉXITO" : "FALLO");
            
            return ResponseEntity.ok(testResults);
            
        } catch (Exception e) {
            logger.error("Error en prueba automatizada: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Error en prueba automatizada: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Crea un resultado de error.
     */
    private EntityExtractionResult createErrorResult(String errorMessage) {
        EntityExtractionResult result = new EntityExtractionResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
} 