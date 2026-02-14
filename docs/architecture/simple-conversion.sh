#!/bin/bash

# Script simple y robusto para convertir TFM a Word
# Sin heredocs complejos que causan problemas de sintaxis

set -e

echo "🚀 Conversión Simple TFM a Word"
echo "==============================="

# Variables
DOCS_DIR="docs/architecture"
DIAGRAMS_DIR="diagrams"
OUTPUT_DIR="docs/architecture/output"
TEMP_DIR="docs/architecture/temp"

INPUT_FILE="$DOCS_DIR/tfm-capitulo-arquitectura.md"
OUTPUT_FILE="$OUTPUT_DIR/TFM-Capitulo-Arquitectura-URBIX.docx"
TEMP_MD="$TEMP_DIR/tfm-with-images.md"
MAPPER_SCRIPT="$DOCS_DIR/diagram-mapper.py"

# Crear directorios
mkdir -p "$OUTPUT_DIR"
mkdir -p "$TEMP_DIR"

echo "📁 Directorios creados"

# Verificar archivo de entrada
if [[ ! -f "$INPUT_FILE" ]]; then
    echo "❌ Error: Archivo no encontrado: $INPUT_FILE"
    exit 1
fi

echo "✅ Archivo encontrado: $INPUT_FILE"

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

# Procesar Markdown con mapper
echo "📝 Procesando diagramas..."

if [[ -f "$MAPPER_SCRIPT" ]]; then
    python3 "$MAPPER_SCRIPT" "$INPUT_FILE" "$TEMP_MD"
    echo "✅ Diagramas procesados"
else
    echo "❌ Mapper no encontrado: $MAPPER_SCRIPT"
    exit 1
fi

# Crear metadatos para Pandoc
if [[ "$PANDOC_OK" == "true" ]]; then
    echo "⚙️  Creando configuración..."
    
    # Crear archivo de metadatos simple
    cat > "$TEMP_DIR/metadata.yaml" << 'METADATA_END'
---
title: "Capítulo 4: Arquitectura y Diseño del Sistema URBIX"
subtitle: "Trabajo de Fin de Máster en Ingeniería de Software"
author: "Estudiante TFM"
date: "Febrero 2026"
lang: es-ES
toc: true
toc-depth: 3
numbersections: true
---
METADATA_END

    echo "✅ Configuración creada"
    
    # Convertir a Word
    echo "📄 Convirtiendo a Word..."
    
    pandoc \
        --from markdown \
        --to docx \
        --metadata-file="$TEMP_DIR/metadata.yaml" \
        --toc \
        --number-sections \
        --resource-path=".:$DIAGRAMS_DIR:$DOCS_DIR" \
        --output="$OUTPUT_FILE" \
        "$TEMP_MD"
    
    echo "✅ Conversión completada"
fi

# Mostrar estadísticas
echo "📊 Estadísticas:"

if [[ -f "$INPUT_FILE" ]]; then
    WORDS=$(wc -w < "$INPUT_FILE" 2>/dev/null || echo "N/A")
    LINES=$(wc -l < "$INPUT_FILE" 2>/dev/null || echo "N/A")
    DIAGRAMS=$(grep -c "```mermaid" "$INPUT_FILE" 2>/dev/null || echo "0")
    
    echo "   📝 Palabras: $WORDS"
    echo "   📄 Líneas: $LINES"
    echo "   🎨 Diagramas: $DIAGRAMS"
fi

# Mostrar resultados
echo ""
echo "🎉 ¡Proceso completado!"

if [[ -f "$OUTPUT_FILE" ]]; then
    FILE_SIZE=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    echo "📁 Archivo Word: $OUTPUT_FILE ($FILE_SIZE)"
    echo "💡 Abrir con: open '$OUTPUT_FILE'"
fi

if [[ -f "$TEMP_MD" ]]; then
    echo "📝 Markdown procesado: $TEMP_MD"
fi

echo ""
echo "✅ ¡Conversión exitosa!"