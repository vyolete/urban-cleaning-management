package com.urbanclean.controller;

import com.urbanclean.dto.request.LoginRequest;
import com.urbanclean.dto.request.RegisterRequest;
import com.urbanclean.dto.response.LoginResponse;
import com.urbanclean.entity.User;
import com.urbanclean.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
