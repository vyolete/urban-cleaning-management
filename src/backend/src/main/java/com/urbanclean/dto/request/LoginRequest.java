package com.urbanclean.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for user authentication")
public class LoginRequest {

    @Schema(
        description = "Username for authentication",
        example = "admin",
        required = true,
        minLength = 1,
        maxLength = 50
    )
    @NotBlank(message = "Username is required")
    private String username;

    @Schema(
        description = "User password",
        example = "SecurePassword123!",
        required = true,
        format = "password",
        minLength = 8
    )
    @NotBlank(message = "Password is required")
    private String password;
}
