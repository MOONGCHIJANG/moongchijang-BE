-- 상품 카탈로그와 함께 사기 방(group_buys)을 연결한다.
-- 레거시 방은 상품 스냅샷만 보유하므로 product_id를 NULL 허용으로 유지한다.
ALTER TABLE group_buys
    ADD COLUMN product_id BIGINT NULL,
    ADD CONSTRAINT fk_group_buys_product_id
        FOREIGN KEY (product_id) REFERENCES products (id);

CREATE INDEX idx_group_buys_product_status
    ON group_buys (product_id, status);
