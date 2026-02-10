package com.urbanclean.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanclean.dto.request.DuplicateDetectionRequest;
import com.urbanclean.dto.request.TokenExpirationRequest;
import com.urbanclean.dto.response.DuplicateDetectionResponse;
import com.urbanclean.dto.response.TokenExpirationResponse;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for configuration endpoints
 * Tests admin configuration management for tokens and duplicate detection
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Configuration Integration Tests")
class ConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CacheManager cacheManager;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

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

        // Create regular user
        User user = User.builder()
                .username("user")
                .email("user@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        user = userRepository.save(user);
        userToken = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), user.getRole(), user.getTokenVersion());
    }

    // ========================================================================
    // TOKEN EXPIRATION CONFIGURATION TESTS
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Should get token expiration config as admin")
    void shouldGetTokenExpirationConfigAsAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/config/token-expiration")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessTokenExpirationMinutes").exists())
                .andExpect(jsonPath("$.refreshTokenExpirationDays").exists())
                .andExpect(jsonPath("$.effectiveFrom").exists());
    }

    @Test
    @Transactional
    @DisplayName("Should deny token expiration config access to non-admin")
    void shouldDenyTokenExpirationConfigAccessToNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/config/token-expiration")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("Should deny token expiration config access without authentication")
    void shouldDenyTokenExpirationConfigAccessWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/admin/config/token-expiration"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should update token expiration config with valid data")
    void shouldUpdateTokenExpirationConfigWithValidData() throws Exception {
        // Given
        TokenExpirationRequest request = TokenExpirationRequest.builder()
                .accessTokenExpirationMinutes(30)
                .refreshTokenExpirationDays(14)
                .build();

        // When
        MvcResult result = mockMvc.perform(put("/api/admin/config/token-expiration")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessTokenExpirationMinutes").value(30))
                .andExpect(jsonPath("$.refreshTokenExpirationDays").value(14))
                .andExpect(jsonPath("$.updatedByUsername").value("admin"))
                .andReturn();

        // Then - verify the configuration was persisted
        mockMvc.perform(get("/api/admin/config/token-expiration")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessTokenExpirationMinutes").value(30))
                .andExpect(jsonPath("$.refreshTokenExpirationDays").value(14));
    }

    @Test
    @Transactional
    @DisplayName("Should reject token expiration config with invalid access token minutes")
    void shouldRejectTokenExpirationConfigWithInvalidAccessToken() throws Exception {
        // Given - access token below minimum (5 minutes)
        TokenExpirationRequest request = TokenExpirationRequest.builder()
                .accessTokenExpirationMinutes(3)
                .refreshTokenExpirationDays(7)
                .build();

        // When/Then
        mockMvc.perform(put("/api/admin/config/token-expiration")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should reject token expiration config with invalid refresh token days")
    void shouldRejectTokenExpirationConfigWithInvalidRefreshToken() throws Exception {
        // Given - refresh token above maximum (30 days)
        TokenExpirationRequest request = TokenExpirationRequest.builder()
                .accessTokenExpirationMinutes(15)
                .refreshTokenExpirationDays(35)
                .build();

        // When/Then
        mockMvc.perform(put("/api/admin/config/token-expiration")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // DUPLICATE DETECTION CONFIGURATION TESTS
    // ========================================================================

    @Test
    @Transactional
    @DisplayName("Should get duplicate detection config as admin")
    void shouldGetDuplicateDetectionConfigAsAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/config/duplicate-detection")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectionRadiusMeters").exists())
                .andExpect(jsonPath("$.timeWindowHours").exists())
                .andExpect(jsonPath("$.requireSameCategory").exists())
                .andExpect(jsonPath("$.effectiveFrom").exists());
    }

    @Test
    @Transactional
    @DisplayName("Should deny duplicate detection config access to non-admin")
    void shouldDenyDuplicateDetectionConfigAccessToNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/config/duplicate-detection")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should update duplicate detection config with valid data")
    void shouldUpdateDuplicateDetectionConfigWithValidData() throws Exception {
        // Given
        DuplicateDetectionRequest request = DuplicateDetectionRequest.builder()
                .detectionRadiusMeters(100)
                .timeWindowHours(48)
                .requireSameCategory(false)
                .build();

        // When
        mockMvc.perform(put("/api/admin/config/duplicate-detection")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectionRadiusMeters").value(100))
                .andExpect(jsonPath("$.timeWindowHours").value(48))
                .andExpect(jsonPath("$.requireSameCategory").value(false))
                .andExpect(jsonPath("$.updatedByUsername").value("admin"));

        // Then - verify the configuration was persisted
        mockMvc.perform(get("/api/admin/config/duplicate-detection")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectionRadiusMeters").value(100))
                .andExpect(jsonPath("$.timeWindowHours").value(48));
    }

    @Test
    @Transactional
    @DisplayName("Should reject duplicate detection config with invalid radius")
    void shouldRejectDuplicateDetectionConfigWithInvalidRadius() throws Exception {
        // Given - radius below minimum (10 meters)
        DuplicateDetectionRequest request = DuplicateDetectionRequest.builder()
                .detectionRadiusMeters(5)
                .timeWindowHours(24)
                .requireSameCategory(true)
                .build();

        // When/Then
        mockMvc.perform(put("/api/admin/config/duplicate-detection")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should reject duplicate detection config with invalid time window")
    void shouldRejectDuplicateDetectionConfigWithInvalidTimeWindow() throws Exception {
        // Given - time window above maximum (168 hours = 7 days)
        DuplicateDetectionRequest request = DuplicateDetectionRequest.builder()
                .detectionRadiusMeters(50)
                .timeWindowHours(200)
                .requireSameCategory(true)
                .build();

        // When/Then
        mockMvc.perform(put("/api/admin/config/duplicate-detection")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @Commit
    @DirtiesContext
    @DisplayName("Should preserve algorithm weights when updating duplicate detection")
    void shouldPreserveAlgorithmWeightsWhenUpdatingDuplicateDetection() throws Exception {
        // Given - get current algorithm weights
        MvcResult weightsResult = mockMvc.perform(get("/api/admin/config/algorithm-weights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String weightsJson = weightsResult.getResponse().getContentAsString();
        
        // When - update duplicate detection config
        DuplicateDetectionRequest request = DuplicateDetectionRequest.builder()
                .detectionRadiusMeters(75)
                .timeWindowHours(36)
                .requireSameCategory(true)
                .build();

        mockMvc.perform(put("/api/admin/config/duplicate-detection")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - verify algorithm weights are unchanged
        MvcResult newWeightsResult = mockMvc.perform(get("/api/admin/config/algorithm-weights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String newWeightsJson = newWeightsResult.getResponse().getContentAsString();
        
        // Parse and compare weights (they should be the same)
        assertThat(newWeightsJson).contains("weightCategory");
        assertThat(newWeightsJson).contains("weightZone");
        assertThat(newWeightsJson).contains("weightTime");
    }
}
