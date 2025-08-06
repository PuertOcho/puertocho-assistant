#!/usr/bin/env python3
"""
Script de pruebas automatizadas para el Dynamic Subtask Decomposer.
Prueba la funcionalidad de descomposición dinámica de subtareas.
"""

import requests
import json
import time
import sys
from typing import Dict, Any, List
from datetime import datetime

class DynamicSubtaskDecomposerTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.test_results = []
        self.start_time = time.time()
        
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
        print(f"{status} - {test_name}")
        if details:
            print(f"   Detalles: {details}")
        print()
    
    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        """Espera a que el servicio esté disponible."""
        print("🔄 Esperando a que el servicio esté disponible...")
        
        for attempt in range(max_attempts):
            try:
                response = requests.get(f"{self.base_url}/api/v1/subtask-decomposer/health", timeout=5)
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
        """Prueba el health check del servicio."""
        try:
            response = requests.get(f"{self.base_url}/api/v1/subtask-decomposer/health", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                if data.get("status") == "UP" and data.get("service") == "Dynamic Subtask Decomposer":
                    self.log_test("Health Check", True, f"Servicio: {data.get('service')}, Versión: {data.get('version')}")
                    return True
                else:
                    self.log_test("Health Check", False, f"Respuesta inesperada: {data}")
                    return False
            else:
                self.log_test("Health Check", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Health Check", False, f"Error: {str(e)}")
            return False
    
    def test_statistics(self) -> bool:
        """Prueba el endpoint de estadísticas."""
        try:
            response = requests.get(f"{self.base_url}/api/v1/subtask-decomposer/statistics", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                required_fields = ["service", "status", "supported_actions", "max_subtasks_per_request"]
                
                if all(field in data for field in required_fields):
                    self.log_test("Statistics", True, f"Acciones soportadas: {len(data.get('supported_actions', []))}")
                    return True
                else:
                    self.log_test("Statistics", False, f"Campos faltantes en respuesta")
                    return False
            else:
                self.log_test("Statistics", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Statistics", False, f"Error: {str(e)}")
            return False
    
    def test_simple_decomposition(self) -> bool:
        """Prueba la descomposición de una petición simple."""
        try:
            request_data = {
                "user_message": "¿Qué tiempo hace en Madrid?",
                "conversation_session_id": "test_session_001",
                "max_subtasks": 5
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/subtask-decomposer/decompose",
                json=request_data,
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                
                if (data.get("subtasks") and 
                    len(data.get("subtasks", [])) > 0 and
                    data.get("decomposition_confidence", 0) > 0):
                    
                    subtasks = data.get("subtasks", [])
                    self.log_test("Simple Decomposition", True, 
                                f"Subtareas generadas: {len(subtasks)}, Confianza: {data.get('decomposition_confidence'):.2f}")
                    return True
                else:
                    self.log_test("Simple Decomposition", False, "No se generaron subtareas válidas")
                    return False
            else:
                self.log_test("Simple Decomposition", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Simple Decomposition", False, f"Error: {str(e)}")
            return False
    
    def test_complex_decomposition(self) -> bool:
        """Prueba la descomposición de una petición compleja."""
        try:
            request_data = {
                "user_message": "Consulta el tiempo de Madrid y programa una alarma si va a llover",
                "conversation_session_id": "test_session_002",
                "max_subtasks": 10,
                "enable_dependency_detection": True,
                "enable_priority_assignment": True
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/subtask-decomposer/decompose",
                json=request_data,
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                
                if (data.get("subtasks") and 
                    len(data.get("subtasks", [])) >= 2 and
                    data.get("dependencies_detected") is not None):
                    
                    subtasks = data.get("subtasks", [])
                    dependencies_detected = data.get("dependencies_detected", False)
                    
                    self.log_test("Complex Decomposition", True, 
                                f"Subtareas: {len(subtasks)}, Dependencias: {dependencies_detected}, "
                                f"Confianza: {data.get('decomposition_confidence'):.2f}")
                    return True
                else:
                    self.log_test("Complex Decomposition", False, "No se generaron suficientes subtareas o dependencias")
                    return False
            else:
                self.log_test("Complex Decomposition", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Complex Decomposition", False, f"Error: {str(e)}")
            return False
    
    def test_multiple_actions_decomposition(self) -> bool:
        """Prueba la descomposición de peticiones con múltiples acciones."""
        try:
            request_data = {
                "user_message": "Enciende las luces del salón, pon música relajante y ajusta la temperatura a 22°",
                "conversation_session_id": "test_session_003",
                "max_subtasks": 10,
                "enable_parallel_execution": True
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/subtask-decomposer/decompose",
                json=request_data,
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                
                if (data.get("subtasks") and 
                    len(data.get("subtasks", [])) >= 3 and
                    data.get("can_execute_parallel") is not None):
                    
                    subtasks = data.get("subtasks", [])
                    can_execute_parallel = data.get("can_execute_parallel", False)
                    
                    self.log_test("Multiple Actions Decomposition", True, 
                                f"Subtareas: {len(subtasks)}, Ejecución paralela: {can_execute_parallel}, "
                                f"Confianza: {data.get('decomposition_confidence'):.2f}")
                    return True
                else:
                    self.log_test("Multiple Actions Decomposition", False, "No se generaron suficientes subtareas")
                    return False
            else:
                self.log_test("Multiple Actions Decomposition", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Multiple Actions Decomposition", False, f"Error: {str(e)}")
            return False
    
    def test_simple_decomposition_endpoint(self) -> bool:
        """Prueba el endpoint de descomposición simple."""
        try:
            request_data = {
                "user_message": "Programa una alarma para las 8:00",
                "session_id": "test_session_004"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/subtask-decomposer/decompose-simple",
                json=request_data,
                timeout=30
            )
            
            if response.status_code == 200:
                data = response.json()
                
                if data.get("subtasks") and len(data.get("subtasks", [])) > 0:
                    subtasks = data.get("subtasks", [])
                    self.log_test("Simple Decomposition Endpoint", True, 
                                f"Subtareas generadas: {len(subtasks)}")
                    return True
                else:
                    self.log_test("Simple Decomposition Endpoint", False, "No se generaron subtareas")
                    return False
            else:
                self.log_test("Simple Decomposition Endpoint", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Simple Decomposition Endpoint", False, f"Error: {str(e)}")
            return False
    
    def test_validation_endpoint(self) -> bool:
        """Prueba el endpoint de validación."""
        try:
            # Test con solicitud válida
            valid_request = {
                "user_message": "Test message",
                "conversation_session_id": "test_session_005",
                "max_subtasks": 5,
                "confidence_threshold": 0.7
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/subtask-decomposer/validate",
                json=valid_request,
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("valid") is True:
                    self.log_test("Validation Endpoint", True, "Solicitud válida aceptada")
                    return True
                else:
                    self.log_test("Validation Endpoint", False, f"Validación falló: {data.get('errors')}")
                    return False
            else:
                self.log_test("Validation Endpoint", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Validation Endpoint", False, f"Error: {str(e)}")
            return False
    
    def test_available_actions(self) -> bool:
        """Prueba el endpoint de acciones disponibles."""
        try:
            response = requests.get(f"{self.base_url}/api/v1/subtask-decomposer/available-actions", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                if (data.get("available_actions") and 
                    len(data.get("available_actions", [])) > 0 and
                    data.get("total_actions") > 0):
                    
                    actions = data.get("available_actions", [])
                    self.log_test("Available Actions", True, f"Acciones disponibles: {len(actions)}")
                    return True
                else:
                    self.log_test("Available Actions", False, "No se obtuvieron acciones")
                    return False
            else:
                self.log_test("Available Actions", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Available Actions", False, f"Error: {str(e)}")
            return False
    
    def test_action_info(self) -> bool:
        """Prueba el endpoint de información de acción específica."""
        try:
            response = requests.get(f"{self.base_url}/api/v1/subtask-decomposer/actions/consultar_tiempo", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                if (data.get("name") == "consultar_tiempo" and 
                    data.get("description") and
                    data.get("required_entities") is not None):
                    
                    self.log_test("Action Info", True, f"Acción: {data.get('name')}, Duración estimada: {data.get('estimated_duration_ms')}ms")
                    return True
                else:
                    self.log_test("Action Info", False, "Información de acción incompleta")
                    return False
            else:
                self.log_test("Action Info", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Action Info", False, f"Error: {str(e)}")
            return False
    
    def test_examples_endpoint(self) -> bool:
        """Prueba el endpoint de ejemplos."""
        try:
            response = requests.get(f"{self.base_url}/api/v1/subtask-decomposer/examples", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                if (data.get("simple_requests") and 
                    data.get("complex_requests") and
                    data.get("multi_step_requests")):
                    
                    simple_count = len(data.get("simple_requests", []))
                    complex_count = len(data.get("complex_requests", []))
                    
                    self.log_test("Examples Endpoint", True, 
                                f"Ejemplos simples: {simple_count}, Complejos: {complex_count}")
                    return True
                else:
                    self.log_test("Examples Endpoint", False, "Ejemplos incompletos")
                    return False
            else:
                self.log_test("Examples Endpoint", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Examples Endpoint", False, f"Error: {str(e)}")
            return False
    
    def test_service_test_endpoint(self) -> bool:
        """Prueba el endpoint de test automatizado del servicio."""
        try:
            response = requests.post(f"{self.base_url}/api/v1/subtask-decomposer/test", timeout=60)
            
            if response.status_code == 200:
                data = response.json()
                
                if (data.get("service") == "Dynamic Subtask Decomposer" and
                    data.get("total_tests") > 0 and
                    data.get("test_cases")):
                    
                    total_tests = data.get("total_tests", 0)
                    successful_tests = data.get("successful_tests", 0)
                    success_rate = data.get("success_rate", 0.0)
                    
                    self.log_test("Service Test Endpoint", True, 
                                f"Tests: {successful_tests}/{total_tests}, Tasa de éxito: {success_rate:.2f}")
                    return True
                else:
                    self.log_test("Service Test Endpoint", False, "Resultados de test incompletos")
                    return False
            else:
                self.log_test("Service Test Endpoint", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Service Test Endpoint", False, f"Error: {str(e)}")
            return False
    
    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas."""
        print("🚀 Iniciando pruebas del Dynamic Subtask Decomposer")
        print("=" * 60)
        
        # Verificar disponibilidad del servicio
        if not self.wait_for_service():
            return False
        
        # Ejecutar pruebas
        tests = [
            ("Health Check", self.test_health_check),
            ("Statistics", self.test_statistics),
            ("Simple Decomposition", self.test_simple_decomposition),
            ("Complex Decomposition", self.test_complex_decomposition),
            ("Multiple Actions Decomposition", self.test_multiple_actions_decomposition),
            ("Simple Decomposition Endpoint", self.test_simple_decomposition_endpoint),
            ("Validation Endpoint", self.test_validation_endpoint),
            ("Available Actions", self.test_available_actions),
            ("Action Info", self.test_action_info),
            ("Examples Endpoint", self.test_examples_endpoint),
            ("Service Test Endpoint", self.test_service_test_endpoint)
        ]
        
        successful_tests = 0
        total_tests = len(tests)
        
        for test_name, test_func in tests:
            try:
                if test_func():
                    successful_tests += 1
            except Exception as e:
                self.log_test(test_name, False, f"Excepción: {str(e)}")
        
        # Resumen final
        print("=" * 60)
        print("📊 RESUMEN DE PRUEBAS")
        print("=" * 60)
        
        success_rate = (successful_tests / total_tests) * 100 if total_tests > 0 else 0
        total_time = time.time() - self.start_time
        
        print(f"✅ Pruebas exitosas: {successful_tests}/{total_tests}")
        print(f"📈 Tasa de éxito: {success_rate:.1f}%")
        print(f"⏱️  Tiempo total: {total_time:.2f} segundos")
        print(f"🎯 Estado general: {'PASÓ' if successful_tests == total_tests else 'FALLÓ'}")
        
        # Guardar resultados en archivo
        self.save_test_results()
        
        return successful_tests == total_tests
    
    def save_test_results(self):
        """Guarda los resultados de las pruebas en un archivo JSON."""
        results = {
            "test_suite": "Dynamic Subtask Decomposer",
            "timestamp": datetime.now().isoformat(),
            "total_time_seconds": time.time() - self.start_time,
            "results": self.test_results,
            "summary": {
                "total_tests": len(self.test_results),
                "successful_tests": len([r for r in self.test_results if r["success"]]),
                "failed_tests": len([r for r in self.test_results if not r["success"]])
            }
        }
        
        filename = f"dynamic_subtask_decomposer_test_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(results, f, indent=2, ensure_ascii=False)
            print(f"💾 Resultados guardados en: {filename}")
        except Exception as e:
            print(f"⚠️  Error guardando resultados: {e}")

def main():
    """Función principal."""
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    else:
        base_url = "http://localhost:9904"
    
    print(f"🔗 URL base: {base_url}")
    print()
    
    tester = DynamicSubtaskDecomposerTester(base_url)
    success = tester.run_complete_test_suite()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 