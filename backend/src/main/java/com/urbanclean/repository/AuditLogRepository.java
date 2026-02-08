package com.urbanclean.repository;

import com.urbanclean.entity.AuditLog;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entity operations
 * Note: This repository should only support read and create operations
 * Updates and deletes should be prevented to maintain audit trail immutability
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs for a specific task ordered chronologically
     * @param task the task to get audit history for
     * @return list of audit logs in chronological order
     */
    List<AuditLog> findByTaskOrderByChangedAtAsc(Task task);

    /**
     * Find audit logs by user
     * @param user the user who performed the changes
     * @return list of audit logs
     */
    List<AuditLog> findByUser(User user);

    /**
     * Find audit logs within a time range
     * @param start the start of the time range
     * @param end the end of the time range
     * @return list of audit logs in the time range
     */
    List<AuditLog> findByChangedAtBetweenOrderByChangedAtAsc(
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * Find recent audit logs for a task
     * @param task the task
     * @param since the time threshold
     * @return list of recent audit logs
     */
    List<AuditLog> findByTaskAndChangedAtAfterOrderByChangedAtAsc(
        Task task,
        LocalDateTime since
    );

    /**
     * Count audit logs for a task
     * @param task the task
     * @return number of state changes for the task
     */
    long countByTask(Task task);
}
