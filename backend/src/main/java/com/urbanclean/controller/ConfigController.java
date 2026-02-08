package com.urbanclean.controller;

import com.urbanclean.dto.request.AlgorithmWeightsRequest;
import com.urbanclean.dto.response.AlgorithmWeightsResponse;
import com.urbanclean.entity.AlgorithmConfig;
import com.urbanclean.repository.AlgorithmConfigRepository;
import com.urbanclean.service.ConfigService;
import com.urbanclean.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for algorithm configuration management
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@Slf4j
public class ConfigController {

    private final ConfigService configService;
    private final TaskService taskService;
    private final AlgorithmConfigRepository configRepository;

    /**
     * Get current algorithm weights
     * GET /api/admin/config/algorithm-weights
     * Accessible by admins only
     */
    @GetMapping("/algorithm-weights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlgorithmWeightsResponse> getCurrentWeights() {
        log.info("Get current algorithm weights request");
        
        AlgorithmConfig config = configService.getCurrentConfig();
        AlgorithmWeightsResponse response = mapToResponse(config);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update algorithm weights
     * PUT /api/admin/config/algorithm-weights
     * Accessible by admins only
     */
    @PutMapping("/algorithm-weights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlgorithmWeightsResponse> updateWeights(
            @Valid @RequestBody AlgorithmWeightsRequest request) {
        
        log.info("Update algorithm weights request: category={}, zone={}, time={}",
                request.getWeightCategory(), request.getWeightZone(), request.getWeightTime());

        // Update configuration
        AlgorithmConfig newConfig = configService.updateWeights(
                request.getWeightCategory(),
                request.getWeightZone(),
                request.getWeightTime(),
                request.getDeduplicationDistanceMeters(),
                request.getDeduplicationTimeWindowHours()
        );

        // Trigger priority recalculation for pending tasks
        log.info("Triggering priority recalculation for pending tasks");
        taskService.recalculatePendingTasksPriority();

        AlgorithmWeightsResponse response = mapToResponse(newConfig);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get configuration history
     * GET /api/admin/config/algorithm-weights/history
     * Accessible by admins only
     */
    @GetMapping("/algorithm-weights/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AlgorithmWeightsResponse>> getConfigurationHistory() {
        log.info("Get configuration history request");
        
        List<AlgorithmConfig> history = configRepository.findAllByOrderByEffectiveFromDesc();
        
        List<AlgorithmWeightsResponse> response = history.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Map AlgorithmConfig entity to AlgorithmWeightsResponse DTO
     */
    private AlgorithmWeightsResponse mapToResponse(AlgorithmConfig config) {
        return AlgorithmWeightsResponse.builder()
                .id(config.getId())
                .weightCategory(config.getWeightCategory())
                .weightZone(config.getWeightZone())
                .weightTime(config.getWeightTime())
                .deduplicationDistanceMeters(java.math.BigDecimal.valueOf(config.getDistanceThresholdMeters()))
                .deduplicationTimeWindowHours(config.getTimeWindowHours())
                .effectiveFrom(config.getEffectiveFrom())
                .effectiveTo(config.getEffectiveTo())
                .createdByUsername(
                    config.getCreatedBy() != null ? 
                    config.getCreatedBy().getUsername() : "system"
                )
                .build();
    }
}
