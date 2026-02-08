import { useState, useEffect, useCallback } from 'react';

/**
 * Custom hook for accessing browser geolocation
 * @returns {Object} Geolocation state and methods
 */
function useGeolocation() {
  const [location, setLocation] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  /**
   * Get current position from browser
   */
  const getCurrentLocation = useCallback(() => {
    // Check if geolocation is supported
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser');
      return;
    }

    setLoading(true);
    setError(null);

    // Options for geolocation
    const options = {
      enableHighAccuracy: true, // Use GPS if available
      timeout: 10000, // 10 seconds timeout
      maximumAge: 0, // Don't use cached position
    };

    // Get current position
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude, accuracy } = position.coords;
        
        setLocation({
          latitude,
          longitude,
          accuracy,
          timestamp: position.timestamp,
        });
        setLoading(false);
        setError(null);
      },
      (err) => {
        let errorMessage = 'Unable to retrieve your location';

        switch (err.code) {
          case err.PERMISSION_DENIED:
            errorMessage = 'Location permission denied. Please enable location access in your browser settings.';
            break;
          case err.POSITION_UNAVAILABLE:
            errorMessage = 'Location information is unavailable. Please check your device settings.';
            break;
          case err.TIMEOUT:
            errorMessage = 'Location request timed out. Please try again.';
            break;
          default:
            errorMessage = `Error getting location: ${err.message}`;
        }

        setError(errorMessage);
        setLoading(false);
      },
      options
    );
  }, []);

  /**
   * Watch position continuously
   * Returns a cleanup function to stop watching
   */
  const watchPosition = useCallback(() => {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser');
      return null;
    }

    setLoading(true);
    setError(null);

    const options = {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0,
    };

    const watchId = navigator.geolocation.watchPosition(
      (position) => {
        const { latitude, longitude, accuracy } = position.coords;
        
        setLocation({
          latitude,
          longitude,
          accuracy,
          timestamp: position.timestamp,
        });
        setLoading(false);
        setError(null);
      },
      (err) => {
        let errorMessage = 'Unable to retrieve your location';

        switch (err.code) {
          case err.PERMISSION_DENIED:
            errorMessage = 'Location permission denied';
            break;
          case err.POSITION_UNAVAILABLE:
            errorMessage = 'Location information is unavailable';
            break;
          case err.TIMEOUT:
            errorMessage = 'Location request timed out';
            break;
          default:
            errorMessage = `Error: ${err.message}`;
        }

        setError(errorMessage);
        setLoading(false);
      },
      options
    );

    // Return cleanup function
    return () => {
      navigator.geolocation.clearWatch(watchId);
    };
  }, []);

  /**
   * Clear location data
   */
  const clearLocation = useCallback(() => {
    setLocation(null);
    setError(null);
    setLoading(false);
  }, []);

  /**
   * Check if location is within geofencing boundaries
   */
  const isWithinBounds = useCallback((lat, lon) => {
    const minLat = parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LAT);
    const maxLat = parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LAT);
    const minLon = parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LON);
    const maxLon = parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LON);

    return (
      lat >= minLat &&
      lat <= maxLat &&
      lon >= minLon &&
      lon <= maxLon
    );
  }, []);

  /**
   * Get geofencing boundaries
   */
  const getBounds = useCallback(() => {
    return {
      minLat: parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LAT),
      maxLat: parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LAT),
      minLon: parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LON),
      maxLon: parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LON),
    };
  }, []);

  /**
   * Calculate distance between two points (Haversine formula)
   * @param {number} lat1 - Latitude of point 1
   * @param {number} lon1 - Longitude of point 1
   * @param {number} lat2 - Latitude of point 2
   * @param {number} lon2 - Longitude of point 2
   * @returns {number} Distance in meters
   */
  const calculateDistance = useCallback((lat1, lon1, lat2, lon2) => {
    const R = 6371e3; // Earth's radius in meters
    const φ1 = (lat1 * Math.PI) / 180;
    const φ2 = (lat2 * Math.PI) / 180;
    const Δφ = ((lat2 - lat1) * Math.PI) / 180;
    const Δλ = ((lon2 - lon1) * Math.PI) / 180;

    const a =
      Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
      Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c; // Distance in meters
  }, []);

  return {
    location,
    error,
    loading,
    getCurrentLocation,
    watchPosition,
    clearLocation,
    isWithinBounds,
    getBounds,
    calculateDistance,
  };
}

export default useGeolocation;
