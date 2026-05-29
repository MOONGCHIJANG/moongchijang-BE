-- key 기반 이미지 저장 단일화를 위해 URL 컬럼을 제거한다.

ALTER TABLE group_buys
    DROP COLUMN thumbnail_url;

ALTER TABLE group_buy_images
    DROP COLUMN image_url;

ALTER TABLE owner_group_buy_requests
    DROP COLUMN thumbnail_url;

ALTER TABLE owner_group_buy_request_images
    DROP COLUMN image_url;
