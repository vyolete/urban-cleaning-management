import { createContext, useContext, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import authService from '../services/authService';

/**
 * Authentication Context
 * Provides global authentication state and methods
 */
const AuthContext = createContext(null);

/**
 * Authentication Provider Component
 * Wraps the application to provide authentication state
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  /**
   * Initialize authentication state from localStorage
   */
  useEffect(() => {
    const initAuth = () => {
      try {
        const storedToken = authService.getToken();
        const storedUser = authService.getCurrentUser();

        if (storedToken && authService.isAuthenticated()) {
          setToken(storedToken);
          setUser(storedUser);
          
          // Start automatic token refresh
          authService.startTokenRefresh();
        } else {
          // Token expired or invalid, clear storage
          authService.logout();
        }
      } catch (error) {
        console.error('Error initializing auth:', error);
        authService.logout();
      } finally {
        setLoading(false);
      }
    };

    initAuth();

    // Cleanup: stop token refresh on unmount
    return () => {
      authService.stopTokenRefresh();
    };
  }, []);

  /**
   * Login user with credentials
   * @param {string} username - Username
   * @param {string} password - Password
   * @returns {Promise<Object>} Login response
   */
  const login = async (username, password) => {
    try {
      setLoading(true);
      setError(null);

      const response = await authService.login(username, password);
      
      setToken(response.token);
      setUser(response.user);

      return response;
    } catch (error) {
      setError(error.message || 'Login failed');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  /**
   * Register new user
   * @param {Object} userData - User registration data
   * @returns {Promise<Object>} Registration response
   */
  const register = async (userData) => {
    try {
      setLoading(true);
      setError(null);

      const response = await authService.register(userData);
      
      // Optionally auto-login after registration
      // For now, just return the response
      return response;
    } catch (error) {
      setError(error.message || 'Registration failed');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  /**
   * Logout current user
   */
  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setToken(null);
      setUser(null);
      setError(null);
    }
  };

  /**
   * Check if user is authenticated
   * @returns {boolean}
   */
  const isAuthenticated = () => {
    return token !== null && authService.isAuthenticated();
  };

  /**
   * Check if user has specific role
   * @param {string} role - Role to check
   * @returns {boolean}
   */
  const hasRole = (role) => {
    return user && user.role === role;
  };

  /**
   * Check if user has any of the specified roles
   * @param {string[]} roles - Array of roles to check
   * @returns {boolean}
   */
  const hasAnyRole = (roles) => {
    return user && roles.includes(user.role);
  };

  /**
   * Get user's display name
   * @returns {string}
   */
  const getUserDisplayName = () => {
    if (!user) return '';
    return user.username || user.email || 'Usuario';
  };

  /**
   * Get user's role display name
   * @returns {string}
   */
  const getRoleDisplayName = () => {
    if (!user) return '';
    
    const roleNames = {
      ROLE_CIUDADANO: 'Ciudadano',
      ROLE_TECNICO: 'Técnico',
      ROLE_ADMIN: 'Administrador',
    };

    return roleNames[user.role] || user.role;
  };

  /**
   * Clear error state
   */
  const clearError = () => {
    setError(null);
  };

  // Context value
  const value = {
    // State
    user,
    token,
    loading,
    error,
    
    // Methods
    login,
    register,
    logout,
    isAuthenticated,
    hasRole,
    hasAnyRole,
    getUserDisplayName,
    getRoleDisplayName,
    clearError,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

AuthProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

/**
 * Custom hook to use authentication context
 * @returns {Object} Authentication context value
 * @throws {Error} If used outside AuthProvider
 */
export function useAuth() {
  const context = useContext(AuthContext);
  
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  
  return context;
}

export default AuthContext;
