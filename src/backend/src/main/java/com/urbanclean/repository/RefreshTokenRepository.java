package com.urbanclean.repository;

import com.urbanclean.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity.
 * Provides methods for token management and cleanup.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its hash.
     * Used for token validation during refresh operations.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all non-revoked refresh tokens for a user.
     * Used for session management and logout operations.
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    /**
     * Count non-revoked refresh tokens for a user.
     * Used for enforcing session limits.
     */
    int countByUserIdAndRevokedFalse(UUID userId);

    /**
     * Delete expired refresh tokens.
     * Used for cleanup operations.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :date")
    void deleteByExpiresAtBefore(@Param("date") LocalDateTime date);

    /**
     * Find all refresh tokens for a user (including revoked).
     * Used for admin operations.
     */
    List<RefreshToken> findByUserId(UUID userId);

    /**
     * Revoke all refresh tokens for a user.
     * Used for logout all and security operations.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.userId = :userId AND rt.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * Find oldest active refresh token for a user.
     * Used for session limit enforcement.
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false ORDER BY rt.createdAt ASC")
    List<RefreshToken> findOldestByUserId(@Param("userId") UUID userId);
}
