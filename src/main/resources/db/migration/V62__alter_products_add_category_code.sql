ALTER TABLE products
    ADD COLUMN category_code VARCHAR(50) NOT NULL DEFAULT 'ETC';

CREATE INDEX idx_products_category_status_created_at
    ON products (category_code, status, created_at);
