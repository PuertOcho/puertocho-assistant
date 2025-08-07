#!/usr/bin/env python3
"""
Script de pruebas automatizadas para TaskOrchestrator
Prueba la funcionalidad completa del orquestador de tareas.
"""

import requests
import json
import time
import sys
from datetime import datetime
from typing import Dict, List, Any

# Configuración
BASE_URL = "http://localhost:9904"
ORCHESTRATOR_BASE = f"{BASE_URL}/api/v1/task-orchestrator"

class TaskOrchestratorTester:
    def __init__(self):
        self.session = requests.Session()
        self.test_results = []
        self.start_time = datetime.now()
        
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
    
    def test_health_check(self) -> bool:
        """Prueba el health check del TaskOrchestrator."""
        try:
            response = self.session.get(f"{ORCHESTRATOR_BASE}/health")
            
            if response.status_code == 200:
                data = response.json()
                if data.get("status") == "healthy" and data.get("service") == "TaskOrchestrator":
                    self.log_test("Health Check", True, f"Status: {data.get('status')}")
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
        """Prueba la obtención de estadísticas."""
        try:
            response = self.session.get(f"{ORCHESTRATOR_BASE}/statistics")
            
            if response.status_code == 200:
                data = response.json()
                if "service" in data and data["service"] == "TaskOrchestrator":
                    self.log_test("Statistics", True, f"Active executions: {data.get('active_executions', 0)}")
                    return True
                else:
                    self.log_test("Statistics", False, f"Respuesta inesperada: {data}")
                    return False
            else:
                self.log_test("Statistics", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Statistics", False, f"Error: {str(e)}")
            return False
    
    def test_execute_subtasks(self) -> bool:
        """Prueba la ejecución de subtareas."""
        try:
            # Crear subtareas de prueba
            subtasks = [
                {
                    "subtask_id": "test_001",
                    "action": "consultar_tiempo",
                    "description": "Consultar tiempo en Barcelona",
                    "entities": {"ubicacion": "Barcelona"},
                    "priority": "high",
                    "dependencies": [],
                    "max_retries": 3
                },
                {
                    "subtask_id": "test_002", 
                    "action": "programar_alarma",
                    "description": "Programar alarma para mañana",
                    "entities": {"hora": "08:00", "mensaje": "Despertar"},
                    "priority": "medium",
                    "dependencies": ["test_001"],
                    "max_retries": 3
                }
            ]
            
            request_data = {
                "subtasks": subtasks,
                "conversation_session_id": "test_session_001"
            }
            
            response = self.session.post(
                f"{ORCHESTRATOR_BASE}/execute",
                json=request_data,
                headers={"Content-Type": "application/json"}
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("total_tasks") == 2 and data.get("successful_tasks") == 2:
                    self.log_test("Execute Subtasks", True, 
                                f"Ejecutadas: {data.get('successful_tasks')}/{data.get('total_tasks')}")
                    return True
                else:
                    self.log_test("Execute Subtasks", False, 
                                f"Resultado inesperado: {data.get('successful_tasks')}/{data.get('total_tasks')}")
                    return False
            else:
                self.log_test("Execute Subtasks", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Execute Subtasks", False, f"Error: {str(e)}")
            return False
    
    def test_decompose_and_execute(self) -> bool:
        """Prueba la descomposición y ejecución completa."""
        try:
            request_data = {
                "user_message": "Consulta el tiempo de Madrid y programa una alarma si va a llover",
                "conversation_session_id": "test_session_002"
            }
            
            response = self.session.post(
                f"{ORCHESTRATOR_BASE}/decompose-and-execute",
                json=request_data,
                headers={"Content-Type": "application/json"}
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success") and data.get("decomposed_subtasks", 0) > 0:
                    self.log_test("Decompose and Execute", True, 
                                f"Subtareas descompuestas: {data.get('decomposed_subtasks')}")
                    return True
                else:
                    self.log_test("Decompose and Execute", False, 
                                f"Resultado inesperado: {data}")
                    return False
            else:
                self.log_test("Decompose and Execute", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Decompose and Execute", False, f"Error: {str(e)}")
            return False
    
    def test_session_management(self) -> bool:
        """Prueba la gestión de sesiones de ejecución."""
        try:
            # Primero ejecutar algunas subtareas para crear una sesión
            subtasks = [
                {
                    "subtask_id": "session_test_001",
                    "action": "consultar_tiempo",
                    "description": "Test de sesión",
                    "entities": {"ubicacion": "Madrid"},
                    "priority": "high",
                    "dependencies": [],
                    "maxRetries": 3
                }
            ]
            
            request_data = {
                "subtasks": subtasks,
                "conversation_session_id": "session_test_001"
            }
            
            # Ejecutar subtareas
            response = self.session.post(
                f"{ORCHESTRATOR_BASE}/execute",
                json=request_data,
                headers={"Content-Type": "application/json"}
            )
            
            if response.status_code == 200:
                data = response.json()
                execution_id = data.get("execution_id")
                
                if execution_id:
                    # Intentar obtener la sesión (puede que ya no esté activa)
                    session_response = self.session.get(f"{ORCHESTRATOR_BASE}/session/{execution_id}")
                    
                    if session_response.status_code in [200, 404]:  # 404 es válido si la sesión ya terminó
                        self.log_test("Session Management", True, f"Execution ID: {execution_id}")
                        return True
                    else:
                        self.log_test("Session Management", False, f"Status code: {session_response.status_code}")
                        return False
                else:
                    self.log_test("Session Management", False, "No se obtuvo execution ID")
                    return False
            else:
                self.log_test("Session Management", False, f"Error ejecutando subtareas: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Session Management", False, f"Error: {str(e)}")
            return False
    
    def test_cancel_execution(self) -> bool:
        """Prueba la cancelación de ejecuciones."""
        try:
            # Intentar cancelar una ejecución inexistente
            response = self.session.post(f"{ORCHESTRATOR_BASE}/cancel/nonexistent_execution")
            
            if response.status_code == 404:
                self.log_test("Cancel Execution", True, "Cancelación de ejecución inexistente manejada correctamente")
                return True
            else:
                self.log_test("Cancel Execution", False, f"Status code inesperado: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Cancel Execution", False, f"Error: {str(e)}")
            return False
    
    def test_automated_test(self) -> bool:
        """Prueba el endpoint de test automatizado."""
        try:
            response = self.session.post(f"{ORCHESTRATOR_BASE}/test")
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success") and data.get("subtasks_created", 0) > 0:
                    self.log_test("Automated Test", True, 
                                f"Test completado: {data.get('success_rate', 0):.1%} éxito")
                    return True
                else:
                    self.log_test("Automated Test", False, f"Test falló: {data}")
                    return False
            else:
                self.log_test("Automated Test", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Automated Test", False, f"Error: {str(e)}")
            return False
    
    def test_error_handling(self) -> bool:
        """Prueba el manejo de errores."""
        try:
            # Probar con request inválido
            invalid_request = {
                "subtasks": [],  # Lista vacía
                "conversation_session_id": "test_session"
            }
            
            response = self.session.post(
                f"{ORCHESTRATOR_BASE}/execute",
                json=invalid_request,
                headers={"Content-Type": "application/json"}
            )
            
            if response.status_code == 400:
                self.log_test("Error Handling", True, "Request inválido manejado correctamente")
                return True
            else:
                self.log_test("Error Handling", False, f"Status code inesperado: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Error Handling", False, f"Error: {str(e)}")
            return False
    
    def run_all_tests(self) -> Dict[str, Any]:
        """Ejecuta todas las pruebas."""
        print("🚀 Iniciando pruebas del TaskOrchestrator")
        print("=" * 60)
        print()
        
        tests = [
            ("Health Check", self.test_health_check),
            ("Statistics", self.test_statistics),
            ("Execute Subtasks", self.test_execute_subtasks),
            ("Decompose and Execute", self.test_decompose_and_execute),
            ("Session Management", self.test_session_management),
            ("Cancel Execution", self.test_cancel_execution),
            ("Automated Test", self.test_automated_test),
            ("Error Handling", self.test_error_handling)
        ]
        
        passed = 0
        total = len(tests)
        
        for test_name, test_func in tests:
            try:
                if test_func():
                    passed += 1
            except Exception as e:
                self.log_test(test_name, False, f"Excepción: {str(e)}")
        
        # Generar reporte
        end_time = datetime.now()
        duration = (end_time - self.start_time).total_seconds()
        
        report = {
            "test_suite": "TaskOrchestrator",
            "start_time": self.start_time.isoformat(),
            "end_time": end_time.isoformat(),
            "duration_seconds": duration,
            "total_tests": total,
            "passed_tests": passed,
            "failed_tests": total - passed,
            "success_rate": passed / total if total > 0 else 0,
            "test_results": self.test_results
        }
        
        print("=" * 60)
        print(f"📊 RESUMEN DE PRUEBAS")
        print(f"Total: {total}")
        print(f"✅ Pasaron: {passed}")
        print(f"❌ Fallaron: {total - passed}")
        print(f"📈 Tasa de éxito: {passed/total*100:.1f}%")
        print(f"⏱️  Duración: {duration:.2f} segundos")
        print()
        
        return report
    
    def save_report(self, report: Dict[str, Any], filename: str = None):
        """Guarda el reporte en un archivo JSON."""
        if filename is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"task_orchestrator_test_results_{timestamp}.json"
        
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(report, f, indent=2, ensure_ascii=False)
            print(f"📄 Reporte guardado en: {filename}")
        except Exception as e:
            print(f"❌ Error guardando reporte: {str(e)}")

def main():
    """Función principal."""
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
        global BASE_URL, ORCHESTRATOR_BASE
        BASE_URL = base_url
        ORCHESTRATOR_BASE = f"{BASE_URL}/api/v1/task-orchestrator"
        print(f"🌐 Usando URL base: {BASE_URL}")
    
    tester = TaskOrchestratorTester()
    
    try:
        report = tester.run_all_tests()
        tester.save_report(report)
        
        # Código de salida basado en el éxito de las pruebas
        if report["success_rate"] >= 0.8:  # 80% o más de éxito
            print("🎉 Pruebas completadas exitosamente")
            sys.exit(0)
        else:
            print("⚠️  Algunas pruebas fallaron")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n⏹️  Pruebas interrumpidas por el usuario")
        sys.exit(1)
    except Exception as e:
        print(f"💥 Error fatal: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main() 