#!/bin/bash
# -----------------------------------------------------------------------------
# Script: test_nlu_detailed.sh
# Descripción: Pruebas detalladas de funcionalidades NLU específicas
# -----------------------------------------------------------------------------
set -euo pipefail

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

print_ok()   { echo -e "${GREEN}✅ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_fail() { echo -e "${RED}❌ $1${NC}"; exit 1; }

# Verificar que jq está disponible
if ! command -v jq &> /dev/null; then
  print_fail "'jq' no está instalado (requerido para parsear JSON)"
fi

# Verificar que NLU está funcionando
if ! curl -s http://localhost:5001/health | grep -q 'OK'; then
  print_fail "Servicio NLU no está disponible en localhost:5001"
fi

print_info "Iniciando pruebas detalladas del servicio NLU..."

# Función para probar una predicción y mostrar resultados detallados
test_prediction() {
  local text="$1"
  local expected_intent="$2"
  
  print_info "Probando: '$text'"
  
  # Codificar URL
  ENCODED=$(echo "$text" | perl -MURI::Escape -ne 'chomp; print uri_escape($_)')
  
  # Realizar predicción
  RESPONSE=$(curl -s -X POST "http://localhost:5001/predict?domain=hogar&locale=es&userUtterance=${ENCODED}")
  
  # Parsear respuesta
  INTENT=$(echo "$RESPONSE" | jq -r '.message' | jq -r '.intent.name')
  CONFIDENCE=$(echo "$RESPONSE" | jq -r '.message' | jq -r '.intent.confidence')
  ENTITIES=$(echo "$RESPONSE" | jq -r '.message' | jq -r '.entities[]? | "\(.entity): \(.value)"')
  
  echo "  → Intención: $INTENT (confianza: $CONFIDENCE)"
  if [ -n "$ENTITIES" ]; then
    echo "  → Entidades: $ENTITIES"
  else
    echo "  → Entidades: ninguna"
  fi
  
  # Verificar intención esperada
  if [ "$INTENT" == "$expected_intent" ]; then
    print_ok "Intención correcta"
  else
    echo "  ⚠️  Esperado: $expected_intent, Obtenido: $INTENT"
  fi
  echo
}

# Entrenar modelo si es necesario
print_info "Verificando entrenamiento del modelo..."
TRAIN_RESPONSE=$(curl -s -X POST "http://localhost:5001/train?domain=hogar&locale=es")
TRAIN_STATUS=$(echo "$TRAIN_RESPONSE" | jq -r '.messageId')
if [ "$TRAIN_STATUS" == "TRAIN_SUCCESS" ]; then
  print_ok "Modelo entrenado correctamente"
else
  print_fail "Error entrenando el modelo: $TRAIN_STATUS"
fi

echo
print_info "=== PRUEBAS DE COMANDOS DE LUCES ==="

test_prediction "enciende la luz" "encender_luz"
test_prediction "enciende la luz de la cocina" "encender_luz"
test_prediction "prende la luz del salón" "encender_luz"
test_prediction "apaga la luz" "apagar_luz"
test_prediction "apaga las luces del dormitorio" "apagar_luz"

echo
print_info "=== PRUEBAS DE COMANDOS DE MÚSICA ==="

test_prediction "reproduce música" "reproducir_musica"
test_prediction "pon música de rock" "reproducir_musica"
test_prediction "para la música" "parar_musica"
test_prediction "detén la música" "parar_musica"

echo
print_info "=== PRUEBAS DE COMANDOS DE VOLUMEN ==="

test_prediction "sube el volumen" "subir_volumen"
test_prediction "baja el volumen" "bajar_volumen"
test_prediction "aumenta el volumen" "subir_volumen"

echo
print_info "=== PRUEBAS DE INFORMACIÓN ==="

test_prediction "qué hora es" "consultar_hora"
test_prediction "dime la hora" "consultar_hora"
test_prediction "cómo está el tiempo" "consultar_tiempo"

echo
print_info "=== PRUEBAS CONVERSACIONALES ==="

test_prediction "hola" "saludo"
test_prediction "buenos días" "saludo"
test_prediction "adiós" "despedida"
test_prediction "hasta luego" "despedida"
test_prediction "ayuda" "ayuda"
test_prediction "qué puedes hacer" "ayuda"

echo
print_info "=== PRUEBAS DE CONFIANZA BAJA ==="

test_prediction "xyzabc123" "unknown"
test_prediction "comando inexistente" "unknown"

print_ok "Pruebas detalladas completadas" 