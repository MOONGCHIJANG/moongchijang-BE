ALTER TABLE group_buys
    ADD COLUMN close_request_review_status VARCHAR(20) NULL AFTER close_requested_at,
    ADD COLUMN close_request_rejection_reason VARCHAR(200) NULL AFTER close_request_review_status,
    ADD COLUMN close_reviewed_at DATETIME NULL AFTER close_request_rejection_reason;

CREATE INDEX idx_group_buys_close_request_review_status ON group_buys (close_request_review_status);
