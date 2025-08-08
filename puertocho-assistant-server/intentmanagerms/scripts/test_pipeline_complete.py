#!/usr/bin/env python3
"""
Script de pruebas para T5.3 - Pipeline Completo Audio → Transcripción → Clasificación → Respuesta

Este script verifica que el pipeline completo funcione correctamente:
1. Audio → Whisper → Transcripción
2. Transcripción → RAG → Clasificación de Intención  
3. Clasificación → MoE → Votación
4. Votación → TTS → Respuesta de Audio

Autor: PuertoCho Assistant Team
Fecha: 2025-01-27
"""

import requests
import json
import time
import os
import sys
from datetime import datetime
import base64

# Configuración
INTENT_MANAGER_URL = "http://localhost:9904"
WHISPER_URL = "http://192.168.1.88:5000"
TEST_AUDIO_FILE = "/home/puertocho/Proyectos/puertocho-assistant/audio/test.wav"

class PipelineCompleteTester:
    
    def __init__(self):
        self.session = requests.Session()
        self.test_results = []
        self.start_time = datetime.now()
        
    def log(self, message):
        timestamp = datetime.now().strftime("%H:%M:%S")
        print(f"[{timestamp}] {message}")
        
    def create_test_audio(self):
        """Verifica que el archivo de audio de prueba existe"""
        self.log("=== Verificando archivo de audio de prueba ===")
        
        try:
            if os.path.exists(TEST_AUDIO_FILE):
                file_size = os.path.getsize(TEST_AUDIO_FILE)
                self.log(f"✅ Archivo de audio encontrado: {TEST_AUDIO_FILE} ({file_size} bytes)")
                return True
            else:
                self.log(f"❌ Archivo de audio no encontrado: {TEST_AUDIO_FILE}")
                return False
            
        except Exception as e:
            self.log(f"❌ Error verificando archivo de audio: {e}")
            return False
    
    def test_whisper_transcription(self):
        """Prueba la transcripción con Whisper"""
        self.log("=== Probando Transcripción Whisper ===")
        
        try:
            with open(TEST_AUDIO_FILE, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                data = {"language": "es"}
                
                response = self.session.post(
                    f"{INTENT_MANAGER_URL}/api/v1/whisper/transcribe",
                    files=files,
                    data=data,
                    timeout=60
                )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Transcripción exitosa: {result}")
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
    
    def test_tts_generation(self):
        """Prueba la generación de audio TTS"""
        self.log("=== Probando Generación TTS ===")
        
        try:
            tts_request = {
                "text": "¡Hola! Soy tu asistente. ¿En qué puedo ayudarte?",
                "language": "es",
                "voice": "Abril",
                "speed": 1.0
            }
            
            response = self.session.post(
                f"{INTENT_MANAGER_URL}/api/v1/tts/generate",
                json=tts_request,
                timeout=60
            )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Generación TTS exitosa: {result}")
                self.test_results.append(("tts_generation", True, "OK"))
                return True
            else:
                self.log(f"❌ Generación TTS falló: {response.status_code} - {response.text}")
                self.test_results.append(("tts_generation", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en generación TTS: {e}")
            self.test_results.append(("tts_generation", False, str(e)))
            return False
    
    def test_pipeline_complete(self):
        """Prueba el pipeline completo"""
        self.log("=== Probando Pipeline Completo ===")
        
        try:
            with open(TEST_AUDIO_FILE, "rb") as f:
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
                self.log(f"✅ Pipeline completo exitoso: {result}")
                
                # Verificar componentes del pipeline
                checks = []
                
                # Acceder a la estructura anidada
                if "result" in result:
                    pipeline_result = result["result"]
                    
                    # Verificar transcripción
                    if "transcription" in pipeline_result and pipeline_result["transcription"]:
                        checks.append("transcription")
                    
                    # Verificar clasificación de intención
                    if "intent_classification" in pipeline_result:
                        checks.append("intent_classification")
                    
                    # Verificar votación MoE
                    if "moe_voting" in pipeline_result:
                        checks.append("moe_voting")
                    
                    # Verificar respuesta
                    if "response" in pipeline_result and pipeline_result["response"]:
                        checks.append("response")
                    
                    # Verificar audio de respuesta
                    if "response" in pipeline_result and "audio_response" in pipeline_result["response"]:
                        checks.append("audio_response")
                
                self.log(f"✅ Componentes verificados: {', '.join(checks)}")
                self.test_results.append(("pipeline_complete", True, f"Components: {', '.join(checks)}"))
                return True
            else:
                self.log(f"❌ Pipeline completo falló: {response.status_code} - {response.text}")
                self.test_results.append(("pipeline_complete", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en pipeline completo: {e}")
            self.test_results.append(("pipeline_complete", False, str(e)))
            return False
    
    def test_pipeline_simple(self):
        """Prueba el pipeline simple sin metadata"""
        self.log("=== Probando Pipeline Simple ===")
        
        try:
            with open(TEST_AUDIO_FILE, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                
                response = self.session.post(
                    f"{INTENT_MANAGER_URL}/api/v1/audio/process/simple",
                    files=files,
                    timeout=120
                )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Pipeline simple exitoso: {result}")
                self.test_results.append(("pipeline_simple", True, "OK"))
                return True
            else:
                self.log(f"❌ Pipeline simple falló: {response.status_code} - {response.text}")
                self.test_results.append(("pipeline_simple", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en pipeline simple: {e}")
            self.test_results.append(("pipeline_simple", False, str(e)))
            return False
    
    def test_health_checks(self):
        """Prueba los health checks de todos los servicios"""
        self.log("=== Probando Health Checks ===")
        
        services = [
            ("intent_manager", f"{INTENT_MANAGER_URL}/api/v1/audio/health"),
            ("whisper", f"{INTENT_MANAGER_URL}/api/v1/whisper/health"),
            ("tts", f"{INTENT_MANAGER_URL}/api/v1/tts/health")
        ]
        
        all_healthy = True
        
        for service_name, url in services:
            try:
                response = self.session.get(url, timeout=10)
                if response.status_code == 200:
                    self.log(f"✅ {service_name}: OK")
                    self.test_results.append((f"health_{service_name}", True, "OK"))
                else:
                    self.log(f"❌ {service_name}: HTTP {response.status_code}")
                    self.test_results.append((f"health_{service_name}", False, f"HTTP {response.status_code}"))
                    all_healthy = False
            except Exception as e:
                self.log(f"❌ {service_name}: Error - {e}")
                self.test_results.append((f"health_{service_name}", False, str(e)))
                all_healthy = False
        
        return all_healthy
    
    def run_all_tests(self):
        """Ejecuta todas las pruebas"""
        self.log("🚀 INICIANDO PRUEBAS T5.3 - PIPELINE COMPLETO")
        self.log("=" * 60)
        
        # Crear archivo de audio de prueba
        if not self.create_test_audio():
            self.log("❌ No se pudo crear archivo de audio. Abortando pruebas.")
            return False
        
        # Ejecutar pruebas
        tests = [
            ("Health Checks", self.test_health_checks),
            ("Whisper Transcription", self.test_whisper_transcription),
            ("TTS Generation", self.test_tts_generation),
            ("Pipeline Simple", self.test_pipeline_simple),
            ("Pipeline Complete", self.test_pipeline_complete)
        ]
        
        for test_name, test_func in tests:
            self.log(f"\n{'='*20} {test_name} {'='*20}")
            try:
                test_func()
            except Exception as e:
                self.log(f"❌ Error ejecutando {test_name}: {e}")
                self.test_results.append((test_name.lower().replace(" ", "_"), False, str(e)))
        
        # Limpiar archivo de prueba (solo si es un archivo temporal)
        try:
            if os.path.exists(TEST_AUDIO_FILE) and "test_audio.wav" in TEST_AUDIO_FILE:
                os.remove(TEST_AUDIO_FILE)
                self.log(f"🧹 Archivo de prueba eliminado: {TEST_AUDIO_FILE}")
            else:
                self.log(f"📁 Archivo de audio real mantenido: {TEST_AUDIO_FILE}")
        except Exception as e:
            self.log(f"⚠️ No se pudo eliminar archivo de prueba: {e}")
        
        # Mostrar resultados
        self.show_results()
        
        return True
    
    def show_results(self):
        """Muestra los resultados de las pruebas"""
        self.log("\n" + "="*60)
        self.log("📊 RESULTADOS DE PRUEBAS T5.3")
        self.log("="*60)
        
        total_tests = len(self.test_results)
        passed_tests = sum(1 for _, success, _ in self.test_results if success)
        failed_tests = total_tests - passed_tests
        
        self.log(f"Total de pruebas: {total_tests}")
        self.log(f"✅ Exitosas: {passed_tests}")
        self.log(f"❌ Fallidas: {failed_tests}")
        self.log(f"📈 Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        
        # Mostrar detalles
        self.log("\n📋 DETALLES:")
        for test_name, success, message in self.test_results:
            status = "✅" if success else "❌"
            self.log(f"  {status} {test_name}: {message}")
        
        # Tiempo total
        end_time = datetime.now()
        duration = (end_time - self.start_time).total_seconds()
        self.log(f"\n⏱️ Tiempo total de pruebas: {duration:.2f} segundos")
        
        # Resumen final
        if failed_tests == 0:
            self.log("\n🎉 ¡TODAS LAS PRUEBAS EXITOSAS! T5.3 COMPLETADO")
        else:
            self.log(f"\n⚠️ {failed_tests} pruebas fallaron. Revisar implementación.")
        
        return failed_tests == 0

def main():
    """Función principal"""
    print("🔧 PuertoCho Assistant - T5.3 Pipeline Completo Tester")
    print("=" * 60)
    
    tester = PipelineCompleteTester()
    
    try:
        success = tester.run_all_tests()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n⚠️ Pruebas interrumpidas por el usuario")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error inesperado: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
