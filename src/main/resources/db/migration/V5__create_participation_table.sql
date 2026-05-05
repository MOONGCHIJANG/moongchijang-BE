CREATE TABLE participation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    group_buy_id BIGINT NOT NULL,
    pickup_processed_by BIGINT NULL,
    quantity INT NOT NULL,
    product_amount INT NOT NULL,
    fee_amount INT NOT NULL,
    total_amount INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    pickup_status VARCHAR(30) NOT NULL,
    pickup_token VARCHAR(100) NULL,
    picked_up_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    refunded_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_participation_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_participation_group_buy_id FOREIGN KEY (group_buy_id) REFERENCES group_buys (id),
    CONSTRAINT fk_participation_pickup_processed_by FOREIGN KEY (pickup_processed_by) REFERENCES users (id)
);

CREATE UNIQUE INDEX uk_participation_user_group_buy ON participation (user_id, group_buy_id);
CREATE INDEX idx_participation_user_id ON participation (user_id);
CREATE INDEX idx_participation_group_buy_id ON participation (group_buy_id);
