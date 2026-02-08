package com.urbanclean.service;

import com.urbanclean.entity.AuditLog;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.TaskState;
import com.urbanclean.entity.User;
import com.urbanclean.repository.AuditLogRepository;
import com.urbanclean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Log a state change for a task
     * Creates an immutable audit log entry
     */
    @Transactional
    public AuditLog logStateChange(Task task, TaskState previousState, TaskState newState) {
        User currentUser = getCurrentUser();
        
        log.info("Logging state change for task {}: {} -> {} by user {}",
                task.getId(), previousState, newState, currentUser.getUsername());

        AuditLog auditLog = AuditLog.builder()
                .task(task)
                .changedBy(currentUser)
                .previousState(previousState)
                .newState(newState)
                .changedAt(LocalDateTime.now())
                .build();

        return auditLogRepository.save(auditLog);
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
        return auditLogRepository.findByChangedByOrderByChangedAtDesc(user);
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
