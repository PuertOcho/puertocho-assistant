package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.SubtaskProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de estados de tareas para el sistema de progreso
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@Service
public class TaskStatusManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskStatusManager.class);
    
    // Almacenamiento de estados de tareas
    private final Map<String, SubtaskProgress> taskStatuses = new ConcurrentHashMap<>();
    
    /**
     * Actualiza el estado de una tarea
     */
    public void updateTaskStatus(String taskId, SubtaskProgress.SubtaskStatus status, 
                               double progressPercentage, String message) {
        
        try {
            SubtaskProgress taskProgress = taskStatuses.get(taskId);
            if (taskProgress == null) {
                taskProgress = new SubtaskProgress();
                taskProgress.setSubtaskId(taskId);
                taskStatuses.put(taskId, taskProgress);
            }
            
            taskProgress.setStatus(status);
            taskProgress.updateProgress(progressPercentage);
            taskProgress.setLastUpdatedAt(LocalDateTime.now());
            
            if (message != null) {
                switch (status) {
                    case COMPLETED:
                        taskProgress.setCompletionMessage(message);
                        break;
                    case FAILED:
                        taskProgress.setErrorMessage(message);
                        break;
                }
            }
            
            logger.debug("Estado de tarea actualizado: {} - {} - {}%", taskId, status, progressPercentage);
            
        } catch (Exception e) {
            logger.error("Error al actualizar estado de tarea: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene el estado de una tarea
     */
    public SubtaskProgress getTaskStatus(String taskId) {
        return taskStatuses.get(taskId);
    }
    
    /**
     * Obtiene todos los estados de tareas
     */
    public List<SubtaskProgress> getAllTaskStatuses() {
        return List.copyOf(taskStatuses.values());
    }
    
    /**
     * Obtiene tareas por estado
     */
    public List<SubtaskProgress> getTasksByStatus(SubtaskProgress.SubtaskStatus status) {
        return taskStatuses.values().stream()
                .filter(task -> status.equals(task.getStatus()))
                .toList();
    }
    
    /**
     * Verifica si una tarea está completada
     */
    public boolean isTaskCompleted(String taskId) {
        SubtaskProgress taskProgress = taskStatuses.get(taskId);
        return taskProgress != null && taskProgress.isCompleted();
    }
    
    /**
     * Verifica si una tarea ha fallado
     */
    public boolean isTaskFailed(String taskId) {
        SubtaskProgress taskProgress = taskStatuses.get(taskId);
        return taskProgress != null && taskProgress.isFailed();
    }
    
    /**
     * Obtiene estadísticas de estados de tareas
     */
    public Map<String, Object> getTaskStatusStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        try {
            stats.put("total_tasks", taskStatuses.size());
            stats.put("completed_tasks", getTasksByStatus(SubtaskProgress.SubtaskStatus.COMPLETED).size());
            stats.put("failed_tasks", getTasksByStatus(SubtaskProgress.SubtaskStatus.FAILED).size());
            stats.put("in_progress_tasks", getTasksByStatus(SubtaskProgress.SubtaskStatus.IN_PROGRESS).size());
            stats.put("pending_tasks", getTasksByStatus(SubtaskProgress.SubtaskStatus.PENDING).size());
            stats.put("cancelled_tasks", getTasksByStatus(SubtaskProgress.SubtaskStatus.CANCELLED).size());
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de estados de tareas: {}", e.getMessage(), e);
        }
        
        return stats;
    }
    
    /**
     * Limpia estados de tareas completadas
     */
    public void cleanupCompletedTasks() {
        try {
            List<String> completedTaskIds = taskStatuses.entrySet().stream()
                    .filter(entry -> entry.getValue().isCompleted())
                    .map(Map.Entry::getKey)
                    .toList();
            
            for (String taskId : completedTaskIds) {
                taskStatuses.remove(taskId);
            }
            
            if (!completedTaskIds.isEmpty()) {
                logger.info("Limpieza completada. {} tareas completadas eliminadas", completedTaskIds.size());
            }
            
        } catch (Exception e) {
            logger.error("Error en limpieza de tareas completadas: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Reinicia el estado de una tarea
     */
    public void resetTaskStatus(String taskId) {
        try {
            SubtaskProgress taskProgress = taskStatuses.get(taskId);
            if (taskProgress != null) {
                taskProgress.setStatus(SubtaskProgress.SubtaskStatus.PENDING);
                taskProgress.setProgressPercentage(0.0);
                taskProgress.setCompleted(false);
                taskProgress.setFailed(false);
                taskProgress.setErrorMessage(null);
                taskProgress.setCompletionMessage(null);
                taskProgress.setRetryCount(0);
                taskProgress.setLastUpdatedAt(LocalDateTime.now());
                
                logger.info("Estado de tarea reiniciado: {}", taskId);
            }
            
        } catch (Exception e) {
            logger.error("Error al reiniciar estado de tarea: {}", e.getMessage(), e);
        }
    }
}
