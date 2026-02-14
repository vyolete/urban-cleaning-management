#!/usr/bin/env python3
"""
Script to convert the URBIX TFM Project Management Chapter to Word format
with proper academic formatting, tables, and structure.
"""

import os
import sys
from pathlib import Path

def convert_tfm_chapter_to_docx():
    """Convert the TFM Chapter Markdown document to DOCX format using pandoc"""
    
    input_file = "URBIX_Gestion_Proyecto_TFM_Capitulo.md"
    output_file = "URBIX_Gestion_Proyecto_TFM_Capitulo.docx"
    
    # Check if input file exists
    if not os.path.exists(input_file):
        print(f"Error: Input file {input_file} not found")
        return False
    
    # Pandoc command with academic formatting options (updated syntax)
    pandoc_cmd = f"""pandoc "{input_file}" \
        -o "{output_file}" \
        --from markdown \
        --to docx \
        --toc \
        --toc-depth=4 \
        --number-sections \
        --syntax-highlighting \
        --reference-doc=tfm_reference.docx \
        --metadata title="Capítulo 4: Gestión y Dirección del Proyecto URBIX" \
        --metadata subtitle="Sistema de Gestión de Limpieza Urbana" \
        --metadata author="Trabajo de Fin de Máster" \
        --metadata date="11 de febrero de 2026"
    """
    
    # Try to run pandoc
    try:
        result = os.system(pandoc_cmd)
        if result == 0:
            print(f"✅ Successfully converted {input_file} to {output_file}")
            return True
        else:
            print(f"❌ Pandoc conversion failed with exit code {result}")
            return False
    except Exception as e:
        print(f"❌ Error running pandoc: {e}")
        return False

def create_tfm_reference_docx():
    """Create a reference document for TFM academic formatting"""
    
    reference_content = """
# Capítulo de Trabajo de Fin de Máster

Este documento sirve como referencia para el formateo académico de TFM.

## Sección Principal

### Subsección

#### Subsubsección

Texto normal con **texto en negrita** y *texto en cursiva*.

## Ejemplo de Tabla

| Métrica | Valor | Evaluación |
|---------|-------|------------|
| Completitud | 100% | Excelente |
| Calidad | 9.3/10 | Superior |
| Cronograma | +9% | Aceptable |

## Ejemplo de Lista

1. Primer elemento
2. Segundo elemento
   - Subelemento A
   - Subelemento B
3. Tercer elemento

## Ejemplo de Código

```java
public class ProjectManagement {
    private String methodology;
    
    public void executePhase(String phase) {
        System.out.println("Executing: " + phase);
    }
}
```

## Cita Académica

> La gestión efectiva de proyectos requiere la aplicación sistemática de metodologías probadas y la adaptación continua a las necesidades específicas del contexto.

## Nota al Pie

Este es un ejemplo de texto con nota al pie[^1].

[^1]: Esta es una nota al pie de página académica.
    """
    
    with open("tfm_reference_template.md", "w", encoding="utf-8") as f:
        f.write(reference_content)
    
    # Create basic reference docx with academic styling
    reference_cmd = """pandoc tfm_reference_template.md \
        -o tfm_reference.docx \
        --from markdown \
        --to docx \
        --toc \
        --number-sections"""
    
    os.system(reference_cmd)
    os.remove("tfm_reference_template.md")

def check_pandoc_installation():
    """Check if pandoc is installed and provide installation instructions"""
    if os.system("pandoc --version > /dev/null 2>&1") != 0:
        print("❌ Pandoc is not installed. Please install pandoc first:")
        print("   macOS: brew install pandoc")
        print("   Ubuntu: sudo apt-get install pandoc")
        print("   Windows: Download from https://pandoc.org/installing.html")
        print("   Or use conda: conda install pandoc")
        return False
    return True

def main():
    """Main function"""
    print("🔄 Converting URBIX TFM Project Management Chapter to Word format...")
    
    # Check if pandoc is available
    if not check_pandoc_installation():
        return False
    
    # Create reference document for better academic formatting
    print("📝 Creating academic reference template...")
    create_tfm_reference_docx()
    
    # Convert the document
    print("🔄 Converting Markdown to DOCX...")
    success = convert_tfm_chapter_to_docx()
    
    # Clean up temporary files
    if os.path.exists("tfm_reference.docx"):
        os.remove("tfm_reference.docx")
    
    if success:
        print("✅ Document conversion completed successfully!")
        print("📄 Output file: URBIX_Gestion_Proyecto_TFM_Capitulo.docx")
        print("📋 Features included:")
        print("   - Table of Contents with 4 levels")
        print("   - Numbered sections")
        print("   - Academic formatting")
        print("   - Proper table formatting")
        print("   - Code syntax highlighting")
        print("   - Metadata (title, author, date)")
    else:
        print("❌ Document conversion failed")
        print("💡 Troubleshooting tips:")
        print("   - Ensure pandoc is properly installed")
        print("   - Check that the input file exists")
        print("   - Verify file permissions")
    
    return success

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)