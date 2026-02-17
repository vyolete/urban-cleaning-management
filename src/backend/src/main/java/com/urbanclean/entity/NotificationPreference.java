package com.urbanclean.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "task_assigned", nullable = false)
    private Boolean taskAssigned = true;

    @Column(name = "task_resolved", nullable = false)
    private Boolean taskResolved = true;

    @Column(name = "task_reopened", nullable = false)
    private Boolean taskReopened = true;

    @Column(name = "report_created", nullable = false)
    private Boolean reportCreated = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public NotificationPreference() {
    }

    public NotificationPreference(UUID userId) {
        this.userId = userId;
        this.taskAssigned = true;
        this.taskResolved = true;
        this.taskReopened = true;
        this.reportCreated = true;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
