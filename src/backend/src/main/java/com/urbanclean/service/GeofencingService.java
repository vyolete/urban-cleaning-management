package com.urbanclean.service;

import com.urbanclean.entity.Country;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.exception.custom.ValidationException;
import com.urbanclean.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for validating coordinates against geofencing boundaries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeofencingService {

    @Value("${geofence.min-lat}")
    private Double minLatitude;

    @Value("${geofence.max-lat}")
    private Double maxLatitude;

    @Value("${geofence.min-lon}")
    private Double minLongitude;

    @Value("${geofence.max-lon}")
    private Double maxLongitude;

    private final CountryRepository countryRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * Validate coordinates against configured geofencing boundaries (legacy method)
     */
    public void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new ValidationException("Latitude and longitude are required");
        }

        // Validate latitude range (-90 to 90)
        if (latitude < -90 || latitude > 90) {
            throw new ValidationException(
                String.format("Invalid latitude: %.6f. Must be between -90 and 90", latitude)
            );
        }

        // Validate longitude range (-180 to 180)
        if (longitude < -180 || longitude > 180) {
            throw new ValidationException(
                String.format("Invalid longitude: %.6f. Must be between -180 and 180", longitude)
            );
        }

        // Validate against geofencing boundaries
        if (!isWithinBoundaries(latitude, longitude)) {
            throw new ValidationException(
                String.format("Coordinates (%.6f, %.6f) are outside the configured geofencing boundaries. " +
                    "Valid area: latitude [%.6f, %.6f], longitude [%.6f, %.6f]",
                    latitude, longitude, minLatitude, maxLatitude, minLongitude, maxLongitude)
            );
        }

        log.debug("Coordinates validated: ({}, {})", latitude, longitude);
    }

    /**
     * Validate coordinates against specific country boundaries
     */
    public void validateCoordinates(Double latitude, Double longitude, UUID countryId) {
        if (latitude == null || longitude == null) {
            throw new ValidationException("Latitude and longitude are required");
        }

        // Validate latitude range (-90 to 90)
        if (latitude < -90 || latitude > 90) {
            throw new ValidationException(
                String.format("Invalid latitude: %.6f. Must be between -90 and 90", latitude)
            );
        }

        // Validate longitude range (-180 to 180)
        if (longitude < -180 || longitude > 180) {
            throw new ValidationException(
                String.format("Invalid longitude: %.6f. Must be between -180 and 180", longitude)
            );
        }

        // If no country ID provided, use default country
        if (countryId == null) {
            Country defaultCountry = countryRepository.findByDefaultCountryTrue()
                    .orElseThrow(() -> new ResourceNotFoundException("No default country configured"));
            countryId = defaultCountry.getId();
        }

        // Get country and validate boundaries
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + countryId));

        if (!country.getEnabled()) {
            throw new ValidationException("Country " + country.getName() + " is not enabled");
        }

        // Validate against country boundaries
        if (!isWithinBoundaries(latitude, longitude, countryId)) {
            throw new ValidationException(
                String.format("Coordinates (%.6f, %.6f) are outside %s geofencing boundaries. " +
                    "Valid area: latitude [%s, %s], longitude [%s, %s]",
                    latitude, longitude, country.getName(),
                    country.getMinLat(), country.getMaxLat(),
                    country.getMinLon(), country.getMaxLon())
            );
        }

        log.debug("Coordinates validated for country {}: ({}, {})", country.getName(), latitude, longitude);
    }

    /**
     * Check if coordinates are within configured boundaries (legacy method)
     */
    public boolean isWithinBoundaries(Double latitude, Double longitude) {
        return latitude >= minLatitude && latitude <= maxLatitude &&
               longitude >= minLongitude && longitude <= maxLongitude;
    }

    /**
     * Check if coordinates are within country boundaries
     */
    public boolean isWithinBoundaries(Double latitude, Double longitude, UUID countryId) {
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + countryId));

        BigDecimal lat = BigDecimal.valueOf(latitude);
        BigDecimal lon = BigDecimal.valueOf(longitude);

        return lat.compareTo(country.getMinLat()) >= 0 && lat.compareTo(country.getMaxLat()) <= 0 &&
               lon.compareTo(country.getMinLon()) >= 0 && lon.compareTo(country.getMaxLon()) <= 0;
    }

    /**
     * Create a Point geometry from coordinates
     */
    public Point createPoint(Double latitude, Double longitude) {
        // Note: PostGIS uses (longitude, latitude) order for coordinates
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    /**
     * Get the geofencing boundary as a Polygon (legacy method)
     */
    public Polygon getBoundaryPolygon() {
        Coordinate[] coordinates = new Coordinate[]{
            new Coordinate(minLongitude, minLatitude),
            new Coordinate(maxLongitude, minLatitude),
            new Coordinate(maxLongitude, maxLatitude),
            new Coordinate(minLongitude, maxLatitude),
            new Coordinate(minLongitude, minLatitude) // Close the polygon
        };
        return geometryFactory.createPolygon(coordinates);
    }

    /**
     * Get the geofencing boundary as a Polygon for a specific country
     */
    public Polygon getBoundaryPolygon(UUID countryId) {
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + countryId));

        Coordinate[] coordinates = new Coordinate[]{
            new Coordinate(country.getMinLon().doubleValue(), country.getMinLat().doubleValue()),
            new Coordinate(country.getMaxLon().doubleValue(), country.getMinLat().doubleValue()),
            new Coordinate(country.getMaxLon().doubleValue(), country.getMaxLat().doubleValue()),
            new Coordinate(country.getMinLon().doubleValue(), country.getMaxLat().doubleValue()),
            new Coordinate(country.getMinLon().doubleValue(), country.getMinLat().doubleValue()) // Close the polygon
        };
        return geometryFactory.createPolygon(coordinates);
    }

    /**
     * Check if a point is within the geofencing boundary using PostGIS (legacy method)
     */
    public boolean isPointWithinBoundary(Point point) {
        Polygon boundary = getBoundaryPolygon();
        return boundary.contains(point);
    }

    /**
     * Check if a point is within the country boundary using PostGIS
     */
    public boolean isPointWithinBoundary(Point point, UUID countryId) {
        Polygon boundary = getBoundaryPolygon(countryId);
        return boundary.contains(point);
    }

    /**
     * Get country by ID
     */
    public Country getCountryById(UUID countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + countryId));
    }
}
