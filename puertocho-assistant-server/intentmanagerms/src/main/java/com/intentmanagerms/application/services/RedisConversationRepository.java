package com.intentmanagerms.application.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.domain.model.ConversationSession;
import com.intentmanagerms.domain.model.ConversationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Repositorio Redis para gestión de sesiones conversacionales persistentes.
 * 
 * Proporciona operaciones CRUD completas para sesiones de conversación con:
 * - TTL automático configurable
 * - Serialización/deserialización JSON
 * - Operaciones de búsqueda y limpieza
 * - Gestión de contexto persistente
 */
@Repository
public class RedisConversationRepository {

    private static final Logger logger = LoggerFactory.getLogger(RedisConversationRepository.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Configuración
    @Value("${conversation.session-ttl:3600}")
    private int sessionTtlSeconds;

    @Value("${conversation.max-history-entries:50}")
    private int maxHistoryEntries;

    @Value("${conversation.auto-complete-threshold:0.85}")
    private double autoCompleteThreshold;

    // Prefijos para claves Redis
    private static final String SESSION_PREFIX = "conversation:session:";
    private static final String CONTEXT_PREFIX = "conversation:context:";
    private static final String USER_SESSIONS_PREFIX = "conversation:user:";
    private static final String ACTIVE_SESSIONS_KEY = "conversation:active_sessions";
    private static final String EXPIRED_SESSIONS_KEY = "conversation:expired_sessions";

    /**
     * Guarda una sesión de conversación en Redis con TTL automático.
     */
    public boolean saveSession(ConversationSession session) {
        try {
            String sessionKey = SESSION_PREFIX + session.getSessionId();
            String contextKey = CONTEXT_PREFIX + session.getSessionId();
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();

            // Actualizar timestamps
            session.setUpdatedAt(LocalDateTime.now());
            session.setLastActivity(LocalDateTime.now());

            // Guardar sesión principal
            redisTemplate.opsForValue().set(sessionKey, session, sessionTtlSeconds, TimeUnit.SECONDS);

            // Guardar contexto por separado para optimización
            if (session.getContext() != null) {
                redisTemplate.opsForValue().set(contextKey, session.getContext(), sessionTtlSeconds, TimeUnit.SECONDS);
            }

            // Actualizar índice de sesiones por usuario
            redisTemplate.opsForSet().add(userSessionsKey, session.getSessionId());
            redisTemplate.expire(userSessionsKey, sessionTtlSeconds, TimeUnit.SECONDS);

            // Actualizar índice de sesiones activas
            if (session.isActive()) {
                redisTemplate.opsForSet().add(ACTIVE_SESSIONS_KEY, session.getSessionId());
            }

            logger.debug("Sesión guardada en Redis: {} con TTL {} segundos", session.getSessionId(), sessionTtlSeconds);
            return true;

        } catch (Exception e) {
            logger.error("Error guardando sesión en Redis: {}", session.getSessionId(), e);
            return false;
        }
    }

    /**
     * Recupera una sesión de conversación desde Redis.
     */
    public Optional<ConversationSession> findSessionById(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String contextKey = CONTEXT_PREFIX + sessionId;

            // Recuperar sesión principal
            ConversationSession session = (ConversationSession) redisTemplate.opsForValue().get(sessionKey);
            if (session == null) {
                logger.debug("Sesión no encontrada en Redis: {}", sessionId);
                return Optional.empty();
            }

            // Recuperar contexto si existe
            ConversationContext context = (ConversationContext) redisTemplate.opsForValue().get(contextKey);
            if (context != null) {
                session.setContext(context);
            }

            // Verificar si la sesión ha expirado
            if (session.isExpired()) {
                logger.debug("Sesión expirada: {}", sessionId);
                markSessionAsExpired(sessionId);
                return Optional.empty();
            }

            logger.debug("Sesión recuperada de Redis: {}", sessionId);
            return Optional.of(session);

        } catch (Exception e) {
            logger.error("Error recuperando sesión de Redis: {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Busca sesiones activas de un usuario específico.
     */
    public List<ConversationSession> findActiveSessionsByUserId(String userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
            
            List<ConversationSession> sessions = new ArrayList<>();
            if (sessionIds != null) {
                for (Object sessionIdObj : sessionIds) {
                    String sessionId = sessionIdObj.toString();
                    Optional<ConversationSession> session = findSessionById(sessionId);
                    if (session.isPresent() && session.get().isActive()) {
                        sessions.add(session.get());
                    }
                }
            }

            logger.debug("Encontradas {} sesiones activas para usuario: {}", sessions.size(), userId);
            return sessions;

        } catch (Exception e) {
            logger.error("Error buscando sesiones activas para usuario: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene todas las sesiones activas en el sistema.
     */
    public List<ConversationSession> findAllActiveSessions() {
        try {
            Set<Object> sessionIds = redisTemplate.opsForSet().members(ACTIVE_SESSIONS_KEY);
            
            List<ConversationSession> sessions = new ArrayList<>();
            if (sessionIds != null) {
                for (Object sessionIdObj : sessionIds) {
                    String sessionId = sessionIdObj.toString();
                    Optional<ConversationSession> session = findSessionById(sessionId);
                    if (session.isPresent() && session.get().isActive()) {
                        sessions.add(session.get());
                    }
                }
            }

            logger.debug("Encontradas {} sesiones activas en total", sessions.size());
            return sessions;

        } catch (Exception e) {
            logger.error("Error obteniendo todas las sesiones activas", e);
            return new ArrayList<>();
        }
    }

    /**
     * Elimina una sesión de conversación de Redis.
     */
    public boolean deleteSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String contextKey = CONTEXT_PREFIX + sessionId;

            // Recuperar sesión para obtener userId antes de eliminar
            Optional<ConversationSession> session = findSessionById(sessionId);
            if (session.isPresent()) {
                String userId = session.get().getUserId();
                String userSessionsKey = USER_SESSIONS_PREFIX + userId;

                // Eliminar de índices
                redisTemplate.opsForSet().remove(ACTIVE_SESSIONS_KEY, sessionId);
                redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            }

            // Eliminar datos principales
            redisTemplate.delete(sessionKey);
            redisTemplate.delete(contextKey);

            logger.debug("Sesión eliminada de Redis: {}", sessionId);
            return true;

        } catch (Exception e) {
            logger.error("Error eliminando sesión de Redis: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Actualiza el TTL de una sesión existente.
     */
    public boolean refreshSessionTtl(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String contextKey = CONTEXT_PREFIX + sessionId;

            // Verificar si la sesión existe
            if (!redisTemplate.hasKey(sessionKey)) {
                logger.debug("Sesión no existe para refrescar TTL: {}", sessionId);
                return false;
            }

            // Refrescar TTL
            redisTemplate.expire(sessionKey, sessionTtlSeconds, TimeUnit.SECONDS);
            redisTemplate.expire(contextKey, sessionTtlSeconds, TimeUnit.SECONDS);

            logger.debug("TTL refrescado para sesión: {}", sessionId);
            return true;

        } catch (Exception e) {
            logger.error("Error refrescando TTL de sesión: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Marca una sesión como expirada.
     */
    public boolean markSessionAsExpired(String sessionId) {
        try {
            redisTemplate.opsForSet().remove(ACTIVE_SESSIONS_KEY, sessionId);
            redisTemplate.opsForSet().add(EXPIRED_SESSIONS_KEY, sessionId);
            
            logger.debug("Sesión marcada como expirada: {}", sessionId);
            return true;

        } catch (Exception e) {
            logger.error("Error marcando sesión como expirada: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Limpia sesiones expiradas del sistema.
     */
    public int cleanupExpiredSessions() {
        try {
            Set<Object> expiredSessionIds = redisTemplate.opsForSet().members(EXPIRED_SESSIONS_KEY);
            int cleanedCount = 0;

            if (expiredSessionIds != null) {
                for (Object sessionIdObj : expiredSessionIds) {
                    String sessionId = sessionIdObj.toString();
                    if (deleteSession(sessionId)) {
                        cleanedCount++;
                    }
                }
            }

            // Limpiar lista de sesiones expiradas
            redisTemplate.delete(EXPIRED_SESSIONS_KEY);

            logger.info("Limpieza completada: {} sesiones expiradas eliminadas", cleanedCount);
            return cleanedCount;

        } catch (Exception e) {
            logger.error("Error durante limpieza de sesiones expiradas", e);
            return 0;
        }
    }

    /**
     * Obtiene estadísticas del repositorio.
     */
    public Map<String, Object> getRepositoryStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Contar sesiones activas
            Set<Object> activeSessions = redisTemplate.opsForSet().members(ACTIVE_SESSIONS_KEY);
            stats.put("active_sessions_count", activeSessions != null ? activeSessions.size() : 0);

            // Contar sesiones expiradas
            Set<Object> expiredSessions = redisTemplate.opsForSet().members(EXPIRED_SESSIONS_KEY);
            stats.put("expired_sessions_count", expiredSessions != null ? expiredSessions.size() : 0);

            // Obtener TTL configurado
            stats.put("session_ttl_seconds", sessionTtlSeconds);
            stats.put("max_history_entries", maxHistoryEntries);
            stats.put("auto_complete_threshold", autoCompleteThreshold);

            // Información de Redis
            stats.put("redis_connection_active", redisTemplate.getConnectionFactory().getConnection().ping() != null);

            logger.debug("Estadísticas del repositorio obtenidas");
            return stats;

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del repositorio", e);
            return new HashMap<>();
        }
    }

    /**
     * Verifica la salud del repositorio Redis.
     */
    public boolean isHealthy() {
        try {
            // Verificar conexión Redis
            String pingResult = redisTemplate.getConnectionFactory().getConnection().ping();
            boolean redisConnected = pingResult != null && pingResult.equals("PONG");

            // Verificar operaciones básicas
            String testKey = "health_check_" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "test", 10, TimeUnit.SECONDS);
            Object testValue = redisTemplate.opsForValue().get(testKey);
            boolean operationsWorking = "test".equals(testValue);

            logger.debug("Health check Redis - Conectado: {}, Operaciones: {}", redisConnected, operationsWorking);
            return redisConnected && operationsWorking;

        } catch (Exception e) {
            logger.error("Error en health check del repositorio Redis", e);
            return false;
        }
    }

    /**
     * Busca sesiones por criterios específicos.
     */
    public List<ConversationSession> searchSessions(Map<String, Object> criteria) {
        try {
            List<ConversationSession> results = new ArrayList<>();
            List<ConversationSession> allActiveSessions = findAllActiveSessions();

            for (ConversationSession session : allActiveSessions) {
                if (matchesCriteria(session, criteria)) {
                    results.add(session);
                }
            }

            logger.debug("Búsqueda completada: {} sesiones encontradas con criterios", results.size());
            return results;

        } catch (Exception e) {
            logger.error("Error en búsqueda de sesiones", e);
            return new ArrayList<>();
        }
    }

    /**
     * Verifica si una sesión coincide con los criterios de búsqueda.
     */
    private boolean matchesCriteria(ConversationSession session, Map<String, Object> criteria) {
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case "user_id":
                    if (!session.getUserId().equals(value)) return false;
                    break;
                case "state":
                    if (!session.getState().toString().equals(value)) return false;
                    break;
                case "current_intent":
                    if (!session.getCurrentIntent().equals(value)) return false;
                    break;
                case "is_active":
                    if (session.isActive() != (Boolean) value) return false;
                    break;
                case "created_after":
                    LocalDateTime createdAfter = (LocalDateTime) value;
                    if (session.getCreatedAt().isBefore(createdAfter)) return false;
                    break;
                case "created_before":
                    LocalDateTime createdBefore = (LocalDateTime) value;
                    if (session.getCreatedAt().isAfter(createdBefore)) return false;
                    break;
            }
        }
        return true;
    }
} 