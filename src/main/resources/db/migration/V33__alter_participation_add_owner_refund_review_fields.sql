ALTER TABLE participation
    ADD COLUMN owner_refund_review_status VARCHAR(30) NULL AFTER refunded_at,
    ADD COLUMN owner_refund_dispute_reason VARCHAR(500) NULL AFTER owner_refund_review_status,
    ADD COLUMN owner_refund_reviewed_at DATETIME NULL AFTER owner_refund_dispute_reason;

