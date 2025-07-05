package com.intentmanagerms.application.services;

/**
 * Tipos de resultado para el procesamiento de diálogos.
 */
public enum DialogResultType {
    /**
     * Se necesita hacer una pregunta de seguimiento para completar la acción.
     */
    FOLLOW_UP,
    
    /**
     * La acción está lista para ejecutarse con todas las entidades requeridas.
     */
    ACTION_READY,
    
    /**
     * Operación completada exitosamente.
     */
    SUCCESS,
    
    /**
     * Se necesita clarificación del usuario.
     */
    CLARIFICATION,
    
    /**
     * Error en el procesamiento.
     */
    ERROR
} 