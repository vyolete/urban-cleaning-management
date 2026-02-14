package com.urbanclean.service;

import com.urbanclean.entity.RefreshToken;
import com.urbanclean.entity.TokenBlacklist;
import com.urbanclean.repository.RefreshTokenRepository;
import com.urbanclean.repository.TokenBlacklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenService
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UUID testUserId;
    private String testDeviceFingerprint;
    private String testIpAddress;
    private String testUserAgent;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testDeviceFingerprint = "test-fingerprint-123";
        testIpAddress = "192.168.1.1";
        testUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        
        // Set refresh token expiration days
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationDays", 7);
    }

    @Test
    void testCreateRefreshToken_Success() {
        // Arrange
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String token = refreshTokenService.createRefreshToken(
                testUserId,
                testDeviceFingerprint,
                testIpAddress,
                testUserAgent
        );

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void testValidateRefreshToken_ValidToken() {
        // Arrange
        String token = "valid-token-123";
        String tokenHash = hashToken(token);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(testUserId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setDeviceFingerprint(testDeviceFingerprint);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        when(tokenBlacklistRepository.existsByTokenHash(tokenHash)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        RefreshToken result = refreshTokenService.validateRefreshToken(token);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        verify(tokenBlacklistRepository, times(1)).existsByTokenHash(tokenHash);
        verify(refreshTokenRepository, times(1)).findByTokenHash(tokenHash);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void testValidateRefreshToken_BlacklistedToken() {
        // Arrange
        String token = "blacklisted-token";
        String tokenHash = hashToken(token);

        when(tokenBlacklistRepository.existsByTokenHash(tokenHash)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            refreshTokenService.validateRefreshToken(token);
        });

        verify(tokenBlacklistRepository, times(1)).existsByTokenHash(tokenHash);
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }

    @Test
    void testValidateRefreshToken_ExpiredToken() {
        // Arrange
        String token = "expired-token";
        String tokenHash = hashToken(token);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(testUserId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired
        refreshToken.setRevoked(false);

        when(tokenBlacklistRepository.existsByTokenHash(tokenHash)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            refreshTokenService.validateRefreshToken(token);
        });
    }

    @Test
    void testValidateRefreshToken_RevokedToken() {
        // Arrange
        String token = "revoked-token";
        String tokenHash = hashToken(token);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(testUserId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(true); // Revoked

        when(tokenBlacklistRepository.existsByTokenHash(tokenHash)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            refreshTokenService.validateRefreshToken(token);
        });
    }

    @Test
    void testRevokeRefreshToken_Success() {
        // Arrange
        String token = "token-to-revoke";
        String tokenHash = hashToken(token);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(testUserId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);
        when(tokenBlacklistRepository.save(any(TokenBlacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        refreshTokenService.revokeRefreshToken(token, "TEST_REASON");

        // Assert
        verify(refreshTokenRepository, times(1)).findByTokenHash(tokenHash);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        verify(tokenBlacklistRepository, times(1)).save(any(TokenBlacklist.class));
    }

    @Test
    void testRevokeAllUserTokens_Success() {
        // Arrange
        RefreshToken token1 = createMockRefreshToken();
        RefreshToken token2 = createMockRefreshToken();
        List<RefreshToken> tokens = Arrays.asList(token1, token2);

        when(refreshTokenRepository.findByUserId(testUserId)).thenReturn(tokens);
        when(tokenBlacklistRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(tokenBlacklistRepository.save(any(TokenBlacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        refreshTokenService.revokeAllUserTokens(testUserId);

        // Assert
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(eq(testUserId), any(LocalDateTime.class));
        verify(refreshTokenRepository, times(1)).findByUserId(testUserId);
        verify(tokenBlacklistRepository, times(2)).save(any(TokenBlacklist.class));
    }

    @Test
    void testRotateRefreshToken_Success() {
        // Arrange
        String oldToken = "old-token";
        String oldTokenHash = hashToken(oldToken);
        
        RefreshToken oldRefreshToken = new RefreshToken();
        oldRefreshToken.setId(UUID.randomUUID());
        oldRefreshToken.setUserId(testUserId);
        oldRefreshToken.setTokenHash(oldTokenHash);
        oldRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        oldRefreshToken.setRevoked(false);

        when(tokenBlacklistRepository.existsByTokenHash(oldTokenHash)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(oldTokenHash)).thenReturn(Optional.of(oldRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenBlacklistRepository.save(any(TokenBlacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String newToken = refreshTokenService.rotateRefreshToken(
                oldToken,
                testDeviceFingerprint,
                testIpAddress,
                testUserAgent
        );

        // Assert
        assertNotNull(newToken);
        assertNotEquals(oldToken, newToken);
        // validateRefreshToken saves once, createRefreshToken saves once, revokeRefreshToken saves once = 3 times
        verify(refreshTokenRepository, times(3)).save(any(RefreshToken.class));
        verify(tokenBlacklistRepository, times(1)).save(any(TokenBlacklist.class));
    }

    @Test
    void testCleanupExpiredTokens() {
        // Arrange
        doNothing().when(refreshTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));

        // Act
        refreshTokenService.cleanupExpiredTokens();

        // Assert
        verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    // Helper methods

    private RefreshToken createMockRefreshToken() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUserId(testUserId);
        token.setTokenHash("hash-" + UUID.randomUUID());
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setRevoked(false);
        return token;
    }

    private String hashToken(String token) {
        // Simple hash for testing (matches the service implementation)
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
