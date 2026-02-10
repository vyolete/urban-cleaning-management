# Requirements Document: Enhanced Session Management

## Introduction

This document specifies the requirements for enhancing the session management capabilities of the Urban Cleaning Management System. The current system uses simple JWT access tokens with 24-hour expiration and basic logout functionality. This enhancement will add refresh token support, token revocation, multi-device session management, session expiration warnings, and automatic token refresh to improve security and user experience.

## Glossary

- **System**: The Urban Cleaning Management WebApp
- **Access_Token**: Short-lived JWT token (15 minutes) used for API authentication
- **Refresh_Token**: Long-lived token (7 days) used to obtain new access tokens
- **Token_Pair**: A combination of access token and refresh token issued together
- **Session**: An authenticated user connection identified by a refresh token
- **Token_Revocation**: The process of invalidating a token before its natural expiration
- **Token_Blacklist**: A database table storing revoked tokens that should be rejected
- **Multi_Device_Session**: Multiple active sessions for the same user on different devices
- **Session_Expiration_Warning**: A notification shown to users before their session expires
- **Automatic_Token_Refresh**: Background process that renews access tokens before expiration
- **Device_Fingerprint**: A unique identifier for a user's device/browser combination
- **Session_Metadata**: Information about a session (device, location, last activity)

## Requirements

### Requirement 1: Refresh Token Implementation

**User Story:** As a system user, I want my session to remain active without frequent re-authentication, so that I can work efficiently without interruptions.

#### Acceptance Criteria

1. WHEN a user successfully authenticates, THE System SHALL generate both an access token (15 minutes expiration) and a refresh token (7 days expiration)
2. WHEN an access token expires, THE System SHALL accept a valid refresh token to issue a new token pair
3. THE System SHALL store refresh tokens in the database with user association, expiration time, and device information
4. WHEN a refresh token is used, THE System SHALL validate it against the database before issuing new tokens
5. WHEN a refresh token expires, THE System SHALL require full re-authentication
6. THE System SHALL implement token rotation: each refresh generates a new refresh token and invalidates the old one

### Requirement 2: Token Revocation

**User Story:** As a system user, I want to explicitly logout and invalidate my session, so that my account remains secure when I'm done using the system.

#### Acceptance Criteria

1. WHEN a user logs out, THE System SHALL revoke both the access token and refresh token
2. WHEN a token is revoked, THE System SHALL add it to a token blacklist table
3. WHEN a revoked token is used, THE System SHALL reject the request with error code "TOKEN_REVOKED"
4. THE System SHALL automatically clean up expired tokens from the blacklist after 30 days
5. WHERE ROLE_ADMIN is assigned, THE System SHALL provide endpoints to revoke all sessions for a specific user
6. WHEN an administrator revokes a user's sessions, THE System SHALL invalidate all active refresh tokens for that user

### Requirement 3: Multi-Device Session Management

**User Story:** As a system user, I want to manage my active sessions across multiple devices, so that I can control where I'm logged in.

#### Acceptance Criteria

1. WHEN a user logs in, THE System SHALL create a session record with device fingerprint, IP address, user agent, and login timestamp
2. THE System SHALL allow multiple concurrent sessions per user (up to 5 active sessions)
3. WHEN a user exceeds the session limit, THE System SHALL revoke the oldest session automatically
4. THE System SHALL provide an endpoint to list all active sessions for the current user
5. THE System SHALL display session information: device type, browser, location (city/country), last activity time
6. WHEN a user requests to revoke a specific session, THE System SHALL invalidate that session's refresh token
7. WHEN a user requests to revoke all other sessions, THE System SHALL invalidate all refresh tokens except the current one

### Requirement 4: Session Expiration Warning

**User Story:** As a system user, I want to be warned before my session expires, so that I don't lose unsaved work.

#### Acceptance Criteria

1. WHEN an access token has 2 minutes remaining before expiration, THE System SHALL send a warning notification to the frontend
2. THE System SHALL include remaining time in seconds in the warning notification
3. WHEN a user receives an expiration warning, THE Frontend SHALL display a non-intrusive notification
4. THE Frontend SHALL provide a "Stay Logged In" button that triggers token refresh
5. WHEN a user clicks "Stay Logged In", THE System SHALL use the refresh token to obtain new tokens
6. IF the refresh token is also expired, THE System SHALL redirect to login with a message explaining the session expired

### Requirement 5: Automatic Token Refresh

**User Story:** As a system user, I want my session to automatically renew while I'm actively using the system, so that I don't experience interruptions.

#### Acceptance Criteria

1. WHEN an access token has 5 minutes remaining before expiration, THE Frontend SHALL automatically request a token refresh
2. THE System SHALL use the refresh token to obtain a new token pair without user interaction
3. WHEN automatic refresh succeeds, THE Frontend SHALL update stored tokens transparently
4. WHEN automatic refresh fails due to expired refresh token, THE Frontend SHALL redirect to login
5. WHEN automatic refresh fails due to network error, THE Frontend SHALL retry up to 3 times with exponential backoff
6. THE Frontend SHALL not trigger automatic refresh if the user is inactive (no mouse/keyboard activity for 10 minutes)

### Requirement 6: Token Security Enhancements

**User Story:** As a security engineer, I want enhanced token security measures, so that the system is protected against token theft and replay attacks.

#### Acceptance Criteria

1. THE System SHALL bind refresh tokens to device fingerprints
2. WHEN a refresh token is used from a different device fingerprint, THE System SHALL reject it and revoke the token
3. THE System SHALL implement refresh token rotation: old refresh token becomes invalid after use
4. THE System SHALL detect suspicious activity: multiple failed refresh attempts from different IPs
5. WHEN suspicious activity is detected, THE System SHALL revoke all user sessions and send a security alert
6. THE System SHALL store refresh tokens as hashed values in the database (not plaintext)

### Requirement 7: Session Activity Tracking

**User Story:** As a system user, I want to see when and where my account was accessed, so that I can detect unauthorized access.

#### Acceptance Criteria

1. WHEN a user logs in, THE System SHALL record login timestamp, IP address, device information, and location
2. WHEN a user performs any authenticated action, THE System SHALL update the session's last activity timestamp
3. THE System SHALL provide an endpoint to retrieve session activity history for the current user
4. THE System SHALL display activity history with: timestamp, action type, IP address, device, location
5. THE System SHALL retain activity history for 90 days
6. WHEN a user detects suspicious activity, THE System SHALL provide a "Report Suspicious Activity" button that revokes all sessions

### Requirement 8: Refresh Token Endpoint

**User Story:** As a frontend developer, I want a dedicated refresh token endpoint, so that I can implement automatic token renewal.

#### Acceptance Criteria

1. THE System SHALL expose a POST /api/auth/refresh endpoint
2. WHEN the refresh endpoint receives a valid refresh token, THE System SHALL return a new token pair
3. WHEN the refresh endpoint receives an invalid or expired refresh token, THE System SHALL return error code "INVALID_REFRESH_TOKEN"
4. WHEN the refresh endpoint receives a revoked refresh token, THE System SHALL return error code "TOKEN_REVOKED"
5. THE System SHALL implement rate limiting on the refresh endpoint: maximum 10 requests per minute per user
6. THE System SHALL log all refresh token usage for security auditing

### Requirement 9: Session Timeout Configuration

**User Story:** As an administrator, I want to configure session timeout durations, so that I can balance security and user convenience.

#### Acceptance Criteria

1. WHERE ROLE_ADMIN is assigned, THE System SHALL provide endpoints to configure access token expiration time
2. WHERE ROLE_ADMIN is assigned, THE System SHALL provide endpoints to configure refresh token expiration time
3. THE System SHALL validate that access token expiration is shorter than refresh token expiration
4. THE System SHALL validate that expiration times are within acceptable ranges (access: 5-60 minutes, refresh: 1-30 days)
5. WHEN timeout configuration changes, THE System SHALL apply new values to newly issued tokens only
6. THE System SHALL store timeout configuration in the database with effective timestamp

### Requirement 10: Logout from All Devices

**User Story:** As a system user, I want to logout from all devices at once, so that I can quickly secure my account if needed.

#### Acceptance Criteria

1. THE System SHALL provide a POST /api/auth/logout-all endpoint
2. WHEN a user requests logout from all devices, THE System SHALL revoke all active refresh tokens for that user
3. WHEN a user requests logout from all devices, THE System SHALL add all active tokens to the blacklist
4. THE System SHALL send a confirmation message indicating the number of sessions revoked
5. THE System SHALL send email notification to the user about the logout-all action
6. THE Frontend SHALL redirect to login after successful logout-all

### Requirement 11: Session Persistence Across Browser Restarts

**User Story:** As a system user, I want my session to persist when I close and reopen my browser, so that I don't have to login repeatedly.

#### Acceptance Criteria

1. THE Frontend SHALL store refresh tokens in localStorage (not sessionStorage)
2. WHEN the browser is closed and reopened, THE Frontend SHALL attempt to use the stored refresh token
3. WHEN the stored refresh token is valid, THE Frontend SHALL obtain new access tokens automatically
4. WHEN the stored refresh token is invalid or expired, THE Frontend SHALL redirect to login
5. THE Frontend SHALL provide a "Remember Me" checkbox on login that controls token storage location
6. WHEN "Remember Me" is unchecked, THE Frontend SHALL use sessionStorage instead of localStorage

### Requirement 12: Token Introspection

**User Story:** As a system administrator, I want to inspect active tokens, so that I can monitor and manage user sessions.

#### Acceptance Criteria

1. WHERE ROLE_ADMIN is assigned, THE System SHALL provide endpoints to list all active sessions
2. WHERE ROLE_ADMIN is assigned, THE System SHALL provide endpoints to view session details by session ID
3. THE System SHALL display session information: user, device, IP, login time, last activity, expiration
4. WHERE ROLE_ADMIN is assigned, THE System SHALL provide endpoints to revoke specific sessions by session ID
5. THE System SHALL implement pagination for session listing (50 sessions per page)
6. THE System SHALL provide filtering by user, device type, and date range

### Requirement 13: Graceful Token Expiration Handling

**User Story:** As a system user, I want clear feedback when my session expires, so that I understand why I need to login again.

#### Acceptance Criteria

1. WHEN an access token expires during an API request, THE System SHALL return error code "TOKEN_EXPIRED" with HTTP 401
2. WHEN the Frontend receives "TOKEN_EXPIRED", THE Frontend SHALL attempt automatic refresh using the refresh token
3. WHEN automatic refresh succeeds, THE Frontend SHALL retry the original API request transparently
4. WHEN automatic refresh fails, THE Frontend SHALL display a modal explaining the session expired
5. THE Frontend SHALL provide options: "Login Again" or "Continue as Guest" (for public pages)
6. THE Frontend SHALL preserve the current page URL to redirect back after re-authentication

