#!/usr/bin/env python3
"""
Script de prueba para el motor RAG de clasificación de intenciones.
Prueba todos los endpoints y funcionalidades del sistema RAG.
"""

import requests
import json
import time
from typing import Dict, Any, List
import sys

class RagClassifierTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        
    def test_health(self) -> bool:
        """Prueba el health check del motor RAG"""
        print("🔍 Probando health check del motor RAG...")
        
        try:
            response = self.session.get(f"{self.base_url}/api/v1/rag-classifier/health")
            
            if response.status_code == 200:
                health_data = response.json()
                print(f"✅ Health check: {health_data.get('status', 'UNKNOWN')}")
                print(f"   Componentes: {health_data.get('components', {})}")
                return True
            else:
                print(f"❌ Health check falló: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error en health check: {e}")
            return False
    
    def test_statistics(self) -> bool:
        """Prueba las estadísticas del motor RAG"""
        print("📊 Probando estadísticas del motor RAG...")
        
        try:
            response = self.session.get(f"{self.base_url}/api/v1/rag-classifier/statistics")
            
            if response.status_code == 200:
                stats = response.json()
                print(f"✅ Estadísticas obtenidas:")
                print(f"   Motor: {stats.get('engine_name', 'N/A')}")
                print(f"   Versión: {stats.get('version', 'N/A')}")
                print(f"   Estado: {stats.get('status', 'N/A')}")
                print(f"   Umbral confianza: {stats.get('default_confidence_threshold', 'N/A')}")
                return True
            else:
                print(f"❌ Estadísticas fallaron: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error obteniendo estadísticas: {e}")
            return False
    
    def test_simple_classification(self) -> bool:
        """Prueba clasificación simple de texto"""
        print("🎯 Probando clasificación simple...")
        
        test_cases = [
            "¿qué tiempo hace en Madrid?",
            "enciende la luz del salón",
            "ayúdame con algo",
            "reproduce música relajante",
            "programa una alarma para mañana"
        ]
        
        success_count = 0
        
        for text in test_cases:
            try:
                response = self.session.post(
                    f"{self.base_url}/api/v1/rag-classifier/classify",
                    params={'text': text}
                )
                
                if response.status_code == 200:
                    result = response.json()
                    intent_id = result.get('intent_id', 'unknown')
                    confidence = result.get('confidence_score', 0.0)
                    processing_time = result.get('processing_time_ms', 0)
                    
                    print(f"✅ '{text}' → {intent_id} (conf: {confidence:.2f}, {processing_time}ms)")
                    success_count += 1
                else:
                    print(f"❌ Error clasificando '{text}': {response.status_code}")
                    
            except Exception as e:
                print(f"❌ Error en clasificación simple: {e}")
        
        print(f"📈 Clasificación simple: {success_count}/{len(test_cases)} exitosas")
        return success_count == len(test_cases)
    
    def test_advanced_classification(self) -> bool:
        """Prueba clasificación avanzada con metadata"""
        print("🚀 Probando clasificación avanzada...")
        
        request_data = {
            "text": "¿qué tiempo hace en Barcelona?",
            "session_id": "test-session-123",
            "user_id": "test-user",
            "context_metadata": {
                "location": "Madrid",
                "device_type": "raspberry_pi",
                "temperature": 22.5
            },
            "max_examples_for_rag": 3,
            "confidence_threshold": 0.8,
            "enable_fallback": True
        }
        
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/classify/advanced",
                json=request_data
            )
            
            if response.status_code == 200:
                result = response.json()
                intent_id = result.get('intent_id', 'unknown')
                confidence = result.get('confidence_score', 0.0)
                rag_examples = result.get('rag_examples_used', [])
                
                print(f"✅ Clasificación avanzada exitosa:")
                print(f"   Intención: {intent_id}")
                print(f"   Confianza: {confidence:.2f}")
                print(f"   Ejemplos RAG: {len(rag_examples)}")
                print(f"   Tiempo total: {result.get('processing_time_ms', 0)}ms")
                return True
            else:
                print(f"❌ Clasificación avanzada falló: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error en clasificación avanzada: {e}")
            return False
    
    def test_session_classification(self) -> bool:
        """Prueba clasificación con session ID"""
        print("🔗 Probando clasificación con session...")
        
        session_id = "test-session-" + str(int(time.time()))
        text = "enciende las luces del dormitorio"
        
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/classify/session/{session_id}",
                params={'text': text}
            )
            
            if response.status_code == 200:
                result = response.json()
                intent_id = result.get('intent_id', 'unknown')
                confidence = result.get('confidence_score', 0.0)
                
                print(f"✅ Clasificación con session exitosa:")
                print(f"   Session: {session_id}")
                print(f"   Intención: {intent_id}")
                print(f"   Confianza: {confidence:.2f}")
                return True
            else:
                print(f"❌ Clasificación con session falló: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error en clasificación con session: {e}")
            return False
    
    def test_batch_classification(self) -> bool:
        """Prueba clasificación en batch"""
        print("📦 Probando clasificación batch...")
        
        batch_data = {
            "text1": "¿qué tiempo hace?",
            "text2": "enciende la luz",
            "text3": "ayúdame",
            "text4": "reproduce música",
            "text5": "programa alarma"
        }
        
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/classify/batch",
                json=batch_data
            )
            
            if response.status_code == 200:
                results = response.json()
                success_count = 0
                
                print(f"✅ Clasificación batch completada:")
                for text_id, result in results.items():
                    intent_id = result.get('intent_id', 'unknown')
                    confidence = result.get('confidence_score', 0.0)
                    success = result.get('success', False)
                    
                    if success:
                        success_count += 1
                        print(f"   {text_id}: {intent_id} (conf: {confidence:.2f})")
                    else:
                        print(f"   {text_id}: ERROR - {result.get('error_message', 'Unknown error')}")
                
                print(f"📈 Batch: {success_count}/{len(batch_data)} exitosas")
                return success_count == len(batch_data)
            else:
                print(f"❌ Clasificación batch falló: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error en clasificación batch: {e}")
            return False
    
    def test_automated_test(self) -> bool:
        """Ejecuta el test automatizado del motor RAG"""
        print("🤖 Ejecutando test automatizado del motor RAG...")
        
        try:
            response = self.session.post(f"{self.base_url}/api/v1/rag-classifier/test")
            
            if response.status_code == 200:
                test_results = response.json()
                total_tests = test_results.get('total_tests', 0)
                successful_tests = test_results.get('successful_tests', 0)
                success_rate = test_results.get('success_rate', 0.0)
                avg_processing_time = test_results.get('average_processing_time_ms', 0)
                
                print(f"✅ Test automatizado completado:")
                print(f"   Total tests: {total_tests}")
                print(f"   Exitosos: {successful_tests}")
                print(f"   Tasa éxito: {success_rate:.2%}")
                print(f"   Tiempo promedio: {avg_processing_time}ms")
                
                # Mostrar resultados individuales
                results = test_results.get('results', {})
                for text, result in results.items():
                    intent_id = result.get('intent_id', 'unknown')
                    confidence = result.get('confidence_score', 0.0)
                    success = result.get('success', False)
                    
                    status = "✅" if success else "❌"
                    print(f"   {status} '{text}' → {intent_id} (conf: {confidence:.2f})")
                
                return success_rate >= 0.8  # Al menos 80% de éxito
            else:
                print(f"❌ Test automatizado falló: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error en test automatizado: {e}")
            return False
    
    def test_error_handling(self) -> bool:
        """Prueba el manejo de errores"""
        print("⚠️ Probando manejo de errores...")
        
        # Test con texto vacío
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/classify",
                params={'text': ''}
            )
            
            if response.status_code == 400:
                print("✅ Error manejado correctamente para texto vacío")
            else:
                print(f"❌ Error no manejado para texto vacío: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error en test de texto vacío: {e}")
            return False
        
        # Test con texto muy largo
        long_text = "a" * 10000
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/classify",
                params={'text': long_text}
            )
            
            if response.status_code in [200, 400, 413]:
                print("✅ Error manejado correctamente para texto largo")
                return True
            else:
                print(f"❌ Error no manejado para texto largo: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error en test de texto largo: {e}")
            return False
    
    def run_all_tests(self) -> bool:
        """Ejecuta todas las pruebas"""
        print("🧪 INICIANDO PRUEBAS COMPLETAS DEL MOTOR RAG")
        print("=" * 60)
        
        tests = [
            ("Health Check", self.test_health),
            ("Estadísticas", self.test_statistics),
            ("Clasificación Simple", self.test_simple_classification),
            ("Clasificación Avanzada", self.test_advanced_classification),
            ("Clasificación con Session", self.test_session_classification),
            ("Clasificación Batch", self.test_batch_classification),
            ("Test Automatizado", self.test_automated_test),
            ("Manejo de Errores", self.test_error_handling)
        ]
        
        passed_tests = 0
        total_tests = len(tests)
        
        for test_name, test_func in tests:
            print(f"\n🔍 {test_name}")
            print("-" * 40)
            
            try:
                if test_func():
                    print(f"✅ {test_name}: PASÓ")
                    passed_tests += 1
                else:
                    print(f"❌ {test_name}: FALLÓ")
            except Exception as e:
                print(f"❌ {test_name}: ERROR - {e}")
        
        print("\n" + "=" * 60)
        print(f"📊 RESUMEN DE PRUEBAS")
        print(f"   Total: {total_tests}")
        print(f"   Pasaron: {passed_tests}")
        print(f"   Fallaron: {total_tests - passed_tests}")
        print(f"   Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        
        if passed_tests == total_tests:
            print("🎉 ¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE!")
        else:
            print("⚠️ Algunas pruebas fallaron. Revisar logs para más detalles.")
        
        return passed_tests == total_tests

def main():
    """Función principal"""
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    else:
        base_url = "http://localhost:9904"
    
    print(f"🚀 Iniciando pruebas del motor RAG en: {base_url}")
    
    tester = RagClassifierTester(base_url)
    success = tester.run_all_tests()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 