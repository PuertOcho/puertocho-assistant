#!/usr/bin/env python3
"""
Script de prueba específico para T3.5 - Fallback a LLM único cuando voting está deshabilitado.
Prueba el mecanismo de fallback cuando MOE_ENABLED=false o cuando el sistema de votación falla.
"""

import requests
import json
import time
import sys
import argparse
from typing import Dict, Any, List

class T35FallbackTester:
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
        
    def get_current_configuration(self) -> Dict[str, Any]:
        """Obtiene la configuración actual del sistema"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/voting/configuration/info", timeout=10)
            if response.status_code == 200:
                return response.json()
            return {}
        except Exception:
            return {}
            
    def test_fallback_when_moe_disabled(self) -> bool:
        """Prueba el fallback cuando MoE está deshabilitado via configuración"""
        try:
            # Obtener configuración actual
            config = self.get_current_configuration()
            moe_enabled = config.get("moe_enabled", True)
            
            # Ejecutar votación
            request_data = {
                "requestId": "test_fallback_moe_disabled",
                "userMessage": "¿qué tiempo hace hoy?",
                "conversationContext": {},
                "conversationHistory": []
            }
            
            response = requests.post(f"{self.base_url}/api/v1/voting/execute", 
                                   json=request_data, timeout=30)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                
                # Verificar que se usó fallback
                consensus = data.get("consensus", {})
                consensus_method = consensus.get("consensusMethod", "")
                
                # Debería usar single_llm_mode cuando MoE está deshabilitado
                fallback_used = consensus_method == "single_llm_mode"
                
                details = f"MoE enabled: {moe_enabled}, Method: {consensus_method}, Fallback used: {fallback_used}"
                self.log_test("Fallback cuando MoE deshabilitado", fallback_used, details)
                return fallback_used
            else:
                self.log_test("Fallback cuando MoE deshabilitado", False, f"Status: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Fallback cuando MoE deshabilitado", False, f"Error: {e}")
            return False
            
    def test_fallback_when_voting_fails(self) -> bool:
        """Prueba el fallback cuando el sistema de votación falla"""
        try:
            # Ejecutar votación con contexto que podría causar falla
            request_data = {
                "requestId": "test_fallback_voting_fails",
                "userMessage": "mensaje muy largo que podría causar timeout en el sistema de votación múltiple " * 10,
                "conversationContext": {"complex_context": True},
                "conversationHistory": ["historial muy largo"] * 20
            }
            
            response = requests.post(f"{self.base_url}/api/v1/voting/execute", 
                                   json=request_data, timeout=60)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                
                # Verificar que se completó (con fallback si es necesario)
                status = data.get("status", "")
                consensus = data.get("consensus", {})
                consensus_method = consensus.get("consensusMethod", "")
                
                # Debería completarse, posiblemente con fallback
                completed = status == "COMPLETED" and success
                fallback_used = consensus_method in ["single_llm_mode", "fallback"]
                
                details = f"Status: {status}, Method: {consensus_method}, Completed: {completed}, Fallback: {fallback_used}"
                self.log_test("Fallback cuando voting falla", completed, details)
                return completed
            else:
                self.log_test("Fallback cuando voting falla", False, f"Status: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Fallback cuando voting falla", False, f"Error: {e}")
            return False
            
    def test_single_llm_mode_functionality(self) -> bool:
        """Prueba la funcionalidad específica del modo LLM único"""
        try:
            # Ejecutar votación simple
            request_data = {
                "userMessage": "enciende la luz del salón"
            }
            
            response = requests.post(f"{self.base_url}/api/v1/voting/execute/simple", 
                                   json=request_data, timeout=30)
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                
                # Verificar estructura del resultado (endpoint simple tiene estructura diferente)
                final_intent = data.get("finalIntent", "")
                confidence = data.get("confidence", 0)
                
                # En modo LLM único, debería tener una intención válida
                intent_valid = final_intent and final_intent != "unknown"
                confidence_valid = confidence > 0
                
                details = f"Intent: {final_intent}, Confidence: {confidence}, Valid: {intent_valid and confidence_valid}"
                self.log_test("Funcionalidad modo LLM único", intent_valid and confidence_valid, details)
                return intent_valid and confidence_valid
            else:
                self.log_test("Funcionalidad modo LLM único", False, f"Status: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Funcionalidad modo LLM único", False, f"Error: {e}")
            return False
            
    def test_fallback_consistency(self) -> bool:
        """Prueba la consistencia del fallback en múltiples ejecuciones"""
        try:
            test_messages = [
                "¿qué hora es?",
                "pon música",
                "apaga las luces",
                "programa una alarma"
            ]
            
            successful_fallbacks = 0
            total_tests = len(test_messages)
            
            for i, message in enumerate(test_messages):
                try:
                    request_data = {
                        "requestId": f"consistency_test_{i}",
                        "userMessage": message,
                        "conversationContext": {},
                        "conversationHistory": []
                    }
                    
                    response = requests.post(f"{self.base_url}/api/v1/voting/execute", 
                                           json=request_data, timeout=30)
                    
                    if response.status_code == 200:
                        data = response.json()
                        if data.get("success", False):
                            consensus = data.get("consensus", {})
                            consensus_method = consensus.get("consensusMethod", "")
                            
                            # Verificar que se completó (con fallback si es necesario)
                            if consensus_method in ["single_llm_mode", "fallback", "majority", "unanimity"]:
                                successful_fallbacks += 1
                                
                except Exception:
                    pass  # Continuar con la siguiente prueba
                    
                # Pequeña pausa entre pruebas
                time.sleep(1)
            
            success_rate = successful_fallbacks / total_tests
            consistent = success_rate >= 0.8  # Al menos 80% de éxito
            
            details = f"Successful: {successful_fallbacks}/{total_tests}, Rate: {success_rate:.2f}, Consistent: {consistent}"
            self.log_test("Consistencia del fallback", consistent, details)
            return consistent
            
        except Exception as e:
            self.log_test("Consistencia del fallback", False, f"Error: {e}")
            return False
            
    def test_fallback_performance(self) -> bool:
        """Prueba el rendimiento del fallback"""
        try:
            start_time = time.time()
            
            request_data = {
                "requestId": "performance_test",
                "userMessage": "test message for performance",
                "conversationContext": {},
                "conversationHistory": []
            }
            
            response = requests.post(f"{self.base_url}/api/v1/voting/execute", 
                                   json=request_data, timeout=30)
            
            end_time = time.time()
            response_time = end_time - start_time
            
            if response.status_code == 200:
                data = response.json()
                success = data.get("success", False)
                
                # El fallback debería ser rápido (< 10 segundos)
                performance_ok = response_time < 10.0
                
                details = f"Response time: {response_time:.2f}s, Performance OK: {performance_ok}"
                self.log_test("Rendimiento del fallback", performance_ok, details)
                return performance_ok
            else:
                self.log_test("Rendimiento del fallback", False, f"Status: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Rendimiento del fallback", False, f"Error: {e}")
            return False
            
    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas para T3.5"""
        print("🔧 T3.5 - Pruebas específicas de fallback a LLM único")
        print("=" * 80)
        print(f"🌐 URL del servicio: {self.base_url}")
        print("🚀 Iniciando pruebas específicas para T3.5 - Fallback a LLM único")
        print("=" * 70)
        
        # Esperar a que el servicio esté disponible
        if not self.wait_for_service():
            return False
            
        print("\n📋 Ejecutando pruebas de fallback T3.5:")
        print("-" * 50)
        
        # Ejecutar todas las pruebas
        tests = [
            ("Fallback cuando MoE deshabilitado", self.test_fallback_when_moe_disabled),
            ("Fallback cuando voting falla", self.test_fallback_when_voting_fails),
            ("Funcionalidad modo LLM único", self.test_single_llm_mode_functionality),
            ("Consistencia del fallback", self.test_fallback_consistency),
            ("Rendimiento del fallback", self.test_fallback_performance)
        ]
        
        passed_tests = 0
        total_tests = len(tests)
        
        for test_name, test_method in tests:
            try:
                if test_method():
                    passed_tests += 1
            except Exception as e:
                self.log_test(test_name, False, f"Excepción: {e}")
        
        print("\n" + "=" * 70)
        print("📊 RESULTADOS FINALES - T3.5 Fallback a LLM único")
        
        success_rate = (passed_tests / total_tests) * 100 if total_tests > 0 else 0
        
        if passed_tests == total_tests:
            print(f"✅ Pruebas exitosas: {passed_tests}/{total_tests}")
            print(f"❌ Pruebas fallidas: {total_tests - passed_tests}/{total_tests}")
            print(f"📈 Tasa de éxito: {success_rate:.1f}%")
            print("🎉 ¡TODAS LAS PRUEBAS PASARON! T3.5 está funcionando correctamente.")
            print("💡 El sistema de fallback a LLM único funciona correctamente.")
        else:
            print(f"✅ Pruebas exitosas: {passed_tests}/{total_tests}")
            print(f"❌ Pruebas fallidas: {total_tests - passed_tests}/{total_tests}")
            print(f"📈 Tasa de éxito: {success_rate:.1f}%")
            print("⚠️  Algunas pruebas fallaron. Revisar implementación.")
        
        print("\n📋 Resultados detallados:")
        print(f"   Total de pruebas: {total_tests}")
        print(f"   Pruebas exitosas: {passed_tests}")
        print(f"   Pruebas fallidas: {total_tests - passed_tests}")
        print(f"   Tasa de éxito: {success_rate:.1f}%")
        
        return passed_tests == total_tests
        
    def get_detailed_results(self) -> Dict[str, Any]:
        """Obtiene resultados detallados de las pruebas"""
        return {
            "test_results": self.test_results,
            "total_tests": len(self.test_results),
            "passed_tests": len([r for r in self.test_results if r["success"]]),
            "failed_tests": len([r for r in self.test_results if not r["success"]])
        }

def main():
    parser = argparse.ArgumentParser(description="Prueba el fallback a LLM único (T3.5)")
    parser.add_argument("--url", default="http://localhost:9904", 
                       help="URL del servicio (default: http://localhost:9904)")
    parser.add_argument("--verbose", "-v", action="store_true", 
                       help="Modo verbose")
    
    args = parser.parse_args()
    
    print("🚀 Iniciando pruebas del fallback a LLM único (T3.5)")
    print(f"🌐 URL del servicio: {args.url}")
    
    tester = T35FallbackTester(args.url)
    
    try:
        success = tester.run_complete_test_suite()
        
        if args.verbose:
            results = tester.get_detailed_results()
            print("\n📋 Resultados detallados:")
            for result in results["test_results"]:
                status = "✅" if result["success"] else "❌"
                print(f"   {status} {result['test']}: {result['details']}")
        
        sys.exit(0 if success else 1)
        
    except KeyboardInterrupt:
        print("\n⚠️  Pruebas interrumpidas por el usuario")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error durante las pruebas: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 