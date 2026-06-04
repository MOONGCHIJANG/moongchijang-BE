CREATE TABLE search_corrections
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_keyword VARCHAR(100) NOT NULL,
    target_keyword VARCHAR(100) NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    hit_count      BIGINT       NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    CONSTRAINT uk_search_corrections_source_keyword UNIQUE (source_keyword)
);

INSERT INTO search_corrections (source_keyword, target_keyword, type, enabled, hit_count, created_at, updated_at)
VALUES ('카래', '카레', 'TYPO', TRUE, 0, NOW(), NOW()),
       ('소굼빵', '소금빵', 'TYPO', TRUE, 0, NOW(), NOW()),
       ('시오빵', '소금빵', 'ALIAS', TRUE, 0, NOW(), NOW()),
       ('부대찌게', '부대찌개', 'TYPO', TRUE, 0, NOW(), NOW()),
       ('떡복이', '떡볶이', 'TYPO', TRUE, 0, NOW(), NOW());
