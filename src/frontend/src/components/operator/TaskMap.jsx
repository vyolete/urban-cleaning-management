import { useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './TaskMap.css';

// Fix for default marker icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

/**
 * Task map component for visualizing tasks on a map
 */
function TaskMap({ tasks, selectedTask, onTaskSelect, height = '600px' }) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersRef = useRef({});

  // Get map center from env or use default
  const defaultCenter = [
    parseFloat(import.meta.env.VITE_MAP_CENTER_LAT) || 40.416775,
    parseFloat(import.meta.env.VITE_MAP_CENTER_LON) || -3.703790,
  ];

  /**
   * Get marker color based on priority
   */
  const getMarkerColor = (priority) => {
    if (priority >= 80) return '#dc3545'; // Critical - Red
    if (priority >= 60) return '#fd7e14'; // High - Orange
    if (priority >= 40) return '#ffc107'; // Medium - Yellow
    return '#6c757d'; // Low - Gray
  };

  /**
   * Create custom marker icon
   */
  const createMarkerIcon = (priority, isSelected = false) => {
    const color = getMarkerColor(priority);
    const size = isSelected ? 35 : 25;
    
    return L.divIcon({
      className: 'custom-marker',
      html: `
        <div style="
          background-color: ${color};
          width: ${size}px;
          height: ${size}px;
          border-radius: 50% 50% 50% 0;
          transform: rotate(-45deg);
          border: 3px solid white;
          box-shadow: 0 2px 5px rgba(0,0,0,0.3);
          ${isSelected ? 'border-color: #000; border-width: 4px;' : ''}
        ">
          <div style="
            transform: rotate(45deg);
            color: white;
            font-weight: bold;
            font-size: ${size * 0.5}px;
            text-align: center;
            line-height: ${size}px;
          ">
            ${Math.round(priority)}
          </div>
        </div>
      `,
      iconSize: [size, size],
      iconAnchor: [size / 2, size],
      popupAnchor: [0, -size],
    });
  };

  /**
   * Format date
   */
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  /**
   * Get state label in Spanish
   */
  const getStateLabel = (state) => {
    const labels = {
      PENDIENTE: 'Pendiente',
      ASIGNADO: 'Asignado',
      EN_PROGRESO: 'En Progreso',
      RESUELTO: 'Resuelto',
    };
    return labels[state] || state;
  };

  // Initialize map
  useEffect(() => {
    if (!mapInstanceRef.current && mapRef.current) {
      mapInstanceRef.current = L.map(mapRef.current).setView(defaultCenter, 13);

      // Add tile layer
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19,
      }).addTo(mapInstanceRef.current);
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        markersRef.current = {};
      }
    };
  }, [defaultCenter]);

  // Update markers when tasks change
  useEffect(() => {
    if (!mapInstanceRef.current || !tasks) return;

    // Clear existing markers
    Object.values(markersRef.current).forEach((marker) => {
      mapInstanceRef.current.removeLayer(marker);
    });
    markersRef.current = {};

    // Add new markers
    tasks.forEach((task) => {
      if (!task.location || !task.location.latitude || !task.location.longitude) {
        return;
      }

      const { latitude, longitude } = task.location;
      const position = [latitude, longitude];
      const isSelected = selectedTask && selectedTask.id === task.id;

      const marker = L.marker(position, {
        icon: createMarkerIcon(task.priorityScore, isSelected),
      }).addTo(mapInstanceRef.current);

      // Create popup content
      const popupContent = `
        <div class="task-popup">
          <h3>${task.category}</h3>
          <p><strong>ID:</strong> ${task.id.substring(0, 8)}...</p>
          <p><strong>Estado:</strong> ${getStateLabel(task.state)}</p>
          <p><strong>Prioridad:</strong> ${task.priorityScore.toFixed(1)}</p>
          ${task.duplicateCount > 0 ? `<p><strong>Duplicados:</strong> ${task.duplicateCount}</p>` : ''}
          <p><strong>Fecha:</strong> ${formatDate(task.createdAt)}</p>
          <button class="popup-btn" data-task-id="${task.id}">Ver Detalles</button>
        </div>
      `;

      marker.bindPopup(popupContent);

      // Handle marker click
      marker.on('click', () => {
        if (onTaskSelect) {
          onTaskSelect(task);
        }
      });

      // Handle popup button click
      marker.on('popupopen', () => {
        const button = document.querySelector(`[data-task-id="${task.id}"]`);
        if (button) {
          button.addEventListener('click', () => {
            if (onTaskSelect) {
              onTaskSelect(task);
            }
          });
        }
      });

      markersRef.current[task.id] = marker;
    });

    // Fit bounds to show all markers
    if (tasks.length > 0) {
      const bounds = tasks
        .filter((t) => t.location && t.location.latitude && t.location.longitude)
        .map((t) => [t.location.latitude, t.location.longitude]);
      
      if (bounds.length > 0) {
        mapInstanceRef.current.fitBounds(bounds, { padding: [50, 50] });
      }
    }
  }, [tasks, selectedTask, onTaskSelect]);

  // Update selected marker when selectedTask changes
  useEffect(() => {
    if (!mapInstanceRef.current || !selectedTask) return;

    // Update all markers
    tasks.forEach((task) => {
      const marker = markersRef.current[task.id];
      if (marker) {
        const isSelected = task.id === selectedTask.id;
        marker.setIcon(createMarkerIcon(task.priorityScore, isSelected));
        
        // Center map on selected task
        if (isSelected && task.location) {
          mapInstanceRef.current.setView(
            [task.location.latitude, task.location.longitude],
            15
          );
          marker.openPopup();
        }
      }
    });
  }, [selectedTask, tasks]);

  return (
    <div className="task-map">
      <div className="map-header">
        <h3>Mapa de Tareas</h3>
        <div className="map-legend">
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#dc3545' }}></span>
            <span>Crítica (≥80)</span>
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#fd7e14' }}></span>
            <span>Alta (60-79)</span>
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#ffc107' }}></span>
            <span>Media (40-59)</span>
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#6c757d' }}></span>
            <span>Baja (&lt;40)</span>
          </div>
        </div>
      </div>
      <div
        ref={mapRef}
        style={{
          width: '100%',
          height: height,
          borderRadius: '8px',
          border: '1px solid var(--border-color)',
        }}
      />
      {tasks && tasks.length > 0 && (
        <div className="map-footer">
          <p>Mostrando {tasks.length} tarea(s) en el mapa</p>
        </div>
      )}
    </div>
  );
}

TaskMap.propTypes = {
  tasks: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      location: PropTypes.shape({
        latitude: PropTypes.number.isRequired,
        longitude: PropTypes.number.isRequired,
      }),
      category: PropTypes.string.isRequired,
      state: PropTypes.string.isRequired,
      priorityScore: PropTypes.number.isRequired,
      duplicateCount: PropTypes.number,
      createdAt: PropTypes.string.isRequired,
    })
  ),
  selectedTask: PropTypes.object,
  onTaskSelect: PropTypes.func,
  height: PropTypes.string,
};

export default TaskMap;
