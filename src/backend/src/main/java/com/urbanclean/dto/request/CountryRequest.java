package com.urbanclean.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for country creation and update requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for creating or updating a country configuration")
public class CountryRequest {

    @Schema(
        description = "Country name",
        example = "España",
        required = true
    )
    @NotBlank(message = "Country name is required")
    private String name;

    @Schema(
        description = "ISO 3166-1 alpha-3 country code",
        example = "ESP",
        required = true,
        minLength = 3,
        maxLength = 3
    )
    @NotBlank(message = "Country code is required")
    @Size(min = 3, max = 3, message = "Country code must be exactly 3 characters")
    private String code;

    @Schema(
        description = "Minimum latitude for geofencing boundary",
        example = "36.0",
        required = true
    )
    @NotNull(message = "Minimum latitude is required")
    private BigDecimal minLat;

    @Schema(
        description = "Maximum latitude for geofencing boundary",
        example = "43.8",
        required = true
    )
    @NotNull(message = "Maximum latitude is required")
    private BigDecimal maxLat;

    @Schema(
        description = "Minimum longitude for geofencing boundary",
        example = "-9.3",
        required = true
    )
    @NotNull(message = "Minimum longitude is required")
    private BigDecimal minLon;

    @Schema(
        description = "Maximum longitude for geofencing boundary",
        example = "3.3",
        required = true
    )
    @NotNull(message = "Maximum longitude is required")
    private BigDecimal maxLon;

    @Schema(
        description = "Administrative area (state, province, department, region)",
        example = "Comunidad de Madrid"
    )
    private String administrativeArea;

    @Schema(
        description = "Municipality (city)",
        example = "Madrid"
    )
    private String municipality;

    @Schema(
        description = "Center latitude for map centering",
        example = "40.4168"
    )
    private BigDecimal centerLat;

    @Schema(
        description = "Center longitude for map centering",
        example = "-3.7038"
    )
    private BigDecimal centerLon;
}
