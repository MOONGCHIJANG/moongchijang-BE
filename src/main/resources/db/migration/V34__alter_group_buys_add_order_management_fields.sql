ALTER TABLE group_buys
    ADD COLUMN achieved_at DATETIME NULL AFTER closed_by_type,
    ADD COLUMN order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' AFTER achieved_at,
    ADD COLUMN order_owner_contacted_at DATETIME NULL AFTER order_status,
    ADD COLUMN order_confirmed_at DATETIME NULL AFTER order_owner_contacted_at,
    ADD COLUMN order_cancelled_at DATETIME NULL AFTER order_confirmed_at;
