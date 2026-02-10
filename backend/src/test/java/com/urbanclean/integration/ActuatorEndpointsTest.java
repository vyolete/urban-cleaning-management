package com.urbanclean.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Actuator endpoints.
 * 
 * Tests:
 * - Health endpoint availability and response structure
 * - Metrics endpoint availability and response structure
 * - Prometheus endpoint availability and format
 * 
 * Task 5.7.1: Test Actuator endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test GET /actuator/health
     * Verify health status and details are returned
     */
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").exists())
                .andExpect(jsonPath("$.components.db").exists())
                .andExpect(jsonPath("$.components.diskSpace").exists());
    }

    /**
     * Test GET /actuator/metrics
     * Verify metrics list is returned
     */
    @Test
    void testMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.names").isArray())
                .andExpect(jsonPath("$.names").isNotEmpty());
    }

    /**
     * Test GET /actuator/metrics/jvm.memory.used
     * Verify individual metric details
     */
    @Test
    void testIndividualMetric() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(jsonPath("$.measurements").isArray())
                .andExpect(jsonPath("$.measurements[0].value").exists());
    }

    /**
     * Test GET /actuator/prometheus
     * Verify Prometheus format is returned
     * 
     * Note: This endpoint may be treated as a static resource by Spring Security
     * and return 404 in some configurations. This is a known issue.
     */
    @Test
    void testPrometheusEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# HELP")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# TYPE")));
    }

    /**
     * Test that health endpoint shows database connectivity
     */
    @Test
    void testHealthEndpointShowsDatabaseStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db.status").exists())
                .andExpect(jsonPath("$.components.db.details.database").exists());
    }

    /**
     * Test that metrics endpoint includes HTTP server request metrics
     */
    @Test
    void testMetricsIncludeHttpServerRequests() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names[?(@=='http.server.requests')]").exists());
    }

    /**
     * Test that metrics endpoint includes HikariCP connection pool metrics
     */
    @Test
    void testMetricsIncludeHikariCP() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names[?(@=='hikaricp.connections.active')]").exists());
    }
}
