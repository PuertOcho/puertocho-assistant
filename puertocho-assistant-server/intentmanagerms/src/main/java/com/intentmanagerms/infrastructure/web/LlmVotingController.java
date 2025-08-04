package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.LlmVotingService;
import com.intentmanagerms.application.services.VotingConfigurationInitializationService;
import com.intentmanagerms.domain.model.VotingRound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el sistema de votación MoE (Mixture of Experts).
 * Proporciona endpoints para ejecutar rondas de votación y obtener información del sistema.
 */
@RestController
@RequestMapping("/api/v1/voting")
@CrossOrigin(origins = "*")
public class LlmVotingController {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmVotingController.class);
    
    @Autowired
    private LlmVotingService llmVotingService;
    
    @Autowired
    private VotingConfigurationInitializationService votingConfigurationService;
    
    /**
     * Ejecuta una ronda de votación para clasificar la intención del usuario.
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeVotingRound(
            @RequestBody Map<String, Object> request) {
        
        try {
            logger.info("Recibida solicitud de votación: {}", request);
            
            String requestId = (String) request.get("requestId");
            String userMessage = (String) request.get("userMessage");
            @SuppressWarnings("unchecked")
            Map<String, Object> conversationContext = (Map<String, Object>) request.get("conversationContext");
            @SuppressWarnings("unchecked")
            List<String> conversationHistory = (List<String>) request.get("conversationHistory");
            
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("userMessage es requerido"));
            }
            
            if (requestId == null) {
                requestId = "req_" + System.currentTimeMillis();
            }
            
            // Ejecutar ronda de votación
            VotingRound round = llmVotingService.executeVotingRound(
                requestId, userMessage, conversationContext, conversationHistory);
            
            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roundId", round.getRoundId());
            response.put("requestId", round.getRequestId());
            response.put("status", round.getStatus().toString());
            response.put("durationMs", round.getDurationMs());
            
            if (round.getConsensus() != null) {
                Map<String, Object> consensus = new HashMap<>();
                consensus.put("finalIntent", round.getConsensus().getFinalIntent());
                consensus.put("finalEntities", round.getConsensus().getFinalEntities());
                consensus.put("consensusConfidence", round.getConsensus().getConsensusConfidence());
                consensus.put("participatingVotes", round.getConsensus().getParticipatingVotes());
                consensus.put("totalVotes", round.getConsensus().getTotalVotes());
                consensus.put("agreementLevel", round.getConsensus().getAgreementLevel().toString());
                consensus.put("consensusMethod", round.getConsensus().getConsensusMethod());
                consensus.put("reasoning", round.getConsensus().getReasoning());
                consensus.put("finalSubtasks", round.getConsensus().getFinalSubtasks());
                response.put("consensus", consensus);
            }
            
            if (round.getVotes() != null) {
                response.put("votesCount", round.getVotes().size());
                response.put("validVotesCount", round.getVotes().stream()
                    .filter(vote -> vote.isValid()).count());
            }
            
            if (round.getErrorMessage() != null) {
                response.put("errorMessage", round.getErrorMessage());
            }
            
            logger.info("Ronda de votación completada: {}", round.getRoundId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error ejecutando ronda de votación: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    /**
     * Ejecuta una ronda de votación simple (solo con mensaje de usuario).
     */
    @PostMapping("/execute/simple")
    public ResponseEntity<Map<String, Object>> executeSimpleVotingRound(
            @RequestBody Map<String, String> request) {
        
        try {
            String userMessage = request.get("userMessage");
            
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("userMessage es requerido"));
            }
            
            String requestId = "simple_" + System.currentTimeMillis();
            
            // Ejecutar ronda de votación simple
            VotingRound round = llmVotingService.executeVotingRound(
                requestId, userMessage, null, null);
            
            // Construir respuesta simplificada
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roundId", round.getRoundId());
            response.put("userMessage", userMessage);
            response.put("status", round.getStatus().toString());
            response.put("durationMs", round.getDurationMs());
            
            if (round.getConsensus() != null) {
                response.put("finalIntent", round.getConsensus().getFinalIntent());
                response.put("finalEntities", round.getConsensus().getFinalEntities());
                response.put("confidence", round.getConsensus().getConsensusConfidence());
                response.put("agreementLevel", round.getConsensus().getAgreementLevel().toString());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error ejecutando ronda de votación simple: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene estadísticas del sistema de votación.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getVotingStatistics() {
        try {
            Map<String, Object> stats = llmVotingService.getVotingStatistics();
            stats.put("success", true);
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas de votación: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error obteniendo estadísticas: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene estadísticas de la configuración de votación.
     */
    @GetMapping("/configuration/statistics")
    public ResponseEntity<Map<String, Object>> getConfigurationStatistics() {
        try {
            Map<String, Object> stats = votingConfigurationService.getStatistics();
            stats.put("success", true);
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas de configuración: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error obteniendo estadísticas de configuración: " + e.getMessage()));
        }
    }
    
    /**
     * Verifica la salud del sistema de votación.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getVotingHealth() {
        try {
            boolean votingHealthy = llmVotingService.isHealthy();
            boolean configHealthy = votingConfigurationService.isHealthy();
            boolean overallHealthy = votingHealthy && configHealthy;
            
            Map<String, Object> health = new HashMap<>();
            health.put("success", true);
            health.put("overall", overallHealthy ? "HEALTHY" : "UNHEALTHY");
            health.put("voting_service", votingHealthy ? "HEALTHY" : "UNHEALTHY");
            health.put("configuration_service", configHealthy ? "HEALTHY" : "UNHEALTHY");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error verificando salud del sistema de votación: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error verificando salud: " + e.getMessage()));
        }
    }
    
    /**
     * Recarga forzada de la configuración de votación.
     */
    @PostMapping("/configuration/reload")
    public ResponseEntity<Map<String, Object>> reloadConfiguration() {
        try {
            logger.info("Solicitud de recarga forzada de configuración de votación");
            
            votingConfigurationService.forceReload();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuración recargada exitosamente");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error recargando configuración: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error recargando configuración: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene información detallada de la configuración actual.
     */
    @GetMapping("/configuration/info")
    public ResponseEntity<Map<String, Object>> getConfigurationInfo() {
        try {
            var config = votingConfigurationService.getCurrentConfiguration();
            
            if (config == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> info = new HashMap<>();
            info.put("success", true);
            info.put("version", config.getVersion());
            info.put("description", config.getDescription());
            info.put("timestamp", System.currentTimeMillis());
            
            if (config.getVotingSystem() != null) {
                Map<String, Object> votingSystem = new HashMap<>();
                votingSystem.put("enabled", config.getVotingSystem().isEnabled());
                votingSystem.put("maxDebateRounds", config.getVotingSystem().getMaxDebateRounds());
                votingSystem.put("consensusThreshold", config.getVotingSystem().getConsensusThreshold());
                votingSystem.put("timeoutPerVote", config.getVotingSystem().getTimeoutPerVote());
                votingSystem.put("parallelVoting", config.getVotingSystem().isParallelVoting());
                votingSystem.put("participantsCount", config.getVotingSystem().getLlmParticipants().size());
                info.put("votingSystem", votingSystem);
            }
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            logger.error("Error obteniendo información de configuración: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error obteniendo información: " + e.getMessage()));
        }
    }
    
    /**
     * Test del sistema de votación con un mensaje de prueba.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testVotingSystem(
            @RequestBody(required = false) Map<String, String> request) {
        
        try {
            String testMessage = "¿qué tiempo hace en Madrid?";
            if (request != null && request.containsKey("testMessage")) {
                testMessage = request.get("testMessage");
            }
            
            logger.info("Ejecutando test del sistema de votación con mensaje: '{}'", testMessage);
            
            String requestId = "test_" + System.currentTimeMillis();
            
            // Ejecutar ronda de votación de prueba
            VotingRound round = llmVotingService.executeVotingRound(
                requestId, testMessage, null, null);
            
            // Construir respuesta de test
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("testMessage", testMessage);
            response.put("roundId", round.getRoundId());
            response.put("status", round.getStatus().toString());
            response.put("durationMs", round.getDurationMs());
            response.put("testType", "voting_system_test");
            
            if (round.getConsensus() != null) {
                response.put("finalIntent", round.getConsensus().getFinalIntent());
                response.put("confidence", round.getConsensus().getConsensusConfidence());
                response.put("agreementLevel", round.getConsensus().getAgreementLevel().toString());
            }
            
            if (round.getVotes() != null) {
                response.put("totalVotes", round.getVotes().size());
                response.put("validVotes", round.getVotes().stream().filter(v -> v.isValid()).count());
            }
            
            logger.info("Test del sistema de votación completado exitosamente");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error en test del sistema de votación: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Error en test: " + e.getMessage()));
        }
    }
    
    /**
     * Crea una respuesta de error estandarizada.
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
} 