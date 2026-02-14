#!/usr/bin/env python3
"""
Script to convert the URBIX analysis Markdown document to Word format
with proper formatting, tables, and academic structure.
"""

import os
import sys
from pathlib import Path

def convert_markdown_to_docx():
    """Convert the Markdown document to DOCX format using pandoc"""
    
    input_file = "URBIX_Analisis_Metodologico_Complementario_Capitulo_4.md"
    output_file = "URBIX_Analisis_Metodologico_Complementario_Capitulo_4.docx"
    
    # Check if input file exists
    if not os.path.exists(input_file):
        print(f"Error: Input file {input_file} not found")
        return False
    
    # Pandoc command with academic formatting options
    pandoc_cmd = f"""pandoc "{input_file}" \
        -o "{output_file}" \
        --from markdown \
        --to docx \
        --toc \
        --toc-depth=3 \
        --number-sections \
        --highlight-style=tango \
        --reference-doc=reference.docx \
        --metadata title="Análisis Integral del Desarrollo del Proyecto URBIX" \
        --metadata subtitle="Complemento Metodológico al Capítulo 4" \
        --metadata author="Kiro AI Development Team" \
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

def create_reference_docx():
    """Create a reference document for better formatting"""
    
    reference_content = """
    # Reference Document for Academic Formatting
    
    This document serves as a reference for academic formatting.
    
    ## Table Example
    
    | Column 1 | Column 2 | Column 3 |
    |----------|----------|----------|
    | Data 1   | Data 2   | Data 3   |
    
    ## Code Example
    
    ```java
    public class Example {
        public void method() {
            System.out.println("Example");
        }
    }
    ```
    """
    
    with open("reference_template.md", "w", encoding="utf-8") as f:
        f.write(reference_content)
    
    # Create basic reference docx
    os.system("pandoc reference_template.md -o reference.docx")
    os.remove("reference_template.md")

def main():
    """Main function"""
    print("🔄 Converting URBIX Analysis to Word format...")
    
    # Check if pandoc is available
    if os.system("pandoc --version > /dev/null 2>&1") != 0:
        print("❌ Pandoc is not installed. Please install pandoc first:")
        print("   macOS: brew install pandoc")
        print("   Ubuntu: sudo apt-get install pandoc")
        print("   Windows: Download from https://pandoc.org/installing.html")
        return False
    
    # Create reference document for better formatting
    create_reference_docx()
    
    # Convert the document
    success = convert_markdown_to_docx()
    
    # Clean up
    if os.path.exists("reference.docx"):
        os.remove("reference.docx")
    
    if success:
        print("✅ Document conversion completed successfully!")
        print("📄 Output file: URBIX_Analisis_Metodologico_Complementario_Capitulo_4.docx")
    else:
        print("❌ Document conversion failed")
    
    return success

if __name__ == "__main__":
    main()