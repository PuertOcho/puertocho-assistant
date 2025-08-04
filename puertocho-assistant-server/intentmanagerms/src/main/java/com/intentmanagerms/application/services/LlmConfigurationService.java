package com.intentmanagerms.application.services;

import com.intentmanagerms.domain.model.LlmConfiguration;
import com.intentmanagerms.domain.model.LlmProvider;
import com.intentmanagerms.domain.model.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar múltiples LLMs de forma configurable.
 * 
 * Responsabilidades:
 * - Configurar y gestionar múltiples LLMs
 * - Proporcionar acceso a LLMs específicos por ID
 * - Gestionar configuraciones dinámicas
 * - Validar configuraciones de LLMs
 * - Proporcionar fallbacks cuando LLMs fallan
 */
@Service
public class LlmConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmConfigurationService.class);
    
    private final Map<String, LlmConfiguration> llmConfigurations = new ConcurrentHashMap<>();
    private final Map<String, Object> llmInstances = new ConcurrentHashMap<>();
    
    // Configuración desde application.yml
    @Value("${llm.primary.model:gpt-4}")
    private String primaryLlmModel;
    
    @Value("${llm.primary.provider:openai}")
    private String primaryLlmProvider;
    
    @Value("${llm.primary.api-key:${OPENAI_API_KEY}}")
    private String primaryLlmApiKey;
    
    @Value("${llm.timeout:30s}")
    private Duration defaultTimeout;
    
    @Value("${llm.max-tokens:4096}")
    private Integer defaultMaxTokens;
    
    @Value("${llm.temperature:0.7}")
    private Double defaultTemperature;
    
    @Value("${llm.max-retries:3}")
    private Integer defaultMaxRetries;
    
    /**
     * Inicializa las configuraciones de LLM por defecto.
     */
    public void initializeDefaultConfigurations() {
        logger.info("Inicializando configuraciones de LLM por defecto...");
        
        // LLM Primario
        LlmConfiguration primaryLlm = new LlmConfiguration(
            "primary",
            "LLM Primario",
            LlmProvider.fromCode(primaryLlmProvider),
            primaryLlmModel,
            primaryLlmApiKey
        );
        primaryLlm.setTimeout(defaultTimeout);
        primaryLlm.setMaxTokens(defaultMaxTokens);
        primaryLlm.setTemperature(defaultTemperature);
        primaryLlm.setMaxRetries(defaultMaxRetries);
        primaryLlm.setRole("LLM Principal para clasificación de intenciones");
        primaryLlm.setWeight(1.0);
        
        addLlmConfiguration(primaryLlm);
        
        // LLM de Fallback (si es diferente al primario)
        if (!"gpt-3.5-turbo".equals(primaryLlmModel)) {
            LlmConfiguration fallbackLlm = new LlmConfiguration(
                "fallback",
                "LLM de Fallback",
                LlmProvider.OPENAI,
                "gpt-3.5-turbo",
                primaryLlmApiKey
            );
            fallbackLlm.setTimeout(defaultTimeout);
            fallbackLlm.setMaxTokens(defaultMaxTokens);
            fallbackLlm.setTemperature(defaultTemperature);
            fallbackLlm.setMaxRetries(defaultMaxRetries);
            fallbackLlm.setRole("LLM de respaldo cuando el primario falla");
            fallbackLlm.setWeight(0.8);
            
            addLlmConfiguration(fallbackLlm);
        }
        
        logger.info("Configuraciones de LLM inicializadas: {}", 
                   llmConfigurations.keySet());
    }
    
    /**
     * Añade una nueva configuración de LLM.
     */
    public void addLlmConfiguration(LlmConfiguration configuration) {
        validateConfiguration(configuration);
        llmConfigurations.put(configuration.getId(), configuration);
        logger.info("LLM configurado: {} - {} ({})", 
                   configuration.getId(), configuration.getName(), configuration.getModel());
    }
    
    /**
     * Obtiene una configuración de LLM por ID.
     */
    public Optional<LlmConfiguration> getLlmConfiguration(String llmId) {
        return Optional.ofNullable(llmConfigurations.get(llmId));
    }
    
    /**
     * Obtiene todas las configuraciones de LLM habilitadas.
     */
    public List<LlmConfiguration> getEnabledLlmConfigurations() {
        return llmConfigurations.values().stream()
                .filter(LlmConfiguration::isEnabled)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene el LLM primario.
     */
    public Optional<LlmConfiguration> getPrimaryLlm() {
        return getLlmConfiguration("primary");
    }
    
    /**
     * Obtiene el LLM de fallback.
     */
    public Optional<LlmConfiguration> getFallbackLlm() {
        return getLlmConfiguration("fallback");
    }
    
    /**
     * Obtiene un LLM aleatorio de los habilitados (útil para load balancing).
     */
    public Optional<LlmConfiguration> getRandomLlm() {
        List<LlmConfiguration> enabled = getEnabledLlmConfigurations();
        if (enabled.isEmpty()) {
            return Optional.empty();
        }
        
        Random random = new Random();
        return Optional.of(enabled.get(random.nextInt(enabled.size())));
    }
    
    /**
     * Obtiene múltiples LLMs para el sistema de votación MoE.
     */
    public List<LlmConfiguration> getLlmForVoting(int count) {
        List<LlmConfiguration> enabled = getEnabledLlmConfigurations();
        
        // Si no hay suficientes LLMs, devolver todos los disponibles
        if (enabled.size() <= count) {
            return enabled;
        }
        
        // Seleccionar los primeros 'count' LLMs (priorizando por peso)
        return enabled.stream()
                .sorted((a, b) -> Double.compare(b.getWeight(), a.getWeight()))
                .limit(count)
                .collect(Collectors.toList());
    }
    
    /**
     * Valida una configuración de LLM.
     */
    private void validateConfiguration(LlmConfiguration configuration) {
        if (configuration.getId() == null || configuration.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID de LLM no puede estar vacío");
        }
        
        if (configuration.getName() == null || configuration.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de LLM no puede estar vacío");
        }
        
        if (configuration.getProvider() == null) {
            throw new IllegalArgumentException("Proveedor de LLM no puede estar vacío");
        }
        
        if (configuration.getModel() == null || configuration.getModel().trim().isEmpty()) {
            throw new IllegalArgumentException("Modelo de LLM no puede estar vacío");
        }
        
        if (configuration.getApiKey() == null || configuration.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("API Key de LLM no puede estar vacía");
        }
        
        // Validar valores por defecto si no están establecidos
        if (configuration.getTimeout() == null) {
            configuration.setTimeout(defaultTimeout);
        }
        
        if (configuration.getMaxTokens() == null) {
            configuration.setMaxTokens(defaultMaxTokens);
        }
        
        if (configuration.getTemperature() == null) {
            configuration.setTemperature(defaultTemperature);
        }
        
        if (configuration.getMaxRetries() == null) {
            configuration.setMaxRetries(defaultMaxRetries);
        }
        
        if (configuration.getWeight() == null) {
            configuration.setWeight(1.0);
        }
    }
    
    /**
     * Actualiza una configuración de LLM existente.
     */
    public void updateLlmConfiguration(String llmId, LlmConfiguration newConfiguration) {
        if (!llmConfigurations.containsKey(llmId)) {
            throw new IllegalArgumentException("LLM con ID '" + llmId + "' no encontrado");
        }
        
        validateConfiguration(newConfiguration);
        newConfiguration.setId(llmId); // Asegurar que el ID no cambie
        
        llmConfigurations.put(llmId, newConfiguration);
        logger.info("LLM actualizado: {} - {} ({})", 
                   llmId, newConfiguration.getName(), newConfiguration.getModel());
    }
    
    /**
     * Habilita o deshabilita un LLM.
     */
    public void setLlmEnabled(String llmId, boolean enabled) {
        LlmConfiguration config = llmConfigurations.get(llmId);
        if (config == null) {
            throw new IllegalArgumentException("LLM con ID '" + llmId + "' no encontrado");
        }
        
        config.setEnabled(enabled);
        logger.info("LLM {} {}: {}", llmId, enabled ? "habilitado" : "deshabilitado", config.getName());
    }
    
    /**
     * Elimina una configuración de LLM.
     */
    public void removeLlmConfiguration(String llmId) {
        LlmConfiguration removed = llmConfigurations.remove(llmId);
        if (removed != null) {
            logger.info("LLM eliminado: {} - {} ({})", 
                       llmId, removed.getName(), removed.getModel());
        }
    }
    
    /**
     * Obtiene estadísticas de los LLMs configurados.
     */
    public Map<String, Object> getLlmStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long total = llmConfigurations.size();
        long enabled = llmConfigurations.values().stream()
                .filter(LlmConfiguration::isEnabled)
                .count();
        
        stats.put("total", total);
        stats.put("enabled", enabled);
        stats.put("disabled", total - enabled);
        
        // Agrupar por proveedor
        Map<LlmProvider, Long> byProvider = llmConfigurations.values().stream()
                .collect(Collectors.groupingBy(LlmConfiguration::getProvider, Collectors.counting()));
        stats.put("byProvider", byProvider);
        
        // Lista de LLMs habilitados
        List<String> enabledIds = llmConfigurations.values().stream()
                .filter(LlmConfiguration::isEnabled)
                .map(LlmConfiguration::getId)
                .collect(Collectors.toList());
        stats.put("enabledIds", enabledIds);
        
        return stats;
    }
    
    /**
     * Verifica la salud de un LLM específico.
     */
    public boolean isLlmHealthy(String llmId) {
        LlmConfiguration config = llmConfigurations.get(llmId);
        if (config == null || !config.isEnabled()) {
            return false;
        }
        
        // Aquí se podría implementar un health check real
        // Por ahora, solo verificamos que la configuración sea válida
        return config.getApiKey() != null && !config.getApiKey().trim().isEmpty();
    }
    
    /**
     * Obtiene el mejor LLM disponible basado en criterios de calidad.
     */
    public Optional<LlmConfiguration> getBestLlm() {
        return getEnabledLlmConfigurations().stream()
                .max(Comparator.comparing(LlmConfiguration::getWeight));
    }
} 