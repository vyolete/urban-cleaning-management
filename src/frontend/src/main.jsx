import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// CSS Load Order:
// 1. Application styles (main.css) - loaded first to establish base styles
import './styles/main.css';

// 2. Third-party library styles (Leaflet) - loaded after to allow library-specific overrides
// Note: Leaflet CSS should not override application button/form styles
import 'leaflet/dist/leaflet.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
