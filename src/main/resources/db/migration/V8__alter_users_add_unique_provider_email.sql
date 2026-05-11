ALTER TABLE users
    ADD CONSTRAINT uidx_users_provider_email UNIQUE (provider, email);
