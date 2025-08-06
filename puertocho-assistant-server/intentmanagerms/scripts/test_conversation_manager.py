#!/usr/bin/env python3
"""
Script de prueba para el ConversationManager del Epic 4 - T4.1
Verifica la funcionalidad completa del gestor de conversaciones.
"""

import requests
import json
import time
import uuid
from typing import Dict, Any, Optional
import sys

class ConversationManagerTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.session_id = f"test-session-{uuid.uuid4()}"
        self.user_id = f"test-user-{uuid.uuid4()}"
        self.test_results = []
        
    def log_test(self, test_name: str, success: bool, details: str = ""):
        """Registra el resultado de una prueba"""
        status = "✅ PASÓ" if success else "❌ FALLÓ"
        print(f"{status} {test_name}")
        if details:
            print(f"   Detalles: {details}")
        print()
        
        self.test_results.append({
            "test_name": test_name,
            "success": success,
            "details": details
        })
        
    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        """Espera a que el servicio esté disponible"""
        print("🔄 Esperando a que el servicio esté disponible...")
        
        for attempt in range(max_attempts):
            try:
                response = requests.get(f"{self.base_url}/api/v1/conversation/health", timeout=5)
                if response.status_code == 200:
                    print("✅ Servicio disponible")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            if attempt < max_attempts - 1:
                print(f"   Intento {attempt + 1}/{max_attempts} - Esperando {delay}s...")
                time.sleep(delay)
        
        print("❌ Servicio no disponible después de múltiples intentos")
        return False
        
    def test_health_check(self) -> bool:
        """Prueba el endpoint de health check"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/conversation/health", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                is_healthy = data.get("status") == "healthy"
                
                self.log_test("Health Check", is_healthy, 
                             f"Status: {data.get('status')}, Service: {data.get('service')}")
                return is_healthy
            else:
                self.log_test("Health Check", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Health Check", False, f"Error: {str(e)}")
            return False
            
    def test_statistics(self) -> bool:
        """Prueba el endpoint de estadísticas"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/conversation/statistics", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                has_required_fields = all(key in data for key in [
                    "active_sessions", "total_sessions_created", "total_turns_processed"
                ])
                
                self.log_test("Statistics", has_required_fields, 
                             f"Active sessions: {data.get('active_sessions')}, "
                             f"Total created: {data.get('total_sessions_created')}")
                return has_required_fields
            else:
                self.log_test("Statistics", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Statistics", False, f"Error: {str(e)}")
            return False
            
    def test_create_session(self) -> bool:
        """Prueba la creación de una sesión"""
        try:
            response = requests.post(
                f"{self.base_url}/api/v1/conversation/session",
                params={"userId": self.user_id, "sessionId": self.session_id},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                session_created = (data.get("session_id") == self.session_id and 
                                 data.get("user_id") == self.user_id and
                                 data.get("state") == "ACTIVE")
                
                self.log_test("Create Session", session_created,
                             f"Session ID: {data.get('session_id')}, State: {data.get('state')}")
                return session_created
            else:
                self.log_test("Create Session", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Create Session", False, f"Error: {str(e)}")
            return False
            
    def test_get_session(self) -> bool:
        """Prueba la obtención de una sesión"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/conversation/session/{self.session_id}", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                session_found = (data.get("session_id") == self.session_id and 
                               data.get("user_id") == self.user_id)
                
                self.log_test("Get Session", session_found,
                             f"Session ID: {data.get('session_id')}, Turn count: {data.get('turn_count')}")
                return session_found
            else:
                self.log_test("Get Session", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Get Session", False, f"Error: {str(e)}")
            return False
            
    def test_process_message_simple(self) -> bool:
        """Prueba el procesamiento de un mensaje simple"""
        try:
            request_data = {
                "sessionId": self.session_id,
                "userId": self.user_id,
                "userMessage": "¿Qué tiempo hace en Madrid?"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request_data,
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                message_processed = (data.get("success") == True and
                                   data.get("sessionId") == self.session_id and
                                   data.get("systemResponse") is not None)
                
                self.log_test("Process Message (Simple)", message_processed,
                             f"Intent: {data.get('detectedIntent')}, "
                             f"Confidence: {data.get('confidenceScore')}, "
                             f"Response: {data.get('systemResponse')[:50]}...")
                return message_processed
            else:
                self.log_test("Process Message (Simple)", False, 
                             f"Status code: {response.status_code}, Response: {response.text}")
                return False
                
        except Exception as e:
            self.log_test("Process Message (Simple)", False, f"Error: {str(e)}")
            return False
            
    def test_process_message_complex(self) -> bool:
        """Prueba el procesamiento de un mensaje complejo"""
        try:
            request_data = {
                "sessionId": self.session_id,
                "userId": self.user_id,
                "userMessage": "Enciende la luz del salón y ajusta la temperatura a 22 grados"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request_data,
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                message_processed = (data.get("success") == True and
                                   data.get("sessionId") == self.session_id and
                                   data.get("systemResponse") is not None)
                
                self.log_test("Process Message (Complex)", message_processed,
                             f"Intent: {data.get('detectedIntent')}, "
                             f"Confidence: {data.get('confidenceScore')}, "
                             f"Response: {data.get('systemResponse')[:50]}...")
                return message_processed
            else:
                self.log_test("Process Message (Complex)", False, 
                             f"Status code: {response.status_code}, Response: {response.text}")
                return False
                
        except Exception as e:
            self.log_test("Process Message (Complex)", False, f"Error: {str(e)}")
            return False
            
    def test_conversation_flow(self) -> bool:
        """Prueba un flujo completo de conversación"""
        try:
            # Primer mensaje
            request1 = {
                "sessionId": self.session_id,
                "userId": self.user_id,
                "userMessage": "¿Qué tiempo hace?"
            }
            
            response1 = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request1,
                timeout=30
            )
            
            if response1.status_code != 200:
                self.log_test("Conversation Flow", False, "Error en primer mensaje")
                return False
                
            # Segundo mensaje (continuación)
            request2 = {
                "sessionId": self.session_id,
                "userId": self.user_id,
                "userMessage": "En Madrid"
            }
            
            response2 = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request2,
                timeout=30
            )
            
            if response2.status_code == 200:
                data2 = response2.json()
                flow_successful = (data2.get("success") == True and
                                 data2.get("turnCount") > 1)
                
                self.log_test("Conversation Flow", flow_successful,
                             f"Turn count: {data2.get('turnCount')}, "
                             f"Intent: {data2.get('detectedIntent')}")
                return flow_successful
            else:
                self.log_test("Conversation Flow", False, "Error en segundo mensaje")
                return False
                
        except Exception as e:
            self.log_test("Conversation Flow", False, f"Error: {str(e)}")
            return False
            
    def test_session_management(self) -> bool:
        """Prueba la gestión de sesiones"""
        try:
            # Crear nueva sesión
            new_session_id = f"test-session-mgmt-{uuid.uuid4()}"
            create_response = requests.post(
                f"{self.base_url}/api/v1/conversation/session",
                params={"userId": self.user_id, "sessionId": new_session_id},
                timeout=10
            )
            
            if create_response.status_code != 200:
                self.log_test("Session Management", False, "Error creando sesión")
                return False
                
            # Procesar mensaje en nueva sesión
            request_data = {
                "sessionId": new_session_id,
                "userId": self.user_id,
                "userMessage": "Hola, esta es una nueva sesión"
            }
            
            process_response = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request_data,
                timeout=30
            )
            
            if process_response.status_code != 200:
                self.log_test("Session Management", False, "Error procesando mensaje en nueva sesión")
                return False
                
            # Finalizar sesión
            end_response = requests.delete(
                f"{self.base_url}/api/v1/conversation/session/{new_session_id}",
                timeout=10
            )
            
            if end_response.status_code == 200:
                self.log_test("Session Management", True,
                             f"Session created, message processed, and session ended successfully")
                return True
            else:
                self.log_test("Session Management", False, "Error finalizando sesión")
                return False
                
        except Exception as e:
            self.log_test("Session Management", False, f"Error: {str(e)}")
            return False
            
    def test_error_handling(self) -> bool:
        """Prueba el manejo de errores"""
        try:
            # Procesar mensaje con sesión inexistente
            request_data = {
                "sessionId": "non-existent-session",
                "userId": self.user_id,
                "userMessage": "Este mensaje debería fallar"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request_data,
                timeout=30
            )
            
            # Debería crear una nueva sesión automáticamente
            if response.status_code == 200:
                data = response.json()
                error_handled = data.get("success") == True
                
                self.log_test("Error Handling", error_handled,
                             "Non-existent session handled gracefully")
                return error_handled
            else:
                self.log_test("Error Handling", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Error Handling", False, f"Error: {str(e)}")
            return False
            
    def test_cleanup_functionality(self) -> bool:
        """Prueba la funcionalidad de limpieza"""
        try:
            response = requests.post(f"{self.base_url}/api/v1/conversation/cleanup", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                cleanup_successful = data.get("success") == True
                
                self.log_test("Cleanup Functionality", cleanup_successful,
                             "Cleanup operation completed")
                return cleanup_successful
            else:
                self.log_test("Cleanup Functionality", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Cleanup Functionality", False, f"Error: {str(e)}")
            return False
            
    def test_end_to_end(self) -> bool:
        """Prueba completa de extremo a extremo"""
        try:
            # Crear sesión
            session_response = requests.post(
                f"{self.base_url}/api/v1/conversation/session",
                params={"userId": self.user_id, "sessionId": self.session_id},
                timeout=10
            )
            
            if session_response.status_code != 200:
                self.log_test("End-to-End Test", False, "Error creando sesión")
                return False
                
            # Procesar múltiples mensajes
            messages = [
                "¿Qué tiempo hace en Barcelona?",
                "Y en Madrid?",
                "Gracias por la información"
            ]
            
            for i, message in enumerate(messages):
                request_data = {
                    "sessionId": self.session_id,
                    "userId": self.user_id,
                    "userMessage": message
                }
                
                response = requests.post(
                    f"{self.base_url}/api/v1/conversation/process",
                    json=request_data,
                    timeout=30
                )
                
                if response.status_code != 200:
                    self.log_test("End-to-End Test", False, f"Error en mensaje {i+1}")
                    return False
                    
                time.sleep(1)  # Pequeña pausa entre mensajes
                
            # Verificar estadísticas finales
            stats_response = requests.get(f"{self.base_url}/api/v1/conversation/statistics", timeout=10)
            
            if stats_response.status_code == 200:
                stats = stats_response.json()
                e2e_successful = (stats.get("total_turns_processed", 0) > 0 and
                                stats.get("active_sessions", 0) > 0)
                
                self.log_test("End-to-End Test", e2e_successful,
                             f"Total turns: {stats.get('total_turns_processed')}, "
                             f"Active sessions: {stats.get('active_sessions')}")
                return e2e_successful
            else:
                self.log_test("End-to-End Test", False, "Error obteniendo estadísticas finales")
                return False
                
        except Exception as e:
            self.log_test("End-to-End Test", False, f"Error: {str(e)}")
            return False
            
    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas"""
        print("🚀 INICIANDO PRUEBAS DEL CONVERSATION MANAGER (T4.1)")
        print("=" * 60)
        
        # Verificar disponibilidad del servicio
        if not self.wait_for_service():
            return False
            
        print("\n📋 EJECUTANDO PRUEBAS:")
        print("-" * 40)
        
        # Ejecutar todas las pruebas
        tests = [
            ("Health Check", self.test_health_check),
            ("Statistics", self.test_statistics),
            ("Create Session", self.test_create_session),
            ("Get Session", self.test_get_session),
            ("Process Message (Simple)", self.test_process_message_simple),
            ("Process Message (Complex)", self.test_process_message_complex),
            ("Conversation Flow", self.test_conversation_flow),
            ("Session Management", self.test_session_management),
            ("Error Handling", self.test_error_handling),
            ("Cleanup Functionality", self.test_cleanup_functionality),
            ("End-to-End Test", self.test_end_to_end)
        ]
        
        passed_tests = 0
        total_tests = len(tests)
        
        for test_name, test_func in tests:
            try:
                if test_func():
                    passed_tests += 1
            except Exception as e:
                self.log_test(test_name, False, f"Excepción: {str(e)}")
                
        # Resumen final
        print("\n📊 RESUMEN DE PRUEBAS:")
        print("=" * 40)
        print(f"✅ Pruebas pasadas: {passed_tests}/{total_tests}")
        print(f"❌ Pruebas fallidas: {total_tests - passed_tests}/{total_tests}")
        print(f"📈 Porcentaje de éxito: {(passed_tests/total_tests)*100:.1f}%")
        
        success = passed_tests == total_tests
        
        if success:
            print("\n🎉 ¡TODAS LAS PRUEBAS PASARON! ConversationManager está funcionando correctamente.")
        else:
            print("\n⚠️  Algunas pruebas fallaron. Revisar los detalles arriba.")
            
        return success
        
    def get_detailed_results(self) -> Dict[str, Any]:
        """Obtiene resultados detallados de las pruebas"""
        passed_tests = sum(1 for result in self.test_results if result["success"])
        total_tests = len(self.test_results)
        
        return {
            "total_tests": total_tests,
            "passed_tests": passed_tests,
            "failed_tests": total_tests - passed_tests,
            "success_rate": (passed_tests / total_tests * 100) if total_tests > 0 else 0,
            "test_results": self.test_results,
            "session_id": self.session_id,
            "user_id": self.user_id,
            "base_url": self.base_url
        }

def main():
    """Función principal"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Prueba el ConversationManager (T4.1)")
    parser.add_argument("--base-url", default="http://localhost:9904", 
                       help="URL base del servicio (default: http://localhost:9904)")
    parser.add_argument("--verbose", "-v", action="store_true", 
                       help="Modo verbose")
    
    args = parser.parse_args()
    
    # Crear tester y ejecutar pruebas
    tester = ConversationManagerTester(args.base_url)
    
    try:
        success = tester.run_complete_test_suite()
        
        if args.verbose:
            print("\n📋 RESULTADOS DETALLADOS:")
            print(json.dumps(tester.get_detailed_results(), indent=2))
        
        # Código de salida
        sys.exit(0 if success else 1)
        
    except KeyboardInterrupt:
        print("\n⚠️  Pruebas interrumpidas por el usuario")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error inesperado: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main() 