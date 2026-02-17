#!/bin/bash

# Script para convertir el capítulo TFM de Markdown a Word con diagramas
# Requiere: pandoc, python3

set -e

echo "🚀 Iniciando conversión del TFM a Word..."

# Directorios
DOCS_DIR="docs/architecture"
DIAGRAMS_DIR="diagrams"
OUTPUT_DIR="docs/architecture/output"
TEMP_DIR="docs/architecture/temp"

# Archivos
INPUT_FILE="$DOCS_DIR/tfm-capitulo-arquitectura.md"
OUTPUT_FILE="$OUTPUT_DIR/TFM-Capitulo-Arquitectura-URBIX.docx"
TEMP_MD="$TEMP_DIR/tfm-with-images.md"
MAPPER_SCRIPT="$DOCS_DIR/diagram-mapper.py"

# Crear directorios si no existen
mkdir -p "$OUTPUT_DIR"
mkdir -p "$TEMP_DIR"

echo "📁 Directorios creados"

# Función para verificar dependencias
check_dependencies() {
    echo "🔍 Verificando dependencias..."
    
    if ! command -v pandoc &> /dev/null; then
        echo "❌ Error: pandoc no está instalado"
        echo "Instalar con:"
        echo "  macOS: brew install pandoc"
        echo "  Ubuntu: sudo apt-get install pandoc"
        echo "  Windows: choco install pandoc"
        exit 1
    fi
    
    if ! command -v python3 &> /dev/null; then
        echo "❌ Error: python3 no está instalado"
        exit 1
    fi
    
    if ! command -v mmdc &> /dev/null; then
        echo "⚠️  Advertencia: mermaid-cli no está instalado"
        echo "Para generar diagramas automáticamente, instalar con:"
        echo "  npm install -g @mermaid-js/mermaid-cli"
        echo "Continuando con diagramas PNG existentes..."
    fi
    
    echo "✅ Dependencias verificadas"
}

# Función para generar diagramas PNG desde Mermaid (si mmdc está disponible)
generate_diagrams() {
    if command -v mmdc &> /dev/null; then
        echo "🎨 Generando diagramas PNG desde archivos Mermaid..."
        
        local generated_count=0
        
        for mmd_file in "$DIAGRAMS_DIR"/*.mmd; do
            if [[ -f "$mmd_file" ]]; then
                filename=$(basename "$mmd_file" .mmd)
                png_file="$DIAGRAMS_DIR/${filename}.png"
                
                if [[ ! -f "$png_file" ]] || [[ "$mmd_file" -nt "$png_file" ]]; then
                    echo "  Generando: $filename.png"
                    mmdc -i "$mmd_file" -o "$png_file" \
                         -t neutral \
                         -b white \
                         --width 1200 \
                         --height 800 \
                         --configFile /dev/null 2>/dev/null || {
                        echo "    ⚠️  Error generando $filename.png, continuando..."
                    }
                    ((generated_count++))
                fi
            fi
        done
        
        echo "✅ Diagramas generados: $generated_count"
    else
        echo "⏭️  Saltando generación de diagramas (usando PNG existentes)"
    fi
}

# Función para procesar el Markdown usando el mapper de Python
process_markdown() {
    echo "📝 Procesando Markdown con mapper inteligente de diagramas..."
    
    if [[ -f "$MAPPER_SCRIPT" ]]; then
        python3 "$MAPPER_SCRIPT" "$INPUT_FILE" "$TEMP_MD"
    else
        echo "❌ Error: Script mapper no encontrado: $MAPPER_SCRIPT"
        exit 1
    fi
}

# Función para crear el archivo de configuración de Pandoc
create_pandoc_config() {
    echo "⚙️  Creando configuración de Pandoc..."
    
    # Crear archivo de metadatos YAML
    cat > "$TEMP_DIR/metadata.yaml" << 'EOF'
---
title: "Capítulo 4: Arquitectura y Diseño del Sistema URBIX"
subtitle: "Trabajo de Fin de Máster en Ingeniería de Software"
author: "Estudiante TFM"
date: "Febrero 2026"
lang: es-ES
documentclass: article
geometry: 
  - margin=2.5cm
  - a4paper
fontsize: 11pt
linestretch: 1.5
mainfont: "Times New Roman"
sansfont: "Arial"
monofont: "Courier New"
toc: true
toc-depth: 3
numbersections: true
secnumdepth: 3
colorlinks: true
linkcolor: blue
urlcolor: blue
citecolor: blue
header-includes: |
  \usepackage{fancyhdr}
  \pagestyle{fancy}
  \fancyhead[L]{TFM - Sistema URBIX}
  \fancyhead[R]{Arquitectura y Diseño}
  \fancyfoot[C]{\thepage}
  \usepackage{float}
  \floatplacement{figure}{H}
---
EOF

    echo "✅ Configuración creada"
}

# Función para convertir a Word
convert_to_word() {
    echo "📄 Convirtiendo a Word con Pandoc..."
    
    # Opciones de Pandoc optimizadas para documentos académicos
    pandoc \
        --from markdown+smart+table_captions+fenced_code_blocks+backtick_code_blocks+pipe_tables+grid_tables+multiline_tables \
        --to docx \
        --metadata-file="$TEMP_DIR/metadata.yaml" \
        --toc \
        --toc-depth=3 \
        --number-sections \
        --highlight-style=tango \
        --resource-path=".:$DIAGRAMS_DIR:$DOCS_DIR" \
        --wrap=auto \
        --columns=80 \
        --output="$OUTPUT_FILE" \
        "$TEMP_MD" \
        --verbose
    
    echo "✅ Conversión completada"
}

# Función para generar estadísticas del documento
generate_stats() {
    echo "📊 Generando estadísticas del documento..."
    
    local word_count=$(wc -w < "$INPUT_FILE")
    local line_count=$(wc -l < "$INPUT_FILE")
    local char_count=$(wc -c < "$INPUT_FILE")
    local diagram_count=$(grep -c "```mermaid" "$INPUT_FILE" || echo "0")
    local png_count=$(find "$DIAGRAMS_DIR" -name "*.png" | wc -l)
    
    echo "📈 Estadísticas del documento:"
    echo "   📝 Palabras: $word_count"
    echo "   📄 Líneas: $line_count"
    echo "   🔤 Caracteres: $char_count"
    echo "   🎨 Diagramas Mermaid: $diagram_count"
    echo "   🖼️  Imágenes PNG disponibles: $png_count"
    
    if [[ -f "$OUTPUT_FILE" ]]; then
        local file_size=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
        echo "   📦 Tamaño archivo Word: $file_size"
    fi
}

# Función para validar el resultado
validate_output() {
    echo "🔍 Validando archivo de salida..."
    
    if [[ -f "$OUTPUT_FILE" ]]; then
        local file_size=$(stat -f%z "$OUTPUT_FILE" 2>/dev/null || stat -c%s "$OUTPUT_FILE" 2>/dev/null || echo "0")
        
        if [[ $file_size -gt 1000000 ]]; then  # > 1MB
            echo "✅ Archivo generado correctamente (${file_size} bytes)"
            return 0
        else
            echo "⚠️  Advertencia: Archivo muy pequeño (${file_size} bytes)"
            return 1
        fi
    else
        echo "❌ Error: Archivo de salida no generado"
        return 1
    fi
}

# Función para limpiar archivos temporales
cleanup() {
    echo "🧹 Limpiando archivos temporales..."
    
    if [[ -d "$TEMP_DIR" ]]; then
        rm -rf "$TEMP_DIR"
        echo "✅ Archivos temporales eliminados"
    fi
}

# Función para mostrar instrucciones de apertura
show_instructions() {
    echo ""
    echo "🎉 ¡Conversión completada exitosamente!"
    echo "📁 Archivo generado: $OUTPUT_FILE"
    echo ""
    echo "💡 Para abrir el documento:"
    
    case "$OSTYPE" in
        darwin*)  echo "   open '$OUTPUT_FILE'" ;;
        linux*)   echo "   xdg-open '$OUTPUT_FILE'" ;;
        msys*)    echo "   start '$OUTPUT_FILE'" ;;
        *)        echo "   Abrir manualmente: $OUTPUT_FILE" ;;
    esac
    
    echo ""
    echo "📋 Contenido incluido:"
    echo "   ✅ Texto completo del TFM"
    echo "   ✅ Diagramas convertidos a imágenes"
    echo "   ✅ Tabla de contenidos automática"
    echo "   ✅ Numeración de secciones"
    echo "   ✅ Formato académico profesional"
}

# Función principal
main() {
    echo "🎯 Convirtiendo TFM URBIX a Word"
    echo "=================================="
    echo ""
    
    # Verificar que el archivo de entrada existe
    if [[ ! -f "$INPUT_FILE" ]]; then
        echo "❌ Error: Archivo de entrada no encontrado: $INPUT_FILE"
        exit 1
    fi
    
    # Ejecutar pasos de conversión
    check_dependencies
    generate_diagrams
    create_pandoc_config
    process_markdown
    convert_to_word
    
    # Validar y mostrar resultados
    if validate_output; then
        generate_stats
        show_instructions
    else
        echo "❌ Error en la conversión. Revisar logs anteriores."
        exit 1
    fi
    
    # Limpiar archivos temporales
    cleanup
    
    echo ""
    echo "🚀 Proceso completado exitosamente!"
}

# Manejar señales para limpieza
trap cleanup EXIT INT TERM

# Ejecutar función principal
main "$@"