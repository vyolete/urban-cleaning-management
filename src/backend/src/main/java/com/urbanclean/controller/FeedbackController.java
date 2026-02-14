package com.urbanclean.controller;

import com.urbanclean.dto.request.RejectFeedbackRequest;
import com.urbanclean.dto.response.FeedbackResponse;
import com.urbanclean.entity.CitizenFeedback;
import com.urbanclean.entity.User;
import com.urbanclean.repository.CitizenFeedbackRepository;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for citizen feedback on task resolutions
 * Only authenticated citizens can provide feedback
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CitizenFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    /**
     * Confirm task resolution
     * POST /api/tasks/{taskId}/feedback/confirm
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'TECNICO', 'ADMIN')")
    public ResponseEntity<FeedbackResponse> confirmResolution(@PathVariable UUID taskId) {
        
        UUID userId = getCurrentUserId();
        log.info("User {} confirming resolution of task {}", userId, taskId);
        
        feedbackService.confirmResolution(taskId, userId);
        
        // Retrieve the created feedback
        CitizenFeedback feedback = feedbackRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalStateException("Feedback not found after creation"));
        
        FeedbackResponse response = mapToResponse(feedback);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reject task resolution and reopen the task
     * POST /api/tasks/{taskId}/feedback/reject
     */
    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'TECNICO', 'ADMIN')")
    public ResponseEntity<FeedbackResponse> rejectResolution(
            @PathVariable UUID taskId,
            @Valid @RequestBody RejectFeedbackRequest request) {
        
        UUID userId = getCurrentUserId();
        log.info("User {} rejecting resolution of task {}", userId, taskId);
        
        feedbackService.rejectResolution(taskId, userId, request.getJustification());
        
        // Retrieve the created feedback
        CitizenFeedback feedback = feedbackRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalStateException("Feedback not found after creation"));
        
        FeedbackResponse response = mapToResponse(feedback);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get feedback for a task
     * GET /api/tasks/{taskId}/feedback
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CIUDADANO', 'TECNICO', 'ADMIN')")
    public ResponseEntity<FeedbackResponse> getFeedback(@PathVariable UUID taskId) {
        
        log.debug("Retrieving feedback for task {}", taskId);
        
        CitizenFeedback feedback = feedbackRepository.findByTaskId(taskId)
            .orElse(null);
        
        if (feedback == null) {
            return ResponseEntity.notFound().build();
        }
        
        FeedbackResponse response = mapToResponse(feedback);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user ID
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return user.getId();
    }

    /**
     * Map CitizenFeedback entity to FeedbackResponse DTO
     */
    private FeedbackResponse mapToResponse(CitizenFeedback feedback) {
        return FeedbackResponse.builder()
            .id(feedback.getId())
            .taskId(feedback.getTask().getId())
            .type(feedback.getType())
            .justification(feedback.getJustification())
            .submittedAt(feedback.getSubmittedAt())
            .feedbackDeadline(feedback.getFeedbackDeadline())
            .deadlinePassed(feedback.isDeadlinePassed())
            .build();
    }
}
