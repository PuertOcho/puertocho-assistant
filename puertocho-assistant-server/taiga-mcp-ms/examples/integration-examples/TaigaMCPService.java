package com.intentmanagerms.application.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio avanzado para integrar con Taiga MCP API
 * Incluye funcionalidades de IA para acciones complejas
 */
@Service
public class TaigaMCPService {

    @Value("${taiga.mcp.url:http://localhost:5007}")
    private String taigaMcpUrl;

    @Value("${taiga.host:http://localhost:9000}")
    private String taigaHost;

    @Value("${taiga.username:#{null}}")
    private String taigaUsername;

    @Value("${taiga.password:#{null}}")
    private String taigaPassword;

    private final RestTemplate restTemplate;
    private String sessionId;

    public TaigaMCPService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Autenticarse en Taiga MCP
     */
    public boolean authenticate() {
        try {
            String loginUrl = taigaMcpUrl + "/login";
            
            Map<String, String> loginRequest = Map.of(
                "username", taigaUsername,
                "password", taigaPassword,
                "host", taigaHost
            );
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                loginUrl, loginRequest, Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                this.sessionId = (String) response.getBody().get("session_id");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("Error autenticando: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ejecutar acción compleja usando texto natural
     */
    public ComplexActionResult executeComplexAction(String actionText, Integer projectId, String trackerContent) {
        if (sessionId == null) {
            if (!authenticate()) {
                throw new RuntimeException("No se pudo autenticar en Taiga MCP");
            }
        }
        
        try {
            String url = taigaMcpUrl + "/execute_complex_action";
            
            Map<String, Object> request = new HashMap<>();
            request.put("session_id", sessionId);
            request.put("action_text", actionText);
            
            if (projectId != null) {
                request.put("project_id", projectId);
            }
            
            if (trackerContent != null && !trackerContent.isEmpty()) {
                request.put("tracker_content", trackerContent);
            }
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return ComplexActionResult.fromMap(response.getBody());
            } else {
                throw new RuntimeException("Error ejecutando acción compleja: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                // Token expirado, intentar re-autenticar
                if (authenticate()) {
                    return executeComplexAction(actionText, projectId, trackerContent);
                }
            }
            throw new RuntimeException("Error en petición: " + e.getMessage());
        }
    }

    /**
     * Crear proyecto completo desde archivo PROJECT_TRACKER.md
     */
    public ProjectFromTrackerResult createProjectFromTracker(String trackerContent) {
        String actionText = "Crear proyecto desde tracker";
        ComplexActionResult result = executeComplexAction(actionText, null, trackerContent);
        
        return new ProjectFromTrackerResult(result);
    }

    /**
     * Crear proyecto simple con nombre específico
     */
    public SimpleProjectResult createSimpleProject(String projectName, String description) {
        String actionText = String.format("Crear proyecto '%s' %s", projectName, description);
        ComplexActionResult result = executeComplexAction(actionText, null, null);
        
        return new SimpleProjectResult(result);
    }

    /**
     * Analizar proyecto y obtener recomendaciones
     */
    public ProjectAnalysisResult analyzeProject(Integer projectId, String context) {
        String actionText = String.format("Analizar proyecto y generar recomendaciones %s", context);
        ComplexActionResult result = executeComplexAction(actionText, projectId, null);
        
        return new ProjectAnalysisResult(result);
    }

    /**
     * Generar reporte de proyecto o general
     */
    public ProjectReportResult generateReport(Integer projectId) {
        String actionText = projectId != null ? 
            "Generar reporte detallado del proyecto" : 
            "Generar reporte general de todos los proyectos";
            
        ComplexActionResult result = executeComplexAction(actionText, projectId, null);
        
        return new ProjectReportResult(result);
    }

    /**
     * Crear historia de usuario usando texto natural
     */
    public UserStoryResult createUserStoryFromText(Integer projectId, String storyText) {
        String actionText = String.format("Crear historia: %s", storyText);
        ComplexActionResult result = executeComplexAction(actionText, projectId, null);
        
        return new UserStoryResult(result);
    }

    /**
     * Procesar lista de tareas y crear historias automáticamente
     */
    public BatchTaskResult processBatchTasks(Integer projectId, String tasksList) {
        ComplexActionResult result = executeComplexAction(tasksList, projectId, null);
        return new BatchTaskResult(result);
    }

    /**
     * Ejecutar flujo completo: desde idea de proyecto hasta creación con historias
     */
    public CompletableFuture<FullProjectFlowResult> executeFullProjectFlow(
            String projectIdea, 
            String trackerContent,
            List<String> additionalStories) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Crear proyecto desde tracker
                ProjectFromTrackerResult trackerResult = createProjectFromTracker(trackerContent);
                Integer projectId = trackerResult.getProjectId();
                
                // 2. Añadir historias adicionales si las hay
                List<UserStoryResult> additionalStoryResults = new ArrayList<>();
                if (additionalStories != null) {
                    for (String story : additionalStories) {
                        UserStoryResult storyResult = createUserStoryFromText(projectId, story);
                        additionalStoryResults.add(storyResult);
                    }
                }
                
                // 3. Analizar proyecto recién creado
                ProjectAnalysisResult analysis = analyzeProject(projectId, "análisis inicial con foco en planificación");
                
                // 4. Generar reporte
                ProjectReportResult report = generateReport(projectId);
                
                return new FullProjectFlowResult(
                    trackerResult,
                    additionalStoryResults,
                    analysis,
                    report
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Error en flujo completo: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Cerrar sesión
     */
    public void logout() {
        if (sessionId == null) return;
        
        try {
            String logoutUrl = taigaMcpUrl + "/logout";
            Map<String, String> request = Map.of("session_id", sessionId);
            
            restTemplate.postForEntity(logoutUrl, request, Void.class);
            sessionId = null;
            
        } catch (Exception e) {
            System.err.println("Error cerrando sesión: " + e.getMessage());
        }
    }

    // =================== CLASES DE RESULTADO ===================
    
    public static class ComplexActionResult {
        private String actionText;
        private List<Map<String, Object>> results;
        private String timestamp;
        private String sessionId;
        
        public static ComplexActionResult fromMap(Map<String, Object> data) {
            ComplexActionResult result = new ComplexActionResult();
            result.actionText = (String) data.get("action_text");
            result.results = (List<Map<String, Object>>) data.get("results");
            result.timestamp = (String) data.get("timestamp");
            result.sessionId = (String) data.get("session_id");
            return result;
        }
        
        public boolean isSuccess() {
            return results != null && results.stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.get("success")));
        }
        
        public String getFirstError() {
            return results.stream()
                .filter(r -> Boolean.FALSE.equals(r.get("success")))
                .map(r -> (String) r.get("error"))
                .findFirst()
                .orElse(null);
        }
        
        // Getters
        public String getActionText() { return actionText; }
        public List<Map<String, Object>> getResults() { return results; }
        public String getTimestamp() { return timestamp; }
        public String getSessionId() { return sessionId; }
    }
    
    public static class ProjectFromTrackerResult {
        private Integer projectId;
        private String projectName;
        private String projectSlug;
        private int createdStoriesCount;
        private List<Map<String, Object>> createdStories;
        private String summary;
        
        public ProjectFromTrackerResult(ComplexActionResult complexResult) {
            if (complexResult.isSuccess()) {
                Map<String, Object> successResult = complexResult.getResults().stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("success")))
                    .findFirst()
                    .orElse(new HashMap<>());
                
                Map<String, Object> project = (Map<String, Object>) successResult.get("project");
                if (project != null) {
                    this.projectId = (Integer) project.get("id");
                    this.projectName = (String) project.get("name");
                    this.projectSlug = (String) project.get("slug");
                }
                
                this.createdStories = (List<Map<String, Object>>) successResult.get("created_stories");
                this.createdStoriesCount = createdStories != null ? createdStories.size() : 0;
                this.summary = (String) successResult.get("summary");
            }
        }
        
        // Getters
        public Integer getProjectId() { return projectId; }
        public String getProjectName() { return projectName; }
        public String getProjectSlug() { return projectSlug; }
        public int getCreatedStoriesCount() { return createdStoriesCount; }
        public List<Map<String, Object>> getCreatedStories() { return createdStories; }
        public String getSummary() { return summary; }
    }
    
    public static class SimpleProjectResult {
        private Integer projectId;
        private String projectName;
        private String projectSlug;
        
        public SimpleProjectResult(ComplexActionResult complexResult) {
            if (complexResult.isSuccess()) {
                Map<String, Object> successResult = complexResult.getResults().stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("success")))
                    .findFirst()
                    .orElse(new HashMap<>());
                
                Map<String, Object> project = (Map<String, Object>) successResult.get("project");
                if (project != null) {
                    this.projectId = (Integer) project.get("id");
                    this.projectName = (String) project.get("name");
                    this.projectSlug = (String) project.get("slug");
                }
            }
        }
        
        // Getters
        public Integer getProjectId() { return projectId; }
        public String getProjectName() { return projectName; }
        public String getProjectSlug() { return projectSlug; }
    }
    
    public static class ProjectAnalysisResult {
        private String projectName;
        private int totalStories;
        private int completedStories;
        private int pendingStories;
        private double progressPercentage;
        private List<String> recommendations;
        
        public ProjectAnalysisResult(ComplexActionResult complexResult) {
            if (complexResult.isSuccess()) {
                Map<String, Object> successResult = complexResult.getResults().stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("success")))
                    .findFirst()
                    .orElse(new HashMap<>());
                
                Map<String, Object> analysis = (Map<String, Object>) successResult.get("analysis");
                if (analysis != null) {
                    this.projectName = (String) analysis.get("project_name");
                    this.totalStories = (Integer) analysis.get("total_stories");
                    this.completedStories = (Integer) analysis.get("completed_stories");
                    this.pendingStories = (Integer) analysis.get("pending_stories");
                    this.progressPercentage = (Double) analysis.get("progress_percentage");
                }
                
                this.recommendations = (List<String>) successResult.get("recommendations");
            }
        }
        
        // Getters
        public String getProjectName() { return projectName; }
        public int getTotalStories() { return totalStories; }
        public int getCompletedStories() { return completedStories; }
        public int getPendingStories() { return pendingStories; }
        public double getProgressPercentage() { return progressPercentage; }
        public List<String> getRecommendations() { return recommendations; }
    }
    
    public static class ProjectReportResult {
        private Map<String, Object> report;
        
        public ProjectReportResult(ComplexActionResult complexResult) {
            if (complexResult.isSuccess()) {
                Map<String, Object> successResult = complexResult.getResults().stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("success")))
                    .findFirst()
                    .orElse(new HashMap<>());
                
                this.report = (Map<String, Object>) successResult.get("report");
            }
        }
        
        public Map<String, Object> getReport() { return report; }
        public String getProjectName() { 
            return report != null ? (String) report.get("project_name") : null; 
        }
        public Integer getTotalProjects() { 
            return report != null ? (Integer) report.get("total_projects") : null; 
        }
    }
    
    public static class UserStoryResult {
        private Integer storyId;
        private String storyRef;
        private String storySubject;
        
        public UserStoryResult(ComplexActionResult complexResult) {
            if (complexResult.isSuccess()) {
                Map<String, Object> successResult = complexResult.getResults().stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("success")))
                    .findFirst()
                    .orElse(new HashMap<>());
                
                Map<String, Object> story = (Map<String, Object>) successResult.get("story");
                if (story != null) {
                    this.storyId = (Integer) story.get("id");
                    this.storyRef = (String) story.get("ref");
                    this.storySubject = (String) story.get("subject");
                }
            }
        }
        
        // Getters
        public Integer getStoryId() { return storyId; }
        public String getStoryRef() { return storyRef; }
        public String getStorySubject() { return storySubject; }
    }
    
    public static class BatchTaskResult {
        private List<String> commandsFound;
        private String rawText;
        
        public BatchTaskResult(ComplexActionResult complexResult) {
            if (complexResult.isSuccess()) {
                Map<String, Object> successResult = complexResult.getResults().stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("success")))
                    .findFirst()
                    .orElse(new HashMap<>());
                
                this.commandsFound = (List<String>) successResult.get("commands_found");
                this.rawText = (String) successResult.get("raw_text");
            }
        }
        
        // Getters
        public List<String> getCommandsFound() { return commandsFound; }
        public String getRawText() { return rawText; }
    }
    
    public static class FullProjectFlowResult {
        private ProjectFromTrackerResult trackerResult;
        private List<UserStoryResult> additionalStories;
        private ProjectAnalysisResult analysis;
        private ProjectReportResult report;
        private LocalDateTime completedAt;
        
        public FullProjectFlowResult(
                ProjectFromTrackerResult trackerResult,
                List<UserStoryResult> additionalStories,
                ProjectAnalysisResult analysis,
                ProjectReportResult report) {
            this.trackerResult = trackerResult;
            this.additionalStories = additionalStories;
            this.analysis = analysis;
            this.report = report;
            this.completedAt = LocalDateTime.now();
        }
        
        // Getters
        public ProjectFromTrackerResult getTrackerResult() { return trackerResult; }
        public List<UserStoryResult> getAdditionalStories() { return additionalStories; }
        public ProjectAnalysisResult getAnalysis() { return analysis; }
        public ProjectReportResult getReport() { return report; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        
        public String getSummary() {
            return String.format(
                "Proyecto '%s' creado con %d historias base + %d adicionales. Progreso: %.1f%%",
                trackerResult.getProjectName(),
                trackerResult.getCreatedStoriesCount(),
                additionalStories.size(),
                analysis.getProgressPercentage()
            );
        }
    }
} 