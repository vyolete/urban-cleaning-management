package com.urbanclean.service;

import com.urbanclean.entity.UserSession;
import com.urbanclean.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua_parser.Client;
import ua_parser.Parser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user sessions across devices.
 * Implements session tracking, limits, and multi-device management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final RefreshTokenService refreshTokenService;
    private final Parser uaParser = new Parser();

    @Value("${session.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    /**
     * Create a new user session.
     * 
     * @param userId User ID
     * @param refreshTokenId Refresh token ID
     * @param deviceFingerprint Device fingerprint
     * @param ipAddress IP address
     * @param userAgent User agent string
     * @return The created session
     */
    @Transactional
    public UserSession createSession(UUID userId, UUID refreshTokenId, String deviceFingerprint, 
                                     String ipAddress, String userAgent) {
        // Parse user agent
        Client client = uaParser.parse(userAgent);
        
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setRefreshTokenId(refreshTokenId);
        session.setDeviceFingerprint(deviceFingerprint);
        session.setIpAddress(ipAddress);
        
        // Extract device information
        session.setDeviceType(determineDeviceType(client));
        session.setBrowser(formatBrowser(client));
        session.setOs(formatOS(client));
        
        // Location would be set by IP geolocation service (not implemented)
        session.setCity(null);
        session.setCountry(null);
        
        session.setActive(true);

        UserSession savedSession = userSessionRepository.save(session);

        // Enforce session limit
        enforceSessionLimit(userId);

        log.info("Created session for user: {}, device: {}, IP: {}", userId, session.getDeviceType(), ipAddress);

        return savedSession;
    }

    /**
     * Get all active sessions for a user.
     * 
     * @param userId User ID
     * @return List of active sessions
     */
    public List<UserSession> getActiveSessions(UUID userId) {
        return userSessionRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
     * Get all sessions for a user (including inactive).
     * 
     * @param userId User ID
     * @return List of all sessions
     */
    public List<UserSession> getAllSessions(UUID userId) {
        return userSessionRepository.findByUserIdOrderByLastActivityDesc(userId);
    }

    /**
     * Revoke a specific session.
     * 
     * @param sessionId Session ID
     * @param userId User ID (for authorization check)
     */
    @Transactional
    public void revokeSession(UUID sessionId, UUID userId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // Verify ownership
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to user");
        }

        // Deactivate session
        session.deactivate();
        userSessionRepository.save(session);

        // Revoke associated refresh token if exists
        if (session.getRefreshTokenId() != null) {
            // The refresh token will be revoked through the RefreshTokenService
            log.info("Revoked session {} for user: {}", sessionId, userId);
        }
    }

    /**
     * Revoke all sessions except the current one.
     * 
     * @param userId User ID
     * @param currentSessionId Current session ID to keep active
     */
    @Transactional
    public void revokeAllSessionsExceptCurrent(UUID userId, UUID currentSessionId) {
        userSessionRepository.deactivateAllExceptCurrent(userId, currentSessionId);
        
        // Revoke all refresh tokens except current
        refreshTokenService.revokeAllUserTokens(userId);
        
        log.info("Revoked all sessions except {} for user: {}", currentSessionId, userId);
    }

    /**
     * Revoke all sessions for a user.
     * 
     * @param userId User ID
     */
    @Transactional
    public void revokeAllSessions(UUID userId) {
        userSessionRepository.deactivateAllByUserId(userId);
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("Revoked all sessions for user: {}", userId);
    }

    /**
     * Update session activity timestamp.
     * 
     * @param sessionId Session ID
     */
    @Transactional
    public void updateSessionActivity(UUID sessionId) {
        userSessionRepository.findById(sessionId).ifPresent(session -> {
            session.updateActivity();
            userSessionRepository.save(session);
        });
    }

    /**
     * Enforce session limit for a user.
     * If user has more than max sessions, revoke the oldest ones.
     * 
     * @param userId User ID
     */
    @Transactional
    public void enforceSessionLimit(UUID userId) {
        int activeSessionCount = userSessionRepository.countByUserIdAndActiveTrue(userId);
        
        if (activeSessionCount > maxConcurrentSessions) {
            int sessionsToRevoke = activeSessionCount - maxConcurrentSessions;
            List<UserSession> oldestSessions = userSessionRepository.findOldestActiveByUserId(userId);
            
            for (int i = 0; i < sessionsToRevoke && i < oldestSessions.size(); i++) {
                UserSession session = oldestSessions.get(i);
                session.deactivate();
                userSessionRepository.save(session);
                
                log.info("Revoked oldest session {} for user {} due to session limit", 
                        session.getId(), userId);
            }
        }
    }

    /**
     * Cleanup stale sessions.
     * Runs daily at 5:00 AM.
     * Removes sessions with no activity for more than 30 days.
     */
    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void cleanupStaleSessions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        userSessionRepository.deleteStaleSessions(cutoffDate);
        log.info("Cleaned up stale sessions older than {}", cutoffDate);
    }

    /**
     * Determine device type from user agent.
     */
    private UserSession.DeviceType determineDeviceType(Client client) {
        if (client.device == null || client.device.family == null) {
            return UserSession.DeviceType.UNKNOWN;
        }
        
        String deviceFamily = client.device.family.toLowerCase();
        
        if (deviceFamily.contains("mobile") || deviceFamily.contains("phone")) {
            return UserSession.DeviceType.MOBILE;
        } else if (deviceFamily.contains("tablet") || deviceFamily.contains("ipad")) {
            return UserSession.DeviceType.TABLET;
        } else {
            return UserSession.DeviceType.DESKTOP;
        }
    }

    /**
     * Format browser information.
     */
    private String formatBrowser(Client client) {
        if (client.userAgent == null) {
            return "Unknown";
        }
        
        String browser = client.userAgent.family;
        String version = client.userAgent.major;
        
        if (version != null) {
            return browser + " " + version;
        }
        
        return browser;
    }

    /**
     * Format OS information.
     */
    private String formatOS(Client client) {
        if (client.os == null) {
            return "Unknown";
        }
        
        String os = client.os.family;
        String version = client.os.major;
        
        if (version != null) {
            return os + " " + version;
        }
        
        return os;
    }
}
