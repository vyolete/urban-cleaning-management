package com.urbanclean.controller;

import com.urbanclean.dto.response.UserSessionResponse;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserSession;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for user session management
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Session Management", description = "Endpoints for managing user sessions and multi-device authentication")
public class SessionController {

    private final UserSessionService userSessionService;
    private final UserRepository userRepository;

    /**
     * Get all active sessions for current user
     * GET /api/sessions
     */
    @Operation(
        summary = "Get active sessions",
        description = "Retrieves all currently active sessions for the authenticated user. " +
                     "Shows device information, IP addresses, and last activity timestamps. " +
                     "Useful for security monitoring and multi-device management.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active sessions retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserSessionResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        )
    })
    @GetMapping
    public ResponseEntity<List<UserSessionResponse>> getActiveSessions(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserSession> sessions = userSessionService.getActiveSessions(user.getId());
        
        // Convert to DTOs (we don't have current session ID here, so all marked as not current)
        List<UserSessionResponse> response = sessions.stream()
                .map(session -> UserSessionResponse.fromEntity(session, null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all sessions (including inactive) for current user
     * GET /api/sessions/all
     */
    @Operation(
        summary = "Get all sessions (including inactive)",
        description = "Retrieves complete session history for the authenticated user, including both active and revoked sessions. " +
                     "Provides full audit trail of login activity across all devices and time periods.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "All sessions retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserSessionResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        )
    })
    @GetMapping("/all")
    public ResponseEntity<List<UserSessionResponse>> getAllSessions(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserSession> sessions = userSessionService.getAllSessions(user.getId());
        
        // Convert to DTOs
        List<UserSessionResponse> response = sessions.stream()
                .map(session -> UserSessionResponse.fromEntity(session, null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Revoke a specific session
     * DELETE /api/sessions/{sessionId}
     */
    @Operation(
        summary = "Revoke specific session",
        description = "Revokes a specific session by ID. This immediately invalidates all tokens associated with that session. " +
                     "Useful for logging out from a specific device or terminating a suspicious session. " +
                     "Users can only revoke their own sessions.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Session revoked successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Cannot revoke another user's session"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Session not found"
        )
    })
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> revokeSession(
            @Parameter(description = "UUID of the session to revoke", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID sessionId,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        log.info("Revoking session {} for user: {}", sessionId, username);
        userSessionService.revokeSession(sessionId, user.getId());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Session revoked successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke all sessions except current
     * POST /api/sessions/revoke-others
     */
    @Operation(
        summary = "Revoke all other sessions",
        description = "Revokes all sessions except the current one. This logs the user out from all other devices " +
                     "while keeping the current session active. Useful for security purposes when a user suspects " +
                     "unauthorized access. Optionally provide currentSessionId in request body to preserve that specific session.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Other sessions revoked successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token"
        )
    })
    @PostMapping("/revoke-others")
    public ResponseEntity<Map<String, String>> revokeOtherSessions(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Optional: Provide currentSessionId to preserve that specific session",
                content = @Content(
                    schema = @Schema(
                        example = "{\"currentSessionId\": \"550e8400-e29b-41d4-a716-446655440000\"}"
                    )
                )
            )
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get current session ID from request body (optional)
        UUID currentSessionId = body.get("currentSessionId") != null 
                ? UUID.fromString(body.get("currentSessionId")) 
                : null;

        log.info("Revoking all sessions except current for user: {}", username);
        
        if (currentSessionId != null) {
            userSessionService.revokeAllSessionsExceptCurrent(user.getId(), currentSessionId);
        } else {
            userSessionService.revokeAllSessions(user.getId());
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Other sessions revoked successfully");
        return ResponseEntity.ok(response);
    }
}
