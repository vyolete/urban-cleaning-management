import api from './api';

/**
 * Report service for managing citizen reports
 */
const reportService = {
  /**
   * Submit a new report with photo
   * @param {Object} reportData - Report data
   * @param {number} reportData.latitude - Latitude coordinate
   * @param {number} reportData.longitude - Longitude coordinate
   * @param {string} reportData.category - Report category
   * @param {string} reportData.description - Report description
   * @param {File} photo - Photo file (JPEG or PNG, max 5MB)
   * @returns {Promise<Object>} Created report response
   */
  async submitReport(reportData, photo) {
    try {
      // Create FormData for multipart request
      const formData = new FormData();
      
      // Add JSON data as a blob
      const dataBlob = new Blob([JSON.stringify(reportData)], {
        type: 'application/json',
      });
      formData.append('data', dataBlob);
      
      // Add photo file
      formData.append('photo', photo);

      const response = await api.post('/reports', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get all reports (admin/operator only)
   * @returns {Promise<Array>} Array of reports
   */
  async getAllReports() {
    try {
      const response = await api.get('/reports');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get report by ID
   * @param {string} id - Report UUID
   * @returns {Promise<Object>} Report object
   */
  async getReportById(id) {
    try {
      const response = await api.get(`/reports/${id}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get current user's reports
   * @returns {Promise<Array>} Array of user's reports
   */
  async getMyReports() {
    try {
      const response = await api.get('/reports/my');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Validate report data before submission
   * @param {Object} reportData - Report data to validate
   * @param {File} photo - Photo file to validate
   * @returns {Object} Validation result { isValid, errors }
   */
  validateReport(reportData, photo) {
    const errors = {};

    // Validate country
    if (!reportData.countryId) {
      errors.countryId = 'Country is required';
    }

    // Validate coordinates
    if (!reportData.latitude || !reportData.longitude) {
      errors.location = 'Location is required';
    } else {
      if (reportData.latitude < -90 || reportData.latitude > 90) {
        errors.latitude = 'Invalid latitude';
      }
      if (reportData.longitude < -180 || reportData.longitude > 180) {
        errors.longitude = 'Invalid longitude';
      }
    }

    // Validate category
    if (!reportData.category || reportData.category.trim() === '') {
      errors.category = 'Category is required';
    }

    // Validate description
    if (!reportData.description || reportData.description.trim() === '') {
      errors.description = 'Description is required';
    } else if (reportData.description.length < 10) {
      errors.description = 'Description must be at least 10 characters';
    }

    // Validate photo
    if (!photo) {
      errors.photo = 'Photo is required';
    } else {
      // Check file type
      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png'];
      if (!allowedTypes.includes(photo.type)) {
        errors.photo = 'Photo must be JPEG or PNG';
      }

      // Check file size (max 5MB)
      const maxSize = 5 * 1024 * 1024;
      if (photo.size > maxSize) {
        errors.photo = 'Photo size must not exceed 5MB';
      }
    }

    return {
      isValid: Object.keys(errors).length === 0,
      errors,
    };
  },
};

export default reportService;
