package com.urbanclean.service;

import com.urbanclean.entity.TokenBlacklist;
import com.urbanclean.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing the token blacklist.
 * Provides methods to add tokens to blacklist and check if tokens are blacklisted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    /**
     * Add a token to the blacklist.
     * 
     * @param token The token to blacklist (plaintext)
     * @param tokenType Type of token (ACCESS or REFRESH)
     * @param userId User ID
     * @param expiresAt Token expiration time
     * @param reason Revocation reason
     */
    @Transactional
    public void addToBlacklist(String token, TokenBlacklist.TokenType tokenType, UUID userId, 
                               LocalDateTime expiresAt, String reason) {
        String tokenHash = hashToken(token);

        // Check if already blacklisted
        if (tokenBlacklistRepository.existsByTokenHash(tokenHash)) {
            log.debug("Token already blacklisted: {}", tokenHash.substring(0, 8));
            return;
        }

        TokenBlacklist blacklistEntry = new TokenBlacklist();
        blacklistEntry.setTokenHash(tokenHash);
        blacklistEntry.setTokenType(tokenType);
        blacklistEntry.setUserId(userId);
        blacklistEntry.setExpiresAt(expiresAt);
        blacklistEntry.setReason(reason);

        tokenBlacklistRepository.save(blacklistEntry);

        log.info("Added {} token to blacklist for user: {}, reason: {}", tokenType, userId, reason);
    }

    /**
     * Check if a token is blacklisted.
     * 
     * @param token The token to check (plaintext)
     * @return true if blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        String tokenHash = hashToken(token);
        boolean blacklisted = tokenBlacklistRepository.existsByTokenHash(tokenHash);
        
        if (blacklisted) {
            log.warn("Attempted to use blacklisted token: {}", tokenHash.substring(0, 8));
        }
        
        return blacklisted;
    }

    /**
     * Cleanup expired blacklist entries.
     * Runs daily at 4:00 AM.
     * Removes entries older than 30 days.
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredEntries() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        tokenBlacklistRepository.deleteByExpiresAtBefore(cutoffDate);
        log.info("Cleaned up blacklist entries older than {}", cutoffDate);
    }

    /**
     * Hash a token using SHA-256.
     * 
     * @param token The token to hash
     * @return The hashed token (hex string)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
