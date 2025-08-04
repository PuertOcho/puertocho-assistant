#!/usr/bin/env python3
"""
Script de prueba para IntentConfigManager
Verifica que la configuración de intenciones se carga correctamente y los endpoints funcionan
"""

import requests
import json
import time
import sys
from typing import Dict, Any

class IntentConfigTester:
    def __init__(self, base_url: str = "http://localhost:8082"):
        self.base_url = base_url
        self.session = requests.Session()
        
    def test_health(self) -> bool:
        """Prueba el endpoint de health"""
        print("🔍 Probando health check...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/health")
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
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/statistics")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Estadísticas cargadas:")
                print(f"   - Estado: {data.get('status')}")
                print(f"   - Versión: {data.get('version')}")
                print(f"   - Intenciones: {data.get('intentCount')}")
                print(f"   - Ejemplos totales: {data.get('totalExampleCount')}")
                print(f"   - Hot-reload: {data.get('hotReloadEnabled')}")
                return True
            else:
                print(f"❌ Estadísticas fallaron: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en estadísticas: {e}")
            return False
    
    def test_get_all_intents(self) -> bool:
        """Prueba obtener todas las intenciones"""
        print("🎯 Probando obtener todas las intenciones...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/intents")
            if response.status_code == 200:
                intents = response.json()
                print(f"✅ Intenciones obtenidas: {len(intents)}")
                
                # Mostrar algunas intenciones
                for i, (intent_id, intent) in enumerate(list(intents.items())[:3]):
                    print(f"   - {intent_id}: {intent.get('description', 'Sin descripción')}")
                
                return len(intents) > 0
            else:
                print(f"❌ Obtener intenciones falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo intenciones: {e}")
            return False
    
    def test_get_specific_intent(self, intent_id: str = "consultar_tiempo") -> bool:
        """Prueba obtener una intención específica"""
        print(f"🎯 Probando obtener intención específica: {intent_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/intents/{intent_id}")
            if response.status_code == 200:
                intent = response.json()
                print(f"✅ Intención obtenida:")
                print(f"   - Descripción: {intent.get('description')}")
                print(f"   - Ejemplos: {len(intent.get('examples', []))}")
                print(f"   - Acción MCP: {intent.get('mcp_action')}")
                return True
            else:
                print(f"❌ Obtener intención falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo intención: {e}")
            return False
    
    def test_get_intents_by_domain(self) -> bool:
        """Prueba obtener intenciones por dominio"""
        print("🏷️ Probando obtener intenciones por dominio...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/intents/domains")
            if response.status_code == 200:
                domains = response.json()
                print(f"✅ Dominios obtenidos: {len(domains)}")
                
                for domain, intents in domains.items():
                    print(f"   - {domain}: {len(intents)} intenciones")
                
                return len(domains) > 0
            else:
                print(f"❌ Obtener dominios falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo dominios: {e}")
            return False
    
    def test_get_mcp_actions(self) -> bool:
        """Prueba obtener acciones MCP disponibles"""
        print("🔧 Probando obtener acciones MCP...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/mcp-actions")
            if response.status_code == 200:
                actions = response.json()
                print(f"✅ Acciones MCP obtenidas: {len(actions)}")
                
                for action in list(actions)[:5]:  # Mostrar primeras 5
                    print(f"   - {action}")
                
                return len(actions) > 0
            else:
                print(f"❌ Obtener acciones MCP falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo acciones MCP: {e}")
            return False
    
    def test_search_intents(self, query: str = "tiempo") -> bool:
        """Prueba buscar intenciones"""
        print(f"🔍 Probando búsqueda de intenciones: '{query}'")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/intents/search", 
                                      params={"query": query})
            if response.status_code == 200:
                results = response.json()
                print(f"✅ Búsqueda completada: {len(results)} resultados")
                
                for intent_id, intent in results.items():
                    print(f"   - {intent_id}: {intent.get('description')}")
                
                return len(results) > 0
            else:
                print(f"❌ Búsqueda falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en búsqueda: {e}")
            return False
    
    def test_reload_configuration(self) -> bool:
        """Prueba recargar la configuración"""
        print("🔄 Probando recarga de configuración...")
        try:
            response = self.session.post(f"{self.base_url}/api/v1/intent-config/reload")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Configuración recargada: {data.get('message')}")
                return True
            else:
                print(f"❌ Recarga falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en recarga: {e}")
            return False
    
    def test_get_intent_examples(self, intent_id: str = "consultar_tiempo") -> bool:
        """Prueba obtener ejemplos de una intención"""
        print(f"📝 Probando obtener ejemplos de: {intent_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/intents/{intent_id}/examples")
            if response.status_code == 200:
                examples = response.json()
                print(f"✅ Ejemplos obtenidos: {len(examples)}")
                
                for i, example in enumerate(examples[:3]):  # Mostrar primeros 3
                    print(f"   - {example}")
                
                return len(examples) > 0
            else:
                print(f"❌ Obtener ejemplos falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo ejemplos: {e}")
            return False
    
    def test_get_intent_entities(self, intent_id: str = "consultar_tiempo") -> bool:
        """Prueba obtener entidades de una intención"""
        print(f"🏷️ Probando obtener entidades de: {intent_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/intent-config/intents/{intent_id}/entities")
            if response.status_code == 200:
                entities = response.json()
                print(f"✅ Entidades obtenidas:")
                print(f"   - Requeridas: {entities.get('required', [])}")
                print(f"   - Opcionales: {entities.get('optional', [])}")
                return True
            else:
                print(f"❌ Obtener entidades falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo entidades: {e}")
            return False
    
    def run_all_tests(self) -> bool:
        """Ejecuta todas las pruebas"""
        print("🚀 Iniciando pruebas de IntentConfigManager")
        print("=" * 50)
        
        tests = [
            ("Health Check", self.test_health),
            ("Statistics", self.test_statistics),
            ("Get All Intents", self.test_get_all_intents),
            ("Get Specific Intent", lambda: self.test_get_specific_intent("consultar_tiempo")),
            ("Get Intents by Domain", self.test_get_intents_by_domain),
            ("Get MCP Actions", self.test_get_mcp_actions),
            ("Search Intents", lambda: self.test_search_intents("tiempo")),
            ("Get Intent Examples", lambda: self.test_get_intent_examples("consultar_tiempo")),
            ("Get Intent Entities", lambda: self.test_get_intent_entities("consultar_tiempo")),
            ("Reload Configuration", self.test_reload_configuration),
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
        base_url = "http://localhost:8082"
    
    print(f"🎯 Probando IntentConfigManager en: {base_url}")
    
    tester = IntentConfigTester(base_url)
    success = tester.run_all_tests()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 