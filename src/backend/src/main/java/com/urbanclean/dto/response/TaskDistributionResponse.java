package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for task distribution analytics
 * Contains distribution data by category or state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDistributionResponse {
    
    private List<DistributionItem> distribution;
    private Integer totalTasks;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionItem {
        private String label;
        private Integer count;
        private Double percentage;
    }
}
