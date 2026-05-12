package com.urbanclean.integration;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Heatmap API endpoints
 * Tests heatmap generation with country filtering
 * 
 * Task 5.6: Write integration tests for Heatmap API
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Heatmap API Integration Tests")
class HeatmapControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    // GET /api/analytics/heatmap with country_id parameter
    // ========================================================================

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should generate heatmap with country_id parameter (Spain)")
    void shouldGenerateHeatmapWithCountryIdParameterSpain() throws Exception {
        // Given - create multiple reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", spainCountry);
        createTestReport(40.4200, -3.7100, "BASURA_ACUMULADA", spainCountry);
        createTestReport(40.4150, -3.7050, "CONTENEDOR_DANADO", spainCountry);
        
        // Given - create reports in Colombia (should not appear in Spain heatmap)
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", colombiaCountry);
        createTestReport(4.7200, -74.0800, "LIMPIEZA_GRAFFITI", colombiaCountry);

        // When/Then - request heatmap for Spain
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("countryId", spainCountry.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(3))
                .andExpect(jsonPath("$.cells").isArray())
                .andExpect(jsonPath("$.cells", not(empty())))
                .andExpect(jsonPath("$.maxIntensity").exists())
                .andExpect(jsonPath("$.cellSizeMeters").exists());
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should generate heatmap with country_id parameter (Colombia)")
    void shouldGenerateHeatmapWithCountryIdParameterColombia() throws Exception {
        // Given - create reports in Spain (should not appear in Colombia heatmap)
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", spainCountry);
        
        // Given - create multiple reports in Colombia
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", colombiaCountry);
        createTestReport(4.7200, -74.0800, "LIMPIEZA_GRAFFITI", colombiaCountry);
        createTestReport(4.7150, -74.0750, "CONTENEDOR_DANADO", colombiaCountry);
        createTestReport(4.7180, -74.0780, "BASURA_ACUMULADA", colombiaCountry);

        // When/Then - request heatmap for Colombia
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("countryId", colombiaCountry.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(4))
                .andExpect(jsonPath("$.cells").isArray())
                .andExpect(jsonPath("$.cells", not(empty())))
                .andExpect(jsonPath("$.maxIntensity").exists())
                .andExpect(jsonPath("$.cellSizeMeters").exists());
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should return empty heatmap when no reports exist for country")
    void shouldReturnEmptyHeatmapWhenNoReportsExistForCountry() throws Exception {
        // Given - create reports only in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", spainCountry);

        // When/Then - request heatmap for Colombia (no reports)
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("countryId", colombiaCountry.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(0))
                .andExpect(jsonPath("$.cells").isArray())
                .andExpect(jsonPath("$.cells", empty()))
                .andExpect(jsonPath("$.maxIntensity").value(0.0));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should generate heatmap with custom cell size for specific country")
    void shouldGenerateHeatmapWithCustomCellSizeForSpecificCountry() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", spainCountry);
        createTestReport(40.4200, -3.7100, "CONTENEDOR_DANADO", spainCountry);

        // When/Then - request heatmap with custom cell size
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("countryId", spainCountry.getId().toString())
                        .param("cellSize", "1000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(2))
                .andExpect(jsonPath("$.cells").isArray())
                .andExpect(jsonPath("$.cellSizeMeters").value(1000.0));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should filter heatmap by category and country")
    void shouldFilterHeatmapByCategoryAndCountry() throws Exception {
        // Given - create reports with different categories in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", spainCountry);
        createTestReport(40.4200, -3.7100, "BASURA_ACUMULADA", spainCountry);
        createTestReport(40.4150, -3.7050, "CONTENEDOR_DANADO", spainCountry);
        
        // Given - create reports in Colombia
        createTestReport(4.7110, -74.0721, "BASURA_ACUMULADA", colombiaCountry);

        // When/Then - filter by BASURA_ACUMULADA category in Spain
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("countryId", spainCountry.getId().toString())
                        .param("category", "BASURA_ACUMULADA")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(2))
                .andExpect(jsonPath("$.cells").isArray());
    }

    // ========================================================================
    // GET /api/analytics/heatmap without country_id parameter
    // ========================================================================

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should generate heatmap without country_id parameter (all countries)")
    void shouldGenerateHeatmapWithoutCountryIdParameter() throws Exception {
        // Given - create reports in Spain
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", spainCountry);
        createTestReport(40.4200, -3.7100, "CONTENEDOR_DANADO", spainCountry);
        
        // Given - create reports in Colombia
        createTestReport(4.7110, -74.0721, "VERTIDO_ILEGAL", colombiaCountry);
        createTestReport(4.7200, -74.0800, "LIMPIEZA_GRAFFITI", colombiaCountry);

        // When/Then - request heatmap without country filter (should include all)
        mockMvc.perform(get("/api/analytics/heatmap")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(4))
                .andExpect(jsonPath("$.cells").isArray())
                .andExpect(jsonPath("$.cells", not(empty())));
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should generate heatmap for all countries with category filter")
    void shouldGenerateHeatmapForAllCountriesWithCategoryFilter() throws Exception {
        // Given - create reports with different categories in both countries
        createTestReport(40.4168, -3.7038, "BASURA_ACUMULADA", spainCountry);
        createTestReport(40.4200, -3.7100, "CONTENEDOR_DANADO", spainCountry);
        createTestReport(4.7110, -74.0721, "BASURA_ACUMULADA", colombiaCountry);
        createTestReport(4.7200, -74.0800, "LIMPIEZA_GRAFFITI", colombiaCountry);

        // When/Then - filter by BASURA_ACUMULADA across all countries
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("category", "BASURA_ACUMULADA")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(2))
                .andExpect(jsonPath("$.cells").isArray());
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should return empty heatmap when no reports exist")
    void shouldReturnEmptyHeatmapWhenNoReportsExist() throws Exception {
        // Given - no reports created

        // When/Then - request heatmap without any reports
        mockMvc.perform(get("/api/analytics/heatmap")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(0))
                .andExpect(jsonPath("$.cells").isArray())
                .andExpect(jsonPath("$.cells", empty()))
                .andExpect(jsonPath("$.maxIntensity").value(0.0));
    }

    // ========================================================================
    // Authorization Tests
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Should allow admin to access heatmap")
    void shouldAllowAdminToAccessHeatmap() throws Exception {
        mockMvc.perform(get("/api/analytics/heatmap")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    @DisplayName("Should allow tecnico to access heatmap")
    void shouldAllowTecnicoToAccessHeatmap() throws Exception {
        mockMvc.perform(get("/api/analytics/heatmap")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    @DisplayName("Should deny unauthenticated access to heatmap")
    void shouldDenyUnauthenticatedAccessToHeatmap() throws Exception {
        mockMvc.perform(get("/api/analytics/heatmap"))
                .andExpect(status().isUnauthorized());
    }

    // ========================================================================
    // Validation Tests
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Should reject invalid cell size (too small)")
    void shouldRejectInvalidCellSizeTooSmall() throws Exception {
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("cellSize", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should reject invalid cell size (too large)")
    void shouldRejectInvalidCellSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("cellSize", "10000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should handle invalid country ID gracefully")
    void shouldHandleInvalidCountryIdGracefully() throws Exception {
        // When/Then - request with non-existent country ID
        mockMvc.perform(get("/api/analytics/heatmap")
                        .param("countryId", java.util.UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(0))
                .andExpect(jsonPath("$.cells", empty()));
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Helper method to create a test report
     */
    private Report createTestReport(double latitude, double longitude, String category, Country country) {
        Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        
        Report report = Report.builder()
                .location(location)
                .category(category)
                .description("Test report for heatmap")
                .photoUrl("/uploads/test.jpg")
                .isDuplicate(false)
                .country(country)
                .build();
        
        return reportRepository.save(report);
    }
}
