# PROJECT TRACKER – IntentManagerMS LLM-RAG + MoE Implementation

> Última actualización: 2025-01-27 (Actualizado con implementación real T1.1 y T1.2)
> 
> **Objetivo**: Implementación completamente nueva de intentmanagerms usando arquitectura LLM-RAG + Mixture of Experts (MoE) para clasificación de intenciones escalable, conversación multivuelta inteligente y soporte nativo MCP.

---

## Leyenda de estados
- ✅ Completado
- 🔄 En progreso  
- ⏳ Pendiente
- 🚧 Bloqueado

---

## Epic 1 – Arquitectura Base y Configuración LLM-RAG

**Descripción del Epic**: Establecer los fundamentos técnicos del sistema LLM-RAG. Este Epic crea la infraestructura base necesaria para soportar múltiples LLMs, almacenamiento vectorial para RAG, configuración dinámica desde JSON, y el registro de acciones MCP. Es la base sobre la que se construyen todos los demás Epics.

**Objetivos clave**: 
- Infraestructura Spring Boot funcional con LangChain4j
- Gestión configurable de múltiples LLMs  
- Vector store operativo para embeddings RAG
- Sistema de configuración JSON hot-reload
- Registro centralizado de acciones MCP disponibles

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T1.1 | Crear estructura base Java Spring Boot con dependencias LangChain4j | – | ✅ |
| T1.2 | Implementar `LlmConfigurationService` para gestión de múltiples LLMs | T1.1 | ✅ |
| T1.3 | Crear `VectorStoreService` para embeddings RAG (Chroma/In-memory) | T1.1, T1.2 | ⏳ |
| T1.4 | Diseñar `IntentConfigManager` para cargar intenciones desde JSON dinámico | T1.1, T1.2 | ⏳ |
| T1.5 | Implementar `McpActionRegistry` para acciones configurables | T1.1, T1.2 | ⏳ |

---

## 📋 **IMPLEMENTACIÓN REAL COMPLETADA**

### **T1.1 ✅ - Estructura Base Java Spring Boot**
**Archivos Implementados:**
- ✅ `pom.xml` - Dependencias Spring Boot 3.2.1 + Spring Cloud + OpenAPI
- ✅ `IntentManagerApplication.java` - Clase principal Spring Boot
- ✅ `application.yml` - Configuración simplificada para configms
- ✅ `Dockerfile` - Multi-stage build optimizado
- ✅ `configms/intent-manager.yml` - Configuración centralizada

**Configuración Centralizada:**
```yaml
# configms/src/main/resources/configurations/intent-manager.yml
llm:
  primary:
    model: ${PRIMARY_LLM_MODEL:gpt-4}
    provider: ${PRIMARY_LLM_PROVIDER:openai}
    api-key: ${OPENAI_API_KEY:}
  timeout: ${LLM_TIMEOUT:30s}
  max-tokens: ${LLM_MAX_TOKENS:4096}
  temperature: ${LLM_TEMPERATURE:0.7}
```

### **T1.2 ✅ - LlmConfigurationService**
**Modelos de Dominio:**
- ✅ `LlmProvider` - Enum (OPENAI, ANTHROPIC, GOOGLE, AZURE, LOCAL)
- ✅ `LlmConfiguration` - Configuración completa con timeout, tokens, temperatura
- ✅ `LlmResponse` - Respuestas con metadatos y métricas

**Servicios Implementados:**
- ✅ `LlmConfigurationService` - Gestión centralizada de múltiples LLMs
- ✅ `LlmInitializationService` - Inicialización automática con `@EventListener`
- ✅ `LlmConfigurationController` - 12 endpoints REST completos

**LLMs Configurados Automáticamente:**
```
✅ primary: LLM Primario (gpt-4) - Peso: 1.0
✅ fallback: LLM de Fallback (gpt-3.5-turbo) - Peso: 0.8  
✅ moe-llm-a: Juez A - Análisis Crítico (gpt-4) - Peso: 1.0
✅ moe-llm-c: Juez C - Practicidad de Acción (gpt-3.5-turbo) - Peso: 0.9
⚠️ moe-llm-b: Omitido (API Key Anthropic no configurada)
```

**API REST Disponible:**
```bash
GET /api/v1/llm-config/statistics    # Estadísticas completas
GET /api/v1/llm-config/primary      # LLM primario
GET /api/v1/llm-config/voting       # LLMs para votación MoE
GET /api/v1/llm-config/{id}/health  # Health check individual
POST/PUT/DELETE /api/v1/llm-config  # CRUD completo
```

**Variables de Entorno Clave:**
```bash
OPENAI_API_KEY=sk-proj-...
ANTHROPIC_API_KEY=sk-ant-...
MOE_ENABLED=true
PRIMARY_LLM_MODEL=gpt-4
MOE_LLM_A_MODEL=gpt-4
MOE_LLM_B_MODEL=claude-3-sonnet-20240229
MOE_LLM_C_MODEL=gpt-3.5-turbo
```

---

## Epic 2 – Motor RAG para Clasificación de Intenciones

**Descripción del Epic**: Implementar el motor de Retrieval Augmented Generation (RAG) que reemplaza completamente el sistema RASA/DU. Utiliza embeddings vectoriales para realizar few-shot learning con ejemplos de intenciones almacenados dinámicamente. El sistema busca ejemplos similares, construye prompts contextuales y classifica intenciones sin necesidad de entrenamiento tradicional.

**Objetivos clave**:
- Clasificación de intenciones sin entrenamiento previo
- Few-shot learning basado en ejemplos JSON
- Similarity search eficiente con embeddings
- Confidence scoring robusto y configurable
- Fallback inteligente para casos edge

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T2.1 | Crear `RagIntentClassifier` con embeddings vectoriales para few-shot learning | T1.3, T1.4 | ⏳ |
| T2.2 | Implementar sistema de similarity search para ejemplos de intenciones | T2.1 | ⏳ |
| T2.3 | Desarrollar prompt engineering dinámico con contexto RAG | T2.1 | ⏳ |
| T2.4 | Añadir confidence scoring usando múltiples métricas | T2.2 | ⏳ |
| T2.5 | Crear fallback inteligente con degradación gradual | T2.4 | ⏳ |

## Epic 3 – MoE Voting System (Sistema de Votación LLM)

**Descripción del Epic**: Implementar sistema de votación donde múltiples LLMs debaten brevemente la mejor acción a tomar, reemplazando el concepto tradicional de "expertos especializados" por un "jurado de LLMs". Cada LLM vota independientemente y un motor de consenso determina la acción final. Sistema completamente configurable que puede habilitarse/deshabilitarse via variables de entorno.

**Objetivos clave**:
- Votación simultánea de 3 LLMs con roles específicos
- Motor de consenso para procesar votos y decidir acción final
- Configuración flexible: habilitar/deshabilitar via MOE_ENABLED
- Fallback a LLM único cuando voting está deshabilitado
- Logging transparente del proceso de votación para debugging

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T3.1 | Implementar `LlmVotingService` para sistema de debate entre múltiples LLMs | T1.2 | ⏳ |
| T3.2 | Crear `VotingRound` donde 3 LLMs debaten brevemente la acción a tomar | T3.1 | ⏳ |
| T3.3 | Desarrollar `ConsensusEngine` para procesar votos y llegar a decisión final | T3.2 | ⏳ |
| T3.4 | Implementar configuración para habilitar/deshabilitar voting (MoE_ENABLED=true/false) | T3.3 | ⏳ |
| T3.5 | Crear fallback a LLM único cuando voting está deshabilitado | T3.4 | ⏳ |

## Epic 4 – Sistema Conversacional Inteligente + Orquestación de Subtareas

**Descripción del Epic**: Desarrollar sistema conversacional avanzado que usa LLM para descomponer dinámicamente peticiones complejas en múltiples subtareas ejecutables. NO usa configuraciones predefinidas, sino que el LLM analiza cada petición y identifica automáticamente qué MCPs/servicios necesita invocar. Mantiene estado de progreso y marca conversación como completada solo cuando todas las subtareas están ejecutadas exitosamente.

**Objetivos clave**:
- Conversación multivuelta con memoria contextual persistente
- Slot-filling automático usando LLM para preguntas dinámicas  
- **Descomposición dinámica**: LLM identifica automáticamente múltiples acciones en una petición
- **Orquestador inteligente**: Ejecuta subtareas secuencial o paralelamente según dependencias detectadas
- **Estado de progreso**: Tracking automático hasta completion de todas las subtareas
- Manejo de anáforas y referencias contextuales

**Casos de uso - Descomposición Dinámica**:
- "Consulta el tiempo de Madrid y programa una alarma si va a llover" 
  → LLM detecta: [consultar_tiempo + programar_alarma_condicional]
- "Crea un issue en GitHub sobre el weather bug y actualiza el estado en Taiga"
  → LLM detecta: [crear_github_issue + actualizar_taiga_story]  
- "Enciende las luces del salón, pon música relajante y ajusta la temperatura a 22°"
  → LLM detecta: [encender_luces + reproducir_musica + ajustar_temperatura]

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T4.1 | Diseñar `ConversationManager` con contexto LLM-powered | T2.1 | ⏳ |
| T4.2 | Implementar slot-filling automático usando LLM para preguntas dinámicas | T4.1 | ⏳ |
| T4.3 | Crear `EntityExtractor` basado en LLM para extracción contextual | T4.1 | ⏳ |
| T4.4 | Desarrollar memoria conversacional con Redis para sesiones persistentes | T4.1 | ⏳ |
| T4.5 | Crear `DynamicSubtaskDecomposer` - LLM analiza petición y identifica múltiples acciones automáticamente | T4.1 | ⏳ |
| T4.6 | Implementar `TaskOrchestrator` para ejecución secuencial/paralela de subtareas detectadas dinámicamente | T4.5 | ⏳ |
| T4.7 | Desarrollar sistema de estado de progreso: tracking automático hasta completion de todas las subtareas | T4.6 | ⏳ |
| T4.8 | Implementar resolución de anáforas y referencias contextuales | T4.4 | ⏳ |

## Epic 5 – Integración Audio y Transcripción

**Descripción del Epic**: Integrar completamente el pipeline de audio desde recepción hasta respuesta. Reemplaza la dependencia de servicios externos de transcripción por integración directa con whisper-ms, maneja metadata de audio contextual (ubicación, temperatura, etc.), y soporta tanto entrada de texto como audio de forma unificada.

**Objetivos clave**:
- Pipeline completo: Audio → Transcripción → Clasificación → Respuesta
- Integración directa con whisper-ms para transcripción
- Soporte para metadata contextual de dispositivos (temperatura, ubicación)
- Manejo robusto de errores de transcripción
- API unificada para texto y audio

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T5.1 | Crear `AudioProcessingController` para recibir audio multipart/form-data | T1.1 | ⏳ |
| T5.2 | Implementar cliente `WhisperTranscriptionService` para whisper-ms | T5.1 | ⏳ |
| T5.3 | Desarrollar pipeline Audio → Transcripción → Clasificación → Respuesta | T5.2, T2.1 | ⏳ |
| T5.4 | Añadir soporte para metadata de audio y contexto de dispositivo | T5.1 | ⏳ |
| T5.5 | Implementar manejo de errores de transcripción con fallbacks | T5.2 | ⏳ |

## Epic 6 – MCP (Model Context Protocol) Integration

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T6.1 | Crear `McpClientService` para comunicación con servicios MCP externos | T1.5 | ⏳ |
| T6.2 | Implementar `TaigaMcpClient` reutilizando taiga-mcp-ms existente | T6.1 | ⏳ |
| T6.3 | Desarrollar `WeatherMcpClient` para consultas meteorológicas | T6.1 | ⏳ |
| T6.4 | Crear `SystemMcpClient` para acciones de sistema (hora, alarmas, etc.) | T6.1 | ⏳ |
| T6.5 | Añadir configuración JSON dinámica para nuevos clientes MCP | T6.1 | ⏳ |

## Epic 7 – API y Compatibilidad

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T7.1 | Diseñar nuevos endpoints REST manteniendo compatibilidad con gatewayms | T5.1 | ⏳ |
| T7.2 | Crear `AssistantController` con soporte para audio + texto + contexto | T7.1, T5.3 | ⏳ |
| T7.3 | Implementar respuestas múltiples: texto, audio TTS, acciones MCP | T7.2 | ⏳ |
| T7.4 | Añadir endpoints de diagnóstico y métricas de rendimiento | T7.2 | ⏳ |
| T7.5 | Crear documentación OpenAPI/Swagger para nuevos endpoints | T7.1 | ⏳ |

## Epic 8 – Configuración JSON Dinámica

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T8.1 | Diseñar esquema JSON para definición de intenciones y ejemplos | T1.4 | ⏳ |
| T8.2 | Crear esquema JSON para configuración de expertos MoE | T3.1 | ⏳ |
| T8.3 | Implementar esquema JSON para acciones MCP y parámetros | T1.5 | ⏳ |
| T8.4 | Añadir hot-reload de configuraciones sin reiniciar el servicio | T8.1, T8.2, T8.3 | ⏳ |
| T8.5 | Crear validación de esquemas JSON con feedback detallado | T8.4 | ⏳ |

## Epic 9 – Testing y Evaluación

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T9.1 | Crear suite de tests unitarios para todos los componentes | Todas | ⏳ |
| T9.2 | Implementar tests de integración para flujo completo audio→respuesta | T5.3, T7.3 | ⏳ |
| T9.3 | Desarrollar benchmarks de rendimiento vs sistema RASA/DU anterior | T2.1, T3.5 | ⏳ |
| T9.4 | Crear tests de escalabilidad para adición dinámica de intenciones | T8.4 | ⏳ |
| T9.5 | Implementar métricas de calidad conversacional y accuracy | T4.2, T4.5 | ⏳ |

## Epic 10 – Despliegue y Configuración

| ID | Descripción | Dependencias | Estado |
|----|-------------|--------------|--------|
| T10.1 | Actualizar Dockerfile para nuevas dependencias LangChain4j | T1.1 | ⏳ |
| T10.2 | Configurar variables de entorno para LLMs y servicios externos | T1.2 | ⏳ |
| T10.3 | Crear archivos JSON de configuración por defecto para demo | T8.1, T8.2, T8.3 | ⏳ |
| T10.4 | Integrar con docker-compose existente manteniendo compatibilidad | T10.1 | ⏳ |
| T10.5 | Documentar migración desde sistema anterior | Todas | ⏳ |

---

## Roadmap de Implementación

### ✅ **Fase 1: Fundamentos (COMPLETADA)**
- ✅ **Epic 1**: Arquitectura Base (T1.1, T1.2 completados)
- ⏳ **Epic 1**: Resto de tareas (T1.3, T1.4, T1.5)
- ⏳ **Epic 2**: Motor RAG básico
- ⏳ **Epic 5**: Integración Audio (básica)

### 🔄 **Fase 2: Inteligencia (EN PROGRESO)**
- ✅ **Epic 3**: MoE Architecture (Base T1.2 completada)
- ⏳ **Epic 4**: Sistema Conversacional
- ⏳ **Epic 8**: Configuración JSON

### ⏳ **Fase 3: Integración**
- ⏳ **Epic 6**: MCP Integration
- ⏳ **Epic 7**: API y Compatibilidad
- ⏳ **Epic 9**: Testing básico

### ⏳ **Fase 4: Optimización**
- ⏳ **Epic 9**: Testing completo y evaluación
- ⏳ **Epic 10**: Despliegue y documentación

### **📊 Progreso Actual:**
- **Epic 1**: 2/5 tareas completadas (40%)
- **Epic 2**: 0/5 tareas completadas (0%)
- **Epic 3**: Base preparada, pendiente implementación completa
- **Total General**: 2/50 tareas completadas (4%)

---

## Arquitectura Implementada vs Objetivo

### **✅ IMPLEMENTADO (T1.1 + T1.2)**

```
┌─────────────────┐    Config    ┌─────────────────┐    REST API    ┌─────────────────┐
│   CONFIGMS      │◄─────────────│  INTENTMGR-V2   │───────────────▶│  LLM CONFIG     │
│ (Centralizada)  │              │ (Spring Boot)   │                │   SERVICE       │
└─────────────────┘              └─────────────────┘                └─────────────────┘
                                          │                                │
                                          ▼                                ▼
                                 ┌─────────────────┐              ┌─────────────────┐
                                 │ LLM INIT SERVICE│              │ LLM CONFIG API  │
                                 │ (Auto Setup)    │              │ (12 endpoints)  │
                                 └─────────────────┘              └─────────────────┘
                                          │                                │
                                          ▼                                ▼
                                 ┌─────────────────┐              ┌─────────────────┐
                                 │   LLM POOL      │              │   LLM HEALTH    │
                                 │ (4 LLMs Ready)  │              │   (Monitoring)  │
                                 └─────────────────┘              └─────────────────┘
                                          │
                                          ▼
┌─────────────────┐              ┌─────────────────┐    ┌─────────────────┐
│   PRIMARY LLM   │              │   MOE LLM-A     │    │   MOE LLM-C     │
│   (GPT-4)       │              │ (GPT-4 Juez A)  │    │(GPT-3.5 Juez C) │
│   Peso: 1.0     │              │   Peso: 1.0     │    │   Peso: 0.9     │
└─────────────────┘              └─────────────────┘    └─────────────────┘
        │                                │                        │
        └────────────────┬───────────────┴────────────────────────┘
                         ▼
                ┌─────────────────┐
                │ FALLBACK LLM    │
                │ (GPT-3.5-Turbo) │
                │   Peso: 0.8     │
                └─────────────────┘
```

### **🎯 OBJETIVO COMPLETO (Futuras Tareas)**

```
┌─────────────────┐    Audio     ┌─────────────────┐    JSON    ┌─────────────────┐
│  WHISPER-MS     │◄─────────────│  GATEWAY-MS     │───────────▶│ INTENTMGR-V2    │
│  (Transcripción)│              │  (Routing)      │            │ (LLM-RAG+MoE)   │
└─────────────────┘              └─────────────────┘            └─────────────────┘
                                                                          │
                                                                          ▼
┌─────────────────┐              ┌─────────────────┐            ┌─────────────────┐
│ VECTOR STORE    │◄─────────────│   RAG ENGINE    │◄───────────│  EXPERT ROUTER  │
│ (Embeddings)    │              │ (Similarity)    │            │  (MoE Selection) │
└─────────────────┘              └─────────────────┘            └─────────────────┘
                                                                          │
                                                                          ▼
┌─────────────────┐              ┌─────────────────┐    ┌─────────────────┐
│     LLM-A       │              │     LLM-B       │    │     LLM-C       │
│   (GPT-4)       │              │  (Claude-3)     │    │ (GPT-3.5-Turbo) │
│   "Voto: X"     │              │   "Voto: Y"     │    │   "Voto: Z"     │
└─────────────────┘              └─────────────────┘    └─────────────────┘
        │                                │                        │
        └────────────────┬───────────────┴────────────────────────┘
                         ▼
                ┌─────────────────┐
                │ CONSENSUS ENGINE│
                │ (Procesa votos) │
                │ Decisión final  │
                └─────────────────┘
                         │
                         ▼
┌─────────────────┐              ┌─────────────────┐    ┌─────────────────┐
│ CONVERSATION    │              │   MCP ACTIONS   │    │   SINGLE LLM    │
│   MANAGER       │              │ (Taiga/Weather) │    │   (Fallback)    │
└─────────────────┘              └─────────────────┘    └─────────────────┘
```

---

## Configuración de Ejemplo

### intents.json
```json
{
  "intents": {
    "consultar_tiempo": {
      "examples": [
        "¿qué tiempo hace?",
        "dime el clima de hoy",
        "cómo está el tiempo"
      ],
      "required_entities": ["ubicacion"],
      "mcp_action": "weather_query",
      "expert_domain": "general",
      "slot_filling_questions": {
        "ubicacion": "¿De qué ciudad quieres consultar el tiempo?"
      }
    },
    "encender_luz": {
      "examples": [
        "enciende la luz",
        "prende la lámpara",
        "ilumina la habitación"
      ],
      "required_entities": ["lugar"],
      "mcp_action": "smart_home_light_on",
      "expert_domain": "smart_home",
      "slot_filling_questions": {
        "lugar": "¿En qué habitación quieres encender la luz?"
      }
    }
  }
}
```

### moe_voting.json (Implementado)
```json
{
  "voting_system": {
    "enabled": "${MOE_ENABLED:true}",
    "max_debate_rounds": 1,
    "consensus_threshold": 0.6,
    "llm_participants": [
      {
        "id": "moe-llm-a", 
        "model": "gpt-4",
        "api_key": "${OPENAI_API_KEY}",
        "role": "Juez A - Análisis crítico y evaluación detallada",
        "weight": 1.0,
        "temperature": 0.3,
        "max_tokens": 2048
      },
      {
        "id": "moe-llm-b",
        "model": "claude-3-sonnet-20240229", 
        "api_key": "${ANTHROPIC_API_KEY}",
        "role": "Juez B - Análisis de contexto conversacional y coherencia",
        "weight": 1.0,
        "temperature": 0.3,
        "max_tokens": 2048
      },
      {
        "id": "moe-llm-c",
        "model": "gpt-3.5-turbo",
        "api_key": "${OPENAI_API_KEY}",
        "role": "Juez C - Evaluación de practicidad y factibilidad de acciones",
        "weight": 0.9,
        "temperature": 0.3,
        "max_tokens": 2048
      }
    ],
    "fallback_llm": {
      "id": "primary",
      "model": "gpt-4",
      "api_key": "${OPENAI_API_KEY}",
      "used_when": "voting_disabled_or_failed"
    }
  }
}
```

### Ejemplo de Flujo de Votación Simple
```
Usuario: "Enciende la luz del salón"

LLM-A Voto: "ACCIÓN: encender_luz, ENTIDADES: {lugar: 'salón'}, CONFIANZA: 0.9"
LLM-B Voto: "ACCIÓN: encender_luz, ENTIDADES: {lugar: 'salón'}, CONFIANZA: 0.85" 
LLM-C Voto: "ACCIÓN: encender_luz, ENTIDADES: {lugar: 'salón'}, CONFIANZA: 0.8"

CONSENSO: encender_luz (3/3 votos) → EJECUTAR ACCIÓN MCP
```

### Ejemplo de Descomposición Dinámica por LLM
```
Usuario: "Consulta el tiempo de Madrid y programa una alarma si va a llover"

PASO 1 - ANÁLISIS LLM:
LLM Prompt: "Analiza esta petición y lista las acciones MCP necesarias: 'Consulta el tiempo de Madrid y programa una alarma si va a llover'"

LLM Response: {
  "subtasks": [
    {
      "action": "consultar_tiempo",
      "entities": {"ubicacion": "Madrid"},
      "description": "Consultar información meteorológica de Madrid"
    },
    {
      "action": "programar_alarma_condicional", 
      "entities": {"condicion": "si_llueve", "ubicacion": "Madrid"},
      "description": "Programar alarma basada en condición meteorológica",
      "depends_on_result": "consultar_tiempo"
    }
  ]
}

PASO 2 - EJECUCIÓN ORQUESTADA:
- Estado inicial: PENDING [consultar_tiempo, programar_alarma_condicional]
- Ejecuta: consultar_tiempo(Madrid) → Resultado: "Lluvia probable 70%"  
- Estado: PENDING [programar_alarma_condicional], COMPLETED [consultar_tiempo]
- Ejecuta: programar_alarma_condicional(condicion="si_llueve") → Resultado: "Alarma creada"
- Estado: COMPLETED [consultar_tiempo, programar_alarma_condicional]
- CONVERSACIÓN COMPLETADA: "En Madrid hay 70% probabilidad de lluvia. He programado una alarma para avisarte."
```

### Configuración de MCPs Disponibles (mcp_registry.json)
```json
{
  "available_mcps": {
    "consultar_tiempo": {
      "description": "Consulta información meteorológica de una ubicación",
      "required_entities": ["ubicacion"],
      "service_endpoint": "weather-mcp-service"
    },
    "programar_alarma": {
      "description": "Programa una alarma para una fecha/hora específica", 
      "required_entities": ["fecha_hora", "mensaje"],
      "service_endpoint": "system-mcp-service"
    },
    "programar_alarma_condicional": {
      "description": "Programa alarma basada en condiciones externas",
      "required_entities": ["condicion"],
      "service_endpoint": "system-mcp-service"
    },
    "crear_github_issue": {
      "description": "Crea un issue en un repositorio de GitHub",
      "required_entities": ["titulo", "descripcion"],
      "service_endpoint": "github-mcp-service"
    }
  }
}
```