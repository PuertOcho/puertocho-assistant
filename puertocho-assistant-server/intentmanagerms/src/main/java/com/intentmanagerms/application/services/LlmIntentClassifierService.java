package com.intentmanagerms.application.services;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Clasificador de intents basado en LLM. Se invoca como fallback cuando Rasa
 * devuelve una confianza baja.
 */
@Service
public class LlmIntentClassifierService {

    private final ChatLanguageModel chatModel;

    // Lista breve de intents admitidos con descripción
    private static final String INTENT_INFO = """
            Intenciones disponibles:
            - encender_luz: encender la luz de una habitación.
            - apagar_luz: apagar la luz de una habitación.
            - reproducir_musica: reproducir música.
            - parar_musica: detener la música.
            - subir_volumen: aumentar volumen.
            - bajar_volumen: disminuir volumen.
            - consultar_hora: decir la hora actual.
            - consultar_tiempo: dar información meteorológica.
            - poner_alarma: programar una alarma.
            - cancelar_alarma: cancelar una alarma.
            - saludo: saludo estándar.
            - despedida: despedida.
            - ayuda: petición de ayuda.
            - confirmar: afirmación.
            - negar: negación.
            - accion_compleja_taiga: cualquier operación compleja en Taiga (reportes, epics, historias).
            """;

    public LlmIntentClassifierService(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Devuelve el intent más probable o null si no está seguro.
     */
    public String predictIntent(String userText) {
        String prompt = INTENT_INFO + "\n" +
                "Devuelve SOLO el nombre de la intención que mejor describa la siguiente frase del usuario. " +
                "Si no estás seguro responde 'desconocido'.\n" +
                "Frase: " + userText;

        Response<dev.langchain4j.data.message.AiMessage> resp = chatModel.generate(List.of(SystemMessage.from(prompt)));
        if (resp == null || resp.content() == null) return null;
        String text = resp.content().text().trim().toLowerCase();
        if (text.contains("desconocido")) return null;
        // Tomar primera palabra (sin puntuación)
        return text.split("[\n ,.;]")[0];
    }
} 