package com.urbanclean.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) Configuration
 * Allows frontend applications to communicate with the backend API
 * 
 * In production, only HTTPS origins should be allowed
 */
@Configuration
@Slf4j
public class CorsConfiguration {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${cors.max-age}")
    private long maxAge;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    /**
     * Configure CORS for Spring MVC
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                List<String> origins = parseOrigins(allowedOrigins);
                List<String> methods = Arrays.asList(allowedMethods.split(","));
                
                log.info("Configuring CORS:");
                log.info("  Allowed Origins: {}", origins);
                log.info("  Allowed Methods: {}", methods);
                log.info("  Allow Credentials: {}", allowCredentials);
                log.info("  Max Age: {} seconds", maxAge);
                log.info("  SSL Enabled: {}", sslEnabled);
                
                // Validate HTTPS in production
                if (sslEnabled) {
                    origins = validateHttpsOrigins(origins);
                }
                
                registry.addMapping("/api/**")
                        .allowedOrigins(origins.toArray(new String[0]))
                        .allowedMethods(methods.toArray(new String[0]))
                        .allowedHeaders(allowedHeaders.equals("*") ? "*" : allowedHeaders.split(","))
                        .allowCredentials(allowCredentials)
                        .maxAge(maxAge);
            }
        };
    }

    /**
     * Configure CORS for Spring Security
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        List<String> origins = parseOrigins(allowedOrigins);
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        
        // Validate HTTPS in production
        if (sslEnabled) {
            origins = validateHttpsOrigins(origins);
        }
        
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(methods);
        
        if (allowedHeaders.equals("*")) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }
        
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }

    /**
     * Parse comma-separated origins
     */
    private List<String> parseOrigins(String originsString) {
        return Arrays.asList(originsString.split(","));
    }

    /**
     * Validate that origins use HTTPS in production
     * Filters out HTTP origins when SSL is enabled
     */
    private List<String> validateHttpsOrigins(List<String> origins) {
        List<String> httpsOrigins = origins.stream()
                .filter(origin -> {
                    if (origin.startsWith("http://") && !origin.contains("localhost")) {
                        log.warn("Filtering out non-HTTPS origin in production: {}", origin);
                        return false;
                    }
                    return true;
                })
                .toList();
        
        if (httpsOrigins.isEmpty()) {
            log.error("No valid HTTPS origins configured! CORS will not work properly.");
        }
        
        return httpsOrigins;
    }
}
