package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.VectorStoreService;
import com.intentmanagerms.domain.model.EmbeddingDocument;
import com.intentmanagerms.domain.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para gestionar el vector store.
 * Proporciona endpoints para consultar, añadir, buscar y eliminar documentos con embeddings.
 */
@RestController
@RequestMapping("/api/v1/vector-store")
public class VectorStoreController {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreController.class);
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    /**
     * Obtiene estadísticas del vector store.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.info("GET /api/v1/vector-store/statistics - Obteniendo estadísticas");
        
        Map<String, Object> stats = vectorStoreService.getStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Verifica la salud del vector store.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        logger.info("GET /api/v1/vector-store/health - Verificando salud");
        
        boolean isHealthy = vectorStoreService.isHealthy();
        
        Map<String, Object> healthInfo = Map.of(
            "healthy", isHealthy,
            "timestamp", System.currentTimeMillis(),
            "status", isHealthy ? "OK" : "ERROR"
        );
        
        return ResponseEntity.ok(healthInfo);
    }
    
    /**
     * Añade un documento con embedding al vector store.
     */
    @PostMapping("/documents")
    public ResponseEntity<EmbeddingDocument> addDocument(@RequestBody EmbeddingDocument document) {
        logger.info("POST /api/v1/vector-store/documents - Añadiendo documento: {}", document.getId());
        
        try {
            vectorStoreService.addDocument(document);
            return ResponseEntity.ok(document);
        } catch (IllegalArgumentException e) {
            logger.error("Error al añadir documento: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error inesperado al añadir documento", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtiene un documento por ID.
     */
    @GetMapping("/documents/{id}")
    public ResponseEntity<EmbeddingDocument> getDocument(@PathVariable String id) {
        logger.info("GET /api/v1/vector-store/documents/{} - Obteniendo documento", id);
        
        Optional<EmbeddingDocument> document = vectorStoreService.getDocument(id);
        
        if (document.isPresent()) {
            return ResponseEntity.ok(document.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Elimina un documento por ID.
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        logger.info("DELETE /api/v1/vector-store/documents/{} - Eliminando documento", id);
        
        boolean deleted = vectorStoreService.deleteDocument(id);
        
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Busca documentos similares por embedding.
     */
    @PostMapping("/search")
    public ResponseEntity<SearchResult> searchSimilar(
            @RequestParam String query,
            @RequestBody List<Float> embedding,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("POST /api/v1/vector-store/search - Buscando documentos similares para: '{}'", query);
        
        try {
            SearchResult result = vectorStoreService.searchSimilar(query, embedding, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error al buscar documentos similares", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Busca documentos similares por texto (requiere generar embedding).
     */
    @PostMapping("/search/text")
    public ResponseEntity<SearchResult> searchByText(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("POST /api/v1/vector-store/search/text - Buscando por texto: '{}'", query);
        
        try {
            // TODO: Generar embedding del texto usando el servicio de embeddings
            // Por ahora, usamos un embedding simulado
            List<Float> embedding = generateMockEmbedding(query);
            
            SearchResult result = vectorStoreService.searchSimilar(query, embedding, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error al buscar por texto", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Limpia todos los documentos (solo para testing).
     */
    @DeleteMapping("/documents")
    public ResponseEntity<Void> clearAll() {
        logger.warn("DELETE /api/v1/vector-store/documents - Limpiando todos los documentos");
        
        try {
            vectorStoreService.clearAll();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error al limpiar documentos", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtiene información sobre el tipo de vector store configurado.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        logger.info("GET /api/v1/vector-store/info - Obteniendo información");
        
        var stats = vectorStoreService.getStatistics();
        
        Map<String, Object> info = Map.of(
            "type", stats.get("type"),
            "collectionName", stats.get("collectionName"),
            "embeddingDimension", stats.get("embeddingDimension"),
            "totalDocuments", stats.get("totalDocuments"),
            "similarityThreshold", stats.get("similarityThreshold"),
            "maxResults", stats.get("maxResults"),
            "healthy", vectorStoreService.isHealthy()
        );
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Genera un embedding simulado para testing.
     * En producción, esto vendría del servicio de embeddings del LLM.
     */
    private List<Float> generateMockEmbedding(String content) {
        List<Float> embedding = new java.util.ArrayList<>();
        
        // Usar el hash del contenido para generar valores pseudo-aleatorios consistentes
        int hash = content.hashCode();
        
        for (int i = 0; i < 1536; i++) {
            // Generar valor entre -1 y 1 usando el hash
            float value = (float) Math.sin(hash + i * 0.1) * 0.5f;
            embedding.add(value);
        }
        
        return embedding;
    }
} 