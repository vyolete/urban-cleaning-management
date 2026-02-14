package com.urbanclean.dto.request;

import com.urbanclean.entity.TaskState;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for task state update request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStateUpdateRequest {
    
    @NotNull(message = "New state is required")
    private TaskState newState;
}
