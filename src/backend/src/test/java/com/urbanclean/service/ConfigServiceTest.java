package com.urbanclean.service;

import com.urbanclean.dto.request.DuplicateDetectionRequest;
import com.urbanclean.dto.request.TokenExpirationRequest;
import com.urbanclean.dto.response.DuplicateDetectionResponse;
import com.urbanclean.dto.response.TokenExpirationResponse;
import com.urbanclean.entity.AlgorithmConfig;
import com.urbanclean.entity.User;
import com.urbanclean.repository.AlgorithmConfigRepository;
import com.urbanclean.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfigService
 * Tests configuration management for tokens and duplicate detection
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigService Unit Tests")
class ConfigServiceTest {

    @Mock
    private AlgorithmConfigRepository configRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ConfigService configService;

    private User testUser;
    private AlgorithmConfig testConfig;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("admin");
        testUser.setEmail("admin@test.com");

        // Setup test config
        testConfig = AlgorithmConfig.builder()
                .id(UUID.randomUUID())
                .weightCategory(new BigDecimal("0.40"))
                .weightZone(new BigDecimal("0.35"))
                .weightTime(new BigDecimal("0.25"))
                .distanceThresholdMeters(50.0)
                .timeWindowHours(24)
                .accessTokenExpirationMinutes(15)
                .refreshTokenExpirationDays(7)
                .effectiveFrom(LocalDateTime.now())
                .updatedBy(testUser)
                .build();
    }

    private void setupSecurityContext() {
        when(authentication.getName()).thenReturn("admin");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ========================================================================
    // TOKEN EXPIRATION CONFIGURATION TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get token expiration config when exists")
    void shouldGetTokenExpirationConfigWhenExists() {
        // Given
        when(configRepository.findCurrentConfigByType("TOKEN_EXPIRATION"))
                .thenReturn(Optional.of(testConfig));

        // When
        TokenExpirationResponse response = configService.getTokenExpirationConfig();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessTokenExpirationMinutes()).isEqualTo(15);
        assertThat(response.getRefreshTokenExpirationDays()).isEqualTo(7);
        assertThat(response.getUpdatedById()).isEqualTo(testUser.getId());
        assertThat(response.getUpdatedByUsername()).isEqualTo("admin");

        verify(configRepository).findCurrentConfigByType("TOKEN_EXPIRATION");
        verifyNoMoreInteractions(configRepository);
    }

    @Test
    @DisplayName("Should create default token expiration config when not exists")
    void shouldCreateDefaultTokenExpirationConfigWhenNotExists() {
        // Given
        when(configRepository.findCurrentConfigByType("TOKEN_EXPIRATION"))
                .thenReturn(Optional.empty());
        when(configRepository.save(any(AlgorithmConfig.class)))
                .thenReturn(testConfig);

        // When
        TokenExpirationResponse response = configService.getTokenExpirationConfig();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessTokenExpirationMinutes()).isEqualTo(15);
        assertThat(response.getRefreshTokenExpirationDays()).isEqualTo(7);

        verify(configRepository).findCurrentConfigByType("TOKEN_EXPIRATION");
        verify(configRepository).save(any(AlgorithmConfig.class));
    }

    @Test
    @DisplayName("Should update token expiration config with valid data")
    void shouldUpdateTokenExpirationConfigWithValidData() {
        // Given
        setupSecurityContext();
        TokenExpirationRequest request = TokenExpirationRequest.builder()
                .accessTokenExpirationMinutes(30)
                .refreshTokenExpirationDays(14)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(configRepository.save(any(AlgorithmConfig.class))).thenReturn(testConfig);

        // When
        TokenExpirationResponse response = configService.updateTokenExpirationConfig(request);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).findByUsername("admin");
        verify(configRepository).save(any(AlgorithmConfig.class));
    }

    @Test
    @DisplayName("Should throw exception when updating token config without authenticated user")
    void shouldThrowExceptionWhenUpdatingTokenConfigWithoutUser() {
        // Given
        setupSecurityContext();
        TokenExpirationRequest request = TokenExpirationRequest.builder()
                .accessTokenExpirationMinutes(30)
                .refreshTokenExpirationDays(14)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> configService.updateTokenExpirationConfig(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Current user not found");

        verify(userRepository).findByUsername("admin");
        verifyNoInteractions(configRepository);
    }

    // ========================================================================
    // DUPLICATE DETECTION CONFIGURATION TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get duplicate detection config")
    void shouldGetDuplicateDetectionConfig() {
        // Given
        when(configRepository.findCurrentConfig()).thenReturn(Optional.of(testConfig));

        // When
        DuplicateDetectionResponse response = configService.getDuplicateDetectionConfig();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDetectionRadiusMeters()).isEqualTo(50);
        assertThat(response.getTimeWindowHours()).isEqualTo(24);
        assertThat(response.getRequireSameCategory()).isTrue();
        assertThat(response.getUpdatedById()).isEqualTo(testUser.getId());

        verify(configRepository).findCurrentConfig();
    }

    @Test
    @DisplayName("Should update duplicate detection config with valid data")
    void shouldUpdateDuplicateDetectionConfigWithValidData() {
        // Given
        setupSecurityContext();
        DuplicateDetectionRequest request = DuplicateDetectionRequest.builder()
                .detectionRadiusMeters(100)
                .timeWindowHours(48)
                .requireSameCategory(false)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(configRepository.findCurrentConfig()).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any(AlgorithmConfig.class))).thenReturn(testConfig);

        // When
        DuplicateDetectionResponse response = configService.updateDuplicateDetectionConfig(request);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).findByUsername("admin");
        verify(configRepository).findCurrentConfig();
        verify(configRepository).save(any(AlgorithmConfig.class));
    }

    @Test
    @DisplayName("Should preserve algorithm weights when updating duplicate detection")
    void shouldPreserveAlgorithmWeightsWhenUpdatingDuplicateDetection() {
        // Given
        setupSecurityContext();
        DuplicateDetectionRequest request = DuplicateDetectionRequest.builder()
                .detectionRadiusMeters(100)
                .timeWindowHours(48)
                .requireSameCategory(true)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(configRepository.findCurrentConfig()).thenReturn(Optional.of(testConfig));
        
        AlgorithmConfig savedConfig = AlgorithmConfig.builder()
                .id(UUID.randomUUID())
                .weightCategory(testConfig.getWeightCategory())
                .weightZone(testConfig.getWeightZone())
                .weightTime(testConfig.getWeightTime())
                .distanceThresholdMeters(100.0)
                .timeWindowHours(48)
                .effectiveFrom(LocalDateTime.now())
                .updatedBy(testUser)
                .build();
        
        when(configRepository.save(any(AlgorithmConfig.class))).thenReturn(savedConfig);

        // When
        DuplicateDetectionResponse response = configService.updateDuplicateDetectionConfig(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDetectionRadiusMeters()).isEqualTo(100);
        assertThat(response.getTimeWindowHours()).isEqualTo(48);
        
        // Verify that weights were preserved
        verify(configRepository).findCurrentConfig();
        verify(configRepository).save(argThat(config -> 
            config.getWeightCategory().equals(testConfig.getWeightCategory()) &&
            config.getWeightZone().equals(testConfig.getWeightZone()) &&
            config.getWeightTime().equals(testConfig.getWeightTime())
        ));
    }

    // ========================================================================
    // ALGORITHM WEIGHTS TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get current config or create default")
    void shouldGetCurrentConfigOrCreateDefault() {
        // Given
        when(configRepository.findCurrentConfig()).thenReturn(Optional.empty());
        when(configRepository.save(any(AlgorithmConfig.class))).thenReturn(testConfig);

        // When
        AlgorithmConfig result = configService.getCurrentConfig();

        // Then
        assertThat(result).isNotNull();
        verify(configRepository).findCurrentConfig();
        verify(configRepository).save(any(AlgorithmConfig.class));
    }

    @Test
    @DisplayName("Should update weights with valid data")
    void shouldUpdateWeightsWithValidData() {
        // Given
        BigDecimal weightCategory = new BigDecimal("0.50");
        BigDecimal weightZone = new BigDecimal("0.30");
        BigDecimal weightTime = new BigDecimal("0.20");
        BigDecimal dedupDistance = new BigDecimal("75.0");
        Integer dedupTimeWindow = 36;

        when(configRepository.save(any(AlgorithmConfig.class))).thenReturn(testConfig);

        // When
        AlgorithmConfig result = configService.updateWeights(
                weightCategory, weightZone, weightTime, dedupDistance, dedupTimeWindow);

        // Then
        assertThat(result).isNotNull();
        verify(configRepository).save(any(AlgorithmConfig.class));
    }

    @Test
    @DisplayName("Should throw exception when weights don't sum to 1.0")
    void shouldThrowExceptionWhenWeightsDontSumToOne() {
        // Given
        BigDecimal weightCategory = new BigDecimal("0.50");
        BigDecimal weightZone = new BigDecimal("0.30");
        BigDecimal weightTime = new BigDecimal("0.30"); // Sum = 1.10
        BigDecimal dedupDistance = new BigDecimal("75.0");
        Integer dedupTimeWindow = 36;

        // When/Then
        assertThatThrownBy(() -> configService.updateWeights(
                weightCategory, weightZone, weightTime, dedupDistance, dedupTimeWindow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weight values must sum to 1.0");

        verifyNoInteractions(configRepository);
    }

    @Test
    @DisplayName("Should throw exception when weights are negative")
    void shouldThrowExceptionWhenWeightsAreNegative() {
        // Given
        BigDecimal weightCategory = new BigDecimal("-0.40");
        BigDecimal weightZone = new BigDecimal("0.70");
        BigDecimal weightTime = new BigDecimal("0.70");
        BigDecimal dedupDistance = new BigDecimal("75.0");
        Integer dedupTimeWindow = 36;

        // When/Then
        assertThatThrownBy(() -> configService.updateWeights(
                weightCategory, weightZone, weightTime, dedupDistance, dedupTimeWindow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weight values must be positive");

        verifyNoInteractions(configRepository);
    }

    @Test
    @DisplayName("Should throw exception when weights are null")
    void shouldThrowExceptionWhenWeightsAreNull() {
        // Given
        BigDecimal dedupDistance = new BigDecimal("75.0");
        Integer dedupTimeWindow = 36;

        // When/Then
        assertThatThrownBy(() -> configService.updateWeights(
                null, null, null, dedupDistance, dedupTimeWindow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("All weight values are required");

        verifyNoInteractions(configRepository);
    }

    @Test
    @DisplayName("Should throw exception when deduplication distance is negative")
    void shouldThrowExceptionWhenDeduplicationDistanceIsNegative() {
        // Given
        BigDecimal weightCategory = new BigDecimal("0.40");
        BigDecimal weightZone = new BigDecimal("0.35");
        BigDecimal weightTime = new BigDecimal("0.25");
        BigDecimal dedupDistance = new BigDecimal("-50.0");
        Integer dedupTimeWindow = 36;

        // When/Then
        assertThatThrownBy(() -> configService.updateWeights(
                weightCategory, weightZone, weightTime, dedupDistance, dedupTimeWindow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deduplication distance must be positive");

        verifyNoInteractions(configRepository);
    }

    @Test
    @DisplayName("Should throw exception when deduplication time window is zero or negative")
    void shouldThrowExceptionWhenDeduplicationTimeWindowIsInvalid() {
        // Given
        BigDecimal weightCategory = new BigDecimal("0.40");
        BigDecimal weightZone = new BigDecimal("0.35");
        BigDecimal weightTime = new BigDecimal("0.25");
        BigDecimal dedupDistance = new BigDecimal("50.0");
        Integer dedupTimeWindow = 0;

        // When/Then
        assertThatThrownBy(() -> configService.updateWeights(
                weightCategory, weightZone, weightTime, dedupDistance, dedupTimeWindow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deduplication time window must be positive");

        verifyNoInteractions(configRepository);
    }
}
