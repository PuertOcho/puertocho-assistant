package com.intentmanagerms.application.tools;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class SystemTools {

    private static final Logger logger = LoggerFactory.getLogger(SystemTools.class);

    @Value("${agent.system-tools.allowed-projects:knowly,puertocho-assistant}")
    private String allowedProjectsStr;

    @Value("${agent.system-tools.base-projects-path:/home/puertocho/Proyectos}")
    private String baseProjectsPath;

    @Value("${agent.system-tools.enabled:true}")
    private boolean systemToolsEnabled;

    private List<String> getAllowedProjects() {
        return Arrays.asList(allowedProjectsStr.split(","));
    }

    @Tool("Abre el entorno de trabajo para un proyecto de software específico. Solo funciona para proyectos predefinidos.")
    public String abrirEntornoDeTrabajo(String nombreProyecto) {
        logger.info("Ejecutando 'abrirEntornoDeTrabajo' para el proyecto: {}", nombreProyecto);
        
        if (!systemToolsEnabled) {
            logger.warn("Herramientas del sistema deshabilitadas");
            return "Error: Las herramientas del sistema no están habilitadas.";
        }

        if (nombreProyecto == null || nombreProyecto.trim().isEmpty()) {
            return "Error: Debes especificar el nombre del proyecto.";
        }

        nombreProyecto = nombreProyecto.trim();
        List<String> allowedProjects = getAllowedProjects();
        
        if (!allowedProjects.contains(nombreProyecto)) {
            logger.warn("Proyecto no permitido: {}. Proyectos permitidos: {}", nombreProyecto, allowedProjects);
            return "Error: El proyecto '" + nombreProyecto + "' no está en la lista de proyectos permitidos. " +
                   "Proyectos disponibles: " + String.join(", ", allowedProjects);
        }

        try {
            String projectPath = baseProjectsPath + "/" + nombreProyecto;
            Path path = Paths.get(projectPath);
            
            if (!Files.exists(path)) {
                logger.error("El directorio del proyecto no existe: {}", projectPath);
                return "Error: El directorio del proyecto '" + nombreProyecto + "' no existe en la ruta: " + projectPath;
            }

            // Comando para abrir VS Code en el proyecto
            String command = "code " + projectPath;
            
            logger.info("Ejecutando comando: {}", command);
            
            String output = new ProcessExecutor()
                    .command("bash", "-c", command)
                    .timeout(10, TimeUnit.SECONDS)
                    .readOutput(true)
                    .destroyOnExit()
                    .execute()
                    .outputUTF8();

            logger.info("Proyecto '{}' abierto exitosamente en VS Code", nombreProyecto);
            return "✓ Proyecto '" + nombreProyecto + "' abierto exitosamente en el entorno de desarrollo.";

        } catch (TimeoutException e) {
            logger.error("Timeout abriendo proyecto {}: {}", nombreProyecto, e.getMessage());
            return "Error: Timeout al intentar abrir el proyecto. El comando tomó demasiado tiempo.";
        } catch (IOException | InterruptedException e) {
            logger.error("Error abriendo proyecto {}: {}", nombreProyecto, e.getMessage());
            return "Error al abrir el proyecto: " + e.getMessage();
        }
    }

    @Tool("Lista los proyectos disponibles que se pueden abrir en el entorno de desarrollo.")
    public String listarProyectosDisponibles() {
        logger.info("Ejecutando 'listarProyectosDisponibles'");
        
        if (!systemToolsEnabled) {
            return "Error: Las herramientas del sistema no están habilitadas.";
        }

        try {
            List<String> allowedProjects = getAllowedProjects();
            StringBuilder result = new StringBuilder();
            result.append("📁 Proyectos disponibles:\n\n");
            
            for (String project : allowedProjects) {
                String projectPath = baseProjectsPath + "/" + project;
                Path path = Paths.get(projectPath);
                
                if (Files.exists(path)) {
                    result.append("✓ ").append(project).append(" - Disponible\n");
                } else {
                    result.append("✗ ").append(project).append(" - No encontrado\n");
                }
            }
            
            result.append("\nPara abrir un proyecto, usa: 'abre el proyecto [nombre]'");
            
            logger.info("Lista de proyectos generada exitosamente");
            return result.toString();
            
        } catch (Exception e) {
            logger.error("Error listando proyectos: {}", e.getMessage());
            return "Error al listar proyectos: " + e.getMessage();
        }
    }

    @Tool("Ejecuta un comando de terminal simple de forma segura. Solo comandos básicos de información.")
    public String ejecutarComandoSeguro(String comando) {
        logger.info("Ejecutando comando seguro: {}", comando);
        
        if (!systemToolsEnabled) {
            return "Error: Las herramientas del sistema no están habilitadas.";
        }

        if (comando == null || comando.trim().isEmpty()) {
            return "Error: Debes especificar un comando.";
        }

        final String comandoFinal = comando.trim();
        
        // Lista de comandos permitidos (solo comandos de información, no destructivos)
        List<String> allowedCommands = Arrays.asList(
            "date", "whoami", "pwd", "ls -la", "df -h", "free -h", 
            "uptime", "ps aux", "uname -a", "java -version",
            "docker --version", "git --version", "node --version"
        );

        boolean isAllowed = allowedCommands.stream()
            .anyMatch(allowedCmd -> comandoFinal.equals(allowedCmd) || comandoFinal.startsWith(allowedCmd + " "));

        if (!isAllowed) {
            logger.warn("Comando no permitido: {}", comandoFinal);
            return "Error: El comando '" + comandoFinal + "' no está en la lista de comandos permitidos por seguridad. " +
                   "Comandos disponibles: " + String.join(", ", allowedCommands);
        }

        try {
            logger.info("Ejecutando comando permitido: {}", comandoFinal);
            
            String output = new ProcessExecutor()
                    .command("bash", "-c", comandoFinal)
                    .timeout(5, TimeUnit.SECONDS)
                    .readOutput(true)
                    .destroyOnExit()
                    .execute()
                    .outputUTF8();

            logger.info("Comando ejecutado exitosamente");
            return "📋 Resultado del comando '" + comandoFinal + "':\n\n" + output;

        } catch (TimeoutException e) {
            logger.error("Timeout ejecutando comando {}: {}", comandoFinal, e.getMessage());
            return "Error: Timeout al ejecutar el comando.";
        } catch (IOException | InterruptedException e) {
            logger.error("Error ejecutando comando {}: {}", comandoFinal, e.getMessage());
            return "Error ejecutando comando: " + e.getMessage();
        }
    }
} 