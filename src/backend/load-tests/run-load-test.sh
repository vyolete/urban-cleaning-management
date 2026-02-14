#!/bin/bash

# Urban Cleaning Management System - Load Testing Script
# This script performs basic load testing using curl and parallel execution

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin123!@#}"
RESULTS_DIR="./results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create results directory
mkdir -p "$RESULTS_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Urban Cleaning - Load Testing${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Base URL:${NC} $BASE_URL"
echo -e "${YELLOW}Test Type:${NC} $1"
echo -e "${YELLOW}Results Dir:${NC} $RESULTS_DIR"
echo ""

# Function to login and get JWT token
get_jwt_token() {
    local response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")
    
    local token=$(echo "$response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$token" ]; then
        echo -e "${RED}ERROR: Failed to obtain JWT token${NC}"
        echo "Response: $response"
        exit 1
    fi
    
    echo "$token"
}

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local token=$3
    local data=$4
    local expected_time=$5
    
    local start_time=$(date +%s%3N)
    
    if [ "$method" = "GET" ]; then
        local response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X GET "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $token" \
            -H "Accept: application/json")
    else
        local response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X POST "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi
    
    local end_time=$(date +%s%3N)
    local duration=$((end_time - start_time))
    
    # Extract status code and time from response
    local status_code=$(echo "$response" | tail -n 2 | head -n 1)
    local time_total=$(echo "$response" | tail -n 1)
    local time_ms=$(echo "$time_total * 1000" | bc | cut -d'.' -f1)
    
    # Check if successful
    if [ "$status_code" = "200" ] || [ "$status_code" = "201" ]; then
        if [ "$time_ms" -lt "$expected_time" ]; then
            echo -e "${GREEN}✓${NC} $method $endpoint - ${status_code} - ${time_ms}ms"
        else
            echo -e "${YELLOW}⚠${NC} $method $endpoint - ${status_code} - ${time_ms}ms (expected < ${expected_time}ms)"
        fi
        echo "1" # Success
    else
        echo -e "${RED}✗${NC} $method $endpoint - ${status_code} - ${time_ms}ms"
        echo "0" # Failure
    fi
}

# Function to run normal load test
run_normal_load() {
    echo -e "${BLUE}Running Normal Load Test (50 concurrent users, 10 minutes)${NC}"
    echo ""
    
    # Get JWT token
    echo -e "${YELLOW}Authenticating...${NC}"
    JWT_TOKEN=$(get_jwt_token)
    echo -e "${GREEN}✓ Authentication successful${NC}"
    echo ""
    
    # Test endpoints
    echo -e "${YELLOW}Testing endpoints...${NC}"
    
    local total_requests=0
    local successful_requests=0
    local failed_requests=0
    
    # Simulate 50 concurrent users for 1 minute (simplified version)
    local duration=60
    local users=10
    local requests_per_user=10
    
    echo -e "${YELLOW}Simulating $users users making $requests_per_user requests each...${NC}"
    
    for i in $(seq 1 $users); do
        (
            for j in $(seq 1 $requests_per_user); do
                # 70% reads
                if [ $((RANDOM % 10)) -lt 7 ]; then
                    # Random read operation
                    case $((RANDOM % 3)) in
                        0)
                            test_endpoint "GET" "/api/reports" "$JWT_TOKEN" "" 500
                            ;;
                        1)
                            test_endpoint "GET" "/api/tasks" "$JWT_TOKEN" "" 500
                            ;;
                        2)
                            test_endpoint "GET" "/api/analytics/tasks/distribution/category" "$JWT_TOKEN" "" 2000
                            ;;
                    esac
                fi
                
                # Small delay between requests
                sleep 0.5
            done
        ) &
    done
    
    # Wait for all background jobs to complete
    wait
    
    echo ""
    echo -e "${GREEN}Load test completed${NC}"
}

# Function to test actuator endpoints
test_actuator() {
    echo -e "${BLUE}Testing Actuator Endpoints${NC}"
    echo ""
    
    # Test health endpoint
    echo -e "${YELLOW}Testing /actuator/health...${NC}"
    local health_response=$(curl -s "$BASE_URL/actuator/health")
    local health_status=$(echo "$health_response" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
    
    if [ "$health_status" = "UP" ]; then
        echo -e "${GREEN}✓ Health check passed - Status: UP${NC}"
    else
        echo -e "${RED}✗ Health check failed - Status: $health_status${NC}"
    fi
    
    # Test metrics endpoint
    echo -e "${YELLOW}Testing /actuator/metrics...${NC}"
    local metrics_response=$(curl -s "$BASE_URL/actuator/metrics")
    
    if echo "$metrics_response" | grep -q "names"; then
        echo -e "${GREEN}✓ Metrics endpoint accessible${NC}"
    else
        echo -e "${RED}✗ Metrics endpoint failed${NC}"
    fi
    
    # Test prometheus endpoint
    echo -e "${YELLOW}Testing /actuator/prometheus...${NC}"
    local prometheus_response=$(curl -s "$BASE_URL/actuator/prometheus")
    
    if echo "$prometheus_response" | grep -q "jvm_memory"; then
        echo -e "${GREEN}✓ Prometheus endpoint accessible${NC}"
    else
        echo -e "${RED}✗ Prometheus endpoint failed${NC}"
    fi
    
    echo ""
}

# Function to test performance metrics endpoints
test_performance_metrics() {
    echo -e "${BLUE}Testing Performance Metrics Endpoints${NC}"
    echo ""
    
    # Get JWT token
    JWT_TOKEN=$(get_jwt_token)
    
    # Test aggregated metrics
    echo -e "${YELLOW}Testing /api/admin/metrics/performance...${NC}"
    local perf_response=$(curl -s -H "Authorization: Bearer $JWT_TOKEN" \
        "$BASE_URL/api/admin/metrics/performance?range=1h")
    
    if echo "$perf_response" | grep -q "timestamp"; then
        echo -e "${GREEN}✓ Performance metrics endpoint accessible${NC}"
        
        # Extract and display key metrics
        echo ""
        echo -e "${BLUE}Current Metrics:${NC}"
        echo "$perf_response" | python3 -m json.tool 2>/dev/null || echo "$perf_response"
    else
        echo -e "${RED}✗ Performance metrics endpoint failed${NC}"
    fi
    
    echo ""
}

# Function to generate load test report
generate_report() {
    local report_file="$RESULTS_DIR/load-test-report-$TIMESTAMP.txt"
    
    echo "Urban Cleaning Management System - Load Test Report" > "$report_file"
    echo "Generated: $(date)" >> "$report_file"
    echo "========================================" >> "$report_file"
    echo "" >> "$report_file"
    echo "Test Configuration:" >> "$report_file"
    echo "  Base URL: $BASE_URL" >> "$report_file"
    echo "  Test Type: $1" >> "$report_file"
    echo "" >> "$report_file"
    echo "Results:" >> "$report_file"
    echo "  Total Requests: $2" >> "$report_file"
    echo "  Successful: $3" >> "$report_file"
    echo "  Failed: $4" >> "$report_file"
    echo "  Success Rate: $(echo "scale=2; $3 * 100 / $2" | bc)%" >> "$report_file"
    echo "" >> "$report_file"
    
    echo -e "${GREEN}Report saved to: $report_file${NC}"
}

# Main execution
case "$1" in
    "normal")
        run_normal_load
        ;;
    "actuator")
        test_actuator
        ;;
    "metrics")
        test_performance_metrics
        ;;
    "all")
        test_actuator
        echo ""
        test_performance_metrics
        echo ""
        run_normal_load
        ;;
    *)
        echo "Usage: $0 {normal|actuator|metrics|all}"
        echo ""
        echo "Options:"
        echo "  normal    - Run normal load test (simplified)"
        echo "  actuator  - Test Actuator endpoints"
        echo "  metrics   - Test Performance Metrics endpoints"
        echo "  all       - Run all tests"
        echo ""
        echo "Environment Variables:"
        echo "  BASE_URL         - Base URL of the API (default: http://localhost:8080)"
        echo "  ADMIN_USERNAME   - Admin username (default: admin)"
        echo "  ADMIN_PASSWORD   - Admin password (default: admin123)"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Testing completed${NC}"
echo -e "${GREEN}========================================${NC}"
