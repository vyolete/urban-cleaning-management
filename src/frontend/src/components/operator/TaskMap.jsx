import { useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './TaskMap.css';

// Fix for default marker icons in bundled builds
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Stable module-level constant — prevents the init effect from re-firing on every render
const DEFAULT_CENTER = [
  parseFloat(import.meta.env.VITE_MAP_CENTER_LAT) || 40.416775,
  parseFloat(import.meta.env.VITE_MAP_CENTER_LON) || -3.703790,
];

// ─────────────────────────────────────────────────────────────────────────────

function TaskMap({ tasks, selectedTask, onTaskSelect, country }) {
  const mapRef            = useRef(null);   // DOM node Leaflet attaches to
  const mapInstanceRef    = useRef(null);   // L.Map instance
  const markersRef        = useRef({});     // task.id → L.Marker
  const selectedMarkerRef = useRef(null);   // temporary marker for out-of-list selection
  const countryLayerRef   = useRef(null);   // L.Rectangle for the country boundary
  const roRef             = useRef(null);   // ResizeObserver instance

  // ── helpers ──────────────────────────────────────────────────────────────

  const markerColor = (priority) => {
    if (priority >= 80) return '#dc3545';
    if (priority >= 60) return '#fd7e14';
    if (priority >= 40) return '#ffc107';
    return '#6c757d';
  };

  const makeIcon = (priority, selected = false) => {
    const color = markerColor(priority);
    const size  = selected ? 36 : 26;
    return L.divIcon({
      className: 'custom-marker',
      html: `<div style="
          background:${color};width:${size}px;height:${size}px;
          border-radius:50% 50% 50% 0;transform:rotate(-45deg);
          border:${selected ? '3px solid #000' : '2px solid #fff'};
          box-shadow:0 2px 6px rgba(0,0,0,0.35);">
          <span style="display:block;transform:rotate(45deg);color:#fff;
            font-weight:700;font-size:${Math.round(size * 0.38)}px;
            text-align:center;line-height:${size}px;">
            ${Math.round(priority)}
          </span></div>`,
      iconSize:     [size, size],
      iconAnchor:   [size / 2, size],
      popupAnchor:  [0, -size],
    });
  };

  const fmtDate = (s) => new Date(s).toLocaleDateString('es-ES', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });

  const stateLabel = (s) => ({ PENDIENTE: 'Pendiente', ASIGNADO: 'Asignado',
    EN_PROGRESO: 'En Progreso', RESUELTO: 'Resuelto' }[s] || s);

  // ── Effect 1: initialise Leaflet once, use ResizeObserver for invalidateSize
  useEffect(() => {
    if (!mapRef.current || mapInstanceRef.current) return;

    const map = L.map(mapRef.current, { center: DEFAULT_CENTER, zoom: 6 });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map);

    mapInstanceRef.current = map;

    // ResizeObserver fires exactly when the flex container resolves to real px —
    // more reliable than setTimeout / requestAnimationFrame for Leaflet sizing.
    const ro = new ResizeObserver(() => map.invalidateSize());
    ro.observe(mapRef.current);
    roRef.current = ro;

    return () => {
      ro.disconnect();
      map.remove();
      mapInstanceRef.current    = null;
      markersRef.current        = {};
      countryLayerRef.current   = null;
      selectedMarkerRef.current = null;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Effect 2: country boundary rectangle + zoom
  // CountryResponse fields: minLat, maxLat, minLon, maxLon, centerLat, centerLon
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;

    // Remove previous boundary
    if (countryLayerRef.current) {
      map.removeLayer(countryLayerRef.current);
      countryLayerRef.current = null;
    }

    if (!country) {
      // "All countries" — reset to world view; the task-marker effect handles fitBounds
      map.setView(DEFAULT_CENTER, 5);
      return;
    }

    const { minLat, maxLat, minLon, maxLon, centerLat, centerLon } = country;

    // Build a bounding box from the country's boundary columns
    if (minLat != null && maxLat != null && minLon != null && maxLon != null) {
      const sw = [parseFloat(minLat), parseFloat(minLon)];
      const ne = [parseFloat(maxLat), parseFloat(maxLon)];
      const bounds = L.latLngBounds(sw, ne);

      const rect = L.rectangle(bounds, {
        color:       '#3498db',
        weight:      2,
        fillColor:   '#3498db',
        fillOpacity: 0.07,
        dashArray:   '8 5',
      }).addTo(map);

      countryLayerRef.current = rect;
      map.fitBounds(bounds, { padding: [40, 40] });

    } else if (centerLat != null && centerLon != null) {
      // No bounding box available — centre on the country
      map.setView([parseFloat(centerLat), parseFloat(centerLon)], 6);
    }
  }, [country]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Effect 3: rebuild task markers when the task list changes
  // TaskResponse has flat latitude/longitude fields — NOT a nested location object
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || !tasks) return;

    // Clear existing task markers
    Object.values(markersRef.current).forEach((m) => map.removeLayer(m));
    markersRef.current = {};

    tasks.forEach((task) => {
      const lat = parseFloat(task.latitude);
      const lng = parseFloat(task.longitude);
      if (isNaN(lat) || isNaN(lng)) return;

      const score = parseFloat(task.priorityScore) || 0;

      const marker = L.marker([lat, lng], { icon: makeIcon(score, false) })
        .addTo(map);

      marker.bindPopup(`
        <div class="task-popup">
          <h3>${task.category}</h3>
          <p><strong>ID:</strong> ${task.id.substring(0, 8)}…</p>
          <p><strong>Estado:</strong> ${stateLabel(task.state)}</p>
          <p><strong>Prioridad:</strong> ${score.toFixed(1)}</p>
          ${task.duplicateCount > 0
            ? `<p><strong>Duplicados:</strong> ${task.duplicateCount}</p>` : ''}
          <p><strong>Fecha:</strong> ${fmtDate(task.createdAt)}</p>
          <button class="popup-btn" data-task-id="${task.id}">Ver Detalles</button>
        </div>`);

      marker.on('click', () => { if (onTaskSelect) onTaskSelect(task); });
      marker.on('popupopen', () => {
        const btn = document.querySelector(`[data-task-id="${task.id}"]`);
        if (btn) btn.addEventListener('click', () => { if (onTaskSelect) onTaskSelect(task); });
      });

      markersRef.current[task.id] = marker;
    });

    // Only fitBounds to task markers when no country boundary is showing
    if (!countryLayerRef.current) {
      const pts = tasks
        .filter((t) => !isNaN(parseFloat(t.latitude)) && !isNaN(parseFloat(t.longitude)))
        .map((t) => [parseFloat(t.latitude), parseFloat(t.longitude)]);
      if (pts.length > 0) {
        map.fitBounds(pts, { padding: [50, 50] });
      }
    }
  }, [tasks]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Effect 4: highlight selected task and pan to it
  // selectedTask also has flat latitude/longitude fields
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;

    // Clean up previous temporary marker
    if (selectedMarkerRef.current) {
      map.removeLayer(selectedMarkerRef.current);
      selectedMarkerRef.current = null;
    }

    // Deselect all existing markers
    tasks.forEach((task) => {
      const m = markersRef.current[task.id];
      if (m) m.setIcon(makeIcon(parseFloat(task.priorityScore) || 0, false));
    });

    const lat = parseFloat(selectedTask?.latitude);
    const lng = parseFloat(selectedTask?.longitude);
    if (isNaN(lat) || isNaN(lng)) return;

    const score    = parseFloat(selectedTask.priorityScore) || 0;
    const existing = markersRef.current[selectedTask.id];

    if (existing) {
      existing.setIcon(makeIcon(score, true));
      existing.openPopup();
    } else {
      // Task not in the current map list — add a temporary highlighted pin
      const tmp = L.marker([lat, lng], { icon: makeIcon(score, true) }).addTo(map);

      tmp.bindPopup(`
        <div class="task-popup">
          <h3>${selectedTask.category}</h3>
          <p><strong>ID:</strong> ${selectedTask.id.substring(0, 8)}…</p>
          <p><strong>Estado:</strong> ${stateLabel(selectedTask.state)}</p>
          <p><strong>Prioridad:</strong> ${score.toFixed(1)}</p>
        </div>`).openPopup();

      selectedMarkerRef.current = tmp;
    }

    map.setView([lat, lng], 15);
  }, [selectedTask]); // eslint-disable-line react-hooks/exhaustive-deps

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div className="task-map">
      <div className="map-header">
        <h3>Mapa de Tareas</h3>
        <div className="map-legend">
          {[
            { color: '#dc3545', label: 'Crítica (≥80)' },
            { color: '#fd7e14', label: 'Alta (60-79)' },
            { color: '#ffc107', label: 'Media (40-59)' },
            { color: '#6c757d', label: 'Baja (<40)'    },
          ].map(({ color, label }) => (
            <div key={label} className="legend-item">
              <span className="legend-color" style={{ backgroundColor: color }} />
              <span>{label}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Leaflet mounts here — sized entirely by CSS flex */}
      <div ref={mapRef} className="map-canvas" />

      {tasks && tasks.length > 0 && (
        <div className="map-footer">
          <p>Mostrando <strong>{tasks.length}</strong> tarea(s) en el mapa</p>
        </div>
      )}
    </div>
  );
}

TaskMap.propTypes = {
  // TaskResponse has flat latitude/longitude — no nested location object
  tasks: PropTypes.arrayOf(PropTypes.shape({
    id:            PropTypes.string.isRequired,
    latitude:      PropTypes.number,
    longitude:     PropTypes.number,
    category:      PropTypes.string.isRequired,
    state:         PropTypes.string.isRequired,
    priorityScore: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    duplicateCount: PropTypes.number,
    createdAt:     PropTypes.string.isRequired,
  })),
  selectedTask: PropTypes.object,
  onTaskSelect: PropTypes.func,
  country:      PropTypes.object,
};

export default TaskMap;
