package com.urbanclean.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for refresh token requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for refreshing an access token using a refresh token")
public class RefreshTokenRequest {

    @Schema(
        description = "Valid refresh token obtained during login or previous refresh. Used to obtain a new access token without re-authentication.",
        example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTYwOTQ1OTIwMCwiZXhwIjoxNjEwMDY0MDAwfQ.signature",
        required = true
    )
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
