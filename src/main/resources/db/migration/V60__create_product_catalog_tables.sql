ALTER TABLE stores
    ADD COLUMN place_id VARCHAR(100) NULL,
    ADD COLUMN business_registration_number VARCHAR(20) NULL,
    ADD COLUMN representative_name VARCHAR(50) NULL,
    ADD COLUMN business_address VARCHAR(255) NULL,
    ADD COLUMN sales_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_stores_place_id ON stores (place_id);
CREATE INDEX idx_stores_sales_status ON stores (sales_status);

CREATE TABLE store_holidays
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id     BIGINT       NOT NULL,
    holiday_type VARCHAR(20)   NOT NULL,
    day_of_week  TINYINT       NULL,
    holiday_date DATE          NULL,
    reason       VARCHAR(255)  NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   DATETIME      NOT NULL,
    updated_at   DATETIME      NOT NULL,
    CONSTRAINT fk_store_holidays_store_id
        FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE INDEX idx_store_holidays_store_active_day
    ON store_holidays (store_id, active, day_of_week);
CREATE INDEX idx_store_holidays_store_active_date
    ON store_holidays (store_id, active, holiday_date);

CREATE TABLE store_pickup_time_slots
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id      BIGINT      NOT NULL,
    day_of_week   TINYINT     NOT NULL,
    start_time    TIME        NOT NULL,
    end_time      TIME        NOT NULL,
    slot_capacity INT         NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    DATETIME    NOT NULL,
    updated_at    DATETIME    NOT NULL,
    CONSTRAINT fk_store_pickup_time_slots_store_id
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT uk_store_pickup_time_slots_schedule
        UNIQUE (store_id, day_of_week, start_time, end_time)
);

CREATE INDEX idx_store_pickup_time_slots_store_active_day
    ON store_pickup_time_slots (store_id, active, day_of_week);

CREATE TABLE products
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id         BIGINT       NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      TEXT         NOT NULL,
    unit_price       INT          NOT NULL,
    min_quantity     INT          NOT NULL,
    max_quantity     INT          NOT NULL,
    preparation_days INT          NOT NULL,
    storage_type     VARCHAR(30)  NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    thumbnail_key    VARCHAR(500) NOT NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    CONSTRAINT fk_products_store_id
        FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE INDEX idx_products_store_status ON products (store_id, status);
CREATE INDEX idx_products_status_created_at ON products (status, created_at);

CREATE TABLE product_images
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL,
    image_key  VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    CONSTRAINT fk_product_images_product_id
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uk_product_images_product_sort_order
        UNIQUE (product_id, sort_order)
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);
