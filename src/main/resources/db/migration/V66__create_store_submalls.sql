-- 소비자단 테이블 재설계 — PG 서브몰 매핑
-- 설계 근거: docs/table-redesign-consumer-together-buy.md § 2.4, § 4.3.3

CREATE TABLE store_submalls
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id            BIGINT       NOT NULL,
    pg_provider         VARCHAR(20)  NOT NULL,
    submall_merchant_id VARCHAR(100) NOT NULL,
    submall_status      VARCHAR(20)  NOT NULL,
    registered_at       DATETIME     NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    CONSTRAINT fk_store_submalls_store_id
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT uk_store_submalls_store_id UNIQUE (store_id)
);

CREATE INDEX idx_store_submalls_merchant_id ON store_submalls (submall_merchant_id);

ALTER TABLE payment_orders
    ADD COLUMN store_submall_id BIGINT NULL,
    ADD CONSTRAINT fk_payment_orders_store_submall_id
        FOREIGN KEY (store_submall_id) REFERENCES store_submalls (id);
