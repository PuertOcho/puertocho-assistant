#!/bin/bash

# =============================================================================
# SCRIPT DE INICIO INTELIGENTE PARA PUERTOCHO-ASSISTANT
# =============================================================================
# Este script verifica qué servicios TTS/ASR están disponibles y levanta
# solo los microservicios necesarios para evitar conflictos de recursos.

set -euo pipefail

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Función para imprimir mensajes con color
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}$message${NC}"
}

# Función para verificar dependencias
check_dependencies() {
    print_status $BLUE "🔍 Verificando dependencias del sistema..."
    
    local missing_deps=()
    
    if ! command -v docker &> /dev/null; then
        missing_deps+=("docker")
    fi
    
    if ! command -v docker compose &> /dev/null; then
        missing_deps+=("docker compose")
    fi
    
    if ! command -v curl &> /dev/null; then
        missing_deps+=("curl")
    fi
    
    if ! command -v netstat &> /dev/null; then
        missing_deps+=("net-tools")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        print_status $RED "❌ Dependencias faltantes: ${missing_deps[*]}"
        print_status $YELLOW "💡 Instala las dependencias:"
        print_status $YELLOW "   sudo apt-get update"
        print_status $YELLOW "   sudo apt-get install ${missing_deps[*]}"
        exit 1
    fi
    
    print_status $GREEN "✅ Todas las dependencias están disponibles"
}

# Función para verificar archivo .env
check_env_file() {
    print_status $BLUE "📄 Verificando configuración..."
    
    # Buscar .env en el directorio raíz del proyecto
    PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
    ENV_FILE="$PROJECT_ROOT/.env"
    
    if [ ! -f "$ENV_FILE" ]; then
        print_status $RED "❌ Archivo .env no encontrado en la raíz del proyecto ($PROJECT_ROOT)"
        print_status $YELLOW "💡 Crea el archivo .env en la raíz antes de continuar."
        exit 1
    fi
    
    # Exportar variables de entorno desde .env
    set -a
    source "$ENV_FILE"
    set +a
    print_status $GREEN "✅ Configuración cargada desde $ENV_FILE"
}

# Función para verificar GPU NVIDIA (opcional)
check_gpu() {
    print_status $BLUE "🖥️  Verificando soporte GPU..."
    
    if command -v nvidia-smi &> /dev/null; then
        if nvidia-smi > /dev/null 2>&1; then
            local gpu_count=$(nvidia-smi --query-gpu=count --format=csv,noheader,nounits | head -1)
            print_status $GREEN "✅ GPU NVIDIA detectada ($gpu_count dispositivos)"
            print_status $GREEN "   Los servicios TTS con GPU tendrán mejor rendimiento"
            export GPU_AVAILABLE="true"
        else
            print_status $YELLOW "⚠️  nvidia-smi instalado pero GPU no accesible"
            export GPU_AVAILABLE="false"
        fi
    else
        print_status $YELLOW "⚠️  GPU NVIDIA no detectada - servicios TTS usarán CPU"
        print_status $YELLOW "   Esto puede resultar en síntesis más lenta"
        export GPU_AVAILABLE="false"
    fi
}

# Función para avisar de contenedores parados con nombres en conflicto
check_stopped_conflicting_containers() {
    print_status $BLUE "🔎 Buscando contenedores parados con nombres en conflicto..."
    local conflict_names=("whisper-api" "azure-tts-service" "kokoro-tts")
    local found_conflict=false
    for cname in "${conflict_names[@]}"; do
        if docker ps -a --filter "name=^/${cname}$" --filter "status=exited" --format '{{.Names}}' | grep -q "^${cname}$"; then
            print_status $YELLOW "⚠️  Contenedor parado en conflicto: $cname"
            print_status $YELLOW "   Si quieres eliminarlo, ejecuta: docker rm $cname"
            found_conflict=true
        fi
    done
    if [ "$found_conflict" = true ]; then
        print_status $YELLOW "⚠️  Elimina los contenedores parados en conflicto antes de continuar si quieres que Docker Compose pueda crearlos."
    else
        print_status $GREEN "✅ No hay contenedores parados en conflicto."
    fi
}

# Función principal de inicio
main() {
    print_status $PURPLE "🚀 INICIANDO PUERTOCHO-ASSISTANT"
    print_status $PURPLE "=================================="
    echo
    
    # 1. Verificar dependencias
    check_dependencies
    echo
    
    # 2. Verificar configuración
    check_env_file
    echo
    
    # 3. Verificar GPU
    check_gpu
    echo
    
    # 4. Avisar de contenedores parados en conflicto
    check_stopped_conflicting_containers
    echo
    
    # 5. Verificar servicios TTS/ASR existentes
    print_status $BLUE "🔍 Verificando servicios TTS/ASR disponibles..."
    # Buscar la raíz del proyecto y el script verificador
    PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
    TTS_CHECK_SCRIPT="$PROJECT_ROOT/scripts/check_tts_services.sh"
    if [ -f "$TTS_CHECK_SCRIPT" ]; then
        bash "$TTS_CHECK_SCRIPT"
        print_status $GREEN "✅ Verificación TTS completada"
    else
        print_status $YELLOW "⚠️  Script de verificación TTS no encontrado"
        print_status $YELLOW "   Usando configuración estática"
        export DOCKER_COMPOSE_PROFILES="tts-stack"
    fi
    echo
    
    # 6. Mostrar plan de ejecución
    print_status $BLUE "📋 PLAN DE EJECUCIÓN:"
    echo "====================="
    echo "Perfil Docker Compose: ${DOCKER_COMPOSE_PROFILES:-tts-stack}"
    
    if [ "${DOCKER_COMPOSE_PROFILES:-tts-stack}" == "tts-stack" ]; then
        print_status $BLUE "📦 Servicios que se levantarán:"
        echo "  • Microservicios principales (Eureka, Config, Gateway, etc.)"
        
        if [ "${ENABLE_WHISPER:-true}" == "true" ]; then
            echo "  • Whisper STT (Speech-to-Text)"
        fi
        
        echo "  • Azure TTS (Síntesis principal)"
        echo "  • Kokoro TTS (Síntesis fallback)"
    else
        print_status $GREEN "📦 Solo microservicios principales (TTS externos detectados)"
    fi
    echo
    
    # 7. Preguntar confirmación
    print_status $YELLOW "⚡ ¿Proceder con el levantamiento? (Y/n): "
    read -n 1 -r
    echo
    if [[ $REPLY =~ ^[Nn]$ ]]; then
        print_status $BLUE "ℹ️  Operación cancelada por el usuario"
        exit 0
    fi
    echo
    
    # 8. Bajar servicios existentes si están corriendo
    print_status $BLUE "🛑 Deteniendo servicios existentes..."
    docker compose down --remove-orphans || true
    echo
    
    # 9. Levantar servicios según el perfil determinado
    print_status $BLUE "🚀 Levantando servicios..."
    
    if [ "${DOCKER_COMPOSE_PROFILES:-tts-stack}" == "minimal" ]; then
        # Solo microservicios principales
        docker compose up -d --build
    else
        # Stack completo con TTS
        docker compose --profile ${DOCKER_COMPOSE_PROFILES:-tts-stack} up -d --build
    fi
    
    if [ $? -eq 0 ]; then
        print_status $GREEN "✅ Servicios levantados exitosamente"
    else
        print_status $RED "❌ Error levantando servicios"
        exit 1
    fi
    echo
    
    # 10. Verificar estado de servicios
    print_status $BLUE "⏳ Esperando que los servicios estén listos..."
    sleep 10
    
    print_status $BLUE "📊 ESTADO FINAL DE SERVICIOS:"
    echo "============================="
    
    # Verificar servicios principales
    local services_ok=0
    local total_services=0
    
    # Gateway
    total_services=$((total_services + 1))
    if curl -s -f "http://localhost:${PORT_GATEWAY:-8080}/actuator/health" > /dev/null 2>&1; then
        print_status $GREEN "✅ Gateway accesible en puerto ${PORT_GATEWAY:-8080}"
        services_ok=$((services_ok + 1))
    else
        print_status $RED "❌ Gateway no responde en puerto ${PORT_GATEWAY:-8080}"
    fi
    
    # Intent Manager
    total_services=$((total_services + 1))
    if curl -s -f "http://localhost:${PORT_INTENT_MANAGER:-9904}/actuator/health" > /dev/null 2>&1; then
        print_status $GREEN "✅ Intent Manager accesible en puerto ${PORT_INTENT_MANAGER:-9904}"
        services_ok=$((services_ok + 1))
    else
        print_status $RED "❌ Intent Manager no responde en puerto ${PORT_INTENT_MANAGER:-9904}"
    fi
    
    # Servicios TTS/ASR (si están habilitados)
    if [ "${ENABLE_WHISPER:-true}" == "true" ]; then
        total_services=$((total_services + 1))
        if curl -s -f "http://localhost:${WHISPER_API_PORT:-5000}/health" > /dev/null 2>&1; then
            print_status $GREEN "✅ Whisper STT accesible en puerto ${WHISPER_API_PORT:-5000}"
            services_ok=$((services_ok + 1))
        else
            print_status $RED "❌ Whisper STT no responde en puerto ${WHISPER_API_PORT:-5000}"
        fi
    fi
    
    # F5-TTS deshabilitado permanentemente (modelo demasiado pesado)
    
    # Verificar Azure TTS (servicio principal)
    total_services=$((total_services + 1))
    if curl -s -f "http://localhost:${AZURE_TTS_HOST_PORT:-5004}/health" > /dev/null 2>&1; then
        print_status $GREEN "✅ Azure TTS accesible en puerto ${AZURE_TTS_HOST_PORT:-5004}"
        services_ok=$((services_ok + 1))
    else
        print_status $RED "❌ Azure TTS no responde en puerto ${AZURE_TTS_HOST_PORT:-5004}"
    fi
    
    # Verificar Kokoro TTS (servicio fallback)
    total_services=$((total_services + 1))
    if curl -s -f "http://localhost:${KOKORO_TTS_HOST_PORT:-5002}/health" > /dev/null 2>&1; then
        print_status $GREEN "✅ Kokoro TTS accesible en puerto ${KOKORO_TTS_HOST_PORT:-5002}"
        services_ok=$((services_ok + 1))
    else
        print_status $RED "❌ Kokoro TTS no responde en puerto ${KOKORO_TTS_HOST_PORT:-5002}"
    fi
    
    echo
    print_status $BLUE "📈 RESUMEN: $services_ok/$total_services servicios funcionando correctamente"
    
    if [ $services_ok -eq $total_services ]; then
        print_status $GREEN "🎉 ¡PUERTOCHO-ASSISTANT ESTÁ LISTO!"
        echo
        print_status $BLUE "📡 URLs de acceso:"
        echo "  • Gateway API: http://localhost:${PORT_GATEWAY:-8080}"
        echo "  • Intent Manager: http://localhost:${PORT_INTENT_MANAGER:-9904}"
        
        if [ "${ENABLE_WHISPER:-true}" == "true" ]; then
            echo "  • Whisper STT: http://localhost:${WHISPER_API_PORT:-5000}"
        fi
        
        echo "  • Azure TTS (Principal): http://localhost:${AZURE_TTS_HOST_PORT:-5004}"
        echo "  • Kokoro TTS (Fallback): http://localhost:${KOKORO_TTS_HOST_PORT:-5002}"
        
        echo
        print_status $BLUE "📋 Comandos útiles:"
        echo "  • Ver logs: docker compose logs -f [servicio]"
        echo "  • Estado servicios: docker compose ps"
        echo "  • Parar servicios: docker compose down"
        echo "  • Verificar TTS: ./scripts/check_tts_services.sh"
        
    else
        print_status $YELLOW "⚠️  Algunos servicios no están funcionando correctamente"
        print_status $BLUE "🔍 Para diagnosticar:"
        echo "  • docker compose logs [nombre_servicio]"
        echo "  • docker compose ps"
        echo "  • ./scripts/check_tts_services.sh"
    fi
}

# Manejo de señales para limpieza
cleanup() {
    print_status $YELLOW "🛑 Recibida señal de interrupción"
    print_status $BLUE "ℹ️  Para parar los servicios ejecuta: docker compose down"
    exit 0
}

trap cleanup SIGINT SIGTERM

# Ejecutar función principal
main "$@" 