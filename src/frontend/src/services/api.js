import axios from 'axios';

/**
 * Base Axios instance configured for the Urban Cleaning API
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds
});

/**
 * Request interceptor to attach JWT token to all requests
 */
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor to handle errors globally
 */
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      // Server responded with error status
      const { status, data } = error.response;

      switch (status) {
        case 401:
          // Unauthorized - clear token and redirect to login
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          window.location.href = '/login';
          break;

        case 403:
          // Forbidden - user doesn't have permission
          console.error('Access denied:', data.message);
          break;

        case 404:
          // Not found
          console.error('Resource not found:', data.message);
          break;

        case 429:
          // Too many requests - rate limit exceeded
          console.error('Rate limit exceeded. Please try again later.');
          break;

        case 500:
          // Server error
          console.error('Server error:', data.message);
          break;

        default:
          console.error('API error:', data.message || 'Unknown error');
      }

      // Return structured error
      return Promise.reject({
        status,
        message: data.message || 'An error occurred',
        errorCode: data.errorCode,
        details: data.details,
      });
    } else if (error.request) {
      // Request was made but no response received
      console.error('Network error: No response from server');
      return Promise.reject({
        status: 0,
        message: 'Network error. Please check your connection.',
      });
    } else {
      // Something else happened
      console.error('Request error:', error.message);
      return Promise.reject({
        status: 0,
        message: error.message || 'An unexpected error occurred',
      });
    }
  }
);

export default api;
