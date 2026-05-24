ALTER TABLE notifications
    ADD COLUMN trigger_type VARCHAR(50) NULL AFTER deeplink_type;
