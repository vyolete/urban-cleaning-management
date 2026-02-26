import { useEffect, useRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import L from 'leaflet';
// Note: Leaflet CSS is imported globally in main.jsx

// Fix for default marker icons in Leaflet with bundlers
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

/**
 * Map view component for displaying location
 */
function MapView({ location, showGeofence = false, height = '400px', zoom = 15 }) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markerRef = useRef(null);
  const geofenceRef = useRef(null);

  // Get map center from env or use default - memoized to prevent re-renders
  const defaultCenter = useMemo(() => [
    parseFloat(import.meta.env.VITE_MAP_CENTER_LAT) || 40.416775,
    parseFloat(import.meta.env.VITE_MAP_CENTER_LON) || -3.703790,
  ], []);

  // Initialize map once when component mounts and location is available
  useEffect(() => {
    console.log('[MapView] Mount effect - mapRef.current:', !!mapRef.current, 'location:', location);
    
    if (!mapRef.current) {
      console.log('[MapView] mapRef.current is null, skipping initialization');
      return;
    }

    if (mapInstanceRef.current) {
      console.log('[MapView] Map already initialized');
      return;
    }

    // Wait for location before initializing
    if (!location) {
      console.log('[MapView] Waiting for location before initializing map');
      return;
    }

    try {
      const center = [location.latitude, location.longitude];
      console.log('[MapView] Creating map with center:', center);
      
      mapInstanceRef.current = L.map(mapRef.current).setView(center, zoom);

      // Add tile layer
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19,
      }).addTo(mapInstanceRef.current);

      console.log('[MapView] Map initialized successfully');
    } catch (error) {
      console.error('[MapView] Error initializing map:', error);
    }
  }, [location, zoom]); // Initialize when location becomes available

  // Update marker when location changes (after map is initialized)
  useEffect(() => {
    if (!mapInstanceRef.current || !location) {
      console.log('[MapView] Skipping marker update - map:', !!mapInstanceRef.current, 'location:', !!location);
      return;
    }

    console.log('[MapView] Updating marker with location:', location);

    const { latitude, longitude } = location;
    const position = [latitude, longitude];

    try {
      // Remove existing marker
      if (markerRef.current) {
        mapInstanceRef.current.removeLayer(markerRef.current);
      }

      // Add new marker
      markerRef.current = L.marker(position)
        .addTo(mapInstanceRef.current)
        .bindPopup('Tu ubicación')
        .openPopup();

      // Center map on location
      mapInstanceRef.current.setView(position, zoom);
      
      console.log('[MapView] Marker updated successfully');
    } catch (error) {
      console.error('[MapView] Error updating marker:', error);
    }
  }, [location, zoom]);

  // Update geofence when showGeofence changes
  useEffect(() => {
    if (!mapInstanceRef.current || !showGeofence) {
      return;
    }

    console.log('[MapView] Updating geofence...');

    const minLat = parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LAT);
    const maxLat = parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LAT);
    const minLon = parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LON);
    const maxLon = parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LON);

    if (isNaN(minLat) || isNaN(maxLat) || isNaN(minLon) || isNaN(maxLon)) {
      console.log('[MapView] Geofence coordinates not configured');
      return;
    }

    try {
      // Remove existing geofence
      if (geofenceRef.current) {
        mapInstanceRef.current.removeLayer(geofenceRef.current);
      }

      // Add geofence rectangle
      const bounds = [
        [minLat, minLon],
        [maxLat, maxLon],
      ];

      geofenceRef.current = L.rectangle(bounds, {
        color: '#3388ff',
        weight: 2,
        fillOpacity: 0.1,
      })
        .addTo(mapInstanceRef.current)
        .bindPopup('Área de servicio');

      console.log('[MapView] Geofence updated successfully');
    } catch (error) {
      console.error('[MapView] Error updating geofence:', error);
    }
  }, [showGeofence]);

  // Cleanup on unmount only
  useEffect(() => {
    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        markerRef.current = null;
        geofenceRef.current = null;
      }
    };
  }, []);

  return (
    <div className="map-view">
      <div
        ref={mapRef}
        style={{
          width: '100%',
          height: height,
          minHeight: '400px',
          borderRadius: '8px',
          border: '1px solid var(--border-color)',
          backgroundColor: '#f0f0f0',
        }}
      />
      {location && (
        <div className="map-info">
          <p className="coordinates">
            Coordenadas: {location.latitude.toFixed(6)}, {location.longitude.toFixed(6)}
          </p>
          {location.accuracy && (
            <p className="accuracy">
              Precisión: ±{Math.round(location.accuracy)}m
            </p>
          )}
        </div>
      )}
    </div>
  );
}

MapView.propTypes = {
  location: PropTypes.shape({
    latitude: PropTypes.number.isRequired,
    longitude: PropTypes.number.isRequired,
    accuracy: PropTypes.number,
  }),
  showGeofence: PropTypes.bool,
  height: PropTypes.string,
  zoom: PropTypes.number,
};

export default MapView;
