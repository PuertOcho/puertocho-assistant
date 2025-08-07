#!/usr/bin/env python3
"""
Script de pruebas automatizadas para el servicio de procesamiento de audio.
Prueba todos los endpoints del AudioProcessingController.
"""

import requests
import json
import time
import os
import sys
from pathlib import Path

# Configuración
BASE_URL = "http://localhost:9904"
AUDIO_ENDPOINTS = {
    "process": "/api/v1/audio/process",
    "process_simple": "/api/v1/audio/process/simple",
    "supported_formats": "/api/v1/audio/supported-formats",
    "health": "/api/v1/audio/health",
    "test": "/api/v1/audio/test"
}

# Colores para output
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_success(message):
    print(f"{Colors.GREEN}✅ {message}{Colors.ENDC}")

def print_error(message):
    print(f"{Colors.RED}❌ {message}{Colors.ENDC}")

def print_warning(message):
    print(f"{Colors.YELLOW}⚠️  {message}{Colors.ENDC}")

def print_info(message):
    print(f"{Colors.BLUE}ℹ️  {message}{Colors.ENDC}")

def print_header(message):
    print(f"\n{Colors.BOLD}{Colors.BLUE}{'='*60}{Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.BLUE}{message}{Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.BLUE}{'='*60}{Colors.ENDC}")

def check_service_availability():
    """Verifica si el servicio está disponible"""
    print_header("Verificando disponibilidad del servicio")
    
    try:
        response = requests.get(f"{BASE_URL}/actuator/health", timeout=10)
        if response.status_code == 200:
            print_success("Servicio disponible")
            return True
        else:
            print_error(f"Servicio no disponible. Status: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print_error(f"Error conectando al servicio: {e}")
        return False

def test_health_endpoint():
    """Prueba el endpoint de health check"""
    print_header("Prueba: Health Check del servicio de audio")
    
    try:
        response = requests.get(f"{BASE_URL}{AUDIO_ENDPOINTS['health']}", timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Health check exitoso")
            print_info(f"Status: {data.get('status')}")
            print_info(f"Formatos soportados: {data.get('supported_formats_count')}")
            print_info(f"Tamaño máximo: {data.get('max_file_size_mb')} MB")
            return True
        else:
            print_error(f"Health check falló. Status: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en health check: {e}")
        return False

def test_supported_formats():
    """Prueba el endpoint de formatos soportados"""
    print_header("Prueba: Formatos de audio soportados")
    
    try:
        response = requests.get(f"{BASE_URL}{AUDIO_ENDPOINTS['supported_formats']}", timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Obtención de formatos exitosa")
            print_info(f"Formatos: {', '.join(data.get('supported_formats', []))}")
            print_info(f"Tamaño máximo: {data.get('max_file_size_mb')} MB")
            return True
        else:
            print_error(f"Obtención de formatos falló. Status: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error obteniendo formatos: {e}")
        return False

def create_test_audio_file():
    """Crea un archivo de audio de prueba"""
    test_audio_path = "test_audio.wav"
    
    # Crear un archivo WAV simple de prueba
    # Header WAV básico (44 bytes)
    wav_header = (
        b'RIFF' +                    # Chunk ID
        (36).to_bytes(4, 'little') + # Chunk Size
        b'WAVE' +                    # Format
        b'fmt ' +                    # Subchunk1 ID
        (16).to_bytes(4, 'little') + # Subchunk1 Size
        (1).to_bytes(2, 'little') +  # Audio Format (PCM)
        (1).to_bytes(2, 'little') +  # Num Channels
        (8000).to_bytes(4, 'little') + # Sample Rate
        (8000).to_bytes(4, 'little') + # Byte Rate
        (1).to_bytes(2, 'little') +  # Block Align
        (8).to_bytes(2, 'little') +  # Bits per Sample
        b'data' +                    # Subchunk2 ID
        (0).to_bytes(4, 'little')    # Subchunk2 Size
    )
    
    with open(test_audio_path, 'wb') as f:
        f.write(wav_header)
    
    return test_audio_path

def test_simple_audio_processing():
    """Prueba el procesamiento simple de audio"""
    print_header("Prueba: Procesamiento simple de audio")
    
    try:
        # Crear archivo de audio de prueba
        test_audio_path = create_test_audio_file()
        
        with open(test_audio_path, 'rb') as f:
            files = {'audio': ('test.wav', f, 'audio/wav')}
            
            response = requests.post(
                f"{BASE_URL}{AUDIO_ENDPOINTS['process_simple']}", 
                files=files,
                timeout=30
            )
        
        # Limpiar archivo de prueba
        os.remove(test_audio_path)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Procesamiento simple exitoso")
            
            if 'result' in data:
                result = data['result']
                print_info(f"Status: {result.get('status')}")
                print_info(f"Request ID: {result.get('request_id')}")
                
                if 'transcription' in result:
                    transcription = result['transcription']
                    print_info(f"Transcripción: '{transcription.get('text')}'")
                    print_info(f"Confianza: {transcription.get('confidence')}")
                
                if 'intent_classification' in result:
                    intent = result['intent_classification']
                    print_info(f"Intención: {intent.get('intent_id')}")
                    print_info(f"Confianza: {intent.get('confidence_score')}")
                
                if 'response' in result:
                    response_data = result['response']
                    print_info(f"Respuesta: '{response_data.get('text_response')}'")
            
            return True
        else:
            print_error(f"Procesamiento simple falló. Status: {response.status_code}")
            print_error(f"Respuesta: {response.text}")
            return False
            
    except Exception as e:
        print_error(f"Error en procesamiento simple: {e}")
        return False

def test_audio_processing_with_metadata():
    """Prueba el procesamiento de audio con metadata"""
    print_header("Prueba: Procesamiento de audio con metadata")
    
    try:
        # Crear archivo de audio de prueba
        test_audio_path = create_test_audio_file()
        
        # Metadata de prueba
        metadata = {
            "device_id": "test-device-001",
            "user_id": "test-user",
            "location": "Madrid, España",
            "temperature": "22°C",
            "device_type": "smartphone",
            "language": "es"
        }
        
        with open(test_audio_path, 'rb') as f:
            files = {'audio': ('test_with_metadata.wav', f, 'audio/wav')}
            data = {
                'metadata': json.dumps(metadata)
            }
            
            response = requests.post(
                f"{BASE_URL}{AUDIO_ENDPOINTS['process']}", 
                files=files,
                data=data,
                timeout=30
            )
        
        # Limpiar archivo de prueba
        os.remove(test_audio_path)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Procesamiento con metadata exitoso")
            
            if 'result' in data:
                result = data['result']
                print_info(f"Status: {result.get('status')}")
                
                if 'original_metadata' in result:
                    original_metadata = result['original_metadata']
                    print_info(f"Dispositivo: {original_metadata.get('device_id')}")
                    print_info(f"Ubicación: {original_metadata.get('location')}")
                    print_info(f"Temperatura: {original_metadata.get('temperature')}")
            
            return True
        else:
            print_error(f"Procesamiento con metadata falló. Status: {response.status_code}")
            print_error(f"Respuesta: {response.text}")
            return False
            
    except Exception as e:
        print_error(f"Error en procesamiento con metadata: {e}")
        return False

def test_audio_processing_with_config():
    """Prueba el procesamiento de audio con configuración personalizada"""
    print_header("Prueba: Procesamiento de audio con configuración")
    
    try:
        # Crear archivo de audio de prueba
        test_audio_path = create_test_audio_file()
        
        # Configuración de procesamiento
        processing_config = {
            "preferred_language": "es",
            "whisper_model": "base",
            "generate_audio_response": True,
            "generate_text_response": True,
            "timeout_seconds": 30
        }
        
        with open(test_audio_path, 'rb') as f:
            files = {'audio': ('test_with_config.wav', f, 'audio/wav')}
            data = {
                'processing_config': json.dumps(processing_config)
            }
            
            response = requests.post(
                f"{BASE_URL}{AUDIO_ENDPOINTS['process']}", 
                files=files,
                data=data,
                timeout=30
            )
        
        # Limpiar archivo de prueba
        os.remove(test_audio_path)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Procesamiento con configuración exitoso")
            
            if 'result' in data:
                result = data['result']
                print_info(f"Status: {result.get('status')}")
                
                if 'processing_config' in result:
                    config = result['processing_config']
                    print_info(f"Idioma preferido: {config.get('preferred_language')}")
                    print_info(f"Modelo Whisper: {config.get('whisper_model')}")
            
            return True
        else:
            print_error(f"Procesamiento con configuración falló. Status: {response.status_code}")
            print_error(f"Respuesta: {response.text}")
            return False
            
    except Exception as e:
        print_error(f"Error en procesamiento con configuración: {e}")
        return False

def test_endpoint():
    """Prueba el endpoint de test"""
    print_header("Prueba: Endpoint de test")
    
    try:
        response = requests.post(f"{BASE_URL}{AUDIO_ENDPOINTS['test']}", timeout=30)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Test endpoint exitoso")
            print_info(f"Mensaje: {data.get('message')}")
            
            if 'test_result' in data:
                test_result = data['test_result']
                print_info(f"Status: {test_result.get('status')}")
            
            return True
        else:
            print_error(f"Test endpoint falló. Status: {response.status_code}")
            print_error(f"Respuesta: {response.text}")
            return False
            
    except Exception as e:
        print_error(f"Error en test endpoint: {e}")
        return False

def test_error_handling():
    """Prueba el manejo de errores"""
    print_header("Prueba: Manejo de errores")
    
    tests_passed = 0
    total_tests = 3
    
    # Test 1: Archivo vacío
    try:
        files = {'audio': ('empty.wav', b'', 'audio/wav')}
        response = requests.post(
            f"{BASE_URL}{AUDIO_ENDPOINTS['process_simple']}", 
            files=files,
            timeout=10
        )
        
        if response.status_code == 400:
            print_success("Error de archivo vacío manejado correctamente")
            tests_passed += 1
        else:
            print_error("Error de archivo vacío no manejado correctamente")
    except Exception as e:
        print_error(f"Error en test de archivo vacío: {e}")
    
    # Test 2: Sin archivo
    try:
        # Enviar petición multipart/form-data con un campo dummy pero sin archivo
        response = requests.post(
            f"{BASE_URL}{AUDIO_ENDPOINTS['process_simple']}", 
            files={'dummy': ('dummy.txt', 'dummy content', 'text/plain')},  # Campo dummy para forzar multipart
            timeout=10
        )
        
        if response.status_code == 400:
            print_success("Error de archivo faltante manejado correctamente")
            tests_passed += 1
        else:
            print_error(f"Error de archivo faltante no manejado correctamente. Status: {response.status_code}")
            print_error(f"Respuesta: {response.text}")
    except Exception as e:
        print_error(f"Error en test de archivo faltante: {e}")
    
    # Test 3: Metadata JSON inválido
    try:
        test_audio_path = create_test_audio_file()
        
        with open(test_audio_path, 'rb') as f:
            files = {'audio': ('test_invalid_metadata.wav', f, 'audio/wav')}
            data = {'metadata': 'invalid json'}
            
            response = requests.post(
                f"{BASE_URL}{AUDIO_ENDPOINTS['process']}", 
                files=files,
                data=data,
                timeout=10
            )
        
        os.remove(test_audio_path)
        
        if response.status_code == 200:  # Debería continuar sin metadata
            print_success("Metadata JSON inválido manejado correctamente")
            tests_passed += 1
        else:
            print_error("Metadata JSON inválido no manejado correctamente")
    except Exception as e:
        print_error(f"Error en test de metadata inválido: {e}")
    
    print_info(f"Tests de manejo de errores: {tests_passed}/{total_tests} pasaron")
    return tests_passed == total_tests

def main():
    """Función principal"""
    print_header("INICIO DE PRUEBAS - SERVICIO DE PROCESAMIENTO DE AUDIO")
    print_info(f"URL base: {BASE_URL}")
    print_info(f"Timestamp: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Verificar disponibilidad del servicio
    if not check_service_availability():
        print_error("El servicio no está disponible. Abortando pruebas.")
        sys.exit(1)
    
    # Ejecutar pruebas
    tests = [
        ("Health Check", test_health_endpoint),
        ("Formatos Soportados", test_supported_formats),
        ("Procesamiento Simple", test_simple_audio_processing),
        ("Procesamiento con Metadata", test_audio_processing_with_metadata),
        ("Procesamiento con Configuración", test_audio_processing_with_config),
        ("Endpoint de Test", test_endpoint),
        ("Manejo de Errores", test_error_handling)
    ]
    
    passed_tests = 0
    total_tests = len(tests)
    
    for test_name, test_function in tests:
        try:
            if test_function():
                passed_tests += 1
            time.sleep(1)  # Pausa entre pruebas
        except Exception as e:
            print_error(f"Error ejecutando {test_name}: {e}")
    
    # Resumen final
    print_header("RESUMEN DE PRUEBAS")
    print_info(f"Pruebas ejecutadas: {total_tests}")
    print_info(f"Pruebas exitosas: {passed_tests}")
    print_info(f"Pruebas fallidas: {total_tests - passed_tests}")
    
    if passed_tests == total_tests:
        print_success("🎉 TODAS LAS PRUEBAS PASARON EXITOSAMENTE")
        sys.exit(0)
    else:
        print_error(f"❌ {total_tests - passed_tests} PRUEBAS FALLARON")
        sys.exit(1)

if __name__ == "__main__":
    main()
