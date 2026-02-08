package com.urbanclean.dto.response;

import com.urbanclean.entity.TaskState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for task response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    private UUID id;
    private Double latitude;
    private Double longitude;
    private String category;
    private TaskState state;
    private BigDecimal priorityScore;
    private Integer duplicateCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Report information
    private UUID reportId;
    private String description;
    private String photoUrl;
    
    // Assignment information
    private String assignedOperatorUsername;
}
