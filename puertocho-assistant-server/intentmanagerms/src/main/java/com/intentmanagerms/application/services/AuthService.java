package com.intentmanagerms.application.services;

import com.intentmanagerms.application.services.dto.LoginRequest;
import com.intentmanagerms.application.services.dto.LoginResponse;
import com.intentmanagerms.application.services.dto.RegisterRequest;
import com.intentmanagerms.application.services.dto.RegisterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Servicio para manejar autenticación automática con el microservicio de usuarios.
 * Incluye login automático, auto-registro y manejo de tokens JWT.
 */
@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final WebClient webClient;
    private final boolean autoLoginEnabled;
    private final String serviceUsername;
    private final String servicePassword;
    private final String serviceEmail;
    private final String serviceFullName;
    
    // Token y metadatos en memoria
    private final AtomicReference<String> currentToken = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> tokenExpiry = new AtomicReference<>();
    
    public AuthService(
            WebClient.Builder webClientBuilder,
            @Value("${gateway.url:http://puertocho-assistant-gateway:10002}") String gatewayUrl,
            @Value("${auth.autologin.enabled}") boolean autoLoginEnabled,
            @Value("${auth.service.username:PuertochoService}") String serviceUsername,
            @Value("${auth.service.password:servicepass123}") String servicePassword,
            @Value("${auth.service.email:service@puertocho.local}") String serviceEmail,
            @Value("${auth.service.fullname:Puertocho Service Account}") String serviceFullName) {
        
        this.webClient = webClientBuilder
                .baseUrl(gatewayUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        
        this.autoLoginEnabled = autoLoginEnabled;
        this.serviceUsername = serviceUsername;
        this.servicePassword = servicePassword;
        this.serviceEmail = serviceEmail;
        this.serviceFullName = serviceFullName;
        
        logger.info("AuthService inicializado. AutoLogin habilitado: {}, Service user: {}", 
                   autoLoginEnabled, serviceUsername);
        
        // Si el autologin está habilitado, intentar login al inicio
        if (autoLoginEnabled) {
            try {
                getValidToken();
                logger.info("Token inicial obtenido exitosamente");
            } catch (Exception e) {
                logger.warn("No se pudo obtener token inicial: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Obtiene un token JWT válido. Si no existe o está expirado, realiza login automático.
     * 
     * @return Token JWT válido
     * @throws AuthServiceException si el autologin está deshabilitado o falla
     */
    public String getValidToken() {
        if (!autoLoginEnabled) {
            throw new AuthServiceException("El autologin está deshabilitado");
        }
        
        // Verificar si el token actual es válido
        String token = currentToken.get();
        LocalDateTime expiry = tokenExpiry.get();
        
        if (token != null && expiry != null && LocalDateTime.now().isBefore(expiry.minusMinutes(5))) {
            logger.debug("Usando token existente válido");
            return token;
        }
        
        // Token expirado o no existe, realizar login
        logger.info("Token expirado o no existe. Realizando autologin...");
        return performAutoLogin();
    }
    
    /**
     * Realiza login automático. Si el usuario no existe, intenta auto-registrarlo.
     */
    private String performAutoLogin() {
        try {
            // Intentar login directo
            LoginResponse loginResponse = doLogin();
            updateTokenInfo(loginResponse);
            logger.info("Login automático exitoso para usuario: {}", serviceUsername);
            return loginResponse.getToken();
            
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 404) {
                // Usuario no existe o credenciales incorrectas, intentar auto-registro
                logger.info("Usuario no encontrado. Intentando auto-registro...");
                return performAutoRegisterAndLogin();
            } else {
                String errorMsg = String.format("Error HTTP %d en login automático: %s", 
                                              e.getStatusCode().value(), e.getResponseBodyAsString());
                logger.error(errorMsg);
                throw new AuthServiceException(errorMsg, e);
            }
        } catch (Exception e) {
            String errorMsg = "Error inesperado en login automático: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new AuthServiceException(errorMsg, e);
        }
    }
    
    /**
     * Realiza auto-registro y luego login.
     */
    private String performAutoRegisterAndLogin() {
        try {
            // Registrar usuario de servicio
            RegisterResponse registerResponse = doRegister();
            logger.info("Auto-registro exitoso: {}", registerResponse.getMessage());
            
            // Realizar login después del registro
            LoginResponse loginResponse = doLogin();
            updateTokenInfo(loginResponse);
            logger.info("Login automático exitoso después del registro");
            return loginResponse.getToken();
            
        } catch (Exception e) {
            String errorMsg = "Error en auto-registro y login: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new AuthServiceException(errorMsg, e);
        }
    }
    
    /**
     * Ejecuta el login via HTTP.
     */
    private LoginResponse doLogin() {
        LoginRequest loginRequest = new LoginRequest(serviceEmail, servicePassword);
        
        return webClient.post()
                .uri("/api/auth/login")
                .bodyValue(loginRequest)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .timeout(Duration.ofSeconds(10))
                .block();
    }
    
    /**
     * Ejecuta el registro via HTTP.
     */
    private RegisterResponse doRegister() {
        RegisterRequest registerRequest = new RegisterRequest(
                serviceUsername, servicePassword, serviceEmail, serviceFullName);
        
        return webClient.post()
                .uri("/api/auth/register")
                .bodyValue(registerRequest)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(RegisterResponse.class)
                .timeout(Duration.ofSeconds(10))
                .block();
    }
    
    /**
     * Actualiza la información del token en memoria.
     */
    private void updateTokenInfo(LoginResponse loginResponse) {
        currentToken.set(loginResponse.getToken());
        // Asumir que el token expira en 24 horas (puedes ajustar según tu configuración JWT)
        tokenExpiry.set(LocalDateTime.now().plusHours(24));
        logger.debug("Token actualizado. Expira aproximadamente: {}", tokenExpiry.get());
    }
    
    /**
     * Invalida el token actual, forzando un nuevo login en la próxima llamada.
     */
    public void invalidateToken() {
        currentToken.set(null);
        tokenExpiry.set(null);
        logger.info("Token invalidado manualmente");
    }
    
    /**
     * Verifica si el autologin está habilitado.
     */
    public boolean isAutoLoginEnabled() {
        return autoLoginEnabled;
    }
    
    /**
     * Excepción personalizada para errores del servicio de autenticación.
     */
    public static class AuthServiceException extends RuntimeException {
        public AuthServiceException(String message) {
            super(message);
        }
        
        public AuthServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
