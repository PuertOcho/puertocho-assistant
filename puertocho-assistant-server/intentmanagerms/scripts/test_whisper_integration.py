#!/usr/bin/env python3
"""
Script unificado de pruebas para el servicio de transcripción de Whisper
T5.2 - WhisperTranscriptionService Integration Test

Este script prueba la integración completa con whisper-ms y todos los endpoints del controlador.
Combina pruebas básicas, avanzadas y de integración en un solo script.
"""

import requests
import json
import time
import io
import os
import sys
from pathlib import Path

# Configuración
INTENT_MANAGER_URL = "http://localhost:9904"
WHISPER_MS_URL = "http://192.168.1.88:5000"
TEST_AUDIO_FILE = "/home/puertocho/Proyectos/puertocho-assistant/audio/test_whisper.wav"

class WhisperIntegrationTester:
    def __init__(self):
        self.session = requests.Session()
        self.test_results = []
        
    def log(self, message, level="INFO"):
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] {level}: {message}")
        
    def create_test_audio(self, duration_seconds=2):
        """Crea un archivo de audio de prueba (WAV silencioso)"""
        # WAV header para audio silencioso
        wav_header = bytes([
            0x52, 0x49, 0x46, 0x46,  # "RIFF"
            0x00, 0x00, 0x00, 0x00,  # File size - 8 (se calculará)
            0x57, 0x41, 0x56, 0x45,  # "WAVE"
            0x66, 0x6D, 0x74, 0x20,  # "fmt "
            0x10, 0x00, 0x00, 0x00,  # Chunk size
            0x01, 0x00,              # Audio format (PCM)
            0x01, 0x00,              # Channels (mono)
            0x40, 0x3E, 0x00, 0x00,  # Sample rate (16000)
            0x80, 0x7C, 0x00, 0x00,  # Byte rate
            0x02, 0x00,              # Block align
            0x10, 0x00,              # Bits per sample
            0x64, 0x61, 0x74, 0x61,  # "data"
            0x00, 0x00, 0x00, 0x00   # Data size (se calculará)
        ])
        
        # Calcular tamaños
        samples = 16000 * duration_seconds  # 16kHz * duración
        data_size = samples * 2  # 16-bit = 2 bytes por sample
        file_size = 36 + data_size  # Header (36) + datos
        
        # Actualizar tamaños en el header
        wav_header = bytearray(wav_header)
        wav_header[4:8] = (file_size - 8).to_bytes(4, 'little')  # File size - 8
        wav_header[40:44] = data_size.to_bytes(4, 'little')      # Data size
        
        # Crear datos de silencio
        silence = bytes([0x00] * data_size)
        
        return bytes(wav_header) + silence

    def load_test_audio(self):
        """Carga el archivo de audio de prueba real"""
        if os.path.exists(TEST_AUDIO_FILE):
            with open(TEST_AUDIO_FILE, 'rb') as f:
                return f.read()
        else:
            self.log(f"⚠️ Archivo de audio no encontrado: {TEST_AUDIO_FILE}")
            self.log("   Usando audio silencioso de prueba...")
            return self.create_test_audio(2)

    def test_whisper_ms_health(self):
        """Prueba el health check de whisper-ms"""
        self.log("=== Probando Health Check de Whisper-MS ===")
        
        try:
            response = self.session.get(f"{WHISPER_MS_URL}/health", timeout=10)
            
            if response.status_code == 200:
                self.log("✅ Whisper-MS está funcionando correctamente")
                self.test_results.append(("whisper_ms_health", True, "OK"))
                return True
            else:
                self.log(f"❌ Whisper-MS health check falló: {response.status_code}")
                self.test_results.append(("whisper_ms_health", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error conectando a Whisper-MS: {e}")
            self.test_results.append(("whisper_ms_health", False, str(e)))
            return False

    def test_whisper_ms_status(self):
        """Prueba el status detallado de whisper-ms"""
        self.log("=== Probando Status de Whisper-MS ===")
        
        try:
            response = self.session.get(f"{WHISPER_MS_URL}/status", timeout=10)
            
            if response.status_code == 200:
                status_data = response.json()
                self.log(f"✅ Status obtenido:")
                self.log(f"   - Device: {status_data.get('device', 'unknown')}")
                self.log(f"   - Local model: {status_data.get('local_model', 'unknown')}")
                self.log(f"   - External API: {status_data.get('external_api_enabled', False)}")
                self.log(f"   - Default method: {status_data.get('default_transcription_method', 'unknown')}")
                self.test_results.append(("whisper_ms_status", True, "OK"))
                return True
            else:
                self.log(f"❌ Status falló: {response.status_code}")
                self.test_results.append(("whisper_ms_status", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error obteniendo status: {e}")
            self.test_results.append(("whisper_ms_status", False, str(e)))
            return False

    def test_whisper_ms_direct_transcription(self):
        """Prueba la transcripción directa en whisper-ms"""
        self.log("=== Probando Transcripción Directa en Whisper-MS ===")
        
        try:
            audio_data = self.load_test_audio()
            files = {'audio': ('test_whisper.wav', io.BytesIO(audio_data), 'audio/wav')}
            data = {'language': 'es'}
            
            self.log(f"   - Usando archivo: {TEST_AUDIO_FILE}")
            self.log(f"   - Tamaño del archivo: {len(audio_data)} bytes")
            
            response = self.session.post(f"{WHISPER_MS_URL}/transcribe", files=files, data=data, timeout=30)
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Transcripción directa exitosa:")
                self.log(f"   - Transcription: '{result.get('transcription', '')}'")
                self.log(f"   - Method: {result.get('method', 'unknown')}")
                self.log(f"   - Language: {result.get('language', 'unknown')}")
                self.log(f"   - Detected language: {result.get('detected_language', 'unknown')}")
                self.test_results.append(("whisper_ms_direct_transcription", True, "OK"))
                return True
            else:
                self.log(f"❌ Transcripción directa falló: {response.text}")
                self.test_results.append(("whisper_ms_direct_transcription", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en transcripción directa: {e}")
            self.test_results.append(("whisper_ms_direct_transcription", False, str(e)))
            return False

    def test_intent_manager_whisper_health(self):
        """Prueba el health check del servicio de transcripción en IntentManager"""
        self.log("=== Probando Health Check de Whisper en IntentManager ===")
        
        try:
            response = self.session.get(f"{INTENT_MANAGER_URL}/api/v1/whisper/health", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                status = data.get("status", "UNKNOWN")
                
                if status == "HEALTHY":
                    self.log("✅ Servicio de transcripción en IntentManager está funcionando")
                    self.test_results.append(("intent_manager_whisper_health", True, "HEALTHY"))
                    return True
                else:
                    self.log(f"⚠️ Servicio de transcripción en estado: {status}")
                    self.test_results.append(("intent_manager_whisper_health", False, status))
                    return False
            else:
                self.log(f"❌ Health check falló: {response.status_code}")
                self.test_results.append(("intent_manager_whisper_health", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error conectando a IntentManager: {e}")
            self.test_results.append(("intent_manager_whisper_health", False, str(e)))
            return False

    def test_whisper_stats(self):
        """Prueba el endpoint de estadísticas"""
        self.log("=== Probando Estadísticas del Servicio ===")
        
        try:
            response = self.session.get(f"{INTENT_MANAGER_URL}/api/v1/whisper/stats", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                self.log(f"✅ Estadísticas obtenidas:")
                self.log(f"   - Service status: {data.get('status', 'unknown')}")
                self.log(f"   - Supported languages: {data.get('supported_languages', 'unknown')}")
                self.log(f"   - Default model: {data.get('default_model', 'unknown')}")
                self.test_results.append(("whisper_stats", True, "OK"))
                return True
            else:
                self.log(f"❌ Estadísticas fallaron: {response.status_code}")
                self.test_results.append(("whisper_stats", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error obteniendo estadísticas: {e}")
            self.test_results.append(("whisper_stats", False, str(e)))
            return False

    def test_whisper_info(self):
        """Prueba el endpoint de información del servicio"""
        self.log("=== Probando Información del Servicio ===")
        
        try:
            response = self.session.get(f"{INTENT_MANAGER_URL}/api/v1/whisper/info", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                self.log(f"✅ Información del servicio: {data}")
                self.test_results.append(("whisper_info", True, "OK"))
                return True
            else:
                self.log(f"❌ Información del servicio falló: {response.status_code}")
                self.test_results.append(("whisper_info", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error obteniendo información: {e}")
            self.test_results.append(("whisper_info", False, str(e)))
            return False

    def test_whisper_transcription(self):
        """Prueba la transcripción de audio"""
        self.log("=== Probando Transcripción de Audio ===")
        
        try:
            audio_data = self.load_test_audio()
            files = {'audio': ('test_whisper.wav', io.BytesIO(audio_data), 'audio/wav')}
            data = {'language': 'es'}
            
            self.log(f"   - Usando archivo: {TEST_AUDIO_FILE}")
            self.log(f"   - Tamaño del archivo: {len(audio_data)} bytes")
            
            response = self.session.post(
                f"{INTENT_MANAGER_URL}/api/v1/whisper/transcribe",
                files=files,
                data=data,
                timeout=60
            )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Transcripción exitosa:")
                self.log(f"   - Status: {result.get('status', 'unknown')}")
                self.log(f"   - Transcription: '{result.get('transcription', '')}'")
                self.log(f"   - Language: {result.get('language', 'unknown')}")
                self.log(f"   - Confidence: {result.get('confidence', 'unknown')}")
                
                # Verificar campos requeridos
                required_fields = ["transcription", "language", "status", "timestamp"]
                missing_fields = [field for field in required_fields if field not in result]
                
                if missing_fields:
                    self.log(f"⚠️ Campos faltantes en respuesta: {missing_fields}")
                    self.test_results.append(("whisper_transcription", False, f"Missing fields: {missing_fields}"))
                    return False
                else:
                    self.test_results.append(("whisper_transcription", True, "OK"))
                    return True
            else:
                self.log(f"❌ Transcripción falló: {response.status_code} - {response.text}")
                self.test_results.append(("whisper_transcription", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en transcripción: {e}")
            self.test_results.append(("whisper_transcription", False, str(e)))
            return False

    def test_whisper_transcription_async(self):
        """Prueba la transcripción asíncrona"""
        self.log("=== Probando Transcripción Asíncrona ===")
        
        try:
            audio_data = self.load_test_audio()
            files = {'audio': ('test_whisper.wav', io.BytesIO(audio_data), 'audio/wav')}
            data = {'language': 'es'}
            
            response = self.session.post(
                f"{INTENT_MANAGER_URL}/api/v1/whisper/transcribe/async",
                files=files,
                data=data,
                timeout=30
            )
            
            if response.status_code == 202:  # Accepted
                result = response.json()
                self.log(f"✅ Transcripción asíncrona iniciada:")
                self.log(f"   - Task ID: {result.get('task_id', 'unknown')}")
                self.log(f"   - Status: {result.get('status', 'unknown')}")
                self.log(f"   - Message: {result.get('message', 'unknown')}")
                self.test_results.append(("whisper_transcription_async", True, "OK"))
                return True
            else:
                self.log(f"❌ Transcripción asíncrona falló: {response.status_code} - {response.text}")
                self.test_results.append(("whisper_transcription_async", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en transcripción asíncrona: {e}")
            self.test_results.append(("whisper_transcription_async", False, str(e)))
            return False

    def test_whisper_test_endpoint(self):
        """Prueba el endpoint de test"""
        self.log("=== Probando Endpoint de Test ===")
        
        try:
            response = self.session.post(
                f"{INTENT_MANAGER_URL}/api/v1/whisper/test",
                data={"language": "es"},
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Test exitoso:")
                self.log(f"   - Status: {result.get('status', 'unknown')}")
                if result.get('status') == 'SUCCESS':
                    self.log(f"   - Transcription: '{result.get('transcription', '')}'")
                    self.log(f"   - Language: {result.get('language', 'unknown')}")
                else:
                    self.log(f"   - Error: {result.get('error_message', 'unknown')}")
                self.test_results.append(("whisper_test", True, "OK"))
                return True
            else:
                self.log(f"❌ Test falló: {response.status_code} - {response.text}")
                self.test_results.append(("whisper_test", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en test: {e}")
            self.test_results.append(("whisper_test", False, str(e)))
            return False

    def test_audio_processing_integration(self):
        """Prueba la integración con el pipeline de procesamiento de audio"""
        self.log("=== Probando Integración con Pipeline de Audio ===")
        
        try:
            audio_data = self.load_test_audio()
            files = {'audio': ('test_whisper.wav', io.BytesIO(audio_data), 'audio/wav')}
            data = {
                'language': 'es',
                'location': 'Madrid',
                'temperature': '22°C',
                'device_id': 'test_device'
            }
            
            self.log(f"   - Usando archivo: {TEST_AUDIO_FILE}")
            self.log(f"   - Tamaño del archivo: {len(audio_data)} bytes")
            
            response = self.session.post(
                f"{INTENT_MANAGER_URL}/api/v1/audio/process",
                files=files,
                data=data,
                timeout=120
            )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Integración exitosa:")
                self.log(f"   - Success: {result.get('success', False)}")
                
                if result.get('success'):
                    audio_result = result.get('result', {})
                    self.log(f"   - Status: {audio_result.get('status', 'unknown')}")
                    
                    transcription = audio_result.get('transcription', {})
                    if transcription:
                        self.log(f"   - Transcription: '{transcription.get('text', '')}'")
                        self.log(f"   - Confidence: {transcription.get('confidence', 0)}")
                        self.log(f"   - Language: {transcription.get('language', 'unknown')}")
                        self.log(f"   - Processing time: {transcription.get('processing_time_ms', 0)}ms")
                    
                    intent_classification = audio_result.get('intent_classification', {})
                    if intent_classification:
                        self.log(f"   - Intent: {intent_classification.get('intent_id', 'unknown')}")
                        self.log(f"   - Confidence: {intent_classification.get('confidence_score', 0)}")
                        self.log(f"   - Fallback used: {intent_classification.get('fallback_used', False)}")
                    
                    moe_voting = audio_result.get('moe_voting', {})
                    if moe_voting:
                        self.log(f"   - MoE Intent: {moe_voting.get('finalIntent', 'unknown')}")
                        self.log(f"   - MoE Confidence: {moe_voting.get('consensusConfidence', 0)}")
                    
                    # Verificar que la transcripción se realizó
                    if transcription and transcription.get('text'):
                        self.log("✅ Transcripción integrada correctamente")
                        self.test_results.append(("audio_processing_integration", True, "OK"))
                        return True
                    else:
                        self.log("⚠️ Transcripción no encontrada en resultado")
                        self.test_results.append(("audio_processing_integration", False, "No transcription"))
                        return False
                else:
                    self.log(f"   - Error: {result.get('error', 'unknown')}")
                    self.test_results.append(("audio_processing_integration", False, f"Pipeline failed"))
                    return False
            else:
                self.log(f"❌ Integración falló: {response.status_code} - {response.text}")
                self.test_results.append(("audio_processing_integration", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en integración: {e}")
            self.test_results.append(("audio_processing_integration", False, str(e)))
            return False

    def run_all_tests(self):
        """Ejecuta todas las pruebas"""
        self.log("🚀 Iniciando pruebas completas del servicio de transcripción de Whisper (T5.2)")
        self.log(f"Whisper-MS URL: {WHISPER_MS_URL}")
        self.log(f"IntentManager URL: {INTENT_MANAGER_URL}")
        self.log(f"Test Audio File: {TEST_AUDIO_FILE}")
        
        # Pruebas básicas de Whisper-MS
        self.test_whisper_ms_health()
        self.test_whisper_ms_status()
        self.test_whisper_ms_direct_transcription()
        
        # Pruebas del servicio IntentManager
        self.test_intent_manager_whisper_health()
        self.test_whisper_stats()
        self.test_whisper_info()
        
        # Pruebas de transcripción
        self.test_whisper_transcription()
        self.test_whisper_transcription_async()
        self.test_whisper_test_endpoint()
        
        # Prueba de integración completa
        self.test_audio_processing_integration()
        
        # Mostrar resumen
        self.show_summary()
    
    def show_summary(self):
        """Muestra el resumen de las pruebas"""
        self.log("=== RESUMEN DE PRUEBAS ===")
        
        total_tests = len(self.test_results)
        passed_tests = sum(1 for _, passed, _ in self.test_results if passed)
        failed_tests = total_tests - passed_tests
        
        self.log(f"Total de pruebas: {total_tests}")
        self.log(f"Pruebas exitosas: {passed_tests}")
        self.log(f"Pruebas fallidas: {failed_tests}")
        self.log(f"Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        
        if failed_tests > 0:
            self.log("\n❌ Pruebas fallidas:")
            for test_name, passed, error in self.test_results:
                if not passed:
                    self.log(f"  - {test_name}: {error}")
        
        if passed_tests == total_tests:
            self.log("\n🎉 ¡Todas las pruebas pasaron! T5.2 completado exitosamente.")
        else:
            self.log(f"\n⚠️ {failed_tests} pruebas fallaron. Revisar configuración.")

def main():
    """Función principal"""
    tester = WhisperIntegrationTester()
    tester.run_all_tests()

if __name__ == "__main__":
    main()
