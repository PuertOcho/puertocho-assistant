package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.SlotFillingService;
import com.intentmanagerms.domain.model.SlotFillingRequest;
import com.intentmanagerms.domain.model.SlotFillingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el servicio de slot-filling automático.
 * Proporciona endpoints para procesamiento, validación y estadísticas de slot-filling.
 */
@RestController
@RequestMapping("/api/v1/slot-filling")
public class SlotFillingController {

    private static final Logger logger = LoggerFactory.getLogger(SlotFillingController.class);

    @Autowired
    private SlotFillingService slotFillingService;

    /**
     * Procesa slot-filling para una intención específica
     */
    @PostMapping("/process")
    public ResponseEntity<SlotFillingResult> processSlotFilling(@RequestBody SlotFillingRequest request) {
        try {
            logger.info("Procesando slot-filling para intent '{}' con mensaje: {}", 
                       request.getIntentId(), request.getUserMessage());
            
            SlotFillingResult result = slotFillingService.processSlotFilling(request);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando slot-filling: {}", e.getMessage(), e);
            
            SlotFillingResult errorResult = new SlotFillingResult(false);
            errorResult.setErrorMessage("Error interno: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * Extrae un slot específico desde un mensaje
     */
    @PostMapping("/extract-slot")
    public ResponseEntity<SlotFillingResult> extractSpecificSlot(@RequestBody ExtractSlotRequest request) {
        
        try {
            logger.info("Extrayendo slot '{}' para intent '{}' desde: {}", request.getSlotName(), request.getIntentId(), request.getUserMessage());
            
            Map<String, Object> currentSlots = new HashMap<>();
            Map<String, Object> conversationContext = request.getContext() != null ? request.getContext() : new HashMap<>();
            
            SlotFillingResult result = slotFillingService.extractSpecificSlot(
                request.getIntentId(), request.getSlotName(), request.getUserMessage(), currentSlots, conversationContext
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error extrayendo slot '{}': {}", request.getSlotName(), e.getMessage(), e);
            
            SlotFillingResult errorResult = new SlotFillingResult(false);
            errorResult.setErrorMessage("Error extrayendo slot: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * Verifica si todos los slots requeridos están completos
     */
    @PostMapping("/validate-completeness")
    public ResponseEntity<Map<String, Object>> validateCompleteness(@RequestBody ValidateCompletenessRequest request) {
        
        try {
            logger.info("Validando completitud de slots para intent '{}': {}", request.getIntentId(), request.getSlots());
            
            boolean isComplete = slotFillingService.areRequiredSlotsComplete(request.getIntentId(), request.getSlots());
            
            Map<String, Object> response = new HashMap<>();
            response.put("intent_id", request.getIntentId());
            response.put("slots_complete", isComplete);
            response.put("provided_slots", request.getSlots());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validando completitud de slots: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error validando slots: " + e.getMessage());
            errorResponse.put("intent_id", request.getIntentId());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Obtiene la siguiente pregunta a hacer para completar slots
     */
    @PostMapping("/next-question")
    public ResponseEntity<Map<String, Object>> getNextQuestion(@RequestBody NextQuestionRequest request) {
        
        try {
            logger.info("Obteniendo siguiente pregunta para intent '{}' con slots: {}", request.getIntentId(), request.getCurrentSlots());
            
            Map<String, Object> conversationContext = request.getContext() != null ? request.getContext() : new HashMap<>();
            
            String nextQuestion = slotFillingService.getNextQuestion(
                request.getIntentId(), request.getCurrentSlots(), conversationContext, request.getLastUserMessage()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("intent_id", request.getIntentId());
            response.put("next_question", nextQuestion);
            response.put("has_question", nextQuestion != null);
            response.put("current_slots", request.getCurrentSlots());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error obteniendo siguiente pregunta: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error obteniendo pregunta: " + e.getMessage());
            errorResponse.put("intent_id", request.getIntentId());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Obtiene estadísticas del servicio de slot-filling
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            logger.debug("Obteniendo estadísticas de slot-filling");
            
            Map<String, Object> stats = slotFillingService.getStatistics();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas de slot-filling: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error obteniendo estadísticas: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check del servicio de slot-filling
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean isHealthy = slotFillingService.isHealthy();
            
            Map<String, Object> response = new HashMap<>();
            response.put("service", "slot-filling");
            response.put("status", isHealthy ? "healthy" : "unhealthy");
            response.put("timestamp", System.currentTimeMillis());
            
            if (isHealthy) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error en health check de slot-filling: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "slot-filling");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Test básico del slot-filling
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSlotFilling() {
        try {
            logger.info("Ejecutando test básico de slot-filling");
            
            // Test simple con intención de consultar tiempo
            SlotFillingRequest testRequest = new SlotFillingRequest(
                "consultar_tiempo", 
                "¿Qué tiempo hace en Madrid?", 
                "test-session"
            );
            testRequest.setCurrentSlots(new HashMap<>());
            testRequest.setConversationContext(new HashMap<>());
            
            SlotFillingResult result = slotFillingService.processSlotFilling(testRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("test_status", result.isSuccess() ? "passed" : "failed");
            response.put("test_result", result);
            response.put("test_intent", "consultar_tiempo");
            response.put("test_message", "¿Qué tiempo hace en Madrid?");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error en test de slot-filling: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("test_status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Clase de request para extract-slot endpoint
     */
    public static class ExtractSlotRequest {
        private String intentId;
        private String slotName;
        private String userMessage;
        private Map<String, Object> context;

        // Getters y Setters
        public String getIntentId() { return intentId; }
        public void setIntentId(String intentId) { this.intentId = intentId; }

        public String getSlotName() { return slotName; }
        public void setSlotName(String slotName) { this.slotName = slotName; }

        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    /**
     * Clase de request para validate-completeness endpoint
     */
    public static class ValidateCompletenessRequest {
        private String intentId;
        private Map<String, Object> slots;

        // Getters y Setters
        public String getIntentId() { return intentId; }
        public void setIntentId(String intentId) { this.intentId = intentId; }

        public Map<String, Object> getSlots() { return slots; }
        public void setSlots(Map<String, Object> slots) { this.slots = slots; }
    }

    /**
     * Clase de request para next-question endpoint
     */
    public static class NextQuestionRequest {
        private String intentId;
        private String lastUserMessage;
        private Map<String, Object> currentSlots;
        private Map<String, Object> context;

        // Getters y Setters
        public String getIntentId() { return intentId; }
        public void setIntentId(String intentId) { this.intentId = intentId; }

        public String getLastUserMessage() { return lastUserMessage; }
        public void setLastUserMessage(String lastUserMessage) { this.lastUserMessage = lastUserMessage; }

        public Map<String, Object> getCurrentSlots() { return currentSlots; }
        public void setCurrentSlots(Map<String, Object> currentSlots) { this.currentSlots = currentSlots; }

        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }
}