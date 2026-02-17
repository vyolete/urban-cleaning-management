package com.urbanclean.controller;

import com.urbanclean.dto.request.AnalyticsFilters;
import com.urbanclean.dto.response.HeatmapResponse;
import com.urbanclean.dto.response.MTTRResponse;
import com.urbanclean.dto.response.OperatorPerformanceResponse;
import com.urbanclean.dto.response.TaskDistributionResponse;
import com.urbanclean.service.AnalyticsService;
import com.urbanclean.service.HeatmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST Controller for analytics endpoints
 * Provides aggregated data for operational dashboard
 * Accessible by operators and administrators
 */
@Tag(name = "Analytics", description = "Endpoints for operational analytics, KPIs, and performance metrics")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final HeatmapService heatmapService;
    
    /**
     * Get task distribution by category
     * 
     * @param startDate optional start date (ISO 8601 format)
     * @param endDate optional end date (ISO 8601 format)
     * @param zoneId optional zone filter
     * @param category optional category filter
     * @return task distribution response
     */
    @Operation(
        summary = "Get task distribution by category",
        description = "Retrieve task counts and percentages grouped by category. " +
                     "Results are cached for 5 minutes. Defaults to last 30 days if no date range specified.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Distribution retrieved successfully",
            content = @Content(schema = @Schema(implementation = TaskDistributionResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires TECNICO or ADMIN role"
        )
    })
    @GetMapping("/tasks/distribution/category")
    public ResponseEntity<TaskDistributionResponse> getTaskDistributionByCategory(
        @Parameter(description = "Start date (ISO 8601)", example = "2026-01-01T00:00:00")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO 8601)", example = "2026-02-09T23:59:59")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @Parameter(description = "Zone ID filter")
        @RequestParam(required = false) UUID zoneId,
        @Parameter(description = "Category filter", example = "BASURA")
        @RequestParam(required = false) String category
    ) {
        log.info("GET /api/analytics/tasks/distribution/category - startDate: {}, endDate: {}", startDate, endDate);
        
        AnalyticsFilters filters = new AnalyticsFilters(startDate, endDate, zoneId, category, 0, 20);
        TaskDistributionResponse response = analyticsService.getTaskDistributionByCategory(filters);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get task distribution by state
     * 
     * @param startDate optional start date (ISO 8601 format)
     * @param endDate optional end date (ISO 8601 format)
     * @param zoneId optional zone filter
     * @param category optional category filter
     * @return task distribution response
     */
    @Operation(
        summary = "Get task distribution by state",
        description = "Retrieve task counts and percentages grouped by state (PENDIENTE, ASIGNADO, EN_PROGRESO, RESUELTO). " +
                     "Results are cached for 5 minutes.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Distribution retrieved successfully",
            content = @Content(schema = @Schema(implementation = TaskDistributionResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden"
        )
    })
    @GetMapping("/tasks/distribution/state")
    public ResponseEntity<TaskDistributionResponse> getTaskDistributionByState(
        @Parameter(description = "Start date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @Parameter(description = "Zone ID filter")
        @RequestParam(required = false) UUID zoneId,
        @Parameter(description = "Category filter")
        @RequestParam(required = false) String category
    ) {
        log.info("GET /api/analytics/tasks/distribution/state - startDate: {}, endDate: {}", startDate, endDate);
        
        AnalyticsFilters filters = new AnalyticsFilters(startDate, endDate, zoneId, category, 0, 20);
        TaskDistributionResponse response = analyticsService.getTaskDistributionByState(filters);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Calculate Mean Time To Resolution (MTTR)
     * 
     * @param startDate optional start date (ISO 8601 format)
     * @param endDate optional end date (ISO 8601 format)
     * @param category optional category filter
     * @return MTTR response with resolution time distribution
     */
    @Operation(
        summary = "Calculate Mean Time To Resolution (MTTR)",
        description = "Calculate average time to resolve tasks in hours. " +
                     "Includes resolution time distribution (<24h, 24-48h, 48-72h, >72h). " +
                     "Only includes tasks in RESUELTO state. Results cached for 5 minutes.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "MTTR calculated successfully",
            content = @Content(schema = @Schema(implementation = MTTRResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden"
        )
    })
    @GetMapping("/tasks/mttr")
    public ResponseEntity<MTTRResponse> calculateMTTR(
        @Parameter(description = "Start date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @Parameter(description = "Category filter")
        @RequestParam(required = false) String category
    ) {
        log.info("GET /api/analytics/tasks/mttr - startDate: {}, endDate: {}", startDate, endDate);
        
        AnalyticsFilters filters = new AnalyticsFilters(startDate, endDate, null, category, 0, 20);
        MTTRResponse response = analyticsService.calculateMTTR(filters);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get resolution time distribution histogram
     * Same as MTTR but focuses on distribution data
     * 
     * @param startDate optional start date (ISO 8601 format)
     * @param endDate optional end date (ISO 8601 format)
     * @param category optional category filter
     * @return MTTR response with distribution histogram
     */
    @Operation(
        summary = "Get resolution time distribution",
        description = "Retrieve histogram of task resolution times grouped into buckets (<24h, 24-48h, 48-72h, >72h). " +
                     "Useful for visualizing resolution time patterns.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Distribution retrieved successfully",
            content = @Content(schema = @Schema(implementation = MTTRResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden"
        )
    })
    @GetMapping("/tasks/resolution-time-distribution")
    public ResponseEntity<MTTRResponse> getResolutionTimeDistribution(
        @Parameter(description = "Start date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @Parameter(description = "Category filter")
        @RequestParam(required = false) String category
    ) {
        log.info("GET /api/analytics/tasks/resolution-time-distribution - startDate: {}, endDate: {}", startDate, endDate);
        
        // Reuse MTTR calculation which includes distribution
        AnalyticsFilters filters = new AnalyticsFilters(startDate, endDate, null, category, 0, 20);
        MTTRResponse response = analyticsService.calculateMTTR(filters);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generate geographic heatmap data
     * 
     * @param cellSize optional cell size in meters (default 500m, range 10-1000)
     * @param startDate optional start date (ISO 8601 format)
     * @param endDate optional end date (ISO 8601 format)
     * @param category optional category filter
     * @return heatmap response with grid cells and intensity
     */
    @Operation(
        summary = "Generate geographic heatmap",
        description = "Generate heatmap data showing incident concentration across geographic grid. " +
                     "Uses PostGIS spatial functions for efficient aggregation. " +
                     "Results cached for 10 minutes. Limited to top 1000 cells by intensity.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Heatmap generated successfully",
            content = @Content(schema = @Schema(implementation = HeatmapResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid cell size (must be 10-1000 meters)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden"
        )
    })
    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapResponse> generateHeatmap(
        @Parameter(description = "Grid cell size in meters", example = "500")
        @RequestParam(required = false, defaultValue = "500") Double cellSize,
        @Parameter(description = "Start date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @Parameter(description = "Category filter")
        @RequestParam(required = false) String category
    ) {
        log.info("GET /api/analytics/heatmap - cellSize: {}, startDate: {}, endDate: {}", cellSize, startDate, endDate);
        
        // Validate cell size
        if (cellSize < 10 || cellSize > 1000) {
            throw new IllegalArgumentException("Cell size must be between 10 and 1000 meters");
        }
        
        AnalyticsFilters filters = new AnalyticsFilters(startDate, endDate, null, category, 0, 20);
        HeatmapResponse response = heatmapService.generateHeatmap(filters, cellSize);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get operator performance metrics
     * 
     * @param startDate optional start date (ISO 8601 format)
     * @param endDate optional end date (ISO 8601 format)
     * @param operatorId optional operator filter
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @return operator performance response with pagination
     */
    @Operation(
        summary = "Get operator performance metrics",
        description = "Retrieve performance metrics for operators including tasks resolved, " +
                     "average resolution time, tasks in progress, and tasks reopened. " +
                     "Results ranked by tasks resolved (descending). Cached for 5 minutes.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Performance metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = OperatorPerformanceResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden"
        )
    })
    @GetMapping("/operators/performance")
    public ResponseEntity<OperatorPerformanceResponse> getOperatorPerformance(
        @Parameter(description = "Start date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @Parameter(description = "End date (ISO 8601)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(required = false) UUID operatorId,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        log.info("GET /api/analytics/operators/performance - page: {}, size: {}", page, size);
        
        AnalyticsFilters filters = new AnalyticsFilters(startDate, endDate, null, null, page, size);
        OperatorPerformanceResponse response = analyticsService.getOperatorPerformance(filters);
        
        return ResponseEntity.ok(response);
    }
}
