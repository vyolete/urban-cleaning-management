---
inclusion: always
---

# Urban Cleaning Management System - Development Standards

## Project Structure

This project follows a monorepo structure with clear separation of concerns:

```
/
├── backend/          # Spring Boot application
├── frontend/         # React SPA
└── docker/           # Docker configuration files
```

## Backend Standards (Spring Boot + PostgreSQL)

### Architecture Principles

- **Clean Architecture**: Maintain clear separation between layers
  - Controllers (Presentation Layer)
  - Services (Application Layer)
  - Entities (Domain Layer)
  - Repositories (Data Layer)

- **Dependency Rule**: Dependencies should point inward (Controllers → Services → Repositories)

### Code Organization

```
backend/src/main/java/com/urbanclean/
├── config/              # Configuration classes
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── CorsConfig.java
├── controller/          # REST Controllers
│   ├── AuthController.java
│   ├── ReportController.java
│   ├── TaskController.java
│   └── ConfigController.java
├── service/             # Business logic
│   ├── AuthService.java
│   ├── ReportService.java
│   ├── TaskService.java
│   ├── PriorityCalculatorService.java
│   ├── DeduplicationService.java
│   ├── AuditService.java
│   └── ConfigService.java
├── repository/          # JPA Repositories
│   ├── UserRepository.java
│   ├── ReportRepository.java
│   ├── TaskRepository.java
│   ├── AuditLogRepository.java
│   └── AlgorithmConfigRepository.java
├── entity/              # JPA Entities
│   ├── User.java
│   ├── Report.java
│   ├── Task.java
│   ├── AuditLog.java
│   └── AlgorithmConfig.java
├── dto/                 # Data Transfer Objects
│   ├── request/
│   └── response/
├── security/            # Security components
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── exception/           # Custom exceptions
│   ├── GlobalExceptionHandler.java
│   └── custom/
└── util/                # Utility classes
```

### Entity Design Rules

1. **Use UUID for primary keys**: All entities should use `UUID` as primary key type
2. **Immutable audit fields**: Use `@Column(updatable = false)` for audit log fields
3. **PostGIS for coordinates**: Use `Point` type with `@Column(columnDefinition = "geometry(Point,4326)")`
4. **Enum for states**: Use `@Enumerated(EnumType.STRING)` for state fields
5. **Timestamps**: Use `LocalDateTime` for all timestamp fields

### Security Standards

#### Password Hashing
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

#### JWT Configuration
- Token expiration: 24 hours
- Include claims: username, role, userId
- Sign with HS512 algorithm
- Store secret in environment variables

#### Endpoint Security
```java
// Public endpoints
@PostMapping("/api/auth/login")
@PostMapping("/api/auth/register")

// Citizen endpoints
@PreAuthorize("hasAnyRole('CIUDADANO', 'TECNICO', 'ADMIN')")
@PostMapping("/api/reports")

// Operator endpoints
@PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
@GetMapping("/api/tasks")

// Admin endpoints
@PreAuthorize("hasRole('ADMIN')")
@PutMapping("/api/admin/config/algorithm-weights")
```

### Priority Calculator Service - CRITICAL

The priority calculation algorithm is the core of the system. Implementation must follow this exact formula:

```java
@Service
public class PriorityCalculatorService {
    
    public BigDecimal calculatePriority(Report report) {
        AlgorithmConfig config = getCurrentConfig();
        
        BigDecimal categoryComponent = config.getWeightCategory()
            .multiply(mapCategoryToValue(report.getCategory()));
        
        BigDecimal zoneComponent = config.getWeightZone()
            .multiply(calculateZoneRiskIndex(report.getLocation()));
        
        BigDecimal timeComponent = config.getWeightTime()
            .multiply(calculateHoursElapsed(report.getCreatedAt()));
        
        return categoryComponent.add(zoneComponent).add(timeComponent);
    }
}
```

**Trigger Points**:
- Calculate on report creation (`@PostPersist` or in service layer)
- Recalculate all pending tasks when weights change
- Consider scheduled job for time-based recalculation

### API Design Standards

#### REST Conventions
- Use proper HTTP verbs: GET, POST, PUT, PATCH, DELETE
- Use plural nouns for resources: `/api/reports`, `/api/tasks`
- Use sub-resources for relationships: `/api/tasks/{id}/audit-history`
- Version API if needed: `/api/v1/reports`

#### Request/Response Format
```java
// Always use DTOs, never expose entities directly
@PostMapping("/api/reports")
public ResponseEntity<ReportResponse> createReport(
    @Valid @RequestPart("data") ReportSubmissionRequest request,
    @RequestPart("photo") MultipartFile photo) {
    // Implementation
}
```

#### Status Codes
- 200 OK: Successful GET, PUT, PATCH
- 201 Created: Successful POST
- 204 No Content: Successful DELETE
- 400 Bad Request: Validation errors
- 401 Unauthorized: Authentication required
- 403 Forbidden: Insufficient permissions
- 404 Not Found: Resource not found
- 500 Internal Server Error: Server errors

#### Error Response Format
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Coordinates outside geofencing boundaries",
  "timestamp": "2026-02-08T10:30:00.000Z",
  "details": {
    "latitude": "40.7128",
    "longitude": "-74.0060"
  }
}
```

### Database Standards

#### PostGIS Setup
```sql
CREATE EXTENSION IF NOT EXISTS postgis;

-- Spatial index for location queries
CREATE INDEX idx_report_location ON reportes USING GIST(location);
CREATE INDEX idx_task_location ON tareas USING GIST(location);
```

#### Naming Conventions
- Tables: lowercase with underscores (snake_case)
- Columns: lowercase with underscores (snake_case)
- Foreign keys: `{referenced_table}_id`
- Indexes: `idx_{table}_{column}`

#### Migration Strategy
- Use Flyway or Liquibase for database migrations
- Version all schema changes
- Never modify existing migrations

### Testing Standards

#### Unit Tests
- Test class naming: `{ClassName}Test.java`
- Use JUnit 5 and AssertJ
- Mock dependencies with Mockito
- Aim for 80% line coverage

#### Property-Based Tests
- Use JUnit-QuickCheck
- Minimum 100 iterations per property
- Tag with feature and property number
- Focus on universal properties

```java
@Property
@Tag("Feature: urban-cleaning-management, Property 14: Priority score formula correctness")
public void priorityScoreMatchesFormula(
    @ForAll @InRange(min = "1", max = "10") int categoryValue,
    @ForAll @InRange(min = "1", max = "10") int zoneValue,
    @ForAll @InRange(min = "0", max = "168") int hoursElapsed) {
    // Test implementation
}
```

## Frontend Standards (React)

### Project Structure

```
frontend/src/
├── components/          # Reusable components
│   ├── common/         # Generic components
│   ├── citizen/        # Citizen-specific components
│   ├── operator/       # Operator-specific components
│   └── admin/          # Admin-specific components
├── pages/              # Page components
│   ├── LoginPage.jsx
│   ├── CitizenReportPage.jsx
│   ├── OperatorDashboard.jsx
│   └── AdminConfigPage.jsx
├── services/           # API service layer
│   ├── authService.js
│   ├── reportService.js
│   ├── taskService.js
│   └── configService.js
├── hooks/              # Custom React hooks
│   ├── useAuth.js
│   ├── useGeolocation.js
│   └── useTasks.js
├── context/            # React Context providers
│   └── AuthContext.jsx
├── utils/              # Utility functions
│   ├── api.js
│   └── validators.js
└── App.jsx             # Main application component
```

### Component Design Principles

1. **Functional Components**: Use function components with hooks
2. **Single Responsibility**: Each component should have one clear purpose
3. **Props Validation**: Use PropTypes or TypeScript
4. **Controlled Components**: Forms should use controlled inputs

### State Management

- Use React Context for global state (authentication, user info)
- Use local state for component-specific data
- Consider React Query for server state management

### API Integration

```javascript
// services/api.js
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add JWT token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
```

### Geolocation Usage

```javascript
// hooks/useGeolocation.js
import { useState, useEffect } from 'react';

export const useGeolocation = () => {
  const [location, setLocation] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported');
      setLoading(false);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLocation({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });
        setLoading(false);
      },
      (err) => {
        setError(err.message);
        setLoading(false);
      }
    );
  }, []);

  return { location, error, loading };
};
```

### Map Integration

Use Leaflet for map visualization:

```javascript
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';

const TaskMap = ({ tasks }) => {
  return (
    <MapContainer center={[40.7128, -74.0060]} zoom={13}>
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; OpenStreetMap contributors'
      />
      {tasks.map((task) => (
        <Marker
          key={task.id}
          position={[task.location.latitude, task.location.longitude]}
        >
          <Popup>
            <div>
              <h3>{task.category}</h3>
              <p>Priority: {task.priorityScore}</p>
              <p>State: {task.state}</p>
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
};
```

## Docker Configuration Standards

### Backend Dockerfile (Multi-stage Build)

```dockerfile
# Build stage
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile

```dockerfile
# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  postgres:
    image: postgis/postgis:15-3.3
    environment:
      POSTGRES_DB: urbanclean
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/urbanclean
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - postgres

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    environment:
      REACT_APP_API_URL: http://localhost:8080/api
    depends_on:
      - backend

volumes:
  postgres_data:
```

## Environment Variables

### Backend (.env)
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=urbanclean
DB_USER=urbanclean_user
DB_PASSWORD=secure_password
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRATION=86400000
UPLOAD_DIR=/uploads
MAX_FILE_SIZE=5242880
```

### Frontend (.env)
```
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_MAP_CENTER_LAT=40.7128
REACT_APP_MAP_CENTER_LON=-74.0060
```

## Git Workflow

- **Branch naming**: `feature/`, `bugfix/`, `hotfix/`
- **Commit messages**: Use conventional commits format
- **Pull requests**: Require code review before merge
- **CI/CD**: Run tests on every push

## Code Quality

- **Linting**: Use ESLint for JavaScript, Checkstyle for Java
- **Formatting**: Use Prettier for JavaScript, Google Java Format for Java
- **Code reviews**: Mandatory for all changes
- **Documentation**: JavaDoc for public APIs, JSDoc for complex functions
