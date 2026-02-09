package com.urbanclean.dto.request;

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
public class DuplicateDetectionRequest {

    @NotNull(message = "Detection radius is required")
    @Min(value = 10, message = "Detection radius must be at least 10 meters")
    @Max(value = 1000, message = "Detection radius must not exceed 1000 meters")
    private Integer detectionRadiusMeters;

    @NotNull(message = "Time window is required")
    @Min(value = 1, message = "Time window must be at least 1 hour")
    @Max(value = 168, message = "Time window must not exceed 168 hours (7 days)")
    private Integer timeWindowHours;

    @NotNull(message = "Require same category flag is required")
    private Boolean requireSameCategory;
}
