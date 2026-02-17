package com.urbanclean.controller;

import com.urbanclean.dto.request.LoginRequest;
import com.urbanclean.dto.request.RefreshTokenRequest;
import com.urbanclean.dto.request.RegisterRequest;
import com.urbanclean.dto.response.LoginResponse;
import com.urbanclean.dto.response.RefreshTokenResponse;
import com.urbanclean.entity.User;
import com.urbanclean.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Endpoints for user authentication, registration, and session management")
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
    @Operation(
        summary = "User login",
        description = "Authenticate a user with username and password. Returns access token (15 min) and refresh token (7 days)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing or invalid credentials"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed - invalid username or password"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Parameter(description = "Login credentials", required = true)
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
    @Operation(
        summary = "Register new user",
        description = "Create a new user account. Available roles: CIUDADANO (citizen), TECNICO (operator), ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = User.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors or username/email already exists"
        )
    })
    @PostMapping("/register")
    public ResponseEntity<User> register(
            @Parameter(description = "User registration data", required = true)
            @Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for user: {}", request.getUsername());
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Refresh token endpoint
     * POST /api/auth/refresh
     */
    @Operation(
        summary = "Refresh access token",
        description = "Obtain a new access token and refresh token pair using a valid refresh token. Implements token rotation for security."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing refresh token"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Parameter(description = "Refresh token request", required = true)
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
    @Operation(
        summary = "Logout from current session",
        description = "Invalidate access token and optionally refresh token. Adds tokens to blacklist.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logged out successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token"
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Parameter(description = "Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzUxMiJ9...")
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "Optional refresh token to revoke")
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
    @Operation(
        summary = "Logout from all devices",
        description = "Invalidate all active sessions and refresh tokens for the current user. Useful for security purposes.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logged out from all devices successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token"
        )
    })
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @Parameter(description = "Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzUxMiJ9...")
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
