package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Orquestador principal para la ejecución de subtareas.
 * Ejecuta subtareas secuencial o paralelamente según dependencias detectadas.
 */
@Service
public class TaskOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskOrchestrator.class);
    
    @Autowired
    private ExecutionEngine executionEngine;
    
    @Autowired
    private DependencyManager dependencyManager;
    
    @Autowired
    private ParallelExecutor parallelExecutor;
    
    @Autowired
    private McpActionRegistry mcpActionRegistry;
    
    @Autowired
    private ConversationManager conversationManager;
    
    @Value("${task.orchestrator.enable-parallel-execution:true}")
    private Boolean enableParallelExecution;
    
    @Value("${task.orchestrator.max-parallel-tasks:3}")
    private Integer maxParallelTasks;
    
    @Value("${task.orchestrator.enable-error-recovery:true}")
    private Boolean enableErrorRecovery;
    
    @Value("${task.orchestrator.enable-rollback-on-failure:true}")
    private Boolean enableRollbackOnFailure;
    
    @Value("${task.orchestrator.task-timeout-seconds:120}")
    private Integer taskTimeoutSeconds;
    
    @Value("${task.orchestrator.max-retries:3}")
    private Integer maxRetries;
    
    @Value("${task.orchestrator.retry-delay-ms:1000}")
    private Long retryDelayMs;
    
    @Value("${task.orchestrator.enable-progress-tracking:true}")
    private Boolean enableProgressTracking;
    
    @Value("${task.orchestrator.progress-update-interval-ms:1000}")
    private Long progressUpdateIntervalMs;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Map<String, TaskExecutionSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Ejecuta una lista de subtareas con orquestación inteligente.
     */
    public TaskExecutionResult executeSubtasks(List<Subtask> subtasks, String conversationSessionId) {
        long startTime = System.currentTimeMillis();
        String executionId = generateExecutionId();
        
        logger.info("Iniciando ejecución de {} subtareas con ID: {}", subtasks.size(), executionId);
        
        try {
            // Crear sesión de ejecución
            TaskExecutionSession session = createExecutionSession(executionId, subtasks, conversationSessionId);
            activeSessions.put(executionId, session);
            
            // Validar subtareas
            validateSubtasks(subtasks);
            
            // Resolver dependencias y crear plan de ejecución
            ExecutionPlan executionPlan = dependencyManager.createExecutionPlan(subtasks);
            session.setExecutionPlan(executionPlan);
            
            logger.info("Plan de ejecución creado: {} niveles, {} grupos paralelos", 
                       executionPlan.getDependencyLevels().size(), 
                       executionPlan.getParallelGroups().size());
            
            // Ejecutar subtareas según el plan
            List<SubtaskExecutionResult> results = executePlan(executionPlan, session);
            
            // Procesar resultados
            TaskExecutionResult finalResult = processResults(results, session, startTime);
            
            logger.info("Ejecución completada en {}ms. Éxito: {}/{}", 
                       finalResult.getTotalExecutionTimeMs(),
                       finalResult.getSuccessfulTasks(),
                       finalResult.getTotalTasks());
            
            return finalResult;
            
        } catch (Exception e) {
            logger.error("Error durante la ejecución de subtareas: {}", e.getMessage(), e);
            return createErrorResult(executionId, e.getMessage(), startTime);
        } finally {
            activeSessions.remove(executionId);
        }
    }
    
    /**
     * Ejecuta el plan de ejecución nivel por nivel.
     */
    private List<SubtaskExecutionResult> executePlan(ExecutionPlan executionPlan, TaskExecutionSession session) {
        List<SubtaskExecutionResult> allResults = new ArrayList<>();
        
        for (int level = 0; level < executionPlan.getDependencyLevels().size(); level++) {
            List<Subtask> levelSubtasks = executionPlan.getDependencyLevels().get(level);
            logger.info("Ejecutando nivel {} con {} subtareas", level, levelSubtasks.size());
            
            // Ejecutar subtareas del nivel actual
            List<SubtaskExecutionResult> levelResults = executeLevel(levelSubtasks, session);
            allResults.addAll(levelResults);
            
            // Verificar si hay errores críticos
            if (hasCriticalErrors(levelResults) && enableRollbackOnFailure) {
                logger.warn("Errores críticos detectados en nivel {}. Iniciando rollback.", level);
                performRollback(allResults, session);
                break;
            }
            
            // Actualizar progreso
            if (enableProgressTracking) {
                updateProgress(session, level, levelResults);
            }
        }
        
        return allResults;
    }
    
    /**
     * Ejecuta las subtareas de un nivel específico.
     */
    private List<SubtaskExecutionResult> executeLevel(List<Subtask> levelSubtasks, TaskExecutionSession session) {
        if (enableParallelExecution && levelSubtasks.size() > 1) {
            return executeLevelParallel(levelSubtasks, session);
        } else {
            return executeLevelSequential(levelSubtasks, session);
        }
    }
    
    /**
     * Ejecuta subtareas de un nivel en paralelo.
     */
    private List<SubtaskExecutionResult> executeLevelParallel(List<Subtask> levelSubtasks, TaskExecutionSession session) {
        logger.debug("Ejecutando {} subtareas en paralelo", levelSubtasks.size());
        
        List<CompletableFuture<SubtaskExecutionResult>> futures = levelSubtasks.stream()
                .map(subtask -> CompletableFuture.supplyAsync(() -> 
                    executeSubtask(subtask, session), executorService))
                .collect(Collectors.toList());
        
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            // Esperar con timeout
            allFutures.get(taskTimeoutSeconds, TimeUnit.SECONDS);
            
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                    
        } catch (TimeoutException e) {
            logger.error("Timeout en ejecución paralela de nivel");
            return handleTimeout(futures, levelSubtasks);
        } catch (Exception e) {
            logger.error("Error en ejecución paralela: {}", e.getMessage());
            return handleExecutionError(futures, levelSubtasks, e);
        }
    }
    
    /**
     * Ejecuta subtareas de un nivel secuencialmente.
     */
    private List<SubtaskExecutionResult> executeLevelSequential(List<Subtask> levelSubtasks, TaskExecutionSession session) {
        logger.debug("Ejecutando {} subtareas secuencialmente", levelSubtasks.size());
        
        List<SubtaskExecutionResult> results = new ArrayList<>();
        
        for (Subtask subtask : levelSubtasks) {
            SubtaskExecutionResult result = executeSubtask(subtask, session);
            results.add(result);
            
            // Si hay error crítico y rollback está habilitado, detener ejecución
            if (result.isCriticalError() && enableRollbackOnFailure) {
                logger.warn("Error crítico detectado en subtarea {}. Deteniendo ejecución secuencial.", 
                           subtask.getSubtaskId());
                break;
            }
        }
        
        return results;
    }
    
    /**
     * Ejecuta una subtarea individual.
     */
    private SubtaskExecutionResult executeSubtask(Subtask subtask, TaskExecutionSession session) {
        long startTime = System.currentTimeMillis();
        logger.debug("Ejecutando subtarea: {} - {}", subtask.getSubtaskId(), subtask.getAction());
        
        try {
            // Marcar como ejecutando
            subtask.startExecution();
            session.updateSubtaskStatus(subtask.getSubtaskId(), SubtaskStatus.EXECUTING);
            
            // Ejecutar la acción MCP
            Object result = executionEngine.executeAction(subtask, session.getConversationSessionId());
            
            // Marcar como completada
            subtask.completeExecution(result);
            session.updateSubtaskStatus(subtask.getSubtaskId(), SubtaskStatus.COMPLETED);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return SubtaskExecutionResult.builder()
                    .subtaskId(subtask.getSubtaskId())
                    .action(subtask.getAction())
                    .status(SubtaskStatus.COMPLETED)
                    .result(result)
                    .executionTimeMs(executionTime)
                    .success(true)
                    .criticalError(false)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error ejecutando subtarea {}: {}", subtask.getSubtaskId(), e.getMessage());
            
            // Manejar reintentos
            if (subtask.canRetry() && enableErrorRecovery) {
                return handleRetry(subtask, session, e, startTime);
            } else {
                subtask.failExecution(e.getMessage());
                session.updateSubtaskStatus(subtask.getSubtaskId(), SubtaskStatus.FAILED);
                
                return SubtaskExecutionResult.builder()
                        .subtaskId(subtask.getSubtaskId())
                        .action(subtask.getAction())
                        .status(SubtaskStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .success(false)
                        .criticalError(isCriticalError(subtask, e))
                        .build();
            }
        }
    }
    
    /**
     * Maneja reintentos de subtareas fallidas.
     */
    private SubtaskExecutionResult handleRetry(Subtask subtask, TaskExecutionSession session, Exception error, long startTime) {
        subtask.incrementRetryCount();
        session.updateSubtaskStatus(subtask.getSubtaskId(), SubtaskStatus.RETRYING);
        
        logger.info("Reintentando subtarea {} (intento {}/{})", 
                   subtask.getSubtaskId(), subtask.getRetryCount(), subtask.getMaxRetries());
        
        try {
            // Esperar antes del reintento
            Thread.sleep(retryDelayMs);
            
            // Reintentar ejecución
            Object result = executionEngine.executeAction(subtask, session.getConversationSessionId());
            
            subtask.completeExecution(result);
            session.updateSubtaskStatus(subtask.getSubtaskId(), SubtaskStatus.COMPLETED);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return SubtaskExecutionResult.builder()
                    .subtaskId(subtask.getSubtaskId())
                    .action(subtask.getAction())
                    .status(SubtaskStatus.COMPLETED)
                    .result(result)
                    .executionTimeMs(executionTime)
                    .success(true)
                    .criticalError(false)
                    .retryCount(subtask.getRetryCount())
                    .build();
                    
        } catch (Exception retryError) {
            logger.error("Reintento fallido para subtarea {}: {}", subtask.getSubtaskId(), retryError.getMessage());
            
            subtask.failExecution(retryError.getMessage());
            session.updateSubtaskStatus(subtask.getSubtaskId(), SubtaskStatus.FAILED);
            
            return SubtaskExecutionResult.builder()
                    .subtaskId(subtask.getSubtaskId())
                    .action(subtask.getAction())
                    .status(SubtaskStatus.FAILED)
                    .errorMessage(retryError.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .success(false)
                    .criticalError(isCriticalError(subtask, retryError))
                    .retryCount(subtask.getRetryCount())
                    .build();
        }
    }
    
    /**
     * Maneja timeout en ejecución paralela.
     */
    private List<SubtaskExecutionResult> handleTimeout(List<CompletableFuture<SubtaskExecutionResult>> futures, 
                                                      List<Subtask> levelSubtasks) {
        List<SubtaskExecutionResult> results = new ArrayList<>();
        
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<SubtaskExecutionResult> future = futures.get(i);
            Subtask subtask = levelSubtasks.get(i);
            
            if (future.isDone()) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    results.add(createTimeoutResult(subtask, e.getMessage()));
                }
            } else {
                future.cancel(true);
                results.add(createTimeoutResult(subtask, "Timeout en ejecución"));
            }
        }
        
        return results;
    }
    
    /**
     * Maneja errores en ejecución paralela.
     */
    private List<SubtaskExecutionResult> handleExecutionError(List<CompletableFuture<SubtaskExecutionResult>> futures,
                                                             List<Subtask> levelSubtasks, Exception error) {
        List<SubtaskExecutionResult> results = new ArrayList<>();
        
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<SubtaskExecutionResult> future = futures.get(i);
            Subtask subtask = levelSubtasks.get(i);
            
            if (future.isDone()) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    results.add(createErrorResult(subtask, e.getMessage()));
                }
            } else {
                future.cancel(true);
                results.add(createErrorResult(subtask, "Ejecución cancelada por error"));
            }
        }
        
        return results;
    }
    
    /**
     * Verifica si hay errores críticos en los resultados.
     */
    private boolean hasCriticalErrors(List<SubtaskExecutionResult> results) {
        return results.stream().anyMatch(SubtaskExecutionResult::isCriticalError);
    }
    
    /**
     * Determina si un error es crítico.
     */
    private boolean isCriticalError(Subtask subtask, Exception error) {
        // Errores críticos: timeout, errores de conexión, errores de autenticación
        String errorMessage = error.getMessage().toLowerCase();
        return errorMessage.contains("timeout") || 
               errorMessage.contains("connection") || 
               errorMessage.contains("authentication") ||
               errorMessage.contains("unauthorized");
    }
    
    /**
     * Realiza rollback de subtareas ejecutadas.
     */
    private void performRollback(List<SubtaskExecutionResult> results, TaskExecutionSession session) {
        logger.info("Iniciando rollback de {} subtareas", results.size());
        
        // Filtrar subtareas exitosas para rollback
        List<SubtaskExecutionResult> successfulResults = results.stream()
                .filter(SubtaskExecutionResult::isSuccess)
                .collect(Collectors.toList());
        
        for (SubtaskExecutionResult result : successfulResults) {
            try {
                executionEngine.rollbackAction(result.getSubtaskId(), result.getResult());
                session.updateSubtaskStatus(result.getSubtaskId(), SubtaskStatus.CANCELLED);
                logger.debug("Rollback exitoso para subtarea: {}", result.getSubtaskId());
            } catch (Exception e) {
                logger.error("Error en rollback de subtarea {}: {}", result.getSubtaskId(), e.getMessage());
            }
        }
    }
    
    /**
     * Actualiza el progreso de la ejecución.
     */
    private void updateProgress(TaskExecutionSession session, int level, List<SubtaskExecutionResult> levelResults) {
        int completedTasks = (int) levelResults.stream()
                .filter(SubtaskExecutionResult::isSuccess)
                .count();
        
        int totalTasks = session.getTotalSubtasks();
        int overallCompleted = session.getCompletedSubtasks() + completedTasks;
        
        double progress = (double) overallCompleted / totalTasks * 100;
        
        session.updateProgress(progress, level, levelResults);
        
        logger.debug("Progreso actualizado: {:.1f}% ({}/{})", progress, overallCompleted, totalTasks);
    }
    
    /**
     * Procesa los resultados finales de la ejecución.
     */
    private TaskExecutionResult processResults(List<SubtaskExecutionResult> results, 
                                             TaskExecutionSession session, 
                                             long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;
        
        int successfulTasks = (int) results.stream()
                .filter(SubtaskExecutionResult::isSuccess)
                .count();
        
        int failedTasks = (int) results.stream()
                .filter(r -> !r.isSuccess())
                .count();
        
        boolean allSuccessful = failedTasks == 0;
        
        return TaskExecutionResult.builder()
                .executionId(session.getExecutionId())
                .conversationSessionId(session.getConversationSessionId())
                .totalTasks(results.size())
                .successfulTasks(successfulTasks)
                .failedTasks(failedTasks)
                .allSuccessful(allSuccessful)
                .totalExecutionTimeMs(totalTime)
                .results(results)
                .executionPlan(session.getExecutionPlan())
                .statistics(calculateStatistics(results, totalTime))
                .build();
    }
    
    /**
     * Calcula estadísticas de la ejecución.
     */
    private Map<String, Object> calculateStatistics(List<SubtaskExecutionResult> results, long totalTime) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_execution_time_ms", totalTime);
        stats.put("average_task_time_ms", results.stream()
                .mapToLong(SubtaskExecutionResult::getExecutionTimeMs)
                .average()
                .orElse(0.0));
        
        stats.put("success_rate", (double) results.stream()
                .filter(SubtaskExecutionResult::isSuccess)
                .count() / results.size());
        
        stats.put("parallel_execution_enabled", enableParallelExecution);
        stats.put("error_recovery_enabled", enableErrorRecovery);
        stats.put("rollback_enabled", enableRollbackOnFailure);
        
        return stats;
    }
    
    /**
     * Crea una sesión de ejecución.
     */
    private TaskExecutionSession createExecutionSession(String executionId, List<Subtask> subtasks, String conversationSessionId) {
        return TaskExecutionSession.builder()
                .executionId(executionId)
                .conversationSessionId(conversationSessionId)
                .subtasks(subtasks)
                .totalSubtasks(subtasks.size())
                .completedSubtasks(0)
                .progress(0.0)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Valida las subtareas antes de la ejecución.
     */
    private void validateSubtasks(List<Subtask> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            throw new IllegalArgumentException("La lista de subtareas no puede estar vacía");
        }
        
        for (Subtask subtask : subtasks) {
            if (subtask.getAction() == null || subtask.getAction().trim().isEmpty()) {
                throw new IllegalArgumentException("Todas las subtareas deben tener una acción válida");
            }
        }
    }
    
    /**
     * Genera un ID único para la ejecución.
     */
    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }
    
    /**
     * Crea un resultado de error.
     */
    private SubtaskExecutionResult createTimeoutResult(Subtask subtask, String errorMessage) {
        return SubtaskExecutionResult.builder()
                .subtaskId(subtask.getSubtaskId())
                .action(subtask.getAction())
                .status(SubtaskStatus.TIMEOUT)
                .errorMessage(errorMessage)
                .executionTimeMs(0L)
                .success(false)
                .criticalError(true)
                .build();
    }
    
    /**
     * Crea un resultado de error.
     */
    private SubtaskExecutionResult createErrorResult(Subtask subtask, String errorMessage) {
        return SubtaskExecutionResult.builder()
                .subtaskId(subtask.getSubtaskId())
                .action(subtask.getAction())
                .status(SubtaskStatus.FAILED)
                .errorMessage(errorMessage)
                .executionTimeMs(0L)
                .success(false)
                .criticalError(false)
                .build();
    }
    
    /**
     * Crea un resultado de error para la ejecución completa.
     */
    private TaskExecutionResult createErrorResult(String executionId, String errorMessage, long startTime) {
        return TaskExecutionResult.builder()
                .executionId(executionId)
                .totalTasks(0)
                .successfulTasks(0)
                .failedTasks(0)
                .allSuccessful(false)
                .totalExecutionTimeMs(System.currentTimeMillis() - startTime)
                .errorMessage(errorMessage)
                .results(new ArrayList<>())
                .build();
    }
    
    /**
     * Obtiene el estado de una sesión de ejecución activa.
     */
    public TaskExecutionSession getActiveSession(String executionId) {
        return activeSessions.get(executionId);
    }
    
    /**
     * Cancela una ejecución en progreso.
     */
    public boolean cancelExecution(String executionId) {
        TaskExecutionSession session = activeSessions.get(executionId);
        if (session != null) {
            session.cancel();
            activeSessions.remove(executionId);
            logger.info("Ejecución cancelada: {}", executionId);
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene estadísticas de ejecuciones activas.
     */
    public Map<String, Object> getActiveExecutionsStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("active_executions", activeSessions.size());
        stats.put("total_subtasks_in_progress", activeSessions.values().stream()
                .mapToInt(TaskExecutionSession::getTotalSubtasks)
                .sum());
        stats.put("completed_subtasks", activeSessions.values().stream()
                .mapToInt(TaskExecutionSession::getCompletedSubtasks)
                .sum());
        return stats;
    }
    
    /**
     * Cierra el servicio y libera recursos.
     */
    public void shutdown() {
        logger.info("Cerrando TaskOrchestrator");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 