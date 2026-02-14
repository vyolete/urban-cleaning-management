package com.urbanclean.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for rejecting task resolution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectFeedbackRequest {

    @NotBlank(message = "Justification is required when rejecting resolution")
    @Size(min = 10, max = 500, message = "Justification must be between 10 and 500 characters")
    private String justification;
}
