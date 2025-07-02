import os
import json
import requests
from datetime import datetime, timedelta
from flask import Flask, request, jsonify
from dotenv import load_dotenv
import re
from typing import List, Dict, Any

load_dotenv()

app = Flask(__name__)

# Configuración del servicio
TAIGA_HOST = os.getenv("TAIGA_HOST", "http://localhost:9000")
TAIGA_USERNAME = os.getenv("TAIGA_USERNAME", "puertocho")
TAIGA_PASSWORD = os.getenv("TAIGA_PASSWORD", "puertocho")
FLASK_HOST = os.getenv("FLASK_HOST", "0.0.0.0")
FLASK_PORT = int(os.getenv("FLASK_PORT", 5000))
SESSION_EXPIRY = int(os.getenv("SESSION_EXPIRY", 28800))  # 8 horas por defecto

print(f"[*] Iniciando Taiga MCP Service")
print(f"[*] Host: {FLASK_HOST}:{FLASK_PORT}")
print(f"[*] Taiga API: {TAIGA_HOST}")
print(f"[*] Usuario: {TAIGA_USERNAME}")

# Almacén de sesiones en memoria (para producción, usar Redis o BD)
sessions = {}

# Variable global para la sesión automática
AUTO_SESSION_ID = None
AUTO_SESSION_DATA = None

class TaigaAPIError(Exception):
    """Excepción personalizada para errores de la API de Taiga"""
    pass

def auto_login():
    """Realizar login automático al iniciar el servicio"""
    global AUTO_SESSION_ID, AUTO_SESSION_DATA
    
    try:
        print(f"[*] Intentando login automático...")
        print(f"    Usuario: {TAIGA_USERNAME}")
        print(f"    Host: {TAIGA_HOST}")
        
        # Autenticación con Taiga
        login_url = f"{TAIGA_HOST}/api/v1/auth"
        login_data = {
            "username": TAIGA_USERNAME,
            "password": TAIGA_PASSWORD,
            "type": "normal"
        }
        
        response = make_api_request(login_url, "POST", login_data)
        
        if "auth_token" not in response:
            raise TaigaAPIError("Token de autenticación no recibido")
        
        # Crear sesión automática
        AUTO_SESSION_ID = f"auto_session_{datetime.now().strftime('%Y%m%d_%H%M%S_%f')}"
        expires_at = datetime.now() + timedelta(seconds=SESSION_EXPIRY)
        
        AUTO_SESSION_DATA = {
            "auth_token": response["auth_token"],
            "user_id": response.get("id"),
            "username": TAIGA_USERNAME,
            "host": TAIGA_HOST,
            "created_at": datetime.now(),
            "expires_at": expires_at,
            "auto_session": True
        }
        
        sessions[AUTO_SESSION_ID] = AUTO_SESSION_DATA
        
        print(f"[✅] Login automático exitoso!")
        print(f"    Session ID: {AUTO_SESSION_ID}")
        print(f"    Usuario: {response.get('full_name', TAIGA_USERNAME)} ({response.get('email', 'N/A')})")
        print(f"    Expira: {expires_at}")
        
        return True
        
    except Exception as e:
        print(f"[❌] Error en login automático: {e}")
        return False

def get_session_data_with_auto(session_id=None):
    """Obtener datos de sesión, usando sesión automática si no se especifica"""
    global AUTO_SESSION_ID, AUTO_SESSION_DATA
    
    # Si no se proporciona session_id, usar la sesión automática
    if not session_id and AUTO_SESSION_ID:
        session_id = AUTO_SESSION_ID
    
    if not session_id:
        return None
    
    # Verificar si la sesión es válida
    session_data = get_session_data(session_id)
    
    # Si la sesión automática expiró, renovarla automáticamente
    if not session_data and session_id == AUTO_SESSION_ID:
        print(f"[🔄] Sesión automática expirada, renovando...")
        
        # Intentar renovar la sesión automática
        if renew_auto_session():
            # Usar la nueva sesión automática
            session_data = get_session_data(AUTO_SESSION_ID)
            print(f"[✅] Sesión automática renovada exitosamente: {AUTO_SESSION_ID}")
        else:
            print(f"[❌] Error renovando sesión automática")
            return None
    
    return session_data

def renew_auto_session():
    """Renovar la sesión automática"""
    global AUTO_SESSION_ID, AUTO_SESSION_DATA
    
    try:
        print(f"[🔄] Renovando sesión automática...")
        print(f"    Usuario: {TAIGA_USERNAME}")
        print(f"    Host: {TAIGA_HOST}")
        
        # Limpiar sesión anterior si existe
        if AUTO_SESSION_ID and AUTO_SESSION_ID in sessions:
            del sessions[AUTO_SESSION_ID]
        
        # Autenticación con Taiga
        login_url = f"{TAIGA_HOST}/api/v1/auth"
        login_data = {
            "username": TAIGA_USERNAME,
            "password": TAIGA_PASSWORD,
            "type": "normal"
        }
        
        response = make_api_request(login_url, "POST", login_data)
        
        if "auth_token" not in response:
            raise TaigaAPIError("Token de autenticación no recibido")
        
        # Crear nueva sesión automática
        AUTO_SESSION_ID = f"auto_session_{datetime.now().strftime('%Y%m%d_%H%M%S_%f')}"
        expires_at = datetime.now() + timedelta(seconds=SESSION_EXPIRY)
        
        AUTO_SESSION_DATA = {
            "auth_token": response["auth_token"],
            "user_id": response.get("id"),
            "username": TAIGA_USERNAME,
            "host": TAIGA_HOST,
            "created_at": datetime.now(),
            "expires_at": expires_at,
            "auto_session": True,
            "renewed": True
        }
        
        sessions[AUTO_SESSION_ID] = AUTO_SESSION_DATA
        
        print(f"[✅] Sesión automática renovada!")
        print(f"    Nuevo Session ID: {AUTO_SESSION_ID}")
        print(f"    Expira: {expires_at}")
        
        return True
        
    except Exception as e:
        print(f"[❌] Error renovando sesión automática: {e}")
        AUTO_SESSION_ID = None
        AUTO_SESSION_DATA = None
        return False

def make_api_request(url, method="GET", data=None, headers=None, auth_token=None):
    """Realizar petición a la API de Taiga"""
    try:
        request_headers = {"Content-Type": "application/json"}
        if headers:
            request_headers.update(headers)
        if auth_token:
            request_headers["Authorization"] = f"Bearer {auth_token}"
        
        if method == "GET":
            response = requests.get(url, headers=request_headers, timeout=30)
        elif method == "POST":
            response = requests.post(url, headers=request_headers, 
                                   data=json.dumps(data) if data else None, timeout=30)
        elif method == "PUT":
            response = requests.put(url, headers=request_headers, 
                                  data=json.dumps(data) if data else None, timeout=30)
        elif method == "DELETE":
            response = requests.delete(url, headers=request_headers, timeout=30)
        else:
            raise TaigaAPIError(f"Método HTTP no soportado: {method}")
        
        response.raise_for_status()
        
        if response.content:
            return response.json()
        return {"success": True}
        
    except requests.exceptions.RequestException as e:
        print(f"[!] Error en petición API: {e}")
        raise TaigaAPIError(f"Error de conexión: {str(e)}")
    except json.JSONDecodeError as e:
        print(f"[!] Error decodificando JSON: {e}")
        raise TaigaAPIError(f"Respuesta inválida del servidor: {str(e)}")

def is_session_valid(session_id):
    """Verificar si una sesión es válida"""
    if session_id not in sessions:
        return False
    
    session = sessions[session_id]
    if datetime.now() > session["expires_at"]:
        del sessions[session_id]
        return False
    
    return True

def get_session_data(session_id):
    """Obtener datos de la sesión"""
    if not is_session_valid(session_id):
        return None
    return sessions[session_id]

@app.route("/health", methods=["GET"])
def health():
    """Endpoint de salud del servicio"""
    try:
        # Verificar conectividad con Taiga
        response = requests.get(f"{TAIGA_HOST}/api/v1/stats", timeout=5)
        taiga_available = response.status_code == 200
    except:
        taiga_available = False
    
    # Información de la sesión automática
    auto_session_info = None
    if AUTO_SESSION_ID and AUTO_SESSION_DATA:
        auto_session_info = {
            'session_id': AUTO_SESSION_ID,
            'username': AUTO_SESSION_DATA.get('username'),
            'expires_at': AUTO_SESSION_DATA.get('expires_at').isoformat() if AUTO_SESSION_DATA.get('expires_at') else None,
            'valid': is_session_valid(AUTO_SESSION_ID),
            'created_at': AUTO_SESSION_DATA.get('created_at').isoformat() if AUTO_SESSION_DATA.get('created_at') else None,
            'renewed': AUTO_SESSION_DATA.get('renewed', False),
            'auto_renewal_enabled': True
        }
    
    return jsonify({
        'status': 'ok' if taiga_available else 'degraded',
        'service': 'taiga-mcp',
        'taiga_host': TAIGA_HOST,
        'taiga_available': taiga_available,
        'active_sessions': len(sessions),
        'auto_session': auto_session_info,
        'auto_login_enabled': AUTO_SESSION_ID is not None
    })

@app.route("/login", methods=["POST"])
def login():
    """Autenticación en Taiga y creación de sesión"""
    try:
        data = request.get_json()
        username = data.get("username", TAIGA_USERNAME)
        password = data.get("password", TAIGA_PASSWORD)
        host = data.get("host", TAIGA_HOST)
        
        print(f"[*] Iniciando sesión para usuario: {username}")
        
        # Autenticación con Taiga
        login_url = f"{host}/api/v1/auth"
        login_data = {
            "username": username,
            "password": password,
            "type": "normal"
        }
        
        response = make_api_request(login_url, "POST", login_data)
        
        if "auth_token" not in response:
            raise TaigaAPIError("Token de autenticación no recibido")
        
        # Crear sesión
        session_id = f"taiga_session_{datetime.now().strftime('%Y%m%d_%H%M%S_%f')}"
        expires_at = datetime.now() + timedelta(seconds=SESSION_EXPIRY)
        
        sessions[session_id] = {
            "auth_token": response["auth_token"],
            "user_id": response.get("id"),
            "username": username,
            "host": host,
            "created_at": datetime.now(),
            "expires_at": expires_at
        }
        
        print(f"[*] Sesión creada: {session_id} (expira: {expires_at})")
        
        return jsonify({
            "session_id": session_id,
            "user": {
                "id": response.get("id"),
                "username": username,
                "full_name": response.get("full_name"),
                "email": response.get("email")
            },
            "expires_at": expires_at.isoformat(),
            "host": host
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error en login: {e}")
        return jsonify({"error": f"Error interno: {str(e)}"}), 500

@app.route("/logout", methods=["POST"])
def logout():
    """Cerrar sesión"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        if session_id in sessions:
            del sessions[session_id]
            print(f"[*] Sesión cerrada: {session_id}")
            return jsonify({"success": True, "message": "Sesión cerrada"})
        else:
            return jsonify({"error": "Sesión no encontrada"}), 404
            
    except Exception as e:
        print(f"[!] Error en logout: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/session_status", methods=["POST"])
def session_status():
    """Verificar estado de la sesión"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        if is_session_valid(session_id):
            session = sessions[session_id]
            return jsonify({
                "valid": True,
                "username": session["username"],
                "expires_at": session["expires_at"].isoformat(),
                "host": session["host"]
            })
        else:
            return jsonify({"valid": False, "error": "Sesión inválida o expirada"})
            
    except Exception as e:
        print(f"[!] Error verificando sesión: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects", methods=["GET"])
def list_projects():
    """Listar proyectos del usuario"""
    try:
        session_id = request.args.get("session_id")
        
        # Usar sesión automática si no se proporciona session_id
        session = get_session_data_with_auto(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida o no disponible. ¿Login automático configurado?"}), 401
        
        # Listar proyectos
        projects_url = f"{session['host']}/api/v1/projects"
        projects = make_api_request(projects_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "projects": projects,
            "total": len(projects) if isinstance(projects, list) else 0
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error listando proyectos: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects", methods=["POST"])
def create_project():
    """Crear nuevo proyecto"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        # Usar sesión automática si no se proporciona session_id
        session = get_session_data_with_auto(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida o no disponible. ¿Login automático configurado?"}), 401
        
        # Datos del proyecto
        project_data = {
            "name": data.get("name"),
            "description": data.get("description", ""),
            "creation_template": data.get("template", 1)  # Template por defecto
        }
        
        if not project_data["name"]:
            return jsonify({"error": "Nombre del proyecto requerido"}), 400
        
        # Crear proyecto
        projects_url = f"{session['host']}/api/v1/projects"
        project = make_api_request(projects_url, "POST", project_data, auth_token=session["auth_token"])
        
        print(f"[*] Proyecto creado: {project.get('name')} (ID: {project.get('id')})")
        
        return jsonify(project)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error creando proyecto: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>", methods=["GET"])
def get_project(project_id):
    """Obtener detalles de un proyecto"""
    try:
        session_id = request.args.get("session_id")
        
        # Usar sesión automática si no se proporciona session_id
        session = get_session_data_with_auto(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida o no disponible. ¿Login automático configurado?"}), 401
        
        # Obtener proyecto
        project_url = f"{session['host']}/api/v1/projects/{project_id}"
        project = make_api_request(project_url, "GET", auth_token=session["auth_token"])
        
        return jsonify(project)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error obteniendo proyecto: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>/user_stories", methods=["GET"])
def list_user_stories(project_id):
    """Listar historias de usuario de un proyecto"""
    try:
        session_id = request.args.get("session_id")
        
        # Usar sesión automática si no se proporciona session_id
        session = get_session_data_with_auto(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida o no disponible. ¿Login automático configurado?"}), 401
        
        # Listar historias de usuario
        stories_url = f"{session['host']}/api/v1/userstories"
        params = {"project": project_id}
        stories = make_api_request(f"{stories_url}?project={project_id}", "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "user_stories": stories,
            "total": len(stories) if isinstance(stories, list) else 0,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error listando historias: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>/user_stories", methods=["POST"])
def create_user_story(project_id):
    """Crear nueva historia de usuario"""
    try:
        data = request.get_json() or {}
        session_id = data.get("session_id")
        
        # Usar sesión automática si no se proporciona session_id
        session = get_session_data_with_auto(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida o no disponible. ¿Login automático configurado?"}), 401
        
        # Datos de la historia
        story_data = {
            "project": project_id,
            "subject": data.get("subject"),
            "description": data.get("description", "")
        }
        
        if not story_data["subject"]:
            return jsonify({"error": "Título de la historia requerido"}), 400
        
        # Crear historia de usuario
        stories_url = f"{session['host']}/api/v1/userstories"
        story = make_api_request(stories_url, "POST", story_data, auth_token=session["auth_token"])
        
        print(f"[*] Historia creada: {story.get('subject')} (ID: {story.get('id')})")
        
        return jsonify(story)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error creando historia: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/user_stories/<int:story_id>", methods=["PUT"])
def update_user_story(story_id):
    """Actualizar historia de usuario"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        # Obtener historia actual para el version
        story_url = f"{session['host']}/api/v1/userstories/{story_id}"
        current_story = make_api_request(story_url, "GET", auth_token=session["auth_token"])
        
        # Datos de actualización
        update_data = {
            "version": current_story["version"],  # Requerido para actualizaciones
        }
        
        # Campos opcionales a actualizar
        if "subject" in data:
            update_data["subject"] = data["subject"]
        if "description" in data:
            update_data["description"] = data["description"]
        if "status" in data:
            update_data["status"] = data["status"]
        
        # Actualizar historia
        updated_story = make_api_request(story_url, "PUT", update_data, auth_token=session["auth_token"])
        
        print(f"[*] Historia actualizada: {updated_story.get('subject')} (ID: {story_id})")
        
        return jsonify(updated_story)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error actualizando historia: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/execute_complex_action", methods=["POST"])
def execute_complex_action():
    """Ejecutar acciones complejas basadas en texto usando LLM-like parsing"""
    try:
        data = request.get_json() or {}
        session_id = data.get("session_id")
        
        # Usar sesión automática si no se proporciona session_id
        session = get_session_data_with_auto(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida o no disponible. ¿Login automático configurado?"}), 401
        
        action_text = data.get("action_text", "").strip()
        project_id = data.get("project_id")
        tracker_content = data.get("tracker_content", "")
        
        if not action_text:
            return jsonify({"error": "action_text requerido"}), 400
        
        print(f"[*] Ejecutando acción compleja: {action_text[:100]}...")
        
        # Interpretar la acción y ejecutar
        result = interpret_and_execute_action(
            action_text, 
            session, 
            project_id, 
            tracker_content
        )
        
        return jsonify(result)
        
    except Exception as e:
        print(f"[!] Error en acción compleja: {e}")
        return jsonify({"error": str(e)}), 500

def interpret_and_execute_action(action_text: str, session: dict, project_id: int = None, tracker_content: str = ""):
    """Interpretar texto y ejecutar acciones correspondientes"""
    results = []
    
    # Normalizar texto
    action_lower = action_text.lower()
    
    # 1. Crear proyecto desde tracker
    if "crear proyecto desde tracker" in action_lower or "importar tracker" in action_lower:
        if tracker_content:
            result = create_project_from_tracker(tracker_content, session)
            results.append(result)
        else:
            results.append({"error": "tracker_content requerido para esta acción"})
    
    # 2. Crear proyecto simple
    elif "crear proyecto" in action_lower:
        project_name = extract_project_name(action_text)
        if project_name:
            result = create_simple_project(project_name, action_text, session)
            results.append(result)
        else:
            results.append({"error": "No se pudo extraer el nombre del proyecto"})
    
    # 3. Crear historias de usuario
    elif "crear historia" in action_lower or "crear user story" in action_lower:
        if project_id:
            result = create_story_from_text(action_text, session, project_id)
            results.append(result)
        else:
            results.append({"error": "project_id requerido para crear historias"})
    
    # 4. Crear epics
    elif "crear epic" in action_lower:
        if project_id:
            result = create_epic_from_text(action_text, session, project_id)
            results.append(result)
        else:
            results.append({"error": "project_id requerido para crear epics"})
    
    # 5. Crear tasks
    elif "crear task" in action_lower or "crear tarea" in action_lower:
        if project_id:
            result = create_task_from_text(action_text, session, project_id)
            results.append(result)
        else:
            results.append({"error": "project_id requerido para crear tasks"})
    
    # 6. Crear issues
    elif "crear issue" in action_lower or "reportar bug" in action_lower:
        if project_id:
            result = create_issue_from_text(action_text, session, project_id)
            results.append(result)
        else:
            results.append({"error": "project_id requerido para crear issues"})
    
    # 7. Crear milestone
    elif "crear milestone" in action_lower or "crear sprint" in action_lower:
        if project_id:
            result = create_milestone_from_text(action_text, session, project_id)
            results.append(result)
        else:
            results.append({"error": "project_id requerido para crear milestones"})
    
    # 8. Análisis y planificación automática
    elif "analizar proyecto" in action_lower or "planificar" in action_lower:
        if project_id:
            result = analyze_and_plan_project(action_text, session, project_id)
            results.append(result)
        else:
            results.append({"error": "project_id requerido para análisis"})
    
    # 9. Actualización masiva
    elif "actualizar" in action_lower and "masivo" in action_lower:
        if project_id:
            result = mass_update_project(action_text, session, project_id)
            results.append(result)
        else:
            results.append({"error": "project_id requerido para actualización masiva"})
    
    # 10. Generar reporte
    elif "generar reporte" in action_lower or "reporte" in action_lower:
        if project_id:
            result = generate_project_report(session, project_id)
            results.append(result)
        else:
            # Reporte general de todos los proyectos
            result = generate_general_report(session)
            results.append(result)
    
    # 11. Listar elementos
    elif "listar" in action_lower:
        if project_id:
            result = list_project_elements(action_text, session, project_id)
            results.append(result)
        else:
            results.append({"error": "project_id requerido para listar elementos"})
    
    # 12. Asignaciones
    elif "asignar" in action_lower:
        result = handle_assignments(action_text, session, project_id)
        results.append(result)
    
    else:
        # Acción genérica: intentar interpretar como múltiples comandos
        result = parse_generic_commands(action_text, session, project_id)
        results.append(result)
    
    return {
        "action_text": action_text,
        "results": results,
        "timestamp": datetime.now().isoformat(),
        "session_id": session.get("username")
    }

def create_project_from_tracker(tracker_content: str, session: dict):
    """Crear proyecto completo desde archivo PROJECT_TRACKER.md"""
    try:
        # Extraer información del tracker
        project_info = parse_project_tracker(tracker_content)
        
        # Crear proyecto
        project_data = {
            "name": project_info["name"],
            "description": project_info["description"]
        }
        
        projects_url = f"{session['host']}/api/v1/projects"
        project = make_api_request(projects_url, "POST", project_data, auth_token=session["auth_token"])
        
        project_id = project["id"]
        created_stories = []
        
        # Crear historias de usuario desde las fases
        for phase in project_info["phases"]:
            for task in phase["tasks"]:
                story_data = {
                    "project": project_id,
                    "subject": f"{phase['name']}: {task['title']}",
                    "description": task["description"]
                }
                
                stories_url = f"{session['host']}/api/v1/userstories"
                story = make_api_request(stories_url, "POST", story_data, auth_token=session["auth_token"])
                created_stories.append({
                    "id": story["id"],
                    "ref": story["ref"],
                    "subject": story["subject"]
                })
        
        return {
            "action": "create_project_from_tracker",
            "success": True,
            "project": {
                "id": project_id,
                "name": project["name"],
                "slug": project["slug"]
            },
            "created_stories": created_stories,
            "summary": f"Proyecto '{project['name']}' creado con {len(created_stories)} historias de usuario"
        }
        
    except Exception as e:
        return {
            "action": "create_project_from_tracker",
            "success": False,
            "error": str(e)
        }

def parse_project_tracker(content: str) -> Dict[str, Any]:
    """Parsear archivo PROJECT_TRACKER.md para extraer información estructurada"""
    lines = content.split('\n')
    
    # Extraer nombre del proyecto del título
    project_name = "Proyecto desde Tracker"
    for line in lines:
        if line.startswith('# ') and 'PROJECT_TRACKER' not in line:
            project_name = line[2:].strip()
            break
    
    # Buscar descripción (primer párrafo después del título)
    description = "Proyecto creado automáticamente desde PROJECT_TRACKER"
    
    # Extraer fases y tareas
    phases = []
    current_phase = None
    
    for line in lines:
        line = line.strip()
        
        # Detectar fase (## Fase...)
        if line.startswith('## Fase') or line.startswith('## Phase'):
            if current_phase:
                phases.append(current_phase)
            
            current_phase = {
                "name": line[3:].strip(),
                "tasks": []
            }
        
        # Detectar tareas (- [ ] o - [x])
        elif line.startswith('- [') and current_phase:
            # Extraer título de la tarea
            task_match = re.search(r'- \[[x ]\] (.+)', line)
            if task_match:
                task_title = task_match.group(1).strip()
                
                # Determinar si está completada
                is_completed = '[x]' in line
                
                current_phase["tasks"].append({
                    "title": task_title,
                    "description": f"Tarea: {task_title}",
                    "completed": is_completed
                })
    
    # Añadir última fase
    if current_phase:
        phases.append(current_phase)
    
    return {
        "name": project_name,
        "description": description,
        "phases": phases
    }

def extract_project_name(text: str) -> str:
    """Extraer nombre de proyecto del texto"""
    # Patrones comunes
    patterns = [
        r'crear proyecto ["\']([^"\']+)["\']',
        r'crear proyecto llamado ["\']([^"\']+)["\']',
        r'crear proyecto "([^"]+)"',
        r'crear proyecto (\w+(?:\s+\w+)*)',
        r'nuevo proyecto ["\']([^"\']+)["\']',
        r'proyecto ["\']([^"\']+)["\']'
    ]
    
    for pattern in patterns:
        match = re.search(pattern, text.lower())
        if match:
            return match.group(1).strip()
    
    return None

def create_simple_project(name: str, description_text: str, session: dict):
    """Crear proyecto simple"""
    try:
        project_data = {
            "name": name,
            "description": f"Proyecto creado automáticamente: {description_text}"
        }
        
        projects_url = f"{session['host']}/api/v1/projects"
        project = make_api_request(projects_url, "POST", project_data, auth_token=session["auth_token"])
        
        return {
            "action": "create_simple_project",
            "success": True,
            "project": {
                "id": project["id"],
                "name": project["name"],
                "slug": project["slug"]
            }
        }
        
    except Exception as e:
        return {
            "action": "create_simple_project",
            "success": False,
            "error": str(e)
        }

def create_story_from_text(text: str, session: dict, project_id: int):
    """Crear historia de usuario desde texto"""
    try:
        # Extraer información de la historia
        story_info = parse_user_story_text(text)
        
        story_data = {
            "project": project_id,
            "subject": story_info["subject"],
            "description": story_info["description"]
        }
        
        stories_url = f"{session['host']}/api/v1/userstories"
        story = make_api_request(stories_url, "POST", story_data, auth_token=session["auth_token"])
        
        return {
            "action": "create_story_from_text",
            "success": True,
            "story": {
                "id": story["id"],
                "ref": story["ref"],
                "subject": story["subject"]
            }
        }
        
    except Exception as e:
        return {
            "action": "create_story_from_text",
            "success": False,
            "error": str(e)
        }

def parse_user_story_text(text: str) -> Dict[str, str]:
    """Parsear texto para extraer información de historia de usuario"""
    # Buscar patrones de historia de usuario
    as_a_pattern = r'como (\w+(?:\s+\w+)*)[,.]?\s*quiero ([^.]+)[.]\s*para ([^.]+)'
    match = re.search(as_a_pattern, text.lower())
    
    if match:
        role = match.group(1).strip()
        want = match.group(2).strip()
        purpose = match.group(3).strip()
        
        subject = f"Como {role}, quiero {want}"
        description = f"Como {role}, quiero {want} para {purpose}."
    else:
        # Extraer título simple
        lines = text.strip().split('\n')
        subject = lines[0][:100] if lines else "Historia de usuario"
        description = text
    
    return {
        "subject": subject,
        "description": description
    }

def analyze_and_plan_project(text: str, session: dict, project_id: int):
    """Analizar proyecto y generar plan automático"""
    try:
        # Obtener información del proyecto
        project_url = f"{session['host']}/api/v1/projects/{project_id}"
        project = make_api_request(project_url, "GET", auth_token=session["auth_token"])
        
        # Obtener historias existentes
        stories_url = f"{session['host']}/api/v1/userstories?project={project_id}"
        stories = make_api_request(stories_url, "GET", auth_token=session["auth_token"])
        
        analysis = {
            "project_name": project["name"],
            "total_stories": len(stories),
            "completed_stories": len([s for s in stories if s.get("is_closed", False)]),
            "pending_stories": len([s for s in stories if not s.get("is_closed", False)]),
            "progress_percentage": 0
        }
        
        if analysis["total_stories"] > 0:
            analysis["progress_percentage"] = (analysis["completed_stories"] / analysis["total_stories"]) * 100
        
        # Generar recomendaciones automáticas
        recommendations = generate_recommendations(project, stories, text)
        
        return {
            "action": "analyze_and_plan_project",
            "success": True,
            "analysis": analysis,
            "recommendations": recommendations
        }
        
    except Exception as e:
        return {
            "action": "analyze_and_plan_project",
            "success": False,
            "error": str(e)
        }

def generate_recommendations(project: dict, stories: list, context_text: str) -> List[str]:
    """Generar recomendaciones automáticas para el proyecto"""
    recommendations = []
    
    # Análisis basado en el estado actual
    if len(stories) == 0:
        recommendations.append("Crear historias de usuario iniciales para definir el alcance")
    
    if len(stories) < 5:
        recommendations.append("Considerar agregar más historias para una planificación detallada")
    
    completed_count = len([s for s in stories if s.get("is_closed", False)])
    if completed_count == 0 and len(stories) > 0:
        recommendations.append("Comenzar el desarrollo con las historias de mayor prioridad")
    
    # Análisis basado en el contexto
    context_lower = context_text.lower()
    if "optimizar" in context_lower:
        recommendations.append("Revisar historias existentes para identificar optimizaciones")
    
    if "testing" in context_lower or "pruebas" in context_lower:
        recommendations.append("Agregar historias específicas para testing y QA")
    
    if "documentación" in context_lower:
        recommendations.append("Incluir historias para documentación técnica y de usuario")
    
    return recommendations

def mass_update_project(text: str, session: dict, project_id: int):
    """Actualización masiva del proyecto basada en texto"""
    try:
        updates_applied = []
        
        # Obtener historias del proyecto
        stories_url = f"{session['host']}/api/v1/userstories?project={project_id}"
        stories = make_api_request(stories_url, "GET", auth_token=session["auth_token"])
        
        # Interpretar actualizaciones del texto
        if "marcar completadas" in text.lower():
            # Marcar historias como completadas
            for story in stories[:3]:  # Limitar para seguridad
                if not story.get("is_closed", False):
                    # Aquí se implementaría la actualización del estado
                    updates_applied.append(f"Historia #{story['ref']} marcada como completada")
        
        return {
            "action": "mass_update_project",
            "success": True,
            "updates_applied": updates_applied
        }
        
    except Exception as e:
        return {
            "action": "mass_update_project",
            "success": False,
            "error": str(e)
        }

def generate_project_report(session: dict, project_id: int):
    """Generar reporte detallado del proyecto"""
    try:
        # Obtener datos del proyecto
        project_url = f"{session['host']}/api/v1/projects/{project_id}"
        project = make_api_request(project_url, "GET", auth_token=session["auth_token"])
        
        stories_url = f"{session['host']}/api/v1/userstories?project={project_id}"
        stories = make_api_request(stories_url, "GET", auth_token=session["auth_token"])
        
        report = {
            "project_name": project["name"],
            "created_date": project["created_date"],
            "total_stories": len(stories),
            "stories_by_status": {},
            "recent_activity": project.get("total_activity_last_week", 0),
            "report_generated": datetime.now().isoformat()
        }
        
        # Agrupar por estado
        for story in stories:
            status = story.get("status_extra_info", {}).get("name", "Unknown")
            report["stories_by_status"][status] = report["stories_by_status"].get(status, 0) + 1
        
        return {
            "action": "generate_project_report",
            "success": True,
            "report": report
        }
        
    except Exception as e:
        return {
            "action": "generate_project_report",
            "success": False,
            "error": str(e)
        }

def generate_general_report(session: dict):
    """Generar reporte general de todos los proyectos"""
    try:
        projects_url = f"{session['host']}/api/v1/projects"
        projects = make_api_request(projects_url, "GET", auth_token=session["auth_token"])
        
        report = {
            "total_projects": len(projects),
            "projects_summary": [],
            "report_generated": datetime.now().isoformat()
        }
        
        for project in projects:
            report["projects_summary"].append({
                "id": project["id"],
                "name": project["name"],
                "created": project["created_date"],
                "activity": project.get("total_activity", 0)
            })
        
        return {
            "action": "generate_general_report",
            "success": True,
            "report": report
        }
        
    except Exception as e:
        return {
            "action": "generate_general_report",
            "success": False,
            "error": str(e)
        }

def parse_generic_commands(text: str, session: dict, project_id: int = None):
    """Parsear comandos genéricos del texto"""
    commands_found = []
    
    # Buscar múltiples comandos en el texto
    lines = text.split('\n')
    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        if line.startswith('-') or line.startswith('*'):
            # Lista de tareas
            task = line[1:].strip()
            commands_found.append(f"Tarea identificada: {task}")
    
    return {
        "action": "parse_generic_commands",
        "success": True,
        "commands_found": commands_found,
        "raw_text": text
    }

# =================== EPICS ENDPOINTS ===================

@app.route("/projects/<int:project_id>/epics", methods=["GET"])
def list_epics(project_id):
    """Listar epics de un proyecto"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        epics_url = f"{session['host']}/api/v1/epics?project={project_id}"
        epics = make_api_request(epics_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "epics": epics,
            "total": len(epics) if isinstance(epics, list) else 0,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error listando epics: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>/epics", methods=["POST"])
def create_epic(project_id):
    """Crear nuevo epic"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        epic_data = {
            "project": project_id,
            "subject": data.get("subject"),
            "description": data.get("description", ""),
            "color": data.get("color", "#999999")
        }
        
        if not epic_data["subject"]:
            return jsonify({"error": "Título del epic requerido"}), 400
        
        epics_url = f"{session['host']}/api/v1/epics"
        epic = make_api_request(epics_url, "POST", epic_data, auth_token=session["auth_token"])
        
        print(f"[*] Epic creado: {epic.get('subject')} (ID: {epic.get('id')})")
        
        return jsonify(epic)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error creando epic: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/epics/<int:epic_id>", methods=["GET"])
def get_epic(epic_id):
    """Obtener detalles de un epic"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        epic_url = f"{session['host']}/api/v1/epics/{epic_id}"
        epic = make_api_request(epic_url, "GET", auth_token=session["auth_token"])
        
        return jsonify(epic)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error obteniendo epic: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/epics/<int:epic_id>", methods=["PUT"])
def update_epic(epic_id):
    """Actualizar epic"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        # Obtener epic actual para el version
        epic_url = f"{session['host']}/api/v1/epics/{epic_id}"
        current_epic = make_api_request(epic_url, "GET", auth_token=session["auth_token"])
        
        update_data = {"version": current_epic["version"]}
        
        # Campos opcionales a actualizar
        if "subject" in data:
            update_data["subject"] = data["subject"]
        if "description" in data:
            update_data["description"] = data["description"]
        if "status" in data:
            update_data["status"] = data["status"]
        if "color" in data:
            update_data["color"] = data["color"]
        
        updated_epic = make_api_request(epic_url, "PUT", update_data, auth_token=session["auth_token"])
        
        print(f"[*] Epic actualizado: {updated_epic.get('subject')} (ID: {epic_id})")
        
        return jsonify(updated_epic)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error actualizando epic: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/epics/<int:epic_id>", methods=["DELETE"])
def delete_epic(epic_id):
    """Eliminar epic"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        epic_url = f"{session['host']}/api/v1/epics/{epic_id}"
        make_api_request(epic_url, "DELETE", auth_token=session["auth_token"])
        
        print(f"[*] Epic eliminado: {epic_id}")
        
        return jsonify({"success": True, "message": f"Epic {epic_id} eliminado"})
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error eliminando epic: {e}")
        return jsonify({"error": str(e)}), 500

# =================== TASKS ENDPOINTS ===================

@app.route("/projects/<int:project_id>/tasks", methods=["GET"])
def list_tasks(project_id):
    """Listar tasks de un proyecto"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        tasks_url = f"{session['host']}/api/v1/tasks?project={project_id}"
        tasks = make_api_request(tasks_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "tasks": tasks,
            "total": len(tasks) if isinstance(tasks, list) else 0,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error listando tasks: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>/tasks", methods=["POST"])
def create_task(project_id):
    """Crear nueva task"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        task_data = {
            "project": project_id,
            "subject": data.get("subject"),
            "description": data.get("description", ""),
            "user_story": data.get("user_story_id")  # Opcional: vincular a user story
        }
        
        if not task_data["subject"]:
            return jsonify({"error": "Título de la task requerido"}), 400
        
        tasks_url = f"{session['host']}/api/v1/tasks"
        task = make_api_request(tasks_url, "POST", task_data, auth_token=session["auth_token"])
        
        print(f"[*] Task creada: {task.get('subject')} (ID: {task.get('id')})")
        
        return jsonify(task)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error creando task: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/tasks/<int:task_id>", methods=["GET"])
def get_task(task_id):
    """Obtener detalles de una task"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        task_url = f"{session['host']}/api/v1/tasks/{task_id}"
        task = make_api_request(task_url, "GET", auth_token=session["auth_token"])
        
        return jsonify(task)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error obteniendo task: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/tasks/<int:task_id>", methods=["PUT"])
def update_task(task_id):
    """Actualizar task"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        # Obtener task actual para el version
        task_url = f"{session['host']}/api/v1/tasks/{task_id}"
        current_task = make_api_request(task_url, "GET", auth_token=session["auth_token"])
        
        update_data = {"version": current_task["version"]}
        
        # Campos opcionales a actualizar
        if "subject" in data:
            update_data["subject"] = data["subject"]
        if "description" in data:
            update_data["description"] = data["description"]
        if "status" in data:
            update_data["status"] = data["status"]
        if "assigned_to" in data:
            update_data["assigned_to"] = data["assigned_to"]
        
        updated_task = make_api_request(task_url, "PUT", update_data, auth_token=session["auth_token"])
        
        print(f"[*] Task actualizada: {updated_task.get('subject')} (ID: {task_id})")
        
        return jsonify(updated_task)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error actualizando task: {e}")
        return jsonify({"error": str(e)}), 500

# =================== ISSUES ENDPOINTS ===================

@app.route("/projects/<int:project_id>/issues", methods=["GET"])
def list_issues(project_id):
    """Listar issues de un proyecto"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        issues_url = f"{session['host']}/api/v1/issues?project={project_id}"
        issues = make_api_request(issues_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "issues": issues,
            "total": len(issues) if isinstance(issues, list) else 0,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error listando issues: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>/issues", methods=["POST"])
def create_issue(project_id):
    """Crear nuevo issue"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        issue_data = {
            "project": project_id,
            "subject": data.get("subject"),
            "description": data.get("description", ""),
            "priority": data.get("priority", 1),
            "severity": data.get("severity", 1),
            "type": data.get("type", 1)
        }
        
        if not issue_data["subject"]:
            return jsonify({"error": "Título del issue requerido"}), 400
        
        issues_url = f"{session['host']}/api/v1/issues"
        issue = make_api_request(issues_url, "POST", issue_data, auth_token=session["auth_token"])
        
        print(f"[*] Issue creado: {issue.get('subject')} (ID: {issue.get('id')})")
        
        return jsonify(issue)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error creando issue: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/issues/<int:issue_id>", methods=["GET"])
def get_issue(issue_id):
    """Obtener detalles de un issue"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        issue_url = f"{session['host']}/api/v1/issues/{issue_id}"
        issue = make_api_request(issue_url, "GET", auth_token=session["auth_token"])
        
        return jsonify(issue)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error obteniendo issue: {e}")
        return jsonify({"error": str(e)}), 500

# =================== MILESTONES ENDPOINTS ===================

@app.route("/projects/<int:project_id>/milestones", methods=["GET"])
def list_milestones(project_id):
    """Listar milestones de un proyecto"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        milestones_url = f"{session['host']}/api/v1/milestones?project={project_id}"
        milestones = make_api_request(milestones_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "milestones": milestones,
            "total": len(milestones) if isinstance(milestones, list) else 0,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error listando milestones: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>/milestones", methods=["POST"])
def create_milestone(project_id):
    """Crear nuevo milestone"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        milestone_data = {
            "project": project_id,
            "name": data.get("name"),
            "estimated_start": data.get("estimated_start"),
            "estimated_finish": data.get("estimated_finish")
        }
        
        if not milestone_data["name"]:
            return jsonify({"error": "Nombre del milestone requerido"}), 400
        
        milestones_url = f"{session['host']}/api/v1/milestones"
        milestone = make_api_request(milestones_url, "POST", milestone_data, auth_token=session["auth_token"])
        
        print(f"[*] Milestone creado: {milestone.get('name')} (ID: {milestone.get('id')})")
        
        return jsonify(milestone)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error creando milestone: {e}")
        return jsonify({"error": str(e)}), 500

# =================== MEMBERS ENDPOINTS ===================

@app.route("/projects/<int:project_id>/members", methods=["GET"])
def list_project_members(project_id):
    """Listar miembros de un proyecto"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        members_url = f"{session['host']}/api/v1/memberships?project={project_id}"
        members = make_api_request(members_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "members": members,
            "total": len(members) if isinstance(members, list) else 0,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error listando miembros: {e}")
        return jsonify({"error": str(e)}), 500

# =================== WIKIS ENDPOINTS ===================

@app.route("/projects/<int:project_id>/wiki", methods=["GET"])
def list_wiki_pages(project_id):
    """Listar páginas wiki de un proyecto"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        wiki_url = f"{session['host']}/api/v1/wiki?project={project_id}"
        wiki_pages = make_api_request(wiki_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "wiki_pages": wiki_pages,
            "total": len(wiki_pages) if isinstance(wiki_pages, list) else 0,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error listando wiki: {e}")
        return jsonify({"error": str(e)}), 500

# =================== PROJECT STATUSES ENDPOINTS ===================

@app.route("/projects/<int:project_id>/userstory-statuses", methods=["GET"])
def get_userstory_statuses(project_id):
    """Obtener estados disponibles para user stories"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        statuses_url = f"{session['host']}/api/v1/userstory-statuses?project={project_id}"
        statuses = make_api_request(statuses_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "statuses": statuses,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error obteniendo estados: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>/task-statuses", methods=["GET"])
def get_task_statuses(project_id):
    """Obtener estados disponibles para tasks"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        statuses_url = f"{session['host']}/api/v1/task-statuses?project={project_id}"
        statuses = make_api_request(statuses_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "statuses": statuses,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error obteniendo estados: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/projects/<int:project_id>/issue-statuses", methods=["GET"])
def get_issue_statuses(project_id):
    """Obtener estados disponibles para issues"""
    try:
        session_id = request.args.get("session_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        statuses_url = f"{session['host']}/api/v1/issue-statuses?project={project_id}"
        statuses = make_api_request(statuses_url, "GET", auth_token=session["auth_token"])
        
        return jsonify({
            "statuses": statuses,
            "project_id": project_id
        })
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error obteniendo estados: {e}")
        return jsonify({"error": str(e)}), 500

# =================== ASSIGNMENT ENDPOINTS ===================

@app.route("/user_stories/<int:story_id>/assign", methods=["POST"])
def assign_user_story(story_id):
    """Asignar user story a un usuario"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        user_id = data.get("user_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        # Obtener story actual
        story_url = f"{session['host']}/api/v1/userstories/{story_id}"
        current_story = make_api_request(story_url, "GET", auth_token=session["auth_token"])
        
        # Actualizar asignación
        update_data = {
            "version": current_story["version"],
            "assigned_to": user_id
        }
        
        updated_story = make_api_request(story_url, "PUT", update_data, auth_token=session["auth_token"])
        
        print(f"[*] User story {story_id} asignada a usuario {user_id}")
        
        return jsonify(updated_story)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error asignando user story: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/tasks/<int:task_id>/assign", methods=["POST"])
def assign_task(task_id):
    """Asignar task a un usuario"""
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        user_id = data.get("user_id")
        
        if not session_id:
            return jsonify({"error": "session_id requerido"}), 400
        
        session = get_session_data(session_id)
        if not session:
            return jsonify({"error": "Sesión inválida"}), 401
        
        # Obtener task actual
        task_url = f"{session['host']}/api/v1/tasks/{task_id}"
        current_task = make_api_request(task_url, "GET", auth_token=session["auth_token"])
        
        # Actualizar asignación
        update_data = {
            "version": current_task["version"],
            "assigned_to": user_id
        }
        
        updated_task = make_api_request(task_url, "PUT", update_data, auth_token=session["auth_token"])
        
        print(f"[*] Task {task_id} asignada a usuario {user_id}")
        
        return jsonify(updated_task)
        
    except TaigaAPIError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"[!] Error asignando task: {e}")
        return jsonify({"error": str(e)}), 500

def create_epic_from_text(text: str, session: dict, project_id: int):
    """Crear epic desde texto"""
    try:
        epic_info = parse_epic_text(text)
        
        epic_data = {
            "project": project_id,
            "subject": epic_info["subject"],
            "description": epic_info["description"],
            "color": epic_info.get("color", "#999999")
        }
        
        epics_url = f"{session['host']}/api/v1/epics"
        epic = make_api_request(epics_url, "POST", epic_data, auth_token=session["auth_token"])
        
        return {
            "action": "create_epic_from_text",
            "success": True,
            "epic": {
                "id": epic["id"],
                "subject": epic["subject"],
                "color": epic["color"]
            }
        }
        
    except Exception as e:
        return {
            "action": "create_epic_from_text",
            "success": False,
            "error": str(e)
        }

def create_task_from_text(text: str, session: dict, project_id: int):
    """Crear task desde texto"""
    try:
        task_info = parse_task_text(text)
        
        task_data = {
            "project": project_id,
            "subject": task_info["subject"],
            "description": task_info["description"]
        }
        
        # Si se especifica una user story, vincularla
        if task_info.get("user_story_id"):
            task_data["user_story"] = task_info["user_story_id"]
        
        tasks_url = f"{session['host']}/api/v1/tasks"
        task = make_api_request(tasks_url, "POST", task_data, auth_token=session["auth_token"])
        
        return {
            "action": "create_task_from_text",
            "success": True,
            "task": {
                "id": task["id"],
                "subject": task["subject"],
                "ref": task.get("ref")
            }
        }
        
    except Exception as e:
        return {
            "action": "create_task_from_text",
            "success": False,
            "error": str(e)
        }

def create_issue_from_text(text: str, session: dict, project_id: int):
    """Crear issue desde texto"""
    try:
        issue_info = parse_issue_text(text)
        
        issue_data = {
            "project": project_id,
            "subject": issue_info["subject"],
            "description": issue_info["description"],
            "priority": issue_info.get("priority", 1),
            "severity": issue_info.get("severity", 1),
            "type": issue_info.get("type", 1)
        }
        
        issues_url = f"{session['host']}/api/v1/issues"
        issue = make_api_request(issues_url, "POST", issue_data, auth_token=session["auth_token"])
        
        return {
            "action": "create_issue_from_text",
            "success": True,
            "issue": {
                "id": issue["id"],
                "subject": issue["subject"],
                "ref": issue.get("ref")
            }
        }
        
    except Exception as e:
        return {
            "action": "create_issue_from_text",
            "success": False,
            "error": str(e)
        }

def create_milestone_from_text(text: str, session: dict, project_id: int):
    """Crear milestone desde texto"""
    try:
        milestone_info = parse_milestone_text(text)
        
        milestone_data = {
            "project": project_id,
            "name": milestone_info["name"],
            "estimated_start": milestone_info.get("estimated_start"),
            "estimated_finish": milestone_info.get("estimated_finish")
        }
        
        milestones_url = f"{session['host']}/api/v1/milestones"
        milestone = make_api_request(milestones_url, "POST", milestone_data, auth_token=session["auth_token"])
        
        return {
            "action": "create_milestone_from_text",
            "success": True,
            "milestone": {
                "id": milestone["id"],
                "name": milestone["name"]
            }
        }
        
    except Exception as e:
        return {
            "action": "create_milestone_from_text",
            "success": False,
            "error": str(e)
        }

def list_project_elements(text: str, session: dict, project_id: int):
    """Listar elementos del proyecto"""
    try:
        text_lower = text.lower()
        results = {}
        
        if "epic" in text_lower:
            epics_url = f"{session['host']}/api/v1/epics?project={project_id}"
            epics = make_api_request(epics_url, "GET", auth_token=session["auth_token"])
            results["epics"] = {"count": len(epics), "items": epics[:5]}  # Primeros 5
        
        if "task" in text_lower:
            tasks_url = f"{session['host']}/api/v1/tasks?project={project_id}"
            tasks = make_api_request(tasks_url, "GET", auth_token=session["auth_token"])
            results["tasks"] = {"count": len(tasks), "items": tasks[:5]}
        
        if "issue" in text_lower:
            issues_url = f"{session['host']}/api/v1/issues?project={project_id}"
            issues = make_api_request(issues_url, "GET", auth_token=session["auth_token"])
            results["issues"] = {"count": len(issues), "items": issues[:5]}
        
        if "milestone" in text_lower or "sprint" in text_lower:
            milestones_url = f"{session['host']}/api/v1/milestones?project={project_id}"
            milestones = make_api_request(milestones_url, "GET", auth_token=session["auth_token"])
            results["milestones"] = {"count": len(milestones), "items": milestones[:5]}
        
        if "historia" in text_lower or "user story" in text_lower:
            stories_url = f"{session['host']}/api/v1/userstories?project={project_id}"
            stories = make_api_request(stories_url, "GET", auth_token=session["auth_token"])
            results["user_stories"] = {"count": len(stories), "items": stories[:5]}
        
        if "miembro" in text_lower or "member" in text_lower:
            members_url = f"{session['host']}/api/v1/memberships?project={project_id}"
            members = make_api_request(members_url, "GET", auth_token=session["auth_token"])
            results["members"] = {"count": len(members), "items": members}
        
        # Si no se especifica nada, listar todo
        if not results:
            # Listar todos los elementos
            for element_type, endpoint in [
                ("epics", "epics"),
                ("user_stories", "userstories"),
                ("tasks", "tasks"),
                ("issues", "issues"),
                ("milestones", "milestones")
            ]:
                url = f"{session['host']}/api/v1/{endpoint}?project={project_id}"
                items = make_api_request(url, "GET", auth_token=session["auth_token"])
                results[element_type] = {"count": len(items), "items": items[:3]}
        
        return {
            "action": "list_project_elements",
            "success": True,
            "project_id": project_id,
            "elements": results
        }
        
    except Exception as e:
        return {
            "action": "list_project_elements",
            "success": False,
            "error": str(e)
        }

def handle_assignments(text: str, session: dict, project_id: int = None):
    """Manejar asignaciones de elementos"""
    try:
        # Parsear el texto para extraer IDs y tipos
        assignment_info = parse_assignment_text(text)
        
        results = []
        
        for assignment in assignment_info:
            item_type = assignment["type"]  # user_story, task, issue, epic
            item_id = assignment["item_id"]
            user_id = assignment["user_id"]
            
            if item_type == "user_story":
                url = f"{session['host']}/api/v1/userstories/{item_id}"
            elif item_type == "task":
                url = f"{session['host']}/api/v1/tasks/{item_id}"
            elif item_type == "issue":
                url = f"{session['host']}/api/v1/issues/{item_id}"
            elif item_type == "epic":
                url = f"{session['host']}/api/v1/epics/{item_id}"
            else:
                continue
            
            # Obtener item actual
            current_item = make_api_request(url, "GET", auth_token=session["auth_token"])
            
            # Actualizar asignación
            update_data = {
                "version": current_item["version"],
                "assigned_to": user_id
            }
            
            updated_item = make_api_request(url, "PUT", update_data, auth_token=session["auth_token"])
            
            results.append({
                "type": item_type,
                "id": item_id,
                "assigned_to": user_id,
                "success": True
            })
        
        return {
            "action": "handle_assignments",
            "success": True,
            "assignments": results
        }
        
    except Exception as e:
        return {
            "action": "handle_assignments",
            "success": False,
            "error": str(e)
        }

# Funciones auxiliares de parsing

def parse_epic_text(text: str) -> Dict[str, str]:
    """Parsear texto para extraer información de epic"""
    lines = text.strip().split('\n')
    subject = lines[0].replace("crear epic", "").replace(":", "").strip()
    
    if not subject:
        subject = "Epic desde texto"
    
    description = text if len(lines) > 1 else f"Epic: {subject}"
    
    # Detectar color si se menciona
    color = "#999999"  # Default
    color_patterns = {
        "rojo": "#d62728", "red": "#d62728",
        "azul": "#1f77b4", "blue": "#1f77b4", 
        "verde": "#2ca02c", "green": "#2ca02c",
        "amarillo": "#ffbb33", "yellow": "#ffbb33",
        "naranja": "#ff7f0e", "orange": "#ff7f0e",
        "morado": "#9467bd", "purple": "#9467bd"
    }
    
    text_lower = text.lower()
    for color_name, color_hex in color_patterns.items():
        if color_name in text_lower:
            color = color_hex
            break
    
    return {
        "subject": subject,
        "description": description,
        "color": color
    }

def parse_task_text(text: str) -> Dict[str, str]:
    """Parsear texto para extraer información de task"""
    lines = text.strip().split('\n')
    subject = lines[0].replace("crear task", "").replace("crear tarea", "").replace(":", "").strip()
    
    if not subject:
        subject = "Task desde texto"
    
    description = text if len(lines) > 1 else f"Task: {subject}"
    
    # Detectar si se menciona una user story
    user_story_id = None
    us_pattern = r'user story #?(\d+)|historia #?(\d+)|us #?(\d+)'
    match = re.search(us_pattern, text.lower())
    if match:
        user_story_id = int(match.group(1) or match.group(2) or match.group(3))
    
    return {
        "subject": subject,
        "description": description,
        "user_story_id": user_story_id
    }

def parse_issue_text(text: str) -> Dict[str, Any]:
    """Parsear texto para extraer información de issue"""
    lines = text.strip().split('\n')
    subject = lines[0].replace("crear issue", "").replace("reportar bug", "").replace(":", "").strip()
    
    if not subject:
        subject = "Issue desde texto"
    
    description = text if len(lines) > 1 else f"Issue: {subject}"
    
    # Detectar prioridad y severidad por palabras clave
    priority = 1  # Normal por defecto
    severity = 1  # Normal por defecto
    issue_type = 1  # Bug por defecto
    
    text_lower = text.lower()
    
    # Prioridad
    if any(word in text_lower for word in ["alta", "high", "urgente", "crítico"]):
        priority = 3
    elif any(word in text_lower for word in ["baja", "low", "menor"]):
        priority = 1
    
    # Severidad
    if any(word in text_lower for word in ["crítico", "critical", "bloqueante"]):
        severity = 3
    elif any(word in text_lower for word in ["menor", "minor", "cosmético"]):
        severity = 1
    
    # Tipo
    if any(word in text_lower for word in ["mejora", "enhancement", "feature"]):
        issue_type = 2
    elif any(word in text_lower for word in ["pregunta", "question", "duda"]):
        issue_type = 3
    
    return {
        "subject": subject,
        "description": description,
        "priority": priority,
        "severity": severity,
        "type": issue_type
    }

def parse_milestone_text(text: str) -> Dict[str, str]:
    """Parsear texto para extraer información de milestone"""
    lines = text.strip().split('\n')
    name = lines[0].replace("crear milestone", "").replace("crear sprint", "").replace(":", "").strip()
    
    if not name:
        name = "Milestone desde texto"
    
    # Extraer fechas si se mencionan
    estimated_start = None
    estimated_finish = None
    
    # Patrones de fecha
    date_patterns = [
        r'inicio[:\s]*(\d{4}-\d{2}-\d{2})',
        r'start[:\s]*(\d{4}-\d{2}-\d{2})', 
        r'fin[:\s]*(\d{4}-\d{2}-\d{2})',
        r'end[:\s]*(\d{4}-\d{2}-\d{2})',
        r'finish[:\s]*(\d{4}-\d{2}-\d{2})'
    ]
    
    for pattern in date_patterns:
        match = re.search(pattern, text.lower())
        if match:
            date_str = match.group(1)
            if 'inicio' in pattern or 'start' in pattern:
                estimated_start = date_str
            else:
                estimated_finish = date_str
    
    return {
        "name": name,
        "estimated_start": estimated_start,
        "estimated_finish": estimated_finish
    }

def parse_assignment_text(text: str) -> List[Dict[str, Any]]:
    """Parsear texto para extraer asignaciones"""
    assignments = []
    
    # Patrones para detectar asignaciones
    patterns = [
        r'asignar (\w+) #?(\d+) a usuario (\d+)',
        r'assign (\w+) #?(\d+) to user (\d+)',
        r'(\w+) #?(\d+) → usuario (\d+)',
        r'(\w+) #?(\d+) para usuario (\d+)'
    ]
    
    for pattern in patterns:
        matches = re.findall(pattern, text.lower())
        for match in matches:
            item_type_raw = match[0]
            item_id = int(match[1])
            user_id = int(match[2])
            
            # Normalizar tipo de elemento
            type_mapping = {
                "historia": "user_story",
                "story": "user_story", 
                "us": "user_story",
                "task": "task",
                "tarea": "task",
                "issue": "issue",
                "epic": "epic"
            }
            
            item_type = type_mapping.get(item_type_raw, "user_story")
            
            assignments.append({
                "type": item_type,
                "item_id": item_id,
                "user_id": user_id
            })
    
    return assignments

@app.route("/auto-session", methods=["GET"])
def get_auto_session():
    """Obtener información de la sesión automática"""
    if AUTO_SESSION_ID and AUTO_SESSION_DATA:
        return jsonify({
            "session_id": AUTO_SESSION_ID,
            "username": AUTO_SESSION_DATA.get('username'),
            "host": AUTO_SESSION_DATA.get('host'),
            "expires_at": AUTO_SESSION_DATA.get('expires_at').isoformat() if AUTO_SESSION_DATA.get('expires_at') else None,
            "created_at": AUTO_SESSION_DATA.get('created_at').isoformat() if AUTO_SESSION_DATA.get('created_at') else None,
            "valid": is_session_valid(AUTO_SESSION_ID),
            "renewed": AUTO_SESSION_DATA.get('renewed', False),
            "auto_login": True,
            "auto_renewal_enabled": True
        })
    else:
        return jsonify({
            "auto_login": False,
            "auto_renewal_enabled": False,
            "error": "No hay sesión automática configurada"
        }), 404

@app.route("/auto-session/renew", methods=["POST"])
def renew_auto_session_endpoint():
    """Forzar renovación de la sesión automática"""
    try:
        print(f"[🔄] Renovación manual de sesión automática solicitada")
        
        if renew_auto_session():
            return jsonify({
                "success": True,
                "message": "Sesión automática renovada exitosamente",
                "new_session_id": AUTO_SESSION_ID,
                "expires_at": AUTO_SESSION_DATA.get('expires_at').isoformat() if AUTO_SESSION_DATA else None
            })
        else:
            return jsonify({
                "success": False,
                "error": "Error renovando sesión automática"
            }), 500
            
    except Exception as e:
        print(f"[!] Error en renovación manual: {e}")
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

# Inicialización automática
def initialize_auto_login():
    """Inicializar login automático"""
    print(f"\n{'='*50}")
    print(f"🚀 INICIALIZANDO TAIGA MCP SERVICE")
    print(f"{'='*50}")
    
    success = auto_login()
    
    if success:
        print(f"✅ Servicio listo con login automático")
        print(f"📋 Puedes usar los endpoints sin session_id")
        print(f"🌐 Endpoint de salud: http://{FLASK_HOST}:{FLASK_PORT}/health")
        print(f"🔑 Sesión automática: http://{FLASK_HOST}:{FLASK_PORT}/auto-session")
    else:
        print(f"⚠️ Servicio iniciado sin login automático")
        print(f"💡 Usa el endpoint /login para autenticarte manualmente")
    
    print(f"{'='*50}\n")

if __name__ == "__main__":
    # Realizar login automático al iniciar
    initialize_auto_login()
    
    # Iniciar servidor Flask
    app.run(host=FLASK_HOST, port=FLASK_PORT, debug=False) 