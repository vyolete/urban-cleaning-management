package com.urbanclean.dto.request;

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
public class AlgorithmWeightsRequest {
    
    @NotNull(message = "Category weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Category weight must be positive")
    @DecimalMax(value = "1.0", message = "Category weight must not exceed 1.0")
    private BigDecimal weightCategory;
    
    @NotNull(message = "Zone weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Zone weight must be positive")
    @DecimalMax(value = "1.0", message = "Zone weight must not exceed 1.0")
    private BigDecimal weightZone;
    
    @NotNull(message = "Time weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Time weight must be positive")
    @DecimalMax(value = "1.0", message = "Time weight must not exceed 1.0")
    private BigDecimal weightTime;
    
    @NotNull(message = "Deduplication distance is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Deduplication distance must be positive")
    private BigDecimal deduplicationDistanceMeters;
    
    @NotNull(message = "Deduplication time window is required")
    @DecimalMin(value = "1", message = "Deduplication time window must be at least 1 hour")
    private Integer deduplicationTimeWindowHours;
}
