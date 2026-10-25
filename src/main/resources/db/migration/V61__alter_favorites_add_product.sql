ALTER TABLE favorites
    MODIFY COLUMN group_buy_id BIGINT NULL,
    ADD COLUMN product_id BIGINT NULL;

ALTER TABLE favorites
    ADD CONSTRAINT fk_favorites_product_id
        FOREIGN KEY (product_id) REFERENCES products (id),
    ADD CONSTRAINT chk_favorites_single_target
        CHECK (
            (group_buy_id IS NOT NULL AND product_id IS NULL)
            OR (group_buy_id IS NULL AND product_id IS NOT NULL)
        );

CREATE UNIQUE INDEX uk_favorites_user_product ON favorites (user_id, product_id);
CREATE INDEX idx_favorites_product_id ON favorites (product_id);
