CREATE TABLE group_buy_embeddings
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_buy_id BIGINT NOT NULL UNIQUE,
    embedding    JSON   NOT NULL,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL,
    INDEX idx_group_buy_embeddings_group_buy_id (group_buy_id)
);
