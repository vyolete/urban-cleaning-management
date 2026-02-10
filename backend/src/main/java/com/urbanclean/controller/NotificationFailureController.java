package com.urbanclean.controller;

import com.urbanclean.dto.response.NotificationFailureResponse;
import com.urbanclean.entity.NotificationFailure;
import com.urbanclean.service.NotificationFailureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing notification failures (Admin only)
 */
@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationFailureController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationFailureController.class);

    @Autowired
    private NotificationFailureService notificationFailureService;

    /**
     * Get all notification failures with optional filtering
     * GET /api/admin/notifications/failures
     */
    @GetMapping("/failures")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationFailureResponse>> getFailures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) UUID userId) {
        
        logger.info("Get notification failures request: startDate={}, endDate={}, type={}, userId={}", 
                   startDate, endDate, notificationType, userId);

        List<NotificationFailure> failures = notificationFailureService.getFailures(
            startDate, endDate, notificationType, userId
        );

        List<NotificationFailureResponse> response = failures.stream()
                .map(NotificationFailureResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Retry a failed notification
     * POST /api/admin/notifications/failures/{id}/retry
     */
    @PostMapping("/failures/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> retryFailedNotification(@PathVariable UUID id) {
        logger.info("Retry notification failure request: id={}", id);
        
        notificationFailureService.retryFailedNotification(id);
        
        return ResponseEntity.ok().build();
    }
}
