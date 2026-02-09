package com.urbanclean.controller;

import com.urbanclean.enums.NotificationType;
import com.urbanclean.service.NotificationPreferenceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Controller for handling email unsubscribe requests
 */
@Controller
@RequestMapping("/api/notifications")
public class UnsubscribeController {

    private static final Logger logger = LoggerFactory.getLogger(UnsubscribeController.class);

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${email.base-url}")
    private String baseUrl;

    /**
     * Handle unsubscribe request from email link
     * GET /api/notifications/unsubscribe?token={token}
     */
    @GetMapping("/unsubscribe")
    public String unsubscribe(@RequestParam String token, Model model) {
        try {
            logger.info("Processing unsubscribe request");

            // Parse and validate token
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userIdStr = claims.get("userId", String.class);
            String notificationTypeStr = claims.get("notificationType", String.class);

            if (userIdStr == null || notificationTypeStr == null) {
                logger.warn("Invalid unsubscribe token: missing claims");
                model.addAttribute("success", false);
                model.addAttribute("message", "Invalid unsubscribe link");
                return "unsubscribe-result";
            }

            UUID userId = UUID.fromString(userIdStr);
            NotificationType notificationType = NotificationType.valueOf(notificationTypeStr);

            // Disable the notification type
            switch (notificationType) {
                case TASK_ASSIGNED:
                    notificationPreferenceService.updatePreferences(userId, false, null, null, null);
                    break;
                case TASK_RESOLVED:
                    notificationPreferenceService.updatePreferences(userId, null, false, null, null);
                    break;
                case TASK_REOPENED:
                    notificationPreferenceService.updatePreferences(userId, null, null, false, null);
                    break;
                case REPORT_CREATED:
                    notificationPreferenceService.updatePreferences(userId, null, null, null, false);
                    break;
            }

            logger.info("User {} unsubscribed from {}", userId, notificationType);

            model.addAttribute("success", true);
            model.addAttribute("notificationType", notificationType.getDisplayName());
            model.addAttribute("preferencesUrl", baseUrl + "/profile/notifications");

        } catch (Exception e) {
            logger.error("Error processing unsubscribe request: {}", e.getMessage(), e);
            model.addAttribute("success", false);
            model.addAttribute("message", "Failed to process unsubscribe request");
        }

        return "unsubscribe-result";
    }
}
