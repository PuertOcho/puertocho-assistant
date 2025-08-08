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
import java.text.Normalizer;
// import com.fasterxml.jackson.core.type.TypeReference; // Eliminado: no usado
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio principal para el sistema de votación MoE (Mixture of Experts).
 * 
 * Responsabilidades:
 * - Coordinar votación entre múltiples LLMs
 * - Gestionar rondas de debate y consenso
 * - Procesar votos y calcular consenso final
 * - Manejar fallbacks cuando el sistema de votación falla
 * - Proporcionar logging transparente del proceso
 * 
 * T3.2: Sistema de debate mejorado donde 3 LLMs debaten brevemente la acción a tomar
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
    
    @Autowired
    private ConsensusEngine consensusEngine;
    
    // Configuración desde application.yml
    @Value("${moe.enabled:true}")
    private boolean moeEnabled;
    
    @Value("${moe.timeout-per-vote:30}")
    private int timeoutPerVote;
    
    @Value("${moe.parallel-voting:true}")
    private boolean parallelVoting;
    
    @Value("${moe.consensus-threshold:0.6}")
    private double consensusThreshold;
    
    @Value("${moe.max-debate-rounds:2}")
    private int maxDebateRounds;
    
    @Value("${moe.debate-timeout:60}")
    private int debateTimeout;
    
    @Value("${moe.enable-debate:true}")
    private boolean enableDebate;
    
    @Value("${moe.debate-consensus-improvement-threshold:0.1}")
    private double debateConsensusImprovementThreshold;
    
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
     * Ejecuta una ronda de votación con debate para clasificar la intención del usuario.
     * T3.2: Sistema de debate mejorado donde los LLMs pueden debatir entre sí.
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
            
            // T3.2: Ejecutar sistema de debate mejorado
            if (enableDebate && maxDebateRounds > 1) {
                return executeDebateVotingRound(round);
            } else {
                // Modo de votación simple (sin debate)
                return executeSimpleVotingRound(round);
            }
            
        } catch (Exception e) {
            logger.error("Error en la ronda de votación {}: {}", roundId, e.getMessage(), e);
            return applyFallbackStrategy(round, e);
        } finally {
            round.setEndTime(LocalDateTime.now());
            round.setStatus(VotingRound.VotingStatus.COMPLETED);
            activeRounds.remove(roundId);
        }
    }
    
    /**
     * T3.2: Ejecuta una ronda de votación con debate entre múltiples LLMs.
     * Los LLMs pueden debatir entre sí en múltiples rondas antes del consenso final.
     */
    private VotingRound executeDebateVotingRound(VotingRound round) {
        logger.info("Iniciando debate entre {} LLMs para ronda {}", 
                   votingConfiguration.getVotingSystem().getLlmParticipants().size(), 
                   round.getRoundId());
        
        List<LlmVote> allVotes = new ArrayList<>();
        VotingConsensus currentConsensus = null;
        double previousConsensusConfidence = 0.0;
        
        // Ejecutar múltiples rondas de debate
        for (int debateRound = 1; debateRound <= maxDebateRounds; debateRound++) {
            logger.info("Ronda de debate {}/{} para {}", debateRound, maxDebateRounds, round.getRoundId());
            
            // Ejecutar votación en esta ronda
            List<LlmVote> roundVotes;
            try {
                roundVotes = parallelVoting ? 
                    executeParallelVoting(round, debateRound, allVotes) : 
                    executeSequentialVoting(round, debateRound, allVotes);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error en votación de ronda {}: {}", debateRound, e.getMessage());
                roundVotes = new ArrayList<>();
            }
            
            allVotes.addAll(roundVotes);
            
            // Calcular consenso actual
            VotingConsensus roundConsensus = calculateConsensus(roundVotes, round);
            currentConsensus = roundConsensus;
            
            logger.info("Consenso ronda {}: {} (confianza: {})", 
                       debateRound, roundConsensus.getFinalIntent(), 
                       roundConsensus.getConsensusConfidence());
            
            // Verificar si el debate ha mejorado significativamente el consenso
            if (debateRound > 1) {
                double confidenceImprovement = roundConsensus.getConsensusConfidence() - previousConsensusConfidence;
                logger.info("Mejora en confianza: {} (umbral: {})", 
                           confidenceImprovement, debateConsensusImprovementThreshold);
                
                // Si la mejora es mínima, terminar el debate
                if (confidenceImprovement < debateConsensusImprovementThreshold) {
                    logger.info("Mejora mínima en consenso, terminando debate en ronda {}", debateRound);
                    break;
                }
            }
            
            previousConsensusConfidence = roundConsensus.getConsensusConfidence();
            
            // Si ya tenemos unanimidad, no necesitamos más rondas
            if (roundConsensus.getAgreementLevel() == VotingConsensus.AgreementLevel.UNANIMOUS) {
                logger.info("Unanimidad alcanzada en ronda {}, terminando debate", debateRound);
                break;
            }
            
            // Si no es la última ronda, preparar para la siguiente
            if (debateRound < maxDebateRounds) {
                // Pequeña pausa entre rondas de debate
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // Establecer el resultado final
        round.setVotes(allVotes);
        round.setConsensus(currentConsensus);
        
        // T3.5: Verificar si el consenso falló y activar fallback automáticamente
        if (currentConsensus == null ||
            currentConsensus.getAgreementLevel() == VotingConsensus.AgreementLevel.FAILED || 
            currentConsensus.getConsensusConfidence() < consensusThreshold ||
            "unknown".equals(currentConsensus.getFinalIntent())) {
            
            logger.warn("Consenso del debate falló para ronda {}, activando fallback a LLM único", round.getRoundId());
            logger.warn("Consenso: {} (confianza: {}), umbral: {}", 
                       currentConsensus.getFinalIntent(), currentConsensus.getConsensusConfidence(), consensusThreshold);
            
            return executeSingleLlmMode(round);
        }
        
        logger.info("Debate completado para ronda {}: {} votos, consenso final: {} (confianza: {})", 
                   round.getRoundId(), allVotes.size(), 
                   currentConsensus.getFinalIntent(), 
                   currentConsensus.getConsensusConfidence());
        
        return round;
    }
    
    /**
     * Ejecuta una ronda de votación simple (sin debate).
     */
    private VotingRound executeSimpleVotingRound(VotingRound round) {
        logger.info("Ejecutando votación simple para ronda {}", round.getRoundId());
        
        // Ejecutar votación paralela o secuencial
        List<LlmVote> votes;
        try {
            votes = parallelVoting ? 
                executeParallelVoting(round, 1, new ArrayList<>()) : 
                executeSequentialVoting(round, 1, new ArrayList<>());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error en votación simple: {}", e.getMessage());
            votes = new ArrayList<>();
        }
        
        round.setVotes(votes);
        
        // Calcular consenso
        VotingConsensus consensus = calculateConsensus(votes, round);
        round.setConsensus(consensus);
        
        // T3.5: Verificar si el consenso falló y activar fallback automáticamente
        if (consensus.getAgreementLevel() == VotingConsensus.AgreementLevel.FAILED || 
            consensus.getConsensusConfidence() < consensusThreshold ||
            "unknown".equals(consensus.getFinalIntent())) {
            
            logger.warn("Consenso falló para ronda {}, activando fallback a LLM único", round.getRoundId());
            logger.warn("Consenso: {} (confianza: {}), umbral: {}", 
                       consensus.getFinalIntent(), consensus.getConsensusConfidence(), consensusThreshold);
            
            return executeSingleLlmMode(round);
        }
        
        logger.info("Votación simple completada para ronda {}: {} votos, consenso: {} (confianza: {})", 
                   round.getRoundId(), votes.size(), 
                   consensus.getFinalIntent(), 
                   consensus.getConsensusConfidence());
        
        return round;
    }
    
    /**
     * Ejecuta votación paralela con soporte para debate.
     */
    private List<LlmVote> executeParallelVoting(VotingRound round, int debateRound, List<LlmVote> previousVotes) 
            throws InterruptedException, ExecutionException {
        
        List<VotingConfiguration.LlmParticipant> participants = 
            votingConfiguration.getVotingSystem().getLlmParticipants();
        
        List<Future<LlmVote>> voteFutures = new ArrayList<>();
        
        // Crear tareas de votación para cada participante
        for (VotingConfiguration.LlmParticipant participant : participants) {
            Future<LlmVote> future = votingExecutor.submit(() -> 
                executeSingleVote(round, participant, debateRound, previousVotes));
            voteFutures.add(future);
        }
        
        // Recopilar resultados con timeout
        List<LlmVote> votes = new ArrayList<>();
        for (Future<LlmVote> future : voteFutures) {
            try {
                LlmVote vote = future.get(timeoutPerVote, TimeUnit.SECONDS);
                votes.add(vote);
            } catch (TimeoutException e) {
                logger.warn("Timeout en voto de LLM, creando voto de timeout");
                votes.add(createTimeoutVote());
            } catch (Exception e) {
                logger.error("Error en voto paralelo: {}", e.getMessage());
                votes.add(createErrorVote("Error en votación: " + e.getMessage()));
            }
        }
        
        return votes;
    }
    
    /**
     * Ejecuta votación secuencial con soporte para debate.
     */
    private List<LlmVote> executeSequentialVoting(VotingRound round, int debateRound, List<LlmVote> previousVotes) {
        List<VotingConfiguration.LlmParticipant> participants = 
            votingConfiguration.getVotingSystem().getLlmParticipants();
        
        List<LlmVote> votes = new ArrayList<>();
        
        for (VotingConfiguration.LlmParticipant participant : participants) {
            try {
                LlmVote vote = executeSingleVote(round, participant, debateRound, previousVotes);
                votes.add(vote);
            } catch (Exception e) {
                logger.error("Error en voto secuencial de {}: {}", participant.getId(), e.getMessage());
                votes.add(createErrorVote("Error en votación: " + e.getMessage()));
            }
        }
        
        return votes;
    }
    
    /**
     * Ejecuta un voto individual con soporte para debate.
     */
    private LlmVote executeSingleVote(VotingRound round, VotingConfiguration.LlmParticipant participant, 
                                     int debateRound, List<LlmVote> previousVotes) {
        
        long startTime = System.currentTimeMillis();
        String voteId = generateVoteId(round.getRoundId(), participant.getId());
        
        LlmVote vote = new LlmVote(voteId, participant.getId(), participant.getName(), null, 0.0);
        vote.setLlmRole(participant.getRole());
        vote.setLlmWeight(participant.getWeight());
        vote.setStatus(LlmVote.VoteStatus.IN_PROGRESS);
        
        try {
            logger.debug("Ejecutando voto {} para LLM {} (ronda {})", voteId, participant.getId(), debateRound);
            
            // Construir prompt según la ronda de debate
            String prompt = buildDebatePrompt(round, participant, debateRound, previousVotes);
            
            // Obtener configuración del LLM
            Optional<LlmConfiguration> llmConfigOpt = llmConfigurationService.getLlmConfiguration(participant.getId());
            if (llmConfigOpt.isEmpty()) {
                throw new RuntimeException("Configuración LLM no encontrada para: " + participant.getId());
            }
            LlmConfiguration llmConfig = llmConfigOpt.get();
            
            // Ejecutar llamada al LLM
            String llmResponse = executeLlmCall(llmConfig, prompt);
            
            // Parsear respuesta
            parseLlmVoteResponse(vote, llmResponse);
            
            // Calcular tiempo de procesamiento
            long processingTime = System.currentTimeMillis() - startTime;
            vote.setProcessingTimeMs(processingTime);
            vote.setStatus(LlmVote.VoteStatus.COMPLETED);
            
            logger.debug("Voto {} completado en {}ms: {} (confianza: {})", 
                        voteId, processingTime, vote.getIntent(), vote.getConfidence());
            
        } catch (Exception e) {
            logger.error("Error en voto {}: {}", voteId, e.getMessage());
            vote.setStatus(LlmVote.VoteStatus.FAILED);
            vote.setErrorMessage(e.getMessage());
        }
        
        return vote;
    }
    
    /**
     * T3.2: Construye prompt de debate que considera votos previos.
     */
    private String buildDebatePrompt(VotingRound round, VotingConfiguration.LlmParticipant participant, 
                                    int debateRound, List<LlmVote> previousVotes) {
        
        StringBuilder prompt = new StringBuilder();
        
        // Prompt base según el rol del LLM
        prompt.append(participant.getPromptTemplate()).append("\n\n");
        
        // Información del contexto
        prompt.append("Petición del usuario: ").append(round.getUserMessage()).append("\n");
        
        if (round.getConversationContext() != null && !round.getConversationContext().isEmpty()) {
            prompt.append("Contexto de conversación: ").append(formatConversationContext(round.getConversationContext())).append("\n");
        }
        
        if (round.getConversationHistory() != null && !round.getConversationHistory().isEmpty()) {
            prompt.append("Historial de conversación: ").append(formatConversationHistory(round.getConversationHistory())).append("\n");
        }
        
        // T3.2: Si es una ronda de debate posterior, incluir votos previos
        if (debateRound > 1 && !previousVotes.isEmpty()) {
            prompt.append("\n=== DEBATE EN CURSO ===\n");
            prompt.append("Esta es la ronda de debate ").append(debateRound).append(". ");
            prompt.append("Considera los votos de otros expertos y explica si mantienes tu posición o la cambias:\n\n");
            
            for (LlmVote prevVote : previousVotes) {
                if (prevVote.isValid()) {
                    prompt.append("Voto de ").append(prevVote.getLlmName()).append(" (").append(prevVote.getLlmRole()).append("):\n");
                    prompt.append("- Intención: ").append(prevVote.getIntent()).append("\n");
                    prompt.append("- Confianza: ").append(prevVote.getConfidence()).append("\n");
                    if (prevVote.getReasoning() != null) {
                        prompt.append("- Razonamiento: ").append(prevVote.getReasoning()).append("\n");
                    }
                    if (prevVote.getEntities() != null && !prevVote.getEntities().isEmpty()) {
                        prompt.append("- Entidades: ").append(prevVote.getEntities()).append("\n");
                    }
                    prompt.append("\n");
                }
            }
            
            prompt.append("Como ").append(participant.getName()).append(", ");
            prompt.append("analiza estos votos y proporciona tu voto final considerando los argumentos de otros expertos.\n");
            prompt.append("Si cambias tu posición, explica por qué. Si la mantienes, refuerza tu argumento.\n\n");
        }
        
        // Acciones MCP disponibles
        Map<String, McpAction> allActions = mcpActionRegistry.getAllActions();
        if (!allActions.isEmpty()) {
            List<String> actionNames = new ArrayList<>(allActions.keySet());
            prompt.append("Acciones MCP disponibles: ").append(String.join(", ", actionNames)).append("\n\n");
        }
        
        // Instrucciones específicas para debate
        if (debateRound > 1) {
            prompt.append("INSTRUCCIONES PARA DEBATE:\n");
            prompt.append("1. Considera cuidadosamente los votos de otros expertos\n");
            prompt.append("2. Si hay desacuerdo, explica tu razonamiento\n");
            prompt.append("3. Si cambias tu posición, justifica el cambio\n");
            prompt.append("4. Mantén un enfoque constructivo y colaborativo\n");
            prompt.append("5. Proporciona un nivel de confianza realista\n\n");
        }
        
        prompt.append("Responde en formato JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"intent\": \"nombre_intencion\",\n");
        prompt.append("  \"entities\": {\"entidad\": \"valor\"},\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"reasoning\": \"explicación detallada\",\n");
        prompt.append("  \"subtasks\": [{\"action\": \"accion\", \"entities\": {}}],\n");
        prompt.append("  \"debate_response\": \"respuesta a otros expertos si aplica\"\n");
        prompt.append("}");
        
        return prompt.toString();
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
        // Simulación mejorada: clasificar intención y extraer entidades a partir del mensaje del usuario
        logger.debug("Ejecutando llamada al LLM {} (simulada) con prompt", llmConfig.getId());

        String userMessage = extractUserMessageFromPrompt(prompt);
        Map<String, Object> entities = new HashMap<>();
        String detectedIntent = classifyIntentHeuristically(userMessage, entities);
        double confidence = estimateConfidence(detectedIntent, userMessage);
        String reasoning = "Clasificación heurística basada en palabras clave y patrones";

        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("intent", detectedIntent);
            response.put("entities", entities);
            response.put("confidence", confidence);
            response.put("reasoning", reasoning);
            response.put("subtasks", new ArrayList<>());
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            // Fallback mínimo si algo falla
            return "{\"intent\": \"ayuda\", \"entities\": {}, \"confidence\": 0.5, \"reasoning\": \"Fallback por error de simulación\"}";
        }
    }

    private String extractUserMessageFromPrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        // Buscar etiquetas comunes en prompts
        String[] markers = new String[] {"Petición del usuario:", "Mensaje del usuario:", "Petición:", "Mensaje:"};
        for (String marker : markers) {
            int idx = prompt.indexOf(marker);
            if (idx >= 0) {
                int start = idx + marker.length();
                int end = prompt.indexOf('\n', start);
                String line = end >= 0 ? prompt.substring(start, end) : prompt.substring(start);
                return line.trim();
            }
        }
        return "";
    }

    private String classifyIntentHeuristically(String message, Map<String, Object> entitiesOut) {
        if (message == null) message = "";
        String normalized = normalize(message);

        // Diccionarios simples
        Set<String> roomWords = new HashSet<>(Arrays.asList(
            "salon", "salón", "dormitorio", "habitacion", "habitación", "comedor", "cocina", "baño", "bano", "sala", "cuarto", "estudio"
        ));
        Set<String> genres = new HashSet<>(Arrays.asList("jazz", "rock", "clasica", "clásica", "relajante", "pop", "electrónica", "electronica"));
        Set<String> colors = new HashSet<>(Arrays.asList("roja", "rojo", "verde", "azul", "blanca", "blanco", "amarilla", "amarillo", "morado", "violeta"));

        // Reglas por intención
        // consultar_tiempo
        if (containsAny(normalized, "tiempo", "clima", "llueve", "temperatura", "pronostico", "pronóstico")) {
            String location = extractLocation(message);
            if (location != null && !location.isEmpty()) {
                entitiesOut.put("ubicacion", location);
            }
            return "consultar_tiempo";
        }

        // encender_luz
        if (containsAny(normalized, "enciende", "prende", "activa", "ilumina") && containsAny(normalized, "luz", "luces")) {
            String room = extractRoom(normalized, roomWords);
            if (room != null) {
                entitiesOut.put("lugar", room);
            }
            String color = extractColor(normalized, colors);
            if (color != null) {
                entitiesOut.put("color", color);
            }
            return "encender_luz";
        }

        // apagar_luz
        if (containsAny(normalized, "apaga", "apagar", "desactiva", "oscurece") && containsAny(normalized, "luz", "luces")) {
            String room = extractRoom(normalized, roomWords);
            if (room != null) {
                entitiesOut.put("lugar", room);
            }
            return "apagar_luz";
        }

        // reproducir_musica
        if (containsAny(normalized, "pon musica", "pon música", "reproduce", "playlist", "quiero escuchar musica", "quiero escuchar música", "musica relajante", "música relajante")) {
            String genre = extractGenre(normalized, genres);
            if (genre != null) {
                entitiesOut.put("genero", genre);
            }
            return "reproducir_musica";
        }

        // parar_musica
        if (containsAny(normalized, "para la musica", "deten la musica", "pausa la musica", "silencio", "stop musica", "detén la música", "para la música")) {
            return "parar_musica";
        }

        // programar_alarma
        if (containsAny(normalized, "alarma", "despiertame", "despiértame", "avísame", "avisame")) {
            // Heurística mínima de hora (HH:MM)
            String time = extractTime(message);
            if (time != null) {
                entitiesOut.put("fecha_hora", time);
            }
            return "programar_alarma";
        }

        // crear_github_issue
        if (containsAny(normalized, "github", "issue", "bug", "ticket")) {
            return "crear_github_issue";
        }

        // actualizar_taiga_story
        if (containsAny(normalized, "taiga", "story", "estado", "tarea")) {
            return "actualizar_taiga_story";
        }

        // saludo
        if (containsAny(normalized, "hola", "buenos dias", "buenas tardes", "buenas noches", "hey", "saludos")) {
            return "saludo";
        }

        // despedida
        if (containsAny(normalized, "adios", "adiós", "hasta luego", "nos vemos", "chao")) {
            return "despedida";
        }

        // agradecimiento
        if (containsAny(normalized, "gracias", "muy amable", "te agradezco")) {
            return "agradecimiento";
        }

        // ayuda
        if (containsAny(normalized, "ayuda", "que puedes hacer", "qué puedes hacer", "help")) {
            return "ayuda";
        }

        return "ayuda";
    }

    private String normalize(String text) {
        String n = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(normalize(term))) {
                return true;
            }
        }
        return false;
    }

    private String extractLocation(String originalMessage) {
        // Buscar patrón "en <lugar>" o "de <lugar>"
        Pattern p = Pattern.compile("\\b(?:en|de)\\s+([A-Za-zÁÉÍÓÚáéíóúñÑ]+(?:\\s+[A-Za-zÁÉÍÓÚáéíóúñÑ]+){0,3})", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(originalMessage);
        if (m.find()) {
            String loc = m.group(1).trim();
            // Limpiar signos al final
            loc = loc.replaceAll("[\\?\\.,!]+$", "");
            return loc;
        }
        return null;
    }

    private String extractRoom(String normalizedMessage, Set<String> roomWords) {
        for (String room : roomWords) {
            if (normalizedMessage.contains(room)) {
                return room;
            }
        }
        return null;
    }

    private String extractGenre(String normalizedMessage, Set<String> genres) {
        for (String g : genres) {
            if (normalizedMessage.contains(g)) {
                return g;
            }
        }
        return null;
    }

    private String extractColor(String normalizedMessage, Set<String> colors) {
        for (String c : colors) {
            if (normalizedMessage.contains(c)) {
                return c;
            }
        }
        return null;
    }

    private String extractTime(String originalMessage) {
        Pattern p = Pattern.compile("(\\b[01]?\\d|2[0-3]):[0-5]\\d");
        Matcher m = p.matcher(originalMessage);
        if (m.find()) {
            return m.group(0);
        }
        return null;
    }

    private double estimateConfidence(String intent, String message) {
        if ("ayuda".equals(intent)) return 0.6;
        if ("consultar_tiempo".equals(intent)) return 0.9;
        if ("encender_luz".equals(intent) || "apagar_luz".equals(intent)) return 0.9;
        if ("reproducir_musica".equals(intent) || "parar_musica".equals(intent)) return 0.85;
        if ("programar_alarma".equals(intent)) return 0.8;
        if ("crear_github_issue".equals(intent) || "actualizar_taiga_story".equals(intent)) return 0.75;
        if ("saludo".equals(intent) || "despedida".equals(intent) || "agradecimiento".equals(intent)) return 0.95;
        return 0.7;
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
     * Calcula el consenso usando el ConsensusEngine avanzado.
     * T3.3: Integración con ConsensusEngine para procesar votos y llegar a decisión final
     */
    private VotingConsensus calculateConsensus(List<LlmVote> votes, VotingRound round) {
        logger.info("Usando ConsensusEngine para procesar {} votos en ronda {}", votes.size(), round.getRoundId());
        
        try {
            // Delegar el procesamiento al ConsensusEngine
            VotingConsensus consensus = consensusEngine.processConsensus(votes, round);
            
            logger.info("Consenso procesado por ConsensusEngine: {} (confianza: {})", 
                       consensus.getFinalIntent(), consensus.getConsensusConfidence());
            
            return consensus;
            
        } catch (Exception e) {
            logger.error("Error en ConsensusEngine, usando fallback: {}", e.getMessage(), e);
            
            // Fallback a lógica simple si el ConsensusEngine falla
            return createSimpleConsensus(votes, round);
        }
    }
    
    /**
     * Fallback simple para cuando el ConsensusEngine falla.
     */
    private VotingConsensus createSimpleConsensus(List<LlmVote> votes, VotingRound round) {
        List<LlmVote> validVotes = votes.stream()
            .filter(LlmVote::isValid)
            .collect(Collectors.toList());
        
        if (validVotes.isEmpty()) {
            return createFailedConsensus(votes.size());
        }
        
        // Lógica simple de consenso como fallback
        Map<String, List<LlmVote>> votesByIntent = validVotes.stream()
            .collect(Collectors.groupingBy(LlmVote::getIntent));
        
        String mostVotedIntent = votesByIntent.entrySet().stream()
            .max(Comparator.comparing(entry -> entry.getValue().size()))
            .map(Map.Entry::getKey)
            .orElse("ayuda");
        
        double weightedConfidence = validVotes.stream()
            .mapToDouble(LlmVote::getWeightedScore)
            .average()
            .orElse(0.0);
        
        VotingConsensus consensus = new VotingConsensus(
            generateConsensusId(round.getRoundId()),
            mostVotedIntent,
            weightedConfidence,
            validVotes.size(),
            votes.size()
        );
        
        consensus.setAgreementLevel(determineAgreementLevel(votesByIntent, validVotes.size()));
        consensus.setConsensusMethod("fallback_simple");
        consensus.setReasoning("Consenso calculado usando fallback simple debido a error en ConsensusEngine");
        
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
            
            // Construir prompt para clasificación de intención
            String prompt = buildSingleLlmPrompt(round);
            
            // Ejecutar llamada al LLM
            String llmResponse = executeLlmCall(primaryLlm.get(), prompt);
            
            // Parsear la respuesta del LLM
            String intent = "ayuda"; // fallback por defecto
            double confidence = 0.8;
            
            try {
                // Intentar parsear la respuesta del LLM
                parseSingleLlmResponse(llmResponse, round);
                intent = round.getVotes().get(0).getIntent();
                confidence = round.getVotes().get(0).getConfidence();
            } catch (Exception parseError) {
                logger.warn("Error parseando respuesta del LLM único, usando fallback: {}", parseError.getMessage());
            }
            
            // Crear voto único
            LlmVote singleVote = new LlmVote(
                generateVoteId(round.getRoundId(), "single"),
                primaryLlm.get().getId(),
                primaryLlm.get().getName(),
                intent,
                confidence
            );
            singleVote.setLlmRole("LLM Único");
            singleVote.setLlmWeight(1.0);
            singleVote.setStatus(LlmVote.VoteStatus.COMPLETED);
            singleVote.setReasoning("Respuesta del LLM: " + llmResponse);
            
            // Crear consenso simple
            VotingConsensus consensus = new VotingConsensus(
                generateConsensusId(round.getRoundId()),
                intent,
                confidence,
                1,
                1
            );
            consensus.setAgreementLevel(VotingConsensus.AgreementLevel.UNANIMOUS);
            consensus.setConsensusMethod("single_llm_mode");
            consensus.setReasoning("Modo LLM único - clasificación directa");
            
            round.setVotes(List.of(singleVote));
            round.setConsensus(consensus);
            round.setStatus(VotingRound.VotingStatus.COMPLETED);
            round.setEndTime(LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error en modo LLM único para ronda {}: {}", round.getRoundId(), e.getMessage());
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
    
    /**
     * Construye el prompt para el modo LLM único.
     */
    private String buildSingleLlmPrompt(VotingRound round) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Eres un asistente especializado en clasificar la intención del usuario. ");
        prompt.append("Analiza el mensaje del usuario y responde con la intención más apropiada.\n\n");
        
        prompt.append("Mensaje del usuario: ").append(round.getUserMessage()).append("\n\n");
        
        if (round.getConversationContext() != null && !round.getConversationContext().isEmpty()) {
            prompt.append("Contexto de la conversación:\n");
            prompt.append(formatConversationContext(round.getConversationContext()));
            prompt.append("\n\n");
        }
        
        if (round.getConversationHistory() != null && !round.getConversationHistory().isEmpty()) {
            prompt.append("Historial de conversación:\n");
            prompt.append(formatConversationHistory(round.getConversationHistory()));
            prompt.append("\n\n");
        }
        
        prompt.append("Intenciones disponibles:\n");
        prompt.append("- ayuda: Solicitud de ayuda general\n");
        prompt.append("- tiempo: Consulta sobre el clima\n");
        prompt.append("- musica: Solicitud de música\n");
        prompt.append("- luz: Control de iluminación\n");
        prompt.append("- alarma: Programación de alarmas\n");
        prompt.append("- noticia: Solicitud de noticias\n");
        prompt.append("- chiste: Solicitud de chistes\n");
        prompt.append("- calculadora: Operaciones matemáticas\n");
        prompt.append("- traductor: Traducción de idiomas\n");
        prompt.append("- recordatorio: Gestión de recordatorios\n\n");
        
        prompt.append("Responde únicamente con el nombre de la intención (ej: 'ayuda', 'tiempo', 'musica'). ");
        prompt.append("Si no estás seguro, responde 'ayuda'.");
        
        return prompt.toString();
    }
    
    /**
     * Parsea la respuesta del LLM único.
     */
    private void parseSingleLlmResponse(String llmResponse, VotingRound round) {
        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            throw new RuntimeException("Respuesta del LLM vacía");
        }
        
        // Limpiar la respuesta
        String cleanResponse = llmResponse.trim().toLowerCase();
        
        // Mapear respuestas a intenciones
        String intent = "ayuda"; // fallback por defecto
        double confidence = 0.8;
        
        if (cleanResponse.contains("tiempo") || cleanResponse.contains("clima") || cleanResponse.contains("weather")) {
            intent = "tiempo";
            confidence = 0.9;
        } else if (cleanResponse.contains("musica") || cleanResponse.contains("música") || cleanResponse.contains("music")) {
            intent = "musica";
            confidence = 0.9;
        } else if (cleanResponse.contains("luz") || cleanResponse.contains("light") || cleanResponse.contains("iluminacion")) {
            intent = "luz";
            confidence = 0.9;
        } else if (cleanResponse.contains("alarma") || cleanResponse.contains("alarm")) {
            intent = "alarma";
            confidence = 0.9;
        } else if (cleanResponse.contains("noticia") || cleanResponse.contains("news")) {
            intent = "noticia";
            confidence = 0.9;
        } else if (cleanResponse.contains("chiste") || cleanResponse.contains("joke")) {
            intent = "chiste";
            confidence = 0.9;
        } else if (cleanResponse.contains("calculadora") || cleanResponse.contains("calculator") || cleanResponse.contains("calcular")) {
            intent = "calculadora";
            confidence = 0.9;
        } else if (cleanResponse.contains("traductor") || cleanResponse.contains("translate") || cleanResponse.contains("traducir")) {
            intent = "traductor";
            confidence = 0.9;
        } else if (cleanResponse.contains("recordatorio") || cleanResponse.contains("reminder")) {
            intent = "recordatorio";
            confidence = 0.9;
        } else if (cleanResponse.contains("ayuda") || cleanResponse.contains("help")) {
            intent = "ayuda";
            confidence = 0.8;
        }
        
        // Crear voto temporal para almacenar el resultado
        LlmVote tempVote = new LlmVote();
        tempVote.setIntent(intent);
        tempVote.setConfidence(confidence);
        tempVote.setReasoning("Respuesta del LLM: " + llmResponse);
        
        round.setVotes(List.of(tempVote));
    }
} 