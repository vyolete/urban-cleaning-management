# Requirements Document

## Introduction

This document specifies critical security and user feedback requirements for the Urban Cleaning Management System. These requirements address essential gaps identified in the IDRQ compliance analysis, focusing on password recovery, citizen feedback loops, task reopening workflow, and GDPR compliance.

## Glossary

- **System**: The Urban Cleaning Management WebApp
- **Citizen**: A user with ROLE_CIUDADANO who can report cleaning incidents
- **Operator**: A user with ROLE_TECNICO who manages and resolves cleaning tasks
- **Administrator**: A user with ROLE_ADMIN who configures system parameters
- **Task**: A work item created from one or more reports, assigned to operators
- **Task_State**: The current status of a task (PENDIENTE, ASIGNADO, EN_PROGRESO, RESUELTO, REABIERTO)
- **OTP**: One-Time Password token used for password recovery
- **Password_Reset_Token**: A cryptographically secure token with expiration for password recovery
- **Citizen_Feedback**: User confirmation or rejection of task resolution
- **REABIERTO**: Task state when a citizen rejects the resolution and reopens the task
- **Feedback_Deadline**: 72-hour window for citizen to respond to task resolution
- **RGPD**: General Data Protection Regulation (EU privacy law)
- **Right_to_Erasure**: GDPR right allowing users to request deletion of their personal data
- **Data_Portability**: GDPR right allowing users to export their personal data
- **Anonymization**: Process of removing personally identifiable information from historical records

## Requirements

### Requirement 1: Password Recovery System

**User Story:** As a user who forgot my password, I want to securely reset it via email, so that I can regain access to my account.

#### Acceptance Criteria

1. WHEN a user requests password recovery, THE System SHALL generate a cryptographically secure one-time token using UUID
2. WHEN a recovery token is generated, THE System SHALL set an expiration time of 15 minutes from creation
3. WHEN a recovery request is submitted, THE System SHALL send an email with a unique recovery link to the registered email address
4. THE System SHALL not reveal whether an email address exists in the system when processing recovery requests
5. WHEN a recovery token is used successfully, THE System SHALL invalidate it immediately to prevent reuse
6. WHEN a recovery link expires, THE System SHALL reject password reset attempts with that token and return an error message
7. THE System SHALL only accept recovery links over HTTPS connections in production environments
8. WHEN a password is successfully reset, THE System SHALL invalidate all existing JWT tokens for that user
9. WHEN a user attempts to use an already-used token, THE System SHALL reject the request and return an error
10. THE System SHALL limit password recovery requests to 3 attempts per email address per hour to prevent abuse

### Requirement 2: Task Reopening Workflow

**User Story:** As a Citizen, I want to confirm or reject task resolution, so that I can ensure my reported incident was properly addressed.

#### Acceptance Criteria

1. WHEN a task transitions to RESUELTO state, THE System SHALL send a notification to the original reporter with feedback options
2. THE System SHALL provide two actions in the notification: "Confirm Solution" and "Reject/Reopen"
3. WHEN a Citizen confirms resolution, THE System SHALL mark the task as citizen-approved and record the confirmation timestamp
4. WHEN a Citizen rejects resolution, THE System SHALL transition the task state from RESUELTO to REABIERTO
5. WHEN reopening a task, THE System SHALL require a mandatory justification field with minimum 10 characters
6. IF no citizen response is received within 72 hours of RESUELTO state, THEN THE System SHALL automatically mark the task as closed
7. THE System SHALL only allow the original reporter to provide feedback on their own reports
8. WHEN a task is reopened, THE System SHALL send a notification to the assigned operator with the citizen's justification
9. THE System SHALL record citizen satisfaction feedback (confirmed/rejected) for quality statistics
10. WHEN a task is in REABIERTO state, THE System SHALL allow operators to transition it back to EN_PROGRESO
11. THE System SHALL prevent citizens from reopening a task more than once after each resolution

### Requirement 3: Task State Machine Enhancement

**User Story:** As an Operator, I want a complete task workflow including reopening capability, so that I can properly handle citizen feedback.

#### Acceptance Criteria

1. THE System SHALL support the REABIERTO state in the task state machine
2. THE System SHALL enforce valid state transitions: PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO → REABIERTO → EN_PROGRESO
3. WHEN a task is in REABIERTO state, THE System SHALL display the citizen's justification to the operator
4. WHEN an operator attempts to close a task without resolution evidence, THE System SHALL reject the transition and return an error
5. THE System SHALL require either a photo or a comment (minimum 20 characters) as resolution evidence
6. WHEN a task transitions to RESUELTO, THE System SHALL store the resolution evidence reference
7. THE System SHALL track the number of times a task has been reopened
8. IF a task is reopened more than 3 times, THEN THE System SHALL flag it for administrator review
9. THE System SHALL prevent direct transitions from PENDIENTE to RESUELTO without passing through intermediate states

### Requirement 4: GDPR Compliance - Right to Erasure

**User Story:** As a user, I want to delete my account and personal data, so that I can exercise my right to be forgotten under GDPR.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint for users to request account deletion
2. WHEN a user requests account deletion, THE System SHALL require password confirmation for security
3. WHEN account deletion is confirmed, THE System SHALL anonymize the user's personal data within 24 hours
4. THE System SHALL replace the user's name with "Usuario Anónimo" in all historical records
5. THE System SHALL replace the user's email with a hashed identifier to maintain referential integrity
6. THE System SHALL preserve historical reports and tasks for municipal records but remove personal identifiers
7. THE System SHALL delete the user's authentication credentials (username, password hash, email)
8. THE System SHALL prevent the user from logging in after account deletion
9. WHEN anonymizing data, THE System SHALL maintain audit trail integrity by preserving action timestamps
10. THE System SHALL send a confirmation email before processing the deletion request
11. THE System SHALL provide a 7-day grace period where the user can cancel the deletion request

### Requirement 5: GDPR Compliance - Data Portability

**User Story:** As a user, I want to export my personal data, so that I can exercise my right to data portability under GDPR.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint for users to request a complete export of their personal data
2. WHEN a user requests data export, THE System SHALL generate a JSON file containing all their personal information
3. THE System SHALL include in the export: profile data, all submitted reports, task feedback, and account activity history
4. THE System SHALL complete the data export generation within 30 seconds for typical user data volumes
5. THE System SHALL provide a download link valid for 24 hours after generation
6. THE System SHALL include timestamps in ISO 8601 format in the exported data
7. THE System SHALL include geographic coordinates in WGS84 format (latitude/longitude)
8. THE System SHALL structure the exported JSON according to a documented schema
9. THE System SHALL log all data export requests for audit purposes
10. THE System SHALL allow users to export their data at most once per 24 hours to prevent abuse

### Requirement 6: Enhanced Input Validation

**User Story:** As a security engineer, I want comprehensive input validation for authentication, so that the system is protected against weak credentials.

#### Acceptance Criteria

1. WHEN a user registers, THE System SHALL validate password complexity: minimum 8 characters, at least 1 uppercase letter, at least 1 lowercase letter, at least 1 number, at least 1 special character
2. WHEN a user registers, THE System SHALL validate email format using RFC 5322 compliant regular expression
3. WHEN password validation fails, THE System SHALL return a descriptive error message indicating which requirements are not met
4. WHEN email validation fails, THE System SHALL return an error message without revealing whether the email already exists
5. THE System SHALL reject passwords that contain the username or email address
6. THE System SHALL reject commonly used passwords from a blacklist of top 10,000 common passwords
7. WHEN a user changes their password, THE System SHALL enforce the same complexity requirements
8. THE System SHALL validate that the new password is different from the previous password
9. THE System SHALL sanitize all text inputs to prevent XSS attacks by escaping HTML special characters
10. THE System SHALL validate coordinate ranges: latitude between -90 and 90, longitude between -180 and 180

### Requirement 7: Audit Trail Enhancement

**User Story:** As an Administrator, I want complete audit trails including IP addresses, so that I can track security-relevant actions.

#### Acceptance Criteria

1. WHEN any state change occurs, THE System SHALL capture the originating IP address from the HTTP request
2. THE System SHALL store the IP address in the audit log entry
3. WHEN a password reset is requested, THE System SHALL log the request with IP address and timestamp
4. WHEN a password is successfully changed, THE System SHALL log the action with IP address
5. WHEN an account deletion is requested, THE System SHALL log the request with IP address
6. THE System SHALL log failed authentication attempts with IP address for security monitoring
7. WHEN multiple failed login attempts occur from the same IP, THE System SHALL flag it for review
8. THE System SHALL support IPv4 and IPv6 address formats
9. THE System SHALL handle requests behind proxies by extracting the real client IP from X-Forwarded-For header
10. THE System SHALL validate and sanitize IP addresses before storage to prevent injection attacks

## Non-Functional Requirements

### Security Requirements

1. All password reset tokens SHALL be stored hashed in the database
2. Email sending SHALL be performed asynchronously to avoid blocking API responses
3. All sensitive operations SHALL require HTTPS in production
4. Rate limiting SHALL be enforced on all authentication-related endpoints
5. All personal data SHALL be encrypted at rest in the database

### Performance Requirements

1. Password reset email SHALL be sent within 5 seconds of request
2. Account deletion anonymization SHALL complete within 24 hours
3. Data export generation SHALL complete within 30 seconds
4. Feedback notification SHALL be sent within 10 seconds of task resolution

### Reliability Requirements

1. Email sending failures SHALL be retried up to 3 times with exponential backoff
2. Failed email deliveries SHALL be logged for administrator review
3. System SHALL continue operating if email service is temporarily unavailable
4. All database operations SHALL be transactional to ensure data consistency

### Compliance Requirements

1. System SHALL comply with GDPR Articles 17 (Right to Erasure) and 20 (Data Portability)
2. System SHALL maintain audit logs for all GDPR-related operations
3. System SHALL provide evidence of compliance for regulatory audits
4. System SHALL document all data processing activities in accordance with GDPR Article 30
