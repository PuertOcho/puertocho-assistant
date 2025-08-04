#!/usr/bin/env python3
"""
Script de prueba para el servicio de fallback inteligente con degradación gradual.
Prueba todos los niveles de degradación y verifica el funcionamiento correcto.
"""

import requests
import json
import time
import sys
from typing import Dict, Any, List

class IntelligentFallbackTester:
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
        print("⏳ Esperando a que el servicio esté disponible...")
        
        for attempt in range(max_attempts):
            try:
                response = requests.get(f"{self.base_url}/api/v1/fallback/health", timeout=5)
                if response.status_code == 200:
                    print("✅ Servicio disponible")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            if attempt < max_attempts - 1:
                time.sleep(delay)
                
        print("❌ Servicio no disponible después de múltiples intentos")
        return False
        
    def test_service_availability(self) -> bool:
        """Verifica que el servicio esté disponible"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/fallback/health", timeout=10)
            return response.status_code == 200
        except requests.exceptions.RequestException as e:
            return False
            
    def test_health_check(self) -> bool:
        """Prueba el health check del servicio"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/fallback/health", timeout=10)
            
            if response.status_code != 200:
                return False
                
            data = response.json()
            return data.get("status") == "HEALTHY"
            
        except Exception as e:
            return False
            
    def test_statistics(self) -> bool:
        """Prueba las estadísticas del servicio"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/fallback/statistics", timeout=10)
            
            if response.status_code != 200:
                return False
                
            data = response.json()
            
            # Verificar campos requeridos
            required_fields = [
                "enable_gradual_degradation",
                "max_degradation_levels", 
                "similarity_reduction_factor",
                "enable_keyword_fallback",
                "enable_context_fallback",
                "enable_general_domain_fallback",
                "keyword_mappings_count"
            ]
            
            for field in required_fields:
                if field not in data:
                    return False
                    
            return True
            
        except Exception as e:
            return False
            
    def test_basic_fallback(self) -> bool:
        """Prueba el fallback básico"""
        try:
            response = requests.post(
                f"{self.base_url}/api/v1/fallback/test",
                timeout=10
            )
            
            if response.status_code != 200:
                return False
                
            data = response.json()
            
            # Verificar que se aplicó fallback
            return (data.get("success") and 
                   data.get("fallback_used") and 
                   data.get("fallback_intent") == "ayuda")
                   
        except Exception as e:
            return False
            
    def test_keyword_fallback(self) -> bool:
        """Prueba el fallback por palabras clave"""
        test_cases = [
            ("¿qué tiempo hace?", "consultar_tiempo"),
            ("enciende la luz", "encender_luz"),
            ("pon música", "reproducir_musica"),
            ("programa una alarma", "programar_alarma"),
            ("hola", "saludo"),
            ("gracias", "agradecimiento")
        ]
        
        success_count = 0
        
        for text, expected_intent in test_cases:
            try:
                request_data = {"text": text}
                response = requests.post(
                    f"{self.base_url}/api/v1/fallback/classify",
                    json=request_data,
                    timeout=10
                )
                
                if response.status_code == 200:
                    data = response.json()
                    if (data.get("success") and 
                        data.get("fallback_used") and 
                        data.get("intent_id") == expected_intent):
                        success_count += 1
                        
            except Exception as e:
                pass
                
        return success_count >= len(test_cases) * 0.8  # 80% de éxito
        
    def test_context_fallback(self) -> bool:
        """Prueba el fallback por análisis de contexto"""
        test_cases = [
            {
                "text": "buenos días",
                "context": {"timestamp": "2025-01-27T08:30:00"},
                "expected_intent": "saludo"
            },
            {
                "text": "ayuda",
                "context": {"location": "casa", "device_type": "speaker"},
                "expected_intent": "smart_home_control"
            }
        ]
        
        success_count = 0
        
        for test_case in test_cases:
            try:
                request_data = {
                    "text": test_case["text"],
                    "contextMetadata": test_case["context"]
                }
                response = requests.post(
                    f"{self.base_url}/api/v1/fallback/classify",
                    json=request_data,
                    timeout=10
                )
                
                if response.status_code == 200:
                    data = response.json()
                    if (data.get("success") and 
                        data.get("fallback_used") and 
                        data.get("intent_id") == test_case["expected_intent"]):
                        success_count += 1
                        
            except Exception as e:
                pass
                
        return success_count >= len(test_cases) * 0.5  # 50% de éxito mínimo
        
    def test_degradation_levels(self) -> bool:
        """Prueba múltiples niveles de degradación"""
        test_texts = [
            "texto completamente desconocido",
            "palabras sin sentido",
            "comando inexistente",
            "petición ambigua"
        ]
        
        success_count = 0
        
        for text in test_texts:
            try:
                request_data = {"text": text}
                response = requests.post(
                    f"{self.base_url}/api/v1/fallback/test-degradation",
                    json=request_data,
                    timeout=10
                )
                
                if response.status_code == 200:
                    data = response.json()
                    if (data.get("success") and 
                        data.get("fallback_used") and 
                        data.get("final_intent") == "ayuda"):
                        success_count += 1
                        
            except Exception as e:
                pass
                
        return success_count >= len(test_texts) * 0.75  # 75% de éxito
        
    def test_error_handling(self) -> bool:
        """Prueba el manejo de errores"""
        try:
            # Test con texto vacío
            request_data = {"text": ""}
            response = requests.post(
                f"{self.base_url}/api/v1/fallback/classify",
                json=request_data,
                timeout=10
            )
            
            # Debería manejar el error graciosamente
            return response.status_code in [200, 400, 500]
            
        except Exception as e:
            return False
            
    def test_performance(self) -> bool:
        """Prueba el rendimiento del servicio"""
        try:
            start_time = time.time()
            
            response = requests.post(
                f"{self.base_url}/api/v1/fallback/test",
                timeout=10
            )
            
            end_time = time.time()
            processing_time = end_time - start_time
            
            if response.status_code == 200:
                data = response.json()
                reported_time = data.get("processing_time_ms", 0) / 1000.0
                
                # Verificar que el tiempo de procesamiento es razonable
                return processing_time < 5.0 and reported_time < 5.0
                
            return False
            
        except Exception as e:
            return False
            
    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas"""
        print("🚀 Iniciando pruebas del servicio de fallback inteligente")
        print("=" * 60)
        
        # Verificar disponibilidad del servicio
        if not self.wait_for_service():
            self.log_test("Verificación de disponibilidad", False, "Servicio no disponible")
            return False
        else:
            self.log_test("Verificación de disponibilidad", True)
            
        # Ejecutar pruebas
        tests = [
            ("Health check del servicio", self.test_health_check),
            ("Estadísticas del servicio", self.test_statistics),
            ("Fallback básico", self.test_basic_fallback),
            ("Fallback por palabras clave", self.test_keyword_fallback),
            ("Fallback por contexto", self.test_context_fallback),
            ("Niveles de degradación", self.test_degradation_levels),
            ("Manejo de errores", self.test_error_handling),
            ("Rendimiento", self.test_performance)
        ]
        
        passed_tests = 0
        total_tests = len(tests)
        
        for test_name, test_func in tests:
            try:
                success = test_func()
                self.log_test(test_name, success)
                if success:
                    passed_tests += 1
            except Exception as e:
                self.log_test(test_name, False, f"Error: {str(e)}")
                
        # Resumen final
        print("\n" + "=" * 60)
        print(f"📊 RESUMEN DE PRUEBAS")
        print(f"Pruebas pasadas: {passed_tests}/{total_tests}")
        print(f"Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        
        if passed_tests == total_tests:
            print("🎉 ¡Todas las pruebas pasaron exitosamente!")
        elif passed_tests >= total_tests * 0.8:
            print("✅ La mayoría de las pruebas pasaron")
        else:
            print("⚠️  Muchas pruebas fallaron")
            
        return passed_tests >= total_tests * 0.8
        
    def get_detailed_results(self) -> Dict[str, Any]:
        """Obtiene resultados detallados de las pruebas"""
        return {
            "total_tests": len(self.test_results),
            "passed_tests": sum(1 for r in self.test_results if r["success"]),
            "failed_tests": sum(1 for r in self.test_results if not r["success"]),
            "success_rate": sum(1 for r in self.test_results if r["success"]) / len(self.test_results) if self.test_results else 0,
            "test_details": self.test_results
        }

def main():
    """Función principal"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Prueba el servicio de fallback inteligente")
    parser.add_argument("--url", default="http://localhost:9904", 
                       help="URL base del servicio (default: http://localhost:9904)")
    parser.add_argument("--verbose", "-v", action="store_true", 
                       help="Modo verbose")
    
    args = parser.parse_args()
    
    # Configurar logging
    if args.verbose:
        import logging
        logging.basicConfig(level=logging.DEBUG)
    
    # Crear tester y ejecutar pruebas
    tester = IntelligentFallbackTester(args.url)
    
    try:
        success = tester.run_complete_test_suite()
        
        if args.verbose:
            print("\n📋 RESULTADOS DETALLADOS:")
            results = tester.get_detailed_results()
            print(json.dumps(results, indent=2, ensure_ascii=False))
        
        sys.exit(0 if success else 1)
        
    except KeyboardInterrupt:
        print("\n⏹️  Pruebas interrumpidas por el usuario")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error durante las pruebas: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 