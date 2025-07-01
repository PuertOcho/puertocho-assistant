package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/execute")
    public ResponseEntity<Response> execute(@Valid @RequestBody Request request) {
        try {
            logger.info("Ejecutando acción para el prompt: {}", request.prompt());
            
            String result = agentService.executeAction(request.prompt());
            
            Response response = new Response(
                true, 
                result, 
                null, 
                LocalDateTime.now()
            );
            
            logger.info("Acción ejecutada exitosamente");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error ejecutando acción: {}", e.getMessage(), e);
            
            Response errorResponse = new Response(
                false, 
                null, 
                "Error procesando la solicitud: " + e.getMessage(), 
                LocalDateTime.now()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
            "UP", 
            "Intent Manager está funcionando correctamente", 
            LocalDateTime.now()
        ));
    }

    // DTOs con validación
    public record Request(
        @NotBlank(message = "El prompt no puede estar vacío")
        @Size(min = 1, max = 1000, message = "El prompt debe tener entre 1 y 1000 caracteres")
        String prompt
    ) {}

    public record Response(
        boolean success,
        String message,
        String error,
        LocalDateTime timestamp
    ) {}

    public record HealthResponse(
        String status,
        String message,
        LocalDateTime timestamp
    ) {}
} 