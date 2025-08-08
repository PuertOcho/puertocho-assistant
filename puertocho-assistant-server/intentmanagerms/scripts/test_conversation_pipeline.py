#!/usr/bin/env python3
"""
Script de pruebas para ConversationManagerController - Pipeline Conversacional Completo

Este script verifica que el sistema de conversación funcione correctamente:
1. Crear sesión de conversación
2. Procesar mensaje de texto
3. Procesar mensaje de audio
4. Mantener estado de conversación
5. Generar respuestas contextuales

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
TEST_AUDIO_FILE = "/home/puertocho/Proyectos/puertocho-assistant/audio/test.wav"

class ConversationPipelineTester:
    
    def __init__(self):
        self.session = requests.Session()
        self.test_results = []
        self.start_time = datetime.now()
        self.test_session_id = None
        self.test_user_id = None
        
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
    
    def test_create_conversation_session(self):
        """Prueba la creación de una sesión de conversación"""
        self.log("=== Probando Creación de Sesión de Conversación ===")
        
        try:
            self.test_user_id = f"test-user-{int(time.time())}"
            
            response = self.session.post(
                f"{INTENT_MANAGER_URL}/api/v1/conversation/session",
                params={
                    "userId": self.test_user_id
                },
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                self.test_session_id = result.get("session_id")
                self.log(f"✅ Sesión creada exitosamente: {self.test_session_id}")
                self.test_results.append(("create_session", True, f"Session: {self.test_session_id}"))
                return True
            else:
                self.log(f"❌ Error creando sesión: {response.status_code} - {response.text}")
                self.test_results.append(("create_session", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en creación de sesión: {e}")
            self.test_results.append(("create_session", False, str(e)))
            return False
    
    def test_text_conversation(self):
        """Prueba la conversación con texto"""
        self.log("=== Probando Conversación con Texto ===")
        
        try:
            if not self.test_session_id:
                self.log("❌ No hay sesión de prueba disponible")
                return False
            
            conversation_request = {
                "sessionId": self.test_session_id,
                "userId": self.test_user_id,
                "userMessage": "¿Qué tiempo hace en Madrid?"
            }
            
            response = self.session.post(
                f"{INTENT_MANAGER_URL}/api/v1/conversation/process",
                json=conversation_request,
                timeout=60
            )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Conversación de texto exitosa: {result}")
                
                # Verificar componentes de la respuesta
                checks = []
                if result.get("success"):
                    checks.append("success")
                if result.get("systemResponse"):
                    checks.append("system_response")
                if result.get("detectedIntent"):
                    checks.append("detected_intent")
                if result.get("confidenceScore") is not None:
                    checks.append("confidence_score")
                if result.get("sessionState"):
                    checks.append("session_state")
                
                self.log(f"✅ Componentes verificados: {', '.join(checks)}")
                self.test_results.append(("text_conversation", True, f"Components: {', '.join(checks)}"))
                return True
            else:
                self.log(f"❌ Error en conversación de texto: {response.status_code} - {response.text}")
                self.test_results.append(("text_conversation", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en conversación de texto: {e}")
            self.test_results.append(("text_conversation", False, str(e)))
            return False
    
    def test_audio_conversation(self):
        """Prueba la conversación con audio"""
        self.log("=== Probando Conversación con Audio ===")
        
        try:
            if not self.test_session_id:
                self.log("❌ No hay sesión de prueba disponible")
                return False
            
            with open(TEST_AUDIO_FILE, "rb") as f:
                files = {"audio": ("test.wav", f, "audio/wav")}
                data = {
                    "sessionId": self.test_session_id,
                    "userId": self.test_user_id,
                    "language": "es",
                    "generateAudioResponse": "true"
                }
                
                response = self.session.post(
                    f"{INTENT_MANAGER_URL}/api/v1/conversation/process/audio",
                    files=files,
                    data=data,
                    timeout=120
                )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Conversación de audio exitosa: {result}")
                
                # Verificar componentes de la respuesta
                checks = []
                if result.get("success"):
                    checks.append("success")
                if result.get("transcribedText"):
                    checks.append("transcribed_text")
                if result.get("systemResponse"):
                    checks.append("system_response")
                if result.get("detectedIntent"):
                    checks.append("detected_intent")
                if result.get("confidenceScore") is not None:
                    checks.append("confidence_score")
                if result.get("whisperConfidence") is not None:
                    checks.append("whisper_confidence")
                if result.get("audioResponseGenerated"):
                    checks.append("audio_response_generated")
                
                self.log(f"✅ Componentes verificados: {', '.join(checks)}")
                self.test_results.append(("audio_conversation", True, f"Components: {', '.join(checks)}"))
                return True
            else:
                self.log(f"❌ Error en conversación de audio: {response.status_code} - {response.text}")
                self.test_results.append(("audio_conversation", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en conversación de audio: {e}")
            self.test_results.append(("audio_conversation", False, str(e)))
            return False
    
    def test_conversation_state_persistence(self):
        """Prueba que el estado de la conversación se mantenga"""
        self.log("=== Probando Persistencia del Estado de Conversación ===")
        
        try:
            if not self.test_session_id:
                self.log("❌ No hay sesión de prueba disponible")
                return False
            
            # Obtener la sesión actual
            response = self.session.get(
                f"{INTENT_MANAGER_URL}/api/v1/conversation/session/{self.test_session_id}",
                timeout=30
            )
            
            if response.status_code == 200:
                session_data = response.json()
                self.log(f"✅ Estado de sesión obtenido: {session_data}")
                
                # Verificar que la sesión tiene información de conversación
                checks = []
                if session_data.get("sessionId"):
                    checks.append("session_id")
                if session_data.get("turnCount") is not None:
                    checks.append("turn_count")
                if session_data.get("state"):
                    checks.append("state")
                if session_data.get("conversationHistory"):
                    checks.append("conversation_history")
                
                self.log(f"✅ Componentes de estado verificados: {', '.join(checks)}")
                self.test_results.append(("state_persistence", True, f"Components: {', '.join(checks)}"))
                return True
            else:
                self.log(f"❌ Error obteniendo estado de sesión: {response.status_code} - {response.text}")
                self.test_results.append(("state_persistence", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en persistencia de estado: {e}")
            self.test_results.append(("state_persistence", False, str(e)))
            return False
    
    def test_conversation_continuity(self):
        """Prueba la continuidad de la conversación con múltiples mensajes"""
        self.log("=== Probando Continuidad de Conversación ===")
        
        try:
            if not self.test_session_id:
                self.log("❌ No hay sesión de prueba disponible")
                return False
            
            # Segundo mensaje de texto
            conversation_request = {
                "sessionId": self.test_session_id,
                "userId": self.test_user_id,
                "userMessage": "¿Y en Barcelona?"
            }
            
            response = self.session.post(
                f"{INTENT_MANAGER_URL}/api/v1/conversation/process",
                json=conversation_request,
                timeout=60
            )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Continuidad de conversación exitosa: {result}")
                
                # Verificar que el turnCount ha aumentado
                if result.get("turnCount", 0) > 1:
                    self.log(f"✅ Turno incrementado correctamente: {result.get('turnCount')}")
                    self.test_results.append(("conversation_continuity", True, f"Turn: {result.get('turnCount')}"))
                    return True
                else:
                    self.log(f"❌ Turno no incrementado: {result.get('turnCount')}")
                    self.test_results.append(("conversation_continuity", False, "Turn not incremented"))
                    return False
            else:
                self.log(f"❌ Error en continuidad de conversación: {response.status_code} - {response.text}")
                self.test_results.append(("conversation_continuity", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error en continuidad de conversación: {e}")
            self.test_results.append(("conversation_continuity", False, str(e)))
            return False
    
    def test_end_conversation_session(self):
        """Prueba el finalizar la sesión de conversación"""
        self.log("=== Probando Finalización de Sesión ===")
        
        try:
            if not self.test_session_id:
                self.log("❌ No hay sesión de prueba disponible")
                return False
            
            response = self.session.delete(
                f"{INTENT_MANAGER_URL}/api/v1/conversation/session/{self.test_session_id}",
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                self.log(f"✅ Sesión finalizada exitosamente: {result}")
                self.test_results.append(("end_session", True, "OK"))
                return True
            else:
                self.log(f"❌ Error finalizando sesión: {response.status_code} - {response.text}")
                self.test_results.append(("end_session", False, f"HTTP {response.status_code}"))
                return False
                
        except Exception as e:
            self.log(f"❌ Error finalizando sesión: {e}")
            self.test_results.append(("end_session", False, str(e)))
            return False
    
    def test_health_checks(self):
        """Prueba los health checks de los servicios de conversación"""
        self.log("=== Probando Health Checks ===")
        
        services = [
            ("conversation_manager", f"{INTENT_MANAGER_URL}/api/v1/conversation/health"),
            ("conversation_memory", f"{INTENT_MANAGER_URL}/api/v1/conversation-memory/health"),
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
        self.log("🚀 INICIANDO PRUEBAS - PIPELINE CONVERSACIONAL COMPLETO")
        self.log("=" * 60)
        
        # Crear archivo de audio de prueba
        if not self.create_test_audio():
            self.log("❌ No se pudo verificar archivo de audio. Abortando pruebas.")
            return False
        
        # Ejecutar pruebas
        tests = [
            ("Health Checks", self.test_health_checks),
            ("Create Conversation Session", self.test_create_conversation_session),
            ("Text Conversation", self.test_text_conversation),
            ("Audio Conversation", self.test_audio_conversation),
            ("State Persistence", self.test_conversation_state_persistence),
            ("Conversation Continuity", self.test_conversation_continuity),
            ("End Conversation Session", self.test_end_conversation_session)
        ]
        
        for test_name, test_func in tests:
            self.log(f"\n{'='*20} {test_name} {'='*20}")
            try:
                test_func()
            except Exception as e:
                self.log(f"❌ Error ejecutando {test_name}: {e}")
                self.test_results.append((test_name.lower().replace(" ", "_"), False, str(e)))
        
        # Mostrar resultados
        self.show_results()
        
        return True
    
    def show_results(self):
        """Muestra los resultados de las pruebas"""
        self.log("\n" + "="*60)
        self.log("📊 RESULTADOS DE PRUEBAS - PIPELINE CONVERSACIONAL")
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
            self.log("\n🎉 ¡TODAS LAS PRUEBAS EXITOSAS! PIPELINE CONVERSACIONAL OPERATIVO")
        else:
            self.log(f"\n⚠️ {failed_tests} pruebas fallaron. Revisar implementación.")
        
        return failed_tests == 0

def main():
    """Función principal"""
    print("🔧 PuertoCho Assistant - Pipeline Conversacional Tester")
    print("=" * 60)
    
    tester = ConversationPipelineTester()
    
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
