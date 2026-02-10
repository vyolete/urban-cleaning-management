package com.urbanclean.controller;

import com.urbanclean.dto.request.NotificationPreferenceRequest;
import com.urbanclean.dto.response.NotificationPreferenceResponse;
import com.urbanclean.entity.NotificationPreference;
import com.urbanclean.entity.User;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/notifications")
@Tag(name = "Notification Preferences", description = "Endpoints for managing user notification preferences and email alerts")
public class NotificationPreferenceController {

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get notification preferences for the current user
     */
    @Operation(
        summary = "Get notification preferences",
        description = "Retrieves the current user's notification preferences. Shows which types of email notifications " +
                     "are enabled or disabled. Users can control notifications for task assignments, resolutions, " +
                     "reopenings, and report creations. All authenticated users can access their own preferences.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Notification preferences retrieved successfully",
            content = @Content(schema = @Schema(implementation = NotificationPreferenceResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        )
    })
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
    @Operation(
        summary = "Update notification preferences",
        description = "Updates the current user's notification preferences. Users can enable or disable email notifications " +
                     "for different event types: task assignments (for operators), task resolutions (for citizens who submitted reports), " +
                     "task reopenings, and new report creations (for admins). Changes take effect immediately for future notifications. " +
                     "All authenticated users can modify their own preferences.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Notification preferences updated successfully",
            content = @Content(schema = @Schema(implementation = NotificationPreferenceResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid preference values"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        )
    })
    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            Authentication authentication,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "New notification preferences. All fields are optional - only provided fields will be updated.",
                required = true,
                content = @Content(schema = @Schema(implementation = NotificationPreferenceRequest.class))
            )
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
