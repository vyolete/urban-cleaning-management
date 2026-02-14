-- ============================================================================
-- Migration V19: Add Token Expiration Configuration Columns
-- ============================================================================
-- This migration adds columns for storing JWT token expiration settings
-- ============================================================================

-- Add token expiration columns
ALTER TABLE configuracion_algoritmo 
ADD COLUMN IF NOT EXISTS access_token_expiration_minutes INTEGER;

ALTER TABLE configuracion_algoritmo 
ADD COLUMN IF NOT EXISTS refresh_token_expiration_days INTEGER;

-- Add comments
COMMENT ON COLUMN configuracion_algoritmo.access_token_expiration_minutes IS 'Access token expiration time in minutes (5-60)';
COMMENT ON COLUMN configuracion_algoritmo.refresh_token_expiration_days IS 'Refresh token expiration time in days (1-30)';

-- Insert default token expiration configuration
INSERT INTO configuracion_algoritmo (
    id,
    config_type,
    access_token_expiration_minutes,
    refresh_token_expiration_days,
    effective_from,
    weight_category,
    weight_zone,
    weight_time,
    distance_threshold_meters,
    time_window_hours
)
VALUES (
    gen_random_uuid(),
    'TOKEN_EXPIRATION',
    15,  -- 15 minutes for access token
    7,   -- 7 days for refresh token
    NOW(),
    0.40,  -- Default values (not used for this config type)
    0.35,
    0.25,
    50.0,
    24
)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- Verification
-- ============================================================================

DO $
BEGIN
    RAISE NOTICE 'Migration V19 completed successfully';
    RAISE NOTICE 'Added token expiration columns to configuracion_algoritmo';
    RAISE NOTICE 'Inserted default TOKEN_EXPIRATION configuration';
END $;
