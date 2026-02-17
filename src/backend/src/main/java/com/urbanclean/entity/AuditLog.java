package com.urbanclean.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuditLog entity for immutable tracking of task state changes
 */
@Entity
@Table(name = "historial_cambios", indexes = {
    @Index(name = "idx_audit_task", columnList = "task_id"),
    @Index(name = "idx_audit_timestamp", columnList = "changed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, name = "previous_state", length = 20)
    private TaskState previousState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, name = "new_state", length = 20)
    private TaskState newState;

    @Column(nullable = false, updatable = false, name = "changed_at")
    private LocalDateTime changedAt;

    @Column(updatable = false, name = "ip_address", length = 45)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
