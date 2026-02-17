import { useState } from 'react';
import ConfigPanel from '../components/admin/ConfigPanel';
import UserInfo from '../components/common/UserInfo';
import './AdminConfigPage.css';

/**
 * Admin configuration page - main interface for administrators to manage system configuration
 */
function AdminConfigPage() {
  const [lastUpdate, setLastUpdate] = useState(null);

  /**
   * Handle configuration update
   */
  const handleConfigUpdate = (updatedConfig) => {
    setLastUpdate(new Date());
    console.log('Configuration updated:', updatedConfig);
  };

  return (
    <div className="admin-config-page">
      {/* Header */}
      <div className="page-header">
        <div className="header-content">
          <h1>Panel de Administración</h1>
          <p className="subtitle">Configuración del Sistema de Priorización</p>
        </div>
        
        {/* User Info with Logout */}
        <UserInfo />
        
        {lastUpdate && (
          <div className="last-update">
            <span className="update-label">Última actualización:</span>
            <span className="update-time">
              {lastUpdate.toLocaleTimeString('es-ES', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
              })}
            </span>
          </div>
        )}
      </div>

      {/* Main Content */}
      <div className="page-content">
        <ConfigPanel onConfigUpdate={handleConfigUpdate} />
      </div>

      {/* Info Section */}
      <div className="info-section">
        <h3>Información Importante</h3>
        <div className="info-grid">
          <div className="info-card">
            <div className="info-icon">⚖️</div>
            <h4>Pesos del Algoritmo</h4>
            <p>
              Los pesos determinan la importancia relativa de cada factor en el cálculo de prioridad.
              Deben sumar exactamente 1.0 (100%).
            </p>
          </div>

          <div className="info-card">
            <div className="info-icon">🔄</div>
            <h4>Recalculación Automática</h4>
            <p>
              Al actualizar los pesos, todas las tareas pendientes serán recalculadas automáticamente
              con la nueva configuración.
            </p>
          </div>

          <div className="info-card">
            <div className="info-icon">📍</div>
            <h4>Deduplicación Espacial</h4>
            <p>
              La distancia define el radio en metros para considerar reportes como duplicados.
              Valores típicos: 50-100 metros.
            </p>
          </div>

          <div className="info-card">
            <div className="info-icon">⏱️</div>
            <h4>Deduplicación Temporal</h4>
            <p>
              La ventana de tiempo define cuántas horas deben transcurrir para considerar reportes
              como duplicados. Valores típicos: 24-48 horas.
            </p>
          </div>
        </div>
      </div>

      {/* Formula Section */}
      <div className="formula-section">
        <h3>Fórmula de Priorización</h3>
        <div className="formula-container">
          <div className="formula">
            <span className="formula-text">P = (Wc × Categoría) + (Wz × Zona) + (Wt × Tiempo)</span>
          </div>
          <div className="formula-description">
            <p><strong>P:</strong> Puntuación de prioridad final</p>
            <p><strong>Wc:</strong> Peso de categoría (severidad del tipo de incidencia)</p>
            <p><strong>Wz:</strong> Peso de zona (índice de riesgo de la ubicación)</p>
            <p><strong>Wt:</strong> Peso de tiempo (urgencia basada en tiempo transcurrido)</p>
          </div>
        </div>
      </div>

      {/* Best Practices */}
      <div className="best-practices-section">
        <h3>Mejores Prácticas</h3>
        <ul>
          <li>
            <strong>Categoría (40%):</strong> Recomendado para dar mayor importancia a la severidad
            de la incidencia (basura acumulada vs. graffiti).
          </li>
          <li>
            <strong>Zona (35%):</strong> Útil para priorizar áreas de alto tráfico o zonas sensibles
            (centros históricos, zonas turísticas).
          </li>
          <li>
            <strong>Tiempo (25%):</strong> Asegura que reportes antiguos no queden olvidados,
            aumentando su prioridad con el tiempo.
          </li>
          <li>
            <strong>Deduplicación:</strong> Ajuste la distancia según la densidad urbana. Áreas
            densas pueden requerir distancias menores (30-50m).
          </li>
          <li>
            <strong>Pruebas:</strong> Después de cambiar los pesos, monitoree el dashboard de
            operadores para verificar que las prioridades sean razonables.
          </li>
        </ul>
      </div>
    </div>
  );
}

export default AdminConfigPage;
