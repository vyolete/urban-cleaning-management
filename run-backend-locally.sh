#!/bin/bash

# Script para ejecutar el backend localmente (fuera de Docker)
# Esto evita problemas de compilación de Docker y permite desarrollo más rápido

echo "======================================"
echo "Ejecutando Backend Localmente"
echo "======================================"
echo ""

# Set Java 21 (required for Lombok compatibility)
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: Java 21 no está instalado"
    echo "Por favor instala Java 21 (Amazon Corretto 21 recomendado)"
    exit 1
fi
echo "Using Java: $JAVA_HOME"
echo ""

# Verificar que PostgreSQL esté corriendo en Docker
echo "Verificando PostgreSQL..."
if ! docker ps | grep -q urbanclean-postgres; then
    echo "ERROR: PostgreSQL no está corriendo en Docker"
    echo "Ejecuta: docker-compose -f docker/docker-compose.yml up -d postgres"
    exit 1
fi

echo "PostgreSQL OK"
echo ""

# Configurar variables de entorno
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/urbanclean
export SPRING_DATASOURCE_USERNAME=urbanclean_user
export SPRING_DATASOURCE_PASSWORD=urbanclean_pass
export JWT_SECRET=your_jwt_secret_key_change_this_in_production_min_256_bits_long
export JWT_EXPIRATION=86400000
export UPLOAD_DIR=./uploads
export MAX_FILE_SIZE=5242880

echo "Variables de entorno configuradas"
echo ""

# Compilar y ejecutar
echo "Compilando backend..."
cd backend
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "ERROR: Falló la compilación"
    exit 1
fi

echo ""
echo "======================================"
echo "Iniciando aplicación..."
echo "======================================"
echo ""

java -jar target/*.jar
