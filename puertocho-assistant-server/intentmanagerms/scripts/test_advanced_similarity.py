#!/usr/bin/env python3
"""
Script de prueba para el sistema de similarity search avanzado (T2.2)
Prueba las nuevas funcionalidades de búsqueda por similitud mejorada.
"""

import requests
import json
import time
from typing import Dict, Any, List

class AdvancedSimilarityTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        
    def log_test(self, test_name: str, success: bool, details: str = ""):
        status = "✅ PASÓ" if success else "❌ FALLÓ"
        print(f"   {status} {test_name}")
        if details:
            print(f"      {details}")
        return success
    
    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        print("🔄 Esperando a que el servicio esté disponible...")
        for attempt in range(1, max_attempts + 1):
            try:
                response = self.session.get(f"{self.base_url}/api/v1/similarity-search/health", timeout=5)
                if response.status_code == 200:
                    print(f"✅ Servicio disponible en intento {attempt}")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            print(f"   Intento {attempt}/{max_attempts} - Esperando {delay}s...")
            time.sleep(delay)
        
        print("❌ Servicio no disponible después de todos los intentos")
        return False
    
    def test_service_availability(self) -> bool:
        """Prueba la disponibilidad del servicio de similarity search"""
        print("\n🔍 1. VERIFICACIÓN DE DISPONIBILIDAD DEL SERVICIO")
        print("-" * 50)
        
        try:
            response = self.session.get(f"{self.base_url}/api/v1/similarity-search/health", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                status = data.get('status', 'UNKNOWN')
                return self.log_test("Conectividad básica", status == 'HEALTHY', f"Status: {status}")
            else:
                return self.log_test("Conectividad básica", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            return self.log_test("Conectividad básica", False, f"Error: {str(e)}")
    
    def test_statistics(self) -> bool:
        """Prueba las estadísticas del servicio"""
        print("\n🔍 2. ESTADÍSTICAS DEL SERVICIO")
        print("-" * 50)
        
        try:
            response = self.session.get(f"{self.base_url}/api/v1/similarity-search/statistics", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                stats = data.get('statistics', {})
                
                algorithm = stats.get('search_algorithm', 'unknown')
                diversity_enabled = stats.get('enable_diversity_filtering', False)
                clustering_enabled = stats.get('enable_intent_clustering', False)
                boosting_enabled = stats.get('enable_semantic_boosting', False)
                
                success = True
                success &= self.log_test("Algoritmo configurado", algorithm in ['cosine', 'euclidean', 'manhattan', 'hybrid'], 
                                       f"Algorithm: {algorithm}")
                success &= self.log_test("Diversity filtering", diversity_enabled, 
                                       f"Enabled: {diversity_enabled}")
                success &= self.log_test("Intent clustering", clustering_enabled, 
                                       f"Enabled: {clustering_enabled}")
                success &= self.log_test("Semantic boosting", boosting_enabled, 
                                       f"Enabled: {boosting_enabled}")
                
                return success
            else:
                return self.log_test("Estadísticas disponibles", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            return self.log_test("Estadísticas disponibles", False, f"Error: {str(e)}")
    
    def test_service_info(self) -> bool:
        """Prueba la información del servicio"""
        print("\n🔍 3. INFORMACIÓN DEL SERVICIO")
        print("-" * 50)
        
        try:
            response = self.session.get(f"{self.base_url}/api/v1/similarity-search/info", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                service = data.get('service', '')
                version = data.get('version', '')
                features = data.get('features', {})
                
                success = True
                success &= self.log_test("Información del servicio", service == "Advanced Similarity Search Service", 
                                       f"Service: {service}")
                success &= self.log_test("Versión", version == "1.0.0", f"Version: {version}")
                success &= self.log_test("Algoritmos disponibles", 'algorithms' in features, 
                                       f"Features: {list(features.keys())}")
                
                return success
            else:
                return self.log_test("Información del servicio", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            return self.log_test("Información del servicio", False, f"Error: {str(e)}")
    
    def test_service_test(self) -> bool:
        """Prueba el endpoint de test del servicio"""
        print("\n🔍 4. TEST DEL SERVICIO")
        print("-" * 50)
        
        try:
            response = self.session.post(f"{self.base_url}/api/v1/similarity-search/test", timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                test_status = data.get('test_status', '')
                test_details = data.get('test_details', {})
                
                success = True
                success &= self.log_test("Test del servicio", test_status == 'PASSED', 
                                       f"Status: {test_status}")
                success &= self.log_test("Algoritmo disponible", test_details.get('algorithm_available', False), 
                                       "Algorithm available")
                success &= self.log_test("Diversity filtering disponible", test_details.get('diversity_filtering_available', False), 
                                       "Diversity filtering available")
                success &= self.log_test("Intent clustering disponible", test_details.get('intent_clustering_available', False), 
                                       "Intent clustering available")
                success &= self.log_test("Semantic boosting disponible", test_details.get('semantic_boosting_available', False), 
                                       "Semantic boosting available")
                
                return success
            else:
                return self.log_test("Test del servicio", False, f"HTTP {response.status_code}")
                
        except Exception as e:
            return self.log_test("Test del servicio", False, f"Error: {str(e)}")
    
    def test_rag_integration(self) -> bool:
        """Prueba la integración con el motor RAG"""
        print("\n🔍 5. INTEGRACIÓN CON MOTOR RAG")
        print("-" * 50)
        
        test_texts = [
            "¿qué tiempo hace en Madrid?",
            "enciende la luz del salón",
            "ayúdame con algo",
            "reproduce música relajante",
            "programa una alarma para mañana"
        ]
        
        success_count = 0
        total_tests = len(test_texts)
        
        for i, text in enumerate(test_texts, 1):
            try:
                response = self.session.post(
                    f"{self.base_url}/api/v1/rag-classifier/classify",
                    params={'text': text},
                    timeout=10
                )
                
                if response.status_code == 200:
                    data = response.json()
                    intent_id = data.get('intent_id', '')
                    confidence = data.get('confidence_score', 0.0)
                    rag_examples = data.get('rag_examples_used', [])
                    
                    test_success = self.log_test(f"Clasificación {i}", True, 
                                               f"'{text[:30]}...' → {intent_id} (conf: {confidence:.2f}, ejemplos: {len(rag_examples) if rag_examples else 0})")
                    if test_success:
                        success_count += 1
                else:
                    self.log_test(f"Clasificación {i}", False, f"HTTP {response.status_code}")
                    
            except Exception as e:
                self.log_test(f"Clasificación {i}", False, f"Error: {str(e)}")
        
        overall_success = success_count >= total_tests * 0.8  # 80% de éxito mínimo
        return self.log_test("Integración RAG general", overall_success, 
                           f"{success_count}/{total_tests} exitosas")
    
    def run_complete_test_suite(self) -> bool:
        """Ejecuta todas las pruebas del sistema de similarity search avanzado"""
        print("🚀 Iniciando pruebas del sistema de similarity search avanzado en:", self.base_url)
        print("🧪 PRUEBAS DEL SISTEMA DE SIMILARITY SEARCH AVANZADO - T2.2")
        print("=" * 70)
        print("🎯 URL Base:", self.base_url)
        print("⏰ Inicio:", time.strftime("%Y-%m-%d %H:%M:%S"))
        print("=" * 70)
        
        # Esperar a que el servicio esté disponible
        if not self.wait_for_service():
            return False
        
        # Ejecutar pruebas
        tests = [
            ("Verificación de Disponibilidad", self.test_service_availability),
            ("Estadísticas del Servicio", self.test_statistics),
            ("Información del Servicio", self.test_service_info),
            ("Test del Servicio", self.test_service_test),
            ("Integración con Motor RAG", self.test_rag_integration)
        ]
        
        passed_tests = 0
        total_tests = len(tests)
        
        for test_name, test_func in tests:
            print(f"\n🔍 {test_name}")
            print("-" * 50)
            
            if test_func():
                passed_tests += 1
                print(f"✅ {test_name}: PASÓ")
            else:
                print(f"❌ {test_name}: FALLÓ")
        
        # Resumen final
        print("\n" + "=" * 70)
        print("📊 RESUMEN FINAL DE PRUEBAS - SIMILARITY SEARCH AVANZADO")
        print("=" * 70)
        print(f"   Total de pruebas: {total_tests}")
        print(f"   Pruebas pasadas: {passed_tests}")
        print(f"   Pruebas fallidas: {total_tests - passed_tests}")
        print(f"   Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        print(f"   ⏰ Fin: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        
        if passed_tests == total_tests:
            print("\n🎉 ¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE!")
            print("✅ El sistema de similarity search avanzado está funcionando correctamente")
            print("=" * 70)
            return True
        else:
            print("\n⚠️  ALGUNAS PRUEBAS FALLARON")
            print("❌ El sistema de similarity search avanzado necesita revisión")
            print("=" * 70)
            return False

def main():
    """Función principal"""
    tester = AdvancedSimilarityTester()
    success = tester.run_complete_test_suite()
    
    if success:
        print("\n🎯 T2.2 - SISTEMA DE SIMILARITY SEARCH AVANZADO: ✅ COMPLETADO")
    else:
        print("\n🎯 T2.2 - SISTEMA DE SIMILARITY SEARCH AVANZADO: ❌ FALLÓ")
    
    exit(0 if success else 1)

if __name__ == "__main__":
    main() 