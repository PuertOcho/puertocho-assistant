#!/bin/bash

# Script de despliegue para Taiga MCP Service
# Automatiza el proceso de construcción y despliegue

set -e  # Salir si algún comando falla

echo "🚀 DESPLIEGUE TAIGA MCP SERVICE"
echo "==============================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funciones de logging
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Variables
SERVICE_NAME="taiga-mcp-service"
DOCKER_IMAGE="taiga-mcp-api"
HEALTH_CHECK_URL="http://localhost:5007/health"
MAX_WAIT_TIME=60

# Verificar que Docker esté disponible
check_docker() {
    log_info "Verificando Docker..."
    if ! command -v docker &> /dev/null; then
        log_error "Docker no está instalado"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker no está ejecutándose"
        exit 1
    fi
    
    log_success "Docker disponible"
}

# Verificar que docker-compose esté disponible
check_docker_compose() {
    log_info "Verificando Docker Compose..."
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    else
        log_error "Docker Compose no está disponible"
        exit 1
    fi
    
    log_success "Docker Compose disponible: $COMPOSE_CMD"
}

# Parar servicios existentes
stop_existing_services() {
    log_info "Parando servicios existentes..."
    $COMPOSE_CMD down --remove-orphans || true
    log_success "Servicios parados"
}

# Construir imagen
build_image() {
    log_info "Construyendo imagen Docker..."
    $COMPOSE_CMD build --no-cache
    log_success "Imagen construida exitosamente"
}

# Iniciar servicios
start_services() {
    log_info "Iniciando servicios..."
    $COMPOSE_CMD up -d
    log_success "Servicios iniciados"
}

# Verificar que el servicio esté disponible
wait_for_service() {
    log_info "Esperando que el servicio esté disponible..."
    
    local count=0
    local max_attempts=$((MAX_WAIT_TIME / 5))
    
    while [ $count -lt $max_attempts ]; do
        if curl -s -f "$HEALTH_CHECK_URL" > /dev/null 2>&1; then
            log_success "Servicio disponible"
            return 0
        fi
        
        count=$((count + 1))
        log_info "Intento $count/$max_attempts - Esperando 5 segundos..."
        sleep 5
    done
    
    log_error "El servicio no respondió después de ${MAX_WAIT_TIME} segundos"
    return 1
}

# Ejecutar health check
run_health_check() {
    log_info "Ejecutando health check..."
    
    if python3 scripts/health_check.py; then
        log_success "Health check exitoso"
        return 0
    else
        log_error "Health check falló"
        return 1
    fi
}

# Mostrar logs en caso de error
show_logs_on_error() {
    log_error "Mostrando logs del servicio:"
    echo "=================================="
    $COMPOSE_CMD logs --tail=50 taiga-mcp-api
    echo "=================================="
}

# Ejecutar pruebas rápidas
run_quick_tests() {
    log_info "Ejecutando pruebas rápidas..."
    
    # Verificar endpoints básicos
    local base_url="http://localhost:5007"
    
    # Health check
    if ! curl -s -f "$base_url/health" > /dev/null; then
        log_error "Health endpoint no responde"
        return 1
    fi
    
    log_success "Pruebas rápidas exitosas"
    return 0
}

# Mostrar información del despliegue
show_deployment_info() {
    echo ""
    echo "🎉 DESPLIEGUE COMPLETADO EXITOSAMENTE"
    echo "====================================="
    echo "📋 Información del servicio:"
    echo "   • Nombre: $SERVICE_NAME"
    echo "   • URL: http://localhost:5007"
    echo "   • Health Check: $HEALTH_CHECK_URL"
    echo ""
    echo "📝 Comandos útiles:"
    echo "   • Ver logs: $COMPOSE_CMD logs -f taiga-mcp-api"
    echo "   • Reiniciar: $COMPOSE_CMD restart"
    echo "   • Parar: $COMPOSE_CMD down"
    echo "   • Health check: python3 scripts/health_check.py"
    echo ""
    echo "🧪 Ejecutar pruebas:"
    echo "   • Pruebas básicas: python3 scripts/test_service.py"
    echo "   • Pruebas MCP: python3 scripts/test_full_mcp_compatibility.py"
    echo "   • Pruebas complejas: python3 scripts/test_complex_actions.py"
    echo "====================================="
}

# Función principal
main() {
    local start_time=$(date +%s)
    
    echo "🕐 Iniciando despliegue: $(date)"
    echo ""
    
    # Ejecutar pasos del despliegue
    check_docker
    check_docker_compose
    stop_existing_services
    build_image
    start_services
    
    # Verificar que todo esté funcionando
    if wait_for_service; then
        if run_health_check && run_quick_tests; then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            
            show_deployment_info
            echo "⏱️ Tiempo total: ${duration} segundos"
        else
            log_error "Las verificaciones post-despliegue fallaron"
            show_logs_on_error
            exit 1
        fi
    else
        log_error "El servicio no se inició correctamente"
        show_logs_on_error
        exit 1
    fi
}

# Manejar interrupción del usuario
trap 'log_warning "Despliegue interrumpido por el usuario"; exit 130' INT

# Verificar argumentos
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "Uso: $0 [opciones]"
    echo ""
    echo "Opciones:"
    echo "  --help, -h     Mostrar esta ayuda"
    echo "  --no-tests     Saltar pruebas después del despliegue"
    echo ""
    echo "Este script automatiza el despliegue del servicio Taiga MCP:"
    echo "1. Verifica prerrequisitos (Docker, Docker Compose)"
    echo "2. Para servicios existentes"
    echo "3. Construye nueva imagen"
    echo "4. Inicia servicios"
    echo "5. Verifica que el servicio esté funcionando"
    echo "6. Ejecuta health check y pruebas básicas"
    exit 0
fi

# Ejecutar despliegue
main "$@" 