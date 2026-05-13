package com.urbanclean.dto.response;

import com.urbanclean.entity.TaskState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for task response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Task information including location, priority, state, and assignment details")
public class TaskResponse {
    
    @Schema(description = "Unique task identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "Latitude coordinate of the task location", example = "40.7128", minimum = "-90", maximum = "90")
    private Double latitude;
    
    @Schema(description = "Longitude coordinate of the task location", example = "-74.0060", minimum = "-180", maximum = "180")
    private Double longitude;
    
    @Schema(
        description = "Category of the incident",
        example = "BASURA_ACUMULADA",
        allowableValues = {"BASURA_ACUMULADA", "CONTENEDOR_DANADO", "VERTIDO_ILEGAL", "LIMPIEZA_GRAFFITI", "OTRO"}
    )
    private String category;
    
    @Schema(
        description = "Current state of the task",
        example = "PENDIENTE",
        allowableValues = {"PENDIENTE", "ASIGNADO", "EN_PROGRESO", "RESUELTO", "REABIERTO"}
    )
    private TaskState state;
    
    @Schema(
        description = "Calculated priority score based on category, zone risk, and time elapsed. Higher values indicate higher priority.",
        example = "75.5"
    )
    private BigDecimal priorityScore;
    
    @Schema(
        description = "Number of duplicate reports detected for this incident",
        example = "3"
    )
    private Integer duplicateCount;
    
    @Schema(description = "Timestamp when the task was created", example = "2026-02-09T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the task was last updated", example = "2026-02-09T15:45:00")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Timestamp when the task was resolved (null if not resolved)", example = "2026-02-09T16:30:00")
    private LocalDateTime resolvedAt;
    
    @Schema(description = "ID of the original report that created this task", example = "660e8400-e29b-41d4-a716-446655440000")
    private UUID reportId;
    
    @Schema(description = "Detailed description of the incident from the report", example = "Large pile of garbage bags blocking the sidewalk")
    private String description;
    
    @Schema(description = "URL to the photo of the incident", example = "/uploads/reports/photo123.jpg")
    private String photoUrl;
    
    @Schema(description = "Username of the operator assigned to this task (null if unassigned)", example = "operator1")
    private String assignedOperatorUsername;
    
    @Schema(description = "Country ID where the task is located", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID countryId;
    
    @Schema(description = "Country name where the task is located", example = "España")
    private String countryName;
}
