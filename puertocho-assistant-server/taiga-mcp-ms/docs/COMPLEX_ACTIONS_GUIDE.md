# Guía de Acciones Complejas - Taiga MCP Service

Esta guía describe las potentes funcionalidades de IA del servicio Taiga MCP que permiten ejecutar acciones complejas usando texto natural.

## 🤖 ¿Qué son las Acciones Complejas?

Las acciones complejas te permiten interactuar con Taiga usando **texto natural** en lugar de múltiples llamadas API. El servicio interpreta tu texto y ejecuta automáticamente las acciones correspondientes.

## 🚀 Tipos de Acciones Soportadas

### 1. 📋 Crear Proyecto desde PROJECT_TRACKER.md

Convierte automáticamente un archivo PROJECT_TRACKER.md en un proyecto completo con todas sus historias de usuario.

**Ejemplo de uso:**
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id",
    "action_text": "Crear proyecto desde tracker",
    "tracker_content": "# Mi Proyecto\n\n## Fase 1: Setup\n- [x] Configurar entorno\n- [ ] Instalar dependencias"
  }'
```

**Resultado:**
- ✅ Proyecto creado con nombre extraído del tracker
- ✅ Historias de usuario generadas para cada tarea
- ✅ Estados preservados (completadas/pendientes)
- ✅ Organización por fases

### 2. 🚀 Crear Proyecto Simple

Crea un proyecto con nombre específico usando texto natural.

**Ejemplo:**
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id",
    "action_text": "Crear proyecto \"Sistema de Pagos\" para gestionar transacciones en línea"
  }'
```

### 3. 📝 Crear Historias de Usuario

Genera historias de usuario usando patrones de texto natural.

**Ejemplo con patrón "Como...quiero...para":**
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id",
    "project_id": 1,
    "action_text": "Crear historia: Como administrador, quiero generar reportes mensuales para tomar decisiones estratégicas."
  }'
```

### 4. 📊 Análisis de Proyecto

Analiza el estado de un proyecto y genera recomendaciones automáticas.

**Ejemplo:**
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id",
    "project_id": 1,
    "action_text": "Analizar proyecto y generar recomendaciones con foco en testing y documentación"
  }'
```

**Respuesta incluye:**
- 📈 Estadísticas de progreso
- 📝 Número de historias completadas/pendientes
- 💡 Recomendaciones personalizadas basadas en el contexto

### 5. 📈 Generar Reportes

Crea reportes detallados de proyectos o resúmenes generales.

**Reporte de proyecto específico:**
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id",
    "project_id": 1,
    "action_text": "Generar reporte detallado del proyecto"
  }'
```

**Reporte general:**
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id",
    "action_text": "Generar reporte general de todos los proyectos"
  }'
```

### 6. 🔍 Parsing Genérico

Interpreta listas de tareas y comandos desde texto libre.

**Ejemplo:**
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id",
    "action_text": "Lista de tareas:\n- Implementar autenticación\n- Configurar base de datos\n- Crear APIs"
  }'
```

## 🛠️ Casos de Uso Reales

### Caso 1: Importar Proyecto de openWakeWord

**Comando ejecutado exitosamente:**
```python
# Usando el PROJECT_TRACKER.md real del proyecto
action_data = {
    'session_id': session_id,
    'action_text': 'Crear proyecto desde tracker para openWakeWord',
    'tracker_content': '<contenido del PROJECT_TRACKER.md>'
}
```

**Resultado:**
- ✅ Proyecto "Configuración optimizada para Raspberry Pi" creado
- ✅ **83 historias de usuario** generadas automáticamente
- ✅ Todas las fases del proyecto organizadas (Preparación, Integración, Optimización, Entrenamiento, etc.)

### Caso 2: Flujo Completo E-commerce

```python
# 1. Crear proyecto desde tracker
ecommerce_tracker = """
# Plataforma E-commerce Avanzada

## Fase 1: Configuración inicial ✅ COMPLETADA
- [x] Configurar entorno de desarrollo
- [x] Setup de autenticación básica

## Fase 2: Gestión de productos
- [ ] Crear catálogo de productos
- [ ] Implementar búsqueda y filtros
"""

# 2. Analizar y obtener recomendaciones
analysis = service.analyzeProject(project_id, "con foco en testing")

# 3. Generar reporte final
report = service.generateReport(project_id)
```

## 🎯 Ventajas de las Acciones Complejas

### ⚡ **Productividad**
- **Antes**: 20+ llamadas API para crear proyecto con historias
- **Ahora**: 1 sola llamada con texto natural

### 🧠 **Inteligencia**
- Interpreta contexto y patrones del texto
- Extrae automáticamente información estructurada
- Genera recomendaciones contextuales

### 🔄 **Flexibilidad**
- Acepta formatos de texto variados
- Se adapta a diferentes estilos de escritura
- Maneja múltiples idiomas

### 📊 **Análisis Automático**
- Evalúa progreso de proyectos
- Identifica patrones y problemas
- Sugiere próximos pasos

## 🔧 Integración con intentmanagerms

### Usar el TaigaMCPService Mejorado

```java
@Autowired
private TaigaMCPService taigaMCPService;

// Flujo completo desde un archivo tracker
@PostMapping("/create-project-from-file")
public ResponseEntity<ProjectFromTrackerResult> createFromFile(
        @RequestParam("file") MultipartFile trackerFile) {
    
    String content = new String(trackerFile.getBytes(), StandardCharsets.UTF_8);
    ProjectFromTrackerResult result = taigaMCPService.createProjectFromTracker(content);
    
    return ResponseEntity.ok(result);
}

// Análisis con recomendaciones
@GetMapping("/project/{id}/analyze")
public ResponseEntity<ProjectAnalysisResult> analyzeProject(
        @PathVariable Integer id) {
    
    ProjectAnalysisResult analysis = taigaMCPService.analyzeProject(
        id, "análisis completo con foco en calidad"
    );
    
    return ResponseEntity.ok(analysis);
}

// Flujo asíncrono completo
@PostMapping("/full-project-flow")
public CompletableFuture<FullProjectFlowResult> createFullProject(
        @RequestBody ProjectFlowRequest request) {
    
    return taigaMCPService.executeFullProjectFlow(
        request.getIdea(),
        request.getTrackerContent(),
        request.getAdditionalStories()
    );
}
```

## 📋 Formato de Respuesta

Todas las acciones complejas devuelven la siguiente estructura:

```json
{
  "action_text": "Texto de la acción ejecutada",
  "results": [
    {
      "action": "create_project_from_tracker",
      "success": true,
      "project": {
        "id": 9,
        "name": "Mi Proyecto",
        "slug": "mi-proyecto"
      },
      "created_stories": [
        {
          "id": 1,
          "ref": "#1",
          "subject": "Fase 1: Configurar entorno"
        }
      ],
      "summary": "Proyecto 'Mi Proyecto' creado con 15 historias de usuario"
    }
  ],
  "timestamp": "2025-07-01T17:13:03.422173",
  "session_id": "puertocho"
}
```

## 🔍 Patrones de Texto Reconocidos

### Para Proyectos
- `"Crear proyecto desde tracker"`
- `"Crear proyecto 'Nombre del Proyecto'"`
- `"Nuevo proyecto llamado 'X'"`

### Para Historias
- `"Crear historia: Como [rol], quiero [acción] para [objetivo]"`
- `"Como [usuario], necesito [funcionalidad]"`

### Para Análisis
- `"Analizar proyecto"`
- `"Planificar proyecto con foco en [área]"`
- `"Revisar progreso del proyecto"`

### Para Reportes
- `"Generar reporte"`
- `"Crear reporte del proyecto"`
- `"Reporte general de proyectos"`

## 🚀 Próximas Mejoras

### En desarrollo:
- 🤖 **Integración con LLMs externos** (GPT, Claude) para mejor interpretación
- 📝 **Soporte para más formatos** (YAML, JSON, Jira export)
- 🔄 **Actualizaciones masivas** automáticas
- 📊 **Dashboards dinámicos** generados desde texto
- 🎯 **Plantillas de proyecto** inteligentes

### Casos de uso avanzados planeados:
- **"Migrar proyecto de Jira a Taiga"**
- **"Generar roadmap automático desde descripción"**
- **"Crear sprints balanceados automáticamente"**
- **"Optimizar asignación de recursos usando IA"**

## 💡 Tips y Mejores Prácticas

### ✅ Recomendado
- Usa frases claras y específicas
- Incluye contexto cuando sea relevante
- Aprovecha los patrones de historias de usuario
- Combina múltiples acciones en flujos

### ❌ Evita
- Texto muy ambiguo o genérico
- Mezclar múltiples tipos de acción en una sola petición
- Omitir project_id cuando sea necesario

### 🎯 Casos de Uso Óptimos
1. **Migración de proyectos**: Desde archivos de tracking existentes
2. **Prototipado rápido**: Crear proyectos desde ideas textuales
3. **Automatización**: Análisis y reportes periódicos
4. **Integración**: Con sistemas de IA para gestión inteligente

---

🎉 **¡Las acciones complejas transforman la gestión de proyectos en Taiga!** 

En lugar de navegar manualmente por múltiples pantallas y formularios, simplemente describe lo que quieres hacer en texto natural y el sistema se encarga del resto. 