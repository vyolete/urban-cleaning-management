package com.urbanclean.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AlgorithmConfig entity for storing prioritization algorithm parameters
 */
@Entity
@Table(name = "configuracion_algoritmo", indexes = {
    @Index(name = "idx_config_effective", columnList = "effective_from,effective_to")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlgorithmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 5, scale = 2, name = "weight_category")
    private BigDecimal weightCategory; // Wc

    @Column(nullable = false, precision = 5, scale = 2, name = "weight_zone")
    private BigDecimal weightZone; // Wz

    @Column(nullable = false, precision = 5, scale = 2, name = "weight_time")
    private BigDecimal weightTime; // Wt

    @Column(nullable = false, name = "distance_threshold_meters")
    private Double distanceThresholdMeters;

    @Column(nullable = false, name = "time_window_hours")
    private Integer timeWindowHours;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        if (effectiveFrom == null) {
            effectiveFrom = LocalDateTime.now();
        }
    }
}
