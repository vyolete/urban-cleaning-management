-- ============================================================================
-- Urban Cleaning Management System - Database Initialization Script
-- ============================================================================
-- This script initializes the PostgreSQL database with PostGIS extension
-- and creates necessary spatial indexes for optimal performance
-- ============================================================================

-- Enable PostGIS extension for spatial data support
CREATE EXTENSION IF NOT EXISTS postgis;

-- Verify PostGIS installation
SELECT PostGIS_Version();

-- ============================================================================
-- Create custom types (if needed)
-- ============================================================================

-- Task state enum (will be created by Hibernate, but defined here for reference)
-- DO $$ BEGIN
--     CREATE TYPE task_state AS ENUM ('PENDIENTE', 'ASIGNADO', 'EN_PROGRESO', 'RESUELTO');
-- EXCEPTION
--     WHEN duplicate_object THEN null;
-- END $$;

-- User role enum (will be created by Hibernate, but defined here for reference)
-- DO $$ BEGIN
--     CREATE TYPE user_role AS ENUM ('ROLE_CIUDADANO', 'ROLE_TECNICO', 'ROLE_ADMIN');
-- EXCEPTION
--     WHEN duplicate_object THEN null;
-- END $$;

-- ============================================================================
-- Spatial Indexes
-- ============================================================================
-- Note: These will be created automatically by Hibernate with @Index annotations
-- but are included here for reference and manual creation if needed

-- Spatial index for report locations (for proximity queries)
-- CREATE INDEX IF NOT EXISTS idx_report_location ON reportes USING GIST(location);

-- Spatial index for task locations (for zone filtering and proximity)
-- CREATE INDEX IF NOT EXISTS idx_task_location ON tareas USING GIST(location);

-- ============================================================================
-- Performance Indexes
-- ============================================================================
-- Additional indexes for query optimization (created by Hibernate)

-- Index for task state filtering
-- CREATE INDEX IF NOT EXISTS idx_task_state ON tareas(state);

-- Index for task priority ordering
-- CREATE INDEX IF NOT EXISTS idx_task_priority ON tareas(priority_score DESC);

-- Index for audit log chronological queries
-- CREATE INDEX IF NOT EXISTS idx_audit_log_task_time ON audit_logs(task_id, changed_at);

-- Index for user authentication
-- CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);

-- ============================================================================
-- Default Data
-- ============================================================================

-- Insert default admin user (password: admin123 - CHANGE IN PRODUCTION!)
-- Note: Password hash is BCrypt encoded
-- This will be handled by the application or manual insertion
-- INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
-- VALUES (
--     gen_random_uuid(),
--     'admin',
--     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- admin123
--     'admin@urbanclean.com',
--     'ROLE_ADMIN',
--     NOW(),
--     NOW()
-- )
-- ON CONFLICT (username) DO NOTHING;

-- Insert default algorithm configuration
-- This will be handled by the application on first startup
-- INSERT INTO algorithm_configs (
--     id,
--     weight_category,
--     weight_zone,
--     weight_time,
--     deduplication_distance_meters,
--     deduplication_time_window_hours,
--     effective_from,
--     created_at
-- )
-- VALUES (
--     gen_random_uuid(),
--     0.40,
--     0.35,
--     0.25,
--     50.0,
--     24,
--     NOW(),
--     NOW()
-- )
-- ON CONFLICT DO NOTHING;

-- ============================================================================
-- Database Configuration
-- ============================================================================

-- Set timezone to UTC for consistency
SET timezone = 'UTC';

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Verification Queries
-- ============================================================================

-- Verify PostGIS is working
SELECT 'PostGIS Extension Enabled' AS status, PostGIS_Version() AS version;

-- Verify UUID extension
SELECT 'UUID Extension Enabled' AS status, uuid_generate_v4() AS sample_uuid;

-- Show available spatial reference systems (SRID 4326 is WGS84 - used for GPS coordinates)
SELECT 'SRID 4326 (WGS84) Available' AS status, srtext 
FROM spatial_ref_sys 
WHERE srid = 4326;

-- ============================================================================
-- Multi-Country Support Schema
-- ============================================================================

-- Create countries table
CREATE TABLE IF NOT EXISTS countries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(3) NOT NULL UNIQUE,  -- ISO 3166-1 alpha-3 code
    default_country BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    -- Geofencing boundaries
    min_lat DECIMAL(10, 8) NOT NULL,
    max_lat DECIMAL(10, 8) NOT NULL,
    min_lon DECIMAL(11, 8) NOT NULL,
    max_lon DECIMAL(11, 8) NOT NULL,
    -- Administrative divisions
    administrative_area VARCHAR(100),
    municipality VARCHAR(100),
    -- Geographic center for map centering
    center_lat DECIMAL(10, 8),
    center_lon DECIMAL(11, 8),
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes for countries table
CREATE INDEX IF NOT EXISTS idx_countries_default ON countries(default_country);
CREATE INDEX IF NOT EXISTS idx_countries_enabled ON countries(enabled);
CREATE INDEX IF NOT EXISTS idx_countries_code ON countries(code);

-- Insert default country (Spain/Madrid)
INSERT INTO countries (
    name, code, default_country, enabled,
    min_lat, max_lat, min_lon, max_lon,
    administrative_area, municipality,
    center_lat, center_lon
) VALUES (
    'España',
    'ESP',
    TRUE,
    TRUE,
    36.0, 43.8, -9.3, 3.3,  -- Spain boundaries
    'Comunidad de Madrid',
    'Madrid',
    40.4168, -3.7038  -- Madrid center
)
ON CONFLICT (code) DO NOTHING;

-- Insert Colombia as second country
INSERT INTO countries (
    name, code, default_country, enabled,
    min_lat, max_lat, min_lon, max_lon,
    administrative_area, municipality,
    center_lat, center_lon
) VALUES (
    'Colombia',
    'COL',
    FALSE,
    TRUE,
    -4.2, 12.5, -79.0, -66.9,  -- Colombia boundaries
    'Cundinamarca',
    'Bogotá',
    4.7110, -74.0721  -- Bogotá center
)
ON CONFLICT (code) DO NOTHING;

-- Insert United States as third country
INSERT INTO countries (
    name, code, default_country, enabled,
    min_lat, max_lat, min_lon, max_lon,
    administrative_area, municipality,
    center_lat, center_lon
) VALUES (
    'United States',
    'USA',
    FALSE,
    TRUE,
    24.5, 49.4, -125.0, -66.9,  -- USA boundaries
    'New York',
    'New York City',
    40.7128, -74.0060  -- NYC center
)
ON CONFLICT (code) DO NOTHING;

-- Add country_id column to reportes table (if it doesn't exist)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'reportes' AND column_name = 'country_id'
    ) THEN
        ALTER TABLE reportes ADD COLUMN country_id UUID REFERENCES countries(id);
        
        -- Create index for country-based filtering
        CREATE INDEX idx_report_country ON reportes(country_id);
        
        RAISE NOTICE 'Added country_id column to reportes table';
    ELSE
        RAISE NOTICE 'country_id column already exists in reportes table';
    END IF;
END $$;

-- Migrate existing reports to default country (Spain)
DO $$
DECLARE
    default_country_id UUID;
    updated_count INTEGER;
BEGIN
    -- Get default country ID
    SELECT id INTO default_country_id FROM countries WHERE default_country = TRUE LIMIT 1;
    
    IF default_country_id IS NOT NULL THEN
        -- Update reports without country_id
        UPDATE reportes 
        SET country_id = default_country_id 
        WHERE country_id IS NULL;
        
        GET DIAGNOSTICS updated_count = ROW_COUNT;
        
        RAISE NOTICE 'Migrated % existing reports to default country', updated_count;
    ELSE
        RAISE NOTICE 'No default country found, skipping migration';
    END IF;
END $$;

-- Add country_id column to tareas table (if it doesn't exist)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tareas' AND column_name = 'country_id'
    ) THEN
        ALTER TABLE tareas ADD COLUMN country_id UUID REFERENCES countries(id);
        
        -- Create index for country-based filtering
        CREATE INDEX idx_task_country ON tareas(country_id);
        
        RAISE NOTICE 'Added country_id column to tareas table';
    ELSE
        RAISE NOTICE 'country_id column already exists in tareas table';
    END IF;
END $$;

-- Migrate existing tasks to default country (Spain)
DO $$
DECLARE
    default_country_id UUID;
    updated_count INTEGER;
BEGIN
    -- Get default country ID
    SELECT id INTO default_country_id FROM countries WHERE default_country = TRUE LIMIT 1;
    
    IF default_country_id IS NOT NULL THEN
        -- Update tasks without country_id
        UPDATE tareas 
        SET country_id = default_country_id 
        WHERE country_id IS NULL;
        
        GET DIAGNOSTICS updated_count = ROW_COUNT;
        
        RAISE NOTICE 'Migrated % existing tasks to default country', updated_count;
    ELSE
        RAISE NOTICE 'No default country found, skipping migration';
    END IF;
END $$;

-- ============================================================================
-- Completion Message
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '============================================================================';
    RAISE NOTICE 'Database initialization completed successfully!';
    RAISE NOTICE 'PostGIS extension enabled for spatial data support';
    RAISE NOTICE 'UUID extension enabled for primary key generation';
    RAISE NOTICE 'Multi-country support schema created';
    RAISE NOTICE 'Default countries: Spain (ESP), Colombia (COL), United States (USA)';
    RAISE NOTICE 'Database is ready for Urban Cleaning Management System';
    RAISE NOTICE '============================================================================';
END $$;
