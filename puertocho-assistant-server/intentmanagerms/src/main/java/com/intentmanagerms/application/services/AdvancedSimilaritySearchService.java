package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.EmbeddingDocument;
import com.intentmanagerms.domain.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio avanzado de búsqueda por similitud para el motor RAG.
 * Implementa múltiples algoritmos de similitud y optimizaciones de rendimiento.
 */
@Service
public class AdvancedSimilaritySearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedSimilaritySearchService.class);
    
    @Value("${rag.similarity.search-algorithm:cosine}")
    private String searchAlgorithm;
    
    @Value("${rag.similarity.diversity-threshold:0.3}")
    private double diversityThreshold;
    
    @Value("${rag.similarity.intent-weight:0.7}")
    private double intentWeight;
    
    @Value("${rag.similarity.content-weight:0.3}")
    private double contentWeight;
    
    @Value("${rag.similarity.enable-diversity-filtering:true}")
    private boolean enableDiversityFiltering;
    
    @Value("${rag.similarity.enable-intent-clustering:true}")
    private boolean enableIntentClustering;
    
    @Value("${rag.similarity.max-cluster-size:3}")
    private int maxClusterSize;
    
    @Value("${rag.similarity.enable-semantic-boosting:true}")
    private boolean enableSemanticBoosting;
    
    // Cache para optimización de rendimiento
    private final Map<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();
    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();
    
    /**
     * Realiza búsqueda avanzada por similitud con múltiples algoritmos
     */
    public SearchResult advancedSearch(
            String query, 
            List<Float> queryEmbedding, 
            List<EmbeddingDocument> documents, 
            int limit, 
            double threshold) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Calcular similitudes usando algoritmo seleccionado
            List<SearchCandidate> candidates = calculateSimilarities(queryEmbedding, documents);
            
            // 2. Aplicar filtros de calidad
            candidates = applyQualityFilters(candidates, threshold);
            
            // 3. Aplicar clustering por intención si está habilitado
            if (enableIntentClustering) {
                candidates = applyIntentClustering(candidates);
            }
            
            // 4. Aplicar filtrado de diversidad si está habilitado
            if (enableDiversityFiltering) {
                candidates = applyDiversityFiltering(candidates);
            }
            
            // 5. Aplicar boosting semántico si está habilitado
            if (enableSemanticBoosting) {
                candidates = applySemanticBoosting(candidates, query);
            }
            
            // 6. Ordenar y limitar resultados
            candidates.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
            candidates = candidates.stream().limit(limit).collect(Collectors.toList());
            
            // 7. Convertir a EmbeddingDocument
            List<EmbeddingDocument> results = candidates.stream()
                    .map(SearchCandidate::getDocument)
                    .collect(Collectors.toList());
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            logger.debug("Búsqueda avanzada completada en {}ms. Encontrados {} resultados de {} documentos", 
                    searchTime, results.size(), documents.size());
            
            return new SearchResult(results, query);
            
        } catch (Exception e) {
            logger.error("Error en búsqueda avanzada: {}", e.getMessage(), e);
            return new SearchResult(new ArrayList<>(), query);
        }
    }
    
    /**
     * Calcula similitudes usando múltiples algoritmos
     */
    private List<SearchCandidate> calculateSimilarities(List<Float> queryEmbedding, List<EmbeddingDocument> documents) {
        List<SearchCandidate> candidates = new ArrayList<>();
        
        for (EmbeddingDocument doc : documents) {
            double similarity = 0.0;
            
            switch (searchAlgorithm.toLowerCase()) {
                case "cosine":
                    similarity = calculateCosineSimilarity(queryEmbedding, doc.getEmbedding());
                    break;
                case "euclidean":
                    similarity = calculateEuclideanSimilarity(queryEmbedding, doc.getEmbedding());
                    break;
                case "manhattan":
                    similarity = calculateManhattanSimilarity(queryEmbedding, doc.getEmbedding());
                    break;
                case "hybrid":
                    similarity = calculateHybridSimilarity(queryEmbedding, doc.getEmbedding(), doc);
                    break;
                default:
                    similarity = calculateCosineSimilarity(queryEmbedding, doc.getEmbedding());
            }
            
            SearchCandidate candidate = new SearchCandidate(doc, similarity);
            candidates.add(candidate);
        }
        
        return candidates;
    }
    
    /**
     * Similitud coseno (implementación optimizada)
     */
    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            double v1 = vector1.get(i);
            double v2 = vector2.get(i);
            
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * Similitud euclidiana (convertida a similitud 0-1)
     */
    private double calculateEuclideanSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }
        
        double sumSquaredDiff = 0.0;
        for (int i = 0; i < vector1.size(); i++) {
            double diff = vector1.get(i) - vector2.get(i);
            sumSquaredDiff += diff * diff;
        }
        
        double distance = Math.sqrt(sumSquaredDiff);
        // Convertir distancia a similitud (0-1)
        return 1.0 / (1.0 + distance);
    }
    
    /**
     * Similitud Manhattan (convertida a similitud 0-1)
     */
    private double calculateManhattanSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }
        
        double sumAbsDiff = 0.0;
        for (int i = 0; i < vector1.size(); i++) {
            sumAbsDiff += Math.abs(vector1.get(i) - vector2.get(i));
        }
        
        // Convertir distancia a similitud (0-1)
        return 1.0 / (1.0 + sumAbsDiff);
    }
    
    /**
     * Similitud híbrida que combina embedding + características del documento
     */
    private double calculateHybridSimilarity(List<Float> queryEmbedding, List<Float> docEmbedding, EmbeddingDocument doc) {
        // Similitud de embedding (70%)
        double embeddingSimilarity = calculateCosineSimilarity(queryEmbedding, docEmbedding);
        
        // Similitud de contenido (30%) - basada en características del documento
        double contentSimilarity = calculateContentSimilarity(doc);
        
        return (intentWeight * embeddingSimilarity) + (contentWeight * contentSimilarity);
    }
    
    /**
     * Calcula similitud basada en características del contenido
     */
    private double calculateContentSimilarity(EmbeddingDocument doc) {
        double score = 0.0;
        
        // Factor de longitud del contenido
        int contentLength = doc.getContent().length();
        if (contentLength > 0) {
            score += Math.min(contentLength / 100.0, 1.0) * 0.2;
        }
        
        // Factor de calidad del embedding
        if (doc.getEmbedding() != null && !doc.getEmbedding().isEmpty()) {
            score += 0.3;
        }
        
        // Factor de metadata
        if (doc.getMetadata() != null && !doc.getMetadata().isEmpty()) {
            score += 0.2;
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Aplica filtros de calidad
     */
    private List<SearchCandidate> applyQualityFilters(List<SearchCandidate> candidates, double threshold) {
        return candidates.stream()
                .filter(c -> c.getSimilarity() >= threshold)
                .collect(Collectors.toList());
    }
    
    /**
     * Aplica clustering por intención para diversificar resultados
     */
    private List<SearchCandidate> applyIntentClustering(List<SearchCandidate> candidates) {
        Map<String, List<SearchCandidate>> intentGroups = candidates.stream()
                .collect(Collectors.groupingBy(c -> c.getDocument().getIntent()));
        
        List<SearchCandidate> clusteredCandidates = new ArrayList<>();
        
        for (List<SearchCandidate> group : intentGroups.values()) {
            // Ordenar por similitud y tomar los mejores de cada grupo
            group.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
            clusteredCandidates.addAll(group.stream().limit(maxClusterSize).collect(Collectors.toList()));
        }
        
        return clusteredCandidates;
    }
    
    /**
     * Aplica filtrado de diversidad para evitar resultados muy similares
     */
    private List<SearchCandidate> applyDiversityFiltering(List<SearchCandidate> candidates) {
        if (candidates.size() <= 1) {
            return candidates;
        }
        
        List<SearchCandidate> diverseCandidates = new ArrayList<>();
        diverseCandidates.add(candidates.get(0)); // Siempre incluir el mejor
        
        for (int i = 1; i < candidates.size(); i++) {
            SearchCandidate candidate = candidates.get(i);
            boolean isDiverse = true;
            
            // Verificar diversidad con candidatos ya seleccionados
            for (SearchCandidate selected : diverseCandidates) {
                double similarity = calculateCosineSimilarity(
                        candidate.getDocument().getEmbedding(),
                        selected.getDocument().getEmbedding()
                );
                
                if (similarity > diversityThreshold) {
                    isDiverse = false;
                    break;
                }
            }
            
            if (isDiverse) {
                diverseCandidates.add(candidate);
            }
        }
        
        return diverseCandidates;
    }
    
    /**
     * Aplica boosting semántico basado en palabras clave
     */
    private List<SearchCandidate> applySemanticBoosting(List<SearchCandidate> candidates, String query) {
        String queryLower = query.toLowerCase();
        
        for (SearchCandidate candidate : candidates) {
            double boost = 1.0;
            String content = candidate.getDocument().getContent().toLowerCase();
            
            // Boosting por palabras clave exactas
            if (content.contains(queryLower)) {
                boost += 0.1;
            }
            
            // Boosting por palabras individuales
            String[] queryWords = queryLower.split("\\s+");
            for (String word : queryWords) {
                if (word.length() > 2 && content.contains(word)) {
                    boost += 0.05;
                }
            }
            
            // Aplicar boost
            candidate.setFinalScore(candidate.getSimilarity() * boost);
        }
        
        return candidates;
    }
    
    /**
     * Clase interna para candidatos de búsqueda
     */
    private static class SearchCandidate {
        private final EmbeddingDocument document;
        private final double similarity;
        private double finalScore;
        
        public SearchCandidate(EmbeddingDocument document, double similarity) {
            this.document = document;
            this.similarity = similarity;
            this.finalScore = similarity;
        }
        
        public EmbeddingDocument getDocument() { return document; }
        public double getSimilarity() { return similarity; }
        public double getFinalScore() { return finalScore; }
        public void setFinalScore(double finalScore) { this.finalScore = finalScore; }
    }
    
    /**
     * Obtiene estadísticas del servicio
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("search_algorithm", searchAlgorithm);
        stats.put("diversity_threshold", diversityThreshold);
        stats.put("intent_weight", intentWeight);
        stats.put("content_weight", contentWeight);
        stats.put("enable_diversity_filtering", enableDiversityFiltering);
        stats.put("enable_intent_clustering", enableIntentClustering);
        stats.put("enable_semantic_boosting", enableSemanticBoosting);
        stats.put("max_cluster_size", maxClusterSize);
        stats.put("embedding_cache_size", embeddingCache.size());
        stats.put("similarity_cache_size", similarityCache.size());
        
        return stats;
    }
} 