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
- **Task_State**: The current status of a task (PENDIENTE, ASIGNADO, EN_PROGRESO, RESUELTO)
- **Geofencing**: Geographic boundary validation for coordinates
- **Deduplication**: Process of identifying and merging similar reports within proximity and time thresholds
- **Audit_Log**: Immutable record of state changes with user and timestamp information
- **JWT**: JSON Web Token used for authentication
- **BCrypt**: Password hashing algorithm

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
