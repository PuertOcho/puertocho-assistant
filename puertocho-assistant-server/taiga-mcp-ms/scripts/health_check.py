#!/usr/bin/env python3
"""
Script de health check para monitoreo en producción
Verifica que el servicio Taiga MCP esté funcionando correctamente
"""

import requests
import json
import sys
import time
from datetime import datetime

# Configuración
BASE_URL = "http://localhost:5007"
TIMEOUT = 30
MAX_RETRIES = 3

def check_health():
    """Verificar estado del servicio"""
    try:
        response = requests.get(f"{BASE_URL}/health", timeout=TIMEOUT)
        
        if response.status_code == 200:
            health_data = response.json()
            
            print(f"✅ Servicio disponible: {health_data['status']}")
            print(f"🔗 Taiga conectado: {health_data['taiga_available']}")
            print(f"👥 Sesiones activas: {health_data['active_sessions']}")
            
            if health_data['status'] == 'ok' and health_data['taiga_available']:
                print("🎉 Estado: SALUDABLE")
                return 0
            elif health_data['status'] == 'degraded':
                print("⚠️ Estado: DEGRADADO (Taiga no disponible)")
                return 1
            else:
                print("❌ Estado: ERROR")
                return 2
        else:
            print(f"❌ Servicio no responde: HTTP {response.status_code}")
            return 3
            
    except requests.exceptions.ConnectionError:
        print("❌ No se pudo conectar al servicio")
        return 4
    except requests.exceptions.Timeout:
        print("❌ Timeout al conectar al servicio")
        return 5
    except Exception as e:
        print(f"❌ Error inesperado: {e}")
        return 6

def check_with_retries():
    """Verificar con reintentos"""
    for attempt in range(1, MAX_RETRIES + 1):
        print(f"🔍 Intento {attempt}/{MAX_RETRIES} - {datetime.now().strftime('%H:%M:%S')}")
        
        result = check_health()
        
        if result == 0:
            return 0
        
        if attempt < MAX_RETRIES:
            print(f"⏳ Esperando 5 segundos antes del siguiente intento...")
            time.sleep(5)
    
    print(f"❌ Falló después de {MAX_RETRIES} intentos")
    return result

def main():
    """Función principal"""
    print("🏥 HEALTH CHECK - Taiga MCP Service")
    print("=" * 50)
    print(f"🔗 URL: {BASE_URL}")
    print(f"⏱️ Timeout: {TIMEOUT}s")
    print(f"🔄 Max reintentos: {MAX_RETRIES}")
    print("=" * 50)
    
    exit_code = check_with_retries()
    
    print("\n" + "=" * 50)
    if exit_code == 0:
        print("✅ HEALTH CHECK: EXITOSO")
    else:
        print("❌ HEALTH CHECK: FALLIDO")
    print("=" * 50)
    
    sys.exit(exit_code)

if __name__ == "__main__":
    main() 