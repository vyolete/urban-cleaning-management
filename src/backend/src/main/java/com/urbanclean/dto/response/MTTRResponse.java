package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for Mean Time To Resolution (MTTR) analytics
 * Contains MTTR metrics and resolution time distribution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MTTRResponse {
    
    private Double mttrHours;
    private Integer tasksResolved;
    private Double averagePriorityScore;
    private Map<String, Integer> resolutionTimeDistribution;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
