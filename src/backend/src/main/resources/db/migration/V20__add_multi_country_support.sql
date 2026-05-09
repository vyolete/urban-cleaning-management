-- ============================================================================
-- V20: Multi-Country Support Migration
-- ============================================================================
-- This migration adds multi-country support to the Urban Cleaning Management System
-- ============================================================================

-- Create countries table
CREATE TABLE countries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(3) NOT NULL UNIQUE,
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
CREATE INDEX idx_countries_default ON countries(default_country);
CREATE INDEX idx_countries_enabled ON countries(enabled);
CREATE INDEX idx_countries_code ON countries(code);

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
    36.0, 43.8, -9.3, 3.3,
    'Comunidad de Madrid',
    'Madrid',
    40.4168, -3.7038
);

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
    -4.2, 12.5, -79.0, -66.9,
    'Cundinamarca',
    'Bogotá',
    4.7110, -74.0721
);

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
    24.5, 49.4, -125.0, -66.9,
    'New York',
    'New York City',
    40.7128, -74.0060
);

-- Add country_id column to reportes table
ALTER TABLE reportes ADD COLUMN country_id UUID REFERENCES countries(id);

-- Create index for country-based filtering
CREATE INDEX idx_report_country ON reportes(country_id);

-- Migrate existing reports to default country (Spain)
UPDATE reportes 
SET country_id = (SELECT id FROM countries WHERE default_country = TRUE LIMIT 1)
WHERE country_id IS NULL;

-- Add country_id column to tareas table
ALTER TABLE tareas ADD COLUMN country_id UUID REFERENCES countries(id);

-- Create index for country-based filtering
CREATE INDEX idx_task_country ON tareas(country_id);

-- Migrate existing tasks to default country (Spain)
UPDATE tareas 
SET country_id = (SELECT id FROM countries WHERE default_country = TRUE LIMIT 1)
WHERE country_id IS NULL;
