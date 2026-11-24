CREATE TABLE store_business_hours
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id    BIGINT      NOT NULL,
    day_of_week INT         NOT NULL,
    open_time   TIME        NULL,
    close_time  TIME        NULL,
    is_closed   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  DATETIME    NOT NULL,
    updated_at  DATETIME    NOT NULL,
    CONSTRAINT fk_store_business_hours_store_id
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT uk_store_business_hours_store_day
        UNIQUE (store_id, day_of_week)
);

ALTER TABLE store_holidays
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'ADMIN';
