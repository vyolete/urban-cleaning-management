# Urban Cleaning Management - Backend

Spring Boot backend service for the Urban Cleaning Management System.

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 15+ with PostGIS extension

## Running Locally

1. **Set up environment variables**
   ```bash
   export DB_HOST=localhost
   export DB_PORT=5432
   export DB_NAME=urbanclean
   export DB_USER=urbanclean_user
   export DB_PASSWORD=your_password
   export JWT_SECRET=your_secret_key
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The API will be available at `http://localhost:8080`

## Running Tests

```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest="**/*Test"

# Run only property-based tests
mvn test -Dtest="**/*PropertyTest"
```

## API Documentation

Once running, the API endpoints are available at:
- Authentication: `POST /api/auth/login`, `POST /api/auth/register`
- Reports: `POST /api/reports`, `GET /api/reports/{id}`
- Tasks: `GET /api/tasks`, `PATCH /api/tasks/{id}/state`
- Config: `GET /api/admin/config/algorithm-weights` (Admin only)

## Project Structure

```
src/main/java/com/urbanclean/
├── config/              # Configuration classes
├── controller/          # REST Controllers
├── service/             # Business logic
├── repository/          # JPA Repositories
├── entity/              # JPA Entities
├── dto/                 # Data Transfer Objects
├── security/            # Security components
├── exception/           # Custom exceptions
└── util/                # Utility classes
```
