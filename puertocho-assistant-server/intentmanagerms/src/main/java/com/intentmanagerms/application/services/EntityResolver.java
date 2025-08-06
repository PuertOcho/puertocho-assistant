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
 * Servicio para resolución de anáforas y ambigüedades en entidades.
 * Implementa resolución de pronombres, referencias contextuales y desambiguación.
 */
@Service
public class EntityResolver {

    private static final Logger logger = LoggerFactory.getLogger(EntityResolver.class);

    @Value("${entity.resolution.enable-anaphora-resolution:true}")
    private boolean enableAnaphoraResolution;

    @Value("${entity.resolution.enable-context-resolution:true}")
    private boolean enableContextResolution;

    @Value("${entity.resolution.enable-ambiguity-resolution:true}")
    private boolean enableAmbiguityResolution;

    @Value("${entity.resolution.enable-llm-resolution:true}")
    private boolean enableLlmResolution;

    @Value("${entity.resolution.confidence-threshold:0.6}")
    private double confidenceThreshold;

    @Value("${entity.resolution.max-resolution-attempts:3}")
    private int maxResolutionAttempts;

    @Autowired
    private ConversationManager conversationManager;

    // Patrones de anáforas
    private final Map<String, List<Pattern>> anaphoraPatterns = new HashMap<>();
    
    // Cache de resoluciones
    private final Map<String, String> resolutionCache = new HashMap<>();

    public EntityResolver() {
        initializeAnaphoraPatterns();
    }

    /**
     * Inicializa los patrones de anáforas.
     */
    private void initializeAnaphoraPatterns() {
        // Pronombres personales
        anaphoraPatterns.put("pronombres_personales", Arrays.asList(
            Pattern.compile("\\b(él|ella|ellos|ellas)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(lo|la|los|las)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(le|les)\\b", Pattern.CASE_INSENSITIVE)
        ));

        // Pronombres demostrativos
        anaphoraPatterns.put("pronombres_demostrativos", Arrays.asList(
            Pattern.compile("\\b(este|esta|estos|estas)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(ese|esa|esos|esas)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(aquel|aquella|aquellos|aquellas)\\b", Pattern.CASE_INSENSITIVE)
        ));

        // Referencias temporales
        anaphoraPatterns.put("referencias_temporales", Arrays.asList(
            Pattern.compile("\\b(entonces|ahora|después|antes|más tarde|pronto)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(hoy|mañana|ayer|esta semana|el mes pasado)\\b", Pattern.CASE_INSENSITIVE)
        ));

        // Referencias espaciales
        anaphoraPatterns.put("referencias_espaciales", Arrays.asList(
            Pattern.compile("\\b(aquí|allí|ahí|allá|acá)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(en este lugar|en ese sitio|por allá|por acá)\\b", Pattern.CASE_INSENSITIVE)
        ));

        // Referencias de cantidad
        anaphoraPatterns.put("referencias_cantidad", Arrays.asList(
            Pattern.compile("\\b(todo|toda|todos|todas)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(nada|ninguno|ninguna|ningunos|ningunas)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(alguno|alguna|algunos|algunas)\\b", Pattern.CASE_INSENSITIVE)
        ));
    }

    /**
     * Resuelve anáforas en una lista de entidades.
     */
    public List<Entity> resolveAnaphoras(List<Entity> entities, EntityExtractionRequest request) {
        if (!enableAnaphoraResolution) {
            return entities;
        }

        List<Entity> resolvedEntities = new ArrayList<>();
        
        for (Entity entity : entities) {
            try {
                Entity resolvedEntity = resolveAnaphora(entity, request);
                if (resolvedEntity != null) {
                    resolvedEntities.add(resolvedEntity);
                }
            } catch (Exception e) {
                logger.warn("Error resolviendo anáfora para entidad {}: {}", entity.getEntityId(), e.getMessage());
                resolvedEntities.add(entity); // Mantener entidad original si falla la resolución
            }
        }

        logger.debug("Resolución de anáforas completada: {} entidades resueltas", resolvedEntities.size());
        return resolvedEntities;
    }

    /**
     * Resuelve una anáfora individual.
     */
    public Entity resolveAnaphora(Entity entity, EntityExtractionRequest request) {
        if (entity == null || !entity.isValid()) {
            return entity;
        }

        // Verificar si la entidad contiene anáforas
        if (!containsAnaphora(entity.getValue())) {
            return entity;
        }

        // Intentar resolución múltiples veces
        for (int attempt = 1; attempt <= maxResolutionAttempts; attempt++) {
            try {
                Entity resolved = attemptResolution(entity, request, attempt);
                if (resolved != null && resolved.isResolved()) {
                    logger.debug("Anáfora resuelta en intento {}: {} -> {}", attempt, entity.getValue(), resolved.getResolvedValue());
                    return resolved;
                }
            } catch (Exception e) {
                logger.warn("Error en intento {} de resolución: {}", attempt, e.getMessage());
            }
        }

        logger.warn("No se pudo resolver anáfora después de {} intentos: {}", maxResolutionAttempts, entity.getValue());
        return entity;
    }

    /**
     * Verifica si un valor contiene anáforas.
     */
    private boolean containsAnaphora(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        for (List<Pattern> patterns : anaphoraPatterns.values()) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(value).find()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Intenta resolver una anáfora.
     */
    private Entity attemptResolution(Entity entity, EntityExtractionRequest request, int attempt) {
        String value = entity.getValue();
        String resolvedValue = null;

        // Resolución por contexto conversacional
        if (enableContextResolution && request.hasConversationSession()) {
            resolvedValue = resolveByContext(value, request.getConversationSessionId(), entity.getEntityType());
        }

        // Resolución por LLM (simulada)
        if (resolvedValue == null && enableLlmResolution) {
            resolvedValue = resolveByLlm(value, request, entity.getEntityType());
        }

        // Resolución por reglas
        if (resolvedValue == null) {
            resolvedValue = resolveByRules(value, entity.getEntityType());
        }

        // Resolución por cache
        if (resolvedValue == null) {
            resolvedValue = resolveByCache(value, entity.getEntityType());
        }

        // Si se encontró una resolución, actualizar la entidad
        if (resolvedValue != null && !resolvedValue.equals(value)) {
            entity.resolve(resolvedValue);
            entity.setExtractionMethod(entity.getExtractionMethod() + "+resolved");
            
            // Ajustar confianza basada en el método de resolución
            adjustConfidenceForResolution(entity, attempt);
            
            return entity;
        }

        return null;
    }

    /**
     * Resuelve anáfora por contexto conversacional.
     */
    private String resolveByContext(String value, String sessionId, String entityType) {
        try {
            ConversationSession session = conversationManager.getSession(sessionId);
            if (session == null || session.getContext() == null) {
                return null;
            }

            ConversationContext context = session.getContext();
            
            // Buscar en el cache de entidades del contexto
            if (context.getEntityCache() != null) {
                Object cachedValue = context.getEntityCache().get(entityType);
                if (cachedValue != null) {
                    return cachedValue.toString();
                }
            }

            // Buscar en el historial de conversación
            if (session.getConversationHistory() != null) {
                for (int i = session.getConversationHistory().size() - 1; i >= 0; i--) {
                    ConversationTurn turn = session.getConversationHistory().get(i);
                    if (turn.getUserMessage() != null) {
                        String resolved = findEntityInText(turn.getUserMessage(), entityType);
                        if (resolved != null) {
                            return resolved;
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Error resolviendo por contexto: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Resuelve anáfora usando LLM (simulado).
     */
    private String resolveByLlm(String value, EntityExtractionRequest request, String entityType) {
        // Simulación de resolución LLM
        try {
            // Simular resolución basada en el tipo de entidad y contexto
            switch (entityType) {
                case "ubicacion":
                    if (value.toLowerCase().contains("allí") || value.toLowerCase().contains("ahí")) {
                        return "Madrid"; // Simular ubicación previa
                    }
                    break;
                case "fecha":
                    if (value.toLowerCase().contains("entonces")) {
                        return "ayer"; // Simular fecha previa
                    }
                    break;
                case "lugar":
                    if (value.toLowerCase().contains("allí")) {
                        return "salón"; // Simular lugar previo
                    }
                    break;
            }
        } catch (Exception e) {
            logger.warn("Error en resolución LLM: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Resuelve anáfora por reglas predefinidas.
     */
    private String resolveByRules(String value, String entityType) {
        String lowerValue = value.toLowerCase();

        // Reglas para pronombres personales
        if (lowerValue.matches("\\b(él|ella|ellos|ellas)\\b")) {
            switch (entityType) {
                case "ubicacion":
                    return "Madrid"; // Ubicación por defecto
                case "lugar":
                    return "salón"; // Lugar por defecto
                case "artista":
                    return "Artista anterior"; // Artista por defecto
            }
        }

        // Reglas para referencias temporales
        if (lowerValue.matches("\\b(entonces|ahora|después)\\b")) {
            switch (entityType) {
                case "fecha":
                    return "hoy";
                case "hora":
                    return "ahora";
            }
        }

        // Reglas para referencias espaciales
        if (lowerValue.matches("\\b(aquí|allí|ahí)\\b")) {
            switch (entityType) {
                case "ubicacion":
                    return "ubicación actual";
                case "lugar":
                    return "lugar actual";
            }
        }

        return null;
    }

    /**
     * Resuelve anáfora usando cache.
     */
    private String resolveByCache(String value, String entityType) {
        String cacheKey = value.toLowerCase() + "_" + entityType;
        return resolutionCache.get(cacheKey);
    }

    /**
     * Encuentra una entidad de un tipo específico en un texto.
     */
    private String findEntityInText(String text, String entityType) {
        // Implementación simplificada - en producción usaría el EntityRecognizer
        switch (entityType) {
            case "ubicacion":
                if (text.toLowerCase().contains("madrid")) return "Madrid";
                if (text.toLowerCase().contains("barcelona")) return "Barcelona";
                break;
            case "lugar":
                if (text.toLowerCase().contains("salón")) return "salón";
                if (text.toLowerCase().contains("dormitorio")) return "dormitorio";
                break;
            case "fecha":
                if (text.toLowerCase().contains("hoy")) return "hoy";
                if (text.toLowerCase().contains("mañana")) return "mañana";
                break;
        }
        return null;
    }

    /**
     * Ajusta la confianza de una entidad basada en el método de resolución.
     */
    private void adjustConfidenceForResolution(Entity entity, int attempt) {
        double currentConfidence = entity.getConfidenceScore();
        
        // Reducir confianza basado en el número de intentos
        double reductionFactor = 1.0 - (attempt - 1) * 0.1;
        
        // Ajustar basado en el método de resolución
        String method = entity.getExtractionMethod();
        if (method.contains("context")) {
            currentConfidence *= 0.9; // Contexto es confiable
        } else if (method.contains("llm")) {
            currentConfidence *= 0.85; // LLM es confiable
        } else if (method.contains("rules")) {
            currentConfidence *= 0.8; // Reglas son menos confiables
        } else if (method.contains("cache")) {
            currentConfidence *= 0.7; // Cache es menos confiable
        }
        
        entity.setConfidenceScore(Math.max(currentConfidence * reductionFactor, 0.1));
    }

    /**
     * Resuelve ambigüedades en una lista de entidades.
     */
    public List<Entity> resolveAmbiguities(List<Entity> entities, EntityExtractionRequest request) {
        if (!enableAmbiguityResolution) {
            return entities;
        }

        List<Entity> resolvedEntities = new ArrayList<>();
        Map<String, List<Entity>> entitiesByType = groupEntitiesByType(entities);

        for (Map.Entry<String, List<Entity>> entry : entitiesByType.entrySet()) {
            String entityType = entry.getKey();
            List<Entity> typeEntities = entry.getValue();

            if (typeEntities.size() > 1) {
                // Hay ambigüedad, intentar resolver
                Entity resolved = resolveAmbiguity(typeEntities, request);
                if (resolved != null) {
                    resolvedEntities.add(resolved);
                } else {
                    // Si no se puede resolver, agregar la de mayor confianza
                    resolvedEntities.add(getHighestConfidenceEntity(typeEntities));
                }
            } else {
                resolvedEntities.addAll(typeEntities);
            }
        }

        logger.debug("Resolución de ambigüedades completada: {} entidades resueltas", resolvedEntities.size());
        return resolvedEntities;
    }

    /**
     * Agrupa entidades por tipo.
     */
    private Map<String, List<Entity>> groupEntitiesByType(List<Entity> entities) {
        Map<String, List<Entity>> grouped = new HashMap<>();
        
        for (Entity entity : entities) {
            String type = entity.getEntityType();
            grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(entity);
        }
        
        return grouped;
    }

    /**
     * Resuelve ambigüedad entre entidades del mismo tipo.
     */
    private Entity resolveAmbiguity(List<Entity> entities, EntityExtractionRequest request) {
        if (entities.size() <= 1) {
            return entities.isEmpty() ? null : entities.get(0);
        }

        // Estrategia 1: Mayor confianza
        Entity highestConfidence = getHighestConfidenceEntity(entities);
        if (highestConfidence.getConfidenceScore() > 0.8) {
            return highestConfidence;
        }

        // Estrategia 2: Resolución contextual
        if (request.hasConversationSession()) {
            Entity contextual = resolveByContextualPreference(entities, request);
            if (contextual != null) {
                return contextual;
            }
        }

        // Estrategia 3: Resolución por LLM (simulada)
        Entity llmResolved = resolveByLlmPreference(entities, request);
        if (llmResolved != null) {
            return llmResolved;
        }

        return null;
    }

    /**
     * Obtiene la entidad con mayor confianza.
     */
    private Entity getHighestConfidenceEntity(List<Entity> entities) {
        return entities.stream()
                .max(Comparator.comparingDouble(Entity::getConfidenceScore))
                .orElse(null);
    }

    /**
     * Resuelve ambigüedad por preferencia contextual.
     */
    private Entity resolveByContextualPreference(List<Entity> entities, EntityExtractionRequest request) {
        // Simulación de resolución contextual
        // En implementación real, analizaría el contexto de la conversación
        return null;
    }

    /**
     * Resuelve ambigüedad por preferencia LLM (simulada).
     */
    private Entity resolveByLlmPreference(List<Entity> entities, EntityExtractionRequest request) {
        // Simulación de resolución LLM
        // En implementación real, consultaría al LLM para elegir la mejor opción
        return null;
    }

    /**
     * Limpia el cache de resoluciones.
     */
    public void clearCache() {
        resolutionCache.clear();
        logger.info("Cache de resoluciones limpiado");
    }

    /**
     * Obtiene estadísticas del resolutor.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("anaphora_resolution_enabled", enableAnaphoraResolution);
        stats.put("context_resolution_enabled", enableContextResolution);
        stats.put("ambiguity_resolution_enabled", enableAmbiguityResolution);
        stats.put("llm_resolution_enabled", enableLlmResolution);
        stats.put("confidence_threshold", confidenceThreshold);
        stats.put("max_resolution_attempts", maxResolutionAttempts);
        stats.put("anaphora_patterns_count", anaphoraPatterns.size());
        stats.put("resolution_cache_size", resolutionCache.size());
        return stats;
    }
} 