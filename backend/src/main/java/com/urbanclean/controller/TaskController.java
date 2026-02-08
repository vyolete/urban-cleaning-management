package com.urbanclean.controller;

import com.urbanclean.dto.request.TaskFilterRequest;
import com.urbanclean.dto.request.TaskStateUpdateRequest;
import com.urbanclean.dto.response.AuditLogResponse;
import com.urbanclean.dto.response.TaskResponse;
import com.urbanclean.entity.AuditLog;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.TaskState;
import com.urbanclean.repository.TaskRepository;
import com.urbanclean.service.AuditService;
import com.urbanclean.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for task management operations
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final AuditService auditService;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * Get all tasks with optional filtering
     * GET /api/tasks
     * Accessible by operators and admins
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @RequestParam(required = false) TaskState state,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon) {
        
        log.info("Get tasks request: state={}, geographic filter={}", 
                state, (minLat != null));

        List<Task> tasks;

        // Apply filters
        if (state != null && minLat != null && maxLat != null && minLon != null && maxLon != null) {
            // Filter by state and geographic zone
            Polygon zone = createBoundingBox(minLat, maxLat, minLon, maxLon);
            tasks = taskRepository.findByStateInZone(state, zone);
        } else if (state != null) {
            // Filter by state only
            tasks = taskRepository.findByStateOrderByPriorityScoreDesc(state);
        } else if (minLat != null && maxLat != null && minLon != null && maxLon != null) {
            // Filter by geographic zone only
            Polygon zone = createBoundingBox(minLat, maxLat, minLon, maxLon);
            tasks = taskRepository.findTasksInZone(zone);
        } else {
            // No filters, return all tasks ordered by priority
            tasks = taskRepository.findAllByOrderByPriorityScoreDesc();
        }

        List<TaskResponse> response = tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get task by ID
     * GET /api/tasks/{id}
     * Accessible by operators and admins
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID id) {
        log.info("Get task request: id={}", id);
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(mapToResponse(task));
    }

    /**
     * Update task state
     * PATCH /api/tasks/{id}/state
     * Accessible by operators and admins
     */
    @PatchMapping("/{id}/state")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<TaskResponse> updateTaskState(
            @PathVariable UUID id,
            @Valid @RequestBody TaskStateUpdateRequest request) {
        
        log.info("Update task state request: id={}, newState={}", id, request.getNewState());

        // Get current state before update
        Task task = taskService.getTaskById(id);
        TaskState previousState = task.getState();

        // Update state
        Task updatedTask = taskService.updateState(id, request.getNewState());

        // Log state change
        auditService.logStateChange(updatedTask, previousState, request.getNewState());

        return ResponseEntity.ok(mapToResponse(updatedTask));
    }

    /**
     * Get audit history for a task
     * GET /api/tasks/{id}/audit-history
     * Accessible by operators and admins
     */
    @GetMapping("/{id}/audit-history")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getTaskAuditHistory(@PathVariable UUID id) {
        log.info("Get task audit history request: id={}", id);
        
        List<AuditLog> auditLogs = auditService.getTaskAuditHistory(id);
        
        List<AuditLogResponse> response = auditLogs.stream()
                .map(this::mapAuditLogToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Create a bounding box polygon from coordinates
     */
    private Polygon createBoundingBox(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        Coordinate[] coordinates = new Coordinate[]{
            new Coordinate(minLon, minLat),
            new Coordinate(maxLon, minLat),
            new Coordinate(maxLon, maxLat),
            new Coordinate(minLon, maxLat),
            new Coordinate(minLon, minLat) // Close the polygon
        };
        return geometryFactory.createPolygon(coordinates);
    }

    /**
     * Map Task entity to TaskResponse DTO
     */
    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .latitude(task.getLocation().getY())
                .longitude(task.getLocation().getX())
                .category(task.getCategory())
                .state(task.getState())
                .priorityScore(task.getPriorityScore())
                .duplicateCount(task.getDuplicateCount())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .reportId(task.getReport().getId())
                .description(task.getReport().getDescription())
                .photoUrl(task.getReport().getPhotoUrl())
                .assignedOperatorUsername(
                    task.getAssignedOperator() != null ? 
                    task.getAssignedOperator().getUsername() : null
                )
                .build();
    }

    /**
     * Map AuditLog entity to AuditLogResponse DTO
     */
    private AuditLogResponse mapAuditLogToResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .taskId(auditLog.getTask().getId())
                .changedByUsername(auditLog.getChangedBy().getUsername())
                .previousState(auditLog.getPreviousState())
                .newState(auditLog.getNewState())
                .changedAt(auditLog.getChangedAt())
                .build();
    }
}
