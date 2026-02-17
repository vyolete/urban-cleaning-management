# Instalación de Dependencias para Conversión TFM

## 🚀 Guía Rápida de Instalación

### 1. Instalar Pandoc (Requerido para Word)

**macOS:**
```bash
# Usando Homebrew (recomendado)
brew install pandoc

# O usando MacPorts
sudo port install pandoc
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install pandoc
```

**Windows:**
```bash
# Usando Chocolatey
choco install pandoc

# O descargar desde: https://pandoc.org/installing.html
```

**Verificar instalación:**
```bash
pandoc --version
```

### 2. Instalar Mermaid CLI (Opcional - para generar diagramas)

**Cualquier sistema con Node.js:**
```bash
# Instalar Node.js primero si no está instalado
# macOS: brew install node
# Ubuntu: sudo apt-get install nodejs npm
# Windows: choco install nodejs

# Instalar Mermaid CLI globalmente
npm install -g @mermaid-js/mermaid-cli

# Verificar instalación
mmdc --version
```

### 3. Python 3 (Ya disponible en tu sistema)

Tu sistema ya tiene Python 3.9.6 instalado ✅

## 🔧 Solución de Problemas

### Error: "pandoc: command not found"

1. **Verificar PATH:**
   ```bash
   echo $PATH
   which pandoc
   ```

2. **Reinstalar Pandoc:**
   ```bash
   # macOS
   brew uninstall pandoc
   brew install pandoc
   
   # Ubuntu
   sudo apt-get remove pandoc
   sudo apt-get install pandoc
   ```

3. **Instalación manual (macOS):**
   ```bash
   # Descargar desde GitHub
   curl -LO https://github.com/jgm/pandoc/releases/download/3.1.9/pandoc-3.1.9-macOS.pkg
   sudo installer -pkg pandoc-3.1.9-macOS.pkg -target /
   ```

### Error: "mmdc: command not found"

1. **Verificar Node.js:**
   ```bash
   node --version
   npm --version
   ```

2. **Instalar Node.js si falta:**
   ```bash
   # macOS
   brew install node
   
   # Ubuntu
   curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
   sudo apt-get install -y nodejs
   ```

3. **Reinstalar Mermaid CLI:**
   ```bash
   npm uninstall -g @mermaid-js/mermaid-cli
   npm install -g @mermaid-js/mermaid-cli
   ```

## 🎯 Instalación Completa (Un Solo Comando)

**macOS (con Homebrew):**
```bash
# Instalar todas las dependencias
brew install pandoc node

# Instalar Mermaid CLI
npm install -g @mermaid-js/mermaid-cli

# Verificar instalaciones
pandoc --version && mmdc --version && python3 --version
```

**Ubuntu:**
```bash
# Actualizar sistema
sudo apt-get update

# Instalar dependencias
sudo apt-get install -y pandoc nodejs npm

# Instalar Mermaid CLI
sudo npm install -g @mermaid-js/mermaid-cli

# Verificar instalaciones
pandoc --version && mmdc --version && python3 --version
```

## ✅ Verificación Final

Ejecuta este comando para verificar que todo está instalado:

```bash
echo "🔍 Verificando dependencias..."
echo "Python: $(python3 --version 2>&1)"
echo "Pandoc: $(pandoc --version 2>&1 | head -1)"
echo "Mermaid: $(mmdc --version 2>&1 || echo 'No instalado (opcional)')"
echo "✅ Verificación completada"
```

## 🚀 Ejecutar Conversión

Una vez instaladas las dependencias:

```bash
# Hacer ejecutable el script corregido
chmod +x docs/architecture/fix-and-run-conversion.sh

# Ejecutar conversión
./docs/architecture/fix-and-run-conversion.sh
```

## 📋 Dependencias Mínimas vs Completas

### Mínimas (solo procesamiento de Markdown):
- ✅ Python 3 (ya instalado)
- ✅ Archivos PNG de diagramas (ya disponibles)

### Completas (conversión a Word):
- ✅ Python 3
- 📦 Pandoc (para generar Word)
- 🎨 Mermaid CLI (para generar diagramas nuevos)

**Nota:** El script funciona incluso sin Pandoc - generará el Markdown procesado que puedes convertir manualmente o usar en otros editores.