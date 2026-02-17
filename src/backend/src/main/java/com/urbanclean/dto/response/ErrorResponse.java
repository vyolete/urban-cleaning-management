package com.urbanclean.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for error responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response structure for all API errors")
public class ErrorResponse {
    
    @Schema(
        description = "Machine-readable error code for categorizing the error type",
        example = "VALIDATION_ERROR",
        allowableValues = {"VALIDATION_ERROR", "AUTHENTICATION_ERROR", "AUTHORIZATION_ERROR", "RESOURCE_NOT_FOUND", "DUPLICATE_RESOURCE", "GEOFENCING_ERROR", "INTERNAL_ERROR"}
    )
    private String errorCode;
    
    @Schema(
        description = "Human-readable error message explaining what went wrong",
        example = "Coordinates outside geofencing boundaries"
    )
    private String message;
    
    @Schema(
        description = "Timestamp when the error occurred in ISO 8601 format",
        example = "2026-02-09T19:30:00"
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "Additional details about the error providing context (optional)",
        example = "{\"latitude\": \"40.7128\", \"longitude\": \"-74.0060\", \"maxDistance\": \"50km\"}"
    )
    private Map<String, Object> details;
    
    @Schema(
        description = "HTTP status code",
        example = "400"
    )
    private Integer status;
    
    @Schema(
        description = "Request path where the error occurred",
        example = "/api/reports"
    )
    private String path;
}
