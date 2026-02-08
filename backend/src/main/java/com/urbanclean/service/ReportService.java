package com.urbanclean.service;

import com.urbanclean.dto.request.ReportSubmissionRequest;
import com.urbanclean.dto.response.ReportResponse;
import com.urbanclean.entity.Report;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.User;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.ReportRepository;
import com.urbanclean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for report management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final GeofencingService geofencingService;
    private final TaskService taskService;
    private final DeduplicationService deduplicationService;

    /**
     * Create a new report
     */
    @Transactional
    public Report createReport(ReportSubmissionRequest request, MultipartFile photo) {
        // Validate required fields
        validateReportRequest(request);

        // Validate coordinates using geofencing service
        geofencingService.validateCoordinates(request.getLatitude(), request.getLongitude());

        // Store photo
        String photoUrl = fileStorageService.storeFile(photo);

        // Get current authenticated user
        User submitter = getCurrentUser();

        // Create point geometry
        Point location = geofencingService.createPoint(request.getLatitude(), request.getLongitude());

        // Create report entity
        Report report = Report.builder()
                .submitter(submitter)
                .location(location)
                .category(request.getCategory())
                .description(request.getDescription())
                .photoUrl(photoUrl)
                .isDuplicate(false)
                .build();

        Report savedReport = reportRepository.save(report);
        log.info("Report created: {} by user: {}", savedReport.getId(), 
                submitter != null ? submitter.getUsername() : "Anonymous");

        // Check for duplicates
        Optional<Task> parentTask = deduplicationService.checkForDuplicates(savedReport);
        
        if (parentTask.isEmpty()) {
            // No duplicates found, create new task with priority calculation
            taskService.createTask(savedReport);
        } else {
            // Duplicate found, save the updated report with parent task reference
            reportRepository.save(savedReport);
        }

        return savedReport;
    }

    /**
     * Get report by ID
     */
    @Transactional(readOnly = true)
    public Report getReportById(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    /**
     * Get all reports (admin/operator only)
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get reports by current user
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports() {
        User currentUser = getCurrentUser();
        return reportRepository.findBySubmitter(currentUser).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validate report request
     */
    private void validateReportRequest(ReportSubmissionRequest request) {
        if (request.getLatitude() == null) {
            throw new ValidationException("Latitude is required");
        }
        if (request.getLongitude() == null) {
            throw new ValidationException("Longitude is required");
        }
        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new ValidationException("Category is required");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new ValidationException("Description is required");
        }
    }

    /**
     * Get current authenticated user (or null for anonymous reports)
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated (not anonymous)
        if (authentication == null || 
            !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return null;  // Anonymous report
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElse(null);  // Return null if user not found
    }

    /**
     * Map Report entity to ReportResponse DTO
     */
    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
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
    }
}
