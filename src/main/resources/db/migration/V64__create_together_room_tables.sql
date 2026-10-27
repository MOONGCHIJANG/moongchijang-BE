CREATE TABLE together_rooms
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id             BIGINT      NOT NULL,
    store_id               BIGINT      NOT NULL,
    leader_user_id         BIGINT      NOT NULL,
    room_type              VARCHAR(20) NOT NULL,
    status                 VARCHAR(30) NOT NULL,
    pickup_date            DATE        NOT NULL,
    pickup_time_start      TIME        NOT NULL,
    pickup_time_end        TIME        NOT NULL,
    payment_deadline_at    DATETIME    NOT NULL,
    settlement_deadline_at DATETIME    NULL,
    current_quantity       INT         NOT NULL,
    total_amount           INT         NOT NULL,
    locked_at              DATETIME    NULL,
    paid_at                DATETIME    NULL,
    pickup_completed_at    DATETIME    NULL,
    created_at             DATETIME    NOT NULL,
    updated_at             DATETIME    NOT NULL,
    CONSTRAINT fk_together_rooms_product_id
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_together_rooms_store_id
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_together_rooms_leader_user_id
        FOREIGN KEY (leader_user_id) REFERENCES users (id)
);

CREATE INDEX idx_together_rooms_product_status ON together_rooms (product_id, status);
CREATE INDEX idx_together_rooms_leader_status ON together_rooms (leader_user_id, status);
CREATE INDEX idx_together_rooms_pickup_status ON together_rooms (pickup_date, status, pickup_time_end);
CREATE INDEX idx_together_rooms_payment_deadline ON together_rooms (payment_deadline_at);

CREATE TABLE together_room_members
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id            BIGINT      NOT NULL,
    user_id            BIGINT      NOT NULL,
    role               VARCHAR(20) NOT NULL,
    status             VARCHAR(30) NOT NULL,
    quantity           INT         NOT NULL,
    amount             INT         NOT NULL,
    depositor_name     VARCHAR(50) NULL,
    final_confirmed_at DATETIME    NULL,
    cancelled_at       DATETIME    NULL,
    received_at        DATETIME    NULL,
    created_at         DATETIME    NOT NULL,
    updated_at         DATETIME    NOT NULL,
    CONSTRAINT fk_together_room_members_room_id
        FOREIGN KEY (room_id) REFERENCES together_rooms (id),
    CONSTRAINT fk_together_room_members_user_id
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_together_room_members_room_user
        UNIQUE (room_id, user_id)
);

CREATE INDEX idx_together_room_members_user_status ON together_room_members (user_id, status);
CREATE INDEX idx_together_room_members_room_status ON together_room_members (room_id, status);

CREATE TABLE participant_settlements
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id             BIGINT      NOT NULL,
    member_id           BIGINT      NOT NULL,
    participant_user_id BIGINT      NOT NULL,
    amount              INT         NOT NULL,
    status              VARCHAR(30) NOT NULL,
    transfer_marked_at  DATETIME    NULL,
    confirmed_by        BIGINT      NULL,
    confirmed_at        DATETIME    NULL,
    unpaid_marked_at    DATETIME    NULL,
    reminder_count      INT         NOT NULL DEFAULT 0,
    created_at          DATETIME    NOT NULL,
    updated_at          DATETIME    NOT NULL,
    CONSTRAINT fk_participant_settlements_room_id
        FOREIGN KEY (room_id) REFERENCES together_rooms (id),
    CONSTRAINT fk_participant_settlements_member_id
        FOREIGN KEY (member_id) REFERENCES together_room_members (id),
    CONSTRAINT fk_participant_settlements_participant_user_id
        FOREIGN KEY (participant_user_id) REFERENCES users (id),
    CONSTRAINT fk_participant_settlements_confirmed_by
        FOREIGN KEY (confirmed_by) REFERENCES users (id),
    CONSTRAINT uk_participant_settlements_member_id
        UNIQUE (member_id)
);

CREATE INDEX idx_participant_settlements_participant_status
    ON participant_settlements (participant_user_id, status);
CREATE INDEX idx_participant_settlements_room_status
    ON participant_settlements (room_id, status);

CREATE TABLE pickup_qr_codes
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id                  BIGINT       NOT NULL,
    qr_token                 VARCHAR(100) NOT NULL,
    reservation_number       VARCHAR(100) NOT NULL,
    active_from              DATETIME     NOT NULL,
    used_at                  DATETIME     NULL,
    used_by_store_staff_id   BIGINT       NULL,
    expires_at               DATETIME     NOT NULL,
    created_at               DATETIME     NOT NULL,
    updated_at               DATETIME     NOT NULL,
    CONSTRAINT fk_pickup_qr_codes_room_id
        FOREIGN KEY (room_id) REFERENCES together_rooms (id),
    CONSTRAINT fk_pickup_qr_codes_store_staff_id
        FOREIGN KEY (used_by_store_staff_id) REFERENCES store_staff (id),
    CONSTRAINT uk_pickup_qr_codes_room_id
        UNIQUE (room_id),
    CONSTRAINT uk_pickup_qr_codes_qr_token
        UNIQUE (qr_token),
    CONSTRAINT uk_pickup_qr_codes_reservation_number
        UNIQUE (reservation_number)
);

CREATE INDEX idx_pickup_qr_codes_active_expiry ON pickup_qr_codes (active_from, expires_at);
