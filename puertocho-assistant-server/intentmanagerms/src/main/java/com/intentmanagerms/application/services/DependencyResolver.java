package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.Subtask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para detectar y resolver dependencias entre subtareas.
 * Analiza las relaciones entre acciones y determina el orden de ejecución.
 */
@Service
public class DependencyResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyResolver.class);
    
    @Value("${dependency.resolver.enable-semantic-analysis:true}")
    private Boolean enableSemanticAnalysis;
    
    @Value("${dependency.resolver.enable-pattern-analysis:true}")
    private Boolean enablePatternAnalysis;
    
    @Value("${dependency.resolver.confidence-threshold:0.7}")
    private Double confidenceThreshold;
    
    // Patrones para detectar dependencias semánticas
    private static final Map<String, List<String>> DEPENDENCY_PATTERNS = new HashMap<>();
    
    static {
        // Dependencias basadas en acciones
        DEPENDENCY_PATTERNS.put("consultar_tiempo", Arrays.asList("programar_alarma_condicional", "ajustar_temperatura"));
        DEPENDENCY_PATTERNS.put("buscar_informacion", Arrays.asList("crear_resumen", "enviar_notificacion"));
        DEPENDENCY_PATTERNS.put("crear_issue", Arrays.asList("asignar_issue", "notificar_equipo"));
        DEPENDENCY_PATTERNS.put("obtener_datos", Arrays.asList("procesar_datos", "generar_reporte"));
        
        // Dependencias basadas en entidades
        DEPENDENCY_PATTERNS.put("autenticar_usuario", Arrays.asList("acceder_recurso", "modificar_configuracion"));
        DEPENDENCY_PATTERNS.put("verificar_permisos", Arrays.asList("ejecutar_accion", "modificar_datos"));
    }
    
    // Patrones de texto para detectar dependencias
    private static final List<Pattern> DEPENDENCY_TEXT_PATTERNS = Arrays.asList(
        Pattern.compile("(?:si|cuando|después de|una vez que|cuando se complete).*?(?:entonces|luego|después)", 
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:primero|antes de|previamente).*?(?:después|luego|entonces)", 
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:depende de|requiere|necesita).*?(?:para|antes de|con el fin de)", 
            Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Detecta dependencias entre subtareas usando múltiples métodos.
     */
    public void detectDependencies(List<Subtask> subtasks, Map<String, Object> context) {
        if (subtasks.size() < 2) {
            logger.debug("No hay suficientes subtareas para detectar dependencias");
            return;
        }
        
        logger.debug("Detectando dependencias entre {} subtareas", subtasks.size());
        
        // Detectar dependencias basadas en patrones de acciones
        if (enablePatternAnalysis) {
            detectActionBasedDependencies(subtasks);
        }
        
        // Detectar dependencias basadas en análisis semántico
        if (enableSemanticAnalysis) {
            detectSemanticDependencies(subtasks, context);
        }
        
        // Detectar dependencias basadas en entidades compartidas
        detectEntityBasedDependencies(subtasks);
        
        // Validar dependencias y detectar ciclos
        validateDependencies(subtasks);
        
        logger.debug("Detección de dependencias completada");
    }
    
    /**
     * Detecta dependencias basadas en patrones de acciones predefinidos.
     */
    private void detectActionBasedDependencies(List<Subtask> subtasks) {
        logger.debug("Detectando dependencias basadas en acciones");
        
        for (Subtask subtask : subtasks) {
            String action = subtask.getAction();
            List<String> dependentActions = DEPENDENCY_PATTERNS.get(action);
            
            if (dependentActions != null) {
                for (String dependentAction : dependentActions) {
                    // Buscar subtareas que dependen de esta acción
                    for (Subtask dependentSubtask : subtasks) {
                        if (dependentSubtask.getAction().equals(dependentAction)) {
                            addDependency(dependentSubtask, subtask.getSubtaskId());
                            logger.debug("Dependencia detectada: {} -> {}", action, dependentAction);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Detecta dependencias basadas en análisis semántico del contexto.
     */
    private void detectSemanticDependencies(List<Subtask> subtasks, Map<String, Object> context) {
        logger.debug("Detectando dependencias semánticas");
        
        // Analizar el contexto conversacional para detectar dependencias implícitas
        if (context != null && context.containsKey("conversation_history")) {
            String conversationHistory = context.get("conversation_history").toString();
            
            for (Subtask subtask : subtasks) {
                // Buscar referencias a acciones previas en el historial
                List<String> referencedActions = findReferencedActions(conversationHistory, subtask);
                
                for (String referencedAction : referencedActions) {
                    // Buscar la subtarea correspondiente
                    for (Subtask referencedSubtask : subtasks) {
                        if (referencedSubtask.getAction().equals(referencedAction) && 
                            !referencedSubtask.getSubtaskId().equals(subtask.getSubtaskId())) {
                            addDependency(subtask, referencedSubtask.getSubtaskId());
                            logger.debug("Dependencia semántica detectada: {} -> {}", 
                                    subtask.getAction(), referencedAction);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Detecta dependencias basadas en entidades compartidas entre subtareas.
     */
    private void detectEntityBasedDependencies(List<Subtask> subtasks) {
        logger.debug("Detectando dependencias basadas en entidades");
        
        for (int i = 0; i < subtasks.size(); i++) {
            Subtask subtask1 = subtasks.get(i);
            Map<String, Object> entities1 = subtask1.getEntities();
            
            if (entities1 == null || entities1.isEmpty()) {
                continue;
            }
            
            for (int j = i + 1; j < subtasks.size(); j++) {
                Subtask subtask2 = subtasks.get(j);
                Map<String, Object> entities2 = subtask2.getEntities();
                
                if (entities2 == null || entities2.isEmpty()) {
                    continue;
                }
                
                // Verificar si comparten entidades críticas
                if (shareCriticalEntities(entities1, entities2)) {
                    // Determinar el orden basado en el tipo de acción
                    String dependencyOrder = determineDependencyOrder(subtask1, subtask2);
                    
                    if ("first_second".equals(dependencyOrder)) {
                        addDependency(subtask2, subtask1.getSubtaskId());
                        logger.debug("Dependencia por entidades: {} -> {}", 
                                subtask1.getAction(), subtask2.getAction());
                    } else if ("second_first".equals(dependencyOrder)) {
                        addDependency(subtask1, subtask2.getSubtaskId());
                        logger.debug("Dependencia por entidades: {} -> {}", 
                                subtask2.getAction(), subtask1.getAction());
                    }
                }
            }
        }
    }
    
    /**
     * Verifica si dos conjuntos de entidades comparten entidades críticas.
     */
    private boolean shareCriticalEntities(Map<String, Object> entities1, Map<String, Object> entities2) {
        Set<String> criticalEntityTypes = Set.of("ubicacion", "usuario", "recurso", "archivo", "sesion");
        
        for (String entityType : criticalEntityTypes) {
            if (entities1.containsKey(entityType) && entities2.containsKey(entityType)) {
                Object value1 = entities1.get(entityType);
                Object value2 = entities2.get(entityType);
                
                if (value1 != null && value2 != null && value1.equals(value2)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Determina el orden de dependencia entre dos subtareas basándose en sus tipos de acción.
     */
    private String determineDependencyOrder(Subtask subtask1, Subtask subtask2) {
        String action1 = subtask1.getAction();
        String action2 = subtask2.getAction();
        
        // Reglas de precedencia
        Map<String, Integer> actionPrecedence = new HashMap<>();
        actionPrecedence.put("autenticar", 1);
        actionPrecedence.put("verificar", 1);
        actionPrecedence.put("consultar", 2);
        actionPrecedence.put("obtener", 2);
        actionPrecedence.put("buscar", 2);
        actionPrecedence.put("procesar", 3);
        actionPrecedence.put("crear", 3);
        actionPrecedence.put("modificar", 4);
        actionPrecedence.put("enviar", 4);
        actionPrecedence.put("notificar", 5);
        
        int precedence1 = getActionPrecedence(action1, actionPrecedence);
        int precedence2 = getActionPrecedence(action2, actionPrecedence);
        
        if (precedence1 < precedence2) {
            return "first_second"; // subtask1 debe ejecutarse antes que subtask2
        } else if (precedence2 < precedence1) {
            return "second_first"; // subtask2 debe ejecutarse antes que subtask1
        }
        
        return "no_dependency"; // No hay dependencia clara
    }
    
    /**
     * Obtiene la precedencia de una acción.
     */
    private int getActionPrecedence(String action, Map<String, Integer> actionPrecedence) {
        for (Map.Entry<String, Integer> entry : actionPrecedence.entrySet()) {
            if (action.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 999; // Precedencia baja por defecto
    }
    
    /**
     * Encuentra acciones referenciadas en el historial de conversación.
     */
    private List<String> findReferencedActions(String conversationHistory, Subtask subtask) {
        List<String> referencedActions = new ArrayList<>();
        
        // Buscar referencias a la acción actual
        String action = subtask.getAction();
        Pattern referencePattern = Pattern.compile(
            "(?:después de|cuando se complete|una vez que termine).*?" + action, 
            Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = referencePattern.matcher(conversationHistory);
        while (matcher.find()) {
            // Extraer la acción que debe completarse primero
            String beforeText = conversationHistory.substring(0, matcher.start());
            String[] words = beforeText.split("\\s+");
            if (words.length > 0) {
                String potentialAction = words[words.length - 1];
                if (isValidAction(potentialAction)) {
                    referencedActions.add(potentialAction);
                }
            }
        }
        
        return referencedActions;
    }
    
    /**
     * Verifica si una cadena representa una acción válida.
     */
    private boolean isValidAction(String action) {
        // Lista de acciones válidas (en implementación real, obtener del registro MCP)
        Set<String> validActions = Set.of(
            "consultar_tiempo", "programar_alarma", "encender_luz", "reproducir_musica",
            "crear_issue", "actualizar_estado", "buscar_informacion", "autenticar_usuario"
        );
        
        return validActions.contains(action);
    }
    
    /**
     * Agrega una dependencia a una subtarea.
     */
    private void addDependency(Subtask subtask, String dependencyId) {
        if (subtask.getDependencies() == null) {
            subtask.setDependencies(new ArrayList<>());
        }
        
        // Evitar dependencias duplicadas
        if (!subtask.getDependencies().contains(dependencyId)) {
            subtask.getDependencies().add(dependencyId);
            logger.debug("Dependencia agregada: {} -> {}", subtask.getSubtaskId(), dependencyId);
        }
    }
    
    /**
     * Valida las dependencias y detecta ciclos.
     */
    private void validateDependencies(List<Subtask> subtasks) {
        logger.debug("Validando dependencias y detectando ciclos");
        
        // Crear mapa de subtareas para búsqueda rápida
        Map<String, Subtask> subtaskMap = new HashMap<>();
        for (Subtask subtask : subtasks) {
            subtaskMap.put(subtask.getSubtaskId(), subtask);
        }
        
        // Detectar ciclos usando DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (Subtask subtask : subtasks) {
            if (!visited.contains(subtask.getSubtaskId())) {
                if (hasCycle(subtask, subtaskMap, visited, recursionStack)) {
                    logger.warn("Ciclo detectado en dependencias, removiendo dependencias problemáticas");
                    removeCyclicDependencies(subtasks);
                    break;
                }
            }
        }
        
        // Validar que todas las dependencias referencien subtareas existentes
        validateDependencyReferences(subtasks, subtaskMap);
    }
    
    /**
     * Detecta ciclos en las dependencias usando DFS.
     */
    private boolean hasCycle(Subtask subtask, Map<String, Subtask> subtaskMap, 
                           Set<String> visited, Set<String> recursionStack) {
        String subtaskId = subtask.getSubtaskId();
        
        if (recursionStack.contains(subtaskId)) {
            return true; // Ciclo detectado
        }
        
        if (visited.contains(subtaskId)) {
            return false; // Ya visitado, no hay ciclo
        }
        
        visited.add(subtaskId);
        recursionStack.add(subtaskId);
        
        if (subtask.getDependencies() != null) {
            for (String dependencyId : subtask.getDependencies()) {
                Subtask dependency = subtaskMap.get(dependencyId);
                if (dependency != null && hasCycle(dependency, subtaskMap, visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(subtaskId);
        return false;
    }
    
    /**
     * Remueve dependencias cíclicas para evitar deadlocks.
     */
    private void removeCyclicDependencies(List<Subtask> subtasks) {
        // Estrategia simple: remover dependencias de subtareas con menor confianza
        subtasks.sort((s1, s2) -> {
            double conf1 = s1.getConfidenceScore() != null ? s1.getConfidenceScore() : 0.5;
            double conf2 = s2.getConfidenceScore() != null ? s2.getConfidenceScore() : 0.5;
            return Double.compare(conf1, conf2);
        });
        
        // Remover dependencias de las primeras subtareas (menor confianza)
        for (int i = 0; i < subtasks.size() / 2; i++) {
            subtasks.get(i).setDependencies(new ArrayList<>());
            logger.debug("Dependencias removidas de subtarea: {}", subtasks.get(i).getSubtaskId());
        }
    }
    
    /**
     * Valida que todas las dependencias referencien subtareas existentes.
     */
    private void validateDependencyReferences(List<Subtask> subtasks, Map<String, Subtask> subtaskMap) {
        for (Subtask subtask : subtasks) {
            if (subtask.getDependencies() != null) {
                List<String> validDependencies = new ArrayList<>();
                
                for (String dependencyId : subtask.getDependencies()) {
                    if (subtaskMap.containsKey(dependencyId)) {
                        validDependencies.add(dependencyId);
                    } else {
                        logger.warn("Dependencia inválida removida: {} -> {}", 
                                subtask.getSubtaskId(), dependencyId);
                    }
                }
                
                subtask.setDependencies(validDependencies);
            }
        }
    }
    
    /**
     * Obtiene el orden topológico de las subtareas basándose en sus dependencias.
     */
    public List<String> getTopologicalOrder(List<Subtask> subtasks) {
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> temp = new HashSet<>();
        
        Map<String, Subtask> subtaskMap = new HashMap<>();
        for (Subtask subtask : subtasks) {
            subtaskMap.put(subtask.getSubtaskId(), subtask);
        }
        
        for (Subtask subtask : subtasks) {
            if (!visited.contains(subtask.getSubtaskId())) {
                topologicalSort(subtask, subtaskMap, visited, temp, order);
            }
        }
        
        Collections.reverse(order);
        return order;
    }
    
    /**
     * Algoritmo de ordenamiento topológico recursivo.
     */
    private void topologicalSort(Subtask subtask, Map<String, Subtask> subtaskMap, 
                               Set<String> visited, Set<String> temp, List<String> order) {
        String subtaskId = subtask.getSubtaskId();
        
        if (temp.contains(subtaskId)) {
            return; // Ya en proceso
        }
        
        if (visited.contains(subtaskId)) {
            return; // Ya visitado
        }
        
        temp.add(subtaskId);
        
        if (subtask.getDependencies() != null) {
            for (String dependencyId : subtask.getDependencies()) {
                Subtask dependency = subtaskMap.get(dependencyId);
                if (dependency != null) {
                    topologicalSort(dependency, subtaskMap, visited, temp, order);
                }
            }
        }
        
        temp.remove(subtaskId);
        visited.add(subtaskId);
        order.add(subtaskId);
    }
} 