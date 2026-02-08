-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create spatial indexes (will be created by Hibernate, but included here for reference)
-- CREATE INDEX idx_report_location ON reportes USING GIST(location);
-- CREATE INDEX idx_task_location ON tareas USING GIST(location);

-- Insert default algorithm configuration
-- This will be handled by the application on first startup
