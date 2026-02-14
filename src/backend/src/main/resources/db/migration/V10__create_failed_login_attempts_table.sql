-- Create table for tracking failed login attempts
-- Used for security monitoring and brute force detection

CREATE TABLE failed_login_attempts (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    attempted_at TIMESTAMP NOT NULL,
    flagged BOOLEAN DEFAULT FALSE
);

-- Indexes for efficient querying
CREATE INDEX idx_failed_login_username ON failed_login_attempts(username);
CREATE INDEX idx_failed_login_ip ON failed_login_attempts(ip_address);
CREATE INDEX idx_failed_login_timestamp ON failed_login_attempts(attempted_at);

-- Comments for documentation
COMMENT ON TABLE failed_login_attempts IS 'Tracks failed login attempts for security monitoring';
COMMENT ON COLUMN failed_login_attempts.username IS 'Username that was attempted';
COMMENT ON COLUMN failed_login_attempts.ip_address IS 'IP address of the failed attempt';
COMMENT ON COLUMN failed_login_attempts.user_agent IS 'Browser/client user agent string';
COMMENT ON COLUMN failed_login_attempts.attempted_at IS 'Timestamp of the failed attempt';
COMMENT ON COLUMN failed_login_attempts.flagged IS 'True if this attempt triggered security flags (multiple failures)';
