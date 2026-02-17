package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for JWT token expiration configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenExpirationResponse {

    private UUID id;
    private Integer accessTokenExpirationMinutes;
    private Integer refreshTokenExpirationDays;
    private LocalDateTime effectiveFrom;
    private String updatedByUsername;
    private UUID updatedById;
}
