#!/usr/bin/env python3
"""
Script de prueba para VectorStoreService
Verifica que el vector store se carga correctamente y los endpoints funcionan
"""

import requests
import json
import time
import sys
import uuid
from typing import Dict, Any, List

class VectorStoreTester:
    def __init__(self, base_url: str = "http://localhost:9904"):
        self.base_url = base_url
        self.session = requests.Session()
        
    def test_health(self) -> bool:
        """Prueba el endpoint de health"""
        print("🔍 Probando health check...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/vector-store/health")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Health: {data.get('status', 'UNKNOWN')}")
                return data.get('healthy', False)
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
            response = self.session.get(f"{self.base_url}/api/v1/vector-store/statistics")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Estadísticas cargadas:")
                print(f"   - Tipo: {data.get('type')}")
                print(f"   - Colección: {data.get('collectionName')}")
                print(f"   - Dimensión: {data.get('embeddingDimension')}")
                print(f"   - Documentos: {data.get('totalDocuments')}")
                print(f"   - Búsquedas: {data.get('totalSearches')}")
                print(f"   - Umbral: {data.get('similarityThreshold')}")
                return True
            else:
                print(f"❌ Estadísticas fallaron: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en estadísticas: {e}")
            return False
    
    def test_info(self) -> bool:
        """Prueba el endpoint de información"""
        print("ℹ️ Probando información...")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/vector-store/info")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Información obtenida:")
                print(f"   - Tipo: {data.get('type')}")
                print(f"   - Documentos: {data.get('totalDocuments')}")
                print(f"   - Saludable: {data.get('healthy')}")
                return True
            else:
                print(f"❌ Información falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en información: {e}")
            return False
    
    def test_add_document(self) -> str:
        """Prueba añadir un documento"""
        print("📝 Probando añadir documento...")
        try:
            # Crear documento de prueba
            doc_id = str(uuid.uuid4())
            document = {
                "id": doc_id,
                "content": "¿qué tiempo hace hoy en Madrid?",
                "intent": "consultar_tiempo",
                "embedding": self.generate_mock_embedding("¿qué tiempo hace hoy en Madrid?"),
                "metadata": {
                    "description": "Consulta meteorológica",
                    "language": "es",
                    "example_type": "test"
                }
            }
            
            response = self.session.post(
                f"{self.base_url}/api/v1/vector-store/documents",
                json=document
            )
            
            if response.status_code == 200:
                print(f"✅ Documento añadido: {doc_id}")
                return doc_id
            else:
                print(f"❌ Añadir documento falló: {response.status_code}")
                return None
        except Exception as e:
            print(f"❌ Error añadiendo documento: {e}")
            return None
    
    def test_get_document(self, doc_id: str) -> bool:
        """Prueba obtener un documento"""
        print(f"📖 Probando obtener documento: {doc_id}")
        try:
            response = self.session.get(f"{self.base_url}/api/v1/vector-store/documents/{doc_id}")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Documento obtenido:")
                print(f"   - ID: {data.get('id')}")
                print(f"   - Contenido: {data.get('content')}")
                print(f"   - Intención: {data.get('intent')}")
                return True
            else:
                print(f"❌ Obtener documento falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error obteniendo documento: {e}")
            return False
    
    def test_search_by_text(self) -> bool:
        """Prueba búsqueda por texto"""
        print("🔍 Probando búsqueda por texto...")
        try:
            query = "¿cómo está el clima?"
            response = self.session.post(
                f"{self.base_url}/api/v1/vector-store/search/text",
                params={"query": query, "limit": 3}
            )
            
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Búsqueda completada:")
                print(f"   - Consulta: {data.get('query')}")
                print(f"   - Resultados: {data.get('totalResults')}")
                print(f"   - Tiempo: {data.get('searchTimeMs')}ms")
                
                if data.get('documents'):
                    best_match = data['documents'][0]
                    print(f"   - Mejor coincidencia: {best_match.get('content')}")
                    print(f"   - Similitud: {best_match.get('similarity', 0):.3f}")
                
                return True
            else:
                print(f"❌ Búsqueda falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en búsqueda: {e}")
            return False
    
    def test_search_by_embedding(self) -> bool:
        """Prueba búsqueda por embedding"""
        print("🔍 Probando búsqueda por embedding...")
        try:
            query = "enciende la luz del salón"
            embedding = self.generate_mock_embedding(query)
            
            response = self.session.post(
                f"{self.base_url}/api/v1/vector-store/search",
                params={"query": query, "limit": 3},
                json=embedding
            )
            
            if response.status_code == 200:
                data = response.json()
                print(f"✅ Búsqueda por embedding completada:")
                print(f"   - Consulta: {data.get('query')}")
                print(f"   - Resultados: {data.get('totalResults')}")
                print(f"   - Tiempo: {data.get('searchTimeMs')}ms")
                
                if data.get('documents'):
                    best_match = data['documents'][0]
                    print(f"   - Mejor coincidencia: {best_match.get('content')}")
                    print(f"   - Similitud: {best_match.get('similarity', 0):.3f}")
                
                return True
            else:
                print(f"❌ Búsqueda por embedding falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error en búsqueda por embedding: {e}")
            return False
    
    def test_delete_document(self, doc_id: str) -> bool:
        """Prueba eliminar un documento"""
        print(f"🗑️ Probando eliminar documento: {doc_id}")
        try:
            response = self.session.delete(f"{self.base_url}/api/v1/vector-store/documents/{doc_id}")
            if response.status_code == 200:
                print(f"✅ Documento eliminado: {doc_id}")
                return True
            else:
                print(f"❌ Eliminar documento falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error eliminando documento: {e}")
            return False
    
    def test_clear_all(self) -> bool:
        """Prueba limpiar todos los documentos"""
        print("🧹 Probando limpiar todos los documentos...")
        try:
            response = self.session.delete(f"{self.base_url}/api/v1/vector-store/documents")
            if response.status_code == 200:
                print("✅ Todos los documentos eliminados")
                return True
            else:
                print(f"❌ Limpiar documentos falló: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Error limpiando documentos: {e}")
            return False
    
    def test_multiple_documents(self) -> bool:
        """Prueba añadir múltiples documentos"""
        print("📚 Probando añadir múltiples documentos...")
        try:
            documents = [
                {
                    "id": str(uuid.uuid4()),
                    "content": "programa una alarma para mañana a las 8",
                    "intent": "programar_alarma",
                    "embedding": self.generate_mock_embedding("programa una alarma para mañana a las 8"),
                    "metadata": {"language": "es", "example_type": "test"}
                },
                {
                    "id": str(uuid.uuid4()),
                    "content": "reproduce música relajante",
                    "intent": "reproducir_musica",
                    "embedding": self.generate_mock_embedding("reproduce música relajante"),
                    "metadata": {"language": "es", "example_type": "test"}
                },
                {
                    "id": str(uuid.uuid4()),
                    "content": "apaga todas las luces",
                    "intent": "apagar_luz",
                    "embedding": self.generate_mock_embedding("apaga todas las luces"),
                    "metadata": {"language": "es", "example_type": "test"}
                }
            ]
            
            added_count = 0
            for doc in documents:
                response = self.session.post(
                    f"{self.base_url}/api/v1/vector-store/documents",
                    json=doc
                )
                if response.status_code == 200:
                    added_count += 1
            
            print(f"✅ {added_count}/{len(documents)} documentos añadidos")
            return added_count == len(documents)
        except Exception as e:
            print(f"❌ Error añadiendo múltiples documentos: {e}")
            return False
    
    def test_similarity_search(self) -> bool:
        """Prueba búsqueda de similitud con diferentes consultas"""
        print("🎯 Probando búsqueda de similitud...")
        try:
            test_queries = [
                "¿qué tiempo hace?",
                "enciende la luz",
                "pon una alarma",
                "reproduce música"
            ]
            
            for query in test_queries:
                print(f"   Probando: '{query}'")
                response = self.session.post(
                    f"{self.base_url}/api/v1/vector-store/search/text",
                    params={"query": query, "limit": 2}
                )
                
                if response.status_code == 200:
                    data = response.json()
                    if data.get('documents'):
                        best_match = data['documents'][0]
                        print(f"     ✅ Mejor: {best_match.get('content')} (sim: {best_match.get('similarity', 0):.3f})")
                    else:
                        print(f"     ⚠️ Sin resultados")
                else:
                    print(f"     ❌ Error: {response.status_code}")
            
            return True
        except Exception as e:
            print(f"❌ Error en búsqueda de similitud: {e}")
            return False
    
    def generate_mock_embedding(self, content: str) -> List[float]:
        """Genera un embedding simulado para testing"""
        import math
        
        embedding = []
        hash_val = hash(content)
        
        for i in range(1536):
            value = math.sin(hash_val + i * 0.1) * 0.5
            embedding.append(float(value))
        
        return embedding
    
    def run_all_tests(self) -> bool:
        """Ejecuta todas las pruebas"""
        print("🚀 Iniciando pruebas de VectorStoreService")
        print("=" * 50)
        
        tests = [
            ("Health Check", self.test_health),
            ("Statistics", self.test_statistics),
            ("Info", self.test_info),
            ("Add Document", lambda: self.test_add_document()),
            ("Get Document", lambda: self.test_get_document(self.test_add_document())),
            ("Search by Text", self.test_search_by_text),
            ("Search by Embedding", self.test_search_by_embedding),
            ("Multiple Documents", self.test_multiple_documents),
            ("Similarity Search", self.test_similarity_search),
            ("Delete Document", lambda: self.test_delete_document(self.test_add_document())),
            ("Clear All", self.test_clear_all),
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
    
    print(f"🎯 Probando VectorStoreService en: {base_url}")
    
    tester = VectorStoreTester(base_url)
    success = tester.run_all_tests()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 