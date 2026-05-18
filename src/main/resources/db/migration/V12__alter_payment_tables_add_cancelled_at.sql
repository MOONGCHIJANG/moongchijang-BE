ALTER TABLE payment_orders
    ADD COLUMN cancelled_at DATETIME NULL;

ALTER TABLE payments
    ADD COLUMN cancelled_at DATETIME NULL;
