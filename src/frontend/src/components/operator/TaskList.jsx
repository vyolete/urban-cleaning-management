import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import taskService from '../../services/taskService';
import './TaskList.css';

const PAGE_SIZE = 10;

function TaskList({ onTaskSelect, selectedTaskId, countryId }) {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({ state: '', zone: '' });
  const [currentPage, setCurrentPage] = useState(1);

  const states = [
    { value: '', label: 'Todos los estados' },
    { value: 'PENDIENTE', label: 'Pendiente' },
    { value: 'ASIGNADO', label: 'Asignado' },
    { value: 'EN_PROGRESO', label: 'En Progreso' },
    { value: 'RESUELTO', label: 'Resuelto' },
  ];

  const zones = [
    { value: '', label: 'Todas las zonas' },
    { value: 'CENTRO', label: 'Centro' },
    { value: 'NORTE', label: 'Norte' },
    { value: 'SUR', label: 'Sur' },
    { value: 'ESTE', label: 'Este' },
    { value: 'OESTE', label: 'Oeste' },
  ];

  const loadTasks = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await taskService.getTasks({ ...filters, countryId });
      setTasks(data);
      setCurrentPage(1);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cargar las tareas');
      console.error('Error loading tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTasks();
  }, [filters, countryId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const handleTaskClick = (task) => {
    if (onTaskSelect) onTaskSelect(task);
  };

  const getStateBadgeClass = (state) => ({
    PENDIENTE: 'badge-pending',
    ASIGNADO: 'badge-assigned',
    EN_PROGRESO: 'badge-in-progress',
    RESUELTO: 'badge-resolved',
  }[state] || 'badge-default');

  const getPriorityBadgeClass = (priority) => {
    if (priority >= 80) return 'priority-critical';
    if (priority >= 60) return 'priority-high';
    if (priority >= 40) return 'priority-medium';
    return 'priority-low';
  };

  const formatDate = (dateString) =>
    new Date(dateString).toLocaleDateString('es-ES', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });

  // TaskResponse has flat latitude/longitude fields (not nested under location)
  const formatCoordinates = (task) => {
    const lat = parseFloat(task.latitude);
    const lng = parseFloat(task.longitude);
    if (isNaN(lat) || isNaN(lng)) return 'N/A';
    return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
  };

  // Pagination
  const totalPages = Math.max(1, Math.ceil(tasks.length / PAGE_SIZE));
  const pageStart = (currentPage - 1) * PAGE_SIZE;
  const pageTasks = tasks.slice(pageStart, pageStart + PAGE_SIZE);

  const goToPage = (p) => setCurrentPage(Math.min(Math.max(1, p), totalPages));

  // Show at most 5 page number buttons centred around currentPage
  const pageNumbers = (() => {
    const half = 2;
    let start = Math.max(1, currentPage - half);
    let end = Math.min(totalPages, start + 4);
    start = Math.max(1, end - 4);
    const nums = [];
    for (let i = start; i <= end; i++) nums.push(i);
    return nums;
  })();

  return (
    <div className="task-list">
      {/* Header */}
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
          <select id="state-filter" name="state" value={filters.state} onChange={handleFilterChange}>
            {states.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </div>
        <div className="filter-group">
          <label htmlFor="zone-filter">Zona:</label>
          <select id="zone-filter" name="zone" value={filters.zone} onChange={handleFilterChange}>
            {zones.map((z) => <option key={z.value} value={z.value}>{z.label}</option>)}
          </select>
        </div>
      </div>

      {/* Loading */}
      {loading && (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Cargando tareas...</p>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="error-container">
          <p className="error">{error}</p>
          <button onClick={loadTasks} className="btn-retry">Reintentar</button>
        </div>
      )}

      {/* Table */}
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
                    <th>Dupl.</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {pageTasks.map((task) => (
                    <tr
                      key={task.id}
                      onClick={() => handleTaskClick(task)}
                      className={`task-row ${selectedTaskId === task.id ? 'selected' : ''}`}
                    >
                      <td className="task-id">
                        <span className="id-short" title={task.id}>
                          {task.id.substring(0, 8)}…
                        </span>
                      </td>
                      <td className="task-location">{formatCoordinates(task)}</td>
                      <td className="task-category">{task.category}</td>
                      <td className="task-state">
                        <span className={`badge ${getStateBadgeClass(task.state)}`}>
                          {task.state}
                        </span>
                      </td>
                      <td className="task-priority">
                        <span className={`priority-badge ${getPriorityBadgeClass(task.priorityScore)}`}>
                          {task.priorityScore?.toFixed(1)}
                        </span>
                      </td>
                      <td className="task-duplicates">
                        {task.duplicateCount > 0
                          ? <span className="duplicate-badge">{task.duplicateCount}</span>
                          : '-'}
                      </td>
                      <td className="task-date">{formatDate(task.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {tasks.length > PAGE_SIZE && (
            <div className="pagination">
              <button
                className="page-btn"
                onClick={() => goToPage(currentPage - 1)}
                disabled={currentPage === 1}
                aria-label="Anterior"
              >
                ‹
              </button>

              {pageNumbers[0] > 1 && (
                <>
                  <button className="page-btn" onClick={() => goToPage(1)}>1</button>
                  {pageNumbers[0] > 2 && <span className="page-ellipsis">…</span>}
                </>
              )}

              {pageNumbers.map((n) => (
                <button
                  key={n}
                  className={`page-btn ${n === currentPage ? 'active' : ''}`}
                  onClick={() => goToPage(n)}
                >
                  {n}
                </button>
              ))}

              {pageNumbers[pageNumbers.length - 1] < totalPages && (
                <>
                  {pageNumbers[pageNumbers.length - 1] < totalPages - 1 && (
                    <span className="page-ellipsis">…</span>
                  )}
                  <button className="page-btn" onClick={() => goToPage(totalPages)}>
                    {totalPages}
                  </button>
                </>
              )}

              <button
                className="page-btn"
                onClick={() => goToPage(currentPage + 1)}
                disabled={currentPage === totalPages}
                aria-label="Siguiente"
              >
                ›
              </button>

              <span className="page-info">
                {pageStart + 1}–{Math.min(pageStart + PAGE_SIZE, tasks.length)} de {tasks.length}
              </span>
            </div>
          )}

          {/* Count (when no pagination shown) */}
          {tasks.length <= PAGE_SIZE && (
            <div className="task-count">
              <p>Mostrando <strong>{tasks.length}</strong> tarea(s)</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}

TaskList.propTypes = {
  onTaskSelect: PropTypes.func,
  selectedTaskId: PropTypes.string,
  countryId: PropTypes.string,
};

export default TaskList;
