import { BrowserRouter as Router } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
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
          <header className="App-header">
            <h1>Urban Cleaning Management System</h1>
            <p>Sistema de Gestión de Limpieza Urbana</p>
          </header>
          <main>
            <div className="container">
              <div className="card mt-4">
                <div className="card-header">
                  <h2>Bienvenido</h2>
                </div>
                <p>Frontend en construcción...</p>
                <p>Backend API: {import.meta.env.VITE_API_URL}</p>
                <div className="mt-3">
                  <h3>Estado del Proyecto:</h3>
                  <ul style={{ textAlign: 'left', maxWidth: '600px', margin: '0 auto' }}>
                    <li>✅ Backend completado (65%)</li>
                    <li>✅ Estructura del frontend</li>
                    <li>✅ Servicios API</li>
                    <li>✅ Contexto de autenticación</li>
                    <li>⏳ Componentes de UI (en progreso)</li>
                  </ul>
                </div>
              </div>
            </div>
          </main>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;
