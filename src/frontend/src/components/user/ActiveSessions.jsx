import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import authService from '../../services/authService';
import './ActiveSessions.css';

/**
 * ActiveSessions component
 * Displays and manages user's active sessions across devices
 */
const ActiveSessions = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [revoking, setRevoking] = useState(null);

  /**
   * Fetch active sessions from backend
   */
  const fetchSessions = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get('/sessions');
      setSessions(response.data);
    } catch (err) {
      console.error('Error fetching sessions:', err);
      setError('Failed to load sessions. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Revoke a specific session
   */
  const revokeSession = async (sessionId) => {
    if (!window.confirm('Are you sure you want to revoke this session?')) {
      return;
    }

    try {
      setRevoking(sessionId);
      await api.delete(`/sessions/${sessionId}`);
      
      // Refresh sessions list
      await fetchSessions();
      
      alert('Session revoked successfully');
    } catch (err) {
      console.error('Error revoking session:', err);
      alert('Failed to revoke session. Please try again.');
    } finally {
      setRevoking(null);
    }
  };

  /**
   * Logout from all devices
   */
  const logoutAllDevices = async () => {
    if (!window.confirm('Are you sure you want to logout from all devices? This will end all your active sessions.')) {
      return;
    }

    try {
      setLoading(true);
      await authService.logoutAll();
      
      // Redirect to login page
      window.location.href = '/login';
    } catch (err) {
      console.error('Error logging out from all devices:', err);
      alert('Failed to logout from all devices. Please try again.');
      setLoading(false);
    }
  };

  /**
   * Format date to readable string
   */
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    
    return date.toLocaleDateString();
  };

  /**
   * Get device icon based on device type
   */
  const getDeviceIcon = (deviceType) => {
    switch (deviceType) {
      case 'MOBILE':
        return '📱';
      case 'TABLET':
        return '📱';
      case 'DESKTOP':
        return '💻';
      default:
        return '🖥️';
    }
  };

  // Fetch sessions on mount
  useEffect(() => {
    fetchSessions();

    // Auto-refresh every 30 seconds
    const interval = setInterval(fetchSessions, 30000);

    return () => clearInterval(interval);
  }, []);

  if (loading && sessions.length === 0) {
    return (
      <div className="active-sessions">
        <div className="loading">Loading sessions...</div>
      </div>
    );
  }

  return (
    <div className="active-sessions">
      <div className="sessions-header">
        <h2>Active Sessions</h2>
        <button 
          className="btn-logout-all"
          onClick={logoutAllDevices}
          disabled={loading}
        >
          Logout All Devices
        </button>
      </div>

      {error && (
        <div className="error-message">
          {error}
          <button onClick={fetchSessions}>Retry</button>
        </div>
      )}

      {sessions.length === 0 ? (
        <div className="no-sessions">
          <p>No active sessions found.</p>
        </div>
      ) : (
        <div className="sessions-list">
          {sessions.map((session) => (
            <div 
              key={session.id} 
              className={`session-card ${session.current ? 'current-session' : ''}`}
            >
              <div className="session-icon">
                {getDeviceIcon(session.deviceType)}
              </div>
              
              <div className="session-info">
                <div className="session-device">
                  <strong>{session.browser}</strong> on {session.os}
                  {session.current && <span className="current-badge">Current</span>}
                </div>
                
                <div className="session-details">
                  <div className="session-location">
                    📍 {session.city && session.country 
                      ? `${session.city}, ${session.country}` 
                      : session.ipAddress}
                  </div>
                  
                  <div className="session-time">
                    🕐 Last active: {formatDate(session.lastActivity)}
                  </div>
                  
                  <div className="session-created">
                    Created: {formatDate(session.createdAt)}
                  </div>
                </div>
              </div>

              {!session.current && (
                <div className="session-actions">
                  <button
                    className="btn-revoke"
                    onClick={() => revokeSession(session.id)}
                    disabled={revoking === session.id}
                  >
                    {revoking === session.id ? 'Revoking...' : 'Revoke'}
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="sessions-footer">
        <p className="sessions-info">
          Sessions are automatically removed after 30 days of inactivity.
        </p>
        <button 
          className="btn-refresh"
          onClick={fetchSessions}
          disabled={loading}
        >
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>
    </div>
  );
};

export default ActiveSessions;
