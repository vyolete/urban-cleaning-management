import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import CitizenReportPage from './pages/CitizenReportPage';
import OperatorDashboard from './pages/OperatorDashboard';
import AdminConfigPage from './pages/AdminConfigPage';
import { IconSearch } from './assets/icons';
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
            {/* Home Route */}
            <Route path="/" element={<HomePage />} />
            
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
        <IconSearch size={48} className="not-found-icon" />
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
