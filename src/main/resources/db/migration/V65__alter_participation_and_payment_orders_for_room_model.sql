-- 소비자단 테이블 재설계 — participation 확약 필드 추가 + payment_orders 동의 재정의
-- 설계 근거: docs/table-redesign-consumer-together-buy.md § 3.3, § 3.4, § 4.1.4, § 4.2
-- 결정 2(table-change-decisions-eunseo.md): participation.product_amount 스냅샷 유지
-- 참고: 기존 fee_amount/total_amount, pickup_token, 환불 필드, 기존 동의 필드는
--      애플리케이션 전환 완료 후 별도 PR에서 DROP.

ALTER TABLE participation
    ADD COLUMN purpose_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN confirmation_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUESTED',
    ADD COLUMN confirmed_at DATETIME NULL,
    ADD COLUMN excluded_at DATETIME NULL,
    ADD COLUMN reported_as_unpaid_at DATETIME NULL;

CREATE INDEX idx_participation_confirmation_status
    ON participation (confirmation_status);

ALTER TABLE payment_orders
    ADD COLUMN agreed_order_summary BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN agreed_production_no_cancel BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN agreed_pickup_store_policy BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN agreed_privacy BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN agreed_intermediary BOOLEAN NOT NULL DEFAULT FALSE;
