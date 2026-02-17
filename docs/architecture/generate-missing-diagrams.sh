#!/bin/bash

# Script para generar diagramas PNG faltantes desde archivos Mermaid
# Útil para asegurar que todos los diagramas estén disponibles

set -e

DIAGRAMS_DIR="diagrams"
GENERATED_COUNT=0
FAILED_COUNT=0

echo "🎨 Generando diagramas PNG faltantes..."

# Verificar que mermaid-cli está instalado
if ! command -v mmdc &> /dev/null; then
    echo "❌ Error: mermaid-cli no está instalado"
    echo "Instalar con: npm install -g @mermaid-js/mermaid-cli"
    exit 1
fi

# Crear configuración de Mermaid para mejor calidad
cat > /tmp/mermaid-config.json << 'EOF'
{
  "theme": "neutral",
  "background": "white",
  "width": 1200,
  "height": 800,
  "scale": 2
}
EOF

echo "📁 Procesando archivos en: $DIAGRAMS_DIR"

# Procesar cada archivo .mmd
for mmd_file in "$DIAGRAMS_DIR"/*.mmd; do
    if [[ -f "$mmd_file" ]]; then
        filename=$(basename "$mmd_file" .mmd)
        png_file="$DIAGRAMS_DIR/${filename}.png"
        
        # Verificar si necesita generación
        if [[ ! -f "$png_file" ]] || [[ "$mmd_file" -nt "$png_file" ]]; then
            echo "  🔄 Generando: $filename.png"
            
            # Intentar generar el diagrama
            if mmdc -i "$mmd_file" -o "$png_file" \
                    -c /tmp/mermaid-config.json \
                    --quiet 2>/dev/null; then
                echo "    ✅ Generado exitosamente"
                ((GENERATED_COUNT++))
            else
                echo "    ❌ Error generando $filename.png"
                ((FAILED_COUNT++))
                
                # Intentar con configuración básica
                if mmdc -i "$mmd_file" -o "$png_file" \
                        -t neutral -b white \
                        --width 1200 --height 800 \
                        --quiet 2>/dev/null; then
                    echo "    ✅ Generado con configuración básica"
                    ((GENERATED_COUNT++))
                    ((FAILED_COUNT--))
                fi
            fi
        else
            echo "  ⏭️  Ya existe: $filename.png"
        fi
    fi
done

# Limpiar archivo temporal
rm -f /tmp/mermaid-config.json

echo ""
echo "📊 Resumen de generación:"
echo "   ✅ Generados exitosamente: $GENERATED_COUNT"
echo "   ❌ Fallos: $FAILED_COUNT"
echo "   📁 Total archivos PNG: $(find "$DIAGRAMS_DIR" -name "*.png" | wc -l)"

if [[ $FAILED_COUNT -gt 0 ]]; then
    echo ""
    echo "⚠️  Algunos diagramas no se pudieron generar."
    echo "   Verificar sintaxis de archivos .mmd con errores"
    exit 1
else
    echo ""
    echo "🎉 Todos los diagramas generados exitosamente!"
fi