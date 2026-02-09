package com.urbanclean.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a blacklisted token.
 * Tokens are added to the blacklist when revoked (logout, admin action, rotation).
 * Blacklisted tokens cannot be used for authentication.
 */
@Entity
@Table(name = "token_blacklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hash of the token

    @Column(name = "token_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at", nullable = false, updatable = false)
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "reason", length = 100)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by", insertable = false, updatable = false)
    private User revokedByUser;

    @PrePersist
    protected void onCreate() {
        revokedAt = LocalDateTime.now();
    }

    /**
     * Token type enum.
     */
    public enum TokenType {
        ACCESS,
        REFRESH
    }

    /**
     * Revocation reason enum.
     */
    public enum RevocationReason {
        LOGOUT("User logout"),
        ADMIN_REVOKE("Admin revocation"),
        TOKEN_ROTATION("Token rotation"),
        SECURITY_BREACH("Security breach"),
        EXPIRED("Token expired"),
        INVALID("Invalid token");

        private final String description;

        RevocationReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
