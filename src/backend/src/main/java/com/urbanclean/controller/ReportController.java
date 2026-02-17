package com.urbanclean.controller;

import com.urbanclean.dto.request.ReportSubmissionRequest;
import com.urbanclean.dto.response.ReportResponse;
import com.urbanclean.entity.Report;
import com.urbanclean.service.ReportService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for report operations
 */
@Tag(name = "Reports", description = "Endpoints for managing urban cleaning incident reports")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Submit a new report (multipart request)
     * POST /api/reports
     * Accessible by anyone (anonymous reports allowed)
     */
    @Operation(
        summary = "Submit a new incident report",
        description = "Create a new urban cleaning incident report with photo. Anonymous submissions allowed. " +
                     "Automatically checks for duplicates within configured radius and time window."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Report created successfully",
            content = @Content(schema = @Schema(implementation = ReportResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors or coordinates outside geofence"
        ),
        @ApiResponse(
            responseCode = "413",
            description = "Photo file too large (max 5MB)"
        )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> submitReport(
            @Parameter(description = "Report data (JSON)", required = true)
            @Valid @RequestPart("data") ReportSubmissionRequest request,
            @Parameter(description = "Photo of the incident (max 5MB)", required = true)
            @RequestPart("photo") MultipartFile photo) {
        
        log.info("Report submission request: category={}, location=({}, {})",
                request.getCategory(), request.getLatitude(), request.getLongitude());

        Report report = reportService.createReport(request, photo);
        
        ReportResponse response = ReportResponse.builder()
                .id(report.getId())
                .latitude(report.getLocation().getY())
                .longitude(report.getLocation().getX())
                .category(report.getCategory())
                .description(report.getDescription())
                .photoUrl(report.getPhotoUrl())
                .submitterUsername(report.getSubmitter() != null ? report.getSubmitter().getUsername() : "Anónimo")
                .createdAt(report.getCreatedAt())
                .isDuplicate(report.getIsDuplicate())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get report by ID
     * GET /api/reports/{id}
     * Accessible by operators and admins
     */
    @Operation(
        summary = "Get report by ID",
        description = "Retrieve detailed information about a specific report",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Report found",
            content = @Content(schema = @Schema(implementation = Report.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires TECNICO or ADMIN role"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Report not found"
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<Report> getReport(
            @Parameter(description = "Report ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        log.info("Get report request: id={}", id);
        Report report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    /**
     * Get all reports
     * GET /api/reports
     * Accessible by operators and admins
     */
    @Operation(
        summary = "Get all reports",
        description = "Retrieve all incident reports in the system",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Reports retrieved successfully"
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
    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<List<ReportResponse>> getAllReports() {
        log.info("Get all reports request");
        List<ReportResponse> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * Get my reports (current user's reports)
     * GET /api/reports/my
     * Accessible by authenticated users
     */
    @Operation(
        summary = "Get my reports",
        description = "Retrieve all reports submitted by the current authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Reports retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        )
    })
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'TECNICO', 'ADMIN')")
    public ResponseEntity<List<ReportResponse>> getMyReports() {
        log.info("Get my reports request");
        List<ReportResponse> reports = reportService.getMyReports();
        return ResponseEntity.ok(reports);
    }
}
