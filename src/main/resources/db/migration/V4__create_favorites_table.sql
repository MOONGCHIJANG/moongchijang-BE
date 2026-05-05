CREATE TABLE favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    group_buy_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_favorites_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_favorites_group_buy_id FOREIGN KEY (group_buy_id) REFERENCES group_buys (id)
);

CREATE UNIQUE INDEX uk_favorites_user_group_buy ON favorites (user_id, group_buy_id);
CREATE INDEX idx_favorites_user_id ON favorites (user_id);
CREATE INDEX idx_favorites_group_buy_id ON favorites (group_buy_id);
