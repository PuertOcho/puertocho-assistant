#!/usr/bin/env python3
"""
Script de pruebas para el microservicio whisper-api mejorado
T5.2 - WhisperTranscriptionService

Este script prueba la integración con whisper-ms y los endpoints del controlador,
incluyendo las nuevas funcionalidades de API externa y fallback.
"""

import requests
import json
import time
import os
import sys
from pathlib import Path

# Configuración
INTENT_MANAGER_URL = "http://localhost:9904"
WHISPER_MS_URL = "http://localhost:5000"

class WhisperTranscriptionTester:
    def __init__(self):
        self.session = requests.Session()
        self.test_results = []
        
    def log(self, message, level="INFO"):
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] {level}: {message}")
        
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
        """Prueba el nuevo endpoint de status de whisper-ms"""
        self.log("=== Probando Status de Whisper-MS ===")
        
        try:
            response = self.session.get(f"{WHISPER_MS_URL}/status", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                self.log(f"✅ Status obtenido: Dispositivo={data['device']}, Modelo={data['local_model']}")
                self.log(f"   API externa habilitada: {data['external_api_enabled']}")
                self.log(f"   Fallback habilitado: {data['fallback_enabled']}")
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
                self.log(f"✅ Estadísticas obtenidas: {data}")
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
    
    def create_test_audio(self):
        """Crea un archivo de audio de prueba (WAV silencioso)"""
        self.log("=== Creando Audio de Prueba ===")
        
        # WAV header para 1 segundo de silencio a 16kHz, 16-bit, mono
        wav_header = bytes([
            0x52, 0x49, 0x46, 0x46,  # "RIFF"
            0x24, 0x08, 0x00, 0x00,  # File size - 8
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
            0x00, 0x08, 0x00, 0x00   # Data size
        ])
        
        # 1 segundo de silencio (16000 samples * 2 bytes)
        silence = bytes([0x00] * 32000)
        
        test_audio = wav_header + silence
        
        # Guardar archivo de prueba
        test_file = "test_whisper.wav"
        with open(test_file, "wb") as f:
            f.write(test_audio)
        
        self.log(f"✅ Audio de prueba creado: {test_file}")
        return test_file
    
    def test_whisper_transcription_local(self, audio_file):
        """Prueba la transcripción local de audio"""
        self.log("=== Probando Transcripción Local de Audio ===")
        
        try:
            with open(audio_file, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                data = {"language": "es"}
                
                response = self.session.post(
                    f"{WHISPER_MS_URL}/transcribe/local",
                    files=files,
                    data=data,
                    timeout=60
                )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Transcripción local exitosa: '{result['transcription']}'")
                self.log(f"   Método: {result['method']}")
                
                # Verificar campos requeridos
                required_fields = ["transcription", "language", "detected_language", "method"]
                missing_fields = [field for field in required_fields if field not in result]
                
                if missing_fields:
                    self.log(f"⚠️ Campos faltantes en respuesta: {missing_fields}")
                    self.test_results.append(("whisper_transcription_local", False, f"Missing fields: {missing_fields}"))
                    return False
                else:
                    self.test_results.append(("whisper_transcription_local", True, "OK"))
                    return True
            else:
                self.log(f"❌ Transcripción local falló: {response.status_code} - {response.text}")
                self.test_results.append(("whisper_transcription_local", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en transcripción local: {e}")
            self.test_results.append(("whisper_transcription_local", False, str(e)))
            return False
    
    def test_whisper_transcription_external(self, audio_file):
        """Prueba la transcripción externa de audio"""
        self.log("=== Probando Transcripción Externa de Audio ===")
        
        try:
            with open(audio_file, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                data = {"language": "es"}
                
                response = self.session.post(
                    f"{WHISPER_MS_URL}/transcribe/external",
                    files=files,
                    data=data,
                    timeout=60
                )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Transcripción externa exitosa: '{result['transcription']}'")
                self.log(f"   Método: {result['method']}")
                
                # Verificar si fue fallback
                if result.get("fallback", False):
                    self.log(f"   ⚠️ Fallback a local: {result.get('fallback_reason', 'Unknown')}")
                
                self.test_results.append(("whisper_transcription_external", True, "OK"))
                return True
            else:
                self.log(f"❌ Transcripción externa falló: {response.status_code} - {response.text}")
                self.test_results.append(("whisper_transcription_external", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en transcripción externa: {e}")
            self.test_results.append(("whisper_transcription_external", False, str(e)))
            return False
    
    def test_whisper_transcription_with_param(self, audio_file):
        """Prueba la transcripción con parámetro use_external"""
        self.log("=== Probando Transcripción con Parámetro use_external ===")
        
        try:
            with open(audio_file, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                data = {"language": "es", "use_external": "false"}
                
                response = self.session.post(
                    f"{WHISPER_MS_URL}/transcribe",
                    files=files,
                    data=data,
                    timeout=60
                )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Transcripción con parámetro exitosa: '{result['transcription']}'")
                self.log(f"   Método: {result['method']}")
                
                self.test_results.append(("whisper_transcription_with_param", True, "OK"))
                return True
            else:
                self.log(f"❌ Transcripción con parámetro falló: {response.status_code} - {response.text}")
                self.test_results.append(("whisper_transcription_with_param", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en transcripción con parámetro: {e}")
            self.test_results.append(("whisper_transcription_with_param", False, str(e)))
            return False
    
    def test_whisper_transcription(self, audio_file):
        """Prueba la transcripción de audio (compatibilidad hacia atrás)"""
        self.log("=== Probando Transcripción de Audio (Compatibilidad) ===")
        
        try:
            with open(audio_file, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                data = {"language": "es"}
                
                response = self.session.post(
                    f"{WHISPER_MS_URL}/transcribe",
                    files=files,
                    data=data,
                    timeout=60
                )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Transcripción exitosa: '{result['transcription']}'")
                self.log(f"   Método: {result['method']}")
                
                # Verificar campos requeridos
                required_fields = ["transcription", "language", "detected_language", "method"]
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
    
    def test_whisper_transcription_async(self, audio_file):
        """Prueba la transcripción asíncrona"""
        self.log("=== Probando Transcripción Asíncrona ===")
        
        try:
            with open(audio_file, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                data = {"language": "es"}
                
                response = self.session.post(
                    f"{INTENT_MANAGER_URL}/api/v1/whisper/transcribe/async",
                    files=files,
                    data=data,
                    timeout=30
                )
            
            if response.status_code == 202:  # Accepted
                result = response.json()
                self.log(f"✅ Transcripción asíncrona iniciada: {result}")
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
                self.log(f"✅ Test exitoso: {result}")
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
    
    def test_audio_processing_integration(self, audio_file):
        """Prueba la integración con el pipeline de procesamiento de audio"""
        self.log("=== Probando Integración con Pipeline de Audio ===")
        
        try:
            with open(audio_file, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                data = {
                    "language": "es",
                    "location": "Madrid",
                    "temperature": "22°C",
                    "device_id": "test_device"
                }
                
                response = self.session.post(
                    f"{INTENT_MANAGER_URL}/api/v1/audio/process",
                    files=files,
                    data=data,
                    timeout=120
                )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Integración exitosa: {result}")
                
                # Verificar que la transcripción se realizó
                if "transcription" in result and result["transcription"]:
                    self.log("✅ Transcripción integrada correctamente")
                    self.test_results.append(("audio_processing_integration", True, "OK"))
                    return True
                else:
                    self.log("⚠️ Transcripción no encontrada en resultado")
                    self.test_results.append(("audio_processing_integration", False, "No transcription"))
                    return False
            else:
                self.log(f"❌ Integración falló: {response.status_code} - {response.text}")
                self.test_results.append(("audio_processing_integration", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en integración: {e}")
            self.test_results.append(("audio_processing_integration", False, str(e)))
            return False
    
    def test_error_handling(self):
        """Prueba el manejo de errores"""
        self.log("=== Probando Manejo de Errores ===")
        
        # Prueba sin archivo de audio
        try:
            response = self.session.post(
                f"{WHISPER_MS_URL}/transcribe",
                data={"language": "es"},
                timeout=10
            )
            
            if response.status_code == 400:
                result = response.json()
                self.log(f"✅ Error sin archivo manejado correctamente: {result['error']}")
                self.test_results.append(("error_handling", True, "OK"))
                return True
            else:
                self.log(f"❌ Error sin archivo no manejado correctamente: {response.status_code}")
                self.test_results.append(("error_handling", False, f"HTTP {response.status_code}"))
                return False
        except Exception as e:
            self.log(f"❌ Error en prueba de manejo de errores: {e}")
            self.test_results.append(("error_handling", False, str(e)))
            return False
    
    def run_all_tests(self):
        """Ejecuta todas las pruebas"""
        self.log("🚀 Iniciando pruebas del servicio de transcripción de Whisper (T5.2) - Versión Mejorada")
        
        # Pruebas básicas
        self.test_whisper_ms_health()
        self.test_whisper_ms_status()
        self.test_intent_manager_whisper_health()
        self.test_whisper_stats()
        self.test_whisper_info()
        
        # Crear audio de prueba
        audio_file = self.create_test_audio()
        
        # Pruebas de transcripción mejoradas
        self.test_whisper_transcription_local(audio_file)
        self.test_whisper_transcription_external(audio_file)
        self.test_whisper_transcription_with_param(audio_file)
        self.test_whisper_transcription(audio_file)  # Compatibilidad hacia atrás
        self.test_whisper_transcription_async(audio_file)
        self.test_whisper_test_endpoint()
        
        # Prueba de integración
        self.test_audio_processing_integration(audio_file)
        
        # Prueba de manejo de errores
        self.test_error_handling()
        
        # Limpiar archivo de prueba
        try:
            os.remove(audio_file)
            self.log(f"🧹 Archivo de prueba eliminado: {audio_file}")
        except:
            pass
        
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
            self.log("\n📋 NUEVAS FUNCIONALIDADES VERIFICADAS:")
            self.log("   ✅ Endpoint /status para verificar configuración")
            self.log("   ✅ Endpoint /transcribe/local para transcripción local específica")
            self.log("   ✅ Endpoint /transcribe/external para transcripción externa")
            self.log("   ✅ Parámetro use_external en endpoint principal")
            self.log("   ✅ Sistema de fallback automático")
            self.log("   ✅ Compatibilidad hacia atrás mantenida")
            self.log("   ✅ Manejo de errores mejorado")
        else:
            self.log(f"\n⚠️ {failed_tests} pruebas fallaron. Revisar configuración.")

def main():
    """Función principal"""
    tester = WhisperTranscriptionTester()
    tester.run_all_tests()

if __name__ == "__main__":
    main()
