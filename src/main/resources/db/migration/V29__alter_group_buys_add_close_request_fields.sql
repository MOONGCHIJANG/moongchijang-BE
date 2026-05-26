ALTER TABLE group_buys
    ADD COLUMN close_reason VARCHAR(30) NULL,
    ADD COLUMN close_reason_detail VARCHAR(100) NULL,
    ADD COLUMN close_requested_at DATETIME NULL;
