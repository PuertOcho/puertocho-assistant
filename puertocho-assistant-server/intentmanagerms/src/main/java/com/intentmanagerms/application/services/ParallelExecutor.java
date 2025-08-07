package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.Subtask;
import com.intentmanagerms.domain.model.SubtaskExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Ejecutor paralelo para subtareas.
 * Optimiza la ejecución de múltiples subtareas en paralelo.
 */
@Service
public class ParallelExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(ParallelExecutor.class);
    
    @Value("${task.parallel-executor.max-threads:5}")
    private Integer maxThreads;
    
    @Value("${task.parallel-executor.timeout-seconds:120}")
    private Integer timeoutSeconds;
    
    @Value("${task.parallel-executor.enable-thread-pool:true}")
    private Boolean enableThreadPool;
    
    private final ExecutorService executorService;
    
    public ParallelExecutor() {
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Ejecuta una lista de subtareas en paralelo.
     */
    public List<SubtaskExecutionResult> executeParallel(List<Subtask> subtasks, String conversationSessionId) {
        logger.debug("Ejecutando {} subtareas en paralelo", subtasks.size());
        
        List<CompletableFuture<SubtaskExecutionResult>> futures = null;
        
        try {
            // Crear futures para cada subtarea
            futures = subtasks.stream()
                    .map(subtask -> CompletableFuture.supplyAsync(() -> 
                        executeSubtask(subtask, conversationSessionId), executorService))
                    .collect(Collectors.toList());
            
            // Esperar a que todas las subtareas se completen
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            // Esperar con timeout
            allFutures.get(timeoutSeconds, TimeUnit.SECONDS);
            
            // Recopilar resultados
            List<SubtaskExecutionResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            
            logger.debug("Ejecución paralela completada: {} subtareas", results.size());
            return results;
            
        } catch (TimeoutException e) {
            logger.error("Timeout en ejecución paralela después de {} segundos", timeoutSeconds);
            return handleTimeout(subtasks, futures);
        } catch (Exception e) {
            logger.error("Error en ejecución paralela: {}", e.getMessage());
            return handleExecutionError(subtasks, futures, e);
        }
    }
    
    /**
     * Ejecuta una subtarea individual.
     */
    private SubtaskExecutionResult executeSubtask(Subtask subtask, String conversationSessionId) {
        long startTime = System.currentTimeMillis();
        logger.debug("Ejecutando subtarea en paralelo: {} - {}", subtask.getSubtaskId(), subtask.getAction());
        
        try {
            // Simular ejecución de la subtarea
            // En una implementación real, aquí se llamaría al ExecutionEngine
            Object result = simulateSubtaskExecution(subtask, conversationSessionId);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return SubtaskExecutionResult.builder()
                    .subtaskId(subtask.getSubtaskId())
                    .action(subtask.getAction())
                    .status(com.intentmanagerms.domain.model.SubtaskStatus.COMPLETED)
                    .result(result)
                    .executionTimeMs(executionTime)
                    .success(true)
                    .criticalError(false)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error ejecutando subtarea {} en paralelo: {}", subtask.getSubtaskId(), e.getMessage());
            
            return SubtaskExecutionResult.builder()
                    .subtaskId(subtask.getSubtaskId())
                    .action(subtask.getAction())
                    .status(com.intentmanagerms.domain.model.SubtaskStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .success(false)
                    .criticalError(isCriticalError(e))
                    .build();
        }
    }
    
    /**
     * Simula la ejecución de una subtarea.
     */
    private Object simulateSubtaskExecution(Subtask subtask, String conversationSessionId) {
        String action = subtask.getAction();
        
        // Simular tiempo de ejecución variable
        try {
            Thread.sleep(100 + (long) (Math.random() * 500)); // 100-600ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simular resultado basado en la acción
        return java.util.Map.of(
            "action", action,
            "subtask_id", subtask.getSubtaskId(),
            "status", "completada",
            "message", "Acción '" + action + "' ejecutada exitosamente en paralelo"
        );
    }
    
    /**
     * Maneja timeout en ejecución paralela.
     */
    private List<SubtaskExecutionResult> handleTimeout(List<Subtask> subtasks, 
                                                      List<CompletableFuture<SubtaskExecutionResult>> futures) {
        List<SubtaskExecutionResult> results = new java.util.ArrayList<>();
        
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<SubtaskExecutionResult> future = futures.get(i);
            Subtask subtask = subtasks.get(i);
            
            if (future.isDone()) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    results.add(createTimeoutResult(subtask, e.getMessage()));
                }
            } else {
                future.cancel(true);
                results.add(createTimeoutResult(subtask, "Timeout en ejecución paralela"));
            }
        }
        
        return results;
    }
    
    /**
     * Maneja errores en ejecución paralela.
     */
    private List<SubtaskExecutionResult> handleExecutionError(List<Subtask> subtasks,
                                                             List<CompletableFuture<SubtaskExecutionResult>> futures,
                                                             Exception error) {
        List<SubtaskExecutionResult> results = new java.util.ArrayList<>();
        
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<SubtaskExecutionResult> future = futures.get(i);
            Subtask subtask = subtasks.get(i);
            
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
     * Crea un resultado de timeout.
     */
    private SubtaskExecutionResult createTimeoutResult(Subtask subtask, String errorMessage) {
        return SubtaskExecutionResult.builder()
                .subtaskId(subtask.getSubtaskId())
                .action(subtask.getAction())
                .status(com.intentmanagerms.domain.model.SubtaskStatus.TIMEOUT)
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
                .status(com.intentmanagerms.domain.model.SubtaskStatus.FAILED)
                .errorMessage(errorMessage)
                .executionTimeMs(0L)
                .success(false)
                .criticalError(false)
                .build();
    }
    
    /**
     * Determina si un error es crítico.
     */
    private boolean isCriticalError(Exception error) {
        String errorMessage = error.getMessage().toLowerCase();
        return errorMessage.contains("timeout") || 
               errorMessage.contains("connection") || 
               errorMessage.contains("authentication") ||
               errorMessage.contains("unauthorized");
    }
    
    /**
     * Obtiene estadísticas del ejecutor paralelo.
     */
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            stats.put("active_threads", tpe.getActiveCount());
            stats.put("pool_size", tpe.getPoolSize());
            stats.put("core_pool_size", tpe.getCorePoolSize());
            stats.put("max_pool_size", tpe.getMaximumPoolSize());
            stats.put("queue_size", tpe.getQueue().size());
            stats.put("completed_tasks", tpe.getCompletedTaskCount());
        }
        
        stats.put("max_threads", maxThreads);
        stats.put("timeout_seconds", timeoutSeconds);
        stats.put("enable_thread_pool", enableThreadPool);
        
        return stats;
    }
    
    /**
     * Cierra el ejecutor paralelo.
     */
    public void shutdown() {
        logger.info("Cerrando ParallelExecutor");
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