package com.urbanclean.service;

import com.urbanclean.entity.TokenBlacklist;
import com.urbanclean.repository.TokenBlacklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenBlacklistService
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private UUID testUserId;
    private String testToken;
    private String testTokenHash;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testToken = "test-token-123";
        testTokenHash = hashToken(testToken);
    }

    @Test
    void testAddToBlacklist_Success() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        String reason = "TEST_LOGOUT";

        when(tokenBlacklistRepository.existsByTokenHash(testTokenHash)).thenReturn(false);
        when(tokenBlacklistRepository.save(any(TokenBlacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        tokenBlacklistService.addToBlacklist(
                testToken,
                TokenBlacklist.TokenType.ACCESS,
                testUserId,
                expiresAt,
                reason
        );

        // Assert
        verify(tokenBlacklistRepository, times(1)).existsByTokenHash(testTokenHash);
        verify(tokenBlacklistRepository, times(1)).save(any(TokenBlacklist.class));
    }

    @Test
    void testAddToBlacklist_AlreadyBlacklisted() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        String reason = "TEST_LOGOUT";

        when(tokenBlacklistRepository.existsByTokenHash(testTokenHash)).thenReturn(true);

        // Act
        tokenBlacklistService.addToBlacklist(
                testToken,
                TokenBlacklist.TokenType.ACCESS,
                testUserId,
                expiresAt,
                reason
        );

        // Assert
        verify(tokenBlacklistRepository, times(1)).existsByTokenHash(testTokenHash);
        verify(tokenBlacklistRepository, never()).save(any(TokenBlacklist.class));
    }

    @Test
    void testIsBlacklisted_TokenIsBlacklisted() {
        // Arrange
        when(tokenBlacklistRepository.existsByTokenHash(testTokenHash)).thenReturn(true);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Assert
        assertTrue(result);
        verify(tokenBlacklistRepository, times(1)).existsByTokenHash(testTokenHash);
    }

    @Test
    void testIsBlacklisted_TokenIsNotBlacklisted() {
        // Arrange
        when(tokenBlacklistRepository.existsByTokenHash(testTokenHash)).thenReturn(false);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(testToken);

        // Assert
        assertFalse(result);
        verify(tokenBlacklistRepository, times(1)).existsByTokenHash(testTokenHash);
    }

    @Test
    void testCleanupExpiredEntries() {
        // Arrange
        doNothing().when(tokenBlacklistRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));

        // Act
        tokenBlacklistService.cleanupExpiredEntries();

        // Assert
        verify(tokenBlacklistRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    void testAddToBlacklist_RefreshTokenType() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        String reason = "TOKEN_ROTATION";

        when(tokenBlacklistRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(tokenBlacklistRepository.save(any(TokenBlacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        tokenBlacklistService.addToBlacklist(
                testToken,
                TokenBlacklist.TokenType.REFRESH,
                testUserId,
                expiresAt,
                reason
        );

        // Assert
        verify(tokenBlacklistRepository, times(1)).save(any(TokenBlacklist.class));
    }

    @Test
    void testAddToBlacklist_WithDifferentReasons() {
        // Arrange
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        String[] reasons = {
                TokenBlacklist.RevocationReason.LOGOUT.name(),
                TokenBlacklist.RevocationReason.TOKEN_ROTATION.name(),
                TokenBlacklist.RevocationReason.SECURITY_BREACH.name(),
                TokenBlacklist.RevocationReason.ADMIN_REVOKE.name()
        };

        when(tokenBlacklistRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(tokenBlacklistRepository.save(any(TokenBlacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        for (String reason : reasons) {
            tokenBlacklistService.addToBlacklist(
                    "token-" + reason,
                    TokenBlacklist.TokenType.ACCESS,
                    testUserId,
                    expiresAt,
                    reason
            );
        }

        verify(tokenBlacklistRepository, times(reasons.length)).save(any(TokenBlacklist.class));
    }

    // Helper method

    private String hashToken(String token) {
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
