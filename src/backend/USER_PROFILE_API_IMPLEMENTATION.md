# User Profile API Implementation

## Overview

This document describes the implementation of User Profile Management API endpoints for GDPR compliance (Requirement 18).

## Implemented Components

### 1. UserController (`backend/src/main/java/com/urbanclean/controller/UserController.java`)

REST controller exposing user profile management endpoints:

#### Endpoints

| Method | Endpoint | Description | Requirement |
|--------|----------|-------------|-------------|
| GET | `/api/users/profile` | Get current user's profile information | 18.1 |
| PUT | `/api/users/profile` | Update current user's profile | 18.2, 18.6 |
| POST | `/api/users/change-password` | Change user password | 18.3, 18.7 |
| GET | `/api/users/reports` | Get user's report history | 18.4 |
| POST | `/api/users/delete-account` | Request account deletion (7-day grace period) | 18.5, 18.8 |
| POST | `/api/users/cancel-deletion` | Cancel account deletion during grace period | 18.5 |
| GET | `/api/users/export` | Export user data in JSON format (GDPR portability) | 18.9 |

#### Security

- All endpoints require authentication (`@PreAuthorize("isAuthenticated()")`)
- Users can only access and modify their own data (enforced in controller logic)
- Password verification required for sensitive operations (password change, account deletion)

### 2. Request DTOs

#### UpdateProfileRequest
```java
- username: String (optional, 3-50 characters)
- email: String (optional, validated with @ValidEmail)
```

#### ChangePasswordRequest
```java
- currentPassword: String (required)
- newPassword: String (required, validated with @ValidPassword)
```

#### DeleteAccountRequest
```java
- password: String (required for confirmation)
```

### 3. Response DTOs

#### UserProfileResponse
```java
- userId: UUID
- username: String
- email: String
- role: UserRole
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
- isAnonymized: Boolean
- deletionRequested: Boolean
- gracePeriodDaysRemaining: Long (nullable)
```

## Integration with Existing Services

The UserController integrates with:

1. **UserDataService** - Handles GDPR operations:
   - Account deletion requests
   - Account deletion cancellation
   - Data anonymization
   - Data export

2. **UserRepository** - Database operations for user entities

3. **ReportRepository** - Retrieves user's report history

4. **PasswordEncoder** - Validates and hashes passwords

## GDPR Compliance Features

### Data Portability (18.9)
- Users can export all their data in JSON format
- Export includes: profile, reports, feedback, and metadata
- Complies with GDPR Article 20 (Right to data portability)

### Right to Erasure (18.5, 18.8)
- Users can request account deletion
- 7-day grace period before anonymization
- Users can cancel deletion during grace period
- After grace period, data is anonymized (not deleted) to preserve historical records
- Anonymization replaces PII with hashed identifiers

### Data Access (18.1, 18.4)
- Users can view their profile information
- Users can view complete report history
- Complies with GDPR Article 15 (Right of access)

### Data Rectification (18.2)
- Users can update their profile information
- Email and username uniqueness validated
- Complies with GDPR Article 16 (Right to rectification)

### Security Measures (18.6, 18.7)
- Users can only modify their own data
- Password verification required for sensitive operations
- Current password required to change password

## Error Handling

All exceptions are handled by GlobalExceptionHandler:

- **AuthenticationException** → 401 Unauthorized
- **ValidationException** → 400 Bad Request
- **SecurityException** → 403 Forbidden
- **IllegalStateException** → 400 Bad Request
- **ResourceNotFoundException** → 404 Not Found
- **Generic Exception** → 500 Internal Server Error

## Testing

Integration tests created in `UserControllerTest.java` covering:

- Profile retrieval
- Profile updates
- Password changes
- Report history retrieval
- Account deletion requests
- Data export
- Authentication requirements
- Authorization enforcement

## API Usage Examples

### Get User Profile
```bash
GET /api/users/profile
Authorization: Bearer <jwt_token>
```

### Update Profile
```bash
PUT /api/users/profile
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "email": "newemail@example.com"
}
```

### Change Password
```bash
POST /api/users/change-password
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "currentPassword": "OldPass123!",
  "newPassword": "NewPass456!"
}
```

### Request Account Deletion
```bash
POST /api/users/delete-account
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "password": "MyPass123!"
}
```

### Export User Data
```bash
GET /api/users/export
Authorization: Bearer <jwt_token>
```

## Next Steps

1. Run integration tests to verify all endpoints work correctly
2. Test with frontend integration
3. Add API documentation to Swagger/OpenAPI spec
4. Consider adding rate limiting for sensitive endpoints
5. Add audit logging for profile changes and data exports

## Requirements Coverage

✅ 18.1 - Endpoint for users to retrieve their own profile information  
✅ 18.2 - Endpoint for users to update their own profile information  
✅ 18.3 - Endpoint for users to change their password  
✅ 18.4 - Endpoint for users to view their complete report history  
✅ 18.5 - Endpoint for users to delete their account and associated data  
✅ 18.6 - Validation that users can only modify their own data  
✅ 18.7 - Current password required for password changes  
✅ 18.8 - Account deletion anonymizes historical reports  
✅ 18.9 - Endpoint to export user data in JSON format for portability  

All requirements from Requirement 18 (User Profile Management) have been implemented.
