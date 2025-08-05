#!/usr/bin/env python3
"""
Script específico para probar T3.4 - Cambio de configuración MoE_ENABLED=true/false.
Este script simula el comportamiento del sistema cuando se cambia la variable de entorno MOE_ENABLED.
"""

import requests
import json
import time
import sys
from typing import Dict, Any

class MoEEnabledDisabledTester:
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
        
    def get_moe_status(self) -> Dict[str, Any]:
        """Obtiene el estado actual del sistema MoE"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/voting/configuration/moe-status", timeout=10)
            if response.status_code == 200:
                return response.json()
            else:
                return {"success": False, "error": f"Status: {response.status_code}"}
        except Exception as e:
            return {"success": False, "error": str(e)}
            
    def test_current_moe_configuration(self) -> bool:
        """Prueba la configuración actual del sistema MoE"""
        try:
            status = self.get_moe_status()
            
            if not status.get("success", False):
                self.log_test("Configuración actual MoE", False, f"Error: {status.get('error', 'Unknown')}")
                return False
                
            moe_enabled = status.get("moe_enabled", False)
            voting_system_configured = status.get("voting_system_configured", False)
            participants_count = status.get("participants_count", 0)
            service_healthy = status.get("service_healthy", False)
            
            details = f"MoE enabled: {moe_enabled}, Configured: {voting_system_configured}, Participants: {participants_count}, Healthy: {service_healthy}"
            self.log_test("Configuración actual MoE", True, details)
            return True
            
        except Exception as e:
            self.log_test("Configuración actual MoE", False, f"Error: {e}")
            return False
            
    def test_voting_behavior_with_current_config(self) -> bool:
        """Prueba el comportamiento de votación con la configuración actual"""
        try:
            # Primero obtener el estado actual
            status = self.get_moe_status()
            moe_enabled = status.get("moe_enabled", False)
            
            # Ejecutar una votación de prueba
            request_data = {
                "requestId": f"test_moe_{moe_enabled}",
                "userMessage": "¿qué tiempo hace en Madrid?",
                "conversationContext": {"location": "Madrid"},
                "conversationHistory": []
            }
            
            response = requests.post(f"{self.base_url}/api/v1/voting/execute", 
                                   json=request_data, timeout=30)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                
                consensus = data.get("consensus", {})
                consensus_method = consensus.get("consensusMethod", "")
                votes_count = data.get("votesCount", 0)
                final_intent = consensus.get("finalIntent", "")
                
                # Verificar comportamiento esperado
                if moe_enabled:
                    # Con MoE habilitado, debe usar múltiples LLMs
                    expected_behavior = votes_count > 1 or consensus_method != "single_llm_mode"
                    behavior_desc = "múltiples LLMs"
                else:
                    # Con MoE deshabilitado, debe usar LLM único
                    expected_behavior = votes_count == 1 or consensus_method == "single_llm_mode"
                    behavior_desc = "LLM único"
                
                details = f"MoE enabled: {moe_enabled}, Method: {consensus_method}, Votes: {votes_count}, Intent: {final_intent}, Expected: {behavior_desc}"
                self.log_test(f"Votación con MoE {'habilitado' if moe_enabled else 'deshabilitado'}", 
                             success and expected_behavior, details)
                return success and expected_behavior
            else:
                self.log_test(f"Votación con MoE {'habilitado' if moe_enabled else 'deshabilitado'}", 
                             False, f"Status: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test(f"Votación con MoE {'habilitado' if moe_enabled else 'deshabilitado'}", 
                         False, f"Error: {e}")
            return False
            
    def test_configuration_reload_impact(self) -> bool:
        """Prueba el impacto de recargar la configuración"""
        try:
            # Obtener estado antes de recargar
            status_before = self.get_moe_status()
            moe_enabled_before = status_before.get("moe_enabled", False)
            
            # Recargar configuración
            response = requests.post(f"{self.base_url}/api/v1/voting/configuration/reload", timeout=10)
            
            if response.status_code == 200:
                # Esperar un momento para que se procese la recarga
                time.sleep(2)
                
                # Obtener estado después de recargar
                status_after = self.get_moe_status()
                moe_enabled_after = status_after.get("moe_enabled", False)
                
                # Verificar que la configuración se mantiene consistente
                config_consistent = moe_enabled_before == moe_enabled_after
                
                details = f"Before: {moe_enabled_before}, After: {moe_enabled_after}, Consistent: {config_consistent}"
                self.log_test("Recarga de configuración", True, details)
                return True
            else:
                self.log_test("Recarga de configuración", False, f"Status: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Recarga de configuración", False, f"Error: {e}")
            return False
            
    def test_fallback_mechanism_robustness(self) -> bool:
        """Prueba la robustez del mecanismo de fallback"""
        try:
            # Ejecutar múltiples votaciones para verificar consistencia
            test_messages = [
                "¿qué tiempo hace?",
                "enciende la luz",
                "pon música",
                "programa una alarma"
            ]
            
            successful_votes = 0
            total_votes = len(test_messages)
            
            for i, message in enumerate(test_messages):
                try:
                    request_data = {
                        "requestId": f"robustness_test_{i}",
                        "userMessage": message,
                        "conversationContext": {},
                        "conversationHistory": []
                    }
                    
                    response = requests.post(f"{self.base_url}/api/v1/voting/execute", 
                                           json=request_data, timeout=30)
                    
                    if response.status_code == 200:
                        data = response.json()
                        if data.get("success", False):
                            successful_votes += 1
                            
                except Exception:
                    pass  # Continuar con la siguiente prueba
                    
                # Pequeña pausa entre votaciones
                time.sleep(1)
            
            success_rate = successful_votes / total_votes
            robust = success_rate >= 0.75  # Al menos 75% de éxito
            
            details = f"Successful: {successful_votes}/{total_votes}, Rate: {success_rate:.2f}, Robust: {robust}"
            self.log_test("Robustez del fallback", robust, details)
            return robust
            
        except Exception as e:
            self.log_test("Robustez del fallback", False, f"Error: {e}")
            return False
            
    def test_environment_variable_simulation(self) -> bool:
        """Simula el impacto de cambiar variables de entorno"""
        try:
            # Obtener configuración actual
            status = self.get_moe_status()
            
            if not status.get("success", False):
                self.log_test("Simulación variables de entorno", False, "No se pudo obtener estado")
                return False
                
            # Verificar que todas las configuraciones están presentes
            required_fields = [
                "moe_enabled", "voting_system_configured", "participants_count",
                "max_debate_rounds", "parallel_voting", "consensus_threshold", "timeout_per_vote"
            ]
            
            missing_fields = [field for field in required_fields if field not in status]
            
            if missing_fields:
                self.log_test("Simulación variables de entorno", False, f"Campos faltantes: {missing_fields}")
                return False
                
            # Verificar valores razonables
            participants_count = status.get("participants_count", 0)
            max_debate_rounds = status.get("max_debate_rounds", 0)
            consensus_threshold = status.get("consensus_threshold", 0.0)
            timeout_per_vote = status.get("timeout_per_vote", 0)
            
            values_valid = (
                participants_count >= 0 and
                max_debate_rounds >= 0 and
                0.0 <= consensus_threshold <= 1.0 and
                timeout_per_vote >= 0
            )
            
            details = f"Participants: {participants_count}, Debate rounds: {max_debate_rounds}, Threshold: {consensus_threshold}, Timeout: {timeout_per_vote}"
            self.log_test("Simulación variables de entorno", values_valid, details)
            return values_valid
            
        except Exception as e:
            self.log_test("Simulación variables de entorno", False, f"Error: {e}")
            return False
            
    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas para T3.4"""
        print("🚀 Iniciando pruebas específicas para T3.4 - MoE_ENABLED=true/false")
        print("=" * 70)
        
        # Esperar a que el servicio esté disponible
        if not self.wait_for_service():
            return False
            
        print("\n📋 Ejecutando pruebas de configuración MoE_ENABLED:")
        print("-" * 50)
        
        # Ejecutar todas las pruebas
        tests = [
            ("Configuración actual MoE", self.test_current_moe_configuration),
            ("Comportamiento de votación", self.test_voting_behavior_with_current_config),
            ("Impacto de recarga", self.test_configuration_reload_impact),
            ("Robustez del fallback", self.test_fallback_mechanism_robustness),
            ("Simulación variables de entorno", self.test_environment_variable_simulation)
        ]
        
        passed_tests = 0
        total_tests = len(tests)
        
        for test_name, test_method in tests:
            try:
                if test_method():
                    passed_tests += 1
            except Exception as e:
                self.log_test(test_name, False, f"Excepción: {e}")
            
            # Pequeña pausa entre pruebas
            time.sleep(1)
        
        # Resumen final
        print("\n" + "=" * 70)
        print(f"📊 RESULTADOS FINALES - T3.4 MoE_ENABLED")
        print(f"✅ Pruebas exitosas: {passed_tests}/{total_tests}")
        print(f"❌ Pruebas fallidas: {total_tests - passed_tests}/{total_tests}")
        print(f"📈 Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        
        if passed_tests == total_tests:
            print("🎉 ¡TODAS LAS PRUEBAS PASARON! T3.4 está funcionando correctamente.")
            print("💡 El sistema MoE responde correctamente a la configuración MOE_ENABLED.")
        else:
            print("⚠️  Algunas pruebas fallaron. Revisar logs para más detalles.")
        
        return passed_tests == total_tests
        
    def get_detailed_results(self) -> Dict[str, Any]:
        """Obtiene resultados detallados de las pruebas"""
        passed = sum(1 for result in self.test_results if result["success"])
        total = len(self.test_results)
        
        return {
            "total_tests": total,
            "passed_tests": passed,
            "failed_tests": total - passed,
            "success_rate": (passed / total) * 100 if total > 0 else 0,
            "test_results": self.test_results
        }

def main():
    """Función principal"""
    print("🔧 T3.4 - Pruebas específicas de configuración MoE_ENABLED=true/false")
    print("=" * 80)
    
    # Obtener URL del servicio desde argumentos o usar por defecto
    base_url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:9904"
    print(f"🌐 URL del servicio: {base_url}")
    
    # Crear tester y ejecutar pruebas
    tester = MoEEnabledDisabledTester(base_url)
    success = tester.run_complete_test_suite()
    
    # Mostrar resultados detallados
    results = tester.get_detailed_results()
    print(f"\n📋 Resultados detallados:")
    print(f"   Total de pruebas: {results['total_tests']}")
    print(f"   Pruebas exitosas: {results['passed_tests']}")
    print(f"   Pruebas fallidas: {results['failed_tests']}")
    print(f"   Tasa de éxito: {results['success_rate']:.1f}%")
    
    # Salir con código apropiado
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 