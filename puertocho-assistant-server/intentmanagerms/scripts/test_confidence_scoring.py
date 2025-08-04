#!/usr/bin/env python3
"""
Script de prueba para el sistema de confidence scoring avanzado.
Prueba las 10 métricas implementadas y verifica la configuración dinámica.
"""

import requests
import json
import time
from typing import Dict, Any

class ConfidenceScoringTester:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.api_base = f"{base_url}/api/v1/rag-classifier"
        
    def test_basic_classification(self, text: str) -> Dict[str, Any]:
        """Prueba clasificación básica"""
        print(f"\n🔍 Probando clasificación básica: '{text}'")
        
        try:
            response = requests.post(
                f"{self.api_base}/classify",
                params={"text": text},
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ Clasificación exitosa:")
                print(f"   - Intent: {result.get('intent_id')}")
                print(f"   - Confidence: {result.get('confidence_score', 0):.3f}")
                print(f"   - Processing time: {result.get('processing_time_ms', 0)}ms")
                print(f"   - Fallback used: {result.get('fallback_used', False)}")
                return result
            else:
                print(f"❌ Error en clasificación: {response.status_code}")
                print(f"   Response: {response.text}")
                return {}
                
        except Exception as e:
            print(f"❌ Excepción en clasificación: {e}")
            return {}
    
    def test_advanced_classification(self, text: str, session_id: str = None) -> Dict[str, Any]:
        """Prueba clasificación avanzada con metadata"""
        print(f"\n🔍 Probando clasificación avanzada: '{text}'")
        
        request_data = {
            "text": text,
            "sessionId": session_id or f"test_session_{int(time.time())}",
            "userId": "test_user",
            "maxExamplesForRag": 5,
            "confidenceThreshold": 0.7,
            "contextMetadata": {
                "source": "test_script",
                "timestamp": int(time.time()),
                "test_type": "confidence_scoring"
            }
        }
        
        try:
            response = requests.post(
                f"{self.api_base}/classify/advanced",
                json=request_data,
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ Clasificación avanzada exitosa:")
                print(f"   - Intent: {result.get('intent_id')}")
                print(f"   - Confidence: {result.get('confidence_score', 0):.3f}")
                print(f"   - Processing time: {result.get('processing_time_ms', 0)}ms")
                print(f"   - Vector search time: {result.get('vector_search_time_ms', 0)}ms")
                print(f"   - LLM inference time: {result.get('llm_inference_time_ms', 0)}ms")
                print(f"   - Fallback used: {result.get('fallback_used', False)}")
                return result
            else:
                print(f"❌ Error en clasificación avanzada: {response.status_code}")
                print(f"   Response: {response.text}")
                return {}
                
        except Exception as e:
            print(f"❌ Excepción en clasificación avanzada: {e}")
            return {}
    
    def test_confidence_metrics(self, text: str) -> Dict[str, Any]:
        """Prueba el endpoint de métricas detalladas de confidence"""
        print(f"\n🔍 Probando métricas detalladas de confidence: '{text}'")
        
        request_data = {
            "text": text,
            "sessionId": f"metrics_test_{int(time.time())}",
            "userId": "test_user",
            "maxExamplesForRag": 5,
            "confidenceThreshold": 0.7,
            "contextMetadata": {
                "source": "confidence_metrics_test",
                "timestamp": int(time.time())
            }
        }
        
        try:
            response = requests.post(
                f"{self.api_base}/confidence-metrics",
                json=request_data,
                timeout=30
            )
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ Métricas de confidence obtenidas:")
                print(f"   - Intent: {result.get('intent_id')}")
                print(f"   - Final confidence: {result.get('final_confidence', 0):.3f}")
                print(f"   - Processing time: {result.get('processing_time_ms', 0)}ms")
                
                # Mostrar métricas detalladas
                metrics = result.get('confidence_metrics', {})
                if metrics:
                    print(f"   📊 Métricas detalladas:")
                    for metric, value in metrics.items():
                        print(f"      - {metric}: {value:.3f}")
                
                return result
            else:
                print(f"❌ Error obteniendo métricas: {response.status_code}")
                print(f"   Response: {response.text}")
                return {}
                
        except Exception as e:
            print(f"❌ Excepción obteniendo métricas: {e}")
            return {}
    
    def test_statistics(self) -> Dict[str, Any]:
        """Prueba el endpoint de estadísticas"""
        print(f"\n🔍 Obteniendo estadísticas del motor RAG")
        
        try:
            response = requests.get(f"{self.api_base}/statistics", timeout=30)
            
            if response.status_code == 200:
                stats = response.json()
                print(f"✅ Estadísticas obtenidas:")
                print(f"   - Total classifications: {stats.get('total_classifications', 0)}")
                print(f"   - Average confidence: {stats.get('average_confidence', 0):.3f}")
                print(f"   - Average processing time: {stats.get('average_processing_time_ms', 0)}ms")
                print(f"   - Fallback rate: {stats.get('fallback_rate', 0):.3f}")
                return stats
            else:
                print(f"❌ Error obteniendo estadísticas: {response.status_code}")
                return {}
                
        except Exception as e:
            print(f"❌ Excepción obteniendo estadísticas: {e}")
            return {}
    
    def test_health(self) -> Dict[str, Any]:
        """Prueba el endpoint de health check"""
        print(f"\n🔍 Verificando salud del motor RAG")
        
        try:
            response = requests.get(f"{self.api_base}/health", timeout=30)
            
            if response.status_code == 200:
                health = response.json()
                status = health.get('status', 'UNKNOWN')
                print(f"✅ Health check: {status}")
                
                components = health.get('components', {})
                for component, status in components.items():
                    print(f"   - {component}: {status}")
                
                return health
            else:
                print(f"❌ Error en health check: {response.status_code}")
                return {}
                
        except Exception as e:
            print(f"❌ Excepción en health check: {e}")
            return {}
    
    def run_comprehensive_test(self):
        """Ejecuta una prueba completa del sistema"""
        print("🚀 INICIANDO PRUEBA COMPLETA DEL SISTEMA DE CONFIDENCE SCORING")
        print("=" * 70)
        
        # Test cases con diferentes niveles de complejidad
        test_cases = [
            "¿qué tiempo hace en Madrid?",
            "enciende la luz del salón",
            "ayúdame con algo",
            "reproduce música relajante",
            "programa una alarma para mañana",
            "texto muy ambiguo que no debería tener alta confianza",
            "comando muy específico y claro para el sistema"
        ]
        
        # Verificar salud del sistema
        health_result = self.test_health()
        if not health_result:
            print("❌ Sistema no disponible, abortando pruebas")
            return
        
        # Obtener estadísticas iniciales
        initial_stats = self.test_statistics()
        
        # Probar cada caso de test
        results = []
        for i, text in enumerate(test_cases, 1):
            print(f"\n📝 CASO DE PRUEBA {i}/{len(test_cases)}")
            print("-" * 50)
            
            # Clasificación básica
            basic_result = self.test_basic_classification(text)
            
            # Clasificación avanzada
            advanced_result = self.test_advanced_classification(text)
            
            # Métricas detalladas de confidence
            metrics_result = self.test_confidence_metrics(text)
            
            results.append({
                "text": text,
                "basic": basic_result,
                "advanced": advanced_result,
                "metrics": metrics_result
            })
            
            # Pausa entre pruebas
            time.sleep(1)
        
        # Obtener estadísticas finales
        final_stats = self.test_statistics()
        
        # Resumen de resultados
        print(f"\n📊 RESUMEN DE PRUEBAS")
        print("=" * 70)
        
        successful_tests = sum(1 for r in results if r['basic'] and r['advanced'])
        print(f"✅ Pruebas exitosas: {successful_tests}/{len(test_cases)}")
        
        if successful_tests > 0:
            avg_confidence = sum(
                r['basic'].get('confidence_score', 0) for r in results if r['basic']
            ) / successful_tests
            print(f"📈 Confidence promedio: {avg_confidence:.3f}")
            
            avg_processing_time = sum(
                r['basic'].get('processing_time_ms', 0) for r in results if r['basic']
            ) / successful_tests
            print(f"⏱️  Tiempo de procesamiento promedio: {avg_processing_time:.0f}ms")
        
        # Comparar estadísticas
        if initial_stats and final_stats:
            initial_classifications = initial_stats.get('total_classifications', 0)
            final_classifications = final_stats.get('total_classifications', 0)
            new_classifications = final_classifications - initial_classifications
            print(f"🔄 Nuevas clasificaciones durante la prueba: {new_classifications}")
        
        print(f"\n🎉 PRUEBA COMPLETA FINALIZADA")

def main():
    """Función principal"""
    import sys
    
    # Configurar URL base
    base_url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
    
    print(f"🔧 Configurando pruebas para: {base_url}")
    
    # Crear tester y ejecutar pruebas
    tester = ConfidenceScoringTester(base_url)
    tester.run_comprehensive_test()

if __name__ == "__main__":
    main() 