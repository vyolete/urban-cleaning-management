# Test Resources

This directory contains resources and configuration for integration and unit tests.

## Architecture

The test infrastructure follows clean architecture principles with clear separation between:

- **Production Database**: `urbanclean` - Used by the running application
- **Test Database**: `urbanclean_test` - Isolated database for integration tests
- **Test Configuration**: `application-test.properties` - Test-specific settings

## Database Setup

### Quick Start

```bash
# Initialize test database
./init-test-db.sh

# Run integration tests
cd ../../..
mvn test -Dtest=EndToEndIntegrationTest
```

### Manual Setup

If you prefer manual setup:

```bash
# 1. Ensure PostgreSQL container is running
docker ps | grep urbanclean-postgres

# 2. Create test database
docker exec -it urbanclean-postgres psql -U urbanclean_user -d postgres \
  -c "CREATE DATABASE urbanclean_test OWNER urbanclean_user;"

# 3. Enable PostGIS
docker exec -it urbanclean-postgres psql -U urbanclean_user -d urbanclean_test \
  -c "CREATE EXTENSION IF NOT EXISTS postgis;"
```

## Test Configuration

### application-test.properties

The test profile uses:
- **Database**: `urbanclean_test` (isolated from production)
- **Schema Management**: Flyway migrations run automatically
- **Transaction Management**: `@Transactional` ensures test isolation
- **Data Cleanup**: Automatic rollback after each test

### Key Features

1. **Database Isolation**: Tests never touch production data
2. **Automatic Schema**: Flyway creates tables from migrations
3. **Transaction Rollback**: Each test starts with clean state
4. **PostGIS Support**: Full spatial query capabilities

## Test Types

### Integration Tests

Located in `src/test/java/com/urbanclean/integration/`

- **EndToEndIntegrationTest**: Complete user flows (citizen, operator, admin)
- **TokenRefreshIntegrationTest**: Token rotation and refresh flows
- **SessionManagementIntegrationTest**: Multi-device session management
- **ConfigurationIntegrationTest**: Dynamic configuration management
- **ActuatorEndpointsTest**: Monitoring and metrics endpoints
- **CircuitBreakerTest**: Resilience patterns
- **PerformanceMetricsEndpointTest**: Performance monitoring

### Unit Tests

Located in `src/test/java/com/urbanclean/service/`

- Service layer tests with mocked dependencies
- Repository tests with in-memory database
- Property-based tests for correctness properties

## Running Tests

### All Tests

```bash
mvn test
```

### Specific Test Class

```bash
mvn test -Dtest=EndToEndIntegrationTest
```

### Specific Test Method

```bash
mvn test -Dtest=EndToEndIntegrationTest#testCompleteCitizenFlow
```

### With Coverage

```bash
mvn test jacoco:report
# View report at: target/site/jacoco/index.html
```

## Troubleshooting

### Database Connection Issues

```bash
# Check if container is running
docker ps | grep urbanclean-postgres

# Check database exists
docker exec -it urbanclean-postgres psql -U urbanclean_user -d postgres \
  -c "SELECT datname FROM pg_database WHERE datname = 'urbanclean_test';"

# Recreate database
./init-test-db.sh
```

### Migration Issues

```bash
# Check Flyway migration status
mvn flyway:info -Dflyway.url=jdbc:postgresql://localhost:5432/urbanclean_test

# Clean and recreate (WARNING: Deletes all data)
mvn flyway:clean flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/urbanclean_test
```

### Test Failures

1. **Check logs**: Look for stack traces in test output
2. **Verify database**: Ensure `urbanclean_test` exists and has PostGIS
3. **Check dependencies**: Run `mvn clean install` to rebuild
4. **Isolate test**: Run single test to identify specific issue

## Best Practices

### Writing Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // Automatic rollback after each test
public class MyIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @BeforeEach
    public void setup() {
        // Clean up before each test
        repository.deleteAll();
    }
    
    @Test
    public void testSomething() {
        // Test implementation
    }
}
```

### Test Data Management

- Use `@BeforeEach` for test-specific setup
- Use `@Transactional` for automatic cleanup
- Create minimal test data (only what's needed)
- Use builders for complex entities

### Assertions

```java
// Use AssertJ for readable assertions
assertThat(result).isNotNull();
assertThat(result.getStatus()).isEqualTo(Status.ACTIVE);
assertThat(result.getItems()).hasSize(3);
```

## Maintenance

### Database Cleanup

The test database is automatically cleaned between tests via `@Transactional`. However, you can manually clean it:

```bash
# Drop and recreate
./init-test-db.sh

# Or manually
docker exec -it urbanclean-postgres psql -U urbanclean_user -d postgres \
  -c "DROP DATABASE IF EXISTS urbanclean_test;"
docker exec -it urbanclean-postgres psql -U urbanclean_user -d postgres \
  -c "CREATE DATABASE urbanclean_test OWNER urbanclean_user;"
```

### Updating Test Configuration

When adding new test properties:

1. Add to `application-test.properties`
2. Document in this README
3. Update `init-test-db.sh` if database changes needed

## CI/CD Integration

For continuous integration pipelines:

```yaml
# Example GitHub Actions workflow
- name: Setup Test Database
  run: |
    docker-compose up -d postgres
    ./backend/src/test/resources/init-test-db.sh

- name: Run Tests
  run: mvn test
```

## References

- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Flyway Migrations](https://flywaydb.org/documentation/)
- [PostGIS Documentation](https://postgis.net/documentation/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
