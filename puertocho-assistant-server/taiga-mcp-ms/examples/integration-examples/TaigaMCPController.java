package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.TaigaMCPService;
import com.intentmanagerms.application.services.TaigaMCPService.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controlador de ejemplo para demostrar el uso de las funcionalidades avanzadas
 * del servicio Taiga MCP con acciones complejas de IA
 */
@RestController
@RequestMapping("/api/taiga-mcp")
public class TaigaMCPController {
    
    @Autowired
    private TaigaMCPService taigaMCPService;
    
    /**
     * Crear proyecto completo desde un archivo PROJECT_TRACKER.md
     * Ejemplo de uso con el archivo tracker adjunto
     */
    @PostMapping("/project/from-tracker")
    public ResponseEntity<ProjectFromTrackerResult> createProjectFromTracker(
            @RequestParam("tracker_file") MultipartFile trackerFile) {
        
        try {
            // Leer contenido del archivo
            String trackerContent = new String(trackerFile.getBytes(), StandardCharsets.UTF_8);
            
            // Crear proyecto desde tracker
            ProjectFromTrackerResult result = taigaMCPService.createProjectFromTracker(trackerContent);
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Crear proyecto completo desde texto en el cuerpo de la petición
     */
    @PostMapping("/project/from-tracker-text")
    public ResponseEntity<ProjectFromTrackerResult> createProjectFromTrackerText(
            @RequestBody Map<String, String> request) {
        
        String trackerContent = request.get("tracker_content");
        if (trackerContent == null || trackerContent.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            ProjectFromTrackerResult result = taigaMCPService.createProjectFromTracker(trackerContent);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Crear proyecto simple con descripción
     */
    @PostMapping("/project/simple")
    public ResponseEntity<SimpleProjectResult> createSimpleProject(
            @RequestBody Map<String, String> request) {
        
        String projectName = request.get("project_name");
        String description = request.getOrDefault("description", "para desarrollo y gestión");
        
        if (projectName == null || projectName.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            SimpleProjectResult result = taigaMCPService.createSimpleProject(projectName, description);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Analizar proyecto existente y obtener recomendaciones
     */
    @PostMapping("/project/{projectId}/analyze")
    public ResponseEntity<ProjectAnalysisResult> analyzeProject(
            @PathVariable Integer projectId,
            @RequestBody(required = false) Map<String, String> request) {
        
        String context = request != null ? 
            request.getOrDefault("context", "análisis general") : 
            "análisis general";
        
        try {
            ProjectAnalysisResult result = taigaMCPService.analyzeProject(projectId, context);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generar reporte de un proyecto específico
     */
    @GetMapping("/project/{projectId}/report")
    public ResponseEntity<ProjectReportResult> generateProjectReport(@PathVariable Integer projectId) {
        try {
            ProjectReportResult result = taigaMCPService.generateReport(projectId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generar reporte general de todos los proyectos
     */
    @GetMapping("/projects/report")
    public ResponseEntity<ProjectReportResult> generateGeneralReport() {
        try {
            ProjectReportResult result = taigaMCPService.generateReport(null);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Crear historia de usuario usando texto natural
     */
    @PostMapping("/project/{projectId}/story/from-text")
    public ResponseEntity<UserStoryResult> createUserStoryFromText(
            @PathVariable Integer projectId,
            @RequestBody Map<String, String> request) {
        
        String storyText = request.get("story_text");
        if (storyText == null || storyText.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            UserStoryResult result = taigaMCPService.createUserStoryFromText(projectId, storyText);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Procesar lista de tareas y crear múltiples historias
     */
    @PostMapping("/project/{projectId}/batch-tasks")
    public ResponseEntity<BatchTaskResult> processBatchTasks(
            @PathVariable Integer projectId,
            @RequestBody Map<String, String> request) {
        
        String tasksList = request.get("tasks_list");
        if (tasksList == null || tasksList.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            BatchTaskResult result = taigaMCPService.processBatchTasks(projectId, tasksList);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Ejecutar flujo completo de creación de proyecto con análisis
     * Este es el endpoint más avanzado que combina múltiples operaciones
     */
    @PostMapping("/project/full-flow")
    public CompletableFuture<ResponseEntity<FullProjectFlowResult>> executeFullProjectFlow(
            @RequestBody Map<String, Object> request) {
        
        String projectIdea = (String) request.get("project_idea");
        String trackerContent = (String) request.get("tracker_content");
        List<String> additionalStories = (List<String>) request.get("additional_stories");
        
        if (trackerContent == null || trackerContent.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
        }
        
        return taigaMCPService.executeFullProjectFlow(projectIdea, trackerContent, additionalStories)
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(throwable -> ResponseEntity.internalServerError().build());
    }
    
    /**
     * Ejecutar acción personalizada usando texto libre
     * Este endpoint permite enviar cualquier acción en texto natural
     */
    @PostMapping("/execute-action")
    public ResponseEntity<ComplexActionResult> executeCustomAction(
            @RequestBody Map<String, Object> request) {
        
        String actionText = (String) request.get("action_text");
        Integer projectId = (Integer) request.get("project_id");
        String trackerContent = (String) request.get("tracker_content");
        
        if (actionText == null || actionText.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            ComplexActionResult result = taigaMCPService.executeComplexAction(actionText, projectId, trackerContent);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Casos de uso predefinidos para demostración
     */
    @PostMapping("/demo/use-cases")
    public ResponseEntity<Map<String, Object>> executeDemoUseCases() {
        try {
            // Ejemplo 1: Crear proyecto de e-commerce
            String ecommerceTracker = generateEcommerceTracker();
            ProjectFromTrackerResult ecommerceProject = taigaMCPService.createProjectFromTracker(ecommerceTracker);
            
            // Ejemplo 2: Analizar el proyecto creado
            ProjectAnalysisResult analysis = taigaMCPService.analyzeProject(
                ecommerceProject.getProjectId(), 
                "con foco en testing y documentación"
            );
            
            // Ejemplo 3: Crear historia adicional
            UserStoryResult additionalStory = taigaMCPService.createUserStoryFromText(
                ecommerceProject.getProjectId(),
                "Como administrador, quiero generar reportes de ventas mensuales para tomar decisiones estratégicas."
            );
            
            // Ejemplo 4: Generar reporte final
            ProjectReportResult report = taigaMCPService.generateReport(ecommerceProject.getProjectId());
            
            return ResponseEntity.ok(Map.of(
                "demo_completed", true,
                "ecommerce_project", ecommerceProject,
                "analysis", analysis,
                "additional_story", additionalStory,
                "final_report", report
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generar tracker de ejemplo para e-commerce
     */
    private String generateEcommerceTracker() {
        return """
            # Plataforma E-commerce Avanzada
            
            ## Fase 1: Configuración inicial ✅ COMPLETADA
            - [x] Configurar entorno de desarrollo
            - [x] Instalar dependencias base
            - [x] Configurar base de datos
            - [x] Setup de autenticación básica
            
            ## Fase 2: Gestión de productos
            - [ ] Crear catálogo de productos
            - [ ] Implementar búsqueda y filtros
            - [ ] Gestión de inventario
            - [ ] Subida de imágenes de productos
            
            ## Fase 3: Carrito y pagos
            - [ ] Funcionalidad de carrito de compras
            - [ ] Integración con pasarelas de pago
            - [ ] Gestión de cupones y descuentos
            - [ ] Cálculo de impuestos y envío
            
            ## Fase 4: Gestión de pedidos
            - [ ] Sistema de procesamiento de pedidos
            - [ ] Seguimiento de envíos
            - [ ] Notificaciones por email
            - [ ] Panel de administración de pedidos
            
            ## Fase 5: Analytics y reportes
            - [ ] Dashboard de métricas de ventas
            - [ ] Reportes de productos más vendidos
            - [ ] Análisis de comportamiento de usuarios
            - [ ] Exportación de datos
            """;
    }
} 