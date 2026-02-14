package com.urbanclean.dto.response;

import com.urbanclean.entity.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for feedback response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponse {
    
    private UUID id;
    private UUID taskId;
    private FeedbackType type;
    private String justification;
    private LocalDateTime submittedAt;
    private LocalDateTime feedbackDeadline;
    private boolean deadlinePassed;
}
