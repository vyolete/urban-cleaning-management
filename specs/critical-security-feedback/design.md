# Design Document: Critical Security & Feedback Features

## Overview

This document specifies the design for critical security and user feedback features for the Urban Cleaning Management System, addressing IDRQ compliance gaps.

### Features Covered

1. Password Recovery System (OTP tokens via email)
2. Task Reopening Workflow (citizen feedback)
3. Task State Machine Enhancement (REABIERTO state)
4. GDPR Right to Erasure (data anonymization)
5. GDPR Data Portability (JSON export)
6. Enhanced Input Validation (password/email)
7. Audit Trail Enhancement (IP capture)

### Technology Stack

- Spring Boot (Java 17), Spring Mail (SMTP), Spring Events (@Async)
- Jakarta Validation API, PostgreSQL, BCrypt, JWT

## Architecture

### Component Overview

**New Components:**
- PasswordResetController, PasswordResetService, EmailService
- FeedbackController, FeedbackService
- UserProfileController, UserDataService
- ValidationService (password/email)

**Enhanced Components:**
- TaskService (REABIERTO state, evidence validation)
- AuditService (IP address capture)
- AuthService (enhanced validation)

**New Entities:**
- PasswordResetToken, CitizenFeedback, NotificationQueue

**Enhanced Entities:**
- Task (add REABIERTO to TaskState enum, add resolutionEvidence field)
- AuditLog (add ipAddress field)
- User (add deletedAt, anonymized fields)

## Data Models

### 1. PasswordResetToken Entity

```java
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String token; // UUID
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt; // 15 minutes from creation
    
    @Column(nullable = false)
    private Boolean used = false;
    
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private String ipAddress; // IPv4/IPv6
}
```

### 2. CitizenFeedback Entity

```java
@Entity
@Table(name = "citizen_feedback")
public class CitizenFeedback {
    @Id @GeneratedValue
    private UUID id;
    
    @OneToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    
    @ManyToOne
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType type; // CONFIRMED, REJECTED
    
    @Column(length = 500)
    private String justification; // Required for REJECTED
    
    @Column(nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(nullable = false)
    private LocalDateTime feedbackDeadline; // 72h from task resolution
}
```

### 3. Enhanced Task Entity

```java
// Add to existing Task entity:
@Enumerated(EnumType.STRING)
private TaskState state; // Add REABIERTO to enum

@Column(length = 1000)
private String resolutionEvidence; // Photo URL or comment

@Column
private Integer reopenCount = 0;

@Column
private Boolean citizenApproved = false;
```

### 4. Enhanced AuditLog Entity

```java
// Add to existing AuditLog entity:
@Column(length = 45)
private String ipAddress; // IPv4/IPv6 support
```

### 5. Enhanced User Entity

```java
// Add to existing User entity:
@Column
private LocalDateTime deletedAt; // Soft delete timestamp

@Column
private Boolean anonymized = false;

@Column
private String originalEmailHash; // For referential integrity after deletion
```

## API Endpoints

### Password Recovery

```
POST   /api/auth/password/forgot          - Request password reset
POST   /api/auth/password/reset           - Reset password with token
GET    /api/auth/password/validate/{token} - Validate token
```

### Task Feedback

```
POST   /api/tasks/{id}/feedback/confirm   - Confirm resolution
POST   /api/tasks/{id}/feedback/reject    - Reject and reopen
GET    /api/tasks/{id}/feedback           - Get feedback status
```

### User Profile & GDPR

```
GET    /api/users/me                      - Get own profile
PUT    /api/users/me                      - Update profile
DELETE /api/users/me                      - Request account deletion
GET    /api/users/me/export               - Export personal data (JSON)
POST   /api/users/me/deletion/cancel      - Cancel deletion request
```

## Business Logic

### Password Recovery Flow

1. User requests reset → System generates UUID token
2. Token stored with 15-minute expiration
3. Email sent asynchronously with reset link
4. User clicks link → Frontend validates token
5. User submits new password → Token marked as used
6. All user's JWT tokens invalidated
7. Audit log created with IP address

### Task Reopening Flow

1. Task transitions to RESUELTO → Email sent to citizen
2. Citizen has 72 hours to respond
3. **If CONFIRMED**: Task marked as citizen-approved
4. **If REJECTED**: Task transitions to REABIERTO, operator notified
5. **If no response**: Task auto-closed after 72h
6. Operator can transition REABIERTO → EN_PROGRESO
7. Maximum 3 reopenings per task

### GDPR Deletion Flow

1. User requests deletion → 7-day grace period starts
2. Confirmation email sent
3. User can cancel within 7 days
4. After 7 days: Personal data anonymized
   - username → "usuario_anonimo_{hash}"
   - email → hashed identifier
   - passwordHash → cleared
   - Historical reports preserved with anonymized reference
5. Audit log created

## Correctness Properties

*Properties are universal statements that must hold for all valid inputs.*

### Password Recovery Properties

**Property 1: Token expiration enforcement**
*For any* password reset token older than 15 minutes, the system should reject password reset attempts.
**Validates: Requirements 1.2, 1.6**

**Property 2: Token single-use enforcement**
*For any* password reset token that has been used, subsequent attempts to use the same token should be rejected.
**Validates: Requirements 1.5, 1.9**

**Property 3: Email enumeration prevention**
*For any* password reset request (valid or invalid email), the system should return the same success message.
**Validates: Requirements 1.4**

**Property 4: JWT invalidation on password reset**
*For any* successful password reset, all existing JWT tokens for that user should become invalid.
**Validates: Requirements 1.8**

### Task Reopening Properties

**Property 5: Feedback deadline enforcement**
*For any* task in RESUELTO state, if 72 hours pass without citizen feedback, the task should be automatically closed.
**Validates: Requirements 2.6**

**Property 6: Feedback authorization**
*For any* feedback submission, only the original reporter should be able to provide feedback on their own report.
**Validates: Requirements 2.7**

**Property 7: Reopening limit enforcement**
*For any* task that has been reopened 3 times, the system should flag it for administrator review.
**Validates: Requirements 3.8**

**Property 8: Resolution evidence requirement**
*For any* task transition to RESUELTO, the system should require either a photo or a comment of at least 20 characters.
**Validates: Requirements 3.5**

### GDPR Properties

**Property 9: Anonymization completeness**
*For any* deleted user account, all personal identifiers (username, email, password) should be removed or hashed.
**Validates: Requirements 4.4, 4.5, 4.7**

**Property 10: Historical data preservation**
*For any* deleted user account, historical reports and tasks should be preserved with anonymized references.
**Validates: Requirements 4.6**

**Property 11: Data export completeness**
*For any* user data export request, the JSON should include profile data, all reports, feedback, and activity history.
**Validates: Requirements 5.3**

### Validation Properties

**Property 12: Password complexity enforcement**
*For any* password that doesn't meet complexity requirements (8+ chars, 1 uppercase, 1 number, 1 special), the system should reject it.
**Validates: Requirements 6.1**

**Property 13: Email format validation**
*For any* email that doesn't match RFC 5322 format, the system should reject it.
**Validates: Requirements 6.2**

### Audit Properties

**Property 14: IP address capture**
*For any* security-relevant action (password reset, login, deletion), the system should capture and store the originating IP address.
**Validates: Requirements 7.1, 7.2**

## Testing Strategy

### Unit Tests
- Password reset token generation and validation
- Email format validation (RFC 5322)
- Password complexity validation
- Task state transitions with REABIERTO
- Feedback authorization checks
- Data anonymization logic

### Property-Based Tests
- Use JUnit-QuickCheck
- Minimum 100 iterations per property
- Tag format: `@Tag("Feature: critical-security-feedback, Property N: [text]")`

### Integration Tests
- Email sending (with test SMTP server)
- Complete password reset flow
- Complete feedback flow
- GDPR deletion flow
- Data export generation

## Error Handling

### New Error Codes
- `TOKEN_EXPIRED`: Reset token has expired
- `TOKEN_INVALID`: Reset token not found or already used
- `TOKEN_USED`: Reset token already used
- `FEEDBACK_UNAUTHORIZED`: User not authorized to provide feedback
- `FEEDBACK_DEADLINE_PASSED`: 72-hour window expired
- `REOPEN_LIMIT_EXCEEDED`: Task reopened too many times
- `EVIDENCE_REQUIRED`: Resolution evidence missing
- `PASSWORD_WEAK`: Password doesn't meet complexity requirements
- `EMAIL_INVALID`: Email format invalid
- `DELETION_IN_PROGRESS`: Account deletion already requested

## Security Considerations

1. **Rate Limiting**: 3 password reset requests per email per hour
2. **HTTPS Only**: All password reset links require HTTPS
3. **Token Hashing**: Store hashed tokens in database
4. **Email Enumeration**: Always return same message for forgot-password
5. **IP Logging**: Log IP for all security events
6. **Grace Period**: 7-day cancellation window for account deletion
7. **Audit Trail**: All GDPR operations logged

## Performance Considerations

1. **Async Email**: Non-blocking email sending
2. **Token Cleanup**: Scheduled job to delete expired tokens (daily)
3. **Feedback Auto-Close**: Scheduled job to close tasks after 72h (hourly)
4. **Index Optimization**: Indexes on token, expiresAt, feedbackDeadline
5. **Connection Pooling**: SMTP connection pool for email service

## Deployment Notes

1. **Environment Variables**: SMTP configuration (host, port, username, password)
2. **Email Templates**: HTML templates in resources/templates/email/
3. **Frontend URLs**: Configure base URL for password reset links
4. **Scheduled Jobs**: Enable @EnableScheduling
5. **Async Support**: Enable @EnableAsync
