package com.urbanclean.repository;

import com.urbanclean.entity.PasswordResetToken;
import com.urbanclean.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for password reset token operations
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find a token by its value
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find all unused tokens for a user
     */
    List<PasswordResetToken> findByUserAndUsedFalse(User user);

    /**
     * Delete all expired tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(LocalDateTime now);

    /**
     * Delete all tokens for a user
     */
    void deleteByUser(User user);
}
