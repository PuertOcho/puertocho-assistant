package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.EmbeddingDocument;
import com.intentmanagerms.domain.model.VectorStoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de inicialización que configura automáticamente el vector store
 * al arrancar la aplicación.
 */
@Service
public class VectorStoreInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreInitializationService.class);
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    // Configuración desde variables de entorno
    @Value("${vector-store.type:in-memory}")
    private String vectorStoreType;
    
    @Value("${vector-store.initialize-with-examples:true}")
    private boolean initializeWithExamples;
    
    @Value("${vector-store.example-count:89}")
    private int exampleCount;
    
    /**
     * Se ejecuta cuando la aplicación está lista para recibir requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeVectorStore() {
        logger.info("Inicializando Vector Store...");
        
        try {
            // Inicializar el vector store
            vectorStoreService.initialize();
            
            // Si está habilitado, cargar ejemplos de prueba
            if (initializeWithExamples) {
                loadExampleDocuments();
            }
            
            // Mostrar estadísticas finales
            showVectorStoreStatistics();
            
            logger.info("Vector Store inicializado exitosamente");
            
        } catch (Exception e) {
            logger.error("Error al inicializar Vector Store", e);
            throw new RuntimeException("No se pudo inicializar el Vector Store", e);
        }
    }
    
    /**
     * Carga documentos de ejemplo para testing.
     */
    private void loadExampleDocuments() {
        logger.info("Cargando {} documentos de ejemplo...", exampleCount);
        
        // Ejemplos de intenciones para testing
        String[][] examples = {
            {"consultar_tiempo", "¿qué tiempo hace hoy?", "Consulta información meteorológica"},
            {"consultar_tiempo", "dime el clima", "Consulta información meteorológica"},
            {"consultar_tiempo", "cómo está el tiempo", "Consulta información meteorológica"},
            {"encender_luz", "enciende la luz", "Controla iluminación del hogar"},
            {"encender_luz", "prende la lámpara", "Controla iluminación del hogar"},
            {"encender_luz", "ilumina la habitación", "Controla iluminación del hogar"},
            {"programar_alarma", "programa una alarma", "Configura alarmas del sistema"},
            {"programar_alarma", "pon una alarma", "Configura alarmas del sistema"},
            {"programar_alarma", "despiértame mañana", "Configura alarmas del sistema"},
            {"reproducir_musica", "pon música", "Reproduce contenido multimedia"},
            {"reproducir_musica", "reproduce una canción", "Reproduce contenido multimedia"},
            {"reproducir_musica", "dame música relajante", "Reproduce contenido multimedia"}
        };
        
        int loaded = 0;
        for (String[] example : examples) {
            if (loaded >= exampleCount) break;
            
            String intent = example[0];
            String content = example[1];
            String description = example[2];
            
            // Crear embedding simulado (en producción vendría del LLM)
            List<Float> embedding = generateMockEmbedding(content);
            
            // Crear documento
            EmbeddingDocument document = new EmbeddingDocument(
                UUID.randomUUID().toString(),
                content,
                intent,
                embedding
            );
            
            // Añadir metadata
            document.setMetadata(Map.of(
                "description", description,
                "example_type", "test",
                "language", "es"
            ));
            
            // Añadir al vector store
            vectorStoreService.addDocument(document);
            loaded++;
            
            logger.debug("Documento de ejemplo cargado: {} - {}", intent, content);
        }
        
        logger.info("{} documentos de ejemplo cargados exitosamente", loaded);
    }
    
    /**
     * Genera un embedding simulado para testing.
     * En producción, esto vendría del servicio de embeddings del LLM.
     */
    private List<Float> generateMockEmbedding(String content) {
        // Simular un embedding de 1536 dimensiones (como OpenAI text-embedding-ada-002)
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
    
    /**
     * Muestra estadísticas del vector store.
     */
    private void showVectorStoreStatistics() {
        var stats = vectorStoreService.getStatistics();
        
        logger.info("=== Estadísticas del Vector Store ===");
        logger.info("Tipo: {}", stats.get("type"));
        logger.info("Colección: {}", stats.get("collectionName"));
        logger.info("Dimensión de embedding: {}", stats.get("embeddingDimension"));
        logger.info("Documentos totales: {}", stats.get("totalDocuments"));
        logger.info("Umbral de similitud: {}", stats.get("similarityThreshold"));
        logger.info("Máximo resultados: {}", stats.get("maxResults"));
        
        // Estadísticas específicas por tipo
        VectorStoreType type = VectorStoreType.fromCode(vectorStoreType);
        switch (type) {
            case IN_MEMORY:
                logger.info("Documentos en memoria: {}", stats.get("inMemoryDocuments"));
                logger.info("Embeddings en memoria: {}", stats.get("inMemoryEmbeddings"));
                break;
            case CHROMA:
                logger.info("URL de Chroma: {}", stats.get("chromaUrl"));
                logger.info("Conexión Chroma: {}", stats.get("chromaConnected"));
                break;
        }
        
        // Verificar salud
        boolean isHealthy = vectorStoreService.isHealthy();
        logger.info("Estado de salud: {}", isHealthy ? "✅ SALUDABLE" : "❌ NO SALUDABLE");
        
        if (!isHealthy) {
            logger.warn("⚠️  ADVERTENCIA: Vector Store no está saludable. Algunas funcionalidades pueden no funcionar correctamente.");
        }
    }
    
    /**
     * Verifica la funcionalidad del vector store con una búsqueda de prueba.
     */
    public void testVectorStoreFunctionality() {
        logger.info("Probando funcionalidad del Vector Store...");
        
        try {
            // Generar embedding de prueba
            String testQuery = "¿qué tiempo hace?";
            List<Float> testEmbedding = generateMockEmbedding(testQuery);
            
            // Realizar búsqueda
            var searchResult = vectorStoreService.searchSimilar(testQuery, testEmbedding, 3);
            
            logger.info("Búsqueda de prueba completada:");
            logger.info("- Consulta: {}", searchResult.getQuery());
            logger.info("- Resultados encontrados: {}", searchResult.getTotalResults());
            logger.info("- Tiempo de búsqueda: {}ms", searchResult.getSearchTimeMs());
            logger.info("- Rango de similitud: {}-{}", 
                       searchResult.getMinSimilarity(), searchResult.getMaxSimilarity());
            
            // Mostrar mejores resultados
            if (searchResult.getBestMatch() != null) {
                var bestMatch = searchResult.getBestMatch();
                logger.info("- Mejor coincidencia: {} (similitud: {})", 
                           bestMatch.getContent(), bestMatch.getSimilarity());
            }
            
            logger.info("✅ Funcionalidad del Vector Store verificada correctamente");
            
        } catch (Exception e) {
            logger.error("❌ Error al probar funcionalidad del Vector Store", e);
        }
    }
} 