-- Migration: Add feedback-related fields to tasks table
-- This migration is for documentation purposes
-- Hibernate will auto-create the columns based on the entity

-- Add new columns for task feedback and reopening
ALTER TABLE tareas 
ADD COLUMN IF NOT EXISTS resolution_evidence VARCHAR(1000),
ADD COLUMN IF NOT EXISTS reopen_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS citizen_approved BOOLEAN DEFAULT FALSE;

-- Update TaskState enum to include REABIERTO
-- Note: Postgres will handle enum updates automatically via Hibernate

-- Comments
COMMENT ON COLUMN tareas.resolution_evidence IS 'Evidence provided by operator when marking task as resolved';
COMMENT ON COLUMN tareas.reopen_count IS 'Number of times this task has been reopened by citizen';
COMMENT ON COLUMN tareas.citizen_approved IS 'Whether citizen has approved the resolution';
