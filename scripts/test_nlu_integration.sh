#!/bin/bash
# -----------------------------------------------------------------------------
# Script: test_nlu_integration.sh
# Descripción: Pruebas end-to-end para verificar la integración entre nlu-ms y
#              intentmanagerms dentro del stack de Puerto Ocho.
# -----------------------------------------------------------------------------
set -euo pipefail

# Nombre de los *servicios* en docker-compose.yml
NLU_SERVICE="puertocho-assistant-nlu"
INTENT_MANAGER_SERVICE="puertocho-assistant-intent-manager"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_ok()   { echo -e "${GREEN}✅ $1${NC}"; }
print_fail() { echo -e "${RED}❌ $1${NC}"; exit 1; }

# 1. Construir y levantar servicios necesarios
print_ok "(1/5) Levantando servicios NLU e Intent Manager..."

docker compose up -d ${NLU_SERVICE} ${INTENT_MANAGER_SERVICE}

# 2. Esperar que NLU esté healthy
print_ok "(2/5) Esperando a que NLU esté listo..."
for i in {1..30}; do
  if curl -s http://localhost:5001/health | grep -q 'OK'; then
    print_ok "NLU healthy"
    break
  fi
  sleep 2
done

# 3. Entrenar modelo NLU
print_ok "(3/5) Entrenando modelo NLU..."
if ! command -v jq &> /dev/null; then
  print_fail "'jq' no está instalado (requerido para parsear JSON)"
fi

TRAIN=$(curl -s -X POST "http://localhost:5001/train?domain=intents&locale=es" | jq -r '.messageId')
[ "$TRAIN" == "TRAIN_SUCCESS" ] && print_ok "Entrenamiento OK" || print_fail "Entrenamiento falló"

# 4. Probar predicciones directas
print_ok "(4/5) Probando predicciones directas..."
readarray -t TESTS < <(printf '%s
' \
  "enciende la luz de la cocina" \
  "apaga la luz del salón" \
  "reproduce música")

for t in "${TESTS[@]}"; do
  # Codificar URL usando perl (disponible por defecto en la mayoría de sistemas)
  ENCODED=$(echo "$t" | perl -MURI::Escape -ne 'chomp; print uri_escape($_)')
  OUT=$(curl -s -X POST "http://localhost:5001/predict?domain=intents&locale=es&userUtterance=${ENCODED}" | jq -r '.messageId')
  [ "$OUT" == "PREDICT" ] && print_ok "🧪 '$t'" || print_fail "Predicción falló para '$t'"
done

# 5. Probar integración con Intent Manager
print_ok "(5/5) Probando integración con Intent Manager..."
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d '{"prompt":"enciende la luz del salón"}' http://localhost:9904/api/agent/execute | jq -r '.success')
[ "$RESPONSE" == "true" ] && print_ok "Integración exitosa" || print_fail "Integración falló"

echo -e "${GREEN}🎉 Todas las pruebas pasaron correctamente.${NC}" 