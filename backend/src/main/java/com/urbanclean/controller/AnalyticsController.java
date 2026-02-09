package com.urbanclean.controller;

import com.urbanclean.dto.request.AnalyticsFilters;
import com.urbanclean.dto.response.HeatmapResponse;
import com.urbanclean.dto.response.MTTRResponse;
import com.urbanclean.dto.response.OperatorPerformanceResponse;
import com.urbanclean.dto.response.TaskDistributionResponse;
import com.urbanclean.service.AnalyticsService;
import com.urbanclean.service.HeatmapService;
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
    @GetMapping("/tasks/distribution/category")
    public ResponseEntity<TaskDistributionResponse> getTaskDistributionByCategory(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(required = false) UUID zoneId,
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
    @GetMapping("/tasks/distribution/state")
    public ResponseEntity<TaskDistributionResponse> getTaskDistributionByState(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(required = false) UUID zoneId,
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
    @GetMapping("/tasks/mttr")
    public ResponseEntity<MTTRResponse> calculateMTTR(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
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
    @GetMapping("/tasks/resolution-time-distribution")
    public ResponseEntity<MTTRResponse> getResolutionTimeDistribution(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
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
    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapResponse> generateHeatmap(
        @RequestParam(required = false, defaultValue = "500") Double cellSize,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
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
    @GetMapping("/operators/performance")
    public ResponseEntity<OperatorPerformanceResponse> getOperatorPerformance(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
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
