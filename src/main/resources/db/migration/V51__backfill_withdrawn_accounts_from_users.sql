INSERT INTO withdrawn_accounts (
    provider,
    provider_id,
    email,
    withdrawn_user_id,
    withdrawn_at,
    rejoin_available_at,
    created_at,
    updated_at
)
SELECT
    u.provider,
    u.provider_id,
    u.email,
    u.id,
    u.deleted_at,
    DATE_ADD(u.deleted_at, INTERVAL 30 DAY),
    u.created_at,
    u.updated_at
FROM users u
WHERE u.deleted_at IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM withdrawn_accounts wa
      WHERE wa.withdrawn_user_id = u.id
  );
