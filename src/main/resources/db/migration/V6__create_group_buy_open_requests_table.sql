CREATE TABLE group_buy_open_requests
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    region              VARCHAR(50)  NOT NULL,
    product_name        VARCHAR(100) NOT NULL,
    notification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    INDEX idx_open_req_user_region_product (user_id, region, product_name)
);
