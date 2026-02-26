import { useState } from 'react';
import PropTypes from 'prop-types';
import { Icon } from '../common';
import taskService from '../../services/taskService';

/**
 * Task detail component showing full task information and state transitions
 */
function TaskDetail({ task, onTaskUpdate }) {
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  if (!task) {
    return (
      <div className="task-detail empty">
        <div className="empty-state">
          <p>Seleccione una tarea para ver los detalles</p>
        </div>
      </div>
    );
  }

  /**
   * Get available state transitions based on current state
   */
  const getAvailableTransitions = (currentState) => {
    const transitions = {
      PENDIENTE: ['ASIGNADO'],
      ASIGNADO: ['EN_PROGRESO'],
      EN_PROGRESO: ['RESUELTO'],
      RESUELTO: [],
    };
    return transitions[currentState] || [];
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

  /**
   * Get state button class
   */
  const getStateButtonClass = (state) => {
    const classes = {
      ASIGNADO: 'btn-assign',
      EN_PROGRESO: 'btn-progress',
      RESUELTO: 'btn-resolve',
    };
    return classes[state] || 'btn-default';
  };

  /**
   * Handle state transition
   */
  const handleStateTransition = async (newState) => {
    setUpdating(true);
    setError(null);
    setSuccess(null);

    try {
      const updatedTask = await taskService.updateTaskState(task.id, newState);
      setSuccess(`Estado actualizado a: ${getStateLabel(newState)}`);
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000);

      // Notify parent component
      if (onTaskUpdate) {
        onTaskUpdate(updatedTask);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error al actualizar el estado');
      console.error('Error updating task state:', err);
    } finally {
      setUpdating(false);
    }
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
   * Get priority badge class
   */
  const getPriorityBadgeClass = (priority) => {
    if (priority >= 80) return 'priority-critical';
    if (priority >= 60) return 'priority-high';
    if (priority >= 40) return 'priority-medium';
    return 'priority-low';
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

  const availableTransitions = getAvailableTransitions(task.state);

  return (
    <div className="task-detail">
      <div className="detail-header">
        <h2>Detalles de la Tarea</h2>
      </div>

      {/* Success Message */}
      {success && (
        <div className="success-message">
          <span className="success-icon"><Icon name="check" size="small" /></span>
          {success}
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="error-message">
          <span className="error-icon"><Icon name="close" size="small" /></span>
          {error}
        </div>
      )}

      {/* Task Info */}
      <div className="detail-section">
        <h3>Información General</h3>
        <div className="info-grid">
          <div className="info-item">
            <label>ID:</label>
            <span className="task-id-full" title={task.id}>
              {task.id}
            </span>
          </div>

          <div className="info-item">
            <label>Estado:</label>
            <span className={`badge ${getStateBadgeClass(task.state)}`}>
              {getStateLabel(task.state)}
            </span>
          </div>

          <div className="info-item">
            <label>Categoría:</label>
            <span className="category">{task.category}</span>
          </div>

          <div className="info-item">
            <label>Prioridad:</label>
            <span className={`priority-badge ${getPriorityBadgeClass(task.priorityScore)}`}>
              {task.priorityScore?.toFixed(2)}
            </span>
          </div>

          <div className="info-item">
            <label>Fecha de Creación:</label>
            <span>{formatDate(task.createdAt)}</span>
          </div>

          {task.updatedAt && (
            <div className="info-item">
              <label>Última Actualización:</label>
              <span>{formatDate(task.updatedAt)}</span>
            </div>
          )}
        </div>
      </div>

      {/* Location */}
      {task.location && (
        <div className="detail-section">
          <h3>Ubicación</h3>
          <div className="info-grid">
            <div className="info-item">
              <label>Latitud:</label>
              <span className="coordinate">{task.location.latitude?.toFixed(6)}</span>
            </div>
            <div className="info-item">
              <label>Longitud:</label>
              <span className="coordinate">{task.location.longitude?.toFixed(6)}</span>
            </div>
          </div>
        </div>
      )}

      {/* Duplicates */}
      {task.duplicateCount > 0 && (
        <div className="detail-section">
          <h3>Reportes Duplicados</h3>
          <div className="duplicate-info">
            <p>
              Esta tarea agrupa <strong>{task.duplicateCount}</strong> reporte(s) duplicado(s)
            </p>
            {task.mergedReports && task.mergedReports.length > 0 && (
              <div className="merged-reports">
                <h4>Reportes Fusionados:</h4>
                <ul>
                  {task.mergedReports.map((report) => (
                    <li key={report.id}>
                      <span className="report-id">{report.id.substring(0, 8)}...</span>
                      <span className="report-date">{formatDate(report.createdAt)}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}

      {/* State Transitions */}
      {availableTransitions.length > 0 && (
        <div className="detail-section">
          <h3>Acciones</h3>
          <div className="state-transitions">
            <p className="transition-label">Cambiar estado a:</p>
            <div className="transition-buttons">
              {availableTransitions.map((state) => (
                <button
                  key={state}
                  onClick={() => handleStateTransition(state)}
                  disabled={updating}
                  className={`btn-transition ${getStateButtonClass(state)}`}
                >
                  {updating ? 'Actualizando...' : getStateLabel(state)}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* No transitions available */}
      {availableTransitions.length === 0 && task.state === 'RESUELTO' && (
        <div className="detail-section">
          <div className="resolved-message">
            <span className="resolved-icon"><Icon name="check" size="medium" /></span>
            <p>Esta tarea ha sido resuelta</p>
          </div>
        </div>
      )}
    </div>
  );
}

TaskDetail.propTypes = {
  task: PropTypes.shape({
    id: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    category: PropTypes.string.isRequired,
    priorityScore: PropTypes.number.isRequired,
    duplicateCount: PropTypes.number,
    location: PropTypes.shape({
      latitude: PropTypes.number,
      longitude: PropTypes.number,
    }),
    createdAt: PropTypes.string.isRequired,
    updatedAt: PropTypes.string,
    mergedReports: PropTypes.array,
  }),
  onTaskUpdate: PropTypes.func,
};

export default TaskDetail;
