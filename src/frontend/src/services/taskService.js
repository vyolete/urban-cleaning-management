import api from './api';

/**
 * Task service for managing cleaning tasks (operator/admin)
 */
const taskService = {
  /**
   * Get tasks with optional filters
   * @param {Object} filters - Filter parameters
   * @param {string} filters.state - Task state (PENDIENTE, ASIGNADO, EN_PROGRESO, RESUELTO)
   * @param {number} filters.minLat - Minimum latitude for geographic filter
   * @param {number} filters.maxLat - Maximum latitude for geographic filter
   * @param {number} filters.minLon - Minimum longitude for geographic filter
   * @param {number} filters.maxLon - Maximum longitude for geographic filter
   * @returns {Promise<Array>} Array of tasks ordered by priority
   */
  async getTasks(filters = {}) {
    try {
      const params = new URLSearchParams();

      if (filters.state) {
        params.append('state', filters.state);
      }

      if (filters.minLat && filters.maxLat && filters.minLon && filters.maxLon) {
        params.append('minLat', filters.minLat);
        params.append('maxLat', filters.maxLat);
        params.append('minLon', filters.minLon);
        params.append('maxLon', filters.maxLon);
      }

      const queryString = params.toString();
      const url = queryString ? `/tasks?${queryString}` : '/tasks';

      const response = await api.get(url);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get task by ID
   * @param {string} id - Task UUID
   * @returns {Promise<Object>} Task object with full details
   */
  async getTaskById(id) {
    try {
      const response = await api.get(`/tasks/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Update task state
   * @param {string} id - Task UUID
   * @param {string} newState - New state (ASIGNADO, EN_PROGRESO, RESUELTO)
   * @returns {Promise<Object>} Updated task object
   */
  async updateTaskState(id, newState) {
    try {
      const response = await api.patch(`/tasks/${id}/state`, {
        newState,
      });
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get audit history for a task
   * @param {string} id - Task UUID
   * @returns {Promise<Array>} Array of audit log entries
   */
  async getAuditHistory(id) {
    try {
      const response = await api.get(`/tasks/${id}/audit-history`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get valid state transitions for current state
   * @param {string} currentState - Current task state
   * @returns {Array<string>} Array of valid next states
   */
  getValidTransitions(currentState) {
    const transitions = {
      PENDIENTE: ['ASIGNADO'],
      ASIGNADO: ['EN_PROGRESO'],
      EN_PROGRESO: ['RESUELTO'],
      RESUELTO: [],
    };

    return transitions[currentState] || [];
  },

  /**
   * Check if state transition is valid
   * @param {string} currentState - Current state
   * @param {string} newState - Desired new state
   * @returns {boolean} True if transition is valid
   */
  isValidTransition(currentState, newState) {
    const validTransitions = this.getValidTransitions(currentState);
    return validTransitions.includes(newState);
  },

  /**
   * Get state display name in Spanish
   * @param {string} state - State code
   * @returns {string} Display name
   */
  getStateDisplayName(state) {
    const names = {
      PENDIENTE: 'Pendiente',
      ASIGNADO: 'Asignado',
      EN_PROGRESO: 'En Progreso',
      RESUELTO: 'Resuelto',
    };

    return names[state] || state;
  },

  /**
   * Get state color for UI display
   * @param {string} state - State code
   * @returns {string} Color code
   */
  getStateColor(state) {
    const colors = {
      PENDIENTE: '#f39c12', // Orange
      ASIGNADO: '#3498db', // Blue
      EN_PROGRESO: '#9b59b6', // Purple
      RESUELTO: '#27ae60', // Green
    };

    return colors[state] || '#7f8c8d';
  },

  /**
   * Get priority color based on score
   * @param {number} priorityScore - Priority score
   * @returns {string} Color code
   */
  getPriorityColor(priorityScore) {
    if (priorityScore >= 8) return '#e74c3c'; // High - Red
    if (priorityScore >= 5) return '#f39c12'; // Medium - Orange
    return '#27ae60'; // Low - Green
  },

  /**
   * Format priority score for display
   * @param {number} priorityScore - Priority score
   * @returns {string} Formatted score with label
   */
  formatPriority(priorityScore) {
    const score = parseFloat(priorityScore).toFixed(2);
    if (priorityScore >= 8) return `${score} (Alta)`;
    if (priorityScore >= 5) return `${score} (Media)`;
    return `${score} (Baja)`;
  },
};

export default taskService;
