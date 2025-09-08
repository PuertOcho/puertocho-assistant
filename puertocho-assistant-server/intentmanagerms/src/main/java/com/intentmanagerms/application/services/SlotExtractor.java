package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para extraer información de slots desde texto de usuario.
 * Combina técnicas de NLP basadas en reglas con LLM para extracción contextual avanzada.
 */
@Service
public class SlotExtractor {

    private static final Logger logger = LoggerFactory.getLogger(SlotExtractor.class);

    @Value("${slot-filling.enable-llm-extraction:true}")
    private boolean enableLlmExtraction;

    @Value("${slot-filling.extraction-confidence-threshold:0.7}")
    private double extractionConfidenceThreshold;

    @Value("${slot-filling.enable-pattern-extraction:true}")
    private boolean enablePatternExtraction;

    @Value("${slot-filling.enable-context-extraction:true}")
    private boolean enableContextExtraction;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Patrones de extracción comunes
    private final Map<String, List<Pattern>> extractionPatterns = new HashMap<>();

    public SlotExtractor() {
        initializeExtractionPatterns();
    }

    /**
     * Extrae slots desde un mensaje de usuario
     */
    public ExtractionResult extractSlots(String intentId, String userMessage, 
                                       List<String> targetSlots,
                                       Map<String, Object> conversationContext) {
        
        long startTime = System.currentTimeMillis();
        logger.info("Extrayendo slots para intent '{}' desde mensaje: {}", intentId, userMessage);

        ExtractionResult result = new ExtractionResult();
        result.setIntentId(intentId);
        result.setUserMessage(userMessage);
        result.setExtractedSlots(new HashMap<>());
        result.setConfidenceScores(new HashMap<>());
        result.setExtractionMethods(new HashMap<>());

        if (userMessage == null || userMessage.trim().isEmpty()) {
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }

        try {
            Map<String, Object> extractedSlots = new HashMap<>();
            Map<String, Double> confidenceScores = new HashMap<>();
            Map<String, String> extractionMethods = new HashMap<>();

            // Extracción con patrones si está habilitada
            if (enablePatternExtraction) {
                Map<String, Object> patternSlots = extractWithPatterns(userMessage, targetSlots);
                for (Map.Entry<String, Object> entry : patternSlots.entrySet()) {
                    extractedSlots.put(entry.getKey(), entry.getValue());
                    confidenceScores.put(entry.getKey(), 0.8);
                    extractionMethods.put(entry.getKey(), "pattern");
                }
            }

            // Extracción con LLM si está habilitada
            if (enableLlmExtraction && targetSlots != null && !targetSlots.isEmpty()) {
                LlmExtractionResult llmResult = extractWithLlm(intentId, userMessage, targetSlots, conversationContext);
                
                if (llmResult.isSuccess()) {
                    for (Map.Entry<String, Object> entry : llmResult.getExtractedSlots().entrySet()) {
                        String slotName = entry.getKey();
                        Object slotValue = entry.getValue();
                        
                        // Usar LLM solo si no se extrajo con patrones o si tiene mayor confianza
                        if (!extractedSlots.containsKey(slotName) || 
                            llmResult.getConfidenceScores().getOrDefault(slotName, 0.0) > 
                            confidenceScores.getOrDefault(slotName, 0.0)) {
                            
                            extractedSlots.put(slotName, slotValue);
                            confidenceScores.put(slotName, llmResult.getConfidenceScores().getOrDefault(slotName, 0.7));
                            extractionMethods.put(slotName, "llm");
                        }
                    }
                }
            }

            // Extracción contextual si está habilitada
            if (enableContextExtraction && conversationContext != null) {
                Map<String, Object> contextSlots = extractFromContext(targetSlots, conversationContext);
                for (Map.Entry<String, Object> entry : contextSlots.entrySet()) {
                    String slotName = entry.getKey();
                    if (!extractedSlots.containsKey(slotName)) {
                        extractedSlots.put(slotName, entry.getValue());
                        confidenceScores.put(slotName, 0.6);
                        extractionMethods.put(slotName, "context");
                    }
                }
            }

            result.setExtractedSlots(extractedSlots);
            result.setConfidenceScores(confidenceScores);
            result.setExtractionMethods(extractionMethods);
            result.setSlotsFound(extractedSlots.size());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Extracción completada para intent '{}' en {}ms. Slots encontrados: {}", 
                       intentId, result.getProcessingTimeMs(), result.getSlotsFound());

            return result;

        } catch (Exception e) {
            logger.error("Error extrayendo slots para intent '{}': {}", intentId, e.getMessage(), e);
            result.setErrorMessage("Error en extracción: " + e.getMessage());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * Extrae un slot específico desde un mensaje
     */
    public SlotExtractionResult extractSpecificSlot(String slotName, String userMessage, 
                                                   Map<String, Object> conversationContext) {
        
        logger.debug("Extrayendo slot específico '{}' desde: {}", slotName, userMessage);

        SlotExtractionResult result = new SlotExtractionResult();
        result.setSlotName(slotName);
        result.setUserMessage(userMessage);

        try {
            // Intentar extracción con patrones primero
            Object patternValue = extractSlotWithPattern(slotName, userMessage);
            if (patternValue != null) {
                result.setExtractedValue(patternValue);
                result.setConfidence(0.8);
                result.setExtractionMethod("pattern");
                result.setSuccess(true);
                return result;
            }

            // Intentar extracción con LLM
            if (enableLlmExtraction) {
                LlmSlotResult llmResult = extractSlotWithLlm(slotName, userMessage, conversationContext);
                if (llmResult.isSuccess()) {
                    result.setExtractedValue(llmResult.getValue());
                    result.setConfidence(llmResult.getConfidence());
                    result.setExtractionMethod("llm");
                    result.setSuccess(true);
                    return result;
                }
            }

            // Intentar extracción desde contexto
            Object contextValue = extractSlotFromContext(slotName, conversationContext);
            if (contextValue != null) {
                result.setExtractedValue(contextValue);
                result.setConfidence(0.6);
                result.setExtractionMethod("context");
                result.setSuccess(true);
                return result;
            }

            result.setSuccess(false);
            return result;

        } catch (Exception e) {
            logger.error("Error extrayendo slot '{}': {}", slotName, e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Error en extracción: " + e.getMessage());
            return result;
        }
    }

    // Métodos privados

    private void initializeExtractionPatterns() {
        // ✅ PATRONES MEJORADOS PARA UBICACIONES Y LUGARES
        List<Pattern> locationPatterns = Arrays.asList(
            // Preposiciones de lugar
            Pattern.compile("(?:en|de|desde|hacia|por)\\s+(?:la|el|las|los)?\\s*([A-Za-záéíóúñü\\s]{2,})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:ciudad|pueblo|lugar|zona|barrio|distrito)\\s+(?:de\\s+)?([A-Za-záéíóúñü\\s]{2,})", Pattern.CASE_INSENSITIVE),
            // Ciudades españolas principales y autonómicas
            Pattern.compile("\\b(Madrid|Barcelona|Sevilla|Valencia|Bilbao|Zaragoza|Málaga|Murcia|Palma|Las Palmas|Córdoba|Alicante|Valladolid|Vigo|Gijón|L'Hospitalet|Vitoria|Gasteiz|Granada|Elche|Oviedo|Badalona|Cartagena|Terrassa|Jerez|Sabadell|Móstoles|Santa Cruz|Pamplona|Iruña|Almería|Fuenlabrada|San Sebastián|Donostia|Leganés|Burgos|Santander|Castellón|Alcorcón|Getafe|Logroño|Badajoz|Huelva|Lleida|Marbella|León|Cádiz|Dos Hermanas|Torrejón|Parla|Alcobendas|Reus|Ourense|Girona|Santiago|Lugo|Pontevedra)\\b", Pattern.CASE_INSENSITIVE),
            // Países comunes
            Pattern.compile("\\b(España|Francia|Portugal|Italia|Alemania|Reino Unido|Estados Unidos|México|Argentina|Colombia|Perú|Chile|Venezuela|Ecuador|Bolivia|Uruguay|Paraguay|Brasil|Canadá|Japón|China|Corea|India)\\b", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("ubicacion", locationPatterns);
        extractionPatterns.put("lugar", locationPatterns);
        extractionPatterns.put("ciudad", locationPatterns);

        // ✅ PATRONES PARA HABITACIONES Y ESPACIOS DOMÉSTICOS
        List<Pattern> roomPatterns = Arrays.asList(
            Pattern.compile("(?:en|del|de\\s+la)\\s+(?:la\\s+)?(sala|salón|comedor|cocina|baño|dormitorio|habitación|cuarto|recibidor|pasillo|terraza|balcón|jardín|patio|garaje|sótano|ático|despacho|estudio|lavadero|trastero)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(sala|salón|living|comedor|cocina|kitchen|baño|aseo|dormitorio|habitación|cuarto|bedroom|recibidor|hall|pasillo|corredor|terraza|balcón|jardín|patio|garaje|garage|sótano|basement|ático|despacho|oficina|estudio|lavadero|trastero)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:la\\s+luz|luces?)\\s+(?:de|del)\\s+(?:la\\s+)?(\\w+)", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("lugar", roomPatterns);
        extractionPatterns.put("habitacion", roomPatterns);
        extractionPatterns.put("espacio", roomPatterns);

        // ✅ PATRONES MEJORADOS PARA FECHAS Y TIEMPO
        List<Pattern> datePatterns = Arrays.asList(
            // Fechas numéricas
            Pattern.compile("(\\d{1,2}[/\\-\\.]\\d{1,2}[/\\-\\.]\\d{2,4})"),
            Pattern.compile("(?:el\\s+día\\s+)?(\\d{1,2})\\s+de\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre)", Pattern.CASE_INSENSITIVE),
            // Fechas relativas
            Pattern.compile("\\b(hoy|mañana|ayer|pasado\\s+mañana|anteayer|la\\s+semana\\s+que\\s+viene|el\\s+próximo\\s+\\w+|este\\s+\\w+|el\\s+\\w+\\s+que\\s+viene)\\b", Pattern.CASE_INSENSITIVE),
            // Días de la semana
            Pattern.compile("\\b(lunes|martes|miércoles|jueves|viernes|sábado|domingo)\\b", Pattern.CASE_INSENSITIVE),
            // Fechas en formato YYYY-MM-DD
            Pattern.compile("(\\d{4}[/\\-\\.]\\d{1,2}[/\\-\\.]\\d{1,2})")
        );
        extractionPatterns.put("fecha", datePatterns);
        extractionPatterns.put("fecha_hora", datePatterns);

        // ✅ PATRONES MEJORADOS PARA HORAS
        List<Pattern> timePatterns = Arrays.asList(
            // Horas con formato 24h y 12h
            Pattern.compile("(?:a\\s+las?\\s+)?(\\d{1,2}[:\\.]\\d{2})(?:\\s*(AM|PM|am|pm|h|hrs?|horas?))?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:a\\s+las?\\s+)?(\\d{1,2})(?:\\s*(AM|PM|am|pm|h|hrs?|horas?|en\\s+punto))?\\b", Pattern.CASE_INSENSITIVE),
            // Horas en formato texto
            Pattern.compile("\\b(mediodía|medianoche|madrugada|mañana|tarde|noche)\\b", Pattern.CASE_INSENSITIVE),
            // Rangos de tiempo
            Pattern.compile("(?:en|dentro\\s+de)\\s+(\\d+)\\s*(minutos?|mins?|horas?|hrs?)", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("hora", timePatterns);
        extractionPatterns.put("tiempo", timePatterns);

        // ✅ PATRONES PARA DISPOSITIVOS IOT Y SMART HOME
        List<Pattern> devicePatterns = Arrays.asList(
            Pattern.compile("\\b(luz|luces|lámpara|lamparas|bombilla|bombillas|foco|focos|iluminación)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(aire\\s+acondicionado|calefacción|termostato|climatización|ventilador|radiador)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(televisión|televisor|tv|tele|radio|equipo\\s+de\\s+música|altavoces?|parlantes?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(persiana|persianas|cortina|cortinas|ventana|ventanas|puerta|puertas)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(cámara|cámaras|alarma|alarmas|sensor|sensores|detector)\\b", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("dispositivo", devicePatterns);
        extractionPatterns.put("aparato", devicePatterns);

        // ✅ PATRONES PARA ACCIONES Y OPERACIONES
        List<Pattern> actionPatterns = Arrays.asList(
            Pattern.compile("\\b(encender|enciende|prender|prende|activar|activa|conectar|conecta|abrir|abre)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(apagar|apaga|desactivar|desactiva|desconectar|desconecta|cerrar|cierra|detener|detén|parar|para)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(subir|sube|aumentar|aumenta|bajar|baja|disminuir|disminuye|regular|regula)\\b", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("accion", actionPatterns);
        extractionPatterns.put("operacion", actionPatterns);

        // ✅ PATRONES PARA TEMPERATURAS Y MEDIDAS
        List<Pattern> temperaturePatterns = Arrays.asList(
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:grados?|°)(?:\\s*[CcFf])?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:temperatura|temp)\\s+(?:de|a)?\\s*(\\d+(?:\\.\\d+)?)(?:\\s*grados?|°)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(frío|fría|caliente|templado|templada|tibio|tibia)\\b", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("temperatura", temperaturePatterns);

        // ✅ PATRONES PARA NOMBRES Y IDENTIFICADORES
        List<Pattern> namePatterns = Arrays.asList(
            Pattern.compile("(?:llamado|llamada|llamar|nombre|se\\s+llama|identificado\\s+como)\\s+([A-Za-záéíóúñü\\s]{2,})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:es|son)\\s+([A-Za-záéíóúñü]{2,})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([A-Z][a-záéíóúñü]{2,})\\b") // Nombres propios
        );
        extractionPatterns.put("nombre", namePatterns);

        // ✅ PATRONES PARA CANTIDADES Y NÚMEROS
        List<Pattern> quantityPatterns = Arrays.asList(
            Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\s*(?:unidades?|piezas?|elementos?|items?)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce|trece|catorce|quince|dieciséis|diecisiete|dieciocho|diecinueve|veinte)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(muchos|muchas|pocos|pocas|varios|varias|algunos|algunas|todos|todas)\\b", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("cantidad", quantityPatterns);
        extractionPatterns.put("numero", quantityPatterns);

        // ✅ PATRONES PARA COLORES
        List<Pattern> colorPatterns = Arrays.asList(
            Pattern.compile("\\b(rojo|roja|azul|verde|amarillo|amarilla|negro|negra|blanco|blanca|gris|rosa|morado|morada|naranja|violeta|marrón|beige|dorado|dorada|plateado|plateada)\\b", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("color", colorPatterns);

        // ✅ PATRONES PARA INTENSIDAD Y NIVELES
        List<Pattern> intensityPatterns = Arrays.asList(
            Pattern.compile("\\b(máximo|máxima|mínimo|mínima|alto|alta|bajo|baja|medio|media|fuerte|suave|intenso|intensa|débil)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:al|a)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:por\\s*ciento|%)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:nivel|intensidad)\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
        );
        extractionPatterns.put("intensidad", intensityPatterns);
        extractionPatterns.put("nivel", intensityPatterns);
    }

    private Map<String, Object> extractWithPatterns(String userMessage, List<String> targetSlots) {
        Map<String, Object> extractedSlots = new HashMap<>();

        if (targetSlots == null) {
            return extractedSlots;
        }

        for (String slotName : targetSlots) {
            Object value = extractSlotWithPattern(slotName, userMessage);
            if (value != null) {
                extractedSlots.put(slotName, value);
            }
        }

        return extractedSlots;
    }

    private Object extractSlotWithPattern(String slotName, String userMessage) {
        List<Pattern> patterns = extractionPatterns.get(slotName.toLowerCase());
        
        if (patterns == null) {
            return null;
        }

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(userMessage);
            if (matcher.find()) {
                String extracted = matcher.group(1).trim();
                if (!extracted.isEmpty()) {
                    logger.debug("Extraído slot '{}' con patrón: {}", slotName, extracted);
                    return extracted;
                }
            }
        }

        return null;
    }

    private LlmExtractionResult extractWithLlm(String intentId, String userMessage, 
                                             List<String> targetSlots,
                                             Map<String, Object> conversationContext) {
        
        LlmExtractionResult result = new LlmExtractionResult();
        result.setExtractedSlots(new HashMap<>());
        result.setConfidenceScores(new HashMap<>());

        try {
            String prompt = buildExtractionPrompt(intentId, userMessage, targetSlots, conversationContext);
            LlmResponse llmResponse = simulateLlmResponseForExtraction(prompt);
            
            ExtractionLlmResult llmResult = parseExtractionLlmResponse(llmResponse);
            
            if (llmResult.isSuccess()) {
                result.setExtractedSlots(llmResult.getSlots());
                result.setConfidenceScores(llmResult.getConfidences());
                result.setSuccess(true);
            }

            return result;

        } catch (Exception e) {
            logger.error("Error en extracción LLM: {}", e.getMessage(), e);
            result.setSuccess(false);
            return result;
        }
    }

    private LlmSlotResult extractSlotWithLlm(String slotName, String userMessage, 
                                           Map<String, Object> conversationContext) {
        
        LlmSlotResult result = new LlmSlotResult();

        try {
            String prompt = buildSingleSlotExtractionPrompt(slotName, userMessage, conversationContext);
            LlmResponse llmResponse = simulateLlmResponseForExtraction(prompt);
            
            SingleSlotLlmResult llmResult = parseSingleSlotLlmResponse(llmResponse);
            
            result.setSuccess(llmResult.isSuccess());
            result.setValue(llmResult.getValue());
            result.setConfidence(llmResult.getConfidence());

            return result;

        } catch (Exception e) {
            logger.error("Error en extracción LLM de slot '{}': {}", slotName, e.getMessage());
            result.setSuccess(false);
            return result;
        }
    }

    private String buildExtractionPrompt(String intentId, String userMessage, 
                                       List<String> targetSlots,
                                       Map<String, Object> conversationContext) {
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Extrae información específica del siguiente mensaje de usuario:\n\n");
        prompt.append("Intención: ").append(intentId).append("\n");
        prompt.append("Mensaje: \"").append(userMessage).append("\"\n");
        prompt.append("Slots a extraer: ").append(String.join(", ", targetSlots)).append("\n");
        
        if (conversationContext != null && !conversationContext.isEmpty()) {
            prompt.append("Contexto: ").append(conversationContext).append("\n");
        }
        
        prompt.append("\nResponde en formato JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"slots\": {\n");
        prompt.append("    \"slot_name\": \"valor_extraido\",\n");
        prompt.append("    ...\n");
        prompt.append("  },\n");
        prompt.append("  \"confidences\": {\n");
        prompt.append("    \"slot_name\": 0.0-1.0,\n");
        prompt.append("    ...\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        prompt.append("Solo incluye slots que puedas extraer con confianza del mensaje.");
        
        return prompt.toString();
    }

    private String buildSingleSlotExtractionPrompt(String slotName, String userMessage, 
                                                 Map<String, Object> conversationContext) {
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Extrae el valor del slot '").append(slotName).append("' del siguiente mensaje:\n\n");
        prompt.append("Mensaje: \"").append(userMessage).append("\"\n");
        
        if (conversationContext != null) {
            prompt.append("Contexto: ").append(conversationContext).append("\n");
        }
        
        prompt.append("\nResponde en formato JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"value\": \"valor_extraido_o_null\",\n");
        prompt.append("  \"confidence\": 0.0-1.0\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private ExtractionLlmResult parseExtractionLlmResponse(LlmResponse llmResponse) {
        ExtractionLlmResult result = new ExtractionLlmResult();
        result.setSlots(new HashMap<>());
        result.setConfidences(new HashMap<>());

        try {
            String response = llmResponse.getContent().trim();
            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (jsonNode.has("slots")) {
                JsonNode slotsNode = jsonNode.get("slots");
                slotsNode.fields().forEachRemaining(entry -> {
                    result.getSlots().put(entry.getKey(), entry.getValue().asText());
                });
            }
            
            if (jsonNode.has("confidences")) {
                JsonNode confidencesNode = jsonNode.get("confidences");
                confidencesNode.fields().forEachRemaining(entry -> {
                    result.getConfidences().put(entry.getKey(), entry.getValue().asDouble());
                });
            }
            
            result.setSuccess(true);

        } catch (Exception e) {
            logger.warn("Error parseando respuesta LLM de extracción: {}", e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    private SingleSlotLlmResult parseSingleSlotLlmResponse(LlmResponse llmResponse) {
        SingleSlotLlmResult result = new SingleSlotLlmResult();

        try {
            String response = llmResponse.getContent().trim();
            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (jsonNode.has("value") && !jsonNode.get("value").isNull()) {
                result.setValue(jsonNode.get("value").asText());
                result.setSuccess(true);
            }
            
            if (jsonNode.has("confidence")) {
                result.setConfidence(jsonNode.get("confidence").asDouble());
            }

        } catch (Exception e) {
            logger.warn("Error parseando respuesta LLM de slot único: {}", e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    private Map<String, Object> extractFromContext(List<String> targetSlots, Map<String, Object> conversationContext) {
        Map<String, Object> extractedSlots = new HashMap<>();

        if (targetSlots == null || conversationContext == null) {
            return extractedSlots;
        }

        for (String slotName : targetSlots) {
            Object value = extractSlotFromContext(slotName, conversationContext);
            if (value != null) {
                extractedSlots.put(slotName, value);
            }
        }

        return extractedSlots;
    }

    private Object extractSlotFromContext(String slotName, Map<String, Object> conversationContext) {
        if (conversationContext == null) {
            return null;
        }

        // Buscar en entity_cache
        @SuppressWarnings("unchecked")
        Map<String, Object> entityCache = (Map<String, Object>) conversationContext.get("entity_cache");
        if (entityCache != null && entityCache.containsKey(slotName)) {
            return entityCache.get(slotName);
        }

        // Buscar en user_preferences
        @SuppressWarnings("unchecked")
        Map<String, Object> userPreferences = (Map<String, Object>) conversationContext.get("user_preferences");
        if (userPreferences != null && userPreferences.containsKey(slotName)) {
            return userPreferences.get(slotName);
        }

        return null;
    }

    /**
     * Simula respuesta del LLM para extracción de slots
     */
    private LlmResponse simulateLlmResponseForExtraction(String prompt) {
        LlmResponse response = new LlmResponse();
        response.setLlmId("primary");
        response.setModel("simulated-llm");
        response.setSuccess(true);
        response.setResponseTimeMs(40L);
        
        // Generar respuesta de extracción simulada
        String extractionResponse = generateSimulatedExtraction(prompt);
        response.setContent(extractionResponse);
        
        return response;
    }

    /**
     * Genera extracción simulada basada en el prompt
     */
    private String generateSimulatedExtraction(String prompt) {
        StringBuilder jsonResponse = new StringBuilder();
        jsonResponse.append("{\n  \"slots\": {\n");
        
        Map<String, String> extractedSlots = new HashMap<>();
        Map<String, Double> confidences = new HashMap<>();
        
        // Simular extracción básica basada en contenido del prompt
        if (prompt.contains("Madrid") || prompt.contains("Barcelona") || prompt.contains("Sevilla")) {
            String city = extractCityFromPrompt(prompt);
            if (city != null) {
                extractedSlots.put("ubicacion", city);
                confidences.put("ubicacion", 0.9);
            }
        }
        
        if (prompt.contains("mañana") || prompt.contains("hoy") || prompt.contains("ayer")) {
            String date = extractDateFromPrompt(prompt);
            if (date != null) {
                extractedSlots.put("fecha", date);
                confidences.put("fecha", 0.8);
            }
        }
        
        if (prompt.contains("salón") || prompt.contains("cocina") || prompt.contains("dormitorio")) {
            String room = extractRoomFromPrompt(prompt);
            if (room != null) {
                extractedSlots.put("lugar", room);
                confidences.put("lugar", 0.85);
            }
        }
        
        // Construir JSON de respuesta
        boolean first = true;
        for (Map.Entry<String, String> entry : extractedSlots.entrySet()) {
            if (!first) jsonResponse.append(",\n");
            jsonResponse.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            first = false;
        }
        
        jsonResponse.append("\n  },\n  \"confidences\": {\n");
        
        first = true;
        for (Map.Entry<String, Double> entry : confidences.entrySet()) {
            if (!first) jsonResponse.append(",\n");
            jsonResponse.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            first = false;
        }
        
        jsonResponse.append("\n  }\n}");
        
        return jsonResponse.toString();
    }

    private String extractCityFromPrompt(String prompt) {
        if (prompt.contains("Madrid")) return "Madrid";
        if (prompt.contains("Barcelona")) return "Barcelona";
        if (prompt.contains("Sevilla")) return "Sevilla";
        return null;
    }

    private String extractDateFromPrompt(String prompt) {
        if (prompt.contains("mañana")) return "mañana";
        if (prompt.contains("hoy")) return "hoy";
        if (prompt.contains("ayer")) return "ayer";
        return null;
    }

    private String extractRoomFromPrompt(String prompt) {
        if (prompt.contains("salón")) return "salón";
        if (prompt.contains("cocina")) return "cocina";
        if (prompt.contains("dormitorio")) return "dormitorio";
        return null;
    }

    // Clases internas para resultados

    public static class ExtractionResult {
        private String intentId;
        private String userMessage;
        private Map<String, Object> extractedSlots;
        private Map<String, Double> confidenceScores;
        private Map<String, String> extractionMethods;
        private int slotsFound;
        private long processingTimeMs;
        private String errorMessage;

        // Getters y Setters
        public String getIntentId() { return intentId; }
        public void setIntentId(String intentId) { this.intentId = intentId; }
        
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
        
        public Map<String, Object> getExtractedSlots() { return extractedSlots; }
        public void setExtractedSlots(Map<String, Object> extractedSlots) { this.extractedSlots = extractedSlots; }
        
        public Map<String, Double> getConfidenceScores() { return confidenceScores; }
        public void setConfidenceScores(Map<String, Double> confidenceScores) { this.confidenceScores = confidenceScores; }
        
        public Map<String, String> getExtractionMethods() { return extractionMethods; }
        public void setExtractionMethods(Map<String, String> extractionMethods) { this.extractionMethods = extractionMethods; }
        
        public int getSlotsFound() { return slotsFound; }
        public void setSlotsFound(int slotsFound) { this.slotsFound = slotsFound; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class SlotExtractionResult {
        private String slotName;
        private String userMessage;
        private Object extractedValue;
        private double confidence;
        private String extractionMethod;
        private boolean success;
        private String errorMessage;

        // Getters y Setters
        public String getSlotName() { return slotName; }
        public void setSlotName(String slotName) { this.slotName = slotName; }
        
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
        
        public Object getExtractedValue() { return extractedValue; }
        public void setExtractedValue(Object extractedValue) { this.extractedValue = extractedValue; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public String getExtractionMethod() { return extractionMethod; }
        public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    // Clases auxiliares para resultados LLM
    
    private static class LlmExtractionResult {
        private Map<String, Object> extractedSlots;
        private Map<String, Double> confidenceScores;
        private boolean success;

        public Map<String, Object> getExtractedSlots() { return extractedSlots; }
        public void setExtractedSlots(Map<String, Object> extractedSlots) { this.extractedSlots = extractedSlots; }
        
        public Map<String, Double> getConfidenceScores() { return confidenceScores; }
        public void setConfidenceScores(Map<String, Double> confidenceScores) { this.confidenceScores = confidenceScores; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    private static class ExtractionLlmResult {
        private Map<String, Object> slots;
        private Map<String, Double> confidences;
        private boolean success;

        public Map<String, Object> getSlots() { return slots; }
        public void setSlots(Map<String, Object> slots) { this.slots = slots; }
        
        public Map<String, Double> getConfidences() { return confidences; }
        public void setConfidences(Map<String, Double> confidences) { this.confidences = confidences; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    private static class LlmSlotResult {
        private Object value;
        private double confidence;
        private boolean success;

        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    private static class SingleSlotLlmResult {
        private String value;
        private double confidence;
        private boolean success;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    /**
     * Obtiene estadísticas del extractor
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enable_llm_extraction", enableLlmExtraction);
        stats.put("extraction_confidence_threshold", extractionConfidenceThreshold);
        stats.put("enable_pattern_extraction", enablePatternExtraction);
        stats.put("enable_context_extraction", enableContextExtraction);
        stats.put("extraction_patterns_count", extractionPatterns.size());
        stats.put("service_status", "active");
        return stats;
    }
}