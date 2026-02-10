# Implementation Plan: Architecture Documentation

## Overview

This implementation plan outlines the tasks for generating comprehensive architectural documentation for the Urban Cleaning Management System. The approach involves systematic analysis of the existing codebase (backend Java/Spring Boot, frontend React, Docker configs) to extract architectural patterns and generate UML-compliant documentation suitable for academic evaluation.

## Tasks

- [x] 1. Set up documentation structure and analysis workspace
  - Create documentation output directory (docs/architecture/)
  - Set up file organization for the 7 architectural views
  - Create template structure for Markdown documentation
  - _Requirements: 13.1, 13.5_

- [x] 2. Analyze and document the Data Model view
  - [x] 2.1 Extract all JPA entities from backend/src/main/java/com/urbanclean/entity/
    - Identify all classes with @Entity annotation
    - Extract entity names and table names
    - Document primary keys (@Id annotations)
    - _Requirements: 3.1_

  - [x] 2.2 Document entity attributes and constraints
    - Extract all @Column annotated fields
    - Document data types, nullable constraints, unique constraints
    - Document field lengths, precision, and scale where applicable
    - _Requirements: 3.2_

  - [x] 2.3 Extract and document entity relationships
    - Identify all @OneToMany, @ManyToOne, @OneToOne, @ManyToMany relationships
    - Document cardinality for each relationship
    - Document join columns and mapped-by attributes
    - Extract foreign key information from @JoinColumn annotations
    - _Requirements: 3.3, 3.4_

  - [x] 2.4 Document indexes and constraints
    - Extract @Index annotations from @Table
    - Document spatial indexes for PostGIS geometry columns
    - Document unique constraints and composite keys
    - _Requirements: 3.3_

  - [x] 2.5 Generate logical database schema diagram
    - Create Mermaid ER diagram showing all entities
    - Show relationships with correct cardinality notation
    - Include primary and foreign keys
    - Add diagram legend explaining notation
    - _Requirements: 3.5, 12.4_

- [x] 3. Analyze and document the Use Case view
  - [x] 3.1 Identify system actors
    - Extract UserRole enum values (CIUDADANO, TECNICO, ADMIN)
    - Extract roles from @PreAuthorize annotations in controllers
    - Map roles to actor names (Citizen, Operator, Administrator)
    - _Requirements: 1.1_

  - [x] 3.2 Extract use cases from REST API endpoints
    - Scan all *Controller.java files
    - Extract methods with @GetMapping, @PostMapping, @PutMapping, @PatchMapping, @DeleteMapping
    - Generate use case name from HTTP method + endpoint path
    - Map endpoints to actors based on @PreAuthorize annotations
    - _Requirements: 1.2_

  - [x] 3.3 Generate use case specifications
    - For each endpoint, create use case specification with:
      - Name (derived from endpoint)
      - Actor (from security annotation)
      - Description (from Javadoc or @Operation annotation)
      - Preconditions (authentication, authorization)
      - Main flow (request → service → response)
      - Alternative flows (error cases from exception handling)
      - Postconditions (state changes, side effects)
    - _Requirements: 1.3_

  - [x] 3.4 Create use case diagram
    - Generate Mermaid use case diagram
    - Show all actors and their use cases
    - Group use cases by functional area
    - _Requirements: 1.2_

  - [x] 3.5 Generate activity diagrams for complex workflows
    - Identify 5 most complex service methods (by cyclomatic complexity or method call depth)
    - Create activity diagrams showing:
      - Start/end nodes
      - Decision points (if/switch statements)
      - Parallel activities (@Async methods)
      - Service calls and database operations
    - Suggested workflows: createTask, assignTask, updateStateWithEvidence, calculatePriority, detectDuplicates
    - _Requirements: 1.4_

- [x] 4. Checkpoint - Review Data Model and Use Case views
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Analyze and document the Logical view
  - [x] 5.1 Generate sequence diagrams for critical workflows
    - Identify 10+ critical workflows by analyzing service methods
    - For each workflow, trace method calls from controller → service → repository
    - Create Mermaid sequence diagrams showing:
      - Participants (Controller, Service, Repository, Database, EventPublisher)
      - Method calls with parameters
      - Return values
      - Asynchronous operations
    - Suggested workflows:
      1. User login (AuthController → AuthService → UserRepository)
      2. Report submission (ReportController → ReportService → DeduplicationService → TaskService)
      3. Task assignment (TaskController → TaskService → EventPublisher)
      4. Task state update (TaskController → TaskService → AuditService)
      5. Priority calculation (TaskService → PriorityCalculatorService → ConfigService)
      6. Duplicate detection (ReportService → DeduplicationService)
      7. Password reset (PasswordResetController → PasswordResetService → EmailService)
      8. Token refresh (AuthController → RefreshTokenService)
      9. Analytics query (AnalyticsController → AnalyticsService → TaskRepository)
      10. Feedback submission (FeedbackController → FeedbackService → TaskService)
    - _Requirements: 2.1_

  - [x] 5.2 Generate comprehensive class diagram
    - Extract all entities from backend/src/main/java/com/urbanclean/entity/
    - Extract all DTOs from backend/src/main/java/com/urbanclean/dto/
    - Extract all services from backend/src/main/java/com/urbanclean/service/
    - Extract all controllers from backend/src/main/java/com/urbanclean/controller/
    - Extract all repositories from backend/src/main/java/com/urbanclean/repository/
    - Create Mermaid class diagram showing:
      - Class names and stereotypes (<<Entity>>, <<Service>>, <<Controller>>)
      - Key attributes (for entities and DTOs)
      - Relationships (associations, dependencies, inheritance)
      - Multiplicity on associations
    - _Requirements: 2.2_

  - [x] 5.3 Generate state diagrams for stateful entities
    - Identify entities with state fields (Task.state: TaskState enum)
    - Extract state values from TaskState enum (PENDIENTE, ASIGNADO, EN_PROGRESO, RESUELTO, REABIERTO)
    - Analyze TaskService.validateStateTransition() to extract valid transitions
    - Create Mermaid state diagram showing:
      - All states
      - Valid transitions with triggers (method names)
      - Guard conditions (validation rules)
      - Initial and final states
    - _Requirements: 2.3_

  - [x] 5.4 Generate collaboration diagrams
    - Identify service methods with multiple collaborators
    - Create collaboration diagrams showing:
      - Components involved
      - Message passing between components
      - Sequence numbers
    - Focus on: report submission flow, task lifecycle, authentication flow
    - _Requirements: 2.4_

  - [x] 5.5 Document component roles and responsibilities
    - For each key component (services, controllers, repositories), document:
      - Primary responsibility
      - Key operations
      - Dependencies
      - Design patterns used
    - _Requirements: 2.5_

- [x] 6. Analyze and document the MVC architecture view
  - [x] 6.1 Identify and document View components
    - Scan frontend/src/components/ and frontend/src/pages/
    - Extract all .jsx files
    - Categorize by functional area (citizen/, operator/, admin/, common/)
    - Document component hierarchy and props
    - _Requirements: 4.1_

  - [x] 6.2 Identify and document Controller components
    - Extract all classes with @RestController annotation
    - Document base paths (@RequestMapping at class level)
    - Document endpoints and their mappings
    - _Requirements: 4.2_

  - [x] 6.3 Identify and document Model components
    - Extract all @Entity classes (domain model)
    - Extract all DTO classes (data transfer objects)
    - Document their purposes and usage
    - _Requirements: 4.3_

  - [x] 6.4 Document MVC communication patterns
    - Trace frontend API calls (axios) to backend endpoints
    - Document request/response flow
    - Document state management (React Context)
    - Document data transformation (Entity → DTO → JSON → React State)
    - _Requirements: 4.4_

  - [x] 6.5 Generate MVC architecture diagram
    - Create diagram showing three layers:
      - View layer (React components)
      - Controller layer (REST controllers)
      - Model layer (Entities + DTOs)
    - Show communication flow between layers
    - _Requirements: 4.5_

- [x] 7. Checkpoint - Review Logical and MVC views
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Analyze and document the Process view
  - [x] 8.1 Identify main business processes
    - Analyze service layer for orchestration methods
    - Identify processes by method complexity and cross-service calls
    - Key processes to document:
      1. Citizen report submission
      2. Task creation and prioritization
      3. Task assignment to operator
      4. Task lifecycle management
      5. Duplicate detection and merging
      6. User authentication and session management
      7. Password reset workflow
      8. Analytics data aggregation
      9. Notification delivery
      10. Feedback processing
    - _Requirements: 5.1_

  - [x] 8.2 Document process flows
    - For each process, document:
      - Entry point (which controller/endpoint)
      - Process steps (service method calls)
      - Exit points (response types, events published)
      - Data transformations
      - Transaction boundaries
    - _Requirements: 5.2_

  - [x] 8.3 Classify processes by criticality
    - Primary processes (core business value): report submission, task management
    - Secondary processes (supporting): authentication, notifications, analytics
    - _Requirements: 5.3_

  - [x] 8.4 Create process models
    - Generate activity diagrams or BPMN-style diagrams for key processes
    - Show decision points, parallel activities, error handling
    - _Requirements: 5.4_

  - [x] 8.5 Document process dependencies
    - Map service dependencies for each process
    - Document integration points (database, email service, event bus)
    - _Requirements: 5.5_

- [x] 9. Analyze and document the Deployment view
  - [x] 9.1 Analyze Docker Compose configuration
    - Parse docker/docker-compose.yml
    - Extract all services (postgres, backend, frontend)
    - Document service configurations
    - _Requirements: 6.1_

  - [x] 9.2 Document deployment components
    - For each service, document:
      - Component type (database, application server, web server)
      - Base image (postgis/postgis:15-3.3, openjdk:17, nginx:alpine)
      - Exposed ports
      - Volume mounts
    - _Requirements: 6.2_

  - [x] 9.3 Document component dependencies
    - Extract depends_on relationships from docker-compose.yml
    - Document startup order (postgres → backend → frontend)
    - Document network connectivity
    - _Requirements: 6.3_

  - [x] 9.4 Document environment requirements
    - Extract environment variables from docker-compose.yml and .env files
    - Document configuration requirements (DB credentials, JWT secret, etc.)
    - Document resource requirements (memory, CPU if specified)
    - _Requirements: 6.4_

  - [x] 9.5 Generate deployment diagram
    - Create diagram showing:
      - Physical/logical nodes (containers)
      - Deployment artifacts (JAR, React build, database)
      - Communication paths (HTTP, JDBC)
      - Port mappings
    - _Requirements: 6.5_

- [x] 10. Analyze and document the Implementation view
  - [x] 10.1 Generate component diagram from package structure
    - Map Java packages to components:
      - com.urbanclean.controller → REST API Layer
      - com.urbanclean.service → Business Logic Layer
      - com.urbanclean.repository → Data Access Layer
      - com.urbanclean.entity → Domain Model
      - com.urbanclean.security → Security Infrastructure
      - com.urbanclean.config → Configuration
    - Map frontend directories to components:
      - frontend/src/components → UI Components
      - frontend/src/services → API Client Layer
      - frontend/src/context → State Management
    - _Requirements: 7.1_

  - [x] 10.2 Document component interfaces
    - For each service, document:
      - Public methods (interface)
      - Dependencies (constructor parameters)
      - Provided functionality
    - Document API contracts (REST endpoints)
    - _Requirements: 7.2_

  - [x] 10.3 Explain module integration patterns
    - Document dependency injection pattern (Spring @Autowired, constructor injection)
    - Document event-driven pattern (ApplicationEventPublisher)
    - Document repository pattern (Spring Data JPA)
    - Document layered architecture pattern
    - _Requirements: 7.3_

  - [x] 10.4 Identify and document external dependencies
    - Parse backend/pom.xml for dependencies:
      - Spring Boot starters
      - PostgreSQL driver
      - Hibernate Spatial
      - JWT libraries (jjwt)
      - Testing libraries
    - Parse frontend/package.json for dependencies:
      - React
      - React Router
      - Axios
      - Leaflet
    - _Requirements: 7.4_

  - [x] 10.5 Document component responsibilities
    - For each major component/package, document:
      - Purpose and responsibility
      - Key classes
      - Boundaries and interfaces
    - _Requirements: 7.5_

- [x] 11. Document design decisions and technology choices
  - [x] 11.1 Document detected design patterns
    - Repository Pattern (Spring Data JPA repositories)
    - MVC Pattern (Controllers, Services, Entities)
    - Event-Driven Pattern (ApplicationEventPublisher, event listeners)
    - State Machine Pattern (TaskState enum with validation)
    - Strategy Pattern (PriorityCalculatorService with configurable weights)
    - _Requirements: 8.1_

  - [x] 11.2 Document technology stack
    - Backend: Spring Boot 3.2.2, Java 17
    - Frontend: React 18, Vite
    - Database: PostgreSQL 15 + PostGIS 3.3
    - Security: Spring Security, JWT (jjwt 0.12.3), BCrypt
    - Deployment: Docker, Docker Compose
    - _Requirements: 8.2_

  - [x] 11.3 Document security architecture
    - JWT-based authentication with access and refresh tokens
    - BCrypt password hashing
    - Role-Based Access Control (RBAC) with @PreAuthorize
    - Token blacklisting for logout
    - Session management with device fingerprinting
    - _Requirements: 8.3_

  - [x] 11.4 Document data persistence architecture
    - JPA/Hibernate for ORM
    - Hibernate Spatial for PostGIS integration
    - Flyway for database migrations
    - Transaction management with @Transactional
    - _Requirements: 8.4_

  - [x] 11.5 Provide architectural decision justifications
    - Justify key decisions:
      - Why Spring Boot (rapid development, production-ready)
      - Why PostgreSQL + PostGIS (geospatial capabilities)
      - Why JWT (stateless authentication, scalability)
      - Why React (component-based UI, ecosystem)
      - Why Docker (consistent deployment, portability)
    - _Requirements: 8.5_

- [x] 12. Checkpoint - Review all architectural views
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Synthesize and format final documentation
  - [x] 13.1 Create documentation structure
    - Organize all views into coherent document structure
    - Create table of contents with links
    - Add executive summary
    - _Requirements: 11.1, 11.4, 11.5_

  - [x] 13.2 Add cross-references between sections
    - Link entities in Data Model to classes in Class Diagram
    - Link use cases to sequence diagrams
    - Link components to deployment nodes
    - _Requirements: 11.3_

  - [x] 13.3 Validate Markdown formatting
    - Ensure proper heading hierarchy (h1 > h2 > h3)
    - Verify code blocks have language tags
    - Verify tables are properly formatted
    - Ensure Mermaid diagrams have proper syntax
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

  - [x] 13.4 Add diagram legends and descriptions
    - For each diagram, add:
      - Title and purpose
      - Legend explaining symbols and notation
      - Detailed textual description
    - _Requirements: 12.4_

  - [x] 13.5 Validate code-based accuracy
    - Review all documented features
    - Verify each has source file reference
    - Ensure no assumed functionality
    - Mark ambiguous items as "implementation-dependent"
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 14. Final review and quality assurance
  - [x] 14.1 Verify completeness
    - Confirm all 7 architectural views are present
    - Verify all major components are documented
    - Check that all diagrams are generated
    - _Requirements: 11.1, 11.2_

  - [x] 14.2 Review academic quality
    - Verify UML terminology usage
    - Check formal language and structure
    - Ensure suitable for thesis inclusion
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [x] 14.3 Validate diagram quality
    - Check all diagrams render correctly
    - Verify readability and clarity
    - Ensure proper notation
    - _Requirements: 12.1, 12.2, 12.3, 12.5_

  - [x] 14.4 Final proofreading
    - Check for consistency across sections
    - Verify all cross-references work
    - Fix any formatting issues
    - _Requirements: 13.5_

- [x] 15. Final checkpoint - Documentation complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- This is a documentation generation project, not a software implementation project
- Tasks involve analyzing existing code and generating documentation
- All analysis should be based strictly on existing code without assumptions
- Generated documentation should be in Markdown format with Mermaid diagrams
- Documentation should meet academic standards for a Master's Thesis (TFM)
- Focus on accuracy over completeness - only document what can be verified in code
- Each task builds on previous tasks to create comprehensive architectural documentation
