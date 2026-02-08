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
-- Completion Message
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '============================================================================';
    RAISE NOTICE 'Database initialization completed successfully!';
    RAISE NOTICE 'PostGIS extension enabled for spatial data support';
    RAISE NOTICE 'UUID extension enabled for primary key generation';
    RAISE NOTICE 'Database is ready for Urban Cleaning Management System';
    RAISE NOTICE '============================================================================';
END $$;
