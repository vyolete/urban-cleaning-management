import React, { useState, useEffect } from 'react';
import { countryService } from '../../services';
import { IconWarning, IconInfo } from '../../assets/icons';
import './CountrySelector.css';

/**
 * CountrySelector Component
 * Allows users to select a country for report submission
 * 
 * @param {Object} props
 * @param {string} props.selectedCountryId - Currently selected country ID
 * @param {Function} props.onSelectCountry - Callback when country is selected
 * @param {boolean} props.disabled - Whether the selector is disabled
 */
const CountrySelector = ({ selectedCountryId, onSelectCountry, disabled = false }) => {
  const [countries, setCountries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [defaultCountry, setDefaultCountry] = useState(null);

  useEffect(() => {
    loadCountries();
  }, []);

  /**
   * Load enabled countries from API
   */
  const loadCountries = async () => {
    try {
      setLoading(true);
      setError(null);

      // Load enabled countries
      const enabledCountries = await countryService.getEnabledCountries();
      setCountries(enabledCountries);

      // Load default country
      try {
        const defaultCountryData = await countryService.getDefaultCountry();
        setDefaultCountry(defaultCountryData);

        // Auto-select default country if no country is selected
        if (!selectedCountryId && defaultCountryData) {
          onSelectCountry(defaultCountryData.id);
        }
      } catch (err) {
        console.warn('No default country configured:', err);
      }

      setLoading(false);
    } catch (err) {
      console.error('Error loading countries:', err);
      setError('Error al cargar países. Por favor, intenta de nuevo.');
      setLoading(false);
    }
  };

  /**
   * Handle country selection change
   */
  const handleChange = (e) => {
    const countryId = e.target.value || null;
    onSelectCountry(countryId);
  };

  // Loading state
  if (loading) {
    return (
      <div className="country-selector">
        <label htmlFor="country">País *</label>
        <div className="country-selector-loading">
          <span className="spinner"></span>
          <span>Cargando países...</span>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="country-selector">
        <label htmlFor="country">País *</label>
        <div className="country-selector-error">
          <IconWarning size={16} className="error-icon" />
          <span>{error}</span>
          <button 
            type="button" 
            onClick={loadCountries}
            className="retry-button"
          >
            Reintentar
          </button>
        </div>
      </div>
    );
  }

  // No countries available
  if (countries.length === 0) {
    return (
      <div className="country-selector">
        <label htmlFor="country">País *</label>
        <div className="country-selector-empty">
          <span>No hay países configurados</span>
        </div>
      </div>
    );
  }

  // Disable selector if only one country is enabled
  const isDisabled = disabled || countries.length === 1;

  return (
    <div className="country-selector">
      <label htmlFor="country">
        País *
        {countries.length === 1 && (
          <span className="country-selector-hint">
            (Solo un país disponible)
          </span>
        )}
      </label>
      <select
        id="country"
        name="country"
        value={selectedCountryId || ''}
        onChange={handleChange}
        disabled={isDisabled}
        required
        className={isDisabled ? 'disabled' : ''}
      >
        <option value="">Seleccione un país</option>
        {countries.map((country) => (
          <option key={country.id} value={country.id}>
            {country.name}
            {country.defaultCountry && ' (Predeterminado)'}
            {country.administrativeArea && ` - ${country.administrativeArea}`}
          </option>
        ))}
      </select>
      
      {selectedCountryId && (
        <div className="country-selector-info">
          <IconInfo size={16} className="info-icon" />
          <span className="info-text">
            Los reportes se validarán dentro de los límites de {
              countries.find(c => c.id === selectedCountryId)?.name || 'este país'
            }
          </span>
        </div>
      )}
    </div>
  );
};

export default CountrySelector;
