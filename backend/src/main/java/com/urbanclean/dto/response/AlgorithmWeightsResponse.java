package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for algorithm weights response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmWeightsResponse {
    
    private UUID id;
    private BigDecimal weightCategory;
    private BigDecimal weightZone;
    private BigDecimal weightTime;
    private BigDecimal deduplicationDistanceMeters;
    private Integer deduplicationTimeWindowHours;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String createdByUsername;
}
