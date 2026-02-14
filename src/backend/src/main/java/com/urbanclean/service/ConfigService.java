package com.urbanclean.service;

import com.urbanclean.dto.request.DuplicateDetectionRequest;
import com.urbanclean.dto.request.TokenExpirationRequest;
import com.urbanclean.dto.response.DuplicateDetectionResponse;
import com.urbanclean.dto.response.TokenExpirationResponse;
import com.urbanclean.entity.AlgorithmConfig;
import com.urbanclean.entity.User;
import com.urbanclean.repository.AlgorithmConfigRepository;
import com.urbanclean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing algorithm configuration and system settings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigService {

    private final AlgorithmConfigRepository configRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // Configuration type constants
    private static final String CONFIG_TYPE_ALGORITHM = "ALGORITHM_WEIGHTS";
    private static final String CONFIG_TYPE_TOKEN_EXPIRATION = "TOKEN_EXPIRATION";
    private static final String CONFIG_TYPE_DUPLICATE_DETECTION = "DUPLICATE_DETECTION";

    // Default weight values
    private static final BigDecimal DEFAULT_WEIGHT_CATEGORY = new BigDecimal("0.40");
    private static final BigDecimal DEFAULT_WEIGHT_ZONE = new BigDecimal("0.35");
    private static final BigDecimal DEFAULT_WEIGHT_TIME = new BigDecimal("0.25");
    private static final BigDecimal DEFAULT_DEDUP_DISTANCE = new BigDecimal("50.0");
    private static final Integer DEFAULT_DEDUP_TIME_WINDOW = 24;

    // Default token expiration values
    private static final Integer DEFAULT_ACCESS_TOKEN_EXPIRATION = 15; // minutes
    private static final Integer DEFAULT_REFRESH_TOKEN_EXPIRATION = 7; // days

    /**
     * Get current active configuration
     * If no configuration exists, create and return default configuration
     */
    @Transactional
    public AlgorithmConfig getCurrentConfig() {
        return configRepository.findCurrentConfig()
                .orElseGet(this::createDefaultConfig);
    }

    /**
     * Create default configuration
     */
    private AlgorithmConfig createDefaultConfig() {
        log.info("Creating default algorithm configuration");
        
        AlgorithmConfig config = AlgorithmConfig.builder()
                .configType("ALGORITHM_WEIGHTS")
                .weightCategory(DEFAULT_WEIGHT_CATEGORY)
                .weightZone(DEFAULT_WEIGHT_ZONE)
                .weightTime(DEFAULT_WEIGHT_TIME)
                .distanceThresholdMeters(DEFAULT_DEDUP_DISTANCE.doubleValue())
                .timeWindowHours(DEFAULT_DEDUP_TIME_WINDOW)
                .effectiveFrom(LocalDateTime.now())
                .build();

        return configRepository.save(config);
    }

    /**
     * Update algorithm weights
     * Creates a new configuration entry with current timestamp
     */
    @Transactional
    public AlgorithmConfig updateWeights(
            BigDecimal weightCategory,
            BigDecimal weightZone,
            BigDecimal weightTime,
            BigDecimal deduplicationDistance,
            Integer deduplicationTimeWindow) {
        
        // Validate weights
        validateWeights(weightCategory, weightZone, weightTime);
        validateDeduplicationParams(deduplicationDistance, deduplicationTimeWindow);

        log.info("Updating algorithm configuration: category={}, zone={}, time={}, dedupDist={}, dedupTime={}",
                weightCategory, weightZone, weightTime, deduplicationDistance, deduplicationTimeWindow);

        // Create new configuration
        AlgorithmConfig newConfig = AlgorithmConfig.builder()
                .configType("ALGORITHM_WEIGHTS")
                .weightCategory(weightCategory)
                .weightZone(weightZone)
                .weightTime(weightTime)
                .distanceThresholdMeters(deduplicationDistance.doubleValue())
                .timeWindowHours(deduplicationTimeWindow)
                .effectiveFrom(LocalDateTime.now())
                .build();

        return configRepository.save(newConfig);
    }

    /**
     * Validate weight values
     */
    private void validateWeights(BigDecimal weightCategory, BigDecimal weightZone, BigDecimal weightTime) {
        if (weightCategory == null || weightZone == null || weightTime == null) {
            throw new IllegalArgumentException("All weight values are required");
        }

        // Weights should be positive
        if (weightCategory.compareTo(BigDecimal.ZERO) <= 0 ||
            weightZone.compareTo(BigDecimal.ZERO) <= 0 ||
            weightTime.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight values must be positive");
        }

        // Weights should sum to approximately 1.0 (allow small tolerance)
        BigDecimal sum = weightCategory.add(weightZone).add(weightTime);
        BigDecimal tolerance = new BigDecimal("0.01");
        if (sum.subtract(BigDecimal.ONE).abs().compareTo(tolerance) > 0) {
            throw new IllegalArgumentException(
                String.format("Weight values must sum to 1.0 (current sum: %s)", sum)
            );
        }
    }

    /**
     * Validate deduplication parameters
     */
    private void validateDeduplicationParams(BigDecimal distance, Integer timeWindow) {
        if (distance == null || timeWindow == null) {
            throw new IllegalArgumentException("Deduplication parameters are required");
        }

        if (distance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduplication distance must be positive");
        }

        if (timeWindow <= 0) {
            throw new IllegalArgumentException("Deduplication time window must be positive");
        }
    }

    // ========================================================================
    // TOKEN EXPIRATION CONFIGURATION
    // ========================================================================

    /**
     * Get current token expiration configuration
     */
    @Cacheable(value = "tokenExpirationConfig")
    @Transactional(readOnly = true)
    public TokenExpirationResponse getTokenExpirationConfig() {
        log.debug("Fetching current token expiration configuration");
        
        AlgorithmConfig config = configRepository.findCurrentConfigByType(CONFIG_TYPE_TOKEN_EXPIRATION)
                .orElseGet(this::createDefaultTokenExpirationConfig);

        return TokenExpirationResponse.builder()
                .id(config.getId())
                .accessTokenExpirationMinutes(config.getAccessTokenExpirationMinutes())
                .refreshTokenExpirationDays(config.getRefreshTokenExpirationDays())
                .effectiveFrom(config.getEffectiveFrom())
                .updatedById(config.getUpdatedBy() != null ? config.getUpdatedBy().getId() : null)
                .updatedByUsername(config.getUpdatedBy() != null ? config.getUpdatedBy().getUsername() : null)
                .build();
    }

    /**
     * Update token expiration configuration
     */
    @CacheEvict(value = "tokenExpirationConfig", allEntries = true)
    @Transactional
    public TokenExpirationResponse updateTokenExpirationConfig(TokenExpirationRequest request) {
        log.info("Updating token expiration configuration: access={}min, refresh={}days",
                request.getAccessTokenExpirationMinutes(), request.getRefreshTokenExpirationDays());

        // Get current user
        User currentUser = getCurrentUser();

        // Create new configuration
        AlgorithmConfig newConfig = AlgorithmConfig.builder()
                .configType(CONFIG_TYPE_TOKEN_EXPIRATION)
                .accessTokenExpirationMinutes(request.getAccessTokenExpirationMinutes())
                .refreshTokenExpirationDays(request.getRefreshTokenExpirationDays())
                .effectiveFrom(LocalDateTime.now())
                .updatedBy(currentUser)
                // Set default values for unused fields
                .weightCategory(DEFAULT_WEIGHT_CATEGORY)
                .weightZone(DEFAULT_WEIGHT_ZONE)
                .weightTime(DEFAULT_WEIGHT_TIME)
                .distanceThresholdMeters(DEFAULT_DEDUP_DISTANCE.doubleValue())
                .timeWindowHours(DEFAULT_DEDUP_TIME_WINDOW)
                .build();

        AlgorithmConfig saved = configRepository.save(newConfig);

        // Audit the change
        auditConfigChange(
                "TOKEN_EXPIRATION",
                String.format("access=%dmin, refresh=%ddays", 
                        request.getAccessTokenExpirationMinutes(), 
                        request.getRefreshTokenExpirationDays()),
                currentUser.getId()
        );

        return TokenExpirationResponse.builder()
                .id(saved.getId())
                .accessTokenExpirationMinutes(saved.getAccessTokenExpirationMinutes())
                .refreshTokenExpirationDays(saved.getRefreshTokenExpirationDays())
                .effectiveFrom(saved.getEffectiveFrom())
                .updatedById(currentUser.getId())
                .updatedByUsername(currentUser.getUsername())
                .build();
    }

    /**
     * Create default token expiration configuration
     */
    private AlgorithmConfig createDefaultTokenExpirationConfig() {
        log.info("Creating default token expiration configuration");
        
        AlgorithmConfig config = AlgorithmConfig.builder()
                .configType(CONFIG_TYPE_TOKEN_EXPIRATION)
                .accessTokenExpirationMinutes(DEFAULT_ACCESS_TOKEN_EXPIRATION)
                .refreshTokenExpirationDays(DEFAULT_REFRESH_TOKEN_EXPIRATION)
                .effectiveFrom(LocalDateTime.now())
                // Set default values for unused fields
                .weightCategory(DEFAULT_WEIGHT_CATEGORY)
                .weightZone(DEFAULT_WEIGHT_ZONE)
                .weightTime(DEFAULT_WEIGHT_TIME)
                .distanceThresholdMeters(DEFAULT_DEDUP_DISTANCE.doubleValue())
                .timeWindowHours(DEFAULT_DEDUP_TIME_WINDOW)
                .build();

        return configRepository.save(config);
    }

    // ========================================================================
    // DUPLICATE DETECTION CONFIGURATION
    // ========================================================================

    /**
     * Get current duplicate detection configuration
     */
    @Cacheable(value = "duplicateDetectionConfig")
    @Transactional(readOnly = true)
    public DuplicateDetectionResponse getDuplicateDetectionConfig() {
        log.debug("Fetching current duplicate detection configuration");
        
        // For now, we use the algorithm config which already has these fields
        AlgorithmConfig config = getCurrentConfig();

        return DuplicateDetectionResponse.builder()
                .id(config.getId())
                .detectionRadiusMeters(config.getDistanceThresholdMeters().intValue())
                .timeWindowHours(config.getTimeWindowHours())
                .requireSameCategory(true) // Currently hardcoded, can be made configurable
                .effectiveFrom(config.getEffectiveFrom())
                .updatedById(config.getUpdatedBy() != null ? config.getUpdatedBy().getId() : null)
                .updatedByUsername(config.getUpdatedBy() != null ? config.getUpdatedBy().getUsername() : null)
                .build();
    }

    /**
     * Update duplicate detection configuration
     */
    @CacheEvict(value = {"duplicateDetectionConfig", "algorithmConfig"}, allEntries = true)
    @Transactional
    public DuplicateDetectionResponse updateDuplicateDetectionConfig(DuplicateDetectionRequest request) {
        log.info("Updating duplicate detection configuration: radius={}m, timeWindow={}h, sameCategory={}",
                request.getDetectionRadiusMeters(), request.getTimeWindowHours(), request.getRequireSameCategory());

        // Get current user
        User currentUser = getCurrentUser();

        // Get current algorithm config to preserve weights
        AlgorithmConfig currentConfig = getCurrentConfig();

        // Create new configuration
        AlgorithmConfig newConfig = AlgorithmConfig.builder()
                .configType(CONFIG_TYPE_ALGORITHM)
                .weightCategory(currentConfig.getWeightCategory())
                .weightZone(currentConfig.getWeightZone())
                .weightTime(currentConfig.getWeightTime())
                .distanceThresholdMeters(request.getDetectionRadiusMeters().doubleValue())
                .timeWindowHours(request.getTimeWindowHours())
                .effectiveFrom(LocalDateTime.now())
                .updatedBy(currentUser)
                .build();

        AlgorithmConfig saved = configRepository.save(newConfig);

        // Audit the change
        auditConfigChange(
                "DUPLICATE_DETECTION",
                String.format("radius=%dm, timeWindow=%dh, sameCategory=%s", 
                        request.getDetectionRadiusMeters(), 
                        request.getTimeWindowHours(),
                        request.getRequireSameCategory()),
                currentUser.getId()
        );

        return DuplicateDetectionResponse.builder()
                .id(saved.getId())
                .detectionRadiusMeters(saved.getDistanceThresholdMeters().intValue())
                .timeWindowHours(saved.getTimeWindowHours())
                .requireSameCategory(request.getRequireSameCategory())
                .effectiveFrom(saved.getEffectiveFrom())
                .updatedById(currentUser.getId())
                .updatedByUsername(currentUser.getUsername())
                .build();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }

    /**
     * Audit configuration change
     */
    private void auditConfigChange(String configType, String newValue, UUID userId) {
        log.info("Configuration change audited: type={}, value={}, userId={}", 
                configType, newValue, userId);
        // The audit is already logged, additional audit service call can be added here if needed
    }
}
