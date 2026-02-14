import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ReportForm from '../components/citizen/ReportForm';
import MapView from '../components/citizen/MapView';
import useGeolocation from '../hooks/useGeolocation';
import './CitizenReportPage.css';

/**
 * Citizen report page - main page for citizens to submit incident reports
 */
function CitizenReportPage() {
  const navigate = useNavigate();
  const { location } = useGeolocation();
  const [successMessage, setSuccessMessage] = useState(null);
  const [showMap, setShowMap] = useState(true);

  /**
   * Handle successful report submission
   */
  const handleSuccess = (response) => {
    setSuccessMessage(
      `¡Reporte enviado exitosamente! ID: ${response.id || 'N/A'}`
    );
    
    // Clear success message after 5 seconds
    setTimeout(() => {
      setSuccessMessage(null);
    }, 5000);

    // Scroll to top to show success message
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  /**
   * Handle report submission error
   */
  const handleError = (error) => {
    console.error('Error submitting report:', error);
    // Error is handled in the form component
  };

  /**
   * Toggle map visibility
   */
  const toggleMap = () => {
    setShowMap((prev) => !prev);
  };

  return (
    <div className="citizen-report-page">
      <div className="page-header">
        <h1>Sistema de Gestión de Limpieza Urbana</h1>
        <p className="subtitle">Reporte de Incidencias</p>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="success-banner">
          <div className="success-content">
            <span className="success-icon">✓</span>
            <p>{successMessage}</p>
          </div>
          <button
            onClick={() => setSuccessMessage(null)}
            className="close-btn"
            aria-label="Cerrar"
          >
            ×
          </button>
        </div>
      )}

      <div className="page-content">
        {/* Map Section */}
        <div className="map-section">
          <div className="section-header">
            <h2>Mapa de Ubicación</h2>
            <button
              onClick={toggleMap}
              className="toggle-btn"
              aria-label={showMap ? 'Ocultar mapa' : 'Mostrar mapa'}
            >
              {showMap ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>
          
          {showMap && (
            <div className="map-container">
              {location ? (
                <MapView
                  location={location}
                  showGeofence={true}
                  height="500px"
                  zoom={15}
                />
              ) : (
                <div className="map-placeholder">
                  <p>Esperando ubicación...</p>
                  <p className="help-text">
                    Por favor, permita el acceso a su ubicación para ver el mapa
                  </p>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Form Section */}
        <div className="form-section">
          <ReportForm
            onSuccess={handleSuccess}
            onError={handleError}
          />
        </div>
      </div>

      {/* Info Section */}
      <div className="info-section">
        <h3>Información Importante</h3>
        <ul>
          <li>
            <strong>Ubicación:</strong> Asegúrese de que la ubicación sea precisa antes de enviar el reporte.
          </li>
          <li>
            <strong>Fotografía:</strong> La foto debe mostrar claramente la incidencia reportada.
          </li>
          <li>
            <strong>Descripción:</strong> Proporcione detalles específicos que ayuden a los técnicos a resolver el problema.
          </li>
          <li>
            <strong>Seguimiento:</strong> Recibirá una notificación cuando su reporte sea procesado.
          </li>
        </ul>
      </div>
    </div>
  );
}

export default CitizenReportPage;
