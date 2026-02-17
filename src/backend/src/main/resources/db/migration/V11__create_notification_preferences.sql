-- Create notification_preferences table for user notification settings
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    task_assigned BOOLEAN DEFAULT TRUE,
    task_resolved BOOLEAN DEFAULT TRUE,
    task_reopened BOOLEAN DEFAULT TRUE,
    report_created BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_notification_preferences_user UNIQUE (user_id)
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_notification_preferences_user ON notification_preferences(user_id);

-- Add comment to table
COMMENT ON TABLE notification_preferences IS 'Stores user notification preferences for different event types';
COMMENT ON COLUMN notification_preferences.task_assigned IS 'Enable/disable notifications when a task is assigned to the user';
COMMENT ON COLUMN notification_preferences.task_resolved IS 'Enable/disable notifications when a task is resolved';
COMMENT ON COLUMN notification_preferences.task_reopened IS 'Enable/disable notifications when a task is reopened';
COMMENT ON COLUMN notification_preferences.report_created IS 'Enable/disable notifications when a report is created';
