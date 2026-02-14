package com.urbanclean.dto.response;

import com.urbanclean.entity.TaskState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for audit log response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    
    private UUID id;
    private UUID taskId;
    private String changedByUsername;
    private TaskState previousState;
    private TaskState newState;
    private LocalDateTime changedAt;
}
