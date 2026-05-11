package com.urbanclean.service;

import com.urbanclean.entity.Country;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GeofencingService
 * Tests coordinate validation, boundary checking, and polygon creation for each country
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeofencingService Unit Tests")
class GeofencingServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Spy
    private GeometryFactory geometryFactory = new GeometryFactory();

    @InjectMocks
    private GeofencingService geofencingService;

    private Country spainCountry;
    private Country colombiaCountry;
    private UUID spainId;
    private UUID colombiaId;

    @BeforeEach
    void setUp() {
        spainId = UUID.randomUUID();
        colombiaId = UUID.randomUUID();

        // Setup Spain test country
        spainCountry = Country.builder()
                .id(spainId)
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

        // Setup Colombia test country
        colombiaCountry = Country.builder()
                .id(colombiaId)
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

        // Set legacy geofencing properties for backward compatibility tests
        ReflectionTestUtils.setField(geofencingService, "minLatitude", 40.3);
        ReflectionTestUtils.setField(geofencingService, "maxLatitude", 40.6);
        ReflectionTestUtils.setField(geofencingService, "minLongitude", -3.9);
        ReflectionTestUtils.setField(geofencingService, "maxLongitude", -3.5);
    }

    // ========================================================================
    // COORDINATE VALIDATION TESTS - WITH COUNTRY CONTEXT
    // ========================================================================

    @Test
    @DisplayName("Should validate coordinates within Spain boundaries")
    void shouldValidateCoordinatesWithinSpainBoundaries() {
        // Given - Madrid coordinates
        Double latitude = 40.4168;
        Double longitude = -3.7038;
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When/Then - Should not throw exception
        geofencingService.validateCoordinates(latitude, longitude, spainId);

        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should validate coordinates within Colombia boundaries")
    void shouldValidateCoordinatesWithinColombiaBoundaries() {
        // Given - Bogotá coordinates
        Double latitude = 4.7110;
        Double longitude = -74.0721;
        when(countryRepository.findById(colombiaId)).thenReturn(Optional.of(colombiaCountry));

        // When/Then - Should not throw exception
        geofencingService.validateCoordinates(latitude, longitude, colombiaId);

        verify(countryRepository).findById(colombiaId);
    }

    @Test
    @DisplayName("Should throw exception when coordinates outside Spain boundaries")
    void shouldThrowExceptionWhenCoordinatesOutsideSpainBoundaries() {
        // Given - Paris coordinates (outside Spain)
        Double latitude = 48.8566;
        Double longitude = 2.3522;
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, spainId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Coordinates")
                .hasMessageContaining("are outside España geofencing boundaries");

        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should throw exception when coordinates outside Colombia boundaries")
    void shouldThrowExceptionWhenCoordinatesOutsideColombiaBoundaries() {
        // Given - Lima, Peru coordinates (outside Colombia)
        Double latitude = -12.0464;
        Double longitude = -77.0428;
        when(countryRepository.findById(colombiaId)).thenReturn(Optional.of(colombiaCountry));

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, colombiaId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Coordinates")
                .hasMessageContaining("are outside Colombia geofencing boundaries");

        verify(countryRepository).findById(colombiaId);
    }

    @Test
    @DisplayName("Should throw exception when latitude is null")
    void shouldThrowExceptionWhenLatitudeIsNull() {
        // Given
        Double latitude = null;
        Double longitude = -3.7038;

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, spainId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Latitude and longitude are required");

        verify(countryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when longitude is null")
    void shouldThrowExceptionWhenLongitudeIsNull() {
        // Given
        Double latitude = 40.4168;
        Double longitude = null;

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, spainId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Latitude and longitude are required");

        verify(countryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when latitude is out of valid range (> 90)")
    void shouldThrowExceptionWhenLatitudeGreaterThan90() {
        // Given
        Double latitude = 95.0;
        Double longitude = -3.7038;

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, spainId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid latitude")
                .hasMessageContaining("Must be between -90 and 90");

        verify(countryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when latitude is out of valid range (< -90)")
    void shouldThrowExceptionWhenLatitudeLessThanMinus90() {
        // Given
        Double latitude = -95.0;
        Double longitude = -3.7038;

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, spainId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid latitude")
                .hasMessageContaining("Must be between -90 and 90");

        verify(countryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when longitude is out of valid range (> 180)")
    void shouldThrowExceptionWhenLongitudeGreaterThan180() {
        // Given
        Double latitude = 40.4168;
        Double longitude = 185.0;

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, spainId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid longitude")
                .hasMessageContaining("Must be between -180 and 180");

        verify(countryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when longitude is out of valid range (< -180)")
    void shouldThrowExceptionWhenLongitudeLessThanMinus180() {
        // Given
        Double latitude = 40.4168;
        Double longitude = -185.0;

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, spainId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid longitude")
                .hasMessageContaining("Must be between -180 and 180");

        verify(countryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should use default country when countryId is null")
    void shouldUseDefaultCountryWhenCountryIdIsNull() {
        // Given
        Double latitude = 40.4168;
        Double longitude = -3.7038;
        when(countryRepository.findByDefaultCountryTrue()).thenReturn(Optional.of(spainCountry));
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When
        geofencingService.validateCoordinates(latitude, longitude, null);

        // Then
        verify(countryRepository).findByDefaultCountryTrue();
        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should throw exception when no default country configured")
    void shouldThrowExceptionWhenNoDefaultCountryConfigured() {
        // Given
        Double latitude = 40.4168;
        Double longitude = -3.7038;
        when(countryRepository.findByDefaultCountryTrue()).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No default country configured");

        verify(countryRepository).findByDefaultCountryTrue();
        verify(countryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when country not found")
    void shouldThrowExceptionWhenCountryNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        Double latitude = 40.4168;
        Double longitude = -3.7038;
        when(countryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id: " + nonExistentId);

        verify(countryRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should throw exception when country is disabled")
    void shouldThrowExceptionWhenCountryIsDisabled() {
        // Given
        Country disabledCountry = Country.builder()
                .id(spainId)
                .name("España")
                .code("ESP")
                .enabled(false)
                .minLat(new BigDecimal("36.0"))
                .maxLat(new BigDecimal("43.8"))
                .minLon(new BigDecimal("-9.3"))
                .maxLon(new BigDecimal("3.3"))
                .build();

        Double latitude = 40.4168;
        Double longitude = -3.7038;
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(disabledCountry));

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude, spainId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Country España is not enabled");

        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should validate coordinates at boundary edges for Spain")
    void shouldValidateCoordinatesAtBoundaryEdgesForSpain() {
        // Given - Coordinates at exact boundaries
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When/Then - Test all four corners
        geofencingService.validateCoordinates(36.0, -9.3, spainId);  // Min lat, min lon
        geofencingService.validateCoordinates(36.0, 3.3, spainId);   // Min lat, max lon
        geofencingService.validateCoordinates(43.8, -9.3, spainId);  // Max lat, min lon
        geofencingService.validateCoordinates(43.8, 3.3, spainId);   // Max lat, max lon

        verify(countryRepository, times(4)).findById(spainId);
    }

    @Test
    @DisplayName("Should validate coordinates at boundary edges for Colombia")
    void shouldValidateCoordinatesAtBoundaryEdgesForColombia() {
        // Given - Coordinates at exact boundaries
        when(countryRepository.findById(colombiaId)).thenReturn(Optional.of(colombiaCountry));

        // When/Then - Test all four corners
        geofencingService.validateCoordinates(-4.2, -79.0, colombiaId);  // Min lat, min lon
        geofencingService.validateCoordinates(-4.2, -66.9, colombiaId);  // Min lat, max lon
        geofencingService.validateCoordinates(12.5, -79.0, colombiaId);  // Max lat, min lon
        geofencingService.validateCoordinates(12.5, -66.9, colombiaId);  // Max lat, max lon

        verify(countryRepository, times(4)).findById(colombiaId);
    }

    // ========================================================================
    // COORDINATE VALIDATION TESTS - LEGACY METHOD (NO COUNTRY CONTEXT)
    // ========================================================================

    @Test
    @DisplayName("Should validate coordinates within legacy boundaries")
    void shouldValidateCoordinatesWithinLegacyBoundaries() {
        // Given - Coordinates within legacy Madrid boundaries
        Double latitude = 40.4168;
        Double longitude = -3.7038;

        // When/Then - Should not throw exception
        geofencingService.validateCoordinates(latitude, longitude);
    }

    @Test
    @DisplayName("Should throw exception when coordinates outside legacy boundaries")
    void shouldThrowExceptionWhenCoordinatesOutsideLegacyBoundaries() {
        // Given - Coordinates outside legacy boundaries
        Double latitude = 41.0;
        Double longitude = -3.0;

        // When/Then
        assertThatThrownBy(() -> geofencingService.validateCoordinates(latitude, longitude))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Coordinates")
                .hasMessageContaining("are outside the configured geofencing boundaries");
    }

    // ========================================================================
    // BOUNDARY CHECKING TESTS
    // ========================================================================

    @Test
    @DisplayName("Should return true when coordinates within Spain boundaries")
    void shouldReturnTrueWhenCoordinatesWithinSpainBoundaries() {
        // Given
        Double latitude = 40.4168;
        Double longitude = -3.7038;
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When
        boolean result = geofencingService.isWithinBoundaries(latitude, longitude, spainId);

        // Then
        assertThat(result).isTrue();
        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should return false when coordinates outside Spain boundaries")
    void shouldReturnFalseWhenCoordinatesOutsideSpainBoundaries() {
        // Given
        Double latitude = 48.8566;  // Paris
        Double longitude = 2.3522;
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When
        boolean result = geofencingService.isWithinBoundaries(latitude, longitude, spainId);

        // Then
        assertThat(result).isFalse();
        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should return true when coordinates within Colombia boundaries")
    void shouldReturnTrueWhenCoordinatesWithinColombiaBoundaries() {
        // Given
        Double latitude = 4.7110;
        Double longitude = -74.0721;
        when(countryRepository.findById(colombiaId)).thenReturn(Optional.of(colombiaCountry));

        // When
        boolean result = geofencingService.isWithinBoundaries(latitude, longitude, colombiaId);

        // Then
        assertThat(result).isTrue();
        verify(countryRepository).findById(colombiaId);
    }

    @Test
    @DisplayName("Should return false when coordinates outside Colombia boundaries")
    void shouldReturnFalseWhenCoordinatesOutsideColombiaBoundaries() {
        // Given
        Double latitude = -12.0464;  // Lima, Peru
        Double longitude = -77.0428;
        when(countryRepository.findById(colombiaId)).thenReturn(Optional.of(colombiaCountry));

        // When
        boolean result = geofencingService.isWithinBoundaries(latitude, longitude, colombiaId);

        // Then
        assertThat(result).isFalse();
        verify(countryRepository).findById(colombiaId);
    }

    @Test
    @DisplayName("Should throw exception when checking boundaries for non-existent country")
    void shouldThrowExceptionWhenCheckingBoundariesForNonExistentCountry() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        Double latitude = 40.4168;
        Double longitude = -3.7038;
        when(countryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> geofencingService.isWithinBoundaries(latitude, longitude, nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id: " + nonExistentId);

        verify(countryRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should return true for legacy boundary check when coordinates within boundaries")
    void shouldReturnTrueForLegacyBoundaryCheckWhenCoordinatesWithinBoundaries() {
        // Given
        Double latitude = 40.4168;
        Double longitude = -3.7038;

        // When
        boolean result = geofencingService.isWithinBoundaries(latitude, longitude);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for legacy boundary check when coordinates outside boundaries")
    void shouldReturnFalseForLegacyBoundaryCheckWhenCoordinatesOutsideBoundaries() {
        // Given
        Double latitude = 41.0;
        Double longitude = -3.0;

        // When
        boolean result = geofencingService.isWithinBoundaries(latitude, longitude);

        // Then
        assertThat(result).isFalse();
    }

    // ========================================================================
    // POLYGON CREATION TESTS
    // ========================================================================

    @Test
    @DisplayName("Should create boundary polygon for Spain")
    void shouldCreateBoundaryPolygonForSpain() {
        // Given
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When
        Polygon polygon = geofencingService.getBoundaryPolygon(spainId);

        // Then
        assertThat(polygon).isNotNull();
        assertThat(polygon.getNumPoints()).isEqualTo(5);  // 4 corners + closing point
        
        // Verify coordinates (PostGIS uses lon, lat order)
        Coordinate[] coordinates = polygon.getCoordinates();
        assertThat(coordinates[0].x).isEqualTo(-9.3);  // min lon
        assertThat(coordinates[0].y).isEqualTo(36.0);  // min lat
        assertThat(coordinates[1].x).isEqualTo(3.3);   // max lon
        assertThat(coordinates[1].y).isEqualTo(36.0);  // min lat
        assertThat(coordinates[2].x).isEqualTo(3.3);   // max lon
        assertThat(coordinates[2].y).isEqualTo(43.8);  // max lat
        assertThat(coordinates[3].x).isEqualTo(-9.3);  // min lon
        assertThat(coordinates[3].y).isEqualTo(43.8);  // max lat
        assertThat(coordinates[4]).isEqualTo(coordinates[0]);  // Closing point

        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should create boundary polygon for Colombia")
    void shouldCreateBoundaryPolygonForColombia() {
        // Given
        when(countryRepository.findById(colombiaId)).thenReturn(Optional.of(colombiaCountry));

        // When
        Polygon polygon = geofencingService.getBoundaryPolygon(colombiaId);

        // Then
        assertThat(polygon).isNotNull();
        assertThat(polygon.getNumPoints()).isEqualTo(5);  // 4 corners + closing point
        
        // Verify coordinates (PostGIS uses lon, lat order)
        Coordinate[] coordinates = polygon.getCoordinates();
        assertThat(coordinates[0].x).isEqualTo(-79.0);  // min lon
        assertThat(coordinates[0].y).isEqualTo(-4.2);   // min lat
        assertThat(coordinates[1].x).isEqualTo(-66.9);  // max lon
        assertThat(coordinates[1].y).isEqualTo(-4.2);   // min lat
        assertThat(coordinates[2].x).isEqualTo(-66.9);  // max lon
        assertThat(coordinates[2].y).isEqualTo(12.5);   // max lat
        assertThat(coordinates[3].x).isEqualTo(-79.0);  // min lon
        assertThat(coordinates[3].y).isEqualTo(12.5);   // max lat
        assertThat(coordinates[4]).isEqualTo(coordinates[0]);  // Closing point

        verify(countryRepository).findById(colombiaId);
    }

    @Test
    @DisplayName("Should throw exception when creating polygon for non-existent country")
    void shouldThrowExceptionWhenCreatingPolygonForNonExistentCountry() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(countryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> geofencingService.getBoundaryPolygon(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id: " + nonExistentId);

        verify(countryRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should create legacy boundary polygon")
    void shouldCreateLegacyBoundaryPolygon() {
        // When
        Polygon polygon = geofencingService.getBoundaryPolygon();

        // Then
        assertThat(polygon).isNotNull();
        assertThat(polygon.getNumPoints()).isEqualTo(5);  // 4 corners + closing point
        
        // Verify coordinates match legacy boundaries
        Coordinate[] coordinates = polygon.getCoordinates();
        assertThat(coordinates[0].x).isEqualTo(-3.9);  // min lon
        assertThat(coordinates[0].y).isEqualTo(40.3);  // min lat
        assertThat(coordinates[4]).isEqualTo(coordinates[0]);  // Closing point
    }

    // ========================================================================
    // POINT CREATION AND VALIDATION TESTS
    // ========================================================================

    @Test
    @DisplayName("Should create Point geometry from coordinates")
    void shouldCreatePointGeometryFromCoordinates() {
        // Given
        Double latitude = 40.4168;
        Double longitude = -3.7038;

        // When
        Point point = geofencingService.createPoint(latitude, longitude);

        // Then
        assertThat(point).isNotNull();
        assertThat(point.getX()).isEqualTo(longitude);  // PostGIS uses lon, lat order
        assertThat(point.getY()).isEqualTo(latitude);
    }

    @Test
    @DisplayName("Should validate point within Spain boundary using PostGIS")
    void shouldValidatePointWithinSpainBoundaryUsingPostGIS() {
        // Given
        Point point = geofencingService.createPoint(40.4168, -3.7038);
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When
        boolean result = geofencingService.isPointWithinBoundary(point, spainId);

        // Then
        assertThat(result).isTrue();
        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should validate point outside Spain boundary using PostGIS")
    void shouldValidatePointOutsideSpainBoundaryUsingPostGIS() {
        // Given
        Point point = geofencingService.createPoint(48.8566, 2.3522);  // Paris
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When
        boolean result = geofencingService.isPointWithinBoundary(point, spainId);

        // Then
        assertThat(result).isFalse();
        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should validate point within Colombia boundary using PostGIS")
    void shouldValidatePointWithinColombiaBoundaryUsingPostGIS() {
        // Given
        Point point = geofencingService.createPoint(4.7110, -74.0721);
        when(countryRepository.findById(colombiaId)).thenReturn(Optional.of(colombiaCountry));

        // When
        boolean result = geofencingService.isPointWithinBoundary(point, colombiaId);

        // Then
        assertThat(result).isTrue();
        verify(countryRepository).findById(colombiaId);
    }

    @Test
    @DisplayName("Should validate point within legacy boundary using PostGIS")
    void shouldValidatePointWithinLegacyBoundaryUsingPostGIS() {
        // Given
        Point point = geofencingService.createPoint(40.4168, -3.7038);

        // When
        boolean result = geofencingService.isPointWithinBoundary(point);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate point outside legacy boundary using PostGIS")
    void shouldValidatePointOutsideLegacyBoundaryUsingPostGIS() {
        // Given
        Point point = geofencingService.createPoint(41.0, -3.0);

        // When
        boolean result = geofencingService.isPointWithinBoundary(point);

        // Then
        assertThat(result).isFalse();
    }

    // ========================================================================
    // GET COUNTRY BY ID TESTS
    // ========================================================================

    @Test
    @DisplayName("Should get country by ID successfully")
    void shouldGetCountryByIdSuccessfully() {
        // Given
        when(countryRepository.findById(spainId)).thenReturn(Optional.of(spainCountry));

        // When
        Country result = geofencingService.getCountryById(spainId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(spainId);
        assertThat(result.getName()).isEqualTo("España");
        assertThat(result.getCode()).isEqualTo("ESP");

        verify(countryRepository).findById(spainId);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent country by ID")
    void shouldThrowExceptionWhenGettingNonExistentCountryById() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(countryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> geofencingService.getCountryById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id: " + nonExistentId);

        verify(countryRepository).findById(nonExistentId);
    }
}
