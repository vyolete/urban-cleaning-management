-- ============================================================================
-- Multi-Country Support Rollback Script
-- ============================================================================
-- This script rolls back the multi-country support changes
-- WARNING: This will remove country associations from reports and tasks
-- ============================================================================

-- Remove country_id column from tareas table
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tareas' AND column_name = 'country_id'
    ) THEN
        -- Drop index first
        DROP INDEX IF EXISTS idx_task_country;
        
        -- Drop column
        ALTER TABLE tareas DROP COLUMN country_id;
        
        RAISE NOTICE 'Removed country_id column from tareas table';
    ELSE
        RAISE NOTICE 'country_id column does not exist in tareas table';
    END IF;
END $$;

-- Remove country_id column from reportes table
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'reportes' AND column_name = 'country_id'
    ) THEN
        -- Drop index first
        DROP INDEX IF EXISTS idx_report_country;
        
        -- Drop column
        ALTER TABLE reportes DROP COLUMN country_id;
        
        RAISE NOTICE 'Removed country_id column from reportes table';
    ELSE
        RAISE NOTICE 'country_id column does not exist in reportes table';
    END IF;
END $$;

-- Drop countries table
DROP TABLE IF EXISTS countries CASCADE;

-- Drop indexes (if they still exist)
DROP INDEX IF EXISTS idx_countries_default;
DROP INDEX IF EXISTS idx_countries_enabled;
DROP INDEX IF EXISTS idx_countries_code;

-- ============================================================================
-- Completion Message
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '============================================================================';
    RAISE NOTICE 'Multi-country support rollback completed successfully!';
    RAISE NOTICE 'Countries table and related columns have been removed';
    RAISE NOTICE 'Database restored to single-country configuration';
    RAISE NOTICE '============================================================================';
END $$;
