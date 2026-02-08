import { BrowserRouter as Router } from 'react-router-dom';
import './App.css';

/**
 * Main application component
 * Sets up routing and global providers
 */
function App() {
  return (
    <Router>
      <div className="App">
        <header className="App-header">
          <h1>Urban Cleaning Management System</h1>
          <p>Sistema de Gestión de Limpieza Urbana</p>
        </header>
        <main>
          <p>Frontend en construcción...</p>
          <p>Backend API: {import.meta.env.VITE_API_URL}</p>
        </main>
      </div>
    </Router>
  );
}

export default App;
