package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Motor principal de clasificación de intenciones usando RAG (Retrieval Augmented Generation).
 * 
 * Flujo de trabajo:
 * 1. Genera embedding del texto de entrada
 * 2. Busca ejemplos similares en el vector store
 * 3. Construye prompt contextual con ejemplos encontrados
 * 4. Clasifica la intención usando LLM con few-shot learning
 * 5. Calcula confidence score usando múltiples métricas
 * 6. Aplica fallback inteligente si es necesario
 */
@Service
public class RagIntentClassifier {
    
    private static final Logger logger = LoggerFactory.getLogger(RagIntentClassifier.class);
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Autowired
    private IntentConfigManager intentConfigManager;
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    @Value("${rag.classifier.default-max-examples:5}")
    private int defaultMaxExamples;
    
    @Value("${rag.classifier.default-confidence-threshold:0.7}")
    private double defaultConfidenceThreshold;
    
    @Value("${rag.classifier.similarity-threshold:0.6}")
    private double similarityThreshold;
    
    @Value("${rag.classifier.enable-fallback:true}")
    private boolean enableFallback;
    
    @Value("${rag.classifier.fallback-confidence-threshold:0.5}")
    private double fallbackConfidenceThreshold;
    
    @Value("${rag.classifier.max-processing-time-ms:10000}")
    private long maxProcessingTimeMs;
    
    /**
     * Clasifica la intención de un texto usando RAG
     */
    public IntentClassificationResult classifyIntent(IntentClassificationRequest request) {
        long startTime = System.currentTimeMillis();
        IntentClassificationResult result = new IntentClassificationResult();
        
        try {
            logger.info("Iniciando clasificación RAG para texto: '{}'", request.getText());
            
            // Validar entrada
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("El texto de entrada no puede estar vacío");
            }
            
            // Configurar parámetros
            int maxExamples = request.getMaxExamplesForRag() != null ? 
                request.getMaxExamplesForRag() : defaultMaxExamples;
            double confidenceThreshold = request.getConfidenceThreshold() != null ? 
                request.getConfidenceThreshold() : defaultConfidenceThreshold;
            
            // Paso 1: Generar embedding del texto de entrada
            long embeddingStartTime = System.currentTimeMillis();
            List<Float> inputEmbedding = generateTextEmbedding(request.getText());
            long embeddingTime = System.currentTimeMillis() - embeddingStartTime;
            
            logger.debug("Embedding generado en {}ms", embeddingTime);
            
            // Paso 2: Buscar ejemplos similares en el vector store
            long searchStartTime = System.currentTimeMillis();
            SearchResult searchResult = vectorStoreService.searchSimilar(
                request.getText(), 
                inputEmbedding, 
                maxExamples
            );
            long searchTime = System.currentTimeMillis() - searchStartTime;
            result.setVectorSearchTimeMs(searchTime);
            
            logger.debug("Búsqueda vectorial completada en {}ms, {} resultados encontrados", 
                searchTime, searchResult.getTotalResults());
            
            // Paso 3: Filtrar ejemplos por umbral de similitud
            List<EmbeddingDocument> relevantExamples = filterExamplesBySimilarity(
                searchResult.getDocuments(), 
                similarityThreshold
            );
            
            if (relevantExamples.isEmpty()) {
                logger.warn("No se encontraron ejemplos relevantes con similitud >= {}", similarityThreshold);
                return handleNoRelevantExamples(request, result, startTime);
            }
            
            // Paso 4: Construir prompt contextual
            String contextualPrompt = buildContextualPrompt(request.getText(), relevantExamples);
            result.setPromptUsed(contextualPrompt);
            
            // Paso 5: Clasificar usando LLM
            long llmStartTime = System.currentTimeMillis();
            String llmResponse = classifyWithLlm(contextualPrompt);
            long llmTime = System.currentTimeMillis() - llmStartTime;
            result.setLlmInferenceTimeMs(llmTime);
            result.setLlmResponse(llmResponse);
            
            logger.debug("Clasificación LLM completada en {}ms", llmTime);
            
            // Paso 6: Parsear respuesta del LLM
            IntentClassificationResult parsedResult = parseLlmResponse(llmResponse, relevantExamples);
            
            // Paso 7: Calcular confidence score
            double confidenceScore = calculateConfidenceScore(parsedResult, relevantExamples);
            parsedResult.setConfidenceScore(confidenceScore);
            
            // Paso 8: Aplicar fallback si es necesario
            if (enableFallback && confidenceScore < confidenceThreshold) {
                logger.info("Confidence score {} por debajo del umbral {}, aplicando fallback", 
                    confidenceScore, confidenceThreshold);
                return applyFallback(request, result, startTime);
            }
            
            // Paso 9: Enriquecer resultado con metadata
            enrichResultWithMetadata(parsedResult, request, relevantExamples, startTime);
            
            logger.info("Clasificación RAG completada exitosamente: {} (confidence: {})", 
                parsedResult.getIntentId(), parsedResult.getConfidenceScore());
            
            return parsedResult;
            
        } catch (Exception e) {
            logger.error("Error durante la clasificación RAG: {}", e.getMessage(), e);
            return handleClassificationError(request, result, e, startTime);
        }
    }
    
    /**
     * Genera embedding para un texto usando el LLM primario
     */
    private List<Float> generateTextEmbedding(String text) {
        // TODO: Implementar generación real de embeddings usando OpenAI API
        // Por ahora usamos embeddings mock para testing
        return generateMockEmbedding(text);
    }
    
    /**
     * Genera embedding mock para testing
     */
    private List<Float> generateMockEmbedding(String text) {
        List<Float> embedding = new ArrayList<>();
        Random random = new Random(text.hashCode()); // Deterministico para testing
        
        for (int i = 0; i < 1536; i++) {
            embedding.add(random.nextFloat() * 2 - 1); // Valores entre -1 y 1
        }
        
        return embedding;
    }
    
    /**
     * Filtra ejemplos por umbral de similitud
     */
    private List<EmbeddingDocument> filterExamplesBySimilarity(
            List<EmbeddingDocument> documents, 
            double threshold) {
        return documents.stream()
                .filter(doc -> doc.getSimilarity() >= threshold)
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .collect(Collectors.toList());
    }
    
    /**
     * Construye prompt contextual con ejemplos RAG
     */
    private String buildContextualPrompt(String inputText, List<EmbeddingDocument> examples) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Eres un clasificador de intenciones experto. Tu tarea es clasificar la intención del usuario basándote en ejemplos similares.\n\n");
        
        prompt.append("EJEMPLOS DE ENTRENAMIENTO:\n");
        for (int i = 0; i < examples.size(); i++) {
            EmbeddingDocument example = examples.get(i);
            prompt.append(String.format("%d. Texto: \"%s\" → Intención: %s (similitud: %.3f)\n", 
                i + 1, example.getContent(), example.getIntent(), example.getSimilarity()));
        }
        
        prompt.append("\nTEXTO A CLASIFICAR:\n");
        prompt.append("\"").append(inputText).append("\"\n\n");
        
        prompt.append("INSTRUCCIONES:\n");
        prompt.append("1. Analiza el texto del usuario\n");
        prompt.append("2. Compara con los ejemplos proporcionados\n");
        prompt.append("3. Identifica la intención más probable\n");
        prompt.append("4. Extrae entidades relevantes si las hay\n");
        prompt.append("5. Responde en formato JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"intent_id\": \"nombre_de_la_intencion\",\n");
        prompt.append("  \"confidence\": 0.85,\n");
        prompt.append("  \"entities\": {\"entidad\": \"valor\"},\n");
        prompt.append("  \"reasoning\": \"explicación breve\"\n");
        prompt.append("}\n\n");
        
        prompt.append("RESPUESTA:");
        
        return prompt.toString();
    }
    
    /**
     * Clasifica usando LLM
     */
    private String classifyWithLlm(String prompt) {
        // TODO: Implementar llamada real al LLM
        // Por ahora simulamos una respuesta para testing
        return simulateLlmResponse(prompt);
    }
    
    /**
     * Simula respuesta del LLM para testing
     */
    private String simulateLlmResponse(String prompt) {
        // Simulación simple basada en palabras clave en el prompt
        if (prompt.contains("tiempo") || prompt.contains("clima")) {
            return "{\"intent_id\": \"consultar_tiempo\", \"confidence\": 0.92, \"entities\": {\"ubicacion\": \"Madrid\"}, \"reasoning\": \"Consulta sobre condiciones meteorológicas\"}";
        } else if (prompt.contains("luz") || prompt.contains("encender")) {
            return "{\"intent_id\": \"encender_luz\", \"confidence\": 0.88, \"entities\": {\"lugar\": \"salón\"}, \"reasoning\": \"Solicitud para encender iluminación\"}";
        } else if (prompt.contains("ayuda") || prompt.contains("ayúdame")) {
            return "{\"intent_id\": \"ayuda\", \"confidence\": 0.95, \"entities\": {}, \"reasoning\": \"Solicitud de ayuda general\"}";
        } else {
            return "{\"intent_id\": \"ayuda\", \"confidence\": 0.65, \"entities\": {}, \"reasoning\": \"Intención no clara, usando fallback\"}";
        }
    }
    
    /**
     * Parsea la respuesta del LLM
     */
    private IntentClassificationResult parseLlmResponse(String llmResponse, List<EmbeddingDocument> examples) {
        IntentClassificationResult result = new IntentClassificationResult();
        
        try {
            // TODO: Implementar parsing real de JSON
            // Por ahora extraemos información básica
            if (llmResponse.contains("\"intent_id\"")) {
                String intentId = extractJsonValue(llmResponse, "intent_id");
                String confidenceStr = extractJsonValue(llmResponse, "confidence");
                String reasoning = extractJsonValue(llmResponse, "reasoning");
                
                result.setIntentId(intentId);
                result.setConfidenceScore(Double.parseDouble(confidenceStr));
                
                // Obtener información de la intención desde la configuración
                IntentExample intentExample = intentConfigManager.getIntent(intentId);
                if (intentExample != null) {
                    result.setMcpAction(intentExample.getMcpAction());
                    result.setExpertDomain(intentExample.getExpertDomain());
                }
                
                // Convertir ejemplos a formato RAG
                List<IntentClassificationResult.RagExample> ragExamples = examples.stream()
                        .map(doc -> new IntentClassificationResult.RagExample(
                                doc.getContent(), 
                                doc.getIntent(), 
                                doc.getSimilarity()))
                        .collect(Collectors.toList());
                result.setRagExamplesUsed(ragExamples);
                
                // Extraer scores de similitud
                List<Double> similarityScores = examples.stream()
                        .map(EmbeddingDocument::getSimilarity)
                        .collect(Collectors.toList());
                result.setSimilarityScores(similarityScores);
                
            } else {
                throw new RuntimeException("Respuesta del LLM no contiene formato JSON válido");
            }
            
        } catch (Exception e) {
            logger.error("Error parseando respuesta del LLM: {}", e.getMessage());
            result.setIntentId("ayuda");
            result.setConfidenceScore(0.5);
            result.setFallbackUsed(true);
            result.setFallbackReason("Error parseando respuesta del LLM");
        }
        
        return result;
    }
    
    /**
     * Extrae valor de JSON simple
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        
        // Buscar valores numéricos
        pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
        p = java.util.regex.Pattern.compile(pattern);
        m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        
        return "";
    }
    
    /**
     * Calcula confidence score usando múltiples métricas
     */
    private double calculateConfidenceScore(IntentClassificationResult result, List<EmbeddingDocument> examples) {
        if (examples.isEmpty()) {
            return 0.0;
        }
        
        // Métrica 1: Confidence del LLM (40%)
        double llmConfidence = result.getConfidenceScore() != null ? result.getConfidenceScore() : 0.0;
        
        // Métrica 2: Similitud promedio de ejemplos (30%)
        double avgSimilarity = examples.stream()
                .mapToDouble(EmbeddingDocument::getSimilarity)
                .average()
                .orElse(0.0);
        
        // Métrica 3: Consistencia de intenciones en ejemplos (20%)
        double consistency = calculateIntentConsistency(examples, result.getIntentId());
        
        // Métrica 4: Cantidad de ejemplos relevantes (10%)
        double exampleCount = Math.min(examples.size() / 5.0, 1.0); // Normalizado a 0-1
        
        // Cálculo ponderado
        double finalConfidence = (llmConfidence * 0.4) + 
                                (avgSimilarity * 0.3) + 
                                (consistency * 0.2) + 
                                (exampleCount * 0.1);
        
        return Math.min(finalConfidence, 1.0);
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
     * Maneja caso sin ejemplos relevantes
     */
    private IntentClassificationResult handleNoRelevantExamples(
            IntentClassificationRequest request, 
            IntentClassificationResult result, 
            long startTime) {
        
        result.setIntentId("ayuda");
        result.setConfidenceScore(0.3);
        result.setFallbackUsed(true);
        result.setFallbackReason("No se encontraron ejemplos relevantes");
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setSuccess(true);
        
        return result;
    }
    
    /**
     * Aplica fallback cuando confidence es bajo
     */
    private IntentClassificationResult applyFallback(
            IntentClassificationRequest request, 
            IntentClassificationResult result, 
            long startTime) {
        
        result.setIntentId("ayuda");
        result.setConfidenceScore(fallbackConfidenceThreshold);
        result.setFallbackUsed(true);
        result.setFallbackReason("Confidence score por debajo del umbral");
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setSuccess(true);
        
        return result;
    }
    
    /**
     * Maneja errores durante la clasificación
     */
    private IntentClassificationResult handleClassificationError(
            IntentClassificationRequest request, 
            IntentClassificationResult result, 
            Exception error, 
            long startTime) {
        
        result.setIntentId("ayuda");
        result.setConfidenceScore(0.1);
        result.setFallbackUsed(true);
        result.setFallbackReason("Error durante clasificación: " + error.getMessage());
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setSuccess(false);
        result.setErrorMessage(error.getMessage());
        
        return result;
    }
    
    /**
     * Enriquece resultado con metadata adicional
     */
    private void enrichResultWithMetadata(
            IntentClassificationResult result, 
            IntentClassificationRequest request, 
            List<EmbeddingDocument> examples, 
            long startTime) {
        
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        // Añadir metadata contextual si está disponible
        if (request.getContextMetadata() != null) {
            result.setMetadata(request.getContextMetadata());
        }
        
        // Validar que no exceda tiempo máximo de procesamiento
        if (result.getProcessingTimeMs() > maxProcessingTimeMs) {
            logger.warn("Tiempo de procesamiento {}ms excede el límite de {}ms", 
                result.getProcessingTimeMs(), maxProcessingTimeMs);
        }
    }
    
    /**
     * Clasifica texto simple (método de conveniencia)
     */
    public IntentClassificationResult classifyText(String text) {
        IntentClassificationRequest request = new IntentClassificationRequest(text);
        return classifyIntent(request);
    }
    
    /**
     * Clasifica texto con session ID
     */
    public IntentClassificationResult classifyText(String text, String sessionId) {
        IntentClassificationRequest request = new IntentClassificationRequest(text, sessionId);
        return classifyIntent(request);
    }
} 