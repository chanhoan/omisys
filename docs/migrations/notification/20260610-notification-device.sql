ALTER TABLE notifications ADD COLUMN IF NOT EXISTS device_id VARCHAR(128) NULL;

CREATE INDEX idx_notifications_device_id ON notifications (device_id);
