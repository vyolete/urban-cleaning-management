import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import useGeolocation from '../../hooks/useGeolocation';
import reportService from '../../services/reportService';
import './ReportForm.css';

/**
 * Report form component for citizens to submit incident reports
 */
function ReportForm({ onSuccess, onError }) {
  const { location, error: locationError, loading: locationLoading, getCurrentLocation } = useGeolocation();
  
  const [formData, setFormData] = useState({
    category: '',
    description: '',
  });
  
  const [photo, setPhoto] = useState(null);
  const [photoPreview, setPhotoPreview] = useState(null);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [useManualLocation, setUseManualLocation] = useState(false);
  const [manualLocation, setManualLocation] = useState({
    latitude: '',
    longitude: '',
  });

  // Categories available for selection
  const categories = [
    { value: 'BASURA_ACUMULADA', label: 'Basura Acumulada' },
    { value: 'CONTENEDOR_DANADO', label: 'Contenedor Dañado' },
    { value: 'VERTIDO_ILEGAL', label: 'Vertido Ilegal' },
    { value: 'LIMPIEZA_CALLE', label: 'Limpieza de Calle' },
    { value: 'GRAFFITI', label: 'Graffiti' },
    { value: 'OTRO', label: 'Otro' },
  ];

  // Get location on component mount
  useEffect(() => {
    if (!useManualLocation) {
      getCurrentLocation();
    }
  }, [getCurrentLocation, useManualLocation]);

  /**
   * Handle form field changes
   */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
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
   * Handle manual location changes
   */
  const handleManualLocationChange = (e) => {
    const { name, value } = e.target;
    setManualLocation((prev) => ({
      ...prev,
      [name]: value,
    }));
    
    if (errors.location) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors.location;
        return newErrors;
      });
    }
  };

  /**
   * Handle photo selection
   */
  const handlePhotoChange = (e) => {
    const file = e.target.files[0];
    
    if (file) {
      setPhoto(file);
      
      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setPhotoPreview(reader.result);
      };
      reader.readAsDataURL(file);
      
      // Clear photo error
      if (errors.photo) {
        setErrors((prev) => {
          const newErrors = { ...prev };
          delete newErrors.photo;
          return newErrors;
        });
      }
    }
  };

  /**
   * Remove selected photo
   */
  const handleRemovePhoto = () => {
    setPhoto(null);
    setPhotoPreview(null);
  };

  /**
   * Toggle between automatic and manual location
   */
  const toggleLocationMode = () => {
    setUseManualLocation((prev) => !prev);
    if (!useManualLocation) {
      // Switching to manual, clear auto location errors
      if (errors.location) {
        setErrors((prev) => {
          const newErrors = { ...prev };
          delete newErrors.location;
          return newErrors;
        });
      }
    } else {
      // Switching to auto, get location
      getCurrentLocation();
    }
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Prepare report data
    const reportData = {
      category: formData.category,
      description: formData.description,
      latitude: useManualLocation ? parseFloat(manualLocation.latitude) : location?.latitude,
      longitude: useManualLocation ? parseFloat(manualLocation.longitude) : location?.longitude,
    };

    // Validate
    const validation = reportService.validateReport(reportData, photo);
    
    if (!validation.isValid) {
      setErrors(validation.errors);
      return;
    }

    // Submit report
    setSubmitting(true);
    setErrors({});

    try {
      const response = await reportService.submitReport(reportData, photo);
      
      // Reset form
      setFormData({
        category: '',
        description: '',
      });
      setPhoto(null);
      setPhotoPreview(null);
      setManualLocation({ latitude: '', longitude: '' });
      
      // Call success callback
      if (onSuccess) {
        onSuccess(response);
      }
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Error al enviar el reporte';
      setErrors({ submit: errorMessage });
      
      // Call error callback
      if (onError) {
        onError(error);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="report-form">
      <h2>Reportar Incidencia</h2>
      
      <form onSubmit={handleSubmit}>
        {/* Location Section */}
        <div className="form-section">
          <h3>Ubicación</h3>
          
          <div className="location-mode-toggle">
            <button
              type="button"
              onClick={toggleLocationMode}
              className="btn-secondary"
            >
              {useManualLocation ? 'Usar ubicación automática' : 'Ingresar ubicación manualmente'}
            </button>
          </div>

          {!useManualLocation ? (
            <div className="auto-location">
              {locationLoading && <p className="info">Obteniendo ubicación...</p>}
              {locationError && <p className="error">{locationError}</p>}
              {location && (
                <div className="location-info">
                  <p>
                    <strong>Latitud:</strong> {location.latitude.toFixed(6)}
                  </p>
                  <p>
                    <strong>Longitud:</strong> {location.longitude.toFixed(6)}
                  </p>
                  <p className="accuracy">
                    Precisión: ±{Math.round(location.accuracy)}m
                  </p>
                </div>
              )}
            </div>
          ) : (
            <div className="manual-location">
              <div className="form-group">
                <label htmlFor="latitude">Latitud *</label>
                <input
                  type="number"
                  id="latitude"
                  name="latitude"
                  value={manualLocation.latitude}
                  onChange={handleManualLocationChange}
                  step="0.000001"
                  min="-90"
                  max="90"
                  placeholder="Ej: 40.416775"
                  required
                />
              </div>
              
              <div className="form-group">
                <label htmlFor="longitude">Longitud *</label>
                <input
                  type="number"
                  id="longitude"
                  name="longitude"
                  value={manualLocation.longitude}
                  onChange={handleManualLocationChange}
                  step="0.000001"
                  min="-180"
                  max="180"
                  placeholder="Ej: -3.703790"
                  required
                />
              </div>
            </div>
          )}
          
          {errors.location && <p className="error">{errors.location}</p>}
          {errors.latitude && <p className="error">{errors.latitude}</p>}
          {errors.longitude && <p className="error">{errors.longitude}</p>}
        </div>

        {/* Category Section */}
        <div className="form-section">
          <div className="form-group">
            <label htmlFor="category">Categoría *</label>
            <select
              id="category"
              name="category"
              value={formData.category}
              onChange={handleChange}
              required
            >
              <option value="">Seleccione una categoría</option>
              {categories.map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
            {errors.category && <p className="error">{errors.category}</p>}
          </div>
        </div>

        {/* Description Section */}
        <div className="form-section">
          <div className="form-group">
            <label htmlFor="description">Descripción *</label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              rows="4"
              placeholder="Describa la incidencia con el mayor detalle posible (mínimo 10 caracteres)"
              required
            />
            <p className="char-count">
              {formData.description.length} caracteres
            </p>
            {errors.description && <p className="error">{errors.description}</p>}
          </div>
        </div>

        {/* Photo Section */}
        <div className="form-section">
          <div className="form-group">
            <label htmlFor="photo">Fotografía *</label>
            <input
              type="file"
              id="photo"
              name="photo"
              accept="image/jpeg,image/jpg,image/png"
              onChange={handlePhotoChange}
              required={!photo}
            />
            <p className="help-text">
              Formatos permitidos: JPEG, PNG. Tamaño máximo: 5MB
            </p>
            {errors.photo && <p className="error">{errors.photo}</p>}
          </div>

          {photoPreview && (
            <div className="photo-preview">
              <img src={photoPreview} alt="Vista previa" />
              <button
                type="button"
                onClick={handleRemovePhoto}
                className="btn-remove"
              >
                Eliminar foto
              </button>
            </div>
          )}
        </div>

        {/* Submit Error */}
        {errors.submit && (
          <div className="form-error">
            <p className="error">{errors.submit}</p>
          </div>
        )}

        {/* Submit Button */}
        <div className="form-actions">
          <button
            type="submit"
            className="btn-primary"
            disabled={submitting || locationLoading || (!location && !useManualLocation)}
          >
            {submitting ? 'Enviando...' : 'Enviar Reporte'}
          </button>
        </div>
      </form>
    </div>
  );
}

ReportForm.propTypes = {
  onSuccess: PropTypes.func,
  onError: PropTypes.func,
};

export default ReportForm;
