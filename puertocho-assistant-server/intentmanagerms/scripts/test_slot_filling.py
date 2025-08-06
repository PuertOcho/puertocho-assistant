#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Script de verificación para el sistema de Slot-Filling Automático (T4.2)
Valida la funcionalidad completa del SlotFillingService con preguntas dinámicas LLM.

Autor: Sistema LLM-RAG + MoE
Fecha: 2025-01-27
"""

import requests
import json
import time
import sys
from typing import Dict, Any, List, Optional

class SlotFillingTester:
    """Tester para el sistema de slot-filling automático"""
    
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.test_results = []
        self.total_tests = 0
        self.passed_tests = 0
        self.failed_tests = 0

    def log_test(self, test_name: str, success: bool, details: str = ""):
        """Registra el resultado de un test"""
        status = "✅ PASÓ" if success else "❌ FALLÓ"
        print(f"{status} - {test_name}")
        if details:
            print(f"    Detalles: {details}")
        if not success:
            print(f"    ❌ Error en: {test_name}")
        
        self.test_results.append({
            "test": test_name,
            "success": success,
            "details": details
        })
        
        self.total_tests += 1
        if success:
            self.passed_tests += 1
        else:
            self.failed_tests += 1

    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        """Espera a que el servicio esté disponible"""
        print(f"🔄 Esperando a que el servicio esté disponible en {self.base_url}")
        
        for attempt in range(max_attempts):
            try:
                response = requests.get(f"{self.base_url}/api/v1/slot-filling/health", timeout=5)
                if response.status_code == 200:
                    print(f"✅ Servicio disponible después de {attempt + 1} intentos")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            if attempt < max_attempts - 1:
                time.sleep(delay)
        
        print(f"❌ Servicio no disponible después de {max_attempts} intentos")
        return False

    def test_health_check(self) -> bool:
        """Test básico de health check"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/slot-filling/health", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                is_healthy = data.get("status") == "healthy"
                
                if is_healthy:
                    self.log_test("Health Check", True, f"Servicio healthy: {data}")
                    return True
                else:
                    self.log_test("Health Check", False, f"Servicio unhealthy: {data}")
                    return False
            else:
                self.log_test("Health Check", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Health Check", False, f"Excepción: {str(e)}")
            return False

    def test_statistics(self) -> bool:
        """Test de estadísticas del servicio"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/slot-filling/statistics", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                # Verificar campos esperados
                expected_fields = [
                    "enable_dynamic_questions", 
                    "max_slot_attempts", 
                    "confidence_threshold",
                    "service_status"
                ]
                
                missing_fields = [field for field in expected_fields if field not in data]
                
                if not missing_fields:
                    self.log_test("Statistics", True, f"Campos completos: {list(data.keys())}")
                    return True
                else:
                    self.log_test("Statistics", False, f"Campos faltantes: {missing_fields}")
                    return False
            else:
                self.log_test("Statistics", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Statistics", False, f"Excepción: {str(e)}")
            return False

    def test_basic_slot_filling(self) -> bool:
        """Test básico de slot-filling"""
        try:
            request_data = {
                "intentId": "consultar_tiempo",
                "userMessage": "¿Qué tiempo hace en Madrid?",
                "sessionId": "test-session-001",
                "currentSlots": {},
                "conversationContext": {}
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/slot-filling/process",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                
                # Verificar campos esperados en la respuesta
                required_fields = ["success", "slots_completed", "filled_slots", "missing_slots"]
                missing_fields = [field for field in required_fields if field not in data]
                
                if missing_fields:
                    self.log_test("Basic Slot Filling", False, f"Campos faltantes: {missing_fields}")
                    return False
                
                # Verificar que se extrajo la ubicación "Madrid"
                filled_slots = data.get("filled_slots", {})
                if "ubicacion" in filled_slots and filled_slots["ubicacion"] == "Madrid":
                    self.log_test("Basic Slot Filling", True, f"Ubicación extraída correctamente: {filled_slots}")
                    return True
                else:
                    self.log_test("Basic Slot Filling", False, f"Ubicación no extraída: {filled_slots}")
                    return False
                    
            else:
                self.log_test("Basic Slot Filling", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Basic Slot Filling", False, f"Excepción: {str(e)}")
            return False

    def test_missing_slots_question(self) -> bool:
        """Test de generación de preguntas para slots faltantes"""
        try:
            request_data = {
                "intentId": "consultar_tiempo",
                "userMessage": "¿Qué tiempo hace?",  # Sin ubicación específica
                "sessionId": "test-session-002",
                "currentSlots": {},
                "conversationContext": {}
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/slot-filling/process",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                
                # Debe haber slots faltantes y una pregunta generada
                missing_slots = data.get("missing_slots", [])
                generated_question = data.get("generated_question", "")
                
                if "ubicacion" in missing_slots and generated_question:
                    self.log_test("Missing Slots Question", True, 
                                f"Pregunta generada: '{generated_question}' para slots: {missing_slots}")
                    return True
                else:
                    self.log_test("Missing Slots Question", False, 
                                f"No se generó pregunta. Missing: {missing_slots}, Question: '{generated_question}'")
                    return False
                    
            else:
                self.log_test("Missing Slots Question", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Missing Slots Question", False, f"Excepción: {str(e)}")
            return False

    def test_slot_extraction(self) -> bool:
        """Test de extracción de slot específico"""
        try:
            request_data = {
                "intentId": "encender_luz",
                "slotName": "lugar",
                "userMessage": "Enciende la luz del salón",
                "context": {}
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/slot-filling/extract-slot",
                json=request_data,
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                
                filled_slots = data.get("filled_slots", {})
                if "lugar" in filled_slots and filled_slots["lugar"].lower() == "salón":
                    self.log_test("Slot Extraction", True, f"Lugar extraído correctamente: {filled_slots}")
                    return True
                else:
                    self.log_test("Slot Extraction", False, f"Lugar no extraído: {filled_slots}")
                    return False
                    
            else:
                self.log_test("Slot Extraction", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Slot Extraction", False, f"Excepción: {str(e)}")
            return False

    def test_slot_validation(self) -> bool:
        """Test de validación de completitud de slots"""
        try:
            # Test con slots completos
            request_data = {
                "intentId": "consultar_tiempo",
                "slots": {"ubicacion": "Barcelona"}
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/slot-filling/validate-completeness",
                json=request_data,
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                
                slots_complete = data.get("slots_complete", False)
                if slots_complete:
                    self.log_test("Slot Validation (Complete)", True, f"Slots validados como completos: {data}")
                else:
                    self.log_test("Slot Validation (Complete)", False, f"Slots no validados: {data}")
                    return False
            else:
                self.log_test("Slot Validation (Complete)", False, f"HTTP {response.status_code}")
                return False
            
            # Test con slots incompletos
            request_data = {
                "intentId": "consultar_tiempo",
                "slots": {}
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/slot-filling/validate-completeness",
                json=request_data,
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                
                slots_complete = data.get("slots_complete", True)
                if not slots_complete:
                    self.log_test("Slot Validation (Incomplete)", True, f"Slots validados como incompletos: {data}")
                    return True
                else:
                    self.log_test("Slot Validation (Incomplete)", False, f"Slots incorrectamente validados como completos: {data}")
                    return False
            else:
                self.log_test("Slot Validation (Incomplete)", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Slot Validation", False, f"Excepción: {str(e)}")
            return False

    def test_next_question_generation(self) -> bool:
        """Test de generación de siguiente pregunta"""
        try:
            request_data = {
                "intentId": "consultar_tiempo",
                "lastUserMessage": "¿Qué tiempo hace?",
                "currentSlots": {},
                "context": {}
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/slot-filling/next-question",
                json=request_data,
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                
                has_question = data.get("has_question", False)
                next_question = data.get("next_question", "")
                
                if has_question and next_question:
                    self.log_test("Next Question Generation", True, f"Pregunta generada: '{next_question}'")
                    return True
                else:
                    self.log_test("Next Question Generation", False, f"No se generó pregunta: {data}")
                    return False
                    
            else:
                self.log_test("Next Question Generation", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Next Question Generation", False, f"Excepción: {str(e)}")
            return False

    def test_conversation_integration(self) -> bool:
        """Test de integración con ConversationManager"""
        try:
            # Test usando el endpoint de conversación que debe usar slot-filling
            request_data = {
                "session_id": "test-session-conversation",
                "user_id": "test-user",
                "user_message": "¿Qué tiempo hace?",
                "metadata": {}
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                
                # La respuesta debe incluir una pregunta para obtener ubicación
                system_response = data.get("system_response", "")
                session_state = data.get("session_state", "")
                
                # Verificar que el estado cambió a WAITING_SLOTS y hay una pregunta
                if session_state == "waiting_slots" and system_response:
                    self.log_test("Conversation Integration", True, 
                                f"Estado: {session_state}, Respuesta: '{system_response}'")
                    return True
                else:
                    self.log_test("Conversation Integration", False, 
                                f"Estado: {session_state}, Respuesta: '{system_response}'")
                    return False
                    
            else:
                self.log_test("Conversation Integration", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Conversation Integration", False, f"Excepción: {str(e)}")
            return False

    def test_multi_turn_conversation(self) -> bool:
        """Test de conversación multi-vuelta con slot-filling"""
        try:
            session_id = "test-session-multiturn"
            
            # Primer turno: pregunta sin ubicación
            request_data = {
                "session_id": session_id,
                "user_id": "test-user-multiturn",
                "user_message": "¿Qué tiempo hace?",
                "metadata": {}
            }
            
            response1 = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request_data,
                timeout=15
            )
            
            if response1.status_code != 200:
                self.log_test("Multi-turn Conversation (Turn 1)", False, f"HTTP {response1.status_code}")
                return False
            
            data1 = response1.json()
            if data1.get("session_state") != "waiting_slots":
                self.log_test("Multi-turn Conversation (Turn 1)", False, f"Estado esperado waiting_slots, obtenido: {data1.get('session_state')}")
                return False
            
            # Segundo turno: proporcionar ubicación
            request_data["user_message"] = "Madrid"
            
            response2 = requests.post(
                f"{self.base_url}/api/v1/conversation/process",
                json=request_data,
                timeout=15
            )
            
            if response2.status_code != 200:
                self.log_test("Multi-turn Conversation (Turn 2)", False, f"HTTP {response2.status_code}")
                return False
            
            data2 = response2.json()
            
            # Debe cambiar a executing_tasks o completed
            final_state = data2.get("session_state")
            if final_state in ["executing_tasks", "completed"]:
                self.log_test("Multi-turn Conversation", True, 
                            f"Flujo completo: waiting_slots -> {final_state}")
                return True
            else:
                self.log_test("Multi-turn Conversation", False, 
                            f"Estado final inesperado: {final_state}")
                return False
                
        except Exception as e:
            self.log_test("Multi-turn Conversation", False, f"Excepción: {str(e)}")
            return False

    def test_error_handling(self) -> bool:
        """Test de manejo de errores"""
        try:
            # Test con intent inexistente
            request_data = {
                "intent_id": "intent_inexistente",
                "user_message": "Mensaje de prueba",
                "session_id": "test-error",
                "current_slots": {},
                "conversation_context": {}
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/slot-filling/process",
                json=request_data,
                timeout=10
            )
            
            # Debe manejar el error graciosamente
            if response.status_code in [200, 400]:
                data = response.json()
                
                # Verificar que indica error
                success = data.get("success", True)
                error_message = data.get("error_message", "")
                
                if not success and error_message:
                    self.log_test("Error Handling", True, f"Error manejado correctamente: {error_message}")
                    return True
                else:
                    self.log_test("Error Handling", False, f"Error no manejado correctamente: {data}")
                    return False
            else:
                self.log_test("Error Handling", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Error Handling", False, f"Excepción: {str(e)}")
            return False

    def test_service_test_endpoint(self) -> bool:
        """Test del endpoint de prueba del servicio"""
        try:
            response = requests.post(f"{self.base_url}/api/v1/slot-filling/test", timeout=15)
            
            if response.status_code == 200:
                data = response.json()
                
                test_status = data.get("test_status", "")
                test_result = data.get("test_result", {})
                
                if test_status == "passed" and test_result.get("success", False):
                    self.log_test("Service Test Endpoint", True, f"Test interno pasó: {test_status}")
                    return True
                else:
                    self.log_test("Service Test Endpoint", False, f"Test interno falló: {data}")
                    return False
                    
            else:
                self.log_test("Service Test Endpoint", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Service Test Endpoint", False, f"Excepción: {str(e)}")
            return False

    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de tests"""
        print("=" * 80)
        print("🧪 INICIANDO TESTS DE SLOT-FILLING AUTOMÁTICO (T4.2)")
        print("=" * 80)
        print()
        
        # Lista de tests a ejecutar
        tests = [
            ("Health Check", self.test_health_check),
            ("Statistics", self.test_statistics),
            ("Basic Slot Filling", self.test_basic_slot_filling),
            ("Missing Slots Question", self.test_missing_slots_question),
            ("Slot Extraction", self.test_slot_extraction),
            ("Slot Validation", self.test_slot_validation),
            ("Next Question Generation", self.test_next_question_generation),
            ("Conversation Integration", self.test_conversation_integration),
            ("Multi-turn Conversation", self.test_multi_turn_conversation),
            ("Error Handling", self.test_error_handling),
            ("Service Test Endpoint", self.test_service_test_endpoint)
        ]
        
        # Ejecutar cada test
        for test_name, test_func in tests:
            print(f"\n🧪 Ejecutando: {test_name}")
            try:
                test_func()
            except Exception as e:
                self.log_test(test_name, False, f"Excepción no capturada: {str(e)}")
            
            time.sleep(0.5)  # Pequeña pausa entre tests
        
        # Resumen final
        print("\n" + "=" * 80)
        print("📊 RESUMEN DE TESTS - SLOT-FILLING AUTOMÁTICO")
        print("=" * 80)
        print(f"✅ Tests ejecutados: {self.total_tests}")
        print(f"✅ Tests exitosos: {self.passed_tests}")
        print(f"❌ Tests fallidos: {self.failed_tests}")
        print(f"📈 Tasa de éxito: {(self.passed_tests/self.total_tests*100):.1f}%")
        
        if self.failed_tests > 0:
            print(f"\n❌ TESTS FALLIDOS:")
            for result in self.test_results:
                if not result["success"]:
                    print(f"  - {result['test']}: {result['details']}")
        
        print("\n" + "=" * 80)
        
        # Validar si el sistema está listo para T4.2
        if self.failed_tests == 0:
            print("🎉 TODOS LOS TESTS PASARON - T4.2 SLOT-FILLING IMPLEMENTADO EXITOSAMENTE")
            print("✅ El sistema de slot-filling automático está funcionando correctamente")
            print("✅ Generación dinámica de preguntas operativa")
            print("✅ Integración con ConversationManager exitosa")
            print("✅ Conversaciones multi-vuelta funcionando")
            return True
        else:
            print("❌ ALGUNOS TESTS FALLARON - T4.2 REQUIERE CORRECCIONES")
            print("⚠️  Revisar los errores antes de proceder")
            return False

    def get_detailed_results(self) -> Dict[str, Any]:
        """Obtiene resultados detallados de los tests"""
        return {
            "total_tests": self.total_tests,
            "passed_tests": self.passed_tests,
            "failed_tests": self.failed_tests,
            "success_rate": (self.passed_tests / self.total_tests * 100) if self.total_tests > 0 else 0,
            "test_results": self.test_results,
            "overall_success": self.failed_tests == 0
        }


def main():
    """Función principal"""
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    else:
        base_url = "http://localhost:9904"
    
    print(f"🎯 Iniciando tests de Slot-Filling para: {base_url}")
    
    tester = SlotFillingTester(base_url)
    
    # Esperar a que el servicio esté disponible
    if not tester.wait_for_service():
        print("❌ No se pudo conectar al servicio. Abortando tests.")
        sys.exit(1)
    
    # Ejecutar tests
    success = tester.run_complete_test_suite()
    
    # Salir con código apropiado
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()