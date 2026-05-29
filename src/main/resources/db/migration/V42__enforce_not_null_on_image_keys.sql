ALTER TABLE group_buys
    MODIFY COLUMN thumbnail_key VARCHAR(500) NOT NULL;

ALTER TABLE group_buy_images
    MODIFY COLUMN image_key VARCHAR(500) NOT NULL;

ALTER TABLE owner_group_buy_requests
    MODIFY COLUMN thumbnail_key VARCHAR(500) NOT NULL;

ALTER TABLE owner_group_buy_request_images
    MODIFY COLUMN image_key VARCHAR(500) NOT NULL;
