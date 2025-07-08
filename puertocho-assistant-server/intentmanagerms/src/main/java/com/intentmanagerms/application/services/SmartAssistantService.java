package com.intentmanagerms.application.services;

import com.intentmanagerms.application.services.dto.EntityInfo;
import com.intentmanagerms.application.services.dto.IntentMessage;
import com.intentmanagerms.application.tools.SmartHomeTools;
import com.intentmanagerms.application.tools.SystemTools;
import com.intentmanagerms.application.tools.TaigaTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SmartAssistantService implements Assistant {

    private static final Logger logger = LoggerFactory.getLogger(SmartAssistantService.class);

    private final DialogManager dialogManager;
    private final SystemTools systemTools;
    private final SmartHomeTools smartHomeTools;
    private final TtsService ttsService;
    private final TaigaTools taigaTools;
    private final boolean nluEnabled;
    private final boolean duEnabled;

    public SmartAssistantService(DialogManager dialogManager,
                               SystemTools systemTools,
                               SmartHomeTools smartHomeTools,
                               TtsService ttsService,
                               TaigaTools taigaTools,
                               @Value("${nlu.enabled:false}") boolean nluEnabled,
                               @Value("${du.enabled:true}") boolean duEnabled) {
        this.dialogManager = dialogManager;
        this.systemTools = systemTools;
        this.smartHomeTools = smartHomeTools;
        this.ttsService = ttsService;
        this.taigaTools = taigaTools;
        this.nluEnabled = nluEnabled;
        this.duEnabled = duEnabled;
        
        logger.info("SmartAssistantService inicializado. NLU habilitado: {}, DU habilitado: {}", nluEnabled, duEnabled);
    }

    @Override
    public String chat(String userMessage) {
        return chatWithSession(userMessage, null);
    }

    /**
     * Procesa un mensaje con soporte para sesiones conversacionales.
     * 
     * @param userMessage Mensaje del usuario
     * @param sessionId ID de sesión (opcional)
     * @return Respuesta del asistente
     */
    public String chatWithSession(String userMessage, String sessionId) {
        try {
            logger.info("Procesando mensaje del usuario: '{}' con sesión: {}", userMessage, sessionId);
            
            DialogResult result;
            if (duEnabled) {
                // Usar DU (nuevo motor)
                logger.info("Usando DU para clasificación de intenciones");
                result = dialogManager.processMessageWithDu(userMessage, sessionId);
            } else if (nluEnabled) {
                // Usar NLU clásico
                logger.info("Usando NLU clásico para clasificación de intenciones");
                result = dialogManager.processMessage(userMessage, sessionId);
            } else {
                logger.warn("Ningún motor de NLU/DU habilitado");
                return "Lo siento, el servicio de análisis de lenguaje no está disponible en este momento.";
            }

            logger.debug("Resultado del diálogo: {}", result);

            // Manejar diferentes tipos de resultado
            switch (result.getType()) {
                case FOLLOW_UP:
                    // Pregunta de seguimiento - devolver directamente
                    return result.getMessage();
                    
                case ACTION_READY:
                    // Ejecutar la acción con las entidades completas
                    return executeIntention(result.getIntent(), result.getEntities(), userMessage);
                    
                case SUCCESS:
                case CLARIFICATION:
                    // Devolver mensaje directo
                    return result.getMessage();
                    
                case ERROR:
                    // Error en el procesamiento
                    logger.error("Error en DialogManager: {}", result.getMessage());
                    return result.getMessage();
                    
                default:
                    logger.warn("Tipo de resultado no manejado: {}", result.getType());
                    return "Lo siento, hubo un problema procesando tu solicitud.";
            }

        } catch (Exception e) {
            logger.error("Error procesando mensaje del usuario: {}", e.getMessage(), e);
            return "Lo siento, hubo un error procesando tu solicitud. Por favor, inténtalo de nuevo.";
        }
    }

    private String executeIntention(String intentName, Map<String, String> entities, String originalMessage) {
        try {
            switch (intentName) {
                // Intenciones de domótica - luces
                case "encender_luz":
                    return handleEncenderLuz(entities);
                
                case "apagar_luz":
                    return handleApagarLuz(entities);
                
                // Intenciones de música
                case "reproducir_musica":
                    return handleReproducirMusica(entities);
                
                case "parar_musica":
                    return handlePararMusica();
                
                // Intenciones de volumen
                case "subir_volumen":
                    return handleSubirVolumen();
                
                case "bajar_volumen":
                    return handleBajarVolumen();
                
                // Intenciones de información
                case "consultar_hora":
                    return systemTools.ejecutarComandoSeguro("date '+%H:%M del %d de %B de %Y'");
                
                case "consultar_tiempo":
                    return "Lo siento, la consulta del tiempo aún no está implementada.";
                
                // Intenciones conversacionales
                case "saludo":
                    return "¡Hola! Soy Puerto Ocho, tu asistente personal. ¿En qué puedo ayudarte?";
                
                case "despedida":
                    return "¡Hasta luego! Que tengas un buen día.";
                
                case "ayuda":
                    return generarMensajeAyuda();
                
                case "confirmar":
                    return "Perfecto, entendido.";
                
                case "negar":
                    return "De acuerdo, no realizaré esa acción.";
                
                // Intenciones de alarmas
                case "poner_alarma":
                case "cancelar_alarma":
                    return "La funcionalidad de alarmas estará disponible próximamente.";
                
                // Intención MCP – acción compleja Taiga
                case "accion_compleja_taiga":
                    return handleAccionComplejaTaiga(entities, originalMessage);
                
                default:
                    logger.warn("Intención no reconocida: {}", intentName);
                    return "Entiendo que quieres " + intentName.replace("_", " ") + 
                           ", pero esa funcionalidad aún no está implementada.";
            }
        } catch (Exception e) {
            logger.error("Error ejecutando intención '{}': {}", intentName, e.getMessage(), e);
            return "Hubo un error ejecutando esa acción. Por favor, inténtalo de nuevo.";
        }
    }

    private String handleEncenderLuz(Map<String, String> entities) {
        String lugar = entities.getOrDefault("lugar", "principal");
        try {
            return smartHomeTools.encenderLuz(lugar);
        } catch (Exception e) {
            logger.error("Error encendiendo luz en {}: {}", lugar, e.getMessage());
            return "No pude encender la luz en " + lugar + ". Verifica que el dispositivo esté disponible.";
        }
    }

    private String handleApagarLuz(Map<String, String> entities) {
        String lugar = entities.getOrDefault("lugar", "principal");
        try {
            return smartHomeTools.apagarLuz(lugar);
        } catch (Exception e) {
            logger.error("Error apagando luz en {}: {}", lugar, e.getMessage());
            return "No pude apagar la luz en " + lugar + ". Verifica que el dispositivo esté disponible.";
        }
    }

    private String handleReproducirMusica(Map<String, String> entities) {
        String contenido = entities.get("contenido");
        String genero = entities.get("genero");
        String artista = entities.get("artista");
        
        StringBuilder mensaje = new StringBuilder("Reproduciendo música");
        if (artista != null) {
            mensaje.append(" de ").append(artista);
        } else if (genero != null) {
            mensaje.append(" de ").append(genero);
        } else if (contenido != null) {
            mensaje.append(": ").append(contenido);
        }
        
        // Aquí se integraría con el servicio de música real
        return mensaje.toString();
    }

    private String handlePararMusica() {
        // Aquí se integraría con el servicio de música real
        return "Música detenida.";
    }

    private String handleSubirVolumen() {
        // Aquí se integraría con el servicio de audio real
        return "Volumen aumentado.";
    }

    private String handleBajarVolumen() {
        // Aquí se integraría con el servicio de audio real
        return "Volumen disminuido.";
    }

    private String generarMensajeAyuda() {
        return "Puedo ayudarte con:\n" +
               "• Control de luces: 'enciende/apaga la luz del salón'\n" +
               "• Música: 'reproduce música' o 'para la música'\n" +
               "• Volumen: 'sube/baja el volumen'\n" +
               "• Información: 'qué hora es'\n" +
               "• Y mucho más. Solo dime qué necesitas.";
    }

    private Map<String, String> extractEntities(List<EntityInfo> entities) {
        return entities.stream()
                .collect(Collectors.toMap(
                        EntityInfo::getEntity,
                        EntityInfo::getValue,
                        (existing, replacement) -> existing // En caso de duplicados, mantener el primero
                ));
    }

    /**
     * Método para entrenar el modelo NLU si es necesario
     */
    public boolean trainNluModel() {
        if (!nluEnabled) {
            logger.warn("NLU no está habilitado, no se puede entrenar");
            return false;
        }
        
        try {
            logger.info("Iniciando entrenamiento del modelo NLU");
            // Delegamos al DialogManager que tiene acceso al NluService
            // Por ahora retornamos true, se implementará cuando sea necesario
            logger.info("Entrenamiento delegado al DialogManager");
            return true;
        } catch (Exception e) {
            logger.error("Error durante el entrenamiento del modelo NLU: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Método para verificar el estado del servicio NLU
     */
    public boolean isNluServiceHealthy() {
        if (!nluEnabled) {
            return false;
        }
        
        try {
            // Verificamos haciendo una llamada de prueba al DialogManager
            DialogResult testResult = dialogManager.processMessage("test", "health-check");
            return !testResult.isError();
        } catch (Exception e) {
            logger.warn("Error verificando salud del servicio NLU: {}", e.getMessage());
            return false;
        }
    }

    private String handleAccionComplejaTaiga(Map<String, String> entities, String originalMessage) {
        Integer projectId = null;
        if (entities != null && entities.containsKey("proyecto")) {
            String value = entities.get("proyecto");
            try {
                projectId = Integer.valueOf(value.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {
                // nombre, no id
            }
        }
        return taigaTools.ejecutarAccionComplejaTaiga(originalMessage, projectId);
    }

    public boolean isDuEnabled() {
        return duEnabled;
    }

    public boolean isNluEnabled() {
        return nluEnabled;
    }
} 