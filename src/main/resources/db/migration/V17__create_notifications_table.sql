CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at DATETIME NOT NULL,
    target_id BIGINT NULL,
    deeplink_type VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_notifications_user_occurred_at ON notifications (user_id, occurred_at DESC);
CREATE INDEX idx_notifications_user_type_occurred_at ON notifications (user_id, type, occurred_at DESC);
CREATE INDEX idx_notifications_user_is_read_occurred_at ON notifications (user_id, is_read, occurred_at DESC);
