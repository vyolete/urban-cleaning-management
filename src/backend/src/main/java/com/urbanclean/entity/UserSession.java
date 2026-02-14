package com.urbanclean.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user session across devices.
 * Tracks active sessions for multi-device management and security monitoring.
 */
@Entity
@Table(name = "user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "refresh_token_id")
    private UUID refreshTokenId;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "device_type", length = 50)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "os", length = 100)
    private String os;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refresh_token_id", insertable = false, updatable = false)
    private RefreshToken refreshToken;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }

    /**
     * Device type enum.
     */
    public enum DeviceType {
        MOBILE,
        DESKTOP,
        TABLET,
        UNKNOWN
    }

    /**
     * Update the last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Deactivate this session.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Get formatted location string.
     */
    public String getLocation() {
        if (city != null && country != null) {
            return city + ", " + country;
        } else if (country != null) {
            return country;
        } else {
            return "Unknown";
        }
    }

    /**
     * Check if session is stale (no activity for more than 30 days).
     */
    public boolean isStale() {
        return lastActivity != null && 
               lastActivity.isBefore(LocalDateTime.now().minusDays(30));
    }
}
