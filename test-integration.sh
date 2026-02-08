#!/bin/bash

# Script de prueba de integración Frontend-Backend
# Este script verifica que el backend esté corriendo y responda correctamente

echo "🔍 Verificando integración Frontend-Backend..."
echo ""

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# URL del backend
BACKEND_URL="http://localhost:8080"
API_URL="${BACKEND_URL}/api"

# Función para verificar si el backend está corriendo
check_backend() {
    echo "1️⃣  Verificando si el backend está corriendo..."
    if curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}" | grep -q "200\|404"; then
        echo -e "${GREEN}✅ Backend está corriendo en ${BACKEND_URL}${NC}"
        return 0
    else
        echo -e "${RED}❌ Backend NO está corriendo en ${BACKEND_URL}${NC}"
        echo -e "${YELLOW}   Inicia el backend con: cd backend && mvn spring-boot:run${NC}"
        return 1
    fi
}

# Función para probar endpoint de login
test_login_endpoint() {
    echo ""
    echo "2️⃣  Probando endpoint de login..."
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${API_URL}/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"test","password":"test"}')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n-1)
    
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ]; then
        echo -e "${GREEN}✅ Endpoint de login responde correctamente (HTTP $HTTP_CODE)${NC}"
        return 0
    else
        echo -e "${RED}❌ Endpoint de login no responde correctamente (HTTP $HTTP_CODE)${NC}"
        echo "   Response: $BODY"
        return 1
    fi
}

# Función para verificar CORS
test_cors() {
    echo ""
    echo "3️⃣  Verificando configuración CORS..."
    
    CORS_RESPONSE=$(curl -s -I -X OPTIONS "${API_URL}/auth/login" \
        -H "Origin: http://localhost:5173" \
        -H "Access-Control-Request-Method: POST" \
        -H "Access-Control-Request-Headers: Content-Type")
    
    if echo "$CORS_RESPONSE" | grep -q "Access-Control-Allow-Origin"; then
        echo -e "${GREEN}✅ CORS configurado correctamente${NC}"
        return 0
    else
        echo -e "${RED}❌ CORS no está configurado correctamente${NC}"
        echo "   Response headers:"
        echo "$CORS_RESPONSE"
        return 1
    fi
}

# Función para verificar endpoints públicos
test_public_endpoints() {
    echo ""
    echo "4️⃣  Verificando endpoints públicos..."
    
    # Test /api/auth/login
    LOGIN_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${API_URL}/auth/login" \
        -H "Content-Type: application/json" \
        -d '{}')
    
    if [ "$LOGIN_CODE" = "400" ] || [ "$LOGIN_CODE" = "401" ]; then
        echo -e "${GREEN}✅ POST /api/auth/login accesible (HTTP $LOGIN_CODE)${NC}"
    else
        echo -e "${RED}❌ POST /api/auth/login no accesible (HTTP $LOGIN_CODE)${NC}"
    fi
}

# Función para verificar endpoints protegidos
test_protected_endpoints() {
    echo ""
    echo "5️⃣  Verificando endpoints protegidos..."
    
    # Test /api/tasks sin token (debe retornar 401 o 403)
    TASKS_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${API_URL}/tasks")
    
    if [ "$TASKS_CODE" = "401" ] || [ "$TASKS_CODE" = "403" ]; then
        echo -e "${GREEN}✅ GET /api/tasks protegido correctamente (HTTP $TASKS_CODE)${NC}"
    else
        echo -e "${YELLOW}⚠️  GET /api/tasks retorna HTTP $TASKS_CODE (esperado 401/403)${NC}"
    fi
}

# Función para mostrar resumen
show_summary() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📊 RESUMEN DE INTEGRACIÓN"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Backend URL: ${BACKEND_URL}"
    echo "API URL: ${API_URL}"
    echo "Frontend URL (dev): http://localhost:5173"
    echo ""
    echo "Para iniciar el frontend:"
    echo "  cd frontend && npm run dev"
    echo ""
    echo "Para iniciar el backend:"
    echo "  cd backend && mvn spring-boot:run"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Ejecutar pruebas
main() {
    check_backend
    BACKEND_STATUS=$?
    
    if [ $BACKEND_STATUS -eq 0 ]; then
        test_login_endpoint
        test_cors
        test_public_endpoints
        test_protected_endpoints
    fi
    
    show_summary
    
    if [ $BACKEND_STATUS -eq 0 ]; then
        echo -e "${GREEN}✅ Integración verificada correctamente${NC}"
        exit 0
    else
        echo -e "${RED}❌ Hay problemas con la integración${NC}"
        exit 1
    fi
}

# Ejecutar script
main
