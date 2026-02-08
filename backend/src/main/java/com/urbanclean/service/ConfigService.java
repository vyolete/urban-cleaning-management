package com.urbanclean.service;

import com.urbanclean.entity.AlgorithmConfig;
import com.urbanclean.repository.AlgorithmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for managing algorithm configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigService {

    private final AlgorithmConfigRepository configRepository;

    // Default weight values
    private static final BigDecimal DEFAULT_WEIGHT_CATEGORY = new BigDecimal("0.40");
    private static final BigDecimal DEFAULT_WEIGHT_ZONE = new BigDecimal("0.35");
    private static final BigDecimal DEFAULT_WEIGHT_TIME = new BigDecimal("0.25");
    private static final BigDecimal DEFAULT_DEDUP_DISTANCE = new BigDecimal("50.0");
    private static final Integer DEFAULT_DEDUP_TIME_WINDOW = 24;

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
                .weightCategory(DEFAULT_WEIGHT_CATEGORY)
                .weightZone(DEFAULT_WEIGHT_ZONE)
                .weightTime(DEFAULT_WEIGHT_TIME)
                .deduplicationDistanceMeters(DEFAULT_DEDUP_DISTANCE)
                .deduplicationTimeWindowHours(DEFAULT_DEDUP_TIME_WINDOW)
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
                .weightCategory(weightCategory)
                .weightZone(weightZone)
                .weightTime(weightTime)
                .deduplicationDistanceMeters(deduplicationDistance)
                .deduplicationTimeWindowHours(deduplicationTimeWindow)
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
}
