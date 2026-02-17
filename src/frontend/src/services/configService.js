import api from './api';

/**
 * Configuration service for managing algorithm weights (admin only)
 */
const configService = {
  /**
   * Get current algorithm configuration
   * @returns {Promise<Object>} Current configuration with weights
   */
  async getCurrentConfig() {
    try {
      const response = await api.get('/admin/config/algorithm-weights');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Update algorithm weights
   * @param {Object} weights - Weight configuration
   * @param {number} weights.weightCategory - Category weight (0-1)
   * @param {number} weights.weightZone - Zone weight (0-1)
   * @param {number} weights.weightTime - Time weight (0-1)
   * @param {number} weights.deduplicationDistanceMeters - Deduplication distance in meters
   * @param {number} weights.deduplicationTimeWindowHours - Deduplication time window in hours
   * @returns {Promise<Object>} Updated configuration
   */
  async updateWeights(weights) {
    try {
      const response = await api.put('/admin/config/algorithm-weights', weights);
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get configuration history
   * @returns {Promise<Array>} Array of historical configurations
   */
  async getConfigHistory() {
    try {
      const response = await api.get('/admin/config/algorithm-weights/history');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Validate weight values
   * @param {Object} weights - Weight configuration to validate
   * @returns {Object} Validation result { isValid, errors }
   */
  validateWeights(weights) {
    const errors = {};

    // Validate individual weights
    if (weights.weightCategory === undefined || weights.weightCategory === null) {
      errors.weightCategory = 'Category weight is required';
    } else if (weights.weightCategory <= 0 || weights.weightCategory > 1) {
      errors.weightCategory = 'Category weight must be between 0 and 1';
    }

    if (weights.weightZone === undefined || weights.weightZone === null) {
      errors.weightZone = 'Zone weight is required';
    } else if (weights.weightZone <= 0 || weights.weightZone > 1) {
      errors.weightZone = 'Zone weight must be between 0 and 1';
    }

    if (weights.weightTime === undefined || weights.weightTime === null) {
      errors.weightTime = 'Time weight is required';
    } else if (weights.weightTime <= 0 || weights.weightTime > 1) {
      errors.weightTime = 'Time weight must be between 0 and 1';
    }

    // Validate sum of weights (must equal 1.0 with tolerance)
    if (
      weights.weightCategory !== undefined &&
      weights.weightZone !== undefined &&
      weights.weightTime !== undefined
    ) {
      const sum = parseFloat(weights.weightCategory) + 
                   parseFloat(weights.weightZone) + 
                   parseFloat(weights.weightTime);
      const tolerance = 0.01;

      if (Math.abs(sum - 1.0) > tolerance) {
        errors.sum = `Weights must sum to 1.0 (current sum: ${sum.toFixed(3)})`;
      }
    }

    // Validate deduplication distance
    if (weights.deduplicationDistanceMeters === undefined || weights.deduplicationDistanceMeters === null) {
      errors.deduplicationDistanceMeters = 'Deduplication distance is required';
    } else if (weights.deduplicationDistanceMeters <= 0) {
      errors.deduplicationDistanceMeters = 'Deduplication distance must be positive';
    }

    // Validate deduplication time window
    if (weights.deduplicationTimeWindowHours === undefined || weights.deduplicationTimeWindowHours === null) {
      errors.deduplicationTimeWindowHours = 'Deduplication time window is required';
    } else if (weights.deduplicationTimeWindowHours <= 0) {
      errors.deduplicationTimeWindowHours = 'Deduplication time window must be positive';
    }

    return {
      isValid: Object.keys(errors).length === 0,
      errors,
    };
  },

  /**
   * Normalize weights to sum to 1.0
   * @param {Object} weights - Weight configuration
   * @returns {Object} Normalized weights
   */
  normalizeWeights(weights) {
    const sum = parseFloat(weights.weightCategory) + 
                parseFloat(weights.weightZone) + 
                parseFloat(weights.weightTime);

    if (sum === 0) return weights;

    return {
      ...weights,
      weightCategory: parseFloat((weights.weightCategory / sum).toFixed(3)),
      weightZone: parseFloat((weights.weightZone / sum).toFixed(3)),
      weightTime: parseFloat((weights.weightTime / sum).toFixed(3)),
    };
  },

  /**
   * Get default weight configuration
   * @returns {Object} Default weights
   */
  getDefaultWeights() {
    return {
      weightCategory: 0.40,
      weightZone: 0.35,
      weightTime: 0.25,
      deduplicationDistanceMeters: 50.0,
      deduplicationTimeWindowHours: 24,
    };
  },

  /**
   * Format weight for display (percentage)
   * @param {number} weight - Weight value (0-1)
   * @returns {string} Formatted percentage
   */
  formatWeightPercentage(weight) {
    return `${(weight * 100).toFixed(1)}%`;
  },
};

export default configService;
