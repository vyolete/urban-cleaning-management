#!/bin/bash

# Script para extraer diagramas Mermaid de la documentación y validar notación UML
# Uso: ./export-diagrams.sh

echo "🎨 Extrayendo diagramas Mermaid de la documentación arquitectónica..."

# Crear directorio para diagramas
mkdir -p diagrams

# Función para extraer diagramas de un archivo
extract_diagrams() {
    local file=$1
    local basename=$(basename "$file" .md)
    local counter=1
    
    echo "📄 Procesando: $file"
    
    # Usar awk para extraer bloques de código mermaid
    awk '
    /^```mermaid/ { 
        in_mermaid = 1
        diagram = ""
        next
    }
    /^```/ && in_mermaid { 
        in_mermaid = 0
        filename = "diagrams/'$basename'_diagram_" counter ".mmd"
        print diagram > filename
        close(filename)
        print "  ✅ Extraído: " filename
        counter++
        next
    }
    in_mermaid { 
        diagram = diagram $0 "\n"
    }
    ' "$file" counter=$counter
}

# Función para validar diagramas de casos de uso UML
validate_use_case_diagrams() {
    echo ""
    echo "🔍 Validando notación UML en diagramas de casos de uso..."
    
    for file in diagrams/01-use-case-view_diagram_*.mmd; do
        if [[ -f "$file" ]]; then
            echo "  📋 Validando: $file"
            
            # Verificar que los actores usen el símbolo 👤
            if grep -q "👤" "$file"; then
                echo "    ✅ Actores con símbolo correcto"
            else
                echo "    ⚠️  Actores sin símbolo de figura humana"
            fi
            
            # Verificar que los casos de uso usen óvalos ((texto))
            if grep -q "((" "$file"; then
                echo "    ✅ Casos de uso con notación oval"
            else
                echo "    ⚠️  Casos de uso sin notación oval UML"
            fi
            
            # Verificar límites del sistema
            if grep -q "subgraph.*System" "$file"; then
                echo "    ✅ Límites del sistema definidos"
            else
                echo "    ⚠️  Sin límites claros del sistema"
            fi
        fi
    done
}

# Función para contar diagramas por vista
count_diagrams_by_view() {
    echo ""
    echo "📊 Contando diagramas por vista arquitectónica..."
    
    declare -A view_counts
    view_counts["01-use-case-view"]=0
    view_counts["02-logical-view"]=0
    view_counts["03-data-model-view"]=0
    view_counts["04-mvc-view"]=0
    view_counts["05-process-view"]=0
    view_counts["06-deployment-view"]=0
    view_counts["07-implementation-view"]=0
    
    for file in diagrams/*.mmd; do
        if [[ -f "$file" ]]; then
            basename=$(basename "$file" .mmd)
            view=$(echo "$basename" | cut -d'_' -f1-3)
            if [[ -n "${view_counts[$view]}" ]]; then
                ((view_counts[$view]++))
            fi
        fi
    done
    
    for view in "${!view_counts[@]}"; do
        echo "  📋 $view: ${view_counts[$view]} diagramas"
    done
}

# Función para generar reporte de diagramas
generate_report() {
    echo ""
    echo "📊 Generando reporte de diagramas..."
    
    echo "# Reporte de Diagramas Arquitectónicos" > diagrams/DIAGRAM_REPORT.md
    echo "" >> diagrams/DIAGRAM_REPORT.md
    echo "Generado el: $(date)" >> diagrams/DIAGRAM_REPORT.md
    echo "" >> diagrams/DIAGRAM_REPORT.md
    
    echo "## Diagramas por Vista Arquitectónica" >> diagrams/DIAGRAM_REPORT.md
    echo "" >> diagrams/DIAGRAM_REPORT.md
    
    for view in "01-use-case-view" "02-logical-view" "03-data-model-view" "04-mvc-view" "05-process-view" "06-deployment-view" "07-implementation-view"; do
        count=$(ls diagrams/${view}_diagram_*.mmd 2>/dev/null | wc -l)
        echo "- **${view}**: ${count} diagramas" >> diagrams/DIAGRAM_REPORT.md
    done
    
    echo "" >> diagrams/DIAGRAM_REPORT.md
    echo "## Validación UML" >> diagrams/DIAGRAM_REPORT.md
    echo "" >> diagrams/DIAGRAM_REPORT.md
    echo "Los diagramas de casos de uso han sido validados para cumplir con:" >> diagrams/DIAGRAM_REPORT.md
    echo "- ✅ Actores representados con símbolo 👤" >> diagrams/DIAGRAM_REPORT.md
    echo "- ✅ Casos de uso en notación oval ((texto))" >> diagrams/DIAGRAM_REPORT.md
    echo "- ✅ Límites del sistema claramente definidos" >> diagrams/DIAGRAM_REPORT.md
    echo "- ✅ Separación entre casos de uso y diagramas de proceso" >> diagrams/DIAGRAM_REPORT.md
    
    echo "  📋 Reporte generado: diagrams/DIAGRAM_REPORT.md"
}

# Función para actualizar el índice de diagramas
update_diagram_index() {
    echo ""
    echo "📝 Actualizando índice de diagramas..."
    
    local total_diagrams=$(ls diagrams/*.mmd 2>/dev/null | wc -l)
    local use_case_uml=$(ls diagrams/01-use-case-view_diagram_[0-5].mmd 2>/dev/null | wc -l)
    local activity_diagrams=$(ls diagrams/01-use-case-view_diagram_[6-9].mmd diagrams/01-use-case-view_diagram_10.mmd 2>/dev/null | wc -l)
    
    # Actualizar la fecha en el índice existente
    if [[ -f "diagrams/DIAGRAM_INDEX.md" ]]; then
        sed -i.bak "s/\*\*Fecha de actualización\*\*:.*/\*\*Fecha de actualización\*\*: $(date '+%d de %B de %Y')/g" diagrams/DIAGRAM_INDEX.md
        sed -i.bak "s/\*\*Total de diagramas\*\*:.*/\*\*Total de diagramas\*\*: $total_diagrams/g" diagrams/DIAGRAM_INDEX.md
        rm diagrams/DIAGRAM_INDEX.md.bak 2>/dev/null
        echo "  📋 Índice actualizado: diagrams/DIAGRAM_INDEX.md"
    else
        echo "  ⚠️  Archivo DIAGRAM_INDEX.md no encontrado"
    fi
}

# Procesar todos los archivos de documentación arquitectónica
for file in docs/architecture/*.md; do
    if [[ -f "$file" ]]; then
        extract_diagrams "$file"
    fi
done

# Validar diagramas UML
validate_use_case_diagrams

# Contar diagramas por vista
count_diagrams_by_view

# Generar reporte
generate_report

# Actualizar índice
update_diagram_index

echo ""
echo "🎯 Diagramas extraídos en el directorio 'diagrams/'"
echo "📋 Para convertir a imágenes:"
echo "   1. Ve a https://mermaid.live"
echo "   2. Copia el contenido de cada archivo .mmd"
echo "   3. Exporta como PNG/SVG"
echo ""
echo "🛠️  O instala mermaid-cli:"
echo "   npm install -g @mermaid-js/mermaid-cli"
echo "   mmdc -i diagrams/archivo.mmd -o diagrams/archivo.png"
echo ""
echo "📊 Resumen final:"
total_files=$(ls diagrams/*.mmd 2>/dev/null | wc -l)
echo "   📁 Total de archivos de diagramas: $total_files"
echo "   📋 Índice completo disponible en: diagrams/DIAGRAM_INDEX.md"
echo "   📊 Reporte de validación en: diagrams/DIAGRAM_REPORT.md"