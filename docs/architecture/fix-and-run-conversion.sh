#!/bin/bash

# Script corregido para convertir el TFM a Word
# Soluciona problemas de sintaxis y dependencias

set -e

echo "🚀 Iniciando conversión corregida del TFM a Word..."

# Verificar que estamos en el directorio correcto
if [[ ! -f "docs/architecture/tfm-capitulo-arquitectura.md" ]]; then
    echo "❌ Error: Ejecutar desde el directorio raíz del proyecto"
    echo "Uso: ./docs/architecture/fix-and-run-conversion.sh"
    exit 1
fi

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

# Crear directorios
mkdir -p "$OUTPUT_DIR"
mkdir -p "$TEMP_DIR"

echo "📁 Directorios creados: $OUTPUT_DIR, $TEMP_DIR"

# Función para verificar dependencias
check_dependencies() {
    echo "🔍 Verificando dependencias..."
    
    # Verificar Python
    if ! command -v python3 &> /dev/null; then
        echo "❌ Error: python3 no está instalado"
        exit 1
    fi
    echo "✅ Python3 disponible: $(python3 --version)"
    
    # Verificar Pandoc (opcional)
    if command -v pandoc &> /dev/null; then
        echo "✅ Pandoc disponible: $(pandoc --version | head -1)"
        PANDOC_AVAILABLE=true
    else
        echo "⚠️  Pandoc no disponible - se generará solo el Markdown procesado"
        PANDOC_AVAILABLE=false
    fi
    
    # Verificar Mermaid CLI (opcional)
    if command -v mmdc &> /dev/null; then
        echo "✅ Mermaid CLI disponible"
        MERMAID_AVAILABLE=true
    else
        echo "⚠️  Mermaid CLI no disponible - usando PNG existentes"
        MERMAID_AVAILABLE=false
    fi
}

# Función para procesar Markdown con mapper de diagramas
process_markdown() {
    echo "📝 Procesando Markdown con mapper inteligente..."
    
    if [[ -f "$MAPPER_SCRIPT" ]]; then
        echo "Ejecutando: python3 $MAPPER_SCRIPT $INPUT_FILE $TEMP_MD"
        python3 "$MAPPER_SCRIPT" "$INPUT_FILE" "$TEMP_MD"
        
        if [[ -f "$TEMP_MD" ]]; then
            echo "✅ Markdown procesado creado: $TEMP_MD"
        else
            echo "❌ Error: No se pudo crear el archivo procesado"
            exit 1
        fi
    else
        echo "❌ Error: Script mapper no encontrado: $MAPPER_SCRIPT"
        exit 1
    fi
}

# Función para crear configuración de Pandoc
create_pandoc_config() {
    if [[ "$PANDOC_AVAILABLE" == "true" ]]; then
        echo "⚙️  Creando configuración de Pandoc..."
        
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
toc: true
toc-depth: 3
numbersections: true
secnumdepth: 3
colorlinks: true
linkcolor: blue
urlcolor: blue
citecolor: blue
---
EOF
        echo "✅ Configuración de Pandoc creada"
    fi
}

# Función para convertir a Word
convert_to_word() {
    if [[ "$PANDOC_AVAILABLE" == "true" ]]; then
        echo "📄 Convirtiendo a Word con Pandoc..."
        
        pandoc \
            --from markdown+smart+table_captions+fenced_code_blocks \
            --to docx \
            --metadata-file="$TEMP_DIR/metadata.yaml" \
            --toc \
            --toc-depth=3 \
            --number-sections \
            --resource-path=".:$DIAGRAMS_DIR:$DOCS_DIR" \
            --output="$OUTPUT_FILE" \
            "$TEMP_MD"
        
        echo "✅ Conversión a Word completada"
    else
        echo "⏭️  Saltando conversión a Word (Pandoc no disponible)"
        echo "📝 Archivo Markdown procesado disponible en: $TEMP_MD"
    fi
}

# Función para generar estadísticas
generate_stats() {
    echo "📊 Generando estadísticas del documento..."
    
    if [[ -f "$INPUT_FILE" ]]; then
        local word_count=$(wc -w < "$INPUT_FILE" 2>/dev/null || echo "N/A")
        local line_count=$(wc -l < "$INPUT_FILE" 2>/dev/null || echo "N/A")
        local diagram_count=$(grep -c "```mermaid" "$INPUT_FILE" 2>/dev/null || echo "0")
        local png_count=$(find "$DIAGRAMS_DIR" -name "*.png" 2>/dev/null | wc -l || echo "0")
        
        echo "📈 Estadísticas del documento:"
        echo "   📝 Palabras: $word_count"
        echo "   📄 Líneas: $line_count"
        echo "   🎨 Diagramas Mermaid: $diagram_count"
        echo "   🖼️  Imágenes PNG disponibles: $png_count"
    fi
    
    if [[ -f "$OUTPUT_FILE" ]]; then
        local file_size=$(ls -lh "$OUTPUT_FILE" 2>/dev/null | awk '{print $5}' || echo "N/A")
        echo "   📦 Tamaño archivo Word: $file_size"
    fi
}

# Función para mostrar instrucciones
show_instructions() {
    echo ""
    echo "🎉 ¡Proceso completado!"
    echo ""
    
    if [[ -f "$OUTPUT_FILE" ]]; then
        echo "📁 Archivo Word generado: $OUTPUT_FILE"
        echo "💡 Para abrir:"
        case "$OSTYPE" in
            darwin*)  echo "   open '$OUTPUT_FILE'" ;;
            linux*)   echo "   xdg-open '$OUTPUT_FILE'" ;;
            *)        echo "   Abrir manualmente: $OUTPUT_FILE" ;;
        esac
    fi
    
    if [[ -f "$TEMP_MD" ]]; then
        echo "📝 Markdown procesado: $TEMP_MD"
        echo "💡 Para ver el contenido procesado:"
        echo "   cat '$TEMP_MD'"
    fi
    
    echo ""
    echo "📋 Archivos generados:"
    if [[ -f "$OUTPUT_FILE" ]]; then
        echo "   ✅ $OUTPUT_FILE"
    fi
    if [[ -f "$TEMP_MD" ]]; then
        echo "   ✅ $TEMP_MD"
    fi
    if [[ -f "$TEMP_DIR/metadata.yaml" ]]; then
        echo "   ✅ $TEMP_DIR/metadata.yaml"
    fi
}

# Función principal
main() {
    echo "🎯 Conversión TFM URBIX a Word - Versión Corregida"
    echo "=================================================="
    echo ""
    
    # Verificar archivo de entrada
    if [[ ! -f "$INPUT_FILE" ]]; then
        echo "❌ Error: Archivo de entrada no encontrado: $INPUT_FILE"
        exit 1
    fi
    echo "✅ Archivo de entrada encontrado: $INPUT_FILE"
    
    # Ejecutar pasos
    check_dependencies
    process_markdown
    create_pandoc_config
    convert_to_word
    generate_stats
    show_instructions
    
    echo ""
    echo "🚀 ¡Proceso completado exitosamente!"
}

# Ejecutar función principal
main "$@"