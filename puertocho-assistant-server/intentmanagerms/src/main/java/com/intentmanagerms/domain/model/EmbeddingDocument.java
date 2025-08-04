package com.intentmanagerms.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Documento con embedding para almacenamiento en vector store.
 * Representa un ejemplo de intención con su embedding vectorial.
 */
public class EmbeddingDocument {
    private String id;
    private String content;
    private String intent;
    private List<Float> embedding;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private double similarity; // Para resultados de búsqueda

    // Constructor por defecto
    public EmbeddingDocument() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor con parámetros principales
    public EmbeddingDocument(String id, String content, String intent, List<Float> embedding) {
        this();
        this.id = id;
        this.content = content;
        this.intent = intent;
        this.embedding = embedding;
    }

    // Constructor con metadata
    public EmbeddingDocument(String id, String content, String intent, List<Float> embedding, Map<String, Object> metadata) {
        this(id, content, intent, embedding);
        this.metadata = metadata;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    @Override
    public String toString() {
        return "EmbeddingDocument{" +
                "id='" + id + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\'' +
                ", intent='" + intent + '\'' +
                ", embeddingSize=" + (embedding != null ? embedding.size() : 0) +
                ", similarity=" + similarity +
                ", createdAt=" + createdAt +
                '}';
    }
} 