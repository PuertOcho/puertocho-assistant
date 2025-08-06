package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.ConversationSession;
import com.intentmanagerms.domain.model.ConversationContext;
import com.intentmanagerms.domain.model.ConversationTurn;
import com.intentmanagerms.domain.model.ConversationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de memoria conversacional que coordina la persistencia y gestión de sesiones.
 * 
 * Proporciona:
 * - Gestión de sesiones conversacionales persistentes
 * - Compresión automática de contexto histórico
 * - Limpieza automática de sesiones expiradas
 * - Cache en memoria para sesiones activas
 * - Integración con Redis para persistencia
 */
@Service
public class ConversationMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationMemoryService.class);

    @Autowired
    private RedisConversationRepository redisRepository;

    // Configuración
    @Value("${conversation.session-ttl:3600}")
    private int sessionTtlSeconds;

    @Value("${conversation.max-history-entries:50}")
    private int maxHistoryEntries;

    @Value("${conversation.context-compression-threshold:10}")
    private int contextCompressionThreshold;

    @Value("${conversation.enable-context-compression:true}")
    private boolean enableContextCompression;

    @Value("${conversation.enable-auto-cleanup:true}")
    private boolean enableAutoCleanup;

    // Cache en memoria para sesiones activas
    private final Map<String, ConversationSession> memoryCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAccessTime = new ConcurrentHashMap<>();

    // Estadísticas
    private long totalSessionsCreated = 0;
    private long totalSessionsLoaded = 0;
    private long totalSessionsSaved = 0;
    private long totalContextCompressions = 0;
    private long totalCleanupOperations = 0;

    /**
     * Crea una nueva sesión de conversación.
     */
    public ConversationSession createSession(String userId) {
        try {
            ConversationSession session = new ConversationSession(userId);
            session.setTimeoutMinutes(sessionTtlSeconds / 60);
            session.setMaxTurns(maxHistoryEntries);

            // Guardar en Redis
            if (redisRepository.saveSession(session)) {
                // Agregar al cache en memoria
                memoryCache.put(session.getSessionId(), session);
                lastAccessTime.put(session.getSessionId(), LocalDateTime.now());
                
                totalSessionsCreated++;
                logger.info("Nueva sesión creada: {} para usuario: {}", session.getSessionId(), userId);
                return session;
            } else {
                logger.error("Error creando sesión para usuario: {}", userId);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error creando sesión para usuario: {}", userId, e);
            return null;
        }
    }

    /**
     * Obtiene una sesión existente o crea una nueva si no existe.
     */
    public ConversationSession getOrCreateSession(String sessionId, String userId) {
        try {
            // Buscar en cache en memoria primero
            ConversationSession session = memoryCache.get(sessionId);
            if (session != null) {
                updateLastAccess(sessionId);
                return session;
            }

            // Buscar en Redis
            Optional<ConversationSession> redisSession = redisRepository.findSessionById(sessionId);
            if (redisSession.isPresent()) {
                session = redisSession.get();
                // Agregar al cache en memoria
                memoryCache.put(sessionId, session);
                lastAccessTime.put(sessionId, LocalDateTime.now());
                
                totalSessionsLoaded++;
                logger.debug("Sesión cargada desde Redis: {}", sessionId);
                return session;
            }

            // Crear nueva sesión si no existe
            logger.debug("Sesión no encontrada, creando nueva: {}", sessionId);
            return createSession(userId);

        } catch (Exception e) {
            logger.error("Error obteniendo/creando sesión: {}", sessionId, e);
            return null;
        }
    }

    /**
     * Obtiene una sesión existente sin crear una nueva.
     */
    public Optional<ConversationSession> getSession(String sessionId) {
        try {
            // Buscar en cache en memoria primero
            ConversationSession session = memoryCache.get(sessionId);
            if (session != null) {
                updateLastAccess(sessionId);
                return Optional.of(session);
            }

            // Buscar en Redis
            Optional<ConversationSession> redisSession = redisRepository.findSessionById(sessionId);
            if (redisSession.isPresent()) {
                session = redisSession.get();
                // Agregar al cache en memoria
                memoryCache.put(sessionId, session);
                lastAccessTime.put(sessionId, LocalDateTime.now());
                
                totalSessionsLoaded++;
                logger.debug("Sesión cargada desde Redis: {}", sessionId);
                return Optional.of(session);
            }

            logger.debug("Sesión no encontrada: {}", sessionId);
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error obteniendo sesión: {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Guarda una sesión en memoria y Redis.
     */
    public boolean saveSession(ConversationSession session) {
        try {
            // Actualizar timestamps
            session.setUpdatedAt(LocalDateTime.now());
            session.setLastActivity(LocalDateTime.now());

            // Verificar si necesita compresión de contexto
            if (enableContextCompression && session.getContext() != null) {
                if (session.getContext().needsCompression(contextCompressionThreshold)) {
                    compressSessionContext(session);
                }
            }

            // Guardar en Redis
            if (redisRepository.saveSession(session)) {
                // Actualizar cache en memoria
                memoryCache.put(session.getSessionId(), session);
                lastAccessTime.put(session.getSessionId(), LocalDateTime.now());
                
                totalSessionsSaved++;
                logger.debug("Sesión guardada: {}", session.getSessionId());
                return true;
            } else {
                logger.error("Error guardando sesión en Redis: {}", session.getSessionId());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error guardando sesión: {}", session.getSessionId(), e);
            return false;
        }
    }

    /**
     * Actualiza una sesión existente.
     */
    public boolean updateSession(ConversationSession session) {
        return saveSession(session);
    }

    /**
     * Finaliza una sesión de conversación.
     */
    public boolean endSession(String sessionId) {
        try {
            Optional<ConversationSession> sessionOpt = getSession(sessionId);
            if (sessionOpt.isPresent()) {
                ConversationSession session = sessionOpt.get();
                session.setState(ConversationState.COMPLETED);
                session.setActive(false);
                session.setUpdatedAt(LocalDateTime.now());

                // Guardar cambios
                if (saveSession(session)) {
                    // Remover del cache en memoria después de un tiempo
                    scheduleCacheRemoval(sessionId, 300); // 5 minutos
                    
                    logger.info("Sesión finalizada: {}", sessionId);
                    return true;
                }
            }

            logger.warn("No se pudo finalizar sesión: {}", sessionId);
            return false;

        } catch (Exception e) {
            logger.error("Error finalizando sesión: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Cancela una sesión de conversación.
     */
    public boolean cancelSession(String sessionId) {
        try {
            Optional<ConversationSession> sessionOpt = getSession(sessionId);
            if (sessionOpt.isPresent()) {
                ConversationSession session = sessionOpt.get();
                session.setState(ConversationState.CANCELLED);
                session.setActive(false);
                session.setUpdatedAt(LocalDateTime.now());

                // Guardar cambios
                if (saveSession(session)) {
                    // Remover del cache en memoria después de un tiempo
                    scheduleCacheRemoval(sessionId, 300); // 5 minutos
                    
                    logger.info("Sesión cancelada: {}", sessionId);
                    return true;
                }
            }

            logger.warn("No se pudo cancelar sesión: {}", sessionId);
            return false;

        } catch (Exception e) {
            logger.error("Error cancelando sesión: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Elimina una sesión completamente.
     */
    public boolean deleteSession(String sessionId) {
        try {
            // Eliminar de Redis
            if (redisRepository.deleteSession(sessionId)) {
                // Eliminar del cache en memoria
                memoryCache.remove(sessionId);
                lastAccessTime.remove(sessionId);
                
                logger.info("Sesión eliminada: {}", sessionId);
                return true;
            }

            logger.warn("No se pudo eliminar sesión: {}", sessionId);
            return false;

        } catch (Exception e) {
            logger.error("Error eliminando sesión: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Obtiene todas las sesiones activas de un usuario.
     */
    public List<ConversationSession> getUserActiveSessions(String userId) {
        try {
            List<ConversationSession> sessions = redisRepository.findActiveSessionsByUserId(userId);
            
            // Actualizar cache en memoria
            for (ConversationSession session : sessions) {
                memoryCache.put(session.getSessionId(), session);
                lastAccessTime.put(session.getSessionId(), LocalDateTime.now());
            }

            logger.debug("Encontradas {} sesiones activas para usuario: {}", sessions.size(), userId);
            return sessions;

        } catch (Exception e) {
            logger.error("Error obteniendo sesiones activas para usuario: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene todas las sesiones activas en el sistema.
     */
    public List<ConversationSession> getAllActiveSessions() {
        try {
            List<ConversationSession> sessions = redisRepository.findAllActiveSessions();
            
            // Actualizar cache en memoria
            for (ConversationSession session : sessions) {
                memoryCache.put(session.getSessionId(), session);
                lastAccessTime.put(session.getSessionId(), LocalDateTime.now());
            }

            logger.debug("Encontradas {} sesiones activas en total", sessions.size());
            return sessions;

        } catch (Exception e) {
            logger.error("Error obteniendo todas las sesiones activas", e);
            return new ArrayList<>();
        }
    }

    /**
     * Busca sesiones por criterios específicos.
     */
    public List<ConversationSession> searchSessions(Map<String, Object> criteria) {
        try {
            List<ConversationSession> sessions = redisRepository.searchSessions(criteria);
            
            // Actualizar cache en memoria
            for (ConversationSession session : sessions) {
                memoryCache.put(session.getSessionId(), session);
                lastAccessTime.put(session.getSessionId(), LocalDateTime.now());
            }

            logger.debug("Búsqueda completada: {} sesiones encontradas", sessions.size());
            return sessions;

        } catch (Exception e) {
            logger.error("Error en búsqueda de sesiones", e);
            return new ArrayList<>();
        }
    }

    /**
     * Comprime el contexto de una sesión para optimizar memoria.
     */
    public void compressSessionContext(ConversationSession session) {
        try {
            if (session.getContext() != null) {
                session.getContext().compressContext();
                totalContextCompressions++;
                
                logger.debug("Contexto comprimido para sesión: {}", session.getSessionId());
            }

        } catch (Exception e) {
            logger.error("Error comprimiendo contexto de sesión: {}", session.getSessionId(), e);
        }
    }

    /**
     * Refresca el TTL de una sesión.
     */
    public boolean refreshSessionTtl(String sessionId) {
        try {
            if (redisRepository.refreshSessionTtl(sessionId)) {
                updateLastAccess(sessionId);
                logger.debug("TTL refrescado para sesión: {}", sessionId);
                return true;
            }
            return false;

        } catch (Exception e) {
            logger.error("Error refrescando TTL de sesión: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Limpia sesiones expiradas del sistema.
     */
    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    public void cleanupExpiredSessions() {
        if (!enableAutoCleanup) {
            return;
        }

        try {
            int cleanedCount = redisRepository.cleanupExpiredSessions();
            if (cleanedCount > 0) {
                totalCleanupOperations++;
                logger.info("Limpieza automática completada: {} sesiones expiradas eliminadas", cleanedCount);
            }

        } catch (Exception e) {
            logger.error("Error en limpieza automática de sesiones", e);
        }
    }

    /**
     * Limpia el cache en memoria de sesiones no accedidas recientemente.
     */
    @Scheduled(fixedRate = 600000) // Cada 10 minutos
    public void cleanupMemoryCache() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
            List<String> sessionsToRemove = new ArrayList<>();

            for (Map.Entry<String, LocalDateTime> entry : lastAccessTime.entrySet()) {
                if (entry.getValue().isBefore(cutoffTime)) {
                    sessionsToRemove.add(entry.getKey());
                }
            }

            for (String sessionId : sessionsToRemove) {
                memoryCache.remove(sessionId);
                lastAccessTime.remove(sessionId);
            }

            if (!sessionsToRemove.isEmpty()) {
                logger.debug("Cache en memoria limpiado: {} sesiones removidas", sessionsToRemove.size());
            }

        } catch (Exception e) {
            logger.error("Error limpiando cache en memoria", e);
        }
    }

    /**
     * Obtiene estadísticas del servicio de memoria.
     */
    public Map<String, Object> getMemoryStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Estadísticas del servicio
            stats.put("total_sessions_created", totalSessionsCreated);
            stats.put("total_sessions_loaded", totalSessionsLoaded);
            stats.put("total_sessions_saved", totalSessionsSaved);
            stats.put("total_context_compressions", totalContextCompressions);
            stats.put("total_cleanup_operations", totalCleanupOperations);

            // Estadísticas del cache en memoria
            stats.put("memory_cache_size", memoryCache.size());
            stats.put("last_access_entries", lastAccessTime.size());

            // Estadísticas de Redis
            Map<String, Object> redisStats = redisRepository.getRepositoryStatistics();
            stats.putAll(redisStats);

            // Configuración
            stats.put("session_ttl_seconds", sessionTtlSeconds);
            stats.put("max_history_entries", maxHistoryEntries);
            stats.put("context_compression_threshold", contextCompressionThreshold);
            stats.put("enable_context_compression", enableContextCompression);
            stats.put("enable_auto_cleanup", enableAutoCleanup);

            logger.debug("Estadísticas del servicio de memoria obtenidas");
            return stats;

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del servicio de memoria", e);
            return new HashMap<>();
        }
    }

    /**
     * Verifica la salud del servicio de memoria.
     */
    public boolean isHealthy() {
        try {
            // Verificar Redis
            boolean redisHealthy = redisRepository.isHealthy();
            
            // Verificar cache en memoria
            boolean memoryCacheHealthy = memoryCache.size() >= 0; // Verificación básica

            logger.debug("Health check - Redis: {}, Memory Cache: {}", redisHealthy, memoryCacheHealthy);
            return redisHealthy && memoryCacheHealthy;

        } catch (Exception e) {
            logger.error("Error en health check del servicio de memoria", e);
            return false;
        }
    }

    /**
     * Actualiza el tiempo de último acceso de una sesión.
     */
    private void updateLastAccess(String sessionId) {
        lastAccessTime.put(sessionId, LocalDateTime.now());
    }

    /**
     * Programa la remoción de una sesión del cache en memoria.
     */
    private void scheduleCacheRemoval(String sessionId, int delaySeconds) {
        // Implementación simple - en producción usar ScheduledExecutorService
        new Thread(() -> {
            try {
                Thread.sleep(delaySeconds * 1000L);
                memoryCache.remove(sessionId);
                lastAccessTime.remove(sessionId);
                logger.debug("Sesión removida del cache en memoria: {}", sessionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Remoción programada interrumpida para sesión: {}", sessionId);
            }
        }).start();
    }
} 