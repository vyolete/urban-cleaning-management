package com.urbanclean.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO containing aggregated performance metrics.
 * Used by administrators to monitor system health and performance.
 */
@Schema(description = "Aggregated performance metrics for system monitoring")
public class PerformanceMetricsResponse {

    @Schema(description = "Time range for metrics", example = "HOUR")
    private String timeRange;

    @Schema(description = "Timestamp when metrics were collected", example = "2026-02-09T10:30:00")
    private LocalDateTime timestamp;

    // HTTP Metrics
    @Schema(description = "Total number of HTTP requests", example = "15420")
    private Long requestCount;

    @Schema(description = "Average response time in milliseconds", example = "245.5")
    private Double averageResponseTime;

    @Schema(description = "95th percentile response time in milliseconds", example = "450.2")
    private Double p95ResponseTime;

    @Schema(description = "99th percentile response time in milliseconds", example = "890.7")
    private Double p99ResponseTime;

    @Schema(description = "Error rate as percentage", example = "0.5")
    private Double errorRate;

    // Database Metrics
    @Schema(description = "Number of active database connections", example = "8")
    private Integer activeConnections;

    @Schema(description = "Number of idle database connections", example = "5")
    private Integer idleConnections;

    @Schema(description = "Connection pool usage percentage", example = "40.0")
    private Double connectionPoolUsage;

    // JVM Metrics
    @Schema(description = "Memory used in megabytes", example = "512.5")
    private Double memoryUsedMb;

    @Schema(description = "Maximum memory in megabytes", example = "1024.0")
    private Double memoryMaxMb;

    @Schema(description = "Memory usage percentage", example = "50.0")
    private Double memoryUsagePercent;

    @Schema(description = "CPU usage percentage", example = "35.2")
    private Double cpuUsagePercent;

    // Constructors
    public PerformanceMetricsResponse() {
    }

    // Getters and Setters
    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Long requestCount) {
        this.requestCount = requestCount;
    }

    public Double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(Double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public Double getP95ResponseTime() {
        return p95ResponseTime;
    }

    public void setP95ResponseTime(Double p95ResponseTime) {
        this.p95ResponseTime = p95ResponseTime;
    }

    public Double getP99ResponseTime() {
        return p99ResponseTime;
    }

    public void setP99ResponseTime(Double p99ResponseTime) {
        this.p99ResponseTime = p99ResponseTime;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }

    public Integer getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(Integer activeConnections) {
        this.activeConnections = activeConnections;
    }

    public Integer getIdleConnections() {
        return idleConnections;
    }

    public void setIdleConnections(Integer idleConnections) {
        this.idleConnections = idleConnections;
    }

    public Double getConnectionPoolUsage() {
        return connectionPoolUsage;
    }

    public void setConnectionPoolUsage(Double connectionPoolUsage) {
        this.connectionPoolUsage = connectionPoolUsage;
    }

    public Double getMemoryUsedMb() {
        return memoryUsedMb;
    }

    public void setMemoryUsedMb(Double memoryUsedMb) {
        this.memoryUsedMb = memoryUsedMb;
    }

    public Double getMemoryMaxMb() {
        return memoryMaxMb;
    }

    public void setMemoryMaxMb(Double memoryMaxMb) {
        this.memoryMaxMb = memoryMaxMb;
    }

    public Double getMemoryUsagePercent() {
        return memoryUsagePercent;
    }

    public void setMemoryUsagePercent(Double memoryUsagePercent) {
        this.memoryUsagePercent = memoryUsagePercent;
    }

    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(Double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }
}
