package com.intentmanagerms.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum que representa los posibles estados de una subtarea durante su ciclo de vida.
 */
public enum SubtaskStatus {
    
    PENDING("pending", "Pendiente de ejecución"),
    EXECUTING("executing", "Ejecutándose"),
    COMPLETED("completed", "Completada exitosamente"),
    FAILED("failed", "Falló la ejecución"),
    CANCELLED("cancelled", "Cancelada"),
    RETRYING("retrying", "Reintentando"),
    SKIPPED("skipped", "Omitida"),
    TIMEOUT("timeout", "Tiempo de espera agotado");
    
    private final String value;
    private final String description;
    
    SubtaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    /**
     * Verifica si el estado indica que la subtarea está en progreso.
     */
    public boolean isInProgress() {
        return this == EXECUTING || this == RETRYING;
    }
    
    /**
     * Verifica si el estado indica que la subtarea ha terminado (completada o fallida).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == SKIPPED || this == TIMEOUT;
    }
    
    /**
     * Verifica si el estado indica que la subtarea puede ser ejecutada.
     */
    public boolean isExecutable() {
        return this == PENDING;
    }
    
    /**
     * Verifica si el estado indica que la subtarea falló.
     */
    public boolean isFailed() {
        return this == FAILED || this == TIMEOUT;
    }
} 