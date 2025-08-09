## Epic 6 – MCP (Model Context Protocol) Integration

### Objetivo
Habilitar una integración MCP flexible y escalable sin crear clases específicas por servicio. Se define un router genérico basado en configuración y contratos uniformes de entrada/salida, con transporte pluggable y políticas de resiliencia.

### Arquitectura
- McpRegistry (JSON + hot-reload): catálogo de plugins/acciones con `input_schema`/`output_schema`, auth, timeouts y retries.
- McpRouter: fachada de alto nivel para invocar acciones por nombre (`plugin.action`).
- McpActionInvoker: valida entrada con JSON Schema, aplica políticas (timeout/reintento/circuit breaker), ejecuta por transporte, normaliza la respuesta.
- McpTransport: interfaz con drivers `http` y `stdio` iniciales (extensible a `ws`/`grpc`).
- Observabilidad y seguridad: métricas, trazas, auditoría; secretos vía env; allowlist de acciones.

### Contratos
Request (ejemplo):
```json
{
  "action": "weather.query",
  "input": { "ubicacion": "Madrid" },
  "context": { "session_id": "abc123", "locale": "es-ES", "trace_id": "..." },
  "response": { "format": "auto", "stream": false }
}
```

Response (unificado):
```json
{
  "type": "text",
  "content": "Lluvia probable 70%",
  "mime_type": "text/plain",
  "metadata": { "provider": "weather-mcp", "latency_ms": 420 },
  "stream": false
}
```

Tipos de `type`: `text` | `image` | `audio` | `tool_result`. Para `image`/`audio`, `content` puede ser URL o base64 con `mime_type` obligatorio.

### Esquema de Configuración (`mcp_registry.json`)
Ejemplo de plugin/acción:
```json
{
  "plugins": [
    {
      "id": "weather",
      "transport": "http",
      "endpoint": "http://weather-mcp:5000/api",
      "auth": { "type": "bearer", "env": "WEATHER_TOKEN" },
      "actions": [
        {
          "name": "weather.query",
          "input_schema": {
            "type": "object",
            "required": ["ubicacion"],
            "properties": { "ubicacion": { "type": "string" } },
            "additionalProperties": false
          },
          "output_schema": {
            "type": "object",
            "required": ["type", "content"],
            "properties": {
              "type": { "enum": ["text", "image", "audio"] },
              "content": {},
              "mime_type": { "type": "string" },
              "metadata": { "type": "object" }
            }
          },
          "timeout_ms": 5000,
          "retry": { "retries": 2, "backoff": "exponential", "min_ms": 200, "max_ms": 2000 }
        }
      ]
    }
  ]
}
```

Nota: el fichero actual en `src/main/resources/config/mcp_registry.json` seguirá siendo compatible; se irá ampliando con `input_schema`/`output_schema` y metadatos por acción.

### Interficies (Java, conceptual)
```java
public interface McpTransport {
  McpResponse send(McpPlugin plugin, McpRequest request);
}

public final class McpRouter {
  public McpResponse invoke(String actionFqn, Map<String, Object> input, McpContext ctx) {
    // resolver plugin/acción, validar input, aplicar políticas y enviar por transporte
  }
}
```

### Tareas (Epic 6)
- T6.1: McpRouter + McpRegistry (JSON + hot-reload)
- T6.2: Contratos `McpRequest`/`McpResponse`
- T6.3: `McpTransport` (`http`, `stdio`)
- T6.4: `McpActionInvoker` (validación JSON Schema, políticas, normalización)
- T6.5: Ampliar `mcp_registry.json` con schemas, auth, timeouts, retries
- T6.6: Métricas, trazas, auditoría
- T6.7: Timeouts, reintentos, circuit breaker
- T6.8: Seguridad (env secrets, allowlist, redacción PII)
- T6.9: Tests de contrato e integración (`weather`, `system`)
- T6.10: Integración con `TaskOrchestrator`
- T6.11: CLI/endpoint de diagnóstico
- T6.12: Documentación y ejemplos

### Endpoints/CLI de diagnóstico
- GET `/api/v1/mcp/registry` – listar plugins/acciones
- POST `/api/v1/mcp/validate` – validar `mcp_registry.json`
- POST `/api/v1/mcp/test` – ejecutar acción de prueba

### Variables de entorno sugeridas
- `MCP_REGISTRY_FILE=classpath:config/mcp_registry.json`
- `MCP_REGISTRY_HOT_RELOAD=true`
- `MCP_DEFAULT_TIMEOUT_MS=30000`
- `MCP_CIRCUIT_BREAKER_ENABLED=true`

### Notas de integración con Epics previos
- `DynamicSubtaskDecomposer` y `TaskOrchestrator` ya operan con `mcp_action` + `entities`; solo apuntan a `McpRouter`.
- El sistema MoE vota acciones por nombre; el invoker ejecuta sin acoplarse a proveedores concretos.


