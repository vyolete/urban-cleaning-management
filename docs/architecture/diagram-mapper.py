#!/usr/bin/env python3
"""
Script para mapear diagramas Mermaid a imágenes PNG específicas
basándose en el contenido y contexto del documento TFM.
"""

import re
import os
import sys
from pathlib import Path

class DiagramMapper:
    def __init__(self, diagrams_dir="diagrams"):
        self.diagrams_dir = diagrams_dir
        self.diagram_mappings = self._create_diagram_mappings()
    
    def _create_diagram_mappings(self):
        """Crear mapeo detallado de diagramas basado en contenido y contexto."""
        return {
            # Vista de Casos de Uso
            'use_case_patterns': {
                'Urban Cleaning Management System': 'use-case-complete-system-overview.png',
                'Authentication & Authorization': 'use-case-authentication-flow.png',
                'Report Management': 'use-case-report-task-management.png',
                'User Profile & Account': 'use-case-user-profile-sessions.png',
                'System Configuration': 'use-case-admin-configuration.png',
            },
            
            # Vista Lógica - Diagramas de Secuencia
            'sequence_patterns': {
                'Login request': 'sequence-login-authentication.png',
                'Register User': 'sequence-user-registration.png',
                'Submit Report': 'sequence-report-submission.png',
                'Task assignment': 'sequence-task-assignment.png',
                'State update request': 'sequence-task-state-update.png',
                'Password reset request': 'sequence-password-recovery.png',
                'Account deletion': 'sequence-account-deletion-gdpr.png',
                'Profile management': 'sequence-profile-management.png',
                'Session management': 'sequence-session-management.png',
                'Citizen feedback': 'sequence-citizen-feedback.png',
                'Analytics generation': 'sequence-analytics-generation.png',
                'Email notifications': 'sequence-email-notifications.png',
                'System configuration': 'sequence-system-configuration.png',
                'Audit logging': 'sequence-audit-logging.png',
                'Priority calculation': 'sequence-priority-calculation.png',
            },
            
            # Vista de Procesos
            'process_patterns': {
                'Check for duplicates': 'process-duplicate-detection.png',
                'Report management': 'process-main-report-management.png',
                'Priority calculation': 'process-priority-calculation.png',
                'Email notifications': 'process-email-notifications.png',
                'Session management': 'process-session-management.png',
                'GDPR compliance': 'process-gdpr-compliance.png',
                'Audit trail': 'process-audit-trail.png',
                'Dynamic configuration': 'process-dynamic-configuration.png',
            },
            
            # Vista de Implementación
            'implementation_patterns': {
                'Frontend Application': 'implementation-frontend-component-structure.png',
                'Backend Application': 'implementation-backend-package-structure.png',
                'System Components': 'mvc-general-architecture-flow.png',
            },
            
            # Vista de Despliegue
            'deployment_patterns': {
                'AWS Complete Architecture': 'deployment-aws-complete-architecture.png',
                'Database Configuration': 'deployment-database-configuration.png',
                'Docker containers': 'deployment-docker-containers.png',
                'Network Security': 'deployment-network-security.png',
            },
            
            # Modelo de Datos
            'data_patterns': {
                'User ||--o{ Report': 'erd-complete-database-schema.png',
                'erDiagram': 'erd-complete-database-schema.png',
            },
            
            # Diagramas de Actividad
            'activity_patterns': {
                'Submit Report Process': 'activity-submit-report-process.png',
                'Priority Calculation': 'activity-priority-calculation.png',
                'Task Assignment': 'activity-task-assignment.png',
                'Task State Update': 'activity-task-state-update.png',
                'Algorithm Weights Update': 'activity-algorithm-weights-update.png',
            },
            
            # Diagramas MVC
            'mvc_patterns': {
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
            }
        }
    
    def find_best_diagram(self, mermaid_content, context_section=""):
        """Encontrar el mejor diagrama basado en contenido y contexto."""
        
        # Normalizar contenido para búsqueda
        content_lower = mermaid_content.lower()
        context_lower = context_section.lower()
        
        # Buscar por patrones específicos en orden de prioridad
        search_order = [
            'sequence_patterns',
            'process_patterns', 
            'use_case_patterns',
            'activity_patterns',
            'mvc_patterns',
            'implementation_patterns',
            'deployment_patterns',
            'data_patterns'
        ]
        
        for pattern_type in search_order:
            patterns = self.diagram_mappings.get(pattern_type, {})
            
            for keyword, image_file in patterns.items():
                # Buscar en contenido del diagrama
                if keyword.lower() in content_lower:
                    return image_file
                
                # Buscar en contexto de la sección
                if keyword.lower() in context_lower:
                    return image_file
        
        # Mapeo por tipo de diagrama
        if 'flowchart' in content_lower or 'graph' in content_lower:
            if 'login' in content_lower:
                return 'sequence-login-authentication.png'
            elif 'duplicate' in content_lower:
                return 'process-duplicate-detection.png'
            elif 'task' in content_lower:
                return 'sequence-task-assignment.png'
            else:
                return 'mvc-general-architecture-flow.png'
        
        elif 'erdiagram' in content_lower.replace(' ', ''):
            return 'erd-complete-database-schema.png'
        
        elif 'sequencediagram' in content_lower.replace(' ', ''):
            return 'sequence-login-authentication.png'
        
        # Diagrama por defecto
        return 'mvc-general-architecture-flow.png'
    
    def extract_section_context(self, content, position):
        """Extraer contexto de la sección donde está el diagrama."""
        
        # Buscar hacia atrás para encontrar el encabezado de sección más cercano
        lines = content[:position].split('\n')
        
        for line in reversed(lines):
            if line.startswith('#'):
                return line.strip('# ')
        
        return ""
    
    def process_markdown(self, input_file, output_file):
        """Procesar archivo Markdown reemplazando diagramas Mermaid."""
        
        print(f"📝 Procesando: {input_file}")
        
        with open(input_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Patrón para encontrar bloques mermaid con contexto
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
                print(f"⚠️  Advertencia: {image_file} no encontrado, usando diagrama por defecto")
                image_file = 'mvc-general-architecture-flow.png'
            
            print(f"   🎨 Mapeando diagrama en '{section_context}' -> {image_file}")
            
            # Crear referencia de imagen con ruta relativa
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
        print("Uso: python3 diagram-mapper.py <input.md> <output.md>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    if not os.path.exists(input_file):
        print(f"❌ Error: Archivo de entrada no encontrado: {input_file}")
        sys.exit(1)
    
    # Crear directorio de salida si no existe
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    
    # Procesar archivo
    mapper = DiagramMapper()
    mapper.process_markdown(input_file, output_file)
    
    print("🎉 Procesamiento completado!")

if __name__ == "__main__":
    main()