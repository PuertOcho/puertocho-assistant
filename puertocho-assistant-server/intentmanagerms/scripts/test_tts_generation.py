#!/usr/bin/env python3
"""
Script de pruebas automatizadas para T5.2.5 - Generación de Audio TTS
Prueba la integración con servicios Azure TTS y Kokoro TTS
"""

import requests
import json
import time
import sys
from datetime import datetime

# Configuración
BASE_URL = "http://localhost:9904"
TTS_BASE_URL = f"{BASE_URL}/api/v1/tts"

# Colores para output
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_header(title):
    print(f"\n{Colors.BOLD}{Colors.BLUE}{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}{Colors.ENDC}")

def print_test(test_name, success, message=""):
    status = f"{Colors.GREEN}✅ PASÓ{Colors.ENDC}" if success else f"{Colors.RED}❌ FALLÓ{Colors.ENDC}"
    print(f"  {test_name}: {status}")
    if message:
        print(f"    {Colors.YELLOW}{message}{Colors.ENDC}")

def make_request(method, url, data=None, headers=None):
    """Realiza una petición HTTP con manejo de errores"""
    try:
        if method.upper() == "GET":
            response = requests.get(url, headers=headers, timeout=30)
        elif method.upper() == "POST":
            response = requests.post(url, json=data, headers=headers, timeout=30)
        else:
            raise ValueError(f"Método HTTP no soportado: {method}")
        
        return response
    except requests.exceptions.RequestException as e:
        print(f"    {Colors.RED}Error de conexión: {e}{Colors.ENDC}")
        return None

def test_health_check():
    """Prueba 1: Health check del servicio TTS"""
    print_header("PRUEBA 1: Health Check del Servicio TTS")
    
    response = make_request("GET", f"{TTS_BASE_URL}/health")
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            status = data.get("status", "unknown")
            success = status in ["healthy", "ok"]
            message = f"Status: {status}"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Health Check TTS", success, message)
    return success

def test_providers_info():
    """Prueba 2: Información de proveedores disponibles"""
    print_header("PRUEBA 2: Información de Proveedores TTS")
    
    response = make_request("GET", f"{TTS_BASE_URL}/providers")
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            providers = data.get("available_providers", [])
            success = len(providers) >= 2 and "azure" in providers and "kokoro" in providers
            message = f"Proveedores disponibles: {', '.join(providers)}"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Información de Proveedores", success, message)
    return success

def test_voices_info():
    """Prueba 3: Información de voces disponibles"""
    print_header("PRUEBA 3: Información de Voces TTS")
    
    response = make_request("GET", f"{TTS_BASE_URL}/voices")
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            voices_by_provider = data.get("voices_by_provider", {})
            success = "azure" in voices_by_provider and "kokoro" in voices_by_provider
            message = f"Voces por proveedor: {list(voices_by_provider.keys())}"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Información de Voces", success, message)
    return success

def test_tts_generation_azure():
    """Prueba 4: Generación TTS con Azure"""
    print_header("PRUEBA 4: Generación TTS con Azure")
    
    test_data = {
        "text": "¡Hola! Soy tu asistente de voz. ¿En qué puedo ayudarte?",
        "language": "es-ES",
        "voice": "Abril",
        "provider": "azure",
        "speed": 1.0
    }
    
    response = make_request("POST", f"{TTS_BASE_URL}/generate", test_data)
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            success = data.get("success", False)
            provider = data.get("provider", "")
            duration = data.get("audio_duration", 0)
            message = f"Proveedor: {provider}, Duración: {duration:.2f}s"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Generación TTS Azure", success, message)
    return success

def test_tts_generation_kokoro():
    """Prueba 5: Generación TTS con Kokoro"""
    print_header("PRUEBA 5: Generación TTS con Kokoro")
    
    test_data = {
        "text": "Hola, soy tu asistente virtual. ¿Cómo puedo ayudarte hoy?",
        "language": "es",
        "voice": "ef_dora",
        "provider": "kokoro",
        "speed": 1.0
    }
    
    response = make_request("POST", f"{TTS_BASE_URL}/generate", test_data)
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            success = data.get("success", False)
            provider = data.get("provider", "")
            duration = data.get("audio_duration", 0)
            message = f"Proveedor: {provider}, Duración: {duration:.2f}s"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Generación TTS Kokoro", success, message)
    return success

def test_tts_generation_auto():
    """Prueba 6: Generación TTS con proveedor automático"""
    print_header("PRUEBA 6: Generación TTS con Proveedor Automático")
    
    test_data = {
        "text": "Este es un test de generación automática de audio.",
        "language": "es",
        "speed": 1.0
        # No especificamos provider para usar el automático
    }
    
    response = make_request("POST", f"{TTS_BASE_URL}/generate", test_data)
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            success = data.get("success", False)
            provider = data.get("provider", "")
            duration = data.get("audio_duration", 0)
            message = f"Proveedor automático: {provider}, Duración: {duration:.2f}s"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Generación TTS Automática", success, message)
    return success

def test_tts_generation_async():
    """Prueba 7: Generación TTS asíncrona"""
    print_header("PRUEBA 7: Generación TTS Asíncrona")
    
    test_data = {
        "text": "Test de generación asíncrona de audio TTS.",
        "language": "es",
        "provider": "azure"
    }
    
    response = make_request("POST", f"{TTS_BASE_URL}/generate/async", test_data)
    if not response:
        return False
    
    success = response.status_code == 202  # Accepted
    if success:
        try:
            data = response.json()
            success = data.get("success", False) and data.get("status") == "processing"
            request_id = data.get("request_id", "")
            message = f"Request ID: {request_id}, Status: {data.get('status')}"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Generación TTS Asíncrona", success, message)
    return success

def test_tts_test_endpoint():
    """Prueba 8: Endpoint de test TTS"""
    print_header("PRUEBA 8: Endpoint de Test TTS")
    
    test_data = {
        "text": "Test del endpoint de prueba TTS.",
        "language": "es",
        "provider": "kokoro"
    }
    
    response = make_request("POST", f"{TTS_BASE_URL}/test", test_data)
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            success = data.get("success", False)
            test_text = data.get("test_text", "")
            provider_used = data.get("provider_used", "")
            message = f"Texto: '{test_text[:30]}...', Proveedor: {provider_used}"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Endpoint de Test TTS", success, message)
    return success

def test_tts_statistics():
    """Prueba 9: Estadísticas del servicio TTS"""
    print_header("PRUEBA 9: Estadísticas del Servicio TTS")
    
    response = make_request("GET", f"{TTS_BASE_URL}/stats")
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            total_requests = data.get("total_requests", 0)
            successful_requests = data.get("successful_requests", 0)
            success = total_requests >= 0
            message = f"Total requests: {total_requests}, Exitosos: {successful_requests}"
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Estadísticas TTS", success, message)
    return success

def test_tts_cache_clear():
    """Prueba 10: Limpieza de cache TTS"""
    print_header("PRUEBA 10: Limpieza de Cache TTS")
    
    response = make_request("POST", f"{TTS_BASE_URL}/cache/clear")
    if not response:
        return False
    
    success = response.status_code == 200
    if success:
        try:
            data = response.json()
            success = data.get("success", False)
            message = data.get("message", "Cache limpiado")
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Limpieza de Cache TTS", success, message)
    return success

def test_error_handling():
    """Prueba 11: Manejo de errores"""
    print_header("PRUEBA 11: Manejo de Errores")
    
    # Test con texto vacío
    test_data = {
        "text": "",
        "language": "es"
    }
    
    response = make_request("POST", f"{TTS_BASE_URL}/generate", test_data)
    if not response:
        return False
    
    success = response.status_code == 500  # Debería fallar
    if success:
        try:
            data = response.json()
            success = not data.get("success", True)  # Debería ser false
            error_message = data.get("error_message", "")
            message = f"Error manejado correctamente: {error_message[:50]}..."
        except:
            success = False
            message = "Respuesta no es JSON válido"
    else:
        message = f"Status code: {response.status_code}"
    
    print_test("Manejo de Errores", success, message)
    return success

def main():
    """Función principal que ejecuta todas las pruebas"""
    print_header("INICIANDO PRUEBAS T5.2.5 - GENERACIÓN DE AUDIO TTS")
    print(f"{Colors.BLUE}URL Base: {BASE_URL}{Colors.ENDC}")
    print(f"{Colors.BLUE}Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}{Colors.ENDC}")
    
    # Lista de pruebas
    tests = [
        ("Health Check", test_health_check),
        ("Información de Proveedores", test_providers_info),
        ("Información de Voces", test_voices_info),
        ("Generación TTS Azure", test_tts_generation_azure),
        ("Generación TTS Kokoro", test_tts_generation_kokoro),
        ("Generación TTS Automática", test_tts_generation_auto),
        ("Generación TTS Asíncrona", test_tts_generation_async),
        ("Endpoint de Test", test_tts_test_endpoint),
        ("Estadísticas", test_tts_statistics),
        ("Limpieza de Cache", test_tts_cache_clear),
        ("Manejo de Errores", test_error_handling)
    ]
    
    # Ejecutar pruebas
    results = []
    for test_name, test_func in tests:
        try:
            result = test_func()
            results.append((test_name, result))
        except Exception as e:
            print(f"{Colors.RED}Error en prueba {test_name}: {e}{Colors.ENDC}")
            results.append((test_name, False))
    
    # Resumen final
    print_header("RESUMEN DE PRUEBAS T5.2.5")
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    print(f"{Colors.BOLD}Pruebas ejecutadas: {total}{Colors.ENDC}")
    print(f"{Colors.BOLD}Pruebas exitosas: {Colors.GREEN}{passed}{Colors.ENDC}")
    print(f"{Colors.BOLD}Pruebas fallidas: {Colors.RED}{total - passed}{Colors.ENDC}")
    print(f"{Colors.BOLD}Porcentaje de éxito: {Colors.BLUE}{(passed/total)*100:.1f}%{Colors.ENDC}")
    
    # Detalles de cada prueba
    print(f"\n{Colors.BOLD}Detalles:{Colors.ENDC}")
    for test_name, result in results:
        status = f"{Colors.GREEN}✅ PASÓ{Colors.ENDC}" if result else f"{Colors.RED}❌ FALLÓ{Colors.ENDC}"
        print(f"  {test_name}: {status}")
    
    # Resultado final
    if passed == total:
        print(f"\n{Colors.BOLD}{Colors.GREEN}🎉 TODAS LAS PRUEBAS PASARON - T5.2.5 COMPLETADO AL 100%{Colors.ENDC}")
        return 0
    else:
        print(f"\n{Colors.BOLD}{Colors.RED}⚠️  {total - passed} PRUEBAS FALLARON - REVISAR IMPLEMENTACIÓN{Colors.ENDC}")
        return 1

if __name__ == "__main__":
    try:
        exit_code = main()
        sys.exit(exit_code)
    except KeyboardInterrupt:
        print(f"\n{Colors.YELLOW}Pruebas interrumpidas por el usuario{Colors.ENDC}")
        sys.exit(1)
    except Exception as e:
        print(f"\n{Colors.RED}Error inesperado: {e}{Colors.ENDC}")
        sys.exit(1)
