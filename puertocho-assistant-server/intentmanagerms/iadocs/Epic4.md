# Epic 4 – Sistema Conversacional Inteligente + Orquestación de Subtareas

> **Estado**: En Progreso
> **Fecha de inicio**: 2025-01-27
> **Dependencias**: Epic 1, Epic 2, Epic 3 (Completados)

## Descripción del Epic

Desarrollar sistema conversacional avanzado que usa LLM para descomponer dinámicamente peticiones complejas en múltiples subtareas ejecutables. NO usa configuraciones predefinidas, sino que el LLM analiza cada petición y identifica automáticamente qué MCPs/servicios necesita invocar. Mantiene estado de progreso y marca conversación como completada solo cuando todas las subtareas están ejecutadas exitosamente.

### Objetivos Clave

- **Conversación multivuelta** con memoria contextual persistente
- **Slot-filling automático** usando LLM para preguntas dinámicas  
- **Descomposición dinámica**: LLM identifica automáticamente múltiples acciones en una petición
- **Orquestador inteligente**: Ejecuta subtareas secuencial o paralelamente según dependencias detectadas
- **Estado de progreso**: Tracking automático hasta completion de todas las subtareas
- **Manejo de anáforas** y referencias contextuales

## Tareas del Epic

### T4.1 - Diseñar `ConversationManager` con contexto LLM-powered
**Estado**: ⏳ Pendiente  
**Dependencias**: T2.1 (RagIntentClassifier)  
**Descripción**: Crear el gestor principal de conversaciones que mantiene contexto persistente y coordina el flujo conversacional.

**Componentes a implementar**:
- `ConversationManager`: Gestor principal de conversaciones
- `ConversationSession`: Modelo de sesión conversacional
- `ConversationContext`: Contexto persistente de la conversación
- `ConversationState`: Estados de la conversación (ACTIVE, WAITING_SLOTS, EXECUTING_TASKS, COMPLETED, ERROR)

**Funcionalidades**:
- Gestión de sesiones conversacionales con Redis
- Mantenimiento de contexto histórico
- Coordinación con RagIntentClassifier
- Integración con sistema de votación MoE
- Manejo de estados de conversación

### T4.2 - Implementar slot-filling automático usando LLM para preguntas dinámicas
**Estado**: ⏳ Pendiente  
**Dependencias**: T4.1  
**Descripción**: Sistema de llenado automático de slots usando LLM para generar preguntas contextuales dinámicas.

**Componentes a implementar**:
- `SlotFillingService`: Servicio de llenado de slots
- `DynamicQuestionGenerator`: Generador de preguntas dinámicas
- `SlotValidator`: Validador de slots completados
- `SlotExtractor`: Extractor de información de slots

**Funcionalidades**:
- Análisis LLM de slots faltantes
- Generación de preguntas contextuales
- Validación inteligente de respuestas
- Manejo de slots opcionales vs obligatorios

### T4.3 - Crear `EntityExtractor` basado en LLM para extracción contextual
**Estado**: ⏳ Pendiente  
**Dependencias**: T4.1  
**Descripción**: Extractor de entidades basado en LLM que identifica entidades contextuales en el texto.

**Componentes a implementar**:
- `EntityExtractor`: Extractor principal de entidades
- `EntityRecognizer`: Reconocedor de patrones de entidades
- `EntityValidator`: Validador de entidades extraídas
- `EntityResolver`: Resolutor de ambigüedades

**Funcionalidades**:
- Extracción de entidades nombradas
- Resolución de referencias anafóricas
- Validación contextual de entidades
- Integración con sistema de intenciones

### T4.4 - Desarrollar memoria conversacional con Redis para sesiones persistentes
**Estado**: ⏳ Pendiente  
**Dependencias**: T4.1  
**Descripción**: Sistema de memoria conversacional persistente usando Redis.

**Componentes a implementar**:
- `ConversationMemoryService`: Servicio de memoria conversacional
- `RedisConversationRepository`: Repositorio Redis para conversaciones
- `MemoryManager`: Gestor de memoria y limpieza
- `ContextPersistenceService`: Servicio de persistencia de contexto

**Funcionalidades**:
- Almacenamiento persistente en Redis
- Gestión de TTL de sesiones
- Compresión de contexto histórico
- Limpieza automática de sesiones expiradas

### T4.5 - Crear `DynamicSubtaskDecomposer` - LLM analiza petición y identifica múltiples acciones automáticamente
**Estado**: ⏳ Pendiente  
**Dependencias**: T4.1  
**Descripción**: Componente que analiza peticiones complejas y las descompone en subtareas ejecutables.

**Componentes a implementar**:
- `DynamicSubtaskDecomposer`: Descomponedor principal
- `TaskAnalyzer`: Analizador de tareas
- `DependencyResolver`: Resolutor de dependencias
- `TaskValidator`: Validador de tareas generadas

**Funcionalidades**:
- Análisis LLM de peticiones complejas
- Identificación automática de múltiples acciones
- Detección de dependencias entre tareas
- Generación de plan de ejecución

### T4.6 - Implementar `TaskOrchestrator` para ejecución secuencial/paralela de subtareas detectadas dinámicamente
**Estado**: ⏳ Pendiente  
**Dependencias**: T4.5  
**Descripción**: Orquestador que ejecuta subtareas según dependencias y optimiza el rendimiento.

**Componentes a implementar**:
- `TaskOrchestrator`: Orquestador principal
- `ExecutionEngine`: Motor de ejecución
- `DependencyManager`: Gestor de dependencias
- `ParallelExecutor`: Ejecutor paralelo

**Funcionalidades**:
- Ejecución secuencial de tareas dependientes
- Ejecución paralela de tareas independientes
- Gestión de errores y rollback
- Optimización de rendimiento

### T4.7 - Desarrollar sistema de estado de progreso: tracking automático hasta completion de todas las subtareas
**Estado**: ⏳ Pendiente  
**Dependencias**: T4.6  
**Descripción**: Sistema de seguimiento de progreso que monitorea el estado de todas las subtareas.

**Componentes a implementar**:
- `ProgressTracker`: Seguidor de progreso
- `TaskStatusManager`: Gestor de estados de tareas
- `CompletionValidator`: Validador de completitud
- `ProgressNotifier`: Notificador de progreso

**Funcionalidades**:
- Tracking en tiempo real de progreso
- Estados detallados de cada subtarea
- Notificaciones de progreso
- Validación de completitud

### T4.8 - Implementar resolución de anáforas y referencias contextuales
**Estado**: ⏳ Pendiente  
**Dependencias**: T4.4  
**Descripción**: Sistema que resuelve referencias anafóricas y contextuales en conversaciones.

**Componentes a implementar**:
- `AnaphoraResolver`: Resolutor de anáforas
- `ReferenceTracker`: Seguidor de referencias
- `ContextResolver`: Resolutor de contexto
- `AmbiguityResolver`: Resolutor de ambigüedades

**Funcionalidades**:
- Resolución de pronombres
- Seguimiento de referencias
- Resolución de ambigüedades
- Mantenimiento de contexto

## Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                    ConversationManager                          │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Session   │  │   Context   │  │   State     │            │
│  │  Manager    │  │  Manager    │  │  Manager    │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 SlotFillingService                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  Dynamic    │  │   Slot      │  │   Slot      │            │
│  │  Question   │  │ Validator   │  │ Extractor   │            │
│  │ Generator   │  │             │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                DynamicSubtaskDecomposer                         │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Task      │  │ Dependency  │  │   Task      │            │
│  │ Analyzer    │  │ Resolver    │  │ Validator   │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  TaskOrchestrator                               │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ Execution   │  │ Dependency  │  │  Parallel   │            │
│  │  Engine     │  │  Manager    │  │  Executor   │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  ProgressTracker                                │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Task      │  │ Completion  │  │  Progress   │            │
│  │  Status     │  │ Validator   │  │ Notifier    │            │
│  │  Manager    │  │             │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
```

## Flujo de Conversación

### 1. Inicio de Conversación
```
Usuario: "Consulta el tiempo de Madrid y programa una alarma si va a llover"

ConversationManager:
1. Crea nueva sesión
2. Analiza petición con LLM
3. Identifica múltiples acciones
4. Inicia proceso de descomposición
```

### 2. Descomposición Dinámica
```
DynamicSubtaskDecomposer:
{
  "subtasks": [
    {
      "id": "task_001",
      "action": "consultar_tiempo",
      "entities": {"ubicacion": "Madrid"},
      "dependencies": [],
      "priority": "high"
    },
    {
      "id": "task_002", 
      "action": "programar_alarma_condicional",
      "entities": {"condicion": "si_llueve"},
      "dependencies": ["task_001"],
      "priority": "medium"
    }
  ]
}
```

### 3. Ejecución Orquestada
```
TaskOrchestrator:
1. Ejecuta task_001 (consultar_tiempo)
2. Espera resultado
3. Ejecuta task_002 (programar_alarma_condicional)
4. Monitorea progreso
```

### 4. Completitud
```
ProgressTracker:
- task_001: COMPLETED
- task_002: COMPLETED
- Conversación: COMPLETED

Respuesta: "En Madrid hay 70% probabilidad de lluvia. He programado una alarma para avisarte."
```

## Configuración JSON

### conversation_config.json
```json
{
  "conversation": {
    "session_timeout_minutes": 30,
    "max_conversation_turns": 50,
    "context_compression_threshold": 10,
    "enable_anaphora_resolution": true,
    "enable_dynamic_decomposition": true,
    "enable_parallel_execution": true,
    "max_parallel_tasks": 3,
    "progress_update_interval_ms": 1000
  },
  "slot_filling": {
    "enable_dynamic_questions": true,
    "max_slot_attempts": 3,
    "confidence_threshold": 0.7,
    "enable_context_aware_questions": true
  },
  "task_decomposition": {
    "enable_llm_analysis": true,
    "max_subtasks_per_request": 5,
    "enable_dependency_detection": true,
    "enable_priority_assignment": true
  },
  "execution": {
    "enable_parallel_execution": true,
    "max_parallel_tasks": 3,
    "enable_error_recovery": true,
    "enable_rollback_on_failure": true
  }
}
```

## Variables de Entorno

```bash
# Conversation Configuration
CONVERSATION_SESSION_TIMEOUT_MINUTES=30
CONVERSATION_MAX_TURNS=50
CONVERSATION_CONTEXT_COMPRESSION_THRESHOLD=10
CONVERSATION_ENABLE_ANAPHORA_RESOLUTION=true
CONVERSATION_ENABLE_DYNAMIC_DECOMPOSITION=true

# Slot Filling Configuration
SLOT_FILLING_ENABLE_DYNAMIC_QUESTIONS=true
SLOT_FILLING_MAX_ATTEMPTS=3
SLOT_FILLING_CONFIDENCE_THRESHOLD=0.7
SLOT_FILLING_ENABLE_CONTEXT_AWARE_QUESTIONS=true

# Task Decomposition Configuration
TASK_DECOMPOSITION_ENABLE_LLM_ANALYSIS=true
TASK_DECOMPOSITION_MAX_SUBTASKS=5
TASK_DECOMPOSITION_ENABLE_DEPENDENCY_DETECTION=true
TASK_DECOMPOSITION_ENABLE_PRIORITY_ASSIGNMENT=true

# Execution Configuration
EXECUTION_ENABLE_PARALLEL=true
EXECUTION_MAX_PARALLEL_TASKS=3
EXECUTION_ENABLE_ERROR_RECOVERY=true
EXECUTION_ENABLE_ROLLBACK_ON_FAILURE=true

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=0
REDIS_CONVERSATION_TTL=1800
```

## Casos de Uso

### Caso 1: Petición Simple
```
Usuario: "¿Qué tiempo hace en Barcelona?"
ConversationManager: Clasifica como consultar_tiempo
SlotFilling: Detecta ubicación "Barcelona"
Ejecución: Llama a weather-mcp
Respuesta: "En Barcelona hace 22°C y está soleado"
```

### Caso 2: Petición Compleja
```
Usuario: "Consulta el tiempo de Madrid, programa una alarma si va a llover, y crea un issue en GitHub sobre el bug del weather"

DynamicSubtaskDecomposer:
1. consultar_tiempo(Madrid)
2. programar_alarma_condicional(si_llueve) [depende de 1]
3. crear_github_issue(weather_bug) [independiente]

TaskOrchestrator:
- Ejecuta 1 y 3 en paralelo
- Espera resultado de 1
- Ejecuta 2 si condición se cumple
```

### Caso 3: Conversación Multivuelta
```
Usuario: "Enciende las luces del salón"
Sistema: "¿Quieres que también ajuste la intensidad?"
Usuario: "Sí, al 80%"
Sistema: "Luz encendida al 80% en el salón"
```

## Métricas y Monitoreo

### Métricas de Conversación
- Tiempo promedio de conversación
- Número de vueltas por conversación
- Tasa de completitud de conversaciones
- Tiempo de respuesta promedio

### Métricas de Descomposición
- Número promedio de subtareas por petición
- Tiempo de análisis de descomposición
- Precisión de detección de dependencias
- Tasa de éxito de ejecución paralela

### Métricas de Slot Filling
- Número promedio de preguntas por slot
- Tasa de éxito de llenado de slots
- Tiempo promedio de llenado de slots
- Precisión de validación de slots

## Pruebas y Validación

### Tests Unitarios
- `ConversationManagerTest`
- `SlotFillingServiceTest`
- `DynamicSubtaskDecomposerTest`
- `TaskOrchestratorTest`
- `ProgressTrackerTest`

### Tests de Integración
- `ConversationFlowTest`
- `ComplexRequestDecompositionTest`
- `MultiTurnConversationTest`
- `ParallelExecutionTest`

### Tests de Rendimiento
- `ConversationPerformanceTest`
- `DecompositionPerformanceTest`
- `ParallelExecutionPerformanceTest`
- `MemoryUsageTest`

## Dependencias Técnicas

### Dependencias Internas
- Epic 1: Arquitectura Base (✅ Completado)
- Epic 2: Motor RAG (✅ Completado)
- Epic 3: MoE Voting System (✅ Completado)

### Dependencias Externas
- Redis para persistencia de sesiones
- LLM APIs para análisis y descomposición
- MCP Services para ejecución de acciones

## Riesgos y Mitigaciones

### Riesgos Identificados
1. **Complejidad de descomposición**: LLM puede generar subtareas incorrectas
2. **Gestión de dependencias**: Dependencias circulares o mal detectadas
3. **Rendimiento**: Análisis LLM puede ser lento
4. **Memoria**: Acumulación de contexto histórico

### Estrategias de Mitigación
1. **Validación robusta**: Múltiples validadores de subtareas
2. **Detección de ciclos**: Algoritmos de detección de dependencias circulares
3. **Caching inteligente**: Cache de análisis de peticiones similares
4. **Compresión de contexto**: Algoritmos de compresión de contexto histórico

## Criterios de Aceptación

### T4.1 - ConversationManager
- ✅ Gestión completa de sesiones conversacionales
- ✅ Integración con RagIntentClassifier
- ✅ Manejo de estados de conversación
- ✅ Persistencia en Redis
- ✅ Tests unitarios y de integración

### T4.2 - Slot Filling
- ✅ Generación dinámica de preguntas
- ✅ Validación inteligente de respuestas
- ✅ Manejo de slots opcionales/obligatorios
- ✅ Integración con contexto conversacional

### T4.3 - Entity Extractor
- ✅ Extracción de entidades nombradas
- ✅ Resolución de anáforas
- ✅ Validación contextual
- ✅ Integración con sistema de intenciones

### T4.4 - Memoria Conversacional
- ✅ Almacenamiento persistente en Redis
- ✅ Gestión de TTL de sesiones
- ✅ Compresión de contexto
- ✅ Limpieza automática

### T4.5 - Dynamic Subtask Decomposer
- ✅ Análisis LLM de peticiones complejas
- ✅ Identificación automática de acciones
- ✅ Detección de dependencias
- ✅ Generación de plan de ejecución

### T4.6 - Task Orchestrator
- ✅ Ejecución secuencial de tareas dependientes
- ✅ Ejecución paralela de tareas independientes
- ✅ Gestión de errores y rollback
- ✅ Optimización de rendimiento

### T4.7 - Progress Tracker
- ✅ Tracking en tiempo real
- ✅ Estados detallados de subtareas
- ✅ Notificaciones de progreso
- ✅ Validación de completitud

### T4.8 - Anaphora Resolution
- ✅ Resolución de pronombres
- ✅ Seguimiento de referencias
- ✅ Resolución de ambigüedades
- ✅ Mantenimiento de contexto

## Estado Actual

**Progreso**: 0/8 tareas completadas (0%)  
**Estado**: En Progreso  
**Próxima tarea**: T4.1 - Diseñar ConversationManager

---

*Documentación actualizada: 2025-01-27*
