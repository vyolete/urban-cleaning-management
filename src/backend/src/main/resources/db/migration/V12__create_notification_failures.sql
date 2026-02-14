-- Create notification_failures table for tracking failed email notifications
CREATE TABLE notification_failures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    failure_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_failures_user FOREIGN KEY (user_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Create indexes for efficient querying
CREATE INDEX idx_notification_failures_user ON notification_failures(user_id);
CREATE INDEX idx_notification_failures_attempted ON notification_failures(attempted_at);
CREATE INDEX idx_notification_failures_type ON notification_failures(notification_type);

-- Add comments to table
COMMENT ON TABLE notification_failures IS 'Tracks failed email notification attempts for debugging and retry';
COMMENT ON COLUMN notification_failures.notification_type IS 'Type of notification: TASK_ASSIGNED, TASK_RESOLVED, TASK_REOPENED, REPORT_CREATED';
COMMENT ON COLUMN notification_failures.retry_count IS 'Number of retry attempts made';
COMMENT ON COLUMN notification_failures.attempted_at IS 'Timestamp of the last attempt';
