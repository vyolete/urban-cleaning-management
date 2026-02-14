package com.urbanclean.dto.request;

import com.urbanclean.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for completing password reset
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCompleteRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @ValidPassword
    private String newPassword;
}
