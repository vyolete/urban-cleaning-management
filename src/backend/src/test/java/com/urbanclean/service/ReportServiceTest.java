package com.urbanclean.service;

import com.urbanclean.dto.request.ReportSubmissionRequest;
import com.urbanclean.dto.response.ReportResponse;
import com.urbanclean.entity.Country;
import com.urbanclean.entity.Report;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.User;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.ReportRepository;
import com.urbanclean.repository.TaskRepository;
import com.urbanclean.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportService
 * Tests report creation with country context, filtering by country, administrative area, and municipality
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Unit Tests")
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GeofencingService geofencingService;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private MultipartFile photo;

    @InjectMocks
    private ReportService reportService;

    private GeometryFactory geometryFactory;
    private Country testCountrySpain;
    private Country testCountryColombia;
    private User testUser;
    private ReportSubmissionRequest testRequest;
    private Report testReport;
    private Point testLocation;
    private UUID testCountryId;
    private UUID testReportId;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();
        testCountryId = UUID.randomUUID();
        testReportId = UUID.randomUUID();

        // Setup test country (Spain)
        testCountrySpain = Country.builder()
                .id(testCountryId)
                .name("España")
                .code("ESP")
                .defaultCountry(true)
                .enabled(true)
                .minLat(new BigDecimal("36.0"))
                .maxLat(new BigDecimal("43.8"))
                .minLon(new BigDecimal("-9.3"))
                .maxLon(new BigDecimal("3.3"))
                .administrativeArea("Comunidad de Madrid")
                .municipality("Madrid")
                .centerLat(new BigDecimal("40.4168"))
                .centerLon(new BigDecimal("-3.7038"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup test country (Colombia)
        testCountryColombia = Country.builder()
                .id(UUID.randomUUID())
                .name("Colombia")
                .code("COL")
                .defaultCountry(false)
                .enabled(true)
                .minLat(new BigDecimal("-4.2"))
                .maxLat(new BigDecimal("12.5"))
                .minLon(new BigDecimal("-79.0"))
                .maxLon(new BigDecimal("-66.9"))
                .administrativeArea("Cundinamarca")
                .municipality("Bogotá")
                .centerLat(new BigDecimal("4.7110"))
                .centerLon(new BigDecimal("-74.0721"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();

        // Setup test location
        testLocation = geometryFactory.createPoint(new Coordinate(-3.7038, 40.4168));

        // Setup test request
        testRequest = ReportSubmissionRequest.builder()
                .latitude(40.4168)
                .longitude(-3.7038)
                .category("BASURA_ACUMULADA")
                .description("Test report description")
                .countryId(testCountryId)
                .build();

        // Setup test report
        testReport = Report.builder()
                .id(testReportId)
                .submitter(testUser)
                .location(testLocation)
                .category("BASURA_ACUMULADA")
                .description("Test report description")
                .photoUrl("/uploads/test-photo.jpg")
                .isDuplicate(false)
                .country(testCountrySpain)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========================================================================
    // CREATE REPORT WITH COUNTRY CONTEXT TESTS
    // ========================================================================

    @Test
    @DisplayName("Should create report with country context successfully")
    void shouldCreateReportWithCountryContextSuccessfully() {
        // Given
        setupAuthenticatedUser();
        when(fileStorageService.storeFile(photo)).thenReturn("/uploads/test-photo.jpg");
        when(geofencingService.getCountryById(testCountryId)).thenReturn(testCountrySpain);
        when(geofencingService.createPoint(anyDouble(), anyDouble())).thenReturn(testLocation);
        when(deduplicationService.checkForDuplicatesBeforeSave(any(Report.class))).thenReturn(Optional.empty());
        when(reportRepository.save(any(Report.class))).thenReturn(testReport);
        doNothing().when(geofencingService).validateCoordinates(anyDouble(), anyDouble(), any(UUID.class));

        // When
        Report result = reportService.createReport(testRequest, photo);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCountry()).isEqualTo(testCountrySpain);
        assertThat(result.getCountry().getId()).isEqualTo(testCountryId);
        assertThat(result.getIsDuplicate()).isFalse();

        verify(geofencingService).validateCoordinates(40.4168, -3.7038, testCountryId);
        verify(geofencingService).getCountryById(testCountryId);
        verify(fileStorageService).storeFile(photo);
        verify(reportRepository).save(any(Report.class));
        verify(taskService).createTask(any(Report.class));
    }

    @Test
    @DisplayName("Should create report without country when countryId is null")
    void shouldCreateReportWithoutCountryWhenCountryIdIsNull() {
        // Given
        setupAuthenticatedUser();
        testRequest.setCountryId(null);
        when(fileStorageService.storeFile(photo)).thenReturn("/uploads/test-photo.jpg");
        when(geofencingService.createPoint(anyDouble(), anyDouble())).thenReturn(testLocation);
        when(deduplicationService.checkForDuplicatesBeforeSave(any(Report.class))).thenReturn(Optional.empty());
        when(reportRepository.save(any(Report.class))).thenReturn(testReport);
        doNothing().when(geofencingService).validateCoordinates(anyDouble(), anyDouble(), any());

        // When
        Report result = reportService.createReport(testRequest, photo);

        // Then
        assertThat(result).isNotNull();
        verify(geofencingService).validateCoordinates(40.4168, -3.7038, null);
        verify(geofencingService, never()).getCountryById(any());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("Should throw exception when coordinates outside country boundaries")
    void shouldThrowExceptionWhenCoordinatesOutsideCountryBoundaries() {
        // Given
        setupAuthenticatedUser();
        doThrow(new ValidationException("Coordinates outside geofencing boundaries"))
                .when(geofencingService).validateCoordinates(anyDouble(), anyDouble(), any(UUID.class));

        // When/Then
        assertThatThrownBy(() -> reportService.createReport(testRequest, photo))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Coordinates outside geofencing boundaries");

        verify(geofencingService).validateCoordinates(40.4168, -3.7038, testCountryId);
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("Should throw exception when country not found")
    void shouldThrowExceptionWhenCountryNotFound() {
        // Given
        setupAuthenticatedUser();
        doNothing().when(geofencingService).validateCoordinates(anyDouble(), anyDouble(), any(UUID.class));
        when(geofencingService.getCountryById(testCountryId))
                .thenThrow(new ResourceNotFoundException("Country not found"));

        // When/Then
        assertThatThrownBy(() -> reportService.createReport(testRequest, photo))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found");

        verify(geofencingService).getCountryById(testCountryId);
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("Should create report as duplicate when duplicate detected")
    void shouldCreateReportAsDuplicateWhenDuplicateDetected() {
        // Given
        setupAuthenticatedUser();
        Task parentTask = Task.builder()
                .id(UUID.randomUUID())
                .duplicateCount(0)
                .build();

        Report duplicateReport = Report.builder()
                .id(testReportId)
                .submitter(testUser)
                .location(testLocation)
                .category("BASURA_ACUMULADA")
                .description("Test report description")
                .photoUrl("/uploads/test-photo.jpg")
                .isDuplicate(true)
                .parentTask(parentTask)
                .country(testCountrySpain)
                .createdAt(LocalDateTime.now())
                .build();

        when(fileStorageService.storeFile(photo)).thenReturn("/uploads/test-photo.jpg");
        when(geofencingService.getCountryById(testCountryId)).thenReturn(testCountrySpain);
        when(geofencingService.createPoint(anyDouble(), anyDouble())).thenReturn(testLocation);
        when(deduplicationService.checkForDuplicatesBeforeSave(any(Report.class))).thenReturn(Optional.of(parentTask));
        when(reportRepository.save(any(Report.class))).thenReturn(duplicateReport);
        when(taskRepository.save(any(Task.class))).thenReturn(parentTask);
        doNothing().when(geofencingService).validateCoordinates(anyDouble(), anyDouble(), any(UUID.class));

        // When
        Report result = reportService.createReport(testRequest, photo);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsDuplicate()).isTrue();
        assertThat(result.getParentTask()).isEqualTo(parentTask);

        verify(deduplicationService).checkForDuplicatesBeforeSave(any(Report.class));
        verify(reportRepository).save(any(Report.class));
        verify(taskRepository).save(parentTask);
        verify(taskService, never()).createTask(any(Report.class));
    }

    @Test
    @DisplayName("Should throw exception when latitude is null")
    void shouldThrowExceptionWhenLatitudeIsNull() {
        // Given
        testRequest.setLatitude(null);

        // When/Then
        assertThatThrownBy(() -> reportService.createReport(testRequest, photo))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Latitude is required");

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("Should throw exception when longitude is null")
    void shouldThrowExceptionWhenLongitudeIsNull() {
        // Given
        testRequest.setLongitude(null);

        // When/Then
        assertThatThrownBy(() -> reportService.createReport(testRequest, photo))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Longitude is required");

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("Should throw exception when category is null")
    void shouldThrowExceptionWhenCategoryIsNull() {
        // Given
        testRequest.setCategory(null);

        // When/Then
        assertThatThrownBy(() -> reportService.createReport(testRequest, photo))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Category is required");

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("Should throw exception when description is null")
    void shouldThrowExceptionWhenDescriptionIsNull() {
        // Given
        testRequest.setDescription(null);

        // When/Then
        assertThatThrownBy(() -> reportService.createReport(testRequest, photo))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Description is required");

        verify(reportRepository, never()).save(any(Report.class));
    }

    // ========================================================================
    // FILTERING BY COUNTRY TESTS
    // ========================================================================

    @Test
    @DisplayName("Should filter reports by country successfully")
    void shouldFilterReportsByCountrySuccessfully() {
        // Given
        Report reportSpain1 = createReportWithCountry(testCountrySpain);
        Report reportSpain2 = createReportWithCountry(testCountrySpain);
        Report reportColombia = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportSpain1, reportSpain2, reportColombia));

        // When
        List<ReportResponse> results = reportService.getAllReports(testCountryId, null, null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getCountryId().equals(testCountryId));
        assertThat(results).allMatch(r -> r.getCountryName().equals("España"));

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should return all reports when country filter is null")
    void shouldReturnAllReportsWhenCountryFilterIsNull() {
        // Given
        Report reportSpain = createReportWithCountry(testCountrySpain);
        Report reportColombia = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportSpain, reportColombia));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, null, null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no reports match country filter")
    void shouldReturnEmptyListWhenNoReportsMatchCountryFilter() {
        // Given
        UUID nonExistentCountryId = UUID.randomUUID();
        Report reportSpain = createReportWithCountry(testCountrySpain);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportSpain));

        // When
        List<ReportResponse> results = reportService.getAllReports(nonExistentCountryId, null, null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).isEmpty();

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should handle reports without country when filtering by country")
    void shouldHandleReportsWithoutCountryWhenFilteringByCountry() {
        // Given
        Report reportWithCountry = createReportWithCountry(testCountrySpain);
        Report reportWithoutCountry = createReportWithCountry(null);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportWithCountry, reportWithoutCountry));

        // When
        List<ReportResponse> results = reportService.getAllReports(testCountryId, null, null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCountryId()).isEqualTo(testCountryId);

        verify(reportRepository).findAll();
    }

    // ========================================================================
    // FILTERING BY ADMINISTRATIVE AREA TESTS
    // ========================================================================

    @Test
    @DisplayName("Should filter reports by administrative area successfully")
    void shouldFilterReportsByAdministrativeAreaSuccessfully() {
        // Given
        Report reportMadrid1 = createReportWithCountry(testCountrySpain);
        Report reportMadrid2 = createReportWithCountry(testCountrySpain);
        Report reportBogota = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid1, reportMadrid2, reportBogota));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, "Comunidad de Madrid", null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getCountryName().equals("España"));

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no reports match administrative area")
    void shouldReturnEmptyListWhenNoReportsMatchAdministrativeArea() {
        // Given
        Report reportMadrid = createReportWithCountry(testCountrySpain);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, "Cataluña", null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).isEmpty();

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should handle empty administrative area filter")
    void shouldHandleEmptyAdministrativeAreaFilter() {
        // Given
        Report reportMadrid = createReportWithCountry(testCountrySpain);
        Report reportBogota = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid, reportBogota));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, "", null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should handle reports without country when filtering by administrative area")
    void shouldHandleReportsWithoutCountryWhenFilteringByAdministrativeArea() {
        // Given
        Report reportWithCountry = createReportWithCountry(testCountrySpain);
        Report reportWithoutCountry = createReportWithCountry(null);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportWithCountry, reportWithoutCountry));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, "Comunidad de Madrid", null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);

        verify(reportRepository).findAll();
    }

    // ========================================================================
    // FILTERING BY MUNICIPALITY TESTS
    // ========================================================================

    @Test
    @DisplayName("Should filter reports by municipality successfully")
    void shouldFilterReportsByMunicipalitySuccessfully() {
        // Given
        Report reportMadrid1 = createReportWithCountry(testCountrySpain);
        Report reportMadrid2 = createReportWithCountry(testCountrySpain);
        Report reportBogota = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid1, reportMadrid2, reportBogota));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, null, "Madrid");

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getCountryName().equals("España"));

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no reports match municipality")
    void shouldReturnEmptyListWhenNoReportsMatchMunicipality() {
        // Given
        Report reportMadrid = createReportWithCountry(testCountrySpain);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, null, "Barcelona");

        // Then
        assertThat(results).isNotNull();
        assertThat(results).isEmpty();

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should handle empty municipality filter")
    void shouldHandleEmptyMunicipalityFilter() {
        // Given
        Report reportMadrid = createReportWithCountry(testCountrySpain);
        Report reportBogota = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid, reportBogota));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, null, "");

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should handle reports without country when filtering by municipality")
    void shouldHandleReportsWithoutCountryWhenFilteringByMunicipality() {
        // Given
        Report reportWithCountry = createReportWithCountry(testCountrySpain);
        Report reportWithoutCountry = createReportWithCountry(null);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportWithCountry, reportWithoutCountry));

        // When
        List<ReportResponse> results = reportService.getAllReports(null, null, "Madrid");

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);

        verify(reportRepository).findAll();
    }

    // ========================================================================
    // COMBINED FILTERING TESTS
    // ========================================================================

    @Test
    @DisplayName("Should filter reports by country and administrative area")
    void shouldFilterReportsByCountryAndAdministrativeArea() {
        // Given
        Report reportMadrid = createReportWithCountry(testCountrySpain);
        Report reportBogota = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid, reportBogota));

        // When
        List<ReportResponse> results = reportService.getAllReports(testCountryId, "Comunidad de Madrid", null);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCountryId()).isEqualTo(testCountryId);

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should filter reports by country and municipality")
    void shouldFilterReportsByCountryAndMunicipality() {
        // Given
        Report reportMadrid = createReportWithCountry(testCountrySpain);
        Report reportBogota = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid, reportBogota));

        // When
        List<ReportResponse> results = reportService.getAllReports(testCountryId, null, "Madrid");

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCountryId()).isEqualTo(testCountryId);

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should filter reports by all three parameters")
    void shouldFilterReportsByAllThreeParameters() {
        // Given
        Report reportMadrid = createReportWithCountry(testCountrySpain);
        Report reportBogota = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(reportMadrid, reportBogota));

        // When
        List<ReportResponse> results = reportService.getAllReports(
                testCountryId, "Comunidad de Madrid", "Madrid");

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCountryId()).isEqualTo(testCountryId);

        verify(reportRepository).findAll();
    }

    // ========================================================================
    // GET REPORT BY ID TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get report by ID successfully")
    void shouldGetReportByIdSuccessfully() {
        // Given
        when(reportRepository.findById(testReportId)).thenReturn(Optional.of(testReport));

        // When
        Report result = reportService.getReportById(testReportId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testReportId);
        assertThat(result.getCountry()).isEqualTo(testCountrySpain);

        verify(reportRepository).findById(testReportId);
    }

    @Test
    @DisplayName("Should throw exception when report not found by ID")
    void shouldThrowExceptionWhenReportNotFoundById() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(reportRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> reportService.getReportById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found: " + nonExistentId);

        verify(reportRepository).findById(nonExistentId);
    }

    // ========================================================================
    // GET ALL REPORTS TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get all reports successfully")
    void shouldGetAllReportsSuccessfully() {
        // Given
        Report report1 = createReportWithCountry(testCountrySpain);
        Report report2 = createReportWithCountry(testCountryColombia);

        when(reportRepository.findAll()).thenReturn(Arrays.asList(report1, report2));

        // When
        List<ReportResponse> results = reportService.getAllReports();

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);

        verify(reportRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no reports exist")
    void shouldReturnEmptyListWhenNoReportsExist() {
        // Given
        when(reportRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<ReportResponse> results = reportService.getAllReports();

        // Then
        assertThat(results).isNotNull();
        assertThat(results).isEmpty();

        verify(reportRepository).findAll();
    }

    // ========================================================================
    // GET MY REPORTS TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get reports by current user successfully")
    void shouldGetReportsByCurrentUserSuccessfully() {
        // Given
        setupAuthenticatedUser();
        Report report1 = createReportWithCountry(testCountrySpain);
        Report report2 = createReportWithCountry(testCountryColombia);

        when(reportRepository.findBySubmitter(testUser)).thenReturn(Arrays.asList(report1, report2));

        // When
        List<ReportResponse> results = reportService.getMyReports();

        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);

        verify(reportRepository).findBySubmitter(testUser);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void setupAuthenticatedUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("testuser");
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    private Report createReportWithCountry(Country country) {
        return Report.builder()
                .id(UUID.randomUUID())
                .submitter(testUser)
                .location(testLocation)
                .category("BASURA_ACUMULADA")
                .description("Test report")
                .photoUrl("/uploads/test.jpg")
                .isDuplicate(false)
                .country(country)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
