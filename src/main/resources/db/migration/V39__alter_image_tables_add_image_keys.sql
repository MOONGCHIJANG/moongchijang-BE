ALTER TABLE group_buys
    ADD COLUMN thumbnail_key VARCHAR(500) NULL AFTER thumbnail_url;

ALTER TABLE group_buy_images
    ADD COLUMN image_key VARCHAR(500) NULL AFTER image_url;

ALTER TABLE owner_group_buy_requests
    ADD COLUMN thumbnail_key VARCHAR(500) NULL AFTER thumbnail_url;

ALTER TABLE owner_group_buy_request_images
    ADD COLUMN image_key VARCHAR(500) NULL AFTER image_url;
