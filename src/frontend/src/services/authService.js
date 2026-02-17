import api from './api';

// Token refresh interval (check every minute)
let refreshInterval = null;

/**
 * Authentication service for user login, registration, and session management
 * Supports refresh tokens and automatic token renewal
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

      const { token, refreshToken, role, username: userName, expiresIn } = response.data;

      // Construct user object from response
      const user = {
        username: userName,
        role: role,
      };

      // Store tokens and user in localStorage
      if (token) {
        localStorage.setItem('token', token);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('user', JSON.stringify(user));
        localStorage.setItem('tokenExpiresAt', Date.now() + expiresIn);
      }

      // Start automatic token refresh
      this.startTokenRefresh();

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
   * Refresh access token using refresh token
   * @returns {Promise<Object>} New token pair
   */
  async refreshAccessToken() {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }

      const response = await api.post('/auth/refresh', {
        refreshToken,
      });

      const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data;

      // Update tokens in localStorage
      localStorage.setItem('token', accessToken);
      localStorage.setItem('refreshToken', newRefreshToken);
      localStorage.setItem('tokenExpiresAt', Date.now() + expiresIn);

      return { token: accessToken };
    } catch (error) {
      // If refresh fails, logout user
      console.error('Token refresh failed:', error);
      this.logout();
      // Redirect to login page
      window.location.href = '/login';
      throw error;
    }
  },

  /**
   * Start automatic token refresh
   * Checks every minute if token needs refresh (< 5 minutes remaining)
   */
  startTokenRefresh() {
    // Clear any existing interval
    this.stopTokenRefresh();

    // Check every minute
    refreshInterval = setInterval(async () => {
      const tokenExpiresAt = localStorage.getItem('tokenExpiresAt');
      
      if (!tokenExpiresAt) {
        this.stopTokenRefresh();
        return;
      }

      const expiresAt = parseInt(tokenExpiresAt, 10);
      const now = Date.now();
      const timeUntilExpiry = expiresAt - now;
      const fiveMinutes = 5 * 60 * 1000; // 5 minutes in milliseconds

      // If less than 5 minutes remaining, refresh token
      if (timeUntilExpiry < fiveMinutes && timeUntilExpiry > 0) {
        try {
          await this.refreshAccessToken();
          console.log('Token refreshed automatically');
        } catch (error) {
          console.error('Automatic token refresh failed:', error);
          this.stopTokenRefresh();
        }
      }
    }, 60000); // Check every minute
  },

  /**
   * Stop automatic token refresh
   */
  stopTokenRefresh() {
    if (refreshInterval) {
      clearInterval(refreshInterval);
      refreshInterval = null;
    }
  },

  /**
   * Logout current user
   * Clears tokens and user data from localStorage
   * Revokes tokens on backend
   */
  async logout() {
    try {
      const token = this.getToken();
      const refreshToken = localStorage.getItem('refreshToken');

      // Stop token refresh
      this.stopTokenRefresh();

      // Call backend logout endpoint
      if (token) {
        try {
          await api.post('/auth/logout', {
            refreshToken,
          }, {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });
        } catch (error) {
          console.error('Backend logout failed:', error);
          // Continue with local logout even if backend fails
        }
      }

      // Clear localStorage
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      localStorage.removeItem('tokenExpiresAt');
    } catch (error) {
      console.error('Logout error:', error);
      // Clear localStorage anyway
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      localStorage.removeItem('tokenExpiresAt');
    }
  },

  /**
   * Logout from all devices
   * Revokes all user sessions
   */
  async logoutAll() {
    try {
      const token = this.getToken();

      // Stop token refresh
      this.stopTokenRefresh();

      // Call backend logout-all endpoint
      if (token) {
        await api.post('/auth/logout-all', {}, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
      }

      // Clear localStorage
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      localStorage.removeItem('tokenExpiresAt');
    } catch (error) {
      console.error('Logout all error:', error);
      throw error;
    }
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
