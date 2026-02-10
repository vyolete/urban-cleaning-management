package com.urbanclean.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for refresh token responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response body for token refresh containing new access and refresh tokens")
public class RefreshTokenResponse {

    @Schema(
        description = "New JWT access token. Replace the old token with this one.",
        example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTYwOTQ1OTIwMCwiZXhwIjoxNjA5NDYyODAwfQ.signature"
    )
    private String accessToken;

    @Schema(
        description = "New JWT refresh token. The old refresh token is now invalid (token rotation).",
        example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTYwOTQ1OTIwMCwiZXhwIjoxNjEwMDY0MDAwfQ.signature"
    )
    private String refreshToken;

    @Schema(
        description = "Token type, always 'Bearer' for JWT tokens",
        example = "Bearer",
        defaultValue = "Bearer"
    )
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(
        description = "New access token expiration time in milliseconds from now",
        example = "900000"
    )
    private Long expiresIn;
}
