#!/bin/bash

# Quick Load Test Script - Simplified version without timing issues
# This script tests the key endpoints and collects basic metrics

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin123!@#}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Urban Cleaning - Quick Load Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to login and get JWT token
get_jwt_token() {
    local response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")
    
    local token=$(echo "$response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$token" ]; then
        echo -e "${RED}ERROR: Failed to obtain JWT token${NC}"
        exit 1
    fi
    
    echo "$token"
}

# Test 1: Actuator Health
echo -e "${YELLOW}Test 1: Actuator Health${NC}"
health_response=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/health")
health_status=$(echo "$health_response" | tail -n 1)
if [ "$health_status" = "200" ]; then
    echo -e "${GREEN}✓ Health check passed${NC}"
else
    echo -e "${RED}✗ Health check failed (HTTP $health_status)${NC}"
fi
echo ""

# Test 2: Actuator Metrics
echo -e "${YELLOW}Test 2: Actuator Metrics${NC}"
metrics_response=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/metrics")
metrics_status=$(echo "$metrics_response" | tail -n 1)
if [ "$metrics_status" = "200" ]; then
    echo -e "${GREEN}✓ Metrics endpoint accessible${NC}"
else
    echo -e "${RED}✗ Metrics endpoint failed (HTTP $metrics_status)${NC}"
fi
echo ""

# Test 3: Prometheus Metrics
echo -e "${YELLOW}Test 3: Prometheus Metrics${NC}"
prom_response=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/prometheus")
prom_status=$(echo "$prom_response" | tail -n 1)
if [ "$prom_status" = "200" ]; then
    echo -e "${GREEN}✓ Prometheus endpoint accessible${NC}"
else
    echo -e "${RED}✗ Prometheus endpoint failed (HTTP $prom_status)${NC}"
fi
echo ""

# Get JWT token
echo -e "${YELLOW}Authenticating...${NC}"
JWT_TOKEN=$(get_jwt_token)
echo -e "${GREEN}✓ Authentication successful${NC}"
echo ""

# Test 4: Performance Metrics
echo -e "${YELLOW}Test 4: Performance Metrics${NC}"
perf_response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $JWT_TOKEN" \
    "$BASE_URL/api/admin/metrics/performance?range=1h")
perf_status=$(echo "$perf_response" | tail -n 1)
if [ "$perf_status" = "200" ]; then
    echo -e "${GREEN}✓ Performance metrics accessible${NC}"
    echo ""
    echo -e "${BLUE}Current Metrics:${NC}"
    echo "$perf_response" | sed '$d' | python3 -m json.tool 2>/dev/null || echo "$perf_response" | sed '$d'
else
    echo -e "${RED}✗ Performance metrics failed (HTTP $perf_status)${NC}"
fi
echo ""

# Test 5: Load Test - Simple Queries
echo -e "${YELLOW}Test 5: Load Test - Simple Queries (10 requests)${NC}"
success_count=0
total_time=0

for i in {1..10}; do
    start=$(date +%s%N)
    response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $JWT_TOKEN" \
        "$BASE_URL/api/reports")
    status=$(echo "$response" | tail -n 1)
    end=$(date +%s%N)
    
    duration=$(( (end - start) / 1000000 ))  # Convert to milliseconds
    total_time=$((total_time + duration))
    
    if [ "$status" = "200" ]; then
        success_count=$((success_count + 1))
        if [ $duration -lt 500 ]; then
            echo -e "${GREEN}✓${NC} Request $i: ${duration}ms (< 500ms target)"
        else
            echo -e "${YELLOW}⚠${NC} Request $i: ${duration}ms (> 500ms target)"
        fi
    else
        echo -e "${RED}✗${NC} Request $i: HTTP $status"
    fi
done

avg_time=$((total_time / 10))
success_rate=$((success_count * 100 / 10))

echo ""
echo -e "${BLUE}Results:${NC}"
echo "  Total Requests: 10"
echo "  Successful: $success_count"
echo "  Success Rate: ${success_rate}%"
echo "  Average Response Time: ${avg_time}ms"
echo ""

# Test 6: Load Test - Analytics Queries
echo -e "${YELLOW}Test 6: Load Test - Analytics Queries (5 requests)${NC}"
analytics_success=0
analytics_time=0

for i in {1..5}; do
    start=$(date +%s%N)
    response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $JWT_TOKEN" \
        "$BASE_URL/api/analytics/tasks/distribution/category")
    status=$(echo "$response" | tail -n 1)
    end=$(date +%s%N)
    
    duration=$(( (end - start) / 1000000 ))
    analytics_time=$((analytics_time + duration))
    
    if [ "$status" = "200" ]; then
        analytics_success=$((analytics_success + 1))
        if [ $duration -lt 2000 ]; then
            echo -e "${GREEN}✓${NC} Request $i: ${duration}ms (< 2000ms target)"
        else
            echo -e "${YELLOW}⚠${NC} Request $i: ${duration}ms (> 2000ms target)"
        fi
    else
        echo -e "${RED}✗${NC} Request $i: HTTP $status"
    fi
done

analytics_avg=$((analytics_time / 5))
analytics_rate=$((analytics_success * 100 / 5))

echo ""
echo -e "${BLUE}Results:${NC}"
echo "  Total Requests: 5"
echo "  Successful: $analytics_success"
echo "  Success Rate: ${analytics_rate}%"
echo "  Average Response Time: ${analytics_avg}ms"
echo ""

# Summary
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Test Summary${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Simple Queries:${NC}"
echo "  Success Rate: ${success_rate}% (target: > 99.9%)"
echo "  Avg Response Time: ${avg_time}ms (target: < 500ms)"
echo ""
echo -e "${BLUE}Analytics Queries:${NC}"
echo "  Success Rate: ${analytics_rate}% (target: > 99.9%)"
echo "  Avg Response Time: ${analytics_avg}ms (target: < 2000ms)"
echo ""

# Check SLA compliance
sla_passed=true

if [ $success_rate -lt 99 ]; then
    echo -e "${RED}✗ Simple queries success rate below target${NC}"
    sla_passed=false
fi

if [ $avg_time -gt 500 ]; then
    echo -e "${YELLOW}⚠ Simple queries response time above target${NC}"
fi

if [ $analytics_rate -lt 99 ]; then
    echo -e "${RED}✗ Analytics queries success rate below target${NC}"
    sla_passed=false
fi

if [ $analytics_avg -gt 2000 ]; then
    echo -e "${YELLOW}⚠ Analytics queries response time above target${NC}"
fi

if [ "$sla_passed" = true ]; then
    echo ""
    echo -e "${GREEN}✓ SLA requirements met!${NC}"
else
    echo ""
    echo -e "${RED}✗ SLA requirements not met${NC}"
fi

echo ""
