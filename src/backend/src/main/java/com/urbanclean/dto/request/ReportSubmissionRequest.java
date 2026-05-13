package com.urbanclean.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for report submission requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for submitting a new incident report. Photo must be sent as multipart/form-data.")
public class ReportSubmissionRequest {

    @Schema(
        description = "Latitude coordinate of the incident location. Must be within geofencing boundaries.",
        example = "40.7128",
        required = true,
        minimum = "-90",
        maximum = "90"
    )
    @NotNull(message = "Latitude is required")
    private Double latitude;

    @Schema(
        description = "Longitude coordinate of the incident location. Must be within geofencing boundaries.",
        example = "-74.0060",
        required = true,
        minimum = "-180",
        maximum = "180"
    )
    @NotNull(message = "Longitude is required")
    private Double longitude;

    @Schema(
        description = "Category of the incident",
        example = "BASURA_ACUMULADA",
        required = true,
        allowableValues = {"BASURA_ACUMULADA", "CONTENEDOR_DANADO", "VERTIDO_ILEGAL", "LIMPIEZA_GRAFFITI", "OTRO"}
    )
    @NotBlank(message = "Category is required")
    private String category;

    @Schema(
        description = "Detailed description of the incident. Provide as much information as possible.",
        example = "Large pile of garbage bags on the sidewalk blocking pedestrian access",
        required = true,
        minLength = 10,
        maxLength = 500
    )
    @NotBlank(message = "Description is required")
    private String description;

    @Schema(
        description = "Country ID for the report location",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID countryId;
}
