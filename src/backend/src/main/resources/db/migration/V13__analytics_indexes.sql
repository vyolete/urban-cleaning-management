-- Migration V13: Analytics Performance Indexes
-- Purpose: Optimize analytics queries for task distribution, MTTR, and heatmap generation
-- Date: 2026-02-09

-- Indexes for task analytics queries
CREATE INDEX IF NOT EXISTS idx_tareas_created_at ON tareas(created_at);
CREATE INDEX IF NOT EXISTS idx_tareas_state_created ON tareas(state, created_at);
CREATE INDEX IF NOT EXISTS idx_tareas_category_created ON tareas(category, created_at);
CREATE INDEX IF NOT EXISTS idx_tareas_assigned_to ON tareas(assigned_to);
CREATE INDEX IF NOT EXISTS idx_tareas_resolved_at ON tareas(resolved_at) WHERE resolved_at IS NOT NULL;

-- Spatial indexes for heatmap generation (PostGIS)
CREATE INDEX IF NOT EXISTS idx_reportes_location_gist ON reportes USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_reportes_created_location ON reportes(created_at, category) INCLUDE (location);

-- Comment for documentation
COMMENT ON INDEX idx_tareas_created_at IS 'Optimizes time-based filtering for analytics queries';
COMMENT ON INDEX idx_tareas_state_created IS 'Optimizes task distribution by state queries';
COMMENT ON INDEX idx_tareas_category_created IS 'Optimizes task distribution by category queries';
COMMENT ON INDEX idx_tareas_assigned_to IS 'Optimizes operator performance queries';
COMMENT ON INDEX idx_tareas_resolved_at IS 'Optimizes MTTR calculation queries';
COMMENT ON INDEX idx_reportes_location_gist IS 'Optimizes spatial queries for heatmap generation';
COMMENT ON INDEX idx_reportes_created_location IS 'Optimizes heatmap queries with time and category filters';
