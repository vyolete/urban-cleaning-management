-- ============================================================================
-- Migration V18: Extend Algorithm Configuration Table
-- ============================================================================
-- This migration extends the configuracion_algoritmo table to support
-- different configuration types (TOKEN_EXPIRATION, DUPLICATE_DETECTION, etc.)
-- and adds tracking fields for configuration management
-- ============================================================================

-- Add config_type column to distinguish different configuration types
ALTER TABLE configuracion_algoritmo 
ADD COLUMN IF NOT EXISTS config_type VARCHAR(50) DEFAULT 'ALGORITHM_WEIGHTS';

-- Add updated_by column to track who made configuration changes
ALTER TABLE configuracion_algoritmo 
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES usuarios(id);

-- Create index on config_type for efficient filtering
CREATE INDEX IF NOT EXISTS idx_config_type ON configuracion_algoritmo(config_type);

-- Create index on effective_from for temporal queries
CREATE INDEX IF NOT EXISTS idx_config_effective_from ON configuracion_algoritmo(effective_from);

-- Update existing records to have the default config_type
UPDATE configuracion_algoritmo 
SET config_type = 'ALGORITHM_WEIGHTS' 
WHERE config_type IS NULL;

-- Make config_type NOT NULL after setting defaults
ALTER TABLE configuracion_algoritmo 
ALTER COLUMN config_type SET NOT NULL;

-- Add comment to table
COMMENT ON TABLE configuracion_algoritmo IS 'Stores system configuration parameters including algorithm weights, token expiration, and duplicate detection settings';

-- Add comments to new columns
COMMENT ON COLUMN configuracion_algoritmo.config_type IS 'Type of configuration: ALGORITHM_WEIGHTS, TOKEN_EXPIRATION, DUPLICATE_DETECTION';
COMMENT ON COLUMN configuracion_algoritmo.updated_by IS 'User who last updated this configuration';

-- ============================================================================
-- Verification
-- ============================================================================

-- Verify the migration
DO $
BEGIN
    RAISE NOTICE 'Migration V18 completed successfully';
    RAISE NOTICE 'Added config_type and updated_by columns to configuracion_algoritmo';
    RAISE NOTICE 'Created indexes on config_type and effective_from';
END $;
