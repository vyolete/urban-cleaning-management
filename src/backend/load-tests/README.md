# Load Testing Guide

This directory contains load testing scripts and configurations for the Urban Cleaning Management System.

## Prerequisites

### Option 1: Using Bash Script (Recommended for Quick Tests)
- `curl` command-line tool
- `bc` calculator (for calculations)
- `python3` (optional, for JSON formatting)
- Bash shell

### Option 2: Using Apache JMeter (Recommended for Comprehensive Tests)
- Apache JMeter 5.6 or later
- Java 8 or later

## Quick Start with Bash Script

### 1. Start the Backend Server

```bash
cd backend
mvn spring-boot:run
```

### 2. Run Tests

```bash
cd backend/load-tests

# Test Actuator endpoints
./run-load-test.sh actuator

# Test Performance Metrics endpoints
./run-load-test.sh metrics

# Run simplified load test
./run-load-test.sh normal

# Run all tests
./run-load-test.sh all
```

### 3. Configure Test Parameters

You can customize the tests using environment variables:

```bash
# Custom base URL
BASE_URL=http://localhost:8080 ./run-load-test.sh all

# Custom credentials
ADMIN_USERNAME=admin ADMIN_PASSWORD=admin123 ./run-load-test.sh metrics
```

## Using Apache JMeter

### 1. Install JMeter

**macOS (using Homebrew):**
```bash
brew install jmeter
```

**Linux:**
```bash
# Download from https://jmeter.apache.org/download_jmeter.cgi
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
cd apache-jmeter-5.6.3/bin
```

**Windows:**
- Download from https://jmeter.apache.org/download_jmeter.cgi
- Extract the ZIP file
- Run `bin/jmeter.bat`

### 2. Run JMeter Tests

**GUI Mode (for test development):**
```bash
jmeter -t normal-load-test.jmx
```

**CLI Mode (for actual load testing):**
```bash
# Normal Load Test (50 users, 10 minutes)
jmeter -n -t normal-load-test.jmx -l results/normal-load-results.jtl -e -o results/normal-load-report

# View results
open results/normal-load-report/index.html
```

### 3. Customize JMeter Tests

Edit the `.jmx` files to customize:
- Number of threads (users)
- Ramp-up period
- Test duration
- Target endpoints
- Request parameters

## Test Scenarios

### Normal Load Test
- **Users**: 50 concurrent users
- **Duration**: 10 minutes
- **Ramp-up**: 2 minutes
- **Operations**: 70% reads, 30% writes
- **Target**: Validate system under normal conditions

**Expected Results:**
- Average response time < 500ms for simple queries
- Average response time < 2s for analytics queries
- Success rate > 99.9%
- Error rate < 0.1%

### Peak Load Test
- **Users**: 100 concurrent users
- **Duration**: 5 minutes
- **Ramp-up**: 1 minute
- **Operations**: 60% reads, 40% writes
- **Target**: Validate system under peak traffic

### Stress Test
- **Users**: 200 concurrent users
- **Duration**: 3 minutes
- **Ramp-up**: 30 seconds
- **Operations**: 50% reads, 50% writes
- **Target**: Identify system breaking point

## Monitored Endpoints

### Read Operations (70%)
1. `GET /api/reports` - List reports
2. `GET /api/tasks` - List tasks
3. `GET /api/analytics/tasks/distribution/category` - Analytics

### Write Operations (30%)
1. `POST /api/reports` - Create report
2. `PUT /api/tasks/{id}/state` - Update task state

## Performance Targets

### Simple Queries (GET /api/reports, GET /api/tasks)
- Average: < 500ms
- P95: < 1s
- P99: < 1.5s

### Analytics Queries (GET /api/analytics/*)
- Average: < 2s
- P95: < 3s
- P99: < 4s

### Write Operations (POST, PUT)
- Average: < 1s
- P95: < 2s
- P99: < 3s

### System Metrics
- Success rate: > 99.9%
- Error rate: < 0.1%
- Throughput: > 100 requests/second
- Database connections: < 90% of pool size

## Monitoring During Tests

### 1. Actuator Endpoints

**Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

**Metrics:**
```bash
curl http://localhost:8080/actuator/metrics
```

**Prometheus Format:**
```bash
curl http://localhost:8080/actuator/prometheus
```

### 2. Performance Metrics API

**Aggregated Metrics:**
```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/admin/metrics/performance?range=1h
```

**Response Time:**
```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/admin/metrics/response-time
```

**Error Rate:**
```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/admin/metrics/error-rate
```

**Active Connections:**
```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/admin/metrics/connections
```

**Memory Usage:**
```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/admin/metrics/memory
```

**CPU Usage:**
```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/api/admin/metrics/cpu
```

### 3. Database Monitoring

Monitor HikariCP connection pool:
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.idle
```

## Analyzing Results

### JMeter Results

JMeter generates HTML reports with:
- Response time graphs
- Throughput over time
- Error percentage
- Percentile response times (p50, p95, p99)
- Request distribution

### Key Metrics to Check

1. **Response Time**
   - Average, median, p95, p99
   - Should meet performance targets

2. **Throughput**
   - Requests per second
   - Should be > 100 req/s

3. **Error Rate**
   - Percentage of failed requests
   - Should be < 0.1%

4. **Resource Usage**
   - CPU usage (should be < 80%)
   - Memory usage (should be < 85%)
   - Database connections (should be < 90% of pool)

## Troubleshooting

### High Response Times

1. Check database query performance:
   ```sql
   EXPLAIN ANALYZE SELECT * FROM tareas WHERE state = 'PENDIENTE';
   ```

2. Verify indexes are being used:
   ```sql
   SELECT * FROM pg_stat_user_indexes WHERE schemaname = 'public';
   ```

3. Check cache hit rates:
   ```bash
   curl http://localhost:8080/actuator/metrics/cache.gets
   ```

### High Error Rates

1. Check application logs:
   ```bash
   tail -f backend/logs/application.log
   ```

2. Check database connections:
   ```bash
   curl http://localhost:8080/actuator/metrics/hikaricp.connections
   ```

3. Verify circuit breaker status:
   ```bash
   curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
   ```

### Memory Issues

1. Check heap usage:
   ```bash
   curl http://localhost:8080/actuator/metrics/jvm.memory.used
   ```

2. Check for memory leaks:
   ```bash
   jmap -heap <pid>
   ```

3. Increase heap size if needed:
   ```bash
   java -Xmx2g -Xms1g -jar target/urban-cleaning-backend.jar
   ```

## Best Practices

1. **Warm-up Period**: Run a short warm-up test before actual load testing
2. **Realistic Data**: Use production-like data volumes
3. **Monitor Resources**: Watch CPU, memory, and database during tests
4. **Incremental Load**: Start with low load and gradually increase
5. **Baseline Metrics**: Establish baseline before optimization
6. **Repeat Tests**: Run tests multiple times for consistency
7. **Document Results**: Keep records of all test runs

## Results Directory

Test results are saved in the `results/` directory:
- `*.jtl` - JMeter test results (CSV format)
- `*-report/` - HTML reports
- `load-test-report-*.txt` - Bash script reports

## Next Steps

After running load tests:

1. **Analyze Results**: Review response times, throughput, and error rates
2. **Identify Bottlenecks**: Find slow queries, high CPU usage, etc.
3. **Optimize**: Add indexes, tune cache, adjust pool sizes
4. **Re-test**: Verify improvements with another test run
5. **Document**: Record baseline and optimized metrics

## Support

For issues or questions:
- Check application logs: `backend/logs/application.log`
- Review Actuator metrics: `http://localhost:8080/actuator`
- Consult design document: `.kiro/specs/operational-excellence/design.md`
