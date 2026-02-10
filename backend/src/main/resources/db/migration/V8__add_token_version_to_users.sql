-- Add token_version column to users table for JWT invalidation
-- This allows invalidating all existing JWTs when a user resets their password

ALTER TABLE users 
ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

-- Add comment to explain the column
COMMENT ON COLUMN users.token_version IS 'Version number incremented on password reset to invalidate existing JWTs';
