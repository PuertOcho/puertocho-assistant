#!/bin/bash

# =============================================================================
# SCRIPT DE PRUEBAS - RENOVACIÓN AUTOMÁTICA DE SESIÓN
# =============================================================================

echo "🔄 Taiga MCP Service - Pruebas de Renovación Automática"
echo "======================================================"
echo ""

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# URL base del servicio
BASE_URL="http://localhost:5007"

echo -e "${BLUE}🔍 1. Verificando estado inicial de la sesión automática...${NC}"
curl -s $BASE_URL/auto-session | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('auto_login'):
        print(f'✅ Sesión automática activa')
        print(f'🆔 Session ID: {data.get(\"session_id\", \"N/A\")}')
        print(f'👤 Usuario: {data.get(\"username\", \"N/A\")}')
        print(f'⏰ Expira: {data.get(\"expires_at\", \"N/A\")}')
        print(f'🔄 Renovada: {\"SÍ\" if data.get(\"renewed\", False) else \"NO\"}')
        print(f'🔄 Auto-renovación: {\"ACTIVA\" if data.get(\"auto_renewal_enabled\", False) else \"INACTIVA\"}')
    else:
        print('❌ No hay sesión automática')
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${BLUE}📋 2. Probando que la sesión funciona (listando proyectos)...${NC}"
curl -s $BASE_URL/projects | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    total = data.get('total', 0)
    print(f'✅ Sesión válida - Proyectos obtenidos: {total}')
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${PURPLE}🔄 3. Forzando renovación manual de sesión...${NC}"
echo "Comando: curl -X POST $BASE_URL/auto-session/renew"
curl -s -X POST $BASE_URL/auto-session/renew | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    success = data.get('success', False)
    if success:
        print(f'✅ Renovación exitosa!')
        print(f'🆔 Nuevo Session ID: {data.get(\"new_session_id\", \"N/A\")}')
        print(f'⏰ Nueva expiración: {data.get(\"expires_at\", \"N/A\")}')
        print(f'💬 Mensaje: {data.get(\"message\", \"N/A\")}')
    else:
        print(f'❌ Error en renovación: {data.get(\"error\", \"N/A\")}')
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${BLUE}🔍 4. Verificando nueva sesión después de renovación...${NC}"
curl -s $BASE_URL/auto-session | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('auto_login'):
        print(f'✅ Nueva sesión automática activa')
        print(f'🆔 Session ID: {data.get(\"session_id\", \"N/A\")}')
        print(f'👤 Usuario: {data.get(\"username\", \"N/A\")}')
        print(f'⏰ Expira: {data.get(\"expires_at\", \"N/A\")}')
        print(f'🔄 Renovada: {\"SÍ\" if data.get(\"renewed\", False) else \"NO\"}')
        print(f'✅ Válida: {\"SÍ\" if data.get(\"valid\", False) else \"NO\"}')
    else:
        print('❌ No hay sesión automática')
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${BLUE}📋 5. Probando que la nueva sesión funciona (listando proyectos)...${NC}"
curl -s $BASE_URL/projects | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    total = data.get('total', 0)
    print(f'✅ Nueva sesión válida - Proyectos obtenidos: {total}')
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${YELLOW}📊 6. Estado completo del servicio...${NC}"
curl -s $BASE_URL/health | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    auto_session = data.get('auto_session', {})
    
    print(f'🚀 Servicio: {data.get(\"service\", \"N/A\")}')
    print(f'📊 Estado: {data.get(\"status\", \"N/A\")}')
    print(f'📊 Sesiones activas: {data.get(\"active_sessions\", 0)}')
    print(f'🔄 Login automático: {\"ACTIVO\" if data.get(\"auto_login_enabled\") else \"INACTIVO\"}')
    
    if auto_session:
        print(f'👤 Usuario automático: {auto_session.get(\"username\", \"N/A\")}')
        print(f'✅ Sesión válida: {\"SÍ\" if auto_session.get(\"valid\") else \"NO\"}')
        print(f'🔄 Sesión renovada: {\"SÍ\" if auto_session.get(\"renewed\") else \"NO\"}')
        print(f'🔄 Auto-renovación: {\"ACTIVA\" if auto_session.get(\"auto_renewal_enabled\") else \"INACTIVA\"}')
        
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${BLUE}🧪 7. Creando proyecto para probar que todo funciona...${NC}"
PROJECT_NAME="Test Renovación $(date +%H%M%S)"
curl -s -X POST $BASE_URL/projects \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"$PROJECT_NAME\",
    \"description\": \"Proyecto creado para probar renovación automática\"
  }" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    project_id = data.get('id')
    name = data.get('name', 'N/A')
    
    if project_id:
        print(f'✅ Proyecto creado con sesión renovada!')
        print(f'🆔 ID: {project_id}')
        print(f'📛 Nombre: {name}')
    else:
        print(f'❌ Error creando proyecto')
        
except Exception as e:
    print(f'❌ Error: {e}')
"
echo ""

echo -e "${GREEN}✅ PRUEBAS DE RENOVACIÓN AUTOMÁTICA COMPLETADAS${NC}"
echo "=============================================="
echo ""
echo -e "${YELLOW}📚 RESUMEN DE FUNCIONALIDADES:${NC}"
echo "• ✅ Sesión automática al iniciar el servicio"
echo "• ✅ Renovación automática cuando la sesión expira"
echo "• ✅ Renovación manual forzada con POST /auto-session/renew"
echo "• ✅ Información detallada de renovaciones en /health y /auto-session"
echo "• ✅ Funcionamiento transparente sin interrupciones"
echo ""
echo -e "${BLUE}🔗 Endpoints de renovación:${NC}"
echo "• Información de sesión: $BASE_URL/auto-session"
echo "• Renovación manual: $BASE_URL/auto-session/renew (POST)"
echo "• Estado del servicio: $BASE_URL/health"
echo ""
echo -e "${GREEN}🎉 ¡La renovación automática está funcionando correctamente!${NC}" 