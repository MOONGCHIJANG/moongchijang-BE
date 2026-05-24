CREATE TABLE owner_group_buy_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_description TEXT NOT NULL,
    original_price INT NULL,
    price INT NOT NULL,
    target_quantity INT NOT NULL,
    max_quantity INT NOT NULL,
    per_user_limit INT NULL,
    thumbnail_url VARCHAR(500) NOT NULL,
    deadline DATETIME NOT NULL,
    pickup_date DATE NOT NULL,
    pickup_time_start TIME NOT NULL,
    pickup_time_end TIME NOT NULL,
    pickup_location VARCHAR(200) NOT NULL,
    pickup_contact VARCHAR(20) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(200) NULL,
    approved_group_buy_id BIGINT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_owner_gbr_owner_id FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_owner_gbr_store_id FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_owner_gbr_approved_group_buy_id FOREIGN KEY (approved_group_buy_id) REFERENCES group_buys (id),
    CONSTRAINT fk_owner_gbr_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id)
);

CREATE INDEX idx_owner_gbr_owner_id ON owner_group_buy_requests (owner_id);
CREATE INDEX idx_owner_gbr_store_id ON owner_group_buy_requests (store_id);
CREATE INDEX idx_owner_gbr_status ON owner_group_buy_requests (status);
CREATE INDEX idx_owner_gbr_created_at ON owner_group_buy_requests (created_at);

CREATE TABLE owner_group_buy_request_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_owner_gbr_images_request_id FOREIGN KEY (request_id) REFERENCES owner_group_buy_requests (id)
);

CREATE INDEX idx_owner_gbr_images_request_id ON owner_group_buy_request_images (request_id);
CREATE UNIQUE INDEX uk_owner_gbr_images_request_sort ON owner_group_buy_request_images (request_id, sort_order);
