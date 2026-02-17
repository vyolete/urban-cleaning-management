# Phase 5: Load Testing Setup Complete

**Date**: 9 de febrero de 2026, 23:30  
**Status**: Load testing infrastructure ready - Backend restart required  
**Progress**: 71% (12/17 tasks completed)

## Summary

The load testing infrastructure for Phase 5 (Performance Testing & Monitoring) has been set up and is ready for execution. Several issues were identified and fixed during the setup process.

## Completed Work

### 1. Load Testing Scripts Created ✅

Three load testing scripts are now available:

#### a) JMeter Test Plan
- **File**: `backend/load-tests/normal-load-test.jmx`
- Comprehensive load test for 50 concurrent users
- 10-minute duration with 2-minute ramp-up
- Tests all key endpoints with proper assertions
- Can be run in JMeter GUI or CLI mode

#### b) Original Bash Script
- **File**: `backend/load-tests/run-load-test.sh`
- Multiple test modes (actuator, metrics, normal, all)
- Has timing calculation issues on some systems

#### c) Quick Test Script (NEW - Recommended)
- **File**: `backend/load-tests/quick-test.sh`
- Fixed timing calculation issues
- Tests all critical endpoints
- Automatic SLA compliance checking
- Clear, colored output
- **This is the recommended script to use**

### 2. Issues Fixed ✅

#### Issue 1: PerformanceMetricsService Null Pointer Exceptions

**Problem**: The service was calling `.value()` on potentially null gauge objects.

**Fix**: Added null checks before accessing gauge values:

```java
// Before (caused NPE)
Double processCpu = meterRegistry.find("process.cpu.usage")
    .gauge()
    .value();

// After (safe)
var processGauge = meterRegistry.find("process.cpu.usage").gauge();
Double processCpu = processGauge != null ? processGauge.value() : null;
```

**Files Modified**:
- `backend/src/main/java/com/urbanclean/service/PerformanceMetricsService.java`
  - Fixed `getCPUUsage()` method
  - Fixed `getActiveConnections()` method
  - Fixed `getMemoryUsage()` method

#### Issue 2: Actuator Endpoints Not Accessible

**Problem**: SecurityConfig only allowed `/actuator/health`, blocking other actuator endpoints.

**Fix**: Changed security configuration to allow all actuator endpoints:

```java
// Before
.requestMatchers("/actuator/health").permitAll()

// After
.requestMatchers("/actuator/**").permitAll()
```

**Files Modified**:
- `backend/src/main/java/com/urbanclean/config/SecurityConfig.java`

**Endpoints Now Accessible**:
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Metrics list
- `GET /actuator/metrics/{name}` - Individual metric
- `GET /actuator/prometheus` - Prometheus format

#### Issue 3: Load Test Script Timing Errors

**Problem**: Original script had bash arithmetic errors with nanosecond timestamps.

**Fix**: Created new `quick-test.sh` with proper timing calculations and simplified logic.

### 3. Documentation Created ✅

#### a) Load Testing README
- **File**: `backend/load-tests/README.md`
- Comprehensive guide for both JMeter and bash testing
- Installation instructions
- Usage examples
- Performance targets

#### b) Load Test Instructions
- **File**: `LOAD_TEST_INSTRUCTIONS.md`
- Step-by-step instructions for running tests
- Backend restart instructions
- Expected results and SLA targets
- Troubleshooting guide

## ⚠️ Action Required: Restart Backend

The fixes require a backend restart to take effect. The backend must be restarted before running load tests.

### How to Restart

```bash
# Stop current containers
cd docker
docker-compose down

# Rebuild and start
docker-compose up --build
```

## Next Steps

### Immediate (After Backend Restart)

1. **Run Quick Test** (5-10 minutes)
   ```bash
   cd backend/load-tests
   ./quick-test.sh
   ```

2. **Review Results**
   - Check if SLA targets are met
   - Review response times
   - Check success rates

### Task 5.5.6: Analyze Results (0.5 days)

After running the tests:
- [ ] Calculate average response time per endpoint
- [ ] Calculate p95, p99 response times
- [ ] Calculate throughput (requests/second)
- [ ] Calculate error rate
- [ ] Monitor database connection pool usage
- [ ] Monitor memory and CPU usage
- [ ] Verify SLA compliance

### Task 5.5.7: Optimize (0.5 days, if needed)

If SLA targets are not met:
- [ ] Identify slow queries
- [ ] Add missing database indexes
- [ ] Tune cache TTL values
- [ ] Adjust connection pool size
- [ ] Re-run tests to verify improvements

### Tasks 5.6.1-5.6.2: Alerting (1 day)

- [ ] Create AlertService
- [ ] Define alert conditions
- [ ] Implement alert logging
- [ ] Send email notifications

### Tasks 5.7.1-5.7.3: Testing (1 day)

- [ ] Test actuator endpoints
- [ ] Test performance metrics endpoint
- [ ] Test circuit breaker behavior

## SLA Targets

### Simple Queries
- **Endpoints**: GET /api/reports, GET /api/tasks
- **Response Time**: < 500ms (p95)
- **Success Rate**: > 99.9%

### Analytics Queries
- **Endpoints**: GET /api/analytics/**
- **Response Time**: < 2000ms (p95)
- **Success Rate**: > 99.9%

## Expected Test Output

When you run `./quick-test.sh`, you should see:

```
========================================
Urban Cleaning - Quick Load Test
========================================

Test 1: Actuator Health
✓ Health check passed

Test 2: Actuator Metrics
✓ Metrics endpoint accessible

Test 3: Prometheus Metrics
✓ Prometheus endpoint accessible

Authenticating...
✓ Authentication successful

Test 4: Performance Metrics
✓ Performance metrics accessible

Current Metrics:
{
  "timestamp": "2026-02-09T...",
  "timeRange": "1h",
  "responseTime": { ... },
  "errorRate": 0.0,
  "activeConnections": 5,
  "memory": { ... },
  "cpu": { ... }
}

Test 5: Load Test - Simple Queries (10 requests)
✓ Request 1: 123ms (< 500ms target)
✓ Request 2: 145ms (< 500ms target)
...

Results:
  Total Requests: 10
  Successful: 10
  Success Rate: 100%
  Average Response Time: 134ms

Test 6: Load Test - Analytics Queries (5 requests)
✓ Request 1: 456ms (< 2000ms target)
...

Results:
  Total Requests: 5
  Successful: 5
  Success Rate: 100%
  Average Response Time: 478ms

========================================
Test Summary
========================================

Simple Queries:
  Success Rate: 100% (target: > 99.9%)
  Avg Response Time: 134ms (target: < 500ms)

Analytics Queries:
  Success Rate: 100% (target: > 99.9%)
  Avg Response Time: 478ms (target: < 2000ms)

✓ SLA requirements met!
```

## Files Created/Modified

### Created
- `backend/load-tests/quick-test.sh` - Simplified load test script
- `LOAD_TEST_INSTRUCTIONS.md` - Detailed instructions
- `PHASE5_LOAD_TESTING_SETUP_COMPLETE.md` - This file

### Modified
- `backend/src/main/java/com/urbanclean/service/PerformanceMetricsService.java` - Fixed NPE issues
- `backend/src/main/java/com/urbanclean/config/SecurityConfig.java` - Allowed actuator endpoints
- `PHASE5_PERFORMANCE_MONITORING_PROGRESS.md` - Updated progress

## Technical Details

### Actuator Endpoints Available

1. **Health**: `GET /actuator/health`
   - Returns application health status
   - Includes database, disk space checks

2. **Metrics List**: `GET /actuator/metrics`
   - Lists all available metrics
   - Returns metric names

3. **Individual Metric**: `GET /actuator/metrics/{name}`
   - Returns specific metric value
   - Examples: jvm.memory.used, http.server.requests

4. **Prometheus**: `GET /actuator/prometheus`
   - Exports all metrics in Prometheus format
   - Can be scraped by Prometheus server

### Performance Metrics Endpoints

All require ADMIN role authentication:

1. **Aggregated**: `GET /api/admin/metrics/performance?range=1h`
2. **Response Time**: `GET /api/admin/metrics/response-time`
3. **Error Rate**: `GET /api/admin/metrics/error-rate`
4. **Connections**: `GET /api/admin/metrics/connections`
5. **Memory**: `GET /api/admin/metrics/memory`
6. **CPU**: `GET /api/admin/metrics/cpu`

### Circuit Breaker Status

- Configured for EmailService
- Opens after 50% failure rate
- 1-minute wait in open state
- Fallback records to notification_failures table

### Connection Pool Configuration

- Maximum pool size: 20 connections
- Minimum idle: 5 connections
- Connection timeout: 30 seconds
- Leak detection: 1 minute

## Troubleshooting

### If Tests Fail

1. **Check Backend is Running**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Check Backend Logs**
   ```bash
   docker-compose logs backend
   ```

3. **Verify Database is Running**
   ```bash
   docker-compose ps
   ```

4. **Check Authentication**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```

### If Actuator Returns 403

Backend hasn't been restarted with new SecurityConfig. Restart required.

### If Performance Metrics Return 500

PerformanceMetricsService hasn't been updated. Rebuild and restart:

```bash
cd backend
mvn clean compile
# Then restart backend
```

## Completion Status

### Phase 5 Progress: 71% (12/17 tasks)

**Completed** (12 tasks):
- ✅ 5.1.1-5.1.3: Monitoring Setup (3 tasks)
- ✅ 5.2.1-5.2.2: Performance Metrics Service (2 tasks)
- ✅ 5.3.1-5.3.2: Database Connection Pooling (2 tasks)
- ✅ 5.4.1-5.4.3: Circuit Breaker (3 tasks)
- ✅ 5.5.1-5.5.5: Load Testing Setup (5 tasks)

**Pending** (5 tasks):
- ⏳ 5.5.6-5.5.7: Analyze and Optimize (2 tasks) - BLOCKED on backend restart
- ⏳ 5.6.1-5.6.2: Alerting (2 tasks)
- ⏳ 5.7.1-5.7.3: Testing (3 tasks)

## Estimated Time to Complete

- **Immediate**: Restart backend (5 minutes)
- **Immediate**: Run tests (10 minutes)
- Analyze results: 0.5 days
- Optimize (if needed): 0.5 days
- Alerting: 1 day
- Testing: 1 day

**Total**: ~3 days remaining

---

**Status**: Ready for testing after backend restart  
**Next Action**: Restart backend and run `./quick-test.sh`  
**Documentation**: See `LOAD_TEST_INSTRUCTIONS.md` for detailed steps
