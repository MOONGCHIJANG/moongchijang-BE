CREATE TABLE stores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    phone_number VARCHAR(20) NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    region VARCHAR(30) NOT NULL,
    district VARCHAR(80) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE group_buys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    group_buy_request_id BIGINT NOT NULL,
    thumbnail_url VARCHAR(500) NULL,
    product_name VARCHAR(100) NOT NULL,
    product_description TEXT NOT NULL,
    price INT NOT NULL,
    target_quantity INT NOT NULL,
    current_quantity INT NOT NULL,
    max_quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    deadline DATETIME NOT NULL,
    pickup_date DATE NOT NULL,
    pickup_time_start TIME NOT NULL,
    pickup_time_end TIME NOT NULL,
    pickup_location VARCHAR(200) NOT NULL,
    share_count INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_group_buys_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_group_buys_request_id FOREIGN KEY (group_buy_request_id) REFERENCES group_buy_requests (id)
);

CREATE INDEX idx_group_buys_store_id ON group_buys (store_id);
CREATE INDEX idx_group_buys_group_buy_request_id ON group_buys (group_buy_request_id);
CREATE INDEX idx_group_buys_status ON group_buys (status);
CREATE INDEX idx_group_buys_deadline ON group_buys (deadline);

CREATE TABLE group_buy_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_buy_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_group_buy_images_group_buy_id FOREIGN KEY (group_buy_id) REFERENCES group_buys (id)
);

CREATE INDEX idx_group_buy_images_group_buy_id ON group_buy_images (group_buy_id);

CREATE TABLE store_staff (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_store_staff_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_staff_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_store_staff_store_id ON store_staff (store_id);
CREATE INDEX idx_store_staff_user_id ON store_staff (user_id);
CREATE UNIQUE INDEX uk_store_staff_store_user ON store_staff (store_id, user_id);

ALTER TABLE group_buy_requests
    ADD CONSTRAINT fk_group_buy_requests_opened_group_buy_id
        FOREIGN KEY (opened_group_buy_id) REFERENCES group_buys (id);
