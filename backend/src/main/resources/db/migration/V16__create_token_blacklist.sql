-- Migration: Create token_blacklist table for token revocation
-- Phase 3: IDRQ-RNF-01 (Seguridad - Token Blacklist)

CREATE TABLE token_blacklist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(64) NOT NULL, -- SHA-256 hash
    token_type VARCHAR(20) NOT NULL, -- ACCESS or REFRESH
    user_id UUID REFERENCES usuarios(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_by UUID REFERENCES usuarios(id),
    reason VARCHAR(100), -- LOGOUT, ADMIN_REVOKE, TOKEN_ROTATION, SECURITY_BREACH, etc.
    CONSTRAINT uk_token_blacklist_hash UNIQUE(token_hash)
);

-- Indexes for performance
CREATE INDEX idx_token_blacklist_hash ON token_blacklist(token_hash);
CREATE INDEX idx_token_blacklist_expires ON token_blacklist(expires_at);
CREATE INDEX idx_token_blacklist_user ON token_blacklist(user_id);

-- Comments
COMMENT ON TABLE token_blacklist IS 'Stores revoked tokens to prevent reuse';
COMMENT ON COLUMN token_blacklist.token_hash IS 'SHA-256 hash of the revoked token';
COMMENT ON COLUMN token_blacklist.token_type IS 'Type of token: ACCESS or REFRESH';
COMMENT ON COLUMN token_blacklist.reason IS 'Reason for revocation for audit purposes';
