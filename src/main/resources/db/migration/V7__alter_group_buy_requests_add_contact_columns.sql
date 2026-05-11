UPDATE group_buy_requests
SET status = CASE status
    WHEN 'SUBMITTED' THEN 'IN_REVIEW'
    WHEN 'REVIEWING' THEN 'IN_REVIEW'
    WHEN 'CONTACTING' THEN 'IN_CONTACT'
    ELSE status
END;

ALTER TABLE group_buy_requests
    ADD COLUMN contact_phone VARCHAR(20) NULL AFTER rejection_reason,
    ADD COLUMN contact_instagram VARCHAR(50) NULL AFTER contact_phone,
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_REVIEW',
    MODIFY COLUMN rejection_reason VARCHAR(100) NULL;

UPDATE group_buy_request_status_histories
SET status = CASE status
    WHEN 'SUBMITTED' THEN 'IN_REVIEW'
    WHEN 'REVIEWING' THEN 'IN_REVIEW'
    WHEN 'CONTACTING' THEN 'IN_CONTACT'
    ELSE status
END;
