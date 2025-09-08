package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.EmbeddingDocument;
import com.intentmanagerms.domain.model.IntentClassificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio especializado para calcular confidence scores usando múltiples métricas avanzadas.
 * 
 * Métricas implementadas:
 * 1. Confidence del LLM (25%)
 * 2. Similitud promedio de ejemplos (20%)
 * 3. Consistencia de intenciones (15%)
 * 4. Cantidad de ejemplos relevantes (10%)
 * 5. Diversidad semántica (10%)
 * 6. Confianza temporal (5%)
 * 7. Calidad del embedding (5%)
 * 8. Entropía de similitud (5%)
 * 9. Confianza contextual (3%)
 * 10. Robustez del prompt (2%)
 */
@Service
public class ConfidenceScoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfidenceScoringService.class);
    
    // Pesos configurables para cada métrica
    @Value("${rag.confidence.weights.llm:0.25}")
    private double llmWeight;
    
    @Value("${rag.confidence.weights.similarity:0.20}")
    private double similarityWeight;
    
    @Value("${rag.confidence.weights.consistency:0.15}")
    private double consistencyWeight;
    
    @Value("${rag.confidence.weights.example-count:0.10}")
    private double exampleCountWeight;
    
    @Value("${rag.confidence.weights.semantic-diversity:0.10}")
    private double semanticDiversityWeight;
    
    @Value("${rag.confidence.weights.temporal:0.05}")
    private double temporalWeight;
    
    @Value("${rag.confidence.weights.embedding-quality:0.05}")
    private double embeddingQualityWeight;
    
    @Value("${rag.confidence.weights.entropy:0.05}")
    private double entropyWeight;
    
    @Value("${rag.confidence.weights.contextual:0.03}")
    private double contextualWeight;
    
    @Value("${rag.confidence.weights.prompt-robustness:0.02}")
    private double promptRobustnessWeight;
    
    // Umbrales configurables
    @Value("${rag.confidence.thresholds.optimal-processing-time-ms:500}")
    private long optimalProcessingTimeMs;
    
    @Value("${rag.confidence.thresholds.max-processing-time-ms:2000}")
    private long maxProcessingTimeMs;
    
    @Value("${rag.confidence.thresholds.min-examples:2}")
    private int minExamples;
    
    /**
     * Calcula el confidence score final usando todas las métricas
     */
    public double calculateConfidenceScore(IntentClassificationResult result, List<EmbeddingDocument> examples) {
        if (examples.isEmpty()) {
            return 0.0;
        }
        
        logger.debug("Calculando confidence score con {} ejemplos", examples.size());
        
        // Calcular cada métrica individual
        double llmConfidence = calculateLlmConfidence(result);
        double avgSimilarity = calculateAverageSimilarity(examples);
        double consistency = calculateIntentConsistency(examples, result.getIntentId());
        double exampleCount = calculateExampleCountScore(examples);
        double semanticDiversity = calculateSemanticDiversity(examples);
        double temporalConfidence = calculateTemporalConfidence(result);
        double embeddingQuality = calculateEmbeddingQuality(examples);
        double similarityEntropy = calculateSimilarityEntropy(examples);
        double contextualConfidence = calculateContextualConfidence(result);
        double promptRobustness = calculatePromptRobustness(result);
        
        // Cálculo ponderado
        double finalConfidence = (llmConfidence * llmWeight) + 
                                (avgSimilarity * similarityWeight) + 
                                (consistency * consistencyWeight) + 
                                (exampleCount * exampleCountWeight) +
                                (semanticDiversity * semanticDiversityWeight) +
                                (temporalConfidence * temporalWeight) +
                                (embeddingQuality * embeddingQualityWeight) +
                                (similarityEntropy * entropyWeight) +
                                (contextualConfidence * contextualWeight) +
                                (promptRobustness * promptRobustnessWeight);
        
        // Aplicar factor de corrección
        double qualityFactor = calculateQualityFactor(examples, result);
        finalConfidence *= qualityFactor;
        
        // Log detallado para debugging
        logConfidenceMetrics(result, examples, llmConfidence, avgSimilarity, consistency, 
                           exampleCount, semanticDiversity, temporalConfidence, 
                           embeddingQuality, similarityEntropy, contextualConfidence, 
                           promptRobustness, qualityFactor, finalConfidence);
        
        return Math.min(Math.max(finalConfidence, 0.0), 1.0);
    }
    
    /**
     * Calcula confidence del LLM
     */
    private double calculateLlmConfidence(IntentClassificationResult result) {
        return result.getConfidenceScore() != null ? result.getConfidenceScore() : 0.0;
    }
    
    /**
     * Calcula similitud promedio de ejemplos
     */
    private double calculateAverageSimilarity(List<EmbeddingDocument> examples) {
        return examples.stream()
                .mapToDouble(EmbeddingDocument::getSimilarity)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Calcula consistencia de intenciones en ejemplos
     */
    private double calculateIntentConsistency(List<EmbeddingDocument> examples, String targetIntent) {
        if (examples.isEmpty()) {
            return 0.0;
        }
        
        long matchingIntents = examples.stream()
                .filter(doc -> targetIntent.equals(doc.getIntent()))
                .count();
        
        return (double) matchingIntents / examples.size();
    }
    
    /**
     * Calcula score basado en cantidad de ejemplos
     */
    private double calculateExampleCountScore(List<EmbeddingDocument> examples) {
        return Math.min(examples.size() / 5.0, 1.0);
    }
    
    /**
     * Calcula diversidad semántica de los ejemplos
     */
    private double calculateSemanticDiversity(List<EmbeddingDocument> examples) {
        if (examples.size() < 2) {
            return 0.5;
        }
        
        double mean = examples.stream()
                .mapToDouble(EmbeddingDocument::getSimilarity)
                .average()
                .orElse(0.0);
        
        double variance = examples.stream()
                .mapToDouble(doc -> Math.pow(doc.getSimilarity() - mean, 2))
                .average()
                .orElse(0.0);
        
        // Menor varianza = mayor diversidad semántica
        return Math.max(0.0, 1.0 - Math.sqrt(variance));
    }
    
    /**
     * Calcula confianza basada en tiempo de procesamiento
     */
    private double calculateTemporalConfidence(IntentClassificationResult result) {
        if (result.getProcessingTimeMs() == null) {
            return 0.5;
        }
        
        long processingTime = result.getProcessingTimeMs();
        
        // Tiempo óptimo
        if (processingTime <= optimalProcessingTimeMs) {
            return 1.0;
        }
        
        // Tiempo aceptable
        if (processingTime <= maxProcessingTimeMs) {
            return 0.8;
        }
        
        // Tiempo lento
        return Math.max(0.3, 1.0 - (processingTime - maxProcessingTimeMs) / 5000.0);
    }
    
    /**
     * Calcula calidad del embedding basada en distribución
     */
    private double calculateEmbeddingQuality(List<EmbeddingDocument> examples) {
        if (examples.size() < 2) {
            return 0.5;
        }
        
        double mean = examples.stream()
                .mapToDouble(EmbeddingDocument::getSimilarity)
                .average()
                .orElse(0.0);
        
        double stdDev = Math.sqrt(examples.stream()
                .mapToDouble(doc -> Math.pow(doc.getSimilarity() - mean, 2))
                .average()
                .orElse(0.0));
        
        // Menor desviación = mayor calidad
        return Math.max(0.0, 1.0 - stdDev);
    }
    
    /**
     * Calcula entropía de similitud para medir distribución
     */
    private double calculateSimilarityEntropy(List<EmbeddingDocument> examples) {
        if (examples.size() < 2) {
            return 0.5;
        }
        
        // Agrupar similitudes en bins
        Map<Integer, Long> bins = examples.stream()
                .collect(Collectors.groupingBy(
                    doc -> (int) (doc.getSimilarity() * 10), // 10 bins de 0.1
                    Collectors.counting()
                ));
        
        // Calcular entropía
        double entropy = bins.values().stream()
                .mapToDouble(count -> {
                    double p = (double) count / examples.size();
                    return -p * Math.log(p) / Math.log(2);
                })
                .sum();
        
        // Normalizar entropía (0-1)
        double maxEntropy = Math.log(examples.size()) / Math.log(2);
        return entropy / maxEntropy;
    }
    
    /**
     * Calcula confianza contextual basada en metadata
     */
    private double calculateContextualConfidence(IntentClassificationResult result) {
        double confidence = 0.5;
        
        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
            confidence += 0.2;
        }
        
        if (result.getExamplesUsed() != null && !result.getExamplesUsed().isEmpty()) {
            // Boost de confianza basado en el número de ejemplos utilizados
            double examplesBoost = Math.min(0.2, result.getExamplesUsed().size() * 0.05);
            confidence += examplesBoost;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Calcula robustez del prompt generado
     */
    private double calculatePromptRobustness(IntentClassificationResult result) {
        if (result.getPromptUsed() == null || result.getPromptUsed().trim().isEmpty()) {
            return 0.0;
        }
        
        String prompt = result.getPromptUsed();
        double robustness = 0.5;
        
        if (prompt.length() > 100) {
            robustness += 0.2;
        }
        
        if (prompt.contains("ejemplo") || prompt.contains("example")) {
            robustness += 0.2;
        }
        
        if (prompt.contains("clasifica") || prompt.contains("classify")) {
            robustness += 0.1;
        }
        
        return Math.min(robustness, 1.0);
    }
    
    /**
     * Calcula factor de calidad general
     */
    private double calculateQualityFactor(List<EmbeddingDocument> examples, IntentClassificationResult result) {
        double factor = 1.0;
        
        if (examples.size() < minExamples) {
            factor *= 0.8;
        }
        
        if (result.getProcessingTimeMs() != null && result.getProcessingTimeMs() > maxProcessingTimeMs) {
            factor *= 0.9;
        }
        
        if (result.getFallbackUsed() != null && result.getFallbackUsed()) {
            factor *= 0.7;
        }
        
        return factor;
    }
    
    /**
     * Log detallado de métricas para debugging
     */
    private void logConfidenceMetrics(IntentClassificationResult result, List<EmbeddingDocument> examples,
                                    double llmConfidence, double avgSimilarity, double consistency,
                                    double exampleCount, double semanticDiversity, double temporalConfidence,
                                    double embeddingQuality, double similarityEntropy, double contextualConfidence,
                                    double promptRobustness, double qualityFactor, double finalConfidence) {
        
        if (logger.isDebugEnabled()) {
            logger.debug("=== CONFIDENCE SCORING BREAKDOWN ===");
            logger.debug("LLM Confidence: {:.3f} (weight: {:.2f})", llmConfidence, llmWeight);
            logger.debug("Average Similarity: {:.3f} (weight: {:.2f})", avgSimilarity, similarityWeight);
            logger.debug("Intent Consistency: {:.3f} (weight: {:.2f})", consistency, consistencyWeight);
            logger.debug("Example Count Score: {:.3f} (weight: {:.2f})", exampleCount, exampleCountWeight);
            logger.debug("Semantic Diversity: {:.3f} (weight: {:.2f})", semanticDiversity, semanticDiversityWeight);
            logger.debug("Temporal Confidence: {:.3f} (weight: {:.2f})", temporalConfidence, temporalWeight);
            logger.debug("Embedding Quality: {:.3f} (weight: {:.2f})", embeddingQuality, embeddingQualityWeight);
            logger.debug("Similarity Entropy: {:.3f} (weight: {:.2f})", similarityEntropy, entropyWeight);
            logger.debug("Contextual Confidence: {:.3f} (weight: {:.2f})", contextualConfidence, contextualWeight);
            logger.debug("Prompt Robustness: {:.3f} (weight: {:.2f})", promptRobustness, promptRobustnessWeight);
            logger.debug("Quality Factor: {:.3f}", qualityFactor);
            logger.debug("Final Confidence: {:.3f}", finalConfidence);
            logger.debug("Examples used: {}", examples.size());
            logger.debug("Processing time: {}ms", result.getProcessingTimeMs());
            logger.debug("Fallback used: {}", result.getFallbackUsed());
            logger.debug("=====================================");
        }
    }
    
    /**
     * Obtiene un resumen de métricas para análisis
     */
    public Map<String, Double> getConfidenceMetrics(IntentClassificationResult result, List<EmbeddingDocument> examples) {
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("llm_confidence", calculateLlmConfidence(result));
        metrics.put("average_similarity", calculateAverageSimilarity(examples));
        metrics.put("intent_consistency", calculateIntentConsistency(examples, result.getIntentId()));
        metrics.put("example_count_score", calculateExampleCountScore(examples));
        metrics.put("semantic_diversity", calculateSemanticDiversity(examples));
        metrics.put("temporal_confidence", calculateTemporalConfidence(result));
        metrics.put("embedding_quality", calculateEmbeddingQuality(examples));
        metrics.put("similarity_entropy", calculateSimilarityEntropy(examples));
        metrics.put("contextual_confidence", calculateContextualConfidence(result));
        metrics.put("prompt_robustness", calculatePromptRobustness(result));
        metrics.put("quality_factor", calculateQualityFactor(examples, result));
        metrics.put("final_confidence", calculateConfidenceScore(result, examples));
        return metrics;
    }
} 