package com.urbanclean.dto.response;

import com.urbanclean.entity.NotificationFailure;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for notification failures
 */
public class NotificationFailureResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String email;
    private String notificationType;
    private String emailAddress;
    private String failureReason;
    private Integer retryCount;
    private LocalDateTime attemptedAt;
    private LocalDateTime createdAt;

    // Constructors
    public NotificationFailureResponse() {
    }

    public NotificationFailureResponse(NotificationFailure failure) {
        this.id = failure.getId();
        this.userId = failure.getUserId();
        this.notificationType = failure.getNotificationType();
        this.emailAddress = failure.getEmailAddress();
        this.failureReason = failure.getFailureReason();
        this.retryCount = failure.getRetryCount();
        this.attemptedAt = failure.getAttemptedAt();
        this.createdAt = failure.getCreatedAt();
        
        // Add user details if available
        if (failure.getUser() != null) {
            this.username = failure.getUser().getUsername();
            this.email = failure.getUser().getEmail();
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
