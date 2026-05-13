# Design Document: Multi-Country Support

## Overview

This document describes the design for adding multi-country support to the Urban Cleaning Management System (Urbix). The system currently only supports Madrid, Spain with hardcoded geofencing boundaries. This feature will enable the system to support multiple countries (Colombia, Spain, and others) with configurable geofencing boundaries, country-specific filtering, and administrative configuration capabilities.

## Architecture Overview

### Current Architecture
- Backend: Spring Boot (Java)
- Frontend: React
- Database: PostgreSQL/PostGIS
- Deployment: Docker

### New Components
- **Country Entity**: Stores country configuration including geofencing boundaries
- **GeofencingService**: Enhanced to support multiple countries with dynamic boundary loading
- **CountryRepository**: JPA repository for country management
- **CountryController**: REST API for country management
- **CountryService**: Business logic for country management
- **Frontend CountrySelector**: Component for country selection in report form
- **Frontend MapCentering**: Enhanced map component with country-based centering

## Database Schema

### New Tables

#### countries
```sql
CREATE TABLE countries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(3) NOT NULL UNIQUE,  -- ISO 3166-1 alpha-3 code
    default_country BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    -- Geofencing boundaries
    min_lat DECIMAL(10, 8) NOT NULL,
    max_lat DECIMAL(10, 8) NOT NULL,
    min_lon DECIMAL(11, 8) NOT NULL,
    max_lon DECIMAL(11, 8) NOT NULL,
    -- Administrative divisions
    administrative_area VARCHAR(100),
    municipality VARCHAR(100),
    -- Geographic center for map centering
    center_lat DECIMAL(10, 8),
    center_lon DECIMAL(11, 8),
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_countries_default ON countries(default_country);
CREATE INDEX idx_countries_enabled ON countries(enabled);
CREATE INDEX idx_countries_code ON countries(code);
```

#### report_countries (Migration table for backward compatibility)
```sql
-- Add country_id column to existing reportes table
ALTER TABLE reportes ADD COLUMN country_id UUID REFERENCES countries(id);

-- Index for country-based filtering
CREATE INDEX idx_report_country ON reportes(country_id);
```

### Migration Strategy

1. **Initial Migration**: Create countries table and insert default Spain/Madrid configuration
2. **Data Migration**: Update existing reports to use default country (Spain)
3. **Schema Update**: Add country_id column to reportes table with foreign key constraint

## Backend Implementation

### 1. Entity Layer

#### Country.java
```java
@Entity
@Table(name = "countries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(nullable = false, unique = true, length = 3)
    private String code;  // ISO 3166-1 alpha-3 code
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean defaultCountry = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
    
    // Geofencing boundaries
    @Column(nullable = false)
    private BigDecimal minLat;
    
    @Column(nullable = false)
    private BigDecimal maxLat;
    
    @Column(nullable = false)
    private BigDecimal minLon;
    
    @Column(nullable = false)
    private BigDecimal maxLon;
    
    // Administrative divisions
    @Column(length = 100)
    private String administrativeArea;
    
    @Column(length = 100)
    private String municipality;
    
    // Geographic center
    @Column
    private BigDecimal centerLat;
    
    @Column
    private BigDecimal centerLon;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### 2. Repository Layer

#### CountryRepository.java
```java
@Repository
public interface CountryRepository extends JpaRepository<Country, UUID> {
    Optional<Country> findByDefaultCountryTrue();
    List<Country> findByEnabledTrue();
    Optional<Country> findByCode(String code);
    Optional<Country> findByName(String name);
}
```

### 3. Service Layer

#### CountryService.java
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CountryService {
    private final CountryRepository countryRepository;
    private final ReportRepository reportRepository;
    
    // CRUD operations
    public Country createCountry(Country country) { ... }
    public Country updateCountry(UUID id, Country country) { ... }
    public void deleteCountry(UUID id) { ... }
    public Country getCountryById(UUID id) { ... }
    public List<Country> getAllCountries() { ... }
    public List<Country> getEnabledCountries() { ... }
    
    // Default country operations
    public Country getDefaultCountry() { ... }
    public void setDefaultCountry(UUID id) { ... }
    
    // Validation
    public void validateGeofencingBoundaries(BigDecimal minLat, BigDecimal maxLat, 
                                            BigDecimal minLon, BigDecimal maxLon) { ... }
    
    // Migration
    @Transactional
    public void migrateExistingReportsToDefaultCountry() { ... }
}
```

#### Enhanced GeofencingService.java
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GeofencingService {
    private final CountryRepository countryRepository;
    
    // Validate coordinates against specific country
    public void validateCoordinates(Double latitude, Double longitude, UUID countryId) { ... }
    
    // Validate coordinates against default country
    public void validateCoordinates(Double latitude, Double longitude) { ... }
    
    // Check if coordinates are within country boundaries
    public boolean isWithinBoundaries(Double latitude, Double longitude, UUID countryId) { ... }
    
    // Get country boundary polygon
    public Polygon getBoundaryPolygon(UUID countryId) { ... }
    
    // Get country by ID
    public Country getCountryById(UUID countryId) { ... }
}
```

### 4. Controller Layer

#### CountryController.java
```java
@RestController
@RequestMapping("/api/admin/countries")
@RequiredArgsConstructor
@Slf4j
public class CountryController {
    private final CountryService countryService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CountryResponse>> getAllCountries() { ... }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> createCountry(@Valid @RequestBody CountryRequest request) { ... }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> getCountry(@PathVariable UUID id) { ... }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> updateCountry(@PathVariable UUID id, 
                                                        @Valid @RequestBody CountryRequest request) { ... }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCountry(@PathVariable UUID id) { ... }
    
    @GetMapping("/default")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECNICO', 'CIUDADANO')")
    public ResponseEntity<CountryResponse> getDefaultCountry() { ... }
}
```

### 5. DTOs

#### CountryRequest.java
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    @Size(min = 3, max = 3)
    private String code;
    
    @NotNull
    private BigDecimal minLat;
    
    @NotNull
    private BigDecimal maxLat;
    
    @NotNull
    private BigDecimal minLon;
    
    @NotNull
    private BigDecimal maxLon;
    
    private String administrativeArea;
    private String municipality;
    private BigDecimal centerLat;
    private BigDecimal centerLon;
}
```

#### CountryResponse.java
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryResponse {
    private UUID id;
    private String name;
    private String code;
    private Boolean defaultCountry;
    private Boolean enabled;
    private BigDecimal minLat;
    private BigDecimal maxLat;
    private BigDecimal minLon;
    private BigDecimal maxLon;
    private String administrativeArea;
    private String municipality;
    private BigDecimal centerLat;
    private BigDecimal centerLon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 6. Enhanced ReportService

#### Updated createReport method
```java
@Transactional
public Report createReport(ReportSubmissionRequest request, MultipartFile photo) {
    // Validate required fields
    validateReportRequest(request);
    
    // Get country ID from request (new field)
    UUID countryId = request.getCountryId();
    
    // Validate coordinates using geofencing service with country context
    geofencingService.validateCoordinates(request.getLatitude(), 
                                         request.getLongitude(), 
                                         countryId);
    
    // ... rest of the method
}
```

#### Updated ReportSubmissionRequest
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSubmissionRequest {
    @NotNull
    private Double latitude;
    
    @NotNull
    private Double longitude;
    
    @NotBlank
    private String category;
    
    @NotBlank
    private String description;
    
    // New field for country context
    private UUID countryId;
}
```

### 7. Enhanced ReportController

#### Updated getAllReports method
```java
@GetMapping
@PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
public ResponseEntity<List<ReportResponse>> getAllReports(
        @RequestParam(required = false) UUID countryId,
        @RequestParam(required = false) String administrativeArea,
        @RequestParam(required = false) String municipality) {
    log.info("Get all reports request: countryId={}, administrativeArea={}, municipality={}", 
             countryId, administrativeArea, municipality);
    
    List<ReportResponse> reports = reportService.getAllReports(countryId, 
                                                                administrativeArea, 
                                                                municipality);
    return ResponseEntity.ok(reports);
}
```

### 8. Enhanced ReportService Filtering

#### Updated getAllReports method
```java
@Transactional(readOnly = true)
public List<ReportResponse> getAllReports(UUID countryId, 
                                         String administrativeArea, 
                                         String municipality) {
    List<Report> reports = reportRepository.findAll();
    
    // Apply filters
    if (countryId != null) {
        reports = reports.stream()
                .filter(r -> r.getCountryId().equals(countryId))
                .collect(Collectors.toList());
    }
    
    if (administrativeArea != null) {
        // Get country for administrative area
        Country country = countryRepository.findByAdministrativeArea(administrativeArea)
                .orElseThrow(() -> new ResourceNotFoundException("Administrative area not found"));
        
        reports = reports.stream()
                .filter(r -> r.getCountryId().equals(country.getId()))
                .collect(Collectors.toList());
    }
    
    if (municipality != null) {
        // Get country for municipality
        Country country = countryRepository.findByMunicipality(municipality)
                .orElseThrow(() -> new ResourceNotFoundException("Municipality not found"));
        
        reports = reports.stream()
                .filter(r -> r.getCountryId().equals(country.getId()))
                .collect(Collectors.toList());
    }
    
    return reports.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}
```

### 9. Enhanced ReportRepository

#### Updated heatmap query
```java
@Query(value = "SELECT " +
       "ST_Y(ST_Centroid(grid)) as latitude, " +
       "ST_X(ST_Centroid(grid)) as longitude, " +
       "COUNT(*) as intensity " +
       "FROM ( " +
       "    SELECT ST_SnapToGrid(location, :cellSize) as grid, country_id " +
       "    FROM reportes " +
       "    WHERE (:countryId IS NULL OR country_id = :countryId) " +
       "    AND created_at BETWEEN :startDate AND :endDate " +
       "    AND (:category IS NULL OR category = :category) " +
       ") grouped " +
       "GROUP BY grid, country_id " +
       "ORDER BY intensity DESC " +
       "LIMIT 1000",
       nativeQuery = true)
List<Object[]> getHeatmapDataByCountry(
    @Param("countryId") UUID countryId,
    @Param("cellSize") double cellSize,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate,
    @Param("category") String category
);
```

### 10. Enhanced HeatmapService

#### Updated getHeatmapData method
```java
public List<HeatmapCell> getHeatmapData(UUID countryId, 
                                       double cellSize, 
                                       LocalDateTime startDate, 
                                       LocalDateTime endDate, 
                                       String category) {
    List<Object[]> results = reportRepository.getHeatmapDataByCountry(
        countryId, cellSize, startDate, endDate, category);
    
    return results.stream()
            .map(row -> HeatmapCell.builder()
                    .latitude(((Number) row[0]).doubleValue())
                    .longitude(((Number) row[1]).doubleValue())
                    .intensity(((Number) row[2]).intValue())
                    .build())
            .collect(Collectors.toList());
}
```

### 11. HTTPS Configuration

#### SSL Configuration Properties
```properties
# SSL Configuration
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=${SSL_KEYSTORE_PATH:classpath:keystore.p12}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=${SSL_KEY_ALIAS:tomcat}
```

#### SSL Configuration Class
```java
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class SSLConfiguration {
    
    @Value("${server.ssl.key-store}")
    private String keyStorePath;
    
    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;
    
    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;
    
    @Value("${server.ssl.key-alias}")
    private String keyAlias;
    
    @Bean
    public TomcatServletWebServerFactory servletContainerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addAdditionalTomcatConnectors(redirectConnector());
        return factory;
    }
    
    private Connector redirectConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

### 12. CORS Configuration

#### CORS Configuration Class
```java
@Configuration
@EnableWebMvc
public class CORSConfiguration {
    
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins.stream()
                                .map(origin -> origin.startsWith("https") ? origin : null)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                                .toArray(String[]::new))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
```

## Frontend Implementation

### 1. Country Selector Component

#### CountrySelector.jsx
```jsx
function CountrySelector({ 
    selectedCountryId, 
    onSelectCountry, 
    disabled = false 
}) {
    const [countries, setCountries] = useState([]);
    const [loading, setLoading] = useState(true);
    
    useEffect(() => {
        loadCountries();
    }, []);
    
    const loadCountries = async () => {
        try {
            const response = await api.get('/admin/countries');
            setCountries(response.data.filter(c => c.enabled));
            setLoading(false);
        } catch (error) {
            console.error('Error loading countries:', error);
            setLoading(false);
        }
    };
    
    const handleChange = (e) => {
        const countryId = e.target.value ? UUID.parse(e.target.value) : null;
        onSelectCountry(countryId);
    };
    
    if (loading) {
        return <div>Cargando países...</div>;
    }
    
    if (countries.length === 0) {
        return <div>No hay países configurados</div>;
    }
    
    // Disable selector if only one country is enabled
    const isDisabled = disabled || countries.length === 1;
    
    return (
        <div className="country-selector">
            <label htmlFor="country">País *</label>
            <select
                id="country"
                name="country"
                value={selectedCountryId || ''}
                onChange={handleChange}
                disabled={isDisabled}
                required
            >
                <option value="">Seleccione un país</option>
                {countries.map(country => (
                    <option key={country.id} value={country.id}>
                        {country.name} {country.defaultCountry && '(Predeterminado)'}
                    </option>
                ))}
            </select>
        </div>
    );
}
```

### 2. Enhanced ReportForm

#### Updated ReportForm.jsx
```jsx
function ReportForm({ location: externalLocation, onSuccess, onError }) {
    const [formData, setFormData] = useState({
        category: '',
        description: '',
        countryId: null,  // New field
    });
    
    // ... rest of the component
    
    const handleSubmit = async (e) => {
        e.preventDefault();
        
        const reportData = {
            category: formData.category,
            description: formData.description,
            latitude: useManualLocation ? parseFloat(manualLocation.latitude) : location?.latitude,
            longitude: useManualLocation ? parseFloat(manualLocation.longitude) : location?.longitude,
            countryId: formData.countryId,  // Include country ID
        };
        
        // ... rest of submission logic
    };
    
    return (
        <div className="report-form">
            {/* Country Selector */}
            <div className="form-section">
                <CountrySelector
                    selectedCountryId={formData.countryId}
                    onSelectCountry={(countryId) => 
                        setFormData(prev => ({ ...prev, countryId }))}
                />
            </div>
            
            {/* Location Section */}
            <div className="form-section">
                <h3>Ubicación</h3>
                {/* ... rest of location section */}
            </div>
            
            {/* ... rest of form */}
        </div>
    );
}
```

### 3. Enhanced MapView

#### Updated MapView.jsx
```jsx
function MapView({ 
    location, 
    showGeofence = false, 
    height = '400px', 
    zoom = 15,
    countryId = null  // New prop
}) {
    const [defaultCenter, setDefaultCenter] = useState([
        parseFloat(import.meta.env.VITE_MAP_CENTER_LAT) || 40.416775,
        parseFloat(import.meta.env.VITE_MAP_CENTER_LON) || -3.703790,
    ]);
    
    const [countryBoundary, setCountryBoundary] = useState(null);
    
    useEffect(() => {
        if (countryId) {
            loadCountryBoundary(countryId);
        }
    }, [countryId]);
    
    const loadCountryBoundary = async (countryId) => {
        try {
            const response = await api.get(`/admin/countries/${countryId}`);
            const country = response.data;
            
            setDefaultCenter([country.centerLat, country.centerLon]);
            setCountryBoundary([
                [country.minLat, country.minLon],
                [country.maxLat, country.maxLon],
            ]);
        } catch (error) {
            console.error('Error loading country boundary:', error);
        }
    };
    
    // ... rest of component
    
    return (
        <div className="map-view">
            {/* ... map container */}
            
            {countryBoundary && showGeofence && (
                <L.rectangle(countryBoundary, {
                    color: '#3388ff',
                    weight: 2,
                    fillOpacity: 0.1,
                }).addTo(mapInstanceRef.current)}
            )}
            
            {/* ... rest of component */}
        </div>
    );
}
```

### 4. API Service Updates

#### Updated api.js
```javascript
// Add country selector API call
async function getCountries() {
    try {
        const response = await api.get('/admin/countries');
        return response.data;
    } catch (error) {
        throw error;
    }
}

// Add country by ID API call
async function getCountryById(id) {
    try {
        const response = await api.get(`/admin/countries/${id}`);
        return response.data;
    } catch (error) {
        throw error;
    }
}

// Add default country API call
async function getDefaultCountry() {
    try {
        const response = await api.get('/admin/countries/default');
        return response.data;
    } catch (error) {
        throw error;
    }
}

export default {
    // ... existing methods
    getCountries,
    getCountryById,
    getDefaultCountry,
};
```

## Deployment

### Docker Configuration

#### Updated docker-compose.yml
```yaml
version: '3.8'

services:
  # PostgreSQL with PostGIS extension
  postgres:
    image: postgis/postgis:15-3.3
    container_name: urbanclean-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-urbanclean}
      POSTGRES_USER: ${DB_USER:-urbanclean_user}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-password}
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=en_US.UTF-8"
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    networks:
      - urbanclean-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-urbanclean_user} -d ${DB_NAME:-urbanclean}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # Spring Boot Backend with HTTPS
  backend:
    build:
      context: ../backend
      dockerfile: Dockerfile
    container_name: urbanclean-backend
    restart: unless-stopped
    ports:
      - "${BACKEND_PORT:-8443}:8443"  # HTTPS port
    environment:
      # Database configuration
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-urbanclean}
      SPRING_DATASOURCE_USERNAME: ${DB_USER:-urbanclean_user}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-password}
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_SHOW_SQL: ${SHOW_SQL:-false}
      
      # JWT configuration
      JWT_SECRET: ${JWT_SECRET:-9CbirXCzuIemM8OmZVIiRVzHeLTkoRXqkeUCGRqh+6MzjzrXEMvt9A4F1cVANppaMmhgDLSx4NDVFhP/i5l/dQ==}
      JWT_EXPIRATION: ${JWT_EXPIRATION:-86400000}
      
      # File upload configuration
      UPLOAD_DIR: /uploads
      MAX_FILE_SIZE: ${MAX_FILE_SIZE:-5242880}
      
      # Geofencing configuration (default country)
      GEOFENCE_MIN_LAT: ${GEOFENCE_MIN_LAT:-40.3}
      GEOFENCE_MAX_LAT: ${GEOFENCE_MAX_LAT:-40.6}
      GEOFENCE_MIN_LON: ${GEOFENCE_MIN_LON:--3.9}
      GEOFENCE_MAX_LON: ${GEOFENCE_MAX_LON:--3.5}
      
      # Algorithm default weights
      ALGORITHM_WEIGHT_CATEGORY: ${ALGORITHM_WEIGHT_CATEGORY:-0.40}
      ALGORITHM_WEIGHT_ZONE: ${ALGORITHM_WEIGHT_ZONE:-0.35}
      ALGORITHM_WEIGHT_TIME: ${ALGORITHM_WEIGHT_TIME:-0.25}
      
      # Deduplication configuration
      DEDUPLICATION_DISTANCE_METERS: ${DEDUPLICATION_DISTANCE_METERS:-50.0}
      DEDUPLICATION_TIME_WINDOW_HOURS: ${DEDUPLICATION_TIME_WINDOW_HOURS:-24}
      
      # HTTPS configuration
      SERVER_SSL_ENABLED: ${SERVER_SSL_ENABLED:-true}
      SERVER_SSL_KEY_STORE: ${SERVER_SSL_KEY_STORE:/etc/ssl/certs/keystore.p12}
      SERVER_SSL_KEY_STORE_PASSWORD: ${SERVER_SSL_KEY_STORE_PASSWORD:changeit}
      SERVER_SSL_KEY_STORE_TYPE: ${SERVER_SSL_KEY_STORE_TYPE:PKCS12}
      SERVER_SSL_KEY_ALIAS: ${SERVER_SSL_KEY_ALIAS:tomcat}
      
      # CORS configuration
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-https://localhost:3000}
      
      # Spring Boot Actuator
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,info,metrics
      MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS: when_authorized
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - urbanclean-network
    volumes:
      - backend_uploads:/uploads
      - ${SSL_KEYSTORE_PATH:-./certs/keystore.p12}:/etc/ssl/certs/keystore.p12:ro
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "https://localhost:8443/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # React Frontend with Nginx
  frontend:
    build:
      context: ../frontend
      dockerfile: Dockerfile
      args:
        - VITE_API_URL=${VITE_API_URL:-/api}
    container_name: urbanclean-frontend
    restart: unless-stopped
    ports:
      - "${FRONTEND_PORT:-3000}:80"
    environment:
      VITE_API_URL: ${VITE_API_URL:-/api}
      VITE_MAP_CENTER_LAT: ${VITE_MAP_CENTER_LAT:-40.4168}
      VITE_MAP_CENTER_LON: ${VITE_MAP_CENTER_LON:--3.7038}
      VITE_MAP_ZOOM: ${VITE_MAP_ZOOM:-13}
    depends_on:
      backend:
        condition: service_healthy
    networks:
      - urbanclean-network
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://127.0.0.1:80/health || exit 1"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 10s

volumes:
  postgres_data:
    driver: local
  backend_uploads:
    driver: local

networks:
  urbanclean-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

### HTTPS Certificate Setup

#### Generate SSL Certificate
```bash
# Generate self-signed certificate for development
keytool -genkey -alias tomcat -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 365 -storetype PKCS12

# For production, obtain certificate from Let's Encrypt or other CA
# Example using certbot:
certbot certonly --standalone -d yourdomain.com
```

#### Certificate Mounting
```yaml
volumes:
  - ./certs/keystore.p12:/etc/ssl/certs/keystore.p12:ro
```

## Migration Script

### Database Migration Script

```sql
-- ============================================================================
-- Multi-Country Support Migration Script
-- ============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create countries table
CREATE TABLE countries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(3) NOT NULL UNIQUE,
    default_country BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    min_lat DECIMAL(10, 8) NOT NULL,
    max_lat DECIMAL(10, 8) NOT NULL,
    min_lon DECIMAL(11, 8) NOT NULL,
    max_lon DECIMAL(11, 8) NOT NULL,
    administrative_area VARCHAR(100),
    municipality VARCHAR(100),
    center_lat DECIMAL(10, 8),
    center_lon DECIMAL(11, 8),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX idx_countries_default ON countries(default_country);
CREATE INDEX idx_countries_enabled ON countries(enabled);
CREATE INDEX idx_countries_code ON countries(code);

-- Insert default country (Spain/Madrid)
INSERT INTO countries (
    id, name, code, default_country, enabled,
    min_lat, max_lat, min_lon, max_lon,
    administrative_area, municipality,
    center_lat, center_lon
) VALUES (
    uuid_generate_v4(),
    'España',
    'ESP',
    TRUE,
    TRUE,
    36.0, 43.0, -9.4, 3.7,
    'Comunidad de Madrid',
    'Madrid',
    40.4168,
    -3.7038
) ON CONFLICT (code) DO NOTHING;

-- Add country_id column to reportes table
ALTER TABLE reportes ADD COLUMN country_id UUID REFERENCES countries(id);

-- Create index for country-based filtering
CREATE INDEX idx_report_country ON reportes(country_id);

-- Migrate existing reports to default country
UPDATE reportes 
SET country_id = (SELECT id FROM countries WHERE default_country = TRUE)
WHERE country_id IS NULL;

-- Make country_id NOT NULL after migration
ALTER TABLE reportes ALTER COLUMN country_id SET NOT NULL;

-- ============================================================================
-- Verification Queries
-- ============================================================================

-- Verify countries table
SELECT * FROM countries;

-- Verify report migration
SELECT COUNT(*) as total_reports, 
       COUNT(country_id) as reports_with_country,
       (SELECT name FROM countries WHERE id = (SELECT country_id FROM reportes LIMIT 1)) as default_country
FROM reportes;

-- ============================================================================
-- Rollback Script (if needed)
-- ============================================================================

-- DROP INDEX idx_report_country;
-- ALTER TABLE reportes DROP COLUMN country_id;
-- DROP TABLE countries;
-- DROP INDEX idx_countries_default;
-- DROP INDEX idx_countries_enabled;
-- DROP INDEX idx_countries_code;
```

## Testing Strategy

### Unit Tests

1. **CountryService Tests**
   - Test CRUD operations
   - Test default country management
   - Test geofencing boundary validation

2. **GeofencingService Tests**
   - Test coordinate validation for each country
   - Test boundary checking
   - Test polygon creation

3. **ReportService Tests**
   - Test report creation with country context
   - Test filtering by country
   - Test filtering by administrative area
   - Test filtering by municipality

### Integration Tests

1. **API Tests**
   - Test country management endpoints
   - Test report submission with country
   - Test report filtering by country
   - Test heatmap generation by country

2. **Database Tests**
   - Test migration script
   - Test backward compatibility
   - Test data isolation

### Property-Based Tests

1. **Country Configuration**
   - Test that all countries have valid geofencing boundaries
   - Test that default country is unique
   - Test that enabled countries can be queried

2. **Geofencing Validation**
   - Test that coordinates within boundaries pass validation
   - Test that coordinates outside boundaries fail validation
   - Test that validation works for all configured countries

3. **Report Filtering**
   - Test that filtering by country returns only reports from that country
   - Test that filtering by administrative area includes country filtering
   - Test that filtering by municipality includes country and administrative area filtering

4. **Data Isolation**
   - Test that reports from one country cannot be accessed by filtering for another country
   - Test that operators only see reports from their assigned country

## Security Considerations

1. **Authorization**
   - Only ADMIN role can manage countries
   - All country management endpoints require ROLE_ADMIN

2. **Input Validation**
   - Validate geofencing boundaries (min < max)
   - Validate geographic coordinates (lat: -90 to 90, lon: -180 to 180)
   - Validate country code format (ISO 3166-1 alpha-3)

3. **Data Isolation**
   - Enforce country-based data isolation at database query level
   - Prevent reports from being associated with wrong country

## Performance Considerations

1. **Indexes**
   - Create indexes on country_id in reportes table
   - Create indexes on default_country and enabled in countries table

2. **Caching**
   - Consider caching country boundaries for performance
   - Invalidate cache on country updates

3. **Query Optimization**
   - Use PostGIS spatial indexes for geofencing validation
   - Optimize heatmap queries with proper indexing

## Future Enhancements

1. **Complex Geofencing**
   - Support polygon boundaries instead of just rectangles
   - Support multiple geofencing areas per country

2. **Administrative Divisions**
   - Support hierarchical administrative divisions (country > state > city)
   - Support multiple administrative areas per country

3. **User Country Assignment**
   - Allow operators to be assigned to specific countries
   - Filter reports by operator's assigned country

4. **Multi-Language Support**
   - Support country names in multiple languages
   - Support localized administrative division names

## Deployment Checklist

- [ ] Run database migration script
- [ ] Configure SSL certificates
- [ ] Update docker-compose.yml with HTTPS configuration
- [ ] Update frontend environment variables
- [ ] Test country management API
- [ ] Test report submission with country
- [ ] Test report filtering by country
- [ ] Test heatmap generation by country
- [ ] Test backward compatibility with existing data
- [ ] Test data isolation between countries
- [ ] Update documentation
- [ ] Deploy to production