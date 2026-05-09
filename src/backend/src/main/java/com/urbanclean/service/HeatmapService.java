package com.urbanclean.service;

import com.urbanclean.dto.request.AnalyticsFilters;
import com.urbanclean.dto.response.HeatmapResponse;
import com.urbanclean.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating geographic heatmap data
 * Uses PostGIS spatial functions for efficient aggregation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapService {
    
    private final ReportRepository reportRepository;
    
    // Default cell size in degrees (~500 meters at equator)
    private static final double DEFAULT_CELL_SIZE_DEGREES = 0.005;
    
    /**
     * Generate heatmap data with grid-based aggregation
     * Cached for 10 minutes due to computational cost
     * 
     * @param filters analytics filters including date range and category
     * @param cellSizeMeters optional cell size in meters (default 500m)
     * @return heatmap response with normalized intensity values
     */
    @Cacheable(value = "heatmap", key = "#filters.toString() + '-' + #cellSizeMeters")
    public HeatmapResponse generateHeatmap(AnalyticsFilters filters, Double cellSizeMeters) {
        log.info("Generating heatmap with cell size: {} meters", cellSizeMeters);
        filters.applyDefaults();
        
        // Convert meters to degrees (approximate)
        // 1 degree latitude ≈ 111,000 meters
        double cellSizeDegrees = cellSizeMeters != null 
            ? cellSizeMeters / 111000.0 
            : DEFAULT_CELL_SIZE_DEGREES;
        
        List<Object[]> results = reportRepository.getHeatmapData(
            cellSizeDegrees,
            filters.getStartDate(),
            filters.getEndDate(),
            filters.getCategory()
        );
        
        if (results.isEmpty()) {
            return new HeatmapResponse(
                List.of(),
                0,
                cellSizeMeters != null ? cellSizeMeters : 500.0,
                "grid"
            );
        }
        
        // Find max intensity for normalization
        int maxIntensity = results.stream()
            .mapToInt(row -> ((Number) row[2]).intValue())
            .max()
            .orElse(1);
        
        // Calculate total reports
        int totalReports = results.stream()
            .mapToInt(row -> ((Number) row[2]).intValue())
            .sum();
        
        // Convert to HeatmapCell objects with normalized intensity
        List<HeatmapResponse.HeatmapCell> cells = results.stream()
            .map(row -> {
                Double latitude = ((Number) row[0]).doubleValue();
                Double longitude = ((Number) row[1]).doubleValue();
                Integer intensity = ((Number) row[2]).intValue();
                Double normalizedIntensity = intensity.doubleValue() / maxIntensity;
                
                return new HeatmapResponse.HeatmapCell(
                    latitude,
                    longitude,
                    intensity,
                    Math.round(normalizedIntensity * 1000.0) / 1000.0 // Round to 3 decimals
                );
            })
            .collect(Collectors.toList());
        
        log.info("Generated heatmap with {} cells, total reports: {}", cells.size(), totalReports);
        
        return new HeatmapResponse(
            cells,
            totalReports,
            cellSizeMeters != null ? cellSizeMeters : 500.0,
            "grid"
        );
    }
    
    /**
     * Generate heatmap data with grid-based aggregation filtered by country
     * Cached for 10 minutes due to computational cost
     * 
     * @param countryId optional country ID filter (null for all countries)
     * @param filters analytics filters including date range and category
     * @param cellSizeMeters optional cell size in meters (default 500m)
     * @return heatmap response with normalized intensity values
     */
    @Cacheable(value = "heatmapByCountry", key = "#countryId + '-' + #filters.toString() + '-' + #cellSizeMeters")
    public HeatmapResponse generateHeatmapByCountry(UUID countryId, AnalyticsFilters filters, Double cellSizeMeters) {
        log.info("Generating heatmap for country: {} with cell size: {} meters", countryId, cellSizeMeters);
        filters.applyDefaults();
        
        // Convert meters to degrees (approximate)
        // 1 degree latitude ≈ 111,000 meters
        double cellSizeDegrees = cellSizeMeters != null 
            ? cellSizeMeters / 111000.0 
            : DEFAULT_CELL_SIZE_DEGREES;
        
        // Convert UUID to String for native query (null-safe)
        String countryIdStr = countryId != null ? countryId.toString() : null;
        
        List<Object[]> results = reportRepository.getHeatmapDataByCountry(
            countryIdStr,
            cellSizeDegrees,
            filters.getStartDate(),
            filters.getEndDate(),
            filters.getCategory()
        );
        
        if (results.isEmpty()) {
            return new HeatmapResponse(
                List.of(),
                0,
                cellSizeMeters != null ? cellSizeMeters : 500.0,
                "grid"
            );
        }
        
        // Find max intensity for normalization
        int maxIntensity = results.stream()
            .mapToInt(row -> ((Number) row[2]).intValue())
            .max()
            .orElse(1);
        
        // Calculate total reports
        int totalReports = results.stream()
            .mapToInt(row -> ((Number) row[2]).intValue())
            .sum();
        
        // Convert to HeatmapCell objects with normalized intensity
        List<HeatmapResponse.HeatmapCell> cells = results.stream()
            .map(row -> {
                Double latitude = ((Number) row[0]).doubleValue();
                Double longitude = ((Number) row[1]).doubleValue();
                Integer intensity = ((Number) row[2]).intValue();
                Double normalizedIntensity = intensity.doubleValue() / maxIntensity;
                
                return new HeatmapResponse.HeatmapCell(
                    latitude,
                    longitude,
                    intensity,
                    Math.round(normalizedIntensity * 1000.0) / 1000.0 // Round to 3 decimals
                );
            })
            .collect(Collectors.toList());
        
        log.info("Generated heatmap for country {} with {} cells, total reports: {}", 
                countryId, cells.size(), totalReports);
        
        return new HeatmapResponse(
            cells,
            totalReports,
            cellSizeMeters != null ? cellSizeMeters : 500.0,
            "grid"
        );
    }
}
