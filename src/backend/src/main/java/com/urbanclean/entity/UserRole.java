package com.urbanclean.entity;

/**
 * User roles for role-based access control (RBAC)
 */
public enum UserRole {
    ROLE_CIUDADANO,  // Citizen - can submit reports
    ROLE_TECNICO,    // Operator - can manage tasks
    ROLE_ADMIN       // Administrator - full access including configuration
}
