#!/bin/bash

# Script de pruebas automatizadas de API endpoints
# Urban Cleaning Management System

set -e

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración
API_URL="${API_URL:-http://localhost:8080/api}"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables globales para tokens
ADMIN_TOKEN=""
TECNICO_TOKEN=""
CIUDADANO_TOKEN=""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Urban Cleaning Management System - API Tests${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "API URL: $API_URL"
echo ""

# Función para ejecutar test
run_test() {
    local test_name="$1"
    local expected_code="$2"
    local actual_code="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$actual_code" = "$expected_code" ]; then
        echo -e "${GREEN}✓${NC} $test_name (HTTP $actual_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}✗${NC} $test_name (Expected: $expected_code, Got: $actual_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Función para extraer token de respuesta JSON
extract_token() {
    echo "$1" | grep -o '"token":"[^"]*' | cut -d'"' -f4
}

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  1. AUTENTICACIÓN${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Test 1.1: Login con credenciales inválidas
echo "Test 1.1: Login con credenciales inválidas"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"invalid","password":"wrong"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
run_test "Login con credenciales inválidas debe retornar 401" "401" "$HTTP_CODE"
echo ""

# Test 1.2: Login exitoso como admin
echo "Test 1.2: Login exitoso como admin"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
run_test "Login exitoso debe retornar 200" "200" "$HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
    ADMIN_TOKEN=$(extract_token "$BODY")
    if [ -n "$ADMIN_TOKEN" ]; then
        echo -e "${GREEN}  → Token admin obtenido${NC}"
    else
        echo -e "${RED}  → No se pudo extraer token${NC}"
    fi
fi
echo ""

# Test 1.3: Login exitoso como técnico
echo "Test 1.3: Login exitoso como técnico"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"tecnico","password":"admin123"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
run_test "Login técnico debe retornar 200" "200" "$HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
    TECNICO_TOKEN=$(extract_token "$BODY")
    if [ -n "$TECNICO_TOKEN" ]; then
        echo -e "${GREEN}  → Token técnico obtenido${NC}"
    fi
fi
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  2. ENDPOINTS PROTEGIDOS${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Test 2.1: Acceso sin token
echo "Test 2.1: Acceso a /tasks sin token"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/tasks")
run_test "Acceso sin token debe retornar 401 o 403" "401" "$HTTP_CODE" || \
run_test "Acceso sin token debe retornar 401 o 403" "403" "$HTTP_CODE"
echo ""

# Test 2.2: Acceso con token válido
if [ -n "$TECNICO_TOKEN" ]; then
    echo "Test 2.2: Acceso a /tasks con token válido"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/tasks" \
        -H "Authorization: Bearer $TECNICO_TOKEN")
    run_test "Acceso con token válido debe retornar 200" "200" "$HTTP_CODE"
    echo ""
fi

# Test 2.3: Acceso a endpoint de admin sin rol
if [ -n "$TECNICO_TOKEN" ]; then
    echo "Test 2.3: Técnico intenta acceder a endpoint de admin"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/admin/config/algorithm-weights" \
        -H "Authorization: Bearer $TECNICO_TOKEN")
    run_test "Técnico en endpoint admin debe retornar 403" "403" "$HTTP_CODE"
    echo ""
fi

# Test 2.4: Admin accede a endpoint de admin
if [ -n "$ADMIN_TOKEN" ]; then
    echo "Test 2.4: Admin accede a endpoint de admin"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/admin/config/algorithm-weights" \
        -H "Authorization: Bearer $ADMIN_TOKEN")
    run_test "Admin en endpoint admin debe retornar 200" "200" "$HTTP_CODE"
    echo ""
fi

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  3. GESTIÓN DE TAREAS${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

if [ -n "$TECNICO_TOKEN" ]; then
    # Test 3.1: Listar tareas
    echo "Test 3.1: Listar todas las tareas"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/tasks" \
        -H "Authorization: Bearer $TECNICO_TOKEN")
    run_test "Listar tareas debe retornar 200" "200" "$HTTP_CODE"
    echo ""
    
    # Test 3.2: Filtrar tareas por estado
    echo "Test 3.2: Filtrar tareas por estado PENDIENTE"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/tasks?state=PENDIENTE" \
        -H "Authorization: Bearer $TECNICO_TOKEN")
    run_test "Filtrar por estado debe retornar 200" "200" "$HTTP_CODE"
    echo ""
fi

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  4. CONFIGURACIÓN (ADMIN)${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

if [ -n "$ADMIN_TOKEN" ]; then
    # Test 4.1: Obtener configuración actual
    echo "Test 4.1: Obtener configuración actual"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/admin/config/algorithm-weights" \
        -H "Authorization: Bearer $ADMIN_TOKEN")
    run_test "Obtener configuración debe retornar 200" "200" "$HTTP_CODE"
    echo ""
    
    # Test 4.2: Actualizar configuración con valores válidos
    echo "Test 4.2: Actualizar configuración con valores válidos"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$API_URL/admin/config/algorithm-weights" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "weightCategory": 0.40,
            "weightZone": 0.35,
            "weightTime": 0.25,
            "deduplicationDistanceMeters": 50.0,
            "deduplicationTimeWindowHours": 24
        }')
    run_test "Actualizar configuración debe retornar 200" "200" "$HTTP_CODE"
    echo ""
    
    # Test 4.3: Actualizar configuración con valores inválidos (suma != 1.0)
    echo "Test 4.3: Actualizar configuración con suma inválida"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$API_URL/admin/config/algorithm-weights" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "weightCategory": 0.50,
            "weightZone": 0.50,
            "weightTime": 0.50,
            "deduplicationDistanceMeters": 50.0,
            "deduplicationTimeWindowHours": 24
        }')
    run_test "Configuración inválida debe retornar 400" "400" "$HTTP_CODE"
    echo ""
    
    # Test 4.4: Obtener historial de configuraciones
    echo "Test 4.4: Obtener historial de configuraciones"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/admin/config/algorithm-weights/history" \
        -H "Authorization: Bearer $ADMIN_TOKEN")
    run_test "Obtener historial debe retornar 200" "200" "$HTTP_CODE"
    echo ""
fi

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  5. VALIDACIONES${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Test 5.1: Request con datos faltantes
echo "Test 5.1: Login sin password"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin"}')
run_test "Login sin password debe retornar 400" "400" "$HTTP_CODE"
echo ""

# Test 5.2: Request con JSON malformado
echo "Test 5.2: Request con JSON malformado"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{invalid json}')
run_test "JSON malformado debe retornar 400" "400" "$HTTP_CODE"
echo ""

echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  6. HEALTH CHECKS${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# Test 6.1: Health check endpoint
echo "Test 6.1: Health check endpoint"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/health")
run_test "Health check debe retornar 200" "200" "$HTTP_CODE"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  RESUMEN DE PRUEBAS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Total de pruebas: $TOTAL_TESTS"
echo -e "${GREEN}Pruebas exitosas: $PASSED_TESTS${NC}"
echo -e "${RED}Pruebas fallidas: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ Todas las pruebas pasaron exitosamente${NC}"
    exit 0
else
    echo -e "${RED}✗ Algunas pruebas fallaron${NC}"
    exit 1
fi
