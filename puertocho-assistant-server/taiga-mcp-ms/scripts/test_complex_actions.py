#!/usr/bin/env python3
"""
Script de pruebas para acciones complejas del servicio Taiga MCP
"""

import requests
import json
import time

# Configuración
BASE_URL = "http://localhost:5007"
TAIGA_HOST = "http://host.docker.internal:9000"
USERNAME = "puertocho"
PASSWORD = "puertocho"

# Ejemplo de PROJECT_TRACKER.md
SAMPLE_TRACKER = """# Asistente de IA Avanzado

## Fase 1: Preparación del entorno ✅ COMPLETADA
- [x] Configurar entorno de desarrollo
- [x] Instalar dependencias del sistema
- [x] Configurar variables de entorno
- [x] Documentar instalación en README

## Fase 2: Integración básica ✅ COMPLETADA
- [x] Implementar autenticación básica
- [x] Crear módulo de gestión de sesiones
- [x] Integrar con API externa
- [x] Implementar logging básico

## Fase 3: Funcionalidades avanzadas
- [ ] Implementar procesamiento de comandos complejos
- [ ] Integrar con servicios de IA
- [ ] Crear sistema de plugins
- [ ] Implementar cache inteligente

## Fase 4: Optimización y robustez
- [ ] Optimizar rendimiento
- [ ] Implementar manejo de errores robusto
- [ ] Configurar monitoreo y métricas
- [ ] Realizar pruebas de carga

## Fase 5: Despliegue y documentación
- [ ] Preparar entorno de producción
- [ ] Crear documentación de usuario
- [ ] Implementar CI/CD
- [ ] Realizar pruebas de integración
"""

def authenticate():
    """Autenticarse y obtener session_id"""
    print("🔐 Autenticando...")
    try:
        login_data = {
            "username": USERNAME,
            "password": PASSWORD,
            "host": TAIGA_HOST
        }
        
        response = requests.post(f"{BASE_URL}/login", json=login_data)
        
        if response.status_code == 200:
            data = response.json()
            session_id = data.get("session_id")
            print(f"✅ Autenticación exitosa - Session: {session_id}")
            return session_id
        else:
            print(f"❌ Error en autenticación: {response.status_code}")
            return None
            
    except Exception as e:
        print(f"❌ Error en autenticación: {e}")
        return None

def test_create_project_from_tracker(session_id):
    """Probar creación de proyecto desde PROJECT_TRACKER.md"""
    print("\n📋 Probando creación de proyecto desde tracker...")
    try:
        action_data = {
            "session_id": session_id,
            "action_text": "Crear proyecto desde tracker para el asistente de IA",
            "tracker_content": SAMPLE_TRACKER
        }
        
        response = requests.post(f"{BASE_URL}/execute_complex_action", json=action_data)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Proyecto creado desde tracker:")
            
            for res in result["results"]:
                if res.get("success"):
                    project = res.get("project", {})
                    stories = res.get("created_stories", [])
                    print(f"   Proyecto: {project.get('name')} (ID: {project.get('id')})")
                    print(f"   Historias creadas: {len(stories)}")
                    for story in stories[:3]:  # Mostrar primeras 3
                        print(f"     - #{story['ref']}: {story['subject']}")
                    if len(stories) > 3:
                        print(f"     ... y {len(stories)-3} más")
                    return project.get('id')
                else:
                    print(f"❌ Error: {res.get('error')}")
                    return None
        else:
            print(f"❌ Error en petición: {response.status_code}")
            print(f"   Detalle: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return None

def test_simple_project_creation(session_id):
    """Probar creación de proyecto simple"""
    print("\n🚀 Probando creación de proyecto simple...")
    try:
        action_data = {
            "session_id": session_id,
            "action_text": "Crear proyecto 'Sistema de Monitoreo IoT' para gestionar dispositivos conectados"
        }
        
        response = requests.post(f"{BASE_URL}/execute_complex_action", json=action_data)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Proyecto simple creado:")
            
            for res in result["results"]:
                if res.get("success"):
                    project = res.get("project", {})
                    print(f"   Proyecto: {project.get('name')} (ID: {project.get('id')})")
                    return project.get('id')
                else:
                    print(f"❌ Error: {res.get('error')}")
                    return None
        else:
            print(f"❌ Error en petición: {response.status_code}")
            return None
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return None

def test_user_story_creation(session_id, project_id):
    """Probar creación de historia de usuario"""
    print(f"\n📝 Probando creación de historia de usuario en proyecto {project_id}...")
    try:
        action_data = {
            "session_id": session_id,
            "project_id": project_id,
            "action_text": "Crear historia: Como administrador, quiero monitorear el estado de los dispositivos en tiempo real para poder detectar fallos rápidamente."
        }
        
        response = requests.post(f"{BASE_URL}/execute_complex_action", json=action_data)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Historia de usuario creada:")
            
            for res in result["results"]:
                if res.get("success"):
                    story = res.get("story", {})
                    print(f"   Historia: #{story.get('ref')} - {story.get('subject')}")
                    return story.get('id')
                else:
                    print(f"❌ Error: {res.get('error')}")
                    return None
        else:
            print(f"❌ Error en petición: {response.status_code}")
            return None
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return None

def test_project_analysis(session_id, project_id):
    """Probar análisis de proyecto"""
    print(f"\n📊 Probando análisis de proyecto {project_id}...")
    try:
        action_data = {
            "session_id": session_id,
            "project_id": project_id,
            "action_text": "Analizar proyecto y generar recomendaciones para optimizar el desarrollo con foco en testing y documentación"
        }
        
        response = requests.post(f"{BASE_URL}/execute_complex_action", json=action_data)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Análisis de proyecto completado:")
            
            for res in result["results"]:
                if res.get("success"):
                    analysis = res.get("analysis", {})
                    recommendations = res.get("recommendations", [])
                    
                    print(f"   Proyecto: {analysis.get('project_name')}")
                    print(f"   Total historias: {analysis.get('total_stories')}")
                    print(f"   Completadas: {analysis.get('completed_stories')}")
                    print(f"   Progreso: {analysis.get('progress_percentage'):.1f}%")
                    
                    if recommendations:
                        print("   Recomendaciones:")
                        for rec in recommendations:
                            print(f"     - {rec}")
                else:
                    print(f"❌ Error: {res.get('error')}")
        else:
            print(f"❌ Error en petición: {response.status_code}")
            
    except Exception as e:
        print(f"❌ Error: {e}")

def test_project_report(session_id, project_id):
    """Probar generación de reporte"""
    print(f"\n📋 Probando generación de reporte para proyecto {project_id}...")
    try:
        action_data = {
            "session_id": session_id,
            "project_id": project_id,
            "action_text": "Generar reporte detallado del proyecto"
        }
        
        response = requests.post(f"{BASE_URL}/execute_complex_action", json=action_data)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Reporte generado:")
            
            for res in result["results"]:
                if res.get("success"):
                    report = res.get("report", {})
                    print(f"   Proyecto: {report.get('project_name')}")
                    print(f"   Creado: {report.get('created_date')}")
                    print(f"   Total historias: {report.get('total_stories')}")
                    print(f"   Actividad reciente: {report.get('recent_activity')}")
                    
                    stories_by_status = report.get("stories_by_status", {})
                    if stories_by_status:
                        print("   Estados de historias:")
                        for status, count in stories_by_status.items():
                            print(f"     - {status}: {count}")
                else:
                    print(f"❌ Error: {res.get('error')}")
        else:
            print(f"❌ Error en petición: {response.status_code}")
            
    except Exception as e:
        print(f"❌ Error: {e}")

def test_general_report(session_id):
    """Probar generación de reporte general"""
    print(f"\n📊 Probando generación de reporte general...")
    try:
        action_data = {
            "session_id": session_id,
            "action_text": "Generar reporte general de todos los proyectos"
        }
        
        response = requests.post(f"{BASE_URL}/execute_complex_action", json=action_data)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Reporte general generado:")
            
            for res in result["results"]:
                if res.get("success"):
                    report = res.get("report", {})
                    projects = report.get("projects_summary", [])
                    print(f"   Total proyectos: {report.get('total_projects')}")
                    print("   Proyectos:")
                    for project in projects[:5]:  # Mostrar primeros 5
                        print(f"     - {project.get('name')} (ID: {project.get('id')})")
                    if len(projects) > 5:
                        print(f"     ... y {len(projects)-5} más")
                else:
                    print(f"❌ Error: {res.get('error')}")
        else:
            print(f"❌ Error en petición: {response.status_code}")
            
    except Exception as e:
        print(f"❌ Error: {e}")

def test_generic_parsing(session_id):
    """Probar parsing genérico de comandos"""
    print(f"\n🔍 Probando parsing genérico de comandos...")
    try:
        action_data = {
            "session_id": session_id,
            "action_text": """
            Lista de tareas pendientes:
            - Revisar documentación del API
            - Implementar autenticación OAuth
            - Configurar base de datos de producción
            - Realizar pruebas de carga
            - Preparar despliegue en AWS
            """
        }
        
        response = requests.post(f"{BASE_URL}/execute_complex_action", json=action_data)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Parsing genérico completado:")
            
            for res in result["results"]:
                if res.get("success"):
                    commands = res.get("commands_found", [])
                    print(f"   Comandos encontrados: {len(commands)}")
                    for cmd in commands:
                        print(f"     - {cmd}")
                else:
                    print(f"❌ Error: {res.get('error')}")
        else:
            print(f"❌ Error en petición: {response.status_code}")
            
    except Exception as e:
        print(f"❌ Error: {e}")

def logout(session_id):
    """Cerrar sesión"""
    print(f"\n🚪 Cerrando sesión...")
    try:
        logout_data = {"session_id": session_id}
        response = requests.post(f"{BASE_URL}/logout", json=logout_data)
        
        if response.status_code == 200:
            print("✅ Sesión cerrada exitosamente")
        else:
            print(f"❌ Error cerrando sesión: {response.status_code}")
            
    except Exception as e:
        print(f"❌ Error cerrando sesión: {e}")

def main():
    """Ejecutar todas las pruebas de acciones complejas"""
    print("🧪 Iniciando pruebas de acciones complejas del servicio Taiga MCP")
    print("=" * 70)
    
    # Autenticación
    session_id = authenticate()
    if not session_id:
        print("\n❌ No se pudo autenticar. Terminando pruebas.")
        return
    
    try:
        # Test 1: Crear proyecto desde tracker
        tracker_project_id = test_create_project_from_tracker(session_id)
        
        # Test 2: Crear proyecto simple
        simple_project_id = test_simple_project_creation(session_id)
        
        # Test 3: Crear historia de usuario (usar el proyecto simple)
        if simple_project_id:
            test_user_story_creation(session_id, simple_project_id)
        
        # Test 4: Análisis de proyecto
        if tracker_project_id:
            test_project_analysis(session_id, tracker_project_id)
        
        # Test 5: Reporte específico de proyecto
        if simple_project_id:
            test_project_report(session_id, simple_project_id)
        
        # Test 6: Reporte general
        test_general_report(session_id)
        
        # Test 7: Parsing genérico
        test_generic_parsing(session_id)
        
    finally:
        # Siempre cerrar sesión
        logout(session_id)
    
    print("\n" + "=" * 70)
    print("🎉 Pruebas de acciones complejas completadas")

if __name__ == "__main__":
    main() 