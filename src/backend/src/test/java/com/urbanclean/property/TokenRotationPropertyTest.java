package com.urbanclean.property;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.urbanclean.entity.RefreshToken;
import com.urbanclean.entity.TokenBlacklist;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.repository.RefreshTokenRepository;
import com.urbanclean.repository.TokenBlacklistRepository;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.RefreshTokenService;
import com.urbanclean.service.TokenBlacklistService;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Property-based tests for token rotation.
 * Verifies universal properties that should hold for all token rotations.
 * 
 * Feature: urban-cleaning-management
 * Property: Token rotation atomicity and security
 */
@RunWith(JUnitQuickcheck.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("Feature: urban-cleaning-management, Property: Token rotation atomicity")
public class TokenRotationPropertyTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Property 1: Token rotation always creates a new token and blacklists the old one.
     * 
     * Universal property: For any valid refresh token, rotation should:
     * 1. Create a new valid token
     * 2. Blacklist the old token
     * 3. The new token should be different from the old token
     * 4. Both operations should succeed atomically
     */
    @Property(trials = 100)
    @Tag("Feature: urban-cleaning-management, Property 1: Token rotation creates new token and blacklists old")
    public void tokenRotationCreatesNewTokenAndBlacklistsOld(
            @From(UserAgentGenerator.class) String userAgent,
            @From(IpAddressGenerator.class) String ipAddress,
            @From(DeviceFingerprintGenerator.class) String deviceFingerprint) {
        
        // Arrange - Create test user
        User testUser = createTestUser();
        
        // Create initial refresh token
        String oldToken = refreshTokenService.createRefreshToken(
                testUser.getId(),
                deviceFingerprint,
                ipAddress,
                userAgent
        );
        
        // Verify old token is valid
        RefreshToken oldRefreshToken = refreshTokenService.validateRefreshToken(oldToken);
        assertThat(oldRefreshToken).isNotNull();
        assertThat(oldRefreshToken.getRevoked()).isFalse();
        
        // Act - Rotate token
        String newToken = refreshTokenService.rotateRefreshToken(
                oldToken,
                deviceFingerprint,
                ipAddress,
                userAgent
        );
        
        // Assert - Property 1: New token created
        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEmpty();
        
        // Property 2: New token is different from old token
        assertThat(newToken).isNotEqualTo(oldToken);
        
        // Property 3: New token is valid
        RefreshToken newRefreshToken = refreshTokenService.validateRefreshToken(newToken);
        assertThat(newRefreshToken).isNotNull();
        assertThat(newRefreshToken.getRevoked()).isFalse();
        
        // Property 4: Old token is blacklisted
        boolean oldTokenBlacklisted = tokenBlacklistService.isBlacklisted(oldToken);
        assertThat(oldTokenBlacklisted).isTrue();
        
        // Property 5: Old token cannot be validated anymore
        try {
            refreshTokenService.validateRefreshToken(oldToken);
            assertThat(false).as("Old token should not be valid after rotation").isTrue();
        } catch (IllegalArgumentException e) {
            // Expected - old token should be invalid
            assertThat(e.getMessage()).contains("revoked");
        }
        
        // Property 6: User has exactly 2 refresh tokens (old revoked, new active)
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(testUser.getId());
        assertThat(userTokens).hasSize(2);
        assertThat(userTokens.stream().filter(RefreshToken::getRevoked).count()).isEqualTo(1);
        assertThat(userTokens.stream().filter(t -> !t.getRevoked()).count()).isEqualTo(1);
        
        // Cleanup
        cleanupTestUser(testUser);
    }

    /**
     * Property 2: Multiple consecutive rotations maintain atomicity.
     * 
     * Universal property: Rotating a token multiple times should:
     * 1. Always produce a new unique token
     * 2. Blacklist all previous tokens
     * 3. Only the latest token should be valid
     */
    @Property(trials = 50)
    @Tag("Feature: urban-cleaning-management, Property 2: Multiple rotations maintain atomicity")
    public void multipleRotationsMaintainAtomicity(
            @From(UserAgentGenerator.class) String userAgent,
            @From(IpAddressGenerator.class) String ipAddress,
            @From(DeviceFingerprintGenerator.class) String deviceFingerprint) {
        
        // Arrange
        User testUser = createTestUser();
        
        // Create initial token
        String token1 = refreshTokenService.createRefreshToken(
                testUser.getId(),
                deviceFingerprint,
                ipAddress,
                userAgent
        );
        
        // Act - Rotate 3 times
        String token2 = refreshTokenService.rotateRefreshToken(
                token1,
                deviceFingerprint,
                ipAddress,
                userAgent
        );
        
        String token3 = refreshTokenService.rotateRefreshToken(
                token2,
                deviceFingerprint,
                ipAddress,
                userAgent
        );
        
        String token4 = refreshTokenService.rotateRefreshToken(
                token3,
                deviceFingerprint,
                ipAddress,
                userAgent
        );
        
        // Assert - Property 1: All tokens are unique
        assertThat(token1).isNotEqualTo(token2);
        assertThat(token2).isNotEqualTo(token3);
        assertThat(token3).isNotEqualTo(token4);
        assertThat(token1).isNotEqualTo(token3);
        assertThat(token1).isNotEqualTo(token4);
        assertThat(token2).isNotEqualTo(token4);
        
        // Property 2: Only the latest token is valid
        RefreshToken latestToken = refreshTokenService.validateRefreshToken(token4);
        assertThat(latestToken).isNotNull();
        assertThat(latestToken.getRevoked()).isFalse();
        
        // Property 3: All previous tokens are blacklisted
        assertThat(tokenBlacklistService.isBlacklisted(token1)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(token2)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(token3)).isTrue();
        
        // Property 4: Previous tokens cannot be validated
        for (String oldToken : new String[]{token1, token2, token3}) {
            try {
                refreshTokenService.validateRefreshToken(oldToken);
                assertThat(false).as("Old token should not be valid").isTrue();
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }
        
        // Property 5: User has exactly 4 tokens (3 revoked, 1 active)
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(testUser.getId());
        assertThat(userTokens).hasSize(4);
        assertThat(userTokens.stream().filter(RefreshToken::getRevoked).count()).isEqualTo(3);
        assertThat(userTokens.stream().filter(t -> !t.getRevoked()).count()).isEqualTo(1);
        
        // Cleanup
        cleanupTestUser(testUser);
    }

    /**
     * Property 3: Token rotation with different device fingerprints.
     * 
     * Universal property: Rotating a token with a different device fingerprint should:
     * 1. Still create a new valid token
     * 2. Blacklist the old token
     * 3. Associate new token with new device fingerprint
     */
    @Property(trials = 100)
    @Tag("Feature: urban-cleaning-management, Property 3: Token rotation with different devices")
    public void tokenRotationWithDifferentDevices(
            @From(UserAgentGenerator.class) String userAgent1,
            @From(UserAgentGenerator.class) String userAgent2,
            @From(IpAddressGenerator.class) String ipAddress1,
            @From(IpAddressGenerator.class) String ipAddress2,
            @From(DeviceFingerprintGenerator.class) String fingerprint1,
            @From(DeviceFingerprintGenerator.class) String fingerprint2) {
        
        // Assume fingerprints are different
        assumeTrue(!fingerprint1.equals(fingerprint2));
        
        // Arrange
        User testUser = createTestUser();
        
        // Create token with device 1
        String token1 = refreshTokenService.createRefreshToken(
                testUser.getId(),
                fingerprint1,
                ipAddress1,
                userAgent1
        );
        
        RefreshToken refreshToken1 = refreshTokenService.validateRefreshToken(token1);
        assertThat(refreshToken1.getDeviceFingerprint()).isEqualTo(fingerprint1);
        
        // Act - Rotate with device 2
        String token2 = refreshTokenService.rotateRefreshToken(
                token1,
                fingerprint2,
                ipAddress2,
                userAgent2
        );
        
        // Assert - Property 1: New token created and valid
        RefreshToken refreshToken2 = refreshTokenService.validateRefreshToken(token2);
        assertThat(refreshToken2).isNotNull();
        assertThat(refreshToken2.getRevoked()).isFalse();
        
        // Property 2: New token has new device fingerprint
        assertThat(refreshToken2.getDeviceFingerprint()).isEqualTo(fingerprint2);
        
        // Property 3: Old token blacklisted
        assertThat(tokenBlacklistService.isBlacklisted(token1)).isTrue();
        
        // Property 4: Tokens are different
        assertThat(token1).isNotEqualTo(token2);
        
        // Cleanup
        cleanupTestUser(testUser);
    }

    /**
     * Property 4: Concurrent token rotation attempts should be handled safely.
     * 
     * Universal property: Attempting to rotate the same token twice should:
     * 1. First rotation succeeds
     * 2. Second rotation fails (token already revoked)
     * 3. No inconsistent state
     */
    @Property(trials = 50)
    @Tag("Feature: urban-cleaning-management, Property 4: Concurrent rotation safety")
    public void concurrentRotationAttemptsSafe(
            @From(UserAgentGenerator.class) String userAgent,
            @From(IpAddressGenerator.class) String ipAddress,
            @From(DeviceFingerprintGenerator.class) String deviceFingerprint) {
        
        // Arrange
        User testUser = createTestUser();
        
        String originalToken = refreshTokenService.createRefreshToken(
                testUser.getId(),
                deviceFingerprint,
                ipAddress,
                userAgent
        );
        
        // Act - First rotation succeeds
        String newToken = refreshTokenService.rotateRefreshToken(
                originalToken,
                deviceFingerprint,
                ipAddress,
                userAgent
        );
        
        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(originalToken);
        
        // Second rotation with same original token should fail
        try {
            refreshTokenService.rotateRefreshToken(
                    originalToken,
                    deviceFingerprint,
                    ipAddress,
                    userAgent
            );
            assertThat(false).as("Second rotation should fail").isTrue();
        } catch (IllegalArgumentException e) {
            // Expected - token already revoked
            assertThat(e.getMessage()).containsAnyOf("revoked", "invalid");
        }
        
        // Assert - Property: System is in consistent state
        // Only the new token should be valid
        RefreshToken validToken = refreshTokenService.validateRefreshToken(newToken);
        assertThat(validToken).isNotNull();
        assertThat(validToken.getRevoked()).isFalse();
        
        // Original token should be blacklisted
        assertThat(tokenBlacklistService.isBlacklisted(originalToken)).isTrue();
        
        // Cleanup
        cleanupTestUser(testUser);
    }

    // Helper methods
    
    private User createTestUser() {
        User user = User.builder()
                .username("proptest-" + UUID.randomUUID().toString().substring(0, 8))
                .email("proptest-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        return userRepository.save(user);
    }
    
    private void cleanupTestUser(User user) {
        // Cleanup is handled by @Transactional rollback
    }
}
