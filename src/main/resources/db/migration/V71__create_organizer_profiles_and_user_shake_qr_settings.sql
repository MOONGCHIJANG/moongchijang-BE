-- 소비자단 테이블 재설계 — 총대 프로필(완료 횟수·신고 누적) + 흔들기 QR 토글
-- 설계 근거: docs/table-redesign-consumer-together-buy.md § 2.13, § 2.14, § 6.2.3, § 8.2.2

CREATE TABLE organizer_profiles
(
    user_id             BIGINT   NOT NULL PRIMARY KEY,
    completed_rooms_cnt INT      NOT NULL DEFAULT 0,
    reported_count      INT      NOT NULL DEFAULT 0,
    last_completed_at   DATETIME NULL,
    updated_at          DATETIME NOT NULL,
    CONSTRAINT fk_organizer_profiles_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_shake_qr_settings
(
    user_id    BIGINT   NOT NULL PRIMARY KEY,
    enabled    BOOLEAN  NOT NULL DEFAULT TRUE,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_user_shake_qr_settings_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
);
