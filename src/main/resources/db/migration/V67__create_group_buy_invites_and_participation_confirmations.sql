-- 소비자단 테이블 재설계 — 초대 링크 + 최종 확약 이력
-- 설계 근거: docs/table-redesign-consumer-together-buy.md § 2.5, § 2.6, § 3.3, § 4.1.4

CREATE TABLE group_buy_invites
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_buy_id       BIGINT       NOT NULL,
    token              VARCHAR(64)  NOT NULL,
    created_by_user_id BIGINT       NOT NULL,
    share_channel      VARCHAR(20)  NOT NULL,
    visit_count        INT          NOT NULL DEFAULT 0,
    last_visited_at    DATETIME     NULL,
    revoked_at         DATETIME     NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    CONSTRAINT fk_group_buy_invites_group_buy_id
        FOREIGN KEY (group_buy_id) REFERENCES group_buys (id),
    CONSTRAINT fk_group_buy_invites_created_by_user_id
        FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT uk_group_buy_invites_token UNIQUE (token)
);

CREATE INDEX idx_group_buy_invites_group_buy_id ON group_buy_invites (group_buy_id);
CREATE INDEX idx_group_buy_invites_created_by_user_id ON group_buy_invites (created_by_user_id);

CREATE TABLE participation_confirmations
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    participation_id BIGINT       NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    requested_at     DATETIME     NOT NULL,
    responded_at     DATETIME     NULL,
    deadline         DATETIME     NOT NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    CONSTRAINT fk_participation_confirmations_participation_id
        FOREIGN KEY (participation_id) REFERENCES participation (id)
);

CREATE INDEX idx_participation_confirmations_participation_status
    ON participation_confirmations (participation_id, status);
CREATE INDEX idx_participation_confirmations_deadline_status
    ON participation_confirmations (deadline, status);
