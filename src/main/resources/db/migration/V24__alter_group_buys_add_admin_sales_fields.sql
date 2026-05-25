ALTER TABLE group_buys
    ADD COLUMN original_price INT NULL AFTER price,
    ADD COLUMN per_user_limit INT NULL AFTER max_quantity,
    ADD COLUMN recruitment_start_at DATETIME NULL AFTER status,
    ADD COLUMN pickup_contact VARCHAR(20) NULL AFTER pickup_location;
