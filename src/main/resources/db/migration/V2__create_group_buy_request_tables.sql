CREATE TABLE group_buy_requests (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    store_name          VARCHAR(100) NOT NULL,
    store_address       VARCHAR(200) NULL,
    product_name        VARCHAR(100) NOT NULL,
    desired_quantity    INT          NOT NULL,
    desired_pickup_date DATE         NOT NULL,
    additional_note     VARCHAR(500) NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    rejection_reason    TEXT         NULL,
    opened_group_buy_id BIGINT       NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_group_buy_requests_user_id ON group_buy_requests (user_id);

CREATE TABLE group_buy_request_status_histories (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    group_buy_request_id  BIGINT      NOT NULL,
    status                VARCHAR(20) NOT NULL,
    changed_at            DATETIME    NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_gbr_status_histories_request_id ON group_buy_request_status_histories (group_buy_request_id);
