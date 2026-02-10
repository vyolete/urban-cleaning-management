#!/bin/bash

# Comprehensive Load Testing Script
# Tests the Urban Cleaning Management System under various load conditions

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin123!}"
RESULTS_DIR="./results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create results directory
mkdir -p "$RESULTS_DIR"

echo "========================================="
echo "Comprehensive Load Testing"
echo "========================================="
echo ""
echo "Base URL: $BASE_URL"
echo "Results Directory: $RESULTS_DIR"
echo "Timestamp: $TIMESTAMP"
echo ""

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to get JWT token
get_jwt_token() {
    print_status "$BLUE" "→ Getting JWT token..."
    
    local response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")
    
    local token=$(echo "$response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$token" ]; then
        print_status "$RED" "✗ Failed to get JWT token"
        echo "Response: $response"
        exit 1
    fi
    
    print_status "$GREEN" "✓ JWT token obtained"
    echo "$token"
}

# Function to warm up the system
warmup_system() {
    print_status "$YELLOW" "→ Warming up system..."
    
    # Make a few requests to warm up JVM, caches, etc.
    for i in {1..10}; do
        curl -s "$BASE_URL/actuator/health" > /dev/null
        curl -s "$BASE_URL/api/reports" > /dev/null 2>&1 || true
    done
    
    sleep 2
    print_status "$GREEN" "✓ System warmed up"
}

# Function to collect baseline metrics
collect_baseline_metrics() {
    print_status "$YELLOW" "→ Collecting baseline metrics..."
    
    local metrics_file="$RESULTS_DIR/baseline-metrics-$TIMESTAMP.json"
    
    curl -s -H "Authorization: Bearer $JWT_TOKEN" \
        "$BASE_URL/api/admin/metrics/performance?timeRange=HOUR" \
        > "$metrics_file"
    
    print_status "$GREEN" "✓ Baseline metrics saved to $metrics_file"
}

# Function to run load test with wrk
run_wrk_test() {
    local test_name=$1
    local duration=$2
    local threads=$3
    local connections=$4
    local endpoint=$5
    
    if ! command_exists wrk; then
        print_status "$YELLOW" "⚠ wrk not installed, skipping $test_name"
        return
    fi
    
    print_status "$BLUE" "→ Running $test_name with wrk..."
    print_status "$BLUE" "  Duration: ${duration}s, Threads: $threads, Connections: $connections"
    
    local result_file="$RESULTS_DIR/wrk-$test_name-$TIMESTAMP.txt"
    
    wrk -t$threads -c$connections -d${duration}s "$BASE_URL$endpoint" \
        > "$result_file" 2>&1
    
    print_status "$GREEN" "✓ $test_name completed"
    
    # Extract key metrics
    local requests=$(grep "Requests/sec:" "$result_file" | awk '{print $2}')
    local latency_avg=$(grep "Latency" "$result_file" | awk '{print $2}')
    
    echo "  Requests/sec: $requests"
    echo "  Avg Latency: $latency_avg"
    echo "  Results: $result_file"
}

# Function to run load test with ab
run_ab_test() {
    local test_name=$1
    local requests=$2
    local concurrency=$3
    local endpoint=$4
    
    if ! command_exists ab; then
        print_status "$YELLOW" "⚠ ab not installed, skipping $test_name"
        return
    fi
    
    print_status "$BLUE" "→ Running $test_name with Apache Bench..."
    print_status "$BLUE" "  Requests: $requests, Concurrency: $concurrency"
    
    local result_file="$RESULTS_DIR/ab-$test_name-$TIMESTAMP.txt"
    
    ab -n $requests -c $concurrency "$BASE_URL$endpoint" \
        > "$result_file" 2>&1
    
    print_status "$GREEN" "✓ $test_name completed"
    
    # Extract key metrics
    local rps=$(grep "Requests per second:" "$result_file" | awk '{print $4}')
    local time_per_req=$(grep "Time per request:" "$result_file" | head -1 | awk '{print $4}')
    
    echo "  Requests/sec: $rps"
    echo "  Time/request: ${time_per_req}ms"
    echo "  Results: $result_file"
}

# Function to monitor system during test
monitor_system() {
    local duration=$1
    local output_file=$2
    
    print_status "$BLUE" "→ Monitoring system metrics for ${duration}s..."
    
    local end_time=$(($(date +%s) + duration))
    
    echo "timestamp,cpu,memory,connections,requests" > "$output_file"
    
    while [ $(date +%s) -lt $end_time ]; do
        local timestamp=$(date +%s)
        
        # Get metrics from actuator
        local cpu=$(curl -s "$BASE_URL/actuator/metrics/system.cpu.usage" | grep -o '"value":[0-9.]*' | cut -d':' -f2 || echo "0")
        local memory=$(curl -s "$BASE_URL/actuator/metrics/jvm.memory.used" | grep -o '"value":[0-9.]*' | head -1 | cut -d':' -f2 || echo "0")
        local connections=$(curl -s "$BASE_URL/actuator/metrics/hikaricp.connections.active" | grep -o '"value":[0-9.]*' | cut -d':' -f2 || echo "0")
        local requests=$(curl -s "$BASE_URL/actuator/metrics/http.server.requests" | grep -o '"count":[0-9.]*' | head -1 | cut -d':' -f2 || echo "0")
        
        echo "$timestamp,$cpu,$memory,$connections,$requests" >> "$output_file"
        
        sleep 5
    done
    
    print_status "$GREEN" "✓ Monitoring complete: $output_file"
}

# Main execution
main() {
    print_status "$YELLOW" "========================================="
    print_status "$YELLOW" "Phase 1: Preparation"
    print_status "$YELLOW" "========================================="
    echo ""
    
    # Get JWT token
    JWT_TOKEN=$(get_jwt_token)
    echo ""
    
    # Warm up system
    warmup_system
    echo ""
    
    # Collect baseline
    collect_baseline_metrics
    echo ""
    
    print_status "$YELLOW" "========================================="
    print_status "$YELLOW" "Phase 2: Light Load Tests"
    print_status "$YELLOW" "========================================="
    echo ""
    
    # Test 1: Health endpoint (baseline)
    run_ab_test "health-check" 1000 10 "/actuator/health"
    echo ""
    
    # Test 2: Metrics endpoint
    run_ab_test "metrics-endpoint" 500 5 "/actuator/metrics"
    echo ""
    
    print_status "$YELLOW" "========================================="
    print_status "$YELLOW" "Phase 3: Normal Load Test (50 users)"
    print_status "$YELLOW" "========================================="
    echo ""
    
    # Start monitoring in background
    monitor_system 120 "$RESULTS_DIR/monitor-normal-$TIMESTAMP.csv" &
    MONITOR_PID=$!
    
    # Run normal load test
    if command_exists wrk; then
        run_wrk_test "normal-load" 120 10 50 "/api/reports"
    else
        run_ab_test "normal-load" 6000 50 "/api/reports"
    fi
    
    # Wait for monitoring to complete
    wait $MONITOR_PID
    echo ""
    
    print_status "$YELLOW" "========================================="
    print_status "$YELLOW" "Phase 4: Peak Load Test (100 users)"
    print_status "$YELLOW" "========================================="
    echo ""
    
    # Start monitoring in background
    monitor_system 60 "$RESULTS_DIR/monitor-peak-$TIMESTAMP.csv" &
    MONITOR_PID=$!
    
    # Run peak load test
    if command_exists wrk; then
        run_wrk_test "peak-load" 60 20 100 "/api/reports"
    else
        run_ab_test "peak-load" 6000 100 "/api/reports"
    fi
    
    # Wait for monitoring to complete
    wait $MONITOR_PID
    echo ""
    
    print_status "$YELLOW" "========================================="
    print_status "$YELLOW" "Phase 5: Stress Test (200 users)"
    print_status "$YELLOW" "========================================="
    echo ""
    
    # Start monitoring in background
    monitor_system 30 "$RESULTS_DIR/monitor-stress-$TIMESTAMP.csv" &
    MONITOR_PID=$!
    
    # Run stress test
    if command_exists wrk; then
        run_wrk_test "stress-test" 30 40 200 "/api/reports"
    else
        run_ab_test "stress-test" 6000 200 "/api/reports"
    fi
    
    # Wait for monitoring to complete
    wait $MONITOR_PID
    echo ""
    
    print_status "$YELLOW" "========================================="
    print_status "$YELLOW" "Phase 6: Post-Test Analysis"
    print_status "$YELLOW" "========================================="
    echo ""
    
    # Collect final metrics
    print_status "$BLUE" "→ Collecting final metrics..."
    curl -s -H "Authorization: Bearer $JWT_TOKEN" \
        "$BASE_URL/api/admin/metrics/performance?timeRange=HOUR" \
        > "$RESULTS_DIR/final-metrics-$TIMESTAMP.json"
    
    # Check alerts
    print_status "$BLUE" "→ Checking performance alerts..."
    curl -s -H "Authorization: Bearer $JWT_TOKEN" \
        "$BASE_URL/api/admin/metrics/alerts" \
        > "$RESULTS_DIR/alerts-$TIMESTAMP.json"
    
    print_status "$GREEN" "✓ Post-test analysis complete"
    echo ""
    
    # Generate summary report
    generate_summary_report
}

# Function to generate summary report
generate_summary_report() {
    local report_file="$RESULTS_DIR/summary-report-$TIMESTAMP.txt"
    
    print_status "$YELLOW" "========================================="
    print_status "$YELLOW" "Test Summary Report"
    print_status "$YELLOW" "========================================="
    
    {
        echo "========================================="
        echo "Load Test Summary Report"
        echo "========================================="
        echo ""
        echo "Test Date: $(date)"
        echo "Base URL: $BASE_URL"
        echo ""
        echo "Test Phases:"
        echo "  1. Light Load Tests"
        echo "  2. Normal Load (50 users, 2 minutes)"
        echo "  3. Peak Load (100 users, 1 minute)"
        echo "  4. Stress Test (200 users, 30 seconds)"
        echo ""
        echo "Results Location: $RESULTS_DIR"
        echo ""
        echo "Files Generated:"
        ls -lh "$RESULTS_DIR"/*$TIMESTAMP* | awk '{print "  " $9 " (" $5 ")"}'
        echo ""
        echo "========================================="
        echo "Next Steps:"
        echo "========================================="
        echo ""
        echo "1. Review detailed results in $RESULTS_DIR"
        echo "2. Check alerts: cat $RESULTS_DIR/alerts-$TIMESTAMP.json"
        echo "3. Compare baseline vs final metrics"
        echo "4. Analyze monitoring data (CSV files)"
        echo "5. Identify bottlenecks and optimize"
        echo ""
        echo "Performance Targets:"
        echo "  - Simple queries: < 500ms average"
        echo "  - Analytics queries: < 2s average"
        echo "  - Success rate: > 99.9%"
        echo "  - Throughput: > 100 req/s"
        echo ""
    } | tee "$report_file"
    
    print_status "$GREEN" "✓ Summary report saved to $report_file"
    echo ""
    
    print_status "$GREEN" "========================================="
    print_status "$GREEN" "Load Testing Complete!"
    print_status "$GREEN" "========================================="
}

# Run main function
main
