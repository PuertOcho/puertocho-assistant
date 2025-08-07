package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.ProgressTracker;
import com.intentmanagerms.application.services.TaskStatusManager;
import com.intentmanagerms.application.services.CompletionValidator;
import com.intentmanagerms.application.services.ProgressNotifier;
import com.intentmanagerms.domain.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para el sistema de seguimiento de progreso
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@RestController
@RequestMapping("/api/v1/progress-tracker")
@Tag(name = "Progress Tracker", description = "Sistema de seguimiento de progreso de subtareas")
public class ProgressTrackerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressTrackerController.class);
    
    @Autowired
    private ProgressTracker progressTracker;
    
    @Autowired
    private TaskStatusManager taskStatusManager;
    
    @Autowired
    private CompletionValidator completionValidator;
    
    @Autowired
    private ProgressNotifier progressNotifier;
    
    @GetMapping("/health")
    @Operation(summary = "Health check del sistema de progreso", description = "Verifica el estado del sistema de seguimiento de progreso")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "Progress Tracker",
                "timestamp", System.currentTimeMillis(),
                "version", "1.0.0"
            );
            
            logger.info("Health check del sistema de progreso: OK");
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error en health check: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Estadísticas del sistema", description = "Obtiene estadísticas detalladas del sistema de progreso")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = progressTracker.getSystemStatistics();
            stats.put("task_status_statistics", taskStatusManager.getTaskStatusStatistics());
            stats.put("validation_statistics", completionValidator.getValidationStatistics());
            stats.put("notification_statistics", progressNotifier.getNotificationStatistics());
            
            logger.info("Estadísticas del sistema de progreso obtenidas");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/start-tracking")
    @Operation(summary = "Iniciar seguimiento", description = "Inicia el seguimiento de progreso para una sesión de ejecución")
    public ResponseEntity<ProgressTrackingResult> startTracking(
            @Parameter(description = "Solicitud de seguimiento de progreso") 
            @RequestBody ProgressTrackingRequest request) {
        
        try {
            logger.info("Iniciando seguimiento de progreso para sesión: {}", request.getExecutionSessionId());
            
            ProgressTrackingResult result = progressTracker.startTracking(request);
            
            if (result.isSuccess()) {
                logger.info("Seguimiento iniciado exitosamente: {}", result.getTrackerId());
                return ResponseEntity.ok(result);
            } else {
                logger.warn("Error al iniciar seguimiento: {}", result.getErrorMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            logger.error("Error al iniciar seguimiento: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResult(request.getExecutionSessionId(), e.getMessage()));
        }
    }
    
    @PostMapping("/update-progress")
    @Operation(summary = "Actualizar progreso", description = "Actualiza el progreso de una subtarea específica")
    public ResponseEntity<Map<String, Object>> updateProgress(
            @Parameter(description = "ID del tracker") @RequestParam String trackerId,
            @Parameter(description = "ID de la subtarea") @RequestParam String subtaskId,
            @Parameter(description = "Estado de la subtarea") @RequestParam SubtaskProgress.SubtaskStatus status,
            @Parameter(description = "Porcentaje de progreso") @RequestParam(defaultValue = "0.0") double progressPercentage,
            @Parameter(description = "Mensaje de error (opcional)") @RequestParam(required = false) String errorMessage) {
        
        try {
            logger.debug("Actualizando progreso: {} - {} - {} - {}%", trackerId, subtaskId, status, progressPercentage);
            
            progressTracker.updateSubtaskProgress(trackerId, subtaskId, status, progressPercentage, null, errorMessage);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "tracker_id", trackerId,
                "subtask_id", subtaskId,
                "status", status.getValue(),
                "progress_percentage", progressPercentage
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al actualizar progreso: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/status/{trackerId}")
    @Operation(summary = "Obtener estado de progreso", description = "Obtiene el estado actual del progreso de un tracker")
    public ResponseEntity<ProgressTrackingResult> getProgressStatus(
            @Parameter(description = "ID del tracker") @PathVariable String trackerId) {
        
        try {
            logger.debug("Obteniendo estado de progreso para tracker: {}", trackerId);
            
            ProgressTrackingResult result = progressTracker.getProgressStatus(trackerId);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error al obtener estado de progreso: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResult(trackerId, e.getMessage()));
        }
    }
    
    @PostMapping("/cancel/{trackerId}")
    @Operation(summary = "Cancelar seguimiento", description = "Cancela el seguimiento de progreso de un tracker")
    public ResponseEntity<Map<String, Object>> cancelTracking(
            @Parameter(description = "ID del tracker") @PathVariable String trackerId) {
        
        try {
            logger.info("Cancelando seguimiento de progreso: {}", trackerId);
            
            progressTracker.cancelTracking(trackerId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "tracker_id", trackerId,
                "message", "Seguimiento cancelado exitosamente"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al cancelar seguimiento: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/cleanup")
    @Operation(summary = "Limpieza de trackers", description = "Limpia trackers expirados del sistema")
    public ResponseEntity<Map<String, Object>> cleanupExpiredTrackers() {
        try {
            logger.info("Iniciando limpieza de trackers expirados");
            
            progressTracker.cleanupExpiredTrackers();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Limpieza de trackers completada"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error en limpieza de trackers: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/notifications/{trackerId}")
    @Operation(summary = "Obtener notificaciones", description = "Obtiene las notificaciones de un tracker específico")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @Parameter(description = "ID del tracker") @PathVariable String trackerId) {
        
        try {
            logger.debug("Obteniendo notificaciones para tracker: {}", trackerId);
            
            var notifications = progressNotifier.getNotifications(trackerId);
            
            Map<String, Object> response = Map.of(
                "tracker_id", trackerId,
                "notifications", notifications,
                "count", notifications.size()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al obtener notificaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/notifications/{trackerId}")
    @Operation(summary = "Limpiar notificaciones", description = "Limpia las notificaciones de un tracker específico")
    public ResponseEntity<Map<String, Object>> clearNotifications(
            @Parameter(description = "ID del tracker") @PathVariable String trackerId) {
        
        try {
            logger.info("Limpiando notificaciones para tracker: {}", trackerId);
            
            progressNotifier.clearNotifications(trackerId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "tracker_id", trackerId,
                "message", "Notificaciones limpiadas exitosamente"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error al limpiar notificaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/test")
    @Operation(summary = "Test automatizado", description = "Ejecuta un test automatizado del sistema de progreso")
    public ResponseEntity<Map<String, Object>> runTest() {
        try {
            logger.info("Ejecutando test automatizado del sistema de progreso");
            
            // Crear solicitud de prueba
            ProgressTrackingRequest testRequest = createTestRequest();
            
            // Iniciar seguimiento
            ProgressTrackingResult startResult = progressTracker.startTracking(testRequest);
            
            if (!startResult.isSuccess()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Error al iniciar seguimiento de prueba"));
            }
            
            String trackerId = startResult.getTrackerId();
            
            // Simular actualizaciones de progreso
            progressTracker.updateSubtaskProgress(trackerId, "task_001", SubtaskProgress.SubtaskStatus.IN_PROGRESS, 25.0, null, null);
            progressTracker.updateSubtaskProgress(trackerId, "task_001", SubtaskProgress.SubtaskStatus.COMPLETED, 100.0, Map.of("result", "test"), null);
            progressTracker.updateSubtaskProgress(trackerId, "task_002", SubtaskProgress.SubtaskStatus.IN_PROGRESS, 50.0, null, null);
            progressTracker.updateSubtaskProgress(trackerId, "task_002", SubtaskProgress.SubtaskStatus.COMPLETED, 100.0, Map.of("result", "test"), null);
            
            // Obtener estado final
            ProgressTrackingResult finalResult = progressTracker.getProgressStatus(trackerId);
            
            Map<String, Object> testResult = Map.of(
                "success", true,
                "test_name", "Progress Tracker System Test",
                "tracker_id", trackerId,
                "initial_result", startResult,
                "final_result", finalResult,
                "completion_status", finalResult.getCompletionStatus(),
                "statistics", finalResult.getStatistics()
            );
            
            logger.info("Test automatizado completado exitosamente");
            return ResponseEntity.ok(testResult);
            
        } catch (Exception e) {
            logger.error("Error en test automatizado: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // Métodos privados de ayuda
    
    private ProgressTrackingRequest createTestRequest() {
        ProgressTrackingRequest request = new ProgressTrackingRequest();
        request.setExecutionSessionId("test_exec_" + System.currentTimeMillis());
        request.setConversationSessionId("test_conv_" + System.currentTimeMillis());
        
        // Crear subtareas de prueba
        var subtask1 = new Subtask();
        subtask1.setSubtaskId("task_001");
        subtask1.setAction("consultar_tiempo");
        subtask1.setDescription("Consultar tiempo de Madrid");
        subtask1.setEntities(Map.of("ubicacion", "Madrid"));
        
        var subtask2 = new Subtask();
        subtask2.setSubtaskId("task_002");
        subtask2.setAction("programar_alarma");
        subtask2.setDescription("Programar alarma para mañana");
        subtask2.setEntities(Map.of("fecha", "mañana"));
        
        request.setSubtasks(java.util.List.of(subtask1, subtask2));
        
        return request;
    }
    
    private ProgressTrackingResult createErrorResult(String trackerId, String errorMessage) {
        ProgressTrackingResult result = new ProgressTrackingResult();
        result.setTrackerId(trackerId);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setProcessingTimeMs(0);
        return result;
    }
}
