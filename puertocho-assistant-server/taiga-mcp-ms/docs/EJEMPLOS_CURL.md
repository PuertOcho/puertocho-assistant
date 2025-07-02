# 🚀 Ejemplos de Comandos Curl - Taiga MCP Service

Este archivo contiene ejemplos prácticos de comandos `curl` para usar el microservicio Taiga MCP con **login automático** (sin necesidad de `session_id`).

## 📋 Información del Servicio

### 🔍 Estado del Servicio
```bash
curl http://localhost:5007/health
```

### 🔑 Información de Sesión Automática
```bash
curl http://localhost:5007/auto-session
```

### 🔄 Renovar Sesión Automática (Manual)
```bash
curl -X POST http://localhost:5007/auto-session/renew
```

## 📂 Gestión de Proyectos

### 📋 Listar Todos los Proyectos
```bash
curl http://localhost:5007/projects
```

### 📝 Crear Nuevo Proyecto
```bash
curl -X POST http://localhost:5007/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mi Nuevo Proyecto",
    "description": "Descripción del proyecto"
  }'
```

### 🔍 Obtener Detalles de un Proyecto
```bash
curl http://localhost:5007/projects/1
```

## 📝 Historias de Usuario

### 📋 Listar Historias de un Proyecto
```bash
curl http://localhost:5007/projects/1/user_stories
```

### ✨ Crear Nueva Historia de Usuario
```bash
curl -X POST http://localhost:5007/projects/1/user_stories \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Como usuario, quiero...",
    "description": "Para poder..."
  }'
```

### ✏️ Actualizar Historia de Usuario
```bash
curl -X PUT http://localhost:5007/user_stories/107 \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Historia actualizada",
    "description": "Nueva descripción"
  }'
```

## 🎯 Epics

### 📋 Listar Epics de un Proyecto
```bash
curl http://localhost:5007/projects/1/epics
```

### ✨ Crear Nuevo Epic
```bash
curl -X POST http://localhost:5007/projects/1/epics \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Epic de Autenticación",
    "description": "Epic para todo lo relacionado con autenticación",
    "color": "#E44057"
  }'
```

### 🔍 Obtener Detalles de un Epic
```bash
curl http://localhost:5007/epics/1
```

### ✏️ Actualizar Epic
```bash
curl -X PUT http://localhost:5007/epics/1 \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Epic actualizado",
    "description": "Nueva descripción",
    "color": "#2ca02c"
  }'
```

### 🗑️ Eliminar Epic
```bash
curl -X DELETE http://localhost:5007/epics/1
```

## 🛠️ Tasks

### 📋 Listar Tasks de un Proyecto
```bash
curl http://localhost:5007/projects/1/tasks
```

### ✨ Crear Nueva Task
```bash
curl -X POST http://localhost:5007/projects/1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Implementar login",
    "description": "Desarrollar funcionalidad de login"
  }'
```

### 🔍 Obtener Detalles de una Task
```bash
curl http://localhost:5007/tasks/1
```

### ✏️ Actualizar Task
```bash
curl -X PUT http://localhost:5007/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Task actualizada",
    "description": "Nueva descripción"
  }'
```

## 🐛 Issues

### 📋 Listar Issues de un Proyecto
```bash
curl http://localhost:5007/projects/1/issues
```

### ✨ Crear Nuevo Issue
```bash
curl -X POST http://localhost:5007/projects/1/issues \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Bug en login",
    "description": "El sistema no permite hacer login",
    "priority_id": 2,
    "severity_id": 2,
    "type_id": 1
  }'
```

### 🔍 Obtener Detalles de un Issue
```bash
curl http://localhost:5007/issues/1
```

## 🎯 Milestones (Sprints)

### 📋 Listar Milestones de un Proyecto
```bash
curl http://localhost:5007/projects/1/milestones
```

### ✨ Crear Nuevo Milestone
```bash
curl -X POST http://localhost:5007/projects/1/milestones \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sprint 1",
    "estimated_start": "2025-01-01",
    "estimated_finish": "2025-01-15"
  }'
```

## 👥 Gestión de Miembros

### 📋 Listar Miembros del Proyecto
```bash
curl http://localhost:5007/projects/1/members
```

## 📊 Metadatos

### 📋 Estados de Historias de Usuario
```bash
curl http://localhost:5007/projects/1/userstory-statuses
```

### 📋 Estados de Tasks
```bash
curl http://localhost:5007/projects/1/task-statuses
```

### 📋 Estados de Issues
```bash
curl http://localhost:5007/projects/1/issue-statuses
```

## 🎯 Asignaciones

### 👤 Asignar Historia de Usuario a Usuario
```bash
curl -X POST http://localhost:5007/user_stories/107/assign \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": 1
  }'
```

### 👤 Asignar Task a Usuario
```bash
curl -X POST http://localhost:5007/tasks/1/assign \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": 1
  }'
```

## 🤖 Funciones de IA

### 🧠 Ejecutar Acción Compleja con IA
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "action_text": "crear epic Sistema de Usuarios con color azul",
    "project_id": 1
  }'
```

### 🧠 Crear Múltiples Elementos con IA
```bash
curl -X POST http://localhost:5007/execute_complex_action \
  -H "Content-Type: application/json" \
  -d '{
    "action_text": "crear tres user stories para autenticación: login, registro y recuperación de contraseña",
    "project_id": 1
  }'
```

## 🔐 Autenticación Manual (Opcional)

Si quieres usar autenticación manual en lugar del login automático:

### 🔑 Login Manual
```bash
curl -X POST http://localhost:5007/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "puertocho",
    "password": "puertocho",
    "host": "http://localhost:9000"
  }'
```

### 🚪 Logout
```bash
curl -X POST http://localhost:5007/logout \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id_aqui"
  }'
```

### ✅ Verificar Estado de Sesión
```bash
curl -X POST http://localhost:5007/session_status \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "tu_session_id_aqui"
  }'
```

---

## 📝 Notas Importantes

1. **Sin session_id**: Con el login automático, no necesitas incluir `session_id` en ninguna petición
2. **Puerto**: El servicio corre en el puerto `5007` por defecto
3. **Headers**: Siempre incluye `Content-Type: application/json` en peticiones POST/PUT
4. **IDs**: Reemplaza los números de ID (1, 107, etc.) con los IDs reales de tu instancia
5. **Credenciales**: Las credenciales están configuradas en variables de entorno (`puertocho/puertocho`)
6. **🔄 Renovación Automática**: Las sesiones se renuevan automáticamente cuando expiran (transparente)
7. **⏰ Duración**: Las sesiones duran 8 horas por defecto (configurable con `SESSION_EXPIRY`)

## 🧪 Script de Pruebas

Para ejecutar todas las pruebas automáticamente:
```bash
# Pruebas básicas de login automático
./test-auto-login.sh

# Pruebas de renovación automática
./test-auto-renewal.sh
```

## 🔗 Endpoints de Información

- **Estado del servicio**: `http://localhost:5007/health`
- **Sesión automática**: `http://localhost:5007/auto-session`
- **Renovación manual**: `http://localhost:5007/auto-session/renew` (POST)
- **Documentación**: Ver este archivo
- **README**: `README.md` para información completa 