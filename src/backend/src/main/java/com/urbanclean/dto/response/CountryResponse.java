package com.urbanclean.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for country response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Country configuration details")
public class CountryResponse {

    @Schema(description = "Country unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Country name", example = "España")
    private String name;

    @Schema(description = "ISO 3166-1 alpha-3 country code", example = "ESP")
    private String code;

    @Schema(description = "Whether this is the default country", example = "true")
    private Boolean defaultCountry;

    @Schema(description = "Whether this country is enabled", example = "true")
    private Boolean enabled;

    @Schema(description = "Minimum latitude for geofencing boundary", example = "36.0")
    private BigDecimal minLat;

    @Schema(description = "Maximum latitude for geofencing boundary", example = "43.8")
    private BigDecimal maxLat;

    @Schema(description = "Minimum longitude for geofencing boundary", example = "-9.3")
    private BigDecimal minLon;

    @Schema(description = "Maximum longitude for geofencing boundary", example = "3.3")
    private BigDecimal maxLon;

    @Schema(description = "Administrative area (state, province, department, region)", example = "Comunidad de Madrid")
    private String administrativeArea;

    @Schema(description = "Municipality (city)", example = "Madrid")
    private String municipality;

    @Schema(description = "Center latitude for map centering", example = "40.4168")
    private BigDecimal centerLat;

    @Schema(description = "Center longitude for map centering", example = "-3.7038")
    private BigDecimal centerLon;

    @Schema(description = "Country creation timestamp", example = "2026-05-09T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Country last update timestamp", example = "2026-05-09T10:30:00")
    private LocalDateTime updatedAt;
}
