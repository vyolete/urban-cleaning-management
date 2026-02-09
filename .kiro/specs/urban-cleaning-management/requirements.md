# Requirements Document

## Introduction

This document specifies the requirements for an Urban Cleaning Management WebApp - a collaborative full-stack solution where citizens report geolocated incidents and a backend system automatically prioritizes these tasks for municipal operators using an intelligent algorithm. The system implements role-based access control, automated task prioritization, and comprehensive audit trails.

## Glossary

- **System**: The Urban Cleaning Management WebApp
- **Citizen**: A user with ROLE_CIUDADANO who can report cleaning incidents
- **Operator**: A user with ROLE_TECNICO who manages and resolves cleaning tasks
- **Administrator**: A user with ROLE_ADMIN who configures system parameters
- **Report**: A citizen-submitted incident containing location, category, description, and photo
- **Task**: A work item created from one or more reports, assigned to operators
- **Priority_Score**: A calculated numeric value determining task urgency (P = Wc * Category + Wz * Zone + Wt * Time)
- **Task_State**: The current status of a task (PENDIENTE, ASIGNADO, EN_PROGRESO, RESUELTO, REABIERTO)
- **Geofencing**: Geographic boundary validation for coordinates
- **Deduplication**: Process of identifying and merging similar reports within proximity and time thresholds
- **Audit_Log**: Immutable record of state changes with user and timestamp information
- **JWT**: JSON Web Token used for authentication
- **BCrypt**: Password hashing algorithm
- **OTP**: One-Time Password token used for password recovery
- **MTTR**: Mean Time To Resolution - average time to resolve tasks
- **Heatmap**: Geographic visualization showing incident concentration
- **Citizen_Feedback**: User confirmation or rejection of task resolution
- **Email_Notification**: Asynchronous email alerts for system events
- **RGPD**: General Data Protection Regulation (EU privacy law)
- **RFC_5322**: Email address format standard
- **OpenAPI**: API documentation specification (formerly Swagger)

## Requirements

### Requirement 1: User Authentication and Authorization

**User Story:** As a system user, I want to authenticate securely with my credentials, so that I can access features appropriate to my role.

#### Acceptance Criteria

1. WHEN a user submits valid credentials, THE System SHALL generate a JWT token containing user identity and role information
2. WHEN a user submits invalid credentials, THE System SHALL reject the authentication attempt and return an error message
3. WHEN storing user passwords, THE System SHALL hash them using BCrypt algorithm
4. WHEN a JWT token expires, THE System SHALL require re-authentication
5. THE System SHALL support two distinct roles: ROLE_CIUDADANO and ROLE_TECNICO
6. WHERE ROLE_ADMIN is configured, THE System SHALL grant administrative privileges

### Requirement 2: Role-Based Access Control

**User Story:** As a system administrator, I want to enforce role-based permissions, so that users can only access authorized functionality.

#### Acceptance Criteria

1. WHEN a Citizen attempts to access operator endpoints, THE System SHALL deny access and return an authorization error
2. WHEN an Operator attempts to access administrative endpoints without ROLE_ADMIN, THE System SHALL deny access
3. THE System SHALL validate JWT token roles on every protected endpoint request
4. WHEN a user's role changes, THE System SHALL require a new JWT token to reflect updated permissions

### Requirement 3: Incident Report Submission

**User Story:** As a Citizen, I want to submit geolocated cleaning incidents with photos, so that municipal operators can address them.

#### Acceptance Criteria

1. WHEN a Citizen submits a report, THE System SHALL accept multipart requests containing JSON metadata and binary photo data
2. WHEN receiving report coordinates, THE System SHALL validate them against configured geofencing boundaries
3. IF coordinates fall outside geofencing boundaries, THEN THE System SHALL reject the report with a descriptive error message
4. WHEN a report is submitted, THE System SHALL store the timestamp, location, category, description, photo reference, and submitter identity
5. THE System SHALL require all reports to include latitude, longitude, category, and description fields
6. WHEN a photo is uploaded, THE System SHALL validate file type and size constraints

### Requirement 4: Automated Task Prioritization

**User Story:** As a system operator, I want tasks automatically prioritized by urgency, so that critical incidents are addressed first.

#### Acceptance Criteria

1. WHEN a new report is ingested, THE System SHALL calculate a Priority_Score using the formula: P = (Wc * Category) + (Wz * Zone) + (Wt * Time)
2. THE System SHALL retrieve weight parameters (Wc, Wz, Wt) from the Configuracion_Algoritmo table
3. WHEN calculating Category component, THE System SHALL map report category to a numeric severity value
4. WHEN calculating Zone component, THE System SHALL determine zone priority based on geographic location
5. WHEN calculating Time component, THE System SHALL increase urgency as elapsed time since report submission increases
6. WHEN Priority_Score is calculated, THE System SHALL store it with the associated task
7. WHEN Priority_Score parameters change, THE System SHALL recalculate scores for all pending tasks

### Requirement 5: Report Deduplication

**User Story:** As a system operator, I want duplicate reports automatically merged, so that I don't work on the same incident multiple times.

#### Acceptance Criteria

1. WHEN a new report is submitted, THE System SHALL search for existing reports within a configurable distance threshold
2. WHEN a spatially proximate report exists within a configurable time window, THE System SHALL mark the new report as a duplicate
3. WHEN reports are identified as duplicates, THE System SHALL group them under a single parent task
4. THE System SHALL store references to all child reports within the parent task
5. WHEN displaying a task, THE System SHALL show the count of merged duplicate reports
6. THE System SHALL use the highest Priority_Score among duplicates for the parent task

### Requirement 6: Task State Management

**User Story:** As an Operator, I want to update task status through defined workflow states, so that progress is tracked systematically.

#### Acceptance Criteria

1. WHEN a task is created, THE System SHALL initialize its state to PENDIENTE
2. WHEN an Operator assigns a task, THE System SHALL transition state from PENDIENTE to ASIGNADO
3. WHEN an Operator begins work, THE System SHALL transition state from ASIGNADO to EN_PROGRESO
4. WHEN an Operator completes work, THE System SHALL transition state from EN_PROGRESO to RESUELTO
5. IF an invalid state transition is attempted, THEN THE System SHALL reject the request and return an error
6. THE System SHALL enforce the state machine: PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO

### Requirement 7: Audit Trail

**User Story:** As an Administrator, I want immutable logs of all task state changes, so that I can track accountability and system history.

#### Acceptance Criteria

1. WHEN a task state changes, THE System SHALL create an audit log entry immediately
2. THE System SHALL record the user identity who performed the state change
3. THE System SHALL record the timestamp of the state change with millisecond precision
4. THE System SHALL record both the previous state and new state in the audit log
5. THE System SHALL prevent modification or deletion of audit log entries
6. WHEN querying audit history, THE System SHALL return entries in chronological order

### Requirement 8: Operator Dashboard

**User Story:** As an Operator, I want to view tasks ordered by priority, so that I can focus on the most urgent incidents.

#### Acceptance Criteria

1. WHEN an Operator accesses the dashboard, THE System SHALL display all tasks ordered by Priority_Score in descending order
2. WHEN displaying tasks, THE System SHALL show task identifier, location, category, current state, and Priority_Score
3. WHEN an Operator filters by state, THE System SHALL return only tasks matching the selected state
4. WHEN an Operator filters by geographic zone, THE System SHALL return only tasks within that zone
5. THE System SHALL update the dashboard view when new tasks are created or priorities change

### Requirement 9: RESTful API Design

**User Story:** As a frontend developer, I want well-defined REST endpoints, so that I can integrate the UI with backend services.

#### Acceptance Criteria

1. THE System SHALL expose authentication endpoints using POST method for login operations
2. THE System SHALL expose report submission endpoints using POST method with multipart/form-data content type
3. THE System SHALL expose task retrieval endpoints using GET method with query parameters for filtering
4. THE System SHALL expose task state update endpoints using PATCH or PUT method
5. WHEN API requests succeed, THE System SHALL return appropriate 2xx status codes
6. WHEN API requests fail due to client errors, THE System SHALL return appropriate 4xx status codes with descriptive messages
7. WHEN API requests fail due to server errors, THE System SHALL return appropriate 5xx status codes

### Requirement 10: Data Persistence

**User Story:** As a system architect, I want data stored in PostgreSQL with geospatial support, so that location-based queries are efficient.

#### Acceptance Criteria

1. THE System SHALL store user credentials in a Users table with hashed passwords
2. THE System SHALL store report data in a Reportes table with PostGIS geometry types for coordinates
3. THE System SHALL store task data in a Tareas table with foreign key references to reports
4. THE System SHALL store audit logs in a Historial_Cambios table with immutable constraints
5. THE System SHALL store algorithm configuration in a Configuracion_Algoritmo table
6. WHEN querying by location, THE System SHALL use PostGIS spatial indexes for performance
7. THE System SHALL enforce referential integrity through foreign key constraints

### Requirement 11: Containerized Deployment

**User Story:** As a DevOps engineer, I want the application containerized with Docker, so that deployment is consistent across environments.

#### Acceptance Criteria

1. THE System SHALL provide a Dockerfile for the Spring Boot backend application
2. THE System SHALL provide a Dockerfile for the React frontend application
3. THE System SHALL provide a docker-compose.yml file orchestrating all services
4. WHEN containers start, THE System SHALL initialize the PostgreSQL database with required schema
5. THE System SHALL configure environment-specific parameters through environment variables
6. THE System SHALL expose backend services on configurable ports

### Requirement 12: Security Headers and CORS

**User Story:** As a security engineer, I want proper security headers and CORS configuration, so that the application is protected against common vulnerabilities.

#### Acceptance Criteria

1. THE System SHALL configure CORS to allow requests only from authorized frontend origins
2. THE System SHALL include security headers (X-Content-Type-Options, X-Frame-Options, X-XSS-Protection) in all responses
3. THE System SHALL enforce HTTPS in production environments
4. THE System SHALL validate and sanitize all user inputs to prevent injection attacks
5. THE System SHALL implement rate limiting on authentication endpoints to prevent brute force attacks

### Requirement 13: Algorithm Configuration Management

**User Story:** As an Administrator, I want to configure prioritization algorithm weights, so that I can tune the system to local needs.

#### Acceptance Criteria

1. WHERE ROLE_ADMIN is assigned, THE System SHALL provide endpoints to update algorithm weight parameters
2. WHEN weight parameters are updated, THE System SHALL validate that values are within acceptable ranges
3. WHEN weight parameters change, THE System SHALL trigger recalculation of Priority_Score for pending tasks
4. THE System SHALL store historical weight configurations for audit purposes
5. THE System SHALL apply default weight values when no custom configuration exists

### Requirement 14: Password Recovery

**User Story:** As a user who forgot my password, I want to securely reset it via email, so that I can regain access to my account.

#### Acceptance Criteria

1. WHEN a user requests password recovery, THE System SHALL generate a cryptographically secure one-time token
2. WHEN a recovery token is generated, THE System SHALL set an expiration time of 15 minutes
3. WHEN a recovery request is submitted, THE System SHALL send an email with a unique recovery link
4. THE System SHALL not reveal whether an email address exists in the system when processing recovery requests
5. WHEN a recovery token is used, THE System SHALL invalidate it immediately
6. WHEN a recovery link expires, THE System SHALL reject password reset attempts with that token
7. THE System SHALL only accept recovery links over HTTPS connections
8. WHEN a password is successfully reset, THE System SHALL invalidate all existing JWT tokens for that user

### Requirement 15: Task Reopening and Citizen Feedback

**User Story:** As a Citizen, I want to confirm or reject task resolution, so that I can ensure my reported incident was properly addressed.

#### Acceptance Criteria

1. WHEN a task transitions to RESUELTO state, THE System SHALL send a notification to the original reporter
2. THE System SHALL provide two actions in the notification: "Confirm Solution" and "Reject/Reopen"
3. WHEN a Citizen confirms resolution, THE System SHALL mark the task as citizen-approved
4. WHEN a Citizen rejects resolution, THE System SHALL transition the task to REABIERTO state
5. WHEN reopening a task, THE System SHALL require a mandatory justification field
6. IF no citizen response is received within 72 hours, THEN THE System SHALL automatically close the task
7. THE System SHALL only allow the original reporter to provide feedback on their own reports
8. WHEN a task is reopened, THE System SHALL notify the assigned operator
9. THE System SHALL record citizen satisfaction feedback for quality statistics

### Requirement 16: Email Notification System

**User Story:** As a system user, I want to receive email notifications about important events, so that I stay informed about my reports and tasks.

#### Acceptance Criteria

1. WHEN a task state changes to RESUELTO, THE System SHALL send an email notification to the citizen reporter
2. WHEN a task is reopened, THE System SHALL send an email notification to the assigned operator
3. WHEN a new task is assigned, THE System SHALL send an email notification to the operator
4. THE System SHALL process email sending asynchronously to avoid blocking API responses
5. WHEN email sending fails, THE System SHALL log the failure and retry up to 3 times
6. THE System SHALL use HTML templates for email formatting
7. THE System SHALL allow users to enable or disable notification preferences
8. WHEN an email cannot be delivered after retries, THE System SHALL record the failure for administrator review

### Requirement 17: Analytics Dashboard

**User Story:** As an Administrator, I want to view aggregated analytics and KPIs, so that I can make data-driven operational decisions.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint to retrieve task distribution by category
2. THE System SHALL provide an endpoint to retrieve task distribution by state
3. THE System SHALL provide an endpoint to calculate Mean Time To Resolution (MTTR)
4. THE System SHALL provide an endpoint to generate heatmap data showing incident concentration by geographic area
5. WHEN analytics queries are executed, THE System SHALL use database aggregation functions (GROUP BY, COUNT, AVG)
6. THE System SHALL cache analytics results for 5 minutes to reduce database load
7. THE System SHALL support filtering analytics by date range
8. THE System SHALL support filtering analytics by geographic zone
9. WHEN loading the analytics dashboard, THE System SHALL respond within 2 seconds

### Requirement 18: User Profile Management

**User Story:** As a user, I want to manage my personal information and view my activity history, so that I can maintain control over my data.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint for users to retrieve their own profile information
2. THE System SHALL provide an endpoint for users to update their own profile information
3. THE System SHALL provide an endpoint for users to change their password
4. THE System SHALL provide an endpoint for users to view their complete report history
5. THE System SHALL provide an endpoint for users to delete their account and associated data
6. WHEN a user updates profile information, THE System SHALL validate that they can only modify their own data
7. WHEN a user changes their password, THE System SHALL require the current password for verification
8. WHEN a user deletes their account, THE System SHALL anonymize their historical reports rather than deleting them
9. THE System SHALL provide an endpoint to export user data in JSON format for portability

### Requirement 19: Input Validation and Security

**User Story:** As a security engineer, I want comprehensive input validation, so that the system is protected against malicious inputs.

#### Acceptance Criteria

1. WHEN a user registers, THE System SHALL validate password complexity (minimum 8 characters, 1 uppercase, 1 number, 1 special character)
2. WHEN a user registers, THE System SHALL validate email format using RFC 5322 compliant regular expression
3. WHEN processing user inputs, THE System SHALL sanitize all text fields to prevent XSS attacks
4. WHEN executing database queries, THE System SHALL use parameterized queries to prevent SQL injection
5. THE System SHALL validate file upload MIME types match actual file content
6. THE System SHALL enforce maximum file size limits on all uploads
7. THE System SHALL validate coordinate ranges (latitude: -90 to 90, longitude: -180 to 180)

### Requirement 20: Data Export and Interoperability

**User Story:** As an Administrator, I want to export system data in standard formats, so that I can integrate with other municipal systems.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint to export reports in CSV format
2. THE System SHALL provide an endpoint to export reports in JSON format
3. WHEN generating CSV exports, THE System SHALL include headers with field names
4. WHEN generating exports, THE System SHALL complete the operation within 5 seconds for up to 1000 records
5. THE System SHALL validate that CSV files are compatible with Microsoft Excel and Google Sheets
6. THE System SHALL support filtering export data by date range and status

### Requirement 21: API Documentation

**User Story:** As a frontend developer, I want comprehensive API documentation, so that I can integrate with backend services efficiently.

#### Acceptance Criteria

1. THE System SHALL generate API documentation automatically using OpenAPI/Swagger specification
2. THE System SHALL expose interactive API documentation at /api/docs endpoint
3. WHEN API documentation is accessed, THE System SHALL display all available endpoints with request/response schemas
4. THE System SHALL include example requests and responses in the documentation
5. THE System SHALL document all error codes and their meanings
6. THE System SHALL keep documentation synchronized with actual API implementation
