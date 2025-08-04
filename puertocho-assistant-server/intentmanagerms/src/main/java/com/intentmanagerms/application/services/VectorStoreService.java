package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.EmbeddingDocument;
import com.intentmanagerms.domain.model.SearchResult;
import com.intentmanagerms.domain.model.VectorStoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar el vector store de embeddings.
 * 
 * Responsabilidades:
 * - Almacenar embeddings de ejemplos de intenciones
 * - Realizar búsquedas por similitud
 * - Gestionar diferentes tipos de vector store (Chroma, In-memory)
 * - Proporcionar estadísticas y health checks
 */
@Service
public class VectorStoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    // Configuración desde application.yml
    @Value("${vector-store.type:in-memory}")
    private String vectorStoreType;
    
    @Value("${vector-store.chroma.url:http://localhost:8000}")
    private String chromaUrl;
    
    @Value("${vector-store.collection-name:intent-examples}")
    private String collectionName;
    
    @Value("${vector-store.embedding-dimension:1536}")
    private int embeddingDimension;
    
    @Value("${vector-store.max-results:10}")
    private int maxResults;
    
    @Value("${vector-store.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    // Almacenamiento en memoria para desarrollo/testing
    private final Map<String, EmbeddingDocument> inMemoryStore = new ConcurrentHashMap<>();
    private final Map<String, List<Float>> inMemoryEmbeddings = new ConcurrentHashMap<>();
    
    // Estadísticas
    private long totalDocuments = 0;
    private long totalSearches = 0;
    private long totalSearchTimeMs = 0;
    
    /**
     * Inicializa el vector store según la configuración.
     */
    public void initialize() {
        logger.info("Inicializando Vector Store Service...");
        logger.info("Tipo: {}, Colección: {}, Dimensión: {}", 
                   vectorStoreType, collectionName, embeddingDimension);
        
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        
        switch (type) {
            case IN_MEMORY:
                initializeInMemoryStore();
                break;
            case CHROMA:
                initializeChromaStore();
                break;
            default:
                logger.warn("Tipo de vector store no implementado: {}. Usando in-memory.", type);
                initializeInMemoryStore();
                break;
        }
        
        logger.info("Vector Store Service inicializado exitosamente");
    }
    
    /**
     * Inicializa el almacenamiento en memoria.
     */
    private void initializeInMemoryStore() {
        logger.info("Inicializando vector store en memoria");
        inMemoryStore.clear();
        inMemoryEmbeddings.clear();
        totalDocuments = 0;
    }
    
    /**
     * Inicializa el almacenamiento Chroma.
     */
    private void initializeChromaStore() {
        logger.info("Inicializando vector store Chroma en: {}", chromaUrl);
        // TODO: Implementar conexión a Chroma
        logger.warn("Conexión a Chroma no implementada aún. Usando in-memory como fallback.");
        initializeInMemoryStore();
    }
    
    /**
     * Añade un documento con embedding al vector store.
     */
    public void addDocument(EmbeddingDocument document) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("Documento no puede ser nulo y debe tener ID");
        }
        
        logger.debug("Añadiendo documento al vector store: {}", document.getId());
        
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        
        switch (type) {
            case IN_MEMORY:
                addDocumentToMemory(document);
                break;
            case CHROMA:
                addDocumentToChroma(document);
                break;
            default:
                addDocumentToMemory(document);
                break;
        }
        
        totalDocuments++;
        logger.debug("Documento añadido exitosamente. Total: {}", totalDocuments);
    }
    
    /**
     * Añade un documento al almacenamiento en memoria.
     */
    private void addDocumentToMemory(EmbeddingDocument document) {
        inMemoryStore.put(document.getId(), document);
        if (document.getEmbedding() != null) {
            inMemoryEmbeddings.put(document.getId(), document.getEmbedding());
        }
    }
    
    /**
     * Añade un documento a Chroma.
     */
    private void addDocumentToChroma(EmbeddingDocument document) {
        // TODO: Implementar añadir a Chroma
        logger.warn("Añadir a Chroma no implementado. Usando in-memory como fallback.");
        addDocumentToMemory(document);
    }
    
    /**
     * Busca documentos similares por embedding.
     */
    public SearchResult searchSimilar(String query, List<Float> queryEmbedding, int limit) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            logger.warn("Embedding de consulta vacío");
            return new SearchResult(List.of(), query);
        }
        
        long startTime = System.currentTimeMillis();
        logger.debug("Buscando documentos similares para: '{}'", query);
        
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        SearchResult result;
        
        switch (type) {
            case IN_MEMORY:
                result = searchInMemory(query, queryEmbedding, limit);
                break;
            case CHROMA:
                result = searchInChroma(query, queryEmbedding, limit);
                break;
            default:
                result = searchInMemory(query, queryEmbedding, limit);
                break;
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        result.setSearchTimeMs(searchTime);
        result.setVectorStoreType(vectorStoreType);
        
        totalSearches++;
        totalSearchTimeMs += searchTime;
        
        logger.debug("Búsqueda completada en {}ms. Resultados: {}", 
                   searchTime, result.getTotalResults());
        
        return result;
    }
    
    /**
     * Busca en el almacenamiento en memoria.
     */
    private SearchResult searchInMemory(String query, List<Float> queryEmbedding, int limit) {
        List<EmbeddingDocument> results = new ArrayList<>();
        
        for (Map.Entry<String, List<Float>> entry : inMemoryEmbeddings.entrySet()) {
            String docId = entry.getKey();
            List<Float> docEmbedding = entry.getValue();
            
            if (docEmbedding != null && docEmbedding.size() == queryEmbedding.size()) {
                double similarity = calculateCosineSimilarity(queryEmbedding, docEmbedding);
                
                if (similarity >= similarityThreshold) {
                    EmbeddingDocument doc = inMemoryStore.get(docId);
                    if (doc != null) {
                        doc.setSimilarity(similarity);
                        results.add(doc);
                    }
                }
            }
        }
        
        // Ordenar por similitud descendente y limitar resultados
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        results = results.stream().limit(limit).collect(Collectors.toList());
        
        return new SearchResult(results, query);
    }
    
    /**
     * Busca en Chroma.
     */
    private SearchResult searchInChroma(String query, List<Float> queryEmbedding, int limit) {
        // TODO: Implementar búsqueda en Chroma
        logger.warn("Búsqueda en Chroma no implementada. Usando in-memory como fallback.");
        return searchInMemory(query, queryEmbedding, limit);
    }
    
    /**
     * Calcula la similitud coseno entre dos vectores.
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
     * Obtiene un documento por ID.
     */
    public Optional<EmbeddingDocument> getDocument(String id) {
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        
        switch (type) {
            case IN_MEMORY:
                return Optional.ofNullable(inMemoryStore.get(id));
            case CHROMA:
                return getDocumentFromChroma(id);
            default:
                return Optional.ofNullable(inMemoryStore.get(id));
        }
    }
    
    /**
     * Obtiene un documento de Chroma.
     */
    private Optional<EmbeddingDocument> getDocumentFromChroma(String id) {
        // TODO: Implementar obtener de Chroma
        logger.warn("Obtener de Chroma no implementado. Usando in-memory como fallback.");
        return Optional.ofNullable(inMemoryStore.get(id));
    }
    
    /**
     * Elimina un documento por ID.
     */
    public boolean deleteDocument(String id) {
        logger.debug("Eliminando documento: {}", id);
        
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        boolean deleted = false;
        
        switch (type) {
            case IN_MEMORY:
                deleted = inMemoryStore.remove(id) != null;
                inMemoryEmbeddings.remove(id);
                break;
            case CHROMA:
                deleted = deleteDocumentFromChroma(id);
                break;
            default:
                deleted = inMemoryStore.remove(id) != null;
                inMemoryEmbeddings.remove(id);
                break;
        }
        
        if (deleted) {
            totalDocuments--;
            logger.debug("Documento eliminado: {}", id);
        }
        
        return deleted;
    }
    
    /**
     * Elimina un documento de Chroma.
     */
    private boolean deleteDocumentFromChroma(String id) {
        // TODO: Implementar eliminar de Chroma
        logger.warn("Eliminar de Chroma no implementado. Usando in-memory como fallback.");
        boolean deleted = inMemoryStore.remove(id) != null;
        inMemoryEmbeddings.remove(id);
        return deleted;
    }
    
    /**
     * Obtiene estadísticas del vector store.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("type", vectorStoreType);
        stats.put("collectionName", collectionName);
        stats.put("embeddingDimension", embeddingDimension);
        stats.put("totalDocuments", totalDocuments);
        stats.put("totalSearches", totalSearches);
        stats.put("averageSearchTimeMs", totalSearches > 0 ? totalSearchTimeMs / totalSearches : 0);
        stats.put("similarityThreshold", similarityThreshold);
        stats.put("maxResults", maxResults);
        
        // Estadísticas específicas por tipo
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        switch (type) {
            case IN_MEMORY:
                stats.put("inMemoryDocuments", inMemoryStore.size());
                stats.put("inMemoryEmbeddings", inMemoryEmbeddings.size());
                break;
            case CHROMA:
                stats.put("chromaUrl", chromaUrl);
                stats.put("chromaConnected", false); // TODO: Implementar health check
                break;
        }
        
        return stats;
    }
    
    /**
     * Verifica la salud del vector store.
     */
    public boolean isHealthy() {
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        
        switch (type) {
            case IN_MEMORY:
                return true; // Siempre saludable
            case CHROMA:
                return isChromaHealthy();
            default:
                return true;
        }
    }
    
    /**
     * Verifica la salud de Chroma.
     */
    private boolean isChromaHealthy() {
        // TODO: Implementar health check de Chroma
        logger.warn("Health check de Chroma no implementado");
        return false;
    }
    
    /**
     * Limpia todos los documentos (solo para testing).
     */
    public void clearAll() {
        logger.warn("Limpiando todos los documentos del vector store");
        
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        
        switch (type) {
            case IN_MEMORY:
                inMemoryStore.clear();
                inMemoryEmbeddings.clear();
                break;
            case CHROMA:
                clearChroma();
                break;
            default:
                inMemoryStore.clear();
                inMemoryEmbeddings.clear();
                break;
        }
        
        totalDocuments = 0;
        logger.info("Vector store limpiado. Total documentos: 0");
    }
    
    /**
     * Limpia Chroma.
     */
    private void clearChroma() {
        // TODO: Implementar limpiar Chroma
        logger.warn("Limpiar Chroma no implementado. Usando in-memory como fallback.");
        inMemoryStore.clear();
        inMemoryEmbeddings.clear();
    }
} 