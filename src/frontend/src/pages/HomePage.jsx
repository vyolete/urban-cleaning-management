import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Icon } from '../components/common';
import urbixRobot from '../assets/urbix-robot.png';

/**
 * Home page - Landing page with main actions
 */
function HomePage() {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();

  /**
   * Navigate to login page
   */
  const handleLoginClick = () => {
    navigate('/login');
  };

  /**
   * Navigate to report page
   */
  const handleReportClick = () => {
    navigate('/report');
  };

  /**
   * Navigate to dashboard (for authenticated users)
   */
  const handleDashboardClick = () => {
    if (user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_TECNICO') {
      navigate('/dashboard');
    }
  };

  return (
    <div className="home-page">
      <div className="home-container">
        <div className="home-card">
          {/* Logo / Icon */}
          <div className="logo-container">
            <img 
              src={urbixRobot} 
              alt="Robot Urbix" 
              className="robot-image"
            />
          </div>

          {/* Title */}
          <div className="title-section">
            <h1 className="main-title">Urbix</h1>
            <p className="subtitle">
              Sistema inteligente de gestión de incidencias urbanas
            </p>
            <p className="description">
              Optimiza la gestión urbana con tecnología inteligente.
            </p>
          </div>

          {/* Actions */}
          <div className="actions-section">
            {isAuthenticated() ? (
              <>
                <div className="welcome-message">
                  <span className="welcome-icon"><Icon name="wave" size="medium" ariaLabel="Welcome" /></span>
                  <p>Bienvenido, {user?.username || 'Usuario'}</p>
                </div>
                
                {(user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_TECNICO') && (
                  <button
                    onClick={handleDashboardClick}
                    className="btn-home btn-primary"
                  >
                    <span className="btn-icon"><Icon name="chart" size="small" /></span>
                    Ir al Dashboard
                  </button>
                )}
                
                <button
                  onClick={handleReportClick}
                  className="btn-home btn-outline"
                >
                  <span className="btn-icon"><Icon name="warning" size="small" ariaLabel="Report incident" /></span>
                  Reportar Incidencia
                </button>
              </>
            ) : (
              <>
                <button
                  onClick={handleLoginClick}
                  className="btn-home btn-primary"
                >
                  <span className="btn-icon"><Icon name="lock" size="small" ariaLabel="Secure login" /></span>
                  Iniciar Sesión
                </button>

                <button
                  onClick={handleReportClick}
                  className="btn-home btn-outline"
                >
                  <span className="btn-icon"><Icon name="warning" size="small" ariaLabel="Report incident" /></span>
                  Reportar Incidencia
                </button>
              </>
            )}
          </div>

          {/* Footer */}
          <div className="home-footer">
            <p>© {new Date().getFullYear()} Urbix. Todos los derechos reservados.</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default HomePage;
