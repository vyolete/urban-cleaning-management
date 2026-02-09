package com.urbanclean.security;

import com.urbanclean.entity.User;
import com.urbanclean.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * JWT Authentication Filter that validates JWT tokens on each request
 * Includes token version validation to invalidate tokens after password reset
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Extract JWT from Authorization header
            String jwt = extractJwtFromRequest(request);

            // Validate token and set authentication
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);

                // Load user details
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Validate token against user details
                if (jwtTokenProvider.validateToken(jwt, userDetails)) {
                    // Validate token version to ensure token hasn't been invalidated
                    if (isTokenVersionValid(jwt, username)) {
                        // Create authentication token
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        // Set authentication in security context
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Set authentication for user: {}", username);
                    } else {
                        log.warn("Token version mismatch for user: {}. Token has been invalidated.", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Validate that the token version in JWT matches the current user's token version
     * This ensures tokens are invalidated after password reset
     */
    private boolean isTokenVersionValid(String jwt, String username) {
        try {
            // Extract token version from JWT
            Integer tokenVersionFromJwt = jwtTokenProvider.getTokenVersionFromToken(jwt);
            
            // If token doesn't have version claim (old tokens), treat as version 0
            if (tokenVersionFromJwt == null) {
                tokenVersionFromJwt = 0;
            }

            // Load user from database to get current token version
            Optional<User> userOptional = userRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                log.warn("User not found during token version validation: {}", username);
                return false;
            }

            User user = userOptional.get();
            Integer currentTokenVersion = user.getTokenVersion();
            
            // Token is valid only if versions match
            boolean isValid = tokenVersionFromJwt.equals(currentTokenVersion);
            
            if (!isValid) {
                log.debug("Token version mismatch for user {}: JWT version={}, Current version={}", 
                    username, tokenVersionFromJwt, currentTokenVersion);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error validating token version: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
