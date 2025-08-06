package com.intentmanagerms.domain.model;

/**
 * Estados posibles de una conversación.
 * Define el flujo de estados que puede tener una sesión conversacional.
 */
public enum ConversationState {
    
    /**
     * Conversación activa y lista para recibir mensajes.
     */
    ACTIVE("active", "Conversación activa"),
    
    /**
     * Esperando llenado de slots requeridos.
     */
    WAITING_SLOTS("waiting_slots", "Esperando información adicional"),
    
    /**
     * Ejecutando tareas identificadas.
     */
    EXECUTING_TASKS("executing_tasks", "Ejecutando tareas"),
    
    /**
     * Conversación completada exitosamente.
     */
    COMPLETED("completed", "Conversación completada"),
    
    /**
     * Conversación en estado de error.
     */
    ERROR("error", "Error en la conversación"),
    
    /**
     * Conversación pausada temporalmente.
     */
    PAUSED("paused", "Conversación pausada"),
    
    /**
     * Conversación cancelada por el usuario.
     */
    CANCELLED("cancelled", "Conversación cancelada"),
    
    /**
     * Conversación expirada por timeout.
     */
    EXPIRED("expired", "Conversación expirada");

    private final String code;
    private final String description;

    ConversationState(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Verifica si el estado permite recibir nuevos mensajes.
     */
    public boolean canReceiveMessages() {
        return this == ACTIVE || this == WAITING_SLOTS;
    }

    /**
     * Verifica si el estado es terminal (no puede cambiar).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == ERROR || this == CANCELLED || this == EXPIRED;
    }

    /**
     * Verifica si el estado indica que la conversación está en progreso.
     */
    public boolean isInProgress() {
        return this == ACTIVE || this == WAITING_SLOTS || this == EXECUTING_TASKS;
    }

    /**
     * Obtiene el estado por código.
     */
    public static ConversationState fromCode(String code) {
        for (ConversationState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Estado de conversación no válido: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
} 