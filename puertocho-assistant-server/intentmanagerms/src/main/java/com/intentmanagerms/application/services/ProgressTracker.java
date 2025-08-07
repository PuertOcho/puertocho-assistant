package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio principal para el seguimiento de progreso de subtareas
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@Service
public class ProgressTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressTracker.class);
    
    @Value("${progress.tracker.enable-real-time-tracking:true}")
    private boolean enableRealTimeTracking;
    
    @Value("${progress.tracker.update-interval-ms:1000}")
    private long updateIntervalMs;
    
    @Value("${progress.tracker.enable-notifications:true}")
    private boolean enableNotifications;
    
    @Value("${progress.tracker.enable-completion-validation:true}")
    private boolean enableCompletionValidation;
    
    @Value("${progress.tracker.max-tracking-duration-minutes:30}")
    private int maxTrackingDurationMinutes;
    
    @Value("${progress.tracker.enable-auto-cleanup:true}")
    private boolean enableAutoCleanup;
    
    @Value("${progress.tracker.cleanup-interval-minutes:5}")
    private int cleanupIntervalMinutes;
    
    @Autowired
    private TaskStatusManager taskStatusManager;
    
    @Autowired
    private CompletionValidator completionValidator;
    
    @Autowired
    private ProgressNotifier progressNotifier;
    
    // Almacenamiento de trackers activos
    private final Map<String, com.intentmanagerms.domain.model.ProgressTracker> activeTrackers = new ConcurrentHashMap<>();
    
    // Executor para tareas programadas
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Constructor
    public ProgressTracker() {
        logger.info("ProgressTracker inicializado");
    }
    
    /**
     * Inicia el seguimiento de progreso para una sesión de ejecución
     */
    public ProgressTrackingResult startTracking(ProgressTrackingRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Iniciando seguimiento de progreso para sesión: {}", request.getExecutionSessionId());
            
            // Crear tracker de progreso
            com.intentmanagerms.domain.model.ProgressTracker tracker = createProgressTracker(request);
            
            // Inicializar subtareas
            initializeSubtasks(tracker, request.getSubtasks());
            
            // Almacenar tracker activo
            activeTrackers.put(tracker.getTrackerId(), tracker);
            
            // Iniciar seguimiento en tiempo real si está habilitado
            if (enableRealTimeTracking) {
                startRealTimeTracking(tracker);
            }
            
            // Crear resultado
            ProgressTrackingResult result = createTrackingResult(tracker, startTime);
            
            logger.info("Seguimiento de progreso iniciado exitosamente. Tracker ID: {}", tracker.getTrackerId());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error al iniciar seguimiento de progreso: {}", e.getMessage(), e);
            return createErrorResult(request.getExecutionSessionId(), e.getMessage(), startTime);
        }
    }
    
    /**
     * Actualiza el progreso de una subtarea específica
     */
    public void updateSubtaskProgress(String trackerId, String subtaskId, SubtaskProgress.SubtaskStatus status, 
                                    double progressPercentage, Object result, String errorMessage) {
        
        com.intentmanagerms.domain.model.ProgressTracker tracker = activeTrackers.get(trackerId);
        if (tracker == null) {
            logger.warn("Tracker no encontrado: {}", trackerId);
            System.out.println("DEBUG updateSubtaskProgress() - Tracker no encontrado: " + trackerId);
            System.out.println("DEBUG updateSubtaskProgress() - Trackers activos: " + activeTrackers.keySet());
            return;
        }
        
        try {
            // Buscar subtarea en el progreso
            SubtaskProgress subtaskProgress = findSubtaskProgress(tracker, subtaskId);
            if (subtaskProgress == null) {
                logger.warn("Subtarea no encontrada en tracker: {} - {}", trackerId, subtaskId);
                return;
            }
            
            // Actualizar estado de la subtarea
            updateSubtaskStatus(subtaskProgress, status, progressPercentage, result, errorMessage);
            
            // Actualizar contadores del tracker
            updateTrackerCounters(tracker, subtaskId, status);
            
            // Validar completitud si está habilitado
            if (enableCompletionValidation) {
                completionValidator.validateCompletion(tracker);
            }
            
            // Enviar notificación si está habilitado
            if (enableNotifications) {
                progressNotifier.sendNotification(tracker, subtaskProgress, status);
            }
            
            logger.debug("Progreso actualizado para subtarea: {} - {} - {}%", subtaskId, status, progressPercentage);
            
        } catch (Exception e) {
            logger.error("Error al actualizar progreso de subtarea: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene el estado actual del progreso
     */
    public ProgressTrackingResult getProgressStatus(String trackerId) {
        long startTime = System.currentTimeMillis();
        
        try {
            com.intentmanagerms.domain.model.ProgressTracker tracker = activeTrackers.get(trackerId);
            if (tracker == null) {
                System.out.println("DEBUG getProgressStatus() - Tracker no encontrado: " + trackerId);
                System.out.println("DEBUG getProgressStatus() - Trackers activos: " + activeTrackers.keySet());
                return createErrorResult(trackerId, "Tracker no encontrado", startTime);
            }
            
            // Debug log para verificar el estado del tracker
            System.out.println("DEBUG getProgressStatus() - " +
                              "trackerId: " + trackerId + 
                              ", totalSubtasks: " + tracker.getTotalSubtasks() + 
                              ", completedSubtasks: " + tracker.getCompletedSubtasks() + 
                              ", progressPercentage: " + tracker.getProgressPercentage() + 
                              ", isAllCompleted: " + tracker.isAllCompleted());
            
            // Actualizar estadísticas
            updateStatistics(tracker);
            
            // Crear resultado
            ProgressTrackingResult result = createTrackingResult(tracker, startTime);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error al obtener estado de progreso: {}", e.getMessage(), e);
            return createErrorResult(trackerId, e.getMessage(), startTime);
        }
    }
    
    /**
     * Cancela el seguimiento de progreso
     */
    public void cancelTracking(String trackerId) {
        try {
            com.intentmanagerms.domain.model.ProgressTracker tracker = activeTrackers.get(trackerId);
            if (tracker != null) {
                tracker.setState(com.intentmanagerms.domain.model.ProgressTracker.ProgressState.CANCELLED);
                tracker.setCancelled(true);
                tracker.updateProgress();
                
                // Enviar notificación de cancelación
                if (enableNotifications) {
                    progressNotifier.sendCancellationNotification(tracker);
                }
                
                logger.info("Seguimiento de progreso cancelado: {}", trackerId);
            }
        } catch (Exception e) {
            logger.error("Error al cancelar seguimiento: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Limpia trackers expirados
     */
    public void cleanupExpiredTrackers() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<String> expiredTrackerIds = new ArrayList<>();
            
            for (Map.Entry<String, com.intentmanagerms.domain.model.ProgressTracker> entry : activeTrackers.entrySet()) {
                com.intentmanagerms.domain.model.ProgressTracker tracker = entry.getValue();
                
                // Verificar si el tracker ha expirado
                if (tracker.getStartedAt() != null && 
                    ChronoUnit.MINUTES.between(tracker.getStartedAt(), now) > maxTrackingDurationMinutes) {
                    expiredTrackerIds.add(entry.getKey());
                }
            }
            
            // Eliminar trackers expirados
            for (String trackerId : expiredTrackerIds) {
                activeTrackers.remove(trackerId);
                logger.info("Tracker expirado eliminado: {}", trackerId);
            }
            
            if (!expiredTrackerIds.isEmpty()) {
                logger.info("Limpieza completada. {} trackers expirados eliminados", expiredTrackerIds.size());
            }
            
        } catch (Exception e) {
            logger.error("Error en limpieza de trackers: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene estadísticas del sistema de progreso
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("active_trackers", activeTrackers.size());
            stats.put("total_subtasks_tracked", activeTrackers.values().stream()
                    .mapToInt(com.intentmanagerms.domain.model.ProgressTracker::getTotalSubtasks)
                    .sum());
            stats.put("completed_subtasks", activeTrackers.values().stream()
                    .mapToInt(com.intentmanagerms.domain.model.ProgressTracker::getCompletedSubtasks)
                    .sum());
            stats.put("failed_subtasks", activeTrackers.values().stream()
                    .mapToInt(com.intentmanagerms.domain.model.ProgressTracker::getFailedSubtasks)
                    .sum());
            stats.put("in_progress_subtasks", activeTrackers.values().stream()
                    .mapToInt(com.intentmanagerms.domain.model.ProgressTracker::getInProgressSubtasks)
                    .sum());
            stats.put("enable_real_time_tracking", enableRealTimeTracking);
            stats.put("enable_notifications", enableNotifications);
            stats.put("enable_completion_validation", enableCompletionValidation);
            stats.put("max_tracking_duration_minutes", maxTrackingDurationMinutes);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas del sistema: {}", e.getMessage(), e);
        }
        
        return stats;
    }
    
    // Métodos privados de ayuda
    
    private com.intentmanagerms.domain.model.ProgressTracker createProgressTracker(ProgressTrackingRequest request) {
        com.intentmanagerms.domain.model.ProgressTracker tracker = new com.intentmanagerms.domain.model.ProgressTracker();
        tracker.setTrackerId("tracker_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000));
        tracker.setExecutionSessionId(request.getExecutionSessionId());
        tracker.setConversationSessionId(request.getConversationSessionId());
        tracker.setTotalSubtasks(request.getSubtasks() != null ? request.getSubtasks().size() : 0);
        tracker.setPendingSubtasks(tracker.getTotalSubtasks());
        tracker.setMetadata(request.getMetadata());
        tracker.updateProgress();
        
        return tracker;
    }
    
    private void initializeSubtasks(com.intentmanagerms.domain.model.ProgressTracker tracker, List<Subtask> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            return;
        }
        
        for (Subtask subtask : subtasks) {
            SubtaskProgress subtaskProgress = new SubtaskProgress();
            subtaskProgress.setSubtaskId(subtask.getSubtaskId());
            subtaskProgress.setAction(subtask.getAction());
            subtaskProgress.setDescription(subtask.getDescription());
            subtaskProgress.setEntities(subtask.getEntities());
            subtaskProgress.setConfidenceScore(subtask.getConfidenceScore() != null ? subtask.getConfidenceScore() : 0.0);
            subtaskProgress.setCritical(subtask.getPriority() != null && subtask.getPriority().equals("high"));
            
            tracker.addSubtaskProgress(subtaskProgress);
        }
    }
    
    private void startRealTimeTracking(com.intentmanagerms.domain.model.ProgressTracker tracker) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Actualizar progreso
                tracker.updateProgress();
                
                // Verificar si el tracker ha completado
                if (tracker.isAllCompleted()) {
                    logger.info("Tracker completado: {}", tracker.getTrackerId());
                    // Opcional: remover de trackers activos
                    // activeTrackers.remove(tracker.getTrackerId());
                }
                
            } catch (Exception e) {
                logger.error("Error en seguimiento en tiempo real: {}", e.getMessage(), e);
            }
        }, updateIntervalMs, updateIntervalMs, TimeUnit.MILLISECONDS);
    }
    
    private SubtaskProgress findSubtaskProgress(com.intentmanagerms.domain.model.ProgressTracker tracker, String subtaskId) {
        return tracker.getSubtaskProgress().stream()
                .filter(sp -> subtaskId.equals(sp.getSubtaskId()))
                .findFirst()
                .orElse(null);
    }
    
    private void updateSubtaskStatus(SubtaskProgress subtaskProgress, SubtaskProgress.SubtaskStatus status, 
                                   double progressPercentage, Object result, String errorMessage) {
        
        subtaskProgress.setStatus(status);
        subtaskProgress.updateProgress(progressPercentage);
        
        switch (status) {
            case IN_PROGRESS:
                subtaskProgress.markStarted();
                break;
            case COMPLETED:
                subtaskProgress.markCompleted(result);
                break;
            case FAILED:
                subtaskProgress.markFailed(errorMessage);
                break;
            case RETRYING:
                subtaskProgress.incrementRetry();
                break;
        }
    }
    
    private void updateTrackerCounters(com.intentmanagerms.domain.model.ProgressTracker tracker, String subtaskId, 
                                     SubtaskProgress.SubtaskStatus status) {
        
        switch (status) {
            case IN_PROGRESS:
                tracker.markSubtaskInProgress(subtaskId);
                break;
            case COMPLETED:
                tracker.markSubtaskCompleted(subtaskId);
                break;
            case FAILED:
                tracker.markSubtaskFailed(subtaskId);
                break;
        }
        
        // Forzar actualización del progreso después de cambiar contadores
        tracker.updateProgress();
    }
    
    private void updateStatistics(com.intentmanagerms.domain.model.ProgressTracker tracker) {
        // Calcular tiempo promedio de ejecución
        long totalExecutionTime = tracker.getSubtaskProgress().stream()
                .mapToLong(SubtaskProgress::getExecutionTimeMs)
                .sum();
        
        int completedCount = tracker.getCompletedSubtasks();
        if (completedCount > 0) {
            tracker.setAverageExecutionTimePerTaskMs(totalExecutionTime / completedCount);
        }
        
        tracker.setTotalExecutionTimeMs(totalExecutionTime);
    }
    
    private ProgressTrackingResult createTrackingResult(com.intentmanagerms.domain.model.ProgressTracker tracker, long startTime) {
        ProgressTrackingResult result = new ProgressTrackingResult();
        result.setTrackerId(tracker.getTrackerId());
        result.setExecutionSessionId(tracker.getExecutionSessionId());
        result.setConversationSessionId(tracker.getConversationSessionId());
        result.setProgressTracker(tracker);
        result.setSuccess(true);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        // Crear estado de completitud
        ProgressTrackingResult.CompletionStatus completionStatus = new ProgressTrackingResult.CompletionStatus();
        completionStatus.setCompleted(tracker.isAllCompleted());
        completionStatus.setCompletionPercentage(tracker.getProgressPercentage());
        completionStatus.setRemainingSubtasks(tracker.getTotalSubtasks() - tracker.getCompletedSubtasks());
        completionStatus.setCompletionMessage(tracker.isAllCompleted() ? "Todas las subtareas completadas" : "Progreso en curso");
        result.setCompletionStatus(completionStatus);
        
        // Crear estadísticas
        ProgressTrackingResult.ProgressStatistics statistics = new ProgressTrackingResult.ProgressStatistics();
        statistics.setTotalSubtasks(tracker.getTotalSubtasks());
        statistics.setCompletedSubtasks(tracker.getCompletedSubtasks());
        statistics.setFailedSubtasks(tracker.getFailedSubtasks());
        statistics.setInProgressSubtasks(tracker.getInProgressSubtasks());
        statistics.setPendingSubtasks(tracker.getPendingSubtasks());
        statistics.setAverageExecutionTimeMs(tracker.getAverageExecutionTimePerTaskMs());
        statistics.setTotalExecutionTimeMs(tracker.getTotalExecutionTimeMs());
        
        if (tracker.getTotalSubtasks() > 0) {
            statistics.setSuccessRate((double) tracker.getCompletedSubtasks() / tracker.getTotalSubtasks());
            statistics.setFailureRate((double) tracker.getFailedSubtasks() / tracker.getTotalSubtasks());
        }
        
        result.setStatistics(statistics);
        
        return result;
    }
    
    private ProgressTrackingResult createErrorResult(String trackerId, String errorMessage, long startTime) {
        ProgressTrackingResult result = new ProgressTrackingResult();
        result.setTrackerId(trackerId);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    // Método de inicialización post-construcción
    public void initialize() {
        if (enableAutoCleanup) {
            scheduler.scheduleAtFixedRate(this::cleanupExpiredTrackers, 
                    cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
            logger.info("Limpieza automática programada cada {} minutos", cleanupIntervalMinutes);
        }
    }
}
