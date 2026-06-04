CREATE TABLE payment_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_order_id BIGINT NULL,
    order_id VARCHAR(64) NULL,
    pg_payment_id VARCHAR(200) NULL,
    event_type VARCHAR(50) NOT NULL,
    source VARCHAR(30) NOT NULL,
    previous_order_status VARCHAR(30) NULL,
    current_order_status VARCHAR(30) NULL,
    pg_status VARCHAR(50) NULL,
    reason VARCHAR(500) NULL,
    raw_payload TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_audit_logs_payment_order_id FOREIGN KEY (payment_order_id) REFERENCES payment_orders (id)
);

CREATE INDEX idx_payment_audit_logs_order_id ON payment_audit_logs (order_id);
CREATE INDEX idx_payment_audit_logs_payment_order_id ON payment_audit_logs (payment_order_id);
CREATE INDEX idx_payment_audit_logs_event_created ON payment_audit_logs (event_type, created_at);
