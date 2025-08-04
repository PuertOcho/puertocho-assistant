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
 * Servicio de fallback inteligente con degradación gradual para el motor RAG.
 * 
 * Implementa múltiples estrategias de fallback que se aplican secuencialmente:
 * 1. Fallback por similitud reducida (umbral más bajo)
 * 2. Fallback por dominio general (intenciones de ayuda)
 * 3. Fallback por palabras clave básicas
 * 4. Fallback por análisis de contexto
 * 5. Fallback por respuesta genérica
 * 
 * Cada nivel de degradación intenta mantener la funcionalidad mientras reduce la precisión.
 */
@Service
public class IntelligentFallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntelligentFallbackService.class);
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Autowired
    private IntentConfigManager intentConfigManager;
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    @Autowired
    private DynamicPromptEngineeringService promptEngineeringService;
    
    @Autowired
    private ConfidenceScoringService confidenceScoringService;
    
    // Configuración de niveles de fallback
    @Value("${rag.fallback.enable-gradual-degradation:true}")
    private boolean enableGradualDegradation;
    
    @Value("${rag.fallback.max-degradation-levels:5}")
    private int maxDegradationLevels;
    
    @Value("${rag.fallback.similarity-reduction-factor:0.2}")
    private double similarityReductionFactor;
    
    @Value("${rag.fallback.enable-keyword-fallback:true}")
    private boolean enableKeywordFallback;
    
    @Value("${rag.fallback.enable-context-fallback:true}")
    private boolean enableContextFallback;
    
    @Value("${rag.fallback.enable-general-domain-fallback:true}")
    private boolean enableGeneralDomainFallback;
    
    @Value("${rag.fallback.min-confidence-for-degradation:0.3}")
    private double minConfidenceForDegradation;
    
    @Value("${rag.fallback.max-processing-time-ms:5000}")
    private long maxProcessingTimeMs;
    
    // Palabras clave para fallback básico
    private static final Map<String, String> KEYWORD_INTENT_MAPPING = new HashMap<>();
    
    static {
        KEYWORD_INTENT_MAPPING.put("tiempo", "consultar_tiempo");
        KEYWORD_INTENT_MAPPING.put("clima", "consultar_tiempo");
        KEYWORD_INTENT_MAPPING.put("lluvia", "consultar_tiempo");
        KEYWORD_INTENT_MAPPING.put("temperatura", "consultar_tiempo");
        KEYWORD_INTENT_MAPPING.put("luz", "encender_luz");
        KEYWORD_INTENT_MAPPING.put("encender", "encender_luz");
        KEYWORD_INTENT_MAPPING.put("apagar", "apagar_luz");
        KEYWORD_INTENT_MAPPING.put("música", "reproducir_musica");
        KEYWORD_INTENT_MAPPING.put("canción", "reproducir_musica");
        KEYWORD_INTENT_MAPPING.put("alarma", "programar_alarma");
        KEYWORD_INTENT_MAPPING.put("recordatorio", "programar_alarma");
        KEYWORD_INTENT_MAPPING.put("ayuda", "ayuda");
        KEYWORD_INTENT_MAPPING.put("hola", "saludo");
        KEYWORD_INTENT_MAPPING.put("gracias", "agradecimiento");
    }
    
    /**
     * Aplica fallback inteligente con degradación gradual
     */
    public IntentClassificationResult applyIntelligentFallback(
            IntentClassificationRequest request,
            IntentClassificationResult originalResult,
            List<EmbeddingDocument> originalExamples,
            long startTime) {
        
        logger.info("Iniciando fallback inteligente para texto: '{}'", request.getText());
        
        if (!enableGradualDegradation) {
            return applyBasicFallback(request, originalResult, startTime);
        }
        
        // Nivel 1: Fallback por similitud reducida
        IntentClassificationResult result = applySimilarityReductionFallback(request, originalExamples, startTime);
        if (result.getConfidenceScore() >= minConfidenceForDegradation) {
            logger.info("Fallback por similitud reducida exitoso: {} (confidence: {})", 
                result.getIntentId(), result.getConfidenceScore());
            return result;
        }
        
        // Nivel 2: Fallback por dominio general
        result = applyGeneralDomainFallback(request, startTime);
        if (result.getConfidenceScore() >= minConfidenceForDegradation) {
            logger.info("Fallback por dominio general exitoso: {} (confidence: {})", 
                result.getIntentId(), result.getConfidenceScore());
            return result;
        }
        
        // Nivel 3: Fallback por palabras clave
        if (enableKeywordFallback) {
            result = applyKeywordFallback(request, startTime);
            if (result.getConfidenceScore() >= minConfidenceForDegradation) {
                logger.info("Fallback por palabras clave exitoso: {} (confidence: {})", 
                    result.getIntentId(), result.getConfidenceScore());
                return result;
            }
        }
        
        // Nivel 4: Fallback por análisis de contexto
        if (enableContextFallback) {
            result = applyContextAnalysisFallback(request, startTime);
            if (result.getConfidenceScore() >= minConfidenceForDegradation) {
                logger.info("Fallback por análisis de contexto exitoso: {} (confidence: {})", 
                    result.getIntentId(), result.getConfidenceScore());
                return result;
            }
        }
        
        // Nivel 5: Fallback genérico (último recurso)
        logger.warn("Aplicando fallback genérico como último recurso");
        return applyGenericFallback(request, startTime);
    }
    
    /**
     * Nivel 1: Fallback por similitud reducida
     * Reduce el umbral de similitud y busca ejemplos más amplios
     */
    private IntentClassificationResult applySimilarityReductionFallback(
            IntentClassificationRequest request,
            List<EmbeddingDocument> originalExamples,
            long startTime) {
        
        try {
            logger.debug("Aplicando fallback por similitud reducida");
            
            // Generar embedding del texto
            List<Float> inputEmbedding = generateTextEmbedding(request.getText());
            
            // Buscar con umbral reducido
            double reducedThreshold = Math.max(0.1, similarityReductionFactor);
            SearchResult searchResult = vectorStoreService.searchSimilar(
                request.getText(), 
                inputEmbedding, 
                request.getMaxExamplesForRag() != null ? request.getMaxExamplesForRag() : 10
            );
            
            // Filtrar con umbral reducido
            List<EmbeddingDocument> examples = searchResult.getDocuments().stream()
                .filter(doc -> doc.getSimilarity() >= reducedThreshold)
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(5)
                .collect(Collectors.toList());
            
            if (examples.isEmpty()) {
                return createFallbackResult("ayuda", 0.2, "No se encontraron ejemplos con similitud reducida", startTime);
            }
            
            // Construir prompt con ejemplos de similitud reducida
            String prompt = promptEngineeringService.buildPromptWithStrategy(
                request, examples, "adaptive"
            );
            
            // Clasificar con LLM
            String llmResponse = classifyWithLlm(prompt);
            IntentClassificationResult result = parseLlmResponse(llmResponse, examples);
            
            // Calcular confidence con penalización por similitud reducida
            double baseConfidence = confidenceScoringService.calculateConfidenceScore(result, examples);
            double penalizedConfidence = baseConfidence * 0.8; // Penalización del 20%
            
            result.setConfidenceScore(penalizedConfidence);
            result.setFallbackUsed(true);
            result.setFallbackReason("Fallback por similitud reducida (umbral: " + reducedThreshold + ")");
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error en fallback por similitud reducida: {}", e.getMessage());
            return createFallbackResult("ayuda", 0.1, "Error en fallback por similitud reducida", startTime);
        }
    }
    
    /**
     * Nivel 2: Fallback por dominio general
     * Busca intenciones de dominio general como "ayuda", "saludo", etc.
     */
    private IntentClassificationResult applyGeneralDomainFallback(
            IntentClassificationRequest request,
            long startTime) {
        
        try {
            logger.debug("Aplicando fallback por dominio general");
            
            // Obtener intenciones de dominio general
            Map<String, IntentExample> allIntents = intentConfigManager.getAllIntents();
            List<IntentExample> generalIntents = allIntents.values().stream()
                .filter(intent -> "general".equals(intent.getExpertDomain()))
                .collect(Collectors.toList());
            
            if (generalIntents.isEmpty()) {
                return createFallbackResult("ayuda", 0.3, "No se encontraron intenciones de dominio general", startTime);
            }
            
            // Analizar el texto para determinar la intención general más apropiada
            String text = request.getText().toLowerCase();
            String selectedIntent = null; // No hay intención por defecto
            
            if (text.contains("hola") || text.contains("buenos") || text.contains("buenas")) {
                selectedIntent = "saludo";
            } else if (text.contains("gracias") || text.contains("gracie")) {
                selectedIntent = "agradecimiento";
            } else if (text.contains("adiós") || text.contains("hasta") || text.contains("chao")) {
                selectedIntent = "despedida";
            } else if (text.contains("ayuda") || text.contains("ayúdame")) {
                selectedIntent = "ayuda";
            }
            
            // Si no se detectó ninguna intención de dominio general, fallar para continuar al siguiente nivel
            if (selectedIntent == null) {
                return createFallbackResult("ayuda", 0.1, "No se detectó intención de dominio general", startTime);
            }
            
            // Verificar que la intención existe
            if (!allIntents.containsKey(selectedIntent)) {
                selectedIntent = "ayuda";
            }
            
            IntentExample intent = allIntents.get(selectedIntent);
            double confidence = 0.4; // Confianza moderada para fallback de dominio general
            
            IntentClassificationResult result = new IntentClassificationResult();
            result.setIntentId(selectedIntent);
            result.setConfidenceScore(confidence);
            result.setFallbackUsed(true);
            result.setFallbackReason("Fallback por dominio general - " + intent.getDescription());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            result.setSuccess(true);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error en fallback por dominio general: {}", e.getMessage());
            return createFallbackResult("ayuda", 0.2, "Error en fallback por dominio general", startTime);
        }
    }
    
    /**
     * Nivel 3: Fallback por palabras clave
     * Analiza el texto buscando palabras clave específicas
     */
    private IntentClassificationResult applyKeywordFallback(
            IntentClassificationRequest request,
            long startTime) {
        
        try {
            logger.debug("Aplicando fallback por palabras clave");
            
            String text = request.getText().toLowerCase();
            String bestIntent = "ayuda";
            double bestScore = 0.0;
            
            // Buscar la palabra clave con mayor coincidencia
            for (Map.Entry<String, String> entry : KEYWORD_INTENT_MAPPING.entrySet()) {
                String keyword = entry.getKey();
                String intent = entry.getValue();
                
                if (text.contains(keyword)) {
                    // Calcular score basado en la posición y frecuencia de la palabra clave
                    int firstIndex = text.indexOf(keyword);
                    double positionScore = 1.0 - (firstIndex / (double) text.length());
                    double frequencyScore = (text.length() - text.replace(keyword, "").length()) / keyword.length();
                    double totalScore = (positionScore + frequencyScore) / 2.0;
                    
                    if (totalScore > bestScore) {
                        bestScore = totalScore;
                        bestIntent = intent;
                    }
                }
            }
            
            double confidence = Math.min(0.5, bestScore * 0.6); // Máximo 50% de confianza
            
            return createFallbackResult(bestIntent, confidence, 
                "Fallback por palabras clave - '" + bestIntent + "'", startTime);
            
        } catch (Exception e) {
            logger.error("Error en fallback por palabras clave: {}", e.getMessage());
            return createFallbackResult("ayuda", 0.1, "Error en fallback por palabras clave", startTime);
        }
    }
    
    /**
     * Nivel 4: Fallback por análisis de contexto
     * Analiza el contexto de la conversación y metadata
     */
    private IntentClassificationResult applyContextAnalysisFallback(
            IntentClassificationRequest request,
            long startTime) {
        
        try {
            logger.debug("Aplicando fallback por análisis de contexto");
            
            String selectedIntent = "ayuda";
            double confidence = 0.3;
            
            // Analizar metadata contextual si está disponible
            if (request.getContextMetadata() != null) {
                Map<String, Object> metadata = request.getContextMetadata();
                
                // Analizar timestamp para determinar contexto temporal
                if (metadata.containsKey("timestamp")) {
                    LocalDateTime timestamp = LocalDateTime.parse(metadata.get("timestamp").toString());
                    int hour = timestamp.getHour();
                    
                    if (hour >= 6 && hour < 12) {
                        selectedIntent = "saludo";
                        confidence = 0.4;
                    } else if (hour >= 12 && hour < 18) {
                        selectedIntent = "consulta_general";
                        confidence = 0.35;
                    } else if (hour >= 18 && hour < 22) {
                        selectedIntent = "entretenimiento";
                        confidence = 0.35;
                    }
                }
                
                // Analizar ubicación si está disponible
                if (metadata.containsKey("location")) {
                    String location = metadata.get("location").toString();
                    if (location.contains("casa") || location.contains("hogar")) {
                        if (selectedIntent.equals("ayuda")) {
                            selectedIntent = "smart_home_control";
                            confidence = 0.4;
                        }
                    }
                }
                
                // Analizar dispositivo si está disponible
                if (metadata.containsKey("device_type")) {
                    String deviceType = metadata.get("device_type").toString();
                    if (deviceType.contains("speaker") || deviceType.contains("altavoz")) {
                        if (selectedIntent.equals("ayuda")) {
                            selectedIntent = "reproducir_musica";
                            confidence = 0.35;
                        }
                    }
                }
            }
            
            return createFallbackResult(selectedIntent, confidence, 
                "Fallback por análisis de contexto", startTime);
            
        } catch (Exception e) {
            logger.error("Error en fallback por análisis de contexto: {}", e.getMessage());
            return createFallbackResult("ayuda", 0.2, "Error en fallback por análisis de contexto", startTime);
        }
    }
    
    /**
     * Nivel 5: Fallback genérico (último recurso)
     * Respuesta genérica cuando todos los demás fallbacks fallan
     */
    private IntentClassificationResult applyGenericFallback(
            IntentClassificationRequest request,
            long startTime) {
        
        logger.debug("Aplicando fallback genérico como último recurso");
        
        return createFallbackResult("ayuda", 0.1, 
            "Fallback genérico - todos los niveles de degradación fallaron", startTime);
    }
    
    /**
     * Fallback básico cuando la degradación gradual está deshabilitada
     */
    private IntentClassificationResult applyBasicFallback(
            IntentClassificationRequest request,
            IntentClassificationResult originalResult,
            long startTime) {
        
        logger.debug("Aplicando fallback básico");
        
        return createFallbackResult("ayuda", 0.3, 
            "Fallback básico - degradación gradual deshabilitada", startTime);
    }
    
    /**
     * Crea un resultado de fallback con configuración estándar
     */
    private IntentClassificationResult createFallbackResult(
            String intentId, 
            double confidence, 
            String reason, 
            long startTime) {
        
        IntentClassificationResult result = new IntentClassificationResult();
        result.setIntentId(intentId);
        result.setConfidenceScore(confidence);
        result.setFallbackUsed(true);
        result.setFallbackReason(reason);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setSuccess(true);
        
        return result;
    }
    
    /**
     * Genera embedding para un texto (método auxiliar)
     */
    private List<Float> generateTextEmbedding(String text) {
        // TODO: Implementar generación real de embeddings
        // Por ahora usamos embeddings mock para testing
        List<Float> embedding = new ArrayList<>();
        Random random = new Random(text.hashCode());
        
        for (int i = 0; i < 1536; i++) {
            embedding.add(random.nextFloat() * 2 - 1);
        }
        
        return embedding;
    }
    
    /**
     * Clasifica usando LLM (método auxiliar)
     */
    private String classifyWithLlm(String prompt) {
        // TODO: Implementar llamada real al LLM
        // Por ahora simulamos una respuesta para testing
        return "{\"intent\": \"ayuda\", \"confidence\": 0.3, \"entities\": {}}";
    }
    
    /**
     * Parsea respuesta del LLM (método auxiliar)
     */
    private IntentClassificationResult parseLlmResponse(String llmResponse, List<EmbeddingDocument> examples) {
        IntentClassificationResult result = new IntentClassificationResult();
        
        try {
            // Extraer intent del JSON
            String intent = extractJsonValue(llmResponse, "intent");
            String confidenceStr = extractJsonValue(llmResponse, "confidence");
            
            result.setIntentId(intent != null ? intent : "ayuda");
            result.setConfidenceScore(confidenceStr != null ? Double.parseDouble(confidenceStr) : 0.3);
            result.setSuccess(true);
            
        } catch (Exception e) {
            logger.error("Error parseando respuesta del LLM: {}", e.getMessage());
            result.setIntentId("ayuda");
            result.setConfidenceScore(0.1);
            result.setSuccess(false);
        }
        
        return result;
    }
    
    /**
     * Extrae valor de JSON (método auxiliar)
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.error("Error extrayendo valor JSON: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Obtiene estadísticas del servicio de fallback
     */
    public Map<String, Object> getFallbackStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("enable_gradual_degradation", enableGradualDegradation);
        stats.put("max_degradation_levels", maxDegradationLevels);
        stats.put("similarity_reduction_factor", similarityReductionFactor);
        stats.put("enable_keyword_fallback", enableKeywordFallback);
        stats.put("enable_context_fallback", enableContextFallback);
        stats.put("enable_general_domain_fallback", enableGeneralDomainFallback);
        stats.put("min_confidence_for_degradation", minConfidenceForDegradation);
        stats.put("max_processing_time_ms", maxProcessingTimeMs);
        stats.put("keyword_mappings_count", KEYWORD_INTENT_MAPPING.size());
        stats.put("available_keywords", new ArrayList<>(KEYWORD_INTENT_MAPPING.keySet()));
        
        return stats;
    }
    
    /**
     * Verifica la salud del servicio de fallback
     */
    public boolean isHealthy() {
        try {
            // Verificar que los servicios dependientes estén disponibles
            boolean vectorStoreHealthy = vectorStoreService.isHealthy();
            boolean intentConfigHealthy = intentConfigManager.getHealthInfo().get("status").equals("HEALTHY");
            
            return vectorStoreHealthy && intentConfigHealthy;
        } catch (Exception e) {
            logger.error("Error verificando salud del servicio de fallback: {}", e.getMessage());
            return false;
        }
    }
} 