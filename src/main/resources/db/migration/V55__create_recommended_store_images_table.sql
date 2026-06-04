CREATE TABLE recommended_store_images
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_key  VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    CONSTRAINT uk_recommended_store_images_sort_order UNIQUE (sort_order)
);

CREATE INDEX idx_recommended_store_images_active_sort
    ON recommended_store_images (active, sort_order);
