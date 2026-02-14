package com.urbanclean.entity;

/**
 * Task workflow states
 * State machine: PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO
 * Feedback loop: RESUELTO → REABIERTO → EN_PROGRESO
 */
public enum TaskState {
    PENDIENTE,      // Pending - newly created task
    ASIGNADO,       // Assigned - task assigned to an operator
    EN_PROGRESO,    // In Progress - operator is working on the task
    RESUELTO,       // Resolved - task completed, awaiting citizen feedback
    REABIERTO       // Reopened - citizen rejected resolution
}
