import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import TaskList from '../components/operator/TaskList';
import TaskMap from '../components/operator/TaskMap';
import TaskDetail from '../components/operator/TaskDetail';
import AuditTimeline from '../components/operator/AuditTimeline';
import UserInfo from '../components/common/UserInfo';
import taskService from '../services/taskService';
import countryService from '../services/countryService';
import urbixRobot from '../assets/urbix-robot.png';
import './OperatorDashboard.css';

/**
 * Operator dashboard page - main interface for operators to manage tasks
 */
function OperatorDashboard() {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewMode, setViewMode] = useState('split'); // 'split', 'list', 'map'
  const [countries, setCountries] = useState([]);
  const [selectedCountryId, setSelectedCountryId] = useState('');

  // Full country object derived from the loaded list — no extra API call needed
  const selectedCountry = countries.find((c) => c.id === selectedCountryId) || null;

  // Load available countries for the filter
  useEffect(() => {
    countryService.getEnabledCountries()
      .then(setCountries)
      .catch(() => {}); // non-critical — filter just won't show
  }, []);

  /**
   * Load tasks for the map panel, filtered by selected country
   */
  const loadTasks = async (countryId) => {
    setLoading(true);
    setError(null);

    try {
      const data = await taskService.getTasks(countryId ? { countryId } : {});
      setTasks(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cargar las tareas');
      console.error('Error loading tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  // Reload map tasks whenever the country filter changes
  useEffect(() => {
    loadTasks(selectedCountryId);
  }, [selectedCountryId]);

  const handleCountryChange = (e) => {
    setSelectedCountryId(e.target.value);
    setSelectedTask(null); // clear selection when scope changes
  };

  /**
   * Handle task selection
   */
  const handleTaskSelect = (task) => {
    setSelectedTask(task);
  };

  /**
   * Handle task update (after state change)
   */
  const handleTaskUpdate = (updatedTask) => {
    // Update task in the list
    setTasks((prevTasks) =>
      prevTasks.map((task) =>
        task.id === updatedTask.id ? updatedTask : task
      )
    );

    // Update selected task
    setSelectedTask(updatedTask);
  };

  /**
   * Handle view mode change
   */
  const handleViewModeChange = (mode) => {
    setViewMode(mode);
  };

  return (
    <div className="operator-dashboard">
      {/* Header */}
      <div className="dashboard-header">
        <img 
          src={urbixRobot} 
          alt="Robot Urbix" 
          className="dashboard-robot"
        />
        <div className="header-content">
          <h1>Dashboard de Operadores</h1>
          <p className="subtitle">Gestión de Tareas de Limpieza Urbana</p>
        </div>

        {/* Country filter */}
        {countries.length > 1 && (
          <div className="country-filter">
            <label htmlFor="country-filter">País:</label>
            <select
              id="country-filter"
              value={selectedCountryId}
              onChange={handleCountryChange}
            >
              <option value="">Todos los países</option>
              {countries.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}{c.defaultCountry ? ' (predeterminado)' : ''}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* User Info with Logout */}
        <UserInfo />

        {/* View Mode Selector */}
        <div className="view-mode-selector">
          <button
            className={`view-btn ${viewMode === 'split' ? 'active' : ''}`}
            onClick={() => handleViewModeChange('split')}
            title="Vista dividida"
          >
            ⊞ Dividida
          </button>
          <button
            className={`view-btn ${viewMode === 'list' ? 'active' : ''}`}
            onClick={() => handleViewModeChange('list')}
            title="Solo lista"
          >
            ☰ Lista
          </button>
          <button
            className={`view-btn ${viewMode === 'map' ? 'active' : ''}`}
            onClick={() => handleViewModeChange('map')}
            title="Solo mapa"
          >
            🗺 Mapa
          </button>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="error-banner">
          <span className="error-icon">✕</span>
          <p>{error}</p>
          <button onClick={() => loadTasks(selectedCountryId)} className="btn-retry">
            Reintentar
          </button>
        </div>
      )}

      {/* Main Content */}
      <div className={`dashboard-content view-${viewMode}`}>
        {/* Left Panel - Task List */}
        {(viewMode === 'split' || viewMode === 'list') && (
          <div className="panel panel-list">
            <TaskList
              onTaskSelect={handleTaskSelect}
              selectedTaskId={selectedTask?.id}
              countryId={selectedCountryId || undefined}
            />
          </div>
        )}

        {/* Center Panel - Map */}
        {(viewMode === 'split' || viewMode === 'map') && (
          <div className="panel panel-map">
            <TaskMap
              tasks={tasks}
              selectedTask={selectedTask}
              onTaskSelect={handleTaskSelect}
              country={selectedCountry}
            />
          </div>
        )}

        {/* Right Panel - Task Detail & Audit */}
        {viewMode === 'split' && (
          <div className="panel panel-detail">
            <div className="detail-container">
              <TaskDetail
                task={selectedTask}
                onTaskUpdate={handleTaskUpdate}
              />
            </div>
            
            {selectedTask && (
              <div className="audit-container">
                <AuditTimeline taskId={selectedTask.id} />
              </div>
            )}
          </div>
        )}
      </div>

      {/* Task Detail Modal for List/Map Only Views */}
      {(viewMode === 'list' || viewMode === 'map') && selectedTask && (
        <div className="task-modal-overlay" onClick={() => setSelectedTask(null)}>
          <div className="task-modal" onClick={(e) => e.stopPropagation()}>
            <button
              className="modal-close"
              onClick={() => setSelectedTask(null)}
              aria-label="Cerrar"
            >
              ×
            </button>
            
            <div className="modal-content">
              <div className="modal-detail">
                <TaskDetail
                  task={selectedTask}
                  onTaskUpdate={handleTaskUpdate}
                />
              </div>
              
              <div className="modal-audit">
                <AuditTimeline taskId={selectedTask.id} />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Stats Footer */}
      <div className="dashboard-footer">
        <div className="stat-item">
          <span className="stat-label">Total de Tareas:</span>
          <span className="stat-value">{tasks.length}</span>
        </div>
        <div className="stat-item">
          <span className="stat-label">Pendientes:</span>
          <span className="stat-value">
            {tasks.filter((t) => t.state === 'PENDIENTE').length}
          </span>
        </div>
        <div className="stat-item">
          <span className="stat-label">En Progreso:</span>
          <span className="stat-value">
            {tasks.filter((t) => t.state === 'EN_PROGRESO').length}
          </span>
        </div>
        <div className="stat-item">
          <span className="stat-label">Resueltas:</span>
          <span className="stat-value">
            {tasks.filter((t) => t.state === 'RESUELTO').length}
          </span>
        </div>
      </div>
    </div>
  );
}

export default OperatorDashboard;
