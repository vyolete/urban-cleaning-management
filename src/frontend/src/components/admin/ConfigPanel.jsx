import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import configService from '../../services/configService';
import { IconX, IconCheck } from '../../assets/icons';
import './ConfigPanel.css';

/**
 * Configuration panel component for managing algorithm weights
 */
function ConfigPanel({ onConfigUpdate }) {
  const [currentConfig, setCurrentConfig] = useState(null);
  const [configHistory, setConfigHistory] = useState([]);
  const [formData, setFormData] = useState(configService.getDefaultWeights());
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(null);
  const [error, setError] = useState(null);
  const [showHistory, setShowHistory] = useState(false);

  /**
   * Load current configuration
   */
  const loadCurrentConfig = async () => {
    setLoading(true);
    setError(null);

    try {
      const config = await configService.getCurrentConfig();
      setCurrentConfig(config);
      setFormData({
        weightCategory: config.weightCategory,
        weightZone: config.weightZone,
        weightTime: config.weightTime,
        deduplicationDistanceMeters: config.deduplicationDistanceMeters,
        deduplicationTimeWindowHours: config.deduplicationTimeWindowHours,
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cargar la configuración');
      console.error('Error loading config:', err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Load configuration history
   */
  const loadConfigHistory = async () => {
    try {
      const history = await configService.getConfigHistory();
      setConfigHistory(history);
    } catch (err) {
      console.error('Error loading config history:', err);
    }
  };

  // Load config on mount
  useEffect(() => {
    loadCurrentConfig();
    loadConfigHistory();
  }, []);

  /**
   * Handle input change
   */
  const handleChange = (e) => {
    const { name, value } = e.target;
    const numValue = parseFloat(value);

    setFormData((prev) => ({
      ...prev,
      [name]: isNaN(numValue) ? value : numValue,
    }));

    // Clear error for this field
    if (errors[name]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }
  };

  /**
   * Normalize weights to sum to 1.0
   */
  const handleNormalize = () => {
    const normalized = configService.normalizeWeights(formData);
    setFormData(normalized);
    setSuccess('Pesos normalizados correctamente');
    setTimeout(() => setSuccess(null), 3000);
  };

  /**
   * Reset to current config
   */
  const handleReset = () => {
    if (currentConfig) {
      setFormData({
        weightCategory: currentConfig.weightCategory,
        weightZone: currentConfig.weightZone,
        weightTime: currentConfig.weightTime,
        deduplicationDistanceMeters: currentConfig.deduplicationDistanceMeters,
        deduplicationTimeWindowHours: currentConfig.deduplicationTimeWindowHours,
      });
      setErrors({});
      setSuccess('Formulario restablecido');
      setTimeout(() => setSuccess(null), 3000);
    }
  };

  /**
   * Reset to default values
   */
  const handleResetToDefaults = () => {
    setFormData(configService.getDefaultWeights());
    setErrors({});
    setSuccess('Valores predeterminados cargados');
    setTimeout(() => setSuccess(null), 3000);
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validate
    const validation = configService.validateWeights(formData);
    if (!validation.isValid) {
      setErrors(validation.errors);
      return;
    }

    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      const updatedConfig = await configService.updateWeights(formData);
      setCurrentConfig(updatedConfig);
      setSuccess('Configuración actualizada exitosamente. Las tareas pendientes serán recalculadas.');
      
      // Reload history
      await loadConfigHistory();

      // Clear success message after 5 seconds
      setTimeout(() => setSuccess(null), 5000);

      // Notify parent component
      if (onConfigUpdate) {
        onConfigUpdate(updatedConfig);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error al actualizar la configuración');
      console.error('Error updating config:', err);
    } finally {
      setSaving(false);
    }
  };

  /**
   * Calculate weight sum
   */
  const calculateSum = () => {
    return (
      parseFloat(formData.weightCategory || 0) +
      parseFloat(formData.weightZone || 0) +
      parseFloat(formData.weightTime || 0)
    );
  };

  /**
   * Format date
   */
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const weightSum = calculateSum();
  const isSumValid = Math.abs(weightSum - 1.0) <= 0.01;

  return (
    <div className="config-panel">
      <div className="panel-header">
        <h2>Configuración del Algoritmo</h2>
        <p className="subtitle">
          Ajuste los pesos del algoritmo de priorización: P = (Wc × Categoría) + (Wz × Zona) + (Wt × Tiempo)
        </p>
      </div>

      {/* Loading State */}
      {loading && (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Cargando configuración...</p>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="error-banner">
          <IconX size={18} className="error-icon" />
          <p>{error}</p>
          <button onClick={loadCurrentConfig} className="btn-retry">
            Reintentar
          </button>
        </div>
      )}

      {/* Success Message */}
      {success && (
        <div className="success-banner">
          <IconCheck size={18} className="success-icon" />
          <p>{success}</p>
        </div>
      )}

      {/* Configuration Form */}
      {!loading && currentConfig && (
        <form onSubmit={handleSubmit} className="config-form">
          {/* Weight Configuration */}
          <div className="form-section">
            <h3>Pesos del Algoritmo</h3>
            <p className="section-description">
              Los pesos deben sumar 1.0 (100%). Use el botón "Normalizar" para ajustar automáticamente.
            </p>

            <div className="weights-grid">
              {/* Category Weight */}
              <div className="form-group">
                <label htmlFor="weightCategory">
                  Peso de Categoría (Wc)
                  <span className="weight-percentage">
                    {configService.formatWeightPercentage(formData.weightCategory || 0)}
                  </span>
                </label>
                <input
                  type="number"
                  id="weightCategory"
                  name="weightCategory"
                  value={formData.weightCategory}
                  onChange={handleChange}
                  step="0.01"
                  min="0"
                  max="1"
                  required
                />
                <p className="help-text">
                  Importancia de la severidad de la categoría del reporte
                </p>
                {errors.weightCategory && (
                  <p className="error">{errors.weightCategory}</p>
                )}
              </div>

              {/* Zone Weight */}
              <div className="form-group">
                <label htmlFor="weightZone">
                  Peso de Zona (Wz)
                  <span className="weight-percentage">
                    {configService.formatWeightPercentage(formData.weightZone || 0)}
                  </span>
                </label>
                <input
                  type="number"
                  id="weightZone"
                  name="weightZone"
                  value={formData.weightZone}
                  onChange={handleChange}
                  step="0.01"
                  min="0"
                  max="1"
                  required
                />
                <p className="help-text">
                  Importancia del índice de riesgo de la zona geográfica
                </p>
                {errors.weightZone && (
                  <p className="error">{errors.weightZone}</p>
                )}
              </div>

              {/* Time Weight */}
              <div className="form-group">
                <label htmlFor="weightTime">
                  Peso de Tiempo (Wt)
                  <span className="weight-percentage">
                    {configService.formatWeightPercentage(formData.weightTime || 0)}
                  </span>
                </label>
                <input
                  type="number"
                  id="weightTime"
                  name="weightTime"
                  value={formData.weightTime}
                  onChange={handleChange}
                  step="0.01"
                  min="0"
                  max="1"
                  required
                />
                <p className="help-text">
                  Importancia del tiempo transcurrido desde el reporte
                </p>
                {errors.weightTime && (
                  <p className="error">{errors.weightTime}</p>
                )}
              </div>
            </div>

            {/* Weight Sum Indicator */}
            <div className={`weight-sum ${isSumValid ? 'valid' : 'invalid'}`}>
              <span className="sum-label">Suma de pesos:</span>
              <span className="sum-value">{weightSum.toFixed(3)}</span>
              <span className="sum-status">
                {isSumValid
                  ? <><IconCheck size={14} /> Válido</>
                  : <><IconX size={14} /> Debe sumar 1.0</>
                }
              </span>
            </div>

            {errors.sum && <p className="error">{errors.sum}</p>}

            {/* Weight Actions */}
            <div className="weight-actions">
              <button
                type="button"
                onClick={handleNormalize}
                className="btn-secondary"
              >
                Normalizar Pesos
              </button>
            </div>
          </div>

          {/* Deduplication Configuration */}
          <div className="form-section">
            <h3>Configuración de Deduplicación</h3>
            <p className="section-description">
              Parámetros para detectar reportes duplicados
            </p>

            <div className="dedup-grid">
              {/* Distance */}
              <div className="form-group">
                <label htmlFor="deduplicationDistanceMeters">
                  Distancia (metros)
                </label>
                <input
                  type="number"
                  id="deduplicationDistanceMeters"
                  name="deduplicationDistanceMeters"
                  value={formData.deduplicationDistanceMeters}
                  onChange={handleChange}
                  step="1"
                  min="1"
                  required
                />
                <p className="help-text">
                  Reportes dentro de esta distancia se consideran duplicados
                </p>
                {errors.deduplicationDistanceMeters && (
                  <p className="error">{errors.deduplicationDistanceMeters}</p>
                )}
              </div>

              {/* Time Window */}
              <div className="form-group">
                <label htmlFor="deduplicationTimeWindowHours">
                  Ventana de Tiempo (horas)
                </label>
                <input
                  type="number"
                  id="deduplicationTimeWindowHours"
                  name="deduplicationTimeWindowHours"
                  value={formData.deduplicationTimeWindowHours}
                  onChange={handleChange}
                  step="1"
                  min="1"
                  required
                />
                <p className="help-text">
                  Reportes dentro de esta ventana temporal se consideran duplicados
                </p>
                {errors.deduplicationTimeWindowHours && (
                  <p className="error">{errors.deduplicationTimeWindowHours}</p>
                )}
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="form-actions">
            <button
              type="button"
              onClick={handleResetToDefaults}
              className="btn-default"
            >
              Valores Predeterminados
            </button>
            <button
              type="button"
              onClick={handleReset}
              className="btn-secondary"
            >
              Restablecer
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={saving || !isSumValid}
            >
              {saving ? 'Guardando...' : 'Guardar Configuración'}
            </button>
          </div>
        </form>
      )}

      {/* Configuration History */}
      {!loading && (
        <div className="history-section">
          <div className="history-header">
            <h3>Historial de Configuraciones</h3>
            <button
              onClick={() => setShowHistory(!showHistory)}
              className="btn-toggle"
            >
              {showHistory ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>

          {showHistory && (
            <div className="history-content">
              {configHistory.length === 0 ? (
                <p className="empty-history">No hay historial disponible</p>
              ) : (
                <div className="history-table-container">
                  <table className="history-table">
                    <thead>
                      <tr>
                        <th>Fecha</th>
                        <th>Wc</th>
                        <th>Wz</th>
                        <th>Wt</th>
                        <th>Distancia (m)</th>
                        <th>Tiempo (h)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {configHistory.map((config) => (
                        <tr key={config.id}>
                          <td>{formatDate(config.effectiveDate)}</td>
                          <td>{configService.formatWeightPercentage(config.weightCategory)}</td>
                          <td>{configService.formatWeightPercentage(config.weightZone)}</td>
                          <td>{configService.formatWeightPercentage(config.weightTime)}</td>
                          <td>{config.deduplicationDistanceMeters}</td>
                          <td>{config.deduplicationTimeWindowHours}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

ConfigPanel.propTypes = {
  onConfigUpdate: PropTypes.func,
};

export default ConfigPanel;
