package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.IntentConfigManager;
import com.intentmanagerms.domain.model.IntentConfiguration;
import com.intentmanagerms.domain.model.IntentExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestionar la configuración de intenciones
 */
@RestController
@RequestMapping("/api/v1/intent-config")
@Tag(name = "Intent Configuration", description = "Gestión de configuración de intenciones desde JSON")
public class IntentConfigController {
    
    private final IntentConfigManager intentConfigManager;
    
    public IntentConfigController(IntentConfigManager intentConfigManager) {
        this.intentConfigManager = intentConfigManager;
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Obtener estadísticas de configuración", 
               description = "Retorna estadísticas completas de la configuración de intenciones cargada")
    public ResponseEntity<Map<String, Object>> getConfigurationStatistics() {
        Map<String, Object> stats = intentConfigManager.getConfigurationStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check de configuración", 
               description = "Verifica el estado de salud de la configuración de intenciones")
    public ResponseEntity<Map<String, Object>> getHealthInfo() {
        Map<String, Object> health = intentConfigManager.getHealthInfo();
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/intents")
    @Operation(summary = "Obtener todas las intenciones", 
               description = "Retorna todas las intenciones configuradas")
    public ResponseEntity<Map<String, IntentExample>> getAllIntents() {
        Map<String, IntentExample> intents = intentConfigManager.getAllIntents();
        return ResponseEntity.ok(intents);
    }
    
    @GetMapping("/intents/{intentId}")
    @Operation(summary = "Obtener intención específica", 
               description = "Retorna una intención específica por su ID")
    public ResponseEntity<IntentExample> getIntent(
            @Parameter(description = "ID de la intención") 
            @PathVariable String intentId) {
        
        IntentExample intent = intentConfigManager.getIntent(intentId);
        if (intent == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(intent);
    }
    
    @GetMapping("/intents/domain/{domain}")
    @Operation(summary = "Obtener intenciones por dominio", 
               description = "Retorna todas las intenciones de un dominio de experto específico")
    public ResponseEntity<List<IntentExample>> getIntentsByDomain(
            @Parameter(description = "Dominio de experto") 
            @PathVariable String domain) {
        
        Map<String, List<IntentExample>> intentsByDomain = intentConfigManager.getIntentsByExpertDomain();
        List<IntentExample> domainIntents = intentsByDomain.get(domain);
        
        if (domainIntents == null) {
            return ResponseEntity.ok(List.of());
        }
        
        return ResponseEntity.ok(domainIntents);
    }
    
    @GetMapping("/intents/domains")
    @Operation(summary = "Obtener intenciones por dominio", 
               description = "Retorna todas las intenciones agrupadas por dominio de experto")
    public ResponseEntity<Map<String, List<IntentExample>>> getIntentsByAllDomains() {
        Map<String, List<IntentExample>> intentsByDomain = intentConfigManager.getIntentsByExpertDomain();
        return ResponseEntity.ok(intentsByDomain);
    }
    
    @GetMapping("/mcp-actions")
    @Operation(summary = "Obtener acciones MCP disponibles", 
               description = "Retorna todas las acciones MCP configuradas en las intenciones")
    public ResponseEntity<java.util.Set<String>> getAvailableMcpActions() {
        java.util.Set<String> actions = intentConfigManager.getAvailableMcpActions();
        return ResponseEntity.ok(actions);
    }
    
    @GetMapping("/intents/search")
    @Operation(summary = "Buscar intenciones", 
               description = "Busca intenciones que contengan el texto especificado en ejemplos o descripción")
    public ResponseEntity<Map<String, IntentExample>> searchIntents(
            @Parameter(description = "Texto a buscar") 
            @RequestParam String query) {
        
        Map<String, IntentExample> allIntents = intentConfigManager.getAllIntents();
        Map<String, IntentExample> filteredIntents = new java.util.HashMap<>();
        
        String lowerQuery = query.toLowerCase();
        
        for (Map.Entry<String, IntentExample> entry : allIntents.entrySet()) {
            IntentExample intent = entry.getValue();
            
            // Buscar en descripción
            if (intent.getDescription() != null && 
                intent.getDescription().toLowerCase().contains(lowerQuery)) {
                filteredIntents.put(entry.getKey(), intent);
                continue;
            }
            
            // Buscar en ejemplos
            if (intent.getExamples() != null) {
                boolean foundInExamples = intent.getExamples().stream()
                        .anyMatch(example -> example.toLowerCase().contains(lowerQuery));
                if (foundInExamples) {
                    filteredIntents.put(entry.getKey(), intent);
                }
            }
        }
        
        return ResponseEntity.ok(filteredIntents);
    }
    
    @PostMapping("/reload")
    @Operation(summary = "Recargar configuración", 
               description = "Fuerza una recarga manual de la configuración desde el archivo JSON")
    public ResponseEntity<Map<String, Object>> reloadConfiguration() {
        intentConfigManager.forceReload();
        
        Map<String, Object> response = Map.of(
                "message", "Configuración recargada exitosamente",
                "timestamp", java.time.LocalDateTime.now(),
                "statistics", intentConfigManager.getConfigurationStatistics()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/configuration")
    @Operation(summary = "Obtener configuración completa", 
               description = "Retorna la configuración completa de intenciones")
    public ResponseEntity<IntentConfiguration> getFullConfiguration() {
        IntentConfiguration config = intentConfigManager.getCurrentConfiguration();
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/intents/{intentId}/examples")
    @Operation(summary = "Obtener ejemplos de intención", 
               description = "Retorna los ejemplos de una intención específica")
    public ResponseEntity<List<String>> getIntentExamples(
            @Parameter(description = "ID de la intención") 
            @PathVariable String intentId) {
        
        IntentExample intent = intentConfigManager.getIntent(intentId);
        if (intent == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<String> examples = intent.getExamples();
        if (examples == null) {
            return ResponseEntity.ok(List.of());
        }
        
        return ResponseEntity.ok(examples);
    }
    
    @GetMapping("/intents/{intentId}/examples/rag")
    @Operation(summary = "Obtener ejemplos para RAG", 
               description = "Retorna los ejemplos limitados de una intención para uso en RAG")
    public ResponseEntity<List<String>> getIntentExamplesForRag(
            @Parameter(description = "ID de la intención") 
            @PathVariable String intentId) {
        
        IntentExample intent = intentConfigManager.getIntent(intentId);
        if (intent == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<String> examples = intent.getExamplesForRag();
        return ResponseEntity.ok(examples);
    }
    
    @GetMapping("/intents/{intentId}/entities")
    @Operation(summary = "Obtener entidades de intención", 
               description = "Retorna las entidades requeridas y opcionales de una intención")
    public ResponseEntity<Map<String, Object>> getIntentEntities(
            @Parameter(description = "ID de la intención") 
            @PathVariable String intentId) {
        
        IntentExample intent = intentConfigManager.getIntent(intentId);
        if (intent == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> entities = Map.of(
                "required", intent.getRequiredEntities() != null ? intent.getRequiredEntities() : List.of(),
                "optional", intent.getOptionalEntities() != null ? intent.getOptionalEntities() : List.of(),
                "all", intent.getAllEntities()
        );
        
        return ResponseEntity.ok(entities);
    }
    
    @GetMapping("/intents/{intentId}/slot-filling")
    @Operation(summary = "Obtener preguntas de slot-filling", 
               description = "Retorna las preguntas de slot-filling configuradas para una intención")
    public ResponseEntity<Map<String, String>> getSlotFillingQuestions(
            @Parameter(description = "ID de la intención") 
            @PathVariable String intentId) {
        
        IntentExample intent = intentConfigManager.getIntent(intentId);
        if (intent == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, String> questions = intent.getSlotFillingQuestions();
        if (questions == null) {
            return ResponseEntity.ok(Map.of());
        }
        
        return ResponseEntity.ok(questions);
    }
} 