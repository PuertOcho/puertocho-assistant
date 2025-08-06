package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Servicio principal para la descomposición dinámica de subtareas.
 * Analiza peticiones complejas usando LLM y las descompone en múltiples subtareas ejecutables.
 */
@Service
public class DynamicSubtaskDecomposer {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicSubtaskDecomposer.class);
    
    @Autowired
    private TaskAnalyzer taskAnalyzer;
    
    @Autowired
    private DependencyResolver dependencyResolver;
    
    @Autowired
    private TaskValidator taskValidator;
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    @Autowired
    private McpActionRegistry mcpActionRegistry;
    
    @Autowired
    private ConversationManager conversationManager;
    
    @Value("${task.decomposition.enable-llm-analysis:true}")
    private Boolean enableLlmAnalysis;
    
    @Value("${task.decomposition.max-subtasks-per-request:10}")
    private Integer maxSubtasksPerRequest;
    
    @Value("${task.decomposition.enable-dependency-detection:true}")
    private Boolean enableDependencyDetection;
    
    @Value("${task.decomposition.enable-priority-assignment:true}")
    private Boolean enablePriorityAssignment;
    
    @Value("${task.decomposition.enable-parallel-execution:true}")
    private Boolean enableParallelExecution;
    
    @Value("${task.decomposition.confidence-threshold:0.7}")
    private Double confidenceThreshold;
    
    @Value("${task.decomposition.max-processing-time-ms:10000}")
    private Long maxProcessingTimeMs;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    
    /**
     * Descompone una petición compleja en múltiples subtareas ejecutables.
     */
    public SubtaskDecompositionResult decomposeRequest(SubtaskDecompositionRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Iniciando descomposición de petición: {}", request.getUserMessage());
        
        try {
            // Validar entrada
            validateRequest(request);
            
            // Generar ID único para la solicitud
            String requestId = generateRequestId();
            request.setRequestId(requestId);
            
            // Analizar la petición usando LLM
            List<Subtask> subtasks = new ArrayList<>();
            List<String> extractionMethodsUsed = new ArrayList<>();
            
            if (enableLlmAnalysis) {
                logger.debug("Analizando petición con LLM");
                List<Subtask> llmSubtasks = taskAnalyzer.analyzeWithLLM(request);
                subtasks.addAll(llmSubtasks);
                extractionMethodsUsed.add("llm_analysis");
            }
            
            // Análisis basado en patrones si es necesario
            if (subtasks.isEmpty() || subtasks.size() < 2) {
                logger.debug("Complementando con análisis de patrones");
                List<Subtask> patternSubtasks = taskAnalyzer.analyzeWithPatterns(request);
                subtasks.addAll(patternSubtasks);
                extractionMethodsUsed.add("pattern_analysis");
            }
            
            // Limitar número de subtareas
            if (subtasks.size() > maxSubtasksPerRequest) {
                logger.warn("Limitando subtareas de {} a {}", subtasks.size(), maxSubtasksPerRequest);
                subtasks = subtasks.subList(0, maxSubtasksPerRequest);
            }
            
            // Asignar IDs únicos a las subtareas
            assignSubtaskIds(subtasks);
            
            // Detectar dependencias si está habilitado
            if (enableDependencyDetection && subtasks.size() > 1) {
                logger.debug("Detectando dependencias entre subtareas");
                dependencyResolver.detectDependencies(subtasks, request.getContext());
            }
            
            // Asignar prioridades si está habilitado
            if (enablePriorityAssignment) {
                logger.debug("Asignando prioridades a subtareas");
                assignPriorities(subtasks);
            }
            
            // Validar subtareas generadas
            List<Subtask> validSubtasks = taskValidator.validateSubtasks(subtasks, request);
            
            // Crear plan de ejecución
            SubtaskDecompositionResult.ExecutionPlan executionPlan = createExecutionPlan(validSubtasks, request);
            
            // Calcular estadísticas
            Map<String, Object> statistics = calculateStatistics(validSubtasks, startTime);
            
            // Crear resultado
            SubtaskDecompositionResult result = new SubtaskDecompositionResult(
                    requestId, 
                    request.getConversationSessionId(), 
                    request.getUserMessage()
            );
            
            result.setSubtasks(validSubtasks);
            result.setExecutionPlan(executionPlan);
            result.setDecompositionConfidence(calculateDecompositionConfidence(validSubtasks));
            result.setTotalEstimatedDurationMs(calculateTotalEstimatedDuration(validSubtasks));
            result.setCanExecuteParallel(enableParallelExecution);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            result.setExtractionMethodsUsed(extractionMethodsUsed);
            result.setDependenciesDetected(enableDependencyDetection && hasDependencies(validSubtasks));
            result.setPrioritiesAssigned(enablePriorityAssignment);
            result.setStatistics(statistics);
            
            logger.info("Descomposición completada: {} subtareas generadas en {}ms", 
                    validSubtasks.size(), result.getProcessingTimeMs());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error durante la descomposición de subtareas", e);
            throw new RuntimeException("Error en descomposición de subtareas: " + e.getMessage(), e);
        }
    }
    
    /**
     * Descomposición asíncrona para peticiones complejas.
     */
    public CompletableFuture<SubtaskDecompositionResult> decomposeRequestAsync(SubtaskDecompositionRequest request) {
        return CompletableFuture.supplyAsync(() -> decomposeRequest(request), executorService);
    }
    
    /**
     * Valida la solicitud de descomposición.
     */
    private void validateRequest(SubtaskDecompositionRequest request) {
        if (request.getUserMessage() == null || request.getUserMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje del usuario no puede estar vacío");
        }
        
        if (request.getConversationSessionId() == null || request.getConversationSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de sesión de conversación es requerido");
        }
        
        if (request.getMaxSubtasks() != null && request.getMaxSubtasks() > maxSubtasksPerRequest) {
            logger.warn("MaxSubtasks reducido de {} a {}", request.getMaxSubtasks(), maxSubtasksPerRequest);
            request.setMaxSubtasks(maxSubtasksPerRequest);
        }
    }
    
    /**
     * Genera un ID único para la solicitud.
     */
    private String generateRequestId() {
        return "decomp_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Asigna IDs únicos a las subtareas.
     */
    private void assignSubtaskIds(List<Subtask> subtasks) {
        for (int i = 0; i < subtasks.size(); i++) {
            Subtask subtask = subtasks.get(i);
            if (subtask.getSubtaskId() == null) {
                subtask.setSubtaskId("task_" + (i + 1) + "_" + UUID.randomUUID().toString().substring(0, 8));
            }
        }
    }
    
    /**
     * Asigna prioridades a las subtareas basándose en su importancia.
     */
    private void assignPriorities(List<Subtask> subtasks) {
        // Prioridades: high, medium, low
        String[] priorities = {"high", "medium", "low"};
        
        for (Subtask subtask : subtasks) {
            // Determinar prioridad basada en la acción y contexto
            String priority = determinePriority(subtask);
            subtask.setPriority(priority);
        }
    }
    
    /**
     * Determina la prioridad de una subtarea basándose en su acción.
     */
    private String determinePriority(Subtask subtask) {
        String action = subtask.getAction().toLowerCase();
        
        // Acciones de alta prioridad
        if (action.contains("emergency") || action.contains("urgent") || 
            action.contains("alarm") || action.contains("alert")) {
            return "high";
        }
        
        // Acciones de prioridad media
        if (action.contains("query") || action.contains("search") || 
            action.contains("get") || action.contains("find")) {
            return "medium";
        }
        
        // Acciones de baja prioridad (configuración, reportes, etc.)
        return "low";
    }
    
    /**
     * Crea un plan de ejecución para las subtareas.
     */
    private SubtaskDecompositionResult.ExecutionPlan createExecutionPlan(List<Subtask> subtasks, 
                                                                        SubtaskDecompositionRequest request) {
        SubtaskDecompositionResult.ExecutionPlan plan = new SubtaskDecompositionResult.ExecutionPlan();
        plan.setPlanId("plan_" + UUID.randomUUID().toString().substring(0, 8));
        
        // Crear pasos de ejecución basados en dependencias
        List<SubtaskDecompositionResult.ExecutionStep> steps = createExecutionSteps(subtasks);
        plan.setExecutionSteps(steps);
        
        // Crear grupos paralelos si está habilitado
        if (enableParallelExecution) {
            List<List<String>> parallelGroups = createParallelGroups(subtasks);
            plan.setParallelGroups(parallelGroups);
        }
        
        // Calcular duración estimada total
        long totalDuration = calculateTotalEstimatedDuration(subtasks);
        plan.setEstimatedTotalDurationMs(totalDuration);
        
        return plan;
    }
    
    /**
     * Crea pasos de ejecución basados en las dependencias de las subtareas.
     */
    private List<SubtaskDecompositionResult.ExecutionStep> createExecutionSteps(List<Subtask> subtasks) {
        List<SubtaskDecompositionResult.ExecutionStep> steps = new ArrayList<>();
        
        // Agrupar subtareas por nivel de dependencia
        Map<Integer, List<Subtask>> dependencyLevels = groupByDependencyLevel(subtasks);
        
        int stepOrder = 1;
        for (Map.Entry<Integer, List<Subtask>> entry : dependencyLevels.entrySet()) {
            List<Subtask> levelSubtasks = entry.getValue();
            
            SubtaskDecompositionResult.ExecutionStep step = new SubtaskDecompositionResult.ExecutionStep();
            step.setStepId("step_" + stepOrder);
            step.setStepOrder(stepOrder);
            step.setSubtaskIds(levelSubtasks.stream()
                    .map(Subtask::getSubtaskId)
                    .collect(Collectors.toList()));
            
            // Determinar tipo de ejecución
            if (levelSubtasks.size() > 1 && enableParallelExecution) {
                step.setExecutionType("parallel");
                step.setDescription("Ejecutar " + levelSubtasks.size() + " subtareas en paralelo");
            } else {
                step.setExecutionType("sequential");
                step.setDescription("Ejecutar " + levelSubtasks.size() + " subtareas secuencialmente");
            }
            
            // Calcular duración estimada del paso
            long stepDuration = levelSubtasks.stream()
                    .mapToLong(subtask -> subtask.getEstimatedDurationMs() != null ? 
                            subtask.getEstimatedDurationMs() : 1000L)
                    .max()
                    .orElse(1000L);
            step.setEstimatedDurationMs(stepDuration);
            
            steps.add(step);
            stepOrder++;
        }
        
        return steps;
    }
    
    /**
     * Agrupa las subtareas por nivel de dependencia.
     */
    private Map<Integer, List<Subtask>> groupByDependencyLevel(List<Subtask> subtasks) {
        Map<Integer, List<Subtask>> levels = new HashMap<>();
        
        // Nivel 0: subtareas sin dependencias
        List<Subtask> level0 = subtasks.stream()
                .filter(subtask -> subtask.getDependencies() == null || subtask.getDependencies().isEmpty())
                .collect(Collectors.toList());
        levels.put(0, level0);
        
        // Niveles superiores: subtareas con dependencias
        Set<String> processedIds = new HashSet<>();
        level0.forEach(subtask -> processedIds.add(subtask.getSubtaskId()));
        
        int currentLevel = 1;
        while (processedIds.size() < subtasks.size()) {
            List<Subtask> currentLevelSubtasks = subtasks.stream()
                    .filter(subtask -> !processedIds.contains(subtask.getSubtaskId()))
                    .filter(subtask -> subtask.getDependencies() != null && 
                            subtask.getDependencies().stream().allMatch(processedIds::contains))
                    .collect(Collectors.toList());
            
            if (currentLevelSubtasks.isEmpty()) {
                break; // Evitar bucle infinito
            }
            
            levels.put(currentLevel, currentLevelSubtasks);
            currentLevelSubtasks.forEach(subtask -> processedIds.add(subtask.getSubtaskId()));
            currentLevel++;
        }
        
        return levels;
    }
    
    /**
     * Crea grupos de subtareas que pueden ejecutarse en paralelo.
     */
    private List<List<String>> createParallelGroups(List<Subtask> subtasks) {
        List<List<String>> groups = new ArrayList<>();
        
        // Agrupar subtareas que no tienen dependencias entre sí
        List<Subtask> independentSubtasks = subtasks.stream()
                .filter(subtask -> subtask.getDependencies() == null || subtask.getDependencies().isEmpty())
                .collect(Collectors.toList());
        
        if (!independentSubtasks.isEmpty()) {
            List<String> group1 = independentSubtasks.stream()
                    .map(Subtask::getSubtaskId)
                    .collect(Collectors.toList());
            groups.add(group1);
        }
        
        // Agrupar subtareas dependientes por nivel
        Map<Integer, List<Subtask>> dependencyLevels = groupByDependencyLevel(subtasks);
        for (int level = 1; level < dependencyLevels.size(); level++) {
            List<Subtask> levelSubtasks = dependencyLevels.get(level);
            if (levelSubtasks != null && !levelSubtasks.isEmpty()) {
                List<String> group = levelSubtasks.stream()
                        .map(Subtask::getSubtaskId)
                        .collect(Collectors.toList());
                groups.add(group);
            }
        }
        
        return groups;
    }
    
    /**
     * Calcula la duración estimada total de todas las subtareas.
     */
    private long calculateTotalEstimatedDuration(List<Subtask> subtasks) {
        return subtasks.stream()
                .mapToLong(subtask -> subtask.getEstimatedDurationMs() != null ? 
                        subtask.getEstimatedDurationMs() : 1000L)
                .sum();
    }
    
    /**
     * Calcula la confianza de la descomposición basándose en las subtareas generadas.
     */
    private double calculateDecompositionConfidence(List<Subtask> subtasks) {
        if (subtasks.isEmpty()) {
            return 0.0;
        }
        
        // Promedio de confianza de todas las subtareas
        double avgConfidence = subtasks.stream()
                .mapToDouble(subtask -> subtask.getConfidenceScore() != null ? 
                        subtask.getConfidenceScore() : 0.5)
                .average()
                .orElse(0.5);
        
        // Factor de penalización por número de subtareas (más subtareas = más complejidad)
        double complexityFactor = Math.max(0.1, 1.0 - (subtasks.size() - 1) * 0.05);
        
        return Math.min(1.0, avgConfidence * complexityFactor);
    }
    
    /**
     * Verifica si las subtareas tienen dependencias.
     */
    private boolean hasDependencies(List<Subtask> subtasks) {
        return subtasks.stream()
                .anyMatch(subtask -> subtask.getDependencies() != null && !subtask.getDependencies().isEmpty());
    }
    
    /**
     * Calcula estadísticas del proceso de descomposición.
     */
    private Map<String, Object> calculateStatistics(List<Subtask> subtasks, long startTime) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_subtasks", subtasks.size());
        stats.put("high_priority_subtasks", subtasks.stream()
                .filter(s -> "high".equals(s.getPriority()))
                .count());
        stats.put("medium_priority_subtasks", subtasks.stream()
                .filter(s -> "medium".equals(s.getPriority()))
                .count());
        stats.put("low_priority_subtasks", subtasks.stream()
                .filter(s -> "low".equals(s.getPriority()))
                .count());
        stats.put("subtasks_with_dependencies", subtasks.stream()
                .filter(s -> s.getDependencies() != null && !s.getDependencies().isEmpty())
                .count());
        stats.put("parallel_executable_subtasks", subtasks.stream()
                .filter(Subtask::getCanExecuteParallel)
                .count());
        stats.put("average_confidence_score", subtasks.stream()
                .mapToDouble(s -> s.getConfidenceScore() != null ? s.getConfidenceScore() : 0.5)
                .average()
                .orElse(0.0));
        stats.put("processing_time_ms", System.currentTimeMillis() - startTime);
        stats.put("created_at", LocalDateTime.now());
        
        return stats;
    }
    
    /**
     * Cierra el executor service al destruir el bean.
     */
    public void shutdown() {
        executorService.shutdown();
    }
} 