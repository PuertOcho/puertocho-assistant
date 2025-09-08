#!/usr/bin/env python3
"""
Script de análisis para identificar código innecesario en intentmanagerms
Analiza qué controladores, servicios y endpoints están siendo realmente utilizados.
"""

import os
import re
import json
from pathlib import Path
from typing import Dict, List, Set, Any
from collections import defaultdict

class CodeAnalyzer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.src_main = self.project_root / "src" / "main" / "java" / "com" / "intentmanagerms"
        self.controllers_dir = self.src_main / "infrastructure" / "web"
        self.services_dir = self.src_main / "application" / "services"
        self.scripts_dir = self.project_root / "scripts"
        
        # Almacenar análisis
        self.controllers = {}
        self.services = {}
        self.dependencies = defaultdict(set)
        self.usage_analysis = {}
        
    def analyze_controllers(self):
        """Analiza todos los controladores y sus endpoints"""
        print("🔍 Analizando controladores...")
        
        for java_file in self.controllers_dir.glob("*.java"):
            controller_name = java_file.stem
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Extraer mapping base
            base_mapping = re.search(r'@RequestMapping\("([^"]+)"\)', content)
            base_path = base_mapping.group(1) if base_mapping else ""
            
            # Extraer endpoints
            endpoints = []
            endpoint_patterns = [
                r'@(PostMapping|GetMapping|PutMapping|DeleteMapping|RequestMapping)\("([^"]+)"\)',
                r'@(PostMapping|GetMapping|PutMapping|DeleteMapping|RequestMapping)\(value\s*=\s*"([^"]+)"',
                r'@(PostMapping|GetMapping|PutMapping|DeleteMapping)(?!\()',  # Sin parámetros
            ]
            
            for pattern in endpoint_patterns:
                matches = re.findall(pattern, content)
                for match in matches:
                    if len(match) == 2:
                        method, path = match
                        full_path = base_path + path
                    else:
                        method = match[0] if match else "Unknown"
                        full_path = base_path
                    endpoints.append(f"{method}: {full_path}")
            
            # Extraer dependencias (@Autowired)
            autowired_pattern = r'@Autowired\s+(?:private\s+)?(\w+)\s+(\w+);'
            dependencies = re.findall(autowired_pattern, content)
            
            self.controllers[controller_name] = {
                'file': str(java_file),
                'base_path': base_path,
                'endpoints': endpoints,
                'dependencies': dependencies,
                'content_size': len(content)
            }
    
    def analyze_services(self):
        """Analiza todos los servicios"""
        print("🔍 Analizando servicios...")
        
        for java_file in self.services_dir.glob("*.java"):
            service_name = java_file.stem
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Verificar si es un servicio de Spring
            is_service = '@Service' in content or '@Component' in content
            
            # Extraer dependencias
            autowired_pattern = r'@Autowired\s+(?:private\s+)?(\w+)\s+(\w+);'
            dependencies = re.findall(autowired_pattern, content)
            
            # Extraer métodos públicos
            public_methods = re.findall(r'public\s+\w+\s+(\w+)\s*\(', content)
            
            self.services[service_name] = {
                'file': str(java_file),
                'is_service': is_service,
                'dependencies': dependencies,
                'public_methods': public_methods,
                'content_size': len(content),
                'uses_redis': 'RedisTemplate' in content or 'Redis' in content,
                'uses_llm': any(keyword in content.lower() for keyword in ['llm', 'gpt', 'openai', 'anthropic']),
                'uses_vector': 'vector' in content.lower() or 'embedding' in content.lower()
            }
    
    def analyze_test_usage(self):
        """Analiza qué endpoints están siendo probados"""
        print("🔍 Analizando uso en tests...")
        
        endpoint_usage = defaultdict(set)
        
        for test_file in self.scripts_dir.glob("test_*.py"):
            with open(test_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Buscar llamadas a endpoints
            api_calls = re.findall(r'["\']([^"\']*?/api/v1/[^"\']*?)["\']', content)
            for api_call in api_calls:
                endpoint_usage[api_call].add(test_file.name)
        
        return dict(endpoint_usage)
    
    def analyze_cross_references(self):
        """Analiza referencias cruzadas entre servicios"""
        print("🔍 Analizando referencias cruzadas...")
        
        all_files = list(self.services_dir.glob("*.java")) + list(self.controllers_dir.glob("*.java"))
        
        for java_file in all_files:
            file_name = java_file.stem
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Buscar referencias a otros servicios
            for other_service in self.services.keys():
                if other_service != file_name and other_service in content:
                    self.dependencies[file_name].add(other_service)
    
    def generate_usage_report(self):
        """Genera reporte de uso"""
        print("\n📊 ANÁLISIS DE CÓDIGO INNECESARIO\n")
        print("=" * 60)
        
        # Análisis de tests
        test_usage = self.analyze_test_usage()
        
        # Controladores por prioridad de uso
        controller_priority = {}
        
        for controller, info in self.controllers.items():
            priority_score = 0
            usage_indicators = []
            
            # Verificar si algún endpoint aparece en tests
            for endpoint in info['endpoints']:
                endpoint_path = endpoint.split(': ')[-1] if ': ' in endpoint else endpoint
                if any(endpoint_path in used_endpoint for used_endpoint in test_usage.keys()):
                    priority_score += 10
                    usage_indicators.append(f"Testeado: {endpoint}")
            
            # Verificar dependencias críticas
            critical_services = ['RagIntentClassifier', 'ConversationManager', 'WhisperTranscriptionService', 'TtsGenerationService']
            for dep_type, dep_name in info['dependencies']:
                if dep_name in critical_services:
                    priority_score += 5
                    usage_indicators.append(f"Depende de servicio crítico: {dep_name}")
            
            # Verificar si es controlador principal
            if controller in ['ConversationManagerController', 'RagIntentClassifierController']:
                priority_score += 15
                usage_indicators.append("Controlador principal identificado")
            
            controller_priority[controller] = {
                'score': priority_score,
                'indicators': usage_indicators,
                'info': info
            }
        
        # Mostrar análisis de controladores
        print("🎯 CONTROLADORES POR PRIORIDAD DE USO:")
        print("-" * 50)
        
        sorted_controllers = sorted(controller_priority.items(), key=lambda x: x[1]['score'], reverse=True)
        
        for controller, analysis in sorted_controllers:
            score = analysis['score']
            indicators = analysis['indicators']
            info = analysis['info']
            
            if score >= 10:
                status = "🟢 ACTIVO"
            elif score >= 5:
                status = "🟡 POSIBLEMENTE ACTIVO"
            else:
                status = "🔴 CANDIDATO A ELIMINACIÓN"
            
            print(f"\n{status} {controller}")
            print(f"   Puntuación: {score}")
            print(f"   Base path: {info['base_path']}")
            print(f"   Endpoints: {len(info['endpoints'])}")
            print(f"   Tamaño: {info['content_size']} chars")
            
            if indicators:
                print("   Indicadores de uso:")
                for indicator in indicators:
                    print(f"     • {indicator}")
            else:
                print("   ⚠️  No se encontraron indicadores de uso")
        
        # Análisis de servicios
        print(f"\n\n🔧 SERVICIOS POR CATEGORÍA:")
        print("-" * 50)
        
        service_categories = {
            'core': [],
            'llm': [],
            'redis': [],
            'utility': [],
            'unused': []
        }
        
        for service, info in self.services.items():
            # Clasificar servicios
            if service in ['ConversationManager', 'RagIntentClassifier', 'WhisperTranscriptionService', 'TtsGenerationService']:
                service_categories['core'].append(service)
            elif info['uses_llm']:
                service_categories['llm'].append(service)
            elif info['uses_redis']:
                service_categories['redis'].append(service)
            elif any(dep[1] in ['ConversationManager', 'RagIntentClassifier'] for dep in info['dependencies']):
                service_categories['utility'].append(service)
            else:
                service_categories['unused'].append(service)
        
        for category, services in service_categories.items():
            if services:
                if category == 'core':
                    print(f"\n🟢 SERVICIOS PRINCIPALES ({len(services)}):")
                elif category == 'llm':
                    print(f"\n🟡 SERVICIOS LLM ({len(services)}):")
                elif category == 'redis':
                    print(f"\n🟡 SERVICIOS REDIS ({len(services)}):")
                elif category == 'utility':
                    print(f"\n🟠 SERVICIOS AUXILIARES ({len(services)}):")
                else:
                    print(f"\n🔴 SERVICIOS SIN USO APARENTE ({len(services)}):")
                
                for service in sorted(services):
                    info = self.services[service]
                    print(f"   • {service} ({info['content_size']} chars, {len(info['public_methods'])} métodos)")
        
        # Endpoints en tests
        print(f"\n\n🧪 ENDPOINTS ENCONTRADOS EN TESTS:")
        print("-" * 50)
        
        if test_usage:
            for endpoint, test_files in sorted(test_usage.items()):
                print(f"   • {endpoint}")
                for test_file in test_files:
                    print(f"     └── {test_file}")
        else:
            print("   ⚠️  No se encontraron referencias a endpoints en tests")
        
        # Recomendaciones
        print(f"\n\n💡 RECOMENDACIONES DE LIMPIEZA:")
        print("-" * 50)
        
        candidates_for_removal = [controller for controller, analysis in sorted_controllers 
                                  if analysis['score'] == 0]
        
        if candidates_for_removal:
            print(f"\n🗑️  CONTROLADORES CANDIDATOS A ELIMINACIÓN ({len(candidates_for_removal)}):")
            total_size = 0
            for controller in candidates_for_removal:
                size = controller_priority[controller]['info']['content_size']
                total_size += size
                print(f"   • {controller} ({size} chars)")
            print(f"   Total a eliminar: ~{total_size:,} caracteres")
        
        unused_services = service_categories['unused']
        if unused_services:
            print(f"\n🗑️  SERVICIOS CANDIDATOS A ELIMINACIÓN ({len(unused_services)}):")
            total_size = 0
            for service in unused_services:
                size = self.services[service]['content_size']
                total_size += size
                print(f"   • {service} ({size} chars)")
            print(f"   Total a eliminar: ~{total_size:,} caracteres")
        
        # Resumen final
        total_controllers = len(self.controllers)
        active_controllers = len([c for c, a in sorted_controllers if a['score'] >= 10])
        total_services = len(self.services)
        core_services = len(service_categories['core'])
        
        print(f"\n\n📈 RESUMEN:")
        print("-" * 50)
        print(f"   Controladores totales: {total_controllers}")
        print(f"   Controladores activos: {active_controllers}")
        print(f"   Servicios totales: {total_services}")
        print(f"   Servicios principales: {core_services}")
        print(f"   Endpoints en tests: {len(test_usage)}")
        
        efficiency = ((active_controllers + core_services) / (total_controllers + total_services)) * 100
        print(f"   Eficiencia estimada: {efficiency:.1f}%")
        
        return {
            'controllers': controller_priority,
            'services': service_categories,
            'test_usage': test_usage,
            'recommendations': {
                'remove_controllers': candidates_for_removal,
                'remove_services': unused_services
            }
        }
    
    def save_detailed_report(self, filename: str = "code_analysis_report.json"):
        """Guarda un reporte detallado en JSON"""
        report = {
            'controllers': self.controllers,
            'services': self.services,
            'test_usage': self.analyze_test_usage(),
            'analysis_timestamp': str(Path(__file__).stat().st_mtime)
        }
        
        report_path = self.project_root / filename
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        print(f"\n💾 Reporte detallado guardado en: {report_path}")

def main():
    project_root = "/home/puertocho/Proyectos/puertocho-assistant/puertocho-assistant-server/intentmanagerms"
    
    if not Path(project_root).exists():
        print(f"❌ Error: No se encontró el directorio del proyecto: {project_root}")
        return
    
    analyzer = CodeAnalyzer(project_root)
    
    # Ejecutar análisis
    analyzer.analyze_controllers()
    analyzer.analyze_services()
    analyzer.analyze_cross_references()
    
    # Generar reporte
    analysis_result = analyzer.generate_usage_report()
    
    # Guardar reporte detallado
    analyzer.save_detailed_report()

if __name__ == "__main__":
    main()
