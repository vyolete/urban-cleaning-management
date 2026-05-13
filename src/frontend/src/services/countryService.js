import api from './api';

/**
 * Country Service
 * Handles all country-related API calls
 */

/**
 * Get all countries (admin only)
 * @returns {Promise<Array>} List of all countries
 */
export const getAllCountries = async () => {
  try {
    const response = await api.get('/admin/countries');
    return response.data;
  } catch (error) {
    console.error('Error fetching all countries:', error);
    throw error;
  }
};

/**
 * Get enabled countries (accessible by all authenticated users)
 * @returns {Promise<Array>} List of enabled countries
 */
export const getEnabledCountries = async () => {
  try {
    const response = await api.get('/admin/countries/enabled');
    return response.data;
  } catch (error) {
    console.error('Error fetching enabled countries:', error);
    throw error;
  }
};

/**
 * Get default country
 * @returns {Promise<Object>} Default country object
 */
export const getDefaultCountry = async () => {
  try {
    const response = await api.get('/admin/countries/default');
    return response.data;
  } catch (error) {
    console.error('Error fetching default country:', error);
    throw error;
  }
};

/**
 * Get country by ID
 * @param {string} id - Country ID
 * @returns {Promise<Object>} Country object
 */
export const getCountryById = async (id) => {
  try {
    const response = await api.get(`/admin/countries/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching country ${id}:`, error);
    throw error;
  }
};

/**
 * Create a new country (admin only)
 * @param {Object} countryData - Country data
 * @returns {Promise<Object>} Created country object
 */
export const createCountry = async (countryData) => {
  try {
    const response = await api.post('/admin/countries', countryData);
    return response.data;
  } catch (error) {
    console.error('Error creating country:', error);
    throw error;
  }
};

/**
 * Update a country (admin only)
 * @param {string} id - Country ID
 * @param {Object} countryData - Updated country data
 * @returns {Promise<Object>} Updated country object
 */
export const updateCountry = async (id, countryData) => {
  try {
    const response = await api.put(`/admin/countries/${id}`, countryData);
    return response.data;
  } catch (error) {
    console.error(`Error updating country ${id}:`, error);
    throw error;
  }
};

/**
 * Delete a country (admin only)
 * @param {string} id - Country ID
 * @returns {Promise<void>}
 */
export const deleteCountry = async (id) => {
  try {
    await api.delete(`/admin/countries/${id}`);
  } catch (error) {
    console.error(`Error deleting country ${id}:`, error);
    throw error;
  }
};

/**
 * Set default country (admin only)
 * @param {string} id - Country ID
 * @returns {Promise<void>}
 */
export const setDefaultCountry = async (id) => {
  try {
    await api.put(`/admin/countries/${id}/set-default`);
  } catch (error) {
    console.error(`Error setting default country ${id}:`, error);
    throw error;
  }
};

export default {
  getAllCountries,
  getEnabledCountries,
  getDefaultCountry,
  getCountryById,
  createCountry,
  updateCountry,
  deleteCountry,
  setDefaultCountry,
};
