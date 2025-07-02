package com.intentmanagerms.application.services;

import com.intentmanagerms.application.services.dto.NluEntity;
import com.intentmanagerms.application.services.dto.NluMessage;
import com.intentmanagerms.application.tools.SmartHomeTools;
import com.intentmanagerms.application.tools.SystemTools;
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

    private final NluService nluService;
    private final SystemTools systemTools;
    private final SmartHomeTools smartHomeTools;
    private final TtsService ttsService;
    private final boolean nluEnabled;
    private final double confidenceThreshold;

    public SmartAssistantService(NluService nluService,
                               SystemTools systemTools,
                               SmartHomeTools smartHomeTools,
                               TtsService ttsService,
                               @Value("${nlu.enabled:true}") boolean nluEnabled,
                               @Value("${nlu.confidence-threshold:0.3}") double confidenceThreshold) {
        this.nluService = nluService;
        this.systemTools = systemTools;
        this.smartHomeTools = smartHomeTools;
        this.ttsService = ttsService;
        this.nluEnabled = nluEnabled;
        this.confidenceThreshold = confidenceThreshold;
        
        logger.info("SmartAssistantService inicializado. NLU habilitado: {}, umbral de confianza: {}", 
                   nluEnabled, confidenceThreshold);
    }

    @Override
    public String chat(String userMessage) {
        try {
            logger.info("Procesando mensaje del usuario: '{}'", userMessage);
            
            if (!nluEnabled || !nluService.isServiceHealthy()) {
                logger.warn("Servicio NLU no disponible, usando respuesta de fallback");
                return "Lo siento, el servicio de análisis de lenguaje no está disponible en este momento.";
            }

            // Analizar el mensaje con NLU
            NluMessage nluResult = nluService.analyzeText(userMessage);
            String intentName = nluResult.getIntent().getName();
            double confidence = nluResult.getIntent().getConfidenceAsDouble();
            
            logger.debug("Intención detectada: '{}' con confianza: {}", intentName, confidence);
            
            if (confidence < confidenceThreshold) {
                logger.info("Confianza insuficiente ({}) para intención '{}'", confidence, intentName);
                return "No estoy seguro de qué quieres hacer. ¿Podrías explicármelo de otra manera?";
            }

            // Extraer entidades
            Map<String, String> entities = extractEntities(nluResult.getEntities());
            logger.debug("Entidades extraídas: {}", entities);

            // Ejecutar la acción basada en la intención
            String response = executeIntention(intentName, entities, userMessage);
            
            logger.info("Respuesta generada exitosamente para intención: {}", intentName);
            return response;

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

    private Map<String, String> extractEntities(List<NluEntity> entities) {
        return entities.stream()
                .collect(Collectors.toMap(
                        NluEntity::getEntity,
                        NluEntity::getValue,
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
            boolean success = nluService.trainModel("hogar", "es");
            
            if (success) {
                logger.info("Modelo NLU entrenado exitosamente");
            } else {
                logger.error("Error entrenando el modelo NLU");
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Error durante el entrenamiento del modelo NLU: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Método para verificar el estado del servicio NLU
     */
    public boolean isNluServiceHealthy() {
        return nluEnabled && nluService.isServiceHealthy();
    }
} 