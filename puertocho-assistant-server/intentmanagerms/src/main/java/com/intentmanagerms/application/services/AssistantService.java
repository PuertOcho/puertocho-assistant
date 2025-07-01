package com.intentmanagerms.application.services;

import com.intentmanagerms.application.tools.SmartHomeTools;
import com.intentmanagerms.application.tools.SystemTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.JsonSchemaProperty;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssistantService implements Assistant {

    private final ChatLanguageModel chatLanguageModel;
    private final SystemTools systemTools;
    private final SmartHomeTools smartHomeTools;
    private final String systemMessage;

    public AssistantService(ChatLanguageModel chatLanguageModel,
                           SystemTools systemTools,
                           SmartHomeTools smartHomeTools,
                           @Value("${agent.system-message}") String systemMessage) {
        this.chatLanguageModel = chatLanguageModel;
        this.systemTools = systemTools;
        this.smartHomeTools = smartHomeTools;
        this.systemMessage = systemMessage;
    }

    @Override
    public String chat(String userMessage) {
        try {
            // Crear el mensaje del sistema
            SystemMessage systemMsg = SystemMessage.from(systemMessage);
            
            // Crear el mensaje del usuario
            UserMessage userMsg = UserMessage.from(userMessage);
            
            // Crear la lista de mensajes
            List<ChatMessage> messages = List.of(systemMsg, userMsg);
            
            // Obtener especificaciones de herramientas
            List<ToolSpecification> toolSpecs = getToolSpecifications();
            
            // Enviar al modelo de chat
            Response<AiMessage> response = chatLanguageModel.generate(messages, toolSpecs);
            
            // Procesar la respuesta
            AiMessage aiMessage = response.content();
            
            // Si hay tool calls, ejecutarlos
            if (aiMessage.hasToolExecutionRequests()) {
                return executeToolsAndGetFinalResponse(messages, aiMessage);
            }
            
            return aiMessage.text();
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Lo siento, hubo un error procesando tu solicitud: " + e.getMessage();
        }
    }

    private List<ToolSpecification> getToolSpecifications() {
        List<ToolSpecification> specs = new ArrayList<>();
        
        // Especificación para abrir entorno de trabajo
        specs.add(ToolSpecification.builder()
                .name("abrirEntornoDeTrabajo")
                .description("Abre el entorno de trabajo para un proyecto de software específico. Solo funciona para proyectos predefinidos.")
                .addParameter("nombreProyecto", 
                    JsonSchemaProperty.STRING.description("El nombre del proyecto a abrir"))
                .build());
        
        // Especificación para listar proyectos disponibles
        specs.add(ToolSpecification.builder()
                .name("listarProyectosDisponibles")
                .description("Lista los proyectos disponibles que se pueden abrir en el entorno de desarrollo.")
                .build());
        
        // Especificación para ejecutar comandos seguros
        specs.add(ToolSpecification.builder()
                .name("ejecutarComandoSeguro")
                .description("Ejecuta un comando de terminal simple de forma segura. Solo comandos básicos de información.")
                .addParameter("comando", 
                    JsonSchemaProperty.STRING.description("El comando de terminal a ejecutar (solo comandos seguros permitidos)"))
                .build());
        
        // Especificación para encender luz
        specs.add(ToolSpecification.builder()
                .name("encenderLuz")
                .description("Enciende la luz de una habitación específica. Devuelve un mensaje de confirmación.")
                .addParameter("habitacion", 
                    JsonSchemaProperty.STRING.description("El nombre de la habitación donde encender la luz"))
                .build());
        
        // Especificación para apagar luz
        specs.add(ToolSpecification.builder()
                .name("apagarLuz")
                .description("Apaga la luz de una habitación específica. Devuelve un mensaje de confirmación.")
                .addParameter("habitacion", 
                    JsonSchemaProperty.STRING.description("El nombre de la habitación donde apagar la luz"))
                .build());
        
        return specs;
    }

    private String executeToolsAndGetFinalResponse(List<ChatMessage> originalMessages, AiMessage aiMessage) {
        try {
            List<ChatMessage> messages = new ArrayList<>(originalMessages);
            messages.add(aiMessage);
            
            // Ejecutar todas las herramientas solicitadas
            for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                String result = executeTool(toolRequest);
                
                // Agregar el resultado como mensaje del tool
                messages.add(dev.langchain4j.data.message.ToolExecutionResultMessage.from(
                        toolRequest, result));
            }
            
            // Obtener la respuesta final del modelo
            Response<AiMessage> finalResponse = chatLanguageModel.generate(messages);
            return finalResponse.content().text();
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Error ejecutando las herramientas: " + e.getMessage();
        }
    }

    private String executeTool(ToolExecutionRequest request) {
        String toolName = request.name();
        Map<String, Object> arguments = parseArguments(request.arguments());
        
        try {
            switch (toolName) {
                case "abrirEntornoDeTrabajo":
                    return systemTools.abrirEntornoDeTrabajo((String) arguments.get("nombreProyecto"));
                case "listarProyectosDisponibles":
                    return systemTools.listarProyectosDisponibles();
                case "ejecutarComandoSeguro":
                    return systemTools.ejecutarComandoSeguro((String) arguments.get("comando"));
                case "encenderLuz":
                    return smartHomeTools.encenderLuz((String) arguments.get("habitacion"));
                case "apagarLuz":
                    return smartHomeTools.apagarLuz((String) arguments.get("habitacion"));
                default:
                    return "Herramienta no encontrada: " + toolName;
            }
        } catch (Exception e) {
            return "Error ejecutando " + toolName + ": " + e.getMessage();
        }
    }

    private Map<String, Object> parseArguments(String argumentsJson) {
        // Implementación simple para parsear argumentos JSON
        // En producción usaríamos una librería como Jackson
        Map<String, Object> args = new HashMap<>();
        
        if (argumentsJson != null && !argumentsJson.trim().isEmpty()) {
            // Remover llaves y comillas
            String content = argumentsJson.trim()
                    .replaceAll("^\\{", "")
                    .replaceAll("\\}$", "")
                    .replaceAll("\"", "");
            
            // Dividir por comas
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    args.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        
        return args;
    }
} 