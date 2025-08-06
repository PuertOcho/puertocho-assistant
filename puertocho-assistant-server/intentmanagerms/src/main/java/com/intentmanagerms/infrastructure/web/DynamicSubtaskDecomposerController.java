package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.DynamicSubtaskDecomposer;
import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para el Dynamic Subtask Decomposer.
 * Proporciona endpoints para descomponer peticiones complejas en subtareas ejecutables.
 */
@RestController
@RequestMapping("/api/v1/subtask-decomposer")
@CrossOrigin(origins = "*")
public class DynamicSubtaskDecomposerController {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicSubtaskDecomposerController.class);
    
    @Autowired
    private DynamicSubtaskDecomposer dynamicSubtaskDecomposer;
    
    /**
     * Health check del servicio de descomposición de subtareas.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        logger.debug("Health check solicitado");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Dynamic Subtask Decomposer");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        response.put("description", "Servicio de descomposición dinámica de subtareas");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene estadísticas del servicio de descomposición.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.debug("Estadísticas solicitadas");
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("service", "Dynamic Subtask Decomposer");
        statistics.put("status", "operational");
        statistics.put("uptime", "100%");
        statistics.put("total_requests", 0); // En implementación real, obtener de métricas
        statistics.put("successful_decompositions", 0);
        statistics.put("failed_decompositions", 0);
        statistics.put("average_processing_time_ms", 0);
        statistics.put("max_subtasks_per_request", 10);
        statistics.put("supported_actions", List.of(
            "consultar_tiempo", "programar_alarma", "encender_luz", "reproducir_musica",
            "crear_issue", "actualizar_estado", "buscar_informacion", "autenticar_usuario"
        ));
        statistics.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * Descompone una petición compleja en subtareas ejecutables.
     */
    @PostMapping("/decompose")
    public ResponseEntity<SubtaskDecompositionResult> decomposeRequest(
            @RequestBody SubtaskDecompositionRequest request) {
        
        logger.info("Solicitud de descomposición recibida: {}", request.getUserMessage());
        
        try {
            // Validar entrada básica
            if (request.getUserMessage() == null || request.getUserMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Generar ID de solicitud si no se proporciona
            if (request.getRequestId() == null) {
                request.setRequestId("req_" + System.currentTimeMillis() + "_" + 
                        java.util.UUID.randomUUID().toString().substring(0, 8));
            }
            
            // Ejecutar descomposición
            SubtaskDecompositionResult result = dynamicSubtaskDecomposer.decomposeRequest(request);
            
            logger.info("Descomposición completada: {} subtareas generadas", result.getTotalSubtasks());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error durante la descomposición", e);
            
            // Crear resultado de error
            SubtaskDecompositionResult errorResult = new SubtaskDecompositionResult(
                    request.getRequestId(), 
                    request.getConversationSessionId(), 
                    request.getUserMessage()
            );
            errorResult.setSubtasks(List.of());
            errorResult.setDecompositionConfidence(0.0);
            errorResult.setProcessingTimeMs(0L);
            
            Map<String, Object> errorMetadata = new HashMap<>();
            errorMetadata.put("error", e.getMessage());
            errorMetadata.put("error_type", e.getClass().getSimpleName());
            errorResult.setMetadata(errorMetadata);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * Descomposición asíncrona de una petición compleja.
     */
    @PostMapping("/decompose-async")
    public ResponseEntity<Map<String, Object>> decomposeRequestAsync(
            @RequestBody SubtaskDecompositionRequest request) {
        
        logger.info("Solicitud de descomposición asíncrona recibida: {}", request.getUserMessage());
        
        try {
            // Validar entrada básica
            if (request.getUserMessage() == null || request.getUserMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Generar ID de solicitud si no se proporciona
            if (request.getRequestId() == null) {
                request.setRequestId("req_" + System.currentTimeMillis() + "_" + 
                        java.util.UUID.randomUUID().toString().substring(0, 8));
            }
            
            // Ejecutar descomposición asíncrona
            CompletableFuture<SubtaskDecompositionResult> future = 
                    dynamicSubtaskDecomposer.decomposeRequestAsync(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("request_id", request.getRequestId());
            response.put("status", "processing");
            response.put("message", "Descomposición iniciada asíncronamente");
            response.put("timestamp", LocalDateTime.now());
            response.put("estimated_completion_time_ms", 5000); // Estimación
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            logger.error("Error iniciando descomposición asíncrona", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Descomposición simple con solo el mensaje del usuario.
     */
    @PostMapping("/decompose-simple")
    public ResponseEntity<SubtaskDecompositionResult> decomposeSimple(
            @RequestBody Map<String, String> simpleRequest) {
        
        String userMessage = simpleRequest.get("user_message");
        String sessionId = simpleRequest.get("session_id");
        
        logger.info("Solicitud de descomposición simple recibida: {}", userMessage);
        
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Crear solicitud completa
        SubtaskDecompositionRequest request = new SubtaskDecompositionRequest(userMessage, sessionId);
        
        return decomposeRequest(request);
    }
    
    /**
     * Valida una solicitud de descomposición sin ejecutarla.
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateRequest(
            @RequestBody SubtaskDecompositionRequest request) {
        
        logger.debug("Validación de solicitud solicitada");
        
        Map<String, Object> validationResult = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validar mensaje del usuario
        if (request.getUserMessage() == null || request.getUserMessage().trim().isEmpty()) {
            errors.add("El mensaje del usuario no puede estar vacío");
        } else if (request.getUserMessage().length() > 1000) {
            warnings.add("El mensaje del usuario es muy largo");
        }
        
        // Validar ID de sesión
        if (request.getConversationSessionId() == null || request.getConversationSessionId().trim().isEmpty()) {
            warnings.add("Se recomienda proporcionar un ID de sesión de conversación");
        }
        
        // Validar configuración
        if (request.getMaxSubtasks() != null && request.getMaxSubtasks() > 20) {
            warnings.add("El número máximo de subtareas es muy alto");
        }
        
        if (request.getConfidenceThreshold() != null && 
            (request.getConfidenceThreshold() < 0.0 || request.getConfidenceThreshold() > 1.0)) {
            errors.add("El umbral de confianza debe estar entre 0.0 y 1.0");
        }
        
        validationResult.put("valid", errors.isEmpty());
        validationResult.put("errors", errors);
        validationResult.put("warnings", warnings);
        validationResult.put("timestamp", LocalDateTime.now());
        
        if (errors.isEmpty()) {
            return ResponseEntity.ok(validationResult);
        } else {
            return ResponseEntity.badRequest().body(validationResult);
        }
    }
    
    /**
     * Obtiene información sobre las acciones MCP disponibles.
     */
    @GetMapping("/available-actions")
    public ResponseEntity<Map<String, Object>> getAvailableActions() {
        logger.debug("Acciones disponibles solicitadas");
        
        Map<String, Object> response = new HashMap<>();
        response.put("available_actions", List.of(
            "consultar_tiempo",
            "programar_alarma", 
            "encender_luz",
            "reproducir_musica",
            "crear_issue",
            "actualizar_estado",
            "buscar_informacion",
            "autenticar_usuario",
            "verificar_permisos",
            "obtener_datos",
            "procesar_datos",
            "generar_reporte",
            "enviar_notificacion",
            "crear_resumen",
            "asignar_issue",
            "notificar_equipo",
            "ajustar_temperatura",
            "programar_alarma_condicional",
            "crear_github_issue",
            "actualizar_taiga_story",
            "ayuda"
        ));
        response.put("total_actions", 21);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene información sobre una acción específica.
     */
    @GetMapping("/actions/{actionName}")
    public ResponseEntity<Map<String, Object>> getActionInfo(@PathVariable String actionName) {
        logger.debug("Información de acción solicitada: {}", actionName);
        
        Map<String, Object> actionInfo = new HashMap<>();
        
        // Información de ejemplo para algunas acciones
        switch (actionName) {
            case "consultar_tiempo":
                actionInfo.put("name", "consultar_tiempo");
                actionInfo.put("description", "Consulta información meteorológica de una ubicación");
                actionInfo.put("required_entities", List.of("ubicacion"));
                actionInfo.put("optional_entities", List.of("fecha", "hora"));
                actionInfo.put("estimated_duration_ms", 2000);
                actionInfo.put("priority", "medium");
                break;
                
            case "programar_alarma":
                actionInfo.put("name", "programar_alarma");
                actionInfo.put("description", "Programa una alarma para una fecha/hora específica");
                actionInfo.put("required_entities", List.of("hora", "mensaje"));
                actionInfo.put("optional_entities", List.of("fecha", "repetir"));
                actionInfo.put("estimated_duration_ms", 1000);
                actionInfo.put("priority", "high");
                break;
                
            case "encender_luz":
                actionInfo.put("name", "encender_luz");
                actionInfo.put("description", "Enciende la iluminación en una ubicación específica");
                actionInfo.put("required_entities", List.of("lugar"));
                actionInfo.put("optional_entities", List.of("intensidad", "color"));
                actionInfo.put("estimated_duration_ms", 500);
                actionInfo.put("priority", "medium");
                break;
                
            default:
                actionInfo.put("name", actionName);
                actionInfo.put("description", "Acción MCP genérica");
                actionInfo.put("required_entities", List.of());
                actionInfo.put("optional_entities", List.of());
                actionInfo.put("estimated_duration_ms", 1000);
                actionInfo.put("priority", "low");
        }
        
        actionInfo.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(actionInfo);
    }
    
    /**
     * Ejecuta un test automatizado del servicio.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> runTest() {
        logger.info("Test automatizado solicitado");
        
        Map<String, Object> testResults = new HashMap<>();
        List<Map<String, Object>> testCases = new ArrayList<>();
        
        // Test case 1: Petición simple
        try {
            SubtaskDecompositionRequest request1 = new SubtaskDecompositionRequest(
                    "¿Qué tiempo hace en Madrid?", "test_session_1");
            
            SubtaskDecompositionResult result1 = dynamicSubtaskDecomposer.decomposeRequest(request1);
            
            Map<String, Object> testCase1 = new HashMap<>();
            testCase1.put("test_name", "Petición simple");
            testCase1.put("input", request1.getUserMessage());
            testCase1.put("success", result1.getTotalSubtasks() > 0);
            testCase1.put("subtasks_generated", result1.getTotalSubtasks());
            testCase1.put("processing_time_ms", result1.getProcessingTimeMs());
            testCase1.put("confidence", result1.getDecompositionConfidence());
            
            testCases.add(testCase1);
            
        } catch (Exception e) {
            Map<String, Object> testCase1 = new HashMap<>();
            testCase1.put("test_name", "Petición simple");
            testCase1.put("success", false);
            testCase1.put("error", e.getMessage());
            testCases.add(testCase1);
        }
        
        // Test case 2: Petición compleja
        try {
            SubtaskDecompositionRequest request2 = new SubtaskDecompositionRequest(
                    "Consulta el tiempo de Madrid y programa una alarma si va a llover", "test_session_2");
            
            SubtaskDecompositionResult result2 = dynamicSubtaskDecomposer.decomposeRequest(request2);
            
            Map<String, Object> testCase2 = new HashMap<>();
            testCase2.put("test_name", "Petición compleja");
            testCase2.put("input", request2.getUserMessage());
            testCase2.put("success", result2.getTotalSubtasks() >= 2);
            testCase2.put("subtasks_generated", result2.getTotalSubtasks());
            testCase2.put("processing_time_ms", result2.getProcessingTimeMs());
            testCase2.put("confidence", result2.getDecompositionConfidence());
            testCase2.put("dependencies_detected", result2.getDependenciesDetected());
            
            testCases.add(testCase2);
            
        } catch (Exception e) {
            Map<String, Object> testCase2 = new HashMap<>();
            testCase2.put("test_name", "Petición compleja");
            testCase2.put("success", false);
            testCase2.put("error", e.getMessage());
            testCases.add(testCase2);
        }
        
        // Test case 3: Petición con múltiples acciones
        try {
            SubtaskDecompositionRequest request3 = new SubtaskDecompositionRequest(
                    "Enciende las luces del salón, pon música relajante y ajusta la temperatura a 22°", "test_session_3");
            
            SubtaskDecompositionResult result3 = dynamicSubtaskDecomposer.decomposeRequest(request3);
            
            Map<String, Object> testCase3 = new HashMap<>();
            testCase3.put("test_name", "Múltiples acciones");
            testCase3.put("input", request3.getUserMessage());
            testCase3.put("success", result3.getTotalSubtasks() >= 3);
            testCase3.put("subtasks_generated", result3.getTotalSubtasks());
            testCase3.put("processing_time_ms", result3.getProcessingTimeMs());
            testCase3.put("confidence", result3.getDecompositionConfidence());
            testCase3.put("can_execute_parallel", result3.getCanExecuteParallel());
            
            testCases.add(testCase3);
            
        } catch (Exception e) {
            Map<String, Object> testCase3 = new HashMap<>();
            testCase3.put("test_name", "Múltiples acciones");
            testCase3.put("success", false);
            testCase3.put("error", e.getMessage());
            testCases.add(testCase3);
        }
        
        // Calcular estadísticas del test
        long successfulTests = testCases.stream().filter(tc -> (Boolean) tc.get("success")).count();
        long totalTests = testCases.size();
        
        testResults.put("service", "Dynamic Subtask Decomposer");
        testResults.put("test_timestamp", LocalDateTime.now());
        testResults.put("total_tests", totalTests);
        testResults.put("successful_tests", successfulTests);
        testResults.put("failed_tests", totalTests - successfulTests);
        testResults.put("success_rate", totalTests > 0 ? (double) successfulTests / totalTests : 0.0);
        testResults.put("test_cases", testCases);
        testResults.put("overall_status", successfulTests == totalTests ? "PASSED" : "FAILED");
        
        logger.info("Test completado: {}/{} casos exitosos", successfulTests, totalTests);
        
        return ResponseEntity.ok(testResults);
    }
    
    /**
     * Obtiene ejemplos de peticiones para testing.
     */
    @GetMapping("/examples")
    public ResponseEntity<Map<String, Object>> getExamples() {
        logger.debug("Ejemplos solicitados");
        
        Map<String, Object> examples = new HashMap<>();
        
        examples.put("simple_requests", List.of(
            "¿Qué tiempo hace en Barcelona?",
            "Programa una alarma para las 8:00",
            "Enciende la luz del salón",
            "Pon música relajante"
        ));
        
        examples.put("complex_requests", List.of(
            "Consulta el tiempo de Madrid y programa una alarma si va a llover",
            "Enciende las luces del salón, pon música relajante y ajusta la temperatura a 22°",
            "Crea un issue en GitHub sobre el weather bug y actualiza el estado en Taiga",
            "Busca información sobre el clima de mañana y envíame un resumen por email"
        ));
        
        examples.put("multi_step_requests", List.of(
            "Primero verifica los permisos del usuario, luego obtén los datos del sistema y finalmente genera un reporte",
            "Autentica al usuario, busca información en la base de datos, procesa los resultados y envía una notificación"
        ));
        
        examples.put("conditional_requests", List.of(
            "Si la temperatura es mayor a 25°, enciende el aire acondicionado",
            "Cuando se complete la descarga, notifica al equipo de desarrollo"
        ));
        
        examples.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(examples);
    }
} 