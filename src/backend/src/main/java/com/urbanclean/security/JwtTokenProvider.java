package com.urbanclean.security;

import com.urbanclean.dto.response.TokenExpirationResponse;
import com.urbanclean.entity.UserRole;
import com.urbanclean.service.ConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Token Provider for generating and validating JWT tokens
 * Now supports dynamic token expiration from configuration
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long defaultExpiration;

    private final ConfigService configService;

    // Cache for token expiration config to avoid repeated database queries
    private TokenExpirationResponse cachedConfig;
    private long lastConfigFetch = 0;
    private static final long CONFIG_CACHE_TTL = 60000; // 1 minute

    public JwtTokenProvider(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Get token expiration configuration with caching
     */
    private TokenExpirationResponse getTokenExpirationConfig() {
        long now = System.currentTimeMillis();
        
        // Return cached config if still valid
        if (cachedConfig != null && (now - lastConfigFetch) < CONFIG_CACHE_TTL) {
            return cachedConfig;
        }
        
        // Fetch new config
        try {
            cachedConfig = configService.getTokenExpirationConfig();
            lastConfigFetch = now;
            log.debug("Fetched token expiration config: access={}min, refresh={}days",
                    cachedConfig.getAccessTokenExpirationMinutes(),
                    cachedConfig.getRefreshTokenExpirationDays());
            return cachedConfig;
        } catch (Exception e) {
            log.warn("Failed to fetch token expiration config, using defaults", e);
            // Return default config if fetch fails
            return TokenExpirationResponse.builder()
                    .accessTokenExpirationMinutes(15)
                    .refreshTokenExpirationDays(7)
                    .build();
        }
    }

    /**
     * Get access token expiration in milliseconds
     */
    private long getAccessTokenExpiration() {
        TokenExpirationResponse config = getTokenExpirationConfig();
        return config.getAccessTokenExpirationMinutes() * 60 * 1000L;
    }

    /**
     * Generate JWT token with user identity, role claims, and token version
     */
    public String generateToken(String username, UUID userId, UserRole role, Integer tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("role", role.name());
        claims.put("tokenVersion", tokenVersion);
        return createToken(claims, username);
    }

    /**
     * Generate JWT token with user identity and role claims (backward compatibility)
     */
    public String generateToken(String username, UUID userId, UserRole role) {
        return generateToken(username, userId, role, 0);
    }

    /**
     * Generate token from UserDetails
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Extract role from authorities
        String role = userDetails.getAuthorities().stream()
            .findFirst()
            .map(auth -> auth.getAuthority())
            .orElse("ROLE_CIUDADANO");
        claims.put("role", role);
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Create token with claims and subject
     * Now uses dynamic expiration from configuration
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        long expirationMs = getAccessTokenExpiration();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        log.debug("Creating token for user: {}, expiration: {}ms", subject, expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public UUID getUserIdFromToken(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Extract role from token
     */
    public UserRole getRoleFromToken(String token) {
        String roleStr = extractClaim(token, claims -> claims.get("role", String.class));
        return roleStr != null ? UserRole.valueOf(roleStr) : null;
    }

    /**
     * Extract token version from token
     */
    public Integer getTokenVersionFromToken(String token) {
        return extractClaim(token, claims -> claims.get("tokenVersion", Integer.class));
    }

    /**
     * Extract expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return getExpirationDateFromToken(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validate token against user details
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate token signature and expiration
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get signing key from secret
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
