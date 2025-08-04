package com.intentmanagerms.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Servicio de inicialización para McpActionRegistry.
 * Se ejecuta al arrancar la aplicación para verificar que el registro se carga correctamente.
 */
@Service
public class McpActionRegistryInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(McpActionRegistryInitializationService.class);
    
    private final McpActionRegistry mcpActionRegistry;
    
    public McpActionRegistryInitializationService(McpActionRegistry mcpActionRegistry) {
        this.mcpActionRegistry = mcpActionRegistry;
    }
    
    /**
     * Se ejecuta cuando la aplicación está lista
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("🚀 Inicializando McpActionRegistry...");
        
        try {
            // Verificar que el registro se cargó correctamente
            var registry = mcpActionRegistry.getCurrentRegistry();
            if (registry == null) {
                logger.error("❌ No se pudo cargar el registro de acciones MCP");
                return;
            }
            
            // Obtener estadísticas
            var stats = mcpActionRegistry.getRegistryStatistics();
            
            logger.info("✅ McpActionRegistry inicializado exitosamente");
            logger.info("📊 Estadísticas del registro:");
            logger.info("   - Versión: {}", stats.get("version"));
            logger.info("   - Servicios: {} ({} habilitados)", 
                    stats.get("serviceCount"), stats.get("enabledServiceCount"));
            logger.info("   - Acciones: {} ({} habilitadas)", 
                    stats.get("totalActionCount"), stats.get("totalEnabledActionCount"));
            logger.info("   - Hot-reload: {}", stats.get("hotReloadEnabled"));
            logger.info("   - Intervalo recarga: {}s", stats.get("reloadIntervalSeconds"));
            
            // Mostrar servicios por método HTTP
            var actionsByMethod = mcpActionRegistry.getAllActionsByMethod();
            if (!actionsByMethod.isEmpty()) {
                logger.info("🌐 Acciones por método HTTP:");
                for (var entry : actionsByMethod.entrySet()) {
                    logger.info("   - {}: {} acciones", entry.getKey(), entry.getValue().size());
                }
            }
            
            // Verificar servicios críticos
            verifyCriticalServices();
            
            // Verificar acciones críticas
            verifyCriticalActions();
            
            logger.info("🎯 McpActionRegistry listo para procesar peticiones");
            
        } catch (Exception e) {
            logger.error("❌ Error durante la inicialización de McpActionRegistry", e);
        }
    }
    
    /**
     * Verifica que los servicios críticos estén configurados
     */
    private void verifyCriticalServices() {
        String[] criticalServices = {"weather-mcp", "smart-home-mcp", "system-mcp"};
        
        logger.info("🔍 Verificando servicios críticos...");
        
        for (String serviceId : criticalServices) {
            var service = mcpActionRegistry.getService(serviceId);
            if (service != null && service.isEnabled()) {
                logger.info("   ✅ {}: {} acciones habilitadas", serviceId, service.getEnabledActionCount());
            } else if (service != null) {
                logger.warn("   ⚠️  {}: DESHABILITADO", serviceId);
            } else {
                logger.warn("   ⚠️  {}: NO CONFIGURADO", serviceId);
            }
        }
    }
    
    /**
     * Verifica que las acciones críticas estén configuradas
     */
    private void verifyCriticalActions() {
        String[] criticalActions = {"consultar_tiempo", "smart_home_light_on", "programar_alarma"};
        
        logger.info("🔍 Verificando acciones críticas...");
        
        for (String actionId : criticalActions) {
            var action = mcpActionRegistry.getAction(actionId);
            if (action != null && action.isEnabled()) {
                var service = mcpActionRegistry.getServiceForAction(actionId);
                logger.info("   ✅ {}: {} (servicio: {})", 
                        actionId, action.getDescription(), 
                        service != null ? service.getName() : "desconocido");
            } else if (action != null) {
                logger.warn("   ⚠️  {}: DESHABILITADA", actionId);
            } else {
                logger.warn("   ⚠️  {}: NO CONFIGURADA", actionId);
            }
        }
    }
    
    /**
     * Verifica la salud del servicio
     */
    public boolean isHealthy() {
        try {
            var health = mcpActionRegistry.getHealthInfo();
            return "HEALTHY".equals(health.get("status"));
        } catch (Exception e) {
            logger.error("Error al verificar salud de McpActionRegistry", e);
            return false;
        }
    }
    
    /**
     * Obtiene información de diagnóstico
     */
    public String getDiagnosticInfo() {
        try {
            var stats = mcpActionRegistry.getRegistryStatistics();
            var health = mcpActionRegistry.getHealthInfo();
            
            return String.format(
                "McpActionRegistry - Status: %s, Services: %s, Actions: %s, HotReload: %s",
                health.get("status"),
                stats.get("enabledServiceCount"),
                stats.get("totalEnabledActionCount"),
                stats.get("hotReloadEnabled")
            );
        } catch (Exception e) {
            return "McpActionRegistry - Error: " + e.getMessage();
        }
    }
} 