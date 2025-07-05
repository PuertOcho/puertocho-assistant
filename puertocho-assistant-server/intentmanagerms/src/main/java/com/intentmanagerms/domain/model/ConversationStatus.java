package com.intentmanagerms.domain.model;

/**
 * Estados posibles de una conversación.
 */
public enum ConversationStatus {
    /**
     * Conversación activa, esperando entrada del usuario.
     */
    ACTIVE,
    
    /**
     * Conversación completada exitosamente.
     */
    COMPLETED,
    
    /**
     * Conversación cancelada por el usuario.
     */
    CANCELLED,
    
    /**
     * Conversación expirada por timeout.
     */
    EXPIRED,
    
    /**
     * Conversación en error, requiere intervención.
     */
    ERROR
} 