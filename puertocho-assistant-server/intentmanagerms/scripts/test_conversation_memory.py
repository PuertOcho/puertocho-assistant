#!/usr/bin/env python3
"""
Script de pruebas automatizadas para el sistema de memoria conversacional.
Valida todas las funcionalidades de T4.4 implementadas.

Funcionalidades probadas:
- RedisConversationRepository
- ConversationMemoryService  
- MemoryManager
- ContextPersistenceService
- ConversationMemoryController
"""

import requests
import json
import time
import uuid
from typing import Dict, Any, List
from datetime import datetime

class ConversationMemoryTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.test_results = []
        self.session_ids = []
        self.user_ids = []
        
    def log_test(self, test_name: str, success: bool, details: str = ""):
        """Registra el resultado de una prueba."""
        result = {
            "test_name": test_name,
            "success": success,
            "details": details,
            "timestamp": datetime.now().isoformat()
        }
        self.test_results.append(result)
        
        status = "✅ PASÓ" if success else "❌ FALLÓ"
        print(f"{status} {test_name}")
        if details:
            print(f"   {details}")
        print()

    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        """Espera a que el servicio esté disponible."""
        print("🔄 Esperando a que el servicio esté disponible...")
        
        for attempt in range(max_attempts):
            try:
                response = requests.get(f"{self.base_url}/api/v1/conversation-memory/health", timeout=5)
                if response.status_code == 200:
                    print("✅ Servicio disponible")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            if attempt < max_attempts - 1:
                time.sleep(delay)
        
        print("❌ Servicio no disponible después de múltiples intentos")
        return False

    def test_health_check(self) -> bool:
        """Prueba el health check del sistema de memoria conversacional."""
        try:
            response = requests.get(f"{self.base_url}/api/v1/conversation-memory/health", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                memory_service_healthy = data.get("memory_service_healthy", False)
                memory_manager_healthy = data.get("memory_manager_healthy", False)
                overall_healthy = data.get("overall_healthy", False)
                
                details = f"MemoryService: {memory_service_healthy}, MemoryManager: {memory_manager_healthy}, Overall: {overall_healthy}"
                
                self.log_test("Health Check", overall_healthy, details)
                return overall_healthy
            else:
                self.log_test("Health Check", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Health Check", False, f"Error: {str(e)}")
            return False

    def test_statistics(self) -> bool:
        """Prueba la obtención de estadísticas."""
        try:
            response = requests.get(f"{self.base_url}/api/v1/conversation-memory/statistics", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                has_memory_service = "memory_service" in data
                has_memory_manager = "memory_manager" in data
                has_context_persistence = "context_persistence" in data
                
                details = f"MemoryService: {has_memory_service}, MemoryManager: {has_memory_manager}, ContextPersistence: {has_context_persistence}"
                
                self.log_test("Statistics", has_memory_service and has_memory_manager and has_context_persistence, details)
                return True
            else:
                self.log_test("Statistics", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Statistics", False, f"Error: {str(e)}")
            return False

    def test_create_session(self) -> bool:
        """Prueba la creación de sesiones."""
        try:
            user_id = f"test_user_{uuid.uuid4().hex[:8]}"
            self.user_ids.append(user_id)
            
            request_data = {"user_id": user_id}
            response = requests.post(f"{self.base_url}/api/v1/conversation-memory/session", 
                                   json=request_data, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                session_id = data.get("session_id")
                success = data.get("success", False)
                
                if session_id and success:
                    self.session_ids.append(session_id)
                    details = f"Session ID: {session_id}, User ID: {user_id}"
                    self.log_test("Create Session", True, details)
                    return True
                else:
                    self.log_test("Create Session", False, "No session_id or success=false")
                    return False
            else:
                self.log_test("Create Session", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Create Session", False, f"Error: {str(e)}")
            return False

    def test_get_session(self) -> bool:
        """Prueba la obtención de sesiones."""
        if not self.session_ids:
            self.log_test("Get Session", False, "No session IDs available")
            return False
            
        try:
            session_id = self.session_ids[0]
            response = requests.get(f"{self.base_url}/api/v1/conversation-memory/session/{session_id}", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                retrieved_session_id = data.get("session_id")
                success = data.get("success", False)
                
                if retrieved_session_id == session_id and success:
                    details = f"Retrieved session: {session_id}"
                    self.log_test("Get Session", True, details)
                    return True
                else:
                    self.log_test("Get Session", False, "Session ID mismatch or success=false")
                    return False
            else:
                self.log_test("Get Session", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Get Session", False, f"Error: {str(e)}")
            return False

    def test_get_user_sessions(self) -> bool:
        """Prueba la obtención de sesiones por usuario."""
        if not self.user_ids:
            self.log_test("Get User Sessions", False, "No user IDs available")
            return False
            
        try:
            user_id = self.user_ids[0]
            response = requests.get(f"{self.base_url}/api/v1/conversation-memory/user/{user_id}/sessions", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                sessions_count = data.get("sessions_count", 0)
                success = data.get("success", False)
                
                details = f"User: {user_id}, Sessions: {sessions_count}"
                self.log_test("Get User Sessions", success, details)
                return success
            else:
                self.log_test("Get User Sessions", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Get User Sessions", False, f"Error: {str(e)}")
            return False

    def test_get_all_active_sessions(self) -> bool:
        """Prueba la obtención de todas las sesiones activas."""
        try:
            response = requests.get(f"{self.base_url}/api/v1/conversation-memory/sessions/active", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                sessions_count = data.get("sessions_count", 0)
                success = data.get("success", False)
                
                details = f"Active sessions: {sessions_count}"
                self.log_test("Get All Active Sessions", success, details)
                return success
            else:
                self.log_test("Get All Active Sessions", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Get All Active Sessions", False, f"Error: {str(e)}")
            return False

    def test_search_sessions(self) -> bool:
        """Prueba la búsqueda de sesiones."""
        try:
            criteria = {"is_active": True}
            response = requests.post(f"{self.base_url}/api/v1/conversation-memory/sessions/search", 
                                   json=criteria, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                sessions_count = data.get("sessions_count", 0)
                success = data.get("success", False)
                
                details = f"Found {sessions_count} active sessions"
                self.log_test("Search Sessions", success, details)
                return success
            else:
                self.log_test("Search Sessions", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Search Sessions", False, f"Error: {str(e)}")
            return False

    def test_compress_context(self) -> bool:
        """Prueba la compresión de contexto."""
        if not self.session_ids:
            self.log_test("Compress Context", False, "No session IDs available")
            return False
            
        try:
            session_id = self.session_ids[0]
            response = requests.post(f"{self.base_url}/api/v1/conversation-memory/session/{session_id}/compress-context", 
                                   timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                action = data.get("action")
                success = data.get("success", False)
                
                details = f"Action: {action}"
                self.log_test("Compress Context", success, details)
                return success
            else:
                self.log_test("Compress Context", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Compress Context", False, f"Error: {str(e)}")
            return False

    def test_optimize_memory(self) -> bool:
        """Prueba la optimización de memoria."""
        try:
            response = requests.post(f"{self.base_url}/api/v1/conversation-memory/optimize", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                action = data.get("action")
                success = data.get("success", False)
                
                details = f"Action: {action}"
                self.log_test("Optimize Memory", success, details)
                return success
            else:
                self.log_test("Optimize Memory", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Optimize Memory", False, f"Error: {str(e)}")
            return False

    def test_clear_context_cache(self) -> bool:
        """Prueba la limpieza del cache de contexto."""
        try:
            response = requests.post(f"{self.base_url}/api/v1/conversation-memory/context/cache/clear", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                action = data.get("action")
                success = data.get("success", False)
                
                details = f"Action: {action}"
                self.log_test("Clear Context Cache", success, details)
                return success
            else:
                self.log_test("Clear Context Cache", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Clear Context Cache", False, f"Error: {str(e)}")
            return False

    def test_get_context_versions(self) -> bool:
        """Prueba la obtención de versiones de contexto."""
        if not self.session_ids:
            self.log_test("Get Context Versions", False, "No session IDs available")
            return False
            
        try:
            session_id = self.session_ids[0]
            response = requests.get(f"{self.base_url}/api/v1/conversation-memory/session/{session_id}/context/versions", 
                                  timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                versions_count = data.get("versions_count", 0)
                success = data.get("success", False)
                
                details = f"Versions: {versions_count}"
                self.log_test("Get Context Versions", success, details)
                return success
            else:
                self.log_test("Get Context Versions", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Get Context Versions", False, f"Error: {str(e)}")
            return False

    def test_end_session(self) -> bool:
        """Prueba la finalización de sesiones."""
        if not self.session_ids:
            self.log_test("End Session", False, "No session IDs available")
            return False
            
        try:
            session_id = self.session_ids[0]
            response = requests.post(f"{self.base_url}/api/v1/conversation-memory/session/{session_id}/end", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                action = data.get("action")
                success = data.get("success", False)
                
                details = f"Action: {action}"
                self.log_test("End Session", success, details)
                return success
            else:
                self.log_test("End Session", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("End Session", False, f"Error: {str(e)}")
            return False

    def test_cancel_session(self) -> bool:
        """Prueba la cancelación de sesiones."""
        if len(self.session_ids) < 2:
            self.log_test("Cancel Session", False, "Need at least 2 session IDs")
            return False
            
        try:
            session_id = self.session_ids[1]
            response = requests.post(f"{self.base_url}/api/v1/conversation-memory/session/{session_id}/cancel", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                action = data.get("action")
                success = data.get("success", False)
                
                details = f"Action: {action}"
                self.log_test("Cancel Session", success, details)
                return success
            else:
                self.log_test("Cancel Session", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Cancel Session", False, f"Error: {str(e)}")
            return False

    def test_delete_session(self) -> bool:
        """Prueba la eliminación de sesiones."""
        if len(self.session_ids) < 3:
            self.log_test("Delete Session", False, "Need at least 3 session IDs")
            return False
            
        try:
            session_id = self.session_ids[2]
            response = requests.delete(f"{self.base_url}/api/v1/conversation-memory/session/{session_id}", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                action = data.get("action")
                success = data.get("success", False)
                
                details = f"Action: {action}"
                self.log_test("Delete Session", success, details)
                return success
            else:
                self.log_test("Delete Session", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Delete Session", False, f"Error: {str(e)}")
            return False

    def test_service_test_endpoint(self) -> bool:
        """Prueba el endpoint de test del servicio."""
        try:
            response = requests.post(f"{self.base_url}/api/v1/conversation-memory/test", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                service = data.get("service")
                status = data.get("status")
                features = data.get("features", [])
                
                details = f"Service: {service}, Status: {status}, Features: {len(features)}"
                self.log_test("Service Test Endpoint", status == "operational", details)
                return status == "operational"
            else:
                self.log_test("Service Test Endpoint", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Service Test Endpoint", False, f"Error: {str(e)}")
            return False

    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas."""
        print("🚀 Iniciando pruebas del sistema de memoria conversacional (T4.4)")
        print("=" * 70)
        
        # Esperar a que el servicio esté disponible
        if not self.wait_for_service():
            return False
        
        # Ejecutar pruebas en orden
        tests = [
            ("Health Check", self.test_health_check),
            ("Statistics", self.test_statistics),
            ("Create Session", self.test_create_session),
            ("Create Session 2", self.test_create_session),  # Crear segunda sesión
            ("Create Session 3", self.test_create_session),  # Crear tercera sesión
            ("Get Session", self.test_get_session),
            ("Get User Sessions", self.test_get_user_sessions),
            ("Get All Active Sessions", self.test_get_all_active_sessions),
            ("Search Sessions", self.test_search_sessions),
            ("Compress Context", self.test_compress_context),
            ("Optimize Memory", self.test_optimize_memory),
            ("Clear Context Cache", self.test_clear_context_cache),
            ("Get Context Versions", self.test_get_context_versions),
            ("End Session", self.test_end_session),
            ("Cancel Session", self.test_cancel_session),
            ("Delete Session", self.test_delete_session),
            ("Service Test Endpoint", self.test_service_test_endpoint)
        ]
        
        passed_tests = 0
        total_tests = len(tests)
        
        for test_name, test_func in tests:
            try:
                if test_func():
                    passed_tests += 1
            except Exception as e:
                self.log_test(test_name, False, f"Exception: {str(e)}")
        
        # Resumen final
        print("=" * 70)
        print(f"📊 RESUMEN DE PRUEBAS")
        print(f"Total de pruebas: {total_tests}")
        print(f"Pruebas exitosas: {passed_tests}")
        print(f"Pruebas fallidas: {total_tests - passed_tests}")
        print(f"Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        
        success = passed_tests == total_tests
        status = "✅ TODAS LAS PRUEBAS PASARON" if success else "❌ ALGUNAS PRUEBAS FALLARON"
        print(f"\n{status}")
        
        return success

    def get_detailed_results(self) -> Dict[str, Any]:
        """Obtiene resultados detallados de las pruebas."""
        passed_tests = sum(1 for result in self.test_results if result["success"])
        total_tests = len(self.test_results)
        
        return {
            "total_tests": total_tests,
            "passed_tests": passed_tests,
            "failed_tests": total_tests - passed_tests,
            "success_rate": (passed_tests/total_tests)*100 if total_tests > 0 else 0,
            "test_results": self.test_results,
            "session_ids": self.session_ids,
            "user_ids": self.user_ids,
            "timestamp": datetime.now().isoformat()
        }

def main():
    """Función principal."""
    import sys
    
    # Configurar URL base
    base_url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:9904"
    
    # Crear tester y ejecutar pruebas
    tester = ConversationMemoryTester(base_url)
    success = tester.run_complete_test_suite()
    
    # Guardar resultados detallados
    results = tester.get_detailed_results()
    with open("conversation_memory_test_results.json", "w") as f:
        json.dump(results, f, indent=2)
    
    print(f"\n📄 Resultados detallados guardados en: conversation_memory_test_results.json")
    
    # Código de salida
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 