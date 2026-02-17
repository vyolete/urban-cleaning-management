#!/bin/bash

# Script para convertir diagramas Mermaid a PNG con nombres descriptivos
# Requiere: npm install -g @mermaid-js/mermaid-cli
# Uso: ./convert-diagrams-to-png.sh

echo "🖼️  Convirtiendo diagramas Mermaid a PNG..."

# Verificar si mermaid-cli está instalado
if ! command -v mmdc &> /dev/null; then
    echo "❌ Error: mermaid-cli no está instalado"
    echo "📦 Instalar con: npm install -g @mermaid-js/mermaid-cli"
    exit 1
fi

# Crear directorio para imágenes
mkdir -p diagrams/images

# Función para convertir un diagrama
convert_diagram() {
    local mmd_file=$1
    local png_file="diagrams/images/$(basename "$mmd_file" .mmd).png"
    
    if [[ -f "$mmd_file" ]]; then
        echo "  🔄 Convirtiendo: $(basename "$mmd_file")"
        mmdc -i "$mmd_file" -o "$png_file" -w 1920 -H 1080 --backgroundColor white
        if [[ $? -eq 0 ]]; then
            echo "    ✅ Generado: $(basename "$png_file")"
        else
            echo "    ❌ Error al convertir: $(basename "$mmd_file")"
        fi
    fi
}

echo ""
echo "🎯 01. CASOS DE USO - Convirtiendo..."

# Casos de Uso UML
convert_diagram "diagrams/use-case-complete-system-overview.mmd"
convert_diagram "diagrams/use-case-authentication-flow.mmd"
convert_diagram "diagrams/use-case-report-task-management.mmd"
convert_diagram "diagrams/use-case-admin-configuration.mmd"
convert_diagram "diagrams/use-case-user-profile-sessions.mmd"
convert_diagram "diagrams/use-case-analytics-notifications.mmd"

# Diagramas de Actividad
convert_diagram "diagrams/activity-submit-report-process.mmd"
convert_diagram "diagrams/activity-priority-calculation.mmd"
convert_diagram "diagrams/activity-task-state-update.mmd"
convert_diagram "diagrams/activity-task-assignment.mmd"
convert_diagram "diagrams/activity-algorithm-weights-update.mmd"

echo ""
echo "🔄 02. VISTA LÓGICA - Convirtiendo..."

# Diagramas de Secuencia
convert_diagram "diagrams/sequence-login-authentication.mmd"
convert_diagram "diagrams/sequence-user-registration.mmd"
convert_diagram "diagrams/sequence-report-submission.mmd"
convert_diagram "diagrams/sequence-task-state-update.mmd"
convert_diagram "diagrams/sequence-priority-calculation.mmd"
convert_diagram "diagrams/sequence-task-assignment.mmd"
convert_diagram "diagrams/sequence-citizen-feedback.mmd"
convert_diagram "diagrams/sequence-system-configuration.mmd"
convert_diagram "diagrams/sequence-analytics-generation.mmd"
convert_diagram "diagrams/sequence-session-management.mmd"
convert_diagram "diagrams/sequence-password-recovery.mmd"
convert_diagram "diagrams/sequence-email-notifications.mmd"
convert_diagram "diagrams/sequence-audit-logging.mmd"
convert_diagram "diagrams/sequence-profile-management.mmd"
convert_diagram "diagrams/sequence-account-deletion-gdpr.mmd"

echo ""
echo "🗄️  03. MODELO DE DATOS - Convirtiendo..."

convert_diagram "diagrams/erd-complete-database-schema.mmd"

echo ""
echo "🏗️  04. VISTA MVC - Convirtiendo..."

convert_diagram "diagrams/mvc-general-architecture-flow.mmd"
convert_diagram "diagrams/mvc-controller-layer.mmd"
convert_diagram "diagrams/mvc-service-layer.mmd"
convert_diagram "diagrams/mvc-repository-layer.mmd"
convert_diagram "diagrams/mvc-security-layer.mmd"
convert_diagram "diagrams/mvc-dto-mapping-layer.mmd"
convert_diagram "diagrams/mvc-configuration-layer.mmd"
convert_diagram "diagrams/mvc-exception-handling.mmd"
convert_diagram "diagrams/mvc-data-validation.mmd"
convert_diagram "diagrams/mvc-event-system.mmd"
convert_diagram "diagrams/mvc-frontend-backend-integration.mmd"

echo ""
echo "⚙️  05. PROCESOS - Convirtiendo..."

convert_diagram "diagrams/process-main-report-management.mmd"
convert_diagram "diagrams/process-duplicate-detection.mmd"
convert_diagram "diagrams/process-priority-calculation.mmd"
convert_diagram "diagrams/process-email-notifications.mmd"
convert_diagram "diagrams/process-audit-trail.mmd"
convert_diagram "diagrams/process-session-management.mmd"
convert_diagram "diagrams/process-dynamic-configuration.mmd"
convert_diagram "diagrams/process-gdpr-compliance.mmd"

echo ""
echo "🚀 06. DESPLIEGUE - Convirtiendo..."

convert_diagram "diagrams/deployment-aws-complete-architecture.mmd"
convert_diagram "diagrams/deployment-docker-containers.mmd"
convert_diagram "diagrams/deployment-network-security.mmd"
convert_diagram "diagrams/deployment-database-configuration.mmd"

echo ""
echo "💻 07. IMPLEMENTACIÓN - Convirtiendo..."

convert_diagram "diagrams/implementation-backend-package-structure.mmd"
convert_diagram "diagrams/implementation-frontend-component-structure.mmd"

echo ""
echo "✅ CONVERSIÓN COMPLETADA"
echo ""
echo "📊 Resumen:"
total_png=$(ls diagrams/images/*.png 2>/dev/null | wc -l)
echo "   🖼️  Total de imágenes PNG generadas: $total_png"
echo "   📁 Ubicación: diagrams/images/"
echo "   📐 Resolución: 1920x1080 con fondo blanco"
echo ""
echo "🎯 Las imágenes están listas para integración en TFM"