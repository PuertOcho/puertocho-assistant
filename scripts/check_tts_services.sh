#!/bin/bash

# =============================================================================
# VERIFICADOR DE SERVICIOS TTS/ASR PARA PUERTOCHO-ASSISTANT
# =============================================================================
# Este script verifica qué servicios TTS/ASR están corriendo antes de 
# levantar nuevos contenedores, evitando conflictos de puertos y recursos.

set -euo pipefail

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Función para imprimir mensajes con color
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}$message${NC}"
}

# Función para buscar y cargar .env desde la raíz del proyecto
load_env_from_root() {
    # Buscar la raíz del proyecto (donde está este script)
    PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
    ENV_FILE="$PROJECT_ROOT/.env"
    if [ -f "$ENV_FILE" ]; then
        set -a
        source "$ENV_FILE"
        set +a
        print_status $BLUE "📄 Variables de entorno cargadas desde $ENV_FILE"
    else
        print_status $YELLOW "⚠️  Archivo .env no encontrado en la raíz ($PROJECT_ROOT), usando valores por defecto"
    fi
}

# Función para verificar si un puerto está en uso
check_port() {
    local port=$1
    local service_name=$2
    
    if netstat -tuln | grep -q ":$port "; then
        print_status $GREEN "✅ Puerto $port ($service_name) está en uso - servicio ya corriendo"
        return 0
    else
        print_status $YELLOW "⚠️  Puerto $port ($service_name) libre - servicio no encontrado"
        return 1
    fi
}

# Función para verificar disponibilidad de servicios via HTTP
check_service_health() {
    local url=$1
    local service_name=$2
    
    if curl -s -f "$url" > /dev/null 2>&1; then
        print_status $GREEN "✅ $service_name responde correctamente en $url"
        return 0
    else
        print_status $RED "❌ $service_name no responde en $url"
        return 1
    fi
}

# Función para verificar contenedores Docker
check_docker_container() {
    local container_name=$1
    
    if docker ps --format "table {{.Names}}" | grep -q "^$container_name$"; then
        print_status $GREEN "✅ Contenedor '$container_name' está corriendo"
        return 0
    else
        print_status $YELLOW "⚠️  Contenedor '$container_name' no encontrado"
        return 1
    fi
}

# Función para avisar de contenedores parados con nombres en conflicto (llamativo)
check_stopped_conflicting_containers() {
    local conflict_names=("whisper-api" "azure-tts-service" "kokoro-tts")
    local found_conflict=false
    local conflict_msgs=""
    for cname in "${conflict_names[@]}"; do
        if docker ps -a --filter "name=^/${cname}$" --filter "status=exited" --format '{{.Names}}' | grep -q "^${cname}$"; then
            found_conflict=true
            conflict_msgs+="- Contenedor: ${BOLD}${cname}${NC}\n  👉 Elimínalo con: ${BOLD}docker rm $cname${NC}\n\n"
        fi
    done
    if [ "$found_conflict" = true ]; then
        echo -e "\n${RED}############################################################${NC}"
        echo -e "${YELLOW}⚠️  ${BOLD}ATENCIÓN: CONTENEDORES PARADOS EN CONFLICTO DETECTADOS${NC} ${YELLOW}⚠️${NC}"
        echo -e "${BLUE}------------------------------------------------------------${NC}"
        echo -e "$conflict_msgs"
        echo -e "${YELLOW}⚠️  Elimina estos contenedores antes de continuar si quieres que Docker Compose pueda crearlos.${NC}"
        echo -e "${RED}############################################################${NC}\n"
    else
        print_status $GREEN "✅ No hay contenedores parados en conflicto."
    fi
}

# Main verification function
main() {
    print_status $BLUE "🔍 VERIFICANDO SERVICIOS TTS/ASR DISPONIBLES..."
    echo "================================================================="
    
    # Cargar variables de entorno desde la raíz
    load_env_from_root
    
    # Avisar de contenedores parados en conflicto
    check_stopped_conflicting_containers
    
    # Variables por defecto
    WHISPER_API_PORT=${WHISPER_API_PORT:-5000}
    F5_TTS_HOST_PORT=${F5_TTS_HOST_PORT:-5005}
    AZURE_TTS_HOST_PORT=${AZURE_TTS_HOST_PORT:-5004}
    KOKORO_TTS_HOST_PORT=${KOKORO_TTS_HOST_PORT:-5002}
    
    WHISPER_SERVICE=${WHISPER_SERVICE:-whisper-api}
    F5_TTS_CONTAINER_NAME=${F5_TTS_CONTAINER_NAME:-f5-tts-service}
    AZURE_TTS_CONTAINER_NAME=${AZURE_TTS_CONTAINER_NAME:-azure-tts-service}
    KOKORO_TTS_CONTAINER_NAME=${KOKORO_TTS_CONTAINER_NAME:-kokoro-tts}
    
    # Arrays para almacenar resultados
    declare -A services_running
    declare -A services_healthy
    
    echo ""
    print_status $BLUE "🎤 VERIFICANDO SERVICIO STT (WHISPER)..."
    echo "-----------------------------------------"
    
    # Verificar Whisper STT
    if check_docker_container "$WHISPER_SERVICE"; then
        services_running["whisper"]="docker"
        if check_service_health "http://localhost:$WHISPER_API_PORT/health" "Whisper STT"; then
            services_healthy["whisper"]="true"
        fi
    elif check_port "$WHISPER_API_PORT" "Whisper STT"; then
        services_running["whisper"]="external"
        if check_service_health "http://localhost:$WHISPER_API_PORT/health" "Whisper STT"; then
            services_healthy["whisper"]="true"
        fi
    fi
    
    echo ""
    print_status $BLUE "🗣️  VERIFICANDO SERVICIOS TTS..."
    echo "---------------------------------"
    
    # F5-TTS deshabilitado (modelo demasiado pesado)
    print_status $BLUE "📡 F5-TTS: DESHABILITADO (modelo demasiado pesado)"
    
    # Verificar Azure TTS (respaldo)
    print_status $BLUE "📡 Azure TTS (Respaldo):"
    if check_docker_container "$AZURE_TTS_CONTAINER_NAME"; then
        services_running["azure_tts"]="docker"
        if check_service_health "http://localhost:$AZURE_TTS_HOST_PORT/health" "Azure TTS"; then
            services_healthy["azure_tts"]="true"
        fi
    elif check_port "$AZURE_TTS_HOST_PORT" "Azure TTS"; then
        services_running["azure_tts"]="external"
        if check_service_health "http://localhost:$AZURE_TTS_HOST_PORT/health" "Azure TTS"; then
            services_healthy["azure_tts"]="true"
        fi
    fi
    
    # Verificar Kokoro TTS (alternativo)
    print_status $BLUE "📡 Kokoro TTS (Alternativo):"
    if check_docker_container "$KOKORO_TTS_CONTAINER_NAME"; then
        services_running["kokoro_tts"]="docker"
        if check_service_health "http://localhost:$KOKORO_TTS_HOST_PORT/health" "Kokoro TTS"; then
            services_healthy["kokoro_tts"]="true"
        fi
    elif check_port "$KOKORO_TTS_HOST_PORT" "Kokoro TTS"; then
        services_running["kokoro_tts"]="external"
        if check_service_health "http://localhost:$KOKORO_TTS_HOST_PORT/health" "Kokoro TTS"; then
            services_healthy["kokoro_tts"]="true"
        fi
    fi
    
    echo ""
    print_status $BLUE "📊 RESUMEN DE SERVICIOS..."
    echo "========================="
    
    # Generar variables de entorno dinámicas
    export ENABLE_WHISPER="false"
    export ENABLE_F5_TTS="false"  # F5-TTS deshabilitado permanentemente
    export ENABLE_AZURE_TTS="false"
    export ENABLE_KOKORO_TTS="false"
    export PRIMARY_TTS_SERVICE="azure_tts"  # Azure TTS como principal
    export FALLBACK_TTS_SERVICE="kokoro_tts"  # Kokoro TTS como fallback
    
    # Determinar qué servicios habilitar
    if [[ "${services_healthy[whisper]:-false}" == "true" ]]; then
        print_status $GREEN "✅ Whisper STT disponible (${services_running[whisper]})"
        export WHISPER_URL="http://localhost:$WHISPER_API_PORT"
    else
        print_status $YELLOW "⚠️  Whisper STT no disponible - habilitando en docker-compose"
        export ENABLE_WHISPER="true"
        export WHISPER_URL="http://puertocho-assistant-whisper:${WHISPER_FLASK_PORT:-5000}"
    fi
    
    # Lógica para Azure TTS (servicio principal)
    if [[ "${services_healthy[azure_tts]:-false}" == "true" ]]; then
        print_status $GREEN "✅ Azure TTS disponible como servicio principal (${services_running[azure_tts]})"
        export PRIMARY_TTS_SERVICE="azure_tts"
        export AZURE_TTS_URL="http://localhost:$AZURE_TTS_HOST_PORT"
    else
        print_status $YELLOW "⚠️  Azure TTS no disponible - habilitando en docker-compose"
        export ENABLE_AZURE_TTS="true"
        export AZURE_TTS_URL="http://puertocho-assistant-azure-tts:${AZURE_TTS_CONTAINER_PORT:-5000}"
    fi
    
    # Lógica para Kokoro TTS (servicio fallback)
    if [[ "${services_healthy[kokoro_tts]:-false}" == "true" ]]; then
        print_status $GREEN "✅ Kokoro TTS disponible como servicio fallback (${services_running[kokoro_tts]})"
        export FALLBACK_TTS_SERVICE="kokoro_tts"
        export KOKORO_TTS_URL="http://localhost:$KOKORO_TTS_HOST_PORT"
    else
        print_status $YELLOW "⚠️  Kokoro TTS no disponible - habilitando en docker-compose"
        export ENABLE_KOKORO_TTS="true"
        export KOKORO_TTS_URL="http://puertocho-assistant-kokoro-tts:${KOKORO_TTS_FLASK_PORT:-5002}"
    fi
    
    # F5-TTS deshabilitado permanentemente
    print_status $BLUE "ℹ️  F5-TTS deshabilitado (modelo demasiado pesado)"
    export F5_TTS_URL="http://disabled"
    
    # Determinar perfil de Docker Compose a usar
    if [[ "$ENABLE_WHISPER" == "true" || "$ENABLE_AZURE_TTS" == "true" || "$ENABLE_KOKORO_TTS" == "true" ]]; then
        export DOCKER_COMPOSE_PROFILES="tts-stack"
        print_status $BLUE "🐳 Usando perfil docker-compose: $DOCKER_COMPOSE_PROFILES"
    else
        export DOCKER_COMPOSE_PROFILES="minimal"
        print_status $BLUE "🐳 Todos los servicios TTS/ASR están externos - usando perfil: $DOCKER_COMPOSE_PROFILES"
    fi
    
    echo ""
    print_status $BLUE "🔧 CONFIGURACIÓN FINAL:"
    echo "======================="
    echo "WHISPER_URL=$WHISPER_URL"
    echo "F5_TTS_URL=$F5_TTS_URL"
    echo "AZURE_TTS_URL=$AZURE_TTS_URL"
    echo "KOKORO_TTS_URL=$KOKORO_TTS_URL"
    echo "PRIMARY_TTS_SERVICE=$PRIMARY_TTS_SERVICE"
    echo "FALLBACK_TTS_SERVICE=$FALLBACK_TTS_SERVICE"
    echo "DOCKER_COMPOSE_PROFILES=$DOCKER_COMPOSE_PROFILES"
    
    # Escribir configuración a archivo temporal para docker-compose
    cat > .env.tts << EOF
# Configuración automática de servicios TTS/ASR
# Generado por check_tts_services.sh el $(date)

ENABLE_WHISPER=$ENABLE_WHISPER
ENABLE_F5_TTS=$ENABLE_F5_TTS
ENABLE_AZURE_TTS=$ENABLE_AZURE_TTS
ENABLE_KOKORO_TTS=$ENABLE_KOKORO_TTS

WHISPER_URL=$WHISPER_URL
F5_TTS_URL=$F5_TTS_URL
AZURE_TTS_URL=$AZURE_TTS_URL
KOKORO_TTS_URL=$KOKORO_TTS_URL

PRIMARY_TTS_SERVICE=$PRIMARY_TTS_SERVICE
FALLBACK_TTS_SERVICE=$FALLBACK_TTS_SERVICE

DOCKER_COMPOSE_PROFILES=$DOCKER_COMPOSE_PROFILES
EOF
    
    print_status $GREEN "✅ Configuración guardada en .env.tts"
    
    # Mostrar comandos recomendados
    echo ""
    print_status $BLUE "💡 COMANDOS RECOMENDADOS:"
    echo "========================"
    
    if [[ "$DOCKER_COMPOSE_PROFILES" == "tts-stack" ]]; then
        echo "# Levantar servicios faltantes:"
        echo "docker-compose --profile $DOCKER_COMPOSE_PROFILES up -d"
    else
        echo "# Todos los servicios están disponibles externamente"
        echo "docker-compose up -d  # Solo microservicios principales"
    fi
    
    echo ""
    echo "# Verificar estado después del levantamiento:"
    echo "$0"
    
    echo ""
    print_status $GREEN "🎉 Verificación completada!"
}

# Verificar dependencias
if ! command -v netstat &> /dev/null; then
    print_status $RED "❌ Error: 'netstat' no está instalado. Instala net-tools: sudo apt-get install net-tools"
    exit 1
fi

if ! command -v curl &> /dev/null; then
    print_status $RED "❌ Error: 'curl' no está instalado. Instala curl: sudo apt-get install curl"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    print_status $RED "❌ Error: 'docker' no está instalado."
    exit 1
fi

# Ejecutar verificación principal
main "$@" 