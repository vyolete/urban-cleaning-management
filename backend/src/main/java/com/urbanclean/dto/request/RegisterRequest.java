package com.urbanclean.dto.request;

import com.urbanclean.entity.UserRole;
import com.urbanclean.validation.ValidEmail;
import com.urbanclean.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration requests with enhanced validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for new user registration")
public class RegisterRequest {

    @Schema(
        description = "Unique username for the new account. Must be between 3 and 50 characters.",
        example = "johndoe",
        required = true,
        minLength = 3,
        maxLength = 50
    )
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(
        description = "Valid email address. Must follow standard email format and not be already registered.",
        example = "john.doe@example.com",
        required = true,
        format = "email"
    )
    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;

    @Schema(
        description = "Strong password. Must be at least 8 characters with uppercase, lowercase, number, and special character.",
        example = "SecurePass123!",
        required = true,
        format = "password",
        minLength = 8
    )
    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;

    @Schema(
        description = "User role. Defaults to CIUDADANO if not specified. Only admins can assign TECNICO or ADMIN roles.",
        example = "CIUDADANO",
        allowableValues = {"CIUDADANO", "TECNICO", "ADMIN"},
        defaultValue = "CIUDADANO"
    )
    private UserRole role;
}
