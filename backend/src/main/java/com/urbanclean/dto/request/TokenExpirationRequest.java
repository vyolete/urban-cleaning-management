package com.urbanclean.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating JWT token expiration configuration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenExpirationRequest {

    @NotNull(message = "Access token expiration is required")
    @Min(value = 5, message = "Access token expiration must be at least 5 minutes")
    @Max(value = 60, message = "Access token expiration must not exceed 60 minutes")
    private Integer accessTokenExpirationMinutes;

    @NotNull(message = "Refresh token expiration is required")
    @Min(value = 1, message = "Refresh token expiration must be at least 1 day")
    @Max(value = 30, message = "Refresh token expiration must not exceed 30 days")
    private Integer refreshTokenExpirationDays;
}
