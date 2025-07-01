package com.intentmanagerms.application.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SmartHomeTools {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${agent.smart-home.enabled}")
    private boolean smartHomeEnabled;

    @Value("${agent.smart-home.service-url}")
    private String smartHomeServiceUrl;

    @Tool("Enciende la luz de una habitación específica. Devuelve un mensaje de confirmación.")
    public String encenderLuz(String habitacion) {
        System.out.println("Ejecutando 'encenderLuz' para la habitación: " + habitacion);
        
        if (!smartHomeEnabled) {
            return "Error: El sistema de hogar inteligente no está habilitado en la configuración.";
        }

        try {
            // En el futuro, cuando esté disponible el microservicio de domótica:
            // String url = smartHomeServiceUrl + "/api/lights/turn-on";
            // LightRequest request = new LightRequest(habitacion);
            // String response = restTemplate.postForObject(url, request, String.class);
            // return response;

            // Por ahora, simulamos la funcionalidad
            System.out.println("URL del servicio de domótica configurada: " + smartHomeServiceUrl);
            return "OK. La luz de la habitación '" + habitacion + "' ha sido encendida.";
            
        } catch (Exception e) {
            System.err.println("Error al intentar encender la luz: " + e.getMessage());
            return "Error al intentar encender la luz: " + e.getMessage();
        }
    }

    @Tool("Apaga la luz de una habitación específica. Devuelve un mensaje de confirmación.")
    public String apagarLuz(String habitacion) {
        System.out.println("Ejecutando 'apagarLuz' para la habitación: " + habitacion);
        
        if (!smartHomeEnabled) {
            return "Error: El sistema de hogar inteligente no está habilitado en la configuración.";
        }

        try {
            // Simulación por ahora
            System.out.println("URL del servicio de domótica configurada: " + smartHomeServiceUrl);
            return "OK. La luz de la habitación '" + habitacion + "' ha sido apagada.";
            
        } catch (Exception e) {
            System.err.println("Error al intentar apagar la luz: " + e.getMessage());
            return "Error al intentar apagar la luz: " + e.getMessage();
        }
    }
} 