# Conversión del TFM a Word

Este directorio contiene scripts para convertir el capítulo de arquitectura del TFM desde Markdown a Word (.docx) con todos los diagramas incluidos.

## 📋 Requisitos

### Dependencias Obligatorias

1. **Pandoc** - Conversor de documentos
   ```bash
   # macOS
   brew install pandoc
   
   # Ubuntu/Debian
   sudo apt-get install pandoc
   
   # Windows
   choco install pandoc
   ```

2. **Python 3** - Para el procesamiento de diagramas
   ```bash
   # Verificar instalación
   python3 --version
   ```

### Dependencias Opcionales

3. **Mermaid CLI** - Para generar diagramas PNG desde archivos .mmd
   ```bash
   npm install -g @mermaid-js/mermaid-cli
   ```

## 🚀 Uso Rápido

### Conversión Completa (Recomendado)

```bash
# Hacer ejecutable el script
chmod +x docs/architecture/convert-to-word.sh

# Ejecutar conversión completa
./docs/architecture/convert-to-word.sh
```

### Pasos Individuales

1. **Generar diagramas faltantes** (opcional):
   ```bash
   chmod +x docs/architecture/generate-missing-diagrams.sh
   ./docs/architecture/generate-missing-diagrams.sh
   ```

2. **Procesar Markdown con mapper de diagramas**:
   ```bash
   python3 docs/architecture/diagram-mapper.py \
           docs/architecture/tfm-capitulo-arquitectura.md \
           docs/architecture/temp/tfm-with-images.md
   ```

3. **Convertir a Word**:
   ```bash
   pandoc --from markdown+smart \
          --to docx \
          --toc \
          --number-sections \
          --output docs/architecture/output/TFM-Capitulo-Arquitectura-URBIX.docx \
          docs/architecture/temp/tfm-with-images.md
   ```

## 📁 Estructura de Archivos

```
docs/architecture/
├── tfm-capitulo-arquitectura.md     # Documento fuente
├── convert-to-word.sh               # Script principal de conversión
├── diagram-mapper.py                # Mapper inteligente de diagramas
├── generate-missing-diagrams.sh     # Generador de diagramas PNG
├── README-conversion.md             # Este archivo
├── temp/                           # Archivos temporales (auto-generado)
└── output/                         # Archivo Word final
    └── TFM-Capitulo-Arquitectura-URBIX.docx
```

## 🎨 Mapeo de Diagramas

El script `diagram-mapper.py` mapea automáticamente los diagramas Mermaid a imágenes PNG específicas basándose en:

### Vista de Casos de Uso
- `use-case-complete-system-overview.png` - Vista general del sistema
- `use-case-authentication-flow.png` - Flujos de autenticación
- `use-case-report-task-management.png` - Gestión de reportes y tareas
- `use-case-user-profile-sessions.png` - Perfil de usuario y sesiones
- `use-case-admin-configuration.png` - Configuración administrativa

### Vista Lógica - Diagramas de Secuencia
- `sequence-login-authentication.png` - Autenticación de login
- `sequence-user-registration.png` - Registro de usuarios
- `sequence-report-submission.png` - Envío de reportes
- `sequence-task-assignment.png` - Asignación de tareas
- `sequence-task-state-update.png` - Actualización de estado
- Y más...

### Vista de Procesos
- `process-duplicate-detection.png` - Detección de duplicados
- `process-main-report-management.png` - Gestión de reportes
- `process-priority-calculation.png` - Cálculo de prioridad
- `process-email-notifications.png` - Notificaciones por email
- Y más...

### Vista de Implementación
- `implementation-frontend-component-structure.png` - Estructura frontend
- `implementation-backend-package-structure.png` - Estructura backend
- `mvc-general-architecture-flow.png` - Flujo arquitectónico general

### Vista de Despliegue
- `deployment-aws-complete-architecture.png` - Arquitectura AWS completa
- `deployment-database-configuration.png` - Configuración de base de datos
- `deployment-docker-containers.png` - Contenedores Docker

### Modelo de Datos
- `erd-complete-database-schema.png` - Esquema completo de base de datos

## ⚙️ Configuración Avanzada

### Personalizar Metadatos del Documento

Editar el archivo `metadata.yaml` generado automáticamente:

```yaml
title: "Capítulo 4: Arquitectura y Diseño del Sistema URBIX"
subtitle: "Trabajo de Fin de Máster en Ingeniería de Software"
author: "Tu Nombre"
date: "Febrero 2026"
lang: es-ES
```

### Personalizar Estilo de Diagramas

Modificar la configuración de Mermaid en `generate-missing-diagrams.sh`:

```json
{
  "theme": "neutral",
  "background": "white",
  "width": 1200,
  "height": 800,
  "scale": 2
}
```

### Opciones de Pandoc

El script usa estas opciones optimizadas:

```bash
pandoc \
  --from markdown+smart+table_captions+fenced_code_blocks \
  --to docx \
  --toc \
  --toc-depth=3 \
  --number-sections \
  --highlight-style=tango \
  --resource-path=".:diagrams:docs/architecture"
```

## 🔧 Solución de Problemas

### Error: "pandoc: command not found"
```bash
# Instalar Pandoc según tu sistema operativo
brew install pandoc  # macOS
sudo apt-get install pandoc  # Ubuntu
```

### Error: "mmdc: command not found"
```bash
# Instalar Mermaid CLI (opcional)
npm install -g @mermaid-js/mermaid-cli
```

### Diagramas no se muestran correctamente
1. Verificar que los archivos PNG existen en `diagrams/`
2. Ejecutar `generate-missing-diagrams.sh`
3. Verificar rutas en el mapper de diagramas

### Archivo Word muy pequeño
1. Verificar que el archivo Markdown fuente existe
2. Revisar logs de conversión para errores
3. Verificar que las imágenes se están incluyendo

### Problemas de codificación de caracteres
```bash
# Verificar codificación del archivo fuente
file -I docs/architecture/tfm-capitulo-arquitectura.md

# Debe mostrar: charset=utf-8
```

## 📊 Estadísticas Esperadas

Un documento TFM completo debería tener aproximadamente:

- **Palabras**: 15,000 - 25,000
- **Páginas**: 80 - 120 (formato académico)
- **Diagramas**: 50+ imágenes
- **Tamaño archivo**: 5 - 15 MB
- **Secciones**: 8 - 10 principales

## 🎯 Resultado Final

El archivo generado `TFM-Capitulo-Arquitectura-URBIX.docx` incluirá:

✅ **Formato académico profesional**
- Tabla de contenidos automática
- Numeración de secciones
- Estilo de fuente académico (Times New Roman)
- Márgenes apropiados (2.5cm)
- Interlineado 1.5

✅ **Contenido completo**
- Todo el texto del documento Markdown
- Todos los diagramas convertidos a imágenes
- Tablas formateadas correctamente
- Código con resaltado de sintaxis

✅ **Navegación**
- Enlaces internos funcionales
- Referencias cruzadas
- Índice clickeable

## 🚀 Comandos de Un Solo Paso

```bash
# Conversión completa con una sola línea
chmod +x docs/architecture/convert-to-word.sh && ./docs/architecture/convert-to-word.sh

# Abrir el resultado (macOS)
open docs/architecture/output/TFM-Capitulo-Arquitectura-URBIX.docx

# Abrir el resultado (Linux)
xdg-open docs/architecture/output/TFM-Capitulo-Arquitectura-URBIX.docx
```

## 📞 Soporte

Si encuentras problemas:

1. Verificar que todas las dependencias están instaladas
2. Revisar los logs de error del script
3. Verificar que los archivos fuente existen
4. Comprobar permisos de escritura en directorios de salida

¡El script está diseñado para ser robusto y manejar la mayoría de casos automáticamente!