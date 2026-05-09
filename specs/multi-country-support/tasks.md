# Tasks: Multi-Country Support

## Overview
This document contains the implementation tasks for adding multi-country support to the Urban Cleaning Management System (Urbix).

## Task List

### Phase 1: Database Schema and Migration

#### 1.1 Create countries table and indexes
- [ ] Create countries table with all required fields
- [ ] Create indexes for default_country, enabled, and code
- [ ] Add country_id column to reportes table
- [ ] Create index for country_id in reportes table

#### 1.2 Insert default country configuration
- [ ] Insert Spain/Madrid as default country
- [ ] Verify default country is marked as default_country = TRUE
- [ ] Verify default country is marked as enabled = TRUE

#### 1.3 Migrate existing reports to default country
- [ ] Update existing reports to use default country (Spain)
- [ ] Verify all reports have country_id assigned
- [ ] Make country_id NOT NULL after migration

#### 1.4 Create rollback script
- [ ] Create rollback script for database migration
- [ ] Test rollback script in development environment

### Phase 2: Backend Implementation

#### 2.1 Create Country entity
- [ ] Create Country.java entity class
- [ ] Add all required fields (name, code, boundaries, etc.)
- [ ] Add JPA annotations
- [ ] Add Lombok annotations

#### 2.2 Create CountryRepository
- [ ] Create CountryRepository interface
- [ ] Add methods for CRUD operations
- [ ] Add custom query methods (findByDefaultCountryTrue, findByEnabledTrue, etc.)

#### 2.3 Create Country DTOs
- [ ] Create CountryRequest.java
- [ ] Create CountryResponse.java
- [ ] Add validation annotations

#### 2.4 Create CountryService
- [ ] Implement CRUD operations
- [ ] Implement default country management
- [ ] Implement geofencing boundary validation
- [ ] Implement migration logic

#### 2.5 Create CountryController
- [ ] Implement GET /api/admin/countries
- [ ] Implement POST /api/admin/countries
- [ ] Implement GET /api/admin/countries/{id}
- [ ] Implement PUT /api/admin/countries/{id}
- [ ] Implement DELETE /api/admin/countries/{id}
- [ ] Implement GET /api/admin/countries/default
- [ ] Add authorization (ROLE_ADMIN)

#### 2.6 Update GeofencingService
- [ ] Add method to validate coordinates with country context
- [ ] Add method to get country boundary polygon
- [ ] Add method to get country by ID
- [ ] Update existing methods to support multiple countries

#### 2.7 Update Report entity
- [ ] Add country_id field to Report entity
- [ ] Add @ManyToOne relationship to Country
- [ ] Update JPA annotations

#### 2.8 Update ReportRepository
- [ ] Add findByCountryId method
- [ ] Update heatmap query to support country filtering
- [ ] Add methods for filtering by administrative area and municipality

#### 2.9 Update ReportService
- [ ] Update createReport to accept country_id
- [ ] Update getAllReports to support filtering by country
- [ ] Update getAllReports to support filtering by administrative area
- [ ] Update getAllReports to support filtering by municipality

#### 2.10 Update ReportController
- [ ] Update getAllReports to accept country_id parameter
- [ ] Update getAllReports to accept administrative_area parameter
- [ ] Update getAllReports to accept municipality parameter

#### 2.11 Update ReportSubmissionRequest
- [ ] Add country_id field
- [ ] Add validation if needed

#### 2.12 Update ReportResponse
- [ ] Add country_id field
- [ ] Add country name field

#### 2.13 Update HeatmapService
- [ ] Update getHeatmapData to support country filtering
- [ ] Update method signature to accept country_id parameter

#### 2.14 Update HeatmapController
- [ ] Update endpoint to accept country_id parameter
- [ ] Update method to call HeatmapService with country_id

### Phase 3: HTTPS and Security Configuration

#### 3.1 Configure SSL in application.properties
- [ ] Add server.ssl.enabled property
- [ ] Add server.ssl.key-store property
- [ ] Add server.ssl.key-store-password property
- [ ] Add server.ssl.key-store-type property
- [ ] Add server.ssl.key-alias property

#### 3.2 Create SSL Configuration class
- [ ] Create SSLConfiguration.java
- [ ] Configure Tomcat connector for HTTP to HTTPS redirect
- [ ] Add conditional on SSL enabled property

#### 3.3 Create CORS Configuration class
- [ ] Create CORSConfiguration.java
- [ ] Configure allowed origins (HTTPS only in production)
- [ ] Configure allow credentials
- [ ] Configure max age

#### 3.4 Update docker-compose.yml
- [ ] Update backend port to 8443 (HTTPS)
- [ ] Add SSL environment variables
- [ ] Add volume mount for SSL certificate
- [ ] Update health check to use HTTPS

### Phase 4: Frontend Implementation

#### 4.1 Create CountrySelector component
- [ ] Create CountrySelector.jsx
- [ ] Implement country loading from API
- [ ] Implement country selection handler
- [ ] Handle disabled state (single country)

#### 4.2 Update ReportForm component
- [ ] Add country selector to form
- [ ] Add countryId to formData state
- [ ] Update handleSubmit to include countryId in report data

#### 4.3 Update MapView component
- [ ] Add countryId prop
- [ ] Implement country boundary loading
- [ ] Implement map centering on country
- [ ] Implement geofence boundary display

#### 4.4 Update reportService
- [ ] Add getCountries method
- [ ] Add getCountryById method
- [ ] Add getDefaultCountry method

#### 4.5 Update CitizenReportPage
- [ ] Pass countryId to ReportForm
- [ ] Pass countryId to MapView
- [ ] Handle country selection state

### Phase 5: Testing

#### 5.1 Write unit tests for CountryService
- [ ] Test CRUD operations
- [ ] Test default country management
- [ ] Test geofencing boundary validation

#### 5.2 Write unit tests for GeofencingService
- [ ] Test coordinate validation for each country
- [ ] Test boundary checking
- [ ] Test polygon creation

#### 5.3 Write unit tests for ReportService
- [ ] Test report creation with country context
- [ ] Test filtering by country
- [ ] Test filtering by administrative area
- [ ] Test filtering by municipality

#### 5.4 Write integration tests for Country API
- [ ] Test GET /api/admin/countries
- [ ] Test POST /api/admin/countries
- [ ] Test GET /api/admin/countries/{id}
- [ ] Test PUT /api/admin/countries/{id}
- [ ] Test DELETE /api/admin/countries/{id}
- [ ] Test GET /api/admin/countries/default

#### 5.5 Write integration tests for Report API
- [ ] Test report submission with country
- [ ] Test GET /api/reports with country_id parameter
- [ ] Test GET /api/reports with administrative_area parameter
- [ ] Test GET /api/reports with municipality parameter

#### 5.6 Write integration tests for Heatmap API
- [ ] Test GET /api/heatmap with country_id parameter
- [ ] Test GET /api/heatmap without country_id parameter

#### 5.7 Write property-based tests for country configuration
- [ ] Test that all countries have valid geofencing boundaries
- [ ] Test that default country is unique
- [ ] Test that enabled countries can be queried

#### 5.8 Write property-based tests for geofencing validation
- [ ] Test that coordinates within boundaries pass validation
- [ ] Test that coordinates outside boundaries fail validation
- [ ] Test that validation works for all configured countries

#### 5.9 Write property-based tests for report filtering
- [ ] Test that filtering by country returns only reports from that country
- [ ] Test that filtering by administrative area includes country filtering
- [ ] Test that filtering by municipality includes country and administrative area filtering

#### 5.10 Write property-based tests for data isolation
- [ ] Test that reports from one country cannot be accessed by filtering for another country
- [ ] Test that operators only see reports from their assigned country

### Phase 6: Deployment and Documentation

#### 6.1 Create database migration script
- [ ] Create migration.sql script
- [ ] Include rollback script
- [ ] Test migration in development environment

#### 6.2 Create HTTPS certificate setup guide
- [ ] Document SSL certificate generation
- [ ] Document certificate configuration
- [ ] Document Let's Encrypt integration

#### 6.3 Update docker-compose.yml example
- [ ] Add HTTPS configuration example
- [ ] Add SSL certificate volume mount
- [ ] Document environment variables

#### 6.4 Update README.md
- [ ] Document multi-country support feature
- [ ] Document country management API
- [ ] Document report filtering options
- [ ] Document HTTPS configuration

#### 6.5 Test deployment
- [ ] Deploy to development environment
- [ ] Test country management
- [ ] Test report submission with country
- [ ] Test report filtering
- [ ] Test HTTPS connectivity
- [ ] Test CORS configuration

## Notes
- All tasks should be completed in order
- Property-based tests should use the testing framework specified in the design document
- Integration tests should use the existing test infrastructure
- Documentation should be updated as each phase is completed