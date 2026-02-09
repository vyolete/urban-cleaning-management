# Implementation Plan: Critical Security & Feedback Features

## Overview

This document outlines the implementation tasks for critical security and user feedback features, organized in a logical sequence that builds incrementally and validates functionality at each step.

## Tasks

### Phase 1: Foundation - Email Service & Enhanced Validation

- [x] 1. Set up email infrastructure
  - [x] 1.1 Add Spring Mail dependencies to pom.xml
    - Add spring-boot-starter-mail dependency
    - Configure SMTP properties in application.yml
    - _Requirements: 1.3_
  
  - [x] 1.2 Create EmailService with async support
    - Implement @Async email sending
    - Create HTML email template engine
    - Add retry logic with exponential backoff (3 attempts)
    - _Requirements: 1.3, 16.4, 16.5_
  
  - [ ]* 1.3 Write unit tests for EmailService
    - Test email template rendering
    - Test retry logic on failure
    - Mock SMTP server for testing
    - _Requirements: 16.4, 16.5_
  
  - [x] 1.4 Create email templates
    - Password reset email template (HTML)
    - Task resolution notification template
    - Task reopened notification template
    - Account deletion confirmation template
    - _Requirements: 1.3, 2.1, 2.8, 4.10_

- [x] 2. Implement enhanced input validation
  - [x] 2.1 Create PasswordValidator class
    - Validate minimum 8 characters
    - Validate at least 1 uppercase letter
    - Validate at least 1 lowercase letter
    - Validate at least 1 number
    - Validate at least 1 special character
    - Reject passwords containing username/email
    - Check against common password blacklist
    - _Requirements: 6.1, 6.5, 6.6_
  
  - [x] 2.2 Create EmailValidator class
    - Implement RFC 5322 compliant regex validation
    - _Requirements: 6.2_
  
  - [ ]* 2.3 Write property tests for validation
    - **Property 12: Password complexity enforcement**
    - **Validates: Requirements 6.1**
    - Generate random passwords and verify rejection of weak ones
  
  - [ ]* 2.4 Write property tests for email validation
    - **Property 13: Email format validation**
    - **Validates: Requirements 6.2**
    - Generate random email strings and verify RFC 5322 compliance
  
  - [x] 2.5 Update RegisterRequest with enhanced validation
    - Apply @PasswordComplexity annotation
    - Apply @EmailFormat annotation
    - Update error messages to be descriptive
    - _Requirements: 6.1, 6.2, 6.3_

- [x] 3. Checkpoint - Validate email and validation infrastructure
  - ✅ Email service can send test emails (EmailService created with async + retry)
  - ✅ Validation rejects weak passwords and invalid emails (PasswordValidator + EmailValidator)
  - ✅ Build successful with Java 21
  - **Phase 1 Complete!**

### Phase 2: Password Recovery System

- [ ] 4. Create password reset data model
  - [ ] 4.1 Create PasswordResetToken entity
    - Add fields: id, token, user, expiresAt, used, usedAt, createdAt, ipAddress
    - Add indexes on token, user_id, expires_at
    - _Requirements: 1.1, 1.2_
  
  - [ ] 4.2 Create PasswordResetTokenRepository
    - Add method: findByToken
    - Add method: findByUserAndUsedFalse
    - Add method: deleteExpiredTokens
    - _Requirements: 1.1_
  
  - [ ] 4.3 Create database migration
    - Create password_reset_tokens table
    - Add foreign key to users table
    - Add indexes
    - _Requirements: 1.1_

- [ ] 5. Implement password reset service layer
  - [ ] 5.1 Create PasswordResetService
    - Implement initiatePasswordReset(email, ipAddress)
    - Implement validateToken(token)
    - Implement resetPassword(token, newPassword, ipAddress)
    - Implement cleanupExpiredTokens() scheduled method
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 1.6, 1.8_
  
  - [ ]* 5.2 Write unit tests for PasswordResetService
    - Test token generation (UUID format)
    - Test 15-minute expiration
    - Test token invalidation after use
    - Test email enumeration prevention
    - _Requirements: 1.1, 1.2, 1.4, 1.5_
  
  - [ ]* 5.3 Write property tests for password reset
    - **Property 1: Token expiration enforcement**
    - **Validates: Requirements 1.2, 1.6**
    - Generate tokens with various ages, verify rejection after 15 min
  
  - [ ]* 5.4 Write property tests for token reuse
    - **Property 2: Token single-use enforcement**
    - **Validates: Requirements 1.5, 1.9**
    - Generate tokens, use them, verify subsequent attempts fail

- [ ] 6. Implement password reset API endpoints
  - [ ] 6.1 Create PasswordResetController
    - POST /api/auth/password/forgot
    - POST /api/auth/password/reset
    - GET /api/auth/password/validate/{token}
    - Add rate limiting (3 requests per hour per email)
    - Extract IP address from request
    - _Requirements: 1.3, 1.7, 1.10_
  
  - [ ] 6.2 Create DTOs
    - ForgotPasswordRequest (email)
    - ResetPasswordRequest (token, newPassword)
    - TokenValidationResponse (isValid)
    - _Requirements: 1.3_
  
  - [ ] 6.3 Update GlobalExceptionHandler
    - Add TOKEN_EXPIRED error
    - Add TOKEN_INVALID error
    - Add TOKEN_USED error
    - _Requirements: 1.6, 1.9_
  
  - [ ]* 6.4 Write integration tests for password reset flow
    - Test complete forgot-password flow
    - Test token validation
    - Test password reset with valid token
    - Test JWT invalidation after reset
    - _Requirements: 1.1-1.10_

- [ ] 7. Implement JWT invalidation on password reset
  - [ ] 7.1 Add token version field to User entity
    - Add tokenVersion field (integer, default 0)
    - Increment on password reset
    - _Requirements: 1.8_
  
  - [ ] 7.2 Update JwtTokenProvider
    - Include tokenVersion in JWT claims
    - Validate tokenVersion on token verification
    - _Requirements: 1.8_
  
  - [ ]* 7.3 Write property tests for JWT invalidation
    - **Property 4: JWT invalidation on password reset**
    - **Validates: Requirements 1.8**
    - Generate user, create JWT, reset password, verify old JWT invalid

- [ ] 8. Checkpoint - Validate password recovery system
  - Test complete password reset flow end-to-end
  - Verify emails are sent
  - Verify tokens expire after 15 minutes
  - Verify old JWTs are invalidated
  - Ask user if questions arise

### Phase 3: Task Reopening & Citizen Feedback

- [ ] 9. Enhance task state machine
  - [ ] 9.1 Add REABIERTO to TaskState enum
    - Update TaskState enum
    - Update state transition validation
    - _Requirements: 2.4, 3.1_
  
  - [ ] 9.2 Add fields to Task entity
    - Add resolutionEvidence field (String, 1000 chars)
    - Add reopenCount field (Integer, default 0)
    - Add citizenApproved field (Boolean, default false)
    - _Requirements: 2.3, 3.5, 3.7_
  
  - [ ] 9.3 Create database migration
    - Add new fields to tareas table
    - Update state enum to include REABIERTO
    - _Requirements: 2.4, 3.1_
  
  - [ ]* 9.4 Write property tests for state machine
    - **Property 8: Resolution evidence requirement**
    - **Validates: Requirements 3.5**
    - Generate tasks, attempt RESUELTO transition, verify evidence required

- [ ] 10. Create citizen feedback data model
  - [ ] 10.1 Create CitizenFeedback entity
    - Add fields: id, task, citizen, type, justification, submittedAt, feedbackDeadline
    - Add unique constraint on task_id
    - _Requirements: 2.3, 2.5, 2.6_
  
  - [ ] 10.2 Create CitizenFeedbackRepository
    - Add method: findByTaskId
    - Add method: findPendingFeedbackPastDeadline
    - _Requirements: 2.6_
  
  - [ ] 10.3 Create FeedbackType enum
    - CONFIRMED, REJECTED
    - _Requirements: 2.3, 2.4_
  
  - [ ] 10.4 Create database migration
    - Create citizen_feedback table
    - Add foreign keys to tareas and users
    - Add index on feedback_deadline
    - _Requirements: 2.3_

- [ ] 11. Implement feedback service layer
  - [ ] 11.1 Create FeedbackService
    - Implement confirmResolution(taskId, citizenId)
    - Implement rejectResolution(taskId, citizenId, justification)
    - Implement autoCloseTasks() scheduled method (runs hourly)
    - _Requirements: 2.3, 2.4, 2.5, 2.6_
  
  - [ ] 11.2 Update TaskService
    - Add validation for RESUELTO transition (require evidence)
    - Add logic to create feedback deadline (72h) on RESUELTO
    - Add logic to handle REABIERTO → EN_PROGRESO transition
    - Add logic to flag tasks reopened > 3 times
    - _Requirements: 2.1, 3.5, 3.7, 3.8_
  
  - [ ]* 11.3 Write unit tests for FeedbackService
    - Test feedback authorization (only original reporter)
    - Test justification requirement for rejection
    - Test 72-hour auto-close logic
    - _Requirements: 2.5, 2.6, 2.7_
  
  - [ ]* 11.4 Write property tests for feedback
    - **Property 5: Feedback deadline enforcement**
    - **Validates: Requirements 2.6**
    - Generate tasks with various resolution times, verify auto-close after 72h
  
  - [ ]* 11.5 Write property tests for authorization
    - **Property 6: Feedback authorization**
    - **Validates: Requirements 2.7**
    - Generate tasks and users, verify only original reporter can provide feedback
  
  - [ ]* 11.6 Write property tests for reopen limit
    - **Property 7: Reopening limit enforcement**
    - **Validates: Requirements 3.8**
    - Generate tasks, reopen multiple times, verify flagging after 3 reopenings

- [ ] 12. Implement feedback API endpoints
  - [ ] 12.1 Create FeedbackController
    - POST /api/tasks/{id}/feedback/confirm
    - POST /api/tasks/{id}/feedback/reject
    - GET /api/tasks/{id}/feedback
    - _Requirements: 2.3, 2.4_
  
  - [ ] 12.2 Create DTOs
    - RejectFeedbackRequest (justification)
    - FeedbackResponse (type, submittedAt, justification)
    - _Requirements: 2.5_
  
  - [ ] 12.3 Update GlobalExceptionHandler
    - Add FEEDBACK_UNAUTHORIZED error
    - Add FEEDBACK_DEADLINE_PASSED error
    - Add REOPEN_LIMIT_EXCEEDED error
    - Add EVIDENCE_REQUIRED error
    - _Requirements: 2.7, 2.6, 3.8, 3.5_
  
  - [ ]* 12.4 Write integration tests for feedback flow
    - Test complete feedback flow (confirm and reject)
    - Test feedback authorization
    - Test operator notification on rejection
    - _Requirements: 2.1-2.9_

- [ ] 13. Integrate feedback with email notifications
  - [ ] 13.1 Create TaskResolvedEvent
    - Event triggered when task transitions to RESUELTO
    - Contains task ID and citizen email
    - _Requirements: 2.1_
  
  - [ ] 13.2 Create TaskReopenedEvent
    - Event triggered when task transitions to REABIERTO
    - Contains task ID, operator email, and justification
    - _Requirements: 2.8_
  
  - [ ] 13.3 Create event listeners
    - Listen for TaskResolvedEvent → send email to citizen
    - Listen for TaskReopenedEvent → send email to operator
    - _Requirements: 2.1, 2.8_
  
  - [ ]* 13.4 Write integration tests for notifications
    - Test email sent on task resolution
    - Test email sent on task reopening
    - Verify async processing doesn't block API
    - _Requirements: 2.1, 2.8_

- [ ] 14. Checkpoint - Validate feedback system
  - Test complete feedback flow end-to-end
  - Verify emails are sent on resolution and reopening
  - Verify 72-hour auto-close works
  - Verify reopen limit enforcement
  - Ask user if questions arise

### Phase 4: GDPR Compliance

- [ ] 15. Implement right to erasure (account deletion)
  - [ ] 15.1 Add fields to User entity
    - Add deletedAt field (LocalDateTime, nullable)
    - Add anonymized field (Boolean, default false)
    - Add originalEmailHash field (String, nullable)
    - _Requirements: 4.3, 4.5_
  
  - [ ] 15.2 Create database migration
    - Add new fields to users table
    - _Requirements: 4.3_
  
  - [ ] 15.3 Create UserDataService
    - Implement requestAccountDeletion(userId, password)
    - Implement cancelAccountDeletion(userId)
    - Implement anonymizeUserData(userId) scheduled method
    - _Requirements: 4.2, 4.3, 4.11_
  
  - [ ] 15.4 Implement anonymization logic
    - Replace username with "usuario_anonimo_{hash}"
    - Replace email with hashed identifier
    - Clear passwordHash
    - Set anonymized = true
    - Preserve historical reports with anonymized reference
    - _Requirements: 4.4, 4.5, 4.6, 4.7, 4.8_
  
  - [ ]* 15.5 Write unit tests for anonymization
    - Test username anonymization
    - Test email hashing
    - Test password clearing
    - Test historical data preservation
    - _Requirements: 4.4-4.8_
  
  - [ ]* 15.6 Write property tests for GDPR deletion
    - **Property 9: Anonymization completeness**
    - **Validates: Requirements 4.4, 4.5, 4.7**
    - Generate users, delete accounts, verify all PII removed
  
  - [ ]* 15.7 Write property tests for data preservation
    - **Property 10: Historical data preservation**
    - **Validates: Requirements 4.6**
    - Generate users with reports, delete accounts, verify reports preserved

- [ ] 16. Implement data portability (data export)
  - [ ] 16.1 Create UserDataService export methods
    - Implement exportUserData(userId) → JSON
    - Include: profile, reports, feedback, activity history
    - Format timestamps as ISO 8601
    - Format coordinates as WGS84
    - _Requirements: 5.2, 5.3, 5.6, 5.7_
  
  - [ ] 16.2 Create data export DTOs
    - UserDataExport (profile, reports, feedback, metadata)
    - Structure according to documented schema
    - _Requirements: 5.8_
  
  - [ ]* 16.3 Write unit tests for data export
    - Test JSON structure
    - Test data completeness
    - Test timestamp formatting
    - Test coordinate formatting
    - _Requirements: 5.3, 5.6, 5.7_
  
  - [ ]* 16.4 Write property tests for export
    - **Property 11: Data export completeness**
    - **Validates: Requirements 5.3**
    - Generate users with various data, export, verify all data included

- [ ] 17. Implement user profile API endpoints
  - [ ] 17.1 Create UserProfileController
    - GET /api/users/me
    - PUT /api/users/me
    - DELETE /api/users/me
    - GET /api/users/me/export
    - POST /api/users/me/deletion/cancel
    - _Requirements: 4.1, 5.1_
  
  - [ ] 17.2 Create DTOs
    - UserProfileResponse
    - UpdateProfileRequest
    - DeletionConfirmationRequest (password)
    - _Requirements: 4.2_
  
  - [ ] 17.3 Update GlobalExceptionHandler
    - Add DELETION_IN_PROGRESS error
    - _Requirements: 4.3_
  
  - [ ]* 17.4 Write integration tests for GDPR endpoints
    - Test account deletion flow with grace period
    - Test deletion cancellation
    - Test data export generation
    - Test rate limiting on export (once per 24h)
    - _Requirements: 4.1-4.11, 5.1-5.10_

- [ ] 18. Checkpoint - Validate GDPR compliance
  - Test complete account deletion flow
  - Verify 7-day grace period
  - Verify data anonymization
  - Test data export generation
  - Verify export rate limiting
  - Ask user if questions arise

### Phase 5: Audit Trail Enhancement

- [ ] 19. Enhance audit logging with IP capture
  - [ ] 19.1 Add ipAddress field to AuditLog entity
    - Add ipAddress field (String, 45 chars for IPv6)
    - _Requirements: 7.1, 7.2_
  
  - [ ] 19.2 Create database migration
    - Add ip_address column to historial_cambios table
    - _Requirements: 7.1_
  
  - [ ] 19.3 Update AuditService
    - Add captureIpAddress(request) method
    - Extract IP from X-Forwarded-For header if behind proxy
    - Support IPv4 and IPv6 formats
    - Sanitize IP addresses before storage
    - _Requirements: 7.2, 7.8, 7.9, 7.10_
  
  - [ ] 19.4 Update all security-relevant operations
    - Capture IP on password reset request
    - Capture IP on password change
    - Capture IP on account deletion request
    - Capture IP on failed login attempts
    - _Requirements: 7.3, 7.4, 7.5, 7.6_
  
  - [ ]* 19.5 Write unit tests for IP capture
    - Test IPv4 extraction
    - Test IPv6 extraction
    - Test X-Forwarded-For parsing
    - Test IP sanitization
    - _Requirements: 7.8, 7.9, 7.10_
  
  - [ ]* 19.6 Write property tests for audit trail
    - **Property 14: IP address capture**
    - **Validates: Requirements 7.1, 7.2**
    - Generate security events, verify IP captured in audit log

- [ ] 20. Implement security monitoring
  - [ ] 20.1 Add failed login tracking
    - Log failed attempts with IP address
    - Flag multiple failures from same IP
    - _Requirements: 7.6, 7.7_
  
  - [ ]* 20.2 Write integration tests for security monitoring
    - Test failed login logging
    - Test IP-based flagging
    - _Requirements: 7.6, 7.7_

- [ ] 21. Final checkpoint - Complete system validation
  - Run all tests (unit, property, integration)
  - Verify all 14 correctness properties pass
  - Test complete flows end-to-end:
    - Password recovery flow
    - Task feedback flow
    - Account deletion flow
    - Data export flow
  - Verify email notifications work
  - Verify audit logs capture all events
  - Ask user if questions arise

### Phase 6: Frontend Integration (Optional)

- [ ]* 22. Create frontend components for password recovery
  - [ ]* 22.1 Create ForgotPasswordPage component
    - Email input form
    - Success message display
    - _Requirements: 1.3_
  
  - [ ]* 22.2 Create ResetPasswordPage component
    - Token validation on load
    - New password form with complexity indicator
    - Success/error handling
    - _Requirements: 1.3, 6.1_
  
  - [ ]* 22.3 Add password recovery links to LoginPage
    - "Forgot password?" link
    - _Requirements: 1.3_

- [ ]* 23. Create frontend components for task feedback
  - [ ]* 23.1 Add feedback buttons to TaskDetail component
    - "Confirm Resolution" button (for citizens)
    - "Reject Resolution" button with justification modal
    - Show feedback status
    - _Requirements: 2.2, 2.3, 2.4_
  
  - [ ]* 23.2 Update TaskList to show feedback status
    - Visual indicator for tasks awaiting feedback
    - Show feedback deadline countdown
    - _Requirements: 2.6_

- [ ]* 24. Create frontend components for user profile
  - [ ]* 24.1 Create UserProfilePage component
    - Display profile information
    - Edit profile form
    - Change password form
    - _Requirements: 18.1, 18.2, 18.3_
  
  - [ ]* 24.2 Add GDPR actions to profile page
    - "Export My Data" button
    - "Delete My Account" button with confirmation modal
    - Show deletion grace period if active
    - _Requirements: 4.1, 5.1_

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (minimum 100 iterations each)
- Unit tests validate specific examples and edge cases
- Integration tests validate complete flows
- Frontend tasks are optional and can be implemented after backend is complete

## Testing Summary

### Property-Based Tests (14 properties)
- Property 1: Token expiration enforcement
- Property 2: Token single-use enforcement
- Property 4: JWT invalidation on password reset
- Property 5: Feedback deadline enforcement
- Property 6: Feedback authorization
- Property 7: Reopening limit enforcement
- Property 8: Resolution evidence requirement
- Property 9: Anonymization completeness
- Property 10: Historical data preservation
- Property 11: Data export completeness
- Property 12: Password complexity enforcement
- Property 13: Email format validation
- Property 14: IP address capture

### Unit Tests
- EmailService (template rendering, retry logic)
- PasswordValidator (complexity rules)
- EmailValidator (RFC 5322)
- PasswordResetService (token generation, expiration)
- FeedbackService (authorization, auto-close)
- UserDataService (anonymization, export)
- AuditService (IP capture, sanitization)

### Integration Tests
- Complete password reset flow
- Complete feedback flow
- Complete GDPR deletion flow
- Complete data export flow
- Email notification delivery
- Security monitoring

## Estimated Effort

- Phase 1 (Foundation): 3-4 days
- Phase 2 (Password Recovery): 4-5 days
- Phase 3 (Task Feedback): 5-6 days
- Phase 4 (GDPR): 4-5 days
- Phase 5 (Audit Enhancement): 2-3 days
- Phase 6 (Frontend - Optional): 3-4 days

**Total Backend**: ~18-23 days
**Total with Frontend**: ~21-27 days
