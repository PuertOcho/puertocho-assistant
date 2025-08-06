package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.ConversationMemoryService;
import com.intentmanagerms.application.services.MemoryManager;
import com.intentmanagerms.application.services.ContextPersistenceService;
import com.intentmanagerms.domain.model.ConversationSession;
import com.intentmanagerms.domain.model.ConversationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para gestión de memoria conversacional.
 * 
 * Proporciona endpoints para:
 * - Gestión de sesiones conversacionales
 * - Operaciones de memoria y contexto
 * - Estadísticas y monitoreo
 * - Limpieza y optimización
 */
@RestController
@RequestMapping("/api/v1/conversation-memory")
@CrossOrigin(origins = "*")
public class ConversationMemoryController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationMemoryController.class);

    @Autowired
    private ConversationMemoryService memoryService;

    @Autowired
    private MemoryManager memoryManager;

    @Autowired
    private ContextPersistenceService contextPersistenceService;

    /**
     * Health check del sistema de memoria conversacional.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            boolean memoryServiceHealthy = memoryService.isHealthy();
            boolean memoryManagerHealthy = memoryManager.isHealthy();
            
            response.put("status", "UP");
            response.put("memory_service_healthy", memoryServiceHealthy);
            response.put("memory_manager_healthy", memoryManagerHealthy);
            response.put("overall_healthy", memoryServiceHealthy && memoryManagerHealthy);
            response.put("timestamp", System.currentTimeMillis());

            if (memoryServiceHealthy && memoryManagerHealthy) {
                logger.debug("Health check del sistema de memoria conversacional: OK");
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Health check del sistema de memoria conversacional: DEGRADED");
                return ResponseEntity.status(503).body(response);
            }

        } catch (Exception e) {
            logger.error("Error en health check del sistema de memoria conversacional", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "DOWN");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(errorResponse);
        }
    }

    /**
     * Obtiene estadísticas del sistema de memoria conversacional.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Estadísticas del servicio de memoria
            Map<String, Object> memoryStats = memoryService.getMemoryStatistics();
            response.put("memory_service", memoryStats);
            
            // Estadísticas del gestor de memoria
            Map<String, Object> managerStats = memoryManager.getMemoryUsageStatistics();
            response.put("memory_manager", managerStats);
            
            // Estadísticas de persistencia de contexto
            Map<String, Object> persistenceStats = contextPersistenceService.getPersistenceStatistics();
            response.put("context_persistence", persistenceStats);
            
            response.put("timestamp", System.currentTimeMillis());

            logger.debug("Estadísticas del sistema de memoria conversacional obtenidas");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del sistema de memoria conversacional", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Crea una nueva sesión de conversación.
     */
    @PostMapping("/session")
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("user_id");
            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "user_id es requerido");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            ConversationSession session = memoryService.createSession(userId);
            if (session != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("session_id", session.getSessionId());
                response.put("user_id", session.getUserId());
                response.put("state", session.getState().toString());
                response.put("created_at", session.getCreatedAt());
                response.put("success", true);
                
                logger.info("Nueva sesión creada: {} para usuario: {}", session.getSessionId(), userId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No se pudo crear la sesión");
                return ResponseEntity.status(500).body(errorResponse);
            }

        } catch (Exception e) {
            logger.error("Error creando sesión", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Obtiene una sesión existente.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        try {
            Optional<ConversationSession> sessionOpt = memoryService.getSession(sessionId);
            if (sessionOpt.isPresent()) {
                ConversationSession session = sessionOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("session_id", session.getSessionId());
                response.put("user_id", session.getUserId());
                response.put("state", session.getState().toString());
                response.put("turn_count", session.getTurnCount());
                response.put("is_active", session.isActive());
                response.put("created_at", session.getCreatedAt());
                response.put("last_activity", session.getLastActivity());
                response.put("success", true);
                
                logger.debug("Sesión obtenida: {}", sessionId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Sesión no encontrada");
                errorResponse.put("session_id", sessionId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error obteniendo sesión: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Finaliza una sesión de conversación.
     */
    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endSession(@PathVariable String sessionId) {
        try {
            boolean ended = memoryService.endSession(sessionId);
            if (ended) {
                Map<String, Object> response = new HashMap<>();
                response.put("session_id", sessionId);
                response.put("action", "ended");
                response.put("success", true);
                
                logger.info("Sesión finalizada: {}", sessionId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No se pudo finalizar la sesión");
                errorResponse.put("session_id", sessionId);
                return ResponseEntity.status(500).body(errorResponse);
            }

        } catch (Exception e) {
            logger.error("Error finalizando sesión: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Cancela una sesión de conversación.
     */
    @PostMapping("/session/{sessionId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSession(@PathVariable String sessionId) {
        try {
            boolean cancelled = memoryService.cancelSession(sessionId);
            if (cancelled) {
                Map<String, Object> response = new HashMap<>();
                response.put("session_id", sessionId);
                response.put("action", "cancelled");
                response.put("success", true);
                
                logger.info("Sesión cancelada: {}", sessionId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No se pudo cancelar la sesión");
                errorResponse.put("session_id", sessionId);
                return ResponseEntity.status(500).body(errorResponse);
            }

        } catch (Exception e) {
            logger.error("Error cancelando sesión: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Elimina una sesión completamente.
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        try {
            boolean deleted = memoryService.deleteSession(sessionId);
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("session_id", sessionId);
                response.put("action", "deleted");
                response.put("success", true);
                
                logger.info("Sesión eliminada: {}", sessionId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No se pudo eliminar la sesión");
                errorResponse.put("session_id", sessionId);
                return ResponseEntity.status(500).body(errorResponse);
            }

        } catch (Exception e) {
            logger.error("Error eliminando sesión: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Obtiene todas las sesiones activas de un usuario.
     */
    @GetMapping("/user/{userId}/sessions")
    public ResponseEntity<Map<String, Object>> getUserSessions(@PathVariable String userId) {
        try {
            List<ConversationSession> sessions = memoryService.getUserActiveSessions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user_id", userId);
            response.put("sessions_count", sessions.size());
            response.put("sessions", sessions);
            response.put("success", true);
            
            logger.debug("Sesiones obtenidas para usuario: {} - {} sesiones", userId, sessions.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo sesiones para usuario: {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Obtiene todas las sesiones activas en el sistema.
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<Map<String, Object>> getAllActiveSessions() {
        try {
            List<ConversationSession> sessions = memoryService.getAllActiveSessions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessions_count", sessions.size());
            response.put("sessions", sessions);
            response.put("success", true);
            
            logger.debug("Todas las sesiones activas obtenidas: {} sesiones", sessions.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo todas las sesiones activas", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Busca sesiones por criterios específicos.
     */
    @PostMapping("/sessions/search")
    public ResponseEntity<Map<String, Object>> searchSessions(@RequestBody Map<String, Object> criteria) {
        try {
            List<ConversationSession> sessions = memoryService.searchSessions(criteria);
            
            Map<String, Object> response = new HashMap<>();
            response.put("criteria", criteria);
            response.put("sessions_count", sessions.size());
            response.put("sessions", sessions);
            response.put("success", true);
            
            logger.debug("Búsqueda de sesiones completada: {} sesiones encontradas", sessions.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error buscando sesiones", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Comprime el contexto de una sesión.
     */
    @PostMapping("/session/{sessionId}/compress-context")
    public ResponseEntity<Map<String, Object>> compressSessionContext(@PathVariable String sessionId) {
        try {
            Optional<ConversationSession> sessionOpt = memoryService.getSession(sessionId);
            if (sessionOpt.isPresent()) {
                ConversationSession session = sessionOpt.get();
                memoryService.compressSessionContext(session);
                
                Map<String, Object> response = new HashMap<>();
                response.put("session_id", sessionId);
                response.put("action", "context_compressed");
                response.put("compression_level", session.getContext().getContextCompressionLevel());
                response.put("success", true);
                
                logger.info("Contexto comprimido para sesión: {}", sessionId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Sesión no encontrada");
                errorResponse.put("session_id", sessionId);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error comprimiendo contexto de sesión: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Optimiza la memoria del sistema.
     */
    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeMemory() {
        try {
            memoryManager.optimizeMemory();
            
            Map<String, Object> response = new HashMap<>();
            response.put("action", "memory_optimized");
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Optimización de memoria ejecutada");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error optimizando memoria", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Limpia el cache de contexto.
     */
    @PostMapping("/context/cache/clear")
    public ResponseEntity<Map<String, Object>> clearContextCache() {
        try {
            contextPersistenceService.clearContextCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("action", "context_cache_cleared");
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("Cache de contexto limpiado");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error limpiando cache de contexto", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Obtiene las versiones de contexto de una sesión.
     */
    @GetMapping("/session/{sessionId}/context/versions")
    public ResponseEntity<Map<String, Object>> getContextVersions(@PathVariable String sessionId) {
        try {
            List<ContextPersistenceService.ContextVersion> versions = contextPersistenceService.getContextVersions(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("session_id", sessionId);
            response.put("versions_count", versions.size());
            response.put("versions", versions);
            response.put("success", true);
            
            logger.debug("Versiones de contexto obtenidas para sesión: {} - {} versiones", sessionId, versions.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo versiones de contexto para sesión: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Restaura una versión anterior del contexto.
     */
    @PostMapping("/session/{sessionId}/context/restore/{versionIndex}")
    public ResponseEntity<Map<String, Object>> restoreContextVersion(
            @PathVariable String sessionId, 
            @PathVariable int versionIndex) {
        try {
            boolean restored = contextPersistenceService.restoreContextVersion(sessionId, versionIndex);
            if (restored) {
                Map<String, Object> response = new HashMap<>();
                response.put("session_id", sessionId);
                response.put("version_index", versionIndex);
                response.put("action", "context_restored");
                response.put("success", true);
                
                logger.info("Contexto restaurado a versión {} para sesión: {}", versionIndex, sessionId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No se pudo restaurar la versión del contexto");
                errorResponse.put("session_id", sessionId);
                errorResponse.put("version_index", versionIndex);
                return ResponseEntity.status(500).body(errorResponse);
            }

        } catch (Exception e) {
            logger.error("Error restaurando versión de contexto: {} - {}", sessionId, versionIndex, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Test endpoint para verificar funcionalidad.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("service", "ConversationMemoryController");
            response.put("status", "operational");
            response.put("timestamp", System.currentTimeMillis());
            response.put("features", List.of(
                "session_management",
                "context_persistence", 
                "memory_optimization",
                "context_compression",
                "version_management"
            ));
            
            logger.debug("Test endpoint ejecutado exitosamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en test endpoint", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
} 