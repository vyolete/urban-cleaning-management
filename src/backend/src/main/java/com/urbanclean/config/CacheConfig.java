package com.urbanclean.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for analytics queries
 * Uses in-memory caching with TTL to reduce database load
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            // Analytics caches
            "taskDistribution",
            "mttr",
            "heatmap",
            "operatorMetrics",
            // Config service caches
            "tokenExpirationConfig",
            "duplicateDetectionConfig",
            "algorithmConfig"
        );
    }
}
