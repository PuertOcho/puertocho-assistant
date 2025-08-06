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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio principal para extracción de entidades basado en LLM.
 * Coordina múltiples métodos de extracción y proporciona extracción contextual avanzada.
 */
@Service
public class EntityExtractor {

    private static final Logger logger = LoggerFactory.getLogger(EntityExtractor.class);

    @Value("${entity.extraction.enable-llm-extraction:true}")
    private boolean enableLlmExtraction;

    @Value("${entity.extraction.enable-pattern-extraction:true}")
    private boolean enablePatternExtraction;

    @Value("${entity.extraction.enable-context-extraction:true}")
    private boolean enableContextExtraction;

    @Value("${entity.extraction.enable-anaphora-resolution:true}")
    private boolean enableAnaphoraResolution;

    @Value("${entity.extraction.enable-validation:true}")
    private boolean enableValidation;

    @Value("${entity.extraction.max-parallel-extractors:3}")
    private int maxParallelExtractors;

    @Value("${entity.extraction.timeout-ms:5000}")
    private long extractionTimeoutMs;

    @Value("${entity.extraction.confidence-threshold:0.6}")
    private double defaultConfidenceThreshold;

    @Value("${entity.extraction.enable-caching:true}")
    private boolean enableCaching;

    @Value("${entity.extraction.cache-ttl-minutes:30}")
    private int cacheTtlMinutes;

    // Servicios de extracción
    @Autowired
    private EntityRecognizer entityRecognizer;

    @Autowired
    private EntityValidator entityValidator;

    @Autowired
    private EntityResolver entityResolver;

    @Autowired
    private LlmConfigurationService llmConfigurationService;

    @Autowired
    private ConversationManager conversationManager;

    // Cache para resultados de extracción
    private final Map<String, EntityExtractionResult> extractionCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();

    /**
     * Extrae entidades del texto usando múltiples métodos de extracción.
     */
    public EntityExtractionResult extractEntities(EntityExtractionRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Iniciando extracción de entidades para texto: '{}'", request.getText());

        try {
            // Verificar cache si está habilitado
            if (enableCaching) {
                String cacheKey = generateCacheKey(request);
                EntityExtractionResult cachedResult = getCachedResult(cacheKey);
                if (cachedResult != null) {
                    logger.info("Resultado obtenido de cache para texto: '{}'", request.getText());
                    return cachedResult;
                }
            }

            // Crear resultado inicial
            EntityExtractionResult result = new EntityExtractionResult(request.getText(), new ArrayList<>());
            result.setConfidenceThreshold(request.getConfidenceThreshold());
            result.setProcessingTimeMs(0);

            // Ejecutar extracción paralela si es posible
            List<Entity> allEntities = executeParallelExtraction(request, result);

            // Aplicar resolución de anáforas si está habilitado
            if (enableAnaphoraResolution && request.shouldUseAnaphoraResolution()) {
                allEntities = resolveAnaphoras(allEntities, request);
            }

            // Aplicar validación si está habilitado
            if (enableValidation && request.shouldValidate()) {
                allEntities = validateEntities(allEntities, request, result);
            }

            // Filtrar entidades por umbral de confianza
            allEntities = filterEntitiesByConfidence(allEntities, request.getConfidenceThreshold());

            // Limitar número de entidades
            allEntities = limitEntities(allEntities, request.getMaxEntities());

            // Actualizar resultado
            result.setEntities(allEntities);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            // Guardar en cache si está habilitado
            if (enableCaching) {
                String cacheKey = generateCacheKey(request);
                cacheResult(cacheKey, result);
            }

            logger.info("Extracción completada: {} entidades encontradas en {}ms", 
                       result.getTotalEntitiesFound(), result.getProcessingTimeMs());

            return result;

        } catch (Exception e) {
            logger.error("Error durante la extracción de entidades: {}", e.getMessage(), e);
            EntityExtractionResult errorResult = new EntityExtractionResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("Error durante la extracción: " + e.getMessage());
            errorResult.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return errorResult;
        }
    }

    /**
     * Ejecuta extracción paralela usando múltiples métodos.
     */
    private List<Entity> executeParallelExtraction(EntityExtractionRequest request, EntityExtractionResult result) {
        List<CompletableFuture<List<Entity>>> futures = new ArrayList<>();
        List<String> methodsUsed = new ArrayList<>();

        // Extracción por patrones
        if (enablePatternExtraction) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<Entity> entities = entityRecognizer.extractByPatterns(request);
                    logger.debug("Extracción por patrones: {} entidades encontradas", entities.size());
                    return entities;
                } catch (Exception e) {
                    logger.warn("Error en extracción por patrones: {}", e.getMessage());
                    return new ArrayList<Entity>();
                }
            }));
            methodsUsed.add("pattern");
        }

        // Extracción LLM
        if (enableLlmExtraction) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<Entity> entities = entityRecognizer.extractByLlm(request);
                    logger.debug("Extracción LLM: {} entidades encontradas", entities.size());
                    return entities;
                } catch (Exception e) {
                    logger.warn("Error en extracción LLM: {}", e.getMessage());
                    return new ArrayList<Entity>();
                }
            }));
            methodsUsed.add("llm");
        }

        // Extracción contextual
        if (enableContextExtraction && request.shouldUseContextResolution()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<Entity> entities = entityRecognizer.extractByContext(request);
                    logger.debug("Extracción contextual: {} entidades encontradas", entities.size());
                    return entities;
                } catch (Exception e) {
                    logger.warn("Error en extracción contextual: {}", e.getMessage());
                    return new ArrayList<Entity>();
                }
            }));
            methodsUsed.add("context");
        }

        // Esperar resultados con timeout
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            allFutures.get(extractionTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);

            // Combinar resultados
            List<Entity> allEntities = new ArrayList<>();
            for (CompletableFuture<List<Entity>> future : futures) {
                allEntities.addAll(future.get());
            }

            result.setExtractionMethodsUsed(methodsUsed);
            return allEntities;

        } catch (Exception e) {
            logger.warn("Timeout o error en extracción paralela: {}", e.getMessage());
            // Continuar con los resultados disponibles
            List<Entity> allEntities = new ArrayList<>();
            for (CompletableFuture<List<Entity>> future : futures) {
                try {
                    if (future.isDone()) {
                        allEntities.addAll(future.get());
                    }
                } catch (Exception ex) {
                    logger.warn("Error obteniendo resultado de extracción: {}", ex.getMessage());
                }
            }
            result.setExtractionMethodsUsed(methodsUsed);
            return allEntities;
        }
    }

    /**
     * Resuelve anáforas en las entidades extraídas.
     */
    private List<Entity> resolveAnaphoras(List<Entity> entities, EntityExtractionRequest request) {
        try {
            return entityResolver.resolveAnaphoras(entities, request);
        } catch (Exception e) {
            logger.warn("Error resolviendo anáforas: {}", e.getMessage());
            return entities;
        }
    }

    /**
     * Valida las entidades extraídas.
     */
    private List<Entity> validateEntities(List<Entity> entities, EntityExtractionRequest request, EntityExtractionResult result) {
        try {
            return entityValidator.validateEntities(entities, request, result);
        } catch (Exception e) {
            logger.warn("Error validando entidades: {}", e.getMessage());
            result.addWarning("Error en validación: " + e.getMessage());
            return entities;
        }
    }

    /**
     * Filtra entidades por umbral de confianza.
     */
    private List<Entity> filterEntitiesByConfidence(List<Entity> entities, double threshold) {
        return entities.stream()
                .filter(e -> e.getConfidenceScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * Limita el número de entidades retornadas.
     */
    private List<Entity> limitEntities(List<Entity> entities, int maxEntities) {
        if (entities.size() <= maxEntities) {
            return entities;
        }

        // Ordenar por confianza y tomar las mejores
        return entities.stream()
                .sorted((e1, e2) -> Double.compare(e2.getConfidenceScore(), e1.getConfidenceScore()))
                .limit(maxEntities)
                .collect(Collectors.toList());
    }

    /**
     * Genera clave de cache para el request.
     */
    private String generateCacheKey(EntityExtractionRequest request) {
        return String.format("%s_%s_%s_%.2f_%s",
                request.getText().hashCode(),
                request.getEntityTypes() != null ? request.getEntityTypes().hashCode() : 0,
                request.getIntent() != null ? request.getIntent() : "",
                request.getConfidenceThreshold(),
                request.getLanguage());
    }

    /**
     * Obtiene resultado de cache si existe y no ha expirado.
     */
    private EntityExtractionResult getCachedResult(String cacheKey) {
        LocalDateTime timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp != null && 
            timestamp.plusMinutes(cacheTtlMinutes).isAfter(LocalDateTime.now())) {
            return extractionCache.get(cacheKey);
        }
        
        // Limpiar entrada expirada
        extractionCache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
        return null;
    }

    /**
     * Guarda resultado en cache.
     */
    private void cacheResult(String cacheKey, EntityExtractionResult result) {
        extractionCache.put(cacheKey, result);
        cacheTimestamps.put(cacheKey, LocalDateTime.now());
    }

    /**
     * Limpia el cache de extracción.
     */
    public void clearCache() {
        extractionCache.clear();
        cacheTimestamps.clear();
        logger.info("Cache de extracción de entidades limpiado");
    }

    /**
     * Obtiene estadísticas del servicio.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cache_size", extractionCache.size());
        stats.put("cache_enabled", enableCaching);
        stats.put("cache_ttl_minutes", cacheTtlMinutes);
        stats.put("llm_extraction_enabled", enableLlmExtraction);
        stats.put("pattern_extraction_enabled", enablePatternExtraction);
        stats.put("context_extraction_enabled", enableContextExtraction);
        stats.put("anaphora_resolution_enabled", enableAnaphoraResolution);
        stats.put("validation_enabled", enableValidation);
        stats.put("max_parallel_extractors", maxParallelExtractors);
        stats.put("extraction_timeout_ms", extractionTimeoutMs);
        stats.put("default_confidence_threshold", defaultConfidenceThreshold);
        return stats;
    }

    /**
     * Verifica el estado de salud del servicio.
     */
    public boolean isHealthy() {
        try {
            // Verificar que los servicios dependientes estén disponibles
            return entityRecognizer != null && 
                   entityValidator != null && 
                   entityResolver != null &&
                   llmConfigurationService != null;
        } catch (Exception e) {
            logger.error("Error verificando salud del EntityExtractor: {}", e.getMessage());
            return false;
        }
    }
} 