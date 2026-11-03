-- 소비자단 테이블 재설계 — 신고 플로우를 cs_tickets 로 통합
-- 설계 근거: docs/table-change-decisions-eunseo.md 결정 3
-- type 값 확장(FRAUD_SUSPICION / UNPAID_SETTLEMENT / INAPPROPRIATE_BEHAVIOR)은
-- 코드 레벨(enum) 변경 — 스키마는 reported_user_id, ticket_number 2개 추가 + 인덱스.

ALTER TABLE cs_tickets
    ADD COLUMN reported_user_id BIGINT      NULL,
    ADD COLUMN ticket_number    VARCHAR(30) NULL,
    ADD CONSTRAINT fk_cs_tickets_reported_user_id
        FOREIGN KEY (reported_user_id) REFERENCES users (id),
    ADD CONSTRAINT uk_cs_tickets_ticket_number UNIQUE (ticket_number);

CREATE INDEX idx_cs_tickets_reported_user_id ON cs_tickets (reported_user_id);
CREATE INDEX idx_cs_tickets_type_status_created_at
    ON cs_tickets (type, status, created_at);
