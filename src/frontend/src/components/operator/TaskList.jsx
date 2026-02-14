import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import taskService from '../../services/taskService';
import './TaskList.css';

/**
 * Task list component for operators to view and filter tasks
 */
function TaskList({ onTaskSelect, selectedTaskId }) {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    state: '',
    zone: '',
  });

  // Task states
  const states = [
    { value: '', label: 'Todos los estados' },
    { value: 'PENDIENTE', label: 'Pendiente' },
    { value: 'ASIGNADO', label: 'Asignado' },
    { value: 'EN_PROGRESO', label: 'En Progreso' },
    { value: 'RESUELTO', label: 'Resuelto' },
  ];

  // Geographic zones (example - should match backend configuration)
  const zones = [
    { value: '', label: 'Todas las zonas' },
    { value: 'CENTRO', label: 'Centro' },
    { value: 'NORTE', label: 'Norte' },
    { value: 'SUR', label: 'Sur' },
    { value: 'ESTE', label: 'Este' },
    { value: 'OESTE', label: 'Oeste' },
  ];

  /**
   * Load tasks from API
   */
  const loadTasks = async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await taskService.getTasks(filters);
      setTasks(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cargar las tareas');
      console.error('Error loading tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  // Load tasks on mount and when filters change
  useEffect(() => {
    loadTasks();
  }, [filters]);

  /**
   * Handle filter changes
   */
  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  /**
   * Handle task selection
   */
  const handleTaskClick = (task) => {
    if (onTaskSelect) {
      onTaskSelect(task);
    }
  };

  /**
   * Get state badge class
   */
  const getStateBadgeClass = (state) => {
    const stateClasses = {
      PENDIENTE: 'badge-pending',
      ASIGNADO: 'badge-assigned',
      EN_PROGRESO: 'badge-in-progress',
      RESUELTO: 'badge-resolved',
    };
    return stateClasses[state] || 'badge-default';
  };

  /**
   * Get priority badge class
   */
  const getPriorityBadgeClass = (priority) => {
    if (priority >= 80) return 'priority-critical';
    if (priority >= 60) return 'priority-high';
    if (priority >= 40) return 'priority-medium';
    return 'priority-low';
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
   * Format coordinates
   */
  const formatCoordinates = (location) => {
    if (!location) return 'N/A';
    return `${location.latitude?.toFixed(4)}, ${location.longitude?.toFixed(4)}`;
  };

  return (
    <div className="task-list">
      <div className="task-list-header">
        <h2>Lista de Tareas</h2>
        <button onClick={loadTasks} className="btn-refresh" title="Actualizar">
          ↻ Actualizar
        </button>
      </div>

      {/* Filters */}
      <div className="task-filters">
        <div className="filter-group">
          <label htmlFor="state-filter">Estado:</label>
          <select
            id="state-filter"
            name="state"
            value={filters.state}
            onChange={handleFilterChange}
          >
            {states.map((state) => (
              <option key={state.value} value={state.value}>
                {state.label}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="zone-filter">Zona:</label>
          <select
            id="zone-filter"
            name="zone"
            value={filters.zone}
            onChange={handleFilterChange}
          >
            {zones.map((zone) => (
              <option key={zone.value} value={zone.value}>
                {zone.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Loading State */}
      {loading && (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Cargando tareas...</p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="error-container">
          <p className="error">{error}</p>
          <button onClick={loadTasks} className="btn-retry">
            Reintentar
          </button>
        </div>
      )}

      {/* Tasks Table */}
      {!loading && !error && (
        <>
          {tasks.length === 0 ? (
            <div className="empty-state">
              <p>No se encontraron tareas con los filtros seleccionados</p>
            </div>
          ) : (
            <div className="table-container">
              <table className="tasks-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Ubicación</th>
                    <th>Categoría</th>
                    <th>Estado</th>
                    <th>Prioridad</th>
                    <th>Duplicados</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.map((task) => (
                    <tr
                      key={task.id}
                      onClick={() => handleTaskClick(task)}
                      className={`task-row ${
                        selectedTaskId === task.id ? 'selected' : ''
                      }`}
                    >
                      <td className="task-id">
                        <span className="id-short" title={task.id}>
                          {task.id.substring(0, 8)}...
                        </span>
                      </td>
                      <td className="task-location">
                        {formatCoordinates(task.location)}
                      </td>
                      <td className="task-category">{task.category}</td>
                      <td className="task-state">
                        <span className={`badge ${getStateBadgeClass(task.state)}`}>
                          {task.state}
                        </span>
                      </td>
                      <td className="task-priority">
                        <span
                          className={`priority-badge ${getPriorityBadgeClass(
                            task.priorityScore
                          )}`}
                        >
                          {task.priorityScore?.toFixed(1)}
                        </span>
                      </td>
                      <td className="task-duplicates">
                        {task.duplicateCount > 0 ? (
                          <span className="duplicate-badge">
                            {task.duplicateCount}
                          </span>
                        ) : (
                          '-'
                        )}
                      </td>
                      <td className="task-date">
                        {formatDate(task.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Task Count */}
          <div className="task-count">
            <p>
              Mostrando <strong>{tasks.length}</strong> tarea(s)
            </p>
          </div>
        </>
      )}
    </div>
  );
}

TaskList.propTypes = {
  onTaskSelect: PropTypes.func,
  selectedTaskId: PropTypes.string,
};

export default TaskList;
