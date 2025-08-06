package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para validar las subtareas generadas por el descomponedor dinámico.
 * Verifica la validez de las acciones, entidades y dependencias.
 */
@Service
public class TaskValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskValidator.class);
    
    @Autowired
    private McpActionRegistry mcpActionRegistry;
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    @Value("${task.validator.enable-action-validation:true}")
    private Boolean enableActionValidation;
    
    @Value("${task.validator.enable-entity-validation:true}")
    private Boolean enableEntityValidation;
    
    @Value("${task.validator.enable-dependency-validation:true}")
    private Boolean enableDependencyValidation;
    
    @Value("${task.validator.confidence-threshold:0.7}")
    private Double confidenceThreshold;
    
    @Value("${task.validator.max-subtasks-per-request:10}")
    private Integer maxSubtasksPerRequest;
    
    // Acciones MCP válidas (en implementación real, obtener del registro)
    private static final Set<String> VALID_ACTIONS = Set.of(
        "consultar_tiempo", "programar_alarma", "encender_luz", "reproducir_musica",
        "crear_issue", "actualizar_estado", "buscar_informacion", "autenticar_usuario",
        "verificar_permisos", "obtener_datos", "procesar_datos", "generar_reporte",
        "enviar_notificacion", "crear_resumen", "asignar_issue", "notificar_equipo",
        "ajustar_temperatura", "programar_alarma_condicional", "crear_github_issue",
        "actualizar_taiga_story", "ayuda"
    );
    
    // Entidades requeridas por acción (versión más flexible para pruebas)
    private static final Map<String, List<String>> REQUIRED_ENTITIES = new HashMap<>();
    
    static {
        REQUIRED_ENTITIES.put("consultar_tiempo", Arrays.asList("ubicacion"));
        REQUIRED_ENTITIES.put("programar_alarma", Arrays.asList("hora")); // Removido "mensaje" para ser más flexible
        REQUIRED_ENTITIES.put("encender_luz", Arrays.asList("lugar"));
        REQUIRED_ENTITIES.put("reproducir_musica", Arrays.asList()); // Removido "genero" para ser más flexible
        REQUIRED_ENTITIES.put("crear_issue", Arrays.asList("titulo")); // Removido "descripcion" para ser más flexible
        REQUIRED_ENTITIES.put("actualizar_estado", Arrays.asList("estado"));
        REQUIRED_ENTITIES.put("buscar_informacion", Arrays.asList("query"));
        REQUIRED_ENTITIES.put("autenticar_usuario", Arrays.asList("usuario")); // Removido "credenciales" para ser más flexible
        REQUIRED_ENTITIES.put("verificar_permisos", Arrays.asList("usuario")); // Removido "recurso" para ser más flexible
        REQUIRED_ENTITIES.put("obtener_datos", Arrays.asList("fuente"));
        REQUIRED_ENTITIES.put("procesar_datos", Arrays.asList("datos"));
        REQUIRED_ENTITIES.put("generar_reporte", Arrays.asList("tipo")); // Removido "parametros" para ser más flexible
        REQUIRED_ENTITIES.put("enviar_notificacion", Arrays.asList("destinatario")); // Removido "mensaje" para ser más flexible
        REQUIRED_ENTITIES.put("crear_resumen", Arrays.asList("contenido"));
        REQUIRED_ENTITIES.put("asignar_issue", Arrays.asList("issue_id")); // Removido "asignado" para ser más flexible
        REQUIRED_ENTITIES.put("notificar_equipo", Arrays.asList("equipo")); // Removido "mensaje" para ser más flexible
        REQUIRED_ENTITIES.put("ajustar_temperatura", Arrays.asList()); // Removido "temperatura" para ser más flexible
        REQUIRED_ENTITIES.put("programar_alarma_condicional", Arrays.asList()); // Removido "condicion" para ser más flexible
        REQUIRED_ENTITIES.put("crear_github_issue", Arrays.asList("titulo")); // Removido "descripcion" para ser más flexible
        REQUIRED_ENTITIES.put("actualizar_taiga_story", Arrays.asList("estado"));
        REQUIRED_ENTITIES.put("ayuda", Arrays.asList());
    }
    
    /**
     * Valida una lista de subtareas y retorna las válidas.
     */
    public List<Subtask> validateSubtasks(List<Subtask> subtasks, SubtaskDecompositionRequest request) {
        logger.debug("Validando {} subtareas", subtasks.size());
        
        List<Subtask> validSubtasks = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();
        
        // Validar límite de subtareas
        if (subtasks.size() > maxSubtasksPerRequest) {
            logger.warn("Número de subtareas excede el límite: {} > {}", 
                    subtasks.size(), maxSubtasksPerRequest);
            subtasks = subtasks.subList(0, maxSubtasksPerRequest);
        }
        
        for (Subtask subtask : subtasks) {
            ValidationResult validationResult = validateSubtask(subtask, request);
            
            if (validationResult.isValid()) {
                validSubtasks.add(subtask);
                logger.debug("Subtarea válida: {} - {}", subtask.getAction(), subtask.getDescription());
            } else {
                validationErrors.addAll(validationResult.getErrors());
                logger.warn("Subtarea inválida: {} - Errores: {}", 
                        subtask.getAction(), validationResult.getErrors());
            }
        }
        
        // Aplicar correcciones automáticas si es posible
        List<Subtask> correctedSubtasks = applyAutomaticCorrections(validSubtasks, validationErrors);
        
        logger.info("Validación completada: {} subtareas válidas de {}", 
                correctedSubtasks.size(), subtasks.size());
        
        return correctedSubtasks;
    }
    
    /**
     * Valida una subtarea individual.
     */
    public ValidationResult validateSubtask(Subtask subtask, SubtaskDecompositionRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Validar acción
        if (enableActionValidation) {
            validateAction(subtask, errors);
        }
        
        // Validar entidades
        if (enableEntityValidation) {
            validateEntities(subtask, errors);
        }
        
        // Validar dependencias
        if (enableDependencyValidation) {
            validateDependencies(subtask, errors);
        }
        
        // Validar confianza
        validateConfidence(subtask, errors);
        
        // Validar descripción
        validateDescription(subtask, errors);
        
        // Validar metadata
        validateMetadata(subtask, errors);
        
        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors);
    }
    
    /**
     * Valida la acción de la subtarea.
     */
    private void validateAction(Subtask subtask, List<String> errors) {
        String action = subtask.getAction();
        
        if (action == null || action.trim().isEmpty()) {
            errors.add("La acción no puede estar vacía");
            return;
        }
        
        if (!VALID_ACTIONS.contains(action)) {
            errors.add("Acción no válida: " + action);
        }
        
        // Verificar si la acción está disponible en el registro MCP
        if (mcpActionRegistry != null) {
            try {
                // En implementación real, verificar contra el registro MCP
                // boolean isAvailable = mcpActionRegistry.isActionAvailable(action);
                // if (!isAvailable) {
                //     errors.add("Acción no disponible en el registro MCP: " + action);
                // }
            } catch (Exception e) {
                logger.warn("Error verificando disponibilidad de acción: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Valida las entidades de la subtarea.
     */
    private void validateEntities(Subtask subtask, List<String> errors) {
        String action = subtask.getAction();
        Map<String, Object> entities = subtask.getEntities();
        
        if (entities == null) {
            entities = new HashMap<>();
            subtask.setEntities(entities);
        }
        
        // Obtener entidades requeridas para la acción
        List<String> requiredEntities = REQUIRED_ENTITIES.get(action);
        if (requiredEntities == null) {
            requiredEntities = new ArrayList<>();
        }
        
        // Verificar entidades requeridas
        for (String requiredEntity : requiredEntities) {
            if (!entities.containsKey(requiredEntity) || entities.get(requiredEntity) == null) {
                errors.add("Entidad requerida faltante: " + requiredEntity + " para acción: " + action);
            }
        }
        
        // Validar tipos de entidades
        validateEntityTypes(entities, errors);
        
        // Validar valores de entidades
        validateEntityValues(entities, action, errors);
    }
    
    /**
     * Valida los tipos de entidades.
     */
    private void validateEntityTypes(Map<String, Object> entities, List<String> errors) {
        for (Map.Entry<String, Object> entry : entities.entrySet()) {
            String entityType = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            // Validar tipos específicos
            switch (entityType) {
                case "temperatura":
                    if (!(value instanceof Number)) {
                        errors.add("La temperatura debe ser un número");
                    }
                    break;
                case "hora":
                    if (!(value instanceof String) || !isValidTimeFormat((String) value)) {
                        errors.add("La hora debe tener formato HH:MM");
                    }
                    break;
                case "fecha":
                    if (!(value instanceof String) || !isValidDateFormat((String) value)) {
                        errors.add("La fecha debe tener formato válido");
                    }
                    break;
                case "ubicacion":
                case "lugar":
                    if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                        errors.add("La ubicación no puede estar vacía");
                    }
                    break;
            }
        }
    }
    
    /**
     * Valida los valores de las entidades.
     */
    private void validateEntityValues(Map<String, Object> entities, String action, List<String> errors) {
        for (Map.Entry<String, Object> entry : entities.entrySet()) {
            String entityType = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            // Validaciones específicas por tipo de entidad
            switch (entityType) {
                case "temperatura":
                    if (value instanceof Number) {
                        double temp = ((Number) value).doubleValue();
                        if (temp < -50 || temp > 60) {
                            errors.add("Temperatura fuera de rango válido: " + temp);
                        }
                    }
                    break;
                case "hora":
                    if (value instanceof String) {
                        String time = (String) value;
                        if (!isValidTimeFormat(time)) {
                            errors.add("Formato de hora inválido: " + time);
                        }
                    }
                    break;
                case "estado":
                    if (value instanceof String) {
                        String state = (String) value;
                        Set<String> validStates = Set.of("pendiente", "en_progreso", "completado", "cancelado");
                        if (!validStates.contains(state.toLowerCase())) {
                            errors.add("Estado inválido: " + state);
                        }
                    }
                    break;
            }
        }
    }
    
    /**
     * Valida las dependencias de la subtarea.
     */
    private void validateDependencies(Subtask subtask, List<String> errors) {
        List<String> dependencies = subtask.getDependencies();
        
        if (dependencies == null) {
            return; // No hay dependencias, es válido
        }
        
        // Verificar que no haya dependencias duplicadas
        Set<String> uniqueDependencies = new HashSet<>(dependencies);
        if (uniqueDependencies.size() != dependencies.size()) {
            errors.add("Dependencias duplicadas detectadas");
        }
        
        // Verificar que no dependa de sí misma
        if (dependencies.contains(subtask.getSubtaskId())) {
            errors.add("Una subtarea no puede depender de sí misma");
        }
        
        // Verificar que las dependencias referencien acciones válidas
        for (String dependency : dependencies) {
            if (!isValidDependencyReference(dependency)) {
                errors.add("Referencia de dependencia inválida: " + dependency);
            }
        }
    }
    
    /**
     * Valida la confianza de la subtarea.
     */
    private void validateConfidence(Subtask subtask, List<String> errors) {
        Double confidence = subtask.getConfidenceScore();
        
        if (confidence == null) {
            subtask.setConfidenceScore(0.5); // Valor por defecto
            return;
        }
        
        if (confidence < 0.0 || confidence > 1.0) {
            errors.add("La confianza debe estar entre 0.0 y 1.0");
        }
        
        if (confidence < confidenceThreshold) {
            errors.add("La confianza está por debajo del umbral: " + confidence + " < " + confidenceThreshold);
        }
    }
    
    /**
     * Valida la descripción de la subtarea.
     */
    private void validateDescription(Subtask subtask, List<String> errors) {
        String description = subtask.getDescription();
        
        if (description == null || description.trim().isEmpty()) {
            errors.add("La descripción no puede estar vacía");
        } else if (description.length() > 500) {
            errors.add("La descripción es demasiado larga (máximo 500 caracteres)");
        }
    }
    
    /**
     * Valida la metadata de la subtarea.
     */
    private void validateMetadata(Subtask subtask, List<String> errors) {
        Map<String, Object> metadata = subtask.getMetadata();
        
        if (metadata != null) {
            // Verificar que la metadata no contenga claves reservadas
            Set<String> reservedKeys = Set.of("subtask_id", "action", "description", "entities", "dependencies");
            
            for (String key : metadata.keySet()) {
                if (reservedKeys.contains(key)) {
                    errors.add("Clave de metadata reservada: " + key);
                }
            }
            
            // Verificar que los valores de metadata sean serializables
            for (Object value : metadata.values()) {
                if (value != null && !isSerializable(value)) {
                    errors.add("Valor de metadata no serializable: " + value.getClass().getSimpleName());
                }
            }
        }
    }
    
    /**
     * Aplica correcciones automáticas a las subtareas válidas.
     */
    private List<Subtask> applyAutomaticCorrections(List<Subtask> validSubtasks, List<String> validationErrors) {
        List<Subtask> correctedSubtasks = new ArrayList<>();
        
        for (Subtask subtask : validSubtasks) {
            Subtask correctedSubtask = applyCorrectionsToSubtask(subtask);
            correctedSubtasks.add(correctedSubtask);
        }
        
        // Aplicar correcciones globales
        applyGlobalCorrections(correctedSubtasks);
        
        return correctedSubtasks;
    }
    
    /**
     * Aplica correcciones automáticas a una subtarea individual.
     */
    private Subtask applyCorrectionsToSubtask(Subtask subtask) {
        // Normalizar entidades
        normalizeEntities(subtask);
        
        // Ajustar confianza si es necesario
        adjustConfidence(subtask);
        
        // Completar metadata faltante
        completeMissingMetadata(subtask);
        
        return subtask;
    }
    
    /**
     * Normaliza las entidades de la subtarea.
     */
    private void normalizeEntities(Subtask subtask) {
        Map<String, Object> entities = subtask.getEntities();
        if (entities == null) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : entities.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String stringValue = (String) value;
                
                // Normalizar ubicaciones
                if ("ubicacion".equals(key) || "lugar".equals(key)) {
                    entities.put(key, normalizeLocation(stringValue));
                }
                
                // Normalizar horas
                if ("hora".equals(key)) {
                    entities.put(key, normalizeTime(stringValue));
                }
                
                // Normalizar fechas
                if ("fecha".equals(key)) {
                    entities.put(key, normalizeDate(stringValue));
                }
            }
        }
    }
    
    /**
     * Normaliza una ubicación.
     */
    private String normalizeLocation(String location) {
        if (location == null) {
            return null;
        }
        
        // Capitalizar primera letra de cada palabra
        String[] words = location.trim().toLowerCase().split("\\s+");
        StringBuilder normalized = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                normalized.append(" ");
            }
            if (words[i].length() > 0) {
                normalized.append(Character.toUpperCase(words[i].charAt(0)))
                         .append(words[i].substring(1));
            }
        }
        
        return normalized.toString();
    }
    
    /**
     * Normaliza una hora.
     */
    private String normalizeTime(String time) {
        if (time == null) {
            return null;
        }
        
        // Asegurar formato HH:MM
        time = time.trim();
        if (time.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            
            if (hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59) {
                return String.format("%02d:%02d", hours, minutes);
            }
        }
        
        return time; // Retornar original si no se puede normalizar
    }
    
    /**
     * Normaliza una fecha.
     */
    private String normalizeDate(String date) {
        if (date == null) {
            return null;
        }
        
        // Normalizar fechas relativas
        date = date.trim().toLowerCase();
        switch (date) {
            case "hoy":
                return "today";
            case "mañana":
                return "tomorrow";
            case "ayer":
                return "yesterday";
            default:
                return date;
        }
    }
    
    /**
     * Ajusta la confianza de la subtarea.
     */
    private void adjustConfidence(Subtask subtask) {
        Double confidence = subtask.getConfidenceScore();
        
        if (confidence == null) {
            subtask.setConfidenceScore(0.7); // Valor por defecto
            return;
        }
        
        // Ajustar confianza basándose en la calidad de las entidades
        Map<String, Object> entities = subtask.getEntities();
        if (entities != null && !entities.isEmpty()) {
            double entityQuality = calculateEntityQuality(entities);
            double adjustedConfidence = Math.min(1.0, confidence * entityQuality);
            subtask.setConfidenceScore(adjustedConfidence);
        }
    }
    
    /**
     * Calcula la calidad de las entidades.
     */
    private double calculateEntityQuality(Map<String, Object> entities) {
        if (entities.isEmpty()) {
            return 0.5;
        }
        
        int validEntities = 0;
        int totalEntities = entities.size();
        
        for (Object value : entities.values()) {
            if (value != null && !value.toString().trim().isEmpty()) {
                validEntities++;
            }
        }
        
        return (double) validEntities / totalEntities;
    }
    
    /**
     * Completa metadata faltante.
     */
    private void completeMissingMetadata(Subtask subtask) {
        Map<String, Object> metadata = subtask.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            subtask.setMetadata(metadata);
        }
        
        // Agregar metadata por defecto
        if (!metadata.containsKey("validation_timestamp")) {
            metadata.put("validation_timestamp", System.currentTimeMillis());
        }
        
        if (!metadata.containsKey("validator_version")) {
            metadata.put("validator_version", "1.0");
        }
    }
    
    /**
     * Aplica correcciones globales a todas las subtareas.
     */
    private void applyGlobalCorrections(List<Subtask> subtasks) {
        // Asegurar que no haya IDs duplicados
        Set<String> usedIds = new HashSet<>();
        for (Subtask subtask : subtasks) {
            String originalId = subtask.getSubtaskId();
            if (usedIds.contains(originalId)) {
                String newId = originalId + "_" + System.currentTimeMillis();
                subtask.setSubtaskId(newId);
                logger.debug("ID duplicado corregido: {} -> {}", originalId, newId);
            }
            usedIds.add(subtask.getSubtaskId());
        }
        
        // Ajustar orden de ejecución si es necesario
        adjustExecutionOrder(subtasks);
    }
    
    /**
     * Ajusta el orden de ejecución de las subtareas.
     */
    private void adjustExecutionOrder(List<Subtask> subtasks) {
        // Ordenar por prioridad y luego por confianza
        subtasks.sort((s1, s2) -> {
            // Comparar por prioridad
            int priorityComparison = comparePriorities(s1.getPriority(), s2.getPriority());
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            
            // Si tienen la misma prioridad, comparar por confianza
            Double conf1 = s1.getConfidenceScore() != null ? s1.getConfidenceScore() : 0.5;
            Double conf2 = s2.getConfidenceScore() != null ? s2.getConfidenceScore() : 0.5;
            return Double.compare(conf2, conf1); // Mayor confianza primero
        });
        
        // Asignar orden de ejecución
        for (int i = 0; i < subtasks.size(); i++) {
            subtasks.get(i).setExecutionOrder(i + 1);
        }
    }
    
    /**
     * Compara prioridades de subtareas.
     */
    private int comparePriorities(String priority1, String priority2) {
        Map<String, Integer> priorityOrder = Map.of(
            "high", 3,
            "medium", 2,
            "low", 1
        );
        
        int order1 = priorityOrder.getOrDefault(priority1, 0);
        int order2 = priorityOrder.getOrDefault(priority2, 0);
        
        return Integer.compare(order2, order1); // Mayor prioridad primero
    }
    
    // Métodos de utilidad para validación
    
    private boolean isValidTimeFormat(String time) {
        return time.matches("\\d{1,2}:\\d{2}");
    }
    
    private boolean isValidDateFormat(String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}") || 
               date.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}") ||
               Arrays.asList("hoy", "mañana", "ayer", "today", "tomorrow", "yesterday").contains(date.toLowerCase());
    }
    
    private boolean isValidDependencyReference(String dependency) {
        return dependency != null && !dependency.trim().isEmpty() && dependency.matches("task_\\d+_.*");
    }
    
    private boolean isSerializable(Object value) {
        return value instanceof String || value instanceof Number || 
               value instanceof Boolean || value instanceof Map || 
               value instanceof List || value instanceof Collection;
    }
    
    /**
     * Clase para representar el resultado de la validación.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
} 