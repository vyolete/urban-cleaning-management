# Implementation Plan: Urban Cleaning Management System

## Overview

This implementation plan breaks down the Urban Cleaning Management System into discrete, incremental coding tasks. The system will be built using Spring Boot for the backend, React for the frontend, and PostgreSQL with PostGIS for data storage. Each task builds upon previous work, ensuring continuous integration and validation.

## Tasks

- [x] 1. Initialize project structure and dependencies
  - Create monorepo structure with `/backend`, `/frontend`, and `/docker` directories
  - Initialize Spring Boot project with Maven (Java 17)
  - Initialize React project with Create React App
  - Configure PostgreSQL with PostGIS extension in docker-compose.yml
  - Add core dependencies: Spring Security, JWT, JPA, PostGIS, BCrypt
  - Add frontend dependencies: React Router, Axios, Leaflet, PropTypes
  - _Requirements: 11.1, 11.2, 11.3_

- [x] 2. Set up database schema and entities
  - [x] 2.1 Create JPA entities for domain model
    - Create `User` entity with UUID, username, passwordHash, email, role, timestamps
    - Create `Report` entity with UUID, location (PostGIS Point), category, description, photoUrl, timestamps
    - Create `Task` entity with UUID, location, category, state enum, priorityScore, duplicateCount, timestamps
    - Create `AuditLog` entity with UUID, taskId, userId, previousState, newState, timestamp (immutable fields)
    - Create `AlgorithmConfig` entity with UUID, weight parameters, thresholds, effective dates
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ]* 2.2 Write property test for entity persistence
    - **Property 11: Report data completeness**
    - **Validates: Requirements 3.4**

  - [x] 2.3 Create JPA repositories
    - Create `UserRepository` extending JpaRepository with findByUsername method
    - Create `ReportRepository` with spatial query methods using @Query with PostGIS functions
    - Create `TaskRepository` with findByStateOrderByPriorityScoreDesc method
    - Create `AuditLogRepository` with findByTaskIdOrderByChangedAtAsc method
    - Create `AlgorithmConfigRepository` with findCurrentConfig method
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ]* 2.4 Write property test for referential integrity
    - **Property 40: Referential integrity enforcement**
    - **Validates: Requirements 10.7**

- [x] 3. Implement authentication and authorization
  - [x] 3.1 Create security configuration
    - Configure BCryptPasswordEncoder bean
    - Configure Spring Security filter chain with JWT authentication
    - Configure CORS to allow frontend origin
    - Add security headers (X-Content-Type-Options, X-Frame-Options, X-XSS-Protection)
    - _Requirements: 1.3, 12.1, 12.2_

  - [x] 3.2 Implement JWT token provider
    - Create `JwtTokenProvider` class to generate tokens with user identity and role claims
    - Implement token validation with signature and expiration checks
    - Implement claim extraction methods (username, role, userId)
    - Configure token expiration to 24 hours
    - _Requirements: 1.1, 1.4_

  - [x] 3.3 Implement authentication service
    - Create `AuthService` with login method that validates credentials
    - Integrate BCryptPasswordEncoder for password validation
    - Generate JWT token on successful authentication
    - Create `UserDetailsServiceImpl` for Spring Security integration
    - _Requirements: 1.1, 1.2, 1.3_

  - [ ]* 3.4 Write property tests for authentication
    - **Property 1: Valid credentials generate valid JWT tokens**
    - **Validates: Requirements 1.1**

  - [ ]* 3.5 Write property test for invalid credentials
    - **Property 2: Invalid credentials are rejected**
    - **Validates: Requirements 1.2**

  - [ ]* 3.6 Write property test for password hashing
    - **Property 3: Password storage uses BCrypt**
    - **Validates: Requirements 1.3**

  - [ ]* 3.7 Write property test for token expiration
    - **Property 4: Expired tokens require re-authentication**
    - **Validates: Requirements 1.4**

  - [x] 3.8 Create authentication controller
    - Create `AuthController` with POST /api/auth/login endpoint
    - Create POST /api/auth/register endpoint for user registration
    - Implement DTOs: `LoginRequest`, `LoginResponse`, `RegisterRequest`
    - _Requirements: 1.1, 9.1_

  - [ ]* 3.9 Write unit tests for authentication endpoints
    - Test successful login returns token
    - Test invalid credentials return 401
    - Test registration creates user with hashed password
    - _Requirements: 1.1, 1.2_

- [x] 4. Implement role-based access control
  - [x] 4.1 Create JWT authentication filter
    - Create `JwtAuthenticationFilter` extending OncePerRequestFilter
    - Extract JWT from Authorization header
    - Validate token and set SecurityContext with user details
    - _Requirements: 2.3_

  - [x] 4.2 Configure method-level security
    - Enable @PreAuthorize annotations
    - Configure role hierarchy if needed
    - _Requirements: 2.1, 2.2_

  - [ ]* 4.3 Write property tests for authorization
    - **Property 5: Role-based access control enforcement**
    - **Validates: Requirements 2.1**

  - [ ]* 4.4 Write property test for admin endpoint protection
    - **Property 6: Admin endpoint protection**
    - **Validates: Requirements 2.2**

  - [ ]* 4.5 Write property test for token role validation
    - **Property 7: Token role validation on all protected endpoints**
    - **Validates: Requirements 2.3**

- [x] 5. Checkpoint - Ensure authentication and authorization tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement report submission module
  - [x] 6.1 Create file storage service
    - Create `FileStorageService` to handle photo uploads
    - Implement file type validation (JPEG, PNG only)
    - Implement file size validation (max 5MB)
    - Generate unique filenames and store in configured directory
    - _Requirements: 3.6_

  - [ ]* 6.2 Write property test for file validation
    - **Property 13: Photo file validation**
    - **Validates: Requirements 3.6**

  - [x] 6.3 Create geofencing service
    - Create `GeofencingService` to validate coordinates
    - Load geofencing boundaries from configuration
    - Use PostGIS ST_Contains to check if point is within boundaries
    - _Requirements: 3.2, 3.3_

  - [ ]* 6.4 Write property test for geofencing
    - **Property 10: Geofencing validation**
    - **Validates: Requirements 3.2, 3.3**

  - [x] 6.5 Create report service
    - Create `ReportService` with createReport method
    - Validate required fields (latitude, longitude, category, description)
    - Validate coordinates using GeofencingService
    - Store photo using FileStorageService
    - Save report entity with all required fields
    - _Requirements: 3.1, 3.4, 3.5_

  - [ ]* 6.6 Write property test for required field validation
    - **Property 12: Required field validation**
    - **Validates: Requirements 3.5**

  - [ ]* 6.7 Write property test for multipart acceptance
    - **Property 9: Multipart report acceptance**
    - **Validates: Requirements 3.1**

  - [x] 6.8 Create report controller
    - Create `ReportController` with POST /api/reports endpoint
    - Accept multipart request with @RequestPart for JSON and photo
    - Secure endpoint with @PreAuthorize for authenticated users
    - Create DTOs: `ReportSubmissionRequest`, `ReportResponse`
    - _Requirements: 3.1, 9.2_

  - [ ]* 6.9 Write unit tests for report submission
    - Test successful report creation returns 201
    - Test missing required fields returns 400
    - Test invalid coordinates returns 400
    - Test invalid file type returns 400
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.6_

- [x] 7. Implement priority calculation algorithm
  - [x] 7.1 Create algorithm configuration service
    - Create `ConfigService` to manage algorithm weights
    - Implement getCurrentConfig method to retrieve active configuration
    - Implement default weight values if no configuration exists
    - _Requirements: 4.2, 13.5_

  - [x] 7.2 Implement priority calculator service
    - Create `PriorityCalculatorService` with calculatePriority method
    - Implement formula: P = (Wc * Category) + (Wz * Zone) + (Wt * Time)
    - Implement mapCategoryToValue to convert category string to numeric severity
    - Implement calculateZoneRiskIndex using PostGIS spatial queries
    - Implement calculateHoursElapsed from report timestamp
    - _Requirements: 4.1, 4.3, 4.4, 4.5_

  - [ ]* 7.3 Write property test for priority formula
    - **Property 14: Priority score formula correctness**
    - **Validates: Requirements 4.1**

  - [ ]* 7.4 Write property test for category mapping
    - **Property 15: Category mapping completeness**
    - **Validates: Requirements 4.3**

  - [ ]* 7.5 Write property test for zone calculation
    - **Property 16: Zone calculation from location**
    - **Validates: Requirements 4.4**

  - [ ]* 7.6 Write property test for time-based urgency
    - **Property 17: Time-based urgency increase**
    - **Validates: Requirements 4.5**

  - [ ]* 7.7 Write property test for score persistence
    - **Property 18: Priority score persistence**
    - **Validates: Requirements 4.6**

- [x] 8. Implement deduplication service
  - [x] 8.1 Create deduplication service
    - Create `DeduplicationService` with checkForDuplicates method
    - Use PostGIS ST_DWithin to find reports within distance threshold
    - Check temporal proximity within configured time window
    - Mark duplicate reports and link to parent task
    - Update parent task duplicate count
    - Select highest priority score among duplicates
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 8.2 Write property test for spatial proximity detection
    - **Property 20: Spatial proximity detection**
    - **Validates: Requirements 5.1**

  - [ ]* 8.3 Write property test for duplicate marking
    - **Property 21: Duplicate marking with spatial and temporal constraints**
    - **Validates: Requirements 5.2**

  - [ ]* 8.4 Write property test for duplicate grouping
    - **Property 22: Duplicate grouping under parent task**
    - **Validates: Requirements 5.3**

  - [ ]* 8.5 Write property test for child reference storage
    - **Property 23: Child report reference storage**
    - **Validates: Requirements 5.4**

  - [ ]* 8.6 Write property test for duplicate count
    - **Property 24: Duplicate count accuracy**
    - **Validates: Requirements 5.5**

  - [ ]* 8.7 Write property test for maximum priority selection
    - **Property 25: Maximum priority score selection**
    - **Validates: Requirements 5.6**

- [x] 9. Implement task management module
  - [x] 9.1 Create task service
    - Create `TaskService` with createTask method that creates task from report
    - Calculate priority score using PriorityCalculatorService
    - Check for duplicates using DeduplicationService
    - Initialize task state to PENDIENTE
    - _Requirements: 4.1, 5.1, 6.1_

  - [ ]* 9.2 Write property test for initial state
    - **Property 26: Initial state is PENDIENTE**
    - **Validates: Requirements 6.1**

  - [x] 9.3 Implement state transition logic
    - Add updateState method to TaskService
    - Implement state machine validation: PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO
    - Throw exception for invalid transitions
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6_

  - [ ]* 9.4 Write property test for state machine enforcement
    - **Property 27: State machine enforcement**
    - **Validates: Requirements 6.2, 6.3, 6.4, 6.6**

  - [ ]* 9.5 Write property test for invalid transition rejection
    - **Property 28: Invalid transition rejection**
    - **Validates: Requirements 6.5**

  - [x] 9.6 Integrate report service with task creation
    - Update ReportService to call TaskService.createTask after saving report
    - Ensure transactional consistency
    - _Requirements: 3.1, 4.1_

- [x] 10. Implement audit logging
  - [x] 10.1 Create audit service
    - Create `AuditService` with logStateChange method
    - Create audit log entry with task, user, previous state, new state, timestamp
    - Use millisecond precision for timestamps
    - Mark all fields as non-updatable
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ]* 10.2 Write property test for audit log creation
    - **Property 29: Audit log creation on state change**
    - **Validates: Requirements 7.1**

  - [ ]* 10.3 Write property test for audit log completeness
    - **Property 30: Audit log completeness**
    - **Validates: Requirements 7.2, 7.3, 7.4**

  - [ ]* 10.4 Write property test for audit log immutability
    - **Property 31: Audit log immutability**
    - **Validates: Requirements 7.5**

  - [ ]* 10.5 Write property test for chronological ordering
    - **Property 32: Chronological audit query ordering**
    - **Validates: Requirements 7.6**

  - [x] 10.6 Integrate audit service with task state changes
    - Update TaskService.updateState to call AuditService.logStateChange
    - Ensure audit log is created before state change is committed
    - _Requirements: 7.1_

- [ ] 11. Checkpoint - Ensure core backend functionality tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement task management endpoints
  - [x] 12.1 Create task controller
    - Create `TaskController` with GET /api/tasks endpoint
    - Implement filtering by state and geographic zone
    - Order results by priority score descending
    - Secure endpoint with @PreAuthorize for operators
    - Create DTOs: `TaskResponse`, `TaskFilterRequest`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 9.3_

  - [ ]* 12.2 Write property test for task ordering
    - **Property 33: Task ordering by priority**
    - **Validates: Requirements 8.1**

  - [ ]* 12.3 Write property test for display completeness
    - **Property 34: Task display completeness**
    - **Validates: Requirements 8.2**

  - [ ]* 12.4 Write property test for state filter
    - **Property 35: State filter correctness**
    - **Validates: Requirements 8.3**

  - [ ]* 12.5 Write property test for zone filter
    - **Property 36: Geographic zone filter correctness**
    - **Validates: Requirements 8.4**

  - [x] 12.6 Add task detail endpoint
    - Add GET /api/tasks/{id} endpoint
    - Include duplicate count and merged reports
    - _Requirements: 5.5, 8.2_

  - [x] 12.7 Add state update endpoint
    - Add PATCH /api/tasks/{id}/state endpoint
    - Accept new state in request body
    - Call TaskService.updateState
    - Return updated task
    - _Requirements: 6.2, 6.3, 6.4, 9.4_

  - [x] 12.8 Add audit history endpoint
    - Add GET /api/tasks/{id}/audit-history endpoint
    - Return chronologically ordered audit logs
    - Create `AuditLogResponse` DTO
    - _Requirements: 7.6_

  - [ ]* 12.9 Write unit tests for task endpoints
    - Test GET /api/tasks returns ordered tasks
    - Test filtering by state works correctly
    - Test PATCH /api/tasks/{id}/state updates state
    - Test invalid state transition returns 400
    - Test audit history returns chronological logs
    - _Requirements: 8.1, 8.3, 6.2, 7.6_

- [x] 13. Implement configuration management
  - [x] 13.1 Create configuration controller
    - Create `ConfigController` with GET /api/admin/config/algorithm-weights endpoint
    - Add PUT /api/admin/config/algorithm-weights endpoint
    - Secure endpoints with @PreAuthorize("hasRole('ADMIN')")
    - Create DTOs: `AlgorithmWeightsRequest`, `AlgorithmWeightsResponse`
    - _Requirements: 13.1_

  - [x] 13.2 Implement weight update logic
    - Add updateWeights method to ConfigService
    - Validate weight values are within acceptable ranges
    - Store new configuration with effective dates
    - Store historical configuration
    - Trigger priority recalculation for pending tasks
    - _Requirements: 13.2, 13.3, 13.4_

  - [ ]* 13.3 Write property test for weight validation
    - **Property 45: Weight parameter validation**
    - **Validates: Requirements 13.2**

  - [ ]* 13.4 Write property test for recalculation trigger
    - **Property 46: Weight change triggers recalculation**
    - **Validates: Requirements 13.3**

  - [ ]* 13.5 Write property test for configuration history
    - **Property 47: Weight configuration history**
    - **Validates: Requirements 13.4**

- [x] 14. Implement global exception handling
  - [x] 14.1 Create custom exceptions
    - Create `AuthenticationException` for auth failures
    - Create `ValidationException` for validation errors
    - Create `ResourceNotFoundException` for missing resources
    - Create `InvalidStateTransitionException` for state machine violations
    - _Requirements: 9.6, 9.7_

  - [x] 14.2 Create global exception handler
    - Create `GlobalExceptionHandler` with @RestControllerAdvice
    - Handle AuthenticationException → 401
    - Handle AccessDeniedException → 403
    - Handle ValidationException → 400
    - Handle ResourceNotFoundException → 404
    - Handle generic Exception → 500
    - Create `ErrorResponse` DTO with errorCode, message, timestamp, details
    - _Requirements: 9.6, 9.7_

  - [ ]* 14.3 Write property tests for error responses
    - **Property 37: Success status codes**
    - **Validates: Requirements 9.5**

  - [ ]* 14.4 Write property test for client error codes
    - **Property 38: Client error status codes**
    - **Validates: Requirements 9.6**

  - [ ]* 14.5 Write property test for server error codes
    - **Property 39: Server error status codes**
    - **Validates: Requirements 9.7**

- [x] 15. Implement security features
  - [ ]* 15.1 Write property test for CORS validation
    - **Property 41: CORS origin validation**
    - **Validates: Requirements 12.1**

  - [ ]* 15.2 Write property test for security headers
    - **Property 42: Security headers presence**
    - **Validates: Requirements 12.2**

  - [ ]* 15.3 Write property test for input sanitization
    - **Property 43: Input sanitization**
    - **Validates: Requirements 12.4**

  - [x] 15.4 Implement rate limiting
    - Add rate limiting filter for authentication endpoints
    - Configure maximum requests per time window
    - Return 429 Too Many Requests when limit exceeded
    - _Requirements: 12.5_

  - [ ]* 15.5 Write property test for rate limiting
    - **Property 44: Authentication rate limiting**
    - **Validates: Requirements 12.5**

- [ ] 16. Checkpoint - Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 17. Initialize React frontend
  - [ ] 17.1 Set up React project structure
    - Create folder structure: components, pages, services, hooks, context, utils
    - Install dependencies: react-router-dom, axios, leaflet, react-leaflet, prop-types
    - Configure environment variables for API URL
    - _Requirements: 11.2_

  - [ ] 17.2 Create API service layer
    - Create axios instance with base URL configuration
    - Add request interceptor to attach JWT token
    - Add response interceptor for error handling
    - Create authService with login and register methods
    - Create reportService with submitReport method
    - Create taskService with getTasks, updateTaskState methods
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [ ] 17.3 Create authentication context
    - Create AuthContext with user state and authentication methods
    - Implement login, logout, and token storage in localStorage
    - Create useAuth custom hook
    - _Requirements: 1.1_

- [ ] 18. Implement citizen interface
  - [ ] 18.1 Create geolocation hook
    - Create useGeolocation hook using navigator.geolocation API
    - Return location, error, and loading states
    - _Requirements: 3.1_

  - [ ] 18.2 Create report form component
    - Create ReportForm component with category, description, and photo inputs
    - Use useGeolocation hook to capture current location
    - Implement form validation
    - Display location on map preview
    - Handle photo upload with preview
    - Submit report using reportService
    - _Requirements: 3.1, 3.5_

  - [ ] 18.3 Create map view component
    - Create MapView component using react-leaflet
    - Display user location marker
    - Show geofencing boundaries if available
    - Allow location confirmation
    - _Requirements: 3.2_

  - [ ] 18.4 Create citizen report page
    - Create CitizenReportPage combining ReportForm and MapView
    - Handle successful submission with confirmation message
    - Handle errors with user-friendly messages
    - _Requirements: 3.1_

- [ ] 19. Implement operator dashboard
  - [ ] 19.1 Create task list component
    - Create TaskList component displaying tasks in table format
    - Show task ID, location, category, state, priority score
    - Implement state filter dropdown
    - Implement zone filter dropdown
    - Sort tasks by priority score descending
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ] 19.2 Create task map component
    - Create TaskMap component using react-leaflet
    - Display task markers color-coded by priority
    - Show popup with task details on marker click
    - _Requirements: 8.1_

  - [ ] 19.3 Create task detail component
    - Create TaskDetail component showing full task information
    - Display duplicate count
    - Show state transition buttons based on current state
    - Call taskService.updateTaskState on button click
    - _Requirements: 5.5, 6.2, 6.3, 6.4, 8.2_

  - [ ] 19.4 Create audit timeline component
    - Create AuditTimeline component displaying state changes
    - Show user, timestamp, and state transition for each entry
    - Display in chronological order
    - _Requirements: 7.2, 7.3, 7.4, 7.6_

  - [ ] 19.5 Create operator dashboard page
    - Create OperatorDashboard page combining TaskList, TaskMap, TaskDetail
    - Implement task selection to show details
    - Refresh task list after state updates
    - _Requirements: 8.1, 8.2_

- [ ] 20. Implement admin interface
  - [ ] 20.1 Create configuration panel component
    - Create ConfigPanel component for algorithm weight configuration
    - Display current weight values
    - Provide input fields for updating weights
    - Implement real-time validation
    - Show historical configurations
    - _Requirements: 13.1, 13.2_

  - [ ] 20.2 Create admin config page
    - Create AdminConfigPage with ConfigPanel
    - Handle weight updates with confirmation
    - Display success/error messages
    - _Requirements: 13.1_

- [ ] 21. Implement authentication pages
  - [ ] 21.1 Create login page
    - Create LoginPage with username and password inputs
    - Call authService.login on form submission
    - Store token and redirect on success
    - Display error messages on failure
    - _Requirements: 1.1, 1.2_

  - [ ] 21.2 Create protected route component
    - Create ProtectedRoute component checking authentication
    - Redirect to login if not authenticated
    - Check user role for role-specific routes
    - _Requirements: 2.1, 2.2_

  - [ ] 21.3 Configure routing
    - Set up React Router with routes for all pages
    - Configure protected routes for operator and admin pages
    - Configure public routes for login and citizen report
    - _Requirements: 2.1, 2.2_

- [ ] 22. Checkpoint - Ensure frontend integrates with backend
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 23. Configure Docker deployment
  - [ ] 23.1 Create backend Dockerfile
    - Create multi-stage Dockerfile for Spring Boot
    - Build stage with Maven
    - Runtime stage with OpenJDK
    - Expose port 8080
    - _Requirements: 11.1_

  - [ ] 23.2 Create frontend Dockerfile
    - Create multi-stage Dockerfile for React
    - Build stage with Node.js
    - Runtime stage with Nginx
    - Copy build artifacts to Nginx
    - Configure Nginx for SPA routing
    - Expose port 80
    - _Requirements: 11.2_

  - [ ] 23.3 Create docker-compose configuration
    - Create docker-compose.yml with postgres, backend, frontend services
    - Configure PostgreSQL with PostGIS image
    - Configure environment variables
    - Set up service dependencies
    - Configure volumes for data persistence
    - _Requirements: 11.3, 11.4_

  - [ ] 23.4 Create database initialization script
    - Create SQL script to initialize schema
    - Enable PostGIS extension
    - Create spatial indexes
    - Insert default algorithm configuration
    - _Requirements: 11.4_

- [ ] 24. Final integration and testing
  - [ ] 24.1 Test complete user flows
    - Test citizen report submission flow
    - Test operator task management flow
    - Test admin configuration flow
    - Test authentication and authorization
    - _Requirements: All_

  - [ ]* 24.2 Run full property test suite
    - Execute all 47 property tests
    - Verify minimum 100 iterations per test
    - Ensure all properties pass
    - _Requirements: All_

  - [ ] 24.3 Verify deployment
    - Build and start all Docker containers
    - Verify database initialization
    - Verify backend API accessibility
    - Verify frontend loads and connects to backend
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [ ] 25. Final checkpoint - Complete system validation
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- Property tests validate universal correctness properties with minimum 100 iterations
- Unit tests validate specific examples and edge cases
- All property tests must be tagged with: `@Tag("Feature: urban-cleaning-management, Property N: [property text]")`
- Use JUnit-QuickCheck for property-based testing in Java
- Backend uses Java 17 with Spring Boot
- Frontend uses React with functional components and hooks
- Database uses PostgreSQL 15 with PostGIS 3.3
