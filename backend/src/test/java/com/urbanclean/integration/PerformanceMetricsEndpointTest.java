package com.urbanclean.integration;

import com.urbanclean.entity.User;
import com.urbanclean.entity.UserRole;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Performance Metrics endpoints.
 * 
 * Tests:
 * - Aggregated metrics endpoint
 * - Response time percentiles endpoint
 * - Error rate endpoint
 * - Database connections endpoint
 * - Memory usage endpoint
 * - CPU usage endpoint
 * 
 * Task 5.7.2: Test performance metrics endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerformanceMetricsEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;

    @BeforeEach
    void setUp() {
        // Clean up
        userRepository.deleteAll();

        // Create admin user using builder
        User admin = User.builder()
                .username("admin")
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("Admin123!@#"))
                .role(UserRole.ROLE_ADMIN)
                .build();
        userRepository.save(admin);

        // Generate JWT token
        adminToken = jwtTokenProvider.generateToken(admin.getUsername(), admin.getId(), admin.getRole());
    }

    /**
     * Test GET /api/admin/metrics/performance
     * Verify response structure and all metrics are present
     */
    @Test
    void testAggregatedMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/performance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timeRange").exists())
                .andExpect(jsonPath("$.responseTime").exists())
                .andExpect(jsonPath("$.errorRate").exists())
                .andExpect(jsonPath("$.activeConnections").exists())
                .andExpect(jsonPath("$.memory").exists())
                .andExpect(jsonPath("$.cpu").exists());
    }

    /**
     * Test GET /api/admin/metrics/performance with time range parameter
     */
    @Test
    void testAggregatedMetricsWithTimeRange() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/performance")
                        .param("range", "24h")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeRange").value("24h"));
    }

    /**
     * Test GET /api/admin/metrics/response-time
     * Verify response time percentiles structure
     */
    @Test
    void testResponseTimeEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/response-time")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.p50").exists())
                .andExpect(jsonPath("$.p95").exists())
                .andExpect(jsonPath("$.p99").exists())
                .andExpect(jsonPath("$.max").exists())
                .andExpect(jsonPath("$.count").exists());
    }

    /**
     * Test GET /api/admin/metrics/error-rate
     * Verify error rate is a number
     */
    @Test
    void testErrorRateEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/error-rate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isNumber());
    }

    /**
     * Test GET /api/admin/metrics/connections
     * Verify active connections is a number
     */
    @Test
    void testConnectionsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/connections")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isNumber());
    }

    /**
     * Test GET /api/admin/metrics/memory
     * Verify memory usage structure
     */
    @Test
    void testMemoryEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/memory")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.used").exists())
                .andExpect(jsonPath("$.max").exists())
                .andExpect(jsonPath("$.percentage").exists());
    }

    /**
     * Test GET /api/admin/metrics/cpu
     * Verify CPU usage structure
     */
    @Test
    void testCPUEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/cpu")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.process").exists())
                .andExpect(jsonPath("$.system").exists());
    }

    /**
     * Test that metrics endpoints require authentication
     */
    @Test
    void testMetricsEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/performance"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that metrics endpoints require ADMIN role
     */
    @Test
    void testMetricsEndpointsRequireAdminRole() throws Exception {
        // Create non-admin user using builder
        User citizen = User.builder()
                .username("citizen")
                .email("citizen@test.com")
                .passwordHash(passwordEncoder.encode("Citizen123!@#"))
                .role(UserRole.ROLE_CIUDADANO)
                .build();
        userRepository.save(citizen);

        String citizenToken = jwtTokenProvider.generateToken(
                citizen.getUsername(), 
                citizen.getId(),
                citizen.getRole()
        );

        mockMvc.perform(get("/api/admin/metrics/performance")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    /**
     * Test metrics accuracy - verify values are reasonable
     */
    @Test
    void testMetricsAccuracy() throws Exception {
        mockMvc.perform(get("/api/admin/metrics/performance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseTime.p50").isNumber())
                .andExpect(jsonPath("$.responseTime.p95").isNumber())
                .andExpect(jsonPath("$.responseTime.p99").isNumber())
                .andExpect(jsonPath("$.errorRate").isNumber())
                .andExpect(jsonPath("$.activeConnections").isNumber())
                .andExpect(jsonPath("$.memory.percentage").isNumber())
                .andExpect(jsonPath("$.cpu.process").isNumber());
    }
}
