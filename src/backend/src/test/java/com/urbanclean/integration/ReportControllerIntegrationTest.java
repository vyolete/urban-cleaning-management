package com.urbanclean.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanclean.dto.request.ReportSubmissionRequest;
import com.urbanclean.dto.response.ReportResponse;
import com.urbanclean.entity.Country;
import com.urbanclean.entity.Report;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.repository.CountryRepository;
import com.urbanclean.repository.ReportRepository;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Report API endpoints
 * Tests report submission and filtering operations with multi-country support
 * 
 * Task 5.5: Write integration tests for Report API
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Report API Integration Tests")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String tecnicoToken;
    private String ciudadanoToken;
    private Country spainCountry;
    private Country colombiaCountry;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        // Clear existing data
        reportRepository.deleteAll();
        countryRepository.deleteAll();
        userRepository.deleteAll();

        // Initialize geometry factory for PostGIS
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // Create admin user
        User admin = User.builder()
                .username("admin")
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.ROLE_ADMIN)
                .tokenVersion(0)
                .build();
        admin = userRepository.save(admin);
        adminToken = jwtTokenProvider.generateToken(admin.getUsername(), admin.getId(), admin.getRole(), admin.getTokenVersion());

        // Create tecnico user
        User tecnico = User.builder()
                .username("tecnico")
                .email("tecnico@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.ROLE_TECNICO)
                .tokenVersion(0)
                .build();
        tecnico = userRepository.save(tecnico);
        tecnicoToken = jwtTokenProvider.generateToken(tecnico.getUsername(), tecnico.getId(), tecnico.getRole(), tecnico.getTokenVersion());

        // Create ciudadano user
        User ciudadano = User.builder()
                .username("ciudadano")
                .email("ciudadano@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        ciudadano = userRepository.save(ciudadano);
        ciudadanoToken = jwtTokenProvider.generateToken(ciudadano.getUsername(), ciudadano.getId(), ciudadano.getRole(), ciudadano.getTokenVersion());

        // Create Spain country (default)
        spainCountry = Country.builder()
                .name("España")
                .code("ESP")
                .defaultCountry(true)
                .enabled(true)
                .minLat(new BigDecimal("36.0"))
                .maxLat(new BigDecimal("43.8"))
                .minLon(new BigDecimal("-9.3"))
                .maxLon(new BigDecimal("3.3"))
                .administrativeArea("Comunidad de Madrid")
                .municipality("Madrid")
                .centerLat(new BigDecimal("40.4168"))
                .centerLon(new BigDecimal("-3.7038"))
                .build();
        spainCountry = countryRepository.save(spainCountry);

        // Create Colombia country
        colombiaCountry = Country.builder()
                .name("Colombia")
                .code("COL")
                .defaultCountry(false)
                .enabled(true)
                .minLat(new BigDecimal("-4.2"))
                .maxLat(new BigDecimal("12.5"))
                .minLon(new BigDecimal("-79.0"))
                .maxLon(new BigDecimal("-66.9"))
                .administrativeArea("Cundinamarca")
                .municipality("Bogotá")
                .centerLat(new BigDecimal("4.7110"))
                .centerLon(new BigDecimal("-74.0721"))
                .build();
        colombiaCountry = countryRepository.save(colombiaCountry);
    }

    // ========================================================================
    // POST /api/reports - Submit report with country
    // ========================================================================

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should submit report with country ID (Spain)")
    void shouldSubmitReportWithCountryIdSpain() throws Exception {
        // Given - coordinates within Spain boundaries
        ReportSubmissionRequest request = ReportSubmissionRequest.builder()
                .latitude(40.4168)
                .longitude(-3.7038)
                .category("BASURA_ACUMULADA")
                .description("Test report in Madrid, Spain")
                .countryId(spainCountry.getId())
                .build();

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        // When/Then
        MvcResult result = mockMvc.perform(multipart("/api/reports")
                        .file(photo)
                        .file(data))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.latitude").value(40.4168))
                .andExpect(jsonPath("$.longitude").value(-3.7038))
                .andExpect(jsonPath("$.category").value("BASURA_ACUMULADA"))
                .andExpect(jsonPath("$.description").value("Test report in Madrid, Spain"))
                .andExpect(jsonPath("$.submitterUsername").value("Anónimo"))
                .andExpect(jsonPath("$.isDuplicate").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        // Verify report was persisted with country
        String responseJson = result.getResponse().getContentAsString();
        ReportResponse response = objectMapper.readValue(responseJson, ReportResponse.class);
        
        Report savedReport = reportRepository.findById(response.getId()).orElse(null);
        assertThat(savedReport).isNotNull();
        assertThat(savedReport.getCountry()).isNotNull();
        assertThat(savedReport.getCountry().getId()).isEqualTo(spainCountry.getId());
        assertThat(savedReport.getCountry().getName()).isEqualTo("España");
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should submit report with country ID (Colombia)")
    void shouldSubmitReportWithCountryIdColombia() throws Exception {
        // Given - coordinates within Colombia boundaries
        ReportSubmissionRequest request = ReportSubmissionRequest.builder()
                .latitude(4.7110)
                .longitude(-74.0721)
                .category("CONTENEDOR_DANADO")
                .description("Test report in Bogotá, Colombia")
                .countryId(colombiaCountry.getId())
                .build();

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        // When/Then
        MvcResult result = mockMvc.perform(multipart("/api/reports")
                        .file(photo)
                        .file(data))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.latitude").value(4.7110))
                .andExpect(jsonPath("$.longitude").value(-74.0721))
                .andExpect(jsonPath("$.category").value("CONTENEDOR_DANADO"))
                .andReturn();

        // Verify report was persisted with Colombia
        String responseJson = result.getResponse().getContentAsString();
        ReportResponse response = objectMapper.readValue(responseJson, ReportResponse.class);
        
        Report savedReport = reportRepository.findById(response.getId()).orElse(null);
        assertThat(savedReport).isNotNull();
        assertThat(savedReport.getCountry()).isNotNull();
        assertThat(savedReport.getCountry().getId()).isEqualTo(colombiaCountry.getId());
        assertThat(savedReport.getCountry().getName()).isEqualTo("Colombia");
    }

    @Test
    @Transactional
    @DisplayName("Should reject report with coordinates outside country boundaries")
    void shouldRejectReportWithCoordinatesOutsideCountryBoundaries() throws Exception {
        // Given - coordinates outside Spain boundaries (Paris, France)
        ReportSubmissionRequest request = ReportSubmissionRequest.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .category("BASURA_ACUMULADA")
                .description("Test report in Paris (outside Spain)")
                .countryId(spainCountry.getId())
                .build();

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        // When/Then
        mockMvc.perform(multipart("/api/reports")
                        .file(photo)
                        .file(data))
                .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // GET /api/reports - Get all reports with filtering
    // ========================================================================

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should get all reports with country_id parameter (Spain)")
    void shouldGetAllReportsWithCountryIdParameterSpain() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report 1 in Spain", spainCountry);
        createTestReport(40.4200, -3.7100, "CONTENEDOR_DANADO", "Report 2 in Spain", spainCountry);
        
        // Given - create report in Colombia
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", "Report in Colombia", colombiaCountry);

        // When/Then - filter by Spain
        mockMvc.perform(get("/api/reports")
                        .param("countryId", spainCountry.getId().toString())
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].countryId", everyItem(is(spainCountry.getId().toString()))))
                .andExpect(jsonPath("$[*].countryName", everyItem(is("España"))));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should get all reports with country_id parameter (Colombia)")
    void shouldGetAllReportsWithCountryIdParameterColombia() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report 1 in Spain", spainCountry);
        
        // Given - create reports in Colombia
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", "Report 1 in Colombia", colombiaCountry);
        createTestReport(4.7200, -74.0800, "LIMPIEZA_GRAFFITI", "Report 2 in Colombia", colombiaCountry);

        // When/Then - filter by Colombia
        mockMvc.perform(get("/api/reports")
                        .param("countryId", colombiaCountry.getId().toString())
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].countryId", everyItem(is(colombiaCountry.getId().toString()))))
                .andExpect(jsonPath("$[*].countryName", everyItem(is("Colombia"))));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should get all reports without country_id parameter")
    void shouldGetAllReportsWithoutCountryIdParameter() throws Exception {
        // Given - create reports in both countries
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report in Spain", spainCountry);
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", "Report in Colombia", colombiaCountry);

        // When/Then - no filter, should return all reports
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].countryName", containsInAnyOrder("España", "Colombia")));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should get all reports with administrative_area parameter")
    void shouldGetAllReportsWithAdministrativeAreaParameter() throws Exception {
        // Given - create reports in Spain (Comunidad de Madrid)
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report 1 in Madrid", spainCountry);
        createTestReport(40.4200, -3.7100, "CONTENEDOR_DANADO", "Report 2 in Madrid", spainCountry);
        
        // Given - create report in Colombia (Cundinamarca)
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", "Report in Bogotá", colombiaCountry);

        // When/Then - filter by Comunidad de Madrid
        mockMvc.perform(get("/api/reports")
                        .param("administrativeArea", "Comunidad de Madrid")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].countryName", everyItem(is("España"))));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should get all reports with municipality parameter")
    void shouldGetAllReportsWithMunicipalityParameter() throws Exception {
        // Given - create reports in Madrid, Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report 1 in Madrid", spainCountry);
        createTestReport(40.4200, -3.7100, "CONTENEDOR_DANADO", "Report 2 in Madrid", spainCountry);
        
        // Given - create report in Bogotá, Colombia
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", "Report in Bogotá", colombiaCountry);

        // When/Then - filter by Madrid municipality
        mockMvc.perform(get("/api/reports")
                        .param("municipality", "Madrid")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].countryName", everyItem(is("España"))));

        // When/Then - filter by Bogotá municipality
        mockMvc.perform(get("/api/reports")
                        .param("municipality", "Bogotá")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].countryName").value("Colombia"));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should get empty list when filtering by non-existent country")
    void shouldGetEmptyListWhenFilteringByNonExistentCountry() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report in Spain", spainCountry);

        // When/Then - filter by non-existent country ID
        mockMvc.perform(get("/api/reports")
                        .param("countryId", java.util.UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should get empty list when filtering by non-existent administrative area")
    void shouldGetEmptyListWhenFilteringByNonExistentAdministrativeArea() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report in Spain", spainCountry);

        // When/Then - filter by non-existent administrative area
        mockMvc.perform(get("/api/reports")
                        .param("administrativeArea", "Non-existent Area")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should get empty list when filtering by non-existent municipality")
    void shouldGetEmptyListWhenFilteringByNonExistentMunicipality() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report in Spain", spainCountry);

        // When/Then - filter by non-existent municipality
        mockMvc.perform(get("/api/reports")
                        .param("municipality", "Non-existent City")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should combine multiple filters (country and administrative area)")
    void shouldCombineMultipleFiltersCountryAndAdministrativeArea() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report in Madrid", spainCountry);
        
        // Given - create report in Colombia
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", "Report in Bogotá", colombiaCountry);

        // When/Then - filter by Spain country and Comunidad de Madrid
        mockMvc.perform(get("/api/reports")
                        .param("countryId", spainCountry.getId().toString())
                        .param("administrativeArea", "Comunidad de Madrid")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].countryName").value("España"));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should combine all filters (country, administrative area, and municipality)")
    void shouldCombineAllFilters() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", "Report 1 in Madrid", spainCountry);
        createTestReport(40.4200, -3.7100, "CONTENEDOR_DANADO", "Report 2 in Madrid", spainCountry);
        
        // Given - create report in Colombia
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", "Report in Bogotá", colombiaCountry);

        // When/Then - filter by all parameters
        mockMvc.perform(get("/api/reports")
                        .param("countryId", spainCountry.getId().toString())
                        .param("administrativeArea", "Comunidad de Madrid")
                        .param("municipality", "Madrid")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].countryName", everyItem(is("España"))));
    }

    // ========================================================================
    // Authorization Tests
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Should allow admin to get all reports")
    void shouldAllowAdminToGetAllReports() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    @DisplayName("Should allow tecnico to get all reports")
    void shouldAllowTecnicoToGetAllReports() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    @DisplayName("Should deny ciudadano access to get all reports")
    void shouldDenyCiudadanoAccessToGetAllReports() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + ciudadanoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("Should deny unauthenticated access to get all reports")
    void shouldDenyUnauthenticatedAccessToGetAllReports() throws Exception {
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isUnauthorized());
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Helper method to create a test report
     */
    private Report createTestReport(double latitude, double longitude, String category, 
                                   String description, Country country) {
        Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        
        Report report = Report.builder()
                .location(location)
                .category(category)
                .description(description)
                .photoUrl("/uploads/test.jpg")
                .isDuplicate(false)
                .country(country)
                .build();
        
        return reportRepository.save(report);
    }
}
