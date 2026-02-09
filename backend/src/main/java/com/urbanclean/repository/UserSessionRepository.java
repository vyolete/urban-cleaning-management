package com.urbanclean.repository;

import com.urbanclean.entity.UserSession;
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
 * Repository for UserSession entity.
 * Provides methods for session management across devices.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * Find all active sessions for a user.
     * Used for displaying active sessions to the user.
     */
    List<UserSession> findByUserIdAndActiveTrue(UUID userId);

    /**
     * Find session by refresh token ID.
     * Used for session lookup during token refresh.
     */
    Optional<UserSession> findByRefreshTokenId(UUID refreshTokenId);

    /**
     * Find all sessions for a user (including inactive).
     * Used for admin operations and history.
     */
    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId ORDER BY us.lastActivity DESC")
    List<UserSession> findByUserIdOrderByLastActivityDesc(@Param("userId") UUID userId);

    /**
     * Count active sessions for a user.
     * Used for enforcing session limits.
     */
    int countByUserIdAndActiveTrue(UUID userId);

    /**
     * Deactivate all sessions for a user.
     * Used for logout all operations.
     */
    @Modifying
    @Query("UPDATE UserSession us SET us.active = false WHERE us.userId = :userId AND us.active = true")
    void deactivateAllByUserId(@Param("userId") UUID userId);

    /**
     * Deactivate all sessions except current.
     * Used for "logout all other devices" operation.
     */
    @Modifying
    @Query("UPDATE UserSession us SET us.active = false WHERE us.userId = :userId AND us.id != :currentSessionId AND us.active = true")
    void deactivateAllExceptCurrent(@Param("userId") UUID userId, @Param("currentSessionId") UUID currentSessionId);

    /**
     * Find oldest active session for a user.
     * Used for session limit enforcement.
     */
    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId AND us.active = true ORDER BY us.createdAt ASC")
    List<UserSession> findOldestActiveByUserId(@Param("userId") UUID userId);

    /**
     * Delete stale sessions (no activity for more than 30 days).
     * Used for cleanup operations.
     */
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.lastActivity < :date")
    void deleteStaleSessions(@Param("date") LocalDateTime date);
}
