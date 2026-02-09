package com.urbanclean.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for filtering analytics queries
 * Supports date range, zone, category, and pagination filters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsFilters {
    
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDateTime startDate;
    
    @PastOrPresent(message = "End date cannot be in the future")
    private LocalDateTime endDate;
    
    private UUID zoneId;
    
    private String category;
    
    private Integer page = 0;
    
    private Integer size = 20;
    
    /**
     * Apply default date range if not specified (last 30 days)
     */
    public void applyDefaults() {
        if (startDate == null && endDate == null) {
            endDate = LocalDateTime.now();
            startDate = endDate.minusDays(30);
        } else if (startDate == null) {
            startDate = endDate.minusDays(30);
        } else if (endDate == null) {
            endDate = LocalDateTime.now();
        }
    }
}
