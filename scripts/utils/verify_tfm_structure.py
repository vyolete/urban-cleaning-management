#!/usr/bin/env python3
"""
Script para verificar que la estructura TFM está completa y correcta
"""

import os
import sys
from pathlib import Path

def check_directory_structure():
    """Verificar que la estructura de directorios esté completa"""
    
    required_dirs = [
        "src/backend",
        "src/frontend", 
        "src/docker",
        "docs/tfm",
        "docs/architecture",
        "docs/security",
        "docs/testing",
        "docs/operations",
        "diagrams",
        "specs",
        "scripts/deployment",
        "scripts/testing",
        "scripts/utils",
        "deliverables"
    ]
    
    print("📁 Verificando estructura de directorios...")
    missing_dirs = []
    
    for directory in required_dirs:
        if os.path.exists(directory):
            print(f"   ✅ {directory}")
        else:
            print(f"   ❌ {directory} - FALTANTE")
            missing_dirs.append(directory)
    
    return len(missing_dirs) == 0

def check_tfm_documents():
    """Verificar que los documentos TFM estén presentes"""
    
    required_docs = [
        "docs/tfm/capitulo-gestion-proyecto.md",
        "docs/tfm/capitulo-gestion-proyecto.docx",
        "README.md",
        "QUICK_START.md"
    ]
    
    print("\n📚 Verificando documentos TFM...")
    missing_docs = []
    
    for doc in required_docs:
        if os.path.exists(doc):
            size = os.path.getsize(doc)
            print(f"   ✅ {doc} ({size:,} bytes)")
        else:
            print(f"   ❌ {doc} - FALTANTE")
            missing_docs.append(doc)
    
    return len(missing_docs) == 0

def check_source_code():
    """Verificar que el código fuente esté presente"""
    
    required_files = [
        "src/backend/pom.xml",
        "src/backend/src/main/java",
        "src/frontend/package.json", 
        "src/frontend/src",
        "src/docker/docker-compose.yml"
    ]
    
    print("\n💻 Verificando código fuente...")
    missing_files = []
    
    for file_path in required_files:
        if os.path.exists(file_path):
            if os.path.isfile(file_path):
                size = os.path.getsize(file_path)
                print(f"   ✅ {file_path} ({size:,} bytes)")
            else:
                print(f"   ✅ {file_path} (directorio)")
        else:
            print(f"   ❌ {file_path} - FALTANTE")
            missing_files.append(file_path)
    
    return len(missing_files) == 0

def check_specifications():
    """Verificar que las especificaciones estén presentes"""
    
    spec_dirs = [
        "specs/urban-cleaning-management",
        "specs/operational-excellence",
        "specs/critical-security-feedback",
        "specs/architecture-documentation"
    ]
    
    print("\n📋 Verificando especificaciones...")
    missing_specs = []
    
    for spec_dir in spec_dirs:
        if os.path.exists(spec_dir):
            # Verificar archivos dentro de cada spec
            req_file = f"{spec_dir}/requirements.md"
            design_file = f"{spec_dir}/design.md"
            tasks_file = f"{spec_dir}/tasks.md"
            
            files_present = 0
            if os.path.exists(req_file): files_present += 1
            if os.path.exists(design_file): files_present += 1
            if os.path.exists(tasks_file): files_present += 1
            
            print(f"   ✅ {spec_dir} ({files_present}/3 archivos)")
        else:
            print(f"   ❌ {spec_dir} - FALTANTE")
            missing_specs.append(spec_dir)
    
    return len(missing_specs) == 0

def check_diagrams():
    """Verificar que los diagramas estén presentes"""
    
    print("\n📊 Verificando diagramas...")
    
    if not os.path.exists("diagrams"):
        print("   ❌ Directorio diagrams/ faltante")
        return False
    
    # Contar archivos de diagramas
    diagram_files = []
    for root, dirs, files in os.walk("diagrams"):
        for file in files:
            if file.endswith(('.mmd', '.png', '.svg')):
                diagram_files.append(os.path.join(root, file))
    
    print(f"   ✅ {len(diagram_files)} archivos de diagramas encontrados")
    
    # Verificar que hay diagramas de diferentes tipos
    types_found = set()
    for diagram in diagram_files:
        if 'use-case' in diagram: types_found.add('use-case')
        if 'sequence' in diagram: types_found.add('sequence')
        if 'class' in diagram: types_found.add('class')
        if 'deployment' in diagram: types_found.add('deployment')
    
    print(f"   ✅ Tipos de diagramas: {', '.join(types_found)}")
    
    return len(diagram_files) > 0

def check_scripts():
    """Verificar que los scripts estén organizados"""
    
    print("\n🔧 Verificando scripts...")
    
    script_dirs = ["scripts/deployment", "scripts/testing", "scripts/utils"]
    total_scripts = 0
    
    for script_dir in script_dirs:
        if os.path.exists(script_dir):
            scripts = [f for f in os.listdir(script_dir) if f.endswith(('.sh', '.py'))]
            total_scripts += len(scripts)
            print(f"   ✅ {script_dir} ({len(scripts)} scripts)")
        else:
            print(f"   ❌ {script_dir} - FALTANTE")
    
    return total_scripts > 0

def generate_summary():
    """Generar resumen de la verificación"""
    
    print("\n" + "="*60)
    print("📋 RESUMEN DE VERIFICACIÓN TFM")
    print("="*60)
    
    checks = [
        ("Estructura de directorios", check_directory_structure()),
        ("Documentos TFM", check_tfm_documents()),
        ("Código fuente", check_source_code()),
        ("Especificaciones", check_specifications()),
        ("Diagramas", check_diagrams()),
        ("Scripts", check_scripts())
    ]
    
    passed = 0
    total = len(checks)
    
    for check_name, result in checks:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{check_name:.<30} {status}")
        if result:
            passed += 1
    
    print("="*60)
    print(f"RESULTADO: {passed}/{total} verificaciones pasadas")
    
    if passed == total:
        print("🎉 ¡Estructura TFM COMPLETA y CORRECTA!")
        print("✅ El proyecto está listo para entrega académica")
        return True
    else:
        print("⚠️  Estructura TFM INCOMPLETA")
        print("❌ Revisar elementos faltantes antes de entrega")
        return False

def main():
    """Función principal"""
    
    print("🎓 VERIFICACIÓN DE ESTRUCTURA TFM - URBIX")
    print("="*60)
    
    # Verificar que estamos en el directorio correcto
    if not os.path.exists("README.md") or not os.path.exists("src"):
        print("❌ Error: Ejecutar desde el directorio raíz del proyecto")
        return False
    
    # Ejecutar verificaciones
    result = generate_summary()
    
    if result:
        print("\n🚀 Próximos pasos:")
        print("   1. Revisar documentos en docs/tfm/")
        print("   2. Probar sistema con QUICK_START.md")
        print("   3. Preparar entregables finales")
        print("   4. Commit de la estructura organizada")
    else:
        print("\n🔧 Acciones requeridas:")
        print("   1. Completar elementos faltantes")
        print("   2. Re-ejecutar verificación")
        print("   3. Contactar soporte si persisten problemas")
    
    return result

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)