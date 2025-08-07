package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.ExecutionPlan;
import com.intentmanagerms.domain.model.Subtask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor de dependencias entre subtareas.
 * Crea planes de ejecución optimizados basados en las dependencias detectadas.
 */
@Service
public class DependencyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyManager.class);
    
    /**
     * Crea un plan de ejecución basado en las dependencias de las subtareas.
     */
    public ExecutionPlan createExecutionPlan(List<Subtask> subtasks) {
        logger.debug("Creando plan de ejecución para {} subtareas", subtasks.size());
        
        try {
            // Validar subtareas
            validateSubtasks(subtasks);
            
            // Crear grafo de dependencias
            Map<String, List<String>> dependencyGraph = createDependencyGraph(subtasks);
            
            // Detectar dependencias circulares
            detectCircularDependencies(dependencyGraph);
            
            // Crear niveles de dependencias
            List<List<Subtask>> dependencyLevels = createDependencyLevels(subtasks, dependencyGraph);
            
            // Crear grupos paralelos
            List<List<String>> parallelGroups = createParallelGroups(dependencyLevels);
            
            // Calcular orden de ejecución
            List<String> executionOrder = calculateExecutionOrder(dependencyLevels);
            
            // Calcular ruta crítica
            List<String> criticalPath = calculateCriticalPath(dependencyLevels);
            
            // Calcular duración estimada
            Long estimatedDuration = calculateEstimatedDuration(subtasks, dependencyLevels);
            
            // Crear plan de ejecución
            ExecutionPlan plan = ExecutionPlan.builder()
                    .planId(generatePlanId())
                    .dependencyLevels(dependencyLevels)
                    .parallelGroups(parallelGroups)
                    .executionOrder(executionOrder)
                    .estimatedDurationMs(estimatedDuration)
                    .canExecuteParallel(canExecuteInParallel(dependencyLevels))
                    .dependencyGraph(dependencyGraph)
                    .criticalPath(criticalPath)
                    .optimizationLevel("standard")
                    .metadata(createPlanMetadata(subtasks, dependencyLevels))
                    .build();
            
            logger.info("Plan de ejecución creado: {} niveles, {} grupos paralelos, duración estimada: {}ms", 
                       dependencyLevels.size(), parallelGroups.size(), estimatedDuration);
            
            return plan;
            
        } catch (Exception e) {
            logger.error("Error creando plan de ejecución: {}", e.getMessage(), e);
            throw new RuntimeException("Error creando plan de ejecución: " + e.getMessage(), e);
        }
    }
    
    /**
     * Valida las subtareas antes de crear el plan.
     */
    private void validateSubtasks(List<Subtask> subtasks) {
        logger.info("Validando {} subtareas", subtasks != null ? subtasks.size() : 0);
        
        if (subtasks == null || subtasks.isEmpty()) {
            throw new IllegalArgumentException("La lista de subtareas no puede estar vacía");
        }
        
        // Verificar que todas las subtareas tengan ID
        for (int i = 0; i < subtasks.size(); i++) {
            Subtask subtask = subtasks.get(i);
            logger.info("Validando subtarea {}: id={}, action={}", i, subtask.getSubtaskId(), subtask.getAction());
            
            if (subtask.getSubtaskId() == null || subtask.getSubtaskId().trim().isEmpty()) {
                logger.error("Subtask {} no tiene ID válido: {}", i, subtask.getSubtaskId());
                throw new IllegalArgumentException("Todas las subtareas deben tener un ID válido");
            }
        }
        
        // Verificar IDs únicos
        Set<String> ids = subtasks.stream()
                .map(Subtask::getSubtaskId)
                .collect(Collectors.toSet());
        
        if (ids.size() != subtasks.size()) {
            throw new IllegalArgumentException("Todos los IDs de subtareas deben ser únicos");
        }
    }
    
    /**
     * Crea el grafo de dependencias entre subtareas.
     */
    private Map<String, List<String>> createDependencyGraph(List<Subtask> subtasks) {
        Map<String, List<String>> dependencyGraph = new HashMap<>();
        
        for (Subtask subtask : subtasks) {
            String subtaskId = subtask.getSubtaskId();
            List<String> dependencies = subtask.getDependencies();
            
            if (dependencies != null && !dependencies.isEmpty()) {
                dependencyGraph.put(subtaskId, new ArrayList<>(dependencies));
            } else {
                dependencyGraph.put(subtaskId, new ArrayList<>());
            }
        }
        
        logger.debug("Grafo de dependencias creado: {}", dependencyGraph);
        return dependencyGraph;
    }
    
    /**
     * Detecta dependencias circulares en el grafo.
     */
    private void detectCircularDependencies(Map<String, List<String>> dependencyGraph) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String subtaskId : dependencyGraph.keySet()) {
            if (!visited.contains(subtaskId)) {
                if (hasCircularDependency(subtaskId, dependencyGraph, visited, recursionStack)) {
                    throw new RuntimeException("Dependencia circular detectada en subtarea: " + subtaskId);
                }
            }
        }
        
        logger.debug("No se detectaron dependencias circulares");
    }
    
    /**
     * Verifica si hay dependencia circular usando DFS.
     */
    private boolean hasCircularDependency(String subtaskId, Map<String, List<String>> dependencyGraph, 
                                        Set<String> visited, Set<String> recursionStack) {
        visited.add(subtaskId);
        recursionStack.add(subtaskId);
        
        List<String> dependencies = dependencyGraph.get(subtaskId);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                if (!visited.contains(dependency)) {
                    if (hasCircularDependency(dependency, dependencyGraph, visited, recursionStack)) {
                        return true;
                    }
                } else if (recursionStack.contains(dependency)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(subtaskId);
        return false;
    }
    
    /**
     * Crea niveles de dependencias usando ordenamiento topológico.
     */
    private List<List<Subtask>> createDependencyLevels(List<Subtask> subtasks, Map<String, List<String>> dependencyGraph) {
        Map<String, Subtask> subtaskMap = subtasks.stream()
                .collect(Collectors.toMap(Subtask::getSubtaskId, subtask -> subtask));
        
        Map<String, Integer> inDegree = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        List<List<Subtask>> levels = new ArrayList<>();
        
        // Calcular grados de entrada
        for (String subtaskId : dependencyGraph.keySet()) {
            inDegree.put(subtaskId, 0);
        }
        
        for (List<String> dependencies : dependencyGraph.values()) {
            for (String dependency : dependencies) {
                inDegree.put(dependency, inDegree.getOrDefault(dependency, 0) + 1);
            }
        }
        
        // Añadir subtareas sin dependencias a la cola
        for (String subtaskId : inDegree.keySet()) {
            if (inDegree.get(subtaskId) == 0) {
                queue.offer(subtaskId);
            }
        }
        
        // Procesar niveles
        while (!queue.isEmpty()) {
            List<Subtask> currentLevel = new ArrayList<>();
            int levelSize = queue.size();
            
            for (int i = 0; i < levelSize; i++) {
                String subtaskId = queue.poll();
                Subtask subtask = subtaskMap.get(subtaskId);
                currentLevel.add(subtask);
                
                // Reducir grados de entrada de dependientes
                List<String> dependencies = dependencyGraph.get(subtaskId);
                if (dependencies != null) {
                    for (String dependent : dependencies) {
                        inDegree.put(dependent, inDegree.get(dependent) - 1);
                        if (inDegree.get(dependent) == 0) {
                            queue.offer(dependent);
                        }
                    }
                }
            }
            
            levels.add(currentLevel);
        }
        
        // Verificar que todas las subtareas fueron procesadas
        if (levels.stream().mapToInt(List::size).sum() != subtasks.size()) {
            throw new RuntimeException("No se pudieron procesar todas las subtareas. Posible dependencia circular.");
        }
        
        logger.debug("Niveles de dependencias creados: {}", levels.size());
        return levels;
    }
    
    /**
     * Crea grupos de subtareas que pueden ejecutarse en paralelo.
     */
    private List<List<String>> createParallelGroups(List<List<Subtask>> dependencyLevels) {
        List<List<String>> parallelGroups = new ArrayList<>();
        
        for (List<Subtask> level : dependencyLevels) {
            List<String> group = level.stream()
                    .map(Subtask::getSubtaskId)
                    .collect(Collectors.toList());
            
            if (group.size() > 1) {
                parallelGroups.add(group);
            }
        }
        
        logger.debug("Grupos paralelos creados: {}", parallelGroups.size());
        return parallelGroups;
    }
    
    /**
     * Calcula el orden de ejecución de las subtareas.
     */
    private List<String> calculateExecutionOrder(List<List<Subtask>> dependencyLevels) {
        List<String> executionOrder = new ArrayList<>();
        
        for (List<Subtask> level : dependencyLevels) {
            for (Subtask subtask : level) {
                executionOrder.add(subtask.getSubtaskId());
            }
        }
        
        logger.debug("Orden de ejecución calculado: {} subtareas", executionOrder.size());
        return executionOrder;
    }
    
    /**
     * Calcula la ruta crítica del plan de ejecución.
     */
    private List<String> calculateCriticalPath(List<List<Subtask>> dependencyLevels) {
        List<String> criticalPath = new ArrayList<>();
        
        // Para simplificar, consideramos la ruta crítica como la secuencia de subtareas
        // que deben ejecutarse secuencialmente (una por nivel)
        for (List<Subtask> level : dependencyLevels) {
            if (!level.isEmpty()) {
                // Tomar la subtarea con mayor prioridad o la primera
                Subtask criticalSubtask = level.stream()
                        .max(Comparator.comparing(subtask -> 
                            subtask.getPriority() != null ? subtask.getPriority() : "low"))
                        .orElse(level.get(0));
                
                criticalPath.add(criticalSubtask.getSubtaskId());
            }
        }
        
        logger.debug("Ruta crítica calculada: {} subtareas", criticalPath.size());
        return criticalPath;
    }
    
    /**
     * Calcula la duración estimada del plan de ejecución.
     */
    private Long calculateEstimatedDuration(List<Subtask> subtasks, List<List<Subtask>> dependencyLevels) {
        long totalDuration = 0;
        
        for (List<Subtask> level : dependencyLevels) {
            // Para cada nivel, tomar la duración de la subtarea más larga
            long levelDuration = level.stream()
                    .mapToLong(subtask -> subtask.getEstimatedDurationMs() != null ? 
                        subtask.getEstimatedDurationMs() : 1000) // Default 1 segundo
                    .max()
                    .orElse(1000);
            
            totalDuration += levelDuration;
        }
        
        logger.debug("Duración estimada calculada: {}ms", totalDuration);
        return totalDuration;
    }
    
    /**
     * Determina si el plan puede ejecutarse en paralelo.
     */
    private boolean canExecuteInParallel(List<List<Subtask>> dependencyLevels) {
        // Puede ejecutarse en paralelo si hay al menos un nivel con múltiples subtareas
        return dependencyLevels.stream()
                .anyMatch(level -> level.size() > 1);
    }
    
    /**
     * Crea metadata del plan de ejecución.
     */
    private Map<String, Object> createPlanMetadata(List<Subtask> subtasks, List<List<Subtask>> dependencyLevels) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("total_subtasks", subtasks.size());
        metadata.put("total_levels", dependencyLevels.size());
        metadata.put("max_parallel_tasks", dependencyLevels.stream()
                .mapToInt(List::size)
                .max()
                .orElse(1));
        metadata.put("sequential_levels", dependencyLevels.stream()
                .filter(level -> level.size() == 1)
                .count());
        metadata.put("parallel_levels", dependencyLevels.stream()
                .filter(level -> level.size() > 1)
                .count());
        
        return metadata;
    }
    
    /**
     * Genera un ID único para el plan.
     */
    private String generatePlanId() {
        return "plan_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }
} 