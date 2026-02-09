import { Navigate, useLocation } from 'react-router-dom';
import PropTypes from 'prop-types';
import { useAuth } from '../../context/AuthContext';
import './ProtectedRoute.css';

/**
 * Protected Route Component
 * Redirects to login if user is not authenticated
 * Optionally checks for required roles
 */
function ProtectedRoute({ children, requiredRole, requiredRoles }) {
  const { isAuthenticated, hasRole, hasAnyRole, loading, user } = useAuth();
  const location = useLocation();

  // Show loading state while checking authentication
  if (loading) {
    return (
      <div className="spinner-container">
        <div className="spinner"></div>
        <p>Cargando...</p>
      </div>
    );
  }

  // Check if user is authenticated
  if (!isAuthenticated()) {
    // Save the location they were trying to access
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check for required single role
  if (requiredRole && !hasRole(requiredRole)) {
    return (
      <div className="access-denied-container">
        <div className="access-denied-card">
          <span className="denied-icon">🚫</span>
          <h2>Acceso Denegado</h2>
          <p>No tienes permisos para acceder a esta página.</p>
          <p className="required-role">Rol requerido: {requiredRole}</p>
          <p className="required-role">Tu rol: {user?.role}</p>
        </div>
      </div>
    );
  }

  // Check for required multiple roles (user must have at least one)
  if (requiredRoles && requiredRoles.length > 0 && !hasAnyRole(requiredRoles)) {
    return (
      <div className="access-denied-container">
        <div className="access-denied-card">
          <span className="denied-icon">🚫</span>
          <h2>Acceso Denegado</h2>
          <p>No tienes permisos para acceder a esta página.</p>
          <p className="required-role">
            Roles requeridos: {requiredRoles.join(', ')}
          </p>
          <p className="required-role">Tu rol: {user?.role}</p>
        </div>
      </div>
    );
  }

  // User is authenticated and has required permissions
  return children;
}

ProtectedRoute.propTypes = {
  children: PropTypes.node.isRequired,
  requiredRole: PropTypes.string,
  requiredRoles: PropTypes.arrayOf(PropTypes.string),
};

ProtectedRoute.defaultProps = {
  requiredRole: null,
  requiredRoles: null,
};

export default ProtectedRoute;
