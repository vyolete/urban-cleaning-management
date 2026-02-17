package com.urbanclean.service;

import com.urbanclean.dto.request.AnalyticsFilters;
import com.urbanclean.dto.response.MTTRResponse;
import com.urbanclean.dto.response.OperatorPerformanceResponse;
import com.urbanclean.dto.response.TaskDistributionResponse;
import com.urbanclean.entity.Task;
import com.urbanclean.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics operations
 * Provides aggregated data for dashboard with caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final TaskRepository taskRepository;
    
    /**
     * Get task distribution by category
     * Cached for 5 minutes to reduce database load
     */
    @Cacheable(value = "taskDistribution", key = "'category-' + #filters.toString()")
    public TaskDistributionResponse getTaskDistributionByCategory(AnalyticsFilters filters) {
        log.info("Calculating task distribution by category");
        filters.applyDefaults();
        
        List<Object[]> results = taskRepository.countByCategory(
            filters.getStartDate(),
            filters.getEndDate()
        );
        
        int totalTasks = results.stream()
            .mapToInt(row -> ((Long) row[1]).intValue())
            .sum();
        
        List<TaskDistributionResponse.DistributionItem> distribution = results.stream()
            .map(row -> {
                String category = (String) row[0];
                Integer count = ((Long) row[1]).intValue();
                Double percentage = totalTasks > 0 ? (count * 100.0 / totalTasks) : 0.0;
                return new TaskDistributionResponse.DistributionItem(category, count, percentage);
            })
            .collect(Collectors.toList());
        
        return new TaskDistributionResponse(
            distribution,
            totalTasks,
            filters.getStartDate(),
            filters.getEndDate()
        );
    }
    
    /**
     * Get task distribution by state
     * Cached for 5 minutes to reduce database load
     */
    @Cacheable(value = "taskDistribution", key = "'state-' + #filters.toString()")
    public TaskDistributionResponse getTaskDistributionByState(AnalyticsFilters filters) {
        log.info("Calculating task distribution by state");
        filters.applyDefaults();
        
        List<Object[]> results = taskRepository.countByState(
            filters.getStartDate(),
            filters.getEndDate()
        );
        
        int totalTasks = results.stream()
            .mapToInt(row -> ((Long) row[1]).intValue())
            .sum();
        
        List<TaskDistributionResponse.DistributionItem> distribution = results.stream()
            .map(row -> {
                String state = row[0].toString();
                Integer count = ((Long) row[1]).intValue();
                Double percentage = totalTasks > 0 ? (count * 100.0 / totalTasks) : 0.0;
                return new TaskDistributionResponse.DistributionItem(state, count, percentage);
            })
            .collect(Collectors.toList());
        
        return new TaskDistributionResponse(
            distribution,
            totalTasks,
            filters.getStartDate(),
            filters.getEndDate()
        );
    }
    
    /**
     * Calculate Mean Time To Resolution (MTTR)
     * Cached for 5 minutes to reduce database load
     */
    @Cacheable(value = "mttr", key = "#filters.toString()")
    public MTTRResponse calculateMTTR(AnalyticsFilters filters) {
        log.info("Calculating MTTR");
        filters.applyDefaults();
        
        List<Task> resolvedTasks = taskRepository.findResolvedTasks(
            filters.getStartDate(),
            filters.getEndDate()
        );
        
        if (resolvedTasks.isEmpty()) {
            return new MTTRResponse(
                0.0,
                0,
                0.0,
                new HashMap<>(),
                filters.getStartDate(),
                filters.getEndDate()
            );
        }
        
        // Calculate MTTR in hours
        double totalHours = 0.0;
        double totalPriority = 0.0;
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("<24h", 0);
        distribution.put("24-48h", 0);
        distribution.put("48-72h", 0);
        distribution.put(">72h", 0);
        
        for (Task task : resolvedTasks) {
            if (task.getResolvedAt() != null) {
                Duration duration = Duration.between(task.getCreatedAt(), task.getResolvedAt());
                double hours = duration.toHours();
                totalHours += hours;
                totalPriority += task.getPriorityScore().doubleValue();
                
                // Categorize resolution time
                if (hours < 24) {
                    distribution.put("<24h", distribution.get("<24h") + 1);
                } else if (hours < 48) {
                    distribution.put("24-48h", distribution.get("24-48h") + 1);
                } else if (hours < 72) {
                    distribution.put("48-72h", distribution.get("48-72h") + 1);
                } else {
                    distribution.put(">72h", distribution.get(">72h") + 1);
                }
            }
        }
        
        double mttrHours = totalHours / resolvedTasks.size();
        double avgPriority = totalPriority / resolvedTasks.size();
        
        return new MTTRResponse(
            Math.round(mttrHours * 100.0) / 100.0, // Round to 2 decimals
            resolvedTasks.size(),
            Math.round(avgPriority * 100.0) / 100.0,
            distribution,
            filters.getStartDate(),
            filters.getEndDate()
        );
    }
    
    /**
     * Get operator performance metrics
     * Cached for 5 minutes to reduce database load
     */
    @Cacheable(value = "operatorMetrics", key = "#filters.toString()")
    public OperatorPerformanceResponse getOperatorPerformance(AnalyticsFilters filters) {
        log.info("Calculating operator performance metrics");
        filters.applyDefaults();
        
        List<Object[]> results = taskRepository.getOperatorStatistics(
            filters.getStartDate(),
            filters.getEndDate()
        );
        
        List<OperatorPerformanceResponse.OperatorMetrics> operators = results.stream()
            .map(row -> {
                UUID operatorId = (UUID) row[0];
                String username = (String) row[1];
                Integer tasksResolved = ((Long) row[2]).intValue();
                Double avgResolutionTime = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                Integer tasksInProgress = ((Long) row[4]).intValue();
                Integer tasksReopened = ((Long) row[5]).intValue();
                LocalDateTime activeSince = (LocalDateTime) row[6];
                
                return new OperatorPerformanceResponse.OperatorMetrics(
                    operatorId,
                    username,
                    tasksResolved,
                    Math.round(avgResolutionTime * 100.0) / 100.0,
                    tasksInProgress,
                    tasksReopened,
                    activeSince
                );
            })
            .collect(Collectors.toList());
        
        // Apply pagination
        int page = filters.getPage();
        int size = filters.getSize();
        int totalOperators = operators.size();
        int totalPages = (int) Math.ceil((double) totalOperators / size);
        
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalOperators);
        
        List<OperatorPerformanceResponse.OperatorMetrics> paginatedOperators = 
            fromIndex < totalOperators ? operators.subList(fromIndex, toIndex) : new ArrayList<>();
        
        return new OperatorPerformanceResponse(
            paginatedOperators,
            totalOperators,
            page,
            totalPages
        );
    }
}
