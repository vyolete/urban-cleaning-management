package com.urbanclean.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Country entity representing countries supported by the system
 * Each country has geofencing boundaries and administrative divisions
 */
@Entity
@Table(name = "countries", indexes = {
    @Index(name = "idx_countries_default", columnList = "default_country"),
    @Index(name = "idx_countries_enabled", columnList = "enabled"),
    @Index(name = "idx_countries_code", columnList = "code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String code;  // ISO 3166-1 alpha-3 code

    @Column(nullable = false, name = "default_country")
    @Builder.Default
    private Boolean defaultCountry = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    // Geofencing boundaries
    @Column(nullable = false, precision = 10, scale = 8, name = "min_lat")
    private BigDecimal minLat;

    @Column(nullable = false, precision = 10, scale = 8, name = "max_lat")
    private BigDecimal maxLat;

    @Column(nullable = false, precision = 11, scale = 8, name = "min_lon")
    private BigDecimal minLon;

    @Column(nullable = false, precision = 11, scale = 8, name = "max_lon")
    private BigDecimal maxLon;

    // Administrative divisions
    @Column(length = 100, name = "administrative_area")
    private String administrativeArea;

    @Column(length = 100)
    private String municipality;

    // Geographic center for map centering
    @Column(precision = 10, scale = 8, name = "center_lat")
    private BigDecimal centerLat;

    @Column(precision = 11, scale = 8, name = "center_lon")
    private BigDecimal centerLon;

    // Metadata
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
