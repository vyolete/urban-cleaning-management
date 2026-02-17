package com.urbanclean.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for monitoring system metrics and generating alerts
 * when thresholds are exceeded.
 * 
 * Alert Conditions:
 * - Average response time > 1 second for 5 minutes
 * - Error rate > 1% for 5 minutes
 * - Database connection pool > 90% utilization
 * - Memory usage > 85%
 * - CPU usage > 80% for 10 minutes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final MeterRegistry meterRegistry;
    private final PerformanceMetricsService performanceMetricsService;
    private final EmailService emailService;

    // Alert thresholds
    private static final double RESPONSE_TIME_THRESHOLD_MS = 1000.0;
    private static final double ERROR_RATE_THRESHOLD_PERCENT = 1.0;
    private static final double CONNECTION_POOL_THRESHOLD_PERCENT = 90.0;
    private static final double MEMORY_THRESHOLD_PERCENT = 85.0;
    private static final double CPU_THRESHOLD_PERCENT = 80.0;

    // Tracking for sustained conditions
    private int highResponseTimeCount = 0;
    private int highErrorRateCount = 0;
    private int highCpuCount = 0;

    // Thresholds for sustained conditions (in minutes)
    private static final int RESPONSE_TIME_SUSTAINED_MINUTES = 5;
    private static final int ERROR_RATE_SUSTAINED_MINUTES = 5;
    private static final int CPU_SUSTAINED_MINUTES = 10;

    /**
     * Check alert conditions every minute
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void checkAlertConditions() {
        log.debug("Checking alert conditions...");
        
        List<String> alerts = new ArrayList<>();

        // Check response time
        checkResponseTime(alerts);

        // Check error rate
        checkErrorRate(alerts);

        // Check connection pool
        checkConnectionPool(alerts);

        // Check memory usage
        checkMemoryUsage(alerts);

        // Check CPU usage
        checkCpuUsage(alerts);

        // Process alerts
        if (!alerts.isEmpty()) {
            processAlerts(alerts);
        } else {
            log.debug("All metrics within normal thresholds");
        }
    }

    /**
     * Check if average response time exceeds threshold
     */
    private void checkResponseTime(List<String> alerts) {
        try {
            var responseTimeMetrics = performanceMetricsService.getResponseTimePercentiles();
            Double avgResponseTime = responseTimeMetrics.get("p50");

            if (avgResponseTime != null && avgResponseTime > RESPONSE_TIME_THRESHOLD_MS) {
                highResponseTimeCount++;
                
                if (highResponseTimeCount >= RESPONSE_TIME_SUSTAINED_MINUTES) {
                    String alert = String.format(
                        "ALERT: Average response time (%.2fms) exceeds threshold (%.0fms) for %d minutes",
                        avgResponseTime, RESPONSE_TIME_THRESHOLD_MS, RESPONSE_TIME_SUSTAINED_MINUTES
                    );
                    alerts.add(alert);
                    log.warn(alert);
                }
            } else {
                highResponseTimeCount = 0;
            }
        } catch (Exception e) {
            log.error("Error checking response time", e);
        }
    }

    /**
     * Check if error rate exceeds threshold
     */
    private void checkErrorRate(List<String> alerts) {
        try {
            Double errorRate = performanceMetricsService.getErrorRate();

            if (errorRate != null && errorRate > ERROR_RATE_THRESHOLD_PERCENT) {
                highErrorRateCount++;
                
                if (highErrorRateCount >= ERROR_RATE_SUSTAINED_MINUTES) {
                    String alert = String.format(
                        "ALERT: Error rate (%.2f%%) exceeds threshold (%.1f%%) for %d minutes",
                        errorRate, ERROR_RATE_THRESHOLD_PERCENT, ERROR_RATE_SUSTAINED_MINUTES
                    );
                    alerts.add(alert);
                    log.warn(alert);
                }
            } else {
                highErrorRateCount = 0;
            }
        } catch (Exception e) {
            log.error("Error checking error rate", e);
        }
    }

    /**
     * Check if connection pool utilization exceeds threshold
     */
    private void checkConnectionPool(List<String> alerts) {
        try {
            Integer activeConnections = performanceMetricsService.getActiveConnections();
            
            // Get max pool size from configuration (default 20)
            int maxPoolSize = 20;
            
            if (activeConnections != null) {
                double utilizationPercent = (activeConnections * 100.0) / maxPoolSize;
                
                if (utilizationPercent > CONNECTION_POOL_THRESHOLD_PERCENT) {
                    String alert = String.format(
                        "ALERT: Connection pool utilization (%.1f%%) exceeds threshold (%.0f%%)",
                        utilizationPercent, CONNECTION_POOL_THRESHOLD_PERCENT
                    );
                    alerts.add(alert);
                    log.warn(alert);
                }
            }
        } catch (Exception e) {
            log.error("Error checking connection pool", e);
        }
    }

    /**
     * Check if memory usage exceeds threshold
     */
    private void checkMemoryUsage(List<String> alerts) {
        try {
            var memoryMetrics = performanceMetricsService.getMemoryUsage();
            Object percentageObj = memoryMetrics.get("percentage");
            
            if (percentageObj instanceof Double) {
                Double memoryPercent = (Double) percentageObj;
                
                if (memoryPercent > MEMORY_THRESHOLD_PERCENT) {
                    String alert = String.format(
                        "ALERT: Memory usage (%.1f%%) exceeds threshold (%.0f%%)",
                        memoryPercent, MEMORY_THRESHOLD_PERCENT
                    );
                    alerts.add(alert);
                    log.warn(alert);
                }
            }
        } catch (Exception e) {
            log.error("Error checking memory usage", e);
        }
    }

    /**
     * Check if CPU usage exceeds threshold
     */
    private void checkCpuUsage(List<String> alerts) {
        try {
            var cpuMetrics = performanceMetricsService.getCPUUsage();
            Double processCpu = cpuMetrics.get("process");

            if (processCpu != null && processCpu > CPU_THRESHOLD_PERCENT) {
                highCpuCount++;
                
                if (highCpuCount >= CPU_SUSTAINED_MINUTES) {
                    String alert = String.format(
                        "ALERT: CPU usage (%.1f%%) exceeds threshold (%.0f%%) for %d minutes",
                        processCpu, CPU_THRESHOLD_PERCENT, CPU_SUSTAINED_MINUTES
                    );
                    alerts.add(alert);
                    log.warn(alert);
                }
            } else {
                highCpuCount = 0;
            }
        } catch (Exception e) {
            log.error("Error checking CPU usage", e);
        }
    }

    /**
     * Process alerts by logging and sending email notifications
     */
    private void processAlerts(List<String> alerts) {
        for (String alert : alerts) {
            // Log alert (already logged as WARN in check methods)
            log.error("SYSTEM ALERT: {}", alert);

            // Send email notification to administrators
            try {
                sendAlertEmail(alert);
            } catch (Exception e) {
                log.error("Error sending alert email", e);
            }
        }
    }

    /**
     * Send alert email to administrators
     */
    private void sendAlertEmail(String alertMessage) {
        try {
            String subject = "Urban Cleaning System Alert - " + LocalDateTime.now();
            String body = buildAlertEmailBody(alertMessage);
            
            // In a real system, this would send to a configured list of admin emails
            // For now, we just log it
            log.info("Alert email would be sent: {}", alertMessage);
            
            // Uncomment to actually send emails:
            // emailService.sendEmail("admin@urbanclean.com", subject, body);
        } catch (Exception e) {
            log.error("Error sending alert email", e);
        }
    }

    /**
     * Build HTML email body for alert
     */
    private String buildAlertEmailBody(String alertMessage) {
        return String.format("""
            <html>
            <body>
                <h2>System Alert</h2>
                <p><strong>Time:</strong> %s</p>
                <p><strong>Alert:</strong> %s</p>
                <hr>
                <p>This is an automated alert from the Urban Cleaning Management System.</p>
                <p>Please investigate and take appropriate action.</p>
            </body>
            </html>
            """, LocalDateTime.now(), alertMessage);
    }

    /**
     * Get current alert status for monitoring dashboard
     */
    public AlertStatus getAlertStatus() {
        return AlertStatus.builder()
                .responseTimeAlertActive(highResponseTimeCount >= RESPONSE_TIME_SUSTAINED_MINUTES)
                .errorRateAlertActive(highErrorRateCount >= ERROR_RATE_SUSTAINED_MINUTES)
                .cpuAlertActive(highCpuCount >= CPU_SUSTAINED_MINUTES)
                .lastCheckTime(LocalDateTime.now())
                .build();
    }

    /**
     * DTO for alert status
     */
    @lombok.Builder
    @lombok.Data
    public static class AlertStatus {
        private boolean responseTimeAlertActive;
        private boolean errorRateAlertActive;
        private boolean cpuAlertActive;
        private LocalDateTime lastCheckTime;
    }
}
