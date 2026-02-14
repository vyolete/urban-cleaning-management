package com.urbanclean.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for generating device fingerprints.
 * Device fingerprints are used to bind tokens to specific devices/browsers.
 */
@Slf4j
public class DeviceFingerprintUtil {

    /**
     * Generate a device fingerprint from HTTP request.
     * Combines User-Agent, Accept-Language, and IP address.
     * 
     * @param request HTTP request
     * @return Device fingerprint (SHA-256 hash)
     */
    public static String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String ipAddress = getClientIpAddress(request);
        
        // Combine components
        String fingerprintData = String.format("%s|%s|%s", 
                userAgent != null ? userAgent : "unknown",
                acceptLanguage != null ? acceptLanguage : "unknown",
                ipAddress != null ? ipAddress : "unknown"
        );
        
        // Hash the fingerprint
        return hashString(fingerprintData);
    }

    /**
     * Get client IP address from request.
     * Handles X-Forwarded-For header for proxied requests.
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Hash a string using SHA-256.
     * 
     * @param input Input string
     * @return Hashed string (hex)
     */
    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
