package com.urbanclean.enums;

/**
 * Enum representing different types of notifications in the system
 */
public enum NotificationType {
    TASK_ASSIGNED("Task Assigned", "A task has been assigned to you"),
    TASK_RESOLVED("Task Resolved", "A task has been resolved"),
    TASK_REOPENED("Task Reopened", "A task has been reopened"),
    REPORT_CREATED("Report Created", "Your report has been created successfully");

    private final String displayName;
    private final String description;

    NotificationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
