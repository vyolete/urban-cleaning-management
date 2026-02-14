package com.urbanclean.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating notification preferences
 */
@Schema(description = "Request body for updating user notification preferences. Controls which email notifications the user receives.")
public class NotificationPreferenceRequest {

    @Schema(
        description = "Enable email notifications when a task is assigned to the user (for operators)",
        example = "true",
        required = true
    )
    @NotNull(message = "Task assigned preference is required")
    private Boolean taskAssigned;

    @Schema(
        description = "Enable email notifications when a task is resolved (for citizens who submitted the report)",
        example = "true",
        required = true
    )
    @NotNull(message = "Task resolved preference is required")
    private Boolean taskResolved;

    @Schema(
        description = "Enable email notifications when a task is reopened (for operators)",
        example = "true",
        required = true
    )
    @NotNull(message = "Task reopened preference is required")
    private Boolean taskReopened;

    @Schema(
        description = "Enable email notifications when a new report is created (for admins)",
        example = "false",
        required = true
    )
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
