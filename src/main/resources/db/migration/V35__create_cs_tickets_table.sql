CREATE TABLE cs_tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    consumer_id BIGINT NULL,
    group_buy_id BIGINT NULL,
    refund_participation_id BIGINT NULL,
    assignee_name VARCHAR(50) NULL,
    processing_memo VARCHAR(1000) NULL,
    resolved_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cs_tickets_consumer_id FOREIGN KEY (consumer_id) REFERENCES users (id),
    CONSTRAINT fk_cs_tickets_group_buy_id FOREIGN KEY (group_buy_id) REFERENCES group_buys (id),
    CONSTRAINT fk_cs_tickets_refund_participation_id FOREIGN KEY (refund_participation_id) REFERENCES participation (id)
);

CREATE INDEX idx_cs_tickets_status_created_at ON cs_tickets (status, created_at);
CREATE INDEX idx_cs_tickets_consumer_id ON cs_tickets (consumer_id);
CREATE INDEX idx_cs_tickets_group_buy_id ON cs_tickets (group_buy_id);
