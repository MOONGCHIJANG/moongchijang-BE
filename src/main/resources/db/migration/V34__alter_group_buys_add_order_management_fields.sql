ALTER TABLE group_buys
    ADD COLUMN achieved_at DATETIME NULL AFTER closed_by_type,
    ADD COLUMN order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' AFTER achieved_at,
    ADD COLUMN order_owner_contacted_at DATETIME NULL AFTER order_status,
    ADD COLUMN order_confirmed_at DATETIME NULL AFTER order_owner_contacted_at,
    ADD COLUMN order_cancelled_at DATETIME NULL AFTER order_confirmed_at;

UPDATE group_buys
SET achieved_at = COALESCE(updated_at, created_at)
WHERE status = 'ACHIEVED'
  AND achieved_at IS NULL;

CREATE INDEX idx_group_buys_order_lookup
    ON group_buys (status, order_status, achieved_at);
