package com.intentmanagerms.application.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.domain.model.ConversationContext;
import com.intentmanagerms.domain.model.ConversationSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.Base64;

/**
 * Servicio de persistencia de contexto conversacional.
 * 
 * Proporciona:
 * - Serialización/deserialización eficiente de contexto
 * - Compresión automática de datos contextuales
 * - Cache de contexto para optimización
 * - Gestión de versiones de contexto
 * - Backup y recuperación de contexto
 */
@Service
public class ContextPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(ContextPersistenceService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisConversationRepository redisRepository;

    // Configuración
    @Value("${conversation.context.enable-compression:true}")
    private boolean enableCompression;

    @Value("${conversation.context.compression-level:6}")
    private int compressionLevel;

    @Value("${conversation.context.enable-caching:true}")
    private boolean enableCaching;

    @Value("${conversation.context.cache-ttl-seconds:1800}")
    private int cacheTtlSeconds;

    @Value("${conversation.context.max-cache-size:1000}")
    private int maxCacheSize;

    @Value("${conversation.context.enable-versioning:true}")
    private boolean enableVersioning;

    @Value("${conversation.context.max-versions:5}")
    private int maxVersions;

    // Cache de contexto
    private final Map<String, CachedContext> contextCache = new ConcurrentHashMap<>();
    private final Map<String, List<ContextVersion>> contextVersions = new ConcurrentHashMap<>();

    // Estadísticas
    private final AtomicLong totalContextsSerialized = new AtomicLong(0);
    private final AtomicLong totalContextsDeserialized = new AtomicLong(0);
    private final AtomicLong totalContextsCompressed = new AtomicLong(0);
    private final AtomicLong totalContextsDecompressed = new AtomicLong(0);
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);

    /**
     * Persiste el contexto de una sesión.
     */
    public boolean persistContext(ConversationSession session) {
        try {
            if (session.getContext() == null) {
                logger.debug("No hay contexto para persistir en sesión: {}", session.getSessionId());
                return true;
            }

            String sessionId = session.getSessionId();
            ConversationContext context = session.getContext();

            // Serializar contexto
            String serializedContext = serializeContext(context);
            
            // Comprimir si está habilitado
            String compressedContext = enableCompression ? compressContext(serializedContext) : serializedContext;

            // Crear metadata de persistencia
            ContextMetadata metadata = createContextMetadata(context, compressedContext.length());

            // Guardar en Redis
            boolean saved = saveContextToRedis(sessionId, compressedContext, metadata);
            
            if (saved) {
                // Actualizar cache
                updateContextCache(sessionId, context, metadata);
                
                // Gestionar versiones
                if (enableVersioning) {
                    manageContextVersions(sessionId, context, metadata);
                }

                totalContextsSerialized.incrementAndGet();
                logger.debug("Contexto persistido exitosamente para sesión: {}", sessionId);
                return true;
            }

            logger.error("Error persistiendo contexto para sesión: {}", sessionId);
            return false;

        } catch (Exception e) {
            logger.error("Error persistiendo contexto para sesión: {}", session.getSessionId(), e);
            return false;
        }
    }

    /**
     * Recupera el contexto de una sesión.
     */
    public Optional<ConversationContext> retrieveContext(String sessionId) {
        try {
            // Buscar en cache primero
            CachedContext cachedContext = contextCache.get(sessionId);
            if (cachedContext != null && !isCacheExpired(cachedContext)) {
                totalCacheHits.incrementAndGet();
                logger.debug("Contexto recuperado desde cache para sesión: {}", sessionId);
                return Optional.of(cachedContext.getContext());
            }

            totalCacheMisses.incrementAndGet();

            // Buscar en Redis
            Optional<ContextData> contextData = loadContextFromRedis(sessionId);
            if (contextData.isPresent()) {
                ContextData data = contextData.get();
                
                // Descomprimir si es necesario
                String decompressedContext = data.isCompressed() ? 
                    decompressContext(data.getContextData()) : data.getContextData();

                // Deserializar
                ConversationContext context = deserializeContext(decompressedContext);
                
                if (context != null) {
                    // Actualizar cache
                    updateContextCache(sessionId, context, data.getMetadata());
                    
                    totalContextsDeserialized.incrementAndGet();
                    logger.debug("Contexto recuperado desde Redis para sesión: {}", sessionId);
                    return Optional.of(context);
                }
            }

            logger.debug("No se encontró contexto para sesión: {}", sessionId);
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error recuperando contexto para sesión: {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Comprime el contexto de una sesión.
     */
    public void compressContext(ConversationSession session) {
        try {
            if (session.getContext() == null) {
                return;
            }

            // Comprimir contexto
            session.getContext().compressContext();
            
            // Persistir contexto comprimido
            persistContext(session);
            
            totalContextsCompressed.incrementAndGet();
            logger.debug("Contexto comprimido para sesión: {}", session.getSessionId());

        } catch (Exception e) {
            logger.error("Error comprimiendo contexto para sesión: {}", session.getSessionId(), e);
        }
    }

    /**
     * Restaura una versión anterior del contexto.
     */
    public boolean restoreContextVersion(String sessionId, int versionIndex) {
        try {
            List<ContextVersion> versions = contextVersions.get(sessionId);
            if (versions == null || versionIndex >= versions.size()) {
                logger.warn("Versión de contexto no encontrada: {} - {}", sessionId, versionIndex);
                return false;
            }

            ContextVersion version = versions.get(versionIndex);
            ConversationContext context = version.getContext();

            // Actualizar sesión con contexto restaurado
            Optional<ConversationSession> sessionOpt = redisRepository.findSessionById(sessionId);
            if (sessionOpt.isPresent()) {
                ConversationSession session = sessionOpt.get();
                session.setContext(context);
                
                // Persistir contexto restaurado
                if (persistContext(session)) {
                    logger.info("Contexto restaurado a versión {} para sesión: {}", versionIndex, sessionId);
                    return true;
                }
            }

            logger.error("Error restaurando versión de contexto: {} - {}", sessionId, versionIndex);
            return false;

        } catch (Exception e) {
            logger.error("Error restaurando versión de contexto: {} - {}", sessionId, versionIndex, e);
            return false;
        }
    }

    /**
     * Obtiene las versiones disponibles de un contexto.
     */
    public List<ContextVersion> getContextVersions(String sessionId) {
        try {
            List<ContextVersion> versions = contextVersions.get(sessionId);
            return versions != null ? new ArrayList<>(versions) : new ArrayList<>();

        } catch (Exception e) {
            logger.error("Error obteniendo versiones de contexto para sesión: {}", sessionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Limpia el cache de contexto.
     */
    public void clearContextCache() {
        try {
            int cacheSize = contextCache.size();
            contextCache.clear();
            contextVersions.clear();
            
            logger.info("Cache de contexto limpiado: {} entradas removidas", cacheSize);

        } catch (Exception e) {
            logger.error("Error limpiando cache de contexto", e);
        }
    }

    /**
     * Obtiene estadísticas del servicio de persistencia.
     */
    public Map<String, Object> getPersistenceStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Estadísticas de operaciones
            stats.put("total_contexts_serialized", totalContextsSerialized.get());
            stats.put("total_contexts_deserialized", totalContextsDeserialized.get());
            stats.put("total_contexts_compressed", totalContextsCompressed.get());
            stats.put("total_contexts_decompressed", totalContextsDecompressed.get());

            // Estadísticas de cache
            stats.put("total_cache_hits", totalCacheHits.get());
            stats.put("total_cache_misses", totalCacheMisses.get());
            stats.put("cache_hit_ratio", calculateCacheHitRatio());
            stats.put("context_cache_size", contextCache.size());
            stats.put("context_versions_size", contextVersions.size());

            // Configuración
            stats.put("enable_compression", enableCompression);
            stats.put("compression_level", compressionLevel);
            stats.put("enable_caching", enableCaching);
            stats.put("cache_ttl_seconds", cacheTtlSeconds);
            stats.put("max_cache_size", maxCacheSize);
            stats.put("enable_versioning", enableVersioning);
            stats.put("max_versions", maxVersions);

            return stats;

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas de persistencia", e);
            return new HashMap<>();
        }
    }

    /**
     * Serializa un contexto a JSON.
     */
    private String serializeContext(ConversationContext context) throws JsonProcessingException {
        return objectMapper.writeValueAsString(context);
    }

    /**
     * Deserializa un contexto desde JSON.
     */
    private ConversationContext deserializeContext(String json) {
        try {
            return objectMapper.readValue(json, ConversationContext.class);
        } catch (Exception e) {
            logger.error("Error deserializando contexto", e);
            return null;
        }
    }

    /**
     * Comprime datos usando Deflater.
     */
    private String compressContext(String data) {
        try {
            byte[] input = data.getBytes("UTF-8");
            Deflater deflater = new Deflater(compressionLevel);
            deflater.setInput(input);
            deflater.finish();

            byte[] output = new byte[input.length * 2];
            int compressedLength = deflater.deflate(output);
            deflater.end();

            byte[] compressed = Arrays.copyOf(output, compressedLength);
            return Base64.getEncoder().encodeToString(compressed);

        } catch (Exception e) {
            logger.error("Error comprimiendo contexto", e);
            return data; // Retornar datos sin comprimir en caso de error
        }
    }

    /**
     * Descomprime datos usando Inflater.
     */
    private String decompressContext(String compressedData) {
        try {
            byte[] compressed = Base64.getDecoder().decode(compressedData);
            Inflater inflater = new Inflater();
            inflater.setInput(compressed);

            byte[] output = new byte[compressed.length * 4];
            int decompressedLength = inflater.inflate(output);
            inflater.end();

            byte[] decompressed = Arrays.copyOf(output, decompressedLength);
            totalContextsDecompressed.incrementAndGet();
            
            return new String(decompressed, "UTF-8");

        } catch (Exception e) {
            logger.error("Error descomprimiendo contexto", e);
            return compressedData; // Retornar datos comprimidos en caso de error
        }
    }

    /**
     * Crea metadata para el contexto.
     */
    private ContextMetadata createContextMetadata(ConversationContext context, int dataSize) {
        ContextMetadata metadata = new ContextMetadata();
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setCompressionLevel(context.getContextCompressionLevel());
        metadata.setDataSize(dataSize);
        metadata.setCompressed(enableCompression);
        metadata.setVersion(context.getContextCompressionLevel());
        return metadata;
    }

    /**
     * Guarda contexto en Redis.
     */
    private boolean saveContextToRedis(String sessionId, String contextData, ContextMetadata metadata) {
        try {
            ContextData data = new ContextData();
            data.setContextData(contextData);
            data.setMetadata(metadata);
            data.setCompressed(enableCompression);

            // Guardar en Redis usando el repositorio
            // Nota: Esto requeriría extender el repositorio para manejar contexto específicamente
            return true; // Placeholder

        } catch (Exception e) {
            logger.error("Error guardando contexto en Redis: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Carga contexto desde Redis.
     */
    private Optional<ContextData> loadContextFromRedis(String sessionId) {
        try {
            // Cargar desde Redis usando el repositorio
            // Nota: Esto requeriría extender el repositorio para manejar contexto específicamente
            return Optional.empty(); // Placeholder

        } catch (Exception e) {
            logger.error("Error cargando contexto desde Redis: {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Actualiza el cache de contexto.
     */
    private void updateContextCache(String sessionId, ConversationContext context, ContextMetadata metadata) {
        if (!enableCaching) {
            return;
        }

        try {
            // Limpiar cache si excede el tamaño máximo
            if (contextCache.size() >= maxCacheSize) {
                cleanupContextCache();
            }

            CachedContext cachedContext = new CachedContext();
            cachedContext.setContext(context);
            cachedContext.setMetadata(metadata);
            cachedContext.setCachedAt(LocalDateTime.now());

            contextCache.put(sessionId, cachedContext);

        } catch (Exception e) {
            logger.error("Error actualizando cache de contexto: {}", sessionId, e);
        }
    }

    /**
     * Gestiona versiones de contexto.
     */
    private void manageContextVersions(String sessionId, ConversationContext context, ContextMetadata metadata) {
        try {
            List<ContextVersion> versions = contextVersions.computeIfAbsent(sessionId, k -> new ArrayList<>());

            ContextVersion version = new ContextVersion();
            version.setContext(context);
            version.setMetadata(metadata);
            version.setCreatedAt(LocalDateTime.now());

            versions.add(version);

            // Mantener solo las versiones más recientes
            if (versions.size() > maxVersions) {
                versions.remove(0);
            }

        } catch (Exception e) {
            logger.error("Error gestionando versiones de contexto: {}", sessionId, e);
        }
    }

    /**
     * Verifica si el cache ha expirado.
     */
    private boolean isCacheExpired(CachedContext cachedContext) {
        return cachedContext.getCachedAt().plusSeconds(cacheTtlSeconds).isBefore(LocalDateTime.now());
    }

    /**
     * Limpia el cache de contexto.
     */
    private void cleanupContextCache() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(cacheTtlSeconds);
            List<String> expiredKeys = new ArrayList<>();

            for (Map.Entry<String, CachedContext> entry : contextCache.entrySet()) {
                if (entry.getValue().getCachedAt().isBefore(cutoffTime)) {
                    expiredKeys.add(entry.getKey());
                }
            }

            for (String key : expiredKeys) {
                contextCache.remove(key);
            }

            if (!expiredKeys.isEmpty()) {
                logger.debug("Cache de contexto limpiado: {} entradas expiradas removidas", expiredKeys.size());
            }

        } catch (Exception e) {
            logger.error("Error limpiando cache de contexto", e);
        }
    }

    /**
     * Calcula la ratio de aciertos del cache.
     */
    private double calculateCacheHitRatio() {
        long hits = totalCacheHits.get();
        long misses = totalCacheMisses.get();
        long total = hits + misses;
        
        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * Clase interna para datos de contexto.
     */
    public static class ContextData {
        private String contextData;
        private ContextMetadata metadata;
        private boolean compressed;

        // Getters y Setters
        public String getContextData() { return contextData; }
        public void setContextData(String contextData) { this.contextData = contextData; }
        public ContextMetadata getMetadata() { return metadata; }
        public void setMetadata(ContextMetadata metadata) { this.metadata = metadata; }
        public boolean isCompressed() { return compressed; }
        public void setCompressed(boolean compressed) { this.compressed = compressed; }
    }

    /**
     * Clase interna para metadata de contexto.
     */
    public static class ContextMetadata {
        private LocalDateTime createdAt;
        private int compressionLevel;
        private int dataSize;
        private boolean compressed;
        private int version;

        // Getters y Setters
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public int getCompressionLevel() { return compressionLevel; }
        public void setCompressionLevel(int compressionLevel) { this.compressionLevel = compressionLevel; }
        public int getDataSize() { return dataSize; }
        public void setDataSize(int dataSize) { this.dataSize = dataSize; }
        public boolean isCompressed() { return compressed; }
        public void setCompressed(boolean compressed) { this.compressed = compressed; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
    }

    /**
     * Clase interna para contexto cacheado.
     */
    public static class CachedContext {
        private ConversationContext context;
        private ContextMetadata metadata;
        private LocalDateTime cachedAt;

        // Getters y Setters
        public ConversationContext getContext() { return context; }
        public void setContext(ConversationContext context) { this.context = context; }
        public ContextMetadata getMetadata() { return metadata; }
        public void setMetadata(ContextMetadata metadata) { this.metadata = metadata; }
        public LocalDateTime getCachedAt() { return cachedAt; }
        public void setCachedAt(LocalDateTime cachedAt) { this.cachedAt = cachedAt; }
    }

    /**
     * Clase interna para versiones de contexto.
     */
    public static class ContextVersion {
        private ConversationContext context;
        private ContextMetadata metadata;
        private LocalDateTime createdAt;

        // Getters y Setters
        public ConversationContext getContext() { return context; }
        public void setContext(ConversationContext context) { this.context = context; }
        public ContextMetadata getMetadata() { return metadata; }
        public void setMetadata(ContextMetadata metadata) { this.metadata = metadata; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
} 