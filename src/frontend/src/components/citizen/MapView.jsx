import { useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

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

  // Get map center from env or use default
  const defaultCenter = [
    parseFloat(import.meta.env.VITE_MAP_CENTER_LAT) || 40.416775,
    parseFloat(import.meta.env.VITE_MAP_CENTER_LON) || -3.703790,
  ];

  useEffect(() => {
    // Initialize map if not already initialized
    if (!mapInstanceRef.current && mapRef.current) {
      const center = location ? [location.latitude, location.longitude] : defaultCenter;
      
      mapInstanceRef.current = L.map(mapRef.current).setView(center, zoom);

      // Add tile layer
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19,
      }).addTo(mapInstanceRef.current);
    }

    // Update marker position
    if (mapInstanceRef.current && location) {
      const { latitude, longitude } = location;
      const position = [latitude, longitude];

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
    }

    // Show geofence boundaries if requested
    if (mapInstanceRef.current && showGeofence) {
      const minLat = parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LAT);
      const maxLat = parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LAT);
      const minLon = parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LON);
      const maxLon = parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LON);

      if (!isNaN(minLat) && !isNaN(maxLat) && !isNaN(minLon) && !isNaN(maxLon)) {
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
      }
    }

    // Cleanup function
    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        markerRef.current = null;
        geofenceRef.current = null;
      }
    };
  }, [location, showGeofence, zoom, defaultCenter]);

  return (
    <div className="map-view">
      <div
        ref={mapRef}
        style={{
          width: '100%',
          height: height,
          borderRadius: '8px',
          border: '1px solid var(--border-color)',
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
