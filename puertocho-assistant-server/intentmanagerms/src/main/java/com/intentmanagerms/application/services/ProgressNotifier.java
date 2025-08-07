package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.ProgressTracker;
import com.intentmanagerms.domain.model.ProgressTrackingResult;
import com.intentmanagerms.domain.model.SubtaskProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notificador de progreso para el sistema de seguimiento
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@Service
public class ProgressNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressNotifier.class);
    
    // Almacenamiento de notificaciones
    private final Map<String, List<ProgressTrackingResult.ProgressNotification>> notifications = new ConcurrentHashMap<>();
    
    /**
     * Envía una notificación de progreso
     */
    public void sendNotification(ProgressTracker tracker, SubtaskProgress subtaskProgress, 
                               SubtaskProgress.SubtaskStatus status) {
        
        try {
            ProgressTrackingResult.ProgressNotification notification = createNotification(tracker, subtaskProgress, status);
            
            // Almacenar notificación
            String trackerId = tracker.getTrackerId();
            notifications.computeIfAbsent(trackerId, k -> new ArrayList<>()).add(notification);
            
            // Log de la notificación
            logger.info("Notificación enviada: {} - {} - {} - {}%", 
                       trackerId, subtaskProgress.getSubtaskId(), status, subtaskProgress.getProgressPercentage());
            
        } catch (Exception e) {
            logger.error("Error al enviar notificación: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación de cancelación
     */
    public void sendCancellationNotification(ProgressTracker tracker) {
        try {
            ProgressTrackingResult.ProgressNotification notification = new ProgressTrackingResult.ProgressNotification();
            notification.setNotificationId("notif_" + UUID.randomUUID().toString().substring(0, 8));
            notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.ERROR_OCCURRED);
            notification.setMessage("Seguimiento de progreso cancelado");
            notification.setSubtaskId(null);
            notification.setProgressPercentage(tracker.getProgressPercentage());
            notification.setTimestamp(LocalDateTime.now());
            
            // Almacenar notificación
            String trackerId = tracker.getTrackerId();
            notifications.computeIfAbsent(trackerId, k -> new ArrayList<>()).add(notification);
            
            logger.info("Notificación de cancelación enviada para tracker: {}", trackerId);
            
        } catch (Exception e) {
            logger.error("Error al enviar notificación de cancelación: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación de completitud
     */
    public void sendCompletionNotification(ProgressTracker tracker) {
        try {
            ProgressTrackingResult.ProgressNotification notification = new ProgressTrackingResult.ProgressNotification();
            notification.setNotificationId("notif_" + UUID.randomUUID().toString().substring(0, 8));
            notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.COMPLETION_REACHED);
            notification.setMessage("Todas las subtareas completadas exitosamente");
            notification.setSubtaskId(null);
            notification.setProgressPercentage(100.0);
            notification.setTimestamp(LocalDateTime.now());
            
            // Almacenar notificación
            String trackerId = tracker.getTrackerId();
            notifications.computeIfAbsent(trackerId, k -> new ArrayList<>()).add(notification);
            
            logger.info("Notificación de completitud enviada para tracker: {}", trackerId);
            
        } catch (Exception e) {
            logger.error("Error al enviar notificación de completitud: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación de error
     */
    public void sendErrorNotification(ProgressTracker tracker, String errorMessage) {
        try {
            ProgressTrackingResult.ProgressNotification notification = new ProgressTrackingResult.ProgressNotification();
            notification.setNotificationId("notif_" + UUID.randomUUID().toString().substring(0, 8));
            notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.ERROR_OCCURRED);
            notification.setMessage("Error en el seguimiento: " + errorMessage);
            notification.setSubtaskId(null);
            notification.setProgressPercentage(tracker.getProgressPercentage());
            notification.setTimestamp(LocalDateTime.now());
            
            // Almacenar notificación
            String trackerId = tracker.getTrackerId();
            notifications.computeIfAbsent(trackerId, k -> new ArrayList<>()).add(notification);
            
            logger.warn("Notificación de error enviada para tracker: {} - {}", trackerId, errorMessage);
            
        } catch (Exception e) {
            logger.error("Error al enviar notificación de error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación de advertencia
     */
    public void sendWarningNotification(ProgressTracker tracker, String warningMessage) {
        try {
            ProgressTrackingResult.ProgressNotification notification = new ProgressTrackingResult.ProgressNotification();
            notification.setNotificationId("notif_" + UUID.randomUUID().toString().substring(0, 8));
            notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.WARNING);
            notification.setMessage("Advertencia: " + warningMessage);
            notification.setSubtaskId(null);
            notification.setProgressPercentage(tracker.getProgressPercentage());
            notification.setTimestamp(LocalDateTime.now());
            
            // Almacenar notificación
            String trackerId = tracker.getTrackerId();
            notifications.computeIfAbsent(trackerId, k -> new ArrayList<>()).add(notification);
            
            logger.warn("Notificación de advertencia enviada para tracker: {} - {}", trackerId, warningMessage);
            
        } catch (Exception e) {
            logger.error("Error al enviar notificación de advertencia: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene las notificaciones de un tracker
     */
    public List<ProgressTrackingResult.ProgressNotification> getNotifications(String trackerId) {
        return notifications.getOrDefault(trackerId, new ArrayList<>());
    }
    
    /**
     * Obtiene todas las notificaciones
     */
    public Map<String, List<ProgressTrackingResult.ProgressNotification>> getAllNotifications() {
        return new ConcurrentHashMap<>(notifications);
    }
    
    /**
     * Limpia las notificaciones de un tracker
     */
    public void clearNotifications(String trackerId) {
        notifications.remove(trackerId);
        logger.info("Notificaciones limpiadas para tracker: {}", trackerId);
    }
    
    /**
     * Limpia todas las notificaciones
     */
    public void clearAllNotifications() {
        notifications.clear();
        logger.info("Todas las notificaciones han sido limpiadas");
    }
    
    /**
     * Obtiene estadísticas de notificaciones
     */
    public Map<String, Object> getNotificationStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        try {
            int totalNotifications = notifications.values().stream()
                    .mapToInt(List::size)
                    .sum();
            
            int totalTrackers = notifications.size();
            
            stats.put("total_notifications", totalNotifications);
            stats.put("total_trackers_with_notifications", totalTrackers);
            stats.put("average_notifications_per_tracker", totalTrackers > 0 ? (double) totalNotifications / totalTrackers : 0.0);
            
            // Contar por tipo de notificación
            Map<String, Integer> notificationsByType = new java.util.HashMap<>();
            for (List<ProgressTrackingResult.ProgressNotification> trackerNotifications : notifications.values()) {
                for (ProgressTrackingResult.ProgressNotification notification : trackerNotifications) {
                    String type = notification.getType().getValue();
                    notificationsByType.put(type, notificationsByType.getOrDefault(type, 0) + 1);
                }
            }
            stats.put("notifications_by_type", notificationsByType);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de notificaciones: {}", e.getMessage(), e);
        }
        
        return stats;
    }
    
    // Métodos privados de ayuda
    
    private ProgressTrackingResult.ProgressNotification createNotification(ProgressTracker tracker, 
                                                                         SubtaskProgress subtaskProgress, 
                                                                         SubtaskProgress.SubtaskStatus status) {
        
        ProgressTrackingResult.ProgressNotification notification = new ProgressTrackingResult.ProgressNotification();
        notification.setNotificationId("notif_" + UUID.randomUUID().toString().substring(0, 8));
        notification.setSubtaskId(subtaskProgress.getSubtaskId());
        notification.setProgressPercentage(subtaskProgress.getProgressPercentage());
        notification.setTimestamp(LocalDateTime.now());
        
        // Determinar tipo de notificación basado en el estado
        switch (status) {
            case IN_PROGRESS:
                notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.SUBTASK_STARTED);
                notification.setMessage("Subtarea iniciada: " + subtaskProgress.getDescription());
                break;
            case COMPLETED:
                notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.SUBTASK_COMPLETED);
                notification.setMessage("Subtarea completada: " + subtaskProgress.getDescription());
                break;
            case FAILED:
                notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.SUBTASK_FAILED);
                notification.setMessage("Subtarea fallida: " + subtaskProgress.getDescription() + 
                                     (subtaskProgress.getErrorMessage() != null ? " - " + subtaskProgress.getErrorMessage() : ""));
                break;
            case RETRYING:
                notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.PROGRESS_UPDATE);
                notification.setMessage("Reintentando subtarea: " + subtaskProgress.getDescription());
                break;
            default:
                notification.setType(ProgressTrackingResult.ProgressNotification.NotificationType.PROGRESS_UPDATE);
                notification.setMessage("Actualización de progreso: " + subtaskProgress.getDescription());
                break;
        }
        
        // Agregar metadata
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("action", subtaskProgress.getAction());
        metadata.put("confidence_score", subtaskProgress.getConfidenceScore());
        metadata.put("is_critical", subtaskProgress.isCritical());
        metadata.put("retry_count", subtaskProgress.getRetryCount());
        notification.setMetadata(metadata);
        
        return notification;
    }
}
