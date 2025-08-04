package com.intentmanagerms.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.domain.model.*;
import com.intentmanagerms.domain.model.McpAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Servicio principal para el sistema de votación MoE (Mixture of Experts).
 * 
 * Responsabilidades:
 * - Coordinar votación entre múltiples LLMs
 * - Gestionar rondas de debate y consenso
 * - Procesar votos y calcular consenso final
 * - Manejar fallbacks cuando el sistema de votación falla
 * - Proporcionar logging transparente del proceso
 */
@Service
public class LlmVotingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmVotingService.class);
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    @Autowired
    private McpActionRegistry mcpActionRegistry;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Configuración desde application.yml
    @Value("${moe.enabled:true}")
    private boolean moeEnabled;
    
    @Value("${moe.timeout-per-vote:30}")
    private int timeoutPerVote;
    
    @Value("${moe.parallel-voting:true}")
    private boolean parallelVoting;
    
    @Value("${moe.consensus-threshold:0.6}")
    private double consensusThreshold;
    
    @Value("${moe.max-debate-rounds:1}")
    private int maxDebateRounds;
    
    private VotingConfiguration votingConfiguration;
    private final Map<String, VotingRound> activeRounds = new ConcurrentHashMap<>();
    private final ExecutorService votingExecutor = Executors.newCachedThreadPool();
    
    /**
     * Inicializa el servicio de votación con la configuración JSON.
     */
    public void initializeVotingService(VotingConfiguration configuration) {
        this.votingConfiguration = configuration;
        logger.info("Servicio de votación MoE inicializado con configuración: {}", configuration);
    }
    
    /**
     * Ejecuta una ronda de votación para clasificar la intención del usuario.
     */
    public VotingRound executeVotingRound(String requestId, String userMessage, 
                                         Map<String, Object> conversationContext,
                                         List<String> conversationHistory) {
        
        String roundId = generateRoundId(requestId);
        VotingRound round = new VotingRound(roundId, requestId, userMessage);
        round.setConversationContext(conversationContext);
        round.setConversationHistory(conversationHistory);
        
        activeRounds.put(roundId, round);
        
        try {
            logger.info("Iniciando ronda de votación {} para mensaje: '{}'", roundId, userMessage);
            
            // Verificar si el sistema MoE está habilitado
            if (!moeEnabled || votingConfiguration == null || 
                !votingConfiguration.getVotingSystem().isEnabled()) {
                logger.info("Sistema MoE deshabilitado, usando modo LLM único");
                return executeSingleLlmMode(round);
            }
            
            // Ejecutar votación paralela o secuencial
            List<LlmVote> votes = parallelVoting ? 
                executeParallelVoting(round) : executeSequentialVoting(round);
            
            round.setVotes(votes);
            
            // Calcular consenso
            VotingConsensus consensus = calculateConsensus(votes, round);
            round.setConsensus(consensus);
            
            round.setStatus(VotingRound.VotingStatus.COMPLETED);
            round.setEndTime(LocalDateTime.now());
            
            logger.info("Ronda de votación {} completada exitosamente. Consenso: {}", 
                       roundId, consensus);
            
        } catch (Exception e) {
            logger.error("Error en ronda de votación {}: {}", roundId, e.getMessage(), e);
            round.setStatus(VotingRound.VotingStatus.FAILED);
            round.setErrorMessage(e.getMessage());
            round.setEndTime(LocalDateTime.now());
            
            // Aplicar fallback
            return applyFallbackStrategy(round, e);
        } finally {
            activeRounds.remove(roundId);
        }
        
        return round;
    }
    
    /**
     * Ejecuta votación en paralelo para todos los LLMs participantes.
     */
    private List<LlmVote> executeParallelVoting(VotingRound round) throws InterruptedException, ExecutionException {
        List<VotingConfiguration.LlmParticipant> participants = 
            votingConfiguration.getVotingSystem().getLlmParticipants();
        
        List<Future<LlmVote>> futures = new ArrayList<>();
        
        // Iniciar votación paralela
        for (VotingConfiguration.LlmParticipant participant : participants) {
            Future<LlmVote> future = votingExecutor.submit(() -> 
                executeSingleVote(round, participant));
            futures.add(future);
        }
        
        // Recopilar resultados con timeout
        List<LlmVote> votes = new ArrayList<>();
        for (Future<LlmVote> future : futures) {
            try {
                LlmVote vote = future.get(timeoutPerVote, TimeUnit.SECONDS);
                votes.add(vote);
            } catch (TimeoutException e) {
                logger.warn("Timeout en voto de LLM, marcando como fallido");
                LlmVote timeoutVote = createTimeoutVote();
                votes.add(timeoutVote);
            } catch (Exception e) {
                logger.warn("Error en voto de LLM: {}", e.getMessage());
                LlmVote errorVote = createErrorVote(e.getMessage());
                votes.add(errorVote);
            }
        }
        
        return votes;
    }
    
    /**
     * Ejecuta votación secuencial para todos los LLMs participantes.
     */
    private List<LlmVote> executeSequentialVoting(VotingRound round) {
        List<VotingConfiguration.LlmParticipant> participants = 
            votingConfiguration.getVotingSystem().getLlmParticipants();
        
        List<LlmVote> votes = new ArrayList<>();
        
        for (VotingConfiguration.LlmParticipant participant : participants) {
            try {
                LlmVote vote = executeSingleVote(round, participant);
                votes.add(vote);
            } catch (Exception e) {
                logger.warn("Error en voto secuencial de LLM {}: {}", participant.getId(), e.getMessage());
                LlmVote errorVote = createErrorVote(e.getMessage());
                errorVote.setLlmId(participant.getId());
                errorVote.setLlmName(participant.getName());
                votes.add(errorVote);
            }
        }
        
        return votes;
    }
    
    /**
     * Ejecuta un voto individual de un LLM.
     */
    private LlmVote executeSingleVote(VotingRound round, VotingConfiguration.LlmParticipant participant) {
        String voteId = generateVoteId(round.getRoundId(), participant.getId());
        LlmVote vote = new LlmVote(voteId, participant.getId(), participant.getName(), null, 0.0);
        vote.setLlmRole(participant.getRole());
        vote.setLlmWeight(participant.getWeight());
        vote.setStatus(LlmVote.VoteStatus.IN_PROGRESS);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Construir prompt personalizado
            String prompt = buildVotingPrompt(round, participant);
            
            // Obtener configuración del LLM
            Optional<LlmConfiguration> llmConfig = llmConfigurationService.getLlmConfiguration(participant.getId());
            if (llmConfig.isEmpty()) {
                throw new RuntimeException("Configuración de LLM no encontrada: " + participant.getId());
            }
            
            // Ejecutar llamada al LLM (simulada por ahora)
            String llmResponse = executeLlmCall(llmConfig.get(), prompt);
            
            // Parsear respuesta del LLM
            parseLlmVoteResponse(vote, llmResponse);
            
            vote.setStatus(LlmVote.VoteStatus.COMPLETED);
            
        } catch (Exception e) {
            vote.setStatus(LlmVote.VoteStatus.FAILED);
            vote.setErrorMessage(e.getMessage());
            logger.error("Error en voto de LLM {}: {}", participant.getId(), e.getMessage());
        } finally {
            vote.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        }
        
        return vote;
    }
    
    /**
     * Construye el prompt personalizado para cada LLM participante.
     */
    private String buildVotingPrompt(VotingRound round, VotingConfiguration.LlmParticipant participant) {
        String template = participant.getPromptTemplate();
        if (template == null || template.isEmpty()) {
            template = getDefaultVotingPrompt();
        }
        
        // Obtener acciones MCP disponibles
        List<String> availableActions = mcpActionRegistry.getAllActions()
            .values()
            .stream()
            .map(McpAction::getEndpoint)
            .collect(Collectors.toList());
        
        // Reemplazar placeholders en el template
        return template
            .replace("{user_message}", round.getUserMessage())
            .replace("{conversation_context}", formatConversationContext(round.getConversationContext()))
            .replace("{conversation_history}", formatConversationHistory(round.getConversationHistory()))
            .replace("{available_actions}", String.join(", ", availableActions))
            .replace("{llm_role}", participant.getRole());
    }
    
    /**
     * Ejecuta la llamada al LLM (simulada por ahora).
     */
    private String executeLlmCall(LlmConfiguration llmConfig, String prompt) {
        // TODO: Implementar llamada real al LLM usando la configuración
        // Por ahora, simulamos una respuesta
        logger.debug("Ejecutando llamada al LLM {} con prompt: {}", llmConfig.getId(), prompt);
        
        // Simulación de respuesta
        return "{\"intent\": \"ayuda\", \"entities\": {}, \"confidence\": 0.8, \"reasoning\": \"Respuesta simulada\"}";
    }
    
    /**
     * Parsea la respuesta del LLM y la convierte en un voto.
     */
    private void parseLlmVoteResponse(LlmVote vote, String llmResponse) {
        try {
            JsonNode responseJson = objectMapper.readTree(llmResponse);
            
            vote.setIntent(responseJson.path("intent").asText());
            vote.setConfidence(responseJson.path("confidence").asDouble());
            vote.setReasoning(responseJson.path("reasoning").asText());
            
            // Parsear entidades si existen
            if (responseJson.has("entities")) {
                Map<String, Object> entities = objectMapper.convertValue(
                    responseJson.get("entities"), Map.class);
                vote.setEntities(entities);
            }
            
            // Parsear subtareas si existen
            if (responseJson.has("subtasks")) {
                List<Map<String, Object>> subtasks = objectMapper.convertValue(
                    responseJson.get("subtasks"), List.class);
                vote.setSubtasks(subtasks);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error parseando respuesta del LLM: " + e.getMessage());
        }
    }
    
    /**
     * Calcula el consenso basado en los votos recibidos.
     */
    private VotingConsensus calculateConsensus(List<LlmVote> votes, VotingRound round) {
        List<LlmVote> validVotes = votes.stream()
            .filter(LlmVote::isValid)
            .collect(Collectors.toList());
        
        if (validVotes.isEmpty()) {
            return createFailedConsensus(votes.size());
        }
        
        // Agrupar votos por intención
        Map<String, List<LlmVote>> votesByIntent = validVotes.stream()
            .collect(Collectors.groupingBy(LlmVote::getIntent));
        
        // Encontrar la intención más votada
        String mostVotedIntent = votesByIntent.entrySet().stream()
            .max(Comparator.comparing(entry -> entry.getValue().size()))
            .map(Map.Entry::getKey)
            .orElse("ayuda");
        
        // Calcular confianza promedio ponderada
        double weightedConfidence = validVotes.stream()
            .mapToDouble(LlmVote::getWeightedScore)
            .average()
            .orElse(0.0);
        
        // Determinar nivel de acuerdo
        VotingConsensus.AgreementLevel agreementLevel = determineAgreementLevel(votesByIntent, validVotes.size());
        
        // Crear consenso
        VotingConsensus consensus = new VotingConsensus(
            generateConsensusId(round.getRoundId()),
            mostVotedIntent,
            weightedConfidence,
            validVotes.size(),
            votes.size()
        );
        
        consensus.setAgreementLevel(agreementLevel);
        consensus.setConsensusMethod("weighted_majority");
        consensus.setReasoning("Consenso calculado basado en votos ponderados");
        
        // Combinar entidades de todos los votos válidos
        Map<String, Object> combinedEntities = combineEntities(validVotes);
        consensus.setFinalEntities(combinedEntities);
        
        // Combinar subtareas
        List<Map<String, Object>> combinedSubtasks = combineSubtasks(validVotes);
        consensus.setFinalSubtasks(combinedSubtasks);
        
        return consensus;
    }
    
    /**
     * Determina el nivel de acuerdo basado en los votos.
     */
    private VotingConsensus.AgreementLevel determineAgreementLevel(Map<String, List<LlmVote>> votesByIntent, int totalValidVotes) {
        if (votesByIntent.size() == 1) {
            return VotingConsensus.AgreementLevel.UNANIMOUS;
        }
        
        int maxVotes = votesByIntent.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);
        
        double majorityThreshold = totalValidVotes * 0.5;
        
        if (maxVotes > majorityThreshold) {
            return VotingConsensus.AgreementLevel.MAJORITY;
        } else if (maxVotes > 1) {
            return VotingConsensus.AgreementLevel.PLURALITY;
        } else {
            return VotingConsensus.AgreementLevel.SPLIT;
        }
    }
    
    /**
     * Combina entidades de múltiples votos.
     */
    private Map<String, Object> combineEntities(List<LlmVote> votes) {
        Map<String, Object> combined = new HashMap<>();
        
        for (LlmVote vote : votes) {
            if (vote.getEntities() != null) {
                combined.putAll(vote.getEntities());
            }
        }
        
        return combined;
    }
    
    /**
     * Combina subtareas de múltiples votos.
     */
    private List<Map<String, Object>> combineSubtasks(List<LlmVote> votes) {
        List<Map<String, Object>> combined = new ArrayList<>();
        
        for (LlmVote vote : votes) {
            if (vote.getSubtasks() != null) {
                combined.addAll(vote.getSubtasks());
            }
        }
        
        return combined;
    }
    
    /**
     * Ejecuta modo LLM único cuando el sistema de votación está deshabilitado.
     */
    private VotingRound executeSingleLlmMode(VotingRound round) {
        logger.info("Ejecutando modo LLM único para ronda {}", round.getRoundId());
        
        try {
            // Obtener LLM primario
            Optional<LlmConfiguration> primaryLlm = llmConfigurationService.getPrimaryLlm();
            if (primaryLlm.isEmpty()) {
                throw new RuntimeException("No se encontró LLM primario configurado");
            }
            
            // Crear voto único
            LlmVote singleVote = new LlmVote(
                generateVoteId(round.getRoundId(), "single"),
                primaryLlm.get().getId(),
                primaryLlm.get().getName(),
                "ayuda",
                0.8
            );
            singleVote.setLlmRole("LLM Único");
            singleVote.setLlmWeight(1.0);
            singleVote.setStatus(LlmVote.VoteStatus.COMPLETED);
            
            // Crear consenso simple
            VotingConsensus consensus = new VotingConsensus(
                generateConsensusId(round.getRoundId()),
                "ayuda",
                0.8,
                1,
                1
            );
            consensus.setAgreementLevel(VotingConsensus.AgreementLevel.UNANIMOUS);
            consensus.setConsensusMethod("single_llm_mode");
            consensus.setReasoning("Modo LLM único - sin votación");
            
            round.setVotes(List.of(singleVote));
            round.setConsensus(consensus);
            round.setStatus(VotingRound.VotingStatus.COMPLETED);
            round.setEndTime(LocalDateTime.now());
            
        } catch (Exception e) {
            round.setStatus(VotingRound.VotingStatus.FAILED);
            round.setErrorMessage(e.getMessage());
            round.setEndTime(LocalDateTime.now());
        }
        
        return round;
    }
    
    /**
     * Aplica estrategia de fallback cuando la votación falla.
     */
    private VotingRound applyFallbackStrategy(VotingRound round, Exception error) {
        logger.warn("Aplicando estrategia de fallback para ronda {}: {}", round.getRoundId(), error.getMessage());
        
        try {
            return executeSingleLlmMode(round);
        } catch (Exception fallbackError) {
            logger.error("Error en fallback para ronda {}: {}", round.getRoundId(), fallbackError.getMessage());
            round.setErrorMessage("Error en fallback: " + fallbackError.getMessage());
            return round;
        }
    }
    
    /**
     * Crea un voto de timeout.
     */
    private LlmVote createTimeoutVote() {
        LlmVote vote = new LlmVote();
        vote.setStatus(LlmVote.VoteStatus.TIMEOUT);
        vote.setErrorMessage("Timeout en votación");
        return vote;
    }
    
    /**
     * Crea un voto de error.
     */
    private LlmVote createErrorVote(String errorMessage) {
        LlmVote vote = new LlmVote();
        vote.setStatus(LlmVote.VoteStatus.FAILED);
        vote.setErrorMessage(errorMessage);
        return vote;
    }
    
    /**
     * Crea un consenso fallido.
     */
    private VotingConsensus createFailedConsensus(int totalVotes) {
        VotingConsensus consensus = new VotingConsensus();
        consensus.setFinalIntent("ayuda");
        consensus.setConsensusConfidence(0.0);
        consensus.setParticipatingVotes(0);
        consensus.setTotalVotes(totalVotes);
        consensus.setAgreementLevel(VotingConsensus.AgreementLevel.FAILED);
        consensus.setConsensusMethod("fallback");
        consensus.setReasoning("No se pudieron procesar votos válidos");
        return consensus;
    }
    
    /**
     * Obtiene estadísticas del servicio de votación.
     */
    public Map<String, Object> getVotingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("moe_enabled", moeEnabled);
        stats.put("parallel_voting", parallelVoting);
        stats.put("timeout_per_vote", timeoutPerVote);
        stats.put("consensus_threshold", consensusThreshold);
        stats.put("max_debate_rounds", maxDebateRounds);
        stats.put("active_rounds", activeRounds.size());
        stats.put("voting_executor_pool_size", ((ThreadPoolExecutor) votingExecutor).getPoolSize());
        stats.put("voting_executor_active_threads", ((ThreadPoolExecutor) votingExecutor).getActiveCount());
        
        if (votingConfiguration != null) {
            stats.put("voting_configuration", votingConfiguration.toString());
        }
        
        return stats;
    }
    
    /**
     * Verifica la salud del servicio de votación.
     */
    public boolean isHealthy() {
        try {
            // Verificar que el executor esté funcionando
            if (votingExecutor.isShutdown() || votingExecutor.isTerminated()) {
                return false;
            }
            
            // Verificar configuración
            if (votingConfiguration == null) {
                return false;
            }
            
            // Verificar LLMs configurados
            List<LlmConfiguration> votingLlms = llmConfigurationService.getLlmForVoting(3);
            if (votingLlms.isEmpty()) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error verificando salud del servicio de votación: {}", e.getMessage());
            return false;
        }
    }
    
    // Métodos de utilidad
    private String generateRoundId(String requestId) {
        return "round_" + requestId + "_" + System.currentTimeMillis();
    }
    
    private String generateVoteId(String roundId, String llmId) {
        return "vote_" + roundId + "_" + llmId;
    }
    
    private String generateConsensusId(String roundId) {
        return "consensus_" + roundId;
    }
    
    private String getDefaultVotingPrompt() {
        return "Eres un experto en clasificación de intenciones. Analiza la petición del usuario y determina la intención más precisa.\n\n" +
               "Petición: {user_message}\n" +
               "Contexto: {conversation_context}\n\n" +
               "Responde en formato JSON: {\"intent\": \"nombre_intencion\", \"entities\": {\"entidad\": \"valor\"}, \"confidence\": 0.0-1.0, \"reasoning\": \"breve explicación\"}";
    }
    
    private String formatConversationContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "Sin contexto";
        }
        return context.toString();
    }
    
    private String formatConversationHistory(List<String> history) {
        if (history == null || history.isEmpty()) {
            return "Sin historial";
        }
        return String.join(" | ", history);
    }
} 