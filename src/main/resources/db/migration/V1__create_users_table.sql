CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NULL,
    email VARCHAR(255) NULL,
    password_hash VARCHAR(255) NULL,
    nickname VARCHAR(10) NULL,
    phone_number VARCHAR(20) NULL,
    role VARCHAR(20) NOT NULL,
    signup_completed BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_users_provider_provider_id ON users (provider, provider_id);
CREATE INDEX idx_users_email ON users (email);
