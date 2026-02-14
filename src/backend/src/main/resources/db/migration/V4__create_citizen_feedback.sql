-- Migration: Create citizen_feedback table
-- This migration is for documentation purposes
-- Hibernate will auto-create the table based on the entity

CREATE TABLE IF NOT EXISTS citizen_feedback (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL UNIQUE,
    citizen_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    justification VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL,
    feedback_deadline TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_feedback_task 
        FOREIGN KEY (task_id) 
        REFERENCES tareas(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_feedback_citizen 
        FOREIGN KEY (citizen_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT uk_feedback_task UNIQUE (task_id)
);

-- Index for deadline queries (auto-close job)
CREATE INDEX IF NOT EXISTS idx_feedback_deadline ON citizen_feedback(feedback_deadline);

-- Comments
COMMENT ON TABLE citizen_feedback IS 'Stores citizen feedback on task resolutions';
COMMENT ON COLUMN citizen_feedback.type IS 'CONFIRMED or REJECTED';
COMMENT ON COLUMN citizen_feedback.justification IS 'Required when type is REJECTED';
COMMENT ON COLUMN citizen_feedback.feedback_deadline IS '72 hours from task resolution';
