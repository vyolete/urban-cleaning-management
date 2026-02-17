package com.urbanclean.controller;

import com.urbanclean.dto.request.ChangePasswordRequest;
import com.urbanclean.dto.request.DeleteAccountRequest;
import com.urbanclean.dto.request.UpdateProfileRequest;
import com.urbanclean.dto.response.UserDataExport;
import com.urbanclean.dto.response.UserProfileResponse;
import com.urbanclean.entity.Report;
import com.urbanclean.entity.User;
import com.urbanclean.exception.custom.AuthenticationException;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.ReportRepository;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for user profile management
 * Implements GDPR-compliant user data operations
 * 
 * Validates: Requirements 18.1-18.9
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final UserDataService userDataService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get current user's profile information
     * 
     * GET /api/users/profile
     * 
     * Validates: Requirement 18.1
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        log.info("Fetching profile for user: {}", authentication.getName());
        
        User user = getUserFromAuthentication(authentication);
        
        UserProfileResponse response = UserProfileResponse.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .isAnonymized(user.getAnonymized())
            .deletionRequested(user.getDeletedAt() != null)
            .gracePeriodDaysRemaining(
                user.getDeletedAt() != null ? 
                userDataService.getRemainingGracePeriodDays(user.getId()) : 
                null
            )
            .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user's profile information
     * 
     * PUT /api/users/profile
     * 
     * Validates: Requirements 18.2, 18.6
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        
        log.info("Updating profile for user: {}", authentication.getName());
        
        User user = getUserFromAuthentication(authentication);
        
        // Validate user can only modify their own data (18.6)
        if (!user.getUsername().equals(authentication.getName())) {
            throw new AuthenticationException("Cannot modify another user's profile");
        }
        
        // Check if account is anonymized
        if (user.getAnonymized()) {
            throw new ValidationException("Cannot update anonymized account");
        }
        
        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email is already taken
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new ValidationException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        
        // Update username if provided
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            // Check if username is already taken
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new ValidationException("Username already in use");
            }
            user.setUsername(request.getUsername());
        }
        
        user = userRepository.save(user);
        
        UserProfileResponse response = UserProfileResponse.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .isAnonymized(user.getAnonymized())
            .deletionRequested(user.getDeletedAt() != null)
            .build();
        
        log.info("Profile updated successfully for user: {}", user.getUsername());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Change current user's password
     * 
     * POST /api/users/change-password
     * 
     * Validates: Requirements 18.3, 18.7
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        
        log.info("Password change requested for user: {}", authentication.getName());
        
        User user = getUserFromAuthentication(authentication);
        
        // Check if account is anonymized
        if (user.getAnonymized()) {
            throw new ValidationException("Cannot change password for anonymized account");
        }
        
        // Verify current password (18.7)
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        
        // Validate new password is different
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new ValidationException("New password must be different from current password");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        
        // Increment token version to invalidate all existing JWTs
        Integer currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        user.setTokenVersion(currentVersion + 1);
        
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}. Token version incremented to: {}", 
            user.getUsername(), user.getTokenVersion());
        
        return ResponseEntity.ok(Map.of(
            "message", "Password changed successfully"
        ));
    }

    /**
     * Get current user's report history
     * 
     * GET /api/users/reports
     * 
     * Validates: Requirement 18.4
     */
    @GetMapping("/reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getUserReports(Authentication authentication) {
        log.info("Fetching reports for user: {}", authentication.getName());
        
        User user = getUserFromAuthentication(authentication);
        
        List<Report> reports = reportRepository.findBySubmitter(user);
        
        List<Map<String, Object>> reportData = reports.stream()
            .map(report -> {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("reportId", report.getId().toString());
                data.put("category", report.getCategory());
                data.put("description", report.getDescription());
                data.put("latitude", report.getLocation().getY());
                data.put("longitude", report.getLocation().getX());
                data.put("photoUrl", report.getPhotoUrl() != null ? report.getPhotoUrl() : "");
                data.put("createdAt", report.getCreatedAt().toString());
                data.put("isDuplicate", report.getIsDuplicate());
                data.put("taskId", report.getParentTask() != null ? report.getParentTask().getId().toString() : "");
                return data;
            })
            .collect(Collectors.toList());
        
        log.info("Found {} reports for user: {}", reportData.size(), user.getUsername());
        
        return ResponseEntity.ok(reportData);
    }

    /**
     * Request account deletion (starts 7-day grace period)
     * 
     * POST /api/users/delete-account
     * 
     * Validates: Requirements 18.5, 18.8
     */
    @PostMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> requestAccountDeletion(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication) {
        
        log.info("Account deletion requested for user: {}", authentication.getName());
        
        User user = getUserFromAuthentication(authentication);
        
        // Request deletion through service (validates password and starts grace period)
        boolean success = userDataService.requestAccountDeletion(user.getId(), request.getPassword());
        
        if (success) {
            long gracePeriodDays = userDataService.getRemainingGracePeriodDays(user.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Account deletion requested. You have " + gracePeriodDays + " days to cancel.",
                "gracePeriodDays", gracePeriodDays,
                "canCancel", true
            ));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "Failed to request account deletion"));
    }

    /**
     * Cancel account deletion request (during grace period)
     * 
     * POST /api/users/cancel-deletion
     * 
     * Validates: Requirement 18.5
     */
    @PostMapping("/cancel-deletion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> cancelAccountDeletion(Authentication authentication) {
        log.info("Canceling account deletion for user: {}", authentication.getName());
        
        User user = getUserFromAuthentication(authentication);
        
        boolean success = userDataService.cancelAccountDeletion(user.getId());
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                "message", "Account deletion canceled successfully"
            ));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "Failed to cancel account deletion"));
    }

    /**
     * Export user data in JSON format (GDPR data portability)
     * 
     * GET /api/users/export
     * 
     * Validates: Requirement 18.9
     */
    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDataExport> exportUserData(Authentication authentication) {
        log.info("Data export requested for user: {}", authentication.getName());
        
        User user = getUserFromAuthentication(authentication);
        
        UserDataExport export = userDataService.exportUserData(user.getId());
        
        log.info("Data export completed for user: {}", user.getUsername());
        
        return ResponseEntity.ok(export);
    }

    /**
     * Helper method to get User entity from Authentication
     */
    private User getUserFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
