package com.urbanclean.service;

import com.urbanclean.entity.AlgorithmConfig;
import com.urbanclean.entity.Report;
import com.urbanclean.entity.Task;
import com.urbanclean.repository.ReportRepository;
import com.urbanclean.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for detecting and managing duplicate reports
 * Uses spatial and temporal proximity to identify duplicates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicationService {

    private final ReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final ConfigService configService;

    /**
     * Check for duplicate reports BEFORE saving the new report
     * Returns the parent task if duplicates are found
     * This method is called before the report is persisted to avoid self-detection
     */
    @Transactional(readOnly = true)
    public Optional<Task> checkForDuplicatesBeforeSave(Report newReport) {
        AlgorithmConfig config = configService.getCurrentConfig();
        
        // Get deduplication parameters
        Double distanceThreshold = config.getDistanceThresholdMeters();
        Integer timeWindowHours = config.getTimeWindowHours();
        
        // Calculate time window
        LocalDateTime timeThreshold = newReport.getCreatedAt().minusHours(timeWindowHours);
        
        log.info("Checking for duplicates BEFORE save: distance={} meters, time window={} hours",
                distanceThreshold, timeWindowHours);
        
        // Find nearby reports within time window (no need to exclude ID since report isn't saved yet)
        List<Report> nearbyReports = reportRepository.findNearbyReportsWithinTimeWindowNoExclude(
                newReport.getLocation(),
                distanceThreshold,
                timeThreshold,
                newReport.getCategory()
        );
        
        if (nearbyReports.isEmpty()) {
            log.info("No duplicates found, will create new task");
            return Optional.empty();
        }
        
        log.info("Found {} potential duplicate(s), searching for parent task", 
                nearbyReports.size());
        
        // Find the parent task from nearby reports
        Task parentTask = findExistingParentTask(nearbyReports);
        
        if (parentTask == null) {
            log.info("No parent task found among nearby reports, will create new task");
            return Optional.empty();
        }
        
        log.info("Found parent task {} for duplicate report", parentTask.getId());
        return Optional.of(parentTask);
    }

    /**
     * Check for duplicate reports and link them to parent task
     * Returns the parent task if duplicates are found, or creates a new task
     * @deprecated Use checkForDuplicatesBeforeSave instead
     */
    @Deprecated
    @Transactional
    public Optional<Task> checkForDuplicates(Report newReport) {
        AlgorithmConfig config = configService.getCurrentConfig();
        
        // Get deduplication parameters
        Double distanceThreshold = config.getDistanceThresholdMeters();
        Integer timeWindowHours = config.getTimeWindowHours();
        
        // Calculate time window
        LocalDateTime timeThreshold = newReport.getCreatedAt().minusHours(timeWindowHours);
        
        log.debug("Checking for duplicates: distance={} meters, time window={} hours",
                distanceThreshold, timeWindowHours);
        
        // Find nearby reports within time window (excluding the current report)
        List<Report> nearbyReports = reportRepository.findNearbyReportsWithinTimeWindow(
                newReport.getLocation(),
                distanceThreshold,
                timeThreshold,
                newReport.getCategory(),
                newReport.getId()  // Exclude the current report
        );
        
        if (nearbyReports.isEmpty()) {
            log.debug("No duplicates found for report: {}", newReport.getId());
            return Optional.empty();
        }
        
        log.info("Found {} potential duplicate(s) for report: {}", 
                nearbyReports.size(), newReport.getId());
        
        // Find the parent task (task with highest priority among duplicates)
        Task parentTask = findOrCreateParentTask(nearbyReports, newReport);
        
        // If no parent task found, return empty (will create new task)
        if (parentTask == null) {
            log.debug("No parent task available for duplicates, will create new task");
            return Optional.empty();
        }
        
        // Mark new report as duplicate
        newReport.setIsDuplicate(true);
        newReport.setParentTask(parentTask);
        
        // Increment duplicate count on parent task
        parentTask.setDuplicateCount(parentTask.getDuplicateCount() + 1);
        
        // Update parent task priority if new report has higher priority
        updateParentTaskPriority(parentTask, newReport);
        
        taskRepository.save(parentTask);
        
        log.info("Report {} marked as duplicate of task {}", 
                newReport.getId(), parentTask.getId());
        
        return Optional.of(parentTask);
    }

    /**
     * Find existing parent task from nearby reports
     * Returns null if no task is found
     */
    private Task findExistingParentTask(List<Report> nearbyReports) {
        log.info("Searching for parent task among {} nearby reports", nearbyReports.size());
        
        // Check if any nearby report already has a parent task
        for (Report report : nearbyReports) {
            if (report.getParentTask() != null) {
                log.info("Found existing parent task {} from report {}", 
                        report.getParentTask().getId(), report.getId());
                return report.getParentTask();
            }
        }
        
        // No existing parent task found, try to find task from the first nearby report
        Report firstReport = nearbyReports.get(0);
        Optional<Task> existingTask = taskRepository.findByReportId(firstReport.getId());
        
        if (existingTask.isPresent()) {
            Task parentTask = existingTask.get();
            log.info("Found existing task {} for first nearby report {}", 
                    parentTask.getId(), firstReport.getId());
            return parentTask;
        }
        
        // No task found for nearby reports
        log.info("No existing task found among nearby reports");
        return null;
    }

    /**
     * Find existing parent task or create one from the first report
     */
    private Task findOrCreateParentTask(List<Report> nearbyReports, Report newReport) {
        log.info("DEDUPLICATION FIX: Searching for parent task among {} nearby reports", nearbyReports.size());
        
        // Check if any nearby report already has a parent task
        for (Report report : nearbyReports) {
            if (report.getParentTask() != null) {
                log.debug("Using existing parent task: {}", report.getParentTask().getId());
                return report.getParentTask();
            }
        }
        
        // No existing parent task found, try to find task from the first nearby report
        Report firstReport = nearbyReports.get(0);
        Optional<Task> existingTask = taskRepository.findByReportId(firstReport.getId());
        
        if (existingTask.isPresent()) {
            Task parentTask = existingTask.get();
            // Mark the first report as having duplicates
            firstReport.setParentTask(parentTask);
            log.info("DEDUPLICATION FIX: Found existing task {} for first nearby report", parentTask.getId());
            return parentTask;
        }
        
        // No task found for nearby reports, return null to indicate new task should be created
        log.info("DEDUPLICATION FIX: No existing task found, returning null to create new task");
        return null;
    }

    /**
     * Update parent task priority to the maximum among all duplicates
     */
    private void updateParentTaskPriority(Task parentTask, Report newReport) {
        // Get the task that would be created for the new report
        // We need to calculate its priority to compare
        BigDecimal currentPriority = parentTask.getPriorityScore();
        
        // For simplicity, we'll keep the current priority
        // In a more sophisticated implementation, we could recalculate
        // based on the aggregate of all duplicate reports
        
        log.debug("Parent task {} priority: {}", parentTask.getId(), currentPriority);
    }

    /**
     * Get all duplicate reports for a task
     */
    @Transactional(readOnly = true)
    public List<Report> getDuplicateReports(Task task) {
        return reportRepository.findByParentTask(task);
    }

    /**
     * Get duplicate count for a task
     */
    @Transactional(readOnly = true)
    public int getDuplicateCount(Task task) {
        return reportRepository.countByParentTask(task);
    }

    /**
     * Check if a report is a duplicate
     */
    public boolean isDuplicate(Report report) {
        return report.getIsDuplicate() != null && report.getIsDuplicate();
    }

    /**
     * Unlink a report from its parent task (admin operation)
     */
    @Transactional
    public void unlinkDuplicate(Report report) {
        if (!isDuplicate(report)) {
            log.warn("Report {} is not marked as duplicate", report.getId());
            return;
        }
        
        Task parentTask = report.getParentTask();
        if (parentTask != null) {
            // Decrement duplicate count
            int newCount = Math.max(0, parentTask.getDuplicateCount() - 1);
            parentTask.setDuplicateCount(newCount);
            taskRepository.save(parentTask);
            
            log.info("Unlinked report {} from parent task {}", 
                    report.getId(), parentTask.getId());
        }
        
        // Unmark as duplicate
        report.setIsDuplicate(false);
        report.setParentTask(null);
        reportRepository.save(report);
    }
}
