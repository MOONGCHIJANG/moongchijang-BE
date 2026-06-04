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

INSERT INTO recommended_store_images (image_key, sort_order, active, created_at, updated_at)
VALUES
    ('dev/group-buys/pending/4/20260604/thumbnail/36a2ee7f-6c7f-4c70-a434-5a23932fe279.jpeg', 1, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/f5ae0151-bf93-4a90-b7a3-5f554d849238.jpeg', 2, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/db54e483-acb6-4b28-b385-e16bc6c76c69.jpeg', 3, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/2b2a53f4-405e-4a1e-ba61-60f2313368fc.jpeg', 4, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/530fdfd9-2a9d-4541-a9f4-d3d18569c0f5.jpeg', 5, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/7c4c80f5-7abe-4ab1-bdac-276c3dd84b95.jpeg', 6, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/05f0959c-782d-4b4c-84bd-0635890fdba9.jpeg', 7, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/50370684-f353-48e4-af32-0bc2a271723e.jpeg', 8, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/8df1eee9-7cfe-460d-b327-042e75dc92c4.jpeg', 9, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/3c666f6e-bbfa-4ec1-828e-83a08627ab7d.jpeg', 10, TRUE, NOW(), NOW()),
    ('dev/group-buys/pending/4/20260604/products/54e47f9c-c8b0-4830-8c9d-0db5af2d9cf2.jpeg', 11, TRUE, NOW(), NOW());
