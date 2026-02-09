# Tasks Document: Operational Excellence

## Overview

This document breaks down the implementation of operational excellence features into concrete, actionable tasks. The implementation is organized into 6 phases over 6 weeks, with each phase delivering a complete, testable module.

**Total Tasks**: 85 tasks across 6 phases  
**Estimated Duration**: 6 weeks  
**Priority**: High (completes all pending IDRQ requirements)

---

## PHASE 1: NOTIFICATION SYSTEM COMPLETION (Week 1)

### 1.1 Database Schema

**Task 1.1.1**: Create notification_preferences table
- [ ] Write migration script `V2.0__notification_preferences.sql`
- [ ] Create table with columns: id, user_id, task_assigned, task_resolved, task_reopened, report_created, created_at, updated_at
- [ ] Add foreign key constraint to usuarios table
- [ ] Create index on user_id
- [ ] Add unique constraint on user_id
- [ ] Test migration with Flyway

**Task 1.1.2**: Create notification_failures table
- [ ] Write migration script `V2.1__notification_failures.sql`
- [ ] Create table with columns: id, user_id, notification_type, email_address, failure_reason, retry_count, attempted_at, created_at
- [ ] Add foreign key constraint to usuarios table
- [ ] Create indexes on user_id and attempted_at
- [ ] Test migration with Flyway

### 1.2 Entity and Repository Layer

**Task 1.2.1**: Create NotificationPreference entity
- [ ] Create `NotificationPreference.java` in entity package
- [ ] Add JPA annotations (@Entity, @Table, @Id, @Column)
- [ ] Add fields for all notification types (boolean)
- [ ] Add audit fields (createdAt, updatedAt)
- [ ] Add relationship to User entity
- [ ] Generate getters/setters

**Task 1.2.2**: Create NotificationPreferenceRepository
- [ ] Create `NotificationPreferenceRepository.java` interface
- [ ] Extend JpaRepository<NotificationPreference, UUID>
- [ ] Add method: `Optional<NotificationPreference> findByUserId(UUID userId)`
- [ ] Add method: `boolean existsByUserId(UUID userId)`

**Task 1.2.3**: Create NotificationFailure entity
- [ ] Create `NotificationFailure.java` in entity package
- [ ] Add JPA annotations
- [ ] Add fields: userId, notificationType, emailAddress, failureReason, retryCount, attemptedAt
- [ ] Add relationship to User entity
- [ ] Generate getters/setters

**Task 1.2.4**: Create NotificationFailureRepository
- [ ] Create `NotificationFailureRepository.java` interface
- [ ] Extend JpaRepository<NotificationFailure, UUID>
- [ ] Add method: `List<NotificationFailure> findByUserIdOrderByAttemptedAtDesc(UUID userId)`
- [ ] Add method: `List<NotificationFailure> findByAttemptedAtBefore(LocalDateTime date)`
- [ ] Add method: `void deleteByAttemptedAtBefore(LocalDateTime date)`


### 1.3 Service Layer

**Task 1.3.1**: Create NotificationPreferenceService
- [ ] Create `NotificationPreferenceService.java` in service package
- [ ] Implement method: `getPreferences(UUID userId)`
- [ ] Implement method: `updatePreferences(UUID userId, NotificationPreferenceRequest request)`
- [ ] Implement method: `isNotificationEnabled(UUID userId, NotificationType type)`
- [ ] Implement method: `createDefaultPreferences(UUID userId)` - all types enabled
- [ ] Add @Service annotation
- [ ] Inject NotificationPreferenceRepository

**Task 1.3.2**: Enhance EmailService with retry logic
- [ ] Add @Async annotation to email methods
- [ ] Add @Retryable annotation with maxAttempts=3
- [ ] Configure backoff: delay=60000ms, multiplier=5
- [ ] Implement @Recover method to handle final failure
- [ ] In recover method, save to notification_failures table
- [ ] Add logging for all retry attempts
- [ ] Test retry logic with mock SMTP failures

**Task 1.3.3**: Create email templates
- [ ] Create directory: `src/main/resources/templates/email/`
- [ ] Create `task-assigned.html` template with Thymeleaf
- [ ] Create `task-resolved.html` template
- [ ] Create `task-reopened.html` template
- [ ] Create `report-created.html` template
- [ ] Add CSS styling for responsive design
- [ ] Include unsubscribe link in all templates
- [ ] Test templates with sample data

**Task 1.3.4**: Implement email sending methods
- [ ] Implement `sendTaskAssignmentEmail(UUID taskId, UUID operatorId)`
- [ ] Implement `sendTaskResolvedEmail(UUID taskId, UUID citizenId)`
- [ ] Implement `sendTaskReopenedEmail(UUID taskId, UUID operatorId)`
- [ ] Implement `sendReportCreatedEmail(UUID reportId, UUID citizenId)`
- [ ] Check notification preferences before sending
- [ ] Use Thymeleaf to render templates
- [ ] Add proper error handling

### 1.4 Event Listeners

**Task 1.4.1**: Create TaskAssignedEvent
- [ ] Create `TaskAssignedEvent.java` in events package
- [ ] Add fields: taskId, operatorId, timestamp
- [ ] Add constructor and getters

**Task 1.4.2**: Create TaskAssignmentListener
- [ ] Create `TaskAssignmentListener.java` in listeners package
- [ ] Add @Component annotation
- [ ] Implement @EventListener method for TaskAssignedEvent
- [ ] Add @Async annotation
- [ ] Check notification preferences
- [ ] Call emailService.sendTaskAssignmentEmail()
- [ ] Add error handling and logging

**Task 1.4.3**: Publish TaskAssignedEvent in TaskService
- [ ] Inject ApplicationEventPublisher in TaskService
- [ ] In assignTask() method, publish TaskAssignedEvent after assignment
- [ ] Test event publishing


### 1.5 Controller Layer

**Task 1.5.1**: Create NotificationPreferenceController
- [ ] Create `NotificationPreferenceController.java` in controller package
- [ ] Add @RestController and @RequestMapping("/api/users/notifications")
- [ ] Implement GET /preferences endpoint
- [ ] Implement PUT /preferences endpoint
- [ ] Add @PreAuthorize for authenticated users
- [ ] Add validation for request DTOs
- [ ] Add error handling

**Task 1.5.2**: Create UnsubscribeController
- [ ] Create `UnsubscribeController.java` in controller package
- [ ] Implement GET /api/notifications/unsubscribe endpoint
- [ ] Parse and validate unsubscribe token (JWT)
- [ ] Disable notification type for user
- [ ] Return confirmation HTML page
- [ ] Add error handling for invalid tokens
- [ ] Log unsubscribe actions

**Task 1.5.3**: Create NotificationFailureController (Admin)
- [ ] Create endpoints in AdminController or separate controller
- [ ] Implement GET /api/admin/notifications/failures
- [ ] Support filtering by date range, type, user
- [ ] Implement POST /api/admin/notifications/failures/{id}/retry
- [ ] Add @PreAuthorize("hasRole('ADMIN')")
- [ ] Add pagination support

### 1.6 DTOs

**Task 1.6.1**: Create NotificationPreferenceRequest DTO
- [ ] Create in dto/request package
- [ ] Add fields: taskAssigned, taskResolved, taskReopened, reportCreated (all Boolean)
- [ ] Add validation annotations

**Task 1.6.2**: Create NotificationPreferenceResponse DTO
- [ ] Create in dto/response package
- [ ] Add all preference fields
- [ ] Add createdAt, updatedAt

**Task 1.6.3**: Create NotificationFailureResponse DTO
- [ ] Create in dto/response package
- [ ] Add fields: id, userId, notificationType, emailAddress, failureReason, retryCount, attemptedAt
- [ ] Add user details (username, email)

### 1.7 Configuration

**Task 1.7.1**: Configure Spring Mail
- [ ] Add spring-boot-starter-mail dependency to pom.xml
- [ ] Add mail properties to application.properties
- [ ] Configure SMTP host, port, username, password
- [ ] Enable STARTTLS
- [ ] Set timeout values

**Task 1.7.2**: Configure Async Execution
- [ ] Add @EnableAsync to main application class
- [ ] Configure thread pool in application.properties
- [ ] Set core-size=5, max-size=10, queue-capacity=100

**Task 1.7.3**: Configure Retry
- [ ] Add spring-retry dependency to pom.xml
- [ ] Add @EnableRetry to main application class


### 1.8 Testing

**Task 1.8.1**: Unit test NotificationPreferenceService
- [ ] Create `NotificationPreferenceServiceTest.java`
- [ ] Test getPreferences() with existing and non-existing user
- [ ] Test updatePreferences() with valid data
- [ ] Test isNotificationEnabled() for all types
- [ ] Test createDefaultPreferences()
- [ ] Mock repository dependencies

**Task 1.8.2**: Unit test EmailService
- [ ] Create `EmailServiceTest.java`
- [ ] Test email sending with mocked JavaMailSender
- [ ] Test retry logic with simulated failures
- [ ] Test recover method saves to notification_failures
- [ ] Verify template rendering

**Task 1.8.3**: Integration test notification flow
- [ ] Create `NotificationIntegrationTest.java`
- [ ] Test end-to-end: task assignment → event → email sent
- [ ] Use test SMTP server (GreenMail or similar)
- [ ] Verify email content and recipients
- [ ] Test with preferences disabled

**Task 1.8.4**: Test unsubscribe functionality
- [ ] Test unsubscribe token generation
- [ ] Test unsubscribe endpoint with valid token
- [ ] Test with expired token
- [ ] Verify preference updated in database

---

## PHASE 2: ANALYTICS DASHBOARD (Week 2)

### 2.1 Database Optimization

**Task 2.1.1**: Create analytics indexes
- [ ] Write migration script `V2.2__analytics_indexes.sql`
- [ ] Create index on tareas(created_at)
- [ ] Create index on tareas(state, created_at)
- [ ] Create index on tareas(category, created_at)
- [ ] Create index on tareas(assigned_to)
- [ ] Create index on tareas(resolved_at) WHERE resolved_at IS NOT NULL
- [ ] Create spatial index on reportes(location) using GIST
- [ ] Test index usage with EXPLAIN ANALYZE

### 2.2 Service Layer

**Task 2.2.1**: Create AnalyticsService
- [ ] Create `AnalyticsService.java` in service package
- [ ] Add @Service annotation
- [ ] Inject TaskRepository and ReportRepository
- [ ] Add @Cacheable annotations to methods

**Task 2.2.2**: Implement task distribution analytics
- [ ] Implement `getTaskDistributionByCategory(AnalyticsFilters filters)`
- [ ] Use repository method with GROUP BY category
- [ ] Calculate counts and percentages
- [ ] Apply date range and zone filters
- [ ] Add @Cacheable(value = "taskDistribution", key = "#filters")
- [ ] Implement `getTaskDistributionByState(AnalyticsFilters filters)` similarly

**Task 2.2.3**: Implement MTTR calculation
- [ ] Implement `calculateMTTR(AnalyticsFilters filters)`
- [ ] Query tasks with state = RESUELTO
- [ ] Calculate time difference between created_at and resolved_at
- [ ] Compute average in hours
- [ ] Calculate resolution time distribution (<24h, 24-48h, 48-72h, >72h)
- [ ] Add @Cacheable(value = "mttr", key = "#filters")


**Task 2.2.4**: Create HeatmapService
- [ ] Create `HeatmapService.java` in service package
- [ ] Implement `generateHeatmap(HeatmapFilters filters)`
- [ ] Use PostGIS ST_SnapToGrid to create grid cells
- [ ] Count reports per cell using GROUP BY
- [ ] Normalize intensity values to 0-1 scale
- [ ] Limit results to top 1000 cells
- [ ] Add @Cacheable(value = "heatmap", key = "#filters")

**Task 2.2.5**: Implement operator performance metrics
- [ ] Implement `getOperatorPerformance(AnalyticsFilters filters)`
- [ ] Query tasks grouped by assigned_to
- [ ] Calculate: tasks_resolved, average_resolution_time, tasks_in_progress, tasks_reopened
- [ ] Join with usuarios table for operator details
- [ ] Order by tasks_resolved DESC
- [ ] Add pagination support
- [ ] Add @Cacheable(value = "operatorMetrics", key = "#filters")

### 2.3 Repository Layer

**Task 2.3.1**: Add analytics methods to TaskRepository
- [ ] Add method: `List<Object[]> countByCategory(LocalDateTime start, LocalDateTime end)`
- [ ] Add method: `List<Object[]> countByState(LocalDateTime start, LocalDateTime end)`
- [ ] Add method: `List<Task> findResolvedTasks(LocalDateTime start, LocalDateTime end)`
- [ ] Add method: `List<Object[]> getOperatorStatistics(LocalDateTime start, LocalDateTime end)`
- [ ] Use @Query annotation with native SQL for complex queries

**Task 2.3.2**: Add heatmap method to ReportRepository
- [ ] Add method: `List<Object[]> getHeatmapData(double cellSize, LocalDateTime start, LocalDateTime end, String category)`
- [ ] Use @Query with PostGIS functions
- [ ] Use ST_SnapToGrid for grid creation
- [ ] Use ST_X and ST_Y for coordinates

### 2.4 Controller Layer

**Task 2.4.1**: Create AnalyticsController
- [ ] Create `AnalyticsController.java` in controller package
- [ ] Add @RestController and @RequestMapping("/api/analytics")
- [ ] Add @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")

**Task 2.4.2**: Implement task distribution endpoints
- [ ] Implement GET /tasks/distribution/category
- [ ] Implement GET /tasks/distribution/state
- [ ] Add query parameters: startDate, endDate, zoneId, category
- [ ] Validate date ranges
- [ ] Return TaskDistributionResponse DTO

**Task 2.4.3**: Implement MTTR endpoints
- [ ] Implement GET /tasks/mttr
- [ ] Implement GET /tasks/resolution-time-distribution
- [ ] Add query parameters for filtering
- [ ] Return MTTRResponse DTO

**Task 2.4.4**: Implement heatmap endpoint
- [ ] Implement GET /heatmap
- [ ] Add query parameters: cellSize, startDate, endDate, category
- [ ] Validate cell size (10-1000 meters)
- [ ] Return HeatmapResponse DTO

**Task 2.4.5**: Implement operator performance endpoint
- [ ] Implement GET /operators/performance
- [ ] Add query parameters: startDate, endDate, operatorId, page, size
- [ ] Return OperatorPerformanceResponse DTO with pagination


### 2.5 DTOs

**Task 2.5.1**: Create AnalyticsFilters DTO
- [ ] Create in dto/request package
- [ ] Add fields: startDate, endDate, zoneId, category, page, size
- [ ] Add validation annotations
- [ ] Add default values (last 30 days if not specified)

**Task 2.5.2**: Create TaskDistributionResponse DTO
- [ ] Create in dto/response package
- [ ] Add nested class DistributionItem (label, count, percentage)
- [ ] Add fields: distribution list, totalTasks, startDate, endDate

**Task 2.5.3**: Create MTTRResponse DTO
- [ ] Create in dto/response package
- [ ] Add fields: mttrHours, tasksResolved, averagePriorityScore
- [ ] Add resolutionTimeDistribution map
- [ ] Add startDate, endDate

**Task 2.5.4**: Create HeatmapResponse DTO
- [ ] Create in dto/response package
- [ ] Add nested class HeatmapCell (latitude, longitude, intensity, normalizedIntensity)
- [ ] Add fields: cells list, totalReports, cellSizeMeters, aggregationLevel

**Task 2.5.5**: Create OperatorPerformanceResponse DTO
- [ ] Create in dto/response package
- [ ] Add nested class OperatorMetrics (operatorId, username, tasksResolved, etc.)
- [ ] Add fields: operators list, totalOperators, page, totalPages

### 2.6 Configuration

**Task 2.6.1**: Configure Spring Cache
- [ ] Add spring-boot-starter-cache dependency to pom.xml
- [ ] Add @EnableCaching to main application class
- [ ] Create CacheConfig.java
- [ ] Configure cache names: taskDistribution, mttr, heatmap, operatorMetrics
- [ ] Set TTL values (5-10 minutes)

### 2.7 Testing

**Task 2.7.1**: Unit test AnalyticsService
- [ ] Create `AnalyticsServiceTest.java`
- [ ] Test getTaskDistributionByCategory() with mock data
- [ ] Test getTaskDistributionByState()
- [ ] Test calculateMTTR() with various scenarios
- [ ] Test with empty result sets
- [ ] Mock repository dependencies

**Task 2.7.2**: Unit test HeatmapService
- [ ] Create `HeatmapServiceTest.java`
- [ ] Test generateHeatmap() with mock data
- [ ] Test normalization logic
- [ ] Test cell limiting (top 1000)
- [ ] Mock repository dependencies

**Task 2.7.3**: Integration test analytics endpoints
- [ ] Create `AnalyticsIntegrationTest.java`
- [ ] Test with real database and PostGIS
- [ ] Create test data: reports and tasks
- [ ] Test all analytics endpoints
- [ ] Verify response structure and data accuracy
- [ ] Test caching behavior

**Task 2.7.4**: Performance test analytics queries
- [ ] Test with large dataset (10,000+ tasks)
- [ ] Measure query execution time
- [ ] Verify response time < 2 seconds
- [ ] Use EXPLAIN ANALYZE to verify index usage
- [ ] Optimize slow queries if needed


---

## PHASE 3: ENHANCED SESSION MANAGEMENT (Week 3)

### 3.1 Database Schema

**Task 3.1.1**: Create refresh_tokens table
- [ ] Write migration script `V2.3__refresh_tokens.sql`
- [ ] Create table with columns: id, user_id, token_hash, device_fingerprint, ip_address, user_agent, expires_at, created_at, last_used_at, revoked, revoked_at
- [ ] Add foreign key to usuarios table
- [ ] Create indexes on user_id, token_hash, expires_at
- [ ] Add unique constraint on token_hash
- [ ] Test migration

**Task 3.1.2**: Create token_blacklist table
- [ ] Write migration script `V2.4__token_blacklist.sql`
- [ ] Create table with columns: id, token_hash, token_type, user_id, expires_at, revoked_at, revoked_by, reason
- [ ] Add foreign key to usuarios table
- [ ] Create indexes on token_hash, expires_at
- [ ] Add unique constraint on token_hash
- [ ] Test migration

**Task 3.1.3**: Create user_sessions table
- [ ] Write migration script `V2.5__user_sessions.sql`
- [ ] Create table with columns: id, user_id, refresh_token_id, device_fingerprint, device_type, browser, os, ip_address, city, country, created_at, last_activity, active
- [ ] Add foreign keys to usuarios and refresh_tokens
- [ ] Create indexes on user_id, active, last_activity
- [ ] Add unique constraint on refresh_token_id
- [ ] Test migration

### 3.2 Entity and Repository Layer

**Task 3.2.1**: Create RefreshToken entity
- [ ] Create `RefreshToken.java` in entity package
- [ ] Add JPA annotations
- [ ] Add fields: id, userId, tokenHash, deviceFingerprint, ipAddress, userAgent, expiresAt, createdAt, lastUsedAt, revoked, revokedAt
- [ ] Add relationship to User entity
- [ ] Generate getters/setters

**Task 3.2.2**: Create RefreshTokenRepository
- [ ] Create `RefreshTokenRepository.java` interface
- [ ] Extend JpaRepository<RefreshToken, UUID>
- [ ] Add method: `Optional<RefreshToken> findByTokenHash(String tokenHash)`
- [ ] Add method: `List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId)`
- [ ] Add method: `void deleteByExpiresAtBefore(LocalDateTime date)`
- [ ] Add method: `int countByUserIdAndRevokedFalse(UUID userId)`

**Task 3.2.3**: Create TokenBlacklist entity
- [ ] Create `TokenBlacklist.java` in entity package
- [ ] Add JPA annotations
- [ ] Add fields: id, tokenHash, tokenType, userId, expiresAt, revokedAt, revokedBy, reason
- [ ] Add relationships to User entity
- [ ] Generate getters/setters

**Task 3.2.4**: Create TokenBlacklistRepository
- [ ] Create `TokenBlacklistRepository.java` interface
- [ ] Extend JpaRepository<TokenBlacklist, UUID>
- [ ] Add method: `boolean existsByTokenHash(String tokenHash)`
- [ ] Add method: `void deleteByExpiresAtBefore(LocalDateTime date)`

**Task 3.2.5**: Create UserSession entity
- [ ] Create `UserSession.java` in entity package
- [ ] Add JPA annotations
- [ ] Add fields: id, userId, refreshTokenId, deviceFingerprint, deviceType, browser, os, ipAddress, city, country, createdAt, lastActivity, active
- [ ] Add relationships to User and RefreshToken
- [ ] Generate getters/setters

**Task 3.2.6**: Create UserSessionRepository
- [ ] Create `UserSessionRepository.java` interface
- [ ] Extend JpaRepository<UserSession, UUID>
- [ ] Add method: `List<UserSession> findByUserIdAndActiveTrue(UUID userId)`
- [ ] Add method: `Optional<UserSession> findByRefreshTokenId(UUID refreshTokenId)`
- [ ] Add method: `List<UserSession> findByUserIdOrderByLastActivityDesc(UUID userId)`


### 3.3 Service Layer

**Task 3.3.1**: Create RefreshTokenService
- [ ] Create `RefreshTokenService.java` in service package
- [ ] Implement `createRefreshToken(UUID userId, String deviceFingerprint)`
- [ ] Generate random token, hash with SHA-256, store in database
- [ ] Set expiration to 7 days (configurable)
- [ ] Implement `validateRefreshToken(String token)`
- [ ] Hash token, check database, verify not revoked, verify not expired
- [ ] Implement `revokeRefreshToken(String token)`
- [ ] Add to blacklist, mark as revoked in refresh_tokens
- [ ] Implement `revokeAllUserTokens(UUID userId)`
- [ ] Implement `rotateRefreshToken(String oldToken)`
- [ ] Validate old token, create new token, revoke old token atomically
- [ ] Implement `cleanupExpiredTokens()` scheduled method

**Task 3.3.2**: Create TokenBlacklistService
- [ ] Create `TokenBlacklistService.java` in service package
- [ ] Implement `addToBlacklist(String token, LocalDateTime expiresAt)`
- [ ] Hash token with SHA-256, save to database
- [ ] Implement `isBlacklisted(String token)`
- [ ] Hash token, check existence in database
- [ ] Implement `cleanupExpiredEntries()` scheduled method
- [ ] Delete entries older than 30 days

**Task 3.3.3**: Create UserSessionService
- [ ] Create `UserSessionService.java` in service package
- [ ] Implement `createSession(UUID userId, String deviceFingerprint, String ipAddress, String userAgent)`
- [ ] Parse user agent to extract device type, browser, OS
- [ ] Create session record
- [ ] Implement `getActiveSessions(UUID userId)`
- [ ] Return list of active sessions with details
- [ ] Implement `revokeSession(UUID sessionId)`
- [ ] Mark session as inactive, revoke associated refresh token
- [ ] Implement `revokeAllSessionsExceptCurrent(UUID userId, UUID currentSessionId)`
- [ ] Implement `enforceSessionLimit(UUID userId, int maxSessions)`
- [ ] Count active sessions, revoke oldest if limit exceeded
- [ ] Implement `updateSessionActivity(UUID sessionId)`
- [ ] Update last_activity timestamp

**Task 3.3.4**: Enhance JwtTokenProvider
- [ ] Add method: `generateRefreshToken(Authentication authentication)`
- [ ] Include claims: userId, username, deviceFingerprint
- [ ] Use configurable expiration (7 days default)
- [ ] Modify `generateAccessToken()` to use configurable expiration (15 minutes default)
- [ ] Add method: `extractTokenHash(String token)` - SHA-256 hash
- [ ] Add method: `extractDeviceFingerprint(String token)`

**Task 3.3.5**: Enhance AuthService
- [ ] Modify login() to return both access and refresh tokens
- [ ] Create refresh token and session after successful login
- [ ] Implement `refreshAccessToken(String refreshToken)`
- [ ] Validate refresh token, check blacklist
- [ ] Generate new token pair, rotate refresh token
- [ ] Update session activity
- [ ] Implement `logout(String accessToken, String refreshToken)`
- [ ] Add both tokens to blacklist, mark session as inactive
- [ ] Implement `logoutAll(UUID userId)`
- [ ] Revoke all user's refresh tokens and sessions


### 3.4 Security Layer

**Task 3.4.1**: Enhance JwtAuthenticationFilter
- [ ] Inject TokenBlacklistService
- [ ] In doFilterInternal(), extract token
- [ ] Check if token is blacklisted BEFORE validation
- [ ] If blacklisted, return 401 with error code "TOKEN_REVOKED"
- [ ] If valid, extract session ID and update session activity
- [ ] Add error handling for blacklist check failures

**Task 3.4.2**: Create DeviceFingerprintUtil
- [ ] Create `DeviceFingerprintUtil.java` in util package
- [ ] Implement `generateFingerprint(HttpServletRequest request)`
- [ ] Combine User-Agent, Accept-Language, IP address
- [ ] Generate SHA-256 hash
- [ ] Implement `parseUserAgent(String userAgent)`
- [ ] Extract device type, browser, OS

### 3.5 Controller Layer

**Task 3.5.1**: Enhance AuthController
- [ ] Modify login endpoint to return LoginResponse with accessToken and refreshToken
- [ ] Create refresh token and session after login
- [ ] Add POST /api/auth/refresh endpoint
- [ ] Accept RefreshTokenRequest with refreshToken field
- [ ] Call authService.refreshAccessToken()
- [ ] Return new token pair
- [ ] Modify logout endpoint to accept both tokens
- [ ] Add POST /api/auth/logout-all endpoint
- [ ] Revoke all user sessions

**Task 3.5.2**: Create SessionController
- [ ] Create `SessionController.java` in controller package
- [ ] Add @RestController and @RequestMapping("/api/auth/sessions")
- [ ] Implement GET /sessions endpoint
- [ ] Return list of active sessions for current user
- [ ] Implement DELETE /sessions/{sessionId} endpoint
- [ ] Revoke specific session
- [ ] Add @PreAuthorize for authenticated users

### 3.6 DTOs

**Task 3.6.1**: Update LoginResponse DTO
- [ ] Add refreshToken field
- [ ] Add expiresIn field (access token expiration in seconds)
- [ ] Keep existing accessToken, username, role fields

**Task 3.6.2**: Create RefreshTokenRequest DTO
- [ ] Create in dto/request package
- [ ] Add refreshToken field
- [ ] Add @NotBlank validation

**Task 3.6.3**: Create RefreshTokenResponse DTO
- [ ] Create in dto/response package
- [ ] Add accessToken, refreshToken, expiresIn fields

**Task 3.6.4**: Create UserSessionResponse DTO
- [ ] Create in dto/response package
- [ ] Add fields: id, deviceType, browser, os, location, lastActivity, current
- [ ] Add method to format location (city, country)

### 3.7 Scheduled Tasks

**Task 3.7.1**: Create TokenCleanupScheduler
- [ ] Create `TokenCleanupScheduler.java` in scheduler package
- [ ] Add @Component and @EnableScheduling
- [ ] Create @Scheduled method to run daily
- [ ] Call refreshTokenService.cleanupExpiredTokens()
- [ ] Call tokenBlacklistService.cleanupExpiredEntries()
- [ ] Add logging


### 3.8 Frontend Integration

**Task 3.8.1**: Update authService.js
- [ ] Modify login() to store both access and refresh tokens
- [ ] Store tokens in localStorage: accessToken, refreshToken
- [ ] Implement refreshAccessToken() method
- [ ] Call POST /api/auth/refresh with refresh token
- [ ] Update stored tokens on success
- [ ] Redirect to login on failure (expired refresh token)

**Task 3.8.2**: Implement automatic token refresh
- [ ] Create token refresh interval (check every minute)
- [ ] Calculate time until access token expiration
- [ ] If < 5 minutes remaining, call refreshAccessToken()
- [ ] Handle refresh failures gracefully
- [ ] Stop refresh interval on logout

**Task 3.8.3**: Create ActiveSessions component
- [ ] Create `ActiveSessions.jsx` in components/user
- [ ] Fetch sessions from GET /api/auth/sessions
- [ ] Display session list with device info, location, last activity
- [ ] Add "Revoke" button for each session
- [ ] Add "Logout All Devices" button
- [ ] Highlight current session
- [ ] Auto-refresh every 30 seconds

**Task 3.8.4**: Update logout functionality
- [ ] Modify logout() to send both tokens to backend
- [ ] Clear localStorage
- [ ] Stop token refresh interval
- [ ] Redirect to login page

### 3.9 Testing

**Task 3.9.1**: Unit test RefreshTokenService
- [ ] Create `RefreshTokenServiceTest.java`
- [ ] Test createRefreshToken()
- [ ] Test validateRefreshToken() with valid and invalid tokens
- [ ] Test revokeRefreshToken()
- [ ] Test rotateRefreshToken() atomicity
- [ ] Test cleanupExpiredTokens()
- [ ] Mock repository dependencies

**Task 3.9.2**: Unit test TokenBlacklistService
- [ ] Create `TokenBlacklistServiceTest.java`
- [ ] Test addToBlacklist()
- [ ] Test isBlacklisted() with blacklisted and non-blacklisted tokens
- [ ] Test cleanupExpiredEntries()
- [ ] Mock repository dependencies

**Task 3.9.3**: Unit test UserSessionService
- [ ] Create `UserSessionServiceTest.java`
- [ ] Test createSession()
- [ ] Test getActiveSessions()
- [ ] Test revokeSession()
- [ ] Test enforceSessionLimit() with 6 sessions (limit 5)
- [ ] Test updateSessionActivity()
- [ ] Mock repository dependencies

**Task 3.9.4**: Integration test token refresh flow
- [ ] Create `TokenRefreshIntegrationTest.java`
- [ ] Test login returns both tokens
- [ ] Test refresh endpoint with valid refresh token
- [ ] Verify new tokens returned
- [ ] Verify old refresh token revoked
- [ ] Test refresh with expired token (should fail)
- [ ] Test refresh with blacklisted token (should fail)

**Task 3.9.5**: Integration test session management
- [ ] Create `SessionManagementIntegrationTest.java`
- [ ] Test creating multiple sessions
- [ ] Test session limit enforcement
- [ ] Test revoking specific session
- [ ] Test logout all sessions
- [ ] Verify tokens blacklisted after logout

**Task 3.9.6**: Property-based test token rotation
- [ ] Create property test for token rotation atomicity
- [ ] Generate random tokens
- [ ] Verify old token blacklisted and new token created
- [ ] Run 100+ iterations


---

## PHASE 4: EXTENDED CONFIGURATION (Week 4)

### 4.1 Database Schema

**Task 4.1.1**: Extend system_config table
- [ ] Write migration script `V2.6__extend_system_config.sql`
- [ ] Add column: config_type VARCHAR(50)
- [ ] Add column: effective_from TIMESTAMP
- [ ] Create index on config_type
- [ ] Create index on effective_from
- [ ] Test migration

### 4.2 Entity and Repository Layer

**Task 4.2.1**: Create TokenExpirationConfig entity
- [ ] Create `TokenExpirationConfig.java` in entity package
- [ ] Use @Entity with table name "system_config"
- [ ] Add discriminator or filter for config_type = "TOKEN_EXPIRATION"
- [ ] Add fields: accessTokenExpirationMinutes, refreshTokenExpirationDays
- [ ] Add audit fields: effectiveFrom, updatedBy
- [ ] Generate getters/setters

**Task 4.2.2**: Create DuplicateDetectionConfig entity
- [ ] Create `DuplicateDetectionConfig.java` in entity package
- [ ] Use @Entity with table name "system_config"
- [ ] Add discriminator or filter for config_type = "DUPLICATE_DETECTION"
- [ ] Add fields: detectionRadiusMeters, timeWindowHours, requireSameCategory
- [ ] Add audit fields: effectiveFrom, updatedBy
- [ ] Generate getters/setters

**Task 4.2.3**: Create configuration repositories
- [ ] Create `TokenExpirationConfigRepository.java`
- [ ] Create `DuplicateDetectionConfigRepository.java`
- [ ] Add method to find current configuration (latest effective_from)

### 4.3 Service Layer

**Task 4.3.1**: Enhance ConfigService
- [ ] Add method: `getTokenExpirationConfig()`
- [ ] Return current token expiration configuration
- [ ] Add method: `updateTokenExpirationConfig(TokenExpirationRequest request)`
- [ ] Validate configuration values
- [ ] Save new configuration with current timestamp
- [ ] Audit configuration change
- [ ] Add method: `getDuplicateDetectionConfig()`
- [ ] Add method: `updateDuplicateDetectionConfig(DuplicateDetectionRequest request)`
- [ ] Validate configuration values
- [ ] Save and audit changes

**Task 4.3.2**: Enhance JwtTokenProvider to use dynamic expiration
- [ ] Inject ConfigService
- [ ] In generateAccessToken(), fetch current config
- [ ] Use config.getAccessTokenExpirationMinutes() for expiration
- [ ] In generateRefreshToken(), use config.getRefreshTokenExpirationDays()
- [ ] Cache configuration to avoid repeated database queries

**Task 4.3.3**: Enhance DeduplicationService to use dynamic parameters
- [ ] Inject ConfigService
- [ ] In isDuplicate(), fetch current config
- [ ] Use config.getDetectionRadiusMeters() for spatial query
- [ ] Use config.getTimeWindowHours() for time threshold
- [ ] Use config.isRequireSameCategory() for category filtering
- [ ] Cache configuration


### 4.4 Controller Layer

**Task 4.4.1**: Enhance ConfigController
- [ ] Add GET /api/admin/config/token-expiration endpoint
- [ ] Return current token expiration configuration
- [ ] Add PUT /api/admin/config/token-expiration endpoint
- [ ] Accept TokenExpirationRequest
- [ ] Validate and update configuration
- [ ] Add GET /api/admin/config/duplicate-detection endpoint
- [ ] Add PUT /api/admin/config/duplicate-detection endpoint
- [ ] Add @PreAuthorize("hasRole('ADMIN')") to all endpoints

### 4.5 DTOs

**Task 4.5.1**: Create TokenExpirationRequest DTO
- [ ] Create in dto/request package
- [ ] Add fields: accessTokenExpirationMinutes, refreshTokenExpirationDays
- [ ] Add validation: @Min(5) @Max(60) for access token
- [ ] Add validation: @Min(1) @Max(30) for refresh token
- [ ] Add custom validator: access < refresh

**Task 4.5.2**: Create TokenExpirationResponse DTO
- [ ] Create in dto/response package
- [ ] Add fields: accessTokenExpirationMinutes, refreshTokenExpirationDays, effectiveFrom, updatedBy

**Task 4.5.3**: Create DuplicateDetectionRequest DTO
- [ ] Create in dto/request package
- [ ] Add fields: detectionRadiusMeters, timeWindowHours, requireSameCategory
- [ ] Add validation: @Min(10) @Max(1000) for radius
- [ ] Add validation: @Min(1) @Max(168) for time window

**Task 4.5.4**: Create DuplicateDetectionResponse DTO
- [ ] Create in dto/response package
- [ ] Add all configuration fields plus audit fields

### 4.6 Validation

**Task 4.6.1**: Create TokenExpirationValidator
- [ ] Create custom validator class
- [ ] Implement validation logic: accessTokenExpiration < refreshTokenExpiration
- [ ] Add error message for validation failure

### 4.7 Testing

**Task 4.7.1**: Unit test ConfigService
- [ ] Create `ConfigServiceTest.java`
- [ ] Test getTokenExpirationConfig()
- [ ] Test updateTokenExpirationConfig() with valid data
- [ ] Test validation failures (invalid ranges)
- [ ] Test getDuplicateDetectionConfig()
- [ ] Test updateDuplicateDetectionConfig()
- [ ] Mock repository dependencies

**Task 4.7.2**: Integration test configuration endpoints
- [ ] Create `ConfigurationIntegrationTest.java`
- [ ] Test GET /api/admin/config/token-expiration
- [ ] Test PUT with valid configuration
- [ ] Test PUT with invalid configuration (should return 400)
- [ ] Verify configuration persisted in database
- [ ] Verify audit log entry created
- [ ] Test duplicate detection configuration similarly

**Task 4.7.3**: Test dynamic token expiration
- [ ] Update token expiration configuration
- [ ] Generate new tokens
- [ ] Verify tokens use new expiration times
- [ ] Verify old tokens still use old expiration

**Task 4.7.4**: Test dynamic duplicate detection
- [ ] Update duplicate detection configuration
- [ ] Create reports to test deduplication
- [ ] Verify new parameters applied
- [ ] Test with different radius and time window values


---

## PHASE 5: PERFORMANCE TESTING & MONITORING (Week 5)

### 5.1 Monitoring Setup

**Task 5.1.1**: Add Actuator dependency
- [ ] Add spring-boot-starter-actuator to pom.xml
- [ ] Add micrometer-registry-prometheus to pom.xml

**Task 5.1.2**: Configure Actuator
- [ ] Add actuator properties to application.properties
- [ ] Expose endpoints: health, metrics, prometheus
- [ ] Enable detailed health information
- [ ] Enable JVM, process, and system metrics

**Task 5.1.3**: Create ActuatorConfig
- [ ] Create `ActuatorConfig.java` in config package
- [ ] Configure MeterRegistry with common tags
- [ ] Add application name tag
- [ ] Enable histogram for HTTP request metrics

### 5.2 Performance Metrics Service

**Task 5.2.1**: Create PerformanceMetricsService
- [ ] Create `PerformanceMetricsService.java` in service package
- [ ] Inject MeterRegistry
- [ ] Implement `getAggregatedMetrics(TimeRange range)`
- [ ] Query metrics from MeterRegistry
- [ ] Implement `getResponseTimePercentiles()`
- [ ] Calculate p95, p99 from histogram data
- [ ] Implement `getErrorRate()`
- [ ] Calculate error percentage from request counts
- [ ] Implement `getActiveConnections()`
- [ ] Query HikariCP metrics
- [ ] Implement `getMemoryUsage()`
- [ ] Implement `getCPUUsage()`

**Task 5.2.2**: Create PerformanceMetricsController
- [ ] Create `PerformanceMetricsController.java` in controller package
- [ ] Add GET /api/admin/metrics/performance endpoint
- [ ] Support filtering by time range
- [ ] Return aggregated performance metrics
- [ ] Add @PreAuthorize("hasRole('ADMIN')")

### 5.3 Database Connection Pooling

**Task 5.3.1**: Configure HikariCP
- [ ] Add HikariCP properties to application.properties
- [ ] Set maximum-pool-size=20
- [ ] Set minimum-idle=5
- [ ] Set connection-timeout=30000
- [ ] Set idle-timeout=600000
- [ ] Set max-lifetime=1800000
- [ ] Enable leak-detection-threshold=60000

**Task 5.3.2**: Monitor connection pool
- [ ] Verify HikariCP metrics exposed via Actuator
- [ ] Test connection pool under load
- [ ] Monitor active connections, idle connections, waiting threads

### 5.4 Circuit Breaker

**Task 5.4.1**: Add Resilience4j dependency
- [ ] Add resilience4j-spring-boot3 to pom.xml

**Task 5.4.2**: Configure circuit breaker
- [ ] Add resilience4j properties to application.properties
- [ ] Configure emailService circuit breaker
- [ ] Set failure-rate-threshold=50
- [ ] Set wait-duration-in-open-state=60000
- [ ] Set sliding-window-size=10

**Task 5.4.3**: Apply circuit breaker to EmailService
- [ ] Add @CircuitBreaker annotation to email methods
- [ ] Specify fallback method
- [ ] Implement fallback: log failure to notification_failures
- [ ] Test circuit breaker with simulated failures


### 5.5 Load Testing

**Task 5.5.1**: Install load testing tool
- [ ] Choose tool: Apache JMeter or Gatling
- [ ] Install and configure

**Task 5.5.2**: Create load test script - Normal Load
- [ ] Create test plan for 50 concurrent users
- [ ] Set ramp-up period: 2 minutes
- [ ] Set duration: 10 minutes
- [ ] Add HTTP requests for key endpoints:
  - POST /api/auth/login
  - GET /api/reports
  - POST /api/reports
  - GET /api/tasks
  - PUT /api/tasks/{id}/state
  - GET /api/analytics/tasks/distribution/category
- [ ] Set operation mix: 70% reads, 30% writes
- [ ] Add assertions for response time and status codes
- [ ] Add listeners for results collection

**Task 5.5.3**: Create load test script - Peak Load
- [ ] Create test plan for 100 concurrent users
- [ ] Set ramp-up: 1 minute
- [ ] Set duration: 5 minutes
- [ ] Use same endpoints as normal load
- [ ] Set operation mix: 60% reads, 40% writes

**Task 5.5.4**: Create load test script - Stress Test
- [ ] Create test plan for 200 concurrent users
- [ ] Set ramp-up: 30 seconds
- [ ] Set duration: 3 minutes
- [ ] Use same endpoints
- [ ] Set operation mix: 50% reads, 50% writes

**Task 5.5.5**: Run load tests
- [ ] Execute normal load test
- [ ] Collect metrics: response time, throughput, error rate
- [ ] Execute peak load test
- [ ] Execute stress test
- [ ] Identify bottlenecks

**Task 5.5.6**: Analyze results
- [ ] Calculate average response time per endpoint
- [ ] Calculate p95, p99 response times
- [ ] Calculate throughput (requests/second)
- [ ] Calculate error rate
- [ ] Monitor database connection pool usage
- [ ] Monitor memory and CPU usage
- [ ] Verify SLA compliance:
  - Simple queries < 500ms (p95)
  - Analytics queries < 2s (p95)
  - Success rate > 99.9%

**Task 5.5.7**: Optimize based on results
- [ ] Identify slow queries
- [ ] Add missing indexes if needed
- [ ] Tune cache TTL values
- [ ] Adjust connection pool size if needed
- [ ] Optimize slow endpoints
- [ ] Re-run tests to verify improvements

### 5.6 Alerting

**Task 5.6.1**: Define alert conditions
- [ ] Average response time > 1 second for 5 minutes
- [ ] Error rate > 1% for 5 minutes
- [ ] Database connection pool > 90% utilization
- [ ] Memory usage > 85%
- [ ] CPU usage > 80% for 10 minutes

**Task 5.6.2**: Implement alert logging
- [ ] Create AlertService to check conditions
- [ ] Log alerts to audit system
- [ ] Send email notifications to administrators
- [ ] Add @Scheduled method to check conditions every minute


### 5.7 Testing

**Task 5.7.1**: Test Actuator endpoints
- [ ] Test GET /actuator/health
- [ ] Verify health status and details
- [ ] Test GET /actuator/metrics
- [ ] Verify metrics available
- [ ] Test GET /actuator/prometheus
- [ ] Verify Prometheus format

**Task 5.7.2**: Test performance metrics endpoint
- [ ] Test GET /api/admin/metrics/performance
- [ ] Verify response structure
- [ ] Verify metrics accuracy
- [ ] Test with different time ranges

**Task 5.7.3**: Test circuit breaker
- [ ] Simulate email service failures
- [ ] Verify circuit breaker opens after threshold
- [ ] Verify fallback method called
- [ ] Verify circuit breaker closes after wait duration

---

## PHASE 6: API DOCUMENTATION (Week 6)

### 6.1 Setup

**Task 6.1.1**: Add SpringDoc dependency
- [ ] Add springdoc-openapi-starter-webmvc-ui to pom.xml
- [ ] Version 2.3.0 or later

**Task 6.1.2**: Configure SpringDoc
- [ ] Add springdoc properties to application.properties
- [ ] Set api-docs path: /v3/api-docs
- [ ] Set swagger-ui path: /api/docs
- [ ] Enable operations sorting by method
- [ ] Enable tags sorting alphabetically
- [ ] Enable try-it-out feature

**Task 6.1.3**: Create OpenAPIConfig
- [ ] Create `OpenAPIConfig.java` in config package
- [ ] Configure OpenAPI bean
- [ ] Set API title, description, version
- [ ] Add contact information
- [ ] Add license information
- [ ] Configure JWT security scheme (bearerAuth)
- [ ] Add security requirement globally

### 6.2 Controller Documentation

**Task 6.2.1**: Document AuthController
- [ ] Add @Tag annotation with name and description
- [ ] Add @Operation annotations to all endpoints
- [ ] Add summary and description
- [ ] Add @ApiResponse annotations for all status codes
- [ ] Document 200, 201, 400, 401, 403 responses
- [ ] Add @Parameter annotations to method parameters
- [ ] Add example values

**Task 6.2.2**: Document ReportController
- [ ] Add @Tag annotation
- [ ] Add @Operation annotations
- [ ] Document all endpoints: GET, POST
- [ ] Add @ApiResponse annotations
- [ ] Document multipart/form-data for photo upload
- [ ] Add parameter descriptions

**Task 6.2.3**: Document TaskController
- [ ] Add @Tag annotation
- [ ] Add @Operation annotations
- [ ] Document all endpoints: GET, PUT, PATCH
- [ ] Add @ApiResponse annotations
- [ ] Add parameter descriptions

**Task 6.2.4**: Document AnalyticsController
- [ ] Add @Tag annotation
- [ ] Add @Operation annotations
- [ ] Document all analytics endpoints
- [ ] Add detailed descriptions for query parameters
- [ ] Add example responses

**Task 6.2.5**: Document ConfigController
- [ ] Add @Tag annotation
- [ ] Add @Operation annotations
- [ ] Document configuration endpoints
- [ ] Add validation constraint descriptions
- [ ] Add example configurations

**Task 6.2.6**: Document NotificationPreferenceController
- [ ] Add @Tag annotation
- [ ] Add @Operation annotations
- [ ] Document preference endpoints
- [ ] Add descriptions for notification types

**Task 6.2.7**: Document SessionController
- [ ] Add @Tag annotation
- [ ] Add @Operation annotations
- [ ] Document session management endpoints
- [ ] Add descriptions for session fields


### 6.3 DTO Documentation

**Task 6.3.1**: Document request DTOs
- [ ] Add @Schema annotation to all request DTO classes
- [ ] Add description at class level
- [ ] Add @Schema annotations to all fields
- [ ] Add description, example, required for each field
- [ ] Document validation constraints in descriptions
- [ ] Cover: LoginRequest, RegisterRequest, ReportSubmissionRequest, TaskUpdateRequest, etc.

**Task 6.3.2**: Document response DTOs
- [ ] Add @Schema annotation to all response DTO classes
- [ ] Add descriptions and examples
- [ ] Cover: LoginResponse, ReportResponse, TaskResponse, AnalyticsResponse, etc.

**Task 6.3.3**: Document error response
- [ ] Add @Schema to ErrorResponse class
- [ ] Document error structure: errorCode, message, timestamp, details
- [ ] Add examples for common errors

### 6.4 Testing and Verification

**Task 6.4.1**: Test Swagger UI
- [ ] Access http://localhost:8080/api/docs
- [ ] Verify all endpoints listed
- [ ] Verify endpoints grouped by tags
- [ ] Verify request/response schemas displayed
- [ ] Verify examples shown

**Task 6.4.2**: Test interactive documentation
- [ ] Test "Try it out" feature for public endpoints
- [ ] Test authentication with JWT token
- [ ] Test protected endpoints with token
- [ ] Verify request/response match actual API behavior

**Task 6.4.3**: Verify OpenAPI spec
- [ ] Access http://localhost:8080/v3/api-docs
- [ ] Verify JSON structure
- [ ] Verify all endpoints included
- [ ] Verify schemas defined
- [ ] Verify security schemes configured

**Task 6.4.4**: Generate API documentation export
- [ ] Export OpenAPI JSON spec
- [ ] Save to project documentation
- [ ] Consider generating PDF or HTML documentation

---

## FINAL TASKS

### Integration and Testing

**Task F.1**: End-to-end integration test
- [ ] Create comprehensive integration test
- [ ] Test complete user flows:
  - Register → Login → Create report → Assign task → Resolve task → Receive notifications
  - Admin: Configure system → View analytics → Manage sessions
- [ ] Verify all modules work together
- [ ] Test with real database and email server

**Task F.2**: Security audit
- [ ] Review all authentication/authorization logic
- [ ] Verify token security (hashing, blacklist, rotation)
- [ ] Test for common vulnerabilities (SQL injection, XSS, CSRF)
- [ ] Verify input validation on all endpoints
- [ ] Review error messages (no sensitive data leaked)

**Task F.3**: Performance validation
- [ ] Run final load tests
- [ ] Verify all SLA targets met
- [ ] Document performance metrics
- [ ] Create performance baseline for monitoring

**Task F.4**: Documentation review
- [ ] Review all API documentation
- [ ] Verify completeness and accuracy
- [ ] Update README with new features
- [ ] Create deployment guide
- [ ] Create operations manual

### Deployment Preparation

**Task F.5**: Database migration review
- [ ] Review all migration scripts
- [ ] Test migrations on clean database
- [ ] Test rollback scripts
- [ ] Document migration procedure

**Task F.6**: Configuration review
- [ ] Review all application.properties
- [ ] Document all environment variables
- [ ] Create .env.example files
- [ ] Document configuration options

**Task F.7**: Docker configuration
- [ ] Update Dockerfile if needed
- [ ] Update docker-compose.yml
- [ ] Add MailHog service for testing
- [ ] Test Docker deployment

**Task F.8**: Monitoring setup
- [ ] Configure production monitoring
- [ ] Set up alerting
- [ ] Create monitoring dashboard
- [ ] Document monitoring procedures

---

## TASK SUMMARY

**Total Tasks**: 85 tasks

**By Phase**:
- Phase 1 (Notifications): 18 tasks
- Phase 2 (Analytics): 17 tasks
- Phase 3 (Sessions): 24 tasks
- Phase 4 (Configuration): 14 tasks
- Phase 5 (Performance): 17 tasks
- Phase 6 (Documentation): 15 tasks
- Final Tasks: 8 tasks

**Estimated Effort**: 6 weeks (1 phase per week + final tasks)

**Priority**: High - Completes all pending IDRQ requirements

---

**Document Version**: 1.0  
**Last Updated**: 9 de febrero de 2026  
**Status**: Ready for Implementation  
**Next Step**: Begin Phase 1 implementation
