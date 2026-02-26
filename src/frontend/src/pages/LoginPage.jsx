import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Icon } from '../components/common';
import urbixRobot from '../assets/urbix-robot.png';

/**
 * Get default route based on user role
 */
const getDefaultRoute = (role) => {
  switch (role) {
    case 'ROLE_ADMIN':
      return '/dashboard';
    case 'ROLE_TECNICO':
      return '/dashboard';
    case 'ROLE_CIUDADANO':
      return '/report';
    default:
      return '/report';
  }
};

/**
 * Login page component
 */
function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated, error: authError, clearError, user } = useAuth();

  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated() && user) {
      const from = location.state?.from?.pathname || getDefaultRoute(user.role);
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, location, user]);

  // Clear auth error when component unmounts
  useEffect(() => {
    return () => {
      clearError();
    };
  }, [clearError]);

  /**
   * Handle input change
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
   * Validate form
   */
  const validateForm = () => {
    const newErrors = {};

    if (!formData.username.trim()) {
      newErrors.username = 'El nombre de usuario es requerido';
    }

    if (!formData.password) {
      newErrors.password = 'La contraseña es requerida';
    } else if (formData.password.length < 6) {
      newErrors.password = 'La contraseña debe tener al menos 6 caracteres';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setErrors({});

    try {
      const userData = await login(formData.username, formData.password);
      
      // Redirect based on user role
      const defaultRoute = getDefaultRoute(userData.role);
      const from = location.state?.from?.pathname || defaultRoute;
      navigate(from, { replace: true });
    } catch (error) {
      setErrors({
        submit: error.response?.data?.message || 'Usuario o contraseña incorrectos',
      });
    } finally {
      setLoading(false);
    }
  };

  /**
   * Toggle password visibility
   */
  const togglePasswordVisibility = () => {
    setShowPassword((prev) => !prev);
  };

  return (
    <div className="login-page">
      <div className="login-container">
        {/* Logo/Brand Section */}
        <div className="login-header">
          <Link to="/" className="back-to-home">
            ← Volver al inicio
          </Link>
          <img 
            src={urbixRobot} 
            alt="Robot Urbix" 
            className="login-robot"
          />
          <div className="logo">
            <h1>Urbix</h1>
          </div>
          <p className="tagline">Sistema inteligente de gestión de incidencias urbanas</p>
        </div>

        {/* Login Form */}
        <div className="login-card">
          <h2>Iniciar Sesión</h2>
          <p className="login-subtitle">Ingrese sus credenciales para acceder</p>

          {/* Error Message */}
          {(errors.submit || authError) && (
            <div className="error-banner">
              <span className="error-icon"><Icon name="close" size="small" /></span>
              <p>{errors.submit || authError}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="login-form">
            {/* Username Field */}
            <div className="form-group">
              <label htmlFor="username">
                <span className="label-icon"><Icon name="user" size="small" /></span>
                Nombre de Usuario
              </label>
              <input
                type="text"
                id="username"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="Ingrese su usuario"
                autoComplete="username"
                autoFocus
                disabled={loading}
              />
              {errors.username && (
                <p className="error-text">{errors.username}</p>
              )}
            </div>

            {/* Password Field */}
            <div className="form-group">
              <label htmlFor="password">
                <span className="label-icon"><Icon name="lock" size="small" /></span>
                Contraseña
              </label>
              <div className="password-input-wrapper">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Ingrese su contraseña"
                  autoComplete="current-password"
                  disabled={loading}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={togglePasswordVisibility}
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  disabled={loading}
                >
                  <Icon name={showPassword ? 'eye-off' : 'eye'} size="small" />
                </button>
              </div>
              {errors.password && (
                <p className="error-text">{errors.password}</p>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="btn-login"
              disabled={loading}
            >
              {loading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
            </button>
          </form>

          {/* Additional Links */}
          <div className="login-footer">
            <p>
              ¿No tiene una cuenta?{' '}
              <Link to="/register" className="link-register">
                Registrarse
              </Link>
            </p>
          </div>
        </div>

        {/* Info Section */}
        <div className="login-info">
          <h3>Acceso por Rol</h3>
          <div className="role-info">
            <div className="role-card">
              <span className="role-icon"><Icon name="user" size="large" /></span>
              <h4>Ciudadano</h4>
              <p>Reportar incidencias de limpieza urbana</p>
            </div>
            <div className="role-card">
              <span className="role-icon"><Icon name="wrench" size="large" /></span>
              <h4>Técnico</h4>
              <p>Gestionar y resolver tareas asignadas</p>
            </div>
            <div className="role-card">
              <span className="role-icon"><Icon name="settings" size="large" /></span>
              <h4>Administrador</h4>
              <p>Configurar sistema y algoritmo de priorización</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
