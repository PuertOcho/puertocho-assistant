#!/usr/bin/env python3
"""
Script de pruebas automatizadas para el EntityExtractor
T4.3 - Crear EntityExtractor basado en LLM para extracción contextual

Autor: Sistema de Pruebas Automatizadas
Fecha: 2025-01-27
"""

import requests
import json
import time
import sys
from typing import Dict, Any, List
from datetime import datetime

class EntityExtractorTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.test_results = []
        self.start_time = time.time()
        
        print("🔍 EntityExtractor Tester - T4.3")
        print("=" * 50)
        print(f"URL Base: {base_url}")
        print(f"Inicio: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print()

    def log_test(self, test_name: str, success: bool, details: str = ""):
        """Registra el resultado de una prueba"""
        status = "✅ PASÓ" if success else "❌ FALLÓ"
        print(f"{status} {test_name}")
        if details:
            print(f"   📝 {details}")
        print()
        
        self.test_results.append({
            "test": test_name,
            "success": success,
            "details": details,
            "timestamp": datetime.now().isoformat()
        })

    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        """Espera a que el servicio esté disponible"""
        print("⏳ Esperando que el servicio esté disponible...")
        
        for attempt in range(max_attempts):
            try:
                response = requests.get(f"{self.base_url}/api/v1/entity-extraction/health", timeout=5)
                if response.status_code == 200:
                    data = response.json()
                    if data.get("healthy", False):
                        print(f"✅ Servicio disponible después de {attempt + 1} intentos")
                        return True
            except requests.exceptions.RequestException:
                pass
            
            if attempt < max_attempts - 1:
                time.sleep(delay)
        
        print("❌ Servicio no disponible después de todos los intentos")
        return False

    def test_health_check(self) -> bool:
        """Prueba el health check del servicio"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/entity-extraction/health", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                if data.get("healthy", False):
                    self.log_test("Health Check", True, f"Servicio saludable - {data.get('status')}")
                    return True
                else:
                    self.log_test("Health Check", False, f"Servicio no saludable - {data.get('status')}")
                    return False
            else:
                self.log_test("Health Check", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Health Check", False, f"Error: {str(e)}")
            return False

    def test_statistics(self) -> bool:
        """Prueba la obtención de estadísticas"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/entity-extraction/statistics", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                stats_count = len(data)
                
                self.log_test("Statistics", True, f"Estadísticas obtenidas: {stats_count} campos")
                return True
            else:
                self.log_test("Statistics", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Statistics", False, f"Error: {str(e)}")
            return False

    def test_basic_entity_extraction(self) -> bool:
        """Prueba extracción básica de entidades"""
        try:
            request_data = {
                "text": "¿Qué tiempo hace en Madrid mañana?",
                "confidence_threshold": 0.6,
                "max_entities": 5
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/extract-simple",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities_found = data.get("total_entities_found", 0)
                    self.log_test("Basic Entity Extraction", True, 
                                f"Extracción exitosa: {entities_found} entidades encontradas")
                    return True
                else:
                    self.log_test("Basic Entity Extraction", False, 
                                f"Extracción falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Basic Entity Extraction", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Basic Entity Extraction", False, f"Error: {str(e)}")
            return False

    def test_specific_entity_extraction(self) -> bool:
        """Prueba extracción de entidades específicas"""
        try:
            request_data = {
                "text": "Enciende la luz del salón y pon música de jazz",
                "entity_types": ["lugar", "genero"],
                "confidence_threshold": 0.7
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/extract-specific",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities_found = data.get("total_entities_found", 0)
                    self.log_test("Specific Entity Extraction", True, 
                                f"Extracción específica exitosa: {entities_found} entidades encontradas")
                    return True
                else:
                    self.log_test("Specific Entity Extraction", False, 
                                f"Extracción falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Specific Entity Extraction", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Specific Entity Extraction", False, f"Error: {str(e)}")
            return False

    def test_contextual_extraction(self) -> bool:
        """Prueba extracción con contexto conversacional"""
        try:
            request_data = {
                "text": "¿Y allí?",
                "conversation_session_id": "test_session_123",
                "context": "Conversación previa sobre el tiempo en Madrid",
                "intent": "consultar_tiempo",
                "enable_anaphora_resolution": True,
                "enable_context_resolution": True
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/extract-with-context",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities_found = data.get("total_entities_found", 0)
                    anaphora_resolved = data.get("anaphora_resolved", 0)
                    self.log_test("Contextual Extraction", True, 
                                f"Extracción contextual exitosa: {entities_found} entidades, {anaphora_resolved} anáforas resueltas")
                    return True
                else:
                    self.log_test("Contextual Extraction", False, 
                                f"Extracción falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Contextual Extraction", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Contextual Extraction", False, f"Error: {str(e)}")
            return False

    def test_entity_validation(self) -> bool:
        """Prueba validación de entidades"""
        try:
            request_data = {
                "text": "¿Qué tiempo hace en Madrid mañana?",
                "enable_validation": True,
                "confidence_threshold": 0.8
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/validate",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities_found = data.get("total_entities_found", 0)
                    validation_errors = len(data.get("validation_errors", []))
                    self.log_test("Entity Validation", True, 
                                f"Validación exitosa: {entities_found} entidades validadas, {validation_errors} errores")
                    return True
                else:
                    self.log_test("Entity Validation", False, 
                                f"Validación falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Entity Validation", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Entity Validation", False, f"Error: {str(e)}")
            return False

    def test_anaphora_resolution(self) -> bool:
        """Prueba resolución de anáforas"""
        try:
            request_data = {
                "text": "¿Y allí?",
                "enable_anaphora_resolution": True,
                "confidence_threshold": 0.6
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    anaphora_resolved = data.get("anaphora_resolved", 0)
                    self.log_test("Anaphora Resolution", True, 
                                f"Resolución exitosa: {anaphora_resolved} anáforas resueltas")
                    return True
                else:
                    self.log_test("Anaphora Resolution", False, 
                                f"Resolución falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Anaphora Resolution", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Anaphora Resolution", False, f"Error: {str(e)}")
            return False

    def test_cache_management(self) -> bool:
        """Prueba gestión del cache"""
        try:
            response = requests.post(f"{self.base_url}/api/v1/entity-extraction/clear-cache", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    self.log_test("Cache Management", True, "Cache limpiado exitosamente")
                    return True
                else:
                    self.log_test("Cache Management", False, 
                                f"Limpieza falló: {data.get('error', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Cache Management", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Cache Management", False, f"Error: {str(e)}")
            return False

    def test_error_handling(self) -> bool:
        """Prueba manejo de errores"""
        try:
            # Prueba con texto vacío
            request_data = {"text": ""}
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/extract-simple",
                json=request_data,
                timeout=10
            )
            
            if response.status_code == 400:
                data = response.json()
                if not data.get("success", True):
                    self.log_test("Error Handling", True, "Manejo de errores correcto (texto vacío)")
                    return True
                else:
                    self.log_test("Error Handling", False, "No se detectó error con texto vacío")
                    return False
            else:
                self.log_test("Error Handling", False, f"Status code inesperado: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Error Handling", False, f"Error: {str(e)}")
            return False

    def test_performance(self) -> bool:
        """Prueba rendimiento del servicio"""
        try:
            request_data = {
                "text": "¿Qué tiempo hace en Madrid mañana?",
                "confidence_threshold": 0.6
            }
            
            start_time = time.time()
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/extract-simple",
                json=request_data,
                timeout=30
            )
            end_time = time.time()
            
            processing_time = end_time - start_time
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    reported_time = data.get("processing_time_ms", 0) / 1000.0
                    
                    if processing_time < 5.0:  # Máximo 5 segundos
                        self.log_test("Performance", True, 
                                    f"Rendimiento aceptable: {processing_time:.2f}s (reportado: {reported_time:.2f}s)")
                        return True
                    else:
                        self.log_test("Performance", False, 
                                    f"Rendimiento lento: {processing_time:.2f}s")
                        return False
                else:
                    self.log_test("Performance", False, "Extracción falló durante prueba de rendimiento")
                    return False
            else:
                self.log_test("Performance", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Performance", False, f"Error: {str(e)}")
            return False

    def test_service_test_endpoint(self) -> bool:
        """Prueba el endpoint de test automatizado"""
        try:
            response = requests.post(f"{self.base_url}/api/v1/entity-extraction/test", timeout=30)
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False) and data.get("all_tests_passed", False):
                    self.log_test("Service Test Endpoint", True, "Prueba automatizada del servicio exitosa")
                    return True
                else:
                    failed_tests = []
                    for key, value in data.items():
                        if key.endswith("_extraction") and not value:
                            failed_tests.append(key)
                    
                    self.log_test("Service Test Endpoint", False, 
                                f"Pruebas fallidas: {', '.join(failed_tests)}")
                    return False
            else:
                self.log_test("Service Test Endpoint", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Service Test Endpoint", False, f"Error: {str(e)}")
            return False

    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas"""
        print("🚀 Iniciando suite completa de pruebas del EntityExtractor")
        print("=" * 60)
        
        # Verificar disponibilidad del servicio
        if not self.wait_for_service():
            return False
        
        # Ejecutar pruebas
        tests = [
            ("Health Check", self.test_health_check),
            ("Statistics", self.test_statistics),
            ("Basic Entity Extraction", self.test_basic_entity_extraction),
            ("Specific Entity Extraction", self.test_specific_entity_extraction),
            ("Contextual Extraction", self.test_contextual_extraction),
            ("Entity Validation", self.test_entity_validation),
            ("Anaphora Resolution", self.test_anaphora_resolution),
            ("Cache Management", self.test_cache_management),
            ("Error Handling", self.test_error_handling),
            ("Performance", self.test_performance),
            ("Service Test Endpoint", self.test_service_test_endpoint)
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
        print("=" * 60)
        print("📊 RESUMEN DE PRUEBAS")
        print("=" * 60)
        print(f"✅ Pruebas pasadas: {passed_tests}/{total_tests}")
        print(f"❌ Pruebas fallidas: {total_tests - passed_tests}/{total_tests}")
        print(f"📈 Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        print(f"⏱️  Tiempo total: {time.time() - self.start_time:.2f} segundos")
        
        success = passed_tests == total_tests
        status = "✅ ÉXITO" if success else "❌ FALLO"
        print(f"\n🎯 RESULTADO FINAL: {status}")
        
        return success

    def get_detailed_results(self) -> Dict[str, Any]:
        """Obtiene resultados detallados de las pruebas"""
        passed_tests = sum(1 for result in self.test_results if result["success"])
        total_tests = len(self.test_results)
        
        return {
            "service": "EntityExtractor",
            "version": "T4.3",
            "timestamp": datetime.now().isoformat(),
            "total_tests": total_tests,
            "passed_tests": passed_tests,
            "failed_tests": total_tests - passed_tests,
            "success_rate": (passed_tests/total_tests)*100 if total_tests > 0 else 0,
            "execution_time_seconds": time.time() - self.start_time,
            "test_results": self.test_results,
            "overall_success": passed_tests == total_tests
        }

def main():
    """Función principal"""
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    else:
        base_url = "http://localhost:9904"
    
    tester = EntityExtractorTester(base_url)
    
    try:
        success = tester.run_complete_test_suite()
        
        # Guardar resultados detallados
        results = tester.get_detailed_results()
        with open("entity_extractor_test_results.json", "w") as f:
            json.dump(results, f, indent=2)
        
        print(f"\n📄 Resultados detallados guardados en: entity_extractor_test_results.json")
        
        sys.exit(0 if success else 1)
        
    except KeyboardInterrupt:
        print("\n⚠️  Pruebas interrumpidas por el usuario")
        sys.exit(1)
    except Exception as e:
        print(f"\n💥 Error inesperado: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main() 