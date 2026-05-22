CREATE TABLE notification_dispatch_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    trigger_type VARCHAR(60) NOT NULL,
    target_id BIGINT NULL,
    schedule_key VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    processed_at DATETIME NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_dispatch_dedup UNIQUE (user_id, trigger_type, target_id, schedule_key)
);

CREATE INDEX idx_notification_dispatch_status_next_retry_at
    ON notification_dispatch_histories (status, next_retry_at);

CREATE INDEX idx_notification_dispatch_user_trigger_processed_at
    ON notification_dispatch_histories (user_id, trigger_type, processed_at);
