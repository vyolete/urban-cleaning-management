package com.urbanclean.service;

import com.urbanclean.dto.response.PerformanceMetricsResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for collecting and aggregating performance metrics.
 * Provides insights into system performance, response times, and resource usage.
 */
@Service
public class PerformanceMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsService.class);

    private final MeterRegistry meterRegistry;

    public PerformanceMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Retrieves aggregated performance metrics for the specified time range.
     *
     * @param timeRange Time range for metrics (HOUR, DAY, WEEK)
     * @return Aggregated performance metrics
     */
    public PerformanceMetricsResponse getAggregatedMetrics(String timeRange) {
        logger.info("Retrieving performance metrics for time range: {}", timeRange);

        PerformanceMetricsResponse response = new PerformanceMetricsResponse();
        response.setTimeRange(timeRange);
        response.setTimestamp(LocalDateTime.now());

        // Get HTTP request metrics
        Map<String, Object> httpMetrics = getHttpMetrics();
        response.setRequestCount((Long) httpMetrics.get("totalRequests"));
        response.setAverageResponseTime((Double) httpMetrics.get("averageResponseTime"));
        response.setP95ResponseTime((Double) httpMetrics.get("p95ResponseTime"));
        response.setP99ResponseTime((Double) httpMetrics.get("p99ResponseTime"));
        response.setErrorRate((Double) httpMetrics.get("errorRate"));

        // Get database connection pool metrics
        Map<String, Object> dbMetrics = getDatabaseMetrics();
        response.setActiveConnections((Integer) dbMetrics.get("activeConnections"));
        response.setIdleConnections((Integer) dbMetrics.get("idleConnections"));
        response.setConnectionPoolUsage((Double) dbMetrics.get("poolUsage"));

        // Get JVM metrics
        Map<String, Object> jvmMetrics = getJvmMetrics();
        response.setMemoryUsedMb((Double) jvmMetrics.get("memoryUsedMb"));
        response.setMemoryMaxMb((Double) jvmMetrics.get("memoryMaxMb"));
        response.setMemoryUsagePercent((Double) jvmMetrics.get("memoryUsagePercent"));
        response.setCpuUsagePercent((Double) jvmMetrics.get("cpuUsagePercent"));

        logger.info("Performance metrics retrieved successfully");
        return response;
    }

    /**
     * Retrieves HTTP request metrics including response times and error rates.
     *
     * @return Map containing HTTP metrics
     */
    private Map<String, Object> getHttpMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Get HTTP request timer
            Timer timer = Search.in(meterRegistry)
                    .name("http.server.requests")
                    .timer();

            if (timer != null) {
                long totalRequests = timer.count();
                double averageResponseTime = timer.mean(TimeUnit.MILLISECONDS);
                double p95 = timer.percentile(0.95, TimeUnit.MILLISECONDS);
                double p99 = timer.percentile(0.99, TimeUnit.MILLISECONDS);

                metrics.put("totalRequests", totalRequests);
                metrics.put("averageResponseTime", averageResponseTime);
                metrics.put("p95ResponseTime", p95);
                metrics.put("p99ResponseTime", p99);

                // Calculate error rate
                Counter errorCounter = Search.in(meterRegistry)
                        .name("http.server.requests")
                        .tag("status", "5xx")
                        .counter();

                double errorRate = 0.0;
                if (errorCounter != null && totalRequests > 0) {
                    errorRate = (errorCounter.count() / (double) totalRequests) * 100;
                }
                metrics.put("errorRate", errorRate);
            } else {
                // No metrics available yet
                metrics.put("totalRequests", 0L);
                metrics.put("averageResponseTime", 0.0);
                metrics.put("p95ResponseTime", 0.0);
                metrics.put("p99ResponseTime", 0.0);
                metrics.put("errorRate", 0.0);
            }
        } catch (Exception e) {
            logger.error("Error retrieving HTTP metrics", e);
            metrics.put("totalRequests", 0L);
            metrics.put("averageResponseTime", 0.0);
            metrics.put("p95ResponseTime", 0.0);
            metrics.put("p99ResponseTime", 0.0);
            metrics.put("errorRate", 0.0);
        }

        return metrics;
    }

    /**
     * Retrieves database connection pool metrics from HikariCP.
     *
     * @return Map containing database metrics
     */
    private Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Get HikariCP metrics
            Double activeConnections = meterRegistry.get("hikaricp.connections.active")
                    .gauge()
                    .value();

            Double idleConnections = meterRegistry.get("hikaricp.connections.idle")
                    .gauge()
                    .value();

            Double maxConnections = meterRegistry.get("hikaricp.connections.max")
                    .gauge()
                    .value();

            double poolUsage = (activeConnections / maxConnections) * 100;

            metrics.put("activeConnections", activeConnections.intValue());
            metrics.put("idleConnections", idleConnections.intValue());
            metrics.put("maxConnections", maxConnections.intValue());
            metrics.put("poolUsage", poolUsage);
        } catch (Exception e) {
            logger.warn("HikariCP metrics not available yet", e);
            metrics.put("activeConnections", 0);
            metrics.put("idleConnections", 0);
            metrics.put("maxConnections", 20);
            metrics.put("poolUsage", 0.0);
        }

        return metrics;
    }

    /**
     * Retrieves JVM memory and CPU metrics.
     *
     * @return Map containing JVM metrics
     */
    private Map<String, Object> getJvmMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Get memory metrics
            Double memoryUsed = meterRegistry.get("jvm.memory.used")
                    .tag("area", "heap")
                    .gauge()
                    .value();

            Double memoryMax = meterRegistry.get("jvm.memory.max")
                    .tag("area", "heap")
                    .gauge()
                    .value();

            double memoryUsedMb = memoryUsed / (1024 * 1024);
            double memoryMaxMb = memoryMax / (1024 * 1024);
            double memoryUsagePercent = (memoryUsed / memoryMax) * 100;

            metrics.put("memoryUsedMb", memoryUsedMb);
            metrics.put("memoryMaxMb", memoryMaxMb);
            metrics.put("memoryUsagePercent", memoryUsagePercent);

            // Get CPU usage
            Double cpuUsage = meterRegistry.get("system.cpu.usage")
                    .gauge()
                    .value();

            metrics.put("cpuUsagePercent", cpuUsage * 100);
        } catch (Exception e) {
            logger.warn("JVM metrics not available yet", e);
            metrics.put("memoryUsedMb", 0.0);
            metrics.put("memoryMaxMb", 0.0);
            metrics.put("memoryUsagePercent", 0.0);
            metrics.put("cpuUsagePercent", 0.0);
        }

        return metrics;
    }

    /**
     * Checks if any performance thresholds are exceeded and returns alerts.
     *
     * @return Map of alert conditions and their status
     */
    public Map<String, Boolean> checkPerformanceAlerts() {
        Map<String, Boolean> alerts = new HashMap<>();

        try {
            // Check average response time
            Timer timer = Search.in(meterRegistry)
                    .name("http.server.requests")
                    .timer();

            if (timer != null) {
                double avgResponseTime = timer.mean(TimeUnit.MILLISECONDS);
                alerts.put("highResponseTime", avgResponseTime > 1000); // > 1 second

                // Check error rate
                long totalRequests = timer.count();
                Counter errorCounter = Search.in(meterRegistry)
                        .name("http.server.requests")
                        .tag("status", "5xx")
                        .counter();

                if (errorCounter != null && totalRequests > 0) {
                    double errorRate = (errorCounter.count() / (double) totalRequests) * 100;
                    alerts.put("highErrorRate", errorRate > 1.0); // > 1%
                }
            }

            // Check connection pool usage
            Double activeConnections = meterRegistry.get("hikaricp.connections.active")
                    .gauge()
                    .value();

            Double maxConnections = meterRegistry.get("hikaricp.connections.max")
                    .gauge()
                    .value();

            double poolUsage = (activeConnections / maxConnections) * 100;
            alerts.put("highConnectionPoolUsage", poolUsage > 90); // > 90%

            // Check memory usage
            Double memoryUsed = meterRegistry.get("jvm.memory.used")
                    .tag("area", "heap")
                    .gauge()
                    .value();

            Double memoryMax = meterRegistry.get("jvm.memory.max")
                    .tag("area", "heap")
                    .gauge()
                    .value();

            double memoryUsagePercent = (memoryUsed / memoryMax) * 100;
            alerts.put("highMemoryUsage", memoryUsagePercent > 85); // > 85%

            // Check CPU usage
            Double cpuUsage = meterRegistry.get("system.cpu.usage")
                    .gauge()
                    .value();

            alerts.put("highCpuUsage", cpuUsage > 0.80); // > 80%

        } catch (Exception e) {
            logger.error("Error checking performance alerts", e);
        }

        return alerts;
    }

    /**
     * Get response time percentiles for alert monitoring.
     *
     * @return Map containing p50, p95, p99 response times
     */
    public Map<String, Double> getResponseTimePercentiles() {
        Map<String, Double> percentiles = new HashMap<>();

        try {
            Timer timer = Search.in(meterRegistry)
                    .name("http.server.requests")
                    .timer();

            if (timer != null) {
                percentiles.put("p50", timer.percentile(0.50, TimeUnit.MILLISECONDS));
                percentiles.put("p95", timer.percentile(0.95, TimeUnit.MILLISECONDS));
                percentiles.put("p99", timer.percentile(0.99, TimeUnit.MILLISECONDS));
            } else {
                percentiles.put("p50", 0.0);
                percentiles.put("p95", 0.0);
                percentiles.put("p99", 0.0);
            }
        } catch (Exception e) {
            logger.error("Error getting response time percentiles", e);
            percentiles.put("p50", 0.0);
            percentiles.put("p95", 0.0);
            percentiles.put("p99", 0.0);
        }

        return percentiles;
    }

    /**
     * Get current error rate as percentage.
     *
     * @return Error rate percentage
     */
    public Double getErrorRate() {
        try {
            Timer timer = Search.in(meterRegistry)
                    .name("http.server.requests")
                    .timer();

            if (timer != null) {
                long totalRequests = timer.count();
                Counter errorCounter = Search.in(meterRegistry)
                        .name("http.server.requests")
                        .tag("status", "5xx")
                        .counter();

                if (errorCounter != null && totalRequests > 0) {
                    return (errorCounter.count() / (double) totalRequests) * 100;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting error rate", e);
        }

        return 0.0;
    }

    /**
     * Get active database connections count.
     *
     * @return Number of active connections
     */
    public Integer getActiveConnections() {
        try {
            Double activeConnections = meterRegistry.get("hikaricp.connections.active")
                    .gauge()
                    .value();
            return activeConnections.intValue();
        } catch (Exception e) {
            logger.warn("HikariCP metrics not available", e);
            return 0;
        }
    }

    /**
     * Get memory usage metrics.
     *
     * @return Map containing used, max, and percentage
     */
    public Map<String, Object> getMemoryUsage() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            Double memoryUsed = meterRegistry.get("jvm.memory.used")
                    .tag("area", "heap")
                    .gauge()
                    .value();

            Double memoryMax = meterRegistry.get("jvm.memory.max")
                    .tag("area", "heap")
                    .gauge()
                    .value();

            double memoryUsedMb = memoryUsed / (1024 * 1024);
            double memoryMaxMb = memoryMax / (1024 * 1024);
            double percentage = (memoryUsed / memoryMax) * 100;

            metrics.put("usedMb", memoryUsedMb);
            metrics.put("maxMb", memoryMaxMb);
            metrics.put("percentage", percentage);
        } catch (Exception e) {
            logger.error("Error getting memory usage", e);
            metrics.put("usedMb", 0.0);
            metrics.put("maxMb", 0.0);
            metrics.put("percentage", 0.0);
        }

        return metrics;
    }

    /**
     * Get CPU usage metrics.
     *
     * @return Map containing system and process CPU usage
     */
    public Map<String, Double> getCPUUsage() {
        Map<String, Double> metrics = new HashMap<>();

        try {
            Double systemCpu = meterRegistry.get("system.cpu.usage")
                    .gauge()
                    .value();

            Double processCpu = meterRegistry.get("process.cpu.usage")
                    .gauge()
                    .value();

            metrics.put("system", systemCpu * 100);
            metrics.put("process", processCpu * 100);
        } catch (Exception e) {
            logger.error("Error getting CPU usage", e);
            metrics.put("system", 0.0);
            metrics.put("process", 0.0);
        }

        return metrics;
    }
}
