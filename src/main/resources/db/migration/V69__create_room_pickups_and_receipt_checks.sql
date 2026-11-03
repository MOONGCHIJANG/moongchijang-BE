-- 소비자단 테이블 재설계 — 방 QR 1장 + 참여자 "받았어요" 체크
-- 설계 근거: docs/table-redesign-consumer-together-buy.md § 2.10, § 2.11, § 6.1~6.3

CREATE TABLE room_pickups
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_buy_id    BIGINT        NOT NULL,
    qr_token        VARCHAR(100)  NOT NULL,
    active_from     DATETIME      NOT NULL,
    used_at         DATETIME      NULL,
    used_by_user_id BIGINT        NULL,
    no_show_at      DATETIME      NULL,
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,
    CONSTRAINT fk_room_pickups_group_buy_id
        FOREIGN KEY (group_buy_id) REFERENCES group_buys (id),
    CONSTRAINT fk_room_pickups_used_by_user_id
        FOREIGN KEY (used_by_user_id) REFERENCES users (id),
    CONSTRAINT uk_room_pickups_group_buy_id UNIQUE (group_buy_id),
    CONSTRAINT uk_room_pickups_qr_token UNIQUE (qr_token)
);

CREATE INDEX idx_room_pickups_active_from_used_at
    ON room_pickups (active_from, used_at);

CREATE TABLE participant_receipt_checks
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    participation_id BIGINT   NOT NULL,
    received_at      DATETIME NOT NULL,
    created_at       DATETIME NOT NULL,
    CONSTRAINT fk_participant_receipt_checks_participation_id
        FOREIGN KEY (participation_id) REFERENCES participation (id),
    CONSTRAINT uk_participant_receipt_checks_participation_id
        UNIQUE (participation_id)
);
