# Requirements Document: Multi-Country Support

## Introduction

This document specifies the requirements for adding multi-country support to the Urban Cleaning Management System (Urbix). Currently, the system only supports Madrid, Spain with hardcoded geofencing boundaries. This feature will enable the system to support multiple countries (Colombia, Spain, and others) with configurable geofencing boundaries, country-specific filtering, and administrative configuration capabilities.

The feature is designed for a TFM presentation/demo, focusing on practical implementation that demonstrates the system's capabilities while maintaining the existing architecture (Spring Boot backend, React frontend, PostgreSQL/PostGIS).

## Glossary

- **System**: The Urban Cleaning Management WebApp (Urbix)
- **Country**: A sovereign nation that can be configured in the system (e.g., Spain, Colombia)
- **Geofencing_Boundary**: Geographic boundary (min_lat, max_lat, min_lon, max_lon) defining valid reporting area for a country
- **Administrative_Area**: Sub-national division (state, province, department, region) within a country
- **Municipality**: Local administrative division (city, municipality) within an administrative area
- **Report**: A citizen-submitted incident containing location, category, description, and photo
- **Task**: A work item created from one or more reports, assigned to operators
- **Geofencing_Service**: Backend service responsible for validating coordinates against country boundaries
- **Configurable_Geofencing**: Ability to define and modify geofencing boundaries per country through admin interface
- **HTTPS**: Secure HTTP protocol using SSL/TLS encryption
- **CORS**: Cross-Origin Resource Sharing configuration for web security
- **Docker**: Containerization platform for application deployment

## Requirements

### Requirement 1: Country Configuration and Selection

**User Story:** As a citizen, I want to select my country when submitting a report, so that my report is associated with the correct geographic boundaries.

#### Acceptance Criteria

1. WHEN a citizen accesses the report submission form, THE System SHALL display a country selector dropdown
2. WHEN a country is selected, THE System SHALL center the map on the selected country's geographic center
3. WHEN coordinates are captured, THE System SHALL validate them against the selected country's geofencing boundaries
4. IF coordinates fall outside the selected country's boundaries, THEN THE System SHALL reject the report with a descriptive error message
5. THE System SHALL store the selected country identifier with each report
6. WHERE a default country is configured, THE System SHALL pre-select it in the country selector

### Requirement 2: Geofencing Boundary Validation

**User Story:** As a system operator, I want reports validated against country-specific geofencing boundaries, so that all reports fall within valid operational areas.

#### Acceptance Criteria

1. WHEN a report is submitted, THE Geofencing_Service SHALL validate coordinates against the selected country's geofencing boundaries
2. WHEN validating coordinates, THE System SHALL check that latitude is within [min_lat, max_lat] and longitude is within [min_lon, max_lon]
3. IF coordinates fall outside boundaries, THEN THE System SHALL return error code GEO_FENCING_VIOLATION with descriptive message
4. WHEN geofencing validation passes, THE System SHALL proceed with report storage
5. THE System SHALL support geofencing boundaries for at least 3 countries (Spain, Colombia, and one additional)

### Requirement 3: Country Management API

**User Story:** As an administrator, I want to manage countries through RESTful endpoints, so that I can configure the system for new deployments.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/admin/countries, THE System SHALL return a list of all configured countries
2. WHEN a POST request is made to /api/admin/countries, THE System SHALL create a new country with the provided configuration
3. WHEN a GET request is made to /api/admin/countries/{id}, THE System SHALL return details of the specified country
4. WHEN a PUT request is made to /api/admin/countries/{id}, THE System SHALL update the specified country's configuration
5. WHEN a DELETE request is made to /api/admin/countries/{id}, THE System SHALL mark the country as disabled (soft delete)
6. WHEN creating or updating a country, THE System SHALL validate that min_lat < max_lat and min_lon < max_lon
7. THE System SHALL require ROLE_ADMIN for all country management endpoints

### Requirement 4: Administrative Geofencing Configuration

**User Story:** As an administrator, I want to configure geofencing parameters for each country, so that I can define valid reporting areas.

#### Acceptance Criteria

1. WHEN configuring a country, THE System SHALL require min_lat, max_lat, min_lon, max_lon parameters
2. WHEN configuring a country, THE System SHALL accept optional administrative_area and municipality parameters
3. WHEN a country is created, THE System SHALL store the geofencing boundary as a PostGIS polygon or rectangle
4. WHEN updating geofencing boundaries, THE System SHALL validate that the new boundaries are valid geographic coordinates
5. THE System SHALL allow administrators to enable or disable countries
6. WHERE a country is disabled, THE System SHALL prevent new reports from that country

### Requirement 5: Default Country Configuration

**User Story:** As a system administrator deploying to a new country, I want to configure a default country, so that new users can immediately submit reports without manual selection.

#### Acceptance Criteria

1. WHEN the system is deployed for the first time, THE System SHALL create a default country configuration (Spain/Madrid)
2. WHEN a new user accesses the report form without a selected country, THE System SHALL use the default country
3. WHERE an administrator changes the default country, THE System SHALL apply the change to new reports only
4. THE System SHALL store a flag indicating which country is the default
5. WHEN querying countries, THE System SHALL indicate which country is the default

### Requirement 6: Report Filtering by Country

**User Story:** As an operator or administrator, I want to filter reports by country, so that I can focus on incidents in specific geographic areas.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/reports with a country_id parameter, THE System SHALL return only reports from that country
2. WHEN a GET request is made to /api/tasks with a country_id parameter, THE System SHALL return only tasks from that country
3. WHEN no country_id parameter is provided, THE System SHALL return all reports/tasks accessible to the user
4. THE System SHALL support filtering by country name or identifier
5. WHEN filtering by country, THE System SHALL maintain existing filters for state, category, and date range

### Requirement 7: Report Filtering by Administrative Area

**User Story:** As an operator, I want to filter reports by administrative area (state/province/department), so that I can focus on specific regions within a country.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/reports with an administrative_area parameter, THE System SHALL return only reports from that area
2. WHEN a GET request is made to /api/tasks with an administrative_area parameter, THE System SHALL return only tasks from that area
3. WHEN filtering by administrative_area, THE System SHALL also filter by the associated country
4. THE System SHALL support filtering by administrative area name or identifier
5. WHEN no administrative_area parameter is provided, THE System SHALL return all reports regardless of area

### Requirement 8: Report Filtering by Municipality

**User Story:** As an operator, I want to filter reports by municipality (city), so that I can focus on specific local areas.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/reports with a municipality parameter, THE System SHALL return only reports from that municipality
2. WHEN a GET request is made to /api/tasks with a municipality parameter, THE System SHALL return only tasks from that municipality
3. WHEN filtering by municipality, THE System SHALL also filter by the associated country and administrative area
4. THE System SHALL support filtering by municipality name or identifier
5. WHEN no municipality parameter is provided, THE System SHALL return all reports regardless of municipality

### Requirement 9: Heatmap Visualization by Country

**User Story:** As an administrator, I want to view heatmaps for each country separately, so that I can identify incident concentration patterns.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/heatmap with a country_id parameter, THE System SHALL return heatmap data for that country
2. WHEN a GET request is made to /api/heatmap without a country_id parameter, THE System SHALL return heatmap data for all countries
3. WHEN generating heatmap data, THE System SHALL aggregate reports by geographic grid cells
4. WHEN calculating grid cell values, THE System SHALL count the number of reports within each cell
5. THE System SHALL support configurable heatmap grid resolution (e.g., 1km, 5km, 10km cells)

### Requirement 10: HTTPS Configuration for Production

**User Story:** As a system administrator deploying to production, I want to enable HTTPS, so that all communications are encrypted and secure.

#### Acceptance Criteria

1. WHEN the system is deployed in production mode, THE System SHALL require HTTPS for all API endpoints
2. WHEN HTTPS is enabled, THE System SHALL validate SSL/TLS certificates for incoming connections
3. WHEN an HTTP request is received in production mode, THE System SHALL redirect to HTTPS
4. THE System SHALL support SSL certificate configuration through environment variables
5. WHEN SSL certificate files are not provided, THE System SHALL log a warning but continue operation in development mode

### Requirement 11: SSL Certificate Management

**User Story:** As a DevOps engineer, I want to configure SSL certificates for HTTPS, so that the application can serve secure traffic.

#### Acceptance Criteria

1. WHEN SSL certificate is configured, THE System SHALL accept certificate in PEM or PKCS12 format
2. WHEN SSL certificate is configured, THE System SHALL accept private key in PEM format
3. WHEN PKCS12 format is used, THE System SHALL accept keystore password through environment variable
4. WHEN certificate files are mounted in the container, THE System SHALL load them on startup
5. THE System SHALL support Let's Encrypt certificate renewal without service interruption
6. WHEN certificate validation fails, THE System SHALL log detailed error information

### Requirement 12: CORS Configuration for HTTPS

**User Story:** As a system administrator, I want to configure CORS for HTTPS deployment, so that the frontend can communicate with the backend securely.

#### Acceptance Criteria

1. WHEN HTTPS is enabled, THE System SHALL configure CORS to allow requests from configured frontend origins over HTTPS
2. WHEN CORS preflight request is received, THE System SHALL include Access-Control-Allow-Credentials header
3. WHEN CORS configuration is updated, THE System SHALL apply changes without service restart
4. THE System SHALL validate that CORS origins use HTTPS protocol in production
5. WHEN an unauthorized origin attempts to access the API, THE System SHALL return 403 Forbidden

### Requirement 13: Docker HTTPS Configuration

**User Story:** As a DevOps engineer, I want to configure HTTPS in Docker deployments, so that containerized applications serve secure traffic.

#### Acceptance Criteria

1. WHEN Docker deployment is configured for HTTPS, THE System SHALL mount SSL certificate files into the container
2. WHEN Docker deployment is configured for HTTPS, THE System SHALL configure Spring Boot to use HTTPS
3. THE System SHALL provide a docker-compose.yml example for HTTPS deployment
4. WHEN HTTPS is enabled in Docker, THE System SHALL expose port 443 instead of 8080
5. THE System SHALL support environment variable configuration for SSL settings in Docker

### Requirement 14: Frontend Country Selector

**User Story:** As a citizen, I want to see a country selector in the report form, so that I can choose where I'm reporting from.

#### Acceptance Criteria

1. WHEN the report form is rendered, THE System SHALL display a country selector dropdown
2. WHEN the country selector is clicked, THE System SHALL display a list of enabled countries
3. WHEN a country is selected, THE System SHALL update the map view to center on the selected country
4. WHEN no country is selected and a default exists, THE System SHALL display the default country name
5. THE System SHALL disable the country selector if only one country is enabled
6. WHEN a country is selected, THE System SHALL store the selection in local state

### Requirement 15: Frontend Map Centering

**User Story:** As a citizen, I want the map to center on my selected country, so that I can easily find my location.

#### Acceptance Criteria

1. WHEN a country is selected, THE System SHALL center the map on the country's geographic center
2. WHEN the map is centered, THE System SHALL set an appropriate zoom level for the country's size
3. WHEN the user manually pans the map, THE System SHALL remember the new position
4. WHEN the country selection changes, THE System SHALL re-center the map
5. THE System SHALL use Leaflet or Mapbox GL JS for map rendering

### Requirement 16: Frontend Geofencing Boundary Display

**User Story:** As a citizen, I want to see the geofencing boundary for my selected country, so that I understand where I can report from.

#### Acceptance Criteria

1. WHEN a country is selected, THE System SHALL draw the geofencing boundary on the map
2. WHEN the geofencing boundary is drawn, THE System SHALL use a semi-transparent overlay
3. WHEN coordinates are captured, THE System SHALL validate against the displayed boundary
4. IF coordinates are outside the boundary, THE System SHALL highlight the boundary in red
5. WHEN coordinates are inside the boundary, THE System SHALL highlight the boundary in green

### Requirement 17: Frontend Location Input with Country Context

**User Story:** As a citizen, I want the location input to respect country context, so that I can accurately report my location.

#### Acceptance Criteria

1. WHEN the report form loads, THE System SHALL initialize location input with the default country's context
2. WHEN the country selection changes, THE System SHALL update location input validation rules
3. WHEN coordinates are entered manually, THE System SHALL validate them against the selected country's boundaries
4. WHEN geolocation is captured via browser API, THE System SHALL validate the coordinates
5. THE System SHALL display a clear error message when coordinates are outside valid boundaries

### Requirement 18: Country-Specific Data Isolation

**User Story:** As an administrator, I want reports from different countries to be isolated, so that operators only see relevant incidents.

#### Acceptance Criteria

1. WHEN an operator accesses the dashboard, THE System SHALL filter reports by their assigned country
2. WHEN no country is assigned to an operator, THE System SHALL show all reports accessible to their role
3. WHEN a report is created, THE System SHALL ensure it cannot be associated with a different country
4. THE System SHALL enforce country-based data isolation at the database query level
5. WHEN exporting data, THE System SHALL include country identifier in the export

### Requirement 19: Geofencing Service Architecture

**User Story:** As a system architect, I want the geofencing service to support multiple countries, so that the system can scale to new regions.

#### Acceptance Criteria

1. WHEN validating coordinates, THE Geofencing_Service SHALL retrieve the country's geofencing boundary from the database
2. WHEN validating coordinates, THE System SHALL check if latitude is within [min_lat, max_lat] and longitude is within [min_lon, max_lon]
3. WHEN a country's geofencing boundary is updated, THE System SHALL invalidate any cached boundary data
4. THE System SHALL support both simple rectangle boundaries and complex polygon boundaries
5. WHEN geofencing validation fails, THE System SHALL return a country-specific error message

### Requirement 20: Backward Compatibility with Existing Data

**User Story:** As a system administrator, I want existing Madrid reports to remain accessible after implementing multi-country support, so that historical data is not lost.

#### Acceptance Criteria

1. WHEN the multi-country feature is deployed, THE System SHALL migrate existing reports to the default country (Spain)
2. WHEN querying existing reports, THE System SHALL include reports with no country assigned (defaulting to Spain)
3. THE System SHALL provide a migration script to update existing report country associations
4. WHEN running the migration, THE System SHALL log all changes for audit purposes
5. THE System SHALL allow rollback of the migration if issues are detected
