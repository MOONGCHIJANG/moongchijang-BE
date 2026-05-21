ALTER TABLE group_buy_requests
    ADD COLUMN place_id VARCHAR(100) NULL,
    ADD COLUMN road_address VARCHAR(200) NULL,
    ADD COLUMN lot_address VARCHAR(200) NULL,
    ADD COLUMN latitude DOUBLE NULL,
    ADD COLUMN longitude DOUBLE NULL;
