CREATE TABLE user_role_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_role_assignments_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_role_assignments_user_id ON user_role_assignments (user_id);
CREATE INDEX idx_user_role_assignments_role ON user_role_assignments (role);
CREATE UNIQUE INDEX uidx_user_role_assignments_user_id_role ON user_role_assignments (user_id, role);

INSERT INTO user_role_assignments (user_id, role, created_at, updated_at)
SELECT id, role, NOW(), NOW()
FROM users;
