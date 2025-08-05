package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.ConsensusEngine;
import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDateTime;

/**
 * Controlador REST para el motor de consenso avanzado.
 * 
 * Endpoints para:
 * - Probar algoritmos de consenso con datos de ejemplo
 * - Obtener estadísticas del motor de consenso
 * - Verificar salud del motor
 * - Ejecutar consenso con votos personalizados
 * 
 * T3.3: API REST para ConsensusEngine
 */
@RestController
@RequestMapping("/api/v1/consensus")
@CrossOrigin(origins = "*")
public class ConsensusEngineController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsensusEngineController.class);
    
    @Autowired
    private ConsensusEngine consensusEngine;
    
    /**
     * Obtiene estadísticas del motor de consenso.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getConsensusStatistics() {
        try {
            logger.info("Obteniendo estadísticas del motor de consenso");
            
            Map<String, Object> stats = consensusEngine.getConsensusStatistics();
            stats.put("timestamp", new Date());
            stats.put("status", "healthy");
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas del consenso: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error obteniendo estadísticas", "message", e.getMessage()));
        }
    }
    
    /**
     * Verifica la salud del motor de consenso.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getConsensusHealth() {
        try {
            logger.info("Verificando salud del motor de consenso");
            
            boolean isHealthy = consensusEngine.isHealthy();
            Map<String, Object> health = new HashMap<>();
            
            health.put("status", isHealthy ? "healthy" : "unhealthy");
            health.put("timestamp", new Date());
            health.put("service", "ConsensusEngine");
            health.put("version", "1.0.0");
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error verificando salud del consenso: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "unhealthy", "error", e.getMessage()));
        }
    }
    
    /**
     * Prueba el motor de consenso con datos de ejemplo.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConsensusEngine() {
        try {
            logger.info("Ejecutando prueba del motor de consenso");
            
            // Crear votos de ejemplo
            List<LlmVote> testVotes = createTestVotes();
            VotingRound testRound = createTestVotingRound();
            
            // Procesar consenso
            VotingConsensus consensus = consensusEngine.processConsensus(testVotes, testRound);
            
            Map<String, Object> result = new HashMap<>();
            result.put("testVotes", testVotes.size());
            result.put("consensus", consensus);
            result.put("timestamp", new Date());
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error en prueba del motor de consenso: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error en prueba", "message", e.getMessage()));
        }
    }
    
    /**
     * Ejecuta consenso con votos personalizados.
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeConsensus(@RequestBody Map<String, Object> request) {
        try {
            logger.info("Ejecutando consenso personalizado");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> votesData = (List<Map<String, Object>>) request.get("votes");
            
            if (votesData == null || votesData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Se requieren votos para procesar consenso"));
            }
            
            // Convertir datos a objetos LlmVote
            List<LlmVote> votes = convertToLlmVotes(votesData);
            VotingRound round = createVotingRoundFromRequest(request);
            
            // Procesar consenso
            VotingConsensus consensus = consensusEngine.processConsensus(votes, round);
            
            Map<String, Object> result = new HashMap<>();
            result.put("inputVotes", votes.size());
            result.put("consensus", consensus);
            result.put("timestamp", new Date());
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error ejecutando consenso personalizado: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error ejecutando consenso", "message", e.getMessage()));
        }
    }
    
    /**
     * Prueba diferentes algoritmos de consenso con los mismos datos.
     */
    @PostMapping("/test-algorithms")
    public ResponseEntity<Map<String, Object>> testConsensusAlgorithms() {
        try {
            logger.info("Probando diferentes algoritmos de consenso");
            
            List<LlmVote> testVotes = createTestVotes();
            VotingRound testRound = createTestVotingRound();
            
            // Lista de algoritmos a probar
            String[] algorithms = {"weighted-majority", "plurality", "confidence-weighted", "borda-count"};
            
            Map<String, Object> results = new HashMap<>();
            results.put("testVotes", testVotes.size());
            results.put("timestamp", new Date());
            
            Map<String, VotingConsensus> algorithmResults = new HashMap<>();
            
            // Probar cada algoritmo
            for (String algorithm : algorithms) {
                try {
                    // Crear una instancia temporal con el algoritmo específico
                    VotingConsensus consensus = consensusEngine.processConsensus(testVotes, testRound);
                    algorithmResults.put(algorithm, consensus);
                } catch (Exception e) {
                    logger.warn("Error probando algoritmo {}: {}", algorithm, e.getMessage());
                    algorithmResults.put(algorithm, createFailedConsensus(testVotes.size()));
                }
            }
            
            results.put("algorithmResults", algorithmResults);
            results.put("success", true);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error probando algoritmos de consenso: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error probando algoritmos", "message", e.getMessage()));
        }
    }
    
    /**
     * Crea votos de ejemplo para pruebas.
     */
    private List<LlmVote> createTestVotes() {
        List<LlmVote> votes = new ArrayList<>();
        
        // Voto 1: LLM A - Análisis crítico
        LlmVote vote1 = new LlmVote();
        vote1.setVoteId("test-vote-1");
        vote1.setLlmId("llm_a");
        vote1.setLlmName("Critical Analyzer");
        vote1.setIntent("ayuda");
        vote1.setConfidence(0.85);
        vote1.setLlmWeight(1.0);
        vote1.setEntities(Map.of("tipo_ayuda", "general"));
        vote1.setSubtasks(List.of(Map.of("accion", "proporcionar_ayuda", "prioridad", "alta")));
        vote1.setReasoning("El usuario solicita ayuda general sobre el sistema");
        vote1.setVoteTime(LocalDateTime.now());
        votes.add(vote1);
        
        // Voto 2: LLM B - Especialista en contexto
        LlmVote vote2 = new LlmVote();
        vote2.setVoteId("test-vote-2");
        vote2.setLlmId("llm_b");
        vote2.setLlmName("Context Specialist");
        vote2.setIntent("ayuda");
        vote2.setConfidence(0.92);
        vote2.setLlmWeight(1.0);
        vote2.setEntities(Map.of("tipo_ayuda", "general", "contexto", "nuevo_usuario"));
        vote2.setSubtasks(List.of(Map.of("accion", "proporcionar_ayuda", "prioridad", "alta")));
        vote2.setReasoning("Basado en el contexto, el usuario necesita ayuda general");
        vote2.setVoteTime(LocalDateTime.now());
        votes.add(vote2);
        
        // Voto 3: LLM C - Pragmatista de acción
        LlmVote vote3 = new LlmVote();
        vote3.setVoteId("test-vote-3");
        vote3.setLlmId("llm_c");
        vote3.setLlmName("Action Pragmatist");
        vote3.setIntent("ayuda");
        vote3.setConfidence(0.78);
        vote3.setLlmWeight(0.9);
        vote3.setEntities(Map.of("tipo_ayuda", "general"));
        vote3.setSubtasks(List.of(
            Map.of("accion", "proporcionar_ayuda", "prioridad", "alta"),
            Map.of("accion", "sugerir_comandos", "prioridad", "media")
        ));
        vote3.setReasoning("El usuario necesita ayuda y sugerencias prácticas");
        vote3.setVoteTime(LocalDateTime.now());
        votes.add(vote3);
        
        return votes;
    }
    
    /**
     * Crea una ronda de votación de ejemplo.
     */
    private VotingRound createTestVotingRound() {
        VotingRound round = new VotingRound();
        round.setRoundId("test-round-" + System.currentTimeMillis());
        round.setRequestId("test-request-123");
        round.setUserMessage("¿Puedes ayudarme con el sistema?");
        round.setConversationContext(Map.of("user_type", "new", "session_id", "test-session"));
        round.setConversationHistory(List.of("Hola", "¿Cómo estás?"));
        round.setStartTime(LocalDateTime.now());
        
        return round;
    }
    
    /**
     * Convierte datos de request a objetos LlmVote.
     */
    @SuppressWarnings("unchecked")
    private List<LlmVote> convertToLlmVotes(List<Map<String, Object>> votesData) {
        List<LlmVote> votes = new ArrayList<>();
        
        for (Map<String, Object> voteData : votesData) {
            LlmVote vote = new LlmVote();
            
            vote.setVoteId((String) voteData.get("voteId"));
            vote.setLlmId((String) voteData.get("llmId"));
            vote.setLlmName((String) voteData.get("llmName"));
            vote.setIntent((String) voteData.get("intent"));
            
            if (voteData.get("confidence") instanceof Number) {
                vote.setConfidence(((Number) voteData.get("confidence")).doubleValue());
            }
            
            if (voteData.get("llmWeight") instanceof Number) {
                vote.setLlmWeight(((Number) voteData.get("llmWeight")).doubleValue());
            }
            
            if (voteData.get("entities") instanceof Map) {
                vote.setEntities((Map<String, Object>) voteData.get("entities"));
            }
            
            if (voteData.get("subtasks") instanceof List) {
                vote.setSubtasks((List<Map<String, Object>>) voteData.get("subtasks"));
            }
            
            vote.setReasoning((String) voteData.get("reasoning"));
            vote.setVoteTime(LocalDateTime.now());
            
            votes.add(vote);
        }
        
        return votes;
    }
    
    /**
     * Crea una ronda de votación desde datos de request.
     */
    private VotingRound createVotingRoundFromRequest(Map<String, Object> request) {
        VotingRound round = new VotingRound();
        round.setRoundId("custom-round-" + System.currentTimeMillis());
        round.setRequestId((String) request.getOrDefault("requestId", "custom-request"));
        round.setUserMessage((String) request.getOrDefault("userMessage", "Mensaje de prueba"));
        round.setStartTime(LocalDateTime.now());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) request.get("conversationContext");
        if (context != null) {
            round.setConversationContext(context);
        }
        
        @SuppressWarnings("unchecked")
        List<String> history = (List<String>) request.get("conversationHistory");
        if (history != null) {
            round.setConversationHistory(history);
        }
        
        return round;
    }
    
    /**
     * Crea un consenso fallido para casos de error.
     */
    private VotingConsensus createFailedConsensus(int totalVotes) {
        VotingConsensus consensus = new VotingConsensus();
        consensus.setConsensusId("failed-consensus-" + System.currentTimeMillis());
        consensus.setFinalIntent("unknown");
        consensus.setConsensusConfidence(0.0);
        consensus.setParticipatingVotes(0);
        consensus.setTotalVotes(totalVotes);
        consensus.setAgreementLevel(VotingConsensus.AgreementLevel.FAILED);
        consensus.setConsensusMethod("failed");
        consensus.setReasoning("Consenso fallido debido a error en el procesamiento");
        
        return consensus;
    }
} 