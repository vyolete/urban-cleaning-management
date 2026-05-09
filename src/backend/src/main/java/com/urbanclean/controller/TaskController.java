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
@Tag(name = "Tasks", description = "Endpoints for managing cleaning tasks and assignments")
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
    @Operation(
        summary = "Get all tasks",
        description = "Retrieve all cleaning tasks with optional filtering by state and geographic zone. " +
                     "Tasks are ordered by priority score (descending).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tasks retrieved successfully"
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
    public ResponseEntity<List<TaskResponse>> getTasks(
            @Parameter(description = "Filter by country ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) UUID countryId,
            @Parameter(description = "Filter by task state", example = "PENDIENTE")
            @RequestParam(required = false) TaskState state,
            @Parameter(description = "Filter by category", example = "BASURA_ACUMULADA")
            @RequestParam(required = false) String category,
            @Parameter(description = "Minimum latitude for geographic filter", example = "40.4")
            @RequestParam(required = false) Double minLat,
            @Parameter(description = "Maximum latitude for geographic filter", example = "40.5")
            @RequestParam(required = false) Double maxLat,
            @Parameter(description = "Minimum longitude for geographic filter", example = "-3.8")
            @RequestParam(required = false) Double minLon,
            @Parameter(description = "Maximum longitude for geographic filter", example = "-3.6")
            @RequestParam(required = false) Double maxLon) {
        
        log.info("Get tasks request: countryId={}, state={}, category={}, geographic filter={}", 
                countryId, state, category, (minLat != null));

        List<Task> tasks;

        // Apply country filter first if provided
        if (countryId != null && state != null) {
            tasks = taskRepository.findByCountryIdAndState(countryId, state);
        } else if (countryId != null && category != null) {
            tasks = taskRepository.findByCountryIdAndCategory(countryId, category);
        } else if (countryId != null) {
            tasks = taskRepository.findByCountryId(countryId);
        } else if (state != null && minLat != null && maxLat != null && minLon != null && maxLon != null) {
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
    @Operation(
        summary = "Get task by ID",
        description = "Retrieve detailed information about a specific cleaning task",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task found",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))
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
            description = "Task not found"
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<TaskResponse> getTask(
            @Parameter(description = "Task ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        log.info("Get task request: id={}", id);
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(mapToResponse(task));
    }

    /**
     * Update task state
     * PATCH /api/tasks/{id}/state
     * Accessible by operators and admins
     */
    @Operation(
        summary = "Update task state",
        description = "Change the state of a task (PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO). " +
                     "State changes are logged in audit history.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task state updated successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid state transition"
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
            description = "Task not found"
        )
    })
    @PatchMapping("/{id}/state")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<TaskResponse> updateTaskState(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New task state", required = true)
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
     * Assign task to an operator
     * POST /api/tasks/{id}/assign
     * Accessible by admins
     */
    @Operation(
        summary = "Assign task to operator",
        description = "Assign a task to a specific operator. Task state automatically changes to ASIGNADO. " +
                     "Triggers email notification to the assigned operator.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task assigned successfully",
            content = @Content(schema = @Schema(implementation = TaskResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid operator ID or task already assigned"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires ADMIN role"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task or operator not found"
        )
    })
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> assignTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Operator user ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam UUID operatorId) {
        
        log.info("Assign task request: taskId={}, operatorId={}", id, operatorId);

        // Get current state before assignment
        Task task = taskService.getTaskById(id);
        TaskState previousState = task.getState();

        // Assign task
        Task assignedTask = taskService.assignTask(id, operatorId);

        // Log state change
        auditService.logStateChange(assignedTask, previousState, TaskState.ASIGNADO);

        return ResponseEntity.ok(mapToResponse(assignedTask));
    }

    /**
     * Get audit history for a task
     * GET /api/tasks/{id}/audit-history
     * Accessible by operators and admins
     */
    @Operation(
        summary = "Get task audit history",
        description = "Retrieve complete audit trail for a task including all state changes, assignments, and modifications",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Audit history retrieved successfully"
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
            description = "Task not found"
        )
    })
    @GetMapping("/{id}/audit-history")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getTaskAuditHistory(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID id) {
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
                .resolvedAt(task.getResolvedAt())
                .reportId(task.getPrimaryReport().getId())
                .description(task.getPrimaryReport().getDescription())
                .photoUrl(task.getPrimaryReport().getPhotoUrl())
                .assignedOperatorUsername(
                    task.getAssignedOperator() != null ? 
                    task.getAssignedOperator().getUsername() : null
                )
                .countryId(task.getCountry() != null ? task.getCountry().getId() : null)
                .countryName(task.getCountry() != null ? task.getCountry().getName() : null)
                .build();
    }

    /**
     * Map AuditLog entity to AuditLogResponse DTO
     */
    private AuditLogResponse mapAuditLogToResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .taskId(auditLog.getTask().getId())
                .changedByUsername(auditLog.getUser().getUsername())
                .previousState(auditLog.getPreviousState())
                .newState(auditLog.getNewState())
                .changedAt(auditLog.getChangedAt())
                .build();
    }
}
