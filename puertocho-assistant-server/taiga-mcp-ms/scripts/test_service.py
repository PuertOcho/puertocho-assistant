#!/usr/bin/env python3
"""
Script de pruebas para el servicio Taiga MCP
"""

import requests
import json
import time
import sys

# Configuración
BASE_URL = "http://localhost:5007"
TAIGA_HOST = "http://localhost:9000"
USERNAME = "puertocho"
PASSWORD = "puertocho"

def test_health():
    """Probar endpoint de salud"""
    print("🔍 Probando health check...")
    try:
        response = requests.get(f"{BASE_URL}/health")
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Servicio: {data.get('status')}")
            print(f"   Taiga disponible: {data.get('taiga_available')}")
            print(f"   Sesiones activas: {data.get('active_sessions')}")
            return True
        else:
            print(f"❌ Health check falló: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ Error en health check: {e}")
        return False

def test_login():
    """Probar autenticación"""
    print("\n🔐 Probando autenticación...")
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
            user_info = data.get("user", {})
            
            print(f"✅ Autenticación exitosa")
            print(f"   Session ID: {session_id}")
            print(f"   Usuario: {user_info.get('username')} ({user_info.get('full_name')})")
            print(f"   Email: {user_info.get('email')}")
            print(f"   Expira: {data.get('expires_at')}")
            
            return session_id
        else:
            print(f"❌ Error en autenticación: {response.status_code}")
            if response.content:
                print(f"   Detalle: {response.json()}")
            return None
            
    except Exception as e:
        print(f"❌ Error en autenticación: {e}")
        return None

def test_session_status(session_id):
    """Probar verificación de estado de sesión"""
    print(f"\n📊 Probando estado de sesión...")
    try:
        status_data = {"session_id": session_id}
        response = requests.post(f"{BASE_URL}/session_status", json=status_data)
        
        if response.status_code == 200:
            data = response.json()
            if data.get("valid"):
                print(f"✅ Sesión válida")
                print(f"   Usuario: {data.get('username')}")
                print(f"   Expira: {data.get('expires_at')}")
                return True
            else:
                print(f"❌ Sesión inválida: {data.get('error')}")
                return False
        else:
            print(f"❌ Error verificando sesión: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Error verificando sesión: {e}")
        return False

def test_list_projects(session_id):
    """Probar listado de proyectos"""
    print(f"\n📋 Probando listado de proyectos...")
    try:
        response = requests.get(f"{BASE_URL}/projects", params={"session_id": session_id})
        
        if response.status_code == 200:
            data = response.json()
            projects = data.get("projects", [])
            total = data.get("total", 0)
            
            print(f"✅ Proyectos listados: {total}")
            if projects:
                for i, project in enumerate(projects[:3]):  # Mostrar solo los primeros 3
                    print(f"   {i+1}. {project.get('name')} (ID: {project.get('id')})")
                if len(projects) > 3:
                    print(f"   ... y {len(projects)-3} más")
            else:
                print("   Sin proyectos disponibles")
            
            return projects
        else:
            print(f"❌ Error listando proyectos: {response.status_code}")
            if response.content:
                print(f"   Detalle: {response.json()}")
            return []
            
    except Exception as e:
        print(f"❌ Error listando proyectos: {e}")
        return []

def test_create_project(session_id):
    """Probar creación de proyecto"""
    print(f"\n🚀 Probando creación de proyecto...")
    try:
        project_name = f"Test Project {int(time.time())}"
        project_data = {
            "session_id": session_id,
            "name": project_name,
            "description": "Proyecto de prueba creado por el script de test"
        }
        
        response = requests.post(f"{BASE_URL}/projects", json=project_data)
        
        if response.status_code == 200:
            project = response.json()
            print(f"✅ Proyecto creado exitosamente")
            print(f"   Nombre: {project.get('name')}")
            print(f"   ID: {project.get('id')}")
            print(f"   Slug: {project.get('slug')}")
            
            return project
        else:
            print(f"❌ Error creando proyecto: {response.status_code}")
            if response.content:
                print(f"   Detalle: {response.json()}")
            return None
            
    except Exception as e:
        print(f"❌ Error creando proyecto: {e}")
        return None

def test_create_user_story(session_id, project_id):
    """Probar creación de historia de usuario"""
    print(f"\n📝 Probando creación de historia de usuario...")
    try:
        story_data = {
            "session_id": session_id,
            "subject": f"Historia de prueba {int(time.time())}",
            "description": "Como usuario de prueba, quiero que esto funcione para poder validar el sistema"
        }
        
        response = requests.post(f"{BASE_URL}/projects/{project_id}/user_stories", json=story_data)
        
        if response.status_code == 200:
            story = response.json()
            print(f"✅ Historia creada exitosamente")
            print(f"   Título: {story.get('subject')}")
            print(f"   ID: {story.get('id')}")
            print(f"   Ref: {story.get('ref')}")
            
            return story
        else:
            print(f"❌ Error creando historia: {response.status_code}")
            if response.content:
                print(f"   Detalle: {response.json()}")
            return None
            
    except Exception as e:
        print(f"❌ Error creando historia: {e}")
        return None

def test_list_user_stories(session_id, project_id):
    """Probar listado de historias de usuario"""
    print(f"\n📖 Probando listado de historias de usuario...")
    try:
        response = requests.get(f"{BASE_URL}/projects/{project_id}/user_stories", 
                              params={"session_id": session_id})
        
        if response.status_code == 200:
            data = response.json()
            stories = data.get("user_stories", [])
            total = data.get("total", 0)
            
            print(f"✅ Historias listadas: {total}")
            if stories:
                for i, story in enumerate(stories[:3]):  # Mostrar solo las primeras 3
                    print(f"   {i+1}. {story.get('subject')} (ID: {story.get('id')})")
                if len(stories) > 3:
                    print(f"   ... y {len(stories)-3} más")
            else:
                print("   Sin historias disponibles")
            
            return stories
        else:
            print(f"❌ Error listando historias: {response.status_code}")
            if response.content:
                print(f"   Detalle: {response.json()}")
            return []
            
    except Exception as e:
        print(f"❌ Error listando historias: {e}")
        return []

def test_logout(session_id):
    """Probar cierre de sesión"""
    print(f"\n🚪 Probando cierre de sesión...")
    try:
        logout_data = {"session_id": session_id}
        response = requests.post(f"{BASE_URL}/logout", json=logout_data)
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Sesión cerrada: {data.get('message')}")
            return True
        else:
            print(f"❌ Error cerrando sesión: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Error cerrando sesión: {e}")
        return False

def main():
    """Ejecutar todas las pruebas"""
    print("🧪 Iniciando pruebas del servicio Taiga MCP")
    print("=" * 50)
    
    # Test 1: Health check
    if not test_health():
        print("\n❌ El servicio no está disponible. Verifica que esté ejecutándose.")
        sys.exit(1)
    
    # Test 2: Autenticación
    session_id = test_login()
    if not session_id:
        print("\n❌ No se pudo autenticar. Verifica las credenciales y que Taiga esté disponible.")
        sys.exit(1)
    
    # Test 3: Estado de sesión
    if not test_session_status(session_id):
        print("\n❌ Problema con el estado de la sesión.")
        sys.exit(1)
    
    # Test 4: Listar proyectos
    projects = test_list_projects(session_id)
    
    # Test 5: Crear proyecto
    new_project = test_create_project(session_id)
    
    # Test 6: Crear historia de usuario (si se creó un proyecto)
    if new_project and new_project.get("id"):
        project_id = new_project["id"]
        test_create_user_story(session_id, project_id)
        test_list_user_stories(session_id, project_id)
    
    # Test 7: Logout
    test_logout(session_id)
    
    print("\n" + "=" * 50)
    print("🎉 Pruebas completadas")

if __name__ == "__main__":
    main() 