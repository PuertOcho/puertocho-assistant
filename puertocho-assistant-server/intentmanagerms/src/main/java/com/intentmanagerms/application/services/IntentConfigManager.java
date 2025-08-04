package com.intentmanagerms.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.domain.model.IntentConfiguration;
import com.intentmanagerms.domain.model.IntentExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestionar la configuración de intenciones cargada desde JSON.
 * Soporta hot-reload y validación de configuraciones.
 */
@Service
public class IntentConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentConfigManager.class);
    
    @Value("${intent.config.file:classpath:config/intents.json}")
    private String configFilePath;
    
    @Value("${intent.config.hot-reload.enabled:true}")
    private boolean hotReloadEnabled;
    
    @Value("${intent.config.hot-reload.interval:30}")
    private int reloadIntervalSeconds;
    
    @Value("${intent.config.default-confidence-threshold:0.7}")
    private double defaultConfidenceThreshold;
    
    @Value("${intent.config.default-max-examples-for-rag:5}")
    private int defaultMaxExamplesForRag;
    
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    private IntentConfiguration currentConfiguration;
    private LocalDateTime lastLoadTime;
    private String lastFileHash;
    private final Map<String, Object> configurationCache = new ConcurrentHashMap<>();
    
    public IntentConfigManager(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Inicializa la configuración al arrancar el servicio
     */
    @PostConstruct
    public void initialize() {
        logger.info("Inicializando IntentConfigManager...");
        loadConfiguration();
        
        if (hotReloadEnabled) {
            logger.info("Hot-reload habilitado con intervalo de {} segundos", reloadIntervalSeconds);
        } else {
            logger.info("Hot-reload deshabilitado");
        }
    }
    
    /**
     * Carga la configuración desde el archivo JSON
     */
    public void loadConfiguration() {
        try {
            logger.info("Cargando configuración de intenciones desde: {}", configFilePath);
            
            Resource resource = resourceLoader.getResource(configFilePath);
            if (!resource.exists()) {
                logger.error("Archivo de configuración no encontrado: {}", configFilePath);
                return;
            }
            
            String content = new String(resource.getInputStream().readAllBytes());
            String currentHash = String.valueOf(content.hashCode());
            
            // Verificar si el archivo ha cambiado
            if (currentHash.equals(lastFileHash)) {
                logger.debug("Archivo de configuración no ha cambiado, omitiendo recarga");
                return;
            }
            
            IntentConfiguration newConfiguration = objectMapper.readValue(content, IntentConfiguration.class);
            
            // Aplicar configuraciones por defecto
            applyDefaultSettings(newConfiguration);
            
            // Validar la configuración
            validateConfiguration(newConfiguration);
            
            // Actualizar configuración actual
            this.currentConfiguration = newConfiguration;
            this.lastLoadTime = LocalDateTime.now();
            this.lastFileHash = currentHash;
            
            // Limpiar cache
            configurationCache.clear();
            
            logger.info("Configuración cargada exitosamente: {} intenciones, {} ejemplos totales",
                    newConfiguration.getIntentCount(),
                    newConfiguration.getTotalExampleCount());
            
        } catch (IOException e) {
            logger.error("Error al cargar configuración de intenciones", e);
        } catch (Exception e) {
            logger.error("Error inesperado al cargar configuración", e);
        }
    }
    
    /**
     * Aplica configuraciones por defecto a las intenciones
     */
    private void applyDefaultSettings(IntentConfiguration configuration) {
        if (configuration.getIntents() == null) {
            return;
        }
        
        for (IntentExample intent : configuration.getIntents().values()) {
            // Aplicar umbral de confianza por defecto si no está configurado
            if (intent.getConfidenceThreshold() == null) {
                intent.setConfidenceThreshold(defaultConfidenceThreshold);
            }
            
            // Aplicar máximo de ejemplos para RAG si no está configurado
            if (intent.getMaxExamplesForRag() == null) {
                intent.setMaxExamplesForRag(defaultMaxExamplesForRag);
            }
            
            // Aplicar dominio de experto por defecto si no está configurado
            if (intent.getExpertDomain() == null) {
                intent.setExpertDomain("general");
            }
        }
    }
    
    /**
     * Valida la configuración cargada
     */
    private void validateConfiguration(IntentConfiguration configuration) {
        if (configuration.getIntents() == null || configuration.getIntents().isEmpty()) {
            logger.warn("No se encontraron intenciones en la configuración");
            return;
        }
        
        int validIntents = 0;
        int totalExamples = 0;
        
        for (Map.Entry<String, IntentExample> entry : configuration.getIntents().entrySet()) {
            String intentId = entry.getKey();
            IntentExample intent = entry.getValue();
            
            if (intent == null) {
                logger.warn("Intención '{}' es null", intentId);
                continue;
            }
            
            // Validar ejemplos
            if (intent.getExamples() == null || intent.getExamples().isEmpty()) {
                logger.warn("Intención '{}' no tiene ejemplos", intentId);
                continue;
            }
            
            // Validar descripción
            if (intent.getDescription() == null || intent.getDescription().trim().isEmpty()) {
                logger.warn("Intención '{}' no tiene descripción", intentId);
            }
            
            // Validar acción MCP
            if (intent.getMcpAction() == null || intent.getMcpAction().trim().isEmpty()) {
                logger.warn("Intención '{}' no tiene acción MCP configurada", intentId);
            }
            
            validIntents++;
            totalExamples += intent.getExampleCount();
        }
        
        logger.info("Validación completada: {} intenciones válidas, {} ejemplos totales", 
                validIntents, totalExamples);
    }
    
    /**
     * Obtiene la configuración actual
     */
    public IntentConfiguration getCurrentConfiguration() {
        return currentConfiguration;
    }
    
    /**
     * Obtiene una intención específica por su ID
     */
    public IntentExample getIntent(String intentId) {
        if (currentConfiguration == null) {
            return null;
        }
        return currentConfiguration.getIntent(intentId);
    }
    
    /**
     * Verifica si existe una intención específica
     */
    public boolean hasIntent(String intentId) {
        if (currentConfiguration == null) {
            return false;
        }
        return currentConfiguration.hasIntent(intentId);
    }
    
    /**
     * Obtiene todas las intenciones disponibles
     */
    public Map<String, IntentExample> getAllIntents() {
        if (currentConfiguration == null) {
            return Map.of();
        }
        return currentConfiguration.getIntents();
    }
    
    /**
     * Obtiene intenciones por dominio de experto
     */
    public Map<String, java.util.List<IntentExample>> getIntentsByExpertDomain() {
        if (currentConfiguration == null) {
            return Map.of();
        }
        return currentConfiguration.getIntentsByExpertDomain();
    }
    
    /**
     * Obtiene todas las acciones MCP disponibles
     */
    public java.util.Set<String> getAvailableMcpActions() {
        if (currentConfiguration == null) {
            return java.util.Set.of();
        }
        return currentConfiguration.getAvailableMcpActions();
    }
    
    /**
     * Obtiene estadísticas de la configuración
     */
    public Map<String, Object> getConfigurationStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        if (currentConfiguration == null) {
            stats.put("status", "no_configuration_loaded");
            return stats;
        }
        
        stats.put("status", "loaded");
        stats.put("version", currentConfiguration.getVersion());
        stats.put("description", currentConfiguration.getDescription());
        stats.put("intentCount", currentConfiguration.getIntentCount());
        stats.put("totalExampleCount", currentConfiguration.getTotalExampleCount());
        stats.put("availableMcpActions", currentConfiguration.getAvailableMcpActions());
        stats.put("lastLoadTime", lastLoadTime);
        stats.put("hotReloadEnabled", hotReloadEnabled);
        stats.put("reloadIntervalSeconds", reloadIntervalSeconds);
        
        // Estadísticas por dominio de experto
        Map<String, java.util.List<IntentExample>> intentsByDomain = getIntentsByExpertDomain();
        Map<String, Integer> domainStats = new ConcurrentHashMap<>();
        for (Map.Entry<String, java.util.List<IntentExample>> entry : intentsByDomain.entrySet()) {
            domainStats.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("intentsByDomain", domainStats);
        
        return stats;
    }
    
    /**
     * Programa la recarga automática de configuración
     */
    @Scheduled(fixedDelayString = "${intent.config.hot-reload.interval:30}000")
    public void scheduledReload() {
        if (!hotReloadEnabled) {
            return;
        }
        
        try {
            loadConfiguration();
        } catch (Exception e) {
            logger.error("Error en recarga programada de configuración", e);
        }
    }
    
    /**
     * Fuerza una recarga manual de la configuración
     */
    public void forceReload() {
        logger.info("Forzando recarga manual de configuración");
        loadConfiguration();
    }
    
    /**
     * Obtiene información de salud del servicio
     */
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new ConcurrentHashMap<>();
        
        health.put("status", currentConfiguration != null ? "HEALTHY" : "UNHEALTHY");
        health.put("configurationLoaded", currentConfiguration != null);
        health.put("lastLoadTime", lastLoadTime);
        health.put("hotReloadEnabled", hotReloadEnabled);
        
        if (currentConfiguration != null) {
            health.put("intentCount", currentConfiguration.getIntentCount());
            health.put("totalExamples", currentConfiguration.getTotalExampleCount());
            health.put("version", currentConfiguration.getVersion());
        }
        
        return health;
    }
} 