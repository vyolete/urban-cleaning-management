package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for duplicate detection configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicateDetectionResponse {

    private UUID id;
    private Integer detectionRadiusMeters;
    private Integer timeWindowHours;
    private Boolean requireSameCategory;
    private LocalDateTime effectiveFrom;
    private String updatedByUsername;
    private UUID updatedById;
}
