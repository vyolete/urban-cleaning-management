import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import CitizenReportPage from './pages/CitizenReportPage';
import OperatorDashboard from './pages/OperatorDashboard';
import AdminConfigPage from './pages/AdminConfigPage';
import './App.css';

/**
 * Main application component
 * Sets up routing and global providers
 */
function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="App">
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<LoginPage />} />
            
            {/* Citizen Routes - Public access for reporting */}
            <Route path="/report" element={<CitizenReportPage />} />
            
            {/* Operator Routes - Requires TECNICO or ADMIN role */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute requiredRoles={['ROLE_TECNICO', 'ROLE_ADMIN']}>
                  <OperatorDashboard />
                </ProtectedRoute>
              }
            />
            
            {/* Admin Routes - Requires ADMIN role */}
            <Route
              path="/admin/config"
              element={
                <ProtectedRoute requiredRole="ROLE_ADMIN">
                  <AdminConfigPage />
                </ProtectedRoute>
              }
            />
            
            {/* Default Route - Redirect to report page */}
            <Route path="/" element={<Navigate to="/report" replace />} />
            
            {/* 404 - Not Found */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </div>
      </Router>
    </AuthProvider>
  );
}

/**
 * 404 Not Found Page
 */
function NotFoundPage() {
  return (
    <div className="not-found-container">
      <div className="not-found-card">
        <span className="not-found-icon">🔍</span>
        <h1>404</h1>
        <h2>Página No Encontrada</h2>
        <p>La página que buscas no existe.</p>
        <a href="/" className="btn-home">
          Volver al Inicio
        </a>
      </div>
    </div>
  );
}

export default App;
