package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Clasificador de intenciones basado en JSON estático y análisis semántico directo.
 * Reemplaza el sistema RAG con un enfoque más directo y eficiente.
 * 
 * Flujo de trabajo:
 * 1. Carga intents desde intents.json
 * 2. Analiza texto del usuario con técnicas NLP
 * 3. Compara con ejemplos de intents usando similitud semántica
 * 4. Calcula scores de confianza multimodal
 * 5. Aplica thresholds y fallbacks inteligentes
 */
@Service
public class JsonIntentClassifier {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonIntentClassifier.class);
    
    @Autowired
    private IntentConfigManager intentConfigManager;
    
    @Autowired
    private SlotExtractor slotExtractor;
    
    @Value("${intent.classifier.default-confidence-threshold:0.7}")
    private double defaultConfidenceThreshold;
    
    @Value("${intent.classifier.similarity-threshold:0.6}")
    private double similarityThreshold;
    
    @Value("${intent.classifier.enable-fuzzy-matching:true}")
    private boolean enableFuzzyMatching;
    
    @Value("${intent.classifier.enable-contextual-boost:true}")
    private boolean enableContextualBoost;
    
    @Value("${intent.classifier.contextual-boost-factor:0.15}")
    private double contextualBoostFactor;
    
    @Value("${intent.classifier.fallback-intent:ayuda}")
    private String fallbackIntent;
    
    @Value("${intent.classifier.max-processing-time-ms:5000}")
    private long maxProcessingTimeMs;

    /**
     * Clasifica la intención de un texto usando análisis JSON directo
     */
    public IntentClassificationResult classifyIntent(IntentClassificationRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Iniciando clasificación JSON para texto: '{}'", request.getText());
            
            // Validar entrada
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("El texto de entrada no puede estar vacío");
            }
            
            String userText = request.getText().toLowerCase().trim();
            
            // Obtener todos los intents disponibles
            Map<String, IntentExample> availableIntents = intentConfigManager.getAllIntents();
            if (availableIntents.isEmpty()) {
                logger.warn("No hay intents disponibles para clasificación");
                return createFallbackResult(request, "No hay intents configurados", startTime);
            }
            
            logger.debug("Analizando contra {} intents disponibles", availableIntents.size());
            
            // Calcular similitudes para cada intent
            List<IntentMatch> intentMatches = new ArrayList<>();
            
            for (Map.Entry<String, IntentExample> entry : availableIntents.entrySet()) {
                String intentId = entry.getKey();
                IntentExample intentExample = entry.getValue();
                
                double similarity = calculateIntentSimilarity(userText, intentExample);
                
                if (similarity >= similarityThreshold) {
                    IntentMatch match = new IntentMatch(intentId, intentExample, similarity);
                    intentMatches.add(match);
                }
            }
            
            // Ordenar por similitud descendente
            intentMatches.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
            
            logger.debug("Encontrados {} matches con similitud >= {}", intentMatches.size(), similarityThreshold);
            
            // Seleccionar el mejor match
            IntentMatch bestMatch = null;
            if (!intentMatches.isEmpty()) {
                bestMatch = intentMatches.get(0);
                
                // Aplicar boost contextual si está habilitado
                if (enableContextualBoost && request.getContextMetadata() != null) {
                    double contextBoost = calculateContextualBoost(bestMatch.getIntentId(), request.getContextMetadata());
                    bestMatch.setConfidence(Math.min(1.0, bestMatch.getConfidence() + contextBoost));
                    logger.debug("Boost contextual aplicado a {}: +{} -> {}", 
                               bestMatch.getIntentId(), contextBoost, bestMatch.getConfidence());
                }
            }
            
            // Verificar si el mejor match supera el threshold
            double confidenceThreshold = request.getConfidenceThreshold() != null ? 
                request.getConfidenceThreshold() : defaultConfidenceThreshold;
                
            if (bestMatch == null || bestMatch.getConfidence() < confidenceThreshold) {
                logger.info("Ningún intent supera el threshold de confianza {}. Mejor match: {} ({})", 
                           confidenceThreshold, 
                           bestMatch != null ? bestMatch.getIntentId() : "ninguno",
                           bestMatch != null ? bestMatch.getConfidence() : 0.0);
                return createFallbackResult(request, "Confidence insuficiente", startTime);
            }
            
            // Extraer entidades usando el SlotExtractor mejorado
            Map<String, Object> extractedEntities = extractEntitiesForIntent(
                request.getText(), 
                bestMatch.getIntentId(),
                bestMatch.getIntentExample(), 
                request.getContextMetadata()
            );
            
            // Crear resultado exitoso
            IntentClassificationResult result = new IntentClassificationResult();
            result.setSuccess(true);
            result.setIntentId(bestMatch.getIntentId());
            result.setConfidenceScore(bestMatch.getConfidence());
            result.setDetectedEntities(extractedEntities);
            result.setMcpAction(bestMatch.getIntentExample().getMcpAction());
            result.setExpertDomain(bestMatch.getIntentExample().getExpertDomain());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            // Añadir metadata de clasificación
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("classification_method", "json_direct");
            metadata.put("matches_found", intentMatches.size());
            metadata.put("similarity_threshold_used", similarityThreshold);
            metadata.put("confidence_threshold_used", confidenceThreshold);
            metadata.put("contextual_boost_applied", enableContextualBoost);
            
            if (request.getContextMetadata() != null) {
                metadata.putAll(request.getContextMetadata());
            }
            result.setMetadata(metadata);
            
            logger.info("Clasificación JSON completada exitosamente: {} (confidence: {}) en {}ms", 
                       result.getIntentId(), result.getConfidenceScore(), result.getProcessingTimeMs());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error durante la clasificación JSON: {}", e.getMessage(), e);
            return createErrorResult(request, e, startTime);
        }
    }
    
    /**
     * Calcula la similitud semántica entre el texto del usuario y un intent
     */
    private double calculateIntentSimilarity(String userText, IntentExample intentExample) {
        if (intentExample.getExamples() == null || intentExample.getExamples().isEmpty()) {
            return 0.0;
        }
        
        double maxSimilarity = 0.0;
        
        // Comparar con cada ejemplo del intent
        for (String example : intentExample.getExamples()) {
            double similarity = calculateTextSimilarity(userText, example.toLowerCase().trim());
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }
        
        return maxSimilarity;
    }
    
    /**
     * Calcula similitud entre dos textos usando múltiples técnicas
     */
    private double calculateTextSimilarity(String text1, String text2) {
        // Técnica 1: Similitud exacta (weight: 0.4)
        double exactMatch = text1.equals(text2) ? 1.0 : 0.0;
        
        // Técnica 2: Similitud de contenido (weight: 0.3)
        double containsMatch = 0.0;
        if (text1.contains(text2) || text2.contains(text1)) {
            containsMatch = 0.8;
        }
        
        // Técnica 3: Similitud de palabras clave (weight: 0.2)
        double keywordMatch = calculateKeywordSimilarity(text1, text2);
        
        // Técnica 4: Similitud de Levenshtein normalizada (weight: 0.1)
        double levenshteinMatch = calculateLevenshteinSimilarity(text1, text2);
        
        // Combinar similitudes con pesos
        double combinedSimilarity = (exactMatch * 0.4) + 
                                   (containsMatch * 0.3) + 
                                   (keywordMatch * 0.2) + 
                                   (levenshteinMatch * 0.1);
        
        return Math.min(1.0, combinedSimilarity);
    }
    
    /**
     * Calcula similitud basada en palabras clave compartidas
     */
    private double calculateKeywordSimilarity(String text1, String text2) {
        // Palabras de parada en español
        Set<String> stopWords = Set.of("el", "la", "los", "las", "un", "una", "de", "del", "en", "con", 
                                      "por", "para", "a", "al", "y", "o", "que", "es", "se", "te", "me",
                                      "mi", "tu", "su", "le", "lo", "si", "no", "como", "cuando", "donde");
        
        Set<String> words1 = Arrays.stream(text1.split("\\s+"))
                                  .filter(word -> !stopWords.contains(word) && word.length() > 2)
                                  .collect(Collectors.toSet());
        
        Set<String> words2 = Arrays.stream(text2.split("\\s+"))
                                  .filter(word -> !stopWords.contains(word) && word.length() > 2)
                                  .collect(Collectors.toSet());
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        // Calcular intersección
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        // Calcular unión
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * Calcula similitud de Levenshtein normalizada
     */
    private double calculateLevenshteinSimilarity(String text1, String text2) {
        int distance = levenshteinDistance(text1, text2);
        int maxLength = Math.max(text1.length(), text2.length());
        
        if (maxLength == 0) return 1.0;
        
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Calcula la distancia de Levenshtein entre dos strings
     */
    private int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1),
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }
        
        return dp[str1.length()][str2.length()];
    }
    
    /**
     * Calcula boost contextual basado en metadata de la conversación
     */
    private double calculateContextualBoost(String intentId, Map<String, Object> contextMetadata) {
        double boost = 0.0;
        
        // Boost por historial de intents
        Object intentHistory = contextMetadata.get("intent_history");
        if (intentHistory instanceof List) {
            List<?> history = (List<?>) intentHistory;
            if (history.contains(intentId)) {
                boost += contextualBoostFactor * 0.5; // Boost por repetición
            }
        }
        
        // Boost por contexto de dispositivo
        Object deviceContext = contextMetadata.get("device_context");
        if (deviceContext != null && isDeviceRelatedIntent(intentId)) {
            boost += contextualBoostFactor * 0.3;
        }
        
        // Boost por contexto temporal
        Object temporalContext = contextMetadata.get("temporal_context");
        if (temporalContext != null && isTimeRelatedIntent(intentId)) {
            boost += contextualBoostFactor * 0.2;
        }
        
        return boost;
    }
    
    /**
     * Verifica si un intent está relacionado con dispositivos
     */
    private boolean isDeviceRelatedIntent(String intentId) {
        return intentId.contains("luz") || intentId.contains("encender") || 
               intentId.contains("apagar") || intentId.contains("musica");
    }
    
    /**
     * Verifica si un intent está relacionado con tiempo
     */
    private boolean isTimeRelatedIntent(String intentId) {
        return intentId.contains("tiempo") || intentId.contains("alarma") || 
               intentId.contains("recordatorio");
    }
    
    /**
     * Extrae entidades específicas para un intent usando el SlotExtractor
     */
    private Map<String, Object> extractEntitiesForIntent(String userText, 
                                                        String intentId,
                                                        IntentExample intentExample, 
                                                        Map<String, Object> contextMetadata) {
        try {
            // Combinar entidades requeridas y opcionales
            List<String> targetEntities = new ArrayList<>();
            if (intentExample.getRequiredEntities() != null) {
                targetEntities.addAll(intentExample.getRequiredEntities());
            }
            if (intentExample.getOptionalEntities() != null) {
                targetEntities.addAll(intentExample.getOptionalEntities());
            }
            
            if (targetEntities.isEmpty()) {
                return new HashMap<>();
            }
            
            // Usar SlotExtractor mejorado
            SlotExtractor.ExtractionResult extractionResult = slotExtractor.extractSlots(
                intentId, 
                userText, 
                targetEntities, 
                contextMetadata
            );
            
            return extractionResult.getExtractedSlots() != null ? 
                   extractionResult.getExtractedSlots() : new HashMap<>();
                   
        } catch (Exception e) {
            logger.warn("Error extrayendo entidades para intent {}: {}", 
                       intentId, e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Crea resultado de fallback cuando no se encuentra intent adecuado
     */
    private IntentClassificationResult createFallbackResult(IntentClassificationRequest request, 
                                                           String reason, 
                                                           long startTime) {
        IntentClassificationResult result = new IntentClassificationResult();
        result.setSuccess(true);
        result.setIntentId(fallbackIntent);
        result.setConfidenceScore(0.5);
        result.setDetectedEntities(new HashMap<>());
        result.setFallbackUsed(true);
        result.setFallbackReason(reason);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        // Obtener configuración del intent de fallback
        IntentExample fallbackExample = intentConfigManager.getIntent(fallbackIntent);
        if (fallbackExample != null) {
            result.setMcpAction(fallbackExample.getMcpAction());
            result.setExpertDomain(fallbackExample.getExpertDomain());
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("classification_method", "json_fallback");
        metadata.put("fallback_reason", reason);
        if (request.getContextMetadata() != null) {
            metadata.putAll(request.getContextMetadata());
        }
        result.setMetadata(metadata);
        
        logger.info("Aplicado fallback '{}': {}", fallbackIntent, reason);
        
        return result;
    }
    
    /**
     * Crea resultado de error
     */
    private IntentClassificationResult createErrorResult(IntentClassificationRequest request, 
                                                        Exception error, 
                                                        long startTime) {
        IntentClassificationResult result = new IntentClassificationResult();
        result.setSuccess(false);
        result.setIntentId(fallbackIntent);
        result.setConfidenceScore(0.1);
        result.setDetectedEntities(new HashMap<>());
        result.setFallbackUsed(true);
        result.setFallbackReason("Error de clasificación: " + error.getMessage());
        result.setErrorMessage(error.getMessage());
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("classification_method", "json_error");
        metadata.put("error_message", error.getMessage());
        result.setMetadata(metadata);
        
        return result;
    }
    
    /**
     * Métodos de conveniencia para clasificación simple
     */
    public IntentClassificationResult classifyText(String text) {
        IntentClassificationRequest request = new IntentClassificationRequest(text);
        return classifyIntent(request);
    }
    
    public IntentClassificationResult classifyText(String text, String sessionId) {
        IntentClassificationRequest request = new IntentClassificationRequest(text, sessionId);
        return classifyIntent(request);
    }
    
    /**
     * Obtiene estadísticas del clasificador
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("classifier_type", "json_direct");
        stats.put("default_confidence_threshold", defaultConfidenceThreshold);
        stats.put("similarity_threshold", similarityThreshold);
        stats.put("enable_fuzzy_matching", enableFuzzyMatching);
        stats.put("enable_contextual_boost", enableContextualBoost);
        stats.put("contextual_boost_factor", contextualBoostFactor);
        stats.put("fallback_intent", fallbackIntent);
        stats.put("max_processing_time_ms", maxProcessingTimeMs);
        stats.put("available_intents", intentConfigManager.getAllIntents().size());
        stats.put("service_status", "active");
        return stats;
    }
    
    /**
     * Verifica el estado de salud del clasificador
     */
    public boolean isHealthy() {
        try {
            Map<String, IntentExample> intents = intentConfigManager.getAllIntents();
            return intents != null && !intents.isEmpty();
        } catch (Exception e) {
            logger.error("Error en health check de JsonIntentClassifier: {}", e.getMessage());
            return false;
        }
    }
    
    // Clase interna para matches de intents
    private static class IntentMatch {
        private String intentId;
        private IntentExample intentExample;
        private double confidence;
        
        public IntentMatch(String intentId, IntentExample intentExample, double confidence) {
            this.intentId = intentId;
            this.intentExample = intentExample;
            this.confidence = confidence;
        }
        
        public String getIntentId() { return intentId; }
        public IntentExample getIntentExample() { return intentExample; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}