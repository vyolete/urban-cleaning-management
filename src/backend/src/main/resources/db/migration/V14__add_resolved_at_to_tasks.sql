-- Migration V14: Add resolved_at timestamp to tasks
-- Purpose: Track when tasks are resolved for MTTR calculation
-- Date: 2026-02-09

ALTER TABLE tareas ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP;

-- Create index for analytics queries
CREATE INDEX IF NOT EXISTS idx_tareas_resolved_at ON tareas(resolved_at) WHERE resolved_at IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN tareas.resolved_at IS 'Timestamp when task was marked as resolved (for MTTR calculation)';
