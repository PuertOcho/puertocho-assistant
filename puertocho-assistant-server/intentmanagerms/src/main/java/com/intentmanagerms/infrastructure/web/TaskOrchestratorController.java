package com.intentmanagerms.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intentmanagerms.application.services.TaskOrchestrator;
import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el TaskOrchestrator.
 * Proporciona endpoints para la gestión y ejecución de tareas.
 */
@RestController
@RequestMapping("/api/v1/task-orchestrator")
@CrossOrigin(origins = "*")
public class TaskOrchestratorController {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskOrchestratorController.class);
    
    @Autowired
    private TaskOrchestrator taskOrchestrator;
    
    /**
     * Health check del TaskOrchestrator.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Health check solicitado para TaskOrchestrator");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "TaskOrchestrator");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene estadísticas del TaskOrchestrator.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.debug("Estadísticas solicitadas para TaskOrchestrator");
        
        try {
            Map<String, Object> statistics = taskOrchestrator.getActiveExecutionsStatistics();
            statistics.put("service", "TaskOrchestrator");
            statistics.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error obteniendo estadísticas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Ejecuta una lista de subtareas con orquestación inteligente.
     */
    @PostMapping("/execute")
    public ResponseEntity<TaskExecutionResult> executeSubtasks(@RequestBody TaskExecutionRequest request) {
        logger.info("Ejecución de subtareas solicitada: {} subtareas", 
                   request.getSubtasks() != null ? request.getSubtasks().size() : 0);
        
        try {
            // Validar request
            validateExecutionRequest(request);
            
            // Debug: Log de las subtareas recibidas
            logger.info("Subtareas recibidas: {}", request.getSubtasks());
            if (request.getSubtasks() != null) {
                for (Subtask subtask : request.getSubtasks()) {
                    logger.info("Subtask: id={}, action={}, description={}", 
                               subtask.getSubtaskId(), subtask.getAction(), subtask.getDescription());
                }
            }
            
            // Ejecutar subtareas
            TaskExecutionResult result = taskOrchestrator.executeSubtasks(
                request.getSubtasks(), 
                request.getConversationSessionId()
            );
            
            logger.info("Ejecución completada: {} exitosas, {} fallidas", 
                       result.getSuccessfulTasks(), result.getFailedTasks());
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Request inválido: {}", e.getMessage());
            TaskExecutionResult errorResult = createErrorResult(e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
            
        } catch (Exception e) {
            logger.error("Error en ejecución de subtareas: {}", e.getMessage(), e);
            TaskExecutionResult errorResult = createErrorResult("Error interno: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * Obtiene el estado de una sesión de ejecución activa.
     */
    @GetMapping("/session/{executionId}")
    public ResponseEntity<TaskExecutionSession> getActiveSession(@PathVariable String executionId) {
        logger.debug("Estado de sesión solicitado: {}", executionId);
        
        try {
            TaskExecutionSession session = taskOrchestrator.getActiveSession(executionId);
            
            if (session != null) {
                return ResponseEntity.ok(session);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error obteniendo sesión {}: {}", executionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Cancela una ejecución en progreso.
     */
    @PostMapping("/cancel/{executionId}")
    public ResponseEntity<Map<String, Object>> cancelExecution(@PathVariable String executionId) {
        logger.info("Cancelación de ejecución solicitada: {}", executionId);
        
        try {
            boolean cancelled = taskOrchestrator.cancelExecution(executionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("execution_id", executionId);
            response.put("cancelled", cancelled);
            response.put("message", cancelled ? "Ejecución cancelada exitosamente" : "Ejecución no encontrada");
            
            if (cancelled) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error cancelando ejecución {}: {}", executionId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error cancelando ejecución: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Ejecuta una descomposición y orquestación completa.
     */
    @PostMapping("/decompose-and-execute")
    public ResponseEntity<Map<String, Object>> decomposeAndExecute(@RequestBody DecomposeAndExecuteRequest request) {
        logger.info("Descomposición y ejecución solicitada para: {}", request.getUserMessage());
        
        try {
            // Validar request
            validateDecomposeRequest(request);
            
            // Simular descomposición (en una implementación real, se llamaría al DynamicSubtaskDecomposer)
            List<Subtask> subtasks = simulateDecomposition(request.getUserMessage());
            
            // Ejecutar subtareas
            TaskExecutionResult result = taskOrchestrator.executeSubtasks(subtasks, request.getConversationSessionId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("user_message", request.getUserMessage());
            response.put("decomposed_subtasks", subtasks.size());
            response.put("execution_result", result);
            response.put("success", result.isSuccessful());
            
            logger.info("Descomposición y ejecución completada: {} subtareas, {} exitosas", 
                       subtasks.size(), result.getSuccessfulTasks());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error en descomposición y ejecución: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error en descomposición y ejecución: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Test automatizado del TaskOrchestrator.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testOrchestrator() {
        logger.info("Test automatizado solicitado para TaskOrchestrator");
        
        try {
            // Crear subtareas de prueba
            List<Subtask> testSubtasks = createTestSubtasks();
            
            // Ejecutar subtareas
            TaskExecutionResult result = taskOrchestrator.executeSubtasks(testSubtasks, "test_session");
            
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("test_name", "TaskOrchestrator Integration Test");
            testResult.put("subtasks_created", testSubtasks.size());
            testResult.put("execution_result", result);
            testResult.put("success", result.isSuccessful());
            testResult.put("success_rate", result.getSuccessRate());
            testResult.put("execution_time_ms", result.getTotalExecutionTimeMs());
            
            logger.info("Test completado: {} exitosas, {} fallidas, tiempo: {}ms", 
                       result.getSuccessfulTasks(), result.getFailedTasks(), result.getTotalExecutionTimeMs());
            
            return ResponseEntity.ok(testResult);
            
        } catch (Exception e) {
            logger.error("Error en test: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error en test: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Valida una solicitud de ejecución.
     */
    private void validateExecutionRequest(TaskExecutionRequest request) {
        logger.info("Validando request: {}", request);
        
        if (request == null) {
            throw new IllegalArgumentException("Request no puede ser null");
        }
        
        logger.info("Subtasks: {}", request.getSubtasks());
        logger.info("ConversationSessionId: {}", request.getConversationSessionId());
        
        if (request.getSubtasks() == null || request.getSubtasks().isEmpty()) {
            throw new IllegalArgumentException("La lista de subtareas no puede estar vacía");
        }
        
        if (request.getConversationSessionId() == null || request.getConversationSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID de sesión de conversación es requerido");
        }
    }
    
    /**
     * Valida una solicitud de descomposición.
     */
    private void validateDecomposeRequest(DecomposeAndExecuteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request no puede ser null");
        }
        
        if (request.getUserMessage() == null || request.getUserMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Mensaje de usuario es requerido");
        }
        
        if (request.getConversationSessionId() == null || request.getConversationSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID de sesión de conversación es requerido");
        }
    }
    
    /**
     * Crea un resultado de error.
     */
    private TaskExecutionResult createErrorResult(String errorMessage) {
        return TaskExecutionResult.builder()
                .executionId("error_" + System.currentTimeMillis())
                .totalTasks(0)
                .successfulTasks(0)
                .failedTasks(0)
                .allSuccessful(false)
                .totalExecutionTimeMs(0L)
                .errorMessage(errorMessage)
                .results(List.of())
                .build();
    }
    
    /**
     * Simula descomposición de un mensaje de usuario.
     */
    private List<Subtask> simulateDecomposition(String userMessage) {
        List<Subtask> subtasks = new java.util.ArrayList<>();
        
        // Simular descomposición basada en el mensaje
        if (userMessage.toLowerCase().contains("tiempo") && userMessage.toLowerCase().contains("madrid")) {
            Subtask weatherTask = new Subtask("consultar_tiempo", "Consultar tiempo en Madrid");
            weatherTask.setSubtaskId("task_001");
            weatherTask.setEntities(Map.of("ubicacion", "Madrid"));
            weatherTask.setPriority("high");
            subtasks.add(weatherTask);
        }
        
        if (userMessage.toLowerCase().contains("alarma") && userMessage.toLowerCase().contains("llover")) {
            Subtask alarmTask = new Subtask("programar_alarma_condicional", "Programar alarma si llueve");
            alarmTask.setSubtaskId("task_002");
            alarmTask.setEntities(Map.of("condicion", "si_llueve"));
            alarmTask.setDependencies(List.of("task_001"));
            alarmTask.setPriority("medium");
            subtasks.add(alarmTask);
        }
        
        if (userMessage.toLowerCase().contains("github") && userMessage.toLowerCase().contains("issue")) {
            Subtask githubTask = new Subtask("crear_github_issue", "Crear issue en GitHub");
            githubTask.setSubtaskId("task_003");
            githubTask.setEntities(Map.of("titulo", "Bug report", "descripcion", "Issue reportado por usuario"));
            githubTask.setPriority("low");
            subtasks.add(githubTask);
        }
        
        // Si no se detectó ninguna acción específica, crear una genérica
        if (subtasks.isEmpty()) {
            Subtask genericTask = new Subtask("accion_generica", "Procesar petición genérica");
            genericTask.setSubtaskId("task_001");
            genericTask.setEntities(Map.of("mensaje", userMessage));
            genericTask.setPriority("medium");
            subtasks.add(genericTask);
        }
        
        return subtasks;
    }
    
    /**
     * Crea subtareas de prueba.
     */
    private List<Subtask> createTestSubtasks() {
        List<Subtask> testSubtasks = new java.util.ArrayList<>();
        
        // Subtarea 1: Consultar tiempo
        Subtask weatherTask = new Subtask("consultar_tiempo", "Consultar tiempo en Barcelona");
        weatherTask.setSubtaskId("test_001");
        weatherTask.setEntities(Map.of("ubicacion", "Barcelona"));
        weatherTask.setPriority("high");
        testSubtasks.add(weatherTask);
        
        // Subtarea 2: Programar alarma (depende de la primera)
        Subtask alarmTask = new Subtask("programar_alarma", "Programar alarma para mañana");
        alarmTask.setSubtaskId("test_002");
        alarmTask.setEntities(Map.of("hora", "08:00", "mensaje", "Despertar"));
        alarmTask.setDependencies(List.of("test_001"));
        alarmTask.setPriority("medium");
        testSubtasks.add(alarmTask);
        
        // Subtarea 3: Crear issue (independiente)
        Subtask githubTask = new Subtask("crear_github_issue", "Crear issue de prueba");
        githubTask.setSubtaskId("test_003");
        githubTask.setEntities(Map.of("titulo", "Test Issue", "descripcion", "Issue de prueba"));
        githubTask.setPriority("low");
        testSubtasks.add(githubTask);
        
        return testSubtasks;
    }
    
    /**
     * Modelo de request para ejecución de subtareas.
     */
    public static class TaskExecutionRequest {
        @JsonProperty("subtasks")
        private List<Subtask> subtasks;
        
        @JsonProperty("conversation_session_id")
        private String conversationSessionId;
        
        // Getters y Setters
        public List<Subtask> getSubtasks() {
            return subtasks;
        }
        
        public void setSubtasks(List<Subtask> subtasks) {
            this.subtasks = subtasks;
        }
        
        public String getConversationSessionId() {
            return conversationSessionId;
        }
        
        public void setConversationSessionId(String conversationSessionId) {
            this.conversationSessionId = conversationSessionId;
        }
    }
    
    /**
     * Modelo de request para descomposición y ejecución.
     */
    public static class DecomposeAndExecuteRequest {
        @JsonProperty("user_message")
        private String userMessage;
        
        @JsonProperty("conversation_session_id")
        private String conversationSessionId;
        
        // Getters y Setters
        public String getUserMessage() {
            return userMessage;
        }
        
        public void setUserMessage(String userMessage) {
            this.userMessage = userMessage;
        }
        
        public String getConversationSessionId() {
            return conversationSessionId;
        }
        
        public void setConversationSessionId(String conversationSessionId) {
            this.conversationSessionId = conversationSessionId;
        }
    }
} 