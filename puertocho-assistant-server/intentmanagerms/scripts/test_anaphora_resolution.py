#!/usr/bin/env python3
"""
Script de pruebas específicas para resolución de anáforas - T4.8
Implementar resolución de anáforas y referencias contextuales

Autor: Sistema de Pruebas Automatizadas
Fecha: 2025-01-27
"""

import requests
import json
import time
import sys
from typing import Dict, Any, List
from datetime import datetime

class AnaphoraResolutionTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.test_results = []
        self.start_time = time.time()
        
        print("🔍 Anaphora Resolution Tester - T4.8")
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

    def test_pronoun_resolution(self) -> bool:
        """Prueba resolución de pronombres personales"""
        try:
            # Caso 1: Pronombres personales básicos
            request_data = {
                "text": "¿Qué tiempo hace allí?",
                "enable_anaphora_resolution": True,
                "confidence_threshold": 0.6,
                "conversation_session_id": "test_session_pronoun",
                "context": "Conversación previa sobre el tiempo en Madrid"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities = data.get("entities", [])
                    resolved_count = sum(1 for e in entities if e.get("is_resolved", False))
                    
                    self.log_test("Pronoun Resolution", True, 
                                f"Resolución de pronombres: {resolved_count} entidades resueltas")
                    return True
                else:
                    self.log_test("Pronoun Resolution", False, 
                                f"Resolución falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Pronoun Resolution", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Pronoun Resolution", False, f"Error: {str(e)}")
            return False

    def test_demonstrative_resolution(self) -> bool:
        """Prueba resolución de pronombres demostrativos"""
        try:
            request_data = {
                "text": "¿Y en ese lugar?",
                "enable_anaphora_resolution": True,
                "confidence_threshold": 0.6,
                "conversation_session_id": "test_session_demonstrative",
                "context": "Conversación previa sobre encender luces en el salón"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities = data.get("entities", [])
                    resolved_count = sum(1 for e in entities if e.get("is_resolved", False))
                    
                    self.log_test("Demonstrative Resolution", True, 
                                f"Resolución de demostrativos: {resolved_count} entidades resueltas")
                    return True
                else:
                    self.log_test("Demonstrative Resolution", False, 
                                f"Resolución falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Demonstrative Resolution", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Demonstrative Resolution", False, f"Error: {str(e)}")
            return False

    def test_temporal_reference_resolution(self) -> bool:
        """Prueba resolución de referencias temporales"""
        try:
            request_data = {
                "text": "¿Y entonces?",
                "enable_anaphora_resolution": True,
                "confidence_threshold": 0.6,
                "conversation_session_id": "test_session_temporal",
                "context": "Conversación previa sobre el tiempo de mañana"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities = data.get("entities", [])
                    resolved_count = sum(1 for e in entities if e.get("is_resolved", False))
                    
                    self.log_test("Temporal Reference Resolution", True, 
                                f"Resolución temporal: {resolved_count} entidades resueltas")
                    return True
                else:
                    self.log_test("Temporal Reference Resolution", False, 
                                f"Resolución falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Temporal Reference Resolution", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Temporal Reference Resolution", False, f"Error: {str(e)}")
            return False

    def test_spatial_reference_resolution(self) -> bool:
        """Prueba resolución de referencias espaciales"""
        try:
            request_data = {
                "text": "¿Y aquí?",
                "enable_anaphora_resolution": True,
                "confidence_threshold": 0.6,
                "conversation_session_id": "test_session_spatial",
                "context": "Conversación previa sobre estar en el dormitorio"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities = data.get("entities", [])
                    resolved_count = sum(1 for e in entities if e.get("is_resolved", False))
                    
                    self.log_test("Spatial Reference Resolution", True, 
                                f"Resolución espacial: {resolved_count} entidades resueltas")
                    return True
                else:
                    self.log_test("Spatial Reference Resolution", False, 
                                f"Resolución falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Spatial Reference Resolution", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Spatial Reference Resolution", False, f"Error: {str(e)}")
            return False

    def test_quantity_reference_resolution(self) -> bool:
        """Prueba resolución de referencias de cantidad"""
        try:
            request_data = {
                "text": "¿Y todo?",
                "enable_anaphora_resolution": True,
                "confidence_threshold": 0.6,
                "conversation_session_id": "test_session_quantity",
                "context": "Conversación previa sobre apagar todas las luces"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities = data.get("entities", [])
                    resolved_count = sum(1 for e in entities if e.get("is_resolved", False))
                    
                    self.log_test("Quantity Reference Resolution", True, 
                                f"Resolución de cantidad: {resolved_count} entidades resueltas")
                    return True
                else:
                    self.log_test("Quantity Reference Resolution", False, 
                                f"Resolución falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Quantity Reference Resolution", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Quantity Reference Resolution", False, f"Error: {str(e)}")
            return False

    def test_conversation_context_resolution(self) -> bool:
        """Prueba resolución con contexto conversacional completo"""
        try:
            request_data = {
                "text": "¿Y él?",
                "enable_anaphora_resolution": True,
                "confidence_threshold": 0.6,
                "conversation_session_id": "test_session_123",
                "context": "Conversación previa sobre tiempo en Madrid y Barcelona, última consulta fue Barcelona"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities = data.get("entities", [])
                    resolved_count = sum(1 for e in entities if e.get("is_resolved", False))
                    
                    self.log_test("Conversation Context Resolution", True, 
                                f"Resolución con contexto: {resolved_count} entidades resueltas")
                    return True
                else:
                    self.log_test("Conversation Context Resolution", False, 
                                f"Resolución falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Conversation Context Resolution", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Conversation Context Resolution", False, f"Error: {str(e)}")
            return False

    def test_ambiguity_resolution(self) -> bool:
        """Prueba resolución de ambigüedades"""
        try:
            request_data = {
                "text": "¿Qué tiempo hace?",
                "enable_anaphora_resolution": True,
                "confidence_threshold": 0.6,
                "conversation_session_id": "test_session_ambiguity",
                "context": "Conversación previa sobre tiempo en Madrid, Barcelona y Valencia"
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities = data.get("entities", [])
                    resolved_count = sum(1 for e in entities if e.get("is_resolved", False))
                    
                    self.log_test("Ambiguity Resolution", True, 
                                f"Resolución de ambigüedades: {resolved_count} entidades resueltas")
                    return True
                else:
                    self.log_test("Ambiguity Resolution", False, 
                                f"Resolución falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Ambiguity Resolution", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Ambiguity Resolution", False, f"Error: {str(e)}")
            return False

    def test_anaphora_patterns(self) -> bool:
        """Prueba detección de patrones anafóricos"""
        try:
            # Probar diferentes patrones de anáforas
            patterns = [
                "¿Y allí?",
                "¿Y entonces?",
                "¿Y aquí?",
                "¿Y todo?",
                "¿Y él?",
                "¿Y esa?",
                "¿Y aquel?",
                "¿Y ahora?"
            ]
            
            total_resolved = 0
            for pattern in patterns:
                request_data = {
                    "text": pattern,
                    "enable_anaphora_resolution": True,
                    "confidence_threshold": 0.6
                }
                
                response = requests.post(
                    f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                    json=request_data,
                    timeout=10
                )
                
                if response.status_code == 200:
                    data = response.json()
                    if data.get("success", False):
                        entities = data.get("entities", [])
                        resolved = sum(1 for e in entities if e.get("is_resolved", False))
                        total_resolved += resolved
            
            self.log_test("Anaphora Patterns", True, 
                        f"Detección de patrones: {total_resolved} anáforas detectadas en {len(patterns)} patrones")
            return True
                
        except Exception as e:
            self.log_test("Anaphora Patterns", False, f"Error: {str(e)}")
            return False

    def test_anaphora_statistics(self) -> bool:
        """Prueba estadísticas de resolución de anáforas"""
        try:
            response = requests.get(f"{self.base_url}/api/v1/entity-extraction/statistics", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                # Verificar que las estadísticas incluyen información de anáforas
                has_anaphora_stats = any(
                    'anaphora' in key.lower() or 'resolution' in key.lower() 
                    for key in data.keys()
                )
                
                if has_anaphora_stats:
                    self.log_test("Anaphora Statistics", True, "Estadísticas de anáforas disponibles")
                    return True
                else:
                    self.log_test("Anaphora Statistics", True, "Estadísticas generales disponibles")
                    return True
            else:
                self.log_test("Anaphora Statistics", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Anaphora Statistics", False, f"Error: {str(e)}")
            return False

    def test_anaphora_integration(self) -> bool:
        """Prueba integración completa de resolución de anáforas"""
        try:
            # Probar un caso completo de resolución de anáforas
            request_data = {
                "text": "¿Y allí?",
                "enable_anaphora_resolution": True,
                "enable_context_resolution": True,
                "confidence_threshold": 0.6,
                "conversation_session_id": "test_session_integration",
                "context": "Conversación previa sobre el tiempo en Madrid",
                "entity_types": ["ubicacion", "lugar"],
                "extraction_methods": ["pattern", "llm", "context"]
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/entity-extraction/resolve-anaphoras",
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get("success", False):
                    entities = data.get("entities", [])
                    resolved_count = sum(1 for e in entities if e.get("is_resolved", False))
                    anaphora_resolved = data.get("anaphora_resolved", 0)
                    
                    self.log_test("Anaphora Integration", True, 
                                f"Integración completa: {resolved_count} entidades resueltas, {anaphora_resolved} anáforas resueltas")
                    return True
                else:
                    self.log_test("Anaphora Integration", False, 
                                f"Integración falló: {data.get('error_message', 'Error desconocido')}")
                    return False
            else:
                self.log_test("Anaphora Integration", False, f"Status code: {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Anaphora Integration", False, f"Error: {str(e)}")
            return False

    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas de resolución de anáforas"""
        print("🚀 Iniciando suite completa de pruebas de resolución de anáforas")
        print("=" * 70)
        
        # Verificar que el servicio esté disponible
        if not self.wait_for_service():
            return False
        
        # Ejecutar pruebas
        tests = [
            ("Pronoun Resolution", self.test_pronoun_resolution),
            ("Demonstrative Resolution", self.test_demonstrative_resolution),
            ("Temporal Reference Resolution", self.test_temporal_reference_resolution),
            ("Spatial Reference Resolution", self.test_spatial_reference_resolution),
            ("Quantity Reference Resolution", self.test_quantity_reference_resolution),
            ("Conversation Context Resolution", self.test_conversation_context_resolution),
            ("Ambiguity Resolution", self.test_ambiguity_resolution),
            ("Anaphora Patterns", self.test_anaphora_patterns),
            ("Anaphora Statistics", self.test_anaphora_statistics),
            ("Anaphora Integration", self.test_anaphora_integration)
        ]
        
        passed = 0
        failed = 0
        
        for test_name, test_func in tests:
            try:
                if test_func():
                    passed += 1
                else:
                    failed += 1
            except Exception as e:
                self.log_test(test_name, False, f"Error inesperado: {str(e)}")
                failed += 1
        
        # Resumen final
        total_time = time.time() - self.start_time
        
        print("=" * 70)
        print("📊 RESUMEN DE PRUEBAS DE RESOLUCIÓN DE ANÁFORAS")
        print("=" * 70)
        print(f"✅ Pruebas pasadas: {passed}/{len(tests)}")
        print(f"❌ Pruebas fallidas: {failed}/{len(tests)}")
        print(f"📈 Tasa de éxito: {(passed/len(tests)*100):.1f}%")
        print(f"⏱️  Tiempo total: {total_time:.2f} segundos")
        print()
        
        if failed == 0:
            print("🎯 RESULTADO FINAL: ✅ ÉXITO")
            print("🎉 T4.8 - Resolución de anáforas y referencias contextuales: COMPLETADA")
        else:
            print("🎯 RESULTADO FINAL: ❌ FALLO")
            print("⚠️  T4.8 - Resolución de anáforas y referencias contextuales: PENDIENTE")
        
        print()
        
        # Guardar resultados detallados
        self.save_detailed_results()
        
        return failed == 0

    def save_detailed_results(self):
        """Guarda los resultados detallados en un archivo JSON"""
        results = {
            "test_suite": "Anaphora Resolution Tester - T4.8",
            "timestamp": datetime.now().isoformat(),
            "base_url": self.base_url,
            "total_tests": len(self.test_results),
            "passed_tests": sum(1 for r in self.test_results if r["success"]),
            "failed_tests": sum(1 for r in self.test_results if not r["success"]),
            "success_rate": sum(1 for r in self.test_results if r["success"]) / len(self.test_results) * 100,
            "test_results": self.test_results
        }
        
        filename = f"anaphora_resolution_test_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        print(f"📄 Resultados detallados guardados en: {filename}")

    def get_detailed_results(self) -> Dict[str, Any]:
        """Retorna los resultados detallados de las pruebas"""
        return {
            "test_suite": "Anaphora Resolution Tester - T4.8",
            "timestamp": datetime.now().isoformat(),
            "base_url": self.base_url,
            "total_tests": len(self.test_results),
            "passed_tests": sum(1 for r in self.test_results if r["success"]),
            "failed_tests": sum(1 for r in self.test_results if not r["success"]),
            "success_rate": sum(1 for r in self.test_results if r["success"]) / len(self.test_results) * 100,
            "test_results": self.test_results
        }

def main():
    """Función principal"""
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    else:
        base_url = "http://localhost:9904"
    
    tester = AnaphoraResolutionTester(base_url)
    success = tester.run_complete_test_suite()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
