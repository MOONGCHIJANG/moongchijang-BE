-- 소비자단 테이블 재설계 — 매장 준비기간 + group_buys "방" 전환 필드 추가
-- 설계 근거: docs/table-redesign-consumer-together-buy.md § 3.1, § 3.2
-- 결정 1(table-change-decisions-eunseo.md): group_buys 테이블명 유지

ALTER TABLE stores
    ADD COLUMN preparation_days INT NOT NULL DEFAULT 3,
    ADD COLUMN max_lead_days INT NOT NULL DEFAULT 14;

ALTER TABLE group_buys
    ADD COLUMN organizer_user_id BIGINT NULL,
    ADD COLUMN min_quantity INT NULL,
    ADD COLUMN is_solo_room BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN resale_prohibition_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN payment_deadline DATETIME NULL,
    ADD COLUMN locked_at DATETIME NULL,
    ADD COLUMN confirmation_deadline DATETIME NULL,
    ADD COLUMN paid_at DATETIME NULL,
    ADD COLUMN store_closed_pickup_change BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT fk_group_buys_organizer_user_id
        FOREIGN KEY (organizer_user_id) REFERENCES users (id);

CREATE INDEX idx_group_buys_organizer_user_id ON group_buys (organizer_user_id);
CREATE INDEX idx_group_buys_status_payment_deadline ON group_buys (status, payment_deadline);
CREATE INDEX idx_group_buys_locked_at ON group_buys (locked_at);
