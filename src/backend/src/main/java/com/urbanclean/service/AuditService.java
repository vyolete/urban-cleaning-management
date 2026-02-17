package com.urbanclean.service;

import com.urbanclean.entity.AuditLog;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.TaskState;
import com.urbanclean.entity.User;
import com.urbanclean.repository.AuditLogRepository;
import com.urbanclean.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for audit logging of task state changes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Log a state change for a task with IP address capture
     * Creates an immutable audit log entry
     */
    @Transactional
    public AuditLog logStateChange(Task task, TaskState previousState, TaskState newState, String ipAddress) {
        User currentUser = getCurrentUser();
        
        log.info("Logging state change for task {}: {} -> {} by user {} from IP {}",
                task.getId(), previousState, newState, currentUser.getUsername(), ipAddress);

        AuditLog auditLog = AuditLog.builder()
                .task(task)
                .user(currentUser)
                .previousState(previousState)
                .newState(newState)
                .changedAt(LocalDateTime.now())
                .ipAddress(sanitizeIpAddress(ipAddress))
                .build();

        return auditLogRepository.save(auditLog);
    }

    /**
     * Log a state change for a task (backward compatibility - no IP)
     * Creates an immutable audit log entry
     */
    @Transactional
    public AuditLog logStateChange(Task task, TaskState previousState, TaskState newState) {
        return logStateChange(task, previousState, newState, null);
    }

    /**
     * Capture IP address from HTTP request
     * Handles X-Forwarded-For header for proxied requests
     * Supports both IPv4 and IPv6
     */
    public String captureIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // Check X-Forwarded-For header (for requests behind proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // Take the first one (original client IP)
            String clientIp = xForwardedFor.split(",")[0].trim();
            log.debug("IP from X-Forwarded-For: {}", clientIp);
            return clientIp;
        }

        // Check X-Real-IP header (alternative proxy header)
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            log.debug("IP from X-Real-IP: {}", xRealIp);
            return xRealIp;
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        log.debug("IP from RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }

    /**
     * Sanitize IP address before storage
     * Validates format and removes any malicious content
     */
    private String sanitizeIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return null;
        }

        // Trim whitespace
        String sanitized = ipAddress.trim();

        // Basic validation: IPv4 or IPv6 format
        // IPv4: xxx.xxx.xxx.xxx (max 15 chars)
        // IPv6: xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx (max 45 chars)
        if (sanitized.length() > 45) {
            log.warn("IP address too long, truncating: {}", sanitized);
            sanitized = sanitized.substring(0, 45);
        }

        // Remove any non-IP characters (basic sanitization)
        // Allow: digits, dots (IPv4), colons (IPv6), and letters a-f (IPv6)
        if (!sanitized.matches("[0-9a-fA-F.:]+")) {
            log.warn("Invalid IP address format, rejecting: {}", sanitized);
            return null;
        }

        return sanitized;
    }

    /**
     * Get audit history for a task in chronological order
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getTaskAuditHistory(UUID taskId) {
        return auditLogRepository.findByTaskIdOrderByChangedAtAsc(taskId);
    }

    /**
     * Get audit history for a task (entity version)
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getTaskAuditHistory(Task task) {
        return auditLogRepository.findByTaskOrderByChangedAtAsc(task);
    }

    /**
     * Get recent audit logs (last N entries)
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentAuditLogs(int limit) {
        return auditLogRepository.findRecentAuditLogs(limit);
    }

    /**
     * Get audit logs by user
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByUser(User user) {
        return auditLogRepository.findByUserOrderByChangedAtDesc(user);
    }

    /**
     * Get audit logs within a time range
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByChangedAtBetweenOrderByChangedAtAsc(start, end);
    }

    /**
     * Count state changes for a task
     */
    @Transactional(readOnly = true)
    public long countStateChanges(Task task) {
        return auditLogRepository.countByTask(task);
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}
