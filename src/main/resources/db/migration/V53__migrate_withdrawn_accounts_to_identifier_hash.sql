ALTER TABLE withdrawn_accounts
    ADD COLUMN identifier_hash CHAR(64) NULL AFTER provider;

UPDATE withdrawn_accounts
SET identifier_hash = CASE
    WHEN provider = 'EMAIL' AND email IS NOT NULL AND TRIM(email) <> '' THEN SHA2(CONCAT(provider, ':', LOWER(TRIM(email))), 256)
    WHEN provider_id IS NOT NULL AND TRIM(provider_id) <> '' THEN SHA2(CONCAT(provider, ':', TRIM(provider_id)), 256)
    WHEN email IS NOT NULL AND TRIM(email) <> '' THEN SHA2(CONCAT(provider, ':', LOWER(TRIM(email))), 256)
    ELSE NULL
END
WHERE identifier_hash IS NULL;

DELETE FROM withdrawn_accounts
WHERE identifier_hash IS NULL;

ALTER TABLE withdrawn_accounts
    MODIFY COLUMN identifier_hash CHAR(64) NOT NULL,
    ADD UNIQUE KEY uidx_withdrawn_accounts_provider_identifier_hash (provider, identifier_hash);

UPDATE withdrawn_accounts
SET provider_id = NULL,
    email = NULL
WHERE identifier_hash IS NOT NULL;
