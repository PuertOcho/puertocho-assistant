package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.SmartAssistantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nlu")
public class NluController {
    
    private static final Logger logger = LoggerFactory.getLogger(NluController.class);
    
    private final SmartAssistantService smartAssistantService;
    
    public NluController(SmartAssistantService smartAssistantService) {
        this.smartAssistantService = smartAssistantService;
    }
    
    /**
     * Endpoint para verificar el estado del servicio NLU
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkNluHealth() {
        try {
            boolean isHealthy = smartAssistantService.isNluServiceHealthy();
            
            Map<String, Object> response = Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "service", "NLU",
                "healthy", isHealthy
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verificando estado del servicio NLU: {}", e.getMessage(), e);
            
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "service", "NLU",
                "healthy", false,
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para entrenar el modelo NLU
     */
    @PostMapping("/train")
    public ResponseEntity<Map<String, Object>> trainNluModel() {
        try {
            logger.info("Iniciando entrenamiento del modelo NLU vía API");
            
            boolean success = smartAssistantService.trainNluModel();
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Modelo entrenado exitosamente" : "Error entrenando el modelo",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error entrenando modelo NLU: {}", e.getMessage(), e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error entrenando el modelo: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para probar el análisis de texto con NLU
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeText(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            
            if (text == null || text.trim().isEmpty()) {
                Map<String, Object> response = Map.of(
                    "error", "El campo 'text' es requerido y no puede estar vacío"
                );
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.debug("Analizando texto vía API: '{}'", text);
            
            String response = smartAssistantService.chat(text);
            
            Map<String, Object> result = Map.of(
                "input", text,
                "response", response,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error analizando texto: {}", e.getMessage(), e);
            
            Map<String, Object> response = Map.of(
                "error", "Error analizando el texto: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
} 