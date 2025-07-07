package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.SmartAssistantService;
import com.intentmanagerms.application.services.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controlador REST para la API conversacional del asistente.
 * Soporta conversaciones multivuelta y respuestas con posible audio TTS.
 */
@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(origins = "*")
public class AssistantController {

    private static final Logger logger = LoggerFactory.getLogger(AssistantController.class);
    
    private final SmartAssistantService smartAssistantService;
    private final TtsService ttsService;

    public AssistantController(SmartAssistantService smartAssistantService, TtsService ttsService) {
        this.smartAssistantService = smartAssistantService;
        this.ttsService = ttsService;
    }

    /**
     * Endpoint principal para conversación con el asistente.
     * Soporta conversaciones multivuelta con gestión de sesiones.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            logger.info("Nueva petición de chat: sessionId='{}', mensaje='{}'", 
                       request.sessionId(), request.message());
            
            // Generar sessionId si no se proporciona
            String sessionId = (request.sessionId() != null && !request.sessionId().trim().isEmpty()) 
                             ? request.sessionId() 
                             : UUID.randomUUID().toString();
            
            // Procesar mensaje con conversación multivuelta
            String assistantResponse = smartAssistantService.chatWithSession(request.message(), sessionId);
            
            // Determinar el motor usado
            String engine = smartAssistantService.isDuEnabled() ? "DU" : (smartAssistantService.isNluEnabled() ? "NLU" : "NONE");
            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("engine", engine);
            
            // Generar audio TTS si se solicita
            String audioUrl = null;
            TtsService.TtsResponse ttsResponse = null;
            
            if (request.generateAudio() != null && request.generateAudio()) {
                try {
                    ttsResponse = ttsService.synthesizeText(
                        assistantResponse, 
                        request.language() != null ? request.language() : "es",
                        request.voice() != null ? request.voice() : "es_female"
                    );
                    
                    if (ttsResponse.isSuccess()) {
                        audioUrl = ttsResponse.getDebugAudioUrl();
                        logger.debug("Audio TTS generado exitosamente: {}", audioUrl);
                    } else {
                        logger.warn("Error generando audio TTS: {}", ttsResponse.getError());
                    }
                } catch (Exception e) {
                    logger.warn("Error en síntesis TTS: {}", e.getMessage());
                }
            }
            
            // Construir respuesta
            ChatResponse response = new ChatResponse(
                true,
                assistantResponse,
                sessionId,
                null, // error
                audioUrl,
                ttsResponse != null ? ttsResponse.getServiceUsed() : null,
                ttsResponse != null ? ttsResponse.getProcessingTimeMs() : null,
                LocalDateTime.now(),
                
                // Información adicional (por ahora valores por defecto)
                "action_completed", // conversationState
                java.util.Map.of(), // extractedEntities (vacío por ahora)
                java.util.Map.of(), // missingEntities (vacío por ahora)
                null, // suggestedAction
                metadata
            );
            
            logger.info("Respuesta generada exitosamente para sesión: {}", sessionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error procesando chat: {}", e.getMessage(), e);
            
            ChatResponse errorResponse = new ChatResponse(
                false,
                null,
                request.sessionId(),
                "Error procesando tu mensaje: " + e.getMessage(),
                null,
                null,
                null,
                LocalDateTime.now(),
                
                // Información adicional para error
                "error", // conversationState
                java.util.Map.of(), // extractedEntities
                java.util.Map.of(), // missingEntities
                null, // suggestedAction
                java.util.Map.of() // metadata
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Endpoint para obtener el estado de una sesión conversacional.
     */
    @GetMapping("/session/{sessionId}/status")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId) {
        try {
            // Por ahora devolvemos información básica
            // En el futuro se puede expandir para consultar el estado real de la conversación
            SessionStatusResponse response = new SessionStatusResponse(
                sessionId,
                "ACTIVE", // Se podría consultar desde ConversationRepository
                LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error obteniendo estado de sesión {}: {}", sessionId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para finalizar una sesión conversacional.
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> endSession(@PathVariable String sessionId) {
        try {
            logger.info("Finalizando sesión: {}", sessionId);
            // Aquí se podría implementar lógica para limpiar la sesión
            // Por ejemplo, marcar como COMPLETED en ConversationRepository
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error finalizando sesión {}: {}", sessionId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint de salud para el servicio conversacional.
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        try {
            boolean nluHealthy = smartAssistantService.isNluServiceHealthy();
            
            HealthResponse response = new HealthResponse(
                nluHealthy ? "UP" : "DEGRADED",
                "Assistant service " + (nluHealthy ? "funcionando correctamente" : "con servicios limitados"),
                nluHealthy,
                LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verificando salud del asistente: {}", e.getMessage());
            
            HealthResponse response = new HealthResponse(
                "DOWN",
                "Error verificando estado del asistente",
                false,
                LocalDateTime.now()
            );
            
            return ResponseEntity.status(503).body(response);
        }
    }

    // DTOs para la API conversacional

    /**
     * Petición de chat conversacional.
     */
    public record ChatRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(min = 1, max = 1000, message = "El mensaje debe tener entre 1 y 1000 caracteres")
        String message,
        
        String sessionId, // Opcional, se genera automáticamente si no se proporciona
        Boolean generateAudio, // Si generar audio TTS de la respuesta
        String language, // Idioma para TTS (opcional, default: es)
        String voice, // Voz para TTS (opcional, default: es_female)
        
        // NUEVO: Contexto adicional del dispositivo
        DeviceContext deviceContext // Información del dispositivo/entorno (opcional)
    ) {}

    /**
     * Respuesta de chat conversacional.
     */
    public record ChatResponse(
        boolean success,
        String message,
        String sessionId,
        String error,
        String audioUrl, // URL del audio generado (si se solicitó)
        String ttsService, // Servicio TTS usado (f5_tts, azure_tts, etc.)
        Long ttsProcessingTimeMs, // Tiempo de procesamiento TTS
        LocalDateTime timestamp,
        
        // NUEVO: Información adicional de la respuesta
        String conversationState, // "waiting_input", "slot_filling", "action_completed"
        java.util.Map<String, String> extractedEntities, // Entidades extraídas en esta vuelta
        java.util.Map<String, String> missingEntities, // Entidades que aún faltan
        String suggestedAction, // Acción sugerida para el dispositivo
        java.util.Map<String, Object> metadata // Metadatos adicionales
    ) {}

    /**
     * Estado de sesión conversacional.
     */
    public record SessionStatusResponse(
        String sessionId,
        String status, // ACTIVE, COMPLETED, EXPIRED, etc.
        LocalDateTime lastActivity
    ) {}

    /**
     * Respuesta de salud del servicio.
     */
    public record HealthResponse(
        String status,
        String message,
        boolean nluServiceHealthy,
        LocalDateTime timestamp
    ) {}

    /**
     * Contexto del dispositivo/entorno para enriquecer las respuestas del asistente.
     */
    public record DeviceContext(
        String deviceType, // "raspberry_pi", "android", "web", etc.
        String location, // Ubicación física del dispositivo
        String room, // Habitación donde está el dispositivo
        Double temperature, // Temperatura ambiente (opcional)
        Double humidity, // Humedad ambiente (opcional)
        String timeZone, // Zona horaria del dispositivo
        Boolean isNightMode, // Si está en modo nocturno
        java.util.Map<String, Object> sensors, // Datos de sensores adicionales
        java.util.Map<String, String> capabilities // Capacidades del dispositivo
    ) {}
} 