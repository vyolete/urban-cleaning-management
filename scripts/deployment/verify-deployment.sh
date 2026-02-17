#!/bin/bash

# Script de verificación de despliegue Docker
# Urban Cleaning Management System

set -e

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Verificación de Despliegue Docker${NC}"
echo -e "${BLUE}  Urban Cleaning Management System${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Cambiar al directorio docker
cd docker

# Verificar que existe docker-compose.yml
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}✗ No se encontró docker-compose.yml${NC}"
    exit 1
fi

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  1. CONSTRUIR IMÁGENES${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

echo "Construyendo imágenes Docker..."
if docker-compose build --no-cache; then
    echo -e "${GREEN}✓ Imágenes construidas exitosamente${NC}"
else
    echo -e "${RED}✗ Error al construir imágenes${NC}"
    exit 1
fi
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  2. INICIAR SERVICIOS${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

echo "Iniciando servicios..."
if docker-compose up -d; then
    echo -e "${GREEN}✓ Servicios iniciados${NC}"
else
    echo -e "${RED}✗ Error al iniciar servicios${NC}"
    exit 1
fi
echo ""

echo "Esperando a que los servicios estén listos..."
sleep 10
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  3. VERIFICAR CONTENEDORES${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Verificar que los contenedores están corriendo
CONTAINERS=$(docker-compose ps -q)
if [ -z "$CONTAINERS" ]; then
    echo -e "${RED}✗ No hay contenedores corriendo${NC}"
    exit 1
fi

echo "Estado de contenedores:"
docker-compose ps
echo ""

# Verificar cada servicio
echo "Verificando servicios individuales..."
echo ""

# PostgreSQL
echo -n "PostgreSQL: "
if docker-compose exec -T postgres pg_isready -U urbanclean_user -d urbanclean > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Corriendo${NC}"
else
    echo -e "${RED}✗ No responde${NC}"
fi

# Backend
echo -n "Backend: "
BACKEND_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
if [ "$BACKEND_HEALTH" = "200" ]; then
    echo -e "${GREEN}✓ Corriendo (HTTP $BACKEND_HEALTH)${NC}"
else
    echo -e "${RED}✗ No responde (HTTP $BACKEND_HEALTH)${NC}"
fi

# Frontend
echo -n "Frontend: "
FRONTEND_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/health)
if [ "$FRONTEND_HEALTH" = "200" ]; then
    echo -e "${GREEN}✓ Corriendo (HTTP $FRONTEND_HEALTH)${NC}"
else
    echo -e "${RED}✗ No responde (HTTP $FRONTEND_HEALTH)${NC}"
fi
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  4. VERIFICAR BASE DE DATOS${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Verificar PostGIS
echo "Verificando extensión PostGIS..."
POSTGIS_VERSION=$(docker-compose exec -T postgres psql -U urbanclean_user -d urbanclean -t -c "SELECT PostGIS_Version();" 2>/dev/null | tr -d ' ')
if [ -n "$POSTGIS_VERSION" ]; then
    echo -e "${GREEN}✓ PostGIS instalado: $POSTGIS_VERSION${NC}"
else
    echo -e "${RED}✗ PostGIS no encontrado${NC}"
fi

# Verificar UUID extension
echo "Verificando extensión UUID..."
UUID_TEST=$(docker-compose exec -T postgres psql -U urbanclean_user -d urbanclean -t -c "SELECT uuid_generate_v4();" 2>/dev/null)
if [ -n "$UUID_TEST" ]; then
    echo -e "${GREEN}✓ UUID extension instalada${NC}"
else
    echo -e "${RED}✗ UUID extension no encontrada${NC}"
fi
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  5. VERIFICAR CONECTIVIDAD${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Verificar que backend puede conectarse a postgres
echo "Verificando conectividad backend → postgres..."
if docker-compose exec -T backend ping -c 1 postgres > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Backend puede alcanzar PostgreSQL${NC}"
else
    echo -e "${RED}✗ Backend no puede alcanzar PostgreSQL${NC}"
fi

# Verificar que frontend puede alcanzar backend
echo "Verificando conectividad frontend → backend..."
if docker-compose exec -T frontend ping -c 1 backend > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Frontend puede alcanzar Backend${NC}"
else
    echo -e "${RED}✗ Frontend no puede alcanzar Backend${NC}"
fi
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  6. VERIFICAR VOLÚMENES${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

echo "Volúmenes creados:"
docker volume ls | grep "docker_"
echo ""

# Verificar que los volúmenes existen
if docker volume inspect docker_postgres_data > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Volumen postgres_data existe${NC}"
else
    echo -e "${RED}✗ Volumen postgres_data no existe${NC}"
fi

if docker volume inspect docker_backend_uploads > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Volumen backend_uploads existe${NC}"
else
    echo -e "${RED}✗ Volumen backend_uploads no existe${NC}"
fi
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  7. VERIFICAR LOGS${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

echo "Últimas líneas de logs de cada servicio:"
echo ""

echo -e "${BLUE}--- PostgreSQL ---${NC}"
docker-compose logs --tail=5 postgres
echo ""

echo -e "${BLUE}--- Backend ---${NC}"
docker-compose logs --tail=5 backend
echo ""

echo -e "${BLUE}--- Frontend ---${NC}"
docker-compose logs --tail=5 frontend
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  8. PRUEBA DE API${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Probar endpoint de login
echo "Probando endpoint de login..."
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}')
LOGIN_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)

if [ "$LOGIN_CODE" = "200" ] || [ "$LOGIN_CODE" = "401" ]; then
    echo -e "${GREEN}✓ API responde correctamente (HTTP $LOGIN_CODE)${NC}"
else
    echo -e "${RED}✗ API no responde correctamente (HTTP $LOGIN_CODE)${NC}"
fi
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  9. VERIFICAR ACCESO WEB${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Verificar que el frontend sirve contenido
echo "Verificando que el frontend sirve contenido..."
FRONTEND_CONTENT=$(curl -s http://localhost:3000/)
if echo "$FRONTEND_CONTENT" | grep -q "<!DOCTYPE html>"; then
    echo -e "${GREEN}✓ Frontend sirve HTML correctamente${NC}"
else
    echo -e "${RED}✗ Frontend no sirve HTML${NC}"
fi
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  RESUMEN${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "URLs de acceso:"
echo "  - Frontend: http://localhost:3000"
echo "  - Backend API: http://localhost:8080/api"
echo "  - Backend Health: http://localhost:8080/actuator/health"
echo "  - PostgreSQL: localhost:5432"
echo ""
echo "Comandos útiles:"
echo "  - Ver logs: docker-compose logs -f"
echo "  - Detener: docker-compose stop"
echo "  - Reiniciar: docker-compose restart"
echo "  - Eliminar: docker-compose down"
echo ""
echo -e "${GREEN}✓ Verificación de despliegue completada${NC}"
echo ""
