#!/usr/bin/env python3
"""
Script de pruebas automatizadas para el sistema de seguimiento de progreso
T4.7 - Sistema de estado de progreso: tracking automático hasta completion de todas las subtareas

Autor: PuertoCho Assistant Team
Fecha: 2025-01-27
"""

import requests
import json
import time
import sys
from datetime import datetime
from typing import Dict, Any, List

# Configuración
BASE_URL = "http://localhost:9904"
PROGRESS_TRACKER_BASE = f"{BASE_URL}/api/v1/progress-tracker"

class ProgressTrackerTester:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        self.test_results = []
        
    def log_test(self, test_name: str, success: bool, message: str = "", data: Dict[str, Any] = None):
        """Registra el resultado de una prueba"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        result = {
            "timestamp": timestamp,
            "test_name": test_name,
            "success": success,
            "message": message,
            "data": data or {}
        }
        self.test_results.append(result)
        
        status = "✅ PASÓ" if success else "❌ FALLÓ"
        print(f"{status} {test_name}: {message}")
        
        if data and not success:
            print(f"   Datos: {json.dumps(data, indent=2, ensure_ascii=False)}")
    
    def test_health_check(self) -> bool:
        """Prueba el health check del sistema"""
        try:
            response = self.session.get(f"{PROGRESS_TRACKER_BASE}/health")
            
            if response.status_code == 200:
                data = response.json()
                if data.get("status") == "UP":
                    self.log_test("Health Check", True, "Sistema de progreso operativo")
                    return True
                else:
                    self.log_test("Health Check", False, f"Estado inesperado: {data.get('status')}", data)
                    return False
            else:
                self.log_test("Health Check", False, f"HTTP {response.status_code}", {"response": response.text})
                return False
                
        except Exception as e:
            self.log_test("Health Check", False, f"Error de conexión: {str(e)}")
            return False
    
    def test_statistics(self) -> bool:
        """Prueba la obtención de estadísticas"""
        try:
            response = self.session.get(f"{PROGRESS_TRACKER_BASE}/statistics")
            
            if response.status_code == 200:
                data = response.json()
                required_keys = ["active_trackers", "total_subtasks_tracked", "enable_real_time_tracking"]
                
                if all(key in data for key in required_keys):
                    self.log_test("Statistics", True, "Estadísticas obtenidas correctamente")
                    return True
                else:
                    missing_keys = [key for key in required_keys if key not in data]
                    self.log_test("Statistics", False, f"Claves faltantes: {missing_keys}", data)
                    return False
            else:
                self.log_test("Statistics", False, f"HTTP {response.status_code}", {"response": response.text})
                return False
                
        except Exception as e:
            self.log_test("Statistics", False, f"Error: {str(e)}")
            return False
    
    def test_start_tracking(self) -> tuple[bool, str]:
        """Prueba el inicio de seguimiento de progreso"""
        try:
            # Crear solicitud de prueba
            test_request = {
                "execution_session_id": f"test_exec_{int(time.time())}",
                "conversation_session_id": f"test_conv_{int(time.time())}",
                "subtasks": [
                    {
                        "subtask_id": "task_001",
                        "action": "consultar_tiempo",
                        "description": "Consultar tiempo de Madrid",
                        "entities": {"ubicacion": "Madrid"},
                        "confidence_score": 0.9
                    },
                    {
                        "subtask_id": "task_002", 
                        "action": "programar_alarma",
                        "description": "Programar alarma para mañana",
                        "entities": {"fecha": "mañana"},
                        "confidence_score": 0.8
                    }
                ],
                "tracking_config": {
                    "enable_real_time_tracking": True,
                    "update_interval_ms": 1000,
                    "enable_notifications": True,
                    "enable_completion_validation": True
                }
            }
            
            response = self.session.post(
                f"{PROGRESS_TRACKER_BASE}/start-tracking",
                json=test_request
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success") and data.get("tracker_id"):
                    tracker_id = data["tracker_id"]
                    self.log_test("Start Tracking", True, f"Tracker creado: {tracker_id}")
                    return True, tracker_id
                else:
                    self.log_test("Start Tracking", False, "Respuesta inválida", data)
                    return False, ""
            else:
                self.log_test("Start Tracking", False, f"HTTP {response.status_code}", {"response": response.text})
                return False, ""
                
        except Exception as e:
            self.log_test("Start Tracking", False, f"Error: {str(e)}")
            return False, ""
    
    def test_update_progress(self, tracker_id: str) -> bool:
        """Prueba la actualización de progreso"""
        try:
            # Actualizar progreso de la primera subtarea
            params = {
                "trackerId": tracker_id,
                "subtaskId": "task_001",
                "status": "IN_PROGRESS",
                "progressPercentage": 25.0
            }
            
            response = self.session.post(f"{PROGRESS_TRACKER_BASE}/update-progress", params=params)
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    self.log_test("Update Progress", True, "Progreso actualizado correctamente")
                    return True
                else:
                    self.log_test("Update Progress", False, "Respuesta inválida", data)
                    return False
            else:
                self.log_test("Update Progress", False, f"HTTP {response.status_code}", {"response": response.text})
                return False
                
        except Exception as e:
            self.log_test("Update Progress", False, f"Error: {str(e)}")
            return False
    
    def test_get_status(self, tracker_id: str) -> bool:
        """Prueba la obtención del estado de progreso"""
        try:
            response = self.session.get(f"{PROGRESS_TRACKER_BASE}/status/{tracker_id}")
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success") and data.get("progress_tracker"):
                    progress_tracker = data["progress_tracker"]
                    if progress_tracker.get("tracker_id") == tracker_id:
                        self.log_test("Get Status", True, f"Estado obtenido: {progress_tracker.get('progress_percentage')}%")
                        return True
                    else:
                        self.log_test("Get Status", False, "Tracker ID no coincide", data)
                        return False
                else:
                    self.log_test("Get Status", False, "Respuesta inválida", data)
                    return False
            else:
                self.log_test("Get Status", False, f"HTTP {response.status_code}", {"response": response.text})
                return False
                
        except Exception as e:
            self.log_test("Get Status", False, f"Error: {str(e)}")
            return False
    
    def test_complete_subtasks(self, tracker_id: str) -> bool:
        """Prueba la completitud de subtareas"""
        try:
            # Completar primera subtarea
            params1 = {
                "trackerId": tracker_id,
                "subtaskId": "task_001",
                "status": "COMPLETED",
                "progressPercentage": 100.0
            }
            
            response1 = self.session.post(f"{PROGRESS_TRACKER_BASE}/update-progress", params=params1)
            
            if response1.status_code != 200:
                self.log_test("Complete Subtasks", False, f"Error al completar task_001: HTTP {response1.status_code}")
                return False
            
            # Completar segunda subtarea
            params2 = {
                "trackerId": tracker_id,
                "subtaskId": "task_002",
                "status": "COMPLETED",
                "progressPercentage": 100.0
            }
            
            response2 = self.session.post(f"{PROGRESS_TRACKER_BASE}/update-progress", params=params2)
            
            if response2.status_code == 200:
                self.log_test("Complete Subtasks", True, "Todas las subtareas completadas")
                return True
            else:
                self.log_test("Complete Subtasks", False, f"Error al completar task_002: HTTP {response2.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Complete Subtasks", False, f"Error: {str(e)}")
            return False
    
    def test_completion_validation(self, tracker_id: str) -> bool:
        """Prueba la validación de completitud"""
        try:
            response = self.session.get(f"{PROGRESS_TRACKER_BASE}/status/{tracker_id}")
            
            if response.status_code == 200:
                data = response.json()
                completion_status = data.get("completion_status", {})
                
                if completion_status.get("is_completed"):
                    self.log_test("Completion Validation", True, "Completitud validada correctamente")
                    return True
                else:
                    self.log_test("Completion Validation", False, "Completitud no validada", completion_status)
                    return False
            else:
                self.log_test("Completion Validation", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Completion Validation", False, f"Error: {str(e)}")
            return False
    
    def test_notifications(self, tracker_id: str) -> bool:
        """Prueba el sistema de notificaciones"""
        try:
            response = self.session.get(f"{PROGRESS_TRACKER_BASE}/notifications/{tracker_id}")
            
            if response.status_code == 200:
                data = response.json()
                notifications = data.get("notifications", [])
                
                if isinstance(notifications, list):
                    self.log_test("Notifications", True, f"{len(notifications)} notificaciones obtenidas")
                    return True
                else:
                    self.log_test("Notifications", False, "Formato de notificaciones inválido", data)
                    return False
            else:
                self.log_test("Notifications", False, f"HTTP {response.status_code}", {"response": response.text})
                return False
                
        except Exception as e:
            self.log_test("Notifications", False, f"Error: {str(e)}")
            return False
    
    def test_cancel_tracking(self, tracker_id: str) -> bool:
        """Prueba la cancelación de seguimiento"""
        try:
            response = self.session.post(f"{PROGRESS_TRACKER_BASE}/cancel/{tracker_id}")
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    self.log_test("Cancel Tracking", True, "Seguimiento cancelado correctamente")
                    return True
                else:
                    self.log_test("Cancel Tracking", False, "Respuesta inválida", data)
                    return False
            else:
                self.log_test("Cancel Tracking", False, f"HTTP {response.status_code}", {"response": response.text})
                return False
                
        except Exception as e:
            self.log_test("Cancel Tracking", False, f"Error: {str(e)}")
            return False
    
    def test_cleanup(self) -> bool:
        """Prueba la limpieza de trackers"""
        try:
            response = self.session.post(f"{PROGRESS_TRACKER_BASE}/cleanup")
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    self.log_test("Cleanup", True, "Limpieza completada correctamente")
                    return True
                else:
                    self.log_test("Cleanup", False, "Respuesta inválida", data)
                    return False
            else:
                self.log_test("Cleanup", False, f"HTTP {response.status_code}", {"response": response.text})
                return False
                
        except Exception as e:
            self.log_test("Cleanup", False, f"Error: {str(e)}")
            return False
    
    def test_automated_test(self) -> tuple[bool, str]:
        """Prueba el endpoint de test automatizado"""
        try:
            response = self.session.post(f"{PROGRESS_TRACKER_BASE}/test")
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    test_name = data.get("test_name", "Unknown")
                    tracker_id = data.get("tracker_id", "Unknown")
                    self.log_test("Automated Test", True, f"{test_name} completado: {tracker_id}")
                    return True, tracker_id
                else:
                    self.log_test("Automated Test", False, "Test falló", data)
                    return False, ""
            else:
                self.log_test("Automated Test", False, f"HTTP {response.status_code}", {"response": response.text})
                return False, ""
                
        except Exception as e:
            self.log_test("Automated Test", False, f"Error: {str(e)}")
            return False, ""
    
    def run_all_tests(self) -> bool:
        """Ejecuta todas las pruebas"""
        print("🚀 Iniciando pruebas del sistema de seguimiento de progreso")
        print("=" * 60)
        
        # Pruebas básicas
        if not self.test_health_check():
            return False
        
        if not self.test_statistics():
            return False
        
        # Prueba de test automatizado
        success, tracker_id = self.test_automated_test()
        if not success:
            return False
        
        # Usar el tracker del test automatizado para las pruebas posteriores
        # No crear un nuevo tracker manual
        
        if not self.test_update_progress(tracker_id):
            return False
        
        if not self.test_get_status(tracker_id):
            return False
        
        if not self.test_complete_subtasks(tracker_id):
            return False
        
        if not self.test_completion_validation(tracker_id):
            return False
        
        if not self.test_notifications(tracker_id):
            return False
        
        if not self.test_cancel_tracking(tracker_id):
            return False
        
        if not self.test_cleanup():
            return False
        
        return True
    
    def print_summary(self):
        """Imprime el resumen de las pruebas"""
        print("\n" + "=" * 60)
        print("📊 RESUMEN DE PRUEBAS")
        print("=" * 60)
        
        total_tests = len(self.test_results)
        passed_tests = sum(1 for result in self.test_results if result["success"])
        failed_tests = total_tests - passed_tests
        
        print(f"Total de pruebas: {total_tests}")
        print(f"Pruebas exitosas: {passed_tests} ✅")
        print(f"Pruebas fallidas: {failed_tests} ❌")
        print(f"Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        
        if failed_tests > 0:
            print("\n❌ PRUEBAS FALLIDAS:")
            for result in self.test_results:
                if not result["success"]:
                    print(f"  - {result['test_name']}: {result['message']}")
        
        print("\n" + "=" * 60)
        
        return failed_tests == 0

def main():
    """Función principal"""
    tester = ProgressTrackerTester()
    
    try:
        success = tester.run_all_tests()
        final_success = tester.print_summary()
        
        if final_success:
            print("🎉 TODAS LAS PRUEBAS PASARON EXITOSAMENTE")
            sys.exit(0)
        else:
            print("💥 ALGUNAS PRUEBAS FALLARON")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n⚠️  Pruebas interrumpidas por el usuario")
        sys.exit(1)
    except Exception as e:
        print(f"\n💥 Error inesperado: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()
