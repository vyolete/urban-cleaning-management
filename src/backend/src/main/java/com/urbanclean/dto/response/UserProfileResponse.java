package com.urbanclean.dto.response;

import com.urbanclean.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user profile information
 * Validates: Requirement 18.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID userId;
    private String username;
    private String email;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isAnonymized;
    private Boolean deletionRequested;
    private Long gracePeriodDaysRemaining;
}
