package com.urbanclean.dto.response;

import com.urbanclean.entity.UserRole;
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
public class LoginResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;

    private UserRole role;

    private String username;
}
