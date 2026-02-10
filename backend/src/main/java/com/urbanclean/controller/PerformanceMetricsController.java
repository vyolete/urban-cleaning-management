package com.urbanclean.controller;

import com.urbanclean.dto.response.PerformanceMetricsResponse;
import com.urbanclean.service.PerformanceMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for performance metrics and monitoring.
 * Provides endpoints for administrators to monitor system performance.
 */
@RestController
@RequestMapping("/api/admin/metrics")
@Tag(name = "Performance Metrics", description = "System performance monitoring endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PerformanceMetricsController {

    private final PerformanceMetricsService performanceMetricsService;

    public PerformanceMetricsController(PerformanceMetricsService performanceMetricsService) {
        this.performanceMetricsService = performanceMetricsService;
    }

    /**
     * Retrieves aggregated performance metrics for the specified time range.
     *
     * @param timeRange Time range for metrics (HOUR, DAY, WEEK)
     * @return Performance metrics response
     */
    @GetMapping("/performance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get performance metrics",
            description = "Retrieves aggregated performance metrics including response times, error rates, database connections, and resource usage"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<PerformanceMetricsResponse> getPerformanceMetrics(
            @Parameter(description = "Time range for metrics", example = "HOUR")
            @RequestParam(defaultValue = "HOUR") String timeRange
    ) {
        PerformanceMetricsResponse metrics = performanceMetricsService.getAggregatedMetrics(timeRange);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Checks performance alert conditions and returns their status.
     *
     * @return Map of alert conditions and whether they are triggered
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Check performance alerts",
            description = "Checks if any performance thresholds are exceeded (response time > 1s, error rate > 1%, connection pool > 90%, memory > 85%, CPU > 80%)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert status retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<Map<String, Boolean>> getPerformanceAlerts() {
        Map<String, Boolean> alerts = performanceMetricsService.checkPerformanceAlerts();
        return ResponseEntity.ok(alerts);
    }
}
