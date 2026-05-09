import { useEffect, useRef, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { countryService } from '../../services';

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
function MapView({ location, countryId = null, showGeofence = false, height = '400px', zoom = 15 }) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markerRef = useRef(null);
  const geofenceRef = useRef(null);
  const [countryData, setCountryData] = useState(null);

  // Get map center from env or use default - memoized to prevent re-renders
  const defaultCenter = useMemo(() => [
    parseFloat(import.meta.env.VITE_MAP_CENTER_LAT) || 40.416775,
    parseFloat(import.meta.env.VITE_MAP_CENTER_LON) || -3.703790,
  ], []);

  // Load country data when countryId changes
  useEffect(() => {
    if (!countryId) {
      setCountryData(null);
      return;
    }

    const loadCountryData = async () => {
      try {
        console.log('[MapView] Loading country data for:', countryId);
        const country = await countryService.getCountryById(countryId);
        setCountryData(country);
        console.log('[MapView] Country data loaded:', country);
      } catch (error) {
        console.error('[MapView] Error loading country data:', error);
        setCountryData(null);
      }
    };

    loadCountryData();
  }, [countryId]);

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

    // Determine initial center: location > country center > default
    let center;
    if (location) {
      center = [location.latitude, location.longitude];
    } else if (countryData?.centerLatitude && countryData?.centerLongitude) {
      center = [countryData.centerLatitude, countryData.centerLongitude];
    } else {
      center = defaultCenter;
    }

    try {
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
  }, [location, countryData, defaultCenter, zoom]); // Initialize when location or country becomes available

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

  // Update geofence when showGeofence or countryData changes
  useEffect(() => {
    if (!mapInstanceRef.current) {
      return;
    }

    // Remove existing geofence
    if (geofenceRef.current) {
      mapInstanceRef.current.removeLayer(geofenceRef.current);
      geofenceRef.current = null;
    }

    if (!showGeofence) {
      return;
    }

    console.log('[MapView] Updating geofence...');

    // Try to use country boundaries first, fall back to env variables
    let minLat, maxLat, minLon, maxLon, boundaryName;

    if (countryData?.geofencingBoundary) {
      try {
        // Parse GeoJSON boundary
        const boundary = JSON.parse(countryData.geofencingBoundary);
        
        if (boundary.type === 'Polygon' && boundary.coordinates && boundary.coordinates[0]) {
          const coords = boundary.coordinates[0];
          
          // Calculate bounding box from polygon coordinates
          minLat = Math.min(...coords.map(c => c[1]));
          maxLat = Math.max(...coords.map(c => c[1]));
          minLon = Math.min(...coords.map(c => c[0]));
          maxLon = Math.max(...coords.map(c => c[0]));
          boundaryName = countryData.name;

          console.log('[MapView] Using country boundary:', { minLat, maxLat, minLon, maxLon });
        }
      } catch (error) {
        console.error('[MapView] Error parsing country boundary:', error);
      }
    }

    // Fall back to environment variables if country boundary not available
    if (!minLat || !maxLat || !minLon || !maxLon) {
      minLat = parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LAT);
      maxLat = parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LAT);
      minLon = parseFloat(import.meta.env.VITE_GEOFENCE_MIN_LON);
      maxLon = parseFloat(import.meta.env.VITE_GEOFENCE_MAX_LON);
      boundaryName = 'Área de servicio';

      if (isNaN(minLat) || isNaN(maxLat) || isNaN(minLon) || isNaN(maxLon)) {
        console.log('[MapView] Geofence coordinates not configured');
        return;
      }
    }

    try {
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
        .bindPopup(boundaryName);

      console.log('[MapView] Geofence updated successfully');
    } catch (error) {
      console.error('[MapView] Error updating geofence:', error);
    }
  }, [showGeofence, countryData]);

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
  countryId: PropTypes.string,
  showGeofence: PropTypes.bool,
  height: PropTypes.string,
  zoom: PropTypes.number,
};

export default MapView;
