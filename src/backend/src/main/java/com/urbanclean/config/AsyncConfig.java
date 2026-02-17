package com.urbanclean.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for async processing, retry logic, and scheduled tasks
 */
@Configuration
@EnableAsync
@EnableRetry
@EnableScheduling
public class AsyncConfig {
    // Spring Boot auto-configures ThreadPoolTaskExecutor based on application.properties
    // No additional configuration needed for basic async support
}
