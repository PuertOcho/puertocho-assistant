package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.EmbeddingDocument;
import com.intentmanagerms.domain.model.IntentExample;
import com.intentmanagerms.domain.model.IntentClassificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio especializado para prompt engineering dinámico con contexto RAG
 * 
 * Características principales:
 * - Prompts adaptativos basados en el contexto de la conversación
 * - Múltiples estrategias de prompt (few-shot, zero-shot, chain-of-thought)
 * - Personalización por dominio de experto
 * - Optimización de tokens y contexto
 * - Templates dinámicos con variables
 */
@Service
public class DynamicPromptEngineeringService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicPromptEngineeringService.class);

    @Autowired
    private IntentConfigManager intentConfigManager;

    // Configuración de prompt engineering
    @Value("${rag.prompt.strategy:adaptive}")
    private String promptStrategy;

    @Value("${rag.prompt.max-context-length:3000}")
    private int maxContextLength;

    @Value("${rag.prompt.enable-chain-of-thought:true}")
    private boolean enableChainOfThought;

    @Value("${rag.prompt.enable-contextual-hints:true}")
    private boolean enableContextualHints;

    @Value("${rag.prompt.enable-entity-extraction:true}")
    private boolean enableEntityExtraction;

    @Value("${rag.prompt.enable-confidence-calibration:true}")
    private boolean enableConfidenceCalibration;

    @Value("${rag.prompt.temperature:0.3}")
    private double promptTemperature;

    @Value("${rag.prompt.max-tokens:2048}")
    private int maxTokens;

    @Value("${rag.prompt.language:es}")
    private String promptLanguage;

    /**
     * Construye un prompt dinámico basado en la estrategia configurada
     */
    public String buildDynamicPrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples) {
        logger.debug("Construyendo prompt dinámico con estrategia: {}", promptStrategy);
        return buildPromptWithStrategy(request, examples, promptStrategy);
    }

    /**
     * Construye un prompt con estrategia específica
     */
    public String buildPromptWithStrategy(IntentClassificationRequest request, List<EmbeddingDocument> examples, String strategy) {
        logger.debug("Construyendo prompt con estrategia específica: {}", strategy);
        
        switch (strategy.toLowerCase()) {
            case "adaptive":
                return buildAdaptivePrompt(request, examples);
            case "few-shot":
                return buildFewShotPrompt(request, examples);
            case "zero-shot":
                return buildZeroShotPrompt(request, examples);
            case "chain-of-thought":
                return buildChainOfThoughtPrompt(request, examples);
            case "expert-domain":
                return buildExpertDomainPrompt(request, examples);
            default:
                logger.warn("Estrategia '{}' no reconocida, usando estrategia adaptativa por defecto", strategy);
                return buildAdaptivePrompt(request, examples);
        }
    }

    /**
     * Prompt adaptativo que se ajusta según el contexto y calidad de ejemplos
     */
    private String buildAdaptivePrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples) {
        StringBuilder prompt = new StringBuilder();
        
        // Header contextual
        prompt.append(buildContextualHeader(request));
        
        // Análisis de calidad de ejemplos
        PromptQualityAnalysis qualityAnalysis = analyzeExampleQuality(examples);
        
        // Construir prompt según calidad
        if (qualityAnalysis.isHighQuality()) {
            prompt.append(buildHighQualityPrompt(request, examples, qualityAnalysis));
        } else if (qualityAnalysis.isMediumQuality()) {
            prompt.append(buildMediumQualityPrompt(request, examples, qualityAnalysis));
        } else {
            prompt.append(buildLowQualityPrompt(request, examples, qualityAnalysis));
        }
        
        // Footer con instrucciones específicas
        prompt.append(buildPromptFooter(request));
        
        return optimizePromptLength(prompt.toString());
    }

    /**
     * Prompt few-shot tradicional con ejemplos de alta calidad
     */
    private String buildFewShotPrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Eres un clasificador de intenciones experto. Clasifica la intención del usuario basándote en estos ejemplos:\n\n");
        
        // Ejemplos organizados por intención
        Map<String, List<EmbeddingDocument>> examplesByIntent = examples.stream()
                .collect(Collectors.groupingBy(EmbeddingDocument::getIntent));
        
        for (Map.Entry<String, List<EmbeddingDocument>> entry : examplesByIntent.entrySet()) {
            String intentId = entry.getKey();
            List<EmbeddingDocument> intentExamples = entry.getValue();
            
            prompt.append("INTENCIÓN: ").append(intentId).append("\n");
            for (int i = 0; i < Math.min(3, intentExamples.size()); i++) {
                EmbeddingDocument example = intentExamples.get(i);
                prompt.append(String.format("  Ejemplo %d: \"%s\" (similitud: %.3f)\n", 
                    i + 1, example.getContent(), example.getSimilarity()));
            }
            prompt.append("\n");
        }
        
        prompt.append("TEXTO A CLASIFICAR: \"").append(request.getText()).append("\"\n\n");
        prompt.append("Responde en formato JSON con la intención más probable.");
        
        return prompt.toString();
    }

    /**
     * Prompt zero-shot sin ejemplos específicos
     */
    private String buildZeroShotPrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Eres un clasificador de intenciones experto. Analiza el siguiente texto y clasifica la intención del usuario.\n\n");
        
        // Información contextual sobre intenciones disponibles
        Set<String> availableIntents = intentConfigManager.getAllIntents().keySet();
        prompt.append("INTENCIONES DISPONIBLES:\n");
        for (String intentId : availableIntents) {
            IntentExample intent = intentConfigManager.getIntent(intentId);
            if (intent != null) {
                prompt.append("- ").append(intentId).append(": ").append(intent.getDescription()).append("\n");
            }
        }
        
        prompt.append("\nTEXTO A CLASIFICAR: \"").append(request.getText()).append("\"\n\n");
        prompt.append("Basándote en el contexto y las intenciones disponibles, clasifica la intención del usuario.");
        
        return prompt.toString();
    }

    /**
     * Prompt chain-of-thought con razonamiento paso a paso
     */
    private String buildChainOfThoughtPrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Eres un clasificador de intenciones experto. Analiza el siguiente texto paso a paso:\n\n");
        
        prompt.append("PASO 1 - ANÁLISIS DEL TEXTO:\n");
        prompt.append("Texto: \"").append(request.getText()).append("\"\n");
        prompt.append("Analiza las palabras clave, el tono y el contexto del mensaje.\n\n");
        
        prompt.append("PASO 2 - EJEMPLOS SIMILARES:\n");
        for (int i = 0; i < Math.min(3, examples.size()); i++) {
            EmbeddingDocument example = examples.get(i);
            prompt.append(String.format("Ejemplo %d: \"%s\" → %s (similitud: %.3f)\n", 
                i + 1, example.getContent(), example.getIntent(), example.getSimilarity()));
        }
        prompt.append("\n");
        
        prompt.append("PASO 3 - RAZONAMIENTO:\n");
        prompt.append("Compara el texto con los ejemplos y explica tu razonamiento.\n\n");
        
        prompt.append("PASO 4 - CLASIFICACIÓN:\n");
        prompt.append("Basándote en tu análisis, proporciona la clasificación final en formato JSON.");
        
        return prompt.toString();
    }

    /**
     * Prompt especializado por dominio de experto
     */
    private String buildExpertDomainPrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples) {
        StringBuilder prompt = new StringBuilder();
        
        // Determinar dominio principal de los ejemplos
        String primaryDomain = determinePrimaryDomain(examples);
        
        prompt.append("Eres un experto en el dominio: ").append(primaryDomain).append("\n\n");
        prompt.append("Tu especialización incluye:\n");
        prompt.append(getDomainExpertise(primaryDomain));
        prompt.append("\n");
        
        // Ejemplos del dominio específico
        List<EmbeddingDocument> domainExamples = examples.stream()
                .filter(example -> {
                    IntentExample intent = intentConfigManager.getIntent(example.getIntent());
                    return intent != null && primaryDomain.equals(intent.getExpertDomain());
                })
                .collect(Collectors.toList());
        
        prompt.append("EJEMPLOS DE TU DOMINIO:\n");
        for (int i = 0; i < Math.min(5, domainExamples.size()); i++) {
            EmbeddingDocument example = domainExamples.get(i);
            prompt.append(String.format("%d. \"%s\" → %s\n", 
                i + 1, example.getContent(), example.getIntent()));
        }
        
        prompt.append("\nTEXTO A CLASIFICAR: \"").append(request.getText()).append("\"\n\n");
        prompt.append("Como experto en ").append(primaryDomain).append(", clasifica la intención del usuario.");
        
        return prompt.toString();
    }

    /**
     * Construye header contextual con metadata
     */
    private String buildContextualHeader(IntentClassificationRequest request) {
        StringBuilder header = new StringBuilder();
        
        header.append("=== CLASIFICACIÓN DE INTENCIONES - CONTEXTO DINÁMICO ===\n");
        header.append("Timestamp: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        header.append("Idioma: ").append(promptLanguage.toUpperCase()).append("\n");
        
        if (request.getSessionId() != null) {
            header.append("Sesión: ").append(request.getSessionId()).append("\n");
        }
        
        if (request.getContextMetadata() != null && !request.getContextMetadata().isEmpty()) {
            header.append("Contexto: ").append(request.getContextMetadata()).append("\n");
        }
        
        header.append("==================================================\n\n");
        
        return header.toString();
    }

    /**
     * Prompt para ejemplos de alta calidad
     */
    private String buildHighQualityPrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples, PromptQualityAnalysis analysis) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("EJEMPLOS DE ALTA CALIDAD (Similitud promedio: ").append(String.format("%.3f", analysis.getAverageSimilarity())).append("):\n");
        
        // Agrupar por intención y mostrar los mejores ejemplos
        Map<String, List<EmbeddingDocument>> bestExamples = examples.stream()
                .filter(example -> example.getSimilarity() >= analysis.getHighQualityThreshold())
                .collect(Collectors.groupingBy(EmbeddingDocument::getIntent));
        
        for (Map.Entry<String, List<EmbeddingDocument>> entry : bestExamples.entrySet()) {
            String intentId = entry.getKey();
            List<EmbeddingDocument> intentExamples = entry.getValue();
            
            prompt.append("\nINTENCIÓN: ").append(intentId).append("\n");
            for (int i = 0; i < Math.min(2, intentExamples.size()); i++) {
                EmbeddingDocument example = intentExamples.get(i);
                prompt.append(String.format("  ✓ \"%s\" (%.3f)\n", example.getContent(), example.getSimilarity()));
            }
        }
        
        prompt.append("\nTEXTO A CLASIFICAR: \"").append(request.getText()).append("\"\n\n");
        prompt.append("INSTRUCCIONES ESPECÍFICAS:\n");
        prompt.append("1. Los ejemplos proporcionados son de alta calidad y muy relevantes\n");
        prompt.append("2. Usa estos ejemplos como referencia principal para la clasificación\n");
        prompt.append("3. Extrae entidades específicas mencionadas en el texto\n");
        prompt.append("4. Proporciona un nivel de confianza alto si hay similitud clara\n");
        
        return prompt.toString();
    }

    /**
     * Prompt para ejemplos de calidad media
     */
    private String buildMediumQualityPrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples, PromptQualityAnalysis analysis) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("EJEMPLOS DE CALIDAD MEDIA (Similitud promedio: ").append(String.format("%.3f", analysis.getAverageSimilarity())).append("):\n");
        
        // Mostrar ejemplos con similitud media
        List<EmbeddingDocument> mediumExamples = examples.stream()
                .filter(example -> example.getSimilarity() >= analysis.getMediumQualityThreshold())
                .limit(5)
                .collect(Collectors.toList());
        
        for (int i = 0; i < mediumExamples.size(); i++) {
            EmbeddingDocument example = mediumExamples.get(i);
            prompt.append(String.format("%d. \"%s\" → %s (%.3f)\n", 
                i + 1, example.getContent(), example.getIntent(), example.getSimilarity()));
        }
        
        prompt.append("\nTEXTO A CLASIFICAR: \"").append(request.getText()).append("\"\n\n");
        prompt.append("INSTRUCCIONES ESPECÍFICAS:\n");
        prompt.append("1. Los ejemplos tienen similitud moderada, usa criterio adicional\n");
        prompt.append("2. Considera el contexto general y las palabras clave\n");
        prompt.append("3. Si no hay similitud clara, considera intenciones relacionadas\n");
        prompt.append("4. Proporciona un nivel de confianza moderado\n");
        
        return prompt.toString();
    }

    /**
     * Prompt para ejemplos de baja calidad
     */
    private String buildLowQualityPrompt(IntentClassificationRequest request, List<EmbeddingDocument> examples, PromptQualityAnalysis analysis) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("ADVERTENCIA: Ejemplos de baja similitud (Promedio: ").append(String.format("%.3f", analysis.getAverageSimilarity())).append(")\n");
        prompt.append("Usando estrategia de clasificación alternativa.\n\n");
        
        // Mostrar intenciones disponibles como referencia
        Set<String> availableIntents = intentConfigManager.getAllIntents().keySet();
        prompt.append("INTENCIONES DISPONIBLES:\n");
        for (String intentId : availableIntents) {
            IntentExample intent = intentConfigManager.getIntent(intentId);
            if (intent != null) {
                prompt.append("- ").append(intentId).append(": ").append(intent.getDescription()).append("\n");
            }
        }
        
        prompt.append("\nTEXTO A CLASIFICAR: \"").append(request.getText()).append("\"\n\n");
        prompt.append("INSTRUCCIONES ESPECÍFICAS:\n");
        prompt.append("1. Los ejemplos no son muy relevantes, usa análisis semántico\n");
        prompt.append("2. Considera palabras clave y patrones generales\n");
        prompt.append("3. Si no hay coincidencia clara, usa la intención de ayuda\n");
        prompt.append("4. Proporciona un nivel de confianza bajo\n");
        
        return prompt.toString();
    }

    /**
     * Construye el footer del prompt con instrucciones específicas
     */
    private String buildPromptFooter(IntentClassificationRequest request) {
        StringBuilder footer = new StringBuilder();
        
        footer.append("\nFORMATO DE RESPUESTA REQUERIDO:\n");
        footer.append("{\n");
        footer.append("  \"intent_id\": \"nombre_de_la_intencion\",\n");
        footer.append("  \"confidence\": 0.85,\n");
        footer.append("  \"entities\": {\"entidad\": \"valor\"},\n");
        footer.append("  \"reasoning\": \"explicación breve del razonamiento\",\n");
        footer.append("  \"alternative_intents\": [\"intencion_alternativa_1\", \"intencion_alternativa_2\"],\n");
        footer.append("  \"suggested_questions\": [\"pregunta_sugerida_1\", \"pregunta_sugerida_2\"]\n");
        footer.append("}\n\n");
        
        if (enableEntityExtraction) {
            footer.append("INSTRUCCIONES DE EXTRACCIÓN DE ENTIDADES:\n");
            footer.append("- Identifica entidades mencionadas en el texto\n");
            footer.append("- Incluye ubicaciones, fechas, nombres, etc.\n");
            footer.append("- Usa el formato entidad: valor en el JSON\n");
        }
        
        if (enableConfidenceCalibration) {
            footer.append("CALIBRACIÓN DE CONFIANZA:\n");
            footer.append("- 0.9-1.0: Muy alta confianza, ejemplos muy similares\n");
            footer.append("- 0.7-0.9: Alta confianza, ejemplos similares\n");
            footer.append("- 0.5-0.7: Confianza moderada, similitud parcial\n");
            footer.append("- 0.3-0.5: Baja confianza, similitud débil\n");
            footer.append("- 0.0-0.3: Muy baja confianza, usar fallback\n");
        }
        
        footer.append("\nRESPUESTA:");
        
        return footer.toString();
    }

    /**
     * Analiza la calidad de los ejemplos proporcionados
     */
    private PromptQualityAnalysis analyzeExampleQuality(List<EmbeddingDocument> examples) {
        if (examples.isEmpty()) {
            return new PromptQualityAnalysis(0.0, 0.0, 0.0, PromptQuality.LOW);
        }
        
        double averageSimilarity = examples.stream()
                .mapToDouble(EmbeddingDocument::getSimilarity)
                .average()
                .orElse(0.0);
        
        double highQualityThreshold = 0.8;
        double mediumQualityThreshold = 0.6;
        
        PromptQuality quality;
        if (averageSimilarity >= highQualityThreshold) {
            quality = PromptQuality.HIGH;
        } else if (averageSimilarity >= mediumQualityThreshold) {
            quality = PromptQuality.MEDIUM;
        } else {
            quality = PromptQuality.LOW;
        }
        
        return new PromptQualityAnalysis(averageSimilarity, highQualityThreshold, mediumQualityThreshold, quality);
    }

    /**
     * Determina el dominio principal de los ejemplos
     */
    private String determinePrimaryDomain(List<EmbeddingDocument> examples) {
        Map<String, Long> domainCounts = examples.stream()
                .map(EmbeddingDocument::getIntent)
                .map(intentId -> {
                    IntentExample intent = intentConfigManager.getIntent(intentId);
                    return intent != null ? intent.getExpertDomain() : "general";
                })
                .collect(Collectors.groupingBy(domain -> domain, Collectors.counting()));
        
        return domainCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("general");
    }

    /**
     * Obtiene la descripción de expertise por dominio
     */
    private String getDomainExpertise(String domain) {
        switch (domain.toLowerCase()) {
            case "weather":
                return "- Consultas meteorológicas y climáticas\n- Predicciones del tiempo\n- Condiciones ambientales";
            case "smart_home":
                return "- Control de dispositivos domóticos\n- Automatización del hogar\n- Gestión de iluminación y temperatura";
            case "entertainment":
                return "- Reproducción de música y video\n- Control de entretenimiento\n- Gestión de contenido multimedia";
            case "development":
                return "- Herramientas de desarrollo\n- Control de versiones\n- Gestión de proyectos de software";
            case "project_management":
                return "- Gestión de tareas y proyectos\n- Seguimiento de progreso\n- Organización de trabajo";
            case "system":
                return "- Control del sistema\n- Gestión de alarmas y recordatorios\n- Operaciones básicas del sistema";
            default:
                return "- Clasificación general de intenciones\n- Análisis de contexto conversacional\n- Gestión de consultas diversas";
        }
    }

    /**
     * Optimiza la longitud del prompt para no exceder límites
     */
    private String optimizePromptLength(String prompt) {
        if (prompt.length() <= maxContextLength) {
            return prompt;
        }
        
        logger.warn("Prompt excede longitud máxima ({} chars), optimizando...", maxContextLength);
        
        // Estrategia simple: truncar manteniendo las partes más importantes
        String[] parts = prompt.split("\n\n");
        StringBuilder optimized = new StringBuilder();
        
        for (String part : parts) {
            if (optimized.length() + part.length() + 2 <= maxContextLength) {
                optimized.append(part).append("\n\n");
            } else {
                break;
            }
        }
        
        optimized.append("... [Prompt truncado por límite de longitud]");
        
        return optimized.toString();
    }

    /**
     * Clase interna para análisis de calidad de prompts
     */
    private static class PromptQualityAnalysis {
        private final double averageSimilarity;
        private final double highQualityThreshold;
        private final double mediumQualityThreshold;
        private final PromptQuality quality;

        public PromptQualityAnalysis(double averageSimilarity, double highQualityThreshold, 
                                   double mediumQualityThreshold, PromptQuality quality) {
            this.averageSimilarity = averageSimilarity;
            this.highQualityThreshold = highQualityThreshold;
            this.mediumQualityThreshold = mediumQualityThreshold;
            this.quality = quality;
        }

        public double getAverageSimilarity() { return averageSimilarity; }
        public double getHighQualityThreshold() { return highQualityThreshold; }
        public double getMediumQualityThreshold() { return mediumQualityThreshold; }
        public PromptQuality getQuality() { return quality; }
        public boolean isHighQuality() { return quality == PromptQuality.HIGH; }
        public boolean isMediumQuality() { return quality == PromptQuality.MEDIUM; }
        public boolean isLowQuality() { return quality == PromptQuality.LOW; }
    }

    /**
     * Enum para calidad de prompts
     */
    private enum PromptQuality {
        HIGH, MEDIUM, LOW
    }
} 