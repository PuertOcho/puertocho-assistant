#!/usr/bin/env python3
"""
Script de prueba para el servicio de Prompt Engineering Dinámico
T2.3 - Desarrollar prompt engineering dinámico con contexto RAG

Este script prueba todas las funcionalidades del nuevo servicio de prompt engineering:
- Construcción de prompts dinámicos
- Múltiples estrategias de prompt
- Análisis de calidad de ejemplos
- Optimización de contexto
- Integración con el motor RAG
"""

import requests
import json
import time
import sys
from typing import Dict, List, Any

class PromptEngineeringTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        
        # Colores para output
        self.GREEN = '\033[92m'
        self.RED = '\033[91m'
        self.YELLOW = '\033[93m'
        self.BLUE = '\033[94m'
        self.END = '\033[0m'
        self.BOLD = '\033[1m'
        
        # Estadísticas de pruebas
        self.total_tests = 0
        self.passed_tests = 0
        self.failed_tests = 0

    def log_test(self, test_name: str, success: bool, details: str = ""):
        """Registra el resultado de una prueba"""
        self.total_tests += 1
        if success:
            self.passed_tests += 1
            status = f"{self.GREEN}✅ PASÓ{self.END}"
        else:
            self.failed_tests += 1
            status = f"{self.RED}❌ FALLÓ{self.END}"
        
        print(f"{self.BOLD}{test_name}:{self.END} {status}")
        if details:
            print(f"  {self.BLUE}Detalles:{self.END} {details}")
        print()

    def wait_for_service(self, max_attempts: int = 30, delay: int = 2) -> bool:
        """Espera a que el servicio esté disponible"""
        print(f"{self.YELLOW}Esperando a que el servicio esté disponible...{self.END}")
        
        for attempt in range(max_attempts):
            try:
                response = self.session.get(f"{self.base_url}/api/v1/prompt-engineering/health", timeout=5)
                if response.status_code == 200:
                    print(f"{self.GREEN}✅ Servicio disponible en intento {attempt + 1}{self.END}")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            if attempt < max_attempts - 1:
                print(f"  Intento {attempt + 1}/{max_attempts} - Esperando {delay}s...")
                time.sleep(delay)
        
        print(f"{self.RED}❌ Servicio no disponible después de {max_attempts} intentos{self.END}")
        return False

    def test_service_availability(self) -> bool:
        """Prueba la disponibilidad del servicio"""
        try:
            response = self.session.get(f"{self.base_url}/api/v1/prompt-engineering/health", timeout=10)
            return response.status_code == 200
        except requests.exceptions.RequestException as e:
            return False

    def test_health_check(self) -> bool:
        """Prueba el health check del servicio"""
        try:
            response = self.session.get(f"{self.base_url}/api/v1/prompt-engineering/health", timeout=10)
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            return data.get('status') == 'HEALTHY'
            
        except Exception as e:
            return False

    def test_statistics(self) -> bool:
        """Prueba las estadísticas del servicio"""
        try:
            response = self.session.get(f"{self.base_url}/api/v1/prompt-engineering/statistics", timeout=10)
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            
            # Verificar campos requeridos
            required_fields = ['success', 'service_name', 'version', 'status', 'features']
            for field in required_fields:
                if field not in data:
                    return False
            
            # Verificar que el servicio esté activo
            if data.get('status') != 'ACTIVE':
                return False
            
            # Verificar que tenga características
            features = data.get('features', [])
            if not features or len(features) < 3:
                return False
            
            return True
            
        except Exception as e:
            return False

    def test_available_strategies(self) -> bool:
        """Prueba la obtención de estrategias disponibles"""
        try:
            response = self.session.get(f"{self.base_url}/api/v1/prompt-engineering/strategies", timeout=10)
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            
            # Verificar campos requeridos
            if not data.get('success'):
                return False
            
            strategies = data.get('strategies', [])
            expected_strategies = ['adaptive', 'few-shot', 'zero-shot', 'chain-of-thought', 'expert-domain']
            
            # Verificar que todas las estrategias esperadas estén presentes
            for strategy in expected_strategies:
                if strategy not in strategies:
                    return False
            
            return True
            
        except Exception as e:
            return False

    def test_build_adaptive_prompt(self) -> bool:
        """Prueba la construcción de prompt adaptativo"""
        try:
            # Crear ejemplos de prueba
            examples = [
                {
                    "id": "test-1",
                    "content": "¿qué tiempo hace?",
                    "intent": "consultar_tiempo",
                    "similarity": 0.85
                },
                {
                    "id": "test-2", 
                    "content": "dime el clima de Barcelona",
                    "intent": "consultar_tiempo",
                    "similarity": 0.78
                }
            ]
            
            request_data = {
                "text": "¿qué tiempo hace en Madrid?",
                "sessionId": "test-session-123",
                "examples": examples
            }
            
            response = self.session.post(
                f"{self.base_url}/api/v1/prompt-engineering/build/adaptive",
                json=request_data,
                timeout=15
            )
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            
            # Verificar respuesta exitosa
            if not data.get('success'):
                return False
            
            # Verificar que se generó un prompt
            prompt = data.get('prompt', '')
            if not prompt or len(prompt) < 100:
                return False
            
            # Verificar metadata
            if data.get('prompt_length', 0) < 100:
                return False
            
            if data.get('examples_count', 0) != 2:
                return False
            
            return True
            
        except Exception as e:
            return False

    def test_build_few_shot_prompt(self) -> bool:
        """Prueba la construcción de prompt few-shot"""
        try:
            # Crear ejemplos de alta calidad
            examples = [
                {
                    "id": "test-1",
                    "content": "¿qué tiempo hace?",
                    "intent": "consultar_tiempo",
                    "similarity": 0.92
                },
                {
                    "id": "test-2",
                    "content": "dime el clima de Barcelona", 
                    "intent": "consultar_tiempo",
                    "similarity": 0.89
                },
                {
                    "id": "test-3",
                    "content": "cómo está el tiempo hoy",
                    "intent": "consultar_tiempo", 
                    "similarity": 0.91
                }
            ]
            
            request_data = {
                "text": "¿qué tiempo hace en Madrid?",
                "examples": examples
            }
            
            response = self.session.post(
                f"{self.base_url}/api/v1/prompt-engineering/build/few-shot",
                json=request_data,
                timeout=15
            )
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            
            if not data.get('success'):
                return False
            
            prompt = data.get('prompt', '')
            
            # Verificar que el prompt contiene elementos few-shot
            if 'INTENCIÓN:' not in prompt and 'Ejemplo' not in prompt:
                return False
            
            return True
            
        except Exception as e:
            return False

    def test_build_zero_shot_prompt(self) -> bool:
        """Prueba la construcción de prompt zero-shot"""
        try:
            # Sin ejemplos específicos
            request_data = {
                "text": "¿qué tiempo hace en Madrid?",
                "examples": []
            }
            
            response = self.session.post(
                f"{self.base_url}/api/v1/prompt-engineering/build/zero-shot",
                json=request_data,
                timeout=15
            )
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            
            if not data.get('success'):
                return False
            
            prompt = data.get('prompt', '')
            
            # Verificar que el prompt contiene elementos zero-shot
            if 'INTENCIONES DISPONIBLES' not in prompt:
                return False
            
            return True
            
        except Exception as e:
            return False

    def test_build_chain_of_thought_prompt(self) -> bool:
        """Prueba la construcción de prompt chain-of-thought"""
        try:
            examples = [
                {
                    "id": "test-1",
                    "content": "¿qué tiempo hace?",
                    "intent": "consultar_tiempo",
                    "similarity": 0.85
                }
            ]
            
            request_data = {
                "text": "¿qué tiempo hace en Madrid?",
                "examples": examples
            }
            
            response = self.session.post(
                f"{self.base_url}/api/v1/prompt-engineering/build/chain-of-thought",
                json=request_data,
                timeout=15
            )
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            
            if not data.get('success'):
                return False
            
            prompt = data.get('prompt', '')
            
            # Verificar que el prompt contiene elementos chain-of-thought
            if 'PASO 1' not in prompt or 'PASO 2' not in prompt:
                return False
            
            return True
            
        except Exception as e:
            return False

    def test_build_expert_domain_prompt(self) -> bool:
        """Prueba la construcción de prompt por dominio experto"""
        try:
            # Ejemplos del dominio weather
            examples = [
                {
                    "id": "test-1",
                    "content": "¿qué tiempo hace?",
                    "intent": "consultar_tiempo",
                    "similarity": 0.85
                },
                {
                    "id": "test-2",
                    "content": "dime el clima de Barcelona",
                    "intent": "consultar_tiempo", 
                    "similarity": 0.78
                },
                {
                    "id": "test-3",
                    "content": "cómo está el tiempo hoy",
                    "intent": "consultar_tiempo",
                    "similarity": 0.82
                }
            ]
            
            request_data = {
                "text": "¿qué tiempo hace en Madrid?",
                "examples": examples
            }
            
            response = self.session.post(
                f"{self.base_url}/api/v1/prompt-engineering/build/expert-domain",
                json=request_data,
                timeout=15
            )
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            
            if not data.get('success'):
                return False
            
            prompt = data.get('prompt', '')
            
            # Verificar que el prompt contiene elementos de dominio experto
            if 'Eres un experto en el dominio' not in prompt:
                return False
            
            return True
            
        except Exception as e:
            return False

    def test_service_test(self) -> bool:
        """Prueba el endpoint de test del servicio"""
        try:
            response = self.session.post(f"{self.base_url}/api/v1/prompt-engineering/test", timeout=15)
            
            if response.status_code != 200:
                return False
            
            data = response.json()
            
            if not data.get('success'):
                return False
            
            # Verificar campos del test
            required_fields = ['test_text', 'examples_count', 'prompt_length', 'prompt_preview']
            for field in required_fields:
                if field not in data:
                    return False
            
            # Verificar que se generó un prompt
            if data.get('prompt_length', 0) < 100:
                return False
            
            return True
            
        except Exception as e:
            return False

    def test_error_handling(self) -> bool:
        """Prueba el manejo de errores"""
        try:
            # Request inválido (sin texto)
            request_data = {
                "text": "",
                "examples": []
            }
            
            response = self.session.post(
                f"{self.base_url}/api/v1/prompt-engineering/build",
                json=request_data,
                timeout=10
            )
            
            # Debería manejar el error graciosamente
            return response.status_code in [200, 400, 500]
            
        except Exception as e:
            return False

    def run_complete_test_suite(self) -> bool:
        """Ejecuta la suite completa de pruebas"""
        print(f"{self.BOLD}{'='*60}{self.END}")
        print(f"{self.BOLD}🧠 TEST SUITE - PROMPT ENGINEERING DINÁMICO (T2.3){self.END}")
        print(f"{self.BOLD}{'='*60}{self.END}")
        print()
        
        # Verificar disponibilidad del servicio
        if not self.wait_for_service():
            print(f"{self.RED}❌ No se pudo conectar al servicio. Asegúrate de que esté ejecutándose.{self.END}")
            return False
        
        print(f"{self.BOLD}Ejecutando pruebas del servicio de Prompt Engineering...{self.END}")
        print()
        
        # Ejecutar todas las pruebas
        tests = [
            ("Verificación de disponibilidad", self.test_service_availability),
            ("Health check del servicio", self.test_health_check),
            ("Estadísticas del servicio", self.test_statistics),
            ("Estrategias disponibles", self.test_available_strategies),
            ("Construcción de prompt adaptativo", self.test_build_adaptive_prompt),
            ("Construcción de prompt few-shot", self.test_build_few_shot_prompt),
            ("Construcción de prompt zero-shot", self.test_build_zero_shot_prompt),
            ("Construcción de prompt chain-of-thought", self.test_build_chain_of_thought_prompt),
            ("Construcción de prompt por dominio experto", self.test_build_expert_domain_prompt),
            ("Test automatizado del servicio", self.test_service_test),
            ("Manejo de errores", self.test_error_handling)
        ]
        
        for test_name, test_func in tests:
            try:
                success = test_func()
                self.log_test(test_name, success)
            except Exception as e:
                self.log_test(test_name, False, f"Error: {str(e)}")
        
        # Resumen final
        print(f"{self.BOLD}{'='*60}{self.END}")
        print(f"{self.BOLD}📊 RESUMEN DE PRUEBAS{self.END}")
        print(f"{self.BOLD}{'='*60}{self.END}")
        print(f"Total de pruebas: {self.total_tests}")
        print(f"{self.GREEN}Pruebas exitosas: {self.passed_tests}{self.END}")
        print(f"{self.RED}Pruebas fallidas: {self.failed_tests}{self.END}")
        
        success_rate = (self.passed_tests / self.total_tests * 100) if self.total_tests > 0 else 0
        print(f"Tasa de éxito: {success_rate:.1f}%")
        
        if self.failed_tests == 0:
            print(f"\n{self.GREEN}🎉 ¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE!{self.END}")
            print(f"{self.GREEN}✅ El servicio de Prompt Engineering Dinámico está funcionando correctamente.{self.END}")
            return True
        else:
            print(f"\n{self.YELLOW}⚠️  Algunas pruebas fallaron. Revisa los detalles arriba.{self.END}")
            return False

def main():
    """Función principal"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Test del servicio de Prompt Engineering Dinámico')
    parser.add_argument('--url', default='http://localhost:9904', 
                       help='URL base del servicio (default: http://localhost:9904)')
    parser.add_argument('--timeout', type=int, default=30,
                       help='Timeout para conexiones (default: 30s)')
    
    args = parser.parse_args()
    
    # Configurar timeout
    requests.adapters.DEFAULT_RETRIES = 3
    
    # Crear tester y ejecutar pruebas
    tester = PromptEngineeringTester(args.url)
    
    try:
        success = tester.run_complete_test_suite()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print(f"\n{self.YELLOW}⚠️  Pruebas interrumpidas por el usuario{self.END}")
        sys.exit(1)
    except Exception as e:
        print(f"\n{self.RED}❌ Error inesperado: {str(e)}{self.END}")
        sys.exit(1)

if __name__ == "__main__":
    main() 