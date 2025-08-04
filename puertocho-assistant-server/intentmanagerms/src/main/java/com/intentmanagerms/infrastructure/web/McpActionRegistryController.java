package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.McpActionRegistry;
import com.intentmanagerms.domain.model.McpAction;
import com.intentmanagerms.domain.model.McpRegistry;
import com.intentmanagerms.domain.model.McpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestionar el registro de acciones MCP
 */
@RestController
@RequestMapping("/api/v1/mcp-registry")
@Tag(name = "MCP Action Registry", description = "Gestión del registro de acciones MCP configurables")
public class McpActionRegistryController {
    
    private final McpActionRegistry mcpActionRegistry;
    
    public McpActionRegistryController(McpActionRegistry mcpActionRegistry) {
        this.mcpActionRegistry = mcpActionRegistry;
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Obtener estadísticas del registro", 
               description = "Retorna estadísticas completas del registro de acciones MCP")
    public ResponseEntity<Map<String, Object>> getRegistryStatistics() {
        Map<String, Object> stats = mcpActionRegistry.getRegistryStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check del registro", 
               description = "Verifica el estado de salud del registro de acciones MCP")
    public ResponseEntity<Map<String, Object>> getHealthInfo() {
        Map<String, Object> health = mcpActionRegistry.getHealthInfo();
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/services")
    @Operation(summary = "Obtener todos los servicios", 
               description = "Retorna todos los servicios MCP configurados")
    public ResponseEntity<Map<String, McpService>> getAllServices() {
        Map<String, McpService> services = mcpActionRegistry.getAllServices();
        return ResponseEntity.ok(services);
    }
    
    @GetMapping("/services/enabled")
    @Operation(summary = "Obtener servicios habilitados", 
               description = "Retorna solo los servicios MCP habilitados")
    public ResponseEntity<Map<String, McpService>> getEnabledServices() {
        Map<String, McpService> services = mcpActionRegistry.getEnabledServices();
        return ResponseEntity.ok(services);
    }
    
    @GetMapping("/services/{serviceId}")
    @Operation(summary = "Obtener servicio específico", 
               description = "Retorna un servicio MCP específico por su ID")
    public ResponseEntity<McpService> getService(
            @Parameter(description = "ID del servicio") 
            @PathVariable String serviceId) {
        
        McpService service = mcpActionRegistry.getService(serviceId);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service);
    }
    
    @GetMapping("/services/{serviceId}/actions")
    @Operation(summary = "Obtener acciones de un servicio", 
               description = "Retorna todas las acciones de un servicio MCP específico")
    public ResponseEntity<Map<String, McpAction>> getServiceActions(
            @Parameter(description = "ID del servicio") 
            @PathVariable String serviceId) {
        
        McpService service = mcpActionRegistry.getService(serviceId);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, McpAction> actions = service.getActions();
        if (actions == null) {
            return ResponseEntity.ok(Map.of());
        }
        
        return ResponseEntity.ok(actions);
    }
    
    @GetMapping("/services/{serviceId}/actions/enabled")
    @Operation(summary = "Obtener acciones habilitadas de un servicio", 
               description = "Retorna solo las acciones habilitadas de un servicio MCP específico")
    public ResponseEntity<Map<String, McpAction>> getEnabledServiceActions(
            @Parameter(description = "ID del servicio") 
            @PathVariable String serviceId) {
        
        McpService service = mcpActionRegistry.getService(serviceId);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, McpAction> actions = service.getEnabledActions();
        return ResponseEntity.ok(actions);
    }
    
    @GetMapping("/actions")
    @Operation(summary = "Obtener todas las acciones", 
               description = "Retorna todas las acciones MCP disponibles")
    public ResponseEntity<Map<String, McpAction>> getAllActions() {
        Map<String, McpAction> actions = mcpActionRegistry.getAllActions();
        return ResponseEntity.ok(actions);
    }
    
    @GetMapping("/actions/{actionId}")
    @Operation(summary = "Obtener acción específica", 
               description = "Retorna una acción MCP específica por su ID")
    public ResponseEntity<McpAction> getAction(
            @Parameter(description = "ID de la acción") 
            @PathVariable String actionId) {
        
        McpAction action = mcpActionRegistry.getAction(actionId);
        if (action == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(action);
    }
    
    @GetMapping("/actions/{actionId}/service")
    @Operation(summary = "Obtener servicio de una acción", 
               description = "Retorna el servicio que contiene una acción MCP específica")
    public ResponseEntity<McpService> getServiceForAction(
            @Parameter(description = "ID de la acción") 
            @PathVariable String actionId) {
        
        McpService service = mcpActionRegistry.getServiceForAction(actionId);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service);
    }
    
    @GetMapping("/actions/methods")
    @Operation(summary = "Obtener acciones por método HTTP", 
               description = "Retorna todas las acciones agrupadas por método HTTP")
    public ResponseEntity<Map<String, List<McpAction>>> getActionsByMethod() {
        Map<String, List<McpAction>> actionsByMethod = mcpActionRegistry.getAllActionsByMethod();
        return ResponseEntity.ok(actionsByMethod);
    }
    
    @GetMapping("/actions/methods/{method}")
    @Operation(summary = "Obtener acciones por método específico", 
               description = "Retorna todas las acciones de un método HTTP específico")
    public ResponseEntity<List<McpAction>> getActionsBySpecificMethod(
            @Parameter(description = "Método HTTP") 
            @PathVariable String method) {
        
        Map<String, List<McpAction>> actionsByMethod = mcpActionRegistry.getAllActionsByMethod();
        List<McpAction> actions = actionsByMethod.get(method.toUpperCase());
        
        if (actions == null) {
            return ResponseEntity.ok(List.of());
        }
        
        return ResponseEntity.ok(actions);
    }
    
    @GetMapping("/actions/search")
    @Operation(summary = "Buscar acciones", 
               description = "Busca acciones que contengan el texto especificado en descripción o endpoint")
    public ResponseEntity<Map<String, McpAction>> searchActions(
            @Parameter(description = "Texto a buscar") 
            @RequestParam String query) {
        
        Map<String, McpAction> allActions = mcpActionRegistry.getAllActions();
        Map<String, McpAction> filteredActions = new java.util.HashMap<>();
        
        String lowerQuery = query.toLowerCase();
        
        for (Map.Entry<String, McpAction> entry : allActions.entrySet()) {
            McpAction action = entry.getValue();
            
            // Buscar en descripción
            if (action.getDescription() != null && 
                action.getDescription().toLowerCase().contains(lowerQuery)) {
                filteredActions.put(entry.getKey(), action);
                continue;
            }
            
            // Buscar en endpoint
            if (action.getEndpoint() != null && 
                action.getEndpoint().toLowerCase().contains(lowerQuery)) {
                filteredActions.put(entry.getKey(), action);
            }
        }
        
        return ResponseEntity.ok(filteredActions);
    }
    
    @GetMapping("/actions/{actionId}/params")
    @Operation(summary = "Obtener parámetros de una acción", 
               description = "Retorna los parámetros requeridos y opcionales de una acción MCP")
    public ResponseEntity<Map<String, Object>> getActionParams(
            @Parameter(description = "ID de la acción") 
            @PathVariable String actionId) {
        
        McpAction action = mcpActionRegistry.getAction(actionId);
        if (action == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> params = Map.of(
                "required", action.getRequiredParams() != null ? action.getRequiredParams() : List.of(),
                "optional", action.getOptionalParams() != null ? action.getOptionalParams() : List.of(),
                "all", action.getAllParams()
        );
        
        return ResponseEntity.ok(params);
    }
    
    @GetMapping("/fallback-responses")
    @Operation(summary = "Obtener respuestas de fallback", 
               description = "Retorna todas las respuestas de fallback configuradas")
    public ResponseEntity<Map<String, String>> getFallbackResponses() {
        McpRegistry registry = mcpActionRegistry.getCurrentRegistry();
        if (registry == null) {
            return ResponseEntity.ok(Map.of());
        }
        
        Map<String, String> responses = registry.getFallbackResponses();
        if (responses == null) {
            return ResponseEntity.ok(Map.of());
        }
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/fallback-responses/{responseType}")
    @Operation(summary = "Obtener respuesta de fallback específica", 
               description = "Retorna una respuesta de fallback específica")
    public ResponseEntity<Map<String, String>> getFallbackResponse(
            @Parameter(description = "Tipo de respuesta") 
            @PathVariable String responseType) {
        
        String response = mcpActionRegistry.getFallbackResponse(responseType);
        return ResponseEntity.ok(Map.of("response", response));
    }
    
    @PostMapping("/reload")
    @Operation(summary = "Recargar registro", 
               description = "Fuerza una recarga manual del registro MCP desde el archivo JSON")
    public ResponseEntity<Map<String, Object>> reloadRegistry() {
        mcpActionRegistry.forceReload();
        
        Map<String, Object> response = Map.of(
                "message", "Registro MCP recargado exitosamente",
                "timestamp", java.time.LocalDateTime.now(),
                "statistics", mcpActionRegistry.getRegistryStatistics()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/registry")
    @Operation(summary = "Obtener registro completo", 
               description = "Retorna el registro completo de servicios MCP")
    public ResponseEntity<McpRegistry> getFullRegistry() {
        McpRegistry registry = mcpActionRegistry.getCurrentRegistry();
        if (registry == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(registry);
    }
    
    @GetMapping("/services/{serviceId}/health-check")
    @Operation(summary = "Obtener configuración de health check", 
               description = "Retorna la configuración de health check de un servicio")
    public ResponseEntity<Map<String, Object>> getServiceHealthCheckConfig(
            @Parameter(description = "ID del servicio") 
            @PathVariable String serviceId) {
        
        McpService service = mcpActionRegistry.getService(serviceId);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> config = Map.of(
                "endpoint", service.getHealthCheckEndpoint(),
                "interval", service.getHealthCheckIntervalOrDefault(60),
                "enabled", service.getHealthCheckEndpoint() != null
        );
        
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/services/{serviceId}/circuit-breaker")
    @Operation(summary = "Obtener configuración de circuit breaker", 
               description = "Retorna la configuración de circuit breaker de un servicio")
    public ResponseEntity<Map<String, Object>> getServiceCircuitBreakerConfig(
            @Parameter(description = "ID del servicio") 
            @PathVariable String serviceId) {
        
        McpService service = mcpActionRegistry.getService(serviceId);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> config = Map.of(
                "enabled", service.isCircuitBreakerEnabled(),
                "threshold", service.getCircuitBreakerThresholdOrDefault(5)
        );
        
        return ResponseEntity.ok(config);
    }
} 