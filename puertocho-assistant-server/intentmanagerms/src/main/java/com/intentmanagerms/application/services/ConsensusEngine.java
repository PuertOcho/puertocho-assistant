package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de consenso avanzado para procesar votos de múltiples LLMs y llegar a decisiones finales.
 * 
 * Responsabilidades:
 * - Procesar votos de múltiples LLMs con diferentes algoritmos de consenso
 * - Calcular métricas de confianza y acuerdo
 * - Aplicar estrategias de resolución de conflictos
 * - Combinar entidades y subtareas de múltiples votos
 * - Proporcionar razonamiento detallado del proceso de consenso
 * 
 * T3.3: Desarrollar ConsensusEngine para procesar votos y llegar a decisión final
 */
@Service
public class ConsensusEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsensusEngine.class);
    
    // Configuración desde application.yml
    @Value("${moe.consensus.algorithm:weighted-majority}")
    private String consensusAlgorithm;
    
    @Value("${moe.consensus.confidence-threshold:0.6}")
    private double confidenceThreshold;
    
    @Value("${moe.consensus.minimum-votes:2}")
    private int minimumVotes;
    
    @Value("${moe.consensus.enable-weighted-scoring:true}")
    private boolean enableWeightedScoring;
    
    @Value("${moe.consensus.enable-confidence-boosting:true}")
    private boolean enableConfidenceBoosting;
    
    @Value("${moe.consensus.confidence-boost-factor:0.1}")
    private double confidenceBoostFactor;
    
    @Value("${moe.consensus.enable-entity-merging:true}")
    private boolean enableEntityMerging;
    
    @Value("${moe.consensus.enable-subtask-consolidation:true}")
    private boolean enableSubtaskConsolidation;
    
    /**
     * Procesa una lista de votos y genera un consenso final.
     */
    public VotingConsensus processConsensus(List<LlmVote> votes, VotingRound round) {
        logger.info("Procesando consenso para {} votos en ronda {}", votes.size(), round.getRoundId());
        
        if (votes == null || votes.isEmpty()) {
            logger.warn("No hay votos para procesar consenso");
            return createFailedConsensus(0);
        }
        
        // Filtrar votos válidos
        List<LlmVote> validVotes = filterValidVotes(votes);
        
        if (validVotes.size() < minimumVotes) {
            logger.warn("Votos insuficientes para consenso: {} < {}", validVotes.size(), minimumVotes);
            return createFailedConsensus(votes.size());
        }
        
        try {
            // Aplicar algoritmo de consenso seleccionado
            VotingConsensus consensus = applyConsensusAlgorithm(validVotes, round);
            
            // Calcular métricas adicionales
            calculateConsensusMetrics(consensus, validVotes);
            
            // Combinar entidades y subtareas
            if (enableEntityMerging) {
                consensus.setFinalEntities(mergeEntities(validVotes));
            }
            
            if (enableSubtaskConsolidation) {
                consensus.setFinalSubtasks(consolidateSubtasks(validVotes));
            }
            
            // Generar razonamiento del consenso
            consensus.setReasoning(generateConsensusReasoning(consensus, validVotes));
            
            logger.info("Consenso procesado exitosamente: {} (confianza: {})", 
                       consensus.getFinalIntent(), consensus.getConsensusConfidence());
            
            return consensus;
            
        } catch (Exception e) {
            logger.error("Error procesando consenso: {}", e.getMessage(), e);
            return createFailedConsensus(votes.size());
        }
    }
    
    /**
     * Aplica el algoritmo de consenso seleccionado.
     */
    private VotingConsensus applyConsensusAlgorithm(List<LlmVote> validVotes, VotingRound round) {
        switch (consensusAlgorithm.toLowerCase()) {
            case "weighted-majority":
                return applyWeightedMajorityAlgorithm(validVotes, round);
            case "plurality":
                return applyPluralityAlgorithm(validVotes, round);
            case "confidence-weighted":
                return applyConfidenceWeightedAlgorithm(validVotes, round);
            case "borda-count":
                return applyBordaCountAlgorithm(validVotes, round);
            case "condorcet":
                return applyCondorcetAlgorithm(validVotes, round);
            case "approval-voting":
                return applyApprovalVotingAlgorithm(validVotes, round);
            default:
                logger.warn("Algoritmo de consenso desconocido: {}, usando weighted-majority", consensusAlgorithm);
                return applyWeightedMajorityAlgorithm(validVotes, round);
        }
    }
    
    /**
     * Algoritmo de mayoría ponderada (algoritmo principal).
     */
    private VotingConsensus applyWeightedMajorityAlgorithm(List<LlmVote> validVotes, VotingRound round) {
        Map<String, Double> intentScores = new HashMap<>();
        Map<String, List<LlmVote>> votesByIntent = new HashMap<>();
        
        // Agrupar votos por intención y calcular puntuaciones ponderadas
        for (LlmVote vote : validVotes) {
            String intent = vote.getIntent();
            double weight = enableWeightedScoring ? vote.getLlmWeight() : 1.0;
            double confidence = vote.getConfidence() != null ? vote.getConfidence() : 0.5;
            
            intentScores.merge(intent, weight * confidence, Double::sum);
            votesByIntent.computeIfAbsent(intent, k -> new ArrayList<>()).add(vote);
        }
        
        // Encontrar la intención con mayor puntuación
        String winningIntent = intentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
        
        double totalScore = intentScores.values().stream().mapToDouble(Double::doubleValue).sum();
        double winningScore = intentScores.getOrDefault(winningIntent, 0.0);
        double consensusConfidence = totalScore > 0 ? winningScore / totalScore : 0.0;
        
        // Aplicar boost de confianza si está habilitado
        if (enableConfidenceBoosting && consensusConfidence >= confidenceThreshold) {
            consensusConfidence = Math.min(1.0, consensusConfidence + confidenceBoostFactor);
        }
        
        VotingConsensus consensus = new VotingConsensus(
            generateConsensusId(round.getRoundId()),
            winningIntent,
            consensusConfidence,
            validVotes.size(),
            validVotes.size()
        );
        
        consensus.setConsensusMethod("weighted-majority");
        consensus.setAgreementLevel(determineAgreementLevel(votesByIntent, validVotes.size()));
        
        return consensus;
    }
    
    /**
     * Algoritmo de pluralidad (mayoría simple).
     */
    private VotingConsensus applyPluralityAlgorithm(List<LlmVote> validVotes, VotingRound round) {
        Map<String, Long> intentCounts = validVotes.stream()
                .collect(Collectors.groupingBy(LlmVote::getIntent, Collectors.counting()));
        
        String winningIntent = intentCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
        
        long winningCount = intentCounts.getOrDefault(winningIntent, 0L);
        double consensusConfidence = (double) winningCount / validVotes.size();
        
        VotingConsensus consensus = new VotingConsensus(
            generateConsensusId(round.getRoundId()),
            winningIntent,
            consensusConfidence,
            validVotes.size(),
            validVotes.size()
        );
        
        consensus.setConsensusMethod("plurality");
        consensus.setAgreementLevel(determineAgreementLevel(
            validVotes.stream().collect(Collectors.groupingBy(LlmVote::getIntent)), 
            validVotes.size()
        ));
        
        return consensus;
    }
    
    /**
     * Algoritmo ponderado por confianza.
     */
    private VotingConsensus applyConfidenceWeightedAlgorithm(List<LlmVote> validVotes, VotingRound round) {
        Map<String, Double> intentScores = new HashMap<>();
        
        for (LlmVote vote : validVotes) {
            String intent = vote.getIntent();
            double confidence = vote.getConfidence() != null ? vote.getConfidence() : 0.5;
            intentScores.merge(intent, confidence, Double::sum);
        }
        
        String winningIntent = intentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
        
        double totalScore = intentScores.values().stream().mapToDouble(Double::doubleValue).sum();
        double winningScore = intentScores.getOrDefault(winningIntent, 0.0);
        double consensusConfidence = totalScore > 0 ? winningScore / totalScore : 0.0;
        
        VotingConsensus consensus = new VotingConsensus(
            generateConsensusId(round.getRoundId()),
            winningIntent,
            consensusConfidence,
            validVotes.size(),
            validVotes.size()
        );
        
        consensus.setConsensusMethod("confidence-weighted");
        consensus.setAgreementLevel(determineAgreementLevel(
            validVotes.stream().collect(Collectors.groupingBy(LlmVote::getIntent)), 
            validVotes.size()
        ));
        
        return consensus;
    }
    
    /**
     * Algoritmo de conteo Borda.
     */
    private VotingConsensus applyBordaCountAlgorithm(List<LlmVote> validVotes, VotingRound round) {
        // Implementación simplificada del conteo Borda
        Map<String, Double> intentScores = new HashMap<>();
        
        for (LlmVote vote : validVotes) {
            String intent = vote.getIntent();
            double weight = enableWeightedScoring ? vote.getLlmWeight() : 1.0;
            intentScores.merge(intent, weight, Double::sum);
        }
        
        String winningIntent = intentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
        
        double totalScore = intentScores.values().stream().mapToDouble(Double::doubleValue).sum();
        double winningScore = intentScores.getOrDefault(winningIntent, 0.0);
        double consensusConfidence = totalScore > 0 ? winningScore / totalScore : 0.0;
        
        VotingConsensus consensus = new VotingConsensus(
            generateConsensusId(round.getRoundId()),
            winningIntent,
            consensusConfidence,
            validVotes.size(),
            validVotes.size()
        );
        
        consensus.setConsensusMethod("borda-count");
        consensus.setAgreementLevel(determineAgreementLevel(
            validVotes.stream().collect(Collectors.groupingBy(LlmVote::getIntent)), 
            validVotes.size()
        ));
        
        return consensus;
    }
    
    /**
     * Algoritmo de Condorcet (simplificado).
     */
    private VotingConsensus applyCondorcetAlgorithm(List<LlmVote> validVotes, VotingRound round) {
        // Implementación simplificada del método Condorcet
        return applyWeightedMajorityAlgorithm(validVotes, round);
    }
    
    /**
     * Algoritmo de votación por aprobación.
     */
    private VotingConsensus applyApprovalVotingAlgorithm(List<LlmVote> validVotes, VotingRound round) {
        // Implementación simplificada de votación por aprobación
        return applyPluralityAlgorithm(validVotes, round);
    }
    
    /**
     * Filtra votos válidos.
     */
    private List<LlmVote> filterValidVotes(List<LlmVote> votes) {
        return votes.stream()
                .filter(vote -> vote != null && 
                               vote.getIntent() != null && 
                               !vote.getIntent().trim().isEmpty() &&
                               vote.getConfidence() != null &&
                               vote.getConfidence() >= 0.0)
                .collect(Collectors.toList());
    }
    
    /**
     * Determina el nivel de acuerdo basado en la distribución de votos.
     */
    private VotingConsensus.AgreementLevel determineAgreementLevel(Map<String, List<LlmVote>> votesByIntent, int totalVotes) {
        if (votesByIntent.size() == 1) {
            return VotingConsensus.AgreementLevel.UNANIMOUS;
        }
        
        int maxVotes = votesByIntent.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
        
        double majorityThreshold = totalVotes * 0.5;
        
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
    private Map<String, Object> mergeEntities(List<LlmVote> votes) {
        Map<String, Object> mergedEntities = new HashMap<>();
        Map<String, List<Object>> entityValues = new HashMap<>();
        
        for (LlmVote vote : votes) {
            if (vote.getEntities() != null) {
                for (Map.Entry<String, Object> entry : vote.getEntities().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    entityValues.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                }
            }
        }
        
        // Para cada entidad, seleccionar el valor más común o combinar
        for (Map.Entry<String, List<Object>> entry : entityValues.entrySet()) {
            String key = entry.getKey();
            List<Object> values = entry.getValue();
            
            // Si todos los valores son iguales, usar uno
            if (values.stream().distinct().count() == 1) {
                mergedEntities.put(key, values.get(0));
            } else {
                // Si hay diferentes valores, usar el más común
                Object mostCommon = values.stream()
                        .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                        .entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(values.get(0));
                
                mergedEntities.put(key, mostCommon);
            }
        }
        
        return mergedEntities;
    }
    
    /**
     * Consolida subtareas de múltiples votos.
     */
    private List<Map<String, Object>> consolidateSubtasks(List<LlmVote> votes) {
        List<Map<String, Object>> allSubtasks = new ArrayList<>();
        
        for (LlmVote vote : votes) {
            if (vote.getSubtasks() != null) {
                allSubtasks.addAll(vote.getSubtasks());
            }
        }
        
        // Eliminar subtareas duplicadas basándose en el contenido
        return allSubtasks.stream()
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Calcula métricas adicionales del consenso.
     */
    private void calculateConsensusMetrics(VotingConsensus consensus, List<LlmVote> votes) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Calcular confianza promedio
        double avgConfidence = votes.stream()
                .mapToDouble(v -> v.getConfidence() != null ? v.getConfidence() : 0.0)
                .average()
                .orElse(0.0);
        
        metrics.put("averageConfidence", avgConfidence);
        metrics.put("totalVotes", votes.size());
        metrics.put("algorithm", consensusAlgorithm);
        metrics.put("confidenceThreshold", confidenceThreshold);
        metrics.put("weightedScoringEnabled", enableWeightedScoring);
        metrics.put("confidenceBoostingEnabled", enableConfidenceBoosting);
        
        consensus.setConsensusMetrics(metrics);
    }
    
    /**
     * Genera razonamiento detallado del proceso de consenso.
     */
    private String generateConsensusReasoning(VotingConsensus consensus, List<LlmVote> votes) {
        StringBuilder reasoning = new StringBuilder();
        
        reasoning.append("Proceso de consenso completado usando algoritmo '").append(consensusAlgorithm).append("'.\n");
        reasoning.append("Total de votos procesados: ").append(votes.size()).append("\n");
        reasoning.append("Votos válidos: ").append(consensus.getParticipatingVotes()).append("\n");
        reasoning.append("Nivel de acuerdo: ").append(consensus.getAgreementLevel().getDescription()).append("\n");
        reasoning.append("Confianza del consenso: ").append(String.format("%.2f", consensus.getConsensusConfidence())).append("\n");
        
        // Agregar detalles de votos individuales
        reasoning.append("\nDetalles de votos:\n");
        for (LlmVote vote : votes) {
            reasoning.append("- LLM ").append(vote.getLlmId())
                    .append(": ").append(vote.getIntent())
                    .append(" (confianza: ").append(String.format("%.2f", vote.getConfidence()))
                    .append(", peso: ").append(String.format("%.2f", vote.getLlmWeight()))
                    .append(")\n");
        }
        
        return reasoning.toString();
    }
    
    /**
     * Crea un consenso fallido.
     */
    private VotingConsensus createFailedConsensus(int totalVotes) {
        VotingConsensus consensus = new VotingConsensus(
            "failed-consensus-" + System.currentTimeMillis(),
            "unknown",
            0.0,
            0,
            totalVotes
        );
        
        consensus.setAgreementLevel(VotingConsensus.AgreementLevel.FAILED);
        consensus.setConsensusMethod("failed");
        consensus.setReasoning("No se pudo alcanzar consenso debido a votos insuficientes o errores en el procesamiento.");
        
        return consensus;
    }
    
    /**
     * Genera ID único para el consenso.
     */
    private String generateConsensusId(String roundId) {
        return "consensus-" + roundId + "-" + System.currentTimeMillis();
    }
    
    /**
     * Obtiene estadísticas del motor de consenso.
     */
    public Map<String, Object> getConsensusStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("algorithm", consensusAlgorithm);
        stats.put("confidenceThreshold", confidenceThreshold);
        stats.put("minimumVotes", minimumVotes);
        stats.put("weightedScoringEnabled", enableWeightedScoring);
        stats.put("confidenceBoostingEnabled", enableConfidenceBoosting);
        stats.put("confidenceBoostFactor", confidenceBoostFactor);
        stats.put("entityMergingEnabled", enableEntityMerging);
        stats.put("subtaskConsolidationEnabled", enableSubtaskConsolidation);
        
        return stats;
    }
    
    /**
     * Verifica si el motor de consenso está saludable.
     */
    public boolean isHealthy() {
        return true; // El motor de consenso siempre está saludable
    }
} 