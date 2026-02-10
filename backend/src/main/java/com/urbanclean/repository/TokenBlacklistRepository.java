package com.urbanclean.repository;

import com.urbanclean.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository for TokenBlacklist entity.
 * Provides methods for token blacklist management and cleanup.
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    /**
     * Check if a token is blacklisted.
     * Used for token validation during authentication.
     */
    boolean existsByTokenHash(String tokenHash);

    /**
     * Delete expired blacklist entries.
     * Used for cleanup operations (tokens older than 30 days).
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :date")
    void deleteByExpiresAtBefore(@Param("date") LocalDateTime date);

    /**
     * Count blacklisted tokens for a user.
     * Used for monitoring and analytics.
     */
    int countByUserId(UUID userId);

    /**
     * Find all blacklisted tokens for a user.
     * Used for admin operations and auditing.
     */
    @Query("SELECT tb FROM TokenBlacklist tb WHERE tb.userId = :userId ORDER BY tb.revokedAt DESC")
    java.util.List<TokenBlacklist> findByUserIdOrderByRevokedAtDesc(@Param("userId") UUID userId);
}
