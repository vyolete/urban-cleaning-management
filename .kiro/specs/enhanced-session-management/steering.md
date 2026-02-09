---
inclusion: manual
---

# Enhanced Session Management - Implementation Steering Guide

**Feature**: enhanced-session-management  
**Purpose**: Provide implementation guidance for secure token lifecycle and session management  
**Last Updated**: February 9, 2026

---

## 🎯 Overview

This steering guide provides critical implementation details for the Enhanced Session Management feature. It addresses security concerns, concurrency issues, and best practices identified in the project analysis.

---

## 🔐 Security Principles

### Token Security Requirements

1. **JWT Secret Configuration**
   - MUST use HS512 algorithm (requires 512-bit key)
   - Secret MUST be Base64-encoded
   - Secret MUST be stored in environment variables, NEVER in code
   - Minimum entropy: 512 bits (64 bytes)

   ```bash
   # Generate secure JWT secret
   openssl rand -base64 64
   ```

2. **Token Expiration Strategy**
   - Access Token: 15 minutes (short-lived)
   - Refresh Token: 7 days (long-lived)
   - Remember Me: 30 days (optional, user-controlled)
   - Rationale: Short access tokens limit exposure window, refresh tokens enable seamless UX

3. **Token Rotation**
   - MUST rotate refresh tokens on each use
   - Old refresh token MUST be invalidated immediately
   - Prevents token replay attacks
   - Implements "one-time use" refresh token pattern

4. **Device Fingerprinting**
   - Combine multiple signals: User-Agent, Accept-Language, Screen Resolution, Timezone
   - DO NOT rely on IP address alone (users may have dynamic IPs)
   - Hash fingerprint before storage
   - Use for anomaly detection, not as primary security mechanism

---

## 🔄 Concurrency and Race Conditions

### Critical: Refresh Token Race Conditions

**Problem**: Multiple concurrent requests with same refresh token can create race conditions.

**Solution**: Implement distributed locking or database-level constraints.

```java
@Service
public class RefreshTokenService {
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TokenPair refreshAccessToken(String refreshTokenValue) {
        // 1. Find and lock refresh token
        RefreshToken refreshToken = refreshTokenRepository
            .findByTokenValueForUpdate(refreshTokenValue)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        
        // 2. Check if already used (prevents replay)
        if (refreshToken.isUsed()) {
            // Potential token theft - revoke all user sessions
            revokeAllUserSessions(refreshToken.getUserId());
            throw new SecurityException("Refresh token already used - possible theft detected");
        }
        
        // 3. Validate expiration
        if (refreshToken.isExpired()) {
            throw new ExpiredTokenException("Refresh token expired");
        }
        
        // 4. Mark as used BEFORE generating new tokens
        refreshToken.setUsed(true);
        refreshToken.setUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        
        // 5. Generate new token pair
        String newAccessToken = generateAccessToken(refreshToken.getUser());
        RefreshToken newRefreshToken = generateRefreshToken(refreshToken.getUser(), 
            refreshToken.getDeviceFingerprint());
        
        return new TokenPair(newAccessToken, newRefreshToken.getTokenValue());
    }
}
```

**Key Points**:
- Use `SERIALIZABLE` isolation or `SELECT FOR UPDATE` to prevent concurrent access
- Mark token as used BEFORE generating new tokens
- Detect token reuse as potential security breach
- Revoke all sessions if token theft suspected

### Session Limit Enforcement

**Problem**: User creates 6th session when limit is 5 - which session to remove?

**Solution**: Remove oldest inactive session, or oldest session if all active.

```java
@Service
public class SessionService {
    
    private static final int MAX_SESSIONS_PER_USER = 5;
    
    @Transactional
    public UserSession createSession(UUID userId, String deviceFingerprint, String ipAddress) {
        List<UserSession> existingSessions = userSessionRepository
            .findByUserIdOrderByLastActivityDesc(userId);
        
        // Enforce session limit
        if (existingSessions.size() >= MAX_SESSIONS_PER_USER) {
            // Find oldest inactive session, or oldest session if all active
            UserSession toRemove = existingSessions.stream()
                .filter(s -> !s.isActive())
                .min(Comparator.comparing(UserSession::getLastActivity))
                .orElse(existingSessions.get(existingSessions.size() - 1));
            
            // Revoke the session
            revokeSession(toRemove.getId());
        }
        
        // Create new session
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setDeviceFingerprint(deviceFingerprint);
        session.setIpAddress(ipAddress);
        session.setActive(true);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        
        return userSessionRepository.save(session);
    }
}
```

---

## 📊 Database Design Considerations

### Indexes for Performance

```sql
-- Refresh tokens: frequent lookups by token value
CREATE INDEX idx_refresh_tokens_token_value ON refresh_tokens(token_value);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Token blacklist: frequent lookups during validation
CREATE INDEX idx_token_blacklist_token_value ON token_blacklist(token_value);
CREATE INDEX idx_token_blacklist_expires_at ON token_blacklist(expires_at);

-- User sessions: frequent lookups by user and session ID
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_user_sessions_last_activity ON user_sessions(last_activity);

-- Session activity: time-series queries
CREATE INDEX idx_session_activity_session_id ON session_activity(session_id);
CREATE INDEX idx_session_activity_timestamp ON session_activity(timestamp);
```

### Cleanup Jobs

**Problem**: Expired tokens and old activity logs accumulate over time.

**Solution**: Scheduled cleanup jobs.

```java
@Component
public class TokenCleanupScheduler {
    
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now();
        
        // Delete expired refresh tokens
        int deletedRefresh = refreshTokenRepository.deleteByExpiresAtBefore(cutoff);
        log.info("Deleted {} expired refresh tokens", deletedRefresh);
        
        // Delete expired blacklist entries
        int deletedBlacklist = tokenBlacklistRepository.deleteByExpiresAtBefore(cutoff);
        log.info("Deleted {} expired blacklist entries", deletedBlacklist);
    }
    
    @Scheduled(cron = "0 0 3 * * SUN") // Run at 3 AM every Sunday
    public void cleanupOldSessionActivity() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        
        // Delete session activity older than 90 days
        int deleted = sessionActivityRepository.deleteByTimestampBefore(cutoff);
        log.info("Deleted {} old session activity records", deleted);
    }
}
```

---

## 🔍 Token Validation Strategy

### Layered Validation Approach

```java
@Component
public class TokenValidator {
    
    public void validateAccessToken(String token) {
        // Layer 1: JWT signature and structure
        if (!jwtTokenProvider.validateToken(token)) {
            throw new InvalidTokenException("Invalid token signature");
        }
        
        // Layer 2: Expiration check
        if (jwtTokenProvider.isTokenExpired(token)) {
            throw new ExpiredTokenException("Token expired");
        }
        
        // Layer 3: Blacklist check (for logout/revocation)
        if (tokenBlacklistRepository.existsByTokenValue(token)) {
            throw new RevokedTokenException("Token has been revoked");
        }
        
        // Layer 4: User still exists and is active
        String username = jwtTokenProvider.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (!user.isActive()) {
            throw new InactiveUserException("User account is inactive");
        }
    }
}
```

**Performance Optimization**: Cache blacklist in Redis for faster lookups.

```java
@Service
public class TokenBlacklistService {
    
    @Cacheable(value = "token-blacklist", key = "#tokenValue")
    public boolean isBlacklisted(String tokenValue) {
        return tokenBlacklistRepository.existsByTokenValue(tokenValue);
    }
    
    @CacheEvict(value = "token-blacklist", key = "#tokenValue")
    public void addToBlacklist(String tokenValue, LocalDateTime expiresAt) {
        TokenBlacklist entry = new TokenBlacklist();
        entry.setTokenValue(tokenValue);
        entry.setBlacklistedAt(LocalDateTime.now());
        entry.setExpiresAt(expiresAt);
        tokenBlacklistRepository.save(entry);
    }
}
```

---

## 🎨 Frontend Implementation Patterns

### Automatic Token Refresh

**Problem**: Access token expires while user is active - don't interrupt their flow.

**Solution**: Proactive token refresh before expiration.

```javascript
// services/tokenRefreshService.js
import api from './api';
import { jwtDecode } from 'jwt-decode';

class TokenRefreshService {
  constructor() {
    this.refreshTimer = null;
    this.REFRESH_BEFORE_EXPIRY = 5 * 60 * 1000; // 5 minutes in ms
  }

  scheduleTokenRefresh(accessToken) {
    // Clear existing timer
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }

    // Decode token to get expiration
    const decoded = jwtDecode(accessToken);
    const expiresAt = decoded.exp * 1000; // Convert to milliseconds
    const now = Date.now();
    const timeUntilRefresh = expiresAt - now - this.REFRESH_BEFORE_EXPIRY;

    // Schedule refresh
    if (timeUntilRefresh > 0) {
      this.refreshTimer = setTimeout(() => {
        this.refreshToken();
      }, timeUntilRefresh);
    } else {
      // Token expires soon, refresh immediately
      this.refreshToken();
    }
  }

  async refreshToken() {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }

      const response = await api.post('/auth/refresh', { refreshToken });
      const { accessToken, refreshToken: newRefreshToken } = response.data;

      // Update stored tokens
      localStorage.setItem('token', accessToken);
      localStorage.setItem('refreshToken', newRefreshToken);

      // Schedule next refresh
      this.scheduleTokenRefresh(accessToken);

      return accessToken;
    } catch (error) {
      console.error('Token refresh failed:', error);
      // Redirect to login
      window.location.href = '/login';
    }
  }

  stopRefreshTimer() {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }
}

export default new TokenRefreshService();
```

### Axios Interceptor for Automatic Retry

```javascript
// services/api.js
import axios from 'axios';
import tokenRefreshService from './tokenRefreshService';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
});

// Request interceptor: Add token to headers
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: Handle 401 and retry with refreshed token
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and we haven't retried yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Refresh token
        const newAccessToken = await tokenRefreshService.refreshToken();

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed, redirect to login
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

### Session Expiration Warning

```javascript
// components/SessionExpirationWarning.jsx
import React, { useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import tokenRefreshService from '../services/tokenRefreshService';

const SessionExpirationWarning = () => {
  const [showWarning, setShowWarning] = useState(false);
  const [timeRemaining, setTimeRemaining] = useState(0);

  useEffect(() => {
    const checkExpiration = () => {
      const token = localStorage.getItem('token');
      if (!token) return;

      const decoded = jwtDecode(token);
      const expiresAt = decoded.exp * 1000;
      const now = Date.now();
      const remaining = expiresAt - now;

      // Show warning 2 minutes before expiration
      if (remaining > 0 && remaining < 2 * 60 * 1000) {
        setShowWarning(true);
        setTimeRemaining(Math.floor(remaining / 1000));
      } else {
        setShowWarning(false);
      }
    };

    // Check every 10 seconds
    const interval = setInterval(checkExpiration, 10000);
    checkExpiration(); // Check immediately

    return () => clearInterval(interval);
  }, []);

  const handleExtendSession = async () => {
    await tokenRefreshService.refreshToken();
    setShowWarning(false);
  };

  if (!showWarning) return null;

  return (
    <div className="session-warning-banner">
      <p>
        Tu sesión expirará en {timeRemaining} segundos.
      </p>
      <button onClick={handleExtendSession}>
        Extender Sesión
      </button>
    </div>
  );
};

export default SessionExpirationWarning;
```

---

## 🧪 Testing Strategy

### Property-Based Testing for Token Operations

```java
@Property(trials = 100)
@Tag("Feature: enhanced-session-management, Property 1: Token generation uniqueness")
public void generatedTokensAreUnique(
    @ForAll @Size(min = 10) List<@From("users") User> users) {
    
    Set<String> generatedTokens = new HashSet<>();
    
    for (User user : users) {
        String token = tokenService.generateAccessToken(user);
        
        // Each token should be unique
        assertThat(generatedTokens).doesNotContain(token);
        generatedTokens.add(token);
    }
}

@Property(trials = 100)
@Tag("Feature: enhanced-session-management, Property 5: Refresh token rotation")
public void refreshTokenRotationInvalidatesOldToken(
    @ForAll @From("users") User user) {
    
    // Generate initial refresh token
    RefreshToken initialToken = refreshTokenService.generateRefreshToken(user, "device1");
    String initialValue = initialToken.getTokenValue();
    
    // Use refresh token to get new pair
    TokenPair newPair = refreshTokenService.refreshAccessToken(initialValue);
    
    // Old token should be marked as used
    RefreshToken oldToken = refreshTokenRepository.findByTokenValue(initialValue).orElseThrow();
    assertThat(oldToken.isUsed()).isTrue();
    
    // Attempting to use old token again should fail
    assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(initialValue))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("already used");
}

@Property(trials = 100)
@Tag("Feature: enhanced-session-management, Property 12: Session limit enforcement")
public void sessionLimitIsEnforced(
    @ForAll @From("users") User user,
    @ForAll @Size(min = 6, max = 10) List<String> deviceFingerprints) {
    
    // Create more sessions than the limit
    for (String fingerprint : deviceFingerprints) {
        sessionService.createSession(user.getId(), fingerprint, "192.168.1.1");
    }
    
    // User should have exactly MAX_SESSIONS_PER_USER active sessions
    List<UserSession> activeSessions = userSessionRepository
        .findByUserIdAndActiveTrue(user.getId());
    
    assertThat(activeSessions).hasSize(SessionService.MAX_SESSIONS_PER_USER);
}
```

### Unit Tests for Edge Cases

```java
@Test
public void refreshToken_whenUsedConcurrently_onlyOneSucceeds() throws Exception {
    // Setup
    User user = createTestUser();
    RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user, "device1");
    String tokenValue = refreshToken.getTokenValue();
    
    // Execute: Two threads try to use same refresh token
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(2);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    
    for (int i = 0; i < 2; i++) {
        executor.submit(() -> {
            try {
                refreshTokenService.refreshAccessToken(tokenValue);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    
    // Verify: Only one should succeed
    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failureCount.get()).isEqualTo(1);
}

@Test
public void logout_invalidatesAllUserTokens() {
    // Setup
    User user = createTestUser();
    String accessToken1 = tokenService.generateAccessToken(user);
    String accessToken2 = tokenService.generateAccessToken(user);
    RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user, "device1");
    
    // Execute
    authService.logout(user.getId());
    
    // Verify: All tokens should be blacklisted
    assertThat(tokenBlacklistService.isBlacklisted(accessToken1)).isTrue();
    assertThat(tokenBlacklistService.isBlacklisted(accessToken2)).isTrue();
    
    // Refresh token should be revoked
    RefreshToken revokedToken = refreshTokenRepository
        .findByTokenValue(refreshToken.getTokenValue()).orElseThrow();
    assertThat(revokedToken.isRevoked()).isTrue();
}
```

---

## 🚨 Common Pitfalls and Solutions

### Pitfall 1: Storing Tokens in LocalStorage (XSS Vulnerability)

**Problem**: LocalStorage is accessible to JavaScript, making it vulnerable to XSS attacks.

**Solution**: Use httpOnly cookies for refresh tokens, localStorage only for access tokens.

```java
@PostMapping("/auth/login")
public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, 
                                           HttpServletResponse response) {
    // Authenticate user
    User user = authService.authenticate(request.getUsername(), request.getPassword());
    
    // Generate tokens
    String accessToken = tokenService.generateAccessToken(user);
    RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user, 
        extractDeviceFingerprint(request));
    
    // Set refresh token as httpOnly cookie
    Cookie refreshCookie = new Cookie("refreshToken", refreshToken.getTokenValue());
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(true); // HTTPS only
    refreshCookie.setPath("/api/auth/refresh");
    refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
    response.addCookie(refreshCookie);
    
    // Return access token in response body
    return ResponseEntity.ok(new LoginResponse(accessToken, user.getRole(), user.getUsername()));
}
```

### Pitfall 2: Not Handling Token Refresh During Concurrent Requests

**Problem**: Multiple API calls happen simultaneously, all get 401, all try to refresh.

**Solution**: Queue refresh requests, only execute one.

```javascript
// services/tokenRefreshService.js
class TokenRefreshService {
  constructor() {
    this.refreshPromise = null;
  }

  async refreshToken() {
    // If refresh is already in progress, return the same promise
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    // Start new refresh
    this.refreshPromise = this._doRefresh();

    try {
      const result = await this.refreshPromise;
      return result;
    } finally {
      // Clear promise when done
      this.refreshPromise = null;
    }
  }

  async _doRefresh() {
    // Actual refresh logic here
    const response = await api.post('/auth/refresh', {
      refreshToken: localStorage.getItem('refreshToken')
    });
    
    const { accessToken, refreshToken } = response.data;
    localStorage.setItem('token', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    
    return accessToken;
  }
}
```

### Pitfall 3: Not Cleaning Up Timers on Logout

**Problem**: Token refresh timer continues after logout, causing errors.

**Solution**: Always clean up timers.

```javascript
// context/AuthContext.jsx
const logout = () => {
  // Stop token refresh timer
  tokenRefreshService.stopRefreshTimer();
  
  // Clear tokens
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  
  // Clear user state
  setUser(null);
  
  // Redirect to login
  navigate('/login');
};
```

---

## 📈 Monitoring and Observability

### Metrics to Track

```java
@Component
public class SessionMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordTokenRefresh(boolean success) {
        meterRegistry.counter("token.refresh", 
            "success", String.valueOf(success)).increment();
    }
    
    public void recordSessionCreation() {
        meterRegistry.counter("session.created").increment();
    }
    
    public void recordSessionRevocation(String reason) {
        meterRegistry.counter("session.revoked", 
            "reason", reason).increment();
    }
    
    public void recordConcurrentRefreshAttempt() {
        meterRegistry.counter("token.refresh.concurrent").increment();
    }
    
    public void recordTokenTheftDetection() {
        meterRegistry.counter("security.token_theft_detected").increment();
    }
}
```

### Logging Strategy

```java
@Slf4j
@Service
public class RefreshTokenService {
    
    public TokenPair refreshAccessToken(String refreshTokenValue) {
        log.info("Token refresh attempt for token: {}", 
            maskToken(refreshTokenValue));
        
        try {
            RefreshToken refreshToken = refreshTokenRepository
                .findByTokenValueForUpdate(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found: {}", maskToken(refreshTokenValue));
                    return new InvalidTokenException("Refresh token not found");
                });
            
            if (refreshToken.isUsed()) {
                log.error("SECURITY: Refresh token reuse detected for user: {}, token: {}", 
                    refreshToken.getUserId(), maskToken(refreshTokenValue));
                sessionMetrics.recordTokenTheftDetection();
                revokeAllUserSessions(refreshToken.getUserId());
                throw new SecurityException("Refresh token already used");
            }
            
            // ... rest of logic
            
            log.info("Token refresh successful for user: {}", refreshToken.getUserId());
            return newPair;
            
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private String maskToken(String token) {
        if (token == null || token.length() < 10) return "***";
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
```

---

## ✅ Implementation Checklist

Before marking tasks as complete, verify:

- [ ] JWT secret is 512 bits and Base64-encoded
- [ ] Access tokens expire in 15 minutes
- [ ] Refresh tokens expire in 7 days
- [ ] Refresh tokens are rotated on each use
- [ ] Old refresh tokens are marked as used
- [ ] Token reuse triggers security alert and session revocation
- [ ] Session limit (5) is enforced
- [ ] Oldest inactive session is removed when limit exceeded
- [ ] Token blacklist is checked on every request
- [ ] Expired tokens and blacklist entries are cleaned up daily
- [ ] Frontend automatically refreshes tokens 5 minutes before expiration
- [ ] Concurrent refresh requests are queued (only one executes)
- [ ] Session expiration warning appears 2 minutes before expiration
- [ ] All token operations are logged
- [ ] Security metrics are tracked
- [ ] Property-based tests cover all correctness properties
- [ ] Unit tests cover edge cases and error conditions
- [ ] Integration tests verify end-to-end flows

---

## 📚 References

- [RFC 6749: OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)
- [RFC 7519: JSON Web Token (JWT)](https://datatracker.ietf.org/doc/html/rfc7519)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [Token Rotation Best Practices](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)

---

**Remember**: Security is not a feature, it's a requirement. Every decision should prioritize user security and data protection.
