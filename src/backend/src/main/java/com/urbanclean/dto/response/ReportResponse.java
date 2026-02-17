package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for report responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private UUID id;
    private Double latitude;
    private Double longitude;
    private String category;
    private String description;
    private String photoUrl;
    private String submitterUsername;
    private LocalDateTime createdAt;
    private Boolean isDuplicate;
}
