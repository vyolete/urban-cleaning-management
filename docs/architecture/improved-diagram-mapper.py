#!/usr/bin/env python3
"""
Mapper mejorado de diagramas que mapea correctamente cada diagrama
según su contexto específico en el documento TFM.
"""

import re
import os
import sys
from pathlib import Path

class ImprovedDiagramMapper:
    def __init__(self, diagrams_dir="diagrams"):
        self.diagrams_dir = diagrams_dir
        self.diagram_mappings = self._create_improved_mappings()
    
    def _create_improved_mappings(self):
        """Crear mapeo mejorado basado en análisis del contenido real del TFM."""
        return {
            # Mapeo por contenido específico del diagrama
            'content_mappings': {
                # Vista de Casos de Uso
                'Urban Cleaning Management System': 'use-case-complete-system-overview.png',
                'Authentication & Authorization': 'use-case-authentication-flow.png',
                'Report Management': 'use-case-report-task-management.png',
                'User Profile & Account': 'use-case-user-profile-sessions.png',
                'System Configuration': 'use-case-admin-configuration.png',
                
                # Diagramas de Secuencia específicos
                'Login request': 'sequence-login-authentication.png',
                'Register User': 'sequence-user-registration.png',
                'Submit Report': 'sequence-report-submission.png',
                'Task assignment': 'sequence-task-assignment.png',
                'State update': 'sequence-task-state-update.png',
                'Password reset': 'sequence-password-recovery.png',
                'Account deletion': 'sequence-account-deletion-gdpr.png',
                'Profile management': 'sequence-profile-management.png',
                'Session management': 'sequence-session-management.png',
                'Citizen feedback': 'sequence-citizen-feedback.png',
                'Analytics generation': 'sequence-analytics-generation.png',
                'Email notifications': 'sequence-email-notifications.png',
                'System configuration': 'sequence-system-configuration.png',
                'Audit logging': 'sequence-audit-logging.png',
                'Priority calculation': 'sequence-priority-calculation.png',
                
                # Diagramas de Proceso
                'Check for duplicates': 'process-duplicate-detection.png',
                'Report management': 'process-main-report-management.png',
                'Priority calculation process': 'process-priority-calculation.png',
                'Email notification': 'process-email-notifications.png',
                'Session management process': 'process-session-management.png',
                'GDPR compliance': 'process-gdpr-compliance.png',
                'Audit trail': 'process-audit-trail.png',
                'Dynamic configuration': 'process-dynamic-configuration.png',
                
                # Diagramas de Actividad
                'Submit Report Process': 'activity-submit-report-process.png',
                'Priority Calculation Activity': 'activity-priority-calculation.png',
                'Task Assignment Activity': 'activity-task-assignment.png',
                'Task State Update Activity': 'activity-task-state-update.png',
                'Algorithm Weights Update': 'activity-algorithm-weights-update.png',
                
                # Diagramas MVC
                'Controller Layer': 'mvc-controller-layer.png',
                'Service Layer': 'mvc-service-layer.png',
                'Repository Layer': 'mvc-repository-layer.png',
                'Security Layer': 'mvc-security-layer.png',
                'Configuration Layer': 'mvc-configuration-layer.png',
                'DTO Mapping': 'mvc-dto-mapping-layer.png',
                'Data Validation': 'mvc-data-validation.png',
                'Exception Handling': 'mvc-exception-handling.png',
                'Event System': 'mvc-event-system.png',
                'Frontend Backend Integration': 'mvc-frontend-backend-integration.png',
                
                # Diagramas de Implementación
                'Frontend Application': 'implementation-frontend-component-structure.png',
                'Backend Application': 'implementation-backend-package-structure.png',
                
                # Diagramas de Despliegue
                'AWS Complete Architecture': 'deployment-aws-complete-architecture.png',
                'Database Configuration': 'deployment-database-configuration.png',
                'Docker containers': 'deployment-docker-containers.png',
                
                # Modelo de Datos
                'erDiagram': 'erd-complete-database-schema.png',
                'User ||--o{ Report': 'erd-complete-database-schema.png',
            },
            
            # Mapeo por sección del documento
            'section_mappings': {
                '1.2 Visión General del Sistema': 'use-case-complete-system-overview.png',
                '2.3.7 Diagramas UML de Casos de Uso por Actor': 'use-case-complete-system-overview.png',
                '2.4.1 UC-004: Enviar Reporte de Incidencia': 'sequence-report-submission.png',
                '2.4.2 Cálculo de Prioridad de Tareas': 'activity-priority-calculation.png',
                '2.4.3 UC-032: Actualizar Estado de Tarea': 'sequence-task-state-update.png',
                
                # Vista Lógica - Sección 3
                '3.1.1 Flujos de Autenticación y Gestión de Sesiones': 'sequence-login-authentication.png',
                '3.1.2 Flujos de Gestión de Reportes y Tareas': 'sequence-report-submission.png',
                '3.1.3 Flujos de Retroalimentación y Analíticas': 'sequence-citizen-feedback.png',
                '3.1.4 Flujos de Gestión de Perfil y Sesiones': 'sequence-profile-management.png',
                '3.1.5 Flujos de Configuración del Sistema': 'sequence-system-configuration.png',
                '3.1.6 Flujos de Notificaciones y Auditoría': 'sequence-email-notifications.png',
                '3.2 Diagrama de Clases del Sistema': 'mvc-general-architecture-flow.png',
                '3.3.1 Máquina de Estado de Tarea': 'activity-task-state-update.png',
                '3.4.1 Colaboración de Envío de Reporte': 'sequence-report-submission.png',
                '3.4.2 Colaboración de Ciclo de Vida de Tarea': 'sequence-task-assignment.png',
                '3.4.3 Colaboración de Flujo de Autenticación': 'sequence-login-authentication.png',
                
                # Vista de Procesos - Sección 4
                '4.1 Algoritmo de Deduplicación Espacial': 'process-duplicate-detection.png',
                '4.2 Arquitectura de Seguridad': 'mvc-security-layer.png',
                '4.2.1 Proceso P01: Gestión de Reportes Ciudadanos': 'process-main-report-management.png',
                '4.2.2 Proceso P02: Cálculo de Prioridad de Tareas': 'process-priority-calculation.png',
                '4.2.3 Proceso P03: Asignación de Tareas a Operadores': 'sequence-task-assignment.png',
                '4.2.4 Proceso P04: Gestión del Ciclo de Vida de Tareas': 'sequence-task-state-update.png',
                '4.2.5 Proceso P05: Detección y Fusión de Duplicados': 'process-duplicate-detection.png',
                '4.3.2 Arquitectura Orientada a Eventos': 'mvc-event-system.png',
                '4.3.3 Gestión de Sesiones y Autenticación': 'sequence-session-management.png',
                '4.3.4 Procesos de Cumplimiento GDPR': 'sequence-account-deletion-gdpr.png',
                
                # Vista de Implementación - Sección 5
                '5.1 Estructura de Paquetes Backend': 'implementation-backend-package-structure.png',
                '5.3.2 Dependencias Externas Backend': 'mvc-general-architecture-flow.png',
                
                # Vista de Despliegue - Sección 6
                '6.1 Componentes de Despliegue': 'deployment-docker-containers.png',
                '6.3 Dependencias y Orden de Inicio': 'deployment-database-configuration.png',
                '6.4 Topología de Red': 'deployment-aws-complete-architecture.png',
                
                # Modelo de Datos - Sección 7
                '7.4 Diagrama Entidad-Relación': 'erd-complete-database-schema.png',
            }
        }
    
    def find_best_diagram(self, mermaid_content, section_context=""):
        """Encontrar el mejor diagrama basado en contenido y contexto específico."""
        
        # Normalizar contenido
        content_lower = mermaid_content.lower()
        section_lower = section_context.lower()
        
        print(f"   🔍 Analizando sección: '{section_context}'")
        print(f"   📝 Contenido: {content_lower[:100]}...")
        
        # 1. Primero buscar por sección específica (más preciso)
        section_mappings = self.diagram_mappings['section_mappings']
        for section_key, diagram_file in section_mappings.items():
            if section_key.lower() in section_lower:
                print(f"   ✅ Mapeo por sección: {section_key} -> {diagram_file}")
                return diagram_file
        
        # 2. Buscar por contenido específico
        content_mappings = self.diagram_mappings['content_mappings']
        for content_key, diagram_file in content_mappings.items():
            if content_key.lower() in content_lower:
                print(f"   ✅ Mapeo por contenido: {content_key} -> {diagram_file}")
                return diagram_file
        
        # 3. Mapeo por tipo de diagrama y contexto
        if 'sequencediagram' in content_lower.replace(' ', ''):
            if 'login' in content_lower or 'auth' in content_lower:
                return 'sequence-login-authentication.png'
            elif 'report' in content_lower:
                return 'sequence-report-submission.png'
            elif 'task' in content_lower:
                return 'sequence-task-assignment.png'
            elif 'user' in content_lower:
                return 'sequence-user-registration.png'
            else:
                return 'sequence-login-authentication.png'
        
        elif 'erdiagram' in content_lower.replace(' ', ''):
            return 'erd-complete-database-schema.png'
        
        elif 'flowchart' in content_lower or 'graph' in content_lower:
            if 'duplicate' in content_lower:
                return 'process-duplicate-detection.png'
            elif 'priority' in content_lower:
                return 'process-priority-calculation.png'
            elif 'report' in content_lower:
                return 'process-main-report-management.png'
            else:
                return 'mvc-general-architecture-flow.png'
        
        # 4. Mapeo por contexto de sección
        if 'vista de casos de uso' in section_lower or 'use case' in section_lower:
            return 'use-case-complete-system-overview.png'
        elif 'vista lógica' in section_lower or 'logical view' in section_lower:
            return 'sequence-login-authentication.png'
        elif 'vista de procesos' in section_lower or 'process view' in section_lower:
            return 'process-main-report-management.png'
        elif 'vista de implementación' in section_lower:
            return 'implementation-backend-package-structure.png'
        elif 'vista de despliegue' in section_lower:
            return 'deployment-docker-containers.png'
        elif 'modelo de datos' in section_lower or 'data model' in section_lower:
            return 'erd-complete-database-schema.png'
        
        # 5. Diagrama por defecto
        print(f"   ⚠️  Usando diagrama por defecto para: {section_context}")
        return 'mvc-general-architecture-flow.png'
    
    def extract_section_context(self, content, position):
        """Extraer contexto de la sección donde está el diagrama."""
        
        # Buscar hacia atrás para encontrar el encabezado más cercano
        lines = content[:position].split('\n')
        
        # Buscar encabezados de diferentes niveles
        for line in reversed(lines):
            stripped = line.strip()
            if stripped.startswith('#'):
                # Limpiar el encabezado
                header = stripped.lstrip('#').strip()
                return header
        
        return ""
    
    def process_markdown(self, input_file, output_file):
        """Procesar archivo Markdown con mapeo mejorado de diagramas."""
        
        print(f"📝 Procesando con mapper mejorado: {input_file}")
        
        with open(input_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Patrón para encontrar bloques mermaid
        mermaid_pattern = r'```mermaid\n(.*?)\n```'
        
        def replace_mermaid(match):
            mermaid_content = match.group(1)
            position = match.start()
            
            # Extraer contexto de la sección
            section_context = self.extract_section_context(content, position)
            
            # Encontrar el mejor diagrama
            image_file = self.find_best_diagram(mermaid_content, section_context)
            
            # Verificar que el archivo existe
            image_path = Path(self.diagrams_dir) / image_file
            if not image_path.exists():
                print(f"   ⚠️  Advertencia: {image_file} no encontrado")
                # Buscar archivo alternativo
                alternative = 'mvc-general-architecture-flow.png'
                alt_path = Path(self.diagrams_dir) / alternative
                if alt_path.exists():
                    image_file = alternative
                    print(f"   🔄 Usando alternativo: {image_file}")
            
            print(f"   🎨 Mapeo final: '{section_context}' -> {image_file}")
            
            # Crear referencia de imagen
            return f'![Diagrama]({self.diagrams_dir}/{image_file})'
        
        # Reemplazar todos los bloques mermaid
        processed_content = re.sub(mermaid_pattern, replace_mermaid, content, flags=re.DOTALL)
        
        # Escribir archivo procesado
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(processed_content)
        
        print(f"✅ Archivo procesado guardado en: {output_file}")
        
        return output_file

def main():
    """Función principal."""
    
    if len(sys.argv) < 3:
        print("Uso: python3 improved-diagram-mapper.py <input.md> <output.md>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    if not os.path.exists(input_file):
        print(f"❌ Error: Archivo de entrada no encontrado: {input_file}")
        sys.exit(1)
    
    # Crear directorio de salida si no existe
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    
    # Procesar archivo
    mapper = ImprovedDiagramMapper()
    mapper.process_markdown(input_file, output_file)
    
    print("🎉 Procesamiento mejorado completado!")

if __name__ == "__main__":
    main()