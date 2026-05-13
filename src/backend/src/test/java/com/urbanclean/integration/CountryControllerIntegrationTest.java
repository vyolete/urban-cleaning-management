package com.urbanclean.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanclean.dto.request.CountryRequest;
import com.urbanclean.dto.response.CountryResponse;
import com.urbanclean.entity.Country;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.repository.CountryRepository;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Country API endpoints
 * Tests country management operations including CRUD operations and authorization
 * 
 * Task 5.4: Write integration tests for Country API
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Country API Integration Tests")
class CountryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String tecnicoToken;
    private String ciudadanoToken;
    private Country defaultCountry;

    @BeforeEach
    void setUp() {
        // Clear existing data
        countryRepository.deleteAll();
        userRepository.deleteAll();

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

        // Create default country (Spain)
        defaultCountry = Country.builder()
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
        defaultCountry = countryRepository.save(defaultCountry);
    }

    // ========================================================================
    // GET /api/admin/countries - Get all countries
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Should get all countries as admin")
    void shouldGetAllCountriesAsAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/countries")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("España"))
                .andExpect(jsonPath("$[0].code").value("ESP"))
                .andExpect(jsonPath("$[0].defaultCountry").value(true))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    @Transactional
    @DisplayName("Should deny access to get all countries for non-admin")
    void shouldDenyAccessToGetAllCountriesForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/countries")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/countries")
                        .header("Authorization", "Bearer " + ciudadanoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("Should deny access to get all countries without authentication")
    void shouldDenyAccessToGetAllCountriesWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/admin/countries"))
                .andExpect(status().isUnauthorized());
    }

    // ========================================================================
    // POST /api/admin/countries - Create country
    // ========================================================================

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should create country with valid data as admin")
    void shouldCreateCountryWithValidDataAsAdmin() throws Exception {
        // Given
        CountryRequest request = CountryRequest.builder()
                .name("Colombia")
                .code("COL")
                .minLat(new BigDecimal("-4.2"))
                .maxLat(new BigDecimal("12.5"))
                .minLon(new BigDecimal("-79.0"))
                .maxLon(new BigDecimal("-66.9"))
                .administrativeArea("Cundinamarca")
                .municipality("Bogotá")
                .centerLat(new BigDecimal("4.7110"))
                .centerLon(new BigDecimal("-74.0721"))
                .build();

        // When/Then
        MvcResult result = mockMvc.perform(post("/api/admin/countries")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Colombia"))
                .andExpect(jsonPath("$.code").value("COL"))
                .andExpect(jsonPath("$.defaultCountry").value(false))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.minLat").value(new BigDecimal("-4.2")))
                .andExpect(jsonPath("$.maxLat").value(new BigDecimal("12.5")))
                .andExpect(jsonPath("$.minLon").value(new BigDecimal("-79.0")))
                .andExpect(jsonPath("$.maxLon").value(new BigDecimal("-66.9")))
                .andExpect(jsonPath("$.administrativeArea").value("Cundinamarca"))
                .andExpect(jsonPath("$.municipality").value("Bogotá"))
                .andExpect(jsonPath("$.centerLat").value(new BigDecimal("4.7110")))
                .andExpect(jsonPath("$.centerLon").value(new BigDecimal("-74.0721")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        // Verify country was persisted
        String responseJson = result.getResponse().getContentAsString();
        CountryResponse response = objectMapper.readValue(responseJson, CountryResponse.class);
        
        Country savedCountry = countryRepository.findById(response.getId()).orElse(null);
        assertThat(savedCountry).isNotNull();
        assertThat(savedCountry.getName()).isEqualTo("Colombia");
        assertThat(savedCountry.getCode()).isEqualTo("COL");
    }

    @Test
    @Transactional
    @DisplayName("Should reject country creation with invalid boundaries")
    void shouldRejectCountryCreationWithInvalidBoundaries() throws Exception {
        // Given - minLat > maxLat (invalid)
        CountryRequest request = CountryRequest.builder()
                .name("Invalid Country")
                .code("INV")
                .minLat(new BigDecimal("50.0"))
                .maxLat(new BigDecimal("40.0"))  // Less than minLat
                .minLon(new BigDecimal("-10.0"))
                .maxLon(new BigDecimal("10.0"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/admin/countries")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should reject country creation with missing required fields")
    void shouldRejectCountryCreationWithMissingFields() throws Exception {
        // Given - missing name
        CountryRequest request = CountryRequest.builder()
                .code("TST")
                .minLat(new BigDecimal("40.0"))
                .maxLat(new BigDecimal("50.0"))
                .minLon(new BigDecimal("-10.0"))
                .maxLon(new BigDecimal("10.0"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/admin/countries")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should reject country creation with invalid country code length")
    void shouldRejectCountryCreationWithInvalidCodeLength() throws Exception {
        // Given - code with 2 characters instead of 3
        CountryRequest request = CountryRequest.builder()
                .name("Test Country")
                .code("TS")  // Invalid: must be 3 characters
                .minLat(new BigDecimal("40.0"))
                .maxLat(new BigDecimal("50.0"))
                .minLon(new BigDecimal("-10.0"))
                .maxLon(new BigDecimal("10.0"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/admin/countries")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should deny country creation for non-admin")
    void shouldDenyCountryCreationForNonAdmin() throws Exception {
        // Given
        CountryRequest request = CountryRequest.builder()
                .name("Test Country")
                .code("TST")
                .minLat(new BigDecimal("40.0"))
                .maxLat(new BigDecimal("50.0"))
                .minLon(new BigDecimal("-10.0"))
                .maxLon(new BigDecimal("10.0"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/admin/countries")
                        .header("Authorization", "Bearer " + tecnicoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========================================================================
    // GET /api/admin/countries/{id} - Get country by ID
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Should get country by ID as admin")
    void shouldGetCountryByIdAsAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/countries/{id}", defaultCountry.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(defaultCountry.getId().toString()))
                .andExpect(jsonPath("$.name").value("España"))
                .andExpect(jsonPath("$.code").value("ESP"))
                .andExpect(jsonPath("$.defaultCountry").value(true))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @Transactional
    @DisplayName("Should return 404 when country not found")
    void shouldReturn404WhenCountryNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/admin/countries/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @DisplayName("Should deny access to get country by ID for non-admin")
    void shouldDenyAccessToGetCountryByIdForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/countries/{id}", defaultCountry.getId())
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isForbidden());
    }

    // ========================================================================
    // PUT /api/admin/countries/{id} - Update country
    // ========================================================================

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should update country with valid data as admin")
    void shouldUpdateCountryWithValidDataAsAdmin() throws Exception {
        // Given
        CountryRequest request = CountryRequest.builder()
                .name("España Actualizada")
                .code("ESP")
                .minLat(new BigDecimal("35.0"))
                .maxLat(new BigDecimal("44.0"))
                .minLon(new BigDecimal("-10.0"))
                .maxLon(new BigDecimal("4.0"))
                .administrativeArea("Comunidad de Madrid")
                .municipality("Madrid")
                .centerLat(new BigDecimal("40.5"))
                .centerLon(new BigDecimal("-3.7"))
                .build();

        // When/Then
        mockMvc.perform(put("/api/admin/countries/{id}", defaultCountry.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(defaultCountry.getId().toString()))
                .andExpect(jsonPath("$.name").value("España Actualizada"))
                .andExpect(jsonPath("$.minLat").value(new BigDecimal("35.0")))
                .andExpect(jsonPath("$.maxLat").value(new BigDecimal("44.0")))
                .andExpect(jsonPath("$.centerLat").value(new BigDecimal("40.5")));

        // Verify country was updated
        Country updatedCountry = countryRepository.findById(defaultCountry.getId()).orElse(null);
        assertThat(updatedCountry).isNotNull();
        assertThat(updatedCountry.getName()).isEqualTo("España Actualizada");
        assertThat(updatedCountry.getMinLat()).isEqualByComparingTo(new BigDecimal("35.0"));
    }

    @Test
    @Transactional
    @DisplayName("Should reject country update with invalid boundaries")
    void shouldRejectCountryUpdateWithInvalidBoundaries() throws Exception {
        // Given - minLon > maxLon (invalid)
        CountryRequest request = CountryRequest.builder()
                .name("España")
                .code("ESP")
                .minLat(new BigDecimal("36.0"))
                .maxLat(new BigDecimal("43.8"))
                .minLon(new BigDecimal("10.0"))
                .maxLon(new BigDecimal("-10.0"))  // Less than minLon
                .build();

        // When/Then
        mockMvc.perform(put("/api/admin/countries/{id}", defaultCountry.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should return 404 when updating non-existent country")
    void shouldReturn404WhenUpdatingNonExistentCountry() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        CountryRequest request = CountryRequest.builder()
                .name("Test Country")
                .code("TST")
                .minLat(new BigDecimal("40.0"))
                .maxLat(new BigDecimal("50.0"))
                .minLon(new BigDecimal("-10.0"))
                .maxLon(new BigDecimal("10.0"))
                .build();

        // When/Then
        mockMvc.perform(put("/api/admin/countries/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @DisplayName("Should deny country update for non-admin")
    void shouldDenyCountryUpdateForNonAdmin() throws Exception {
        // Given
        CountryRequest request = CountryRequest.builder()
                .name("España Actualizada")
                .code("ESP")
                .minLat(new BigDecimal("36.0"))
                .maxLat(new BigDecimal("43.8"))
                .minLon(new BigDecimal("-9.3"))
                .maxLon(new BigDecimal("3.3"))
                .build();

        // When/Then
        mockMvc.perform(put("/api/admin/countries/{id}", defaultCountry.getId())
                        .header("Authorization", "Bearer " + ciudadanoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========================================================================
    // DELETE /api/admin/countries/{id} - Delete country
    // ========================================================================

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should delete (disable) country as admin")
    void shouldDeleteCountryAsAdmin() throws Exception {
        // Given - create a non-default country to delete
        Country countryToDelete = Country.builder()
                .name("Colombia")
                .code("COL")
                .defaultCountry(false)
                .enabled(true)
                .minLat(new BigDecimal("-4.2"))
                .maxLat(new BigDecimal("12.5"))
                .minLon(new BigDecimal("-79.0"))
                .maxLon(new BigDecimal("-66.9"))
                .build();
        countryToDelete = countryRepository.save(countryToDelete);

        // When/Then
        mockMvc.perform(delete("/api/admin/countries/{id}", countryToDelete.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify country was disabled (soft delete)
        Country deletedCountry = countryRepository.findById(countryToDelete.getId()).orElse(null);
        assertThat(deletedCountry).isNotNull();
        assertThat(deletedCountry.getEnabled()).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("Should reject deletion of default country")
    void shouldRejectDeletionOfDefaultCountry() throws Exception {
        mockMvc.perform(delete("/api/admin/countries/{id}", defaultCountry.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should return 404 when deleting non-existent country")
    void shouldReturn404WhenDeletingNonExistentCountry() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        mockMvc.perform(delete("/api/admin/countries/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @DisplayName("Should deny country deletion for non-admin")
    void shouldDenyCountryDeletionForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/countries/{id}", defaultCountry.getId())
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isForbidden());
    }

    // ========================================================================
    // GET /api/admin/countries/default - Get default country
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Should get default country as admin")
    void shouldGetDefaultCountryAsAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/countries/default")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(defaultCountry.getId().toString()))
                .andExpect(jsonPath("$.name").value("España"))
                .andExpect(jsonPath("$.code").value("ESP"))
                .andExpect(jsonPath("$.defaultCountry").value(true));
    }

    @Test
    @Transactional
    @DisplayName("Should get default country as tecnico")
    void shouldGetDefaultCountryAsTecnico() throws Exception {
        mockMvc.perform(get("/api/admin/countries/default")
                        .header("Authorization", "Bearer " + tecnicoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCountry").value(true));
    }

    @Test
    @Transactional
    @DisplayName("Should get default country as ciudadano")
    void shouldGetDefaultCountryAsCiudadano() throws Exception {
        mockMvc.perform(get("/api/admin/countries/default")
                        .header("Authorization", "Bearer " + ciudadanoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCountry").value(true));
    }

    @Test
    @Transactional
    @DisplayName("Should deny access to default country without authentication")
    void shouldDenyAccessToDefaultCountryWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/admin/countries/default"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    @DisplayName("Should return 404 when no default country exists")
    void shouldReturn404WhenNoDefaultCountryExists() throws Exception {
        // Given - remove default flag from all countries
        defaultCountry.setDefaultCountry(false);
        countryRepository.save(defaultCountry);

        // When/Then
        mockMvc.perform(get("/api/admin/countries/default")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ========================================================================
    // Additional Integration Tests
    // ========================================================================

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should create multiple countries and retrieve all")
    void shouldCreateMultipleCountriesAndRetrieveAll() throws Exception {
        // Given - create Colombia
        CountryRequest colombiaRequest = CountryRequest.builder()
                .name("Colombia")
                .code("COL")
                .minLat(new BigDecimal("-4.2"))
                .maxLat(new BigDecimal("12.5"))
                .minLon(new BigDecimal("-79.0"))
                .maxLon(new BigDecimal("-66.9"))
                .build();

        mockMvc.perform(post("/api/admin/countries")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(colombiaRequest)))
                .andExpect(status().isCreated());

        // Given - create Mexico
        CountryRequest mexicoRequest = CountryRequest.builder()
                .name("México")
                .code("MEX")
                .minLat(new BigDecimal("14.5"))
                .maxLat(new BigDecimal("32.7"))
                .minLon(new BigDecimal("-118.4"))
                .maxLon(new BigDecimal("-86.7"))
                .build();

        mockMvc.perform(post("/api/admin/countries")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mexicoRequest)))
                .andExpect(status().isCreated());

        // When/Then - retrieve all countries
        mockMvc.perform(get("/api/admin/countries")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("España", "Colombia", "México")));
    }

    @Test
    @Transactional
    @DisplayName("Should validate JSON serialization of country response")
    void shouldValidateJsonSerializationOfCountryResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/countries/{id}", defaultCountry.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        CountryResponse response = objectMapper.readValue(responseJson, CountryResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(defaultCountry.getId());
        assertThat(response.getName()).isEqualTo("España");
        assertThat(response.getCode()).isEqualTo("ESP");
        assertThat(response.getDefaultCountry()).isTrue();
        assertThat(response.getEnabled()).isTrue();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }
}
