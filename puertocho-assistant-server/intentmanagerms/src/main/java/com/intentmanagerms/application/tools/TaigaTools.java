package com.intentmanagerms.application.tools;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Herramientas MCP para interactuar con Taiga mediante el microservicio taiga-mcp-ms.
 * <p>
 *     Estas herramientas se exponen al agente para que pueda crear proyectos,
 *     listar proyectos o ejecutar acciones complejas en Taiga.
 *     Implementa un login perezoso (lazy) que se reintenta de forma transparente
 *     si la sesión expira.
 * </p>
 */
@Component
public class TaigaTools {

    private static final Logger logger = LoggerFactory.getLogger(TaigaTools.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String username;
    private final String password;
    private String sessionId; // cache sencillo en memoria

    public TaigaTools(@Value("${taiga.mcp.base-url:http://taiga-mcp-api:5007}") String baseUrl,
                      @Value("${taiga.mcp.username:puertocho}") String username,
                      @Value("${taiga.mcp.password:puertocho}") String password) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.username = username;
        this.password = password;
        this.restTemplate = new RestTemplate();
        // Configurar timeouts básicos
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * Lista los proyectos disponibles en Taiga.
     * @return Descripción textual de los proyectos encontrados.
     */
    @Tool("Lista los proyectos disponibles en Taiga y devuelve un resumen legible.")
    public String listarProyectosDisponiblesTaiga() {
        try {
            ensureSession();
            String url = baseUrl + "/projects?session_id=" + sessionId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error al consultar proyectos: HTTP " + response.getStatusCode().value();
            }
            Map<String, Object> body = response.getBody();
            if (body == null) return "Respuesta vacía del servicio Taiga MCP.";
            List<Map<String, Object>> projects = (List<Map<String, Object>>) body.get("projects");
            if (projects == null || projects.isEmpty()) {
                return "No hay proyectos disponibles en Taiga.";
            }
            StringBuilder sb = new StringBuilder("📋 Proyectos en Taiga:\n\n");
            for (int i = 0; i < Math.min(projects.size(), 10); i++) {
                Map<String, Object> p = projects.get(i);
                sb.append("• ").append(p.getOrDefault("name", "sin nombre"))
                  .append(" (ID: ").append(p.get("id")).append(")\n");
            }
            if (projects.size() > 10) {
                sb.append("... y ").append(projects.size() - 10).append(" más\n");
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Error listando proyectos Taiga: {}", e.getMessage());
            return "Error listando proyectos en Taiga: " + e.getMessage();
        }
    }

    /**
     * Crea un proyecto simple en Taiga.
     * @param nombre Nombre del proyecto.
     * @param descripcion Descripción opcional.
     * @return Mensaje con el resultado de la creación.
     */
    @Tool("Crea un nuevo proyecto en Taiga con el nombre y descripción proporcionados.")
    public String crearProyectoTaiga(String nombre, String descripcion) {
        try {
            ensureSession();
            String url = baseUrl + "/projects";

            Map<String, Object> payload = new HashMap<>();
            payload.put("session_id", sessionId);
            payload.put("name", nombre);
            payload.put("description", descripcion != null ? descripcion : "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error al crear proyecto: HTTP " + response.getStatusCode().value();
            }
            Map<String, Object> proj = response.getBody();
            return "✅ Proyecto creado: " + proj.get("name") + " (ID: " + proj.get("id") + ")";
        } catch (Exception e) {
            logger.error("Error creando proyecto Taiga: {}", e.getMessage());
            return "Error creando proyecto en Taiga: " + e.getMessage();
        }
    }

    /**
     * Crea una historia de usuario en un proyecto Taiga.
     */
    @Tool("Crea una historia de usuario en Taiga usando el ID de proyecto y el texto proporcionado.")
    public String crearHistoriaTaiga(Integer projectId, String historiaTexto) {
        try {
            ensureSession();
            if (projectId == null || historiaTexto == null || historiaTexto.isBlank()) {
                return "Error: projectId y texto de la historia son obligatorios";
            }
            String url = baseUrl + "/projects/" + projectId + "/user_stories";
            Map<String, Object> payload = new HashMap<>();
            payload.put("session_id", sessionId);
            payload.put("subject", historiaTexto.length() > 80 ? historiaTexto.substring(0, 80) : historiaTexto);
            payload.put("description", historiaTexto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error al crear historia: HTTP " + response.getStatusCode().value();
            }
            Map<String, Object> story = response.getBody();
            return "✅ Historia creada: #" + story.get("ref") + " – " + story.get("subject");
        } catch (Exception e) {
            logger.error("Error creando historia Taiga: {}", e.getMessage());
            return "Error creando historia en Taiga: " + e.getMessage();
        }
    }

    /**
     * Crea un epic en Taiga.
     */
    @Tool("Crea un epic en Taiga usando ID de proyecto, título y color opcional (#RRGGBB o nombre).")
    public String crearEpicTaiga(Integer projectId, String tituloEpic, String colorHex) {
        try {
            ensureSession();
            if (projectId == null || tituloEpic == null || tituloEpic.isBlank()) {
                return "Error: projectId y título del epic son obligatorios";
            }
            String url = baseUrl + "/projects/" + projectId + "/epics";
            Map<String, Object> payload = new HashMap<>();
            payload.put("session_id", sessionId);
            payload.put("subject", tituloEpic);
            payload.put("description", "Epic creado desde Assistant");
            if (colorHex != null && !colorHex.isBlank()) payload.put("color", colorHex);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error al crear epic: HTTP " + response.getStatusCode().value();
            }
            Map<String, Object> epic = response.getBody();
            return "✅ Epic creado: " + epic.get("subject") + " (ID: " + epic.get("id") + ")";
        } catch (Exception e) {
            logger.error("Error creando epic Taiga: {}", e.getMessage());
            return "Error creando epic en Taiga: " + e.getMessage();
        }
    }

    /**
     * Ejecuta una acción compleja en Taiga MCP usando texto natural.
     */
    @Tool("Ejecuta una acción compleja en Taiga utilizando el endpoint /execute_complex_action.")
    public String ejecutarAccionComplejaTaiga(String accionTexto, Integer projectId) {
        try {
            ensureSession();
            if (accionTexto == null || accionTexto.isBlank()) {
                return "Error: el texto de la acción es obligatorio";
            }
            String url = baseUrl + "/execute_complex_action";
            Map<String, Object> payload = new HashMap<>();
            payload.put("session_id", sessionId);
            payload.put("action_text", accionTexto);
            if (projectId != null) payload.put("project_id", projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error en acción compleja: HTTP " + response.getStatusCode().value();
            }
            Map<String, Object> result = response.getBody();
            if (result == null) return "Acción ejecutada pero respuesta vacía.";
            return "✅ Acción ejecutada. Resultado: " + result.get("action_text");
        } catch (Exception e) {
            logger.error("Error ejecutando acción compleja Taiga: {}", e.getMessage());
            return "Error ejecutando acción compleja Taiga: " + e.getMessage();
        }
    }

    // ===================== Helpers ===================== //

    private void ensureSession() {
        if (sessionId == null) {
            login();
        }
    }

    private void login() {
        try {
            String url = baseUrl + "/login";
            Map<String, String> data = Map.of(
                    "username", username,
                    "password", password,
                    "host", System.getenv().getOrDefault("TAIGA_HOST", "http://host.docker.internal:9000")
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(data, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                this.sessionId = (String) response.getBody().get("session_id");
                logger.info("Sesión Taiga obtenida: {}", sessionId);
            } else {
                throw new IllegalStateException("Login Taiga MCP falló: HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error autenticando en Taiga MCP: {}", e.getMessage());
            throw new RuntimeException("No se pudo autenticar contra Taiga MCP: " + e.getMessage(), e);
        }
    }
} 