package com.urbanclean.service;

import com.urbanclean.repository.NotificationFailureRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.UUID;

/**
 * Service for sending emails asynchronously with retry logic and circuit breaker
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    private NotificationFailureService notificationFailureService;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${email.from-name}")
    private String fromName;

    @Value("${email.base-url}")
    private String baseUrl;

    /**
     * Send email asynchronously with retry logic and circuit breaker
     * Retries up to 3 times with exponential backoff (1s, 2s, 4s)
     * Circuit breaker opens after 50% failure rate in 10 requests
     */
    @Async
    @CircuitBreaker(name = "emailService", fallbackMethod = "emailFallback")
    @Retryable(
        retryFor = {MessagingException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            log.info("Sending email to: {} with template: {}", to, templateName);
            
            // Add base URL to variables for all templates
            variables.put("baseUrl", baseUrl);
            
            // Process template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);
            
            // Create message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            
            // Send email
            mailSender.send(message);
            
            log.info("Email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}. Error: {}", to, e.getMessage());
            throw new RuntimeException("Unexpected error sending email", e);
        }
    }

    /**
     * Fallback method for circuit breaker.
     * Called when circuit is open or email sending fails.
     */
    private void emailFallback(String to, String subject, String templateName, 
                              Map<String, Object> variables, Exception e) {
        log.error("Circuit breaker activated for email to: {}. Logging failure.", to, e);
        
        // Extract userId and notificationType from variables if available
        UUID userId = variables.containsKey("userId") ? 
            UUID.fromString(variables.get("userId").toString()) : null;
        String notificationType = variables.containsKey("notificationType") ? 
            variables.get("notificationType").toString() : "UNKNOWN";
        
        if (userId != null && notificationFailureService != null) {
            notificationFailureService.recordFailure(
                userId, 
                notificationType, 
                to, 
                "Circuit breaker activated: " + e.getMessage(), 
                0 // Circuit breaker fallback
            );
        }
    }

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        Map<String, Object> variables = Map.of(
            "resetToken", resetToken,
            "resetLink", baseUrl + "/reset-password?token=" + resetToken
        );
        
        sendEmail(to, "Password Reset Request", "email/password-reset", variables);
    }

    /**
     * Send task resolution notification to citizen
     */
    @Async
    public void sendTaskResolvedEmail(String to, String taskId, String category) {
        Map<String, Object> variables = Map.of(
            "taskId", taskId,
            "category", category,
            "confirmLink", baseUrl + "/tasks/" + taskId + "/feedback/confirm",
            "rejectLink", baseUrl + "/tasks/" + taskId + "/feedback/reject"
        );
        
        sendEmail(to, "Task Resolved - Your Feedback Needed", "email/task-resolved", variables);
    }

    /**
     * Send task reopened notification to operator
     */
    @Async
    public void sendTaskReopenedEmail(String to, String taskId, String category, String justification) {
        Map<String, Object> variables = Map.of(
            "taskId", taskId,
            "category", category,
            "justification", justification,
            "taskLink", baseUrl + "/tasks/" + taskId
        );
        
        sendEmail(to, "Task Reopened - Action Required", "email/task-reopened", variables);
    }

    /**
     * Send account deletion confirmation email
     */
    @Async
    public void sendAccountDeletionConfirmationEmail(String to, String username) {
        Map<String, Object> variables = Map.of(
            "username", username,
            "cancelLink", baseUrl + "/profile/cancel-deletion"
        );
        
        sendEmail(to, "Account Deletion Request", "email/account-deletion", variables);
    }

    /**
     * Send task assignment notification to operator
     */
    @Async
    public void sendTaskAssignmentEmail(String to, String taskId, String category, 
                                       String location, Double priorityScore, String operatorName) {
        Map<String, Object> variables = Map.of(
            "operatorName", operatorName,
            "taskId", taskId,
            "category", category,
            "location", location,
            "priorityScore", String.format("%.2f", priorityScore),
            "taskLink", baseUrl + "/tasks/" + taskId
        );
        
        sendEmail(to, "New Task Assigned - Action Required", "email/task-assigned", variables);
    }

    /**
     * Send report creation confirmation to citizen
     */
    @Async
    public void sendReportCreatedEmail(String to, String reportId, String category, 
                                      String citizenName) {
        Map<String, Object> variables = Map.of(
            "citizenName", citizenName,
            "reportId", reportId,
            "category", category,
            "reportLink", baseUrl + "/reports/" + reportId
        );
        
        sendEmail(to, "Report Created Successfully", "email/report-created", variables);
    }

    /**
     * Recovery method for failed email sending
     * Records the failure for later analysis
     */
    @Recover
    public void recoverFromEmailFailure(Exception e, String to, String subject, 
                                       String templateName, Map<String, Object> variables) {
        log.error("All retry attempts failed for email to: {}. Recording failure.", to);
        
        // Extract userId and notificationType from variables if available
        UUID userId = variables.containsKey("userId") ? 
            UUID.fromString(variables.get("userId").toString()) : null;
        String notificationType = variables.containsKey("notificationType") ? 
            variables.get("notificationType").toString() : "UNKNOWN";
        
        if (userId != null && notificationFailureService != null) {
            notificationFailureService.recordFailure(
                userId, 
                notificationType, 
                to, 
                e.getMessage(), 
                3 // Max retry attempts
            );
        }
    }
}
