#!/usr/bin/env python3
"""
Script de prueba para McpActionRegistry
Verifica que el registro de acciones MCP se carga correctamente y los endpoints funcionan
"""

import requests
import json
import time
import sys
from typing import Dict, Any

class McpRegistryTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.session = requests.Session()
        
    def test_health(self) -> bool:
        """Prueba el endpoint de health"""
        print("🔍 Probando health check...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/health")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Health: {data.get('status', 'UNKNOWN')}")
                return data.get('status') == 'HEALTHY'
            else:
                print(f"❌ Health check falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en health check: {e}")
            return False
    
    def test_statistics(self) -> bool:
        """Prueba el endpoint de estadísticas"""
        print("📊 Probando estadísticas...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/statistics")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Estadísticas cargadas:")
                print(f"   - Estado: {data.get('status')}")
                print(f"   - Versión: {data.get('version')}")
                print(f"   - Servicios: {data.get('serviceCount')} ({data.get('enabledServiceCount')} habilitados)")
                print(f"   - Acciones: {data.get('totalActionCount')} ({data.get('totalEnabledActionCount')} habilitadas)")
                print(f"   - Hot-reload: {data.get('hotReloadEnabled')}")
                return True
            else:
                print(f"❌ Estadísticas fallaron: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en estadísticas: {e}")
            return False
    
    def test_get_all_services(self) -> bool:
        """Prueba obtener todos los servicios"""
        print("🔧 Probando obtener todos los servicios...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/services")
            if response.status_code == 200:
                services = response.json()
                print(f"✅ Servicios obtenidos: {len(services)}")
                
                # Mostrar algunos servicios
                for i, (service_id, service) in enumerate(list(services.items())[:3]):
                    print(f"   - {service_id}: {service.get('name', 'Sin nombre')}")
                
                return len(services) > 0
            else:
                print(f"❌ Obtener servicios falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo servicios: {e}")
            return False
    
    def test_get_enabled_services(self) -> bool:
        """Prueba obtener servicios habilitados"""
        print("🔧 Probando obtener servicios habilitados...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/services/enabled")
            if response.status_code == 200:
                services = response.json()
                print(f"✅ Servicios habilitados: {len(services)}")
                return True
            else:
                print(f"❌ Obtener servicios habilitados falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo servicios habilitados: {e}")
            return False
    
    def test_get_specific_service(self, service_id: str = "weather-mcp") -> bool:
        """Prueba obtener un servicio específico"""
        print(f"🔧 Probando obtener servicio específico: {service_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/services/{service_id}")
            if response.status_code == 200:
                service = response.json()
                print(f"✅ Servicio obtenido:")
                print(f"   - Nombre: {service.get('name')}")
                print(f"   - URL: {service.get('url')}")
                print(f"   - Acciones: {service.get('actionCount', 0)}")
                return True
            else:
                print(f"❌ Obtener servicio falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo servicio: {e}")
            return False
    
    def test_get_service_actions(self, service_id: str = "weather-mcp") -> bool:
        """Prueba obtener acciones de un servicio"""
        print(f"🎯 Probando obtener acciones del servicio: {service_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/services/{service_id}/actions")
            if response.status_code == 200:
                actions = response.json()
                print(f"✅ Acciones obtenidas: {len(actions)}")
                
                for action_id, action in actions.items():
                    print(f"   - {action_id}: {action.get('description')}")
                
                return len(actions) > 0
            else:
                print(f"❌ Obtener acciones falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo acciones: {e}")
            return False
    
    def test_get_all_actions(self) -> bool:
        """Prueba obtener todas las acciones"""
        print("🎯 Probando obtener todas las acciones...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/actions")
            if response.status_code == 200:
                actions = response.json()
                print(f"✅ Acciones obtenidas: {len(actions)}")
                
                # Mostrar algunas acciones
                for i, (action_id, action) in enumerate(list(actions.items())[:3]):
                    print(f"   - {action_id}: {action.get('description')}")
                
                return len(actions) > 0
            else:
                print(f"❌ Obtener acciones falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo acciones: {e}")
            return False
    
    def test_get_specific_action(self, action_id: str = "consultar_tiempo") -> bool:
        """Prueba obtener una acción específica"""
        print(f"🎯 Probando obtener acción específica: {action_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/actions/{action_id}")
            if response.status_code == 200:
                action = response.json()
                print(f"✅ Acción obtenida:")
                print(f"   - Descripción: {action.get('description')}")
                print(f"   - Endpoint: {action.get('endpoint')}")
                print(f"   - Método: {action.get('method')}")
                return True
            else:
                print(f"❌ Obtener acción falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo acción: {e}")
            return False
    
    def test_get_service_for_action(self, action_id: str = "consultar_tiempo") -> bool:
        """Prueba obtener el servicio de una acción"""
        print(f"🔧 Probando obtener servicio para acción: {action_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/actions/{action_id}/service")
            if response.status_code == 200:
                service = response.json()
                print(f"✅ Servicio obtenido:")
                print(f"   - Nombre: {service.get('name')}")
                print(f"   - URL: {service.get('url')}")
                return True
            else:
                print(f"❌ Obtener servicio para acción falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo servicio para acción: {e}")
            return False
    
    def test_get_actions_by_method(self) -> bool:
        """Prueba obtener acciones por método HTTP"""
        print("🌐 Probando obtener acciones por método HTTP...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/actions/methods")
            if response.status_code == 200:
                methods = response.json()
                print(f"✅ Métodos obtenidos: {len(methods)}")
                
                for method, actions in methods.items():
                    print(f"   - {method}: {len(actions)} acciones")
                
                return len(methods) > 0
            else:
                print(f"❌ Obtener métodos falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo métodos: {e}")
            return False
    
    def test_search_actions(self, query: str = "tiempo") -> bool:
        """Prueba buscar acciones"""
        print(f"🔍 Probando búsqueda de acciones: '{query}'")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/actions/search", 
                                      params={"query": query})
            if response.status_code == 200:
                results = response.json()
                print(f"✅ Búsqueda completada: {len(results)} resultados")
                
                for action_id, action in results.items():
                    print(f"   - {action_id}: {action.get('description')}")
                
                return len(results) > 0
            else:
                print(f"❌ Búsqueda falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en búsqueda: {e}")
            return False
    
    def test_get_action_params(self, action_id: str = "consultar_tiempo") -> bool:
        """Prueba obtener parámetros de una acción"""
        print(f"🏷️ Probando obtener parámetros de: {action_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/actions/{action_id}/params")
            if response.status_code == 200:
                params = response.json()
                print(f"✅ Parámetros obtenidos:")
                print(f"   - Requeridos: {params.get('required', [])}")
                print(f"   - Opcionales: {params.get('optional', [])}")
                return True
            else:
                print(f"❌ Obtener parámetros falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo parámetros: {e}")
            return False
    
    def test_get_fallback_responses(self) -> bool:
        """Prueba obtener respuestas de fallback"""
        print("🔄 Probando obtener respuestas de fallback...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/mcp-registry/fallback-responses")
            if response.status_code == 200:
                responses = response.json()
                print(f"✅ Respuestas de fallback obtenidas: {len(responses)}")
                
                for response_type, message in responses.items():
                    print(f"   - {response_type}: {message[:50]}...")
                
                return len(responses) > 0
            else:
                print(f"❌ Obtener respuestas de fallback falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo respuestas de fallback: {e}")
            return False
    
    def test_reload_registry(self) -> bool:
        """Prueba recargar el registro"""
        print("🔄 Probando recarga del registro...")
        try:
            response = self.session.post(f"{self.base_url}/api/v1/mcp-registry/reload")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Registro recargado: {data.get('message')}")
                return True
            else:
                print(f"❌ Recarga falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en recarga: {e}")
            return False
    
    def run_all_tests(self) -> bool:
        """Ejecuta todas las pruebas"""
        print("🚀 Iniciando pruebas de McpActionRegistry")
        print("=" * 50)
        
        tests = [
            ("Health Check", self.test_health),
            ("Statistics", self.test_statistics),
            ("Get All Services", self.test_get_all_services),
            ("Get Enabled Services", self.test_get_enabled_services),
            ("Get Specific Service", lambda: self.test_get_specific_service("weather-mcp")),
            ("Get Service Actions", lambda: self.test_get_service_actions("weather-mcp")),
            ("Get All Actions", self.test_get_all_actions),
            ("Get Specific Action", lambda: self.test_get_specific_action("consultar_tiempo")),
            ("Get Service for Action", lambda: self.test_get_service_for_action("consultar_tiempo")),
            ("Get Actions by Method", self.test_get_actions_by_method),
            ("Search Actions", lambda: self.test_search_actions("tiempo")),
            ("Get Action Params", lambda: self.test_get_action_params("consultar_tiempo")),
            ("Get Fallback Responses", self.test_get_fallback_responses),
            ("Reload Registry", self.test_reload_registry),
        ]
        
        passed = 0
        total = len(tests)
        
        for test_name, test_func in tests:
            print(f"\n🧪 {test_name}")
            print("-" * 30)
            
            try:
                if test_func():
                    passed += 1
                    print(f"✅ {test_name} - PASÓ")
                else:
                    print(f"❌ {test_name} - FALLÓ")
            except Exception as e:
                print(f"❌ {test_name} - ERROR: {e}")
            
            time.sleep(0.5)  # Pequeña pausa entre pruebas
        
        print("\n" + "=" * 50)
        print(f"📊 Resultados: {passed}/{total} pruebas pasaron")
        
        if passed == total:
            print("🎉 ¡Todas las pruebas pasaron exitosamente!")
            return True
        else:
            print("⚠️  Algunas pruebas fallaron")
            return False

def main():
    """Función principal"""
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    else:
        base_url = "http://localhost:9904"
    
    print(f"🎯 Probando McpActionRegistry en: {base_url}")
    
    tester = McpRegistryTester(base_url)
    success = tester.run_all_tests()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 