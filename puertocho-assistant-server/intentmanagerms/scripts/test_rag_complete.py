#!/usr/bin/env python3
"""
Script de prueba completo para el motor RAG de clasificación de intenciones.
Verifica que el servicio esté funcionando y ejecuta todas las pruebas de forma detallada.
"""

import requests
import json
import time
import sys
from typing import Dict, Any, List

class RagCompleteTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        self.test_results = []
        
    def log_test(self, test_name: str, success: bool, details: str = ""):
        """Registra el resultado de una prueba"""
        status = "✅ PASÓ" if success else "❌ FALLÓ"
        print(f"   {status} {test_name}")
        if details:
            print(f"      {details}")
        
        self.test_results.append({
            'test': test_name,
            'success': success,
            'details': details
        })
    
    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        """Espera a que el servicio esté disponible"""
        print("🔄 Esperando a que el servicio esté disponible...")
        
        for attempt in range(max_attempts):
            try:
                response = self.session.get(f"{self.base_url}/actuator/health", timeout=5)
                if response.status_code == 200:
                    print(f"✅ Servicio disponible en intento {attempt + 1}")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            print(f"   Intento {attempt + 1}/{max_attempts} - Esperando {delay}s...")
            time.sleep(delay)
        
        print("❌ Servicio no disponible después de todos los intentos")
        return False
    
    def test_service_availability(self) -> bool:
        """Prueba que el servicio esté disponible"""
        print("\n🔍 1. VERIFICACIÓN DE DISPONIBILIDAD DEL SERVICIO")
        print("-" * 50)
        
        # Test de conectividad básica
        try:
            response = self.session.get(f"{self.base_url}/actuator/health", timeout=10)
            if response.status_code == 200:
                health_data = response.json()
                status = health_data.get('status', 'UNKNOWN')
                self.log_test("Conectividad básica", True, f"Status: {status}")
                return True
            else:
                self.log_test("Conectividad básica", False, f"HTTP {response.status_code}")
                return False
        except Exception as e:
            self.log_test("Conectividad básica", False, f"Error: {e}")
            return False
    
    def test_rag_health(self) -> bool:
        """Prueba el health check específico del motor RAG"""
        print("\n🔍 2. HEALTH CHECK DEL MOTOR RAG")
        print("-" * 50)
        
        try:
            response = self.session.get(f"{self.base_url}/api/v1/rag-classifier/health", timeout=10)
            
            if response.status_code == 200:
                health_data = response.json()
                status = health_data.get('status', 'UNKNOWN')
                components = health_data.get('components', {})
                
                self.log_test("Health check RAG", True, f"Status: {status}")
                self.log_test("Componentes disponibles", True, f"Components: {components}")
                return True
            else:
                self.log_test("Health check RAG", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Health check RAG", False, f"Error: {e}")
            return False
    
    def test_rag_statistics(self) -> bool:
        """Prueba las estadísticas del motor RAG"""
        print("\n🔍 3. ESTADÍSTICAS DEL MOTOR RAG")
        print("-" * 50)
        
        try:
            response = self.session.get(f"{self.base_url}/api/v1/rag-classifier/statistics", timeout=10)
            
            if response.status_code == 200:
                stats = response.json()
                engine_name = stats.get('engine_name', 'N/A')
                version = stats.get('version', 'N/A')
                status = stats.get('status', 'N/A')
                
                self.log_test("Estadísticas disponibles", True, f"Engine: {engine_name} v{version}")
                self.log_test("Estado del motor", status == "ACTIVE", f"Status: {status}")
                
                # Verificar configuración
                config_ok = all(key in stats for key in [
                    'default_max_examples', 'default_confidence_threshold', 
                    'similarity_threshold', 'enable_fallback'
                ])
                self.log_test("Configuración completa", config_ok, "Parámetros RAG configurados")
                
                return True
            else:
                self.log_test("Estadísticas disponibles", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Estadísticas disponibles", False, f"Error: {e}")
            return False
    
    def test_simple_classification(self) -> bool:
        """Prueba clasificación simple de texto"""
        print("\n🔍 4. CLASIFICACIÓN SIMPLE DE TEXTO")
        print("-" * 50)
        
        test_cases = [
            ("¿qué tiempo hace en Madrid?", "consultar_tiempo"),
            ("enciende la luz del salón", "encender_luz"),
            ("ayúdame con algo", "ayuda"),
            ("reproduce música relajante", "reproducir_musica"),
            ("programa una alarma para mañana", "programar_alarma")
        ]
        
        success_count = 0
        
        for text, expected_intent in test_cases:
            try:
                response = self.session.post(
                    f"{self.base_url}/api/v1/rag-classifier/classify",
                    params={'text': text},
                    timeout=15
                )
                
                if response.status_code == 200:
                    result = response.json()
                    intent_id = result.get('intent_id', 'unknown')
                    confidence = result.get('confidence_score', 0.0)
                    processing_time = result.get('processing_time_ms', 0)
                    success = result.get('success', False)
                    
                    if success:
                        success_count += 1
                        self.log_test(f"'{text[:30]}...'", True, 
                                     f"Intent: {intent_id} (conf: {confidence:.2f}, {processing_time}ms)")
                    else:
                        self.log_test(f"'{text[:30]}...'", False, 
                                     f"Success: false, Error: {result.get('error_message', 'Unknown')}")
                else:
                    self.log_test(f"'{text[:30]}...'", False, f"HTTP {response.status_code}")
                    
            except Exception as e:
                self.log_test(f"'{text[:30]}...'", False, f"Error: {e}")
        
        overall_success = success_count == len(test_cases)
        self.log_test("Clasificación simple general", overall_success, 
                     f"{success_count}/{len(test_cases)} exitosas")
        return overall_success
    
    def test_advanced_classification(self) -> bool:
        """Prueba clasificación avanzada con metadata"""
        print("\n🔍 5. CLASIFICACIÓN AVANZADA CON METADATA")
        print("-" * 50)
        
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
                json=request_data,
                timeout=15
            )
            
            if response.status_code == 200:
                result = response.json()
                intent_id = result.get('intent_id', 'unknown')
                confidence = result.get('confidence_score', 0.0)
                rag_examples = result.get('rag_examples_used', [])
                processing_time = result.get('processing_time_ms', 0)
                success = result.get('success', False)
                
                if success:
                    self.log_test("Clasificación avanzada", True, 
                                 f"Intent: {intent_id} (conf: {confidence:.2f})")
                    self.log_test("Ejemplos RAG utilizados", rag_examples is not None and len(rag_examples) > 0, 
                                 f"{len(rag_examples) if rag_examples else 0} ejemplos")
                    self.log_test("Tiempo de procesamiento", processing_time < 10000, 
                                 f"{processing_time}ms")
                    return True
                else:
                    self.log_test("Clasificación avanzada", False, 
                                 f"Success: false, Error: {result.get('error_message', 'Unknown')}")
                    return False
            else:
                self.log_test("Clasificación avanzada", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Clasificación avanzada", False, f"Error: {e}")
            return False
    
    def test_session_classification(self) -> bool:
        """Prueba clasificación con session ID"""
        print("\n🔍 6. CLASIFICACIÓN CON SESSION ID")
        print("-" * 50)
        
        session_id = "test-session-" + str(int(time.time()))
        text = "enciende las luces del dormitorio"
        
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/classify/session/{session_id}",
                params={'text': text},
                timeout=15
            )
            
            if response.status_code == 200:
                result = response.json()
                intent_id = result.get('intent_id', 'unknown')
                confidence = result.get('confidence_score', 0.0)
                success = result.get('success', False)
                
                if success:
                    self.log_test("Clasificación con session", True, 
                                 f"Session: {session_id}, Intent: {intent_id} (conf: {confidence:.2f})")
                    return True
                else:
                    self.log_test("Clasificación con session", False, 
                                 f"Success: false, Error: {result.get('error_message', 'Unknown')}")
                    return False
            else:
                self.log_test("Clasificación con session", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Clasificación con session", False, f"Error: {e}")
            return False
    
    def test_batch_classification(self) -> bool:
        """Prueba clasificación en batch"""
        print("\n🔍 7. CLASIFICACIÓN EN BATCH")
        print("-" * 50)
        
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
                json=batch_data,
                timeout=30
            )
            
            if response.status_code == 200:
                results = response.json()
                success_count = 0
                
                for text_id, result in results.items():
                    intent_id = result.get('intent_id', 'unknown')
                    confidence = result.get('confidence_score', 0.0)
                    success = result.get('success', False)
                    
                    if success:
                        success_count += 1
                        self.log_test(f"Batch {text_id}", True, 
                                     f"Intent: {intent_id} (conf: {confidence:.2f})")
                    else:
                        self.log_test(f"Batch {text_id}", False, 
                                     f"Error: {result.get('error_message', 'Unknown error')}")
                
                overall_success = success_count == len(batch_data)
                self.log_test("Clasificación batch general", overall_success, 
                             f"{success_count}/{len(batch_data)} exitosas")
                return overall_success
            else:
                self.log_test("Clasificación batch", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Clasificación batch", False, f"Error: {e}")
            return False
    
    def test_automated_test(self) -> bool:
        """Ejecuta el test automatizado del motor RAG"""
        print("\n🔍 8. TEST AUTOMATIZADO DEL MOTOR RAG")
        print("-" * 50)
        
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/test",
                timeout=30
            )
            
            if response.status_code == 200:
                test_results = response.json()
                total_tests = test_results.get('total_tests', 0)
                successful_tests = test_results.get('successful_tests', 0)
                success_rate = test_results.get('success_rate', 0.0)
                avg_processing_time = test_results.get('average_processing_time_ms', 0)
                
                self.log_test("Test automatizado ejecutado", True, 
                             f"Total: {total_tests}, Exitosos: {successful_tests}")
                self.log_test("Tasa de éxito", success_rate >= 0.8, 
                             f"Tasa: {success_rate:.2%} (mínimo 80%)")
                self.log_test("Tiempo promedio", avg_processing_time < 5000, 
                             f"Tiempo: {avg_processing_time}ms (máximo 5s)")
                
                # Mostrar resultados individuales
                results = test_results.get('results', {})
                for text, result in results.items():
                    intent_id = result.get('intent_id', 'unknown')
                    confidence = result.get('confidence_score', 0.0)
                    success = result.get('success', False)
                    
                    status = "✅" if success else "❌"
                    print(f"      {status} '{text[:30]}...' → {intent_id} (conf: {confidence:.2f})")
                
                return success_rate >= 0.8
            else:
                self.log_test("Test automatizado", False, f"HTTP {response.status_code}")
                return False
                
        except Exception as e:
            self.log_test("Test automatizado", False, f"Error: {e}")
            return False
    
    def test_error_handling(self) -> bool:
        """Prueba el manejo de errores"""
        print("\n🔍 9. MANEJO DE ERRORES")
        print("-" * 50)
        
        # Test con texto vacío
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/classify",
                params={'text': ''},
                timeout=10
            )
            
            if response.status_code == 200:
                result = response.json()
                if not result.get('success', True) and result.get('error_message'):
                    self.log_test("Texto vacío", True, f"Error manejado correctamente: {result.get('error_message')}")
                else:
                    self.log_test("Texto vacío", False, "No se detectó error para texto vacío")
                    return False
            else:
                self.log_test("Texto vacío", False, f"HTTP {response.status_code} (esperado 200)")
                return False
                
        except Exception as e:
            self.log_test("Texto vacío", False, f"Error: {e}")
            return False
        
        # Test con texto muy largo
        long_text = "a" * 10000
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/rag-classifier/classify",
                params={'text': long_text},
                timeout=15
            )
            
            if response.status_code in [200, 400, 413]:
                self.log_test("Texto largo", True, f"HTTP {response.status_code} - manejado correctamente")
                return True
            else:
                self.log_test("Texto largo", False, f"HTTP {response.status_code} (esperado 200/400/413)")
                return False
                
        except Exception as e:
            self.log_test("Texto largo", False, f"Error: {e}")
            return False
    
    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas"""
        print("🧪 PRUEBAS COMPLETAS DEL MOTOR RAG - INTENTMANAGERMS")
        print("=" * 70)
        print(f"🎯 URL Base: {self.base_url}")
        print(f"⏰ Inicio: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 70)
        
        # Verificar disponibilidad del servicio
        if not self.wait_for_service():
            print("❌ No se pudo conectar al servicio. Verificar que esté ejecutándose.")
            return False
        
        # Ejecutar todas las pruebas
        test_functions = [
            ("Verificación de Disponibilidad", self.test_service_availability),
            ("Health Check del Motor RAG", self.test_rag_health),
            ("Estadísticas del Motor RAG", self.test_rag_statistics),
            ("Clasificación Simple", self.test_simple_classification),
            ("Clasificación Avanzada", self.test_advanced_classification),
            ("Clasificación con Session", self.test_session_classification),
            ("Clasificación en Batch", self.test_batch_classification),
            ("Test Automatizado", self.test_automated_test),
            ("Manejo de Errores", self.test_error_handling)
        ]
        
        passed_tests = 0
        total_tests = len(test_functions)
        
        for test_name, test_func in test_functions:
            print(f"\n🔍 {test_name}")
            print("-" * 50)
            
            try:
                if test_func():
                    print(f"✅ {test_name}: PASÓ")
                    passed_tests += 1
                else:
                    print(f"❌ {test_name}: FALLÓ")
            except Exception as e:
                print(f"❌ {test_name}: ERROR - {e}")
        
        # Resumen final
        print("\n" + "=" * 70)
        print("📊 RESUMEN FINAL DE PRUEBAS")
        print("=" * 70)
        print(f"   Total de pruebas: {total_tests}")
        print(f"   Pruebas pasadas: {passed_tests}")
        print(f"   Pruebas fallidas: {total_tests - passed_tests}")
        print(f"   Tasa de éxito: {(passed_tests/total_tests)*100:.1f}%")
        print(f"   ⏰ Fin: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        
        if passed_tests == total_tests:
            print("\n🎉 ¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE!")
            print("✅ El motor RAG está funcionando correctamente")
        else:
            print(f"\n⚠️ {total_tests - passed_tests} prueba(s) fallaron")
            print("🔍 Revisar logs para más detalles")
        
        print("=" * 70)
        
        return passed_tests == total_tests

def main():
    """Función principal"""
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    else:
        base_url = "http://localhost:9904"
    
    print(f"🚀 Iniciando pruebas completas del motor RAG en: {base_url}")
    
    tester = RagCompleteTester(base_url)
    success = tester.run_complete_test_suite()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 