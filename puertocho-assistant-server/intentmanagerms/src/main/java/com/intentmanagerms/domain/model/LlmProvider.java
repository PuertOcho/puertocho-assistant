package com.intentmanagerms.domain.model;

/**
 * Enum que define los proveedores de LLM disponibles en el sistema.
 * Cada proveedor tiene su propia configuración de API y modelos.
 */
public enum LlmProvider {
    OPENAI("openai", "OpenAI GPT Models"),
    ANTHROPIC("anthropic", "Anthropic Claude Models"),
    GOOGLE("google", "Google Gemini Models"),
    AZURE("azure", "Azure OpenAI Models"),
    LOCAL("local", "Local/On-premise Models");

    private final String code;
    private final String description;

    LlmProvider(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static LlmProvider fromCode(String code) {
        for (LlmProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Proveedor LLM no soportado: " + code);
    }
} 