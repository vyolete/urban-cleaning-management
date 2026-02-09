package com.urbanclean.service;

import com.urbanclean.entity.PasswordResetToken;
import com.urbanclean.entity.User;
import com.urbanclean.repository.PasswordResetTokenRepository;
import com.urbanclean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Service for password reset operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    private static final int TOKEN_EXPIRATION_HOURS = 1;
    private static final int TOKEN_LENGTH_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Initiate password reset process
     * Generates a secure token and sends reset email
     * 
     * @param email User's email address
     * @param ipAddress IP address of the request (for audit)
     * @return true if email was sent, false if user not found
     */
    @Transactional
    public boolean initiatePasswordReset(String email, String ipAddress) {
        log.info("Password reset requested for email: {}", email);
        
        // Find user by email
        User user = userRepository.findByEmail(email).orElse(null);
        
        // Always return true to prevent email enumeration attacks
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return true; // Don't reveal that email doesn't exist
        }
        
        // Invalidate any existing unused tokens for this user
        List<PasswordResetToken> existingTokens = tokenRepository.findByUserAndUsedFalse(user);
        existingTokens.forEach(token -> {
            token.setUsed(true);
            token.setUsedAt(LocalDateTime.now());
        });
        tokenRepository.saveAll(existingTokens);
        
        // Generate secure random token
        byte[] tokenBytes = new byte[TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Create token entity
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .user(user)
            .expiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS))
            .used(false)
            .ipAddress(ipAddress)
            .build();
        
        tokenRepository.save(resetToken);
        
        // Send email asynchronously
        emailService.sendPasswordResetEmail(user.getEmail(), token);
        
        log.info("Password reset token generated for user: {}", user.getUsername());
        return true;
    }

    /**
     * Validate a password reset token
     * 
     * @param token The token to validate
     * @return PasswordResetToken if valid, null otherwise
     */
    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String token) {
        log.debug("Validating password reset token");
        
        PasswordResetToken resetToken = tokenRepository.findByToken(token).orElse(null);
        
        if (resetToken == null) {
            log.warn("Invalid password reset token attempted");
            return null;
        }
        
        if (!resetToken.isValid()) {
            log.warn("Expired or used password reset token attempted for user: {}", 
                resetToken.getUser().getUsername());
            return null;
        }
        
        return resetToken;
    }

    /**
     * Reset password using a valid token
     * 
     * @param token The reset token
     * @param newPassword The new password
     * @param ipAddress IP address of the request (for audit)
     * @return true if password was reset successfully
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword, String ipAddress) {
        log.info("Attempting password reset with token");
        
        // Validate token
        PasswordResetToken resetToken = validateToken(token);
        if (resetToken == null) {
            return false;
        }
        
        // Get user
        User user = resetToken.getUser();
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        
        // Increment token version to invalidate all existing JWTs
        Integer currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        user.setTokenVersion(currentVersion + 1);
        
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        resetToken.setIpAddress(ipAddress);
        tokenRepository.save(resetToken);
        
        log.info("Password successfully reset for user: {}. Token version incremented to: {}", 
            user.getUsername(), user.getTokenVersion());
        return true;
    }

    /**
     * Cleanup expired tokens
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired password reset tokens");
        
        int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        
        log.info("Cleaned up {} expired password reset tokens", deletedCount);
    }
}
