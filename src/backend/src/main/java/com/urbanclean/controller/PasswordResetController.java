package com.urbanclean.controller;

import com.urbanclean.dto.request.PasswordResetCompleteRequest;
import com.urbanclean.dto.request.PasswordResetInitiateRequest;
import com.urbanclean.dto.response.PasswordResetResponse;
import com.urbanclean.entity.PasswordResetToken;
import com.urbanclean.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for password reset operations
 * All endpoints are public (no authentication required)
 */
@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Initiate password reset process
     * POST /api/auth/password-reset/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<PasswordResetResponse> initiatePasswordReset(
            @Valid @RequestBody PasswordResetInitiateRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIpAddress(httpRequest);
        log.info("Password reset initiated from IP: {}", ipAddress);
        
        passwordResetService.initiatePasswordReset(request.getEmail(), ipAddress);
        
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(PasswordResetResponse.builder()
            .success(true)
            .message("If the email exists, a password reset link has been sent")
            .build());
    }

    /**
     * Validate password reset token
     * GET /api/auth/password-reset/validate/{token}
     */
    @GetMapping("/validate/{token}")
    public ResponseEntity<PasswordResetResponse> validateToken(@PathVariable String token) {
        
        PasswordResetToken resetToken = passwordResetService.validateToken(token);
        
        if (resetToken == null) {
            return ResponseEntity.badRequest().body(PasswordResetResponse.builder()
                .success(false)
                .message("Invalid or expired token")
                .build());
        }
        
        return ResponseEntity.ok(PasswordResetResponse.builder()
            .success(true)
            .message("Token is valid")
            .build());
    }

    /**
     * Complete password reset
     * POST /api/auth/password-reset/complete
     */
    @PostMapping("/complete")
    public ResponseEntity<PasswordResetResponse> completePasswordReset(
            @Valid @RequestBody PasswordResetCompleteRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIpAddress(httpRequest);
        log.info("Password reset completion attempted from IP: {}", ipAddress);
        
        boolean success = passwordResetService.resetPassword(
            request.getToken(), 
            request.getNewPassword(), 
            ipAddress
        );
        
        if (!success) {
            return ResponseEntity.badRequest().body(PasswordResetResponse.builder()
                .success(false)
                .message("Invalid or expired token")
                .build());
        }
        
        return ResponseEntity.ok(PasswordResetResponse.builder()
            .success(true)
            .message("Password has been reset successfully")
            .build());
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
