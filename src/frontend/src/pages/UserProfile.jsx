import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import ActiveSessions from '../components/user/ActiveSessions';
import './UserProfile.css';

/**
 * UserProfile page
 * Displays user information and active sessions
 */
const UserProfile = () => {
  const { user, getUserDisplayName, getRoleDisplayName } = useAuth();
  const [activeTab, setActiveTab] = useState('sessions');

  if (!user) {
    return (
      <div className="user-profile">
        <div className="error-message">
          User not found. Please login again.
        </div>
      </div>
    );
  }

  return (
    <div className="user-profile">
      <div className="profile-header">
        <div className="profile-avatar">
          {getUserDisplayName().charAt(0).toUpperCase()}
        </div>
        <div className="profile-info">
          <h1>{getUserDisplayName()}</h1>
          <p className="profile-role">{getRoleDisplayName()}</p>
        </div>
      </div>

      <div className="profile-tabs">
        <button
          className={`tab-button ${activeTab === 'sessions' ? 'active' : ''}`}
          onClick={() => setActiveTab('sessions')}
        >
          Active Sessions
        </button>
        <button
          className={`tab-button ${activeTab === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveTab('settings')}
        >
          Settings
        </button>
      </div>

      <div className="profile-content">
        {activeTab === 'sessions' && <ActiveSessions />}
        
        {activeTab === 'settings' && (
          <div className="settings-placeholder">
            <h2>Settings</h2>
            <p>Settings panel coming soon...</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default UserProfile;
