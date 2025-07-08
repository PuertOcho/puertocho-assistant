package com.intentmanagerms.infrastructure.web;

import com.intentmanagerms.application.services.AuthService;
import com.intentmanagerms.application.services.AuthenticatedChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para probar y gestionar la funcionalidad de autologin.
 */
@RestController
@RequestMapping("/api/auth-test")
@CrossOrigin(origins = "*")
public class AuthTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthTestController.class);
    
    private final AuthService authService;
    private final AuthenticatedChatService authenticatedChatService;
    
    public AuthTestController(AuthService authService, AuthenticatedChatService authenticatedChatService) {
        this.authService = authService;
        this.authenticatedChatService = authenticatedChatService;
    }
    
    /**
     * Endpoint para verificar el estado del autologin.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("autoLoginEnabled", authService.isAutoLoginEnabled());
        status.put("authenticatedRequestsEnabled", authenticatedChatService.isAuthenticatedRequestsEnabled());
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Endpoint para obtener un token JWT válido (solo si autologin está habilitado).
     */
    @PostMapping("/get-token")
    public ResponseEntity<Map<String, Object>> getToken() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!authService.isAutoLoginEnabled()) {
                response.put("error", "El autologin está deshabilitado");
                return ResponseEntity.badRequest().body(response);
            }
            
            String token = authService.getValidToken();
            response.put("success", true);
            response.put("token", token);
            response.put("message", "Token obtenido exitosamente");
            
            logger.info("Token solicitado manualmente via API");
            return ResponseEntity.ok(response);
            
        } catch (AuthService.AuthServiceException e) {
            response.put("error", e.getMessage());
            logger.error("Error al obtener token via API: {}", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Endpoint para invalidar el token actual.
     */
    @PostMapping("/invalidate-token")
    public ResponseEntity<Map<String, Object>> invalidateToken() {
        Map<String, Object> response = new HashMap<>();
        
        authService.invalidateToken();
        response.put("success", true);
        response.put("message", "Token invalidado exitosamente");
        
        logger.info("Token invalidado manualmente via API");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint para probar una petición autenticada al chat.
     */
    @PostMapping("/test-chat")
    public ResponseEntity<Map<String, Object>> testAuthenticatedChat(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String message = request.getOrDefault("message", "enciende la luz");
            String sessionId = request.getOrDefault("sessionId", "test-session");
            
            if (!authenticatedChatService.isAuthenticatedRequestsEnabled()) {
                response.put("error", "Las peticiones autenticadas están deshabilitadas");
                return ResponseEntity.badRequest().body(response);
            }
            
            String chatResponse = authenticatedChatService.sendAuthenticatedChatMessage(message, sessionId);
            
            response.put("success", true);
            response.put("message", "Petición autenticada exitosa");
            response.put("chatResponse", chatResponse);
            
            logger.info("Test de chat autenticado exitoso con mensaje: '{}'", message);
            return ResponseEntity.ok(response);
            
        } catch (AuthenticatedChatService.AuthenticatedChatException e) {
            response.put("error", e.getMessage());
            logger.error("Error en test de chat autenticado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("error", "Error inesperado: " + e.getMessage());
            logger.error("Error inesperado en test de chat autenticado", e);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
