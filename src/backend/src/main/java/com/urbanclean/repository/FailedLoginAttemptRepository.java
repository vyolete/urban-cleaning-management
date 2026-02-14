package com.urbanclean.repository;

import com.urbanclean.entity.FailedLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for FailedLoginAttempt entity
 */
@Repository
public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, UUID> {

    /**
     * Count failed attempts for a username within a time window
     */
    @Query("SELECT COUNT(f) FROM FailedLoginAttempt f WHERE f.username = :username AND f.attemptedAt >= :since")
    long countByUsernameAndAttemptedAtAfter(@Param("username") String username, @Param("since") LocalDateTime since);

    /**
     * Count failed attempts from an IP address within a time window
     */
    @Query("SELECT COUNT(f) FROM FailedLoginAttempt f WHERE f.ipAddress = :ipAddress AND f.attemptedAt >= :since")
    long countByIpAddressAndAttemptedAtAfter(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Find recent failed attempts by username
     */
    List<FailedLoginAttempt> findByUsernameOrderByAttemptedAtDesc(String username);

    /**
     * Find recent failed attempts by IP address
     */
    List<FailedLoginAttempt> findByIpAddressOrderByAttemptedAtDesc(String ipAddress);

    /**
     * Find flagged attempts
     */
    List<FailedLoginAttempt> findByFlaggedTrueOrderByAttemptedAtDesc();

    /**
     * Delete old failed attempts (cleanup)
     */
    void deleteByAttemptedAtBefore(LocalDateTime before);
}
