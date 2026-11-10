ALTER TABLE group_buy_requests
    MODIFY COLUMN desired_quantity INT NULL,
    MODIFY COLUMN desired_pickup_date DATE NULL;
