package com.intentmanagerms.application.services;

import java.util.Map;

/**
 * Resultado del procesamiento de un diálogo.
 * Encapsula el tipo de respuesta y los datos necesarios.
 */
public class DialogResult {

    private final DialogResultType type;
    private final String message;
    private final String sessionId;
    private final String intent;
    private final Map<String, String> entities;

    public DialogResult(DialogResultType type, String message, String sessionId, String intent, Map<String, String> entities) {
        this.type = type;
        this.message = message;
        this.sessionId = sessionId;
        this.intent = intent;
        this.entities = entities;
    }

    // Factory methods para crear diferentes tipos de resultados
    
    /**
     * Resultado cuando se necesita hacer una pregunta de seguimiento.
     */
    public static DialogResult followUp(String question, String sessionId) {
        return new DialogResult(DialogResultType.FOLLOW_UP, question, sessionId, null, null);
    }

    /**
     * Resultado cuando la acción está lista para ejecutarse.
     */
    public static DialogResult actionReady(String intent, Map<String, String> entities, String sessionId) {
        return new DialogResult(DialogResultType.ACTION_READY, null, sessionId, intent, entities);
    }

    /**
     * Resultado de éxito con mensaje.
     */
    public static DialogResult success(String message) {
        return new DialogResult(DialogResultType.SUCCESS, message, null, null, null);
    }

    /**
     * Resultado cuando se necesita clarificación.
     */
    public static DialogResult clarification(String message) {
        return new DialogResult(DialogResultType.CLARIFICATION, message, null, null, null);
    }

    /**
     * Resultado de error.
     */
    public static DialogResult error(String errorMessage) {
        return new DialogResult(DialogResultType.ERROR, errorMessage, null, null, null);
    }

    // Getters
    public DialogResultType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getIntent() {
        return intent;
    }

    public Map<String, String> getEntities() {
        return entities;
    }

    // Métodos de conveniencia
    public boolean isFollowUp() {
        return type == DialogResultType.FOLLOW_UP;
    }

    public boolean isActionReady() {
        return type == DialogResultType.ACTION_READY;
    }

    public boolean isSuccess() {
        return type == DialogResultType.SUCCESS;
    }

    public boolean isError() {
        return type == DialogResultType.ERROR;
    }

    public boolean needsClarification() {
        return type == DialogResultType.CLARIFICATION;
    }

    @Override
    public String toString() {
        return "DialogResult{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", intent='" + intent + '\'' +
                ", entities=" + entities +
                '}';
    }
} 