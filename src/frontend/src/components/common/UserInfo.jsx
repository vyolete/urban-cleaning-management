import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './UserInfo.css';

/**
 * User Info Component
 * Displays current user information and logout button
 */
function UserInfo() {
  const navigate = useNavigate();
  const { 
    user, 
    isAuthenticated, 
    getUserDisplayName, 
    getRoleDisplayName, 
    logout 
  } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  if (!isAuthenticated()) {
    return null;
  }

  return (
    <div className="user-info">
      <div className="user-details">
        <span className="user-name">{getUserDisplayName()}</span>
        <span className="user-role">{getRoleDisplayName()}</span>
      </div>
      <button 
        onClick={handleLogout} 
        className="btn btn-secondary btn-sm"
        aria-label="Cerrar sesión"
      >
        Cerrar Sesión
      </button>
    </div>
  );
}

export default UserInfo;
