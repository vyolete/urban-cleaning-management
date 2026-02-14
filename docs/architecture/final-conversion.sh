#!/bin/bash

# Script final de conversión con mapper mejorado de diagramas
# Mapea correctamente cada diagrama según su contexto específico

set -e

echo "🚀 Conversión Final TFM con Mapper Mejorado"
echo "============================================"

# Variables
DOCS_DIR="docs/architecture"
DIAGRAMS_DIR="diagrams"
OUTPUT_DIR="docs/architecture/output"
TEMP_DIR="docs/architecture/temp"

INPUT_FILE="$DOCS_DIR/tfm-capitulo-arquitectura.md"
OUTPUT_FILE="$OUTPUT_DIR/TFM-Capitulo-Arquitectura-URBIX-Final.docx"
TEMP_MD="$TEMP_DIR/tfm-with-improved-images.md"
IMPROVED_MAPPER="$DOCS_DIR/improved-diagram-mapper.py"

# Limpiar y crear directorios
rm -rf "$TEMP_DIR"
mkdir -p "$OUTPUT_DIR"
mkdir -p "$TEMP_DIR"

echo "📁 Directorios preparados"

# Verificar archivos
if [[ ! -f "$INPUT_FILE" ]]; then
    echo "❌ Error: Archivo TFM no encontrado: $INPUT_FILE"
    exit 1
fi

if [[ ! -f "$IMPROVED_MAPPER" ]]; then
    echo "❌ Error: Mapper mejorado no encontrado: $IMPROVED_MAPPER"
    exit 1
fi

echo "✅ Archivos verificados"

# Verificar dependencias
echo "🔍 Verificando dependencias..."

if command -v python3 &> /dev/null; then
    echo "✅ Python3: $(python3 --version)"
else
    echo "❌ Python3 no encontrado"
    exit 1
fi

if command -v pandoc &> /dev/null; then
    echo "✅ Pandoc: $(pandoc --version | head -1)"
    PANDOC_OK=true
else
    echo "⚠️  Pandoc no encontrado - solo se procesará Markdown"
    PANDOC_OK=false
fi

# Procesar con mapper mejorado
echo ""
echo "📝 Procesando diagramas con mapper mejorado..."
echo "=============================================="

python3 "$IMPROVED_MAPPER" "$INPUT_FILE" "$TEMP_MD"

echo ""
echo "✅ Procesamiento de diagramas completado"

# Crear configuración de Pandoc
if [[ "$PANDOC_OK" == "true" ]]; then
    echo ""
    echo "⚙️  Creando configuración de Pandoc..."
    
    cat > "$TEMP_DIR/metadata.yaml" << 'METADATA_END'
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
METADATA_END

    echo "✅ Configuración creada"
    
    # Convertir a Word
    echo ""
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
fi

# Generar estadísticas
echo ""
echo "📊 Estadísticas del documento:"
echo "=============================="

if [[ -f "$INPUT_FILE" ]]; then
    WORDS=$(wc -w < "$INPUT_FILE" 2>/dev/null || echo "N/A")
    LINES=$(wc -l < "$INPUT_FILE" 2>/dev/null || echo "N/A")
    DIAGRAMS=$(grep -c "```mermaid" "$INPUT_FILE" 2>/dev/null || echo "0")
    PNG_COUNT=$(find "$DIAGRAMS_DIR" -name "*.png" 2>/dev/null | wc -l || echo "0")
    
    echo "📝 Palabras: $WORDS"
    echo "📄 Líneas: $LINES"
    echo "🎨 Diagramas Mermaid: $DIAGRAMS"
    echo "🖼️  Imágenes PNG disponibles: $PNG_COUNT"
fi

# Mostrar resultados
echo ""
echo "🎉 ¡Conversión Final Completada!"
echo "================================"

if [[ -f "$OUTPUT_FILE" ]]; then
    FILE_SIZE=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    echo "📁 Archivo Word Final: $OUTPUT_FILE"
    echo "📦 Tamaño: $FILE_SIZE"
    echo ""
    echo "💡 Para abrir el documento:"
    echo "   open '$OUTPUT_FILE'"
fi

if [[ -f "$TEMP_MD" ]]; then
    echo ""
    echo "📝 Markdown procesado: $TEMP_MD"
    echo "💡 Para revisar el contenido procesado:"
    echo "   cat '$TEMP_MD' | head -50"
fi

echo ""
echo "✅ ¡Proceso completado con éxito!"
echo "🎯 El documento Word ahora tiene los diagramas correctamente mapeados"