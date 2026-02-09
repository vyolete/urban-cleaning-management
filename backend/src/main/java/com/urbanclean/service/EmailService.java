package com.urbanclean.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service for sending emails asynchronously with retry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${email.from-name}")
    private String fromName;

    @Value("${email.base-url}")
    private String baseUrl;

    /**
     * Send email asynchronously with retry logic
     * Retries up to 3 times with exponential backoff (1s, 2s, 4s)
     */
    @Async
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
}
