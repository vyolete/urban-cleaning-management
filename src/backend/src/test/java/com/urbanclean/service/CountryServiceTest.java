package com.urbanclean.service;

import com.urbanclean.dto.request.CountryRequest;
import com.urbanclean.dto.response.CountryResponse;
import com.urbanclean.entity.Country;
import com.urbanclean.entity.Report;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.CountryRepository;
import com.urbanclean.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CountryService
 * Tests CRUD operations, default country management, and geofencing boundary validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CountryService Unit Tests")
class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private CountryService countryService;

    private Country testCountry;
    private CountryRequest testRequest;
    private UUID testCountryId;

    @BeforeEach
    void setUp() {
        testCountryId = UUID.randomUUID();
        
        // Setup test country
        testCountry = Country.builder()
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

        // Setup test request
        testRequest = CountryRequest.builder()
                .name("Colombia")
                .code("COL")
                .minLat(new BigDecimal("-4.2"))
                .maxLat(new BigDecimal("12.5"))
                .minLon(new BigDecimal("-79.0"))
                .maxLon(new BigDecimal("-66.9"))
                .administrativeArea("Cundinamarca")
                .municipality("Bogotá")
                .centerLat(new BigDecimal("4.7110"))
                .centerLon(new BigDecimal("-74.0721"))
                .build();
    }

    // ========================================================================
    // CREATE COUNTRY TESTS
    // ========================================================================

    @Test
    @DisplayName("Should create country with valid data")
    void shouldCreateCountryWithValidData() {
        // Given
        when(countryRepository.findByCode(testRequest.getCode())).thenReturn(Optional.empty());
        when(countryRepository.findByName(testRequest.getName())).thenReturn(Optional.empty());
        when(countryRepository.save(any(Country.class))).thenReturn(testCountry);

        // When
        CountryResponse response = countryService.createCountry(testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(testCountry.getName());
        assertThat(response.getCode()).isEqualTo(testCountry.getCode());
        assertThat(response.getDefaultCountry()).isFalse(); // New countries are not default
        assertThat(response.getEnabled()).isTrue();

        verify(countryRepository).findByCode(testRequest.getCode());
        verify(countryRepository).findByName(testRequest.getName());
        verify(countryRepository).save(any(Country.class));
    }

    @Test
    @DisplayName("Should throw exception when creating country with duplicate code")
    void shouldThrowExceptionWhenCreatingCountryWithDuplicateCode() {
        // Given
        when(countryRepository.findByCode(testRequest.getCode())).thenReturn(Optional.of(testCountry));

        // When/Then
        assertThatThrownBy(() -> countryService.createCountry(testRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Country with code " + testRequest.getCode() + " already exists");

        verify(countryRepository).findByCode(testRequest.getCode());
        verify(countryRepository, never()).save(any(Country.class));
    }

    @Test
    @DisplayName("Should throw exception when creating country with duplicate name")
    void shouldThrowExceptionWhenCreatingCountryWithDuplicateName() {
        // Given
        when(countryRepository.findByCode(testRequest.getCode())).thenReturn(Optional.empty());
        when(countryRepository.findByName(testRequest.getName())).thenReturn(Optional.of(testCountry));

        // When/Then
        assertThatThrownBy(() -> countryService.createCountry(testRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Country with name " + testRequest.getName() + " already exists");

        verify(countryRepository).findByCode(testRequest.getCode());
        verify(countryRepository).findByName(testRequest.getName());
        verify(countryRepository, never()).save(any(Country.class));
    }

    @Test
    @DisplayName("Should throw exception when creating country with invalid boundaries")
    void shouldThrowExceptionWhenCreatingCountryWithInvalidBoundaries() {
        // Given - minLat >= maxLat
        CountryRequest invalidRequest = CountryRequest.builder()
                .name("Invalid Country")
                .code("INV")
                .minLat(new BigDecimal("50.0"))
                .maxLat(new BigDecimal("40.0"))  // Less than minLat
                .minLon(new BigDecimal("-10.0"))
                .maxLon(new BigDecimal("10.0"))
                .build();

        // When/Then
        assertThatThrownBy(() -> countryService.createCountry(invalidRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Minimum latitude must be less than maximum latitude");

        verify(countryRepository, never()).save(any(Country.class));
    }

    // ========================================================================
    // UPDATE COUNTRY TESTS
    // ========================================================================

    @Test
    @DisplayName("Should update country with valid data")
    void shouldUpdateCountryWithValidData() {
        // Given
        CountryRequest updateRequest = CountryRequest.builder()
                .name("España")
                .code("ESP")
                .minLat(new BigDecimal("35.0"))
                .maxLat(new BigDecimal("44.0"))
                .minLon(new BigDecimal("-10.0"))
                .maxLon(new BigDecimal("4.0"))
                .administrativeArea("Cataluña")
                .municipality("Barcelona")
                .centerLat(new BigDecimal("41.3851"))
                .centerLon(new BigDecimal("2.1734"))
                .build();

        when(countryRepository.findById(testCountryId)).thenReturn(Optional.of(testCountry));
        when(countryRepository.save(any(Country.class))).thenReturn(testCountry);

        // When
        CountryResponse response = countryService.updateCountry(testCountryId, updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(countryRepository).findById(testCountryId);
        verify(countryRepository).save(any(Country.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent country")
    void shouldThrowExceptionWhenUpdatingNonExistentCountry() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(countryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryService.updateCountry(nonExistentId, testRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id: " + nonExistentId);

        verify(countryRepository).findById(nonExistentId);
        verify(countryRepository, never()).save(any(Country.class));
    }

    @Test
    @DisplayName("Should throw exception when updating country with duplicate code")
    void shouldThrowExceptionWhenUpdatingCountryWithDuplicateCode() {
        // Given
        Country anotherCountry = Country.builder()
                .id(UUID.randomUUID())
                .name("Colombia")
                .code("COL")
                .build();

        CountryRequest updateRequest = CountryRequest.builder()
                .name("España")
                .code("COL")  // Trying to change to existing code
                .minLat(new BigDecimal("36.0"))
                .maxLat(new BigDecimal("43.8"))
                .minLon(new BigDecimal("-9.3"))
                .maxLon(new BigDecimal("3.3"))
                .build();

        when(countryRepository.findById(testCountryId)).thenReturn(Optional.of(testCountry));
        when(countryRepository.findByCode("COL")).thenReturn(Optional.of(anotherCountry));

        // When/Then
        assertThatThrownBy(() -> countryService.updateCountry(testCountryId, updateRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Country with code COL already exists");

        verify(countryRepository).findById(testCountryId);
        verify(countryRepository).findByCode("COL");
        verify(countryRepository, never()).save(any(Country.class));
    }

    @Test
    @DisplayName("Should throw exception when updating country with duplicate name")
    void shouldThrowExceptionWhenUpdatingCountryWithDuplicateName() {
        // Given
        Country anotherCountry = Country.builder()
                .id(UUID.randomUUID())
                .name("Colombia")
                .code("COL")
                .build();

        CountryRequest updateRequest = CountryRequest.builder()
                .name("Colombia")  // Trying to change to existing name
                .code("ESP")
                .minLat(new BigDecimal("36.0"))
                .maxLat(new BigDecimal("43.8"))
                .minLon(new BigDecimal("-9.3"))
                .maxLon(new BigDecimal("3.3"))
                .build();

        when(countryRepository.findById(testCountryId)).thenReturn(Optional.of(testCountry));
        when(countryRepository.findByCode("ESP")).thenReturn(Optional.empty());
        when(countryRepository.findByName("Colombia")).thenReturn(Optional.of(anotherCountry));

        // When/Then
        assertThatThrownBy(() -> countryService.updateCountry(testCountryId, updateRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Country with name Colombia already exists");

        verify(countryRepository).findById(testCountryId);
        verify(countryRepository).findByName("Colombia");
        verify(countryRepository, never()).save(any(Country.class));
    }

    // ========================================================================
    // DELETE COUNTRY TESTS
    // ========================================================================

    @Test
    @DisplayName("Should delete (disable) country successfully")
    void shouldDeleteCountrySuccessfully() {
        // Given
        Country nonDefaultCountry = Country.builder()
                .id(testCountryId)
                .name("Colombia")
                .code("COL")
                .defaultCountry(false)
                .enabled(true)
                .build();

        when(countryRepository.findById(testCountryId)).thenReturn(Optional.of(nonDefaultCountry));
        when(countryRepository.save(any(Country.class))).thenReturn(nonDefaultCountry);

        // When
        countryService.deleteCountry(testCountryId);

        // Then
        verify(countryRepository).findById(testCountryId);
        verify(countryRepository).save(argThat(country -> !country.getEnabled()));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent country")
    void shouldThrowExceptionWhenDeletingNonExistentCountry() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(countryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryService.deleteCountry(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id: " + nonExistentId);

        verify(countryRepository).findById(nonExistentId);
        verify(countryRepository, never()).save(any(Country.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting default country")
    void shouldThrowExceptionWhenDeletingDefaultCountry() {
        // Given
        when(countryRepository.findById(testCountryId)).thenReturn(Optional.of(testCountry));

        // When/Then
        assertThatThrownBy(() -> countryService.deleteCountry(testCountryId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot delete the default country");

        verify(countryRepository).findById(testCountryId);
        verify(countryRepository, never()).save(any(Country.class));
    }

    // ========================================================================
    // GET COUNTRY TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get country by ID successfully")
    void shouldGetCountryByIdSuccessfully() {
        // Given
        when(countryRepository.findById(testCountryId)).thenReturn(Optional.of(testCountry));

        // When
        CountryResponse response = countryService.getCountryById(testCountryId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testCountryId);
        assertThat(response.getName()).isEqualTo("España");
        assertThat(response.getCode()).isEqualTo("ESP");
        assertThat(response.getDefaultCountry()).isTrue();
        assertThat(response.getEnabled()).isTrue();

        verify(countryRepository).findById(testCountryId);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent country")
    void shouldThrowExceptionWhenGettingNonExistentCountry() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(countryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryService.getCountryById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id: " + nonExistentId);

        verify(countryRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should get all countries successfully")
    void shouldGetAllCountriesSuccessfully() {
        // Given
        Country country2 = Country.builder()
                .id(UUID.randomUUID())
                .name("Colombia")
                .code("COL")
                .defaultCountry(false)
                .enabled(true)
                .minLat(new BigDecimal("-4.2"))
                .maxLat(new BigDecimal("12.5"))
                .minLon(new BigDecimal("-79.0"))
                .maxLon(new BigDecimal("-66.9"))
                .build();

        when(countryRepository.findAll()).thenReturn(Arrays.asList(testCountry, country2));

        // When
        List<CountryResponse> responses = countryService.getAllCountries();

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("España");
        assertThat(responses.get(1).getName()).isEqualTo("Colombia");

        verify(countryRepository).findAll();
    }

    @Test
    @DisplayName("Should get enabled countries only")
    void shouldGetEnabledCountriesOnly() {
        // Given
        Country enabledCountry = Country.builder()
                .id(UUID.randomUUID())
                .name("Colombia")
                .code("COL")
                .enabled(true)
                .build();

        when(countryRepository.findByEnabledTrue()).thenReturn(Arrays.asList(testCountry, enabledCountry));

        // When
        List<CountryResponse> responses = countryService.getEnabledCountries();

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.getEnabled());

        verify(countryRepository).findByEnabledTrue();
    }

    // ========================================================================
    // DEFAULT COUNTRY TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get default country successfully")
    void shouldGetDefaultCountrySuccessfully() {
        // Given
        when(countryRepository.findByDefaultCountryTrue()).thenReturn(Optional.of(testCountry));

        // When
        CountryResponse response = countryService.getDefaultCountry();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDefaultCountry()).isTrue();
        assertThat(response.getName()).isEqualTo("España");

        verify(countryRepository).findByDefaultCountryTrue();
    }

    @Test
    @DisplayName("Should throw exception when no default country configured")
    void shouldThrowExceptionWhenNoDefaultCountryConfigured() {
        // Given
        when(countryRepository.findByDefaultCountryTrue()).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryService.getDefaultCountry())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No default country configured");

        verify(countryRepository).findByDefaultCountryTrue();
    }

    @Test
    @DisplayName("Should set default country successfully")
    void shouldSetDefaultCountrySuccessfully() {
        // Given
        UUID newDefaultId = UUID.randomUUID();
        Country currentDefault = Country.builder()
                .id(testCountryId)
                .name("España")
                .code("ESP")
                .defaultCountry(true)
                .build();

        Country newDefault = Country.builder()
                .id(newDefaultId)
                .name("Colombia")
                .code("COL")
                .defaultCountry(false)
                .build();

        when(countryRepository.findById(newDefaultId)).thenReturn(Optional.of(newDefault));
        when(countryRepository.findByDefaultCountryTrue()).thenReturn(Optional.of(currentDefault));
        when(countryRepository.save(any(Country.class))).thenReturn(newDefault);

        // When
        countryService.setDefaultCountry(newDefaultId);

        // Then
        verify(countryRepository).findById(newDefaultId);
        verify(countryRepository).findByDefaultCountryTrue();
        verify(countryRepository, times(2)).save(any(Country.class));
    }

    @Test
    @DisplayName("Should throw exception when setting non-existent country as default")
    void shouldThrowExceptionWhenSettingNonExistentCountryAsDefault() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(countryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryService.setDefaultCountry(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id: " + nonExistentId);

        verify(countryRepository).findById(nonExistentId);
        verify(countryRepository, never()).save(any(Country.class));
    }

    // ========================================================================
    // GEOFENCING BOUNDARY VALIDATION TESTS
    // ========================================================================

    @Test
    @DisplayName("Should validate correct geofencing boundaries")
    void shouldValidateCorrectGeofencingBoundaries() {
        // Given
        BigDecimal minLat = new BigDecimal("36.0");
        BigDecimal maxLat = new BigDecimal("43.8");
        BigDecimal minLon = new BigDecimal("-9.3");
        BigDecimal maxLon = new BigDecimal("3.3");

        // When/Then - Should not throw exception
        countryService.validateGeofencingBoundaries(minLat, maxLat, minLon, maxLon);
    }

    @Test
    @DisplayName("Should throw exception when minLat >= maxLat")
    void shouldThrowExceptionWhenMinLatGreaterThanOrEqualMaxLat() {
        // Given
        BigDecimal minLat = new BigDecimal("50.0");
        BigDecimal maxLat = new BigDecimal("40.0");
        BigDecimal minLon = new BigDecimal("-10.0");
        BigDecimal maxLon = new BigDecimal("10.0");

        // When/Then
        assertThatThrownBy(() -> countryService.validateGeofencingBoundaries(minLat, maxLat, minLon, maxLon))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Minimum latitude must be less than maximum latitude");
    }

    @Test
    @DisplayName("Should throw exception when minLon >= maxLon")
    void shouldThrowExceptionWhenMinLonGreaterThanOrEqualMaxLon() {
        // Given
        BigDecimal minLat = new BigDecimal("36.0");
        BigDecimal maxLat = new BigDecimal("43.8");
        BigDecimal minLon = new BigDecimal("10.0");
        BigDecimal maxLon = new BigDecimal("-10.0");

        // When/Then
        assertThatThrownBy(() -> countryService.validateGeofencingBoundaries(minLat, maxLat, minLon, maxLon))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Minimum longitude must be less than maximum longitude");
    }

    @Test
    @DisplayName("Should throw exception when latitude out of range")
    void shouldThrowExceptionWhenLatitudeOutOfRange() {
        // Given - latitude > 90
        BigDecimal minLat = new BigDecimal("36.0");
        BigDecimal maxLat = new BigDecimal("95.0");
        BigDecimal minLon = new BigDecimal("-10.0");
        BigDecimal maxLon = new BigDecimal("10.0");

        // When/Then
        assertThatThrownBy(() -> countryService.validateGeofencingBoundaries(minLat, maxLat, minLon, maxLon))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Latitude must be between -90 and 90");
    }

    @Test
    @DisplayName("Should throw exception when longitude out of range")
    void shouldThrowExceptionWhenLongitudeOutOfRange() {
        // Given - longitude > 180
        BigDecimal minLat = new BigDecimal("36.0");
        BigDecimal maxLat = new BigDecimal("43.8");
        BigDecimal minLon = new BigDecimal("-10.0");
        BigDecimal maxLon = new BigDecimal("190.0");

        // When/Then
        assertThatThrownBy(() -> countryService.validateGeofencingBoundaries(minLat, maxLat, minLon, maxLon))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Longitude must be between -180 and 180");
    }

    @Test
    @DisplayName("Should validate boundaries at edge of valid range")
    void shouldValidateBoundariesAtEdgeOfValidRange() {
        // Given - Valid edge cases
        BigDecimal minLat = new BigDecimal("-90.0");
        BigDecimal maxLat = new BigDecimal("90.0");
        BigDecimal minLon = new BigDecimal("-180.0");
        BigDecimal maxLon = new BigDecimal("180.0");

        // When/Then - Should not throw exception
        countryService.validateGeofencingBoundaries(minLat, maxLat, minLon, maxLon);
    }

    // ========================================================================
    // MIGRATION TESTS
    // ========================================================================

    @Test
    @DisplayName("Should migrate existing reports to default country")
    void shouldMigrateExistingReportsToDefaultCountry() {
        // Given
        Report report1 = new Report();
        report1.setId(UUID.randomUUID());
        report1.setCountry(null);

        Report report2 = new Report();
        report2.setId(UUID.randomUUID());
        report2.setCountry(null);

        Report report3 = new Report();
        report3.setId(UUID.randomUUID());
        report3.setCountry(testCountry);  // Already has country

        when(countryRepository.findByDefaultCountryTrue()).thenReturn(Optional.of(testCountry));
        when(reportRepository.findAll()).thenReturn(Arrays.asList(report1, report2, report3));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        countryService.migrateExistingReportsToDefaultCountry();

        // Then
        verify(countryRepository).findByDefaultCountryTrue();
        verify(reportRepository).findAll();
        verify(reportRepository, times(2)).save(any(Report.class));  // Only 2 reports without country
    }

    @Test
    @DisplayName("Should throw exception when migrating without default country")
    void shouldThrowExceptionWhenMigratingWithoutDefaultCountry() {
        // Given
        when(countryRepository.findByDefaultCountryTrue()).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryService.migrateExistingReportsToDefaultCountry())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No default country configured");

        verify(countryRepository).findByDefaultCountryTrue();
        verify(reportRepository, never()).findAll();
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("Should handle migration when no reports need migration")
    void shouldHandleMigrationWhenNoReportsNeedMigration() {
        // Given
        Report report1 = new Report();
        report1.setId(UUID.randomUUID());
        report1.setCountry(testCountry);

        when(countryRepository.findByDefaultCountryTrue()).thenReturn(Optional.of(testCountry));
        when(reportRepository.findAll()).thenReturn(Arrays.asList(report1));

        // When
        countryService.migrateExistingReportsToDefaultCountry();

        // Then
        verify(countryRepository).findByDefaultCountryTrue();
        verify(reportRepository).findAll();
        verify(reportRepository, never()).save(any(Report.class));  // No reports to migrate
    }
}
