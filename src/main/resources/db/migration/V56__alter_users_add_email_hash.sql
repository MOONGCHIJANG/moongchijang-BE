ALTER TABLE users
    ADD COLUMN email_hash VARCHAR(64) NULL AFTER email,
    ADD CONSTRAINT uidx_users_provider_email_hash UNIQUE (provider, email_hash),
    ADD INDEX idx_users_email_hash (email_hash);
