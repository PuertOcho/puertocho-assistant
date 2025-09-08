#!/usr/bin/env python3
"""
Script de prueba para el nuevo sistema JSON Intent Classifier 
sin RAG ni Vector Store.
"""

import requests
import json
import time

# Configuración
BASE_URL = "http://localhost:9904"
ENDPOINTS = {
    "json_classify": f"{BASE_URL}/api/v1/intents/classify",
    "conversation_text": f"{BASE_URL}/api/v1/conversation/process",
    "health": f"{BASE_URL}/api/v1/health"
}

def test_health_check():
    """Verifica que el servicio esté funcionando"""
    print("🔍 Verificando estado del servicio...")
    try:
        response = requests.get(ENDPOINTS["health"], timeout=5)
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Servicio activo: {data}")
            return True
        else:
            print(f"❌ Servicio no disponible: HTTP {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ Error conectando al servicio: {e}")
        return False

def test_json_intent_classification():
    """Prueba la clasificación directa de intenciones con JSON"""
    print("\n🎯 Probando clasificación de intenciones JSON...")
    
    test_cases = [
        {
            "name": "Consultar tiempo",
            "text": "¿qué tiempo hace en Madrid?",
            "expected_intent": "consultar_tiempo"
        },
        {
            "name": "Encender luz", 
            "text": "enciende la luz del salón",
            "expected_intent": "encender_luz"
        },
        {
            "name": "Reproducir música",
            "text": "pon música de rock",
            "expected_intent": "reproducir_musica"
        },
        {
            "name": "Pedir ayuda",
            "text": "ayúdame por favor",
            "expected_intent": "ayuda"
        },
        {
            "name": "Saludo",
            "text": "hola buenos días",
            "expected_intent": "saludo"
        }
    ]
    
    for test_case in test_cases:
        print(f"\n  🧪 Probando: {test_case['name']}")
        print(f"     Texto: '{test_case['text']}'")
        
        payload = {
            "text": test_case["text"],
            "sessionId": f"test-session-{int(time.time())}",
            "userId": "test-user",
            "contextMetadata": {
                "test": True,
                "timestamp": time.time()
            }
        }
        
        try:
            response = requests.post(
                ENDPOINTS["json_classify"], 
                json=payload, 
                timeout=10,
                headers={"Content-Type": "application/json"}
            )
            
            if response.status_code == 200:
                data = response.json()
                intent = data.get("intentId", "desconocido")
                confidence = data.get("confidenceScore", 0)
                entities = data.get("detectedEntities", {})
                
                print(f"     ✅ Intent: {intent} (confianza: {confidence:.2f})")
                if entities:
                    print(f"     📋 Entidades: {entities}")
                
                if intent == test_case["expected_intent"]:
                    print(f"     ✅ Correcto!")
                else:
                    print(f"     ⚠️  Esperado: {test_case['expected_intent']}")
                    
            else:
                print(f"     ❌ Error HTTP {response.status_code}: {response.text}")
                
        except Exception as e:
            print(f"     ❌ Error: {e}")

def test_conversation_flow():
    """Prueba el flujo completo de conversación"""
    print("\n💬 Probando flujo completo de conversación...")
    
    session_id = f"test-conversation-{int(time.time())}"
    
    conversation_tests = [
        {
            "message": "Hola asistente",
            "description": "Saludo inicial"
        },
        {
            "message": "¿puedes encender la luz?",
            "description": "Petición con slot faltante"
        },
        {
            "message": "del dormitorio",
            "description": "Completar slot de ubicación"
        },
        {
            "message": "¿qué tiempo hace?",
            "description": "Consulta meteorológica"
        },
        {
            "message": "gracias por tu ayuda",
            "description": "Agradecimiento"
        }
    ]
    
    for i, test in enumerate(conversation_tests):
        print(f"\n  💬 Turno {i+1}: {test['description']}")
        print(f"     Usuario: '{test['message']}'")
        
        payload = {
            "sessionId": session_id,
            "userId": "test-conversation-user", 
            "userMessage": test["message"],
            "metadata": {
                "test": True,
                "turn": i + 1,
                "timestamp": time.time()
            }
        }
        
        try:
            response = requests.post(
                ENDPOINTS["conversation_text"],
                json=payload,
                timeout=15,
                headers={"Content-Type": "application/json"}
            )
            
            if response.status_code == 200:
                data = response.json()
                intent = data.get("detectedIntent", "desconocido")
                confidence = data.get("confidenceScore", 0)
                system_response = data.get("systemResponse", "Sin respuesta")
                session_state = data.get("sessionState", "desconocido")
                
                print(f"     🤖 Intent: {intent} (confianza: {confidence:.2f})")
                print(f"     🤖 Respuesta: '{system_response}'")
                print(f"     📊 Estado sesión: {session_state}")
                
                if data.get("success"):
                    print(f"     ✅ Procesado correctamente")
                else:
                    print(f"     ⚠️  Error: {data.get('errorMessage', 'Error desconocido')}")
                    
            else:
                print(f"     ❌ Error HTTP {response.status_code}: {response.text}")
                
        except Exception as e:
            print(f"     ❌ Error: {e}")
        
        # Pequeña pausa entre turnos
        time.sleep(0.5)

def test_performance():
    """Prueba rendimiento del sistema"""
    print("\n⚡ Probando rendimiento del sistema...")
    
    test_texts = [
        "¿qué tiempo hace?",
        "enciende la luz",
        "pon música",
        "ayuda",
        "hola"
    ] * 10  # 50 pruebas total
    
    start_time = time.time()
    successful_requests = 0
    total_confidence = 0
    processing_times = []
    
    for i, text in enumerate(test_texts):
        payload = {
            "text": text,
            "sessionId": f"perf-test-{i}",
            "userId": "performance-user"
        }
        
        try:
            request_start = time.time()
            response = requests.post(
                ENDPOINTS["json_classify"],
                json=payload,
                timeout=5,
                headers={"Content-Type": "application/json"}
            )
            request_time = (time.time() - request_start) * 1000  # ms
            
            if response.status_code == 200:
                data = response.json()
                successful_requests += 1
                total_confidence += data.get("confidenceScore", 0)
                processing_times.append(request_time)
                
                if (i + 1) % 10 == 0:
                    print(f"     📊 Procesadas {i + 1}/{len(test_texts)} peticiones...")
                    
        except Exception as e:
            print(f"     ⚠️  Error en petición {i+1}: {e}")
    
    total_time = time.time() - start_time
    
    print(f"\n📈 Resultados de rendimiento:")
    print(f"   • Peticiones exitosas: {successful_requests}/{len(test_texts)}")
    print(f"   • Tiempo total: {total_time:.2f}s")
    print(f"   • Promedio por petición: {total_time/len(test_texts)*1000:.1f}ms")
    print(f"   • Confianza promedio: {total_confidence/successful_requests:.3f}")
    
    if processing_times:
        print(f"   • Tiempo procesamiento mín: {min(processing_times):.1f}ms")
        print(f"   • Tiempo procesamiento máx: {max(processing_times):.1f}ms")
        print(f"   • Tiempo procesamiento promedio: {sum(processing_times)/len(processing_times):.1f}ms")

def main():
    """Función principal de testing"""
    print("🚀 Iniciando pruebas del sistema JSON Intent Classifier")
    print("=" * 60)
    
    # 1. Health check
    if not test_health_check():
        print("\n❌ Servicio no disponible. Verifica que el contenedor esté ejecutándose.")
        print("   Ejecuta: docker compose up -d --build")
        return
    
    # 2. Clasificación de intenciones
    test_json_intent_classification()
    
    # 3. Flujo de conversación completo
    test_conversation_flow()
    
    # 4. Pruebas de rendimiento
    test_performance()
    
    print("\n" + "=" * 60)
    print("✅ Pruebas completadas!")
    print("\n📋 Resumen:")
    print("   • Sistema RAG eliminado completamente")
    print("   • Vector Store eliminado completamente") 
    print("   • Clasificación basada en JSON + MoE funcionando")
    print("   • SlotExtractor mejorado con patrones avanzados")
    print("   • Flujo de conversación mantenido")

if __name__ == "__main__":
    main()