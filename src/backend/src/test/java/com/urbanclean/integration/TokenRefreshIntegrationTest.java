package com.urbanclean.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanclean.dto.request.LoginRequest;
import com.urbanclean.dto.request.RefreshTokenRequest;
import com.urbanclean.dto.response.LoginResponse;
import com.urbanclean.dto.response.RefreshTokenResponse;
import com.urbanclean.entity.TokenBlacklist;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.repository.RefreshTokenRepository;
import com.urbanclean.repository.TokenBlacklistRepository;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for token refresh flow.
 * Tests the complete flow: login → refresh → verify tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TokenRefreshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    public void setup() {
        // Clean up
        tokenBlacklistRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    public void testLoginReturnsBothTokens() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andReturn();

        // Assert
        String responseBody = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);

        assertThat(loginResponse.getToken()).isNotEmpty();
        assertThat(loginResponse.getRefreshToken()).isNotEmpty();
        assertThat(loginResponse.getToken()).isNotEqualTo(loginResponse.getRefreshToken());

        // Verify refresh token stored in database
        assertThat(refreshTokenRepository.findByUserId(testUser.getId())).hasSize(1);
    }

    @Test
    public void testRefreshEndpointWithValidToken() throws Exception {
        // Arrange - Login first
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );

        String originalAccessToken = loginResponse.getToken();
        String originalRefreshToken = loginResponse.getRefreshToken();

        // Act - Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(originalRefreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists())
                .andReturn();

        // Assert
        String refreshResponseBody = refreshResult.getResponse().getContentAsString();
        RefreshTokenResponse refreshResponse = objectMapper.readValue(
                refreshResponseBody,
                RefreshTokenResponse.class
        );

        // Verify new tokens returned
        assertThat(refreshResponse.getAccessToken()).isNotEmpty();
        assertThat(refreshResponse.getRefreshToken()).isNotEmpty();

        // Verify new tokens are different from original
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(originalAccessToken);
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(originalRefreshToken);
    }

    @Test
    public void testOldRefreshTokenRevokedAfterRotation() throws Exception {
        // Arrange - Login first
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );

        String originalRefreshToken = loginResponse.getRefreshToken();

        // Act - Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(originalRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk());

        // Assert - Try to use old refresh token again (should fail)
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());

        // Verify old token is blacklisted
        assertThat(refreshTokenService.validateRefreshToken(originalRefreshToken))
                .isNotNull();
    }

    @Test
    public void testRefreshWithExpiredToken() throws Exception {
        // Arrange - Create expired refresh token
        String expiredToken = refreshTokenService.createRefreshToken(
                testUser.getId(),
                "test-fingerprint",
                "127.0.0.1",
                "Test User Agent"
        );

        // Manually expire the token
        refreshTokenRepository.findByUserId(testUser.getId()).forEach(token -> {
            token.setExpiresAt(LocalDateTime.now().minusDays(1));
            refreshTokenRepository.save(token);
        });

        // Act & Assert
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(expiredToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRefreshWithBlacklistedToken() throws Exception {
        // Arrange - Create and blacklist a refresh token
        String refreshToken = refreshTokenService.createRefreshToken(
                testUser.getId(),
                "test-fingerprint",
                "127.0.0.1",
                "Test User Agent"
        );

        // Blacklist the token
        refreshTokenService.revokeRefreshToken(
                refreshToken,
                TokenBlacklist.RevocationReason.LOGOUT.name()
        );

        // Act & Assert
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRefreshWithInvalidToken() throws Exception {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid-token-12345");

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testMultipleRefreshesCreateNewTokens() throws Exception {
        // Arrange - Login first
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );

        String refreshToken1 = loginResponse.getRefreshToken();

        // Act - First refresh
        RefreshTokenRequest refreshRequest1 = new RefreshTokenRequest();
        refreshRequest1.setRefreshToken(refreshToken1);

        MvcResult refreshResult1 = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest1)))
                .andExpect(status().isOk())
                .andReturn();

        RefreshTokenResponse refreshResponse1 = objectMapper.readValue(
                refreshResult1.getResponse().getContentAsString(),
                RefreshTokenResponse.class
        );

        String refreshToken2 = refreshResponse1.getRefreshToken();

        // Second refresh
        RefreshTokenRequest refreshRequest2 = new RefreshTokenRequest();
        refreshRequest2.setRefreshToken(refreshToken2);

        MvcResult refreshResult2 = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest2)))
                .andExpect(status().isOk())
                .andReturn();

        RefreshTokenResponse refreshResponse2 = objectMapper.readValue(
                refreshResult2.getResponse().getContentAsString(),
                RefreshTokenResponse.class
        );

        String refreshToken3 = refreshResponse2.getRefreshToken();

        // Assert - All tokens are different
        assertThat(refreshToken1).isNotEqualTo(refreshToken2);
        assertThat(refreshToken2).isNotEqualTo(refreshToken3);
        assertThat(refreshToken1).isNotEqualTo(refreshToken3);

        // Verify old tokens cannot be reused
        RefreshTokenRequest oldTokenRequest = new RefreshTokenRequest();
        oldTokenRequest.setRefreshToken(refreshToken1);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldTokenRequest)))
                .andExpect(status().isUnauthorized());
    }
}
