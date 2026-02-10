package com.urbanclean.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanclean.dto.request.*;
import com.urbanclean.dto.response.*;
import com.urbanclean.entity.*;
import com.urbanclean.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive end-to-end integration test covering complete user flows.
 * 
 * Test Scenarios:
 * 1. Citizen Flow: Register → Login → Create report → Receive notifications
 * 2. Operator Flow: Login → View tasks → Assign task → Update state → Resolve task
 * 3. Admin Flow: Configure system → View analytics → Manage sessions
 * 
 * This test verifies that all modules work together correctly:
 * - Authentication & Authorization
 * - Report Management
 * - Task Management
 * - Notification System
 * - Session Management
 * - Analytics Dashboard
 * - Configuration Management
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Test data holders
    private static String citizenToken;
    private static String operatorToken;
    private static String adminToken;
    private static UUID reportId;
    private static UUID taskId;
    private static UUID citizenId;
    private static UUID operatorId;
    private static UUID adminId;

    @BeforeEach
    public void setup() {
        // Clean up database
        taskRepository.deleteAll();
        reportRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        userSessionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Test 1: Complete Citizen Flow
     * Register → Login → Create report → Verify notification preferences
     */
    @Test
    @Order(1)
    @DisplayName("Citizen Flow: Register, Login, Create Report")
    public void testCompleteCitizenFlow() throws Exception {
        // Step 1: Register as citizen
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("citizen_user");
        registerRequest.setEmail("citizen@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setRole(UserRole.ROLE_CIUDADANO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("citizen_user"))
                .andExpect(jsonPath("$.email").value("citizen@example.com"));

        // Verify user created
        User citizen = userRepository.findByUsername("citizen_user").orElseThrow();
        citizenId = citizen.getId();
        assertThat(citizen.getRole()).isEqualTo(UserRole.ROLE_CIUDADANO);

        // Step 2: Login as citizen
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("citizen_user");
        loginRequest.setPassword("SecurePass123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("citizen_user"))
                .andExpect(jsonPath("$.role").value("ROLE_CIUDADANO"))
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );
        citizenToken = loginResponse.getToken();

        // Verify session created
        List<UserSession> sessions = userSessionRepository.findByUserIdAndActiveTrue(citizenId);
        assertThat(sessions).hasSize(1);

        // Step 3: Check notification preferences (should be created with defaults)
        mockMvc.perform(get("/api/users/notifications/preferences")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportCreated").value(true))
                .andExpect(jsonPath("$.taskResolved").value(true));

        // Step 4: Create a report (using Madrid coordinates for geofencing)
        ReportSubmissionRequest reportRequest = new ReportSubmissionRequest();
        reportRequest.setCategory("BASURA_ACUMULADA");
        reportRequest.setDescription("Large pile of trash on the sidewalk near the park");
        reportRequest.setLatitude(40.4168);  // Madrid latitude
        reportRequest.setLongitude(-3.7038);  // Madrid longitude

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "test-photo.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(reportRequest)
        );

        MvcResult reportResult = mockMvc.perform(multipart("/api/reports")
                        .file(photo)
                        .file(data)
                        .header("Authorization", "Bearer " + citizenToken))
                .andDo(print())  // Print response for debugging
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.category").value("BASURA_ACUMULADA"))
                .andExpect(jsonPath("$.description").value("Large pile of trash on the sidewalk near the park"))
                .andReturn();

        ReportResponse reportResponse = objectMapper.readValue(
                reportResult.getResponse().getContentAsString(),
                ReportResponse.class
        );
        reportId = reportResponse.getId();

        // Verify report created in database
        Report report = reportRepository.findById(reportId).orElseThrow();
        assertThat(report.getCategory()).isEqualTo("BASURA_ACUMULADA");
        assertThat(report.getSubmitter().getId()).isEqualTo(citizenId);

        // Step 5: Verify citizen can view their reports
        mockMvc.perform(get("/api/reports/my")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId.toString()))
                .andExpect(jsonPath("$[0].category").value("BASURA_ACUMULADA"));
    }

    /**
     * Test 2: Complete Operator Flow
     * Login → View tasks → Assign task → Update state → Resolve task
     */
    @Test
    @Order(2)
    @DisplayName("Operator Flow: Login, View Tasks, Assign, Update, Resolve")
    public void testCompleteOperatorFlow() throws Exception {
        // Setup: Create operator user
        User operator = User.builder()
                .username("operator_user")
                .email("operator@example.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .role(UserRole.ROLE_TECNICO)
                .tokenVersion(0)
                .build();
        operator = userRepository.save(operator);
        operatorId = operator.getId();

        // Setup: Create a report and task
        User citizen = User.builder()
                .username("test_citizen")
                .email("test_citizen@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        citizen = userRepository.save(citizen);

        // Create point geometry for report location (Madrid coordinates)
        org.locationtech.jts.geom.GeometryFactory geometryFactory = new org.locationtech.jts.geom.GeometryFactory(new org.locationtech.jts.geom.PrecisionModel(), 4326);
        org.locationtech.jts.geom.Coordinate coordinate = new org.locationtech.jts.geom.Coordinate(-3.7038, 40.4168);  // Madrid
        org.locationtech.jts.geom.Point location = geometryFactory.createPoint(coordinate);

        Report report = new Report();
        report.setCategory("BASURA_ACUMULADA");
        report.setDescription("Test report for operator flow");
        report.setSubmitter(citizen);
        report.setLocation(location);
        report.setIsDuplicate(false);
        report.setCreatedAt(LocalDateTime.now());
        report = reportRepository.save(report);

        Task task = new Task();
        task.setPrimaryReport(report);
        task.setCategory("BASURA_ACUMULADA");
        task.setState(TaskState.PENDIENTE);
        task.setPriorityScore(java.math.BigDecimal.valueOf(75.5));
        task.setLocation(location);  // Set location from report
        task.setCreatedAt(LocalDateTime.now());
        task = taskRepository.save(task);
        taskId = task.getId();

        // Step 1: Login as operator
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("operator_user");
        loginRequest.setPassword("SecurePass123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_TECNICO"))
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );
        operatorToken = loginResponse.getToken();

        // Step 2: View available tasks
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$[0].state").value("PENDIENTE"));

        // Step 3: Assign task to self (PENDIENTE -> ASIGNADO)
        TaskStateUpdateRequest assignRequest = new TaskStateUpdateRequest();
        assignRequest.setNewState(TaskState.ASIGNADO);

        mockMvc.perform(patch("/api/tasks/" + taskId + "/state")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ASIGNADO"))
                .andExpect(jsonPath("$.assignedOperatorUsername").value("operator_user"));

        // Verify task assigned in database
        Task assignedTask = taskRepository.findById(taskId).orElseThrow();
        assertThat(assignedTask.getState()).isEqualTo(TaskState.ASIGNADO);
        assertThat(assignedTask.getAssignedOperator().getId()).isEqualTo(operatorId);

        // Step 4: Start working on task (ASIGNADO -> EN_PROGRESO)
        TaskStateUpdateRequest startRequest = new TaskStateUpdateRequest();
        startRequest.setNewState(TaskState.EN_PROGRESO);

        mockMvc.perform(patch("/api/tasks/" + taskId + "/state")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EN_PROGRESO"));

        // Verify task in progress
        Task inProgressTask = taskRepository.findById(taskId).orElseThrow();
        assertThat(inProgressTask.getState()).isEqualTo(TaskState.EN_PROGRESO);

        // Step 5: Update task state to resolved (EN_PROGRESO -> RESUELTO)
        TaskStateUpdateRequest resolveRequest = new TaskStateUpdateRequest();
        resolveRequest.setNewState(TaskState.RESUELTO);

        mockMvc.perform(patch("/api/tasks/" + taskId + "/state")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("RESUELTO"))
                .andExpect(jsonPath("$.resolvedAt").exists());

        // Verify task resolved in database
        Task resolvedTask = taskRepository.findById(taskId).orElseThrow();
        assertThat(resolvedTask.getState()).isEqualTo(TaskState.RESUELTO);
        assertThat(resolvedTask.getResolvedAt()).isNotNull();

        // Step 6: View task history/audit log
        mockMvc.perform(get("/api/tasks/" + taskId + "/audit-history")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Test 3: Complete Admin Flow
     * Login → Configure system → View analytics → Manage sessions
     */
    @Test
    @Order(3)
    @DisplayName("Admin Flow: Login, Configure System, View Analytics, Manage Sessions")
    public void testCompleteAdminFlow() throws Exception {
        // Setup: Create admin user
        User admin = User.builder()
                .username("admin_user")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .role(UserRole.ROLE_ADMIN)
                .tokenVersion(0)
                .build();
        admin = userRepository.save(admin);
        adminId = admin.getId();

        // Step 1: Login as admin
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin_user");
        loginRequest.setPassword("SecurePass123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );
        adminToken = loginResponse.getToken();

        // Step 2: View current algorithm configuration
        mockMvc.perform(get("/api/admin/config/algorithm-weights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightCategory").exists())
                .andExpect(jsonPath("$.weightZone").exists())
                .andExpect(jsonPath("$.weightTime").exists());

        // Step 3: Update algorithm weights
        AlgorithmWeightsRequest weightsRequest = new AlgorithmWeightsRequest();
        weightsRequest.setWeightCategory(java.math.BigDecimal.valueOf(0.5));
        weightsRequest.setWeightZone(java.math.BigDecimal.valueOf(0.3));
        weightsRequest.setWeightTime(java.math.BigDecimal.valueOf(0.2));
        weightsRequest.setDeduplicationDistanceMeters(java.math.BigDecimal.valueOf(50.0));
        weightsRequest.setDeduplicationTimeWindowHours(24);

        mockMvc.perform(put("/api/admin/config/algorithm-weights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weightsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightCategory").value(0.5))
                .andExpect(jsonPath("$.weightZone").value(0.3))
                .andExpect(jsonPath("$.weightTime").value(0.2));

        // Step 4: View analytics (requires test data)
        // Create some test data first
        setupAnalyticsTestData();

        // View task distribution by category
        mockMvc.perform(get("/api/analytics/tasks/distribution/category")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", LocalDateTime.now().minusDays(30).toString())
                        .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribution").isArray())
                .andExpect(jsonPath("$.totalTasks").exists());

        // View task distribution by state
        mockMvc.perform(get("/api/analytics/tasks/distribution/state")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", LocalDateTime.now().minusDays(30).toString())
                        .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribution").isArray());

        // Step 5: View active sessions
        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].active").value(true));

        // Step 6: View performance metrics
        mockMvc.perform(get("/api/admin/metrics/performance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeRange").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.requestCount").exists())
                .andExpect(jsonPath("$.averageResponseTime").exists());
    }

    /**
     * Test 4: Token Refresh Flow
     * Login → Wait → Refresh token → Verify new tokens
     */
    @Test
    @Order(4)
    @DisplayName("Token Refresh Flow: Login, Refresh, Verify")
    public void testTokenRefreshFlow() throws Exception {
        // Setup: Create test user
        User user = User.builder()
                .username("refresh_test_user")
                .email("refresh@example.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        user = userRepository.save(user);

        // Step 1: Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("refresh_test_user");
        loginRequest.setPassword("SecurePass123!");

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

        // Wait 1 second to ensure different timestamp in JWT
        Thread.sleep(1000);

        // Step 2: Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(originalRefreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        RefreshTokenResponse refreshResponse = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                RefreshTokenResponse.class
        );

        // Step 3: Verify new tokens are different
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(originalAccessToken);
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(originalRefreshToken);

        // Step 4: Verify old refresh token cannot be reused
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());

        // Step 5: Verify new access token works
        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + refreshResponse.getAccessToken()))
                .andExpect(status().isOk());
    }

    /**
     * Test 5: Multi-Device Session Management
     * Login from multiple devices → View sessions → Revoke session → Logout all
     */
    @Test
    @Order(5)
    @DisplayName("Multi-Device Session Management")
    public void testMultiDeviceSessionManagement() throws Exception {
        // Setup: Create test user
        User user = User.builder()
                .username("multidevice_user")
                .email("multidevice@example.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        user = userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("multidevice_user");
        loginRequest.setPassword("SecurePass123!");

        // Step 1: Login from desktop
        MvcResult desktopLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse desktopResponse = objectMapper.readValue(
                desktopLogin.getResponse().getContentAsString(),
                LoginResponse.class
        );

        // Step 2: Login from mobile
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0) Safari/604.1")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Step 3: Login from tablet
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (iPad; CPU OS 16_0) Safari/604.1")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Step 4: View all sessions
        MvcResult sessionsResult = mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + desktopResponse.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        UserSessionResponse[] sessions = objectMapper.readValue(
                sessionsResult.getResponse().getContentAsString(),
                UserSessionResponse[].class
        );

        // Step 5: Revoke mobile session
        UUID mobileSessionId = sessions[1].getId();
        mockMvc.perform(delete("/api/sessions/" + mobileSessionId)
                        .header("Authorization", "Bearer " + desktopResponse.getToken()))
                .andExpect(status().isOk());

        // Verify only 2 active sessions remain
        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + desktopResponse.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Step 6: Logout all sessions
        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", "Bearer " + desktopResponse.getToken()))
                .andExpect(status().isOk());

        // Verify no active sessions remain
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndActiveTrue(user.getId());
        assertThat(activeSessions).isEmpty();
    }

    /**
     * Test 6: Notification Preferences Management
     * Login → View preferences → Update preferences → Verify changes
     */
    @Test
    @Order(6)
    @DisplayName("Notification Preferences Management")
    public void testNotificationPreferencesManagement() throws Exception {
        // Setup: Create test user
        User user = User.builder()
                .username("notification_user")
                .email("notification@example.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        user = userRepository.save(user);

        // Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("notification_user");
        loginRequest.setPassword("SecurePass123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );

        // Step 1: View default preferences
        mockMvc.perform(get("/api/users/notifications/preferences")
                        .header("Authorization", "Bearer " + loginResponse.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskAssigned").value(true))
                .andExpect(jsonPath("$.taskResolved").value(true))
                .andExpect(jsonPath("$.taskReopened").value(true))
                .andExpect(jsonPath("$.reportCreated").value(true));

        // Step 2: Update preferences (disable some notifications)
        NotificationPreferenceRequest updateRequest = new NotificationPreferenceRequest();
        updateRequest.setTaskAssigned(true);
        updateRequest.setTaskResolved(false);
        updateRequest.setTaskReopened(false);
        updateRequest.setReportCreated(true);

        mockMvc.perform(put("/api/users/notifications/preferences")
                        .header("Authorization", "Bearer " + loginResponse.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskAssigned").value(true))
                .andExpect(jsonPath("$.taskResolved").value(false))
                .andExpect(jsonPath("$.taskReopened").value(false))
                .andExpect(jsonPath("$.reportCreated").value(true));

        // Step 3: Verify preferences persisted
        mockMvc.perform(get("/api/users/notifications/preferences")
                        .header("Authorization", "Bearer " + loginResponse.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskResolved").value(false))
                .andExpect(jsonPath("$.taskReopened").value(false));
    }

    /**
     * Helper method to setup test data for analytics
     */
    private void setupAnalyticsTestData() {
        // Create test users
        User citizen = User.builder()
                .username("analytics_citizen")
                .email("analytics_citizen@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.ROLE_CIUDADANO)
                .tokenVersion(0)
                .build();
        citizen = userRepository.save(citizen);

        User operator = User.builder()
                .username("analytics_operator")
                .email("analytics_operator@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.ROLE_TECNICO)
                .tokenVersion(0)
                .build();
        operator = userRepository.save(operator);

        // Create geometry factory for locations (Madrid coordinates)
        org.locationtech.jts.geom.GeometryFactory geometryFactory = new org.locationtech.jts.geom.GeometryFactory(
            new org.locationtech.jts.geom.PrecisionModel(), 4326);

        // Create test reports and tasks
        for (int i = 0; i < 5; i++) {
            // Create location for each report (Madrid area)
            org.locationtech.jts.geom.Coordinate coordinate = new org.locationtech.jts.geom.Coordinate(
                -3.7038 + (i * 0.001), 40.4168 + (i * 0.001));
            org.locationtech.jts.geom.Point location = geometryFactory.createPoint(coordinate);

            Report report = new Report();
            report.setCategory(i % 2 == 0 ? "BASURA_ACUMULADA" : "BACHE");
            report.setDescription("Test report " + i);
            report.setSubmitter(citizen);
            report.setLocation(location);
            report.setIsDuplicate(false);
            report.setCreatedAt(LocalDateTime.now().minusDays(i));
            report = reportRepository.save(report);

            Task task = new Task();
            task.setPrimaryReport(report);
            task.setCategory(report.getCategory());
            task.setState(i < 3 ? TaskState.RESUELTO : TaskState.PENDIENTE);
            task.setPriorityScore(java.math.BigDecimal.valueOf(50 + i * 10));
            task.setLocation(location);  // Set location from report
            task.setCreatedAt(LocalDateTime.now().minusDays(i));
            task.setAssignedOperator(operator);
            
            if (i < 3) {
                task.setResolvedAt(LocalDateTime.now().minusDays(i).plusHours(2));
            }
            
            taskRepository.save(task);
        }
    }

    @AfterEach
    public void cleanup() {
        // Cleanup is handled by @Transactional
    }
}
