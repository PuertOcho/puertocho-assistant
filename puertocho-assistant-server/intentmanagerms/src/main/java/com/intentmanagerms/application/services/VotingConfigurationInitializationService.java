package com.intentmanagerms.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.domain.model.VotingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Servicio para inicializar y gestionar la configuración del sistema de votación MoE.
 * Carga la configuración desde archivo JSON y soporta hot-reload.
 */
@Service
public class VotingConfigurationInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(VotingConfigurationInitializationService.class);
    
    @Autowired
    private LlmVotingService llmVotingService;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${moe.configuration.file:classpath:config/moe_voting.json}")
    private String configurationFilePath;
    
    @Value("${moe.configuration.hot-reload.enabled:true}")
    private boolean hotReloadEnabled;
    
    @Value("${moe.configuration.hot-reload.interval:30}")
    private int reloadIntervalSeconds;
    
    private VotingConfiguration currentConfiguration;
    private LocalDateTime lastLoadTime;
    private String lastFileHash;
    
    /**
     * Inicializa la configuración al arrancar la aplicación
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        logger.info("🚀 Inicializando VotingConfigurationInitializationService...");
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
            logger.info("Cargando configuración de votación MoE desde: {}", configurationFilePath);
            
            Resource resource = resourceLoader.getResource(configurationFilePath);
            if (!resource.exists()) {
                logger.error("Archivo de configuración MoE no encontrado: {}", configurationFilePath);
                return;
            }
            
            String content = new String(resource.getInputStream().readAllBytes());
            String currentHash = String.valueOf(content.hashCode());
            
            // Verificar si el archivo ha cambiado
            if (currentHash.equals(lastFileHash)) {
                logger.debug("Archivo de configuración MoE no ha cambiado, omitiendo recarga");
                return;
            }
            
            VotingConfiguration newConfiguration = objectMapper.readValue(content, VotingConfiguration.class);
            
            // Validar configuración
            validateConfiguration(newConfiguration);
            
            // Aplicar configuración
            currentConfiguration = newConfiguration;
            lastFileHash = currentHash;
            lastLoadTime = LocalDateTime.now();
            
            // Inicializar servicio de votación
            llmVotingService.initializeVotingService(currentConfiguration);
            
            logger.info("✅ Configuración de votación MoE cargada exitosamente");
            logger.info("   - Versión: {}", currentConfiguration.getVersion());
            logger.info("   - Descripción: {}", currentConfiguration.getDescription());
            logger.info("   - Sistema habilitado: {}", currentConfiguration.getVotingSystem().isEnabled());
            logger.info("   - Participantes LLM: {}", 
                       currentConfiguration.getVotingSystem().getLlmParticipants().size());
            
        } catch (IOException e) {
            logger.error("❌ Error cargando configuración de votación MoE: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("❌ Error procesando configuración de votación MoE: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Valida la configuración cargada
     */
    private void validateConfiguration(VotingConfiguration configuration) {
        logger.info("Validando configuración de votación MoE...");
        
        if (configuration == null) {
            throw new IllegalArgumentException("Configuración de votación no puede ser null");
        }
        
        if (configuration.getVotingSystem() == null) {
            throw new IllegalArgumentException("Sistema de votación no puede ser null");
        }
        
        if (configuration.getVotingSystem().getLlmParticipants() == null || 
            configuration.getVotingSystem().getLlmParticipants().isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos un participante LLM");
        }
        
        // Validar participantes
        for (VotingConfiguration.LlmParticipant participant : configuration.getVotingSystem().getLlmParticipants()) {
            if (participant.getId() == null || participant.getId().trim().isEmpty()) {
                throw new IllegalArgumentException("ID de participante no puede estar vacío");
            }
            
            if (participant.getName() == null || participant.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Nombre de participante no puede estar vacío");
            }
            
            if (participant.getProvider() == null || participant.getProvider().trim().isEmpty()) {
                throw new IllegalArgumentException("Provider de participante no puede estar vacío");
            }
            
            if (participant.getModel() == null || participant.getModel().trim().isEmpty()) {
                throw new IllegalArgumentException("Modelo de participante no puede estar vacío");
            }
            
            if (participant.getWeight() <= 0) {
                throw new IllegalArgumentException("Peso de participante debe ser mayor que 0");
            }
        }
        
        // Validar reglas de consenso
        if (configuration.getVotingSystem().getConsensusRules() != null) {
            VotingConfiguration.ConsensusRules rules = configuration.getVotingSystem().getConsensusRules();
            
            if (rules.getMajorityThreshold() <= 0) {
                throw new IllegalArgumentException("Umbral de mayoría debe ser mayor que 0");
            }
            
            if (rules.getConfidenceWeight() < 0 || rules.getConfidenceWeight() > 1) {
                throw new IllegalArgumentException("Peso de confianza debe estar entre 0 y 1");
            }
            
            if (rules.getIntentAgreementWeight() < 0 || rules.getIntentAgreementWeight() > 1) {
                throw new IllegalArgumentException("Peso de acuerdo de intención debe estar entre 0 y 1");
            }
        }
        
        logger.info("✅ Configuración de votación MoE validada exitosamente");
    }
    
    /**
     * Obtiene la configuración actual
     */
    public VotingConfiguration getCurrentConfiguration() {
        return currentConfiguration;
    }
    
    /**
     * Verifica si la configuración está cargada
     */
    public boolean isConfigurationLoaded() {
        return currentConfiguration != null;
    }
    
    /**
     * Obtiene información de salud del servicio
     */
    public boolean isHealthy() {
        try {
            return currentConfiguration != null && 
                   currentConfiguration.getVotingSystem() != null &&
                   !currentConfiguration.getVotingSystem().getLlmParticipants().isEmpty();
        } catch (Exception e) {
            logger.error("Error verificando salud del servicio de configuración: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene estadísticas del servicio
     */
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        stats.put("configuration_loaded", isConfigurationLoaded());
        stats.put("hot_reload_enabled", hotReloadEnabled);
        stats.put("reload_interval_seconds", reloadIntervalSeconds);
        stats.put("last_load_time", lastLoadTime != null ? lastLoadTime.toString() : "N/A");
        stats.put("configuration_file", configurationFilePath);
        
        if (currentConfiguration != null) {
            stats.put("configuration_version", currentConfiguration.getVersion());
            stats.put("voting_system_enabled", currentConfiguration.getVotingSystem().isEnabled());
            stats.put("llm_participants_count", currentConfiguration.getVotingSystem().getLlmParticipants().size());
            stats.put("max_debate_rounds", currentConfiguration.getVotingSystem().getMaxDebateRounds());
            stats.put("consensus_threshold", currentConfiguration.getVotingSystem().getConsensusThreshold());
            stats.put("parallel_voting", currentConfiguration.getVotingSystem().isParallelVoting());
        }
        
        return stats;
    }
    
    /**
     * Recarga forzada de la configuración
     */
    public void forceReload() {
        logger.info("Forzando recarga de configuración de votación MoE...");
        lastFileHash = null; // Forzar recarga
        loadConfiguration();
    }
    
    /**
     * Recarga programada de la configuración
     */
    @Scheduled(fixedDelayString = "${moe.configuration.hot-reload.interval:30}000")
    public void scheduledReload() {
        if (hotReloadEnabled) {
            logger.debug("Ejecutando recarga programada de configuración de votación MoE...");
            loadConfiguration();
        }
    }
} 