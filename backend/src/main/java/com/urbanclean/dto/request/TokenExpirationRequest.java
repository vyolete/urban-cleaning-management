package com.urbanclean.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body for updating JWT token expiration settings. Changes affect newly issued tokens only.")
public class TokenExpirationRequest {

    @Schema(
        description = "Access token expiration time in minutes. Shorter times increase security but require more frequent refreshes.",
        example = "15",
        required = true,
        minimum = "5",
        maximum = "60"
    )
    @NotNull(message = "Access token expiration is required")
    @Min(value = 5, message = "Access token expiration must be at least 5 minutes")
    @Max(value = 60, message = "Access token expiration must not exceed 60 minutes")
    private Integer accessTokenExpirationMinutes;

    @Schema(
        description = "Refresh token expiration time in days. Determines how long users can stay logged in without re-authentication.",
        example = "7",
        required = true,
        minimum = "1",
        maximum = "30"
    )
    @NotNull(message = "Refresh token expiration is required")
    @Min(value = 1, message = "Refresh token expiration must be at least 1 day")
    @Max(value = 30, message = "Refresh token expiration must not exceed 30 days")
    private Integer refreshTokenExpirationDays;
}
