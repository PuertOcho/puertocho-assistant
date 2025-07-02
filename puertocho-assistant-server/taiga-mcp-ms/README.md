# Taiga MCP Service

Microservicio HTTP que proporciona una interfaz API REST para [Taiga Project Manager](https://taiga.io/) con capacidades avanzadas de IA, compatible 100% con el protocolo [Model Context Protocol (MCP)](https://github.com/talhaorak/pytaiga-mcp).

## 🌟 Características Principales

- **🔐 Autenticación por sesiones**: Gestión segura de tokens de Taiga
- **📋 CRUD completo**: Proyectos, historias, epics, tasks, issues, milestones
- **🤖 IA integrada**: Acciones complejas con texto natural
- **⚡ Alto rendimiento**: Hasta 20x más rápido que múltiples llamadas API
- **🔄 Compatible MCP**: 100% compatible con pytaiga-mcp + extensiones exclusivas
- **🛡️ Listo para producción**: Docker, health checks, logging

## 🚀 Inicio Rápido

### 1. Configuración

```bash
# Clonar y configurar
git clone <repositorio>
cd taiga-mcp-ms

# Configurar variables de entorno
cp .env.example .env
# Editar .env con tus credenciales de Taiga
```

### 2. Ejecutar con Docker

```bash
# Opción 1: Script automatizado
./start.sh

# Opción 2: Docker Compose manual
docker compose up -d

# Opción 3: Despliegue con validación
scripts/deploy.sh
```

### 3. Verificar funcionamiento

```bash
# Health check básico
curl http://localhost:5007/health

# Health check detallado
python3 scripts/health_check.py

# Pruebas completas
python3 scripts/test_service.py
```

## 📚 Cómo Funciona

### Arquitectura del Microservicio

```
┌─────────────────┐    HTTP     ┌─────────────────┐    HTTP     ┌─────────────────┐
│   Cliente       │ ──────────► │ Taiga MCP       │ ──────────► │ Taiga Server    │
│ (LLM/Frontend)  │ ◄────────── │ Service         │ ◄────────── │                 │
└─────────────────┘   JSON      └─────────────────┘  REST API   └─────────────────┘
                                         │
                                         │ Sesiones en memoria
                                         │ + IA Processing
                                         ▼
                                ┌─────────────────┐
                                │ Funciones IA    │
                                │ - Text parsing  │
                                │ - Auto análisis │
                                │ - Reportes      │
                                └─────────────────┘
```

### Flujo de Autenticación

1. **Login**: Cliente envía credenciales
2. **Token**: Servicio obtiene JWT de Taiga  
3. **Session**: Crea session_id interno (8h duración)
4. **Uso**: Todas las peticiones usan session_id
5. **Logout**: Destruye sesión

### Gestión de Sesiones

```python
# Estructura de sesión interna
{
    "session_id": "taiga_session_20250701_120000_123456",
    "auth_token": "jwt_token_from_taiga",
    "user_id": 42,
    "username": "puertocho", 
    "host": "http://localhost:9000",
    "created_at": "2025-07-01T12:00:00",
    "expires_at": "2025-07-01T20:00:00"
}
```

## 🛠️ API Reference

### Autenticación

#### `POST /login`
Iniciar sesión en Taiga.

```bash
curl -X POST http://localhost:5007/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tu_usuario",
    "password": "tu_password", 
    "host": "http://localhost:9000"
  }'
```

**Respuesta:**
```json
{
  "session_id": "taiga_session_20250701_120000_123456",
  "user": {
    "id": 42,
    "username": "puertocho",
    "full_name": "Puerto Ocho",
    "email": "user@example.com"
  },
  "expires_at": "2025-07-01T20:00:00",
  "host": "http://localhost:9000"
}
```

#### `POST /logout`
Cerrar sesión.

```bash
curl -X POST http://localhost:5007/logout \
  -H "Content-Type: application/json" \
  -d '{"session_id": "taiga_session_20250701_120000_123456"}'
```

### Proyectos

#### `GET /projects` - Listar proyectos

```bash
curl "http://localhost:5007/projects?session_id=taiga_session_..."
```

#### `POST /projects` - Crear proyecto

```bash
curl -X POST http://localhost:5007/projects \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "taiga_session_...",
    "name": "Mi Nuevo Proyecto",
    "description": "Descripción del proyecto"
  }'
```

#### `GET /projects/{id}` - Obtener proyecto específico

```bash
curl "http://localhost:5007/projects/42?session_id=taiga_session_..."
```

### User Stories

#### `GET /projects/{id}/user_stories` - Listar historias

```bash
curl "http://localhost:5007/projects/42/user_stories?session_id=taiga_session_..."
```

#### `POST /projects/{id}/user_stories` - Crear historia

```bash
curl -X POST http://localhost:5007/projects/42/user_stories \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "taiga_session_...",
    "subject": "Como usuario, quiero...",
    "description": "Descripción detallada"
  }'
```

#### `PUT /user_stories/{id}` - Actualizar historia

```bash
curl -X PUT http://localhost:5007/user_stories/123 \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "taiga_session_...",
    "subject": "Historia actualizada",
    "description": "Nueva descripción"
  }'
```

### Epics

#### `GET /projects/{id}/epics` - Listar epics

```bash
curl "http://localhost:5007/projects/42/epics?session_id=taiga_session_..."
```

#### `POST /projects/{id}/epics` - Crear epic

```bash
curl -X POST http://localhost:5007/projects/42/epics \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "taiga_session_...",
    "subject": "Epic de Autenticación",
    "description": "Sistema completo de auth",
    "color": "#2ca02c"
  }'
```

### Tasks

#### `GET /projects/{id}/tasks` - Listar tasks

#### `POST /projects/{id}/tasks` - Crear task

```bash
curl -X POST http://localhost:5007/projects/42/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "taiga_session_...",
    "subject": "Implementar login OAuth",
    "description": "Integrar OAuth con Google",
    "user_story_id": 123
  }'
```

### Issues

#### `GET /projects/{id}/issues` - Listar issues

#### `POST /projects/{id}/issues` - Crear issue

```bash
curl -X POST http://localhost:5007/projects/42/issues \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "taiga_session_...",
    "subject": "Bug en validación de formularios",
    "description": "Error al validar email",
    "priority": 3,
    "severity": 2,
    "type": 1
  }'
```

### Milestones

#### `GET /projects/{id}/milestones` - Listar milestones

#### `POST /projects/{id}/milestones` - Crear milestone

```bash
curl -X POST http://localhost:5007/projects/42/milestones \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "taiga_session_...",
    "name": "Sprint 1",
    "estimated_start": "2025-07-01",
    "estimated_finish": "2025-07-15"
  }'
```

### Metadatos

#### Estados disponibles
- `GET /projects/{id}/userstory-statuses`
- `GET /projects/{id}/task-statuses` 
- `GET /projects/{id}/issue-statuses`

#### Miembros del proyecto
- `GET /projects/{id}/members`

#### Asignaciones
- `POST /user_stories/{id}/assign`
- `POST /tasks/{id}/assign`

## 🤖 Funcionalidades de IA

### Acciones Complejas con Texto Natural

La funcionalidad estrella del servicio es `execute_complex_action`, que permite ejecutar múltiples operaciones usando texto natural.

#### `POST /execute_complex_action`

```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "taiga_session_...",
    "action_text": "Crear proyecto desde tracker",
    "tracker_content": "# Mi Proyecto\n\n## Fase 1\n- [x] Tarea completada\n- [ ] Tarea pendiente"
  }'
```

### Ejemplos de Acciones de IA

#### 1. Crear Proyecto desde PROJECT_TRACKER.md

```bash
# Leer archivo y enviarlo al servicio
TRACKER=$(cat PROJECT_TRACKER.md)

curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d "{
    \"session_id\": \"$SESSION_ID\",
    \"action_text\": \"Crear proyecto desde tracker para sistema de monitoreo\",
    \"tracker_content\": \"$TRACKER\"
  }"
```

**Resultado**: Proyecto completo con todas las tareas convertidas en historias de usuario.

#### 2. Crear Elementos Específicos

```bash
# Crear epic con color
curl -X POST http://localhost:5007/execute_complex_action \
  -d '{
    "session_id": "...",
    "action_text": "Crear epic \"Sistema de Autenticación\" con color verde",
    "project_id": 42
  }'

# Crear task vinculada a historia
curl -X POST http://localhost:5007/execute_complex_action \
  -d '{
    "session_id": "...", 
    "action_text": "Crear task \"Implementar OAuth\" para user story #1",
    "project_id": 42
  }'

# Reportar bug crítico  
curl -X POST http://localhost:5007/execute_complex_action \
  -d '{
    "session_id": "...",
    "action_text": "Reportar bug crítico \"Error en validación de formularios\"",
    "project_id": 42
  }'
```

#### 3. Análisis y Reportes

```bash
# Analizar proyecto
curl -X POST http://localhost:5007/execute_complex_action \
  -d '{
    "session_id": "...",
    "action_text": "Analizar proyecto con foco en testing y documentación", 
    "project_id": 42
  }'

# Generar reporte completo
curl -X POST http://localhost:5007/execute_complex_action \
  -d '{
    "session_id": "...",
    "action_text": "Generar reporte detallado del proyecto",
    "project_id": 42  
  }'
```

#### 4. Operaciones Masivas

```bash
# Listar todos los elementos
curl -X POST http://localhost:5007/execute_complex_action \
  -d '{
    "session_id": "...",
    "action_text": "Listar todos los elementos del proyecto",
    "project_id": 42
  }'

# Asignar elementos
curl -X POST http://localhost:5007/execute_complex_action \
  -d '{
    "session_id": "...",
    "action_text": "Asignar task #5 a usuario 2",
    "project_id": 42
  }'
```

## 🔧 Integración con Otros Servicios

### Integración con Java/Spring Boot

Ver ejemplos completos en [`examples/integration-examples/`](examples/integration-examples/):

- `TaigaMCPService.java` - Clase de servicio completa
- `TaigaMCPController.java` - Controlador REST de ejemplo

#### Ejemplo básico:

```java
@Service
public class TaigaMCPService {
    private final RestTemplate restTemplate = new RestTemplate();
    
    public String authenticate(String username, String password) {
        Map<String, String> request = Map.of(
            "username", username,
            "password", password,
            "host", taigaHost
        );
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            mcpBaseUrl + "/login", request, Map.class
        );
        
        return (String) response.getBody().get("session_id");
    }
    
    public List<Project> getProjects(String sessionId) {
        String url = mcpBaseUrl + "/projects?session_id=" + sessionId;
        ProjectsResponse response = restTemplate.getForObject(url, ProjectsResponse.class);
        return response.getProjects();
    }
}
```

### Integración con Frontend

```javascript
// Cliente JavaScript
class TaigaMCPClient {
    constructor(baseUrl = 'http://localhost:5007') {
        this.baseUrl = baseUrl;
        this.sessionId = null;
    }
    
    async login(username, password, host) {
        const response = await fetch(`${this.baseUrl}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, host })
        });
        
        const data = await response.json();
        this.sessionId = data.session_id;
        return data;
    }
    
    async getProjects() {
        const response = await fetch(`${this.baseUrl}/projects?session_id=${this.sessionId}`);
        return response.json();
    }
    
    async executeComplexAction(actionText, projectId = null, trackerContent = null) {
        const response = await fetch(`${this.baseUrl}/execute_complex_action`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                session_id: this.sessionId,
                action_text: actionText,
                project_id: projectId,
                tracker_content: trackerContent
            })
        });
        
        return response.json();
    }
}
```

## 🧪 Testing y Validación

### Scripts de Prueba

```bash
# Pruebas básicas de endpoints
python3 scripts/test_service.py

# Pruebas de compatibilidad MCP completa
python3 scripts/test_full_mcp_compatibility.py

# Pruebas de acciones complejas
python3 scripts/test_complex_actions.py

# Health check
python3 scripts/health_check.py
```

### Validación Manual

```bash
# 1. Verificar servicio
curl http://localhost:5007/health

# 2. Autenticarse
SESSION_ID=$(curl -s -X POST http://localhost:5007/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin","host":"http://localhost:9000"}' | \
  python3 -c "import sys, json; print(json.load(sys.stdin)['session_id'])")

# 3. Probar funcionalidad
curl "http://localhost:5007/projects?session_id=$SESSION_ID"

# 4. Cerrar sesión
curl -X POST http://localhost:5007/logout \
  -H "Content-Type: application/json" \
  -d "{\"session_id\":\"$SESSION_ID\"}"
```

## 📊 Monitoreo y Logs

### Health Check

```bash
# Endpoint de salud
curl http://localhost:5007/health

# Respuesta esperada:
{
  "status": "ok",
  "service": "taiga-mcp",
  "taiga_host": "http://localhost:9000",
  "taiga_available": true,
  "active_sessions": 3
}
```

### Logs del Servicio

```bash
# Ver logs en tiempo real
docker compose logs -f taiga-mcp-api

# Ver logs específicos
docker logs taiga-mcp-api

# Logs con timestamps
docker compose logs -f -t taiga-mcp-api
```

## ⚙️ Configuración

### Variables de Entorno

```bash
# Configuración básica
FLASK_HOST=0.0.0.0
FLASK_PORT=5007
TAIGA_HOST=http://localhost:9000
TAIGA_USERNAME=admin
TAIGA_PASSWORD=admin

# Configuración avanzada
SESSION_EXPIRY=28800    # 8 horas
REQUEST_TIMEOUT=30      # 30 segundos
LOG_LEVEL=INFO
```

### Docker Compose

```yaml
version: '3.8'
services:
  taiga-mcp-api:
    build: .
    ports:
      - "5007:5007"
    environment:
      - TAIGA_HOST=http://host.docker.internal:9000
      - TAIGA_USERNAME=admin
      - TAIGA_PASSWORD=admin
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

## 🔗 Documentación Adicional

- **[Guía de Acciones Complejas](docs/COMPLEX_ACTIONS_GUIDE.md)** - Detalles sobre funcionalidades de IA
- **[Reporte de Compatibilidad MCP](docs/MCP_COMPATIBILITY_REPORT.md)** - Comparativa con pytaiga-mcp
- **[Ejemplos de Integración](examples/integration-examples/)** - Código Java y otros ejemplos

## 🤝 Compatibilidad

### 100% Compatible con pytaiga-mcp

Este servicio implementa **todas las herramientas** del [proyecto pytaiga-mcp](https://github.com/talhaorak/pytaiga-mcp):

- ✅ Autenticación y sesiones
- ✅ CRUD de proyectos
- ✅ CRUD de user stories  
- ✅ CRUD de epics
- ✅ CRUD de tasks
- ✅ CRUD de issues
- ✅ Gestión de milestones
- ✅ Metadatos y estados
- ✅ Gestión de miembros
- ✅ Asignaciones

### Extensiones Exclusivas

- 🌟 **Acciones complejas con IA**: Texto natural → Múltiples operaciones
- 🌟 **Importación automática**: PROJECT_TRACKER.md → Proyecto completo  
- 🌟 **Análisis inteligente**: Recomendaciones automáticas
- 🌟 **Reportes dinámicos**: Dashboards generados automáticamente

## 📈 Rendimiento

| Operación | Método Tradicional | Con Taiga MCP Service | Mejora |
|-----------|-------------------|----------------------|--------|
| Crear proyecto con 20 historias | 21 llamadas API | 1 llamada | **21x más rápido** |
| Análisis de proyecto | Manual | Automático | **∞ más eficiente** |
| Reporte completo | 10+ llamadas | 1 llamada | **10x más rápido** |

## 🚀 Próximos Pasos

1. **Integración con LLMs externos** (GPT, Claude) para mejor interpretación de texto
2. **Soporte para más formatos** (YAML, JSON, Jira export)
3. **Dashboards en tiempo real** generados desde texto
4. **Plantillas de proyecto** inteligentes
5. **Integración con CI/CD** para automatización completa

---

## 📄 Licencia

MIT License - Ver [LICENSE](LICENSE) para más detalles.

## 🙋‍♂️ Soporte

Para preguntas, issues o contribuciones, contacta al equipo de desarrollo. 