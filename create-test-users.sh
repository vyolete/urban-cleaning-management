#!/bin/bash

# Script para crear usuarios de prueba
# Urban Cleaning Management System

set -e

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Creación de Usuarios de Prueba${NC}"
echo -e "${BLUE}  Urban Cleaning Management System${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -d "docker" ]; then
    echo -e "${RED}Error: Debes ejecutar este script desde el directorio raíz del proyecto${NC}"
    exit 1
fi

cd docker

# Verificar que PostgreSQL está corriendo
echo "Verificando que PostgreSQL está corriendo..."
if ! docker-compose exec -T postgres pg_isready -U urbanclean_user -d urbanclean > /dev/null 2>&1; then
    echo -e "${RED}Error: PostgreSQL no está corriendo${NC}"
    echo "Inicia los servicios con: docker-compose up -d"
    exit 1
fi

echo -e "${GREEN}✓ PostgreSQL está corriendo${NC}"
echo ""

# Crear usuarios
echo "Creando usuarios de prueba..."
echo ""

# Usuario Ciudadano
echo -n "Creando usuario CIUDADANO... "
docker-compose exec -T postgres psql -U urbanclean_user -d urbanclean > /dev/null 2>&1 << EOF
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'ciudadano',
    '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ciudadano@test.com',
    'ROLE_CIUDADANO',
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
fi

# Usuario Técnico
echo -n "Creando usuario TECNICO... "
docker-compose exec -T postgres psql -U urbanclean_user -d urbanclean > /dev/null 2>&1 << EOF
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'tecnico',
    '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'tecnico@test.com',
    'ROLE_TECNICO',
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
fi

# Usuario Admin
echo -n "Creando usuario ADMIN... "
docker-compose exec -T postgres psql -U urbanclean_user -d urbanclean > /dev/null 2>&1 << EOF
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin',
    '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@test.com',
    'ROLE_ADMIN',
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
fi

echo ""

# Verificar usuarios creados
echo "Usuarios creados:"
docker-compose exec -T postgres psql -U urbanclean_user -d urbanclean -c "SELECT username, email, role FROM users ORDER BY role;" 2>/dev/null

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✓ Usuarios de prueba creados exitosamente${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Credenciales de acceso:"
echo ""
echo -e "${YELLOW}Usuario Ciudadano:${NC}"
echo "  Username: ciudadano"
echo "  Password: admin123"
echo "  Rol: ROLE_CIUDADANO"
echo ""
echo -e "${YELLOW}Usuario Técnico:${NC}"
echo "  Username: tecnico"
echo "  Password: admin123"
echo "  Rol: ROLE_TECNICO"
echo ""
echo -e "${YELLOW}Usuario Administrador:${NC}"
echo "  Username: admin"
echo "  Password: admin123"
echo "  Rol: ROLE_ADMIN"
echo ""
echo "Ahora puedes hacer login en: http://localhost:3000/login"
echo ""
