# Performance Optimization Plan

**Based on**: Load Test Analysis (February 9, 2026)  
**Status**: ⏳ IN PROGRESS  
**Target**: Meet SLA targets at 100+ concurrent users

---

## Current Performance Issues

### Critical Issues (Blocking Production)
1. ❌ **Response times exceed 500ms at 100+ concurrent users**
   - Normal Load (50 users): 215ms avg ✅
   - Peak Load (100 users): 536ms avg ❌ (7% over target)
   - Stress Test (200 users): 817ms avg ❌ (63% over target)

2. ❌ **P95 latency exceeds 1000ms under peak load**
   - Normal Load: 362ms ✅
   - Peak Load: 1187ms ❌ (19% over target)
   - Stress Test: 1388ms ❌ (39% over target)

3. ❌ **P99 latency exceeds 1500ms under peak load**
   - Normal Load: 1082ms ✅
   - Peak Load: 1824ms ❌ (22% over target)
   - Stress Test: 2590ms ❌ (73% over target)

---

## Optimization Strategy

### Phase 1: Quick Wins (This Week) - Target: 30-40% improvement

#### 1.1 Database Connection Pool Tuning ⏳
**Current Configuration**:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

**Recommended Configuration**:
```properties
# Increase pool size for higher concurrency
spring.datasource.hikari.maximum-pool-size=40
spring.datasource.hikari.minimum-idle=10

# Optimize connection lifecycle
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# Enable connection validation
spring.datasource.hikari.validation-timeout=3000
spring.datasource.hikari.leak-detection-threshold=60000
```

**Expected Impact**: 15-20% throughput increase, 10-15% response time reduction

**Implementation**:
```bash
# Update application.properties
vim backend/src/main/resources/application.properties

# Restart application
./run-backend-locally.sh

# Re-run load tests
cd backend/load-tests
./run-comprehensive-load-test.sh
```

---

#### 1.2 Add Missing Database Indexes ⏳

**Identified Slow Queries** (from existing indexes):
- ✅ `idx_tareas_created_at` - EXISTS
- ✅ `idx_tareas_state_created` - EXISTS
- ✅ `idx_tareas_category_created` - EXISTS
- ✅ `idx_tareas_assigned_to` - EXISTS
- ✅ `idx_tareas_resolved_at` - EXISTS
- ✅ `idx_reportes_location_gist` - EXISTS

**Additional Indexes Needed**:
```sql
-- User authentication queries
CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);

-- Token validation queries
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active 
  ON refresh_tokens(user_id, revoked) WHERE revoked = FALSE;
CREATE INDEX IF NOT EXISTS idx_token_blacklist_lookup 
  ON token_blacklist(token_hash, expires_at);

-- Session management queries
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_active 
  ON user_sessions(user_id, active) WHERE active = TRUE;

-- Audit log queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_timestamp 
  ON audit_logs(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity 
  ON audit_logs(entity_type, entity_id);
```

**Expected Impact**: 20-30% query time reduction

**Implementation**:
```bash
# Create migration file
cat > backend/src/main/resources/db/migration/V20__performance_indexes.sql << 'EOF'
-- Performance optimization indexes
-- Add indexes here
EOF

# Apply migration
mvn flyway:migrate
```

---

#### 1.3 Optimize Cache Configuration ⏳

**Current Cache TTL** (from CacheConfig.java):
- Task Distribution: 5 minutes
- MTTR: 5 minutes
- Heatmap: 10 minutes
- Operator Metrics: 5 minutes

**Recommended Optimization**:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        cacheManager.setCaches(Arrays.asList(
            // Frequently accessed, rarely changes
            new ConcurrentMapCache("taskDistribution"),
            new ConcurrentMapCache("mttr"),
            new ConcurrentMapCache("operatorMetrics"),
            
            // Expensive to compute, can be stale
            new ConcurrentMapCache("heatmap"),
            
            // NEW: Cache user sessions
            new ConcurrentMapCache("userSessions"),
            
            // NEW: Cache configuration
            new ConcurrentMapCache("systemConfig")
        ));
        
        return cacheManager;
    }
    
    // Add cache eviction on data modification
    @CacheEvict(value = {"taskDistribution", "mttr", "operatorMetrics"}, allEntries = true)
    public void evictTaskCaches() {
        // Called when tasks are modified
    }
}
```

**Expected Impact**: 40-60% response time reduction for cached queries

---

### Phase 2: Medium-Term Optimizations (Next 2 Weeks) - Target: 20-30% improvement

#### 2.1 JVM Tuning ⏳

**Current JVM Settings** (default):
```bash
java -jar target/urban-cleaning-backend.jar
```

**Recommended JVM Settings**:
```bash
java -Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:ParallelGCThreads=4 \
  -XX:ConcGCThreads=2 \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -jar target/urban-cleaning-backend.jar
```

**Update run script**:
```bash
# Edit run-backend-locally.sh
vim run-backend-locally.sh

# Add JVM options
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
java $JAVA_OPTS -jar target/urban-cleaning-backend.jar
```

**Expected Impact**: 10-15% P99 latency reduction

---

#### 2.2 Enable Response Compression ⏳

**Add to application.properties**:
```properties
# Enable GZIP compression
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css
server.compression.min-response-size=1024

# Compression level (1-9, higher = more compression)
server.compression.level=6
```

**Expected Impact**: 20-30% bandwidth reduction, 5-10% response time improvement

---

#### 2.3 Implement Query Result Pagination ⏳

**Current Issue**: Large result sets loaded into memory

**Recommended Changes**:
```java
// ReportController.java
@GetMapping
public ResponseEntity<Page<ReportResponse>> getReports(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "createdAt,desc") String sort
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
    Page<Report> reports = reportRepository.findAll(pageable);
    return ResponseEntity.ok(reports.map(ReportResponse::fromEntity));
}

// TaskController.java
@GetMapping
public ResponseEntity<Page<TaskResponse>> getTasks(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String state
) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Task> tasks = state != null 
        ? taskRepository.findByState(TaskState.valueOf(state), pageable)
        : taskRepository.findAll(pageable);
    return ResponseEntity.ok(tasks.map(TaskResponse::fromEntity));
}
```

**Expected Impact**: 30-40% memory reduction, 15-20% response time improvement

---

### Phase 3: Long-Term Optimizations (Next Month) - Target: 50%+ improvement

#### 3.1 Implement Redis Caching ⏳

**Add Redis Dependency**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Configure Redis**:
```properties
# Redis configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000ms
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=2
```

**Update CacheConfig**:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("heatmap", 
                config.entryTtl(Duration.ofMinutes(10)))
            .build();
    }
}
```

**Expected Impact**: 50-70% response time reduction for cached queries

---

#### 3.2 Database Read Replicas ⏳

**Architecture**:
```
┌─────────────┐
│   Primary   │ ◄── Write Operations
│  Database   │
└──────┬──────┘
       │ Replication
       ├──────────────┬──────────────┐
       ▼              ▼              ▼
┌──────────┐   ┌──────────┐   ┌──────────┐
│ Replica 1│   │ Replica 2│   │ Replica 3│
└──────────┘   └──────────┘   └──────────┘
     ▲              ▲              ▲
     └──────────────┴──────────────┘
          Read Operations
```

**Implementation**:
```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        // Write operations
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://primary:5432/urbanclean")
            .build();
    }
    
    @Bean
    public DataSource replicaDataSource() {
        // Read operations
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://replica:5432/urbanclean")
            .build();
    }
    
    @Bean
    public DataSource routingDataSource() {
        RoutingDataSource routing = new RoutingDataSource();
        routing.setDefaultTargetDataSource(primaryDataSource());
        routing.setTargetDataSources(Map.of(
            "primary", primaryDataSource(),
            "replica", replicaDataSource()
        ));
        return routing;
    }
}
```

**Expected Impact**: 40-60% throughput increase for read operations

---

#### 3.3 Horizontal Scaling ⏳

**Architecture**:
```
                ┌──────────────┐
                │ Load Balancer│
                │   (Nginx)    │
                └──────┬───────┘
                       │
        ┏━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━┓
        ▼              ▼               ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│  App Server 1 │ │  App Server 2 │ │  App Server 3 │
└───────┬───────┘ └───────┬───────┘ └───────┬───────┘
        └─────────────────┴─────────────────┘
                          │
                    ┌─────▼──────┐
                    │  Database  │
                    │  (Primary) │
                    └────────────┘
```

**Nginx Configuration**:
```nginx
upstream backend {
    least_conn;
    server app1:8080 weight=1;
    server app2:8080 weight=1;
    server app3:8080 weight=1;
}

server {
    listen 80;
    
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

**Expected Impact**: Linear scalability (3x servers = 3x throughput)

---

## Implementation Checklist

### Week 1: Quick Wins
- [ ] 1.1 Increase database connection pool to 40
- [ ] 1.2 Add missing database indexes
- [ ] 1.3 Optimize cache configuration
- [ ] 1.4 Re-run load tests
- [ ] 1.5 Measure improvements

### Week 2: Medium-Term
- [ ] 2.1 Tune JVM settings (G1GC, heap size)
- [ ] 2.2 Enable GZIP compression
- [ ] 2.3 Implement pagination for large result sets
- [ ] 2.4 Re-run load tests
- [ ] 2.5 Measure improvements

### Week 3-4: Long-Term
- [ ] 3.1 Set up Redis cache
- [ ] 3.2 Configure database read replicas
- [ ] 3.3 Implement horizontal scaling
- [ ] 3.4 Final load test validation
- [ ] 3.5 Document final performance metrics

---

## Success Criteria

### Target Metrics (After Optimization)

| Metric | Current (100 users) | Target | Status |
|--------|---------------------|--------|--------|
| **Avg Response Time** | 536ms | < 400ms | ⏳ |
| **P95 Response Time** | 1187ms | < 800ms | ⏳ |
| **P99 Response Time** | 1824ms | < 1200ms | ⏳ |
| **Throughput** | 186 req/s | > 250 req/s | ⏳ |
| **Error Rate** | 0% | < 0.1% | ✅ |

### Validation Process
1. ✅ Implement optimization
2. ⏳ Run load test (100 concurrent users)
3. ⏳ Measure metrics
4. ⏳ Compare against targets
5. ⏳ Document results
6. ⏳ Repeat until targets met

---

## Monitoring and Alerting

### Add Performance Alerts

**Create AlertService enhancements**:
```java
@Service
public class AlertService {
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkPerformanceMetrics() {
        // Check average response time
        double avgResponseTime = metricsService.getAverageResponseTime();
        if (avgResponseTime > 1000) {
            sendAlert("High response time: " + avgResponseTime + "ms");
        }
        
        // Check error rate
        double errorRate = metricsService.getErrorRate();
        if (errorRate > 0.01) {
            sendAlert("High error rate: " + (errorRate * 100) + "%");
        }
        
        // Check database connections
        int activeConnections = metricsService.getActiveConnections();
        if (activeConnections > 36) { // 90% of 40
            sendAlert("High database connection usage: " + activeConnections);
        }
    }
}
```

---

## Next Steps

1. ⏳ **Implement Phase 1 optimizations** (this week)
2. ⏳ **Re-run load tests** to measure improvements
3. ⏳ **Document results** in LOAD_TEST_ANALYSIS.md
4. ⏳ **Proceed to Phase 2** if targets not met
5. ⏳ **Final validation** before production deployment

---

**Status**: ⏳ READY FOR IMPLEMENTATION  
**Priority**: 🔴 HIGH (Blocking production)  
**Owner**: DevOps Team  
**Due Date**: February 16, 2026
