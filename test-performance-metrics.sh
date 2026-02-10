#!/bin/bash

# Test Performance Metrics and Actuator Endpoints
# This script tests the monitoring and performance endpoints

BASE_URL="http://localhost:8080"
ADMIN_TOKEN=""

echo "========================================="
echo "Performance Metrics & Actuator Test"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test result
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $2"
    else
        echo -e "${RED}✗ FAIL${NC}: $2"
    fi
}

# Function to make HTTP request and check status
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    local auth_header=$5

    echo ""
    echo "Testing: $description"
    echo "Endpoint: $method $endpoint"
    
    if [ -n "$auth_header" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $auth_header" \
            -H "Content-Type: application/json")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq "$expected_status" ]; then
        print_result 0 "$description"
        if [ -n "$body" ] && [ "$body" != "null" ]; then
            echo "Response preview: $(echo "$body" | head -c 200)..."
        fi
    else
        print_result 1 "$description (Expected: $expected_status, Got: $http_code)"
        echo "Response: $body"
    fi
    
    return $([ "$http_code" -eq "$expected_status" ] && echo 0 || echo 1)
}

echo "Step 1: Login as admin to get token"
echo "-----------------------------------"
login_response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "admin",
        "password": "Admin123!"
    }')

ADMIN_TOKEN=$(echo $login_response | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Failed to get admin token. Make sure the backend is running and admin user exists.${NC}"
    echo "Response: $login_response"
    exit 1
fi

echo -e "${GREEN}✓ Admin token obtained${NC}"
echo ""

echo "Step 2: Test Actuator Endpoints (Public)"
echo "----------------------------------------"

# Test health endpoint
test_endpoint "GET" "/actuator/health" 200 "Health check endpoint"

# Test metrics endpoint
test_endpoint "GET" "/actuator/metrics" 200 "Metrics list endpoint"

# Test prometheus endpoint
test_endpoint "GET" "/actuator/prometheus" 200 "Prometheus metrics endpoint"

echo ""
echo "Step 3: Test Specific Metrics"
echo "-----------------------------"

# Test JVM memory metrics
test_endpoint "GET" "/actuator/metrics/jvm.memory.used" 200 "JVM memory used metric"

# Test HTTP server requests metrics
test_endpoint "GET" "/actuator/metrics/http.server.requests" 200 "HTTP server requests metric"

# Test HikariCP metrics
test_endpoint "GET" "/actuator/metrics/hikaricp.connections.active" 200 "HikariCP active connections metric"

echo ""
echo "Step 4: Test Performance Metrics API (Admin Only)"
echo "-------------------------------------------------"

# Test performance metrics endpoint
test_endpoint "GET" "/api/admin/metrics/performance?timeRange=HOUR" 200 \
    "Get performance metrics" "$ADMIN_TOKEN"

# Test performance alerts endpoint
test_endpoint "GET" "/api/admin/metrics/alerts" 200 \
    "Get performance alerts" "$ADMIN_TOKEN"

echo ""
echo "Step 5: Test Unauthorized Access"
echo "--------------------------------"

# Test without token (should fail)
test_endpoint "GET" "/api/admin/metrics/performance" 401 \
    "Performance metrics without auth (should fail)"

echo ""
echo "========================================="
echo "Test Summary"
echo "========================================="
echo ""
echo "All tests completed!"
echo ""
echo "To view detailed metrics, visit:"
echo "  - Health: $BASE_URL/actuator/health"
echo "  - Metrics: $BASE_URL/actuator/metrics"
echo "  - Prometheus: $BASE_URL/actuator/prometheus"
echo "  - Performance API: $BASE_URL/api/admin/metrics/performance"
echo ""
echo "To view in Swagger UI:"
echo "  - $BASE_URL/api/docs"
echo ""
