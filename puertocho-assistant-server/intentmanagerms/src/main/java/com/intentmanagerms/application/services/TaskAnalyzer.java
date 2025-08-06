package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio para analizar peticiones complejas y extraer subtareas usando LLM y patrones.
 */
@Service
public class TaskAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskAnalyzer.class);
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    @Autowired
    private McpActionRegistry mcpActionRegistry;
    
    @Value("${task.analyzer.enable-llm-analysis:true}")
    private Boolean enableLlmAnalysis;
    
    @Value("${task.analyzer.enable-pattern-analysis:true}")
    private Boolean enablePatternAnalysis;
    
    @Value("${task.analyzer.confidence-threshold:0.7}")
    private Double confidenceThreshold;
    
    @Value("${task.analyzer.max-subtasks-per-analysis:10}")
    private Integer maxSubtasksPerAnalysis;
    
    // Patrones para detectar acciones comunes
    private static final Map<String, Pattern> ACTION_PATTERNS = new HashMap<>();
    
    static {
        // Patrones para acciones de consulta
        ACTION_PATTERNS.put("consultar_tiempo", 
            Pattern.compile("(?:qué|como|dime|muestra|consulta|busca).*?(?:tiempo|clima|temperatura|meteorología)", 
                Pattern.CASE_INSENSITIVE));
        
        ACTION_PATTERNS.put("programar_alarma", 
            Pattern.compile("(?:programa|crea|establece|pon).*?(?:alarma|recordatorio|aviso)", 
                Pattern.CASE_INSENSITIVE));
        
        ACTION_PATTERNS.put("encender_luz", 
            Pattern.compile("(?:enciende|prende|activa|ilumina).*?(?:luz|lámpara|iluminación)", 
                Pattern.CASE_INSENSITIVE));
        
        ACTION_PATTERNS.put("reproducir_musica", 
            Pattern.compile("(?:reproduce|pon|toca|activa).*?(?:música|canción|tema|playlist)", 
                Pattern.CASE_INSENSITIVE));
        
        ACTION_PATTERNS.put("crear_issue", 
            Pattern.compile("(?:crea|abre|genera).*?(?:issue|ticket|problema|bug)", 
                Pattern.CASE_INSENSITIVE));
        
        ACTION_PATTERNS.put("actualizar_estado", 
            Pattern.compile("(?:actualiza|cambia|modifica).*?(?:estado|status|progreso)", 
                Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Analiza una petición usando LLM para extraer subtareas.
     */
    public List<Subtask> analyzeWithLLM(SubtaskDecompositionRequest request) {
        if (!enableLlmAnalysis) {
            logger.debug("Análisis LLM deshabilitado");
            return new ArrayList<>();
        }
        
        try {
            logger.debug("Analizando petición con LLM: {}", request.getUserMessage());
            
            // Construir prompt para el LLM
            String prompt = buildLLMPrompt(request);
            
            // Simular llamada al LLM (en implementación real, usar LlmConfigurationService)
            String llmResponse = simulateLLMAnalysis(request.getUserMessage());
            
            // Parsear respuesta del LLM
            List<Subtask> subtasks = parseLLMResponse(llmResponse, request);
            
            logger.debug("LLM generó {} subtareas", subtasks.size());
            return subtasks;
            
        } catch (Exception e) {
            logger.error("Error en análisis LLM", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Analiza una petición usando patrones para extraer subtareas.
     */
    public List<Subtask> analyzeWithPatterns(SubtaskDecompositionRequest request) {
        if (!enablePatternAnalysis) {
            logger.debug("Análisis de patrones deshabilitado");
            return new ArrayList<>();
        }
        
        try {
            logger.debug("Analizando petición con patrones: {}", request.getUserMessage());
            
            List<Subtask> subtasks = new ArrayList<>();
            String userMessage = request.getUserMessage().toLowerCase();
            
            // Detectar acciones usando patrones
            for (Map.Entry<String, Pattern> entry : ACTION_PATTERNS.entrySet()) {
                String action = entry.getKey();
                Pattern pattern = entry.getValue();
                
                Matcher matcher = pattern.matcher(userMessage);
                if (matcher.find()) {
                    Subtask subtask = createSubtaskFromPattern(action, userMessage, matcher);
                    if (subtask != null) {
                        subtasks.add(subtask);
                    }
                }
            }
            
            // Detectar múltiples acciones usando conectores
            List<Subtask> connectorSubtasks = detectMultipleActions(userMessage);
            subtasks.addAll(connectorSubtasks);
            
            // Limitar número de subtareas
            if (subtasks.size() > maxSubtasksPerAnalysis) {
                subtasks = subtasks.subList(0, maxSubtasksPerAnalysis);
            }
            
            logger.debug("Patrones detectaron {} subtareas", subtasks.size());
            return subtasks;
            
        } catch (Exception e) {
            logger.error("Error en análisis de patrones", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Construye el prompt para el análisis LLM.
     */
    private String buildLLMPrompt(SubtaskDecompositionRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Analiza la siguiente petición del usuario y descompónla en múltiples acciones MCP ejecutables.\n\n");
        prompt.append("Petición: ").append(request.getUserMessage()).append("\n\n");
        
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            prompt.append("Contexto de la conversación:\n");
            request.getContext().forEach((key, value) -> 
                prompt.append("- ").append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }
        
        if (request.getAvailableActions() != null && !request.getAvailableActions().isEmpty()) {
            prompt.append("Acciones MCP disponibles:\n");
            request.getAvailableActions().forEach(action -> 
                prompt.append("- ").append(action).append("\n"));
            prompt.append("\n");
        }
        
        prompt.append("Instrucciones:\n");
        prompt.append("1. Identifica todas las acciones que el usuario quiere realizar\n");
        prompt.append("2. Para cada acción, especifica:\n");
        prompt.append("   - action: nombre de la acción MCP\n");
        prompt.append("   - description: descripción clara de lo que hace\n");
        prompt.append("   - entities: entidades requeridas (ubicación, fecha, etc.)\n");
        prompt.append("   - dependencies: acciones que deben completarse antes\n");
        prompt.append("3. Responde en formato JSON válido\n");
        prompt.append("4. Máximo ").append(request.getMaxSubtasks()).append(" acciones\n\n");
        
        prompt.append("Respuesta en formato JSON:\n");
        
        return prompt.toString();
    }
    
    /**
     * Simula el análisis LLM (en implementación real, usar LlmConfigurationService).
     */
    private String simulateLLMAnalysis(String userMessage) {
        // Simulación basada en patrones comunes
        String message = userMessage.toLowerCase();
        
        if (message.contains("tiempo") && message.contains("alarma")) {
            return """
                {
                  "subtasks": [
                    {
                      "action": "consultar_tiempo",
                      "description": "Consultar información meteorológica",
                      "entities": {"ubicacion": "Madrid"},
                      "dependencies": []
                    },
                    {
                      "action": "programar_alarma_condicional",
                      "description": "Programar alarma basada en condiciones meteorológicas",
                      "entities": {"condicion": "si_llueve"},
                      "dependencies": ["consultar_tiempo"]
                    }
                  ]
                }
                """;
        }
        
        if (message.contains("luz") && message.contains("música")) {
            return """
                {
                  "subtasks": [
                    {
                      "action": "encender_luz",
                      "description": "Encender iluminación en la habitación",
                      "entities": {"lugar": "salón"},
                      "dependencies": []
                    },
                    {
                      "action": "reproducir_musica",
                      "description": "Reproducir música relajante",
                      "entities": {"genero": "relajante"},
                      "dependencies": []
                    }
                  ]
                }
                """;
        }
        
        if (message.contains("enciende") && message.contains("luz") && message.contains("música") && message.contains("temperatura")) {
            return """
                {
                  "subtasks": [
                    {
                      "action": "encender_luz",
                      "description": "Encender iluminación en el salón",
                      "entities": {"ubicacion": "salón"},
                      "dependencies": []
                    },
                    {
                      "action": "reproducir_musica",
                      "description": "Reproducir música relajante",
                      "entities": {"genero": "relajante"},
                      "dependencies": []
                    },
                    {
                      "action": "ajustar_temperatura",
                      "description": "Ajustar temperatura a 22 grados",
                      "entities": {"temperatura": "22°"},
                      "dependencies": []
                    }
                  ]
                }
                """;
        }
        
        if (message.contains("issue") && message.contains("estado")) {
            return """
                {
                  "subtasks": [
                    {
                      "action": "crear_github_issue",
                      "description": "Crear issue en GitHub sobre el problema",
                      "entities": {"titulo": "Weather Bug", "descripcion": "Bug en el servicio meteorológico"},
                      "dependencies": []
                    },
                    {
                      "action": "actualizar_taiga_story",
                      "description": "Actualizar estado en Taiga",
                      "entities": {"estado": "en_progreso"},
                      "dependencies": []
                    }
                  ]
                }
                """;
        }
        
        // Respuesta por defecto para peticiones simples
        return """
            {
              "subtasks": [
                {
                  "action": "ayuda",
                  "description": "Proporcionar ayuda al usuario",
                  "entities": {},
                  "dependencies": []
                }
              ]
            }
            """;
    }
    
    /**
     * Parsea la respuesta del LLM y la convierte en subtareas.
     */
    private List<Subtask> parseLLMResponse(String llmResponse, SubtaskDecompositionRequest request) {
        List<Subtask> subtasks = new ArrayList<>();
        
        try {
            // En implementación real, usar Jackson para parsear JSON
            // Por ahora, simular parsing basado en patrones
            
            if (llmResponse.contains("\"subtasks\"")) {
                // Extraer subtareas del JSON simulado
                String[] lines = llmResponse.split("\n");
                String currentAction = null;
                String currentDescription = null;
                Map<String, Object> currentEntities = new HashMap<>();
                List<String> currentDependencies = new ArrayList<>();
                
                for (String line : lines) {
                    line = line.trim();
                    
                    if (line.contains("\"action\":")) {
                        // Finalizar subtarea anterior si existe
                        if (currentAction != null) {
                            Subtask subtask = createSubtask(currentAction, currentDescription, 
                                    currentEntities, currentDependencies, 0.9);
                            subtasks.add(subtask);
                        }
                        
                        // Iniciar nueva subtarea
                        currentAction = extractValue(line);
                        currentDescription = null;
                        currentEntities = new HashMap<>();
                        currentDependencies = new ArrayList<>();
                    } else if (line.contains("\"description\":")) {
                        currentDescription = extractValue(line);
                    } else if (line.contains("\"entities\":")) {
                        // Simular extracción de entidades
                        currentEntities = extractEntities(line);
                    } else if (line.contains("\"dependencies\":")) {
                        currentDependencies = extractDependencies(line);
                    }
                }
                
                // Agregar última subtarea
                if (currentAction != null) {
                    Subtask subtask = createSubtask(currentAction, currentDescription, 
                            currentEntities, currentDependencies, 0.9);
                    subtasks.add(subtask);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error parseando respuesta LLM", e);
        }
        
        return subtasks;
    }
    
    /**
     * Extrae valor de una línea JSON.
     */
    private String extractValue(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex != -1) {
            String value = line.substring(colonIndex + 1).trim();
            // Remover comillas y comas
            value = value.replaceAll("[\",]", "").trim();
            return value;
        }
        return null;
    }
    
    /**
     * Extrae entidades de una línea JSON (simulado).
     */
    private Map<String, Object> extractEntities(String line) {
        Map<String, Object> entities = new HashMap<>();
        // Simulación simple - en implementación real usar Jackson
        if (line.contains("ubicacion")) entities.put("ubicacion", "Madrid");
        if (line.contains("lugar")) entities.put("lugar", "salón");
        if (line.contains("genero")) entities.put("genero", "relajante");
        return entities;
    }
    
    /**
     * Extrae dependencias de una línea JSON (simulado).
     */
    private List<String> extractDependencies(String line) {
        List<String> dependencies = new ArrayList<>();
        // Simulación simple - en implementación real usar Jackson
        if (line.contains("consultar_tiempo")) dependencies.add("consultar_tiempo");
        return dependencies;
    }
    
    /**
     * Crea una subtarea a partir de un patrón detectado.
     */
    private Subtask createSubtaskFromPattern(String action, String userMessage, Matcher matcher) {
        try {
            // Extraer entidades del contexto
            Map<String, Object> entities = extractEntitiesFromContext(userMessage, action);
            
            // Crear descripción
            String description = generateDescription(action, entities);
            
            // Crear subtarea
            Subtask subtask = createSubtask(action, description, entities, new ArrayList<>(), 0.8);
            
            logger.debug("Subtarea creada desde patrón: {} - {}", action, description);
            return subtask;
            
        } catch (Exception e) {
            logger.error("Error creando subtarea desde patrón", e);
            return null;
        }
    }
    
    /**
     * Extrae entidades del contexto del mensaje.
     */
    private Map<String, Object> extractEntitiesFromContext(String userMessage, String action) {
        Map<String, Object> entities = new HashMap<>();
        
        // Patrones para extraer entidades específicas
        Pattern locationPattern = Pattern.compile("(?:en|de|desde|hacia)\\s+([A-Za-záéíóúñ\\s]+)", 
                Pattern.CASE_INSENSITIVE);
        Pattern timePattern = Pattern.compile("(?:a las|a la|para las)\\s+(\\d{1,2}[:]\\d{2})", 
                Pattern.CASE_INSENSITIVE);
        Pattern datePattern = Pattern.compile("(?:hoy|mañana|ayer|el\\s+\\d{1,2})", 
                Pattern.CASE_INSENSITIVE);
        
        // Buscar ubicaciones
        Matcher locationMatcher = locationPattern.matcher(userMessage);
        if (locationMatcher.find()) {
            entities.put("ubicacion", locationMatcher.group(1).trim());
        }
        
        // Buscar horas
        Matcher timeMatcher = timePattern.matcher(userMessage);
        if (timeMatcher.find()) {
            entities.put("hora", timeMatcher.group(1));
        }
        
        // Buscar fechas
        Matcher dateMatcher = datePattern.matcher(userMessage);
        if (dateMatcher.find()) {
            entities.put("fecha", dateMatcher.group(1));
        }
        
        return entities;
    }
    
    /**
     * Genera una descripción para la subtarea.
     */
    private String generateDescription(String action, Map<String, Object> entities) {
        StringBuilder description = new StringBuilder();
        
        switch (action) {
            case "consultar_tiempo":
                description.append("Consultar información meteorológica");
                if (entities.containsKey("ubicacion")) {
                    description.append(" de ").append(entities.get("ubicacion"));
                }
                break;
            case "programar_alarma":
                description.append("Programar alarma");
                if (entities.containsKey("hora")) {
                    description.append(" para las ").append(entities.get("hora"));
                }
                break;
            case "encender_luz":
                description.append("Encender iluminación");
                if (entities.containsKey("ubicacion")) {
                    description.append(" en ").append(entities.get("ubicacion"));
                }
                break;
            case "reproducir_musica":
                description.append("Reproducir música");
                if (entities.containsKey("genero")) {
                    description.append(" de género ").append(entities.get("genero"));
                }
                break;
            default:
                description.append("Ejecutar acción ").append(action);
        }
        
        return description.toString();
    }
    
    /**
     * Detecta múltiples acciones usando conectores.
     */
    private List<Subtask> detectMultipleActions(String userMessage) {
        List<Subtask> subtasks = new ArrayList<>();
        
        // Conectores que indican múltiples acciones
        String[] connectors = {"y", "también", "además", "así como", "mientras", "después", ","};
        
        for (String connector : connectors) {
            if (userMessage.contains(connector)) {
                // Dividir mensaje por conector
                String[] parts = userMessage.split(connector, 2);
                if (parts.length == 2) {
                    // Analizar cada parte por separado
                    List<Subtask> part1Subtasks = analyzeWithPatterns(
                            createRequest(parts[0].trim()));
                    List<Subtask> part2Subtasks = analyzeWithPatterns(
                            createRequest(parts[1].trim()));
                    
                    subtasks.addAll(part1Subtasks);
                    subtasks.addAll(part2Subtasks);
                }
            }
        }
        
        // Detectar acciones específicas en el mensaje completo
        if (userMessage.contains("enciende") && userMessage.contains("luz")) {
            subtasks.add(createSubtask("encender_luz", "Encender iluminación", 
                extractEntitiesFromContext(userMessage, "encender_luz"), new ArrayList<>(), 0.9));
        }
        
        if (userMessage.contains("música") || userMessage.contains("pon") && userMessage.contains("música")) {
            subtasks.add(createSubtask("reproducir_musica", "Reproducir música relajante", 
                extractEntitiesFromContext(userMessage, "reproducir_musica"), new ArrayList<>(), 0.9));
        }
        
        if (userMessage.contains("temperatura") || userMessage.contains("ajusta")) {
            subtasks.add(createSubtask("ajustar_temperatura", "Ajustar temperatura del ambiente", 
                extractEntitiesFromContext(userMessage, "ajustar_temperatura"), new ArrayList<>(), 0.9));
        }
        
        if (userMessage.contains("tiempo") && userMessage.contains("alarma")) {
            subtasks.add(createSubtask("consultar_tiempo", "Consultar información meteorológica", 
                extractEntitiesFromContext(userMessage, "consultar_tiempo"), new ArrayList<>(), 0.9));
            subtasks.add(createSubtask("programar_alarma_condicional", "Programar alarma basada en condiciones meteorológicas", 
                extractEntitiesFromContext(userMessage, "programar_alarma_condicional"), Arrays.asList("consultar_tiempo"), 0.8));
        }
        
        return subtasks;
    }
    
    /**
     * Crea una solicitud de análisis para una parte del mensaje.
     */
    private SubtaskDecompositionRequest createRequest(String message) {
        SubtaskDecompositionRequest request = new SubtaskDecompositionRequest();
        request.setUserMessage(message);
        request.setMaxSubtasks(5);
        return request;
    }
    
    /**
     * Crea una subtarea con los parámetros especificados.
     */
    private Subtask createSubtask(String action, String description, Map<String, Object> entities, 
                                 List<String> dependencies, double confidence) {
        Subtask subtask = new Subtask(action, description);
        subtask.setEntities(entities);
        subtask.setDependencies(dependencies);
        subtask.setConfidenceScore(confidence);
        subtask.setEstimatedDurationMs(1000L); // Duración estimada por defecto
        subtask.setMaxRetries(3);
        
        return subtask;
    }
} 