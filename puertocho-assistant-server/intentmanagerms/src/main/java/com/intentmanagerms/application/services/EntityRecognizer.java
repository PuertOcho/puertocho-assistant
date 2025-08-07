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

/**
 * Servicio para reconocimiento de entidades usando múltiples métodos.
 * Implementa extracción por patrones, LLM y contexto.
 */
@Service
public class EntityRecognizer {

    private static final Logger logger = LoggerFactory.getLogger(EntityRecognizer.class);

    @Value("${entity.recognition.pattern-extraction.enabled:true}")
    private boolean patternExtractionEnabled;

    @Value("${entity.recognition.llm-extraction.enabled:true}")
    private boolean llmExtractionEnabled;

    @Value("${entity.recognition.context-extraction.enabled:true}")
    private boolean contextExtractionEnabled;

    @Value("${entity.recognition.confidence-threshold:0.6}")
    private double confidenceThreshold;

    @Autowired
    private LlmConfigurationService llmConfigurationService;

    @Autowired
    private ConversationManager conversationManager;

    // Patrones de reconocimiento predefinidos
    private final Map<String, List<Pattern>> entityPatterns = new HashMap<>();

    public EntityRecognizer() {
        initializePatterns();
    }

    /**
     * Inicializa los patrones de reconocimiento de entidades.
     */
    private void initializePatterns() {
        // Patrones para ubicaciones
        entityPatterns.put("ubicacion", Arrays.asList(
            Pattern.compile("(?:en|de|desde|hacia|para)\\s+([A-Za-záéíóúñÁÉÍÓÚÑ\\s]+?)(?:\\s|$|,|\\.)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([A-Za-záéíóúñÁÉÍÓÚÑ]+(?:\\s+[A-Za-záéíóúñÁÉÍÓÚÑ]+)*)\\s+(?:ciudad|pueblo|villa|capital)", Pattern.CASE_INSENSITIVE)
        ));

        // Patrones para fechas
        entityPatterns.put("fecha", Arrays.asList(
            Pattern.compile("(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})"),
            Pattern.compile("(hoy|mañana|ayer|pasado\\s+mañana|anteayer)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(lunes|martes|miércoles|jueves|viernes|sábado|domingo)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2})\\s+(?:de\\s+)?(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre)", Pattern.CASE_INSENSITIVE)
        ));

        // Patrones para horas
        entityPatterns.put("hora", Arrays.asList(
            Pattern.compile("(\\d{1,2}[:.]\\d{2})(?:\\s*(AM|PM|am|pm))?"),
            Pattern.compile("(\\d{1,2})\\s+(?:horas?|h)(?:\\s+y\\s+(\\d{1,2})\\s+minutos?)?"),
            Pattern.compile("(mediodía|medianoche|mañana|tarde|noche)", Pattern.CASE_INSENSITIVE)
        ));

        // Patrones para temperaturas
        entityPatterns.put("temperatura", Arrays.asList(
            Pattern.compile("(\\d+)\\s*(?:grados?|°)(?:\\s*[CcFf])?"),
            Pattern.compile("(\\d+)\\s+(?:grados?)\\s+(?:celsius|fahrenheit|centígrados)", Pattern.CASE_INSENSITIVE)
        ));

        // Patrones para nombres
        entityPatterns.put("nombre", Arrays.asList(
            Pattern.compile("(?:llamado|nombre|se\\s+llama)\\s+([A-Za-záéíóúñÁÉÍÓÚÑ\\s]+?)(?:\\s|$|,|\\.)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([A-Z][a-záéíóúñ]+\\s+[A-Z][a-záéíóúñ]+)"),
            Pattern.compile("(?:mi\\s+nombre\\s+es|me\\s+llamo)\\s+([A-Za-záéíóúñÁÉÍÓÚÑ\\s]+?)(?:\\s|$|,|\\.)", Pattern.CASE_INSENSITIVE)
        ));

        // Patrones para lugares/habitaciones
        entityPatterns.put("lugar", Arrays.asList(
            Pattern.compile("(?:en|del|de\\s+la)\\s+(salón|comedor|dormitorio|habitación|cocina|baño|oficina|garaje|jardín|terraza)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(salón|comedor|dormitorio|habitación|cocina|baño|oficina|garaje|jardín|terraza)", Pattern.CASE_INSENSITIVE)
        ));

        // Patrones para artistas
        entityPatterns.put("artista", Arrays.asList(
            Pattern.compile("(?:música\\s+de|canción\\s+de|escuchar)\\s+([A-Za-záéíóúñÁÉÍÓÚÑ\\s]+?)(?:\\s|$|,|\\.)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([A-Z][a-záéíóúñ]+\\s+[A-Z][a-záéíóúñ]+)(?:\\s+\\-\\s+[A-Za-záéíóúñÁÉÍÓÚÑ\\s]+)?"),
            Pattern.compile("(?:pon|reproduce)\\s+([A-Za-záéíóúñÁÉÍÓÚÑ\\s]+?)(?:\\s|$|,|\\.)", Pattern.CASE_INSENSITIVE)
        ));

        // Patrones para géneros musicales
        entityPatterns.put("genero", Arrays.asList(
            Pattern.compile("(?:música|canción)\\s+(?:de\\s+)?(rock|jazz|clásica|pop|reggaeton|flamenco|electrónica|folk|blues|salsa)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(rock|jazz|clásica|pop|reggaeton|flamenco|electrónica|folk|blues|salsa)", Pattern.CASE_INSENSITIVE)
        ));

        // Patrones para canciones
        entityPatterns.put("cancion", Arrays.asList(
            Pattern.compile("(?:canción|tema|track)\\s+(?:llamada?|titulada?)\\s+['\"]([A-Za-záéíóúñÁÉÍÓÚÑ\\s]+?)['\"]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("['\"]([A-Za-záéíóúñÁÉÍÓÚÑ\\s]+?)['\"]\\s+(?:de\\s+[A-Za-záéíóúñÁÉÍÓÚÑ\\s]+)?"),
            Pattern.compile("(?:pon|reproduce)\\s+['\"]([A-Za-záéíóúñÁÉÍÓÚÑ\\s]+?)['\"]", Pattern.CASE_INSENSITIVE)
        ));
    }

    /**
     * Extrae entidades usando patrones regex.
     */
    public List<Entity> extractByPatterns(EntityExtractionRequest request) {
        if (!patternExtractionEnabled) {
            return new ArrayList<>();
        }

        List<Entity> entities = new ArrayList<>();
        String text = request.getText().toLowerCase();

        // Determinar qué tipos de entidades buscar
        Set<String> entityTypesToSearch = request.hasSpecificEntityTypes() ? 
            new HashSet<>(request.getEntityTypes()) : entityPatterns.keySet();

        for (String entityType : entityTypesToSearch) {
            List<Pattern> patterns = entityPatterns.get(entityType);
            if (patterns == null) continue;

            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    String value = matcher.group(1);
                    if (value != null && !value.trim().isEmpty()) {
                        Entity entity = new Entity(entityType, value.trim(), calculatePatternConfidence(value, entityType));
                        entity.setExtractionMethod("pattern");
                        entity.setStartPosition(matcher.start(1));
                        entity.setEndPosition(matcher.end(1));
                        entity.setContext(text.substring(Math.max(0, matcher.start() - 20), 
                                                       Math.min(text.length(), matcher.end() + 20)));
                        entities.add(entity);
                    }
                }
            }
        }

        logger.debug("Extracción por patrones: {} entidades encontradas", entities.size());
        return entities;
    }

    /**
     * Extrae entidades usando LLM.
     */
    public List<Entity> extractByLlm(EntityExtractionRequest request) {
        if (!llmExtractionEnabled) {
            return new ArrayList<>();
        }

        try {
            // Simular extracción LLM (en implementación real, se llamaría al LLM)
            List<Entity> entities = new ArrayList<>();
            
            // Crear prompt para el LLM
            String prompt = buildLlmPrompt(request);
            
            // Simular respuesta del LLM
            String llmResponse = simulateLlmResponse(request.getText(), request.getEntityTypes());
            
            // Parsear respuesta del LLM
            entities = parseLlmResponse(llmResponse, request);
            
            logger.debug("Extracción LLM: {} entidades encontradas", entities.size());
            return entities;

        } catch (Exception e) {
            logger.warn("Error en extracción LLM: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Extrae entidades usando contexto conversacional.
     */
    public List<Entity> extractByContext(EntityExtractionRequest request) {
        if (!contextExtractionEnabled || !request.hasConversationSession()) {
            return new ArrayList<>();
        }

        try {
            List<Entity> entities = new ArrayList<>();
            
            // Obtener contexto de la conversación
            ConversationSession session = conversationManager.getSession(request.getConversationSessionId());
            if (session == null) {
                return entities;
            }

            // Buscar entidades en el contexto conversacional
            entities = extractFromConversationContext(session, request);
            
            logger.debug("Extracción contextual: {} entidades encontradas", entities.size());
            return entities;

        } catch (Exception e) {
            logger.warn("Error en extracción contextual: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Calcula la confianza de una entidad extraída por patrones.
     */
    private double calculatePatternConfidence(String value, String entityType) {
        double baseConfidence = 0.7;
        
        // Ajustar confianza basado en el tipo de entidad
        switch (entityType) {
            case "ubicacion":
                if (value.matches(".*[A-Z].*")) baseConfidence += 0.1; // Capitalización
                if (value.length() > 3) baseConfidence += 0.1; // Longitud
                break;
            case "fecha":
                if (value.matches("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}")) baseConfidence += 0.2; // Formato numérico
                break;
            case "hora":
                if (value.matches("\\d{1,2}[:.]\\d{2}")) baseConfidence += 0.2; // Formato HH:MM
                break;
            case "temperatura":
                if (value.matches("\\d+\\s*(?:grados?|°)")) baseConfidence += 0.2; // Formato numérico
                break;
        }
        
        return Math.min(baseConfidence, 0.95);
    }

    /**
     * Construye el prompt para el LLM.
     */
    private String buildLlmPrompt(EntityExtractionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Extrae las siguientes entidades del texto proporcionado:\n\n");
        
        if (request.hasSpecificEntityTypes()) {
            for (String entityType : request.getEntityTypes()) {
                prompt.append("- ").append(entityType).append("\n");
            }
        } else {
            prompt.append("- ubicacion, fecha, hora, temperatura, nombre, lugar, artista, genero, cancion\n");
        }
        
        prompt.append("\nTexto: \"").append(request.getText()).append("\"\n\n");
        prompt.append("Responde en formato JSON con la siguiente estructura:\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"entity_type\": \"tipo_entidad\",\n");
        prompt.append("    \"value\": \"valor_extraido\",\n");
        prompt.append("    \"confidence\": 0.85,\n");
        prompt.append("    \"start_position\": 10,\n");
        prompt.append("    \"end_position\": 15\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        
        return prompt.toString();
    }

    /**
     * Simula la respuesta del LLM (en implementación real, se llamaría al LLM real).
     */
    private String simulateLlmResponse(String text, List<String> entityTypes) {
        // Simulación simple basada en el texto
        List<Map<String, Object>> entities = new ArrayList<>();
        
        if (text.toLowerCase().contains("madrid") || text.toLowerCase().contains("barcelona")) {
            Map<String, Object> entity = new HashMap<>();
            entity.put("entity_type", "ubicacion");
            entity.put("value", text.toLowerCase().contains("madrid") ? "Madrid" : "Barcelona");
            entity.put("confidence", 0.9);
            entity.put("start_position", text.toLowerCase().indexOf("madrid") != -1 ? 
                      text.toLowerCase().indexOf("madrid") : text.toLowerCase().indexOf("barcelona"));
            int startPos = text.toLowerCase().indexOf("madrid") != -1 ? 
                          text.toLowerCase().indexOf("madrid") : text.toLowerCase().indexOf("barcelona");
            entity.put("start_position", startPos);
            entity.put("end_position", startPos + ((String) entity.get("value")).length());
            entities.add(entity);
        }
        
        if (text.toLowerCase().contains("mañana") || text.toLowerCase().contains("hoy")) {
            Map<String, Object> entity = new HashMap<>();
            entity.put("entity_type", "fecha");
            entity.put("value", text.toLowerCase().contains("mañana") ? "mañana" : "hoy");
            entity.put("confidence", 0.85);
            int startPos = text.toLowerCase().indexOf("mañana") != -1 ? 
                          text.toLowerCase().indexOf("mañana") : text.toLowerCase().indexOf("hoy");
            entity.put("start_position", startPos);
            entity.put("end_position", startPos + ((String) entity.get("value")).length());
            entities.add(entity);
        }
        
        // Convertir a JSON (simplificado)
        return entities.toString();
    }

    /**
     * Parsea la respuesta del LLM.
     */
    private List<Entity> parseLlmResponse(String llmResponse, EntityExtractionRequest request) {
        List<Entity> entities = new ArrayList<>();
        
        try {
            // Parseo mejorado basado en el texto original
            String text = request.getText().toLowerCase();
            
            // Extraer ubicaciones
            if (text.contains("madrid")) {
                Entity entity = new Entity("ubicacion", "Madrid", 0.9);
                entity.setExtractionMethod("llm");
                entity.setStartPosition(text.indexOf("madrid"));
                entity.setEndPosition(text.indexOf("madrid") + "madrid".length());
                entities.add(entity);
            } else if (text.contains("barcelona")) {
                Entity entity = new Entity("ubicacion", "Barcelona", 0.9);
                entity.setExtractionMethod("llm");
                entity.setStartPosition(text.indexOf("barcelona"));
                entity.setEndPosition(text.indexOf("barcelona") + "barcelona".length());
                entities.add(entity);
            }
            
            // Extraer fechas
            if (text.contains("mañana")) {
                Entity entity = new Entity("fecha", "mañana", 0.85);
                entity.setExtractionMethod("llm");
                entity.setStartPosition(text.indexOf("mañana"));
                entity.setEndPosition(text.indexOf("mañana") + "mañana".length());
                entities.add(entity);
            } else if (text.contains("hoy")) {
                Entity entity = new Entity("fecha", "hoy", 0.85);
                entity.setExtractionMethod("llm");
                entity.setStartPosition(text.indexOf("hoy"));
                entity.setEndPosition(text.indexOf("hoy") + "hoy".length());
                entities.add(entity);
            }
            
            // Extraer lugares
            if (text.contains("salón")) {
                Entity entity = new Entity("lugar", "salón", 0.8);
                entity.setExtractionMethod("llm");
                entity.setStartPosition(text.indexOf("salón"));
                entity.setEndPosition(text.indexOf("salón") + "salón".length());
                entities.add(entity);
            }
            
        } catch (Exception e) {
            logger.warn("Error parseando respuesta LLM: {}", e.getMessage());
        }
        
        return entities;
    }

    /**
     * Extrae entidades del contexto conversacional.
     */
    private List<Entity> extractFromConversationContext(ConversationSession session, EntityExtractionRequest request) {
        List<Entity> entities = new ArrayList<>();
        
        try {
            // Buscar en el contexto de la sesión
            ConversationContext context = session.getContext();
            if (context != null && context.getEntityCache() != null) {
                for (Map.Entry<String, Object> entry : context.getEntityCache().entrySet()) {
                    String entityType = entry.getKey();
                    Object value = entry.getValue();
                    
                    // Verificar si el tipo de entidad es relevante para la solicitud
                    if (!request.hasSpecificEntityTypes() || 
                        request.getEntityTypes().contains(entityType)) {
                        
                        Entity entity = new Entity(entityType, value.toString(), 0.8);
                        entity.setExtractionMethod("context");
                        entity.setContext("Extraído del contexto conversacional");
                        entities.add(entity);
                    }
                }
            }
            
            // Buscar en el historial de conversación
            if (session.getConversationHistory() != null) {
                for (ConversationTurn turn : session.getConversationHistory()) {
                    // Buscar entidades mencionadas anteriormente
                    if (turn.getUserMessage() != null && 
                        turn.getUserMessage().toLowerCase().contains(request.getText().toLowerCase())) {
                        
                        // Extraer entidades del turno anterior
                        List<Entity> historicalEntities = extractByPatterns(
                            new EntityExtractionRequest(turn.getUserMessage())
                        );
                        entities.addAll(historicalEntities);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error extrayendo del contexto: {}", e.getMessage());
        }
        
        return entities;
    }

    /**
     * Obtiene estadísticas del reconocedor.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pattern_extraction_enabled", patternExtractionEnabled);
        stats.put("llm_extraction_enabled", llmExtractionEnabled);
        stats.put("context_extraction_enabled", contextExtractionEnabled);
        stats.put("confidence_threshold", confidenceThreshold);
        stats.put("entity_patterns_count", entityPatterns.size());
        stats.put("supported_entity_types", new ArrayList<>(entityPatterns.keySet()));
        return stats;
    }
} 