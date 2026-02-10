package com.urbanclean.service;

import com.urbanclean.entity.RefreshToken;
import com.urbanclean.entity.TokenBlacklist;
import com.urbanclean.repository.RefreshTokenRepository;
import com.urbanclean.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing refresh tokens.
 * Implements token rotation, validation, and cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token-expiration-days:7}")
    private int refreshTokenExpirationDays;

    /**
     * Create a new refresh token for a user.
     * 
     * @param userId User ID
     * @param deviceFingerprint Device fingerprint
     * @param ipAddress IP address
     * @param userAgent User agent string
     * @return The generated refresh token (plaintext)
     */
    @Transactional
    public String createRefreshToken(UUID userId, String deviceFingerprint, String ipAddress, String userAgent) {
        // Generate random token
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash the token for storage
        String tokenHash = hashToken(token);

        // Create refresh token entity
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setDeviceFingerprint(deviceFingerprint);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        log.info("Created refresh token for user: {}, device: {}", userId, deviceFingerprint);

        return token; // Return plaintext token to client
    }

    /**
     * Validate a refresh token.
     * 
     * @param token The refresh token (plaintext)
     * @return The RefreshToken entity if valid
     * @throws IllegalArgumentException if token is invalid
     */
    @Transactional
    public RefreshToken validateRefreshToken(String token) {
        String tokenHash = hashToken(token);

        // Check if token is blacklisted
        if (tokenBlacklistRepository.existsByTokenHash(tokenHash)) {
            log.warn("Attempted to use blacklisted refresh token");
            throw new IllegalArgumentException("Token has been revoked");
        }

        // Find token in database
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Check if token is valid
        if (!refreshToken.isValid()) {
            log.warn("Attempted to use invalid refresh token: revoked={}, expired={}", 
                    refreshToken.getRevoked(), refreshToken.isExpired());
            throw new IllegalArgumentException("Token is invalid or expired");
        }

        // Update last used timestamp
        refreshToken.updateLastUsed();
        refreshTokenRepository.save(refreshToken);

        log.debug("Validated refresh token for user: {}", refreshToken.getUserId());

        return refreshToken;
    }

    /**
     * Rotate a refresh token (create new, revoke old).
     * Implements token rotation for enhanced security.
     * 
     * @param oldToken The old refresh token
     * @param deviceFingerprint Device fingerprint
     * @param ipAddress IP address
     * @param userAgent User agent
     * @return The new refresh token (plaintext)
     */
    @Transactional
    public String rotateRefreshToken(String oldToken, String deviceFingerprint, String ipAddress, String userAgent) {
        // Validate old token
        RefreshToken oldRefreshToken = validateRefreshToken(oldToken);

        // Create new token
        String newToken = createRefreshToken(
                oldRefreshToken.getUserId(),
                deviceFingerprint,
                ipAddress,
                userAgent
        );

        // Revoke old token
        revokeRefreshToken(oldToken, TokenBlacklist.RevocationReason.TOKEN_ROTATION.name());

        log.info("Rotated refresh token for user: {}", oldRefreshToken.getUserId());

        return newToken;
    }

    /**
     * Revoke a refresh token.
     * 
     * @param token The refresh token to revoke
     * @param reason Revocation reason
     */
    @Transactional
    public void revokeRefreshToken(String token, String reason) {
        String tokenHash = hashToken(token);

        // Find and revoke token
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);

            // Add to blacklist
            TokenBlacklist blacklistEntry = new TokenBlacklist();
            blacklistEntry.setTokenHash(tokenHash);
            blacklistEntry.setTokenType(TokenBlacklist.TokenType.REFRESH);
            blacklistEntry.setUserId(refreshToken.getUserId());
            blacklistEntry.setExpiresAt(refreshToken.getExpiresAt());
            blacklistEntry.setReason(reason);
            tokenBlacklistRepository.save(blacklistEntry);

            log.info("Revoked refresh token for user: {}, reason: {}", refreshToken.getUserId(), reason);
        });
    }

    /**
     * Revoke all refresh tokens for a user.
     * Used for logout all and security operations.
     * 
     * @param userId User ID
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.revokeAllByUserId(userId, now);

        // Add all tokens to blacklist
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        for (RefreshToken token : tokens) {
            if (!tokenBlacklistRepository.existsByTokenHash(token.getTokenHash())) {
                TokenBlacklist blacklistEntry = new TokenBlacklist();
                blacklistEntry.setTokenHash(token.getTokenHash());
                blacklistEntry.setTokenType(TokenBlacklist.TokenType.REFRESH);
                blacklistEntry.setUserId(userId);
                blacklistEntry.setExpiresAt(token.getExpiresAt());
                blacklistEntry.setReason(TokenBlacklist.RevocationReason.LOGOUT.name());
                tokenBlacklistRepository.save(blacklistEntry);
            }
        }

        log.info("Revoked all refresh tokens for user: {}", userId);
    }

    /**
     * Cleanup expired refresh tokens.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        refreshTokenRepository.deleteByExpiresAtBefore(cutoffDate);
        log.info("Cleaned up expired refresh tokens older than {}", cutoffDate);
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
