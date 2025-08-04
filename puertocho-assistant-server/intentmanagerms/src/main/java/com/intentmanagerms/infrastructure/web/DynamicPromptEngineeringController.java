package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.DynamicPromptEngineeringService;
import com.intentmanagerms.domain.model.EmbeddingDocument;
import com.intentmanagerms.domain.model.IntentClassificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Controlador REST para el servicio de prompt engineering dinámico
 * 
 * Endpoints disponibles:
 * - POST /api/v1/prompt-engineering/build - Construir prompt dinámico
 * - GET /api/v1/prompt-engineering/strategies - Listar estrategias disponibles
 * - GET /api/v1/prompt-engineering/statistics - Estadísticas del servicio
 * - GET /api/v1/prompt-engineering/health - Health check
 * - POST /api/v1/prompt-engineering/test - Test del servicio
 */
@RestController
@RequestMapping("/api/v1/prompt-engineering")
@CrossOrigin(origins = "*")
public class DynamicPromptEngineeringController {

    private static final Logger logger = LoggerFactory.getLogger(DynamicPromptEngineeringController.class);

    @Autowired
    private DynamicPromptEngineeringService promptEngineeringService;

    /**
     * Construye un prompt dinámico basado en la solicitud y ejemplos
     */
    @PostMapping("/build")
    public ResponseEntity<Map<String, Object>> buildDynamicPrompt(
            @RequestBody PromptBuildRequest request) {
        
        return buildPromptWithStrategy(request, null);
    }

    /**
     * Construye un prompt con estrategia específica
     */
    @PostMapping("/build/{strategy}")
    public ResponseEntity<Map<String, Object>> buildPromptWithStrategy(
            @RequestBody PromptBuildRequest request,
            @PathVariable String strategy) {
        
        logger.info("Construyendo prompt dinámico para texto: '{}'", 
            request.getText() != null ? request.getText().substring(0, Math.min(50, request.getText().length())) + "..." : "null");
        
        try {
            // Crear request de clasificación
            IntentClassificationRequest classificationRequest = new IntentClassificationRequest();
            classificationRequest.setText(request.getText());
            classificationRequest.setSessionId(request.getSessionId());
            classificationRequest.setUserId(request.getUserId());
            classificationRequest.setContextMetadata(request.getContextMetadata());
            classificationRequest.setTimestamp(LocalDateTime.now());
            
            // Construir prompt dinámico con estrategia específica si se proporciona
            String prompt;
            if (strategy != null && !strategy.isEmpty()) {
                // Usar estrategia específica
                prompt = promptEngineeringService.buildPromptWithStrategy(classificationRequest, request.getExamples(), strategy);
            } else {
                // Usar estrategia por defecto
                prompt = promptEngineeringService.buildDynamicPrompt(classificationRequest, request.getExamples());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prompt", prompt);
            response.put("prompt_length", prompt.length());
            response.put("examples_count", request.getExamples() != null ? request.getExamples().size() : 0);
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Prompt dinámico construido exitosamente ({} caracteres)", prompt.length());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error construyendo prompt dinámico: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Lista las estrategias de prompt disponibles
     */
    @GetMapping("/strategies")
    public ResponseEntity<Map<String, Object>> getAvailableStrategies() {
        logger.info("Obteniendo estrategias de prompt disponibles");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("strategies", Arrays.asList(
            "adaptive", "few-shot", "zero-shot", "chain-of-thought", "expert-domain"
        ));
        response.put("default_strategy", "adaptive");
        response.put("description", "Estrategias de prompt engineering dinámico");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene estadísticas del servicio de prompt engineering
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.info("Obteniendo estadísticas del servicio de prompt engineering");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service_name", "Dynamic Prompt Engineering Service");
        response.put("version", "1.0.0");
        response.put("status", "ACTIVE");
        response.put("features", Arrays.asList(
            "Adaptive prompts",
            "Multiple strategies",
            "Quality analysis",
            "Context optimization",
            "Domain expertise"
        ));
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Health check del servicio de prompt engineering");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "HEALTHY");
        response.put("service", "Dynamic Prompt Engineering Service");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test del servicio con ejemplos predefinidos
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testService() {
        logger.info("Ejecutando test del servicio de prompt engineering");
        
        try {
            // Crear ejemplos de prueba
            List<EmbeddingDocument> testExamples = createTestExamples();
            
            // Crear request de prueba
            IntentClassificationRequest testRequest = new IntentClassificationRequest();
            testRequest.setText("¿qué tiempo hace en Madrid?");
            testRequest.setSessionId("test-session-123");
            testRequest.setTimestamp(LocalDateTime.now());
            
            // Construir prompt
            String prompt = promptEngineeringService.buildDynamicPrompt(testRequest, testExamples);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("test_text", "¿qué tiempo hace en Madrid?");
            response.put("examples_count", testExamples.size());
            response.put("prompt_length", prompt.length());
            response.put("prompt_preview", prompt.substring(0, Math.min(200, prompt.length())) + "...");
            response.put("timestamp", LocalDateTime.now());
            
            logger.info("Test del servicio completado exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error en test del servicio: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Crea ejemplos de prueba para el test
     */
    private List<EmbeddingDocument> createTestExamples() {
        List<EmbeddingDocument> examples = new ArrayList<>();
        
        // Ejemplo 1: Consulta de tiempo
        EmbeddingDocument example1 = new EmbeddingDocument();
        example1.setId("test-1");
        example1.setContent("¿qué tiempo hace?");
        example1.setIntent("consultar_tiempo");
        example1.setSimilarity(0.85);
        examples.add(example1);
        
        // Ejemplo 2: Consulta de tiempo con ubicación
        EmbeddingDocument example2 = new EmbeddingDocument();
        example2.setId("test-2");
        example2.setContent("dime el clima de Barcelona");
        example2.setIntent("consultar_tiempo");
        example2.setSimilarity(0.78);
        examples.add(example2);
        
        // Ejemplo 3: Encender luz
        EmbeddingDocument example3 = new EmbeddingDocument();
        example3.setId("test-3");
        example3.setContent("enciende la luz del salón");
        example3.setIntent("encender_luz");
        example3.setSimilarity(0.72);
        examples.add(example3);
        
        return examples;
    }

    /**
     * Clase interna para request de construcción de prompt
     */
    public static class PromptBuildRequest {
        private String text;
        private String sessionId;
        private String userId;
        private Map<String, Object> contextMetadata;
        private List<EmbeddingDocument> examples;

        // Constructores
        public PromptBuildRequest() {}

        public PromptBuildRequest(String text, List<EmbeddingDocument> examples) {
            this.text = text;
            this.examples = examples;
        }

        // Getters y setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public Map<String, Object> getContextMetadata() { return contextMetadata; }
        public void setContextMetadata(Map<String, Object> contextMetadata) { this.contextMetadata = contextMetadata; }

        public List<EmbeddingDocument> getExamples() { return examples; }
        public void setExamples(List<EmbeddingDocument> examples) { this.examples = examples; }
    }
} 