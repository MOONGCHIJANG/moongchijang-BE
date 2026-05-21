ALTER TABLE participation
    ADD COLUMN cancel_reason VARCHAR(50) NULL AFTER picked_up_at,
    ADD COLUMN cancel_reason_detail VARCHAR(500) NULL AFTER cancel_reason;
