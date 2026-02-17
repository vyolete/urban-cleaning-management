package com.urbanclean.service;

import com.urbanclean.exception.custom.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for validating coordinates against geofencing boundaries
 */
@Service
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

    private final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * Validate coordinates against configured geofencing boundaries
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
     * Check if coordinates are within configured boundaries
     */
    public boolean isWithinBoundaries(Double latitude, Double longitude) {
        return latitude >= minLatitude && latitude <= maxLatitude &&
               longitude >= minLongitude && longitude <= maxLongitude;
    }

    /**
     * Create a Point geometry from coordinates
     */
    public Point createPoint(Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        // Note: PostGIS uses (longitude, latitude) order for coordinates
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    /**
     * Get the geofencing boundary as a Polygon
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
     * Check if a point is within the geofencing boundary using PostGIS
     */
    public boolean isPointWithinBoundary(Point point) {
        Polygon boundary = getBoundaryPolygon();
        return boundary.contains(point);
    }
}
