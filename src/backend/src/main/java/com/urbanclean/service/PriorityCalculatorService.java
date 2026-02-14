package com.urbanclean.service;

import com.urbanclean.entity.AlgorithmConfig;
import com.urbanclean.entity.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for calculating task priority scores
 * Implements the formula: P = (Wc * Category) + (Wz * Zone) + (Wt * Time)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriorityCalculatorService {

    private final ConfigService configService;

    // Category severity mapping (1-10 scale)
    private static final Map<String, Integer> CATEGORY_SEVERITY = new HashMap<>();
    
    static {
        // High severity (8-10)
        CATEGORY_SEVERITY.put("RESIDUOS_PELIGROSOS", 10);
        CATEGORY_SEVERITY.put("VERTIDO_ILEGAL", 9);
        CATEGORY_SEVERITY.put("CONTENEDOR_DAÑADO", 8);
        
        // Medium severity (5-7)
        CATEGORY_SEVERITY.put("ACUMULACION_BASURA", 7);
        CATEGORY_SEVERITY.put("GRAFITI", 6);
        CATEGORY_SEVERITY.put("MOBILIARIO_ROTO", 5);
        
        // Low severity (1-4)
        CATEGORY_SEVERITY.put("LIMPIEZA_GENERAL", 4);
        CATEGORY_SEVERITY.put("MANTENIMIENTO_JARDIN", 3);
        CATEGORY_SEVERITY.put("OTROS", 2);
    }

    // Zone risk index mapping (1-10 scale)
    // In a real system, this would be calculated from a spatial database
    // For now, we use a simplified approach based on location
    private static final Map<String, Integer> ZONE_RISK = new HashMap<>();
    
    static {
        ZONE_RISK.put("CENTRO", 8);
        ZONE_RISK.put("RESIDENCIAL", 5);
        ZONE_RISK.put("INDUSTRIAL", 6);
        ZONE_RISK.put("PERIFERIA", 4);
        ZONE_RISK.put("PARQUE", 7);
    }

    /**
     * Calculate priority score for a report
     * Formula: P = (Wc * Category) + (Wz * Zone) + (Wt * Time)
     */
    public BigDecimal calculatePriority(Report report) {
        AlgorithmConfig config = configService.getCurrentConfig();
        
        // Calculate each component
        BigDecimal categoryComponent = calculateCategoryComponent(report, config);
        BigDecimal zoneComponent = calculateZoneComponent(report, config);
        BigDecimal timeComponent = calculateTimeComponent(report, config);
        
        // Sum all components
        BigDecimal priorityScore = categoryComponent
                .add(zoneComponent)
                .add(timeComponent)
                .setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Priority calculated for report {}: category={}, zone={}, time={}, total={}",
                report.getId(), categoryComponent, zoneComponent, timeComponent, priorityScore);
        
        return priorityScore;
    }

    /**
     * Calculate category component: Wc * Category
     */
    private BigDecimal calculateCategoryComponent(Report report, AlgorithmConfig config) {
        Integer categoryValue = mapCategoryToValue(report.getCategory());
        return config.getWeightCategory()
                .multiply(new BigDecimal(categoryValue))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate zone component: Wz * Zone
     */
    private BigDecimal calculateZoneComponent(Report report, AlgorithmConfig config) {
        Integer zoneRiskIndex = calculateZoneRiskIndex(report.getLocation());
        return config.getWeightZone()
                .multiply(new BigDecimal(zoneRiskIndex))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate time component: Wt * Time
     */
    private BigDecimal calculateTimeComponent(Report report, AlgorithmConfig config) {
        BigDecimal hoursElapsed = calculateHoursElapsed(report.getCreatedAt());
        return config.getWeightTime()
                .multiply(hoursElapsed)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Map category string to numeric severity value (1-10)
     */
    public Integer mapCategoryToValue(String category) {
        if (category == null || category.trim().isEmpty()) {
            log.warn("Category is null or empty, using default value");
            return 1;
        }
        
        String normalizedCategory = category.toUpperCase().trim();
        Integer value = CATEGORY_SEVERITY.get(normalizedCategory);
        
        if (value == null) {
            log.warn("Unknown category: {}, using default value", category);
            return 2; // Default for unknown categories
        }
        
        return value;
    }

    /**
     * Calculate zone risk index from location (1-10)
     * In a real system, this would query a spatial database with zone polygons
     * For now, we use a simplified approach based on coordinates
     */
    public Integer calculateZoneRiskIndex(Point location) {
        if (location == null) {
            log.warn("Location is null, using default zone risk");
            return 5;
        }
        
        // Simplified zone determination based on coordinates
        // In production, this would use PostGIS spatial queries against zone polygons
        double latitude = location.getY();
        double longitude = location.getX();
        
        // Example logic: determine zone based on coordinate ranges
        // This is a placeholder - real implementation would use spatial joins
        String zone = determineZoneFromCoordinates(latitude, longitude);
        
        Integer riskIndex = ZONE_RISK.getOrDefault(zone, 5);
        log.debug("Zone determined: {} (risk index: {}) for location ({}, {})",
                zone, riskIndex, latitude, longitude);
        
        return riskIndex;
    }

    /**
     * Determine zone from coordinates (simplified)
     * In production, this would be replaced with PostGIS spatial query
     */
    private String determineZoneFromCoordinates(double latitude, double longitude) {
        // Placeholder logic - replace with actual spatial query
        // Example: SELECT zone_name FROM zones WHERE ST_Contains(geometry, ST_Point(lon, lat))
        
        // For now, use a simple grid-based approach
        if (latitude > 40.42 && latitude < 40.43 && longitude > -3.71 && longitude < -3.70) {
            return "CENTRO";
        } else if (latitude > 40.40 && latitude < 40.45) {
            return "RESIDENCIAL";
        } else if (longitude < -3.75) {
            return "INDUSTRIAL";
        } else if (latitude < 40.38) {
            return "PERIFERIA";
        } else {
            return "PARQUE";
        }
    }

    /**
     * Calculate hours elapsed since report creation
     * Returns a value that increases urgency over time
     */
    public BigDecimal calculateHoursElapsed(LocalDateTime createdAt) {
        if (createdAt == null) {
            log.warn("CreatedAt is null, using 0 hours");
            return BigDecimal.ZERO;
        }
        
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long hours = duration.toHours();
        
        // Convert to BigDecimal and normalize to 1-10 scale
        // Using logarithmic scale to prevent time from dominating the score
        // Formula: min(10, 1 + log10(hours + 1) * 3)
        double normalizedHours;
        if (hours == 0) {
            normalizedHours = 1.0;
        } else {
            normalizedHours = Math.min(10.0, 1.0 + Math.log10(hours + 1) * 3);
        }
        
        return new BigDecimal(normalizedHours).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Recalculate priority for an existing report
     * Used when algorithm weights are updated
     */
    public BigDecimal recalculatePriority(Report report) {
        log.info("Recalculating priority for report: {}", report.getId());
        return calculatePriority(report);
    }
}
