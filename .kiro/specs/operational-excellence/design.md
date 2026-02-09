# Design Document: Operational Excellence

## Overview

This document provides the technical design for implementing operational excellence features in the Urban Cleaning Management System. The design covers 6 major modules that complete all pending IDRQ requirements:

1. **Notification System Completion** - Event-driven email notifications with preferences
2. **Analytics Dashboard** - KPIs, MTTR, heatmaps, and operator metrics
3. **Extended System Configuration** - Dynamic configuration for tokens and deduplication
4. **Enhanced Session Management** - Refresh tokens, multi-device sessions, token blacklist
5. **Performance Testing** - Load testing and performance monitoring
6. **API Documentation** - OpenAPI/Swagger documentation

### Design Principles

- **Asynchronous Processing**: Use Spring Events for non-blocking operations
- **Caching Strategy**: Implement Spring Cache for expensive analytics queries
- **Database Optimization**: Use PostGIS spatial functions and proper indexing
- **Security First**: Hash sensitive tokens, implement token rotation
- **Scalability**: Design for 10,000+ concurrent users and 100,000+ tasks
- **Maintainability**: Follow project standards, comprehensive testing

---

## MODULE 1: NOTIFICATION SYSTEM COMPLETION

### Architecture Overview

```
TaskService → ApplicationEventPublisher → @EventListener → EmailService → SMTP Server
                                                ↓
                                    NotificationPreferenceService
                                                ↓
                                    NotificationFailureRepository
```

### Components

#### 1.1 NotificationPreferenceService

**Responsibility**: Manage user notification preferences

**Methods**:
```java
public class NotificationPreferenceService {
    NotificationPreference getPreferences(UUID userId);
    NotificationPreference updatePreferences(UUID userId, NotificationPreferenceRequest request);
    boolean isNotificationEnabled(UUID userId, NotificationType type);
    NotificationPreference createDefaultPreferences(UUID userId);
}
```

**Business Logic**:
- On user registration, create default preferences with all types enabled
- Check preferences before sending any notification
- Include preferences in GDPR data export

#### 1.2 EmailService (Enhanced)

**Responsibility**: Send emails asynchronously with retry logic

**Methods**:
```java
public class EmailService {
    @Async
    void sendTaskAssignmentEmail(UUID taskId, UUID operatorId);
    
    @Async
    void sendTaskResolvedEmail(UUID taskId, UUID citizenId);
    
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 60000, multiplier = 5))
    void sendEmailWithRetry(String to, String subject, String htmlContent);
    
    @Recover
    void handleEmailFailure(Exception e, String to, String subject, String htmlContent);
}
```

**Retry Strategy**:
- Attempt 1: Immediate
- Attempt 2: After 1 minute
- Attempt 3: After 5 minutes
- Attempt 4: After 15 minutes
- If all fail: Record in notification_failures table

**Template Engine**: Thymeleaf for HTML email templates

#### 1.3 Event Listeners

**TaskAssignmentListener**:
```java
@Component
public class TaskAssignmentListener {
    @EventListener
    @Async
    public void handleTaskAssigned(TaskAssignedEvent event) {
        if (notificationPreferenceService.isNotificationEnabled(
            event.getOperatorId(), NotificationType.TASK_ASSIGNED)) {
            emailService.sendTaskAssignmentEmail(event.getTaskId(), event.getOperatorId());
        }
    }
}
```

#### 1.4 UnsubscribeController

**Responsibility**: Handle email unsubscribe requests

**Endpoints**:
- `GET /api/notifications/unsubscribe?token={token}` - Process unsubscribe
- `GET /api/notifications/unsubscribe/confirm` - Show confirmation page

**Token Format**: JWT with claims: userId, notificationType, expiration (30 days)


### Data Models

#### notification_preferences Table

```sql
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    task_assigned BOOLEAN DEFAULT TRUE,
    task_resolved BOOLEAN DEFAULT TRUE,
    task_reopened BOOLEAN DEFAULT TRUE,
    report_created BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

CREATE INDEX idx_notification_preferences_user ON notification_preferences(user_id);
```

#### notification_failures Table

```sql
CREATE TABLE notification_failures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    failure_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_failures_user ON notification_failures(user_id);
CREATE INDEX idx_notification_failures_attempted ON notification_failures(attempted_at);
```

### Email Templates

**Location**: `src/main/resources/templates/email/`

**Templates**:
- `task-assigned.html` - Task assignment notification
- `task-resolved.html` - Task resolution notification
- `task-reopened.html` - Task reopened notification
- `report-created.html` - Report creation confirmation

**Template Variables**:
- `userName` - Recipient name
- `taskId` - Task identifier
- `category` - Task category
- `location` - Task location
- `priorityScore` - Priority score
- `unsubscribeUrl` - Unsubscribe link


---

## MODULE 2: ANALYTICS DASHBOARD

### Architecture Overview

```
AnalyticsController → AnalyticsService → ReportRepository/TaskRepository
                            ↓
                      @Cacheable (5-10 min TTL)
                            ↓
                      PostgreSQL + PostGIS
```

### Components

#### 2.1 AnalyticsService

**Responsibility**: Provide aggregated analytics data with caching

**Methods**:
```java
public class AnalyticsService {
    @Cacheable(value = "taskDistribution", key = "#filters")
    TaskDistributionResponse getTaskDistributionByCategory(AnalyticsFilters filters);
    
    @Cacheable(value = "taskDistribution", key = "#filters")
    TaskDistributionResponse getTaskDistributionByState(AnalyticsFilters filters);
    
    @Cacheable(value = "mttr", key = "#filters")
    MTTRResponse calculateMTTR(AnalyticsFilters filters);
    
    @Cacheable(value = "heatmap", key = "#filters")
    HeatmapResponse generateHeatmap(HeatmapFilters filters);
    
    @Cacheable(value = "operatorMetrics", key = "#filters")
    OperatorPerformanceResponse getOperatorPerformance(AnalyticsFilters filters);
}
```

#### 2.2 HeatmapService

**Responsibility**: Generate geographic heatmap data using PostGIS

**Methods**:
```java
public class HeatmapService {
    List<HeatmapCell> generateHeatmap(HeatmapFilters filters);
    List<HeatmapCell> aggregateByGrid(double cellSizeMeters);
    List<HeatmapCell> normalizeIntensity(List<HeatmapCell> cells);
}
```

**PostGIS Query Strategy**:
```sql
SELECT 
    ST_Y(ST_Centroid(grid)) as latitude,
    ST_X(ST_Centroid(grid)) as longitude,
    COUNT(*) as intensity
FROM (
    SELECT ST_SnapToGrid(location, :cellSize) as grid
    FROM reportes
    WHERE created_at BETWEEN :startDate AND :endDate
    AND (:category IS NULL OR category = :category)
) grouped
GROUP BY grid
ORDER BY intensity DESC
LIMIT 1000;
```


#### 2.3 AnalyticsController

**Endpoints**:
- `GET /api/analytics/tasks/distribution/category` - Task distribution by category
- `GET /api/analytics/tasks/distribution/state` - Task distribution by state
- `GET /api/analytics/tasks/mttr` - Mean Time To Resolution
- `GET /api/analytics/tasks/resolution-time-distribution` - Resolution time histogram
- `GET /api/analytics/heatmap` - Geographic heatmap data
- `GET /api/analytics/operators/performance` - Operator performance metrics

**Common Query Parameters**:
- `startDate` (ISO 8601) - Filter start date
- `endDate` (ISO 8601) - Filter end date
- `zoneId` (UUID) - Filter by geographic zone
- `category` (String) - Filter by category

### Data Models

#### AnalyticsFilters DTO

```java
public class AnalyticsFilters {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UUID zoneId;
    private String category;
    private Integer page;
    private Integer size;
}
```

#### TaskDistributionResponse DTO

```java
public class TaskDistributionResponse {
    private List<DistributionItem> distribution;
    private Integer totalTasks;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    public static class DistributionItem {
        private String label;
        private Integer count;
        private Double percentage;
    }
}
```

#### MTTRResponse DTO

```java
public class MTTRResponse {
    private Double mttrHours;
    private Integer tasksResolved;
    private Double averagePriorityScore;
    private Map<String, Integer> resolutionTimeDistribution;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
```


#### HeatmapResponse DTO

```java
public class HeatmapResponse {
    private List<HeatmapCell> cells;
    private Integer totalReports;
    private Double cellSizeMeters;
    private String aggregationLevel;
    
    public static class HeatmapCell {
        private Double latitude;
        private Double longitude;
        private Integer intensity;
        private Double normalizedIntensity; // 0.0 to 1.0
    }
}
```

#### OperatorPerformanceResponse DTO

```java
public class OperatorPerformanceResponse {
    private List<OperatorMetrics> operators;
    private Integer totalOperators;
    private Integer page;
    private Integer totalPages;
    
    public static class OperatorMetrics {
        private UUID operatorId;
        private String username;
        private Integer tasksResolved;
        private Double averageResolutionTimeHours;
        private Integer tasksInProgress;
        private Integer tasksReopened;
        private LocalDateTime activeSince;
    }
}
```

### Caching Strategy

**Cache Configuration**:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "taskDistribution",
            "mttr",
            "heatmap",
            "operatorMetrics"
        );
    }
}
```

**TTL Configuration**:
- `taskDistribution`: 5 minutes
- `mttr`: 5 minutes
- `heatmap`: 10 minutes (computationally expensive)
- `operatorMetrics`: 5 minutes

**Cache Eviction**: Automatic on TTL expiration, manual eviction not required


### Database Optimization

**Required Indexes**:
```sql
-- Analytics performance indexes
CREATE INDEX idx_tareas_created_at ON tareas(created_at);
CREATE INDEX idx_tareas_state_created ON tareas(state, created_at);
CREATE INDEX idx_tareas_category_created ON tareas(category, created_at);
CREATE INDEX idx_tareas_assigned_to ON tareas(assigned_to);
CREATE INDEX idx_tareas_resolved_at ON tareas(resolved_at) WHERE resolved_at IS NOT NULL;

-- Spatial indexes for heatmap
CREATE INDEX idx_reportes_location_gist ON reportes USING GIST(location);
CREATE INDEX idx_reportes_created_location ON reportes(created_at, category) INCLUDE (location);
```

**Query Optimization**:
- Use `EXPLAIN ANALYZE` to verify index usage
- Limit result sets to prevent memory issues
- Use database aggregation functions instead of application-level aggregation
- Implement pagination for large result sets

---

## MODULE 3: EXTENDED SYSTEM CONFIGURATION

### Architecture Overview

```
ConfigController → ConfigService → SystemConfigRepository
                        ↓
                  @CacheEvict on update
                        ↓
                  AuditLogService
```

### Components

#### 3.1 ConfigService (Enhanced)

**Responsibility**: Manage dynamic system configuration

**New Methods**:
```java
public class ConfigService {
    // Existing methods...
    
    TokenExpirationConfig getTokenExpirationConfig();
    TokenExpirationConfig updateTokenExpirationConfig(TokenExpirationRequest request);
    
    DuplicateDetectionConfig getDuplicateDetectionConfig();
    DuplicateDetectionConfig updateDuplicateDetectionConfig(DuplicateDetectionRequest request);
    
    void auditConfigChange(String configType, String oldValue, String newValue, UUID userId);
}
```


#### 3.2 JwtTokenProvider (Enhanced)

**Responsibility**: Generate tokens with configurable expiration

**Modified Methods**:
```java
public class JwtTokenProvider {
    public String generateAccessToken(Authentication authentication) {
        TokenExpirationConfig config = configService.getTokenExpirationConfig();
        long expirationMs = config.getAccessTokenExpirationMinutes() * 60 * 1000;
        // Generate token with dynamic expiration
    }
    
    public String generateRefreshToken(Authentication authentication) {
        TokenExpirationConfig config = configService.getTokenExpirationConfig();
        long expirationMs = config.getRefreshTokenExpirationDays() * 24 * 60 * 60 * 1000;
        // Generate token with dynamic expiration
    }
}
```

#### 3.3 DeduplicationService (Enhanced)

**Responsibility**: Detect duplicates with configurable parameters

**Modified Methods**:
```java
public class DeduplicationService {
    public boolean isDuplicate(Report newReport) {
        DuplicateDetectionConfig config = configService.getDuplicateDetectionConfig();
        
        LocalDateTime timeThreshold = LocalDateTime.now()
            .minusHours(config.getTimeWindowHours());
        
        List<Report> nearbyReports = reportRepository.findNearbyReports(
            newReport.getLocation(),
            config.getDetectionRadiusMeters(),
            timeThreshold
        );
        
        if (config.isRequireSameCategory()) {
            nearbyReports = nearbyReports.stream()
                .filter(r -> r.getCategory().equals(newReport.getCategory()))
                .collect(Collectors.toList());
        }
        
        return !nearbyReports.isEmpty();
    }
}
```

### Data Models

#### system_config Table (Extended)

```sql
ALTER TABLE system_config ADD COLUMN IF NOT EXISTS config_type VARCHAR(50);
ALTER TABLE system_config ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_system_config_type ON system_config(config_type);
CREATE INDEX idx_system_config_effective ON system_config(effective_from);
```


#### TokenExpirationConfig Entity

```java
@Entity
@Table(name = "system_config")
public class TokenExpirationConfig {
    @Id
    private UUID id;
    
    @Column(name = "config_type")
    private String configType = "TOKEN_EXPIRATION";
    
    @Column(name = "access_token_expiration_minutes")
    private Integer accessTokenExpirationMinutes = 15;
    
    @Column(name = "refresh_token_expiration_days")
    private Integer refreshTokenExpirationDays = 7;
    
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
}
```

#### DuplicateDetectionConfig Entity

```java
@Entity
@Table(name = "system_config")
public class DuplicateDetectionConfig {
    @Id
    private UUID id;
    
    @Column(name = "config_type")
    private String configType = "DUPLICATE_DETECTION";
    
    @Column(name = "detection_radius_meters")
    private Integer detectionRadiusMeters = 100;
    
    @Column(name = "time_window_hours")
    private Integer timeWindowHours = 24;
    
    @Column(name = "require_same_category")
    private Boolean requireSameCategory = true;
    
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
}
```

### Validation Rules

**TokenExpirationConfig Validation**:
- `accessTokenExpirationMinutes`: 5-60 minutes
- `refreshTokenExpirationDays`: 1-30 days
- `accessTokenExpiration < refreshTokenExpiration` (always)

**DuplicateDetectionConfig Validation**:
- `detectionRadiusMeters`: 10-1000 meters
- `timeWindowHours`: 1-168 hours (7 days)
- `requireSameCategory`: boolean


---

## MODULE 4: ENHANCED SESSION MANAGEMENT

### Architecture Overview

```
AuthController → AuthService → RefreshTokenService
                                      ↓
                              RefreshTokenRepository
                                      ↓
                              TokenBlacklistRepository
                                      ↓
                              UserSessionRepository
```

### Components

#### 4.1 RefreshTokenService

**Responsibility**: Manage refresh token lifecycle

**Methods**:
```java
public class RefreshTokenService {
    RefreshToken createRefreshToken(UUID userId, String deviceFingerprint);
    RefreshToken validateRefreshToken(String token);
    void revokeRefreshToken(String token);
    void revokeAllUserTokens(UUID userId);
    void revokeAllUserTokensExceptCurrent(UUID userId, String currentToken);
    RefreshToken rotateRefreshToken(String oldToken);
    void cleanupExpiredTokens();
}
```

**Token Rotation Strategy**:
1. Validate old refresh token
2. Generate new refresh token
3. Invalidate old refresh token (add to blacklist)
4. Return new token pair (access + refresh)

#### 4.2 TokenBlacklistService

**Responsibility**: Manage revoked tokens

**Methods**:
```java
public class TokenBlacklistService {
    void addToBlacklist(String token, LocalDateTime expiresAt);
    boolean isBlacklisted(String token);
    void cleanupExpiredEntries();
}
```

**Cleanup Strategy**: Scheduled job runs daily to remove expired entries (>30 days old)


#### 4.3 UserSessionService

**Responsibility**: Manage multi-device sessions

**Methods**:
```java
public class UserSessionService {
    UserSession createSession(UUID userId, String deviceFingerprint, String ipAddress, String userAgent);
    List<UserSession> getActiveSessions(UUID userId);
    void revokeSession(UUID sessionId);
    void revokeAllSessionsExceptCurrent(UUID userId, UUID currentSessionId);
    void enforceSessionLimit(UUID userId, int maxSessions);
    void updateSessionActivity(UUID sessionId);
}
```

**Session Limit Enforcement**:
- Maximum 5 concurrent sessions per user
- When limit exceeded, revoke oldest session (by last_activity)
- Notify user via email when session is revoked

#### 4.4 JwtAuthenticationFilter (Enhanced)

**Responsibility**: Validate tokens and check blacklist

**Modified Logic**:
```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                                HttpServletResponse response, 
                                FilterChain filterChain) {
    String token = extractToken(request);
    
    if (token != null) {
        // Check blacklist FIRST
        if (tokenBlacklistService.isBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"TOKEN_REVOKED\"}");
            return;
        }
        
        // Then validate token
        if (jwtTokenProvider.validateToken(token)) {
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // Update session activity
            UUID sessionId = extractSessionId(token);
            userSessionService.updateSessionActivity(sessionId);
        }
    }
    
    filterChain.doFilter(request, response);
}
```


### Data Models

#### refresh_tokens Table

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL, -- SHA-256 hash
    device_fingerprint VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    UNIQUE(token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked) WHERE revoked = FALSE;
```

#### token_blacklist Table

```sql
CREATE TABLE token_blacklist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(64) NOT NULL, -- SHA-256 hash
    token_type VARCHAR(20) NOT NULL, -- ACCESS or REFRESH
    user_id UUID REFERENCES usuarios(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_by UUID REFERENCES usuarios(id),
    reason VARCHAR(100), -- LOGOUT, ADMIN_REVOKE, TOKEN_ROTATION, etc.
    UNIQUE(token_hash)
);

CREATE INDEX idx_token_blacklist_hash ON token_blacklist(token_hash);
CREATE INDEX idx_token_blacklist_expires ON token_blacklist(expires_at);
```

#### user_sessions Table

```sql
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    refresh_token_id UUID REFERENCES refresh_tokens(id) ON DELETE CASCADE,
    device_fingerprint VARCHAR(255),
    device_type VARCHAR(50), -- MOBILE, DESKTOP, TABLET
    browser VARCHAR(100),
    os VARCHAR(100),
    ip_address VARCHAR(45),
    city VARCHAR(100),
    country VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    UNIQUE(refresh_token_id)
);

CREATE INDEX idx_user_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_active ON user_sessions(active) WHERE active = TRUE;
CREATE INDEX idx_user_sessions_last_activity ON user_sessions(last_activity);
```


### Security Considerations

**Token Hashing**:
- Store tokens as SHA-256 hashes, never plaintext
- Use `MessageDigest.getInstance("SHA-256")` for hashing
- Hash before database storage and comparison

**Device Fingerprinting**:
- Combine: User-Agent + Accept-Language + Screen Resolution + Timezone
- Generate fingerprint hash for comparison
- Bind refresh tokens to device fingerprints

**Token Rotation Benefits**:
- Limits token lifetime even if stolen
- Detects token theft (old token used after rotation)
- Provides audit trail of token usage

### API Endpoints

**AuthController (New Endpoints)**:
- `POST /api/auth/refresh` - Refresh access token using refresh token
- `POST /api/auth/logout` - Logout current session
- `POST /api/auth/logout-all` - Logout all sessions
- `GET /api/auth/sessions` - List active sessions
- `DELETE /api/auth/sessions/{sessionId}` - Revoke specific session

**Request/Response Examples**:

```json
// POST /api/auth/refresh
Request:
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 900
}
```

```json
// GET /api/auth/sessions
Response:
{
  "sessions": [
    {
      "id": "uuid",
      "deviceType": "DESKTOP",
      "browser": "Chrome 120",
      "os": "Windows 10",
      "location": "Madrid, Spain",
      "lastActivity": "2026-02-09T10:30:00Z",
      "current": true
    }
  ]
}
```


---

## MODULE 5: PERFORMANCE TESTING

### Architecture Overview

```
Load Testing Tool (JMeter/Gatling) → Backend API → PostgreSQL
                                          ↓
                                    Actuator Metrics
                                          ↓
                                    Monitoring Dashboard
```

### Components

#### 5.1 Performance Monitoring Configuration

**Spring Boot Actuator Setup**:
```java
@Configuration
public class ActuatorConfig {
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "urban-cleaning-backend");
    }
}
```

**application.properties**:
```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.enable.jvm=true
management.metrics.enable.process=true
management.metrics.enable.system=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

#### 5.2 PerformanceMetricsService

**Responsibility**: Aggregate and expose performance metrics

**Methods**:
```java
public class PerformanceMetricsService {
    PerformanceMetrics getAggregatedMetrics(TimeRange range);
    Map<String, Double> getResponseTimePercentiles();
    Double getErrorRate();
    Integer getActiveConnections();
    MemoryMetrics getMemoryUsage();
    CPUMetrics getCPUUsage();
}
```


#### 5.3 Database Connection Pooling

**HikariCP Configuration**:
```properties
# Connection pool settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
```

**Monitoring**:
- Track active connections
- Monitor connection wait time
- Alert on connection pool exhaustion

#### 5.4 Circuit Breaker for Email Service

**Resilience4j Configuration**:
```java
@Configuration
public class CircuitBreakerConfig {
    @Bean
    public CircuitBreaker emailCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .slidingWindowSize(10)
            .build();
        
        return CircuitBreaker.of("emailService", config);
    }
}
```

**EmailService with Circuit Breaker**:
```java
@Service
public class EmailService {
    @CircuitBreaker(name = "emailService", fallbackMethod = "emailFallback")
    public void sendEmail(String to, String subject, String content) {
        // Email sending logic
    }
    
    private void emailFallback(String to, String subject, String content, Exception e) {
        log.error("Email circuit breaker activated, logging failure", e);
        notificationFailureRepository.save(new NotificationFailure(to, e.getMessage()));
    }
}
```

### Load Testing Strategy

#### Test Scenarios

**Scenario 1: Normal Load**
- 50 concurrent users
- Duration: 10 minutes
- Ramp-up: 2 minutes
- Operations: 70% reads, 30% writes

**Scenario 2: Peak Load**
- 100 concurrent users
- Duration: 5 minutes
- Ramp-up: 1 minute
- Operations: 60% reads, 40% writes

**Scenario 3: Stress Test**
- 200 concurrent users
- Duration: 3 minutes
- Ramp-up: 30 seconds
- Operations: 50% reads, 50% writes


#### Test Endpoints

**High Priority Endpoints**:
1. `POST /api/auth/login` - Authentication
2. `GET /api/reports` - List reports
3. `POST /api/reports` - Create report
4. `GET /api/tasks` - List tasks
5. `PUT /api/tasks/{id}/state` - Update task state
6. `GET /api/analytics/tasks/distribution/category` - Analytics

**Performance Targets**:
- Simple queries (GET): < 500ms average, < 1s p95
- Complex queries (Analytics): < 2s average, < 3s p95
- Write operations (POST/PUT): < 1s average, < 2s p95
- Success rate: > 99.9%
- Error rate: < 0.1%

### Data Models

#### performance_metrics Table

```sql
CREATE TABLE performance_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_type VARCHAR(50) NOT NULL, -- RESPONSE_TIME, ERROR_RATE, etc.
    endpoint VARCHAR(255),
    value DECIMAL(10, 2),
    unit VARCHAR(20), -- ms, percentage, count
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_performance_metrics_type ON performance_metrics(metric_type);
CREATE INDEX idx_performance_metrics_recorded ON performance_metrics(recorded_at);
CREATE INDEX idx_performance_metrics_endpoint ON performance_metrics(endpoint);
```

### Monitoring and Alerting

**Alert Conditions**:
1. Average response time > 1 second for 5 minutes
2. Error rate > 1% for 5 minutes
3. Database connection pool > 90% utilization
4. Memory usage > 85%
5. CPU usage > 80% for 10 minutes

**Alert Actions**:
- Send email to administrators
- Log to audit system
- Trigger auto-scaling (if cloud deployment)


---

## MODULE 6: API DOCUMENTATION

### Architecture Overview

```
SpringDoc OpenAPI → Swagger UI → Interactive Documentation
        ↓
   @Operation, @Schema annotations
        ↓
   Auto-generated OpenAPI 3.0 spec
```

### Components

#### 6.1 OpenAPI Configuration

**Dependencies** (pom.xml):
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**Configuration Class**:
```java
@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI urbanCleaningOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Urban Cleaning Management API")
                .description("API for managing urban cleaning reports and tasks")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Urban Cleaning Team")
                    .email("support@urbanclean.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

#### 6.2 Controller Annotations

**Example: ReportController**:
```java
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Citizen report management endpoints")
public class ReportController {
    
    @Operation(
        summary = "Create a new report",
        description = "Submit a new urban cleaning report with photo and location",
        responses = {
            @ApiResponse(responseCode = "201", description = "Report created successfully",
                content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> createReport(
        @Parameter(description = "Report data", required = true)
        @Valid @RequestPart("data") ReportSubmissionRequest request,
        
        @Parameter(description = "Photo of the incident", required = true)
        @RequestPart("photo") MultipartFile photo
    ) {
        // Implementation
    }
}
```


#### 6.3 DTO Schema Annotations

**Example: ReportSubmissionRequest**:
```java
@Schema(description = "Request to create a new report")
public class ReportSubmissionRequest {
    
    @Schema(description = "Report category", example = "BASURA_ACUMULADA", required = true)
    @NotNull
    private String category;
    
    @Schema(description = "Detailed description of the issue", example = "Large pile of trash on the sidewalk", required = true)
    @NotBlank
    @Size(min = 10, max = 500)
    private String description;
    
    @Schema(description = "Latitude coordinate", example = "40.7128", required = true)
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;
    
    @Schema(description = "Longitude coordinate", example = "-74.0060", required = true)
    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;
}
```

### Documentation Structure

**Endpoint Groups**:
1. **Authentication** - Login, register, logout, token refresh
2. **Reports** - Citizen report submission and tracking
3. **Tasks** - Operator task management
4. **Analytics** - Dashboard metrics and KPIs
5. **Configuration** - System configuration management
6. **Notifications** - Notification preferences and management
7. **Admin** - Administrative operations

### Access and Configuration

**Swagger UI URL**: `http://localhost:8080/api/docs`

**OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

**application.properties**:
```properties
# SpringDoc configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/api/docs
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
```


---

## CORRECTNESS PROPERTIES

### Property 1: Notification Delivery Guarantee
**Statement**: For every task assignment event, if the operator has notifications enabled, an email SHALL be sent or a failure SHALL be recorded.

**Verification**:
- Check notification_preferences before sending
- Log all email attempts
- Record failures in notification_failures table
- Property-based test: Generate random task assignments, verify email sent or failure logged

### Property 2: Analytics Cache Consistency
**Statement**: Cached analytics data SHALL be consistent with database state within the TTL window.

**Verification**:
- Cache TTL enforced by Spring Cache
- Manual cache eviction on data modification (optional)
- Property-based test: Query analytics, modify data, wait for TTL, verify cache refresh

### Property 3: Token Rotation Atomicity
**Statement**: When a refresh token is rotated, the old token SHALL be invalidated and the new token SHALL be created atomically.

**Verification**:
- Use database transactions (@Transactional)
- Verify old token in blacklist
- Verify new token in refresh_tokens table
- Property-based test: Concurrent token rotation attempts, verify only one succeeds

### Property 4: Session Limit Enforcement
**Statement**: A user SHALL never have more than the configured maximum number of active sessions.

**Verification**:
- Count active sessions before creating new one
- Revoke oldest session if limit exceeded
- Property-based test: Create N+1 sessions, verify only N remain active

### Property 5: Configuration Change Auditability
**Statement**: Every configuration change SHALL be recorded in the audit log with user, timestamp, and old/new values.

**Verification**:
- Check audit_logs table after configuration change
- Verify all required fields present
- Property-based test: Random configuration changes, verify audit entries

### Property 6: Heatmap Spatial Accuracy
**Statement**: Every report SHALL be counted in exactly one heatmap cell based on its location.

**Verification**:
- Use PostGIS ST_SnapToGrid for deterministic cell assignment
- Sum of cell intensities equals total reports
- Property-based test: Generate random reports, verify sum consistency


### Property 7: Performance SLA Compliance
**Statement**: Under normal load (50 concurrent users), 95% of simple queries SHALL respond within 500ms.

**Verification**:
- Load testing with JMeter/Gatling
- Measure p95 response time
- Property-based test: Simulate load, verify p95 < 500ms

### Property 8: Token Blacklist Effectiveness
**Statement**: A blacklisted token SHALL never be accepted for authentication.

**Verification**:
- Check blacklist before validating token
- Return 401 with TOKEN_REVOKED error
- Property-based test: Generate random tokens, blacklist them, verify rejection

---

## ERROR HANDLING

### Error Scenarios and Responses

#### 1. Email Service Unavailable
**Scenario**: SMTP server is down or unreachable

**Handling**:
- Circuit breaker opens after 50% failure rate
- Fallback: Log failure to notification_failures table
- Retry: Exponential backoff (1min, 5min, 15min)
- User Impact: None (asynchronous operation)

#### 2. Analytics Query Timeout
**Scenario**: Complex analytics query exceeds 30-second timeout

**Handling**:
- Database query timeout configured
- Return 503 Service Unavailable
- Log error with query parameters
- Suggest narrower date range to user

#### 3. Token Blacklist Check Failure
**Scenario**: Database connection lost during blacklist check

**Handling**:
- Fail closed: Reject authentication
- Return 503 Service Unavailable
- Log error for investigation
- Retry connection with exponential backoff

#### 4. Session Limit Exceeded
**Scenario**: User attempts to create 6th session (limit is 5)

**Handling**:
- Revoke oldest session automatically
- Create new session
- Send email notification about revoked session
- Return success with warning message


#### 5. Configuration Validation Failure
**Scenario**: Admin attempts to set invalid configuration values

**Handling**:
- Validate before applying changes
- Return 400 Bad Request with specific error
- Do not modify existing configuration
- Log validation failure

**Example Error Response**:
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid token expiration configuration",
  "timestamp": "2026-02-09T10:30:00.000Z",
  "details": {
    "field": "accessTokenExpirationMinutes",
    "value": 120,
    "constraint": "Must be between 5 and 60 minutes"
  }
}
```

#### 6. Heatmap Generation Failure
**Scenario**: PostGIS spatial query fails or times out

**Handling**:
- Catch SQLException
- Return 500 Internal Server Error
- Log error with query parameters
- Clear cache to prevent serving stale data

---

## TESTING STRATEGY

### Unit Tests

**Coverage Target**: 80% line coverage

**Key Test Classes**:
- `NotificationPreferenceServiceTest`
- `EmailServiceTest` (with mocked SMTP)
- `AnalyticsServiceTest` (with mocked repositories)
- `RefreshTokenServiceTest`
- `TokenBlacklistServiceTest`
- `UserSessionServiceTest`
- `HeatmapServiceTest`

**Testing Approach**:
- Mock external dependencies (repositories, email server)
- Test business logic in isolation
- Verify error handling paths
- Test validation rules

### Integration Tests

**Coverage Target**: All API endpoints

**Test Categories**:
1. **Notification Integration Tests**
   - Test email sending with test SMTP server
   - Verify preference checking
   - Test retry logic

2. **Analytics Integration Tests**
   - Test with real PostgreSQL + PostGIS
   - Verify query performance
   - Test caching behavior

3. **Session Management Integration Tests**
   - Test token rotation flow
   - Verify blacklist checking
   - Test multi-device scenarios


### Property-Based Tests

**Framework**: JUnit-QuickCheck

**Test Properties**:

```java
@Property
@Tag("Feature: operational-excellence, Property 1: Notification delivery guarantee")
public void notificationSentOrFailureRecorded(
    @ForAll UUID taskId,
    @ForAll UUID operatorId,
    @ForAll boolean notificationsEnabled) {
    
    // Setup
    when(notificationPreferenceService.isNotificationEnabled(operatorId, TASK_ASSIGNED))
        .thenReturn(notificationsEnabled);
    
    // Execute
    taskAssignmentListener.handleTaskAssigned(new TaskAssignedEvent(taskId, operatorId));
    
    // Verify
    if (notificationsEnabled) {
        verify(emailService, times(1)).sendTaskAssignmentEmail(taskId, operatorId);
    } else {
        verify(emailService, never()).sendTaskAssignmentEmail(any(), any());
    }
}
```

```java
@Property
@Tag("Feature: operational-excellence, Property 3: Token rotation atomicity")
public void tokenRotationIsAtomic(@ForAll String oldToken) {
    // Setup
    RefreshToken oldRefreshToken = createValidRefreshToken(oldToken);
    
    // Execute
    RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(oldToken);
    
    // Verify
    assertTrue(tokenBlacklistService.isBlacklisted(oldToken));
    assertNotNull(newRefreshToken);
    assertNotEquals(oldToken, newRefreshToken.getToken());
    assertFalse(tokenBlacklistService.isBlacklisted(newRefreshToken.getToken()));
}
```

### Load Tests

**Tool**: Apache JMeter or Gatling

**Test Plan Structure**:
1. Thread Group: 50 concurrent users
2. Ramp-up: 2 minutes
3. Duration: 10 minutes
4. HTTP Request Samplers for each endpoint
5. Assertions for response time and status codes
6. Listeners for results aggregation

**Metrics to Collect**:
- Average response time
- p95, p99 response time
- Throughput (requests/second)
- Error rate
- Database connection pool usage
- Memory usage
- CPU usage


---

## IMPLEMENTATION PHASES

### Phase 1: Notification System (Week 1)
**Priority**: High  
**Dependencies**: None

**Tasks**:
1. Create notification_preferences and notification_failures tables
2. Implement NotificationPreferenceService
3. Enhance EmailService with retry logic
4. Create email templates (Thymeleaf)
5. Implement event listeners for task events
6. Add unsubscribe functionality
7. Write unit and integration tests

**Deliverables**:
- Working notification system with preferences
- Email templates for all notification types
- Unsubscribe functionality
- Test coverage > 80%

### Phase 2: Analytics Dashboard (Week 2)
**Priority**: High  
**Dependencies**: None

**Tasks**:
1. Implement AnalyticsService with caching
2. Create HeatmapService with PostGIS queries
3. Add database indexes for analytics performance
4. Implement AnalyticsController endpoints
5. Create DTOs for analytics responses
6. Write unit and integration tests
7. Performance test analytics queries

**Deliverables**:
- All analytics endpoints functional
- Heatmap generation working
- Query performance < 2 seconds
- Test coverage > 80%

### Phase 3: Enhanced Session Management (Week 3)
**Priority**: Critical  
**Dependencies**: None

**Tasks**:
1. Create refresh_tokens, token_blacklist, user_sessions tables
2. Implement RefreshTokenService
3. Implement TokenBlacklistService
4. Implement UserSessionService
5. Enhance JwtAuthenticationFilter
6. Add session management endpoints
7. Implement automatic token refresh in frontend
8. Write unit and integration tests

**Deliverables**:
- Refresh token system working
- Token blacklist enforced
- Multi-device session management
- Frontend auto-refresh implemented
- Test coverage > 80%


### Phase 4: Extended Configuration (Week 4)
**Priority**: Medium  
**Dependencies**: Phase 3 (for token expiration config)

**Tasks**:
1. Extend system_config table
2. Implement token expiration configuration
3. Implement duplicate detection configuration
4. Enhance ConfigService
5. Update JwtTokenProvider to use dynamic expiration
6. Update DeduplicationService to use dynamic parameters
7. Add configuration endpoints
8. Write unit and integration tests

**Deliverables**:
- Dynamic token expiration configuration
- Dynamic duplicate detection configuration
- Configuration changes audited
- Test coverage > 80%

### Phase 5: Performance Testing & Monitoring (Week 5)
**Priority**: High  
**Dependencies**: All previous phases

**Tasks**:
1. Configure Spring Boot Actuator
2. Implement PerformanceMetricsService
3. Configure HikariCP connection pooling
4. Implement circuit breaker for email service
5. Create JMeter/Gatling load test scripts
6. Run load tests and collect metrics
7. Optimize based on results
8. Set up monitoring and alerting

**Deliverables**:
- Load test results meeting SLA targets
- Performance monitoring dashboard
- Circuit breaker implemented
- Optimization recommendations

### Phase 6: API Documentation (Week 6)
**Priority**: Medium  
**Dependencies**: All previous phases

**Tasks**:
1. Add SpringDoc OpenAPI dependency
2. Configure OpenAPI
3. Add @Operation annotations to all controllers
4. Add @Schema annotations to all DTOs
5. Document error responses
6. Test interactive documentation
7. Generate OpenAPI JSON spec

**Deliverables**:
- Complete API documentation at /api/docs
- All endpoints documented
- Interactive testing working
- OpenAPI 3.0 spec available


---

## DEPENDENCIES AND LIBRARIES

### Backend Dependencies (pom.xml)

```xml
<!-- Email -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Thymeleaf for email templates -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Caching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Actuator for monitoring -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer for metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Resilience4j for circuit breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- SpringDoc OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- JUnit QuickCheck for property-based testing -->
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-core</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-generators</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
```

### Frontend Dependencies (package.json)

```json
{
  "dependencies": {
    "axios": "^1.6.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "leaflet": "^1.9.4",
    "react-leaflet": "^4.2.1",
    "recharts": "^2.10.0"
  }
}
```


---

## CONFIGURATION FILES

### application.properties (Backend)

```properties
# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.timeout=5000

# Async Configuration
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100

# Cache Configuration
spring.cache.type=simple
spring.cache.cache-names=taskDistribution,mttr,heatmap,operatorMetrics

# Actuator Configuration
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.enable.jvm=true
management.metrics.enable.process=true
management.metrics.enable.system=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true

# HikariCP Configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000

# Resilience4j Circuit Breaker
resilience4j.circuitbreaker.instances.emailService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.emailService.wait-duration-in-open-state=60000
resilience4j.circuitbreaker.instances.emailService.sliding-window-size=10

# SpringDoc OpenAPI
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/api/docs
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true

# JWT Configuration (Dynamic from database)
jwt.access-token-expiration-minutes=15
jwt.refresh-token-expiration-days=7
```


---

## SECURITY CONSIDERATIONS

### Token Security

1. **Hashing**: All tokens stored as SHA-256 hashes
2. **Rotation**: Refresh tokens rotated on every use
3. **Blacklist**: Revoked tokens checked on every request
4. **Expiration**: Short-lived access tokens (15 minutes)
5. **Device Binding**: Refresh tokens bound to device fingerprints

### Email Security

1. **Unsubscribe Tokens**: Signed JWT with 30-day expiration
2. **Rate Limiting**: Prevent email bombing attacks
3. **Template Injection**: Use Thymeleaf with auto-escaping
4. **SMTP Authentication**: Use secure credentials

### Configuration Security

1. **Authorization**: Only ROLE_ADMIN can modify configuration
2. **Validation**: Strict validation of all configuration values
3. **Audit Trail**: All changes logged with user and IP
4. **Rollback**: Configuration history allows rollback

### API Security

1. **Authentication**: JWT required for all protected endpoints
2. **Authorization**: Role-based access control
3. **Rate Limiting**: Prevent abuse and DoS attacks
4. **Input Validation**: Validate all user inputs
5. **SQL Injection**: Use parameterized queries
6. **XSS Prevention**: Sanitize outputs

---

## MONITORING AND OBSERVABILITY

### Metrics to Track

**Application Metrics**:
- Request count by endpoint
- Response time (average, p95, p99)
- Error rate by endpoint
- Active sessions count
- Token refresh rate
- Email send success/failure rate

**Infrastructure Metrics**:
- CPU usage
- Memory usage
- Database connection pool usage
- Database query performance
- Network I/O

**Business Metrics**:
- Reports created per hour
- Tasks resolved per hour
- Average MTTR
- Operator performance
- Notification delivery rate

### Logging Strategy

**Log Levels**:
- ERROR: System errors, exceptions
- WARN: Degraded performance, retry attempts
- INFO: Important business events (login, logout, config changes)
- DEBUG: Detailed debugging information

**Structured Logging**:
```java
log.info("Token refreshed", 
    kv("userId", userId),
    kv("deviceFingerprint", fingerprint),
    kv("ipAddress", ipAddress));
```


---

## FRONTEND INTEGRATION

### Analytics Dashboard Component

**Location**: `frontend/src/components/admin/AnalyticsDashboard.jsx`

**Features**:
- Task distribution charts (pie/bar charts)
- MTTR display with trend
- Interactive heatmap using Leaflet
- Operator performance table
- Date range filters
- Export functionality

**Libraries**:
- Recharts for charts
- React-Leaflet for heatmap
- Axios for API calls

### Notification Preferences Component

**Location**: `frontend/src/components/user/NotificationPreferences.jsx`

**Features**:
- Toggle switches for each notification type
- Save preferences button
- Success/error feedback
- Loading states

### Session Management Component

**Location**: `frontend/src/components/user/ActiveSessions.jsx`

**Features**:
- List of active sessions with device info
- Revoke session button
- Logout all devices button
- Current session indicator
- Auto-refresh every 30 seconds

### Automatic Token Refresh

**Location**: `frontend/src/services/authService.js`

**Implementation**:
```javascript
// Check token expiration every minute
setInterval(() => {
  const token = getAccessToken();
  const expiresAt = getTokenExpiration(token);
  const now = Date.now();
  const timeUntilExpiry = expiresAt - now;
  
  // Refresh if less than 5 minutes remaining
  if (timeUntilExpiry < 5 * 60 * 1000) {
    refreshAccessToken();
  }
}, 60 * 1000);
```

---

## DEPLOYMENT CONSIDERATIONS

### Database Migrations

**Tool**: Flyway or Liquibase

**Migration Files**:
- `V1.0__initial_schema.sql` (existing)
- `V2.0__notification_system.sql`
- `V2.1__analytics_indexes.sql`
- `V2.2__session_management.sql`
- `V2.3__extended_configuration.sql`

### Environment Variables

**Required**:
- `MAIL_USERNAME` - SMTP username
- `MAIL_PASSWORD` - SMTP password
- `JWT_SECRET` - JWT signing secret
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` - Database connection

### Docker Compose Updates

**Add email service** (optional for testing):
```yaml
mailhog:
  image: mailhog/mailhog
  ports:
    - "1025:1025"  # SMTP
    - "8025:8025"  # Web UI
```


### Performance Tuning

**Database**:
- Ensure all indexes are created
- Configure PostgreSQL shared_buffers (25% of RAM)
- Enable query logging for slow queries (> 1 second)
- Regular VACUUM and ANALYZE

**Application**:
- JVM heap size: -Xmx2g -Xms1g
- Enable G1GC: -XX:+UseG1GC
- Connection pool tuning based on load tests
- Cache size tuning based on memory usage

**Frontend**:
- Code splitting for large components
- Lazy loading for analytics dashboard
- Debounce search inputs
- Optimize map rendering

---

## ROLLBACK STRATEGY

### Database Rollback

**Approach**: Maintain rollback scripts for each migration

**Example**:
- `V2.0__notification_system.sql` → `U2.0__rollback_notification_system.sql`

**Procedure**:
1. Stop application
2. Run rollback script
3. Deploy previous application version
4. Restart application

### Configuration Rollback

**Approach**: Use configuration history table

**Procedure**:
1. Query previous configuration from system_config table
2. Apply previous values via admin endpoint
3. Verify system behavior
4. Audit rollback action

### Code Rollback

**Approach**: Git revert + redeploy

**Procedure**:
1. Identify commit to revert
2. Create revert commit
3. Run tests
4. Deploy to production
5. Monitor for issues

---

## SUCCESS CRITERIA

### Functional Criteria

✅ All 17 requirements implemented and tested  
✅ 100% IDRQ requirements coverage (RF-07, RF-08, RF-11, RNF-01, RNF-04)  
✅ All API endpoints documented in Swagger  
✅ Email notifications working with preferences  
✅ Analytics dashboard displaying real-time data  
✅ Refresh token system operational  
✅ Multi-device session management working  

### Performance Criteria

✅ Simple queries respond in < 500ms (p95)  
✅ Analytics queries respond in < 2s (p95)  
✅ System handles 50 concurrent users  
✅ Email notifications sent within 10 seconds  
✅ Token refresh completes in < 200ms  
✅ Success rate > 99.9% under normal load  

### Quality Criteria

✅ Unit test coverage > 80%  
✅ All integration tests passing  
✅ Property-based tests for critical properties  
✅ Load tests meeting SLA targets  
✅ Security audit passed  
✅ Code review completed  

---

**Document Version**: 1.0  
**Last Updated**: 9 de febrero de 2026  
**Status**: Ready for Implementation  
**Next Step**: Create tasks.md with detailed implementation tasks
