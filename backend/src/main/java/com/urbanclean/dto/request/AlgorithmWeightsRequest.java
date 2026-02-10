package com.urbanclean.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for algorithm weights update request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for updating priority calculation algorithm weights. All weights must sum to 1.0.")
public class AlgorithmWeightsRequest {
    
    @Schema(
        description = "Weight for incident category in priority calculation. Higher values give more importance to incident type.",
        example = "0.4",
        required = true,
        minimum = "0.0",
        maximum = "1.0",
        exclusiveMinimum = true
    )
    @NotNull(message = "Category weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Category weight must be positive")
    @DecimalMax(value = "1.0", message = "Category weight must not exceed 1.0")
    private BigDecimal weightCategory;
    
    @Schema(
        description = "Weight for geographic zone risk in priority calculation. Higher values prioritize high-risk areas.",
        example = "0.3",
        required = true,
        minimum = "0.0",
        maximum = "1.0",
        exclusiveMinimum = true
    )
    @NotNull(message = "Zone weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Zone weight must be positive")
    @DecimalMax(value = "1.0", message = "Zone weight must not exceed 1.0")
    private BigDecimal weightZone;
    
    @Schema(
        description = "Weight for time elapsed since report creation. Higher values prioritize older reports.",
        example = "0.3",
        required = true,
        minimum = "0.0",
        maximum = "1.0",
        exclusiveMinimum = true
    )
    @NotNull(message = "Time weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Time weight must be positive")
    @DecimalMax(value = "1.0", message = "Time weight must not exceed 1.0")
    private BigDecimal weightTime;
    
    @Schema(
        description = "Geographic radius in meters for duplicate detection. Reports within this distance are considered potential duplicates.",
        example = "100",
        required = true,
        minimum = "0.0",
        exclusiveMinimum = true
    )
    @NotNull(message = "Deduplication distance is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Deduplication distance must be positive")
    private BigDecimal deduplicationDistanceMeters;
    
    @Schema(
        description = "Time window in hours for duplicate detection. Reports within this time frame are considered potential duplicates.",
        example = "24",
        required = true,
        minimum = "1"
    )
    @NotNull(message = "Deduplication time window is required")
    @DecimalMin(value = "1", message = "Deduplication time window must be at least 1 hour")
    private Integer deduplicationTimeWindowHours;
}
