# MVC Architecture View

## Overview

This document describes the Model-View-Controller (MVC) architectural pattern implementation in the Urban Cleaning Management System, showing the separation of concerns between presentation, business logic, and data layers.

## Cross-References

This view is closely related to other architectural views:

- **[Logical View](02-logical-view.md)**: Controllers and Models shown here are detailed with sequence diagrams and class diagrams
- **[Data Model View](03-data-model-view.md)**: Model entities are fully documented with attributes and relationships
- **[Implementation View](07-implementation-view.md)**: Package structure shows the physical organization of MVC components
- **[Use Case View](01-use-case-view.md)**: Controllers implement the endpoints that realize use cases
- **[Design Decisions](08-design-decisions.md#design-patterns)**: MVC pattern rationale and implementation details

## Table of Contents

1. [View Components](#view-components)
2. [Controller Components](#controller-components)
3. [Model Components](#model-components)
4. [Communication Patterns](#communication-patterns)
5. [MVC Architecture Diagram](#mvc-architecture-diagram)

---

## View Components

This section documents all React components from the frontend, organized by functional area.

### Component Hierarchy

```
frontend/src/
├── components/
│   ├── common/          # Shared components (authentication, navigation)
│   ├── citizen/         # Citizen-specific views (report submission, maps)
│   ├── operator/        # Operator-specific views (task management)
│   ├── admin/           # Admin-specific views (configuration)
│   └── user/            # User-specific views (profile, sessions)
└── pages/               # Page-level components (full pages)
```

### Component Catalog

#### Common Components

| Component | Purpose | Key Props | State Management | Source Reference |
|-----------|---------|-----------|------------------|------------------|
| **ProtectedRoute** | Route guard for authentication and authorization | `children`, `requiredRole`, `requiredRoles` | Uses AuthContext for authentication state | `frontend/src/components/common/ProtectedRoute.jsx` |
| **UserInfo** | Display user information and logout button | None (uses context) | Uses AuthContext for user data | `frontend/src/components/common/UserInfo.jsx` |

#### Citizen Components

| Component | Purpose | Key Props | State Management | Source Reference |
|-----------|---------|-----------|------------------|------------------|
| **ReportForm** | Form for submitting incident reports | `onSuccess`, `onError` | Local state for form data, photo, location mode | `frontend/src/components/citizen/ReportForm.jsx` |
| **MapView** | Display location on interactive map | `location`, `showGeofence`, `height`, `zoom` | Refs for map instance and markers | `frontend/src/components/citizen/MapView.jsx` |

#### Operator Components

| Component | Purpose | Key Props | State Management | Source Reference |
|-----------|---------|-----------|------------------|------------------|
| **TaskList** | Display and filter list of tasks | `onTaskSelect`, `selectedTaskId` | Local state for tasks, filters, loading, error | `frontend/src/components/operator/TaskList.jsx` |
| **TaskDetail** | Show detailed task information and state transitions | `task`, `onTaskUpdate` | Local state for updating, error, success messages | `frontend/src/components/operator/TaskDetail.jsx` |
| **TaskMap** | Visualize tasks on interactive map with priority markers | `tasks`, `selectedTask`, `onTaskSelect`, `height` | Refs for map instance and markers | `frontend/src/components/operator/TaskMap.jsx` |
| **AuditTimeline** | Display task state change history | `taskId` | Local state for audit logs, loading, error | `frontend/src/components/operator/AuditTimeline.jsx` |

#### Admin Components

| Component | Purpose | Key Props | State Management | Source Reference |
|-----------|---------|-----------|------------------|------------------|
| **ConfigPanel** | Manage algorithm weights and deduplication settings | `onConfigUpdate` | Local state for config, form data, history, validation | `frontend/src/components/admin/ConfigPanel.jsx` |

#### User Components

| Component | Purpose | Key Props | State Management | Source Reference |
|-----------|---------|-----------|------------------|------------------|
| **ActiveSessions** | Display and manage user's active sessions across devices | None | Local state for sessions, loading, error, revoking | `frontend/src/components/user/ActiveSessions.jsx` |

### Page Components

| Page | Purpose | Key Components Used | Route | Source Reference |
|------|---------|---------------------|-------|------------------|
| **LoginPage** | User authentication | None (standalone form) | `/login` | `frontend/src/pages/LoginPage.jsx` |
| **CitizenReportPage** | Citizen report submission interface | ReportForm, MapView | `/report` | `frontend/src/pages/CitizenReportPage.jsx` |
| **OperatorDashboard** | Operator task management interface | TaskList, TaskMap, TaskDetail, AuditTimeline, UserInfo | `/dashboard` | `frontend/src/pages/OperatorDashboard.jsx` |
| **AdminConfigPage** | Administrator configuration interface | ConfigPanel, UserInfo | `/admin/config` | `frontend/src/pages/AdminConfigPage.jsx` |
| **UserProfile** | User profile and session management | ActiveSessions | `/profile` | `frontend/src/pages/UserProfile.jsx` |

### Component Props Details

#### ReportForm Props
- **onSuccess** (function): Callback invoked when report is successfully submitted, receives response object
- **onError** (function): Callback invoked when report submission fails, receives error object

#### MapView Props
- **location** (object): `{ latitude: number, longitude: number, accuracy?: number }`
- **showGeofence** (boolean): Whether to display service area boundaries
- **height** (string): CSS height value (default: "400px")
- **zoom** (number): Initial map zoom level (default: 15)

#### TaskList Props
- **onTaskSelect** (function): Callback invoked when task is selected, receives task object
- **selectedTaskId** (string): ID of currently selected task for highlighting

#### TaskDetail Props
- **task** (object): Task object with full details
- **onTaskUpdate** (function): Callback invoked when task state is updated, receives updated task object

#### TaskMap Props
- **tasks** (array): Array of task objects to display on map
- **selectedTask** (object): Currently selected task for highlighting
- **onTaskSelect** (function): Callback invoked when task marker is clicked
- **height** (string): CSS height value (default: "600px")

#### AuditTimeline Props
- **taskId** (string): ID of task to display audit history for

#### ConfigPanel Props
- **onConfigUpdate** (function): Callback invoked when configuration is updated, receives updated config object

#### ProtectedRoute Props
- **children** (node): React components to render if authorized
- **requiredRole** (string): Single role required for access (e.g., "ADMIN")
- **requiredRoles** (array): Array of roles, user must have at least one

### View Layer Responsibilities

- **Presentation**: Render UI elements and handle user interactions
- **State Management**: Manage local component state (useState) and global state (React Context)
- **API Integration**: Call backend services through API client layer (axios)
- **Validation**: Client-side input validation before submission
- **Routing**: Navigate between different views using React Router
- **User Feedback**: Display loading states, error messages, and success notifications
- **Geolocation**: Access browser geolocation API for location-based features
- **Map Visualization**: Render interactive maps using Leaflet library

---

## Controller Components

This section documents all REST controllers from the backend that handle HTTP requests and responses.

### Controller Catalog

| Controller | Base Path | Endpoint Count | Security | Purpose | Source Reference |
|------------|-----------|----------------|----------|---------|------------------|
| **AuthController** | `/api/auth` | 5 | Public + Authenticated | User authentication, registration, token management | `backend/src/main/java/com/urbanclean/controller/AuthController.java` |
| **ReportController** | `/api/reports` | 4 | Public + Role-based | Incident report submission and retrieval | `backend/src/main/java/com/urbanclean/controller/ReportController.java` |
| **TaskController** | `/api/tasks` | 5 | TECNICO, ADMIN | Task management and state transitions | `backend/src/main/java/com/urbanclean/controller/TaskController.java` |
| **ConfigController** | `/api/admin/config` | 8 | ADMIN only | System configuration (algorithm weights, tokens, deduplication) | `backend/src/main/java/com/urbanclean/controller/ConfigController.java` |
| **AnalyticsController** | `/api/analytics` | 5 | TECNICO, ADMIN | Operational analytics and KPIs | `backend/src/main/java/com/urbanclean/controller/AnalyticsController.java` |
| **FeedbackController** | `/api/tasks/{taskId}/feedback` | 3 | CIUDADANO, TECNICO, ADMIN | Citizen feedback on task resolution | `backend/src/main/java/com/urbanclean/controller/FeedbackController.java` |
| **SessionController** | `/api/sessions` | 3 | Authenticated | User session management across devices | `backend/src/main/java/com/urbanclean/controller/SessionController.java` |
| **UserController** | `/api/users` | 5 | Authenticated | User profile and account management | `backend/src/main/java/com/urbanclean/controller/UserController.java` |
| **PasswordResetController** | `/api/auth/password-reset` | 2 | Public | Password reset workflow | `backend/src/main/java/com/urbanclean/controller/PasswordResetController.java` |
| **NotificationPreferenceController** | `/api/users/notifications` | 3 | Authenticated | Email notification preferences | `backend/src/main/java/com/urbanclean/controller/NotificationPreferenceController.java` |
| **NotificationFailureController** | `/api/admin/notifications` | 2 | ADMIN only | Monitor and retry failed notifications | `backend/src/main/java/com/urbanclean/controller/NotificationFailureController.java` |
| **PerformanceMetricsController** | `/api/admin/metrics` | 1 | ADMIN only | System performance monitoring | `backend/src/main/java/com/urbanclean/controller/PerformanceMetricsController.java` |
| **UnsubscribeController** | `/api/unsubscribe` | 1 | Public | Email unsubscribe handling | `backend/src/main/java/com/urbanclean/controller/UnsubscribeController.java` |

### Key Endpoint Details

#### AuthController (`/api/auth`)

| Method | Path | Security | Description |
|--------|------|----------|-------------|
| POST | `/login` | Public | Authenticate user, returns access + refresh tokens |
| POST | `/register` | Public | Create new user account |
| POST | `/refresh` | Public | Refresh access token using refresh token |
| POST | `/logout` | Authenticated | Invalidate current session tokens |
| POST | `/logout-all` | Authenticated | Invalidate all user sessions across devices |

#### ReportController (`/api/reports`)

| Method | Path | Security | Description |
|--------|------|----------|-------------|
| POST | `/` | Public | Submit new incident report with photo (multipart) |
| GET | `/{id}` | TECNICO, ADMIN | Get report by ID |
| GET | `/` | TECNICO, ADMIN | Get all reports |
| GET | `/my` | Authenticated | Get current user's reports |

#### TaskController (`/api/tasks`)

| Method | Path | Security | Description |
|--------|------|----------|-------------|
| GET | `/` | TECNICO, ADMIN | Get all tasks with optional filtering (state, geographic zone) |
| GET | `/{id}` | TECNICO, ADMIN | Get task by ID |
| PATCH | `/{id}/state` | TECNICO, ADMIN | Update task state (PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO) |
| POST | `/{id}/assign` | ADMIN | Assign task to operator |
| GET | `/{id}/audit-history` | TECNICO, ADMIN | Get task state change history |

#### ConfigController (`/api/admin/config`)

| Method | Path | Security | Description |
|--------|------|----------|-------------|
| GET | `/algorithm-weights` | ADMIN | Get current algorithm weights |
| PUT | `/algorithm-weights` | ADMIN | Update algorithm weights (triggers priority recalculation) |
| GET | `/algorithm-weights/history` | ADMIN | Get configuration history |
| GET | `/token-expiration` | ADMIN | Get token expiration settings |
| PUT | `/token-expiration` | ADMIN | Update token expiration settings |
| GET | `/duplicate-detection` | ADMIN | Get duplicate detection settings |
| PUT | `/duplicate-detection` | ADMIN | Update duplicate detection settings |

#### AnalyticsController (`/api/analytics`)

| Method | Path | Security | Description |
|--------|------|----------|-------------|
| GET | `/heatmap` | TECNICO, ADMIN | Get geographic heatmap data |
| GET | `/task-distribution` | TECNICO, ADMIN | Get task distribution by state/category |
| GET | `/mttr` | TECNICO, ADMIN | Get Mean Time To Resolution metrics |
| GET | `/operator-performance` | ADMIN | Get operator performance metrics |
| POST | `/duplicate-detection` | ADMIN | Analyze potential duplicates |

### Security Annotations

Controllers use Spring Security's `@PreAuthorize` annotation for role-based access control:

- **Public endpoints**: No annotation (accessible without authentication)
- **Authenticated endpoints**: `@PreAuthorize("hasAnyRole('CIUDADANO', 'TECNICO', 'ADMIN')")`
- **Operator endpoints**: `@PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")`
- **Admin endpoints**: `@PreAuthorize("hasRole('ADMIN')")`

### Request/Response Patterns

#### Request DTOs
- Located in `backend/src/main/java/com/urbanclean/dto/request/`
- Validated using Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.)
- Examples: `LoginRequest`, `ReportSubmissionRequest`, `TaskStateUpdateRequest`

#### Response DTOs
- Located in `backend/src/main/java/com/urbanclean/dto/response/`
- Clean API contracts that don't expose internal entity structure
- Examples: `LoginResponse`, `TaskResponse`, `AlgorithmWeightsResponse`

### Controller Layer Responsibilities

- **Request Handling**: Process HTTP requests and extract parameters (path variables, query params, request body)
- **Validation**: Validate request data using Jakarta Bean Validation annotations
- **Delegation**: Delegate business logic to service layer (controllers are thin, services are thick)
- **Response Formatting**: Map entities to DTOs for clean API responses
- **Error Handling**: Handle exceptions via `@RestControllerAdvice` (GlobalExceptionHandler)
- **Security**: Enforce authentication and authorization using Spring Security
- **Documentation**: Provide OpenAPI/Swagger annotations for API documentation
- **Logging**: Log important operations and errors using SLF4J

---

## Model Components

This section documents entities (domain model) and DTOs (data transfer objects) that represent the data model.

### Entities (Domain Model)

The system uses 16 JPA entities for data persistence. See [Data Model View](./03-data-model-view.md) for complete entity relationship diagrams and detailed schema information.

| Entity | Purpose | Key Attributes | Key Relationships | Source Reference |
|--------|---------|----------------|-------------------|------------------|
| **User** | System users (citizens, operators, admins) | id, username, email, password, role, tokenVersion | OneToMany: reports, tasks, sessions, refreshTokens | `backend/src/main/java/com/urbanclean/entity/User.java` |
| **Report** | Incident reports submitted by citizens | id, location (Point), category, description, photoUrl, isDuplicate | ManyToOne: submitter, task | `backend/src/main/java/com/urbanclean/entity/Report.java` |
| **Task** | Cleaning tasks created from reports | id, location (Point), category, state, priorityScore, duplicateCount | ManyToOne: primaryReport, assignedOperator; OneToMany: mergedReports, auditLogs | `backend/src/main/java/com/urbanclean/entity/Task.java` |
| **AuditLog** | Task state change history | id, previousState, newState, changedAt | ManyToOne: task, user | `backend/src/main/java/com/urbanclean/entity/AuditLog.java` |
| **AlgorithmConfig** | Priority algorithm configuration | id, weightCategory, weightZone, weightTime, distanceThresholdMeters, timeWindowHours | ManyToOne: createdBy | `backend/src/main/java/com/urbanclean/entity/AlgorithmConfig.java` |
| **CitizenFeedback** | Citizen feedback on task resolution | id, rating, comment, feedbackType, rejectionReason | ManyToOne: task, citizen | `backend/src/main/java/com/urbanclean/entity/CitizenFeedback.java` |
| **RefreshToken** | JWT refresh tokens for authentication | id, token, expiryDate, deviceFingerprint | ManyToOne: user | `backend/src/main/java/com/urbanclean/entity/RefreshToken.java` |
| **TokenBlacklist** | Invalidated JWT tokens | id, token, expiryDate, reason | ManyToOne: user | `backend/src/main/java/com/urbanclean/entity/TokenBlacklist.java` |
| **UserSession** | Active user sessions across devices | id, deviceType, browser, os, ipAddress, lastActivity | ManyToOne: user | `backend/src/main/java/com/urbanclean/entity/UserSession.java` |
| **PasswordResetToken** | Password reset tokens | id, token, expiryDate, used | ManyToOne: user | `backend/src/main/java/com/urbanclean/entity/PasswordResetToken.java` |
| **NotificationPreference** | User email notification settings | id, taskAssigned, taskResolved, taskReopened, reportCreated | OneToOne: user | `backend/src/main/java/com/urbanclean/entity/NotificationPreference.java` |
| **NotificationFailure** | Failed email notifications | id, notificationType, recipientEmail, errorMessage, retryCount | ManyToOne: user | `backend/src/main/java/com/urbanclean/entity/NotificationFailure.java` |
| **FailedLoginAttempt** | Failed login tracking for security | id, username, ipAddress, attemptTime | None | `backend/src/main/java/com/urbanclean/entity/FailedLoginAttempt.java` |

### DTOs (Data Transfer Objects)

DTOs provide clean API contracts that decouple the API from internal entity structure.

#### Request DTOs (17 total)

| DTO | Purpose | Key Fields | Validation | Source Reference |
|-----|---------|------------|------------|------------------|
| **LoginRequest** | User login credentials | username, password | @NotBlank | `backend/src/main/java/com/urbanclean/dto/request/LoginRequest.java` |
| **RegisterRequest** | New user registration | username, email, password, role | @NotBlank, @Email, @ValidPassword | `backend/src/main/java/com/urbanclean/dto/request/RegisterRequest.java` |
| **RefreshTokenRequest** | Token refresh | refreshToken | @NotBlank | `backend/src/main/java/com/urbanclean/dto/request/RefreshTokenRequest.java` |
| **ReportSubmissionRequest** | Submit incident report | latitude, longitude, category, description | @NotNull, @Size | `backend/src/main/java/com/urbanclean/dto/request/ReportSubmissionRequest.java` |
| **TaskStateUpdateRequest** | Update task state | newState | @NotNull | `backend/src/main/java/com/urbanclean/dto/request/TaskStateUpdateRequest.java` |
| **TaskFilterRequest** | Filter tasks | state, minLat, maxLat, minLon, maxLon | Optional | `backend/src/main/java/com/urbanclean/dto/request/TaskFilterRequest.java` |
| **AlgorithmWeightsRequest** | Update algorithm weights | weightCategory, weightZone, weightTime, deduplicationDistanceMeters, deduplicationTimeWindowHours | @NotNull, @DecimalMin, @DecimalMax | `backend/src/main/java/com/urbanclean/dto/request/AlgorithmWeightsRequest.java` |
| **AnalyticsFilters** | Filter analytics queries | startDate, endDate, state, category | Optional | `backend/src/main/java/com/urbanclean/dto/request/AnalyticsFilters.java` |
| **ChangePasswordRequest** | Change user password | currentPassword, newPassword | @NotBlank, @ValidPassword | `backend/src/main/java/com/urbanclean/dto/request/ChangePasswordRequest.java` |
| **UpdateProfileRequest** | Update user profile | email, username | @Email | `backend/src/main/java/com/urbanclean/dto/request/UpdateProfileRequest.java` |
| **DeleteAccountRequest** | Delete user account | password, confirmation | @NotBlank | `backend/src/main/java/com/urbanclean/dto/request/DeleteAccountRequest.java` |
| **PasswordResetInitiateRequest** | Initiate password reset | email | @Email | `backend/src/main/java/com/urbanclean/dto/request/PasswordResetInitiateRequest.java` |
| **PasswordResetCompleteRequest** | Complete password reset | token, newPassword | @NotBlank, @ValidPassword | `backend/src/main/java/com/urbanclean/dto/request/PasswordResetCompleteRequest.java` |
| **NotificationPreferenceRequest** | Update notification preferences | taskAssigned, taskResolved, taskReopened, reportCreated | Boolean | `backend/src/main/java/com/urbanclean/dto/request/NotificationPreferenceRequest.java` |
| **RejectFeedbackRequest** | Reject citizen feedback | rejectionReason | @NotBlank | `backend/src/main/java/com/urbanclean/dto/request/RejectFeedbackRequest.java` |
| **TokenExpirationRequest** | Update token expiration | accessTokenExpirationMinutes, refreshTokenExpirationDays | @Min | `backend/src/main/java/com/urbanclean/dto/request/TokenExpirationRequest.java` |
| **DuplicateDetectionRequest** | Update duplicate detection | detectionRadiusMeters, timeWindowHours | @Min | `backend/src/main/java/com/urbanclean/dto/request/DuplicateDetectionRequest.java` |

#### Response DTOs (21 total)

| DTO | Purpose | Key Fields | Source Reference |
|-----|---------|------------|------------------|
| **LoginResponse** | Login success response | accessToken, refreshToken, tokenType, expiresIn, user | `backend/src/main/java/com/urbanclean/dto/response/LoginResponse.java` |
| **RefreshTokenResponse** | Token refresh response | accessToken, refreshToken, tokenType, expiresIn | `backend/src/main/java/com/urbanclean/dto/response/RefreshTokenResponse.java` |
| **ReportResponse** | Report details | id, latitude, longitude, category, description, photoUrl, submitterUsername, createdAt, isDuplicate | `backend/src/main/java/com/urbanclean/dto/response/ReportResponse.java` |
| **TaskResponse** | Task details | id, latitude, longitude, category, state, priorityScore, duplicateCount, createdAt, updatedAt, resolvedAt, assignedOperatorUsername | `backend/src/main/java/com/urbanclean/dto/response/TaskResponse.java` |
| **AuditLogResponse** | Audit log entry | id, taskId, changedByUsername, previousState, newState, changedAt | `backend/src/main/java/com/urbanclean/dto/response/AuditLogResponse.java` |
| **AlgorithmWeightsResponse** | Algorithm configuration | id, weightCategory, weightZone, weightTime, deduplicationDistanceMeters, deduplicationTimeWindowHours, effectiveFrom, effectiveTo, createdByUsername | `backend/src/main/java/com/urbanclean/dto/response/AlgorithmWeightsResponse.java` |
| **HeatmapResponse** | Geographic heatmap data | latitude, longitude, taskCount, avgPriority | `backend/src/main/java/com/urbanclean/dto/response/HeatmapResponse.java` |
| **TaskDistributionResponse** | Task distribution stats | state/category, count, percentage | `backend/src/main/java/com/urbanclean/dto/response/TaskDistributionResponse.java` |
| **MTTRResponse** | Mean Time To Resolution | avgResolutionTimeHours, medianResolutionTimeHours, minResolutionTimeHours, maxResolutionTimeHours | `backend/src/main/java/com/urbanclean/dto/response/MTTRResponse.java` |
| **OperatorPerformanceResponse** | Operator performance metrics | operatorUsername, tasksCompleted, avgResolutionTimeHours, successRate | `backend/src/main/java/com/urbanclean/dto/response/OperatorPerformanceResponse.java` |
| **FeedbackResponse** | Citizen feedback | id, taskId, rating, comment, feedbackType, submittedAt, citizenUsername | `backend/src/main/java/com/urbanclean/dto/response/FeedbackResponse.java` |
| **UserProfileResponse** | User profile | id, username, email, role, createdAt, gdprConsentDate | `backend/src/main/java/com/urbanclean/dto/response/UserProfileResponse.java` |
| **UserSessionResponse** | User session | id, deviceType, browser, os, ipAddress, city, country, lastActivity, createdAt, current | `backend/src/main/java/com/urbanclean/dto/response/UserSessionResponse.java` |
| **UserDataExport** | GDPR data export | user, reports, tasks, feedback, sessions | `backend/src/main/java/com/urbanclean/dto/response/UserDataExport.java` |
| **PasswordResetResponse** | Password reset status | message, success | `backend/src/main/java/com/urbanclean/dto/response/PasswordResetResponse.java` |
| **NotificationPreferenceResponse** | Notification preferences | taskAssigned, taskResolved, taskReopened, reportCreated | `backend/src/main/java/com/urbanclean/dto/response/NotificationPreferenceResponse.java` |
| **NotificationFailureResponse** | Notification failure | id, notificationType, recipientEmail, errorMessage, retryCount, lastAttempt | `backend/src/main/java/com/urbanclean/dto/response/NotificationFailureResponse.java` |
| **TokenExpirationResponse** | Token expiration config | accessTokenExpirationMinutes, refreshTokenExpirationDays | `backend/src/main/java/com/urbanclean/dto/response/TokenExpirationResponse.java` |
| **DuplicateDetectionResponse** | Duplicate detection config | detectionRadiusMeters, timeWindowHours | `backend/src/main/java/com/urbanclean/dto/response/DuplicateDetectionResponse.java` |
| **PerformanceMetricsResponse** | System performance metrics | cpuUsage, memoryUsage, activeConnections, requestsPerSecond | `backend/src/main/java/com/urbanclean/dto/response/PerformanceMetricsResponse.java` |
| **ErrorResponse** | Error details | errorCode, message, timestamp, details | `backend/src/main/java/com/urbanclean/dto/response/ErrorResponse.java` |

### Model Layer Responsibilities

- **Domain Logic**: Encapsulate business rules and invariants within entities
- **Data Persistence**: Map entities to database tables via JPA/Hibernate
- **Data Transfer**: Provide clean API contracts via DTOs that decouple API from internal structure
- **Validation**: Define validation constraints using Jakarta Bean Validation annotations
- **Relationships**: Manage entity relationships (OneToMany, ManyToOne, etc.) with proper cascade and fetch strategies
- **Immutability**: Use Lombok `@Builder` for DTOs to create immutable data transfer objects
- **Separation**: Keep entities separate from DTOs to allow independent evolution of database schema and API contracts

---

## Communication Patterns

This section documents how View, Controller, and Model layers communicate across the frontend-backend boundary.

### Frontend to Backend Flow

```mermaid
sequenceDiagram
    participant User as User
    participant View as React Component
    participant Service as API Service
    participant API as Axios Client
    participant Controller as REST Controller
    participant BizService as Business Service
    participant Entity as Entity/DTO
    participant DB as PostgreSQL
    
    User->>View: Interact (click, submit)
    View->>Service: Call service method
    Service->>Service: Validate input
    Service->>API: HTTP Request (JSON)
    Note over API: Add JWT token<br/>from localStorage
    API->>Controller: HTTP Request + Auth Header
    Controller->>Controller: Validate request (@Valid)
    Controller->>Controller: Check authorization (@PreAuthorize)
    Controller->>BizService: Call business method
    BizService->>Entity: Create/Update entity
    Entity->>DB: Persist via JPA
    DB-->>Entity: Confirmation
    Entity-->>BizService: Entity
    BizService-->>Controller: Result
    Controller->>Controller: Map Entity → DTO
    Controller-->>API: HTTP Response (JSON)
    API-->>Service: Response data
    Service-->>View: Update state
    View->>View: Re-render UI
    View-->>User: Display result
```

### Data Transformation Flow

The data undergoes multiple transformations as it flows through the system:

```
User Input (Form Data)
    ↓
React State (JavaScript Object)
    ↓ [Validation]
API Service (reportService, taskService, etc.)
    ↓ [FormData for multipart, JSON for regular]
Axios Client (api.js)
    ↓ [Add JWT token, set headers]
HTTP Request (JSON/Multipart)
    ↓ [Network]
REST Controller (Java)
    ↓ [Bean Validation @Valid]
Request DTO (Java Object)
    ↓ [Business Logic]
Entity (Domain Model)
    ↓ [JPA/Hibernate]
Database (PostgreSQL + PostGIS)
    ↓ [Query]
Entity (Domain Model)
    ↓ [Mapping]
Response DTO (Java Object)
    ↓ [Jackson Serialization]
HTTP Response (JSON)
    ↓ [Network]
Axios Client (api.js)
    ↓ [Error Handling]
API Service
    ↓ [State Update]
React State (JavaScript Object)
    ↓ [Rendering]
UI Components (JSX)
    ↓
User Display
```

### API Client Layer (frontend/src/services/)

The frontend uses a service layer to abstract API calls:

#### Base API Client (`api.js`)
- **Axios Instance**: Configured with base URL and default headers
- **Request Interceptor**: Automatically adds JWT token from localStorage to all requests
- **Response Interceptor**: Handles errors globally (401 → logout, 403 → access denied, etc.)
- **Error Handling**: Standardizes error responses across the application

#### Service Modules
- **authService.js**: Authentication (login, register, logout, token refresh)
- **reportService.js**: Report submission and retrieval
- **taskService.js**: Task management and state transitions
- **configService.js**: System configuration management

### State Management

#### Frontend State

**Local Component State (useState)**:
- Form data (input values, validation errors)
- Loading states (submitting, loading)
- UI state (selected items, modal visibility)
- Temporary data (photo previews, filters)

**Global State (React Context)**:
- **AuthContext**: User authentication state
  - `user`: Current user object (username, role)
  - `token`: JWT access token
  - `isAuthenticated()`: Check if user is logged in
  - `hasRole()`: Check user permissions
  - `login()`, `logout()`: Authentication actions

**Server State**:
- Data fetched from API (tasks, reports, config)
- Cached in component state
- Refreshed on demand or via polling
- No persistent client-side cache (always fetch fresh data)

#### Backend State

**Stateless Architecture**:
- Controllers and services are stateless (no instance variables)
- Each request is independent
- Horizontal scaling friendly

**Session State**:
- JWT tokens for stateless authentication
- Token payload contains user ID, username, role
- No server-side session storage
- Refresh tokens stored in database for revocation

**Database State**:
- All persistent data in PostgreSQL
- Transactional consistency via `@Transactional`
- Optimistic locking for concurrent updates

### Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant LoginPage
    participant authService
    participant API
    participant AuthController
    participant AuthService
    participant DB
    
    User->>LoginPage: Enter credentials
    LoginPage->>authService: login(username, password)
    authService->>API: POST /api/auth/login
    API->>AuthController: HTTP Request
    AuthController->>AuthService: authenticate()
    AuthService->>DB: Find user by username
    DB-->>AuthService: User entity
    AuthService->>AuthService: Verify password (BCrypt)
    AuthService->>AuthService: Generate JWT tokens
    AuthService-->>AuthController: LoginResponse (tokens + user)
    AuthController-->>API: HTTP 200 + JSON
    API-->>authService: Response data
    authService->>authService: Store tokens in localStorage
    authService->>authService: Start token refresh timer
    authService-->>LoginPage: Success
    LoginPage->>LoginPage: Navigate to dashboard
```

### Report Submission Flow

```mermaid
sequenceDiagram
    participant User
    participant ReportForm
    participant reportService
    participant API
    participant ReportController
    participant ReportService
    participant DeduplicationService
    participant TaskService
    participant DB
    
    User->>ReportForm: Fill form + upload photo
    ReportForm->>reportService: submitReport(data, photo)
    reportService->>reportService: Validate data
    reportService->>reportService: Create FormData (multipart)
    reportService->>API: POST /api/reports (multipart)
    API->>ReportController: HTTP Request
    ReportController->>ReportController: Validate @Valid
    ReportController->>ReportService: createReport()
    ReportService->>ReportService: Save photo to disk
    ReportService->>ReportService: Create Report entity
    ReportService->>DB: Save report
    ReportService->>DeduplicationService: checkDuplicates()
    DeduplicationService->>DB: Query nearby reports (PostGIS)
    DeduplicationService-->>ReportService: Duplicate status
    alt No duplicates
        ReportService->>TaskService: createTask()
        TaskService->>TaskService: Calculate priority
        TaskService->>DB: Save task
    else Duplicates found
        ReportService->>TaskService: mergeWithExistingTask()
        TaskService->>DB: Update task duplicate count
    end
    ReportService-->>ReportController: Report entity
    ReportController->>ReportController: Map to ReportResponse DTO
    ReportController-->>API: HTTP 201 + JSON
    API-->>reportService: Response data
    reportService-->>ReportForm: Success
    ReportForm->>ReportForm: Show success message
    ReportForm->>ReportForm: Reset form
```

### Task State Update Flow

```mermaid
sequenceDiagram
    participant Operator
    participant TaskDetail
    participant taskService
    participant API
    participant TaskController
    participant TaskService
    participant AuditService
    participant EventPublisher
    participant DB
    
    Operator->>TaskDetail: Click state transition button
    TaskDetail->>taskService: updateTaskState(id, newState)
    taskService->>API: PATCH /api/tasks/{id}/state
    API->>TaskController: HTTP Request
    TaskController->>TaskController: Validate @Valid
    TaskController->>TaskService: updateState(id, newState)
    TaskService->>TaskService: Validate state transition
    TaskService->>DB: Update task state
    TaskService->>EventPublisher: Publish TaskStateChangedEvent
    EventPublisher->>EventPublisher: Notify listeners (async)
    TaskService-->>TaskController: Updated task
    TaskController->>AuditService: logStateChange()
    AuditService->>DB: Save audit log
    TaskController->>TaskController: Map to TaskResponse DTO
    TaskController-->>API: HTTP 200 + JSON
    API-->>taskService: Response data
    taskService-->>TaskDetail: Updated task
    TaskDetail->>TaskDetail: Update UI
    TaskDetail->>TaskDetail: Refresh audit timeline
```

### Token Refresh Flow

```mermaid
sequenceDiagram
    participant Timer
    participant authService
    participant API
    participant AuthController
    participant RefreshTokenService
    participant DB
    
    Timer->>authService: Check token expiry (every minute)
    authService->>authService: Token expires in < 5 min?
    alt Token needs refresh
        authService->>API: POST /api/auth/refresh
        API->>AuthController: HTTP Request
        AuthController->>RefreshTokenService: refreshAccessToken()
        RefreshTokenService->>DB: Find refresh token
        RefreshTokenService->>RefreshTokenService: Validate token
        RefreshTokenService->>RefreshTokenService: Generate new token pair
        RefreshTokenService->>DB: Save new refresh token
        RefreshTokenService->>DB: Revoke old refresh token
        RefreshTokenService-->>AuthController: New tokens
        AuthController-->>API: HTTP 200 + JSON
        API-->>authService: New tokens
        authService->>authService: Update localStorage
        authService->>authService: Update expiry time
    end
```

### Error Handling Patterns

#### Frontend Error Handling
- **Network Errors**: Display "Connection error" message
- **401 Unauthorized**: Clear tokens, redirect to login
- **403 Forbidden**: Display "Access denied" message
- **404 Not Found**: Display "Resource not found" message
- **429 Rate Limit**: Display "Too many requests" message
- **500 Server Error**: Display generic error message

#### Backend Error Handling
- **GlobalExceptionHandler** (`@RestControllerAdvice`): Catches all exceptions
- **ValidationException**: Returns 400 with validation errors
- **ResourceNotFoundException**: Returns 404 with error message
- **AuthenticationException**: Returns 401 with error message
- **InvalidStateTransitionException**: Returns 400 with error message
- **Generic Exception**: Returns 500 with sanitized error message

### Request/Response Examples

#### Login Request/Response
```javascript
// Request
POST /api/auth/login
Content-Type: application/json

{
  "username": "operator1",
  "password": "password123"
}

// Response
HTTP 200 OK
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "username": "operator1",
  "role": "TECNICO"
}
```

#### Report Submission Request/Response
```javascript
// Request
POST /api/reports
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="data"
Content-Type: application/json

{
  "latitude": 40.4168,
  "longitude": -3.7038,
  "category": "BASURA_ACUMULADA",
  "description": "Contenedor desbordado en la esquina"
}

--boundary
Content-Disposition: form-data; name="photo"; filename="photo.jpg"
Content-Type: image/jpeg

[binary photo data]
--boundary--

// Response
HTTP 201 Created
Content-Type: application/json

{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "latitude": 40.4168,
  "longitude": -3.7038,
  "category": "BASURA_ACUMULADA",
  "description": "Contenedor desbordado en la esquina",
  "photoUrl": "/uploads/123e4567-e89b-12d3-a456-426614174000.jpg",
  "submitterUsername": "citizen1",
  "createdAt": "2026-02-10T10:30:00Z",
  "isDuplicate": false
}
```

#### Task State Update Request/Response
```javascript
// Request
PATCH /api/tasks/123e4567-e89b-12d3-a456-426614174000/state
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{
  "newState": "EN_PROGRESO"
}

// Response
HTTP 200 OK
Content-Type: application/json

{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "latitude": 40.4168,
  "longitude": -3.7038,
  "category": "BASURA_ACUMULADA",
  "state": "EN_PROGRESO",
  "priorityScore": 7.5,
  "duplicateCount": 2,
  "createdAt": "2026-02-10T10:30:00Z",
  "updatedAt": "2026-02-10T11:15:00Z",
  "assignedOperatorUsername": "operator1"
}
```

---

## MVC Architecture Diagram

This section contains a comprehensive diagram showing the three MVC layers and their interactions.

### High-Level MVC Architecture

```mermaid
graph TB
    subgraph "View Layer (Frontend - React)"
        Pages[Pages<br/>LoginPage, CitizenReportPage,<br/>OperatorDashboard, AdminConfigPage]
        Components[Components<br/>ReportForm, TaskList, TaskDetail,<br/>ConfigPanel, MapView]
        Services[API Services<br/>authService, reportService,<br/>taskService, configService]
        APIClient[Axios Client<br/>JWT Token Management,<br/>Error Handling]
    end
    
    subgraph "Controller Layer (Backend - Spring Boot)"
        Security[Security Filter<br/>JWT Authentication,<br/>Role-Based Authorization]
        Controllers[REST Controllers<br/>AuthController, ReportController,<br/>TaskController, ConfigController,<br/>AnalyticsController]
        ExceptionHandler[Global Exception Handler<br/>Error Response Formatting]
    end
    
    subgraph "Model Layer (Backend - Spring Boot)"
        Services2[Business Services<br/>AuthService, ReportService,<br/>TaskService, ConfigService,<br/>DeduplicationService]
        Entities[JPA Entities<br/>User, Report, Task,<br/>AlgorithmConfig, AuditLog]
        DTOs[DTOs<br/>Request DTOs,<br/>Response DTOs]
        Repositories[JPA Repositories<br/>UserRepository, ReportRepository,<br/>TaskRepository, ConfigRepository]
        Database[(PostgreSQL + PostGIS<br/>Persistent Data Storage)]
    end
    
    Pages --> Components
    Components --> Services
    Services --> APIClient
    APIClient -->|HTTP/JSON<br/>+ JWT Token| Security
    Security --> Controllers
    Controllers --> ExceptionHandler
    Controllers --> Services2
    Services2 --> Entities
    Services2 --> DTOs
    Entities --> Repositories
    Repositories --> Database
    
    style Pages fill:#e3f2fd
    style Components fill:#e3f2fd
    style Services fill:#e3f2fd
    style APIClient fill:#e3f2fd
    style Security fill:#fff3e0
    style Controllers fill:#fff3e0
    style ExceptionHandler fill:#fff3e0
    style Services2 fill:#f3e5f5
    style Entities fill:#f3e5f5
    style DTOs fill:#f3e5f5
    style Repositories fill:#f3e5f5
    style Database fill:#e8f5e9
```

### Detailed MVC Component Interaction

```mermaid
graph LR
    subgraph "View (React)"
        V1[ReportForm]
        V2[TaskList]
        V3[ConfigPanel]
        V4[LoginPage]
    end
    
    subgraph "Controller (Spring)"
        C1[ReportController<br/>/api/reports]
        C2[TaskController<br/>/api/tasks]
        C3[ConfigController<br/>/api/admin/config]
        C4[AuthController<br/>/api/auth]
    end
    
    subgraph "Model (Spring)"
        M1[Report Entity]
        M2[Task Entity]
        M3[AlgorithmConfig Entity]
        M4[User Entity]
        D1[ReportResponse DTO]
        D2[TaskResponse DTO]
        D3[AlgorithmWeightsResponse DTO]
        D4[LoginResponse DTO]
    end
    
    V1 -->|POST /api/reports<br/>multipart/form-data| C1
    C1 --> M1
    M1 --> D1
    D1 -->|JSON| V1
    
    V2 -->|GET /api/tasks| C2
    C2 --> M2
    M2 --> D2
    D2 -->|JSON| V2
    
    V3 -->|PUT /api/admin/config/algorithm-weights| C3
    C3 --> M3
    M3 --> D3
    D3 -->|JSON| V3
    
    V4 -->|POST /api/auth/login| C4
    C4 --> M4
    M4 --> D4
    D4 -->|JSON + JWT| V4
```

### MVC Data Flow by Feature

#### Report Submission Flow
```mermaid
graph LR
    A[ReportForm<br/>View] -->|FormData| B[ReportController<br/>Controller]
    B --> C[ReportService<br/>Model]
    C --> D[Report Entity<br/>Model]
    D --> E[ReportRepository<br/>Model]
    E --> F[(Database)]
    F --> E
    E --> D
    D --> G[ReportResponse DTO<br/>Model]
    G --> B
    B -->|JSON| A
```

#### Task Management Flow
```mermaid
graph LR
    A[TaskList<br/>View] -->|GET Request| B[TaskController<br/>Controller]
    B --> C[TaskService<br/>Model]
    C --> D[Task Entity<br/>Model]
    D --> E[TaskRepository<br/>Model]
    E --> F[(Database)]
    F --> E
    E --> D
    D --> G[TaskResponse DTO<br/>Model]
    G --> B
    B -->|JSON| A
    
    A2[TaskDetail<br/>View] -->|PATCH Request| B
    B --> C
    C --> H[AuditService<br/>Model]
    H --> I[AuditLog Entity<br/>Model]
```

#### Configuration Management Flow
```mermaid
graph LR
    A[ConfigPanel<br/>View] -->|PUT Request| B[ConfigController<br/>Controller]
    B --> C[ConfigService<br/>Model]
    C --> D[AlgorithmConfig Entity<br/>Model]
    D --> E[ConfigRepository<br/>Model]
    E --> F[(Database)]
    F --> E
    E --> D
    D --> G[AlgorithmWeightsResponse DTO<br/>Model]
    G --> B
    B -->|JSON| A
    
    B --> H[TaskService<br/>Model]
    H --> I[Recalculate Priorities]
```

### Layer Responsibilities Summary

| Layer | Components | Responsibilities | Technologies |
|-------|------------|------------------|--------------|
| **View** | Pages, Components, API Services | - Render UI<br/>- Handle user input<br/>- Manage local state<br/>- Call backend APIs<br/>- Display data | React 18, React Router, Axios, Leaflet, PropTypes |
| **Controller** | REST Controllers, Security Filters | - Handle HTTP requests<br/>- Validate input<br/>- Enforce security<br/>- Delegate to services<br/>- Format responses | Spring Boot, Spring Security, Spring Web, Jakarta Validation |
| **Model** | Entities, DTOs, Services, Repositories | - Business logic<br/>- Data persistence<br/>- Data transformation<br/>- Transaction management<br/>- Domain rules | Spring Data JPA, Hibernate, PostgreSQL, PostGIS |

### Communication Protocol

- **Protocol**: HTTP/HTTPS
- **Data Format**: JSON (application/json) and Multipart (multipart/form-data for file uploads)
- **Authentication**: JWT Bearer tokens in Authorization header
- **CORS**: Configured to allow frontend origin
- **Content Negotiation**: JSON by default
- **Error Format**: Standardized ErrorResponse DTO

### Security Integration

```mermaid
graph TB
    Request[HTTP Request] --> Filter[JWT Authentication Filter]
    Filter -->|Valid Token| Controller[REST Controller]
    Filter -->|Invalid Token| Reject[401 Unauthorized]
    Controller --> PreAuth[@PreAuthorize Check]
    PreAuth -->|Authorized| Service[Business Service]
    PreAuth -->|Not Authorized| Forbidden[403 Forbidden]
    Service --> Response[HTTP Response]
```

**Description**: The MVC architecture follows a clean separation of concerns with the View layer (React frontend) handling presentation, the Controller layer (Spring REST controllers) handling HTTP communication and security, and the Model layer (JPA entities, DTOs, and business services) handling data and business logic. Communication between layers is via HTTP/JSON with JWT authentication, and data transformation occurs at each boundary (Entity → DTO → JSON → React State).

---

## Design Principles

### Separation of Concerns

- **View**: Only handles presentation and user interaction
- **Controller**: Only handles HTTP request/response
- **Model**: Only handles business logic and data

### Dependency Direction

- View depends on Controller (via API)
- Controller depends on Model (via Services)
- Model is independent

### Data Flow

- **Unidirectional**: Data flows from View → Controller → Model → Database
- **Response Flow**: Database → Model → Controller → View

---

## Notes

- Frontend uses React with functional components and hooks
- Backend uses Spring Boot with REST controllers
- Communication is via JSON over HTTP
- Authentication uses JWT tokens
- This document will be populated during the analysis phase
