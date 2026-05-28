CREATE TABLE payment_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    group_buy_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    product_amount INT NOT NULL,
    fee_amount INT NOT NULL,
    total_amount INT NOT NULL,
    agreed_no_cancel_after_goal BOOLEAN NOT NULL,
    agreed_refund_before_goal BOOLEAN NOT NULL,
    agreed_no_refund_after_no_show BOOLEAN NOT NULL,
    agreed_no_withdrawal BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    approved_at DATETIME NULL,
    failed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_orders_order_id UNIQUE (order_id),
    CONSTRAINT fk_payment_orders_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_payment_orders_group_buy_id FOREIGN KEY (group_buy_id) REFERENCES group_buys (id)
);

CREATE INDEX idx_payment_orders_user_id ON payment_orders (user_id);
CREATE INDEX idx_payment_orders_group_buy_id ON payment_orders (group_buy_id);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_order_id BIGINT NOT NULL,
    pg_payment_id VARCHAR(200) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    amount INT NOT NULL,
    method VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL,
    approved_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_pg_payment_id UNIQUE (pg_payment_id),
    CONSTRAINT uk_payments_payment_order_id UNIQUE (payment_order_id),
    CONSTRAINT fk_payments_payment_order_id FOREIGN KEY (payment_order_id) REFERENCES payment_orders (id)
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
