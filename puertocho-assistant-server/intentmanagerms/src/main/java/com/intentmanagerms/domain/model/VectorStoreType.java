package com.intentmanagerms.domain.model;

/**
 * Enum que define los tipos de vector store disponibles.
 * Cada tipo tiene sus propias características de rendimiento y persistencia.
 */
public enum VectorStoreType {
    IN_MEMORY("in-memory", "Almacenamiento en memoria para desarrollo y testing"),
    CHROMA("chroma", "Base de datos vectorial persistente Chroma"),
    PINECONE("pinecone", "Servicio vectorial en la nube Pinecone"),
    WEAVIATE("weaviate", "Base de datos vectorial Weaviate"),
    QDRANT("qdrant", "Base de datos vectorial Qdrant");

    private final String code;
    private final String description;

    VectorStoreType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static VectorStoreType fromCode(String code) {
        for (VectorStoreType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de vector store no soportado: " + code);
    }

    public boolean isPersistent() {
        return this != IN_MEMORY;
    }

    public boolean requiresExternalService() {
        return this == PINECONE || this == WEAVIATE || this == QDRANT;
    }
} 