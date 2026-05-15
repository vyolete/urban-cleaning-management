import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ReportForm from '../components/citizen/ReportForm';
import MapView from '../components/citizen/MapView';
import useGeolocation from '../hooks/useGeolocation';
import urbixRobot from '../assets/urbix-robot.png';
import { IconSparkle, IconBarChart, IconLock, IconLogout, IconCheck, IconX } from '../assets/icons';
import './CitizenReportPage.css';

/**
 * Citizen report page - main page for citizens to submit incident reports
 */
function CitizenReportPage() {
  const navigate = useNavigate();
  const { isAuthenticated, user, logout } = useAuth();
  const { location, getCurrentLocation } = useGeolocation();
  const [successMessage, setSuccessMessage] = useState(null);
  const [showMap, setShowMap] = useState(true);
  const [selectedCountryId, setSelectedCountryId] = useState(null);

  // Get location on component mount
  useEffect(() => {
    console.log('[CitizenReportPage] Calling getCurrentLocation...');
    getCurrentLocation();
  }, [getCurrentLocation]);

  // Debug location state
  useEffect(() => {
    console.log('[CitizenReportPage] location state:', location);
    console.log('[CitizenReportPage] showMap state:', showMap);
  }, [location, showMap]);

  /**
   * Handle country selection
   */
  const handleCountrySelect = (countryId) => {
    console.log('[CitizenReportPage] Country selected:', countryId);
    setSelectedCountryId(countryId);
  };

  /**
   * Navigate to login page
   */
  const handleLoginClick = () => {
    navigate('/login');
  };

  /**
   * Navigate to dashboard (for authenticated users)
   */
  const handleDashboardClick = () => {
    if (user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_TECNICO') {
      navigate('/dashboard');
    } else if (user?.role === 'ROLE_ADMIN') {
      navigate('/admin/config');
    }
  };

  /**
   * Handle logout
   */
  const handleLogout = () => {
    logout();
    setSuccessMessage('Sesión cerrada exitosamente');
    setTimeout(() => {
      setSuccessMessage(null);
    }, 3000);
  };

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
        <div className="header-content">
          <div className="header-title">
            <img 
              src={urbixRobot} 
              alt="Robot Urbix" 
              className="header-robot"
            />
            <div className="title-text">
              <h1>Urbix</h1>
              <p className="subtitle">Reporte de Incidencias</p>
            </div>
          </div>
          <div className="header-actions">
            {isAuthenticated() ? (
              <>
                <span className="user-greeting">
                  <IconSparkle size={16} /> {user?.username || 'Usuario'}
                </span>
                {(user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_TECNICO') && (
                  <button
                    onClick={handleDashboardClick}
                    className="btn-dashboard"
                    aria-label="Ir al Dashboard"
                  >
                    <IconBarChart size={18} className="btn-icon" />
                    Dashboard
                  </button>
                )}
                <button
                  onClick={handleLogout}
                  className="btn-logout"
                  aria-label="Cerrar Sesión"
                >
                  <IconLogout size={16} className="btn-icon" />
                  Cerrar Sesión
                </button>
              </>
            ) : (
              <button
                onClick={handleLoginClick}
                className="btn-login-header"
                aria-label="Iniciar Sesión"
              >
                <IconLock size={18} className="btn-icon" />
                Iniciar Sesión
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="success-banner">
          <div>
            <div className="success-content">
              <IconCheck size={20} className="success-icon" />
              <p>{successMessage}</p>
            </div>
            <button
              onClick={() => setSuccessMessage(null)}
              className="close-btn"
              aria-label="Cerrar"
            >
              <IconX size={16} />
            </button>
          </div>
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
                  countryId={selectedCountryId}
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
            location={location}
            onSuccess={handleSuccess}
            onError={handleError}
            onCountrySelect={handleCountrySelect}
          />
        </div>
      </div>

      {/* Info Section */}
      <div className="info-section">
        <div>
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
    </div>
  );
}

export default CitizenReportPage;
