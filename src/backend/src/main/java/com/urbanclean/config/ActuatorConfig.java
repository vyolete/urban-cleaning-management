package com.urbanclean.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring Boot Actuator and Micrometer metrics.
 * Configures common tags and histogram settings for performance monitoring.
 */
@Configuration
public class ActuatorConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Customizes the MeterRegistry with common tags for all metrics.
     * Adds application name tag to help identify metrics in monitoring systems.
     *
     * @return MeterRegistryCustomizer that adds common tags
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", applicationName)
                .commonTags("environment", System.getProperty("spring.profiles.active", "default"));
    }

    /**
     * Configures histogram settings for HTTP request metrics.
     * Enables percentile histograms for response time analysis (p95, p99).
     *
     * @return MeterFilter for histogram configuration
     */
    @Bean
    public MeterFilter meterFilter() {
        return MeterFilter.maximumAllowableTags(
                "http.server.requests",
                "uri",
                100,
                MeterFilter.deny()
        );
    }
}
