package com.urbanclean.service;

import com.urbanclean.entity.FailedLoginAttempt;
import com.urbanclean.repository.FailedLoginAttemptRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for security monitoring and threat detection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityMonitoringService {

    private final FailedLoginAttemptRepository failedLoginAttemptRepository;
    private final AuditService auditService;

    // Thresholds for flagging suspicious activity
    private static final int MAX_FAILED_ATTEMPTS_PER_USERNAME = 5;
    private static final int MAX_FAILED_ATTEMPTS_PER_IP = 10;
    private static final int TIME_WINDOW_MINUTES = 15;

    /**
     * Log a failed login attempt
     * Flags suspicious activity if thresholds are exceeded
     */
    @Transactional
    public void logFailedLoginAttempt(String username, HttpServletRequest request) {
        String ipAddress = auditService.captureIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        log.warn("Failed login attempt - Username: {}, IP: {}", username, ipAddress);

        // Check if this should be flagged
        boolean shouldFlag = shouldFlagAttempt(username, ipAddress);

        // Create failed attempt record
        FailedLoginAttempt attempt = FailedLoginAttempt.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .flagged(shouldFlag)
                .build();

        failedLoginAttemptRepository.save(attempt);

        if (shouldFlag) {
            log.error("SECURITY ALERT: Multiple failed login attempts detected - Username: {}, IP: {}", 
                username, ipAddress);
        }
    }

    /**
     * Check if an attempt should be flagged based on recent history
     */
    private boolean shouldFlagAttempt(String username, String ipAddress) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(TIME_WINDOW_MINUTES);

        // Count recent failures for this username
        long usernameFailures = failedLoginAttemptRepository
                .countByUsernameAndAttemptedAtAfter(username, timeWindow);

        // Count recent failures from this IP
        long ipFailures = failedLoginAttemptRepository
                .countByIpAddressAndAttemptedAtAfter(ipAddress, timeWindow);

        return usernameFailures >= MAX_FAILED_ATTEMPTS_PER_USERNAME 
            || ipFailures >= MAX_FAILED_ATTEMPTS_PER_IP;
    }

    /**
     * Check if a username is currently locked due to too many failed attempts
     */
    public boolean isUsernameLocked(String username) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(TIME_WINDOW_MINUTES);
        long failures = failedLoginAttemptRepository
                .countByUsernameAndAttemptedAtAfter(username, timeWindow);
        
        return failures >= MAX_FAILED_ATTEMPTS_PER_USERNAME;
    }

    /**
     * Check if an IP address is currently blocked due to too many failed attempts
     */
    public boolean isIpAddressBlocked(String ipAddress) {
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(TIME_WINDOW_MINUTES);
        long failures = failedLoginAttemptRepository
                .countByIpAddressAndAttemptedAtAfter(ipAddress, timeWindow);
        
        return failures >= MAX_FAILED_ATTEMPTS_PER_IP;
    }

    /**
     * Cleanup old failed login attempts
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldFailedAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        
        log.info("Cleaning up failed login attempts older than {}", cutoff);
        failedLoginAttemptRepository.deleteByAttemptedAtBefore(cutoff);
        log.info("Cleanup completed");
    }
}
