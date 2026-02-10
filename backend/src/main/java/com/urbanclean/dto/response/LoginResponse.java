package com.urbanclean.dto.response;

import com.urbanclean.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response body for successful authentication containing access token, refresh token, and user information")
public class LoginResponse {

    @Schema(
        description = "JWT access token for API authentication. Include in Authorization header as 'Bearer {token}'",
        example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTYwOTQ1OTIwMCwiZXhwIjoxNjA5NDYyODAwfQ.signature"
    )
    private String token;

    @Schema(
        description = "JWT refresh token for obtaining new access tokens without re-authentication",
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
        description = "Access token expiration time in milliseconds from now",
        example = "900000"
    )
    private Long expiresIn;

    @Schema(
        description = "User role determining access permissions",
        example = "ADMIN",
        allowableValues = {"CIUDADANO", "TECNICO", "ADMIN"}
    )
    private UserRole role;

    @Schema(
        description = "Username of the authenticated user",
        example = "admin"
    )
    private String username;
}
