package com.urbanclean.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanclean.dto.request.LoginRequest;
import com.urbanclean.dto.response.LoginResponse;
import com.urbanclean.dto.response.UserSessionResponse;
import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.entity.UserSession;
import com.urbanclean.repository.RefreshTokenRepository;
import com.urbanclean.repository.TokenBlacklistRepository;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.repository.UserSessionRepository;
import com.urbanclean.service.RefreshTokenService;
import com.urbanclean.service.UserSessionService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for session management.
 * Tests multi-device sessions, session limits, and session revocation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SessionManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    public void setup() {
        // Clean up
        userSessionRepository.deleteAll();
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
    public void testCreatingMultipleSessions() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // Act - Create 3 sessions with different user agents
        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) Safari/604.1",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1.15"
        };

        for (String userAgent : userAgents) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("User-Agent", userAgent)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());
        }

        // Assert
        List<UserSession> sessions = userSessionRepository.findByUserIdAndActiveTrue(testUser.getId());
        assertThat(sessions).hasSize(3);

        // Verify different device types detected
        assertThat(sessions.stream().map(UserSession::getDeviceType).distinct().count())
                .isGreaterThan(1);
    }

    @Test
    public void testSessionLimitEnforcement() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // Act - Create 6 sessions (limit is 5)
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("User-Agent", "Device-" + i)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());

            // Small delay to ensure different timestamps
            Thread.sleep(10);
        }

        // Assert - Only 5 active sessions should remain
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndActiveTrue(testUser.getId());
        assertThat(activeSessions).hasSize(5);

        // Verify oldest session was revoked
        List<UserSession> allSessions = userSessionRepository.findByUserIdOrderByLastActivityDesc(testUser.getId());
        assertThat(allSessions).hasSize(6);
        assertThat(allSessions.stream().filter(s -> !s.getActive()).count()).isEqualTo(1);
    }

    @Test
    public void testGetActiveSessions() throws Exception {
        // Arrange - Create 2 sessions
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse1 = objectMapper.readValue(
                loginResult1.getResponse().getContentAsString(),
                LoginResponse.class
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (iPhone) Safari/604.1")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Act - Get active sessions
        MvcResult result = mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + loginResponse1.getToken()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String responseBody = result.getResponse().getContentAsString();
        UserSessionResponse[] sessions = objectMapper.readValue(responseBody, UserSessionResponse[].class);

        assertThat(sessions).hasSize(2);
        assertThat(sessions[0].getActive()).isTrue();
        assertThat(sessions[1].getActive()).isTrue();
    }

    @Test
    public void testRevokeSpecificSession() throws Exception {
        // Arrange - Create 2 sessions
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse1 = objectMapper.readValue(
                loginResult1.getResponse().getContentAsString(),
                LoginResponse.class
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Get sessions
        List<UserSession> sessions = userSessionRepository.findByUserIdAndActiveTrue(testUser.getId());
        assertThat(sessions).hasSize(2);

        UUID sessionToRevoke = sessions.get(1).getId();

        // Act - Revoke specific session
        mockMvc.perform(delete("/api/sessions/" + sessionToRevoke)
                        .header("Authorization", "Bearer " + loginResponse1.getToken()))
                .andExpect(status().isOk());

        // Assert
        List<UserSession> remainingSessions = userSessionRepository.findByUserIdAndActiveTrue(testUser.getId());
        assertThat(remainingSessions).hasSize(1);

        UserSession revokedSession = userSessionRepository.findById(sessionToRevoke).orElseThrow();
        assertThat(revokedSession.getActive()).isFalse();
    }

    @Test
    public void testRevokeOtherSessions() throws Exception {
        // Arrange - Create 3 sessions
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse1 = objectMapper.readValue(
                loginResult1.getResponse().getContentAsString(),
                LoginResponse.class
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Verify 3 sessions created
        assertThat(userSessionRepository.findByUserIdAndActiveTrue(testUser.getId())).hasSize(3);

        // Act - Revoke all sessions except current
        mockMvc.perform(post("/api/sessions/revoke-others")
                        .header("Authorization", "Bearer " + loginResponse1.getToken()))
                .andExpect(status().isOk());

        // Assert - Only 1 session should remain active
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndActiveTrue(testUser.getId());
        assertThat(activeSessions).hasSize(1);
    }

    @Test
    public void testLogoutAllSessions() throws Exception {
        // Arrange - Create 3 sessions
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse1 = objectMapper.readValue(
                loginResult1.getResponse().getContentAsString(),
                LoginResponse.class
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Verify 3 sessions created
        assertThat(userSessionRepository.findByUserIdAndActiveTrue(testUser.getId())).hasSize(3);

        // Act - Logout all sessions
        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", "Bearer " + loginResponse1.getToken()))
                .andExpect(status().isOk());

        // Assert - No active sessions should remain
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndActiveTrue(testUser.getId());
        assertThat(activeSessions).isEmpty();

        // Verify all refresh tokens revoked
        List<com.urbanclean.entity.RefreshToken> refreshTokens = 
                refreshTokenRepository.findByUserId(testUser.getId());
        assertThat(refreshTokens.stream().allMatch(com.urbanclean.entity.RefreshToken::getRevoked))
                .isTrue();
    }

    @Test
    public void testTokensBlacklistedAfterLogout() throws Exception {
        // Arrange - Login
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

        String accessToken = loginResponse.getToken();
        String refreshToken = loginResponse.getRefreshToken();

        // Act - Logout
        Map<String, String> logoutBody = new HashMap<>();
        logoutBody.put("refreshToken", refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutBody)))
                .andExpect(status().isOk());

        // Assert - Tokens should be blacklisted
        assertThat(tokenBlacklistRepository.count()).isGreaterThan(0);

        // Try to use access token (should fail)
        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCannotRevokeOtherUserSession() throws Exception {
        // Arrange - Create two users
        User user2 = User.builder()
                .username("testuser2")
                .email("test2@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        user2 = userRepository.save(user2);

        // Login as user1
        LoginRequest loginRequest1 = new LoginRequest();
        loginRequest1.setUsername("testuser");
        loginRequest1.setPassword("password123");

        MvcResult loginResult1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest1)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse1 = objectMapper.readValue(
                loginResult1.getResponse().getContentAsString(),
                LoginResponse.class
        );

        // Login as user2
        LoginRequest loginRequest2 = new LoginRequest();
        loginRequest2.setUsername("testuser2");
        loginRequest2.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest2)))
                .andExpect(status().isOk());

        // Get user2's session
        List<UserSession> user2Sessions = userSessionRepository.findByUserIdAndActiveTrue(user2.getId());
        assertThat(user2Sessions).hasSize(1);
        UUID user2SessionId = user2Sessions.get(0).getId();

        // Act & Assert - User1 tries to revoke user2's session (should fail)
        mockMvc.perform(delete("/api/sessions/" + user2SessionId)
                        .header("Authorization", "Bearer " + loginResponse1.getToken()))
                .andExpect(status().isBadRequest());

        // Verify user2's session still active
        UserSession user2Session = userSessionRepository.findById(user2SessionId).orElseThrow();
        assertThat(user2Session.getActive()).isTrue();
    }
}
