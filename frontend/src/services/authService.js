import api from './api';

/**
 * Authentication service for user login, registration, and session management
 */
const authService = {
  /**
   * Login user with username and password
   * @param {string} username - User's username
   * @param {string} password - User's password
   * @returns {Promise<Object>} Login response with token and user data
   */
  async login(username, password) {
    try {
      const response = await api.post('/auth/login', {
        username,
        password,
      });

      console.log('Login response:', response.data);

      const { token, role, username: userName } = response.data;

      // Construct user object from response
      const user = {
        username: userName,
        role: role,
      };

      console.log('Constructed user object:', user);
      console.log('User role:', user.role);

      // Store token and user in localStorage
      if (token) {
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(user));
        console.log('Stored in localStorage - user:', localStorage.getItem('user'));
      }

      return { token, user };
    } catch (error) {
      throw error;
    }
  },

  /**
   * Register a new user
   * @param {Object} userData - User registration data
   * @param {string} userData.username - Username
   * @param {string} userData.password - Password
   * @param {string} userData.email - Email address
   * @param {string} userData.role - User role (ROLE_CIUDADANO, ROLE_TECNICO, ROLE_ADMIN)
   * @returns {Promise<Object>} Registration response
   */
  async register(userData) {
    try {
      const response = await api.post('/auth/register', userData);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Logout current user
   * Clears token and user data from localStorage
   */
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  /**
   * Get current user from localStorage
   * @returns {Object|null} Current user object or null if not logged in
   */
  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch (error) {
        console.error('Error parsing user data:', error);
        return null;
      }
    }
    return null;
  },

  /**
   * Get current JWT token
   * @returns {string|null} JWT token or null if not logged in
   */
  getToken() {
    return localStorage.getItem('token');
  },

  /**
   * Check if user is authenticated
   * @returns {boolean} True if user has a valid token
   */
  isAuthenticated() {
    const token = this.getToken();
    if (!token) return false;

    // Check if token is expired (basic check)
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiry = payload.exp * 1000; // Convert to milliseconds
      return Date.now() < expiry;
    } catch (error) {
      return false;
    }
  },

  /**
   * Check if current user has a specific role
   * @param {string} role - Role to check (e.g., 'ROLE_ADMIN')
   * @returns {boolean} True if user has the role
   */
  hasRole(role) {
    const user = this.getCurrentUser();
    return user && user.role === role;
  },

  /**
   * Check if current user has any of the specified roles
   * @param {string[]} roles - Array of roles to check
   * @returns {boolean} True if user has any of the roles
   */
  hasAnyRole(roles) {
    const user = this.getCurrentUser();
    return user && roles.includes(user.role);
  },
};

export default authService;
