ALTER TABLE withdrawn_accounts
    DROP INDEX uidx_withdrawn_accounts_provider_provider_id,
    DROP INDEX uidx_withdrawn_accounts_provider_email,
    DROP COLUMN provider_id,
    DROP COLUMN email;
