# Requirements Document: Operational Excellence

## Introduction

This document specifies the requirements for completing the Urban Cleaning Management System with operational excellence features. This spec consolidates all pending functional and non-functional requirements from the IDRQ analysis, including:

- **IDRQ-RF-07**: Complete notification system with preferences management
- **IDRQ-RF-08**: Analytics dashboard with KPIs, heatmaps, and MTTR
- **IDRQ-RF-11**: Extended system configuration (token expiration, duplicate detection)
- **IDRQ-RNF-01**: Enhanced security with refresh tokens and session management
- **IDRQ-RNF-04**: Performance testing and validation

This spec ensures 100% coverage of all IDRQ requirements, making the system production-ready.

## Glossary

- **System**: The Urban Cleaning Management WebApp
- **MTTR**: Mean Time To Resolution - average time to resolve tasks
- **Heatmap**: Geographic visualization showing incident concentration
- **KPI**: Key Performance Indicator - metrics for operational decision-making
- **Refresh_Token**: Long-lived token (7 days) used to obtain new access tokens
- **Access_Token**: Short-lived JWT token (15 minutes) used for API authentication
- **Token_Blacklist**: Database table storing revoked tokens
- **Session**: An authenticated user connection identified by a refresh token
- **Notification_Preference**: User settings controlling which notifications they receive
- **Load_Testing**: Performance testing under simulated concurrent user load

---

## MODULE 1: NOTIFICATION SYSTEM COMPLETION (IDRQ-RF-07)

### Requirement 1: Task Assignment Notifications

**User Story:** As an operator, I want to receive email notifications when tasks are assigned to me, so that I can respond promptly.

**IDRQ Mapping**: IDRQ-RF-07 (Sistema de Alertas Asíncronas)

#### Acceptance Criteria

1. WHEN a task is assigned to an operator, THE System SHALL send an email notification to that operator
2. THE System SHALL include task details in the notification: task ID, category, location, priority score
3. THE System SHALL process email sending asynchronously using Spring Events
4. WHEN email sending fails, THE System SHALL log the failure and retry up to 3 times
5. THE System SHALL use exponential backoff for retry attempts (1 minute, 5 minutes, 15 minutes)
6. THE System SHALL use HTML templates for email formatting with responsive design
7. THE System SHALL respect user's notification preferences before sending

---

### Requirement 2: Notification Preferences Management

**User Story:** As a user, I want to control which notifications I receive, so that I only get relevant alerts.

**IDRQ Mapping**: IDRQ-RF-07 (Validación de que el usuario tiene notificaciones activadas)

#### Acceptance Criteria

1. THE System SHALL provide an endpoint GET /api/users/notifications/preferences to retrieve notification preferences
2. THE System SHALL provide an endpoint PUT /api/users/notifications/preferences to update preferences
3. THE System SHALL support the following notification types: TASK_ASSIGNED, TASK_RESOLVED, TASK_REOPENED, REPORT_CREATED
4. WHEN a user registers, THE System SHALL enable all notification types by default
5. THE System SHALL allow users to enable or disable each notification type independently
6. WHEN notification preferences are updated, THE System SHALL apply changes immediately
7. THE System SHALL store notification preferences in the database linked to the user account
8. THE System SHALL include notification preferences in user data exports for GDPR compliance
9. WHEN sending notifications, THE System SHALL check user preferences and skip disabled types

---

### Requirement 3: Notification Failure Tracking

**User Story:** As an administrator, I want to track failed email notifications, so that I can investigate delivery issues.

**IDRQ Mapping**: IDRQ-RF-07 (En caso de fallo persistente, el evento se registra para reintento o análisis posterior)

#### Acceptance Criteria

1. WHEN an email cannot be delivered after all retries, THE System SHALL record the failure in a notification_failures table
2. THE System SHALL store: user_id, notification_type, email_address, failure_reason, attempted_at, retry_count
3. THE System SHALL provide an endpoint GET /api/admin/notifications/failures for administrators to review failures
4. THE System SHALL support filtering failures by date range, notification type, and user
5. THE System SHALL provide an endpoint POST /api/admin/notifications/failures/{id}/retry to manually retry failed notifications
6. THE System SHALL automatically clean up failure records older than 30 days

---

### Requirement 4: Unsubscribe Functionality

**User Story:** As a user, I want to unsubscribe from specific notification types via email, so that I can manage my preferences easily.

**IDRQ Mapping**: IDRQ-RF-07 (Sistema de Alertas Asíncronas - Best Practice)

#### Acceptance Criteria

1. THE System SHALL include unsubscribe links in all notification emails
2. THE System SHALL generate secure unsubscribe tokens with user_id and notification_type encoded
3. WHEN a user clicks an unsubscribe link, THE System SHALL disable that notification type for the user
4. THE System SHALL display a confirmation page after successful unsubscribe
5. THE System SHALL provide a link to manage all notification preferences from the unsubscribe page
6. THE System SHALL log all unsubscribe actions for audit purposes

---

## MODULE 2: ANALYTICS DASHBOARD (IDRQ-RF-08)

### Requirement 5: Task Distribution Analytics

**User Story:** As an administrator, I want to view task distribution metrics, so that I can understand workload patterns.

**IDRQ Mapping**: IDRQ-RF-08 (Visualización de Datos Agregados y KPIs - Distribución por categorías)

#### Acceptance Criteria

1. THE System SHALL provide an endpoint GET /api/analytics/tasks/distribution/category to retrieve task distribution by category
2. THE System SHALL provide an endpoint GET /api/analytics/tasks/distribution/state to retrieve task distribution by state
3. WHEN retrieving task distribution, THE System SHALL return counts and percentages for each category/state
4. THE System SHALL use database aggregation functions (GROUP BY, COUNT) for efficient queries
5. THE System SHALL cache analytics results for 5 minutes using Spring Cache to reduce database load
6. THE System SHALL support filtering analytics by date range using start_date and end_date parameters
7. THE System SHALL support filtering analytics by geographic zone using zone_id parameter
8. WHEN no filters are applied, THE System SHALL return data for the last 30 days
9. THE System SHALL return results in JSON format with clear structure
10. THE System SHALL include metadata in responses: total_tasks, date_range, filters_applied
11. THE System SHALL respond within 2 seconds for typical queries

---

### Requirement 6: Mean Time To Resolution (MTTR)

**User Story:** As an administrator, I want to view MTTR metrics, so that I can measure operational efficiency.

**IDRQ Mapping**: IDRQ-RF-08 (Visualización de Datos Agregados y KPIs - MTTR)

#### Acceptance Criteria

1. THE System SHALL provide an endpoint GET /api/analytics/tasks/mttr to calculate Mean Time To Resolution
2. WHEN calculating MTTR, THE System SHALL compute the average time between task creation and resolution
3. THE System SHALL express MTTR in hours with two decimal places
4. THE System SHALL calculate MTTR only for tasks in RESUELTO state
5. THE System SHALL support filtering MTTR by category, zone, and date range
6. THE System SHALL provide an endpoint GET /api/analytics/tasks/resolution-time-distribution to retrieve histogram data
7. THE System SHALL categorize resolution times into buckets: <24h, 24-48h, 48-72h, >72h
8. THE System SHALL calculate average priority score for resolved tasks
9. THE System SHALL cache MTTR metrics for 5 minutes
10. THE System SHALL respond within 2 seconds

---

### Requirement 7: Geographic Heatmap

**User Story:** As an administrator, I want to view a heatmap of incident concentration, so that I can identify problem areas.

**IDRQ Mapping**: IDRQ-RF-08 (Visualización de Datos Agregados y KPIs - Mapa de calor)

#### Acceptance Criteria

1. THE System SHALL provide an endpoint GET /api/analytics/heatmap to generate heatmap data
2. THE System SHALL divide the municipality into a grid of configurable cell size (default 500m x 500m)
3. WHEN generating heatmap data, THE System SHALL count reports within each grid cell using PostGIS
4. THE System SHALL return heatmap data as an array of objects with: latitude, longitude, intensity (count)
5. THE System SHALL use PostGIS spatial functions (ST_SnapToGrid, ST_Count) for efficient geographic aggregation
6. THE System SHALL support filtering heatmap by category, date range, and task state
7. THE System SHALL normalize intensity values to a 0-1 scale for visualization
8. THE System SHALL include only cells with at least one report to reduce data size
9. THE System SHALL cache heatmap data for 10 minutes due to computational cost
10. THE System SHALL support different aggregation levels: neighborhood, district, municipality
11. WHEN generating heatmap for large areas, THE System SHALL limit results to top 1000 cells by intensity
12. THE System SHALL respond within 3 seconds for municipality-wide data

---

### Requirement 8: Operator Performance Metrics

**User Story:** As an administrator, I want to view operator performance metrics, so that I can evaluate team efficiency.

**IDRQ Mapping**: IDRQ-RF-08 (Visualización de Datos Agregados y KPIs)

#### Acceptance Criteria

1. THE System SHALL provide an endpoint GET /api/analytics/operators/performance to retrieve operator metrics
2. THE System SHALL calculate for each operator: tasks_resolved, average_resolution_time, tasks_in_progress, tasks_reopened
3. THE System SHALL support filtering by date range and operator_id
4. THE System SHALL rank operators by tasks resolved (descending)
5. THE System SHALL include operator details: username, role, active_since
6. THE System SHALL cache operator metrics for 5 minutes
7. THE System SHALL support pagination (20 operators per page)
8. THE System SHALL respond within 2 seconds

---

## MODULE 3: EXTENDED SYSTEM CONFIGURATION (IDRQ-RF-11)

### Requirement 9: Token Expiration Configuration

**User Story:** As an administrator, I want to configure JWT token expiration times, so that I can balance security and user convenience.

**IDRQ Mapping**: IDRQ-RF-11 (Administración Dinámica de Reglas de Negocio - Tiempos de expiración de tokens)

#### Acceptance Criteria

1. THE System SHALL provide an endpoint GET /api/admin/config/token-expiration to retrieve current token expiration settings
2. THE System SHALL provide an endpoint PUT /api/admin/config/token-expiration to update expiration times
3. THE System SHALL support configuration of: access_token_expiration (minutes), refresh_token_expiration (days)
4. THE System SHALL validate that access token expiration is shorter than refresh token expiration
5. THE System SHALL validate that expiration times are within acceptable ranges (access: 5-60 minutes, refresh: 1-30 days)
6. WHEN timeout configuration changes, THE System SHALL apply new values to newly issued tokens only
7. THE System SHALL store timeout configuration in the database with effective timestamp
8. THE System SHALL audit all configuration changes with user_id, IP address, and timestamp
9. THE System SHALL require ROLE_ADMIN for configuration changes
10. THE System SHALL apply changes in runtime without requiring server restart

---

### Requirement 10: Duplicate Detection Configuration

**User Story:** As an administrator, I want to configure duplicate detection parameters, so that I can tune the deduplication algorithm.

**IDRQ Mapping**: IDRQ-RF-11 (Administración Dinámica de Reglas de Negocio - Radio de detección de duplicados)

#### Acceptance Criteria

1. THE System SHALL provide an endpoint GET /api/admin/config/duplicate-detection to retrieve current settings
2. THE System SHALL provide an endpoint PUT /api/admin/config/duplicate-detection to update settings
3. THE System SHALL support configuration of: detection_radius_meters, time_window_hours, require_same_category
4. THE System SHALL validate that detection_radius is between 10 and 1000 meters
5. THE System SHALL validate that time_window is between 1 and 168 hours (7 days)
6. WHEN duplicate detection parameters are updated, THE System SHALL apply them to new reports immediately
7. THE System SHALL store configuration history with timestamps
8. THE System SHALL audit all configuration changes
9. THE System SHALL require ROLE_ADMIN for configuration changes
10. THE System SHALL apply changes in runtime without requiring server restart

---

## MODULE 4: ENHANCED SESSION MANAGEMENT (IDRQ-RNF-01)

### Requirement 11: Refresh Token Implementation

**User Story:** As a system user, I want my session to remain active without frequent re-authentication, so that I can work efficiently.

**IDRQ Mapping**: IDRQ-RNF-01 (Seguridad - Protección de Datos y Cifrado)

#### Acceptance Criteria

1. WHEN a user successfully authenticates, THE System SHALL generate both an access token (15 minutes) and a refresh token (7 days)
2. WHEN an access token expires, THE System SHALL accept a valid refresh token to issue a new token pair
3. THE System SHALL store refresh tokens in the database with user association, expiration time, and device information
4. WHEN a refresh token is used, THE System SHALL validate it against the database before issuing new tokens
5. WHEN a refresh token expires, THE System SHALL require full re-authentication
6. THE System SHALL implement token rotation: each refresh generates a new refresh token and invalidates the old one
7. THE System SHALL store refresh tokens as hashed values in the database (SHA-256)
8. THE System SHALL bind refresh tokens to device fingerprints for additional security

---

### Requirement 12: Token Revocation and Blacklist

**User Story:** As a system user, I want to explicitly logout and invalidate my session, so that my account remains secure.

**IDRQ Mapping**: IDRQ-RNF-01 (Seguridad)

#### Acceptance Criteria

1. WHEN a user logs out, THE System SHALL revoke both the access token and refresh token
2. WHEN a token is revoked, THE System SHALL add it to a token_blacklist table
3. WHEN a revoked token is used, THE System SHALL reject the request with error code "TOKEN_REVOKED"
4. THE System SHALL automatically clean up expired tokens from the blacklist after 30 days
5. WHERE ROLE_ADMIN is assigned, THE System SHALL provide endpoints to revoke all sessions for a specific user
6. WHEN an administrator revokes a user's sessions, THE System SHALL invalidate all active refresh tokens for that user
7. THE System SHALL log all token revocations for security auditing

---

### Requirement 13: Multi-Device Session Management

**User Story:** As a system user, I want to manage my active sessions across multiple devices, so that I can control where I'm logged in.

**IDRQ Mapping**: IDRQ-RNF-01 (Seguridad)

#### Acceptance Criteria

1. WHEN a user logs in, THE System SHALL create a session record with device fingerprint, IP address, user agent, and login timestamp
2. THE System SHALL allow multiple concurrent sessions per user (up to 5 active sessions)
3. WHEN a user exceeds the session limit, THE System SHALL revoke the oldest session automatically
4. THE System SHALL provide an endpoint GET /api/auth/sessions to list all active sessions for the current user
5. THE System SHALL display session information: device type, browser, location (city/country), last activity time
6. WHEN a user requests to revoke a specific session, THE System SHALL invalidate that session's refresh token
7. WHEN a user requests to revoke all other sessions, THE System SHALL invalidate all refresh tokens except the current one
8. THE System SHALL provide an endpoint POST /api/auth/logout-all to logout from all devices

---

### Requirement 14: Automatic Token Refresh

**User Story:** As a system user, I want my session to automatically renew while I'm actively using the system, so that I don't experience interruptions.

**IDRQ Mapping**: IDRQ-RNF-01 (Seguridad - UX)

#### Acceptance Criteria

1. WHEN an access token has 5 minutes remaining before expiration, THE Frontend SHALL automatically request a token refresh
2. THE System SHALL use the refresh token to obtain a new token pair without user interaction
3. WHEN automatic refresh succeeds, THE Frontend SHALL update stored tokens transparently
4. WHEN automatic refresh fails due to expired refresh token, THE Frontend SHALL redirect to login
5. WHEN automatic refresh fails due to network error, THE Frontend SHALL retry up to 3 times with exponential backoff
6. THE Frontend SHALL not trigger automatic refresh if the user is inactive (no mouse/keyboard activity for 10 minutes)
7. THE System SHALL provide an endpoint POST /api/auth/refresh for token renewal

---

## MODULE 5: PERFORMANCE TESTING (IDRQ-RNF-04)

### Requirement 15: Load Testing Requirements

**User Story:** As a DevOps engineer, I want to validate system performance under load, so that I can ensure the system meets SLA requirements.

**IDRQ Mapping**: IDRQ-RNF-04 (Rendimiento del Sistema - Pruebas de estrés)

#### Acceptance Criteria

1. THE System SHALL be tested with 50 concurrent users making simultaneous API requests
2. WHEN under load, THE System SHALL maintain average response time below 500ms for simple queries (GET /api/reports, GET /api/tasks)
3. WHEN under load, THE System SHALL maintain average response time below 2 seconds for analytics queries (GET /api/analytics/*)
4. THE System SHALL handle at least 100 requests per second without errors
5. THE System SHALL maintain 99.9% success rate under normal load conditions
6. THE System SHALL not experience memory leaks during sustained load (4 hours continuous testing)
7. THE System SHALL recover gracefully from temporary database connection issues
8. THE System SHALL implement connection pooling for database connections (HikariCP)
9. THE System SHALL implement request timeout of 30 seconds for all endpoints
10. THE System SHALL return 503 Service Unavailable when system is overloaded
11. THE System SHALL log performance metrics for monitoring and alerting
12. THE System SHALL implement circuit breaker pattern for external service calls (email service)

---

### Requirement 16: Performance Monitoring

**User Story:** As a system administrator, I want to monitor system performance metrics, so that I can detect issues proactively.

**IDRQ Mapping**: IDRQ-RNF-04 (Rendimiento del Sistema)

#### Acceptance Criteria

1. THE System SHALL expose performance metrics via Spring Boot Actuator at /actuator/metrics
2. THE System SHALL track the following metrics: request_count, request_duration, error_rate, database_connection_pool_usage, memory_usage, cpu_usage
3. THE System SHALL provide an endpoint GET /api/admin/metrics/performance to retrieve aggregated performance data
4. THE System SHALL support filtering metrics by time range (last hour, last day, last week)
5. THE System SHALL calculate and display: average_response_time, p95_response_time, p99_response_time, error_percentage
6. THE System SHALL alert administrators when error rate exceeds 1%
7. THE System SHALL alert administrators when average response time exceeds 1 second
8. THE System SHALL store performance metrics for 30 days for historical analysis

---

## MODULE 6: API DOCUMENTATION (Developer Experience)

### Requirement 17: OpenAPI Documentation

**User Story:** As a frontend developer, I want comprehensive API documentation, so that I can integrate with backend services efficiently.

**IDRQ Mapping**: IDRQ-RNF-08 (Implementación - Stack Tecnológico Definido)

#### Acceptance Criteria

1. THE System SHALL generate API documentation automatically using OpenAPI 3.0 specification
2. THE System SHALL use SpringDoc OpenAPI library for automatic documentation generation
3. THE System SHALL expose interactive API documentation at /api/docs endpoint
4. WHEN API documentation is accessed, THE System SHALL display all available endpoints with request/response schemas
5. THE System SHALL include example requests and responses for each endpoint
6. THE System SHALL document all error codes and their meanings
7. THE System SHALL keep documentation synchronized with actual API implementation automatically
8. THE System SHALL document authentication requirements for each endpoint
9. THE System SHALL include descriptions for all request parameters and response fields
10. THE System SHALL group endpoints by functional area (Auth, Reports, Tasks, Analytics, Config, Notifications)
11. THE System SHALL document all possible HTTP status codes for each endpoint
12. THE System SHALL support trying out API calls directly from the documentation interface

---

## Non-Functional Requirements

### Performance Requirements

1. Email notifications SHALL be sent asynchronously within 10 seconds of triggering event
2. Analytics endpoints SHALL respond within 2 seconds for typical queries
3. Heatmap generation SHALL complete within 3 seconds for municipality-wide data
4. Token refresh SHALL complete within 200 milliseconds
5. Configuration changes SHALL apply within 1 second

### Scalability Requirements

1. Email system SHALL handle up to 1000 notifications per minute
2. Analytics queries SHALL perform efficiently with up to 100,000 tasks in database
3. Heatmap generation SHALL scale to 50,000 reports
4. Session management SHALL support up to 10,000 concurrent users
5. System SHALL support up to 50,000 active sessions

### Reliability Requirements

1. Email failures SHALL not affect API response times or user experience
2. Analytics cache SHALL automatically refresh on expiration
3. Configuration changes SHALL be atomic (all or nothing)
4. System SHALL continue operating if email service is unavailable
5. Token refresh SHALL have 99.9% success rate

### Security Requirements

1. Refresh tokens SHALL be stored as hashed values (SHA-256)
2. Token blacklist SHALL be checked on every authenticated request
3. Session fingerprints SHALL prevent token theft across devices
4. All configuration changes SHALL be audited with IP address
5. Failed authentication attempts SHALL be logged and monitored

### Maintainability Requirements

1. Email templates SHALL be stored as separate files for easy modification
2. Configuration parameters SHALL be documented with valid ranges
3. Analytics queries SHALL be optimized and indexed
4. Code SHALL follow established project standards
5. All endpoints SHALL have comprehensive unit tests

---

## IDRQ Requirements Coverage Summary

This spec provides 100% coverage of pending IDRQ requirements:

| IDRQ ID | Requirement | Coverage |
|---------|-------------|----------|
| **RF-07** | Sistema de Notificaciones Event-Driven | ✅ Complete (Req 1-4) |
| **RF-08** | Dashboard de Analítica Operativa | ✅ Complete (Req 5-8) |
| **RF-11** | Gestión de Parámetros del Sistema | ✅ Complete (Req 9-10) |
| **RNF-01** | Seguridad (Refresh Tokens) | ✅ Complete (Req 11-14) |
| **RNF-04** | Rendimiento (Performance Testing) | ✅ Complete (Req 15-16) |

**Total Requirements**: 17 functional requirements  
**IDRQ Coverage**: 5 IDRQ requirements (100% of pending)  
**Expected Outcome**: System ready for production with full IDRQ compliance

---

**Document Version**: 1.0  
**Last Updated**: 9 de febrero de 2026  
**Status**: Ready for Design Phase
