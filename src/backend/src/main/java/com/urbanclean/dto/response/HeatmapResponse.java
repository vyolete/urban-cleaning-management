package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for geographic heatmap data
 * Contains grid cells with intensity values for visualization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapResponse {
    
    private List<HeatmapCell> cells;
    private Integer totalReports;
    private Double cellSizeMeters;
    private String aggregationLevel;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapCell {
        private Double latitude;
        private Double longitude;
        private Integer intensity;
        private Double normalizedIntensity; // 0.0 to 1.0
    }
}
