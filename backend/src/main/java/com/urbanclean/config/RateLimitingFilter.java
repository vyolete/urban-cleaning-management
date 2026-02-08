package com.urbanclean.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanclean.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter for rate limiting authentication endpoints
 * Prevents brute force attacks by limiting requests per IP address
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    // Maximum requests per time window
    private static final int MAX_REQUESTS = 5;
    
    // Time window in milliseconds (5 minutes)
    private static final long TIME_WINDOW_MS = 5 * 60 * 1000;
    
    // Store request counts per IP address
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    
    // Object mapper for JSON responses
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        
        // Only apply rate limiting to authentication endpoints
        if (!isAuthenticationEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIP = getClientIP(request);
        
        // Check if rate limit is exceeded
        if (isRateLimitExceeded(clientIP)) {
            log.warn("Rate limit exceeded for IP: {}", clientIP);
            sendRateLimitError(response, request);
            return;
        }

        // Increment request count
        incrementRequestCount(clientIP);
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request URI is an authentication endpoint
     */
    private boolean isAuthenticationEndpoint(String uri) {
        return uri.startsWith("/api/auth/");
    }

    /**
     * Get client IP address from request
     * Handles X-Forwarded-For header for proxied requests
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Check if rate limit is exceeded for the given IP
     */
    private boolean isRateLimitExceeded(String clientIP) {
        RequestCounter counter = requestCounts.get(clientIP);
        
        if (counter == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceFirstRequest = currentTime - counter.getFirstRequestTime();

        // If time window has passed, reset counter
        if (timeSinceFirstRequest > TIME_WINDOW_MS) {
            requestCounts.remove(clientIP);
            return false;
        }

        // Check if max requests exceeded
        return counter.getCount() >= MAX_REQUESTS;
    }

    /**
     * Increment request count for the given IP
     */
    private void incrementRequestCount(String clientIP) {
        long currentTime = System.currentTimeMillis();
        
        requestCounts.compute(clientIP, (key, counter) -> {
            if (counter == null) {
                return new RequestCounter(currentTime);
            }
            
            long timeSinceFirstRequest = currentTime - counter.getFirstRequestTime();
            
            // If time window has passed, create new counter
            if (timeSinceFirstRequest > TIME_WINDOW_MS) {
                return new RequestCounter(currentTime);
            }
            
            // Increment existing counter
            counter.increment();
            return counter;
        });
    }

    /**
     * Send rate limit error response
     */
    private void sendRateLimitError(HttpServletResponse response, HttpServletRequest request) 
            throws IOException {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("RATE_LIMIT_EXCEEDED")
                .message("Too many requests. Please try again later.")
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Clean up expired entries periodically
     * This method should be called by a scheduled task
     */
    public void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> {
            long timeSinceFirstRequest = currentTime - entry.getValue().getFirstRequestTime();
            return timeSinceFirstRequest > TIME_WINDOW_MS;
        });
        log.debug("Cleaned up expired rate limit entries. Current size: {}", requestCounts.size());
    }

    /**
     * Inner class to track request counts per IP
     */
    private static class RequestCounter {
        private final long firstRequestTime;
        private final AtomicInteger count;

        public RequestCounter(long firstRequestTime) {
            this.firstRequestTime = firstRequestTime;
            this.count = new AtomicInteger(1);
        }

        public long getFirstRequestTime() {
            return firstRequestTime;
        }

        public int getCount() {
            return count.get();
        }

        public void increment() {
            count.incrementAndGet();
        }
    }
}
