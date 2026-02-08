package com.urbanclean.entity;

/**
 * Task workflow states
 * State machine: PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO
 */
public enum TaskState {
    PENDIENTE,      // Pending - newly created task
    ASIGNADO,       // Assigned - task assigned to an operator
    EN_PROGRESO,    // In Progress - operator is working on the task
    RESUELTO        // Resolved - task completed
}
