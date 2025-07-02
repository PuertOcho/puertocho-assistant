#!/bin/bash

# Script de inicio para Taiga MCP Service
echo "🚀 Iniciando Taiga MCP Service..."

# Crear red de Docker si no existe
echo "📡 Configurando red de Docker..."
docker network create taiga-network 2>/dev/null || echo "   Red taiga-network ya existe"

# Construir la imagen si es necesario
echo "🔨 Construyendo imagen Docker..."
docker compose build

# Iniciar el servicio
echo "▶️  Iniciando servicio..."
docker compose up -d

# Esperar a que el servicio esté listo
echo "⏳ Esperando a que el servicio esté listo..."
sleep 3

# Verificar estado
echo "🔍 Verificando estado del servicio..."
curl -s http://localhost:5007/health | jq '.' 2>/dev/null || curl -s http://localhost:5007/health

echo ""
echo "✅ Taiga MCP Service iniciado en http://localhost:5007"
echo ""
echo "📖 Endpoints disponibles:"
echo "   - GET  /health                    - Estado del servicio"
echo "   - POST /login                     - Autenticación"
echo "   - POST /logout                    - Cerrar sesión"
echo "   - POST /session_status            - Estado de sesión"
echo "   - GET  /projects                  - Listar proyectos"
echo "   - POST /projects                  - Crear proyecto"
echo "   - GET  /projects/{id}             - Obtener proyecto"
echo "   - GET  /projects/{id}/user_stories - Listar historias"
echo "   - POST /projects/{id}/user_stories - Crear historia"
echo "   - PUT  /user_stories/{id}         - Actualizar historia"
echo ""
echo "🧪 Para probar el servicio:"
echo "   python test_service.py"
echo ""
echo "🛑 Para detener el servicio:"
echo "   docker compose down" 