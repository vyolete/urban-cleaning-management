package com.urbanclean.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating notification preferences
 */
public class NotificationPreferenceRequest {

    @NotNull(message = "Task assigned preference is required")
    private Boolean taskAssigned;

    @NotNull(message = "Task resolved preference is required")
    private Boolean taskResolved;

    @NotNull(message = "Task reopened preference is required")
    private Boolean taskReopened;

    @NotNull(message = "Report created preference is required")
    private Boolean reportCreated;

    // Constructors
    public NotificationPreferenceRequest() {
    }

    public NotificationPreferenceRequest(Boolean taskAssigned, Boolean taskResolved, 
                                        Boolean taskReopened, Boolean reportCreated) {
        this.taskAssigned = taskAssigned;
        this.taskResolved = taskResolved;
        this.taskReopened = taskReopened;
        this.reportCreated = reportCreated;
    }

    // Getters and Setters
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
}
