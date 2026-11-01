-- 소비자단 테이블 재설계 — 방 정산 세션 + 참여자 송금 상태 + 재발송 레이트리밋
-- 설계 근거: docs/table-redesign-consumer-together-buy.md § 2.7, § 2.8, § 2.9
-- 보안: account_number / account_holder_name 은 애플리케이션 레벨 AES-GCM 암호화

CREATE TABLE room_settlements
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_buy_id             BIGINT         NOT NULL,
    organizer_user_id        BIGINT         NOT NULL,
    bank_code                VARCHAR(20)    NOT NULL,
    account_number_encrypted VARBINARY(255) NOT NULL,
    account_holder_name_enc  VARBINARY(255) NOT NULL,
    third_party_consent_at   DATETIME       NOT NULL,
    settlement_deadline      DATETIME       NOT NULL,
    status                   VARCHAR(20)    NOT NULL,
    closed_at                DATETIME       NULL,
    masked_at                DATETIME       NULL,
    created_at               DATETIME       NOT NULL,
    updated_at               DATETIME       NOT NULL,
    CONSTRAINT fk_room_settlements_group_buy_id
        FOREIGN KEY (group_buy_id) REFERENCES group_buys (id),
    CONSTRAINT fk_room_settlements_organizer_user_id
        FOREIGN KEY (organizer_user_id) REFERENCES users (id),
    CONSTRAINT uk_room_settlements_group_buy_id UNIQUE (group_buy_id)
);

CREATE INDEX idx_room_settlements_organizer_user_id
    ON room_settlements (organizer_user_id);
CREATE INDEX idx_room_settlements_status_deadline
    ON room_settlements (status, settlement_deadline);

CREATE TABLE settlement_transfers
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_settlement_id BIGINT       NOT NULL,
    participation_id   BIGINT       NOT NULL,
    expected_amount    INT          NOT NULL,
    deposit_name       VARCHAR(50)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    submitted_at       DATETIME     NULL,
    confirmed_at       DATETIME     NULL,
    unpaid_at          DATETIME     NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    CONSTRAINT fk_settlement_transfers_room_settlement_id
        FOREIGN KEY (room_settlement_id) REFERENCES room_settlements (id),
    CONSTRAINT fk_settlement_transfers_participation_id
        FOREIGN KEY (participation_id) REFERENCES participation (id),
    CONSTRAINT uk_settlement_transfers_room_participation
        UNIQUE (room_settlement_id, participation_id)
);

CREATE INDEX idx_settlement_transfers_status_submitted_at
    ON settlement_transfers (status, submitted_at);

CREATE TABLE settlement_reminders
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_transfer_id BIGINT   NOT NULL,
    sent_at                DATETIME NOT NULL,
    sent_by_user_id        BIGINT   NOT NULL,
    created_at             DATETIME NOT NULL,
    CONSTRAINT fk_settlement_reminders_settlement_transfer_id
        FOREIGN KEY (settlement_transfer_id) REFERENCES settlement_transfers (id),
    CONSTRAINT fk_settlement_reminders_sent_by_user_id
        FOREIGN KEY (sent_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_settlement_reminders_transfer_sent_at
    ON settlement_reminders (settlement_transfer_id, sent_at);
