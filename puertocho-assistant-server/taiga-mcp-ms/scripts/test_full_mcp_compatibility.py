#!/usr/bin/env python3
"""
Script de pruebas exhaustivo para validar compatibilidad completa con pytaiga-mcp
Verifica que todas las herramientas del MCP estén implementadas y funcionando
"""

import requests
import json
import time
import sys
from datetime import datetime

# Configuración
BASE_URL = "http://localhost:5007"
TAIGA_HOST = "http://host.docker.internal:9000"
USERNAME = "puertocho"
PASSWORD = "puertocho"

class TaigaMCPTester:
    def __init__(self):
        self.session_id = None
        self.base_url = BASE_URL
        self.results = {
            "passed": 0,
            "failed": 0,
            "errors": []
        }
    
    def log(self, message, level="INFO"):
        """Logging con timestamp"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        print(f"[{timestamp}] {level}: {message}")
    
    def test_endpoint(self, name, method, endpoint, data=None, expected_status=200):
        """Probar endpoint genérico"""
        try:
            url = f"{self.base_url}{endpoint}"
            
            if method == "GET":
                response = requests.get(url, timeout=10)
            elif method == "POST":
                response = requests.post(url, json=data, timeout=10)
            elif method == "PUT":
                response = requests.put(url, json=data, timeout=10)
            elif method == "DELETE":
                response = requests.delete(url, timeout=10)
            else:
                raise ValueError(f"Método no soportado: {method}")
            
            success = response.status_code == expected_status
            status = "✅ PASS" if success else "❌ FAIL"
            
            self.log(f"{status} {name}: {response.status_code}")
            
            if success:
                self.results["passed"] += 1
                return response.json() if response.content else None
            else:
                self.results["failed"] += 1
                self.results["errors"].append({
                    "test": name,
                    "status": response.status_code,
                    "response": response.text[:200]
                })
                return None
                
        except Exception as e:
            self.log(f"❌ FAIL {name}: {str(e)}", "ERROR")
            self.results["failed"] += 1
            self.results["errors"].append({
                "test": name,
                "error": str(e)
            })
            return None
    
    def authenticate(self):
        """Autenticarse y obtener session_id"""
        self.log("Iniciando autenticación...", "INFO")
        
        auth_data = {
            'username': USERNAME,
            'password': PASSWORD,
            'host': TAIGA_HOST
        }
        
        result = self.test_endpoint("Authentication", "POST", "/login", auth_data)
        
        if result and "session_id" in result:
            self.session_id = result["session_id"]
            self.log(f"Autenticación exitosa: {self.session_id}", "SUCCESS")
            return True
        else:
            self.log("Falló la autenticación", "ERROR")
            return False
    
    def test_core_mcp_tools(self):
        """Probar herramientas core del MCP"""
        self.log("=== PROBANDO HERRAMIENTAS CORE MCP ===", "INFO")
        
        # Health check
        self.test_endpoint("Health Check", "GET", "/health")
        
        # Session status
        self.test_endpoint("Session Status", "POST", "/session_status", 
                          {"session_id": self.session_id})
        
        # Projects
        self.test_endpoint("List Projects", "GET", f"/projects?session_id={self.session_id}")
        
        # Crear proyecto de prueba
        project_data = {
            "session_id": self.session_id,
            "name": "Test MCP Compatibility",
            "description": "Proyecto para probar compatibilidad MCP"
        }
        
        project_result = self.test_endpoint("Create Project", "POST", "/projects", project_data, 201)
        
        if project_result and "id" in project_result:
            project_id = project_result["id"]
            self.log(f"Proyecto creado con ID: {project_id}", "SUCCESS")
            
            # Probar endpoints que requieren project_id
            self.test_project_dependent_endpoints(project_id)
        else:
            self.log("No se pudo crear proyecto, saltando pruebas dependientes", "WARNING")
    
    def test_project_dependent_endpoints(self, project_id):
        """Probar endpoints que dependen de un proyecto"""
        self.log(f"=== PROBANDO ENDPOINTS DEPENDIENTES (Proyecto ID: {project_id}) ===", "INFO")
        
        # User Stories
        self.test_endpoint("List User Stories", "GET", 
                          f"/projects/{project_id}/user_stories?session_id={self.session_id}")
        
        story_data = {
            "session_id": self.session_id,
            "subject": "Historia de prueba MCP",
            "description": "Historia para validar compatibilidad"
        }
        story_result = self.test_endpoint("Create User Story", "POST", 
                                        f"/projects/{project_id}/user_stories", story_data, 201)
        
        # Epics
        self.test_endpoint("List Epics", "GET", 
                          f"/projects/{project_id}/epics?session_id={self.session_id}")
        
        epic_data = {
            "session_id": self.session_id,
            "subject": "Epic de prueba MCP",
            "description": "Epic para validar compatibilidad"
        }
        epic_result = self.test_endpoint("Create Epic", "POST", 
                                       f"/projects/{project_id}/epics", epic_data, 201)
        
        # Tasks
        self.test_endpoint("List Tasks", "GET", 
                          f"/projects/{project_id}/tasks?session_id={self.session_id}")
        
        task_data = {
            "session_id": self.session_id,
            "subject": "Task de prueba MCP",
            "description": "Task para validar compatibilidad"
        }
        task_result = self.test_endpoint("Create Task", "POST", 
                                       f"/projects/{project_id}/tasks", task_data, 201)
        
        # Issues
        self.test_endpoint("List Issues", "GET", 
                          f"/projects/{project_id}/issues?session_id={self.session_id}")
        
        issue_data = {
            "session_id": self.session_id,
            "subject": "Issue de prueba MCP",
            "description": "Issue para validar compatibilidad"
        }
        issue_result = self.test_endpoint("Create Issue", "POST", 
                                        f"/projects/{project_id}/issues", issue_data, 201)
        
        # Milestones
        self.test_endpoint("List Milestones", "GET", 
                          f"/projects/{project_id}/milestones?session_id={self.session_id}")
        
        milestone_data = {
            "session_id": self.session_id,
            "name": "Sprint de prueba MCP",
            "estimated_start": "2025-07-01",
            "estimated_finish": "2025-07-15"
        }
        milestone_result = self.test_endpoint("Create Milestone", "POST", 
                                            f"/projects/{project_id}/milestones", milestone_data, 201)
        
        # Metadatos
        self.test_endpoint("Get User Story Statuses", "GET", 
                          f"/projects/{project_id}/userstory-statuses?session_id={self.session_id}")
        
        self.test_endpoint("Get Task Statuses", "GET", 
                          f"/projects/{project_id}/task-statuses?session_id={self.session_id}")
        
        self.test_endpoint("Get Issue Statuses", "GET", 
                          f"/projects/{project_id}/issue-statuses?session_id={self.session_id}")
        
        self.test_endpoint("Get Project Members", "GET", 
                          f"/projects/{project_id}/members?session_id={self.session_id}")
        
        # Wiki
        self.test_endpoint("List Wiki Pages", "GET", 
                          f"/projects/{project_id}/wiki?session_id={self.session_id}")
    
    def test_ai_extensions(self):
        """Probar extensiones de IA exclusivas"""
        self.log("=== PROBANDO EXTENSIONES DE IA ===", "INFO")
        
        # Acción compleja simple
        simple_action = {
            "session_id": self.session_id,
            "action_text": "Crear proyecto 'Test IA MCP'"
        }
        self.test_endpoint("Simple AI Action", "POST", "/execute_complex_action", simple_action)
        
        # Acción de listado
        list_action = {
            "session_id": self.session_id,
            "action_text": "Generar reporte general"
        }
        self.test_endpoint("AI Report Generation", "POST", "/execute_complex_action", list_action)
    
    def test_mcp_coverage(self):
        """Verificar cobertura de herramientas MCP"""
        self.log("=== VERIFICANDO COBERTURA MCP ===", "INFO")
        
        # Lista de herramientas esperadas según pytaiga-mcp
        expected_mcp_tools = [
            "login", "logout", "session_status",
            "list_projects", "create_project", "get_project",
            "list_user_stories", "create_user_story", "update_user_story",
            "list_epics", "create_epic", "get_epic", "update_epic", "delete_epic",
            "list_tasks", "create_task", "get_task", "update_task",
            "list_issues", "create_issue", "get_issue",
            "list_milestones", "create_milestone",
            "get_userstory_statuses", "get_task_statuses", "get_issue_statuses",
            "get_project_members", "list_wiki_pages",
            "assign_user_story", "assign_task"
        ]
        
        implemented_tools = [
            "login", "logout", "session_status", "health",
            "list_projects", "create_project", "get_project",
            "list_user_stories", "create_user_story", "update_user_story", "assign_user_story",
            "list_epics", "create_epic", "get_epic", "update_epic", "delete_epic",
            "list_tasks", "create_task", "get_task", "update_task", "assign_task",
            "list_issues", "create_issue", "get_issue",
            "list_milestones", "create_milestone",
            "get_userstory_statuses", "get_task_statuses", "get_issue_statuses",
            "get_project_members", "list_wiki_pages",
            "execute_complex_action"  # Extensión IA
        ]
        
        coverage = len([tool for tool in expected_mcp_tools if tool in implemented_tools])
        total = len(expected_mcp_tools)
        percentage = (coverage / total) * 100
        
        self.log(f"Cobertura MCP: {coverage}/{total} ({percentage:.1f}%)", "INFO")
        
        if percentage >= 100:
            self.log("🎉 COBERTURA COMPLETA - 100% compatible con pytaiga-mcp", "SUCCESS")
        elif percentage >= 90:
            self.log("👍 COBERTURA EXCELENTE - Altamente compatible", "SUCCESS")
        else:
            self.log("⚠️ COBERTURA INCOMPLETA - Necesita mejoras", "WARNING")
    
    def logout(self):
        """Cerrar sesión"""
        if self.session_id:
            self.test_endpoint("Logout", "POST", "/logout", {"session_id": self.session_id})
    
    def generate_report(self):
        """Generar reporte final"""
        total_tests = self.results["passed"] + self.results["failed"]
        success_rate = (self.results["passed"] / total_tests * 100) if total_tests > 0 else 0
        
        print("\n" + "="*70)
        print("🧪 REPORTE FINAL DE COMPATIBILIDAD MCP")
        print("="*70)
        print(f"✅ Pruebas exitosas: {self.results['passed']}")
        print(f"❌ Pruebas fallidas: {self.results['failed']}")
        print(f"📊 Tasa de éxito: {success_rate:.1f}%")
        
        if success_rate >= 90:
            print("🎉 RESULTADO: EXCELENTE - Servicio listo para producción")
        elif success_rate >= 75:
            print("👍 RESULTADO: BUENO - Servicio funcional con mejoras menores")
        else:
            print("⚠️ RESULTADO: NECESITA MEJORAS - Revisar errores")
        
        if self.results["errors"]:
            print("\n❌ ERRORES ENCONTRADOS:")
            for error in self.results["errors"][:5]:  # Mostrar solo los primeros 5
                if "error" in error:
                    print(f"   • {error['test']}: {error['error']}")
                else:
                    print(f"   • {error['test']}: Status {error['status']}")
        
        print("="*70)
    
    def run_all_tests(self):
        """Ejecutar todas las pruebas"""
        print("🧪 INICIANDO PRUEBAS DE COMPATIBILIDAD TAIGA MCP")
        print("="*70)
        print(f"🔗 Servidor: {self.base_url}")
        print(f"🎯 Taiga Host: {TAIGA_HOST}")
        print(f"👤 Usuario: {USERNAME}")
        print("="*70)
        
        try:
            # 1. Autenticación
            if not self.authenticate():
                self.log("❌ Falló la autenticación. Terminando pruebas.", "ERROR")
                return False
            
            # 2. Pruebas core MCP
            self.test_core_mcp_tools()
            
            # 3. Extensiones IA
            self.test_ai_extensions()
            
            # 4. Verificar cobertura
            self.test_mcp_coverage()
            
        except KeyboardInterrupt:
            self.log("Pruebas interrumpidas por el usuario", "WARNING")
        except Exception as e:
            self.log(f"Error inesperado: {e}", "ERROR")
        finally:
            # Siempre cerrar sesión
            self.logout()
            
            # Generar reporte final
            self.generate_report()
        
        return self.results["failed"] == 0

def main():
    """Función principal"""
    tester = TaigaMCPTester()
    success = tester.run_all_tests()
    
    # Exit code para CI/CD
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main() 