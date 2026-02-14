package com.urbanclean.entity;

/**
 * Types of citizen feedback on task resolution
 */
public enum FeedbackType {
    CONFIRMED,  // Citizen confirms the task is resolved
    REJECTED    // Citizen rejects the resolution (task will be reopened)
}
