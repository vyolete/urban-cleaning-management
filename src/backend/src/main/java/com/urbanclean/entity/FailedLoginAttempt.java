package com.urbanclean.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking failed login attempts
 * Used for security monitoring and brute force detection
 */
@Entity
@Table(name = "failed_login_attempts", indexes = {
    @Index(name = "idx_failed_login_username", columnList = "username"),
    @Index(name = "idx_failed_login_ip", columnList = "ip_address"),
    @Index(name = "idx_failed_login_timestamp", columnList = "attempted_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedLoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "attempted_at")
    private LocalDateTime attemptedAt;

    @Column(name = "flagged")
    @Builder.Default
    private Boolean flagged = false;
}
