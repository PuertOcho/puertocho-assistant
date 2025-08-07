package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.ProgressTracker;
import com.intentmanagerms.domain.model.SubtaskProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validador de completitud para el sistema de progreso
 * T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
 */
@Service
public class CompletionValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(CompletionValidator.class);
    
    /**
     * Valida la completitud de un tracker de progreso
     */
    public boolean validateCompletion(ProgressTracker tracker) {
        try {
            logger.debug("Validando completitud para tracker: {}", tracker.getTrackerId());
            
            // Verificar si todas las subtareas están completadas
            boolean allCompleted = tracker.isAllCompleted();
            
            // Verificar si hay subtareas críticas fallidas
            boolean hasCriticalFailures = hasCriticalFailures(tracker);
            
            // Verificar dependencias
            boolean dependenciesSatisfied = validateDependencies(tracker);
            
            // Verificar resultados
            boolean resultsValid = validateResults(tracker);
            
            // Determinar si la validación es exitosa
            boolean isValid = allCompleted && !hasCriticalFailures && dependenciesSatisfied && resultsValid;
            
            if (isValid) {
                tracker.setCompletionMessage("Todas las subtareas completadas exitosamente");
                logger.info("Validación de completitud exitosa para tracker: {}", tracker.getTrackerId());
            } else {
                List<String> validationErrors = new ArrayList<>();
                
                if (!allCompleted) {
                    validationErrors.add("No todas las subtareas están completadas");
                }
                if (hasCriticalFailures) {
                    validationErrors.add("Hay subtareas críticas fallidas");
                }
                if (!dependenciesSatisfied) {
                    validationErrors.add("Las dependencias entre subtareas no están satisfechas");
                }
                if (!resultsValid) {
                    validationErrors.add("Los resultados de algunas subtareas no son válidos");
                }
                
                tracker.setErrorMessage("Validación fallida: " + String.join(", ", validationErrors));
                logger.warn("Validación de completitud fallida para tracker: {}. Errores: {}", 
                          tracker.getTrackerId(), validationErrors);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error en validación de completitud: {}", e.getMessage(), e);
            tracker.setErrorMessage("Error en validación: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica si hay subtareas críticas fallidas
     */
    private boolean hasCriticalFailures(ProgressTracker tracker) {
        if (tracker.getSubtaskProgress() == null) {
            return false;
        }
        
        return tracker.getSubtaskProgress().stream()
                .anyMatch(subtask -> subtask.isCritical() && subtask.isFailed());
    }
    
    /**
     * Valida las dependencias entre subtareas
     */
    private boolean validateDependencies(ProgressTracker tracker) {
        if (tracker.getSubtaskProgress() == null) {
            return true;
        }
        
        try {
            for (SubtaskProgress subtask : tracker.getSubtaskProgress()) {
                if (subtask.getDependencies() != null && !subtask.getDependencies().isEmpty()) {
                    // Verificar que todas las dependencias estén completadas
                    for (Map.Entry<String, Object> dependency : subtask.getDependencies().entrySet()) {
                        String dependencyId = dependency.getKey();
                        SubtaskProgress dependencySubtask = findSubtaskById(tracker, dependencyId);
                        
                        if (dependencySubtask == null) {
                            logger.warn("Dependencia no encontrada: {} para subtarea: {}", dependencyId, subtask.getSubtaskId());
                            return false;
                        }
                        
                        if (!dependencySubtask.isCompleted()) {
                            logger.debug("Dependencia no completada: {} para subtarea: {}", dependencyId, subtask.getSubtaskId());
                            return false;
                        }
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error al validar dependencias: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Valida los resultados de las subtareas
     */
    private boolean validateResults(ProgressTracker tracker) {
        if (tracker.getSubtaskProgress() == null) {
            return true;
        }
        
        try {
            for (SubtaskProgress subtask : tracker.getSubtaskProgress()) {
                if (subtask.isCompleted() && subtask.getResult() == null) {
                    logger.warn("Subtarea completada sin resultado: {}", subtask.getSubtaskId());
                    return false;
                }
                
                // Validaciones específicas por tipo de acción
                if (subtask.isCompleted() && subtask.getResult() != null) {
                    if (!validateActionResult(subtask)) {
                        logger.warn("Resultado de subtarea no válido: {}", subtask.getSubtaskId());
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error al validar resultados: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Valida el resultado de una acción específica
     */
    private boolean validateActionResult(SubtaskProgress subtask) {
        try {
            String action = subtask.getAction();
            Map<String, Object> result = subtask.getResult();
            
            if (action == null || result == null) {
                return false;
            }
            
            // Validaciones específicas por tipo de acción
            switch (action) {
                case "consultar_tiempo":
                    return validateWeatherResult(result);
                case "programar_alarma":
                    return validateAlarmResult(result);
                case "crear_github_issue":
                    return validateGithubIssueResult(result);
                case "encender_luz":
                    return validateLightResult(result);
                default:
                    // Para acciones no específicas, verificar que el resultado no esté vacío
                    return !result.isEmpty();
            }
            
        } catch (Exception e) {
            logger.error("Error al validar resultado de acción: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Valida resultado de consulta de tiempo
     */
    private boolean validateWeatherResult(Map<String, Object> result) {
        return result.containsKey("location") && 
               result.containsKey("temperature") && 
               result.containsKey("condition");
    }
    
    /**
     * Valida resultado de programación de alarma
     */
    private boolean validateAlarmResult(Map<String, Object> result) {
        return result.containsKey("alarm_id") && 
               result.containsKey("scheduled_time");
    }
    
    /**
     * Valida resultado de creación de issue en GitHub
     */
    private boolean validateGithubIssueResult(Map<String, Object> result) {
        return result.containsKey("issue_id") && 
               result.containsKey("issue_url");
    }
    
    /**
     * Valida resultado de encendido de luz
     */
    private boolean validateLightResult(Map<String, Object> result) {
        return result.containsKey("light_id") && 
               result.containsKey("status");
    }
    
    /**
     * Busca una subtarea por ID
     */
    private SubtaskProgress findSubtaskById(ProgressTracker tracker, String subtaskId) {
        if (tracker.getSubtaskProgress() == null) {
            return null;
        }
        
        return tracker.getSubtaskProgress().stream()
                .filter(subtask -> subtaskId.equals(subtask.getSubtaskId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Obtiene estadísticas de validación
     */
    public Map<String, Object> getValidationStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        try {
            stats.put("validation_checks_performed", 0); // Se incrementaría en implementación real
            stats.put("validation_success_rate", 1.0);
            stats.put("validation_errors_count", 0);
            stats.put("dependency_validation_enabled", true);
            stats.put("result_validation_enabled", true);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de validación: {}", e.getMessage(), e);
        }
        
        return stats;
    }
}
