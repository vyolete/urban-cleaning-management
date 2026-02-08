package com.urbanclean.controller;

import com.urbanclean.dto.request.ReportSubmissionRequest;
import com.urbanclean.dto.response.ReportResponse;
import com.urbanclean.entity.Report;
import com.urbanclean.service.ReportService;
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
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Submit a new report (multipart request)
     * POST /api/reports
     * Accessible by authenticated users (citizens, operators, admins)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CIUDADANO', 'TECNICO', 'ADMIN')")
    public ResponseEntity<ReportResponse> submitReport(
            @Valid @RequestPart("data") ReportSubmissionRequest request,
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
                .submitterUsername(report.getSubmitter().getUsername())
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
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<Report> getReport(@PathVariable UUID id) {
        log.info("Get report request: id={}", id);
        Report report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    /**
     * Get all reports
     * GET /api/reports
     * Accessible by operators and admins
     */
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
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'TECNICO', 'ADMIN')")
    public ResponseEntity<List<ReportResponse>> getMyReports() {
        log.info("Get my reports request");
        List<ReportResponse> reports = reportService.getMyReports();
        return ResponseEntity.ok(reports);
    }
}
