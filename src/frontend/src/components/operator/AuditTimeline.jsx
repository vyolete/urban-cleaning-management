import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import taskService from '../../services/taskService';
import './AuditTimeline.css';

/**
 * Audit timeline component displaying state change history
 */
function AuditTimeline({ taskId }) {
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  /**
   * Load audit history
   */
  const loadAuditHistory = async () => {
    if (!taskId) {
      setAuditLogs([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await taskService.getAuditHistory(taskId);
      setAuditLogs(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cargar el historial');
      console.error('Error loading audit history:', err);
    } finally {
      setLoading(false);
    }
  };

  // Load audit history when taskId changes
  useEffect(() => {
    loadAuditHistory();
  }, [taskId]);

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
   * Get state color
   */
  const getStateColor = (state) => {
    const colors = {
      PENDIENTE: '#ffc107',
      ASIGNADO: '#0d6efd',
      EN_PROGRESO: '#0dcaf0',
      RESUELTO: '#198754',
    };
    return colors[state] || '#6c757d';
  };

  /**
   * Format date and time
   */
  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return {
      date: date.toLocaleDateString('es-ES', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      }),
      time: date.toLocaleTimeString('es-ES', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      }),
    };
  };

  /**
   * Calculate time difference
   */
  const getTimeDifference = (currentDate, previousDate) => {
    if (!previousDate) return null;

    const current = new Date(currentDate);
    const previous = new Date(previousDate);
    const diffMs = current - previous;

    const diffMinutes = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 0) {
      return `${diffDays} día(s)`;
    } else if (diffHours > 0) {
      return `${diffHours} hora(s)`;
    } else if (diffMinutes > 0) {
      return `${diffMinutes} minuto(s)`;
    } else {
      return 'menos de 1 minuto';
    }
  };

  if (!taskId) {
    return (
      <div className="audit-timeline empty">
        <div className="empty-state">
          <p>Seleccione una tarea para ver el historial</p>
        </div>
      </div>
    );
  }

  return (
    <div className="audit-timeline">
      <div className="timeline-header">
        <h3>Historial de Cambios</h3>
        <button onClick={loadAuditHistory} className="btn-refresh-small" title="Actualizar">
          ↻
        </button>
      </div>

      {/* Loading State */}
      {loading && (
        <div className="loading-container">
          <div className="spinner-small"></div>
          <p>Cargando historial...</p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="error-container">
          <p className="error">{error}</p>
          <button onClick={loadAuditHistory} className="btn-retry-small">
            Reintentar
          </button>
        </div>
      )}

      {/* Timeline */}
      {!loading && !error && (
        <>
          {auditLogs.length === 0 ? (
            <div className="empty-state">
              <p>No hay cambios registrados para esta tarea</p>
            </div>
          ) : (
            <div className="timeline">
              {auditLogs.map((log, index) => {
                const { date, time } = formatDateTime(log.changedAt);
                const timeDiff =
                  index < auditLogs.length - 1
                    ? getTimeDifference(log.changedAt, auditLogs[index + 1].changedAt)
                    : null;

                return (
                  <div key={log.id} className="timeline-item">
                    {/* Timeline Connector */}
                    {index < auditLogs.length - 1 && (
                      <div className="timeline-connector">
                        {timeDiff && (
                          <span className="time-diff">{timeDiff}</span>
                        )}
                      </div>
                    )}

                    {/* Timeline Dot */}
                    <div
                      className="timeline-dot"
                      style={{ backgroundColor: getStateColor(log.newState) }}
                    ></div>

                    {/* Timeline Content */}
                    <div className="timeline-content">
                      <div className="timeline-header-item">
                        <div className="state-transition">
                          {log.previousState && (
                            <>
                              <span className="state-badge previous">
                                {getStateLabel(log.previousState)}
                              </span>
                              <span className="arrow">→</span>
                            </>
                          )}
                          <span
                            className="state-badge current"
                            style={{
                              backgroundColor: getStateColor(log.newState),
                              color: 'white',
                            }}
                          >
                            {getStateLabel(log.newState)}
                          </span>
                        </div>
                        <div className="timeline-date">
                          <span className="date">{date}</span>
                          <span className="time">{time}</span>
                        </div>
                      </div>

                      {log.user && (
                        <div className="timeline-user">
                          <span className="user-icon">👤</span>
                          <span className="user-name">
                            {log.user.username || log.user.email || 'Usuario'}
                          </span>
                          {log.user.role && (
                            <span className="user-role">({log.user.role})</span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Log Count */}
          {auditLogs.length > 0 && (
            <div className="timeline-footer">
              <p>
                Total de cambios: <strong>{auditLogs.length}</strong>
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
}

AuditTimeline.propTypes = {
  taskId: PropTypes.string,
};

export default AuditTimeline;
