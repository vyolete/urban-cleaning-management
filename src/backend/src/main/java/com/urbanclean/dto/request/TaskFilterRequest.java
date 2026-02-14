package com.urbanclean.dto.request;

import com.urbanclean.entity.TaskState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for task filtering parameters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilterRequest {
    
    /**
     * Filter by task state
     */
    private TaskState state;
    
    /**
     * Filter by geographic zone (bounding box)
     */
    private Double minLatitude;
    private Double maxLatitude;
    private Double minLongitude;
    private Double maxLongitude;
    
    /**
     * Check if geographic filter is provided
     */
    public boolean hasGeographicFilter() {
        return minLatitude != null && maxLatitude != null &&
               minLongitude != null && maxLongitude != null;
    }
}
