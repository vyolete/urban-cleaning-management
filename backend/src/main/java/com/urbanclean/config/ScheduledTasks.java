package com.urbanclean.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduled tasks
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final RateLimitingFilter rateLimitingFilter;

    /**
     * Clean up expired rate limit entries every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    public void cleanupRateLimitEntries() {
        log.debug("Running scheduled cleanup of rate limit entries");
        rateLimitingFilter.cleanupExpiredEntries();
    }
}
