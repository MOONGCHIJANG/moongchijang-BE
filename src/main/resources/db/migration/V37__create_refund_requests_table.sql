CREATE TABLE refund_requests
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    participation_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_amount INT NOT NULL,
    approved_refund_amount INT NULL,
    rejected_reason VARCHAR(200) NULL,
    requested_at DATETIME NOT NULL,
    approved_at DATETIME NULL,
    rejected_at DATETIME NULL,
    refunded_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_requests_participation_id (participation_id),
    CONSTRAINT fk_refund_requests_participation
        FOREIGN KEY (participation_id) REFERENCES participation (id)
);
