package com.urbanclean.service;

import com.urbanclean.dto.request.LoginRequest;
import com.urbanclean.dto.request.RegisterRequest;
import com.urbanclean.dto.response.LoginResponse;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.exception.custom.AuthenticationException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Authenticate user and generate JWT token
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password, HttpServletRequest request) {
        try {
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // Get user details
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Generate JWT token with token version
            String token = jwtTokenProvider.generateToken(
                user.getUsername(), 
                user.getId(), 
                user.getRole(),
                user.getTokenVersion() != null ? user.getTokenVersion() : 0
            );

            log.info("User {} logged in successfully", username);

            return LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(86400000L) // 24 hours in milliseconds
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
