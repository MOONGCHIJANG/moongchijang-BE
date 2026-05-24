ALTER TABLE notifications
    ADD COLUMN trigger_type VARCHAR(50) NULL AFTER deeplink_type;

CREATE INDEX idx_notifications_user_trigger_type_occurred_at
    ON notifications (user_id, trigger_type, occurred_at DESC);
