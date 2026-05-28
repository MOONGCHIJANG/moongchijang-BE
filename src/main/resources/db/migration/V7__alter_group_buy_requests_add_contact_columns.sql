ALTER TABLE group_buy_requests
    ADD COLUMN contact_phone VARCHAR(20) NULL,
    ADD COLUMN contact_instagram VARCHAR(50) NULL,
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_REVIEW';

ALTER TABLE group_buy_request_status_histories
    MODIFY COLUMN status VARCHAR(20) NOT NULL;

UPDATE group_buy_requests
SET status = CASE status
    WHEN 'SUBMITTED' THEN 'IN_REVIEW'
    WHEN 'REVIEWING' THEN 'IN_REVIEW'
    WHEN 'CONTACTING' THEN 'IN_CONTACT'
    ELSE status
END;

UPDATE group_buy_request_status_histories
SET status = CASE status
    WHEN 'SUBMITTED' THEN 'IN_REVIEW'
    WHEN 'REVIEWING' THEN 'IN_REVIEW'
    WHEN 'CONTACTING' THEN 'IN_CONTACT'
    ELSE status
END;
