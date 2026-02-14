-- Migration: Create password_reset_tokens table
-- This migration is for documentation purposes
-- Hibernate will auto-create the table based on the entity

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    
    CONSTRAINT fk_password_reset_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_token ON password_reset_tokens(token);
CREATE INDEX IF NOT EXISTS idx_user_id ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_expires_at ON password_reset_tokens(expires_at);

-- Comments
COMMENT ON TABLE password_reset_tokens IS 'Stores password reset tokens with 1-hour expiration';
COMMENT ON COLUMN password_reset_tokens.token IS 'Unique token sent to user email';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Token expiration timestamp (1 hour from creation)';
COMMENT ON COLUMN password_reset_tokens.used IS 'Whether token has been used';
COMMENT ON COLUMN password_reset_tokens.ip_address IS 'IP address of the request (for audit)';
