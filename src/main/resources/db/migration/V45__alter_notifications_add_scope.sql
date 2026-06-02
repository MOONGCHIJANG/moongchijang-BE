ALTER TABLE notifications
    ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'BUYER' AFTER type;

CREATE INDEX idx_notifications_user_scope_occurred_at ON notifications (user_id, scope, occurred_at DESC);
