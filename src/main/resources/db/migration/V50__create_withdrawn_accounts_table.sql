CREATE TABLE withdrawn_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NULL,
    email VARCHAR(255) NULL,
    withdrawn_user_id BIGINT NOT NULL,
    withdrawn_at DATETIME NOT NULL,
    rejoin_available_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uidx_withdrawn_accounts_provider_provider_id (provider, provider_id),
    UNIQUE KEY uidx_withdrawn_accounts_provider_email (provider, email),
    KEY idx_withdrawn_accounts_rejoin_available_at (rejoin_available_at),
    KEY idx_withdrawn_accounts_withdrawn_user_id (withdrawn_user_id)
);
