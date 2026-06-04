INSERT INTO user_role_assignments (user_id, role, created_at, updated_at)
SELECT u.id, 'BUYER', NOW(), NOW()
FROM users u
WHERE u.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM user_role_assignments ura
    WHERE ura.user_id = u.id
      AND ura.role = 'BUYER'
  );

INSERT INTO user_role_assignments (user_id, role, created_at, updated_at)
SELECT u.id, 'SELLER', NOW(), NOW()
FROM users u
WHERE u.deleted_at IS NULL
  AND (u.role = 'SELLER' OR u.seller_signup_completed = TRUE)
  AND NOT EXISTS (
    SELECT 1
    FROM user_role_assignments ura
    WHERE ura.user_id = u.id
      AND ura.role = 'SELLER'
  );
