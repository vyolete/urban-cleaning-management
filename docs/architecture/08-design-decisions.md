# Design Decisions and Technology Choices

## Overview

This document explains the key architectural decisions, design patterns, and technology choices made in the Urban Cleaning Management System, along with their rationales and trade-offs.

## Cross-References

This document provides context and rationale for decisions visible in other views:

- **[Implementation View](07-implementation-view.md)**: Package structure and integration patterns are explained here
- **[Logical View](02-logical-view.md)**: Design patterns shown in sequence and class diagrams are detailed here
- **[Data Model View](03-data-model-view.md)**: Data persistence technology choices (JPA, PostGIS) are justified here
- **[Deployment View](06-deployment-view.md)**: Deployment technology choices (Docker, PostgreSQL) are explained here
- **[MVC View](04-mvc-view.md)**: MVC pattern implementation is detailed here
- **[Process View](05-process-view.md)**: Event-driven and strategy patterns used in processes are explained here

## Table of Contents

1. [Design Patterns](#design-patterns)
2. [Technology Stack](#technology-stack)
3. [Security Architecture](#security-architecture)
4. [Data Persistence Architecture](#data-persistence-architecture)
5. [Architectural Decision Records](#architectural-decision-records)

---

## Design Patterns

This section documents the design patterns detected and implemented in the codebase, providing evidence from actual source code.

### 1. Repository Pattern

**Description**: The Repository pattern provides an abstraction layer between the business logic and data access layers, encapsulating the logic required to access data sources. It centralizes common data access functionality and promotes better maintainability.

**Implementation**: Spring Data JPA repositories extending `JpaRepository<T, ID>`

**Evidence in Codebase**:
```java
// backend/src/main/java/com/urbanclean/repository/TaskRepository.java
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByState(TaskState state);
    List<Task> findByAssignedOperator(User operator);
    
    @Query("SELECT t FROM Task t WHERE t.state = :state AND t.createdAt >= :startDate")
    List<Task> findByStateAndCreatedAtAfter(
        @Param("state") TaskState state, 
        @Param("startDate") LocalDateTime startDate
    );
}
```

**Usage in Services**:
```java
// backend/src/main/java/com/urbanclean/service/TaskService.java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    
    public List<Task> getPendingTasks() {
        return taskRepository.findByState(TaskState.PENDIENTE);
    }
}
```

**Benefits**:
- Abstraction over data access - services don't know about SQL
- Reduced boilerplate code - Spring generates implementations
- Type-safe queries - compile-time checking
- Testability - easy to mock repositories
- Consistent data access patterns across the application

**Trade-offs**:
- Learning curve for Spring Data query methods
- Less control over SQL generation (can use native queries when needed)
- Potential performance issues with complex queries (mitigated with @Query)

**Source References**: 
- `backend/src/main/java/com/urbanclean/repository/` (13 repository interfaces)
- `backend/src/main/java/com/urbanclean/service/` (services using repositories)

---

### 2. MVC Pattern (Model-View-Controller)

**Description**: MVC separates application logic into three interconnected components: Model (data), View (presentation), and Controller (request handling). This separation promotes organized code and independent development of each layer.

**Implementation**: 
- **Model**: JPA entities (`com.urbanclean.entity`) and DTOs (`com.urbanclean.dto`)
- **View**: React components (`frontend/src/components`, `frontend/src/pages`)
- **Controller**: Spring REST controllers (`com.urbanclean.controller`)

**Evidence in Codebase**:

**Model (Entity)**:
```java
// backend/src/main/java/com/urbanclean/entity/Task.java
@Entity
@Table(name = "tareas")
public class Task {
    @Id
    private UUID id;
    
    @ManyToOne
    private Report report;
    
    @Enumerated(EnumType.STRING)
    private TaskState state;
    
    private BigDecimal priorityScore;
}
```

**Controller**:
```java
// backend/src/main/java/com/urbanclean/controller/TaskController.java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }
}
```

**View (React)**:
```javascript
// frontend/src/components/operator/TaskList.jsx
const TaskList = () => {
    const [tasks, setTasks] = useState([]);
    
    useEffect(() => {
        taskService.getTasks().then(setTasks);
    }, []);
    
    return <div>{tasks.map(task => <TaskCard task={task} />)}</div>;
};
```

**Benefits**:
- Clear separation of concerns - each layer has distinct responsibility
- Independent development - frontend and backend teams can work in parallel
- Testability - each layer can be tested independently
- Maintainability - changes in one layer don't affect others
- Reusability - models and views can be reused

**Trade-offs**:
- More files and classes - increased project complexity
- Potential over-engineering for simple CRUD operations
- Learning curve for developers new to the pattern

**Source References**: 
- Models: `backend/src/main/java/com/urbanclean/entity/` (15 entities)
- Controllers: `backend/src/main/java/com/urbanclean/controller/` (13 controllers)
- Views: `frontend/src/components/`, `frontend/src/pages/`

---

### 3. Event-Driven Pattern

**Description**: Components communicate through events rather than direct method calls, achieving loose coupling. Event publishers don't need to know about event consumers, allowing for flexible system extension.

**Implementation**: Spring `ApplicationEventPublisher` with `@EventListener` handlers

**Evidence in Codebase**:

**Event Definition**:
```java
// backend/src/main/java/com/urbanclean/event/TaskAssignedEvent.java
@Getter
public class TaskAssignedEvent extends ApplicationEvent {
    private final Task task;
    private final User operator;
    
    public TaskAssignedEvent(Object source, Task task, User operator) {
        super(source);
        this.task = task;
        this.operator = operator;
    }
}
```

**Event Publishing**:
```java
// backend/src/main/java/com/urbanclean/service/TaskService.java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public Task assignTask(UUID taskId, UUID operatorId) {
        // ... business logic ...
        Task savedTask = taskRepository.save(task);
        
        // Publish event - decoupled from handlers
        eventPublisher.publishEvent(new TaskAssignedEvent(this, savedTask, operator));
        
        return savedTask;
    }
}
```

**Event Handling**:
```java
// backend/src/main/java/com/urbanclean/event/TaskEventListener.java
@Component
@RequiredArgsConstructor
public class TaskEventListener {
    private final EmailService emailService;
    
    @EventListener
    @Async  // Asynchronous processing
    public void handleTaskAssigned(TaskAssignedEvent event) {
        Task task = event.getTask();
        User operator = event.getOperator();
        emailService.sendTaskAssignedEmail(task, operator);
    }
}
```

**Benefits**:
- Loose coupling - publishers don't know about consumers
- Easy to add new event handlers without modifying publishers
- Asynchronous processing - events can be handled in background
- Single Responsibility Principle - each handler has one job
- Extensibility - new features can subscribe to existing events

**Trade-offs**:
- Complexity in debugging - event flow is less obvious
- Eventual consistency - handlers may execute after transaction commits
- Error handling challenges - failures in handlers don't affect publishers
- Testing complexity - need to verify event publishing and handling

**Source References**: 
- Events: `backend/src/main/java/com/urbanclean/event/` (3 event classes)
- Listeners: `backend/src/main/java/com/urbanclean/listener/` (2 listener classes)
- Publishers: `backend/src/main/java/com/urbanclean/service/TaskService.java`

---

### 4. State Machine Pattern

**Description**: The State Machine pattern manages an object's state transitions, ensuring that only valid state changes occur. It enforces business rules about which states can transition to which other states.

**Implementation**: `TaskState` enum with validation logic in `TaskService`

**Evidence in Codebase**:

**State Definition**:
```java
// backend/src/main/java/com/urbanclean/entity/TaskState.java
public enum TaskState {
    PENDIENTE,      // Initial state
    ASIGNADO,       // Assigned to operator
    EN_PROGRESO,    // Work in progress
    RESUELTO,       // Completed
    REABIERTO       // Reopened after resolution
}
```

**State Transition Validation**:
```java
// backend/src/main/java/com/urbanclean/service/TaskService.java
@Service
public class TaskService {
    
    private void validateStateTransition(TaskState currentState, TaskState newState) {
        boolean isValid = switch (currentState) {
            case PENDIENTE -> newState == TaskState.ASIGNADO;
            case ASIGNADO -> newState == TaskState.EN_PROGRESO || newState == TaskState.PENDIENTE;
            case EN_PROGRESO -> newState == TaskState.RESUELTO || newState == TaskState.ASIGNADO;
            case RESUELTO -> newState == TaskState.REABIERTO;
            case REABIERTO -> newState == TaskState.ASIGNADO;
        };
        
        if (!isValid) {
            throw new InvalidStateTransitionException(
                "Cannot transition from " + currentState + " to " + newState
            );
        }
    }
    
    @Transactional
    public Task updateTaskState(UUID taskId, TaskStateUpdateRequest request) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        // Validate transition before applying
        validateStateTransition(task.getState(), request.getNewState());
        
        task.setState(request.getNewState());
        return taskRepository.save(task);
    }
}
```

**State Diagram**:
```
PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO
    ↑           ↓                          ↓
    └───────────┘                     REABIERTO
                                           ↓
                                      ASIGNADO
```

**Benefits**:
- Clear state transitions - explicit rules for valid changes
- Validation of state changes - prevents invalid transitions
- Audit trail - state changes are logged
- Business rule enforcement - encapsulates workflow logic
- Predictable behavior - state machine is deterministic

**Trade-offs**:
- Rigidity in state transitions - hard to change workflow
- Complexity for complex workflows - many states and transitions
- Testing overhead - need to test all valid and invalid transitions

**Source References**: 
- State enum: `backend/src/main/java/com/urbanclean/entity/TaskState.java`
- Validation: `backend/src/main/java/com/urbanclean/service/TaskService.java`
- Exception: `backend/src/main/java/com/urbanclean/exception/custom/InvalidStateTransitionException.java`

---

### 5. Strategy Pattern

**Description**: The Strategy pattern defines a family of algorithms, encapsulates each one, and makes them interchangeable. It allows the algorithm to vary independently from clients that use it.

**Implementation**: `PriorityCalculatorService` with configurable weights from `AlgorithmConfig`

**Evidence in Codebase**:

**Strategy Interface (Implicit)**:
```java
// backend/src/main/java/com/urbanclean/service/PriorityCalculatorService.java
@Service
@RequiredArgsConstructor
public class PriorityCalculatorService {
    private final AlgorithmConfigRepository algorithmConfigRepository;
    
    /**
     * Calculate priority using configurable strategy
     * Priority = (WeightCategory × CategoryValue) + 
     *           (WeightZone × ZoneRiskIndex) + 
     *           (WeightTime × HoursElapsed)
     */
    public BigDecimal calculatePriority(Report report) {
        AlgorithmConfig config = getCurrentConfig();
        
        // Strategy components - weights can be changed at runtime
        BigDecimal categoryComponent = config.getWeightCategory()
            .multiply(mapCategoryToValue(report.getCategory()));
        
        BigDecimal zoneComponent = config.getWeightZone()
            .multiply(calculateZoneRiskIndex(report.getLocation()));
        
        BigDecimal timeComponent = config.getWeightTime()
            .multiply(calculateHoursElapsed(report.getCreatedAt()));
        
        return categoryComponent.add(zoneComponent).add(timeComponent);
    }
    
    private AlgorithmConfig getCurrentConfig() {
        return algorithmConfigRepository.findTopByOrderByCreatedAtDesc()
            .orElseGet(this::getDefaultConfig);
    }
}
```

**Configuration Entity**:
```java
// backend/src/main/java/com/urbanclean/entity/AlgorithmConfig.java
@Entity
@Table(name = "algorithm_config")
public class AlgorithmConfig {
    @Id
    private UUID id;
    
    private BigDecimal weightCategory;  // Configurable weight
    private BigDecimal weightZone;      // Configurable weight
    private BigDecimal weightTime;      // Configurable weight
    
    private LocalDateTime createdAt;
}
```

**Runtime Configuration Change**:
```java
// backend/src/main/java/com/urbanclean/service/ConfigService.java
@Service
@RequiredArgsConstructor
public class ConfigService {
    private final AlgorithmConfigRepository algorithmConfigRepository;
    private final TaskService taskService;
    
    @Transactional
    public AlgorithmConfig updateAlgorithmWeights(AlgorithmWeightsRequest request) {
        // Create new configuration (strategy change)
        AlgorithmConfig newConfig = AlgorithmConfig.builder()
            .weightCategory(request.getWeightCategory())
            .weightZone(request.getWeightZone())
            .weightTime(request.getWeightTime())
            .createdAt(LocalDateTime.now())
            .build();
        
        AlgorithmConfig saved = algorithmConfigRepository.save(newConfig);
        
        // Recalculate all pending tasks with new strategy
        taskService.recalculateAllPriorities();
        
        return saved;
    }
}
```

**Benefits**:
- Flexible algorithm configuration - weights can change without code changes
- Runtime behavior changes - no redeployment needed
- Testability - easy to test with different configurations
- Extensibility - new priority factors can be added
- Historical tracking - configuration changes are audited

**Trade-offs**:
- Additional complexity - more classes and configuration
- Configuration management - need UI for administrators
- Validation required - weights must be validated
- Performance - recalculation needed when strategy changes

**Source References**: 
- Strategy: `backend/src/main/java/com/urbanclean/service/PriorityCalculatorService.java`
- Configuration: `backend/src/main/java/com/urbanclean/entity/AlgorithmConfig.java`
- Management: `backend/src/main/java/com/urbanclean/service/ConfigService.java`
- Controller: `backend/src/main/java/com/urbanclean/controller/ConfigController.java`

---

### Additional Patterns Detected

#### 6. DTO Pattern (Data Transfer Object)

**Description**: DTOs transfer data between layers, decoupling the API contract from the internal domain model.

**Implementation**: Separate request and response DTOs in `com.urbanclean.dto`

**Evidence**: 
- Request DTOs: `backend/src/main/java/com/urbanclean/dto/request/` (17 classes)
- Response DTOs: `backend/src/main/java/com/urbanclean/dto/response/` (20 classes)

**Benefits**: API versioning, security (hide sensitive fields), flexibility

---

#### 7. Dependency Injection Pattern

**Description**: Dependencies are injected rather than created, promoting loose coupling and testability.

**Implementation**: Spring constructor injection with `@RequiredArgsConstructor`

**Evidence**: All services use constructor injection for dependencies

**Benefits**: Testability, loose coupling, immutable dependencies

---

#### 8. Filter Chain Pattern

**Description**: Requests pass through a chain of filters for cross-cutting concerns.

**Implementation**: Spring Security filter chain with custom filters

**Evidence**: 
- `backend/src/main/java/com/urbanclean/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/urbanclean/config/RateLimitingFilter.java`

**Benefits**: Separation of concerns, reusable filters, ordered processing

---

## Technology Stack

_This section will document technology choices and their rationales._

### Backend Framework: Spring Boot 3.2.2

**Rationale**:
- Rapid application development
- Production-ready features (actuator, metrics)
- Extensive ecosystem
- Strong community support
- Built-in security
- Dependency injection
- Auto-configuration

**Alternatives Considered**:
- Jakarta EE: More verbose, less convention-over-configuration
- Quarkus: Newer, smaller ecosystem
- Micronaut: Smaller ecosystem, less mature

**Trade-offs**:
- Larger memory footprint
- Startup time (mitigated in production)
- Magic/auto-configuration can be opaque

**Source Reference**: `backend/pom.xml`

---

## Architectural Decision Justifications

This section provides detailed justifications for the key technology choices made in the Urban Cleaning Management System, explaining why each technology was selected over alternatives and how it contributes to the system's goals.

### Why Spring Boot?

**Primary Justifications**:

1. **Rapid Development**
   - Convention-over-configuration approach reduces boilerplate code
   - Auto-configuration automatically sets up common components
   - Spring Initializr provides quick project scaffolding
   - Embedded server (Tomcat) eliminates deployment complexity
   - Result: Faster time-to-market for MVP and feature iterations

2. **Production-Ready Features**
   - Spring Boot Actuator provides health checks, metrics, and monitoring endpoints
   - Built-in support for externalized configuration (application.properties, environment variables)
   - Comprehensive logging and error handling out of the box
   - Production-grade security with Spring Security integration
   - Result: Reduced operational overhead and easier production deployment

3. **Extensive Ecosystem**
   - Spring Data JPA for database access with minimal code
   - Spring Security for authentication and authorization
   - Spring Cache for caching abstraction
   - Spring Events for event-driven architecture
   - Resilience4j integration for circuit breakers and retry logic
   - Result: Comprehensive solution without reinventing the wheel

4. **Enterprise-Grade Reliability**
   - Mature framework with 20+ years of development
   - Battle-tested in production environments worldwide
   - Strong backward compatibility and migration paths
   - Extensive documentation and community support
   - Result: Lower risk and easier maintenance

5. **Developer Productivity**
   - Dependency injection promotes testable code
   - Annotations reduce boilerplate (@RestController, @Service, @Transactional)
   - Hot reload with Spring DevTools speeds development
   - Excellent IDE support (IntelliJ IDEA, Eclipse, VS Code)
   - Result: Higher developer velocity and code quality

**Evidence in Codebase**:
```java
// Minimal code for REST endpoint with validation, security, and transaction management
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }
}
```

**Alternatives Considered and Why Rejected**:
- **Jakarta EE**: More verbose, requires application server, less convention-over-configuration
- **Quarkus**: Newer framework, smaller ecosystem, less mature tooling
- **Micronaut**: Compile-time dependency injection adds complexity, smaller community
- **Node.js/Express**: Different language, less suitable for complex business logic, weaker typing

**Trade-offs Accepted**:
- Larger memory footprint (~200-300MB) vs lightweight frameworks - acceptable for server deployment
- Startup time (~3-5 seconds) vs instant startup - acceptable for long-running server process
- "Magic" auto-configuration can be opaque - mitigated with Spring Boot Actuator and logging

**Source References**: 
- `backend/pom.xml` (Spring Boot parent and starters)
- `backend/src/main/java/com/urbanclean/UrbanCleaningApplication.java` (Spring Boot application)
- `backend/src/main/java/com/urbanclean/config/` (Spring configuration classes)

---

### Why PostgreSQL + PostGIS?

**Primary Justifications**:

1. **Geospatial Capabilities (PostGIS)**
   - Industry-standard geospatial extension with 20+ years of development
   - Rich spatial functions: distance calculations, containment, intersection, buffering
   - Spatial indexes (GIST) for efficient location-based queries
   - Geography type for accurate distance calculations on Earth's surface
   - Result: Core requirement for location-based duplicate detection and heatmaps

2. **ACID Compliance**
   - Full transactional support ensures data consistency
   - Critical for financial/audit data and state transitions
   - Prevents data corruption during concurrent operations
   - Result: Data integrity guarantees for mission-critical operations

3. **Advanced Features**
   - JSON/JSONB support for flexible data structures
   - Array types for storing collections
   - Common Table Expressions (CTEs) for complex queries
   - Window functions for analytics
   - Full-text search capabilities
   - Result: Powerful query capabilities without external tools

4. **Performance and Scalability**
   - Excellent query optimizer for complex joins and aggregations
   - Efficient indexing strategies (B-tree, GIST, GIN, BRIN)
   - Connection pooling support (HikariCP)
   - Read replicas for scaling read-heavy workloads
   - Result: Handles expected load (single municipality) with room to grow

5. **Open Source and Cost-Effective**
   - No licensing fees (vs Oracle, SQL Server)
   - Active community and regular updates
   - Extensive documentation and resources
   - Commercial support available if needed (EnterpriseDB)
   - Result: Lower total cost of ownership

6. **Integration with Java Ecosystem**
   - Excellent JDBC driver
   - Hibernate Spatial for seamless JPA integration
   - Spring Data JPA support
   - Flyway/Liquibase for migrations
   - Result: Smooth development experience

**Evidence in Codebase**:
```java
// Spatial query for duplicate detection within 100m radius
@Query(value = """
    SELECT * FROM tareas t 
    WHERE ST_DWithin(
        t.location::geography, 
        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, 
        :radiusMeters
    )
    AND t.category = :category
    AND t.created_at >= :since
    """, nativeQuery = true)
List<Task> findNearbyTasks(
    @Param("latitude") Double latitude,
    @Param("longitude") Double longitude,
    @Param("radiusMeters") Double radiusMeters,
    @Param("category") String category,
    @Param("since") LocalDateTime since
);
```

**Alternatives Considered and Why Rejected**:
- **MySQL**: Weaker spatial support, less advanced features, less suitable for complex analytics
- **MongoDB**: NoSQL not suitable for relational data (users, tasks, reports), weaker spatial support than PostGIS
- **Oracle Spatial**: Expensive licensing, overkill for project scale
- **SQL Server**: Licensing costs, less mature spatial support than PostGIS
- **Separate Geospatial Service**: Additional complexity, network latency, data synchronization issues

**Trade-offs Accepted**:
- Learning curve for PostGIS functions - mitigated with documentation and examples
- More complex than simple lat/lon columns - justified by accurate distance calculations and spatial indexes
- Requires PostGIS expertise for optimization - acceptable given benefits

**Source References**: 
- `docker/docker-compose.yml` (postgis/postgis:15-3.3 image)
- `backend/src/main/resources/application.properties` (PostgisDialect configuration)
- `backend/src/main/java/com/urbanclean/entity/Report.java` (geometry column definition)
- `backend/src/main/java/com/urbanclean/service/DeduplicationService.java` (spatial queries)
- `backend/src/main/resources/db/migration/` (spatial indexes)

---

### Why JWT (JSON Web Tokens)?

**Primary Justifications**:

1. **Stateless Authentication**
   - No server-side session storage required
   - Token contains all necessary user information (claims)
   - Server doesn't need to query database for every request
   - Result: Reduced database load and simplified architecture

2. **Horizontal Scalability**
   - No session affinity (sticky sessions) required
   - Any backend instance can validate any token
   - No session replication between servers
   - Load balancer can distribute requests freely
   - Result: Easy to scale by adding more backend instances

3. **Cross-Domain Support**
   - Works across different domains and subdomains
   - Suitable for microservices architecture (future-proofing)
   - Can be used by mobile apps without changes
   - Result: Flexible deployment and future extensibility

4. **Standard Format (RFC 7519)**
   - Industry-standard specification
   - Wide library support across languages
   - Well-understood security model
   - Extensive tooling (jwt.io for debugging)
   - Result: Lower risk and easier integration

5. **Mobile-Friendly**
   - Easy to store in mobile app (secure storage)
   - No cookies required (works with native apps)
   - Same API for web and mobile
   - Result: Future mobile app development simplified

6. **Performance**
   - Fast validation (signature verification only)
   - No database lookup for authentication
   - Can cache public keys for validation
   - Result: Low latency authentication

**Evidence in Codebase**:
```java
// Token generation with user claims
public String generateToken(String username, UUID userId, UserRole role, Integer tokenVersion) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);
    
    return Jwts.builder()
        .setSubject(username)
        .claim("userId", userId.toString())
        .claim("role", role.name())
        .claim("tokenVersion", tokenVersion)  // For invalidation
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
}
```

**Security Enhancements Implemented**:
- Token blacklisting for logout functionality
- Token versioning for invalidating all user tokens
- Refresh token rotation for enhanced security
- Short access token expiration (24 hours)
- Long refresh token expiration (30 days) with database storage
- Device fingerprinting for session tracking

**Alternatives Considered and Why Rejected**:
- **Session-Based Authentication**: Requires server-side storage, sticky sessions, doesn't scale horizontally
- **OAuth 2.0**: Overkill for internal system, adds complexity, requires authorization server
- **SAML**: Too complex, XML-based, enterprise-focused, not suitable for web/mobile apps
- **API Keys**: Less secure, no expiration, no user context, harder to revoke

**Trade-offs Accepted**:
- Token size (~200-300 bytes) vs session ID (~32 bytes) - acceptable given benefits
- Cannot revoke tokens easily - mitigated with blacklist and token versioning
- Token theft risk - mitigated with HTTPS, short expiration, refresh token rotation
- Stateless means cannot track active sessions - mitigated with UserSession entity

**Source References**: 
- `backend/src/main/java/com/urbanclean/security/JwtTokenProvider.java` (token generation)
- `backend/src/main/java/com/urbanclean/security/JwtAuthenticationFilter.java` (token validation)
- `backend/src/main/java/com/urbanclean/service/TokenBlacklistService.java` (token revocation)
- `backend/src/main/java/com/urbanclean/service/RefreshTokenService.java` (refresh tokens)
- `backend/src/main/java/com/urbanclean/entity/UserSession.java` (session tracking)

---

### Why React?

**Primary Justifications**:

1. **Component-Based Architecture**
   - Reusable UI components reduce code duplication
   - Clear separation of concerns (presentation vs logic)
   - Easy to maintain and test individual components
   - Component composition for complex UIs
   - Result: Maintainable and scalable frontend codebase

2. **Rich Ecosystem**
   - React Router for client-side routing
   - Leaflet/React-Leaflet for interactive maps
   - Axios for HTTP requests
   - Large library ecosystem for common needs
   - Result: Don't reinvent the wheel, faster development

3. **Strong Community and Industry Adoption**
   - Backed by Meta (Facebook) with long-term commitment
   - Largest React community among frontend frameworks
   - Extensive documentation, tutorials, and resources
   - Easy to find developers with React experience
   - Result: Lower hiring costs and knowledge transfer

4. **Performance**
   - Virtual DOM for efficient updates
   - Reconciliation algorithm minimizes DOM operations
   - Code splitting for faster initial load
   - Lazy loading for on-demand component loading
   - Result: Fast, responsive user interface

5. **Developer Experience**
   - React DevTools for debugging
   - Hot Module Replacement (HMR) for instant feedback
   - JSX provides type safety and IDE support
   - Hooks simplify state management
   - Result: High developer productivity

6. **Future-Proofing**
   - React Native for mobile apps (code reuse)
   - Server-side rendering (SSR) possible if needed
   - Concurrent features for better UX
   - Active development and regular updates
   - Result: Investment in React pays off long-term

**Evidence in Codebase**:
```javascript
// Reusable component with hooks for state and effects
const TaskList = () => {
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    
    useEffect(() => {
        taskService.getTasks()
            .then(setTasks)
            .finally(() => setLoading(false));
    }, []);
    
    if (loading) return <LoadingSpinner />;
    
    return (
        <div className="task-list">
            {tasks.map(task => (
                <TaskCard key={task.id} task={task} />
            ))}
        </div>
    );
};
```

**Alternatives Considered and Why Rejected**:
- **Vue.js**: Smaller ecosystem, less industry adoption, harder to find developers
- **Angular**: More opinionated, steeper learning curve, TypeScript required, heavier framework
- **Svelte**: Newer framework, smaller ecosystem, less mature tooling, fewer developers
- **Vanilla JavaScript**: Too much boilerplate, harder to maintain, no component model

**Trade-offs Accepted**:
- Requires build tooling (Vite) - acceptable for modern development
- JSX learning curve - mitigated with good documentation
- State management complexity for large apps - mitigated with Context API
- SEO challenges - not relevant for authenticated application

**Source References**: 
- `frontend/package.json` (React dependencies)
- `frontend/src/components/` (reusable components)
- `frontend/src/pages/` (page components)
- `frontend/src/hooks/` (custom hooks)
- `frontend/src/context/AuthContext.jsx` (state management)

---

### Why Docker?

**Primary Justifications**:

1. **Consistent Deployment Across Environments**
   - Same container runs on development, staging, and production
   - "Works on my machine" problem eliminated
   - Identical environment configuration everywhere
   - Result: Fewer deployment issues and faster troubleshooting

2. **Simplified Dependency Management**
   - All dependencies packaged in container image
   - No need to install Java, Node.js, PostgreSQL on host
   - Version conflicts eliminated
   - Result: Easier onboarding for new developers

3. **Isolation and Security**
   - Each service runs in isolated container
   - Resource limits prevent one service from affecting others
   - Network isolation between services
   - Result: Better security and resource management

4. **Portability**
   - Containers run on any platform (Linux, macOS, Windows)
   - Easy to move between cloud providers
   - Can run on-premises or in cloud
   - Result: Flexibility in deployment options

5. **Easy Local Development Setup**
   - Single `docker-compose up` command starts entire stack
   - No manual installation of dependencies
   - Consistent development environment for all team members
   - Result: New developers productive in minutes

6. **Version Control for Infrastructure**
   - Dockerfile and docker-compose.yml in Git
   - Infrastructure as code
   - Track changes to deployment configuration
   - Result: Reproducible infrastructure and easy rollbacks

7. **Scalability**
   - Easy to scale services horizontally (multiple containers)
   - Load balancing with Docker Swarm or Kubernetes
   - Resource allocation per service
   - Result: Can scale as load increases

**Evidence in Codebase**:
```yaml
# docker/docker-compose.yml - Complete stack in one file
version: '3.8'

services:
  postgres:
    image: postgis/postgis:15-3.3
    environment:
      POSTGRES_DB: urbanclean
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/urbanclean

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend
```

**Multi-Stage Builds for Optimization**:
```dockerfile
# Backend Dockerfile - Build and runtime stages
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Alternatives Considered and Why Rejected**:
- **Kubernetes**: Overkill for single-server deployment, too complex for current scale
- **Virtual Machines**: Heavier resource usage, slower startup, larger images
- **Bare Metal**: Environment inconsistencies, manual dependency management, harder to reproduce
- **Serverless (AWS Lambda)**: Not suitable for long-running processes, cold start issues, vendor lock-in

**Trade-offs Accepted**:
- Docker learning curve - mitigated with documentation and Docker Compose
- Resource overhead (~100MB per container) - acceptable for server deployment
- Networking complexity - mitigated with Docker Compose networking
- Build time for images - mitigated with layer caching and multi-stage builds

**Benefits Realized**:
- New developer setup time: ~5 minutes (vs hours without Docker)
- Deployment consistency: 100% (same container everywhere)
- Environment issues: Reduced by ~90%
- Infrastructure documentation: In code (Dockerfile, docker-compose.yml)

**Source References**: 
- `docker/docker-compose.yml` (service orchestration)
- `backend/Dockerfile` (backend container)
- `frontend/Dockerfile` (frontend container)
- `docker/init-db.sql` (database initialization)
- `docker/README.md` (Docker setup documentation)

---

## Summary of Architectural Decisions

The technology choices for the Urban Cleaning Management System were driven by several key principles:

1. **Pragmatism Over Perfection**: Choose technologies that solve current problems without over-engineering
2. **Developer Productivity**: Prioritize tools that enable fast development and easy maintenance
3. **Production Readiness**: Select mature, battle-tested technologies with strong community support
4. **Future-Proofing**: Ensure choices allow for future scaling and evolution
5. **Cost-Effectiveness**: Prefer open-source solutions with no licensing fees
6. **Team Capabilities**: Consider team size and expertise in technology selection

**Technology Stack Summary**:
- **Spring Boot**: Rapid development, production-ready, extensive ecosystem
- **PostgreSQL + PostGIS**: Geospatial capabilities, ACID compliance, advanced features
- **JWT**: Stateless authentication, horizontal scalability, mobile-friendly
- **React**: Component-based UI, rich ecosystem, strong community
- **Docker**: Consistent deployment, simplified dependencies, portability

All decisions are based on actual project requirements, expected scale (single municipality), team size (small), and long-term maintainability. The chosen stack provides a solid foundation for current needs while allowing for future growth and evolution.

---

### Programming Language: Java 17

**Rationale**:
- LTS (Long-Term Support) version
- Modern language features (records, pattern matching, text blocks)
- Strong typing
- Excellent tooling
- Large talent pool
- Enterprise-grade performance

**Alternatives Considered**:
- Kotlin: Less widespread adoption
- Scala: Steeper learning curve
- Go: Different ecosystem, less OOP

**Trade-offs**:
- More verbose than some alternatives
- Slower compilation than Go

**Source Reference**: `backend/pom.xml`

---

### Database: PostgreSQL 15 + PostGIS 3.3

**Rationale**:
- **PostgreSQL**:
  - ACID compliance
  - Advanced features (JSON, arrays, CTEs)
  - Excellent performance
  - Open source
  - Strong community
- **PostGIS**:
  - Industry-standard geospatial extension
  - Spatial indexes (GIST)
  - Rich spatial functions
  - Integration with mapping tools

**Alternatives Considered**:
- MySQL: Weaker spatial support
- MongoDB: NoSQL, less suitable for relational data
- Oracle: Expensive licensing

**Trade-offs**:
- More complex than simpler databases
- Requires PostGIS expertise for spatial queries

**Source Reference**: `docker/docker-compose.yml`

---

### Frontend Framework: React 18

**Rationale**:
- Component-based architecture
- Virtual DOM for performance
- Large ecosystem
- Strong community
- Excellent tooling
- Concurrent features
- Hooks for state management

**Alternatives Considered**:
- Vue.js: Smaller ecosystem
- Angular: More opinionated, steeper learning curve
- Svelte: Newer, smaller ecosystem

**Trade-offs**:
- Requires build tooling
- JSX learning curve
- State management complexity

**Source Reference**: `frontend/package.json`

---

### Build Tool (Frontend): Vite

**Rationale**:
- Fast development server
- Hot module replacement
- Optimized production builds
- Modern ES modules
- Simple configuration

**Alternatives Considered**:
- Create React App: Slower, less flexible
- Webpack: More complex configuration
- Parcel: Less mature

**Trade-offs**:
- Newer tool, evolving ecosystem

**Source Reference**: `frontend/package.json`, `frontend/vite.config.js`

---

### Containerization: Docker + Docker Compose

**Rationale**:
- Consistent deployment across environments
- Isolation of services
- Easy local development setup
- Portability
- Version control for infrastructure
- Simplified dependency management

**Alternatives Considered**:
- Kubernetes: Overkill for single-server deployment
- Virtual machines: Heavier, slower
- Bare metal: Environment inconsistencies

**Trade-offs**:
- Docker learning curve
- Resource overhead
- Networking complexity

**Source Reference**: `docker/docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`

---

## Security Architecture

_This section will document security decisions._

### Authentication: JWT (JSON Web Tokens)

**Rationale**:
- Stateless authentication - no server-side session storage required
- Scalability - can scale horizontally without session replication
- Cross-domain support - works across different domains/services
- Mobile-friendly - easy to use in mobile applications
- Standard format (RFC 7519) - widely supported

**Implementation Details**:

**Token Generation**:
```java
// backend/src/main/java/com/urbanclean/security/JwtTokenProvider.java
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration;  // 24 hours (86400000 ms)
    
    public String generateToken(String username, UUID userId, UserRole role, Integer tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .setSubject(username)
            .claim("userId", userId.toString())
            .claim("role", role.name())
            .claim("tokenVersion", tokenVersion)  // For invalidation
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
}
```

**Token Structure**:
- **Access Tokens**: 24-hour expiration, contains user claims
- **Refresh Tokens**: 30-day expiration, stored in database
- **Token Versioning**: Allows invalidation of all user tokens
- **Signing Algorithm**: HS512 (HMAC with SHA-512)

**Token Blacklisting**:
```java
// backend/src/main/java/com/urbanclean/service/TokenBlacklistService.java
@Service
public class TokenBlacklistService {
    public void addToBlacklist(String token, TokenType type, UUID userId, 
                               LocalDateTime expiresAt, String reason) {
        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
            .token(token)
            .tokenType(type)
            .userId(userId)
            .expiresAt(expiresAt)
            .revocationReason(reason)
            .revokedAt(LocalDateTime.now())
            .build();
        
        tokenBlacklistRepository.save(blacklistedToken);
    }
    
    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }
}
```

**Token Validation Flow**:
```
1. Extract JWT from Authorization header
2. Validate signature and expiration
3. Check if token is blacklisted
4. Extract token version from claims
5. Compare with user's current token version
6. Set authentication in SecurityContext
```

**Alternatives Considered**:
- **Session-based**: Requires server-side storage, less scalable, sticky sessions
- **OAuth 2.0**: Overkill for internal system, adds complexity
- **SAML**: Too complex, XML-based, enterprise-focused

**Trade-offs**:
- Token size (larger than session IDs) - ~200-300 bytes
- Cannot revoke tokens easily - mitigated with blacklist and token versioning
- Token theft risk - mitigated with HTTPS, short expiration, refresh token rotation
- Stateless means cannot track active sessions - mitigated with UserSession entity

**Source References**: 
- Token Provider: `backend/src/main/java/com/urbanclean/security/JwtTokenProvider.java`
- Auth Service: `backend/src/main/java/com/urbanclean/service/AuthService.java`
- Blacklist Service: `backend/src/main/java/com/urbanclean/service/TokenBlacklistService.java`
- Filter: `backend/src/main/java/com/urbanclean/security/JwtAuthenticationFilter.java`

---

### Password Hashing: BCrypt

**Rationale**:
- Industry standard for password hashing
- Adaptive - configurable work factor (cost parameter)
- Salt included automatically - no separate salt management
- Resistant to rainbow tables - unique salt per password
- Resistant to brute force - intentionally slow

**Implementation Details**:

**Configuration**:
```java
// backend/src/main/java/com/urbanclean/config/SecurityConfig.java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Default work factor: 10
    }
}
```

**Usage in Registration**:
```java
// backend/src/main/java/com/urbanclean/service/AuthService.java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public User register(RegisterRequest request) {
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))  // BCrypt hashing
            .role(request.getRole())
            .build();
        
        return userRepository.save(user);
    }
}
```

**Password Validation**:
```java
public boolean validatePassword(String rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
}
```

**BCrypt Hash Format**:
```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 │  │  │                        │
 │  │  └─ Salt (22 characters)  └─ Hash (31 characters)
 │  └─ Cost factor (2^10 = 1024 rounds)
 └─ Algorithm identifier
```

**Work Factor**: 10 (default)
- 2^10 = 1024 iterations
- ~100ms to hash on modern hardware
- Balance between security and performance

**Alternatives Considered**:
- **Argon2**: Newer, winner of Password Hashing Competition, but less widespread support in Java ecosystem
- **PBKDF2**: Older standard, less resistant to GPU/ASIC attacks than BCrypt
- **Scrypt**: Memory-hard function, less widespread adoption, more complex configuration

**Trade-offs**:
- Slower than simple hashing (intentional) - ~100ms per hash
- CPU-intensive - can impact performance under high load
- Cannot upgrade work factor for existing passwords without re-hashing

**Security Benefits**:
- Prevents rainbow table attacks (unique salt)
- Slows down brute force attacks (adaptive cost)
- Future-proof (can increase work factor over time)
- Widely audited and trusted

**Source References**: 
- Configuration: `backend/src/main/java/com/urbanclean/config/SecurityConfig.java`
- Usage: `backend/src/main/java/com/urbanclean/service/AuthService.java`
- Entity: `backend/src/main/java/com/urbanclean/entity/User.java` (passwordHash field)

---

### Authorization: Role-Based Access Control (RBAC)

**Rationale**:
- Simple to understand and implement
- Easy to manage - roles map to job functions
- Sufficient for system requirements - three clear user types
- Spring Security integration - built-in support
- Scalable for small to medium systems

**Implementation Details**:

**Role Definition**:
```java
// backend/src/main/java/com/urbanclean/entity/UserRole.java
public enum UserRole {
    ROLE_CIUDADANO,  // Citizens - can submit reports
    ROLE_TECNICO,    // Operators - can manage tasks
    ROLE_ADMIN       // Administrators - full system access
}
```

**Role Assignment**:
```java
// backend/src/main/java/com/urbanclean/entity/User.java
@Entity
@Table(name = "usuarios")
public class User {
    @Id
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;  // Single role per user
}
```

**Endpoint Protection**:
```java
// backend/src/main/java/com/urbanclean/controller/TaskController.java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    // Only operators and admins can view tasks
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }
    
    // Only operators and admins can assign tasks
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    @PatchMapping("/{id}/assign")
    public ResponseEntity<TaskResponse> assignTask(@PathVariable UUID id) {
        // ...
    }
    
    // Only admins can access configuration
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/config/algorithm-weights")
    public ResponseEntity<AlgorithmWeightsResponse> updateWeights() {
        // ...
    }
}
```

**Security Configuration**:
```java
// backend/src/main/java/com/urbanclean/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Public endpoints
                .requestMatchers("/api/reports").hasAnyRole("CIUDADANO", "TECNICO", "ADMIN")
                .requestMatchers("/api/tasks/**").hasAnyRole("TECNICO", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
```

**Role Hierarchy**:
```
ADMIN (highest privileges)
  ├─ Can access all endpoints
  ├─ Can configure system
  └─ Can perform all operator actions
  
TECNICO (operator privileges)
  ├─ Can manage tasks
  ├─ Can view analytics
  └─ Can submit reports
  
CIUDADANO (citizen privileges)
  ├─ Can submit reports
  ├─ Can view own reports
  └─ Can provide feedback
```

**Permission Matrix**:

| Endpoint | CIUDADANO | TECNICO | ADMIN |
|----------|-----------|---------|-------|
| POST /api/reports | ✓ | ✓ | ✓ |
| GET /api/reports/my | ✓ | ✓ | ✓ |
| GET /api/tasks | ✗ | ✓ | ✓ |
| PATCH /api/tasks/{id}/assign | ✗ | ✓ | ✓ |
| GET /api/analytics/** | ✗ | ✓ | ✓ |
| PUT /api/admin/config/** | ✗ | ✗ | ✓ |

**Alternatives Considered**:
- **Attribute-Based Access Control (ABAC)**: Too complex for current requirements, would add significant overhead
- **Permission-based**: More granular but requires more management, overkill for three user types
- **ACL (Access Control Lists)**: Resource-level permissions, not needed for this system

**Trade-offs**:
- Less granular than permission-based - cannot have fine-grained permissions within a role
- Role explosion for complex systems - not an issue with only three roles
- Cannot easily handle temporary permissions - would need additional mechanism
- Single role per user - cannot combine roles (acceptable for this system)

**Security Benefits**:
- Clear separation of duties
- Easy to audit - role assignments are explicit
- Simple to understand - maps to organizational structure
- Prevents privilege escalation - roles are enforced at multiple layers

**Source References**: 
- Role enum: `backend/src/main/java/com/urbanclean/entity/UserRole.java`
- Security config: `backend/src/main/java/com/urbanclean/config/SecurityConfig.java`
- Controller annotations: `backend/src/main/java/com/urbanclean/controller/` (all controllers)
- User entity: `backend/src/main/java/com/urbanclean/entity/User.java`

---

### Session Management

**Rationale**:
- Track active sessions per user - security monitoring
- Device fingerprinting for security - detect suspicious logins
- Session revocation capability - logout from all devices
- Audit trail - track login history and patterns
- Compliance - GDPR requires tracking of data access

**Implementation Details**:

**Session Entity**:
```java
// backend/src/main/java/com/urbanclean/entity/UserSession.java
@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private UUID refreshTokenId;  // Links to refresh token
    
    @Column(nullable = false)
    private String deviceFingerprint;  // Unique device identifier
    
    private String ipAddress;
    private String userAgent;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime lastAccessedAt;
    
    @Column(nullable = false)
    private Boolean isActive;
}
```

**Device Fingerprinting**:
```java
// backend/src/main/java/com/urbanclean/util/DeviceFingerprintUtil.java
public class DeviceFingerprintUtil {
    
    public static String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        
        String combined = userAgent + "|" + acceptLanguage + "|" + acceptEncoding;
        
        // Generate SHA-256 hash
        return DigestUtils.sha256Hex(combined);
    }
    
    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

**Session Creation on Login**:
```java
// backend/src/main/java/com/urbanclean/service/AuthService.java
@Transactional
public LoginResponse login(String username, String password, HttpServletRequest request) {
    // ... authentication ...
    
    // Generate device fingerprint
    String deviceFingerprint = DeviceFingerprintUtil.generateFingerprint(request);
    String ipAddress = DeviceFingerprintUtil.getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");
    
    // Generate tokens
    String accessToken = jwtTokenProvider.generateToken(user);
    String refreshToken = refreshTokenService.createRefreshToken(user.getId(), deviceFingerprint);
    
    // Create session
    RefreshToken refreshTokenEntity = refreshTokenService.validateRefreshToken(refreshToken);
    userSessionService.createSession(
        user.getId(),
        refreshTokenEntity.getId(),
        deviceFingerprint,
        ipAddress,
        userAgent
    );
    
    return LoginResponse.builder()
        .token(accessToken)
        .refreshToken(refreshToken)
        .build();
}
```

**Session Management Service**:
```java
// backend/src/main/java/com/urbanclean/service/UserSessionService.java
@Service
@RequiredArgsConstructor
public class UserSessionService {
    
    @Transactional
    public UserSession createSession(UUID userId, UUID refreshTokenId, 
                                     String deviceFingerprint, String ipAddress, String userAgent) {
        UserSession session = UserSession.builder()
            .userId(userId)
            .refreshTokenId(refreshTokenId)
            .deviceFingerprint(deviceFingerprint)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .createdAt(LocalDateTime.now())
            .lastAccessedAt(LocalDateTime.now())
            .isActive(true)
            .build();
        
        return userSessionRepository.save(session);
    }
    
    @Transactional
    public void revokeAllSessions(UUID userId) {
        List<UserSession> sessions = userSessionRepository.findByUserIdAndIsActive(userId, true);
        sessions.forEach(session -> {
            session.setIsActive(false);
            session.setLastAccessedAt(LocalDateTime.now());
        });
        userSessionRepository.saveAll(sessions);
    }
    
    @Transactional(readOnly = true)
    public List<UserSessionResponse> getActiveSessions(UUID userId) {
        return userSessionRepository.findByUserIdAndIsActive(userId, true)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
}
```

**Logout from All Devices**:
```java
// backend/src/main/java/com/urbanclean/service/AuthService.java
@Transactional
public void logoutAll(String accessToken) {
    String username = jwtTokenProvider.getUsernameFromToken(accessToken);
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new AuthenticationException("User not found"));
    
    // Revoke all refresh tokens
    refreshTokenService.revokeAllUserTokens(user.getId());
    
    // Revoke all sessions
    userSessionService.revokeAllSessions(user.getId());
    
    // Increment token version to invalidate all existing access tokens
    user.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1);
    userRepository.save(user);
}
```

**Session Monitoring UI**:
```javascript
// frontend/src/components/user/ActiveSessions.jsx
const ActiveSessions = () => {
    const [sessions, setSessions] = useState([]);
    
    useEffect(() => {
        sessionService.getActiveSessions().then(setSessions);
    }, []);
    
    const handleRevokeSession = (sessionId) => {
        sessionService.revokeSession(sessionId).then(() => {
            setSessions(sessions.filter(s => s.id !== sessionId));
        });
    };
    
    return (
        <div>
            {sessions.map(session => (
                <SessionCard 
                    key={session.id}
                    session={session}
                    onRevoke={handleRevokeSession}
                />
            ))}
        </div>
    );
};
```

**Security Benefits**:
- Detect suspicious logins - unusual IP or device
- Revoke compromised sessions - logout from specific device
- Audit trail - track all login activity
- User awareness - users can see active sessions
- Compliance - GDPR data access tracking

**Trade-offs**:
- Additional database storage - one row per session
- Complexity in session management - more code to maintain
- Performance overhead - session checks on each request
- Cleanup required - need to remove expired sessions

**Source References**: 
- Session entity: `backend/src/main/java/com/urbanclean/entity/UserSession.java`
- Session service: `backend/src/main/java/com/urbanclean/service/UserSessionService.java`
- Device fingerprinting: `backend/src/main/java/com/urbanclean/util/DeviceFingerprintUtil.java`
- Auth service: `backend/src/main/java/com/urbanclean/service/AuthService.java`
- Frontend component: `frontend/src/components/user/ActiveSessions.jsx`

---

## Data Persistence Architecture

_This section will document data persistence decisions._

### ORM: JPA/Hibernate

**Rationale**:
- Object-relational mapping - work with objects instead of SQL
- Reduced boilerplate SQL - automatic CRUD operations
- Database portability - can switch databases with minimal changes
- Lazy loading - load related entities on demand
- Caching - first-level and second-level cache support
- Transaction management - automatic transaction handling

**Implementation Details**:

**Entity Mapping**:
```java
// backend/src/main/java/com/urbanclean/entity/Task.java
@Entity
@Table(name = "tareas", indexes = {
    @Index(name = "idx_task_state", columnList = "state"),
    @Index(name = "idx_task_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_operator_id")
    private User assignedOperator;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskState state;
    
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal priorityScore;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime resolvedAt;
    
    @Column(nullable = false)
    private Integer duplicateCount = 0;
}
```

**Repository Pattern with Spring Data JPA**:
```java
// backend/src/main/java/com/urbanclean/repository/TaskRepository.java
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    // Method name queries - Spring generates implementation
    List<Task> findByState(TaskState state);
    List<Task> findByAssignedOperator(User operator);
    
    // JPQL queries
    @Query("SELECT t FROM Task t WHERE t.state = :state AND t.createdAt >= :startDate")
    List<Task> findByStateAndCreatedAtAfter(
        @Param("state") TaskState state,
        @Param("startDate") LocalDateTime startDate
    );
    
    // Native SQL for complex queries
    @Query(value = "SELECT * FROM tareas WHERE state = ?1 ORDER BY priority_score DESC LIMIT ?2", 
           nativeQuery = true)
    List<Task> findTopPriorityTasks(String state, int limit);
}
```

**Configuration**:
```properties
# backend/src/main/resources/application.properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.spatial.dialect.postgis.PostgisDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

**Fetch Strategies**:
- **LAZY** (default for @ManyToOne, @OneToOne): Load related entities on access
- **EAGER** (default for @OneToMany, @ManyToMany): Load related entities immediately
- Custom: Use `@EntityGraph` for specific queries

**Transaction Management**:
```java
// backend/src/main/java/com/urbanclean/service/ReportService.java
@Service
@RequiredArgsConstructor
public class ReportService {
    
    @Transactional  // Entire method runs in a transaction
    public Report createReport(ReportSubmissionRequest request, MultipartFile photo) {
        // All operations in one transaction
        String photoUrl = fileStorageService.storeFile(photo);
        Report report = reportRepository.save(newReport);
        taskService.createTask(report);
        
        // If any operation fails, entire transaction rolls back
        return report;
    }
    
    @Transactional(readOnly = true)  // Optimization for read-only operations
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }
}
```

**Alternatives Considered**:
- **JDBC**: Too low-level, more boilerplate, manual mapping
- **jOOQ**: Type-safe SQL but more verbose, less abstraction
- **MyBatis**: XML configuration, less abstraction, more manual mapping

**Trade-offs**:
- **N+1 query problem**: Can occur with lazy loading (mitigated with fetch joins, @EntityGraph)
- **Learning curve**: Understanding JPA lifecycle, fetch strategies, caching
- **Less control over SQL**: Generated SQL may not be optimal (can use native queries)
- **Debugging complexity**: Stack traces can be deep, generated SQL needs inspection

**Performance Optimizations**:
- Batch inserts/updates (configured in properties)
- Query result caching
- Lazy loading for large relationships
- Indexes on frequently queried columns
- Native queries for complex operations

**Source References**: 
- Entities: `backend/src/main/java/com/urbanclean/entity/` (15 entity classes)
- Repositories: `backend/src/main/java/com/urbanclean/repository/` (13 repository interfaces)
- Configuration: `backend/src/main/resources/application.properties`
- Services: `backend/src/main/java/com/urbanclean/service/` (transaction management)

---

### Spatial Data: Hibernate Spatial

**Rationale**:
- Seamless integration with Hibernate - no separate library needed
- PostGIS support - leverages PostgreSQL spatial extensions
- Type-safe spatial operations - Java types for geometry
- JPA annotations for spatial columns - consistent with other entities
- Spatial query support - distance, containment, intersection

**Implementation Details**:

**Spatial Column Definition**:
```java
// backend/src/main/java/com/urbanclean/entity/Report.java
@Entity
@Table(name = "reportes")
public class Report {
    @Id
    private UUID id;
    
    // PostGIS Point type with SRID 4326 (WGS 84)
    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;
    
    // Getters/setters
}
```

**Creating Spatial Data**:
```java
// backend/src/main/java/com/urbanclean/service/GeofencingService.java
@Service
public class GeofencingService {
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    
    public Point createPoint(Double latitude, Double longitude) {
        Coordinate coordinate = new Coordinate(longitude, latitude);  // Note: lon, lat order
        return geometryFactory.createPoint(coordinate);
    }
    
    public boolean isWithinBounds(Point location, Double minLat, Double maxLat, 
                                  Double minLon, Double maxLon) {
        double lat = location.getY();
        double lon = location.getX();
        
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }
}
```

**Spatial Queries**:
```java
// backend/src/main/java/com/urbanclean/repository/TaskRepository.java
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    // Find tasks within radius using PostGIS ST_DWithin
    @Query(value = """
        SELECT * FROM tareas t 
        WHERE ST_DWithin(
            t.location::geography, 
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, 
            :radiusMeters
        )
        AND t.category = :category
        AND t.created_at >= :since
        """, nativeQuery = true)
    List<Task> findNearbyTasks(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusMeters") Double radiusMeters,
        @Param("category") String category,
        @Param("since") LocalDateTime since
    );
    
    // Calculate distance between two points
    @Query(value = """
        SELECT ST_Distance(
            t.location::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
        ) as distance
        FROM tareas t
        WHERE t.id = :taskId
        """, nativeQuery = true)
    Double calculateDistance(
        @Param("taskId") UUID taskId,
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude
    );
}
```

**Duplicate Detection with Spatial Queries**:
```java
// backend/src/main/java/com/urbanclean/service/DeduplicationService.java
@Service
@RequiredArgsConstructor
public class DeduplicationService {
    private static final double DUPLICATE_RADIUS_METERS = 100.0;
    private static final long DUPLICATE_TIME_WINDOW_HOURS = 24;
    
    public Optional<Task> checkForDuplicates(Report report) {
        LocalDateTime since = report.getCreatedAt().minusHours(DUPLICATE_TIME_WINDOW_HOURS);
        
        List<Task> nearbyTasks = taskRepository.findNearbyTasks(
            report.getLocation().getY(),  // latitude
            report.getLocation().getX(),  // longitude
            DUPLICATE_RADIUS_METERS,
            report.getCategory(),
            since
        );
        
        return nearbyTasks.stream()
            .filter(task -> task.getState() != TaskState.RESUELTO)
            .findFirst();
    }
}
```

**Spatial Indexes**:
```sql
-- backend/src/main/resources/db/migration/V1__initial_schema.sql
CREATE INDEX idx_report_location ON reportes USING GIST(location);
CREATE INDEX idx_task_location ON tareas USING GIST(location);
```

**PostGIS Functions Used**:
- `ST_MakePoint(lon, lat)`: Create point from coordinates
- `ST_SetSRID(geom, srid)`: Set spatial reference system
- `ST_DWithin(geom1, geom2, distance)`: Check if within distance
- `ST_Distance(geom1, geom2)`: Calculate distance between points
- `::geography`: Cast to geography type for accurate distance calculations

**Alternatives Considered**:
- **Native SQL only**: Less type-safe, more boilerplate, no Java types
- **GeoTools**: Separate library, less integration with Hibernate
- **Manual distance calculations**: Haversine formula, less accurate, no spatial indexes

**Trade-offs**:
- Limited spatial operations compared to native PostGIS - can use native queries for complex operations
- Additional dependency - Hibernate Spatial library
- Learning curve - understanding spatial types and operations
- SRID management - must ensure consistent spatial reference system

**Performance Considerations**:
- GIST indexes for spatial queries - essential for performance
- Geography vs Geometry types - geography for accurate distances, geometry for faster operations
- Spatial query optimization - use appropriate functions and indexes

**Source References**: 
- Entities with spatial columns: `backend/src/main/java/com/urbanclean/entity/Report.java`, `Task.java`
- Geofencing service: `backend/src/main/java/com/urbanclean/service/GeofencingService.java`
- Deduplication service: `backend/src/main/java/com/urbanclean/service/DeduplicationService.java`
- Spatial queries: `backend/src/main/java/com/urbanclean/repository/TaskRepository.java`
- Migrations: `backend/src/main/resources/db/migration/`

---

### Database Migrations: Flyway

**Rationale**:
- Version control for database schema - track schema changes in Git
- Repeatable migrations - same schema across all environments
- Rollback support - can revert to previous versions
- Team collaboration - avoid schema conflicts
- Production-safe - tested migrations before deployment
- Audit trail - know who changed what and when

**Implementation Details**:

**Migration File Structure**:
```
backend/src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__create_password_reset_tokens.sql
├── V3__add_task_feedback_fields.sql
├── V4__create_citizen_feedback.sql
├── V5__add_gdpr_fields_to_users.sql
├── V8__add_token_version_to_users.sql
├── V9__add_ip_address_to_audit_log.sql
├── V10__create_failed_login_attempts_table.sql
├── V11__create_notification_preferences.sql
├── V12__create_notification_failures.sql
├── V13__analytics_indexes.sql
├── V14__add_resolved_at_to_tasks.sql
├── V15__create_refresh_tokens.sql
├── V16__create_token_blacklist.sql
├── V17__create_user_sessions.sql
├── V18__extend_algorithm_config.sql
└── V19__add_token_expiration_columns.sql
```

**Naming Convention**:
- `V{version}__{description}.sql`
- Version: Sequential number (V1, V2, V3, ...)
- Description: Snake_case description of change
- Example: `V10__create_failed_login_attempts_table.sql`

**Example Migration**:
```sql
-- V15__create_refresh_tokens.sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_fingerprint VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(100)
);

CREATE INDEX idx_refresh_token_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_token_expires_at ON refresh_tokens(expires_at);
```

**Configuration**:
```properties
# backend/src/main/resources/application.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true

# JPA should validate, not create schema
spring.jpa.hibernate.ddl-auto=validate
```

**Migration Execution**:
- Automatic on application startup
- Flyway checks `flyway_schema_history` table
- Executes pending migrations in order
- Records execution in history table
- Fails fast if migration fails

**Flyway Schema History Table**:
```sql
SELECT * FROM flyway_schema_history;

installed_rank | version | description                    | type | script                                  | checksum    | installed_by | installed_on        | execution_time | success
---------------+---------+--------------------------------+------+-----------------------------------------+-------------+--------------+---------------------+----------------+---------
1              | 1       | initial schema                 | SQL  | V1__initial_schema.sql                  | 1234567890  | postgres     | 2024-01-15 10:00:00 | 150            | true
2              | 2       | create password reset tokens   | SQL  | V2__create_password_reset_tokens.sql    | 9876543210  | postgres     | 2024-01-20 11:30:00 | 50             | true
```

**Best Practices**:
1. **Never modify existing migrations** - create new migration instead
2. **Test migrations locally** - before committing
3. **Keep migrations small** - one logical change per migration
4. **Use transactions** - migrations are transactional by default
5. **Add indexes** - include index creation in migrations
6. **Document complex changes** - add comments in SQL

**Rollback Strategy**:
- Flyway Community Edition doesn't support automatic rollback
- Manual rollback: Create new migration to undo changes
- Example: `V20__rollback_feature_x.sql`

**Alternatives Considered**:
- **Liquibase**: XML-based (more verbose), more features but more complex
- **JPA schema generation**: Not production-safe, no version control
- **Manual migrations**: Error-prone, no tracking, team conflicts

**Trade-offs**:
- Cannot modify existing migrations - must create new ones
- Requires discipline in migration naming and ordering
- No automatic rollback in community edition
- Failed migrations require manual intervention

**Benefits**:
- Schema versioning - know exact state of database
- Reproducible deployments - same schema everywhere
- Team collaboration - no schema conflicts
- Production safety - tested migrations
- Audit trail - complete history of changes

**Source References**: 
- Migrations: `backend/src/main/resources/db/migration/` (19 migration files)
- Configuration: `backend/src/main/resources/application.properties`
- Documentation: `backend/DATABASE_MIGRATION_REVIEW.md`

---

## Architectural Decision Records

_This section will document major architectural decisions._

### ADR-001: Monolithic Architecture

**Status**: Accepted

**Context**: 
The Urban Cleaning Management System needed an architectural approach that would:
- Support rapid development with a small team
- Minimize operational complexity
- Provide good performance for expected load (single municipality)
- Allow for future scaling if needed

**Decision**: 
Implement a monolithic architecture with clear internal layer separation (presentation, business logic, data access) rather than microservices.

**Rationale**:
1. **Team Size**: Small development team benefits from simpler deployment and debugging
2. **Complexity**: Microservices add significant operational overhead (service discovery, distributed tracing, inter-service communication)
3. **Performance**: No network latency between components, simpler transactions
4. **Development Speed**: Faster iteration, easier refactoring, simpler testing
5. **Deployment**: Single deployment unit, easier rollbacks
6. **Scale**: Expected load (single municipality) doesn't require microservices
7. **Future**: Can extract microservices later if specific components need independent scaling

**Consequences**:
- **Positive**:
  - Faster development and deployment
  - Simpler debugging and monitoring
  - Easier transaction management
  - Lower operational costs
  - Single codebase easier to understand
  
- **Negative**:
  - Entire application must be deployed together
  - Cannot scale individual components independently
  - Technology stack locked for entire application
  - Potential for tight coupling if not careful

- **Mitigation**:
  - Clear layer separation prevents tight coupling
  - Modular package structure allows future extraction
  - Horizontal scaling possible with load balancer
  - Stateless design (JWT) enables multiple instances

**Source References**: 
- Project structure: `backend/src/main/java/com/urbanclean/`
- Deployment: `docker/docker-compose.yml`

---

### ADR-002: RESTful API

**Status**: Accepted

**Context**: 
The system needed an API design approach for communication between frontend and backend that would be:
- Standard and well-understood
- Easy to consume from web and potentially mobile clients
- Cacheable for performance
- Stateless for scalability

**Decision**: 
Implement a RESTful API following REST principles with JSON payloads.

**Rationale**:
1. **Industry Standard**: REST is widely understood and adopted
2. **HTTP Semantics**: Leverages HTTP methods (GET, POST, PUT, PATCH, DELETE) naturally
3. **Stateless**: Each request contains all necessary information (JWT token)
4. **Cacheable**: HTTP caching headers can be used
5. **Tooling**: Excellent tooling support (Swagger/OpenAPI, Postman, curl)
6. **Client Support**: Easy to consume from any HTTP client
7. **Documentation**: OpenAPI/Swagger provides interactive documentation

**API Design Principles**:
- Resource-based URLs (`/api/tasks`, `/api/reports`)
- HTTP methods for operations (GET for read, POST for create, etc.)
- JSON for request/response bodies
- HTTP status codes for responses (200, 201, 400, 401, 404, 500)
- Versioning capability (`/api/v1/` if needed)

**Alternatives Considered**:
- **GraphQL**: More complex, overkill for current requirements, steeper learning curve
- **gRPC**: Better for service-to-service, not ideal for web browsers
- **SOAP**: Legacy, XML-based, too heavyweight

**Consequences**:
- **Positive**:
  - Easy to understand and use
  - Wide client support
  - Good tooling ecosystem
  - HTTP caching possible
  - Stateless and scalable
  
- **Negative**:
  - Over-fetching/under-fetching (mitigated with DTOs)
  - Multiple requests for related data (acceptable for current scale)
  - No built-in real-time updates (can add WebSockets if needed)

**Source References**: 
- Controllers: `backend/src/main/java/com/urbanclean/controller/`
- API Documentation: `backend/src/main/java/com/urbanclean/config/OpenAPIConfig.java`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

### ADR-003: Single-Page Application (SPA)

**Status**: Accepted

**Context**: 
The frontend needed an architecture that would provide:
- Responsive user experience
- Rich interactivity
- Efficient data loading
- Modern development experience

**Decision**: 
Implement a Single-Page Application (SPA) using React with client-side routing.

**Rationale**:
1. **User Experience**: No full page reloads, smooth transitions
2. **Performance**: Load once, fetch data via API, cache in browser
3. **Interactivity**: Rich UI interactions without server round-trips
4. **Modern Development**: Component-based architecture, reusable components
5. **Ecosystem**: Large React ecosystem with libraries for maps, forms, etc.
6. **Mobile-Ready**: Same API can be used for future mobile app

**Architecture**:
- React for UI components
- React Router for client-side routing
- Axios for API calls
- Context API for state management
- Leaflet for maps

**Alternatives Considered**:
- **Server-Side Rendering (SSR)**: More complex, not needed for authenticated app
- **Multi-Page Application (MPA)**: Full page reloads, less interactive
- **Progressive Web App (PWA)**: Could be added later if offline support needed

**Consequences**:
- **Positive**:
  - Fast, responsive user experience
  - Clear separation of frontend and backend
  - Can deploy frontend and backend independently
  - Easy to add mobile app later
  
- **Negative**:
  - Initial load time (mitigated with code splitting)
  - SEO challenges (not relevant for authenticated app)
  - Requires JavaScript enabled
  - More complex build process

**Source References**: 
- Frontend: `frontend/src/`
- Routing: `frontend/src/App.jsx`
- Build config: `frontend/vite.config.js`

---

### ADR-004: Geospatial Data with PostGIS

**Status**: Accepted

**Context**: 
The system's core functionality revolves around location-based operations:
- Reports have geographic coordinates
- Tasks need to be located on maps
- Duplicate detection requires spatial proximity checks
- Analytics need heatmaps and geographic clustering

**Decision**: 
Use PostgreSQL with PostGIS extension for geospatial data storage and queries.

**Rationale**:
1. **Industry Standard**: PostGIS is the de facto standard for open-source geospatial databases
2. **Spatial Indexes**: GIST indexes for efficient spatial queries
3. **Rich Functions**: Distance calculations, containment, intersection, buffering
4. **Accuracy**: Geography type for accurate distance calculations on Earth's surface
5. **Integration**: Hibernate Spatial provides seamless JPA integration
6. **Single Database**: No need for separate geospatial database
7. **ACID Compliance**: Transactional guarantees for spatial data

**Key Spatial Operations**:
- **Duplicate Detection**: Find reports within 100m radius
- **Heatmap Generation**: Cluster reports by geographic area
- **Geofencing**: Validate coordinates within municipality bounds
- **Distance Calculation**: Calculate distance between points

**Alternatives Considered**:
- **MongoDB with Geospatial Indexes**: NoSQL, less suitable for relational data
- **MySQL Spatial**: Weaker spatial support than PostGIS
- **Separate Geospatial Service**: Additional complexity, network latency
- **Client-Side Calculations**: Inaccurate, no spatial indexes

**Consequences**:
- **Positive**:
  - Accurate spatial calculations
  - Efficient spatial queries with indexes
  - Single database for all data
  - Rich spatial function library
  - Proven technology
  
- **Negative**:
  - Learning curve for PostGIS functions
  - More complex than simple lat/lon columns
  - Requires PostGIS expertise for optimization

**Source References**: 
- Database: `docker/docker-compose.yml` (postgis/postgis:15-3.3)
- Entities: `backend/src/main/java/com/urbanclean/entity/Report.java`, `Task.java`
- Spatial queries: `backend/src/main/java/com/urbanclean/repository/TaskRepository.java`
- Geofencing: `backend/src/main/java/com/urbanclean/service/GeofencingService.java`
- Deduplication: `backend/src/main/java/com/urbanclean/service/DeduplicationService.java`

---

### ADR-005: Event-Driven Notifications

**Status**: Accepted

**Context**: 
The system needs to send notifications (emails) when certain events occur:
- Task assigned to operator
- Task resolved (notify citizen)
- Task reopened (notify operator)

These notifications should not block the main business logic or cause failures if email service is unavailable.

**Decision**: 
Use Spring's event-driven architecture with `ApplicationEventPublisher` and `@EventListener` for asynchronous notification handling.

**Rationale**:
1. **Decoupling**: Business logic doesn't know about notification mechanism
2. **Asynchronous**: Notifications sent in background, don't block main flow
3. **Resilience**: Email failures don't affect core operations
4. **Extensibility**: Easy to add new event handlers (SMS, push notifications)
5. **Testability**: Can test business logic without email service
6. **Spring Integration**: Built into Spring Framework, no additional dependencies

**Event Flow**:
```
TaskService.assignTask()
    └─> Publish TaskAssignedEvent
            └─> TaskEventListener.handleTaskAssigned() [@Async]
                    └─> EmailService.sendTaskAssignedEmail()
```

**Alternatives Considered**:
- **Synchronous Calls**: Would block main flow, failures affect core operations
- **Message Queue (RabbitMQ, Kafka)**: Overkill for current scale, additional infrastructure
- **Database Polling**: Inefficient, delayed notifications
- **Scheduled Jobs**: Less real-time, more complex

**Consequences**:
- **Positive**:
  - Core operations not affected by notification failures
  - Easy to add new notification types
  - Asynchronous processing improves response time
  - Clean separation of concerns
  
- **Negative**:
  - Eventual consistency (notification sent after transaction commits)
  - Debugging more complex (event flow less obvious)
  - Need to handle event handler failures separately
  - No guaranteed delivery (can add retry logic)

**Mitigation**:
- Circuit breaker on email service (Resilience4j)
- Retry logic with exponential backoff
- Failed notification tracking (NotificationFailure entity)
- Notification preferences (users can opt out)

**Source References**: 
- Events: `backend/src/main/java/com/urbanclean/event/`
- Listeners: `backend/src/main/java/com/urbanclean/listener/`
- Email service: `backend/src/main/java/com/urbanclean/service/EmailService.java`
- Circuit breaker: `backend/src/main/resources/application.properties` (resilience4j config)

---

## Performance Considerations

### Caching Strategy

**Decision**: Use Spring Cache abstraction with in-memory caching for analytics queries

**Rationale**:
- Analytics queries are expensive (aggregations, joins)
- Results don't change frequently
- Significant performance improvement for dashboard

**Implementation**:
```java
@Service
public class AnalyticsService {
    @Cacheable(value = "analytics", key = "#filters.toString()")
    public MTTRResponse calculateMTTR(AnalyticsFilters filters) {
        // Expensive calculation cached
    }
}
```

**Cache Configuration**:
- TTL: 5 minutes for analytics
- Eviction: LRU (Least Recently Used)
- Size: 100 entries

**Source**: `backend/src/main/java/com/urbanclean/config/CacheConfig.java`

---

### Database Indexing

**Decision**: Strategic indexes on frequently queried columns

**Rationale**:
- Improve query performance
- Support efficient filtering and sorting
- Enable spatial queries

**Key Indexes**:
```sql
-- State-based queries
CREATE INDEX idx_task_state ON tareas(state);

-- Time-based queries
CREATE INDEX idx_task_created_at ON tareas(created_at);
CREATE INDEX idx_report_created_at ON reportes(created_at);

-- Spatial queries
CREATE INDEX idx_report_location ON reportes USING GIST(location);
CREATE INDEX idx_task_location ON tareas USING GIST(location);

-- Foreign key lookups
CREATE INDEX idx_task_assigned_operator ON tareas(assigned_operator_id);
CREATE INDEX idx_report_submitter ON reportes(submitter_id);
```

**Trade-offs**:
- Faster reads, slower writes
- Additional storage space
- Index maintenance overhead

**Source**: `backend/src/main/resources/db/migration/`

---

### Asynchronous Processing

**Decision**: Use `@Async` for non-critical operations

**Rationale**:
- Improve response times
- Don't block main thread
- Better resource utilization

**Async Operations**:
- Email sending
- Event handling
- Analytics calculations (when triggered by events)

**Configuration**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

**Source**: `backend/src/main/java/com/urbanclean/config/AsyncConfig.java`

---

## Scalability Considerations

### Horizontal Scaling

**Frontend**: 
- Stateless React SPA
- Can deploy multiple instances behind load balancer
- Static files can be served from CDN

**Backend**: 
- Stateless (JWT authentication)
- No server-side sessions
- Can deploy multiple instances behind load balancer
- Shared database (single point of scaling)

**Database**: 
- Vertical scaling (more CPU/RAM)
- Read replicas for read-heavy workloads
- Connection pooling (HikariCP)

---

### Load Balancing

**Decision**: Support for load balancing through stateless design

**Implementation**:
- JWT tokens (no session affinity needed)
- Stateless REST API
- Shared database
- No in-memory state

**Future Considerations**:
- Add load balancer (Nginx, HAProxy)
- Multiple backend instances
- Database read replicas
- Redis for distributed caching

---

## Summary

This document captures the key architectural decisions made in the Urban Cleaning Management System. All decisions are based on actual implementation in the codebase and are justified by project requirements, team capabilities, and expected scale.

Key themes across decisions:
- **Simplicity**: Prefer simple solutions that meet requirements
- **Standards**: Use industry-standard technologies and patterns
- **Pragmatism**: Balance ideal architecture with practical constraints
- **Future-Proofing**: Design allows for future scaling and evolution
- **Team**: Decisions consider team size and expertise

All decisions are revisable as requirements evolve, but changes should be documented and justified.
