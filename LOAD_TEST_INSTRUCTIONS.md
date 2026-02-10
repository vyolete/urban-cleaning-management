# Load Testing Instructions

**Date**: February 9, 2026  
**Phase**: 5 - Performance Testing & Monitoring  
**Status**: Ready to Execute

---

## Overview

This document provides step-by-step instructions for running comprehensive load tests on the Urban Cleaning Management System to validate performance under various load conditions.

---

## Prerequisites

### 1. System Requirements

- **Backend**: Running on `http://localhost:8080`
- **Database**: PostgreSQL with PostGIS running
- **Admin User**: Credentials available (default: admin/Admin123!)
- **Disk Space**: At least 500MB for test results

### 2. Install Load Testing Tools

```bash
cd backend/load-tests
./install-tools.sh
```

This will install:
- **wrk**: HTTP benchmarking tool (recommended)
- **Apache Bench (ab)**: Alternative HTTP testing tool
- **Apache JMeter**: Comprehensive load testing (optional)

**Manual Installation (macOS)**:
```bash
# Install wrk
brew install wrk

# Apache Bench comes pre-installed on macOS
ab -V

# Install JMeter (optional)
brew install jmeter
```

**Manual Installation (Linux)**:
```bash
# Install wrk
sudo apt-get install wrk

# Install Apache Bench
sudo apt-get install apache2-utils

# Install JMeter
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
sudo mv apache-jmeter-5.6.3 /opt/
sudo ln -s /opt/apache-jmeter-5.6.3/bin/jmeter /usr/local/bin/jmeter
```

---

## Quick Start

### Option 1: Automated Comprehensive Test (Recommended)

This runs all test phases automatically:

```bash
cd backend/load-tests
./run-comprehensive-load-test.sh
```

**What it does**:
1. Warms up the system
2. Collects baseline metrics
3. Runs light load tests (health checks)
4. Runs normal load test (50 users, 2 minutes)
5. Runs peak load test (100 users, 1 minute)
6. Runs stress test (200 users, 30 seconds)
7. Monitors system metrics during tests
8. Generates summary report

**Duration**: ~5-7 minutes total

### Option 2: Individual Tests

Run specific test scenarios:

```bash
# Test with wrk (if installed)
wrk -t10 -c50 -d120s http://localhost:8080/api/reports

# Test with Apache Bench
ab -n 6000 -c 50 http://localhost:8080/api/reports

# Test with JMeter
jmeter -n -t normal-load-test.jmx -l results/test-results.jtl
```

---

## Test Scenarios

### 1. Normal Load Test
- **Users**: 50 concurrent
- **Duration**: 2 minutes (120 seconds)
- **Ramp-up**: Gradual
- **Target**: Validate normal operation

**Expected Results**:
- Average response time: < 500ms
- Success rate: > 99.9%
- Throughput: > 100 req/s

### 2. Peak Load Test
- **Users**: 100 concurrent
- **Duration**: 1 minute (60 seconds)
- **Ramp-up**: Fast
- **Target**: Validate peak traffic handling

**Expected Results**:
- Average response time: < 1s
- Success rate: > 99.5%
- Throughput: > 150 req/s

### 3. Stress Test
- **Users**: 200 concurrent
- **Duration**: 30 seconds
- **Ramp-up**: Immediate
- **Target**: Identify breaking point

**Expected Results**:
- System remains stable
- No crashes or memory leaks
- Graceful degradation if limits reached

---

## Monitoring During Tests

### Real-time Monitoring

Open multiple terminal windows to monitor:

**Terminal 1 - Run Tests**:
```bash
cd backend/load-tests
./run-comprehensive-load-test.sh
```

**Terminal 2 - Watch Metrics**:
```bash
# Watch health status
watch -n 2 'curl -s http://localhost:8080/actuator/health | jq'

# Watch active connections
watch -n 2 'curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq'

# Watch memory usage
watch -n 2 'curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq'
```

**Terminal 3 - Watch Logs**:
```bash
# Watch application logs
tail -f backend/logs/application.log

# Or if running with Maven
# Logs will appear in the Maven output
```

### Performance Metrics API

Get aggregated metrics during/after tests:

```bash
# Get JWT token first
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}' \
  | jq -r '.token')

# Get performance metrics
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/admin/metrics/performance?timeRange=HOUR" \
  | jq

# Check alerts
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/admin/metrics/alerts" \
  | jq
```

---

## Analyzing Results

### 1. Review Test Results

Results are saved in `backend/load-tests/results/`:

```bash
cd backend/load-tests/results

# View summary report
cat summary-report-*.txt

# View detailed wrk results
cat wrk-normal-load-*.txt

# View monitoring data
cat monitor-normal-*.csv
```

### 2. Key Metrics to Check

**Response Times**:
```bash
# From wrk output
grep "Latency" wrk-*.txt

# From ab output
grep "Time per request" ab-*.txt
```

**Throughput**:
```bash
# From wrk output
grep "Requests/sec" wrk-*.txt

# From ab output
grep "Requests per second" ab-*.txt
```

**Error Rate**:
```bash
# From wrk output
grep "Non-2xx" wrk-*.txt

# From ab output
grep "Failed requests" ab-*.txt
```

### 3. Compare Baseline vs Final Metrics

```bash
# View baseline metrics
cat baseline-metrics-*.json | jq

# View final metrics
cat final-metrics-*.json | jq

# Compare key values
diff <(cat baseline-metrics-*.json | jq -S) \
     <(cat final-metrics-*.json | jq -S)
```

### 4. Analyze Monitoring Data

```bash
# View monitoring CSV
cat monitor-normal-*.csv

# Calculate averages (requires awk)
awk -F',' 'NR>1 {sum+=$2; count++} END {print "Avg CPU:", sum/count}' monitor-normal-*.csv
awk -F',' 'NR>1 {sum+=$3; count++} END {print "Avg Memory:", sum/count}' monitor-normal-*.csv
awk -F',' 'NR>1 {sum+=$4; count++} END {print "Avg Connections:", sum/count}' monitor-normal-*.csv
```

---

## Performance Targets (SLA Requirements)

### Response Time Targets

| Endpoint Type | Average | P95 | P99 |
|--------------|---------|-----|-----|
| Simple Queries (GET /api/reports) | < 500ms | < 1s | < 1.5s |
| Analytics (GET /api/analytics/*) | < 2s | < 3s | < 4s |
| Write Operations (POST, PUT) | < 1s | < 2s | < 3s |

### System Targets

| Metric | Target |
|--------|--------|
| Success Rate | > 99.9% |
| Error Rate | < 0.1% |
| Throughput | > 100 req/s |
| DB Connections | < 90% of pool (< 18/20) |
| Memory Usage | < 85% of heap |
| CPU Usage | < 80% |

---

## Troubleshooting

### High Response Times

**Symptoms**: Average response time > 1s

**Diagnosis**:
```bash
# Check slow queries
curl http://localhost:8080/actuator/metrics/http.server.requests | jq

# Check database connections
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq

# Check cache hit rate
curl http://localhost:8080/actuator/metrics/cache.gets | jq
```

**Solutions**:
1. Add missing database indexes
2. Increase cache TTL
3. Optimize slow queries
4. Increase connection pool size

### High Error Rate

**Symptoms**: Error rate > 1%

**Diagnosis**:
```bash
# Check application logs
tail -100 backend/logs/application.log | grep ERROR

# Check circuit breaker status
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state | jq

# Check database connectivity
curl http://localhost:8080/actuator/health | jq '.components.db'
```

**Solutions**:
1. Check database connection pool
2. Verify circuit breaker configuration
3. Review error logs for patterns
4. Check external service availability

### Memory Issues

**Symptoms**: Memory usage > 85%

**Diagnosis**:
```bash
# Check heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq
curl http://localhost:8080/actuator/metrics/jvm.memory.max | jq

# Check for memory leaks
jmap -heap <pid>
```

**Solutions**:
1. Increase heap size: `-Xmx2g -Xms1g`
2. Check for memory leaks in code
3. Review cache sizes
4. Optimize object creation

### Connection Pool Exhaustion

**Symptoms**: Active connections = max pool size (20)

**Diagnosis**:
```bash
# Check connection metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq
curl http://localhost:8080/actuator/metrics/hikaricp.connections.idle | jq
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending | jq
```

**Solutions**:
1. Increase pool size in application.properties
2. Check for connection leaks
3. Optimize query execution time
4. Review transaction boundaries

---

## Optimization Workflow

### 1. Establish Baseline

```bash
# Run initial test
./run-comprehensive-load-test.sh

# Save results
cp results/summary-report-*.txt results/baseline-report.txt
```

### 2. Identify Bottlenecks

Review results and identify:
- Slowest endpoints
- Highest resource usage
- Error patterns
- Connection pool usage

### 3. Apply Optimizations

Common optimizations:
- Add database indexes
- Tune cache settings
- Adjust connection pool
- Optimize queries
- Enable compression

### 4. Re-test

```bash
# Run test again
./run-comprehensive-load-test.sh

# Compare results
diff results/baseline-report.txt results/summary-report-*.txt
```

### 5. Document Improvements

Record:
- What was changed
- Before/after metrics
- Performance improvement %
- Any trade-offs

---

## Advanced Testing

### Custom Test Scenarios

Create custom wrk scripts:

```lua
-- custom-test.lua
wrk.method = "POST"
wrk.body   = '{"category":"BASURA","description":"Test report"}'
wrk.headers["Content-Type"] = "application/json"
wrk.headers["Authorization"] = "Bearer YOUR_TOKEN"
```

Run with:
```bash
wrk -t10 -c50 -d60s -s custom-test.lua http://localhost:8080/api/reports
```

### JMeter GUI Testing

For visual test development:

```bash
# Open JMeter GUI
jmeter -t normal-load-test.jmx

# Modify test plan
# Add listeners for graphs
# Run test
# View results in real-time
```

### Continuous Load Testing

Run tests periodically:

```bash
# Add to crontab
0 2 * * * cd /path/to/backend/load-tests && ./run-comprehensive-load-test.sh
```

---

## Results Interpretation

### Good Results ✅

- Average response time < 500ms
- P95 response time < 1s
- Success rate > 99.9%
- Throughput > 100 req/s
- No errors in logs
- Stable memory usage
- Connection pool < 90%

### Warning Signs ⚠️

- Average response time 500ms - 1s
- P95 response time 1s - 2s
- Success rate 99% - 99.9%
- Occasional errors
- Memory usage 70% - 85%
- Connection pool 80% - 90%

### Critical Issues ❌

- Average response time > 1s
- P95 response time > 2s
- Success rate < 99%
- Frequent errors
- Memory usage > 85%
- Connection pool > 90%
- System crashes

---

## Next Steps After Testing

1. **Document Results**: Save all test reports
2. **Identify Optimizations**: List improvements needed
3. **Implement Changes**: Apply optimizations
4. **Re-test**: Verify improvements
5. **Update Baseline**: Set new performance baseline
6. **Monitor Production**: Use same metrics in production

---

## Support

For issues or questions:
- Review logs: `backend/logs/application.log`
- Check metrics: `http://localhost:8080/actuator`
- Consult design: `.kiro/specs/operational-excellence/design.md`
- Review requirements: `.kiro/specs/operational-excellence/requirements.md`

---

**Ready to test?** Run: `cd backend/load-tests && ./run-comprehensive-load-test.sh`

