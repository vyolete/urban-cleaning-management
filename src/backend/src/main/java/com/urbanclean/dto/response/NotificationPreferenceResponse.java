package com.urbanclean.dto.response;

import com.urbanclean.entity.NotificationPreference;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for notification preferences
 */
public class NotificationPreferenceResponse {

    private UUID id;
    private UUID userId;
    private Boolean taskAssigned;
    private Boolean taskResolved;
    private Boolean taskReopened;
    private Boolean reportCreated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public NotificationPreferenceResponse() {
    }

    public NotificationPreferenceResponse(NotificationPreference preference) {
        this.id = preference.getId();
        this.userId = preference.getUserId();
        this.taskAssigned = preference.getTaskAssigned();
        this.taskResolved = preference.getTaskResolved();
        this.taskReopened = preference.getTaskReopened();
        this.reportCreated = preference.getReportCreated();
        this.createdAt = preference.getCreatedAt();
        this.updatedAt = preference.getUpdatedAt();
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

    public Boolean getTaskAssigned() {
        return taskAssigned;
    }

    public void setTaskAssigned(Boolean taskAssigned) {
        this.taskAssigned = taskAssigned;
    }

    public Boolean getTaskResolved() {
        return taskResolved;
    }

    public void setTaskResolved(Boolean taskResolved) {
        this.taskResolved = taskResolved;
    }

    public Boolean getTaskReopened() {
        return taskReopened;
    }

    public void setTaskReopened(Boolean taskReopened) {
        this.taskReopened = taskReopened;
    }

    public Boolean getReportCreated() {
        return reportCreated;
    }

    public void setReportCreated(Boolean reportCreated) {
        this.reportCreated = reportCreated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
