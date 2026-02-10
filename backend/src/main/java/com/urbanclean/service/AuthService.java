package com.urbanclean.service;

import com.urbanclean.dto.request.LoginRequest;
import com.urbanclean.dto.request.RegisterRequest;
import com.urbanclean.dto.response.LoginResponse;
import com.urbanclean.dto.response.RefreshTokenResponse;
import com.urbanclean.entity.RefreshToken;
import com.urbanclean.entity.TokenBlacklist;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.entity.UserSession;
import com.urbanclean.exception.custom.AuthenticationException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.security.JwtTokenProvider;
import com.urbanclean.util.DeviceFingerprintUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Service for authentication operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final SecurityMonitoringService securityMonitoringService;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionService userSessionService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Authenticate user and generate JWT token with refresh token
     */
    @Transactional
    public LoginResponse login(String username, String password, HttpServletRequest request) {
        try {
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // Get user details
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Generate device fingerprint
            String deviceFingerprint = DeviceFingerprintUtil.generateFingerprint(request);
            String ipAddress = DeviceFingerprintUtil.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // Generate JWT access token with token version
            String accessToken = jwtTokenProvider.generateToken(
                user.getUsername(), 
                user.getId(), 
                user.getRole(),
                user.getTokenVersion() != null ? user.getTokenVersion() : 0
            );

            // Generate refresh token
            String refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                deviceFingerprint,
                ipAddress,
                userAgent
            );

            // Create user session
            RefreshToken refreshTokenEntity = refreshTokenService.validateRefreshToken(refreshToken);
            userSessionService.createSession(
                user.getId(),
                refreshTokenEntity.getId(),
                deviceFingerprint,
                ipAddress,
                userAgent
            );

            log.info("User {} logged in successfully from IP: {}", username, ipAddress);

            return LoginResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .role(user.getRole())
                    .username(user.getUsername())
                    .build();

        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Failed login attempt for user: {}", username);
            
            // Log failed attempt for security monitoring
            if (request != null) {
                securityMonitoringService.logFailedLoginAttempt(username, request);
            }
            
            throw new AuthenticationException("Invalid username or password");
        }
    }

    /**
     * Authenticate user and generate JWT token (backward compatibility)
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password) {
        return login(username, password, null);
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public RefreshTokenResponse refreshAccessToken(String refreshToken, HttpServletRequest request) {
        try {
            // Validate refresh token
            RefreshToken refreshTokenEntity = refreshTokenService.validateRefreshToken(refreshToken);

            // Get user
            User user = userRepository.findById(refreshTokenEntity.getUserId())
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Generate device fingerprint
            String deviceFingerprint = DeviceFingerprintUtil.generateFingerprint(request);
            String ipAddress = DeviceFingerprintUtil.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // Generate new access token
            String newAccessToken = jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole(),
                user.getTokenVersion() != null ? user.getTokenVersion() : 0
            );

            // Rotate refresh token
            String newRefreshToken = refreshTokenService.rotateRefreshToken(
                refreshToken,
                deviceFingerprint,
                ipAddress,
                userAgent
            );

            log.info("Refreshed access token for user: {}", user.getUsername());

            return RefreshTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn("Failed to refresh token: {}", e.getMessage());
            throw new AuthenticationException("Invalid or expired refresh token");
        }
    }

    /**
     * Logout user and revoke tokens
     */
    @Transactional
    public void logout(String accessToken, String refreshToken, HttpServletRequest request) {
        try {
            // Extract user ID from access token
            String username = jwtTokenProvider.getUsernameFromToken(accessToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Revoke refresh token
            if (refreshToken != null && !refreshToken.isEmpty()) {
                refreshTokenService.revokeRefreshToken(
                    refreshToken,
                    TokenBlacklist.RevocationReason.LOGOUT.name()
                );
            }

            // Add access token to blacklist
            Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(accessToken);
            LocalDateTime expiresAt = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            tokenBlacklistService.addToBlacklist(
                accessToken,
                TokenBlacklist.TokenType.ACCESS,
                user.getId(),
                expiresAt,
                TokenBlacklist.RevocationReason.LOGOUT.name()
            );

            log.info("User {} logged out successfully", username);

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            throw new AuthenticationException("Logout failed");
        }
    }

    /**
     * Logout from all devices
     */
    @Transactional
    public void logoutAll(String accessToken) {
        try {
            // Extract user ID from access token
            String username = jwtTokenProvider.getUsernameFromToken(accessToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Revoke all refresh tokens
            refreshTokenService.revokeAllUserTokens(user.getId());

            // Revoke all sessions
            userSessionService.revokeAllSessions(user.getId());

            // Increment token version to invalidate all existing access tokens
            user.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1);
            userRepository.save(user);

            log.info("User {} logged out from all devices", username);

        } catch (Exception e) {
            log.error("Error during logout all: {}", e.getMessage());
            throw new AuthenticationException("Logout all failed");
        }
    }

    /**
     * Register a new user
     */
    @Transactional
    public User register(RegisterRequest request) {
        // Validate username doesn't exist
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists");
        }

        // Validate email doesn't exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }

        // Create new user with hashed password
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : UserRole.ROLE_CIUDADANO)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * Validate password against stored hash
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
