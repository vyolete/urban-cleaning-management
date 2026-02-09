package com.urbanclean.controller;

import com.urbanclean.dto.request.LoginRequest;
import com.urbanclean.dto.request.RefreshTokenRequest;
import com.urbanclean.dto.request.RegisterRequest;
import com.urbanclean.dto.response.LoginResponse;
import com.urbanclean.dto.response.RefreshTokenResponse;
import com.urbanclean.entity.User;
import com.urbanclean.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for authentication operations
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = authService.login(
            request.getUsername(), 
            request.getPassword(),
            httpRequest
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Register endpoint
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for user: {}", request.getUsername());
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Refresh token endpoint
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        log.info("Token refresh attempt");
        RefreshTokenResponse response = authService.refreshAccessToken(
            request.getRefreshToken(),
            httpRequest
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest httpRequest) {
        log.info("Logout attempt");
        
        // Extract access token from Authorization header
        String accessToken = authHeader.replace("Bearer ", "");
        
        // Extract refresh token from body (optional)
        String refreshToken = body != null ? body.get("refreshToken") : null;
        
        authService.logout(accessToken, refreshToken, httpRequest);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Logout from all devices endpoint
     * POST /api/auth/logout-all
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {
        log.info("Logout all devices attempt for user: {}", authentication.getName());
        
        // Extract access token from Authorization header
        String accessToken = authHeader.replace("Bearer ", "");
        
        authService.logoutAll(accessToken);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out from all devices successfully");
        return ResponseEntity.ok(response);
    }
}
