package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for operator performance metrics
 * Contains performance data for all operators with pagination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorPerformanceResponse {
    
    private List<OperatorMetrics> operators;
    private Integer totalOperators;
    private Integer page;
    private Integer totalPages;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperatorMetrics {
        private UUID operatorId;
        private String username;
        private Integer tasksResolved;
        private Double averageResolutionTimeHours;
        private Integer tasksInProgress;
        private Integer tasksReopened;
        private LocalDateTime activeSince;
    }
}
