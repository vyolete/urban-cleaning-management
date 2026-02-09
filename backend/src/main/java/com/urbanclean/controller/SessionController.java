package com.urbanclean.controller;

import com.urbanclean.dto.response.UserSessionResponse;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserSession;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.UserSessionService;
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
public class SessionController {

    private final UserSessionService userSessionService;
    private final UserRepository userRepository;

    /**
     * Get all active sessions for current user
     * GET /api/sessions
     */
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
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> revokeSession(
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
    @PostMapping("/revoke-others")
    public ResponseEntity<Map<String, String>> revokeOtherSessions(
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
