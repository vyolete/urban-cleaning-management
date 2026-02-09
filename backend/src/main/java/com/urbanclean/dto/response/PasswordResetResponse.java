package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response DTO for password reset operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetResponse {
    
    private boolean success;
    private String message;
}
