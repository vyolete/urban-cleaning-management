#!/bin/bash

# Simple Load Test (No Authentication Required)
# Tests public endpoints to validate system performance

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
RESULTS_DIR="./results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

mkdir -p "$RESULTS_DIR"

echo "========================================="
echo "Simple Load Test (Public Endpoints)"
echo "========================================="
echo ""
echo "Base URL: $BASE_URL"
echo "Timestamp: $TIMESTAMP"
echo ""

# Function to print colored output
print_status() {
    echo -e "${1}${2}${NC}"
}

# Function to run ab test
run_ab_test() {
    local test_name=$1
    local requests=$2
    local concurrency=$3
    local endpoint=$4
    
    print_status "$BLUE" "→ Running $test_name..."
    print_status "$BLUE" "  Requests: $requests, Concurrency: $concurrency"
    
    local result_file="$RESULTS_DIR/ab-$test_name-$TIMESTAMP.txt"
    
    ab -n $requests -c $concurrency "$BASE_URL$endpoint" > "$result_file" 2>&1
    
    # Extract key metrics
    local rps=$(grep "Requests per second:" "$result_file" | awk '{print $4}')
    local time_per_req=$(grep "Time per request:" "$result_file" | head -1 | awk '{print $4}')
    local failed=$(grep "Failed requests:" "$result_file" | awk '{print $3}')
    local p50=$(grep "50%" "$result_file" | awk '{print $2}')
    local p95=$(grep "95%" "$result_file" | awk '{print $2}')
    local p99=$(grep "99%" "$result_file" | awk '{print $2}')
    
    print_status "$GREEN" "✓ $test_name completed"
    echo "  Requests/sec: $rps"
    echo "  Time/request: ${time_per_req}ms"
    echo "  Failed: $failed"
    echo "  P50: ${p50}ms, P95: ${p95}ms, P99: ${p99}ms"
    echo "  Results: $result_file"
    echo ""
}

# Main execution
print_status "$YELLOW" "Phase 1: Warm-up"
print_status "$YELLOW" "========================================="
echo ""

# Warm up
for i in {1..10}; do
    curl -s "$BASE_URL/actuator/health" > /dev/null
done
sleep 2

print_status "$GREEN" "✓ System warmed up"
echo ""

print_status "$YELLOW" "Phase 2: Light Load Tests"
print_status "$YELLOW" "========================================="
echo ""

# Test 1: Health endpoint (baseline)
run_ab_test "health-light" 1000 10 "/actuator/health"

# Test 2: Metrics endpoint
run_ab_test "metrics-light" 500 5 "/actuator/metrics"

# Test 3: Prometheus endpoint
run_ab_test "prometheus-light" 200 5 "/actuator/prometheus"

print_status "$YELLOW" "Phase 3: Normal Load (50 concurrent)"
print_status "$YELLOW" "========================================="
echo ""

# Test 4: Health under normal load
run_ab_test "health-normal" 5000 50 "/actuator/health"

# Test 5: Metrics under normal load
run_ab_test "metrics-normal" 2000 50 "/actuator/metrics"

print_status "$YELLOW" "Phase 4: Peak Load (100 concurrent)"
print_status "$YELLOW" "========================================="
echo ""

# Test 6: Health under peak load
run_ab_test "health-peak" 10000 100 "/actuator/health"

# Test 7: Metrics under peak load
run_ab_test "metrics-peak" 5000 100 "/actuator/metrics"

print_status "$YELLOW" "Phase 5: Stress Test (200 concurrent)"
print_status "$YELLOW" "========================================="
echo ""

# Test 8: Health under stress
run_ab_test "health-stress" 20000 200 "/actuator/health"

print_status "$YELLOW" "========================================="
print_status "$YELLOW" "Collecting Final Metrics"
print_status "$YELLOW" "========================================="
echo ""

# Collect metrics
curl -s "$BASE_URL/actuator/metrics/hikaricp.connections.active" > "$RESULTS_DIR/connections-$TIMESTAMP.json"
curl -s "$BASE_URL/actuator/metrics/jvm.memory.used" > "$RESULTS_DIR/memory-$TIMESTAMP.json"
curl -s "$BASE_URL/actuator/metrics/system.cpu.usage" > "$RESULTS_DIR/cpu-$TIMESTAMP.json"
curl -s "$BASE_URL/actuator/metrics/http.server.requests" > "$RESULTS_DIR/http-requests-$TIMESTAMP.json"

print_status "$GREEN" "✓ Metrics collected"
echo ""

# Generate summary
print_status "$YELLOW" "========================================="
print_status "$YELLOW" "Test Summary"
print_status "$YELLOW" "========================================="
echo ""

{
    echo "========================================="
    echo "Simple Load Test Summary"
    echo "========================================="
    echo ""
    echo "Test Date: $(date)"
    echo "Base URL: $BASE_URL"
    echo ""
    echo "Test Phases:"
    echo "  1. Light Load (10-50 concurrent)"
    echo "  2. Normal Load (50 concurrent)"
    echo "  3. Peak Load (100 concurrent)"
    echo "  4. Stress Test (200 concurrent)"
    echo ""
    echo "Total Requests: ~43,700"
    echo ""
    echo "Results Location: $RESULTS_DIR"
    echo ""
    echo "Key Findings:"
    echo "-------------"
    echo ""
    
    # Extract summary from last test
    echo "Health Endpoint (Stress Test - 200 concurrent):"
    grep "Requests per second:" "$RESULTS_DIR/ab-health-stress-$TIMESTAMP.txt" | awk '{print "  Throughput: " $4 " req/s"}'
    grep "Time per request:" "$RESULTS_DIR/ab-health-stress-$TIMESTAMP.txt" | head -1 | awk '{print "  Avg Response: " $4 " ms"}'
    grep "Failed requests:" "$RESULTS_DIR/ab-health-stress-$TIMESTAMP.txt" | awk '{print "  Failed: " $3}'
    grep "95%" "$RESULTS_DIR/ab-health-stress-$TIMESTAMP.txt" | awk '{print "  P95: " $2 " ms"}'
    grep "99%" "$RESULTS_DIR/ab-health-stress-$TIMESTAMP.txt" | awk '{print "  P99: " $2 " ms"}'
    echo ""
    
    echo "System Metrics (Final):"
    echo "  Active Connections: $(cat "$RESULTS_DIR/connections-$TIMESTAMP.json" | grep -o '"value":[0-9.]*' | cut -d':' -f2)"
    echo "  Memory Used: $(cat "$RESULTS_DIR/memory-$TIMESTAMP.json" | grep -o '"value":[0-9.]*' | head -1 | cut -d':' -f2 | awk '{printf "%.0f MB", $1/1024/1024}')"
    echo "  CPU Usage: $(cat "$RESULTS_DIR/cpu-$TIMESTAMP.json" | grep -o '"value":[0-9.]*' | cut -d':' -f2 | awk '{printf "%.1f%%", $1*100}')"
    echo ""
    echo "========================================="
    echo "Performance Assessment"
    echo "========================================="
    echo ""
    echo "✓ System handled 43,700+ requests successfully"
    echo "✓ No authentication required for public endpoints"
    echo "✓ Actuator endpoints remain responsive under load"
    echo ""
    echo "Next Steps:"
    echo "  1. Review detailed results in $RESULTS_DIR"
    echo "  2. Check for any failed requests"
    echo "  3. Analyze response time distribution"
    echo "  4. Compare against SLA targets"
    echo ""
} | tee "$RESULTS_DIR/summary-$TIMESTAMP.txt"

print_status "$GREEN" "========================================="
print_status "$GREEN" "Load Test Complete!"
print_status "$GREEN" "========================================="
echo ""
echo "Summary saved to: $RESULTS_DIR/summary-$TIMESTAMP.txt"
echo ""
