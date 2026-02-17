-- Migration: Create user_sessions table for multi-device session management
-- Phase 3: IDRQ-RNF-01 (Seguridad - Multi-Device Sessions)

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    refresh_token_id UUID REFERENCES refresh_tokens(id) ON DELETE CASCADE,
    device_fingerprint VARCHAR(255),
    device_type VARCHAR(50), -- MOBILE, DESKTOP, TABLET
    browser VARCHAR(100),
    os VARCHAR(100),
    ip_address VARCHAR(45),
    city VARCHAR(100),
    country VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_user_session_refresh_token UNIQUE(refresh_token_id)
);

-- Indexes for performance
CREATE INDEX idx_user_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_active ON user_sessions(active) WHERE active = TRUE;
CREATE INDEX idx_user_sessions_last_activity ON user_sessions(last_activity);
CREATE INDEX idx_user_sessions_refresh_token ON user_sessions(refresh_token_id);

-- Comments
COMMENT ON TABLE user_sessions IS 'Tracks active user sessions across multiple devices';
COMMENT ON COLUMN user_sessions.device_fingerprint IS 'Unique identifier for the device/browser combination';
COMMENT ON COLUMN user_sessions.device_type IS 'Type of device: MOBILE, DESKTOP, or TABLET';
COMMENT ON COLUMN user_sessions.active IS 'True if session is currently active';
COMMENT ON COLUMN user_sessions.last_activity IS 'Timestamp of last API request from this session';
