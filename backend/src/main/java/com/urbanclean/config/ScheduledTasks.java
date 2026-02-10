package com.urbanclean.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduled tasks
 */
@Configuration
@EnableScheduling
@Slf4j
public class ScheduledTasks {

    private final RateLimitingFilter rateLimitingFilter;

    public ScheduledTasks(@Autowired(required = false) RateLimitingFilter rateLimitingFilter) {
        this.rateLimitingFilter = rateLimitingFilter;
    }

    /**
     * Clean up expired rate limit entries every 10 minutes
     * Only runs if RateLimitingFilter is available (not in test profile)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    public void cleanupRateLimitEntries() {
        if (rateLimitingFilter != null) {
            log.debug("Running scheduled cleanup of rate limit entries");
            rateLimitingFilter.cleanupExpiredEntries();
        }
    }
}
