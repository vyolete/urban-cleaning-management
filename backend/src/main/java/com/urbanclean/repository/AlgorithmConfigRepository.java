package com.urbanclean.repository;

import com.urbanclean.entity.AlgorithmConfig;
import com.urbanclean.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AlgorithmConfig entity operations
 */
@Repository
public interface AlgorithmConfigRepository extends JpaRepository<AlgorithmConfig, UUID> {

    /**
     * Find the current active configuration
     * @return Optional containing the current configuration
     */
    @Query("SELECT c FROM AlgorithmConfig c WHERE " +
           "c.effectiveFrom <= CURRENT_TIMESTAMP AND " +
           "(c.effectiveTo IS NULL OR c.effectiveTo > CURRENT_TIMESTAMP) " +
           "ORDER BY c.effectiveFrom DESC")
    Optional<AlgorithmConfig> findCurrentConfig();

    /**
     * Find the current active configuration by type
     * @param configType the type of configuration
     * @return Optional containing the current configuration
     */
    @Query("SELECT c FROM AlgorithmConfig c WHERE " +
           "c.configType = :configType AND " +
           "c.effectiveFrom <= CURRENT_TIMESTAMP AND " +
           "(c.effectiveTo IS NULL OR c.effectiveTo > CURRENT_TIMESTAMP) " +
           "ORDER BY c.effectiveFrom DESC")
    Optional<AlgorithmConfig> findCurrentConfigByType(@Param("configType") String configType);

    /**
     * Find configuration effective at a specific time
     * @param timestamp the time to check
     * @return Optional containing the configuration
     */
    @Query("SELECT c FROM AlgorithmConfig c WHERE " +
           "c.effectiveFrom <= :timestamp AND " +
           "(c.effectiveTo IS NULL OR c.effectiveTo > :timestamp) " +
           "ORDER BY c.effectiveFrom DESC")
    Optional<AlgorithmConfig> findConfigAt(@Param("timestamp") LocalDateTime timestamp);

    /**
     * Find all historical configurations ordered by effective date
     * @return list of all configurations
     */
    List<AlgorithmConfig> findAllByOrderByEffectiveFromDesc();

    /**
     * Find configurations created by a specific user
     * @param user the user who created the configurations
     * @return list of configurations
     */
    List<AlgorithmConfig> findByCreatedByOrderByEffectiveFromDesc(User user);

    /**
     * Find configurations within a time range
     * @param start the start of the time range
     * @param end the end of the time range
     * @return list of configurations
     */
    List<AlgorithmConfig> findByEffectiveFromBetweenOrderByEffectiveFromDesc(
        LocalDateTime start,
        LocalDateTime end
    );
}
