package com.intentmanagerms.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Servicio de inicialización para IntentConfigManager.
 * Se ejecuta al arrancar la aplicación para verificar que la configuración se carga correctamente.
 */
@Service
public class IntentConfigInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentConfigInitializationService.class);
    
    private final IntentConfigManager intentConfigManager;
    
    public IntentConfigInitializationService(IntentConfigManager intentConfigManager) {
        this.intentConfigManager = intentConfigManager;
    }
    
    /**
     * Se ejecuta cuando la aplicación está lista
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("🚀 Inicializando IntentConfigManager...");
        
        try {
            // Verificar que la configuración se cargó correctamente
            var config = intentConfigManager.getCurrentConfiguration();
            if (config == null) {
                logger.error("❌ No se pudo cargar la configuración de intenciones");
                return;
            }
            
            // Obtener estadísticas
            var stats = intentConfigManager.getConfigurationStatistics();
            
            logger.info("✅ IntentConfigManager inicializado exitosamente");
            logger.info("📊 Estadísticas de configuración:");
            logger.info("   - Versión: {}", stats.get("version"));
            logger.info("   - Intenciones: {}", stats.get("intentCount"));
            logger.info("   - Ejemplos totales: {}", stats.get("totalExampleCount"));
            logger.info("   - Acciones MCP: {}", stats.get("availableMcpActions"));
            logger.info("   - Hot-reload: {}", stats.get("hotReloadEnabled"));
            logger.info("   - Intervalo recarga: {}s", stats.get("reloadIntervalSeconds"));
            
            // Mostrar intenciones por dominio
            var intentsByDomain = intentConfigManager.getIntentsByExpertDomain();
            if (!intentsByDomain.isEmpty()) {
                logger.info("🏷️  Intenciones por dominio:");
                for (var entry : intentsByDomain.entrySet()) {
                    logger.info("   - {}: {} intenciones", entry.getKey(), entry.getValue().size());
                }
            }
            
            // Verificar intenciones críticas
            verifyCriticalIntents();
            
            logger.info("🎯 IntentConfigManager listo para procesar peticiones");
            
        } catch (Exception e) {
            logger.error("❌ Error durante la inicialización de IntentConfigManager", e);
        }
    }
    
    /**
     * Verifica que las intenciones críticas estén configuradas
     */
    private void verifyCriticalIntents() {
        String[] criticalIntents = {"ayuda", "saludo", "despedida"};
        
        logger.info("🔍 Verificando intenciones críticas...");
        
        for (String intentId : criticalIntents) {
            var intent = intentConfigManager.getIntent(intentId);
            if (intent != null) {
                logger.info("   ✅ {}: {} ejemplos", intentId, intent.getExampleCount());
            } else {
                logger.warn("   ⚠️  {}: NO CONFIGURADA", intentId);
            }
        }
    }
    
    /**
     * Verifica la salud del servicio
     */
    public boolean isHealthy() {
        try {
            var health = intentConfigManager.getHealthInfo();
            return "HEALTHY".equals(health.get("status"));
        } catch (Exception e) {
            logger.error("Error al verificar salud de IntentConfigManager", e);
            return false;
        }
    }
    
    /**
     * Obtiene información de diagnóstico
     */
    public String getDiagnosticInfo() {
        try {
            var stats = intentConfigManager.getConfigurationStatistics();
            var health = intentConfigManager.getHealthInfo();
            
            return String.format(
                "IntentConfigManager - Status: %s, Intents: %s, Examples: %s, HotReload: %s",
                health.get("status"),
                stats.get("intentCount"),
                stats.get("totalExampleCount"),
                stats.get("hotReloadEnabled")
            );
        } catch (Exception e) {
            return "IntentConfigManager - Error: " + e.getMessage();
        }
    }
} 