# Implementation Plan: Enhanced Session Management

## Overview

This implementation plan breaks down the enhanced session management feature into incremental, testable tasks. The plan follows a bottom-up approach: database schema → backend services → API endpoints → frontend integration → testing.

## Tasks

- [ ] 1. Database Schema and Entities
  - Create database migration scripts for new tables
  - Implement JPA entities with proper indexes
  - _Requirements: 1.3, 2.2, 3.1, 7.1_

- [ ] 1.1 Create RefreshToken entity and repository
  - Define RefreshToken entity with all fields (token_hash, token_id, user_id, device_fingerprint, etc.)
  - Add indexes on token_hash, token_id, user_id, expires_at
  - Create RefreshTokenRepository with custom queries
  - _Requirements: 1.3_

- [ ] 1.2 Create TokenBlacklist entity and repository
  - Define TokenBlacklist entity (token_hash, token_type, user_id, revoked_at, expires_at)
  - Add indexes on token_hash and expires_at
  - Create TokenBlacklistRepository with cleanup queries
  - _Requirements: 2.2_

- [ ] 1.3 Create UserSession entity and repository
  - Define UserSession entity with device info and location fields
  - Add indexes on user_id, refresh_token_id, active
  - Create UserSessionRepository with active session queries
  - _Requirements: 3.1_

- [ ] 1.4 Create SessionActivity entity and repository
  - Define SessionActivity entity for audit trail
  - Add indexes on session_id, user_id, timestamp
  - Create SessionActivityRepository with history queries
  - _Requirements: 7.1_

- [ ]* 1.5 Write property test for database entities
  - **Property 2: Refresh token database persistence**
  - **Validates: Requirements 1.3**

- [ ] 2. Core Token Service Implementation
  - Implement token generation, validation, and hashing
  - Add blacklist checking logic
  - _Requirements: 1.1, 1.2, 2.3_

- [ ] 2.1 Implement TokenService.generateTokenPair()
  - Generate access token with 15-minute expiration
  - Generate refresh token with 7-day expiration
  - Include proper claims (userId, role, tokenType, deviceFingerprint)
  - _Requirements: 1.1_

- [ ]* 2.2 Write property test for dual token generation
  - **Property 1: Dual token generation on authentication**
  - **Validates: Requirements 1.1**

- [ ] 2.3 Implement TokenService.validateToken()
  - Parse and validate JWT signature
  - Check expiration
  - Extract claims
  - Handle ExpiredJwtException and JwtException
  - _Requirements: 1.2, 1.5_

- [ ] 2.4 Implement TokenService.isTokenRevoked()
  - Hash incoming token
  - Check if hash exists in blacklist
  - _Requirements: 2.3_

- [ ]* 2.5 Write property test for revoked token rejection
  - **Property 6: Revoked token rejection**
  - **Validates: Requirements 2.3**

- [ ] 2.6 Implement TokenService.hashToken()
  - Use SHA-256 for one-way hashing
  - Ensure consistent hashing for same input
  - _Requirements: 6.6_

- [ ]* 2.7 Write property test for token hashing
  - **Property 14: Refresh tokens stored as hashes**
  - **Validates: Requirements 6.6**

- [ ] 3. Refresh Token Service Implementation
  - Implement refresh token storage, validation, and rotation
  - Add device fingerprint checking
  - _Requirements: 1.3, 1.4, 1.6, 6.1, 6.2, 6.3_

- [ ] 3.1 Implement RefreshTokenService.storeRefreshToken()
  - Hash token before storage
  - Store with user, device fingerprint, IP, user agent
  - Set issued_at, expires_at, last_used_at timestamps
  - _Requirements: 1.3_

- [ ] 3.2 Implement RefreshTokenService.validateRefreshToken()
  - Validate JWT structure
  - Check if token is revoked
  - Find token in database by hash
  - Verify device fingerprint matches
  - Update last_used_at timestamp
  - _Requirements: 1.4, 6.1, 6.2_

- [ ]* 3.3 Write property test for device fingerprint binding
  - **Property 13: Device fingerprint binding**
  - **Validates: Requirements 6.1, 6.2**

- [ ] 3.4 Implement RefreshTokenService.rotateRefreshToken()
  - Revoke old refresh token
  - Generate new token pair
  - Store new refresh token
  - Return new token pair
  - _Requirements: 1.6, 6.3_

- [ ]* 3.5 Write property test for token rotation
  - **Property 3: Token rotation invalidates old token**
  - **Validates: Requirements 1.6, 6.3**

- [ ] 3.6 Implement RefreshTokenService.revokeToken()
  - Add token to blacklist
  - Delete from refresh_tokens table
  - _Requirements: 2.1, 2.2_

- [ ] 3.7 Implement RefreshTokenService.revokeAllUserTokens()
  - Find all user's refresh tokens
  - Revoke each token
  - Return count of revoked tokens
  - _Requirements: 2.6, 10.2_

- [ ]* 3.8 Write property test for bulk revocation
  - **Property 8: Admin bulk revocation**
  - **Validates: Requirements 2.6**

- [ ] 3.9 Implement RefreshTokenService.cleanupExpiredTokens()
  - Scheduled task (daily at 2 AM)
  - Delete expired refresh tokens
  - Delete old blacklist entries (30+ days past expiration)
  - _Requirements: 2.4_

- [ ]* 3.10 Write property test for blacklist cleanup
  - **Property 7: Blacklist cleanup after 30 days**
  - **Validates: Requirements 2.4**

- [ ] 4. Session Service Implementation
  - Implement session creation, tracking, and management
  - Add session limit enforcement
  - _Requirements: 3.1, 3.2, 3.3, 3.6, 3.7, 7.2_

- [ ] 4.1 Implement SessionService.createSession()
  - Check if user has too many sessions (>= 5)
  - If limit exceeded, revoke oldest session
  - Parse device info from user agent
  - Get geolocation from IP address
  - Create and save session record
  - _Requirements: 3.1, 3.2, 3.3_

- [ ]* 4.2 Write property test for session limit
  - **Property 10: Maximum 5 concurrent sessions**
  - **Validates: Requirements 3.2, 3.3**

- [ ] 4.3 Implement SessionService.updateSessionActivity()
  - Find session by refresh_token_id
  - Update last_activity_at timestamp
  - _Requirements: 7.2_

- [ ]* 4.4 Write property test for activity timestamp update
  - **Property 21: Last activity timestamp update**
  - **Validates: Requirements 7.2**

- [ ] 4.5 Implement SessionService.getUserSessions()
  - Find all active sessions for user
  - Map to SessionResponse DTOs
  - _Requirements: 3.4_

- [ ] 4.6 Implement SessionService.revokeSession()
  - Mark session as inactive
  - Set revoked_at timestamp
  - Revoke associated refresh token
  - _Requirements: 3.6_

- [ ]* 4.7 Write property test for session revocation
  - **Property 11: Session revocation invalidates refresh token**
  - **Validates: Requirements 3.6**

- [ ] 4.8 Implement SessionService.revokeAllOtherSessions()
  - Find all active sessions except current
  - Revoke each session
  - Return count of revoked sessions
  - _Requirements: 3.7_

- [ ]* 4.9 Write property test for revoke all except current
  - **Property 12: Revoke all except current**
  - **Validates: Requirements 3.7**

- [ ] 5. Device Fingerprint Service Implementation
  - Implement device fingerprint generation and parsing
  - _Requirements: 6.1_

- [ ] 5.1 Implement DeviceFingerprintService.generateFingerprint()
  - Extract User-Agent, Accept-Language, Accept-Encoding headers
  - Combine headers into unique string
  - Hash with SHA-256
  - _Requirements: 6.1_

- [ ] 5.2 Implement DeviceFingerprintService.parseDeviceInfo()
  - Use UserAgentParser library
  - Extract device type (Mobile/Desktop/Tablet)
  - Extract browser name and version
  - Extract operating system
  - _Requirements: 3.1_

- [ ] 5.3 Add GeoLocationService for IP-based location
  - Integrate with IP geolocation API (e.g., MaxMind GeoIP2)
  - Extract city and country from IP address
  - Handle API failures gracefully
  - _Requirements: 3.1_

- [ ] 6. Authentication Controller Endpoints
  - Update existing auth endpoints
  - Add new refresh and session management endpoints
  - _Requirements: 1.1, 1.2, 8.1, 8.2, 10.1_

- [ ] 6.1 Update AuthController.login() to return token pair
  - Generate device fingerprint from request
  - Generate token pair with TokenService
  - Store refresh token in database
  - Create session record
  - Return LoginResponse with both tokens
  - _Requirements: 1.1_

- [ ] 6.2 Create AuthController.refresh() endpoint
  - POST /api/auth/refresh
  - Extract refresh token from request body
  - Generate device fingerprint
  - Validate refresh token
  - Rotate tokens
  - Update session activity
  - Return new token pair
  - _Requirements: 1.2, 8.1, 8.2_

- [ ]* 6.3 Write property test for refresh endpoint
  - **Property 23: Refresh endpoint returns new token pair**
  - **Validates: Requirements 8.2**

- [ ] 6.4 Update AuthController.logout() to revoke tokens
  - Extract refresh token from request
  - Revoke refresh token (add to blacklist)
  - Revoke session
  - Return success response
  - _Requirements: 2.1_

- [ ]* 6.5 Write property test for logout revocation
  - **Property 5: Logout revokes both tokens**
  - **Validates: Requirements 2.1**

- [ ] 6.6 Create AuthController.logoutAll() endpoint
  - POST /api/auth/logout-all
  - Get current user from security context
  - Revoke all user's refresh tokens
  - Return count of revoked sessions
  - Send email notification
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [ ]* 6.7 Write property test for logout all
  - **Property 30: Logout all revokes all refresh tokens**
  - **Validates: Requirements 10.2, 10.3**

- [ ] 6.8 Create AuthController.getSessions() endpoint
  - GET /api/auth/sessions
  - Get current user from security context
  - Return list of active sessions
  - _Requirements: 3.4_

- [ ] 6.9 Create AuthController.revokeSession() endpoint
  - DELETE /api/auth/sessions/{sessionId}
  - Verify session belongs to current user
  - Revoke session
  - Return success response
  - _Requirements: 3.6_

- [ ] 6.10 Create AuthController.revokeOtherSessions() endpoint
  - POST /api/auth/sessions/revoke-others
  - Get current refresh token ID from request
  - Revoke all other sessions
  - Return count of revoked sessions
  - _Requirements: 3.7_

- [ ] 7. Security Filter Updates
  - Update JWT authentication filter to check blacklist
  - Add rate limiting for refresh endpoint
  - _Requirements: 2.3, 8.5_

- [ ] 7.1 Update JwtAuthenticationFilter to check blacklist
  - After validating JWT, check if token is in blacklist
  - If revoked, reject request with TOKEN_REVOKED error
  - _Requirements: 2.3_

- [ ] 7.2 Create RefreshTokenRateLimitFilter
  - Implement rate limiting: max 10 requests per minute per user
  - Use in-memory cache or Redis for rate limit tracking
  - Return 429 Too Many Requests if limit exceeded
  - Add Retry-After header
  - _Requirements: 8.5_

- [ ]* 7.3 Write property test for rate limiting
  - **Property 26: Refresh endpoint rate limiting**
  - **Validates: Requirements 8.5**

- [ ] 8. Admin Session Management Endpoints
  - Add admin endpoints for session introspection
  - _Requirements: 12.1, 12.2, 12.4_

- [ ] 8.1 Create AdminSessionController.getAllSessions() endpoint
  - GET /api/admin/sessions
  - Require ROLE_ADMIN
  - Support pagination (50 per page)
  - Support filtering by user, device type, date range
  - Return list of all sessions
  - _Requirements: 12.1, 12.5, 12.6_

- [ ] 8.2 Create AdminSessionController.getSessionDetails() endpoint
  - GET /api/admin/sessions/{sessionId}
  - Require ROLE_ADMIN
  - Return detailed session information
  - _Requirements: 12.2, 12.3_

- [ ] 8.3 Create AdminSessionController.revokeSession() endpoint
  - DELETE /api/admin/sessions/{sessionId}
  - Require ROLE_ADMIN
  - Revoke specified session
  - Return success response
  - _Requirements: 12.4_

- [ ] 8.4 Create AdminSessionController.revokeUserSessions() endpoint
  - POST /api/admin/users/{userId}/revoke-sessions
  - Require ROLE_ADMIN
  - Revoke all sessions for specified user
  - Return count of revoked sessions
  - _Requirements: 2.5, 2.6_

- [ ] 9. Frontend: Token Refresh Service
  - Implement automatic token refresh logic
  - Add retry mechanism for network failures
  - _Requirements: 5.1, 5.2, 5.3, 5.5_

- [ ] 9.1 Create TokenRefreshService.js
  - Implement checkTokenExpiration() to monitor access token
  - Trigger refresh when 5 minutes remaining
  - Implement refreshTokens() to call /api/auth/refresh
  - Update localStorage with new tokens
  - _Requirements: 5.1, 5.2, 5.3_

- [ ]* 9.2 Write property test for automatic refresh timing
  - **Property 16: Automatic refresh at 5 minutes remaining**
  - **Validates: Requirements 5.1**

- [ ]* 9.3 Write property test for silent token update
  - **Property 17: Silent token update**
  - **Validates: Requirements 5.2, 5.3**

- [ ] 9.4 Implement retry logic with exponential backoff
  - Retry up to 3 times on network failure
  - Use exponential backoff: 1s, 2s, 4s
  - Redirect to login if all retries fail
  - _Requirements: 5.5_

- [ ]* 9.5 Write property test for retry logic
  - **Property 18: Retry on network failure**
  - **Validates: Requirements 5.5**

- [ ] 9.6 Implement inactivity detection
  - Track mouse and keyboard events
  - Mark user as inactive after 10 minutes of no activity
  - Skip automatic refresh if user is inactive
  - _Requirements: 5.6_

- [ ]* 9.7 Write property test for inactivity detection
  - **Property 19: No refresh during inactivity**
  - **Validates: Requirements 5.6**

- [ ] 10. Frontend: Session Expiration Warning
  - Implement warning notification before expiration
  - Add "Stay Logged In" button
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 10.1 Create SessionExpirationWarning component
  - Display non-intrusive notification
  - Show remaining time in seconds
  - Provide "Stay Logged In" button
  - Auto-dismiss after user action
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 10.2 Implement warning trigger logic
  - Monitor access token expiration
  - Show warning at 2 minutes remaining
  - Update countdown every second
  - _Requirements: 4.1, 4.2_

- [ ] 10.3 Implement "Stay Logged In" action
  - Call TokenRefreshService.refreshTokens()
  - Dismiss warning on success
  - Show error message on failure
  - Redirect to login if refresh token expired
  - _Requirements: 4.5, 4.6_

- [ ] 11. Frontend: Session Management UI
  - Create session list and management interface
  - _Requirements: 3.4, 3.5, 3.6, 3.7_

- [ ] 11.1 Create SessionList component
  - Display all active sessions
  - Show device type, browser, location, last activity
  - Highlight current session
  - Provide "Revoke" button for each session
  - _Requirements: 3.4, 3.5_

- [ ] 11.2 Create SessionItem component
  - Display session details with icons
  - Format last activity as relative time ("2 hours ago")
  - Show device icon based on type (mobile/desktop/tablet)
  - _Requirements: 3.5_

- [ ] 11.3 Implement session revocation actions
  - Call /api/auth/sessions/{id} DELETE for single revocation
  - Call /api/auth/sessions/revoke-others POST for bulk revocation
  - Show confirmation dialog before revocation
  - Refresh session list after revocation
  - _Requirements: 3.6, 3.7_

- [ ] 11.4 Add "Logout from All Devices" button
  - Call /api/auth/logout-all POST
  - Show confirmation dialog with warning
  - Display count of revoked sessions
  - Redirect to login after success
  - _Requirements: 10.1, 10.2, 10.6_

- [ ] 12. Frontend: AuthContext Updates
  - Update authentication context for token management
  - _Requirements: 1.1, 11.1, 11.2, 11.3_

- [ ] 12.1 Update AuthContext to store both tokens
  - Store accessToken and refreshToken separately
  - Store token expiration times
  - Provide getAccessToken() and getRefreshToken() methods
  - _Requirements: 1.1_

- [ ] 12.2 Update AuthContext.login() to handle token pair
  - Store both access and refresh tokens
  - Calculate and store expiration times
  - Start automatic refresh monitoring
  - _Requirements: 1.1_

- [ ] 12.3 Implement AuthContext.refreshTokens()
  - Call TokenRefreshService.refreshTokens()
  - Update stored tokens
  - Reset expiration monitoring
  - _Requirements: 1.2_

- [ ] 12.4 Update AuthContext.logout() to revoke tokens
  - Call /api/auth/logout with refresh token
  - Clear stored tokens
  - Stop automatic refresh monitoring
  - _Requirements: 2.1_

- [ ] 12.5 Implement session persistence on browser restart
  - Check localStorage for refresh token on app load
  - Attempt automatic refresh if token exists
  - Redirect to login if refresh fails
  - _Requirements: 11.2, 11.3_

- [ ]* 12.6 Write property test for browser restart persistence
  - **Property 32: Browser restart uses stored refresh token**
  - **Validates: Requirements 11.2, 11.3**

- [ ] 13. Frontend: API Interceptor for Token Expiration
  - Handle TOKEN_EXPIRED errors automatically
  - Retry failed requests after refresh
  - _Requirements: 13.1, 13.2, 13.3_

- [ ] 13.1 Create API response interceptor
  - Intercept all API responses
  - Detect TOKEN_EXPIRED error (401 with specific code)
  - Trigger automatic token refresh
  - _Requirements: 13.1, 13.2_

- [ ]* 13.2 Write property test for TOKEN_EXPIRED detection
  - **Property 33: Expired token returns TOKEN_EXPIRED**
  - **Validates: Requirements 13.1**

- [ ]* 13.3 Write property test for automatic refresh on error
  - **Property 34: Automatic refresh on TOKEN_EXPIRED**
  - **Validates: Requirements 13.2**

- [ ] 13.2 Implement request retry after refresh
  - Store original request configuration
  - After successful refresh, retry request with new token
  - Return retry response to original caller
  - _Requirements: 13.3_

- [ ]* 13.4 Write property test for request retry
  - **Property 35: Request retry after successful refresh**
  - **Validates: Requirements 13.3**

- [ ] 13.3 Implement session expiration modal
  - Show modal when refresh fails
  - Display clear message about session expiration
  - Provide "Login Again" and "Continue as Guest" options
  - Preserve current URL for redirect after login
  - _Requirements: 13.4, 13.5, 13.6_

- [ ]* 13.5 Write property test for URL preservation
  - **Property 36: URL preservation on session expiration**
  - **Validates: Requirements 13.6**

- [ ] 14. Frontend: Remember Me Feature
  - Add "Remember Me" checkbox to login
  - Control token storage location
  - _Requirements: 11.5, 11.6_

- [ ] 14.1 Add "Remember Me" checkbox to LoginPage
  - Add checkbox to login form
  - Store preference in component state
  - Pass preference to login function
  - _Requirements: 11.5_

- [ ] 14.2 Implement conditional token storage
  - If "Remember Me" checked, use localStorage
  - If unchecked, use sessionStorage
  - Update TokenRefreshService to check both storage locations
  - _Requirements: 11.6_

- [ ] 15. Backend: Session Activity Tracking
  - Log user activity for security auditing
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 15.1 Create ActivityLoggingInterceptor
  - Intercept all authenticated requests
  - Log action type, IP address, timestamp
  - Store in session_activity table
  - _Requirements: 7.1, 7.2_

- [ ]* 15.2 Write property test for activity logging
  - **Property 20: Activity logging on login**
  - **Validates: Requirements 7.1**

- [ ] 15.3 Create SessionActivityController.getActivityHistory() endpoint
  - GET /api/auth/activity
  - Return activity history for current user
  - Support pagination and date range filtering
  - _Requirements: 7.3, 7.4_

- [ ] 15.4 Implement activity history cleanup
  - Scheduled task to delete records older than 90 days
  - Run daily at 3 AM
  - _Requirements: 7.5_

- [ ]* 15.5 Write property test for activity retention
  - **Property 22: Activity history retention**
  - **Validates: Requirements 7.5**

- [ ] 16. Backend: Session Timeout Configuration
  - Add admin endpoints to configure token expiration times
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

- [ ] 16.1 Create TokenConfigController.getConfig() endpoint
  - GET /api/admin/config/token-expiration
  - Require ROLE_ADMIN
  - Return current access and refresh token expiration times
  - _Requirements: 9.1, 9.2_

- [ ] 16.2 Create TokenConfigController.updateConfig() endpoint
  - PUT /api/admin/config/token-expiration
  - Require ROLE_ADMIN
  - Validate expiration times (access < refresh, within ranges)
  - Store new configuration with timestamp
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [ ]* 16.3 Write property test for configuration validation
  - **Property 27: Access token shorter than refresh token**
  - **Property 28: Expiration time range validation**
  - **Validates: Requirements 9.3, 9.4**

- [ ] 16.3 Implement configuration application logic
  - Apply new config to newly issued tokens only
  - Existing tokens keep original expiration
  - _Requirements: 9.5_

- [ ]* 16.4 Write property test for configuration application
  - **Property 29: Configuration applies to new tokens only**
  - **Validates: Requirements 9.5**

- [ ] 17. Integration Testing
  - Test complete session lifecycle end-to-end
  - _Requirements: All_

- [ ] 17.1 Write E2E test for complete session lifecycle
  - Login → Refresh → Logout flow
  - Verify tokens at each step
  - Verify session records created/updated/deleted
  - _Requirements: 1.1, 1.2, 2.1_

- [ ] 17.2 Write E2E test for multi-device sessions
  - Login from 6 different devices
  - Verify oldest session revoked
  - Verify 5 active sessions remain
  - _Requirements: 3.2, 3.3_

- [ ] 17.3 Write E2E test for session revocation
  - Create multiple sessions
  - Revoke specific session
  - Revoke all other sessions
  - Logout from all devices
  - _Requirements: 3.6, 3.7, 10.1_

- [ ] 17.4 Write E2E test for automatic token refresh
  - Simulate token near expiration
  - Verify automatic refresh triggered
  - Verify API requests continue working
  - _Requirements: 5.1, 5.2, 13.2, 13.3_

- [ ] 18. Documentation and Cleanup
  - Update API documentation
  - Remove debug logs
  - Update README

- [ ] 18.1 Update API documentation
  - Document new endpoints in OpenAPI/Swagger
  - Add request/response examples
  - Document error codes

- [ ] 18.2 Remove debug console.log statements
  - Clean up frontend debug logs
  - Ensure no sensitive data logged

- [ ] 18.3 Update README with session management features
  - Document token lifecycle
  - Document session management UI
  - Add troubleshooting section

## Notes

- Tasks marked with `*` are optional property-based tests
- Each task references specific requirements for traceability
- Implementation should be done incrementally, testing after each task
- Property tests should run with minimum 100 iterations
- Integration tests should cover all critical user flows
