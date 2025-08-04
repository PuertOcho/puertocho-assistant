package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.LlmConfiguration;
import com.intentmanagerms.domain.model.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Servicio de inicialización que configura automáticamente los LLMs
 * al arrancar la aplicación.
 */
@Service
public class LlmInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmInitializationService.class);
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    // Configuración para MoE desde variables de entorno
    @Value("${moe.enabled:true}")
    private boolean moeEnabled;
    
    @Value("${moe.llm-a.model:gpt-4}")
    private String moeLlmAModel;
    
    @Value("${moe.llm-b.model:claude-3-sonnet-20240229}")
    private String moeLlmBModel;
    
    @Value("${moe.llm-c.model:gpt-3.5-turbo}")
    private String moeLlmCModel;
    
    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;
    
    @Value("${ANTHROPIC_API_KEY:}")
    private String anthropicApiKey;
    
    /**
     * Se ejecuta cuando la aplicación está lista para recibir requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeLlmConfigurations() {
        logger.info("Inicializando configuraciones de LLM...");
        
        try {
            // Inicializar configuración por defecto
            llmConfigurationService.initializeDefaultConfigurations();
            
            // Si MoE está habilitado, configurar LLMs para votación
            if (moeEnabled) {
                initializeMoELlmConfigurations();
            }
            
            // Mostrar estadísticas finales
            showLlmStatistics();
            
            logger.info("Configuración de LLMs completada exitosamente");
            
        } catch (Exception e) {
            logger.error("Error al inicializar configuraciones de LLM", e);
            throw new RuntimeException("No se pudo inicializar las configuraciones de LLM", e);
        }
    }
    
    /**
     * Inicializa las configuraciones de LLM para el sistema MoE.
     */
    private void initializeMoELlmConfigurations() {
        logger.info("Configurando LLMs para sistema MoE (votación)...");
        
        // LLM A - Juez Crítico (GPT-4)
        if (isValidApiKey(openaiApiKey)) {
            LlmConfiguration llmA = new LlmConfiguration(
                "moe-llm-a",
                "Juez A - Análisis Crítico",
                LlmProvider.OPENAI,
                moeLlmAModel,
                openaiApiKey
            );
            configureMoELlm(llmA, "Juez A - Análisis crítico y evaluación detallada", 1.0);
            llmConfigurationService.addLlmConfiguration(llmA);
        } else {
            logger.warn("API Key de OpenAI no configurada, omitiendo LLM A para MoE");
        }
        
        // LLM B - Juez Contextual (Claude)
        if (isValidApiKey(anthropicApiKey)) {
            LlmConfiguration llmB = new LlmConfiguration(
                "moe-llm-b",
                "Juez B - Contexto Conversacional",
                LlmProvider.ANTHROPIC,
                moeLlmBModel,
                anthropicApiKey
            );
            configureMoELlm(llmB, "Juez B - Análisis de contexto conversacional y coherencia", 1.0);
            llmConfigurationService.addLlmConfiguration(llmB);
        } else {
            logger.warn("API Key de Anthropic no configurada, omitiendo LLM B para MoE");
        }
        
        // LLM C - Juez Práctico (GPT-3.5)
        if (isValidApiKey(openaiApiKey)) {
            LlmConfiguration llmC = new LlmConfiguration(
                "moe-llm-c",
                "Juez C - Practicidad de Acción",
                LlmProvider.OPENAI,
                moeLlmCModel,
                openaiApiKey
            );
            configureMoELlm(llmC, "Juez C - Evaluación de practicidad y factibilidad de acciones", 0.9);
            llmConfigurationService.addLlmConfiguration(llmC);
        } else {
            logger.warn("API Key de OpenAI no configurada, omitiendo LLM C para MoE");
        }
    }
    
    /**
     * Configura un LLM para el sistema MoE con parámetros específicos.
     */
    private void configureMoELlm(LlmConfiguration llm, String role, double weight) {
        llm.setRole(role);
        llm.setWeight(weight);
        llm.setTimeout(Duration.ofSeconds(30));
        llm.setMaxTokens(2048);
        llm.setTemperature(0.3); // Más determinístico para votación
        llm.setMaxRetries(2);
        llm.setEnabled(true);
    }
    
    /**
     * Verifica si una API key es válida (no nula y no vacía).
     */
    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("${OPENAI_API_KEY}") && !apiKey.equals("${ANTHROPIC_API_KEY}");
    }
    
    /**
     * Muestra estadísticas de los LLMs configurados.
     */
    private void showLlmStatistics() {
        var stats = llmConfigurationService.getLlmStatistics();
        
        logger.info("=== Estadísticas de LLMs Configurados ===");
        logger.info("Total de LLMs: {}", stats.get("total"));
        logger.info("LLMs habilitados: {}", stats.get("enabled"));
        logger.info("LLMs deshabilitados: {}", stats.get("disabled"));
        logger.info("LLMs por proveedor: {}", stats.get("byProvider"));
        logger.info("IDs de LLMs habilitados: {}", stats.get("enabledIds"));
        
        // Mostrar detalles de cada LLM habilitado
        List<LlmConfiguration> enabled = llmConfigurationService.getEnabledLlmConfigurations();
        logger.info("=== Detalles de LLMs Habilitados ===");
        for (LlmConfiguration llm : enabled) {
            logger.info("- {}: {} ({}) - Peso: {}, Rol: {}", 
                       llm.getId(), llm.getName(), llm.getModel(), 
                       llm.getWeight(), llm.getRole());
        }
    }
    
    /**
     * Verifica la salud de todos los LLMs configurados.
     */
    public void verifyLlmHealth() {
        logger.info("Verificando salud de LLMs configurados...");
        
        List<LlmConfiguration> enabled = llmConfigurationService.getEnabledLlmConfigurations();
        int healthyCount = 0;
        
        for (LlmConfiguration llm : enabled) {
            boolean isHealthy = llmConfigurationService.isLlmHealthy(llm.getId());
            if (isHealthy) {
                healthyCount++;
                logger.info("✅ LLM {} ({}) - SALUDABLE", llm.getId(), llm.getName());
            } else {
                logger.warn("❌ LLM {} ({}) - NO SALUDABLE", llm.getId(), llm.getName());
            }
        }
        
        logger.info("Resumen de salud: {}/{} LLMs saludables", healthyCount, enabled.size());
        
        if (healthyCount == 0) {
            logger.error("⚠️  ADVERTENCIA: Ningún LLM está saludable. El sistema puede no funcionar correctamente.");
        } else if (healthyCount < enabled.size()) {
            logger.warn("⚠️  ADVERTENCIA: Algunos LLMs no están saludables. Se usará fallback.");
        }
    }
} 