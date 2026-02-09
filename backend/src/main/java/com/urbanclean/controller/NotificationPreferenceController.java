package com.urbanclean.controller;

import com.urbanclean.dto.request.NotificationPreferenceRequest;
import com.urbanclean.dto.response.NotificationPreferenceResponse;
import com.urbanclean.entity.NotificationPreference;
import com.urbanclean.entity.User;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/notifications")
public class NotificationPreferenceController {

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get notification preferences for the current user
     */
    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        NotificationPreference preferences = notificationPreferenceService.getPreferences(user.getId());
        
        return ResponseEntity.ok(new NotificationPreferenceResponse(preferences));
    }

    /**
     * Update notification preferences for the current user
     */
    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        
        User user = getUserFromAuthentication(authentication);
        NotificationPreference preferences = notificationPreferenceService.updatePreferences(
            user.getId(),
            request.getTaskAssigned(),
            request.getTaskResolved(),
            request.getTaskReopened(),
            request.getReportCreated()
        );
        
        return ResponseEntity.ok(new NotificationPreferenceResponse(preferences));
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
