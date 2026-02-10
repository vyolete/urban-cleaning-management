package com.urbanclean.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating duplicate detection configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for updating duplicate report detection parameters")
public class DuplicateDetectionRequest {

    @Schema(
        description = "Geographic radius in meters for duplicate detection. Larger values catch more duplicates but may flag distinct incidents.",
        example = "100",
        required = true,
        minimum = "10",
        maximum = "1000"
    )
    @NotNull(message = "Detection radius is required")
    @Min(value = 10, message = "Detection radius must be at least 10 meters")
    @Max(value = 1000, message = "Detection radius must not exceed 1000 meters")
    private Integer detectionRadiusMeters;

    @Schema(
        description = "Time window in hours for duplicate detection. Reports within this time frame and radius are considered potential duplicates.",
        example = "24",
        required = true,
        minimum = "1",
        maximum = "168"
    )
    @NotNull(message = "Time window is required")
    @Min(value = 1, message = "Time window must be at least 1 hour")
    @Max(value = 168, message = "Time window must not exceed 168 hours (7 days)")
    private Integer timeWindowHours;

    @Schema(
        description = "Whether to require the same category for duplicate detection. If true, only reports of the same type are considered duplicates.",
        example = "true",
        required = true
    )
    @NotNull(message = "Require same category flag is required")
    private Boolean requireSameCategory;
}
