-- Migration: Add GDPR-related fields to users table
-- This migration is for documentation purposes
-- Hibernate will auto-create the columns based on the entity

-- Add GDPR fields
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS anonymized BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS original_email_hash VARCHAR(64);

-- Comments
COMMENT ON COLUMN users.deleted_at IS 'Timestamp when user requested account deletion (7-day grace period)';
COMMENT ON COLUMN users.anonymized IS 'Whether user data has been anonymized (GDPR compliance)';
COMMENT ON COLUMN users.original_email_hash IS 'SHA-256 hash of original email for audit trail after anonymization';
