package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.LlmConfigurationService;
import com.intentmanagerms.domain.model.LlmConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para gestionar configuraciones de LLM.
 * Proporciona endpoints para consultar, crear, actualizar y eliminar configuraciones.
 */
@RestController
@RequestMapping("/api/v1/llm-config")
public class LlmConfigurationController {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmConfigurationController.class);
    
    @Autowired
    private LlmConfigurationService llmConfigurationService;
    
    /**
     * Obtiene todas las configuraciones de LLM.
     */
    @GetMapping
    public ResponseEntity<List<LlmConfiguration>> getAllLlmConfigurations() {
        logger.info("GET /api/v1/llm-config - Obteniendo todas las configuraciones");
        
        List<LlmConfiguration> configs = llmConfigurationService.getEnabledLlmConfigurations();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * Obtiene una configuración de LLM por ID.
     */
    @GetMapping("/{llmId}")
    public ResponseEntity<LlmConfiguration> getLlmConfiguration(@PathVariable String llmId) {
        logger.info("GET /api/v1/llm-config/{} - Obteniendo configuración", llmId);
        
        Optional<LlmConfiguration> config = llmConfigurationService.getLlmConfiguration(llmId);
        
        if (config.isPresent()) {
            return ResponseEntity.ok(config.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtiene el LLM primario.
     */
    @GetMapping("/primary")
    public ResponseEntity<LlmConfiguration> getPrimaryLlm() {
        logger.info("GET /api/v1/llm-config/primary - Obteniendo LLM primario");
        
        Optional<LlmConfiguration> primary = llmConfigurationService.getPrimaryLlm();
        
        if (primary.isPresent()) {
            return ResponseEntity.ok(primary.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtiene el LLM de fallback.
     */
    @GetMapping("/fallback")
    public ResponseEntity<LlmConfiguration> getFallbackLlm() {
        logger.info("GET /api/v1/llm-config/fallback - Obteniendo LLM de fallback");
        
        Optional<LlmConfiguration> fallback = llmConfigurationService.getFallbackLlm();
        
        if (fallback.isPresent()) {
            return ResponseEntity.ok(fallback.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtiene estadísticas de los LLMs configurados.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLlmStatistics() {
        logger.info("GET /api/v1/llm-config/statistics - Obteniendo estadísticas");
        
        Map<String, Object> stats = llmConfigurationService.getLlmStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Crea una nueva configuración de LLM.
     */
    @PostMapping
    public ResponseEntity<LlmConfiguration> createLlmConfiguration(@RequestBody LlmConfiguration configuration) {
        logger.info("POST /api/v1/llm-config - Creando nueva configuración: {}", configuration.getId());
        
        try {
            llmConfigurationService.addLlmConfiguration(configuration);
            return ResponseEntity.ok(configuration);
        } catch (IllegalArgumentException e) {
            logger.error("Error al crear configuración de LLM: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error inesperado al crear configuración de LLM", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Actualiza una configuración de LLM existente.
     */
    @PutMapping("/{llmId}")
    public ResponseEntity<LlmConfiguration> updateLlmConfiguration(
            @PathVariable String llmId, 
            @RequestBody LlmConfiguration configuration) {
        logger.info("PUT /api/v1/llm-config/{} - Actualizando configuración", llmId);
        
        try {
            llmConfigurationService.updateLlmConfiguration(llmId, configuration);
            return ResponseEntity.ok(configuration);
        } catch (IllegalArgumentException e) {
            logger.error("Error al actualizar configuración de LLM: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error inesperado al actualizar configuración de LLM", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Habilita o deshabilita un LLM.
     */
    @PatchMapping("/{llmId}/enabled")
    public ResponseEntity<Void> setLlmEnabled(
            @PathVariable String llmId, 
            @RequestParam boolean enabled) {
        logger.info("PATCH /api/v1/llm-config/{}/enabled - Estableciendo enabled={}", llmId, enabled);
        
        try {
            llmConfigurationService.setLlmEnabled(llmId, enabled);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("Error al cambiar estado de LLM: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error inesperado al cambiar estado de LLM", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Elimina una configuración de LLM.
     */
    @DeleteMapping("/{llmId}")
    public ResponseEntity<Void> deleteLlmConfiguration(@PathVariable String llmId) {
        logger.info("DELETE /api/v1/llm-config/{} - Eliminando configuración", llmId);
        
        try {
            llmConfigurationService.removeLlmConfiguration(llmId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error al eliminar configuración de LLM", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Verifica la salud de un LLM específico.
     */
    @GetMapping("/{llmId}/health")
    public ResponseEntity<Map<String, Object>> checkLlmHealth(@PathVariable String llmId) {
        logger.info("GET /api/v1/llm-config/{}/health - Verificando salud", llmId);
        
        boolean isHealthy = llmConfigurationService.isLlmHealthy(llmId);
        
        Map<String, Object> healthInfo = Map.of(
            "llmId", llmId,
            "healthy", isHealthy,
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(healthInfo);
    }
    
    /**
     * Obtiene el mejor LLM disponible.
     */
    @GetMapping("/best")
    public ResponseEntity<LlmConfiguration> getBestLlm() {
        logger.info("GET /api/v1/llm-config/best - Obteniendo mejor LLM");
        
        Optional<LlmConfiguration> best = llmConfigurationService.getBestLlm();
        
        if (best.isPresent()) {
            return ResponseEntity.ok(best.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtiene LLMs para el sistema de votación MoE.
     */
    @GetMapping("/voting")
    public ResponseEntity<List<LlmConfiguration>> getLlmForVoting(
            @RequestParam(defaultValue = "3") int count) {
        logger.info("GET /api/v1/llm-config/voting - Obteniendo {} LLMs para votación", count);
        
        List<LlmConfiguration> votingLlm = llmConfigurationService.getLlmForVoting(count);
        return ResponseEntity.ok(votingLlm);
    }
} 