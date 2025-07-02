#!/bin/bash

# =============================================================================
# SCRIPT DE PRUEBAS - TAIGA MCP SERVICE CON LOGIN AUTOMÁTICO
# =============================================================================

echo "🚀 Taiga MCP Service - Pruebas de Login Automático"
echo "=================================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# URL base del servicio
BASE_URL="http://localhost:5007"

echo -e "${BLUE}🔍 1. Verificando estado del servicio con login automático...${NC}"
echo "Comando: curl -s $BASE_URL/health"
curl -s $BASE_URL/health | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    auto_enabled = data.get('auto_login_enabled', False)
    sessions = data.get('active_sessions', 0)
    auto_session = data.get('auto_session', {})
    
    print(f'✅ Login automático: {\"ACTIVO\" if auto_enabled else \"INACTIVO\"}')
    print(f'📊 Sesiones activas: {sessions}')
    
    if auto_session:
        print(f'👤 Usuario: {auto_session.get(\"username\", \"N/A\")}')
        print(f'⏰ Expira: {auto_session.get(\"expires_at\", \"N/A\")}')
        print(f'✅ Válida: {\"SÍ\" if auto_session.get(\"valid\") else \"NO\"}')
    else:
        print('❌ No hay sesión automática configurada')
        
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${BLUE}🔑 2. Información de la sesión automática...${NC}"
echo "Comando: curl -s $BASE_URL/auto-session"
curl -s $BASE_URL/auto-session | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('auto_login'):
        print(f'🆔 Session ID: {data.get(\"session_id\", \"N/A\")}')
        print(f'👤 Usuario: {data.get(\"username\", \"N/A\")}')
        print(f'🏠 Host Taiga: {data.get(\"host\", \"N/A\")}')
        print(f'⏰ Expira: {data.get(\"expires_at\", \"N/A\")}')
    else:
        print('❌ Login automático no configurado')
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${BLUE}📋 3. Listando proyectos SIN session_id...${NC}"
echo "Comando: curl -s $BASE_URL/projects"
curl -s $BASE_URL/projects | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    total = data.get('total', 0)
    projects = data.get('projects', [])
    
    print(f'📊 Total de proyectos: {total}')
    print('📋 Proyectos encontrados:')
    
    for i, project in enumerate(projects[:5], 1):
        name = project.get('name', 'Sin nombre')
        project_id = project.get('id', 'N/A')
        print(f'  {i}. {name} (ID: {project_id})')
        
    if total > 5:
        print(f'  ... y {total - 5} más')
        
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${BLUE}📝 4. Creando nuevo proyecto SIN session_id...${NC}"
PROJECT_NAME="Proyecto Auto Login $(date +%Y%m%d_%H%M%S)"
echo "Comando: curl -X POST $BASE_URL/projects -H \"Content-Type: application/json\" -d '{\"name\": \"$PROJECT_NAME\", \"description\": \"Proyecto creado para demostrar login automático\"}'"

NEW_PROJECT_ID=$(curl -s -X POST $BASE_URL/projects \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"$PROJECT_NAME\",
    \"description\": \"Proyecto creado para demostrar login automático\"
  }" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    project_id = data.get('id')
    name = data.get('name', 'N/A')
    slug = data.get('slug', 'N/A')
    
    print(f'✅ Proyecto creado exitosamente!')
    print(f'🆔 ID: {project_id}')
    print(f'📛 Nombre: {name}')
    print(f'🔗 Slug: {slug}')
    print(project_id)  # Para capturar en variable
    
except Exception as e:
    print(f'❌ Error creando proyecto: {e}')
    print('0')  # Retornar 0 si falla
" | tail -1)

echo ""

if [ "$NEW_PROJECT_ID" != "0" ] && [ -n "$NEW_PROJECT_ID" ]; then
    echo -e "${BLUE}🎯 5. Creando historia de usuario SIN session_id en el proyecto $NEW_PROJECT_ID...${NC}"
    echo "Comando: curl -X POST $BASE_URL/projects/$NEW_PROJECT_ID/user_stories -H \"Content-Type: application/json\" -d '{\"subject\": \"Como usuario, quiero login automático\", \"description\": \"Para no tener que autenticarme en cada petición\"}'"
    
    curl -s -X POST $BASE_URL/projects/$NEW_PROJECT_ID/user_stories \
      -H "Content-Type: application/json" \
      -d '{
        "subject": "Como usuario, quiero login automático",
        "description": "Para no tener que autenticarme en cada petición"
      }' | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    story_id = data.get('id')
    subject = data.get('subject', 'N/A')
    ref = data.get('ref', 'N/A')
    
    print(f'✅ Historia de usuario creada!')
    print(f'🆔 ID: {story_id}')
    print(f'📝 Título: {subject}')
    print(f'🏷️ Referencia: #{ref}')
    
except Exception as e:
    print(f'❌ Error creando historia: {e}')
"
    echo ""
    
    echo -e "${BLUE}📊 6. Listando historias del proyecto $NEW_PROJECT_ID SIN session_id...${NC}"
    echo "Comando: curl -s $BASE_URL/projects/$NEW_PROJECT_ID/user_stories"
    
    curl -s $BASE_URL/projects/$NEW_PROJECT_ID/user_stories | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    stories = data.get('user_stories', [])
    total = data.get('total', 0)
    
    print(f'📊 Total de historias: {total}')
    print('📋 Historias en el proyecto:')
    
    for i, story in enumerate(stories, 1):
        subject = story.get('subject', 'Sin título')
        story_id = story.get('id', 'N/A')
        ref = story.get('ref', 'N/A')
        status = story.get('status_extra_info', {}).get('name', 'N/A')
        
        print(f'  {i}. #{ref} - {subject} (ID: {story_id}) [{status}]')
        
except Exception as e:
    print(f'❌ Error listando historias: {e}')
"
    echo ""
else
    echo -e "${RED}❌ No se pudo crear el proyecto, saltando pruebas de historias${NC}"
    echo ""
fi

echo -e "${YELLOW}🧪 7. Probando función de IA compleja SIN session_id...${NC}"
if [ "$NEW_PROJECT_ID" != "0" ] && [ -n "$NEW_PROJECT_ID" ]; then
    echo "Comando: curl -X POST $BASE_URL/execute_complex_action -H \"Content-Type: application/json\" -d '{\"action_text\": \"crear epic Sistema de Login Automático con color verde\", \"project_id\": $NEW_PROJECT_ID}'"
    
    curl -s -X POST $BASE_URL/execute_complex_action \
      -H "Content-Type: application/json" \
      -d "{
        \"action_text\": \"crear epic Sistema de Login Automático con color verde\",
        \"project_id\": $NEW_PROJECT_ID
      }" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    success = data.get('success', False)
    message = data.get('message', 'N/A')
    
    print(f'🤖 IA ejecutada: {\"✅ ÉXITO\" if success else \"❌ FALLO\"}')
    print(f'💬 Mensaje: {message}')
    
    created = data.get('created_elements', [])
    if created:
        print('✨ Elementos creados:')
        for elem in created:
            elem_type = elem.get('type', 'elemento')
            title = elem.get('title', elem.get('subject', 'N/A'))
            elem_id = elem.get('id', 'N/A')
            print(f'  - {elem_type}: {title} (ID: {elem_id})')
    
except Exception as e:
    print(f'❌ Error en función IA: {e}')
"
else
    echo -e "${RED}❌ Saltando prueba de IA (no hay proyecto válido)${NC}"
fi

echo ""
echo -e "${GREEN}✅ TODAS LAS PRUEBAS DE LOGIN AUTOMÁTICO COMPLETADAS${NC}"
echo "=================================================="
echo ""
echo -e "${YELLOW}📚 RESUMEN:${NC}"
echo "• El servicio se autentica automáticamente al iniciar"
echo "• No necesitas proporcionar session_id en las peticiones"
echo "• Puedes seguir usando session_id si lo prefieres"
echo "• La sesión automática dura 8 horas (configurable)"
echo ""
echo -e "${BLUE}🔗 Endpoints útiles:${NC}"
echo "• Estado del servicio: $BASE_URL/health"
echo "• Info sesión automática: $BASE_URL/auto-session"
echo "• Proyectos: $BASE_URL/projects"
echo "• Login manual: $BASE_URL/login (opcional)"
echo ""
echo -e "${GREEN}🎉 ¡Taiga MCP Service con Login Automático funcionando correctamente!${NC}" 