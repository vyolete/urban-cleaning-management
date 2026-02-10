package com.urbanclean.dto.request;

import com.urbanclean.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for initiating password reset
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetInitiateRequest {

    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;
}
