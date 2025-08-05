#!/usr/bin/env python3
"""
Script de pruebas automatizadas para el ConsensusEngine.
Prueba todos los algoritmos de consenso y funcionalidades del motor.

T3.3: Pruebas automatizadas para ConsensusEngine
"""

import requests
import json
import time
import sys
from typing import Dict, List, Any

# Configuración
BASE_URL = "http://localhost:9904"
CONSENSUS_BASE_URL = f"{BASE_URL}/api/v1/consensus"

def print_header(title: str):
    """Imprime un encabezado formateado."""
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")

def print_success(message: str):
    """Imprime un mensaje de éxito."""
    print(f"✅ {message}")

def print_error(message: str):
    """Imprime un mensaje de error."""
    print(f"❌ {message}")

def print_info(message: str):
    """Imprime un mensaje informativo."""
    print(f"ℹ️  {message}")

def check_service_availability() -> bool:
    """Verifica si el servicio está disponible."""
    try:
        response = requests.get(f"{BASE_URL}/actuator/health", timeout=5)
        if response.status_code == 200:
            print_success("Servicio disponible")
            return True
        else:
            print_error(f"Servicio no disponible: {response.status_code}")
            return False
    except Exception as e:
        print_error(f"Error conectando al servicio: {e}")
        return False

def test_consensus_health() -> bool:
    """Prueba el endpoint de salud del ConsensusEngine."""
    print_header("PRUEBA DE SALUD DEL CONSENSUS ENGINE")
    
    try:
        response = requests.get(f"{CONSENSUS_BASE_URL}/health", timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            print_success(f"Health check exitoso: {data.get('status')}")
            print_info(f"Servicio: {data.get('service')}")
            print_info(f"Versión: {data.get('version')}")
            return True
        else:
            print_error(f"Health check falló: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en health check: {e}")
        return False

def test_consensus_statistics() -> bool:
    """Prueba el endpoint de estadísticas del ConsensusEngine."""
    print_header("PRUEBA DE ESTADÍSTICAS DEL CONSENSUS ENGINE")
    
    try:
        response = requests.get(f"{CONSENSUS_BASE_URL}/statistics", timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Estadísticas obtenidas exitosamente")
            print_info(f"Algoritmo: {data.get('algorithm')}")
            print_info(f"Umbral de confianza: {data.get('confidenceThreshold')}")
            print_info(f"Votos mínimos: {data.get('minimumVotes')}")
            print_info(f"Scoring ponderado: {data.get('weightedScoringEnabled')}")
            print_info(f"Boost de confianza: {data.get('confidenceBoostingEnabled')}")
            return True
        else:
            print_error(f"Error obteniendo estadísticas: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en estadísticas: {e}")
        return False

def test_consensus_engine() -> bool:
    """Prueba el motor de consenso con datos de ejemplo."""
    print_header("PRUEBA DEL MOTOR DE CONSENSO")
    
    try:
        response = requests.post(f"{CONSENSUS_BASE_URL}/test", timeout=30)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Prueba del motor de consenso exitosa")
            print_info(f"Votos de prueba: {data.get('testVotes')}")
            
            consensus = data.get('consensus', {})
            print_info(f"Intención final: {consensus.get('finalIntent')}")
            print_info(f"Confianza: {consensus.get('consensusConfidence')}")
            print_info(f"Votos participantes: {consensus.get('participatingVotes')}")
            print_info(f"Método de consenso: {consensus.get('consensusMethod')}")
            print_info(f"Nivel de acuerdo: {consensus.get('agreementLevel')}")
            
            return True
        else:
            print_error(f"Error en prueba del motor: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en prueba del motor: {e}")
        return False

def test_consensus_algorithms() -> bool:
    """Prueba diferentes algoritmos de consenso."""
    print_header("PRUEBA DE ALGORITMOS DE CONSENSO")
    
    try:
        response = requests.post(f"{CONSENSUS_BASE_URL}/test-algorithms", timeout=60)
        
        if response.status_code == 200:
            data = response.json()
            print_success("Prueba de algoritmos exitosa")
            print_info(f"Votos de prueba: {data.get('testVotes')}")
            
            algorithm_results = data.get('algorithmResults', {})
            for algorithm, result in algorithm_results.items():
                print_info(f"\nAlgoritmo: {algorithm}")
                print_info(f"  Intención: {result.get('finalIntent')}")
                print_info(f"  Confianza: {result.get('consensusConfidence')}")
                print_info(f"  Método: {result.get('consensusMethod')}")
                print_info(f"  Nivel de acuerdo: {result.get('agreementLevel')}")
            
            return True
        else:
            print_error(f"Error en prueba de algoritmos: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en prueba de algoritmos: {e}")
        return False

def test_custom_consensus() -> bool:
    """Prueba consenso con votos personalizados."""
    print_header("PRUEBA DE CONSENSO PERSONALIZADO")
    
    # Crear votos personalizados
    custom_votes = [
        {
            "voteId": "custom-vote-1",
            "llmId": "llm_a",
            "llmName": "Critical Analyzer",
            "intent": "buscar_informacion",
            "confidence": 0.88,
            "llmWeight": 1.0,
            "entities": {"tipo_busqueda": "general", "fuente": "web"},
            "subtasks": [{"accion": "buscar_web", "prioridad": "alta"}],
            "reasoning": "El usuario busca información general en la web"
        },
        {
            "voteId": "custom-vote-2",
            "llmId": "llm_b",
            "llmName": "Context Specialist",
            "intent": "buscar_informacion",
            "confidence": 0.92,
            "llmWeight": 1.0,
            "entities": {"tipo_busqueda": "general", "fuente": "web", "contexto": "investigacion"},
            "subtasks": [{"accion": "buscar_web", "prioridad": "alta"}],
            "reasoning": "Basado en el contexto, el usuario necesita información para investigación"
        },
        {
            "voteId": "custom-vote-3",
            "llmId": "llm_c",
            "llmName": "Action Pragmatist",
            "intent": "buscar_informacion",
            "confidence": 0.75,
            "llmWeight": 0.9,
            "entities": {"tipo_busqueda": "general"},
            "subtasks": [
                {"accion": "buscar_web", "prioridad": "alta"},
                {"accion": "filtrar_resultados", "prioridad": "media"}
            ],
            "reasoning": "El usuario necesita información y filtrado de resultados"
        }
    ]
    
    request_data = {
        "requestId": "custom-test-123",
        "userMessage": "Necesito buscar información sobre inteligencia artificial",
        "conversationContext": {"user_type": "researcher", "session_id": "custom-session"},
        "conversationHistory": ["Hola", "¿Puedes ayudarme?"],
        "votes": custom_votes
    }
    
    try:
        response = requests.post(
            f"{CONSENSUS_BASE_URL}/execute",
            json=request_data,
            timeout=30
        )
        
        if response.status_code == 200:
            data = response.json()
            print_success("Consenso personalizado exitoso")
            print_info(f"Votos de entrada: {data.get('inputVotes')}")
            
            consensus = data.get('consensus', {})
            print_info(f"Intención final: {consensus.get('finalIntent')}")
            print_info(f"Confianza: {consensus.get('consensusConfidence')}")
            print_info(f"Método: {consensus.get('consensusMethod')}")
            print_info(f"Nivel de acuerdo: {consensus.get('agreementLevel')}")
            
            # Mostrar entidades combinadas
            entities = consensus.get('finalEntities', {})
            if entities:
                print_info("Entidades combinadas:")
                for key, value in entities.items():
                    print_info(f"  {key}: {value}")
            
            # Mostrar subtareas consolidadas
            subtasks = consensus.get('finalSubtasks', [])
            if subtasks:
                print_info("Subtareas consolidadas:")
                for subtask in subtasks:
                    print_info(f"  {subtask}")
            
            return True
        else:
            print_error(f"Error en consenso personalizado: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en consenso personalizado: {e}")
        return False

def test_error_handling() -> bool:
    """Prueba el manejo de errores del ConsensusEngine."""
    print_header("PRUEBA DE MANEJO DE ERRORES")
    
    # Prueba con votos vacíos
    try:
        response = requests.post(
            f"{CONSENSUS_BASE_URL}/execute",
            json={"votes": []},
            timeout=10
        )
        
        if response.status_code == 400:
            print_success("Manejo correcto de votos vacíos")
        else:
            print_error(f"Error inesperado con votos vacíos: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en prueba de votos vacíos: {e}")
        return False
    
    # Prueba con datos inválidos
    try:
        response = requests.post(
            f"{CONSENSUS_BASE_URL}/execute",
            json={"invalid": "data"},
            timeout=10
        )
        
        if response.status_code == 400:
            print_success("Manejo correcto de datos inválidos")
        else:
            print_error(f"Error inesperado con datos inválidos: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en prueba de datos inválidos: {e}")
        return False
    
    return True

def run_performance_test() -> bool:
    """Ejecuta una prueba de rendimiento del ConsensusEngine."""
    print_header("PRUEBA DE RENDIMIENTO")
    
    # Crear múltiples votos para prueba de rendimiento
    performance_votes = []
    for i in range(10):
        vote = {
            "voteId": f"perf-vote-{i}",
            "llmId": f"llm_{i % 3}",
            "llmName": f"LLM {i % 3}",
            "intent": "ayuda" if i % 2 == 0 else "buscar_informacion",
            "confidence": 0.7 + (i * 0.02),
            "llmWeight": 1.0,
            "entities": {"test": f"value_{i}"},
            "subtasks": [{"accion": f"test_action_{i}"}],
            "reasoning": f"Razonamiento de prueba {i}"
        }
        performance_votes.append(vote)
    
    request_data = {
        "requestId": "performance-test",
        "userMessage": "Mensaje de prueba de rendimiento",
        "votes": performance_votes
    }
    
    start_time = time.time()
    
    try:
        response = requests.post(
            f"{CONSENSUS_BASE_URL}/execute",
            json=request_data,
            timeout=60
        )
        
        end_time = time.time()
        processing_time = end_time - start_time
        
        if response.status_code == 200:
            print_success(f"Prueba de rendimiento exitosa")
            print_info(f"Tiempo de procesamiento: {processing_time:.2f} segundos")
            print_info(f"Votos procesados: {len(performance_votes)}")
            print_info(f"Tiempo promedio por voto: {processing_time/len(performance_votes):.3f} segundos")
            
            data = response.json()
            consensus = data.get('consensus', {})
            print_info(f"Intención final: {consensus.get('finalIntent')}")
            print_info(f"Confianza: {consensus.get('consensusConfidence')}")
            
            return True
        else:
            print_error(f"Error en prueba de rendimiento: {response.status_code}")
            return False
            
    except Exception as e:
        print_error(f"Error en prueba de rendimiento: {e}")
        return False

def main():
    """Función principal que ejecuta todas las pruebas."""
    print_header("PRUEBAS AUTOMATIZADAS DEL CONSENSUS ENGINE")
    print_info("Iniciando pruebas del motor de consenso avanzado...")
    
    # Verificar disponibilidad del servicio
    if not check_service_availability():
        print_error("El servicio no está disponible. Asegúrate de que esté ejecutándose.")
        sys.exit(1)
    
    # Ejecutar todas las pruebas
    tests = [
        ("Salud del ConsensusEngine", test_consensus_health),
        ("Estadísticas del ConsensusEngine", test_consensus_statistics),
        ("Motor de Consenso", test_consensus_engine),
        ("Algoritmos de Consenso", test_consensus_algorithms),
        ("Consenso Personalizado", test_custom_consensus),
        ("Manejo de Errores", test_error_handling),
        ("Prueba de Rendimiento", run_performance_test)
    ]
    
    passed_tests = 0
    total_tests = len(tests)
    
    for test_name, test_function in tests:
        try:
            if test_function():
                passed_tests += 1
            else:
                print_error(f"Prueba '{test_name}' falló")
        except Exception as e:
            print_error(f"Error ejecutando prueba '{test_name}': {e}")
    
    # Resumen final
    print_header("RESUMEN DE PRUEBAS")
    print_info(f"Pruebas ejecutadas: {total_tests}")
    print_info(f"Pruebas exitosas: {passed_tests}")
    print_info(f"Pruebas fallidas: {total_tests - passed_tests}")
    
    success_rate = (passed_tests / total_tests) * 100
    print_info(f"Tasa de éxito: {success_rate:.1f}%")
    
    if passed_tests == total_tests:
        print_success("¡Todas las pruebas pasaron exitosamente!")
        print_success("El ConsensusEngine está funcionando correctamente.")
    else:
        print_error(f"Algunas pruebas fallaron. Revisa los errores anteriores.")
    
    return passed_tests == total_tests

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1) 