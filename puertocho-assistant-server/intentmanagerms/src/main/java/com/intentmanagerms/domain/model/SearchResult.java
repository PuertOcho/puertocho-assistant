package com.intentmanagerms.domain.model;

import java.util.List;

/**
 * Resultado de búsqueda en el vector store.
 * Contiene los documentos más similares y metadatos de la búsqueda.
 */
public class SearchResult {
    private List<EmbeddingDocument> documents;
    private String query;
    private int totalResults;
    private long searchTimeMs;
    private double minSimilarity;
    private double maxSimilarity;
    private String vectorStoreType;

    // Constructor por defecto
    public SearchResult() {}

    // Constructor con parámetros principales
    public SearchResult(List<EmbeddingDocument> documents, String query) {
        this.documents = documents;
        this.query = query;
        this.totalResults = documents != null ? documents.size() : 0;
        calculateSimilarityRange();
    }

    // Constructor completo
    public SearchResult(List<EmbeddingDocument> documents, String query, long searchTimeMs, String vectorStoreType) {
        this(documents, query);
        this.searchTimeMs = searchTimeMs;
        this.vectorStoreType = vectorStoreType;
    }

    // Getters y Setters
    public List<EmbeddingDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<EmbeddingDocument> documents) {
        this.documents = documents;
        this.totalResults = documents != null ? documents.size() : 0;
        calculateSimilarityRange();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public long getSearchTimeMs() {
        return searchTimeMs;
    }

    public void setSearchTimeMs(long searchTimeMs) {
        this.searchTimeMs = searchTimeMs;
    }

    public double getMinSimilarity() {
        return minSimilarity;
    }

    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }

    public double getMaxSimilarity() {
        return maxSimilarity;
    }

    public void setMaxSimilarity(double maxSimilarity) {
        this.maxSimilarity = maxSimilarity;
    }

    public String getVectorStoreType() {
        return vectorStoreType;
    }

    public void setVectorStoreType(String vectorStoreType) {
        this.vectorStoreType = vectorStoreType;
    }

    /**
     * Calcula el rango de similitud de los documentos encontrados.
     */
    private void calculateSimilarityRange() {
        if (documents == null || documents.isEmpty()) {
            this.minSimilarity = 0.0;
            this.maxSimilarity = 0.0;
            return;
        }

        this.minSimilarity = documents.stream()
                .mapToDouble(EmbeddingDocument::getSimilarity)
                .min()
                .orElse(0.0);

        this.maxSimilarity = documents.stream()
                .mapToDouble(EmbeddingDocument::getSimilarity)
                .max()
                .orElse(0.0);
    }

    /**
     * Obtiene el documento con mayor similitud.
     */
    public EmbeddingDocument getBestMatch() {
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        return documents.stream()
                .max((a, b) -> Double.compare(a.getSimilarity(), b.getSimilarity()))
                .orElse(null);
    }

    /**
     * Obtiene documentos con similitud por encima de un umbral.
     */
    public List<EmbeddingDocument> getDocumentsAboveThreshold(double threshold) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .filter(doc -> doc.getSimilarity() >= threshold)
                .toList();
    }

    /**
     * Verifica si hay resultados con similitud alta.
     */
    public boolean hasHighConfidenceResults(double threshold) {
        return documents != null && documents.stream()
                .anyMatch(doc -> doc.getSimilarity() >= threshold);
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "query='" + query + '\'' +
                ", totalResults=" + totalResults +
                ", searchTimeMs=" + searchTimeMs +
                ", similarityRange=" + minSimilarity + "-" + maxSimilarity +
                ", vectorStoreType='" + vectorStoreType + '\'' +
                '}';
    }
} 