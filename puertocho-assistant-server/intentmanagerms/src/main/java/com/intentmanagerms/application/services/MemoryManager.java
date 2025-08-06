package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.ConversationSession;
import com.intentmanagerms.domain.model.ConversationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gestor de memoria y limpieza para el sistema conversacional.
 * 
 * Proporciona:
 * - Limpieza automática de sesiones expiradas
 * - Compresión automática de contexto histórico
 * - Optimización de recursos de memoria
 * - Monitoreo de uso de memoria
 * - Gestión de cache y TTL
 */
@Service
public class MemoryManager {

    private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);

    @Autowired
    private ConversationMemoryService memoryService;

    @Autowired
    private RedisConversationRepository redisRepository;

    // Configuración
    @Value("${conversation.memory.cleanup-interval-ms:300000}")
    private long cleanupIntervalMs;

    @Value("${conversation.memory.compression-interval-ms:600000}")
    private long compressionIntervalMs;

    @Value("${conversation.memory.max-sessions-in-memory:1000}")
    private int maxSessionsInMemory;

    @Value("${conversation.memory.compression-threshold:10}")
    private int compressionThreshold;

    @Value("${conversation.memory.enable-aggressive-cleanup:false}")
    private boolean enableAggressiveCleanup;

    @Value("${conversation.memory.enable-context-compression:true}")
    private boolean enableContextCompression;

    @Value("${conversation.memory.enable-memory-monitoring:true}")
    private boolean enableMemoryMonitoring;

    // Métricas de memoria
    private final AtomicLong totalMemoryCleanups = new AtomicLong(0);
    private final AtomicLong totalContextCompressions = new AtomicLong(0);
    private final AtomicLong totalSessionsEvicted = new AtomicLong(0);
    private final AtomicLong totalMemoryOptimizations = new AtomicLong(0);

    // Cache de sesiones para optimización
    private final Map<String, LocalDateTime> sessionAccessTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessionCompressionLevels = new ConcurrentHashMap<>();

    /**
     * Ejecuta limpieza automática de memoria.
     */
    @Scheduled(fixedRateString = "${conversation.memory.cleanup-interval-ms:300000}")
    public void performMemoryCleanup() {
        try {
            logger.debug("Iniciando limpieza automática de memoria");

            // Limpiar sesiones expiradas
            memoryService.cleanupExpiredSessions();
            int expiredCleaned = 0; // La limpieza se ejecuta automáticamente
            
            // Limpiar cache en memoria
            int cacheCleaned = cleanupMemoryCache();
            
            // Optimización agresiva si está habilitada
            int aggressiveCleaned = 0;
            if (enableAggressiveCleanup) {
                aggressiveCleaned = performAggressiveCleanup();
            }

            totalMemoryCleanups.incrementAndGet();
            
            if (expiredCleaned > 0 || cacheCleaned > 0 || aggressiveCleaned > 0) {
                logger.info("Limpieza de memoria completada - Expired: {}, Cache: {}, Aggressive: {}", 
                    expiredCleaned, cacheCleaned, aggressiveCleaned);
            }

        } catch (Exception e) {
            logger.error("Error durante limpieza automática de memoria", e);
        }
    }

    /**
     * Ejecuta compresión automática de contexto.
     */
    @Scheduled(fixedRateString = "${conversation.memory.compression-interval-ms:600000}")
    public void performContextCompression() {
        if (!enableContextCompression) {
            return;
        }

        try {
            logger.debug("Iniciando compresión automática de contexto");

            List<ConversationSession> activeSessions = memoryService.getAllActiveSessions();
            int compressedCount = 0;

            for (ConversationSession session : activeSessions) {
                if (shouldCompressContext(session)) {
                    memoryService.compressSessionContext(session);
                    compressedCount++;
                    
                    // Actualizar nivel de compresión
                    sessionCompressionLevels.put(session.getSessionId(), 
                        session.getContext().getContextCompressionLevel());
                }
            }

            if (compressedCount > 0) {
                totalContextCompressions.addAndGet(compressedCount);
                logger.info("Compresión de contexto completada: {} sesiones comprimidas", compressedCount);
            }

        } catch (Exception e) {
            logger.error("Error durante compresión automática de contexto", e);
        }
    }

    /**
     * Monitorea el uso de memoria del sistema.
     */
    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    public void monitorMemoryUsage() {
        if (!enableMemoryMonitoring) {
            return;
        }

        try {
            Map<String, Object> memoryStats = getMemoryUsageStatistics();
            
            // Verificar umbrales críticos
            checkMemoryThresholds(memoryStats);
            
            // Registrar métricas
            logMemoryMetrics(memoryStats);

        } catch (Exception e) {
            logger.error("Error monitoreando uso de memoria", e);
        }
    }

    /**
     * Optimiza la memoria del sistema.
     */
    public void optimizeMemory() {
        try {
            logger.info("Iniciando optimización de memoria");

            // Limpiar sesiones menos accedidas
            int evictedCount = evictLeastAccessedSessions();
            
            // Comprimir contextos grandes
            int compressedCount = compressLargeContexts();
            
            // Optimizar cache
            int cacheOptimized = optimizeCache();

            totalMemoryOptimizations.incrementAndGet();
            
            logger.info("Optimización de memoria completada - Evicted: {}, Compressed: {}, Cache: {}", 
                evictedCount, compressedCount, cacheOptimized);

        } catch (Exception e) {
            logger.error("Error durante optimización de memoria", e);
        }
    }

    /**
     * Limpia el cache en memoria.
     */
    private int cleanupMemoryCache() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
            List<String> sessionsToRemove = new ArrayList<>();

            for (Map.Entry<String, LocalDateTime> entry : sessionAccessTimes.entrySet()) {
                if (entry.getValue().isBefore(cutoffTime)) {
                    sessionsToRemove.add(entry.getKey());
                }
            }

            for (String sessionId : sessionsToRemove) {
                sessionAccessTimes.remove(sessionId);
                sessionCompressionLevels.remove(sessionId);
            }

            return sessionsToRemove.size();

        } catch (Exception e) {
            logger.error("Error limpiando cache en memoria", e);
            return 0;
        }
    }

    /**
     * Realiza limpieza agresiva de memoria.
     */
    private int performAggressiveCleanup() {
        try {
            int cleanedCount = 0;
            List<ConversationSession> allSessions = memoryService.getAllActiveSessions();

            // Ordenar por último acceso
            allSessions.sort((s1, s2) -> {
                LocalDateTime time1 = sessionAccessTimes.getOrDefault(s1.getSessionId(), s1.getCreatedAt());
                LocalDateTime time2 = sessionAccessTimes.getOrDefault(s2.getSessionId(), s2.getCreatedAt());
                return time1.compareTo(time2);
            });

            // Eliminar sesiones más antiguas si excedemos el límite
            if (allSessions.size() > maxSessionsInMemory) {
                int toRemove = allSessions.size() - maxSessionsInMemory;
                for (int i = 0; i < toRemove; i++) {
                    ConversationSession session = allSessions.get(i);
                    if (memoryService.deleteSession(session.getSessionId())) {
                        cleanedCount++;
                        totalSessionsEvicted.incrementAndGet();
                    }
                }
            }

            return cleanedCount;

        } catch (Exception e) {
            logger.error("Error durante limpieza agresiva", e);
            return 0;
        }
    }

    /**
     * Verifica si se debe comprimir el contexto de una sesión.
     */
    private boolean shouldCompressContext(ConversationSession session) {
        try {
            if (session.getContext() == null) {
                return false;
            }

            // Verificar si ya necesita compresión
            if (session.getContext().needsCompression(compressionThreshold)) {
                return true;
            }

            // Verificar nivel de compresión actual
            Integer currentLevel = sessionCompressionLevels.get(session.getSessionId());
            if (currentLevel == null) {
                currentLevel = 0;
            }

            // Comprimir si el nivel es bajo y la sesión es antigua
            LocalDateTime lastAccess = sessionAccessTimes.getOrDefault(session.getSessionId(), session.getCreatedAt());
            boolean isOldSession = lastAccess.isBefore(LocalDateTime.now().minusHours(1));
            
            return currentLevel < 3 && isOldSession;

        } catch (Exception e) {
            logger.error("Error verificando compresión de contexto para sesión: {}", session.getSessionId(), e);
            return false;
        }
    }

    /**
     * Expulsa las sesiones menos accedidas.
     */
    private int evictLeastAccessedSessions() {
        try {
            List<ConversationSession> allSessions = memoryService.getAllActiveSessions();
            
            // Ordenar por último acceso
            allSessions.sort((s1, s2) -> {
                LocalDateTime time1 = sessionAccessTimes.getOrDefault(s1.getSessionId(), s1.getCreatedAt());
                LocalDateTime time2 = sessionAccessTimes.getOrDefault(s2.getSessionId(), s2.getCreatedAt());
                return time1.compareTo(time2);
            });

            int evictedCount = 0;
            int targetSize = maxSessionsInMemory / 2; // Reducir a la mitad

            for (int i = 0; i < allSessions.size() - targetSize; i++) {
                ConversationSession session = allSessions.get(i);
                if (memoryService.deleteSession(session.getSessionId())) {
                    evictedCount++;
                    totalSessionsEvicted.incrementAndGet();
                }
            }

            return evictedCount;

        } catch (Exception e) {
            logger.error("Error expulsando sesiones menos accedidas", e);
            return 0;
        }
    }

    /**
     * Comprime contextos grandes.
     */
    private int compressLargeContexts() {
        try {
            List<ConversationSession> activeSessions = memoryService.getAllActiveSessions();
            int compressedCount = 0;

            for (ConversationSession session : activeSessions) {
                if (session.getContext() != null && session.getContext().needsCompression(5)) {
                    memoryService.compressSessionContext(session);
                    compressedCount++;
                }
            }

            return compressedCount;

        } catch (Exception e) {
            logger.error("Error comprimiendo contextos grandes", e);
            return 0;
        }
    }

    /**
     * Optimiza el cache del sistema.
     */
    private int optimizeCache() {
        try {
            // Limpiar entradas antiguas del cache
            int cleanedCount = cleanupMemoryCache();
            
            // Actualizar tiempos de acceso
            List<ConversationSession> activeSessions = memoryService.getAllActiveSessions();
            for (ConversationSession session : activeSessions) {
                sessionAccessTimes.put(session.getSessionId(), LocalDateTime.now());
            }

            return cleanedCount;

        } catch (Exception e) {
            logger.error("Error optimizando cache", e);
            return 0;
        }
    }

    /**
     * Obtiene estadísticas de uso de memoria.
     */
    public Map<String, Object> getMemoryUsageStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Estadísticas del gestor
            stats.put("total_memory_cleanups", totalMemoryCleanups.get());
            stats.put("total_context_compressions", totalContextCompressions.get());
            stats.put("total_sessions_evicted", totalSessionsEvicted.get());
            stats.put("total_memory_optimizations", totalMemoryOptimizations.get());

            // Estadísticas de cache
            stats.put("session_access_times_size", sessionAccessTimes.size());
            stats.put("session_compression_levels_size", sessionCompressionLevels.size());

            // Estadísticas del sistema
            Map<String, Object> memoryStats = memoryService.getMemoryStatistics();
            stats.putAll(memoryStats);

            // Información de configuración
            stats.put("cleanup_interval_ms", cleanupIntervalMs);
            stats.put("compression_interval_ms", compressionIntervalMs);
            stats.put("max_sessions_in_memory", maxSessionsInMemory);
            stats.put("compression_threshold", compressionThreshold);
            stats.put("enable_aggressive_cleanup", enableAggressiveCleanup);
            stats.put("enable_context_compression", enableContextCompression);
            stats.put("enable_memory_monitoring", enableMemoryMonitoring);

            return stats;

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas de uso de memoria", e);
            return new HashMap<>();
        }
    }

    /**
     * Verifica umbrales críticos de memoria.
     */
    private void checkMemoryThresholds(Map<String, Object> memoryStats) {
        try {
            Integer activeSessions = (Integer) memoryStats.get("active_sessions_count");
            Integer memoryCacheSize = (Integer) memoryStats.get("memory_cache_size");

            if (activeSessions != null && activeSessions > maxSessionsInMemory * 0.8) {
                logger.warn("Umbral de sesiones activas alcanzado: {} (máximo: {})", 
                    activeSessions, maxSessionsInMemory);
            }

            if (memoryCacheSize != null && memoryCacheSize > maxSessionsInMemory * 0.9) {
                logger.warn("Umbral de cache en memoria alcanzado: {} (máximo: {})", 
                    memoryCacheSize, maxSessionsInMemory);
            }

        } catch (Exception e) {
            logger.error("Error verificando umbrales de memoria", e);
        }
    }

    /**
     * Registra métricas de memoria.
     */
    private void logMemoryMetrics(Map<String, Object> memoryStats) {
        try {
            logger.debug("Métricas de memoria - Cleanups: {}, Compressions: {}, Evicted: {}, Optimizations: {}", 
                totalMemoryCleanups.get(),
                totalContextCompressions.get(),
                totalSessionsEvicted.get(),
                totalMemoryOptimizations.get());

        } catch (Exception e) {
            logger.error("Error registrando métricas de memoria", e);
        }
    }

    /**
     * Actualiza el tiempo de acceso de una sesión.
     */
    public void updateSessionAccess(String sessionId) {
        sessionAccessTimes.put(sessionId, LocalDateTime.now());
    }

    /**
     * Verifica la salud del gestor de memoria.
     */
    public boolean isHealthy() {
        try {
            // Verificar servicios dependientes
            boolean memoryServiceHealthy = memoryService.isHealthy();
            boolean redisHealthy = redisRepository.isHealthy();

            // Verificar cache interno
            boolean cacheHealthy = sessionAccessTimes.size() >= 0;

            logger.debug("Health check MemoryManager - MemoryService: {}, Redis: {}, Cache: {}", 
                memoryServiceHealthy, redisHealthy, cacheHealthy);

            return memoryServiceHealthy && redisHealthy && cacheHealthy;

        } catch (Exception e) {
            logger.error("Error en health check del gestor de memoria", e);
            return false;
        }
    }
} 