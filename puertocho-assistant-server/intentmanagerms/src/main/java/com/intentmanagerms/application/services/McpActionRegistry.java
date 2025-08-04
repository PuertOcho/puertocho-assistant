package com.intentmanagerms.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentmanagerms.domain.model.McpAction;
import com.intentmanagerms.domain.model.McpRegistry;
import com.intentmanagerms.domain.model.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestionar el registro de acciones MCP configurables.
 * Soporta hot-reload y validación de configuraciones.
 */
@Service
public class McpActionRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(McpActionRegistry.class);
    
    @Value("${mcp.registry.file:classpath:config/mcp_registry.json}")
    private String registryFilePath;
    
    @Value("${mcp.registry.hot-reload.enabled:true}")
    private boolean hotReloadEnabled;
    
    @Value("${mcp.registry.hot-reload.interval:30}")
    private int reloadIntervalSeconds;
    
    @Value("${mcp.registry.default-timeout:30}")
    private int defaultTimeout;
    
    @Value("${mcp.registry.default-retry-attempts:3}")
    private int defaultRetryAttempts;
    
    @Value("${mcp.registry.default-health-check-interval:60}")
    private int defaultHealthCheckInterval;
    
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    private McpRegistry currentRegistry;
    private LocalDateTime lastLoadTime;
    private String lastFileHash;
    private final Map<String, Object> registryCache = new ConcurrentHashMap<>();
    
    public McpActionRegistry(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Inicializa el registro al arrancar el servicio
     */
    @PostConstruct
    public void initialize() {
        logger.info("🚀 Inicializando McpActionRegistry...");
        loadRegistry();
        
        if (hotReloadEnabled) {
            logger.info("Hot-reload habilitado con intervalo de {} segundos", reloadIntervalSeconds);
        } else {
            logger.info("Hot-reload deshabilitado");
        }
    }
    
    /**
     * Carga el registro desde el archivo JSON
     */
    public void loadRegistry() {
        try {
            logger.info("Cargando registro MCP desde: {}", registryFilePath);
            
            Resource resource = resourceLoader.getResource(registryFilePath);
            if (!resource.exists()) {
                logger.error("Archivo de registro MCP no encontrado: {}", registryFilePath);
                return;
            }
            
            String content = new String(resource.getInputStream().readAllBytes());
            String currentHash = String.valueOf(content.hashCode());
            
            // Verificar si el archivo ha cambiado
            if (currentHash.equals(lastFileHash)) {
                logger.debug("Archivo de registro MCP no ha cambiado, omitiendo recarga");
                return;
            }
            
            McpRegistry newRegistry = objectMapper.readValue(content, McpRegistry.class);
            
            // Aplicar configuraciones por defecto
            applyDefaultSettings(newRegistry);
            
            // Validar el registro
            validateRegistry(newRegistry);
            
            // Actualizar registro actual
            this.currentRegistry = newRegistry;
            this.lastLoadTime = LocalDateTime.now();
            this.lastFileHash = currentHash;
            
            // Limpiar cache
            registryCache.clear();
            
            logger.info("Registro MCP cargado exitosamente: {} servicios, {} acciones totales",
                    newRegistry.getServiceCount(),
                    newRegistry.getTotalActionCount());
            
        } catch (IOException e) {
            logger.error("Error al cargar registro MCP", e);
        } catch (Exception e) {
            logger.error("Error inesperado al cargar registro MCP", e);
        }
    }
    
    /**
     * Aplica configuraciones por defecto al registro
     */
    private void applyDefaultSettings(McpRegistry registry) {
        if (registry.getServices() == null) {
            return;
        }
        
        for (McpService service : registry.getServices().values()) {
            // Aplicar timeout por defecto si no está configurado
            if (service.getTimeout() == null) {
                service.setTimeout(defaultTimeout);
            }
            
            // Aplicar reintentos por defecto si no está configurado
            if (service.getRetryAttempts() == null) {
                service.setRetryAttempts(defaultRetryAttempts);
            }
            
            // Aplicar intervalo de health check por defecto si no está configurado
            if (service.getHealthCheckInterval() == null) {
                service.setHealthCheckInterval(defaultHealthCheckInterval);
            }
            
            // Aplicar configuraciones por defecto a las acciones
            if (service.getActions() != null) {
                for (McpAction action : service.getActions().values()) {
                    // Aplicar timeout por defecto si no está configurado
                    if (action.getTimeout() == null) {
                        action.setTimeout(service.getTimeout());
                    }
                    
                    // Aplicar reintentos por defecto si no está configurado
                    if (action.getRetryAttempts() == null) {
                        action.setRetryAttempts(service.getRetryAttempts());
                    }
                    
                    // Habilitar acción por defecto si no está configurado
                    if (action.getEnabled() == null) {
                        action.setEnabled(true);
                    }
                }
            }
        }
    }
    
    /**
     * Valida el registro cargado
     */
    private void validateRegistry(McpRegistry registry) {
        if (registry.getServices() == null || registry.getServices().isEmpty()) {
            logger.warn("No se encontraron servicios en el registro MCP");
            return;
        }
        
        int validServices = 0;
        int validActions = 0;
        
        for (Map.Entry<String, McpService> entry : registry.getServices().entrySet()) {
            String serviceId = entry.getKey();
            McpService service = entry.getValue();
            
            if (service == null) {
                logger.warn("Servicio '{}' es null", serviceId);
                continue;
            }
            
            // Validar URL del servicio
            if (service.getUrl() == null || service.getUrl().trim().isEmpty()) {
                logger.warn("Servicio '{}' no tiene URL configurada", serviceId);
            }
            
            // Validar nombre del servicio
            if (service.getName() == null || service.getName().trim().isEmpty()) {
                logger.warn("Servicio '{}' no tiene nombre configurado", serviceId);
            }
            
            // Validar acciones del servicio
            if (service.getActions() == null || service.getActions().isEmpty()) {
                logger.warn("Servicio '{}' no tiene acciones configuradas", serviceId);
            } else {
                for (Map.Entry<String, McpAction> actionEntry : service.getActions().entrySet()) {
                    String actionId = actionEntry.getKey();
                    McpAction action = actionEntry.getValue();
                    
                    if (action == null) {
                        logger.warn("Acción '{}' del servicio '{}' es null", actionId, serviceId);
                        continue;
                    }
                    
                    // Validar endpoint de la acción
                    if (action.getEndpoint() == null || action.getEndpoint().trim().isEmpty()) {
                        logger.warn("Acción '{}' del servicio '{}' no tiene endpoint configurado", actionId, serviceId);
                    }
                    
                    // Validar método HTTP de la acción
                    if (action.getMethod() == null || action.getMethod().trim().isEmpty()) {
                        logger.warn("Acción '{}' del servicio '{}' no tiene método HTTP configurado", actionId, serviceId);
                    }
                    
                    // Validar descripción de la acción
                    if (action.getDescription() == null || action.getDescription().trim().isEmpty()) {
                        logger.warn("Acción '{}' del servicio '{}' no tiene descripción configurada", actionId, serviceId);
                    }
                    
                    validActions++;
                }
            }
            
            validServices++;
        }
        
        logger.info("Validación completada: {} servicios válidos, {} acciones válidas", 
                validServices, validActions);
    }
    
    /**
     * Obtiene el registro actual
     */
    public McpRegistry getCurrentRegistry() {
        return currentRegistry;
    }
    
    /**
     * Obtiene un servicio específico por su ID
     */
    public McpService getService(String serviceId) {
        if (currentRegistry == null) {
            return null;
        }
        return currentRegistry.getService(serviceId);
    }
    
    /**
     * Verifica si existe un servicio específico
     */
    public boolean hasService(String serviceId) {
        if (currentRegistry == null) {
            return false;
        }
        return currentRegistry.hasService(serviceId);
    }
    
    /**
     * Obtiene una acción específica por su ID
     */
    public McpAction getAction(String actionId) {
        if (currentRegistry == null) {
            return null;
        }
        return currentRegistry.getAction(actionId);
    }
    
    /**
     * Verifica si existe una acción específica
     */
    public boolean hasAction(String actionId) {
        if (currentRegistry == null) {
            return false;
        }
        return currentRegistry.hasAction(actionId);
    }
    
    /**
     * Obtiene el servicio que contiene una acción específica
     */
    public McpService getServiceForAction(String actionId) {
        if (currentRegistry == null) {
            return null;
        }
        return currentRegistry.getServiceForAction(actionId);
    }
    
    /**
     * Obtiene todos los servicios disponibles
     */
    public Map<String, McpService> getAllServices() {
        if (currentRegistry == null) {
            return Map.of();
        }
        return currentRegistry.getServices();
    }
    
    /**
     * Obtiene todos los servicios habilitados
     */
    public Map<String, McpService> getEnabledServices() {
        if (currentRegistry == null) {
            return Map.of();
        }
        return currentRegistry.getEnabledServices();
    }
    
    /**
     * Obtiene todas las acciones disponibles
     */
    public Map<String, McpAction> getAllActions() {
        if (currentRegistry == null) {
            return Map.of();
        }
        return currentRegistry.getAllActions();
    }
    
    /**
     * Obtiene todas las acciones por método HTTP
     */
    public Map<String, java.util.List<McpAction>> getAllActionsByMethod() {
        if (currentRegistry == null) {
            return Map.of();
        }
        return currentRegistry.getAllActionsByMethod();
    }
    
    /**
     * Obtiene estadísticas del registro
     */
    public Map<String, Object> getRegistryStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        if (currentRegistry == null) {
            stats.put("status", "no_registry_loaded");
            return stats;
        }
        
        stats.put("status", "loaded");
        stats.put("version", currentRegistry.getVersion());
        stats.put("description", currentRegistry.getDescription());
        stats.put("serviceCount", currentRegistry.getServiceCount());
        stats.put("enabledServiceCount", currentRegistry.getEnabledServiceCount());
        stats.put("totalActionCount", currentRegistry.getTotalActionCount());
        stats.put("totalEnabledActionCount", currentRegistry.getTotalEnabledActionCount());
        stats.put("lastLoadTime", lastLoadTime);
        stats.put("hotReloadEnabled", hotReloadEnabled);
        stats.put("reloadIntervalSeconds", reloadIntervalSeconds);
        
        // Estadísticas por método HTTP
        Map<String, java.util.List<McpAction>> actionsByMethod = getAllActionsByMethod();
        Map<String, Integer> methodStats = new ConcurrentHashMap<>();
        for (Map.Entry<String, java.util.List<McpAction>> entry : actionsByMethod.entrySet()) {
            methodStats.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("actionsByMethod", methodStats);
        
        return stats;
    }
    
    /**
     * Obtiene una respuesta de fallback específica
     */
    public String getFallbackResponse(String responseType) {
        if (currentRegistry == null) {
            return "Ha ocurrido un error inesperado.";
        }
        return currentRegistry.getFallbackResponse(responseType);
    }
    
    /**
     * Obtiene una respuesta de fallback formateada
     */
    public String getFormattedFallbackResponse(String responseType, String... params) {
        if (currentRegistry == null) {
            return "Ha ocurrido un error inesperado.";
        }
        return currentRegistry.getFormattedFallbackResponse(responseType, params);
    }
    
    /**
     * Programa la recarga automática del registro
     */
    @Scheduled(fixedDelayString = "${mcp.registry.hot-reload.interval:30}000")
    public void scheduledReload() {
        if (!hotReloadEnabled) {
            return;
        }
        
        try {
            loadRegistry();
        } catch (Exception e) {
            logger.error("Error en recarga programada del registro MCP", e);
        }
    }
    
    /**
     * Fuerza una recarga manual del registro
     */
    public void forceReload() {
        logger.info("Forzando recarga manual del registro MCP");
        loadRegistry();
    }
    
    /**
     * Obtiene información de salud del servicio
     */
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new ConcurrentHashMap<>();
        
        health.put("status", currentRegistry != null ? "HEALTHY" : "UNHEALTHY");
        health.put("registryLoaded", currentRegistry != null);
        health.put("lastLoadTime", lastLoadTime);
        health.put("hotReloadEnabled", hotReloadEnabled);
        
        if (currentRegistry != null) {
            health.put("serviceCount", currentRegistry.getServiceCount());
            health.put("enabledServiceCount", currentRegistry.getEnabledServiceCount());
            health.put("totalActionCount", currentRegistry.getTotalActionCount());
            health.put("version", currentRegistry.getVersion());
        }
        
        return health;
    }
} 