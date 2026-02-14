package com.urbanclean.service;

import com.urbanclean.entity.UserSession;
import com.urbanclean.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserSessionService
 */
@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserSessionService userSessionService;

    private UUID testUserId;
    private UUID testRefreshTokenId;
    private String testDeviceFingerprint;
    private String testIpAddress;
    private String testUserAgent;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRefreshTokenId = UUID.randomUUID();
        testDeviceFingerprint = "test-fingerprint";
        testIpAddress = "192.168.1.1";
        testUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        
        // Set max concurrent sessions
        ReflectionTestUtils.setField(userSessionService, "maxConcurrentSessions", 5);
    }

    @Test
    void testCreateSession_Success() {
        // Arrange
        when(userSessionRepository.save(any(UserSession.class)))
                .thenAnswer(invocation -> {
                    UserSession session = invocation.getArgument(0);
                    session.setId(UUID.randomUUID());
                    return session;
                });
        when(userSessionRepository.countByUserIdAndActiveTrue(testUserId)).thenReturn(1);

        // Act
        UserSession result = userSessionService.createSession(
                testUserId,
                testRefreshTokenId,
                testDeviceFingerprint,
                testIpAddress,
                testUserAgent
        );

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testRefreshTokenId, result.getRefreshTokenId());
        assertEquals(testDeviceFingerprint, result.getDeviceFingerprint());
        assertEquals(testIpAddress, result.getIpAddress());
        assertTrue(result.getActive());
        verify(userSessionRepository, times(1)).save(any(UserSession.class));
    }

    @Test
    void testGetActiveSessions_Success() {
        // Arrange
        List<UserSession> sessions = createMockSessions(3, true);
        when(userSessionRepository.findByUserIdAndActiveTrue(testUserId)).thenReturn(sessions);

        // Act
        List<UserSession> result = userSessionService.getActiveSessions(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(userSessionRepository, times(1)).findByUserIdAndActiveTrue(testUserId);
    }

    @Test
    void testGetAllSessions_Success() {
        // Arrange
        List<UserSession> sessions = createMockSessions(5, false);
        when(userSessionRepository.findByUserIdOrderByLastActivityDesc(testUserId)).thenReturn(sessions);

        // Act
        List<UserSession> result = userSessionService.getAllSessions(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(userSessionRepository, times(1)).findByUserIdOrderByLastActivityDesc(testUserId);
    }

    @Test
    void testRevokeSession_Success() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        UserSession session = createMockSession(sessionId, testUserId, true);

        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(session);

        // Act
        userSessionService.revokeSession(sessionId, testUserId);

        // Assert
        verify(userSessionRepository, times(1)).findById(sessionId);
        verify(userSessionRepository, times(1)).save(any(UserSession.class));
    }

    @Test
    void testRevokeSession_SessionNotFound() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userSessionService.revokeSession(sessionId, testUserId);
        });

        verify(userSessionRepository, times(1)).findById(sessionId);
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void testRevokeSession_WrongUser() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        UserSession session = createMockSession(sessionId, differentUserId, true);

        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userSessionService.revokeSession(sessionId, testUserId);
        });

        verify(userSessionRepository, times(1)).findById(sessionId);
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void testRevokeAllSessionsExceptCurrent_Success() {
        // Arrange
        UUID currentSessionId = UUID.randomUUID();
        doNothing().when(userSessionRepository).deactivateAllExceptCurrent(testUserId, currentSessionId);
        doNothing().when(refreshTokenService).revokeAllUserTokens(testUserId);

        // Act
        userSessionService.revokeAllSessionsExceptCurrent(testUserId, currentSessionId);

        // Assert
        verify(userSessionRepository, times(1)).deactivateAllExceptCurrent(testUserId, currentSessionId);
        verify(refreshTokenService, times(1)).revokeAllUserTokens(testUserId);
    }

    @Test
    void testRevokeAllSessions_Success() {
        // Arrange
        doNothing().when(userSessionRepository).deactivateAllByUserId(testUserId);
        doNothing().when(refreshTokenService).revokeAllUserTokens(testUserId);

        // Act
        userSessionService.revokeAllSessions(testUserId);

        // Assert
        verify(userSessionRepository, times(1)).deactivateAllByUserId(testUserId);
        verify(refreshTokenService, times(1)).revokeAllUserTokens(testUserId);
    }

    @Test
    void testUpdateSessionActivity_Success() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        UserSession session = createMockSession(sessionId, testUserId, true);

        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userSessionRepository.save(any(UserSession.class))).thenReturn(session);

        // Act
        userSessionService.updateSessionActivity(sessionId);

        // Assert
        verify(userSessionRepository, times(1)).findById(sessionId);
        verify(userSessionRepository, times(1)).save(any(UserSession.class));
    }

    @Test
    void testEnforceSessionLimit_WithinLimit() {
        // Arrange
        when(userSessionRepository.countByUserIdAndActiveTrue(testUserId)).thenReturn(3);

        // Act
        userSessionService.enforceSessionLimit(testUserId);

        // Assert
        verify(userSessionRepository, times(1)).countByUserIdAndActiveTrue(testUserId);
        verify(userSessionRepository, never()).findOldestActiveByUserId(any());
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void testEnforceSessionLimit_ExceedsLimit() {
        // Arrange
        int activeSessionCount = 6; // Exceeds limit of 5
        List<UserSession> oldestSessions = createMockSessions(6, true);

        when(userSessionRepository.countByUserIdAndActiveTrue(testUserId)).thenReturn(activeSessionCount);
        when(userSessionRepository.findOldestActiveByUserId(testUserId)).thenReturn(oldestSessions);
        when(userSessionRepository.save(any(UserSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userSessionService.enforceSessionLimit(testUserId);

        // Assert
        verify(userSessionRepository, times(1)).countByUserIdAndActiveTrue(testUserId);
        verify(userSessionRepository, times(1)).findOldestActiveByUserId(testUserId);
        verify(userSessionRepository, times(1)).save(any(UserSession.class)); // Only oldest one revoked
    }

    @Test
    void testCleanupStaleSessions() {
        // Arrange
        doNothing().when(userSessionRepository).deleteStaleSessions(any(LocalDateTime.class));

        // Act
        userSessionService.cleanupStaleSessions();

        // Assert
        verify(userSessionRepository, times(1)).deleteStaleSessions(any(LocalDateTime.class));
    }

    // Helper methods

    private UserSession createMockSession(UUID sessionId, UUID userId, boolean active) {
        UserSession session = new UserSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setRefreshTokenId(testRefreshTokenId);
        session.setDeviceFingerprint(testDeviceFingerprint);
        session.setDeviceType(UserSession.DeviceType.DESKTOP);
        session.setBrowser("Chrome 120");
        session.setOs("Windows 11");
        session.setIpAddress(testIpAddress);
        session.setActive(active);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        return session;
    }

    private List<UserSession> createMockSessions(int count, boolean active) {
        List<UserSession> sessions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sessions.add(createMockSession(UUID.randomUUID(), testUserId, active));
        }
        return sessions;
    }
}
