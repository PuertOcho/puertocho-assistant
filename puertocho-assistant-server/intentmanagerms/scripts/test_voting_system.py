#!/usr/bin/env python3
"""
Script de prueba para el sistema de votación MoE (Mixture of Experts).
Prueba todos los endpoints del sistema de votación y valida su funcionamiento.
"""

import requests
import json
import time
import sys
from typing import Dict, Any, List

class VotingSystemTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.test_results = []
        
    def log_test(self, test_name: str, success: bool, details: str = ""):
        """Registra el resultado de una prueba"""
        status = "✅ PASÓ" if success else "❌ FALLÓ"
        print(f"{status} {test_name}")
        if details:
            print(f"   {details}")
        
        self.test_results.append({
            "test": test_name,
            "success": success,
            "details": details
        })
        
    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        """Espera a que el servicio esté disponible"""
        print("🔄 Esperando a que el servicio esté disponible...")
        
        for attempt in range(max_attempts):
            try:
                response = requests.get(f"{self.base_url}/api/v1/voting/health", timeout=5)
                if response.status_code == 200:
                    print("✅ Servicio disponible")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            if attempt < max_attempts - 1:
                print(f"   Intento {attempt + 1}/{max_attempts}, esperando {delay}s...")
                time.sleep(delay)
        
        print("❌ Servicio no disponible después de todos los intentos")
        return False
        
    def test_service_availability(self) -> bool:
        """Prueba la disponibilidad del servicio"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/voting/health", timeout=10)
            success = response.status_code == 200
            details = f"Status: {response.status_code}"
            self.log_test("Verificación de disponibilidad", success, details)
            return success
        except Exception as e:
            self.log_test("Verificación de disponibilidad", False, f"Error: {e}")
            return False
            
    def test_health_check(self) -> bool:
        """Prueba el health check del sistema de votación"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/voting/health", timeout=10)
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False) and data.get("overall") == "HEALTHY"
                details = f"Overall: {data.get('overall')}, Voting: {data.get('voting_service')}, Config: {data.get('configuration_service')}"
                self.log_test("Health check del sistema de votación", success, details)
                return success
            else:
                self.log_test("Health check del sistema de votación", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Health check del sistema de votación", False, f"Error: {e}")
            return False
            
    def test_statistics(self) -> bool:
        """Prueba las estadísticas del sistema de votación"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/voting/statistics", timeout=10)
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                details = f"MoE enabled: {data.get('moe_enabled')}, Parallel voting: {data.get('parallel_voting')}, Active rounds: {data.get('active_rounds')}"
                self.log_test("Estadísticas del sistema de votación", success, details)
                return success
            else:
                self.log_test("Estadísticas del sistema de votación", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Estadísticas del sistema de votación", False, f"Error: {e}")
            return False
            
    def test_configuration_statistics(self) -> bool:
        """Prueba las estadísticas de configuración"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/voting/configuration/statistics", timeout=10)
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                details = f"Config loaded: {data.get('configuration_loaded')}, Hot reload: {data.get('hot_reload_enabled')}, Participants: {data.get('llm_participants_count')}"
                self.log_test("Estadísticas de configuración", success, details)
                return success
            else:
                self.log_test("Estadísticas de configuración", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Estadísticas de configuración", False, f"Error: {e}")
            return False
            
    def test_configuration_info(self) -> bool:
        """Prueba la información de configuración"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/voting/configuration/info", timeout=10)
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                details = f"Version: {data.get('version')}, Description: {data.get('description')}"
                self.log_test("Información de configuración", success, details)
                return success
            else:
                self.log_test("Información de configuración", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Información de configuración", False, f"Error: {e}")
            return False
            
    def test_simple_voting(self) -> bool:
        """Prueba la votación simple"""
        try:
            test_message = "¿qué tiempo hace en Madrid?"
            payload = {"userMessage": test_message}
            
            response = requests.post(f"{self.base_url}/api/v1/voting/execute/simple", 
                                   json=payload, timeout=30)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                details = f"Intent: {data.get('finalIntent')}, Confidence: {data.get('confidence')}, Agreement: {data.get('agreementLevel')}"
                self.log_test("Votación simple", success, details)
                return success
            else:
                self.log_test("Votación simple", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Votación simple", False, f"Error: {e}")
            return False
            
    def test_advanced_voting(self) -> bool:
        """Prueba la votación avanzada con contexto"""
        try:
            payload = {
                "requestId": "test_advanced_001",
                "userMessage": "Enciende la luz del salón",
                "conversationContext": {
                    "location": "casa",
                    "time": "noche",
                    "device": "speaker"
                },
                "conversationHistory": [
                    "Hola, ¿cómo estás?",
                    "Bien, gracias. ¿Puedes ayudarme con las luces?"
                ]
            }
            
            response = requests.post(f"{self.base_url}/api/v1/voting/execute", 
                                   json=payload, timeout=30)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                details = f"Round ID: {data.get('roundId')}, Status: {data.get('status')}, Duration: {data.get('durationMs')}ms"
                self.log_test("Votación avanzada", success, details)
                return success
            else:
                self.log_test("Votación avanzada", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Votación avanzada", False, f"Error: {e}")
            return False
            
    def test_voting_test_endpoint(self) -> bool:
        """Prueba el endpoint de test del sistema"""
        try:
            response = requests.post(f"{self.base_url}/api/v1/voting/test", timeout=30)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                details = f"Test message: {data.get('testMessage')}, Final intent: {data.get('finalIntent')}, Valid votes: {data.get('validVotes')}"
                self.log_test("Endpoint de test", success, details)
                return success
            else:
                self.log_test("Endpoint de test", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Endpoint de test", False, f"Error: {e}")
            return False
            
    def test_custom_test_message(self) -> bool:
        """Prueba el endpoint de test con mensaje personalizado"""
        try:
            payload = {"testMessage": "Crea un issue en GitHub sobre el bug del weather"}
            
            response = requests.post(f"{self.base_url}/api/v1/voting/test", 
                                   json=payload, timeout=30)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                details = f"Custom message: {data.get('testMessage')}, Intent: {data.get('finalIntent')}, Confidence: {data.get('confidence')}"
                self.log_test("Test con mensaje personalizado", success, details)
                return success
            else:
                self.log_test("Test con mensaje personalizado", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Test con mensaje personalizado", False, f"Error: {e}")
            return False
            
    def test_configuration_reload(self) -> bool:
        """Prueba la recarga de configuración"""
        try:
            response = requests.post(f"{self.base_url}/api/v1/voting/configuration/reload", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                details = f"Message: {data.get('message')}"
                self.log_test("Recarga de configuración", success, details)
                return success
            else:
                self.log_test("Recarga de configuración", False, f"Status: {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Recarga de configuración", False, f"Error: {e}")
            return False
            
    def test_error_handling(self) -> bool:
        """Prueba el manejo de errores"""
        try:
            # Prueba con mensaje vacío
            payload = {"userMessage": ""}
            response = requests.post(f"{self.base_url}/api/v1/voting/execute/simple", 
                                   json=payload, timeout=10)
            
            success = response.status_code == 400
            details = f"Empty message test - Status: {response.status_code}"
            
            # Prueba con payload inválido
            response2 = requests.post(f"{self.base_url}/api/v1/voting/execute/simple", 
                                    json={"invalid": "payload"}, timeout=10)
            
            success = success and response2.status_code == 400
            details += f", Invalid payload test - Status: {response2.status_code}"
            
            self.log_test("Manejo de errores", success, details)
            return success
        except Exception as e:
            self.log_test("Manejo de errores", False, f"Error: {e}")
            return False
            
    def test_performance(self) -> bool:
        """Prueba el rendimiento del sistema"""
        try:
            start_time = time.time()
            
            # Ejecutar múltiples votaciones simples
            test_messages = [
                "¿qué tiempo hace?",
                "enciende la luz",
                "pon música",
                "programa una alarma",
                "crea un issue"
            ]
            
            all_success = True
            total_duration = 0
            
            for i, message in enumerate(test_messages):
                payload = {"userMessage": message}
                response = requests.post(f"{self.base_url}/api/v1/voting/execute/simple", 
                                       json=payload, timeout=30)
                
                if response.status_code == 200:
                    data = response.json()
                    duration = data.get("durationMs", 0)
                    total_duration += duration
                    print(f"   Test {i+1}: {message} - {duration}ms")
                else:
                    all_success = False
                    print(f"   Test {i+1}: {message} - FAILED")
            
            end_time = time.time()
            total_time = end_time - start_time
            avg_duration = total_duration / len(test_messages) if test_messages else 0
            
            details = f"Total time: {total_time:.2f}s, Avg duration: {avg_duration:.0f}ms, Success rate: {len([m for m in test_messages if all_success])}/{len(test_messages)}"
            
            self.log_test("Prueba de rendimiento", all_success, details)
            return all_success
        except Exception as e:
            self.log_test("Prueba de rendimiento", False, f"Error: {e}")
            return False
            
    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas"""
        print("🚀 Iniciando pruebas del sistema de votación MoE")
        print("=" * 60)
        
        # Esperar a que el servicio esté disponible
        if not self.wait_for_service():
            return False
            
        print("\n📋 Ejecutando pruebas...")
        print("-" * 40)
        
        # Pruebas básicas
        self.test_service_availability()
        self.test_health_check()
        self.test_statistics()
        self.test_configuration_statistics()
        self.test_configuration_info()
        
        # Pruebas de votación
        self.test_simple_voting()
        self.test_advanced_voting()
        self.test_voting_test_endpoint()
        self.test_custom_test_message()
        
        # Pruebas de configuración
        self.test_configuration_reload()
        
        # Pruebas de manejo de errores y rendimiento
        self.test_error_handling()
        self.test_performance()
        
        # Resumen de resultados
        print("\n📊 Resumen de resultados:")
        print("-" * 40)
        
        passed = sum(1 for result in self.test_results if result["success"])
        total = len(self.test_results)
        
        print(f"✅ Pruebas exitosas: {passed}/{total}")
        print(f"❌ Pruebas fallidas: {total - passed}/{total}")
        print(f"📈 Tasa de éxito: {(passed/total)*100:.1f}%")
        
        if passed == total:
            print("\n🎉 ¡Todas las pruebas pasaron exitosamente!")
        else:
            print("\n⚠️  Algunas pruebas fallaron:")
            for result in self.test_results:
                if not result["success"]:
                    print(f"   - {result['test']}: {result['details']}")
        
        return passed == total
        
    def get_detailed_results(self) -> Dict[str, Any]:
        """Obtiene resultados detallados de las pruebas"""
        passed = sum(1 for result in self.test_results if result["success"])
        total = len(self.test_results)
        
        return {
            "total_tests": total,
            "passed_tests": passed,
            "failed_tests": total - passed,
            "success_rate": (passed/total)*100 if total > 0 else 0,
            "results": self.test_results
        }

def main():
    """Función principal"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Prueba el sistema de votación MoE")
    parser.add_argument("--url", default="http://localhost:9904", 
                       help="URL base del servicio (default: http://localhost:9904)")
    parser.add_argument("--timeout", type=int, default=30,
                       help="Timeout para las pruebas (default: 30)")
    
    args = parser.parse_args()
    
    print(f"🎯 Probando sistema de votación MoE en: {args.url}")
    print(f"⏱️  Timeout: {args.timeout} segundos")
    
    tester = VotingSystemTester(args.url)
    success = tester.run_complete_test_suite()
    
    # Retornar código de salida apropiado
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 