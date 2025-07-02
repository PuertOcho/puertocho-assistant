#!/bin/bash
# -----------------------------------------------------------------------------
# Script: verify_nlu_setup.sh
# Descripción: Verificación rápida del setup completo de NLU
# -----------------------------------------------------------------------------
set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_ok()   { echo -e "${GREEN}✅ $1${NC}"; }
print_warn() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_fail() { echo -e "${RED}❌ $1${NC}"; }
print_header() { echo -e "\n${YELLOW}=== $1 ===${NC}"; }

# Verificar archivos de configuración
print_header "VERIFICANDO ARCHIVOS DE CONFIGURACIÓN"

check_file() {
  if [ -f "$1" ]; then
    print_ok "Archivo existe: $1"
  else
    print_fail "Archivo faltante: $1"
    return 1
  fi
}

check_file "puertocho-assistant-server/nlu-ms/Dockerfile"
check_file "puertocho-assistant-server/nlu-ms/server/training_data/intents/hogar_es.yml"
check_file "puertocho-assistant-server/nlu-ms/server/core/config/es_spacy_diet.yml"
check_file "puertocho-assistant-server/nlu-ms/server/config/nlp.properties"
check_file "puertocho-assistant-server/intentmanagerms/src/main/java/com/intentmanagerms/application/services/NluService.java"
check_file "puertocho-assistant-server/intentmanagerms/src/main/java/com/intentmanagerms/application/services/SmartAssistantService.java"

# Verificar configuración en docker-compose
print_header "VERIFICANDO DOCKER COMPOSE"

if grep -q "puertocho-assistant-nlu:" docker-compose.yml; then
  print_ok "Servicio NLU definido en docker-compose.yml"
else
  print_fail "Servicio NLU no encontrado en docker-compose.yml"
fi

if grep -q "NLU_URL=" docker-compose.yml; then
  print_ok "Variable NLU_URL configurada"
else
  print_warn "Variable NLU_URL no encontrada"
fi

# Verificar dependencias
print_header "VERIFICANDO DEPENDENCIAS"

if command -v jq &> /dev/null; then
  print_ok "jq instalado"
else
  print_fail "jq no está instalado"
fi

if command -v curl &> /dev/null; then
  print_ok "curl instalado"
else
  print_fail "curl no está instalado"
fi

if command -v perl &> /dev/null; then
  print_ok "perl instalado"
else
  print_fail "perl no está instalado"
fi

# Verificar servicios Docker
print_header "VERIFICANDO SERVICIOS DOCKER"

if docker ps --format "{{.Names}}" | grep -q "puertocho-nlu"; then
  print_ok "Contenedor NLU ejecutándose"
  
  # Verificar health del servicio
  if curl -s http://localhost:5001/health | grep -q 'OK'; then
    print_ok "Servicio NLU respondiendo correctamente"
  else
    print_warn "Servicio NLU no responde correctamente"
  fi
else
  print_warn "Contenedor NLU no está ejecutándose"
fi

if docker ps --format "{{.Names}}" | grep -q "puertocho-assistant-intent-manager"; then
  print_ok "Contenedor Intent Manager ejecutándose"
  
  # Verificar health del servicio
  if curl -s http://localhost:9904/api/agent/health | grep -q 'UP'; then
    print_ok "Intent Manager respondiendo correctamente"
  else
    print_warn "Intent Manager no responde correctamente"
  fi
else
  print_warn "Contenedor Intent Manager no está ejecutándose"
fi

# Verificar scripts
print_header "VERIFICANDO SCRIPTS"

if [ -x "scripts/test_nlu_integration.sh" ]; then
  print_ok "Script de integración ejecutable"
else
  print_fail "Script de integración no ejecutable"
fi

if [ -x "scripts/test_nlu_detailed.sh" ]; then
  print_ok "Script detallado ejecutable"
else
  print_fail "Script detallado no ejecutable"
fi

# Verificar documentación
print_header "VERIFICANDO DOCUMENTACIÓN"

if [ -f "docs/integracion_nlu_puerto_ocho.md" ]; then
  print_ok "Documentación creada"
else
  print_fail "Documentación no encontrada"
fi

print_header "RESUMEN"
echo "Para ejecutar las pruebas completas:"
echo "  ./scripts/test_nlu_integration.sh    # Pruebas básicas end-to-end"
echo "  ./scripts/test_nlu_detailed.sh       # Pruebas detalladas de funcionalidades"
echo ""
echo "Para consultar la documentación:"
echo "  cat docs/integracion_nlu_puerto_ocho.md"
echo ""
print_ok "Verificación completada" 