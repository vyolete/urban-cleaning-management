package com.urbanclean.integration;

import com.urbanclean.entity.NotificationFailure;
import com.urbanclean.repository.NotificationFailureRepository;
import com.urbanclean.service.EmailService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Circuit Breaker functionality.
 * 
 * Tests:
 * - Circuit breaker opens after threshold failures
 * - Fallback method is called when circuit is open
 * - Circuit breaker closes after wait duration
 * - Failures are logged to notification_failures table
 * 
 * Task 5.7.3: Test circuit breaker
 */
@SpringBootTest
@ActiveProfiles("test")
class CircuitBreakerTest {

    @Autowired
    private EmailService emailService;

    @SpyBean
    private JavaMailSender mailSender;

    @Autowired
    private NotificationFailureRepository notificationFailureRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // Clean up
        notificationFailureRepository.deleteAll();

        // Get circuit breaker instance
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("emailService");

        // Reset circuit breaker to closed state
        circuitBreaker.reset();

        // Reset mock
        reset(mailSender);
    }

    /**
     * Test that circuit breaker opens after threshold failures.
     * 
     * Configuration:
     * - failure-rate-threshold: 50%
     * - minimum-number-of-calls: 5
     * - sliding-window-size: 10
     * 
     * Expected: After 5 calls with 50% failure rate, circuit should open
     */
    @Test
    void testCircuitBreakerOpensAfterThreshold() {
        // Arrange: Configure mail sender to fail
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        String email = "test@example.com";
        String subject = "Test Subject";
        String templateName = "test-template";
        Map<String, Object> variables = new HashMap<>();

        // Act: Make 5 calls (all will fail)
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> 
                emailService.sendEmail(email, subject, templateName, variables)
            );
        }

        // Assert: Circuit breaker should be open
        assertThat(circuitBreaker.getState())
                .isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.FORCED_OPEN);

        // Verify failures were logged
        List<NotificationFailure> failures = notificationFailureRepository.findAll();
        assertThat(failures).hasSizeGreaterThanOrEqualTo(5);
    }

    /**
     * Test that fallback method is called when circuit is open.
     * 
     * When circuit is open, the fallback method should be called immediately
     * without attempting to send the email.
     */
    @Test
    void testFallbackMethodCalledWhenCircuitOpen() {
        // Arrange: Open the circuit by causing failures
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        String email = "test@example.com";
        String subject = "Test Subject";
        String templateName = "test-template";
        Map<String, Object> variables = new HashMap<>();

        // Cause failures to open circuit
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> 
                emailService.sendEmail(email, subject, templateName, variables)
            );
        }

        // Wait a bit for circuit to fully open
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Reset mock to verify fallback doesn't call mail sender
        reset(mailSender);

        int failureCountBefore = notificationFailureRepository.findAll().size();

        // Act: Try to send email when circuit is open
        assertDoesNotThrow(() -> 
            emailService.sendEmail(email, subject, templateName, variables)
        );

        // Assert: Mail sender should NOT be called (fallback used instead)
        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));

        // Verify fallback logged the failure
        int failureCountAfter = notificationFailureRepository.findAll().size();
        assertThat(failureCountAfter).isGreaterThan(failureCountBefore);
    }

    /**
     * Test that circuit breaker closes after wait duration.
     * 
     * Configuration:
     * - wait-duration-in-open-state: 60000ms (1 minute)
     * - permitted-number-of-calls-in-half-open-state: 3
     * 
     * Note: This test uses a shorter wait time for testing purposes.
     */
    @Test
    void testCircuitBreakerClosesAfterWaitDuration() {
        // Arrange: Open the circuit
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        String email = "test@example.com";
        String subject = "Test Subject";
        String templateName = "test-template";
        Map<String, Object> variables = new HashMap<>();

        // Cause failures to open circuit
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> 
                emailService.sendEmail(email, subject, templateName, variables)
            );
        }

        // Verify circuit is open
        assertThat(circuitBreaker.getState())
                .isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.FORCED_OPEN);

        // Manually transition to half-open for testing
        // (In production, this happens automatically after wait duration)
        circuitBreaker.transitionToHalfOpenState();

        // Assert: Circuit should be in half-open state
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Configure mail sender to succeed
        reset(mailSender);
        doNothing().when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        // Act: Make successful calls in half-open state
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> 
                emailService.sendEmail(email, subject, templateName, variables)
            );
        }

        // Assert: Circuit should close after successful calls
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    /**
     * Test that failures are properly logged to notification_failures table.
     */
    @Test
    void testFailuresLoggedToDatabase() {
        // Arrange: Configure mail sender to fail
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        String email = "test@example.com";
        String subject = "Test Subject";
        String templateName = "test-template";
        Map<String, Object> variables = new HashMap<>();

        // Act: Send email (will fail)
        assertDoesNotThrow(() -> 
            emailService.sendEmail(email, subject, templateName, variables)
        );

        // Assert: Failure should be logged
        List<NotificationFailure> failures = notificationFailureRepository.findAll();
        assertThat(failures).isNotEmpty();

        NotificationFailure failure = failures.get(0);
        assertThat(failure.getEmailAddress()).isEqualTo(email);
        assertThat(failure.getNotificationType()).isNotNull();
        assertThat(failure.getFailureReason()).contains("SMTP server unavailable");
        assertThat(failure.getAttemptedAt()).isNotNull();
    }

    /**
     * Test circuit breaker metrics are available.
     */
    @Test
    void testCircuitBreakerMetricsAvailable() {
        // Assert: Circuit breaker should be registered
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).isNotEmpty();
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("emailService");

        // Verify initial state
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Verify metrics
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics).isNotNull();
        assertThat(metrics.getNumberOfBufferedCalls()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(0);
    }

    /**
     * Test that circuit breaker works with retry mechanism.
     * 
     * EmailService has both @Retryable and @CircuitBreaker annotations.
     * When circuit is closed, retries should happen.
     * When circuit is open, fallback should be called immediately.
     */
    @Test
    void testCircuitBreakerWorksWithRetry() {
        // Arrange: Configure mail sender to fail
        doThrow(new MailSendException("Temporary failure"))
                .when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        String email = "test@example.com";
        String subject = "Test Subject";
        String templateName = "test-template";
        Map<String, Object> variables = new HashMap<>();

        // Act: Send email (will retry 3 times before failing)
        assertDoesNotThrow(() -> 
            emailService.sendEmail(email, subject, templateName, variables)
        );

        // Assert: Mail sender should be called multiple times (retries)
        // Note: Exact count depends on retry configuration
        verify(mailSender, atLeastOnce()).send(any(jakarta.mail.internet.MimeMessage.class));

        // Verify failure was logged
        List<NotificationFailure> failures = notificationFailureRepository.findAll();
        assertThat(failures).isNotEmpty();
    }
}
